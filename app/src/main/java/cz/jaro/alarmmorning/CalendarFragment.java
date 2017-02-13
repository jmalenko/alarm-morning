package cz.jaro.alarmmorning;

import android.app.Fragment;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TimePicker;
import android.widget.Toast;

import java.util.Calendar;

import cz.jaro.alarmmorning.clock.Clock;
import cz.jaro.alarmmorning.graphics.RecyclerViewWithContextMenu;
import cz.jaro.alarmmorning.graphics.SimpleDividerItemDecoration;
import cz.jaro.alarmmorning.graphics.TimePickerDialogWithDisable;
import cz.jaro.alarmmorning.model.Day;
import cz.jaro.alarmmorning.model.Defaults;

import static cz.jaro.alarmmorning.calendar.CalendarUtils.addDay;
import static cz.jaro.alarmmorning.calendar.CalendarUtils.addDaysClone;
import static cz.jaro.alarmmorning.calendar.CalendarUtils.beginningOfToday;
import static cz.jaro.alarmmorning.calendar.CalendarUtils.onTheSameDate;

/**
 * Fragment that appears in the "content_frame".
 */
public class CalendarFragment extends Fragment implements View.OnClickListener, TimePickerDialogWithDisable.OnTimeSetWithDisableListener {

    private static final String TAG = CalendarFragment.class.getSimpleName();

    private CalendarAdapter adapter;

    private RecyclerView recyclerView;

    private Calendar today;
    private Day day; // day corresponding to position in variable position
    private int position; // position of the operation (via context menu)

    private int positionNextAlarm; // position of the next alarm
    private static final int POSITION_UNSET = -1; // constant representing "position of the next alarm" when no next alarm exists

    private final HandlerOnClockChange handler = new HandlerOnClockChange();

    public CalendarFragment() {
        // Empty constructor required for fragment subclasses
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_calendar, container, false);

        recyclerView = (RecyclerView) rootView.findViewById(R.id.calendar_recycler_view);

        // use a linear layout manager
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getActivity());
        recyclerView.setLayoutManager(layoutManager);

        // specify an adapter
        adapter = new CalendarAdapter(this);
        recyclerView.setAdapter(adapter);

        // item separator
        recyclerView.addItemDecoration(new SimpleDividerItemDecoration(getActivity()));

        // for disabling the animation on update
        DefaultItemAnimator animator = new DefaultItemAnimator();
        animator.setSupportsChangeAnimations(false);
        recyclerView.setItemAnimator(animator);

        registerForContextMenu(recyclerView);

        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();

        today = getToday(clock());
        positionNextAlarm = calcPositionNextAlarm();

        // Refresh all the alarm times. Solves scenario: Given displayed calendar, when set alarm by voice, then the calendar must refresh.
        adapter.notifyDataSetChanged();

        // Handler for refreshing the content
        handler.start(this::onSystemTimeChange, Calendar.SECOND);
    }

    @Override
    public void onPause() {
        super.onPause();

        handler.stop();
    }

    /*
     * Events
     * ======
     */

    public void onAlarmSet() {
        Log.d(TAG, "onAlarmSet()");

        adapter.notifyItemChanged(position);
        updatePositionNextAlarm();

        if (day != null) { // otherwise the alarm is set because a default alarm time was set. (The DefaultsActivity is running and I don't know why CalendarFragment gets the broadcast message...)
            String toastText = formatToastText(day);
            Toast.makeText(getActivity(), toastText, Toast.LENGTH_LONG).show();
        }
    }

    public void onDismissBeforeRinging() {
        Log.d(TAG, "onDismissBeforeRinging()");
        updatePositionNextAlarm();
    }

    public void onAlarmTimeOfEarlyDismissedAlarm() {
        Log.d(TAG, "onAlarmTimeOfEarlyDismissedAlarm()");
        adapter.notifyItemChanged(positionNextAlarm);
    }

    public void onRing() {
        Log.d(TAG, "onRing()");
        adapter.notifyItemChanged(positionNextAlarm);
    }

    public void onDismiss() {
        Log.d(TAG, "onDismiss()");
        updatePositionNextAlarm();
    }

    public void onSnooze() {
        Log.d(TAG, "onSnooze()");
        adapter.notifyItemChanged(positionNextAlarm);
    }

    public void onCancel() {
        Log.d(TAG, "onCancel()");
        updatePositionNextAlarm();
    }

    /*
     * External events
     * ===============
     */

    public void onTimeOrTimeZoneChange() {
        Log.d(TAG, "onTimeOrTimeZoneChange()");
        today = getToday(clock());
        adapter.notifyDataSetChanged();
    }

    private void onSystemTimeChange() {
        Log.v(TAG, "onSystemTimeChange()");

        // Update time to next alarm
        if (positionNextAlarm != POSITION_UNSET)
            adapter.notifyItemChanged(positionNextAlarm);

        // Shift items when date changes
        Calendar today2 = getToday(clock());

        if (!today.equals(today2)) {
            int diffInDays = -1;
            for (int i = 1; i < GlobalManager.HORIZON_DAYS; i++) {
                Calendar date = addDaysClone(today, i);
                if (today2.equals(date)) {
                    diffInDays = i;
                    break;
                }
            }

            today = today2;

            if (diffInDays != -1) {
                adapter.notifyItemRangeRemoved(0, diffInDays);
            } else {
                adapter.notifyDataSetChanged();
            }
        }
    }

    /*
     * Activity
     * ========
     */

    protected Clock clock() {
        GlobalManager globalManager = GlobalManager.getInstance();
        return globalManager.clock();
    }

    private int calcPositionNextAlarm() {
        GlobalManager globalManager = GlobalManager.getInstance();
        Day day = globalManager.getDayWithNextAlarmToRing();

        return day == null ? POSITION_UNSET : dayToPosition(day);
    }

    private int dayToPosition(Day day) {
        Calendar date = getToday(clock());

        for (int daysInAdvance = 0; daysInAdvance < GlobalManager.HORIZON_DAYS; daysInAdvance++, addDay(date)) {
            if (onTheSameDate(day.getDate(), date)) {
                return daysInAdvance;
            }
        }
        return POSITION_UNSET;
    }

    private void updatePositionNextAlarm() {
        int newPositionNextAlarm = calcPositionNextAlarm();

        if (positionNextAlarm != newPositionNextAlarm) {
            Log.d(TAG, "Next alarm is at position " + newPositionNextAlarm);

            int oldPositionNextAlarm = positionNextAlarm;
            positionNextAlarm = newPositionNextAlarm;

            if (oldPositionNextAlarm != POSITION_UNSET)
                adapter.notifyItemChanged(oldPositionNextAlarm);
            if (newPositionNextAlarm != POSITION_UNSET)
                adapter.notifyItemChanged(newPositionNextAlarm);
        }
    }

    public boolean positionWithNextAlarm(int position) {
        return position == positionNextAlarm;
    }

    private void setCurrent(Day day, int position) {
        this.day = day;
        this.position = position;
    }

    private void save(Day day) {
        Analytics analytics = new Analytics(Analytics.Channel.Activity, Analytics.ChannelName.Calendar);

        GlobalManager globalManager = GlobalManager.getInstance();
        globalManager.saveDay(day, analytics);

        adapter.notifyItemChanged(position);
        updatePositionNextAlarm();
    }

    private String formatToastText(Day day) {
        return formatToastText(getResources(), clock(), day);
    }

    static public String formatToastText(Resources res, Clock clock, Day day) {
        String toastText;

        if (!day.isEnabled()) {
            toastText = res.getString(R.string.time_to_ring_toast_off);
        } else {
            long diff = day.getTimeToRing(clock);

            if (diff < 0) {
                toastText = res.getString(R.string.time_to_ring_toast_passed);
            } else {
                CalendarAdapter.TimeDifference timeDifference = CalendarAdapter.TimeDifference.split(diff);
                if (timeDifference.days > 0) {
                    toastText = res.getString(R.string.time_to_ring_toast_days, timeDifference.days, timeDifference.hours, timeDifference.minutes);
                } else if (timeDifference.hours > 0) {
                    toastText = res.getString(R.string.time_to_ring_toast_hours, timeDifference.hours, timeDifference.minutes);
                } else {
                    toastText = res.getString(R.string.time_to_ring_toast_minutes, timeDifference.minutes, timeDifference.seconds);
                }
            }
        }
        return toastText;
    }

    public Day loadPosition(int position) {
        GlobalManager globalManager = GlobalManager.getInstance();
        Calendar date = addDaysClone(today, position);
        return globalManager.loadDay(date);
    }

    public static Calendar getToday(Clock clock) {
        Calendar now = clock.now();
        return beginningOfToday(now);
    }

    /**
     * Update all the calendar items.
     */
    public void refresh() {
        adapter.notifyDataSetChanged();
    }

    /*
     * On click
     * ========
     */

    @Override
    public void onClick(View view) {
        int position = recyclerView.getChildAdapterPosition(view);
        Log.d(TAG, "Clicked item on position " + position);

        Day day = loadPosition(position);
        setCurrent(day, position);

        showTimePicker();
    }

    private void showTimePicker() {
        TimePickerFragment fragment = new TimePickerFragment();

        fragment.setOnTimeSetListener(this);

        // Preset time
        Calendar now = clock().now();

        GlobalManager globalManager = GlobalManager.getInstance();
        int state = globalManager.getState(day.getDateTime());
        boolean presetNap = position == 0 && (day.isEnabled() ?
                (state == GlobalManager.STATE_SNOOZED || state == GlobalManager.STATE_DISMISSED || state == GlobalManager.STATE_DISMISSED_BEFORE_RINGING)
                : now.after(day.getDateTime()));

        Bundle bundle = new Bundle();
        if (presetNap) {
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
            int napTime = preferences.getInt(SettingsActivity.PREF_NAP_TIME, SettingsActivity.PREF_NAP_TIME_DEFAULT);

            now.add(Calendar.MINUTE, napTime);

            bundle.putInt(TimePickerFragment.HOURS, now.get(Calendar.HOUR_OF_DAY));
            bundle.putInt(TimePickerFragment.MINUTES, now.get(Calendar.MINUTE));
        } else {
            bundle.putInt(TimePickerFragment.HOURS, day.getHourX());
            bundle.putInt(TimePickerFragment.MINUTES, day.getMinuteX());
        }
        fragment.setArguments(bundle);

        fragment.show(getFragmentManager(), "timePicker");
    }

    @Override
    public void onTimeSetWithDisable(TimePicker view, boolean disable, int hourOfDay, int minute) {
        if (view.isShown()) {
            if (disable) {
                day.setState(Day.STATE_DISABLED);
            } else {
                day.setState(Day.STATE_ENABLED);
                day.setHour(hourOfDay);
                day.setMinute(minute);
            }

            save(day);
        }
    }

    /*
     * On context menu
     * ===============
     */

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        int position = ((RecyclerViewWithContextMenu.RecyclerViewContextMenuInfo) menuInfo).position;
        Log.d(TAG, "Long clicked item on position " + position);

        MenuInflater inflater = getActivity().getMenuInflater();
        inflater.inflate(R.menu.day_context_menu, menu);

        Day day = loadPosition(position);
        setCurrent(day, position);

        // Set header

        Calendar date = day.getDate();
        int dayOfWeek = date.get(Calendar.DAY_OF_WEEK);
        String dayOfWeekText = Localization.dayOfWeekToStringShort(getResources(), dayOfWeek);

        String dateText = Localization.dateToStringVeryShort(getResources(), date.getTime());

        String headerTitle;
        if (day.isEnabled()) {
            String timeText = Localization.timeToString(day.getHourX(), day.getMinuteX(), getActivity());
            headerTitle = getResources().getString(R.string.menu_day_header, timeText, dayOfWeekText, dateText);
        } else {
            headerTitle = getResources().getString(R.string.menu_day_header_disabled, dayOfWeekText, dateText);
        }
        menu.setHeaderTitle(headerTitle);

        // Set title
        MenuItem revertItem = menu.findItem(R.id.day_revert);
        Defaults defaults = day.getDefaults();
        String timeText;
        if (defaults.isEnabled()) {
            timeText = Localization.timeToString(defaults.getHour(), defaults.getMinute(), getActivity());
        } else {
            timeText = getResources().getString(R.string.alarm_unset);
        }
        revertItem.setTitle(getString(R.string.action_revert, timeText));

        // Hide irrelevant items

        MenuItem setTime = menu.findItem(R.id.day_set_time);
        MenuItem disable = menu.findItem(R.id.day_disable);
        MenuItem revert = menu.findItem(R.id.day_revert);
        MenuItem dismiss = menu.findItem(R.id.day_dismiss);
        MenuItem snooze = menu.findItem(R.id.day_snooze);

        GlobalManager globalManager = GlobalManager.getInstance();
        int state = globalManager.getState(day.getDateTime());
        switch (state) {
            case GlobalManager.STATE_FUTURE:
                if (position == positionNextAlarm && day.isEnabled() && globalManager.afterNearFuture(day.getDateTime())) {
                    disable.setVisible(false);
                    revert.setVisible(false);
                    dismiss.setVisible(true);
                } else {
                    disable.setVisible(true);
                    revert.setVisible(day.getState() != Day.STATE_RULE);
                    dismiss.setVisible(false);
                }
                snooze.setVisible(false);
                break;
            case GlobalManager.STATE_RINGING:
                disable.setVisible(false);
                revert.setVisible(false);
                dismiss.setVisible(true);
                snooze.setVisible(true);
                break;
            case GlobalManager.STATE_SNOOZED:
                disable.setVisible(false);
                revert.setVisible(false);
                dismiss.setVisible(true);
                snooze.setVisible(false);
                break;
            case GlobalManager.STATE_DISMISSED_BEFORE_RINGING:
            case GlobalManager.STATE_DISMISSED:
                disable.setVisible(false);
                revert.setVisible(false);
                dismiss.setVisible(false);
                snooze.setVisible(false);
                break;
            default:
                throw new IllegalArgumentException("Unexpected argument " + state);
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.day_set_time:
                Log.d(TAG, "Set time");
                showTimePicker();
                break;

            case R.id.day_disable:
                Log.d(TAG, "Disable");
                day.setState(Day.STATE_DISABLED);
                save(day);
                break;

            case R.id.day_revert:
                Log.i(TAG, "Revert");
                day.setState(Day.STATE_RULE);
                save(day);
                break;

            case R.id.day_dismiss:
                Log.i(TAG, "Dismiss");

                Analytics analytics = new Analytics(Analytics.Channel.Activity, Analytics.ChannelName.Calendar);

                GlobalManager globalManager = GlobalManager.getInstance();
                globalManager.onDismissAuto(analytics);
                break;

            case R.id.day_snooze:
                Log.i(TAG, "Snooze");

                Analytics analytics2 = new Analytics(Analytics.Channel.Activity, Analytics.ChannelName.Calendar);

                GlobalManager globalManager2 = GlobalManager.getInstance();
                globalManager2.onSnooze(analytics2);
                break;
        }
        return super.onContextItemSelected(item);
    }

}
