package cz.jaro.alarmmorning;

/**
 * Created by ext93831 on 26.1.2016.
 */

import android.app.Fragment;
import android.app.TimePickerDialog;
import android.content.Context;
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
import cz.jaro.alarmmorning.clock.SystemClock;
import cz.jaro.alarmmorning.graphics.RecyclerViewWithContextMenu;
import cz.jaro.alarmmorning.graphics.SimpleDividerItemDecoration;
import cz.jaro.alarmmorning.model.AlarmDataSource;
import cz.jaro.alarmmorning.model.Day;

/**
 * Fragment that appears in the "content_frame"
 */
public class CalendarFragment extends Fragment implements View.OnClickListener, TimePickerDialog.OnTimeSetListener {

    private static final String TAG = CalendarFragment.class.getSimpleName();

    private AlarmDataSource dataSource;

    protected CalendarAdapter adapter;

    private RecyclerView recyclerView;
    private RecyclerView.LayoutManager layoutManager;

    private Calendar today;
    private Day day;
    private int position;

    private int positionNextAlarm;
    private static final int POSITION_UNSET = -1;

    private HandlerOnClockChange handler = new HandlerOnClockChange();
    private Runnable runnable = new Runnable() {
        @Override
        public void run() {
            onSystemTimeChange();
        }
    };

    public CalendarFragment() {
        // Empty constructor required for fragment subclasses
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_calendar, container, false);

        recyclerView = (RecyclerView) rootView.findViewById(R.id.calendar_recycler_view);

        // use a linear layout manager
        layoutManager = new LinearLayoutManager(getActivity());
        recyclerView.setLayoutManager(layoutManager);

        // specify an adapter
        ActivityInterface activityInterface = (ActivityInterface) getActivity();
        adapter = new CalendarAdapter(this);
        recyclerView.setAdapter(adapter);

        // item separator
        recyclerView.addItemDecoration(new SimpleDividerItemDecoration(getActivity()));

        // for disabling the animation on update
        DefaultItemAnimator animator = new DefaultItemAnimator();
        animator.setSupportsChangeAnimations(false);
        recyclerView.setItemAnimator(animator);

        registerForContextMenu(recyclerView);

        dataSource = new AlarmDataSource(activityInterface.getContextI());
        dataSource.open();

        today = getToday(clock());
        updatePositionNextAlarm();

        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();

        // handler for refreshing the content
        handler.start(runnable, Calendar.SECOND);

        Calendar today2 = getToday(clock());

        if (!today.equals(today2)) {
            today = today2;
            adapter.notifyDataSetChanged();
        } else {
            adapter.notifyItemChanged(positionNextAlarm);
        }

    }

    @Override
    public void onPause() {
        super.onPause();

        handler.stop();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        dataSource.close();
    }

    /*
     * Events
     * ======
     */

    public void onAlarmSet() {
        Log.d(TAG, "onAlarmSet()");
        adapter.notifyItemChanged(0);

        adapter.notifyItemChanged(position);
        updatePositionNextAlarm();

        String toastText = formatToastText(day);
        Toast.makeText(getActivity(), toastText, Toast.LENGTH_LONG).show();
    }

    public void onDismissBeforeRinging() {
        Log.d(TAG, "onDismissBeforeRinging()");
        adapter.notifyItemChanged(0);
        updatePositionNextAlarm();
    }

    public void onAlarmTimeOfEarlyDismissedAlarm() {
        Log.d(TAG, "onAlarmTimeOfEarlyDismissedAlarm()");
        adapter.notifyItemChanged(0);
    }

    public void onRing() {
        Log.d(TAG, "onRing()");
        adapter.notifyItemChanged(0);
    }

    public void onDismiss() {
        Log.d(TAG, "onDismiss()");
        adapter.notifyItemChanged(0);
        updatePositionNextAlarm();
    }

    public void onSnooze() {
        Log.d(TAG, "onSnooze()");
        adapter.notifyItemChanged(0);
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

    public void onSystemTimeChange() {
        Log.v(TAG, "onSystemTimeChange()");

        // Update time to next alarm
        if (positionNextAlarm != POSITION_UNSET)
            adapter.notifyItemChanged(positionNextAlarm);

        // Shift items when date changes
        Calendar today2 = getToday(clock());

        if (!today.equals(today2)) {
            int diffInDays = -1;
            for (int i = 1; i < AlarmDataSource.HORIZON_DAYS; i++) {
                Calendar date = addDays(today, i);
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

    // TODO Solve dependency on clock
    private Clock clock() {
        return new SystemClock();
    }

    private int calcPositionNextAlarm() {
        GlobalManager globalManager = new GlobalManager(getActivity());
        Day day = globalManager.getDayWithNextAlarmToRing();

        if (day == null) {
            return POSITION_UNSET;
        } else {
            int position = dayToPosition(day);
            return position;
        }
    }

    private int dayToPosition(Day day) {
        Calendar date = clock().now();

        for (int daysInAdvance = 0; daysInAdvance < AlarmDataSource.HORIZON_DAYS; daysInAdvance++, date.add(Calendar.DATE, 1)) {
            if (RingActivity.onTheSameDate(day.getDate(), date)) {
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
            if (positionNextAlarm != POSITION_UNSET)
                adapter.notifyItemChanged(positionNextAlarm);
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
        GlobalManager globalManager = new GlobalManager(getActivity());
        globalManager.saveAlarmTime(day, dataSource);

        adapter.notifyItemChanged(position);
        updatePositionNextAlarm();
    }

    private String formatToastText(Day day) {
        Resources res = getResources();
        String toastText;

        if (!day.isEnabled()) {
            toastText = res.getString(R.string.time_to_ring_toast_off);
        } else {
            long diff = day.getTimeToRing(clock());

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
        Calendar date = addDays(today, position);
        Day day = dataSource.loadDayDeep(date);
        return day;
    }

    public static Calendar addDays(Calendar today, int numberOfDays) {
        Calendar date = (Calendar) today.clone();
        date.add(Calendar.DAY_OF_MONTH, numberOfDays);
        return date;
    }

    public static Calendar getToday(Clock clock) {
        Calendar today = clock.now();
        today.set(Calendar.HOUR_OF_DAY, 0);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);
        today.set(Calendar.MILLISECOND, 0);

        return today;
    }

    /*
     * On click
     * ========
     */

    @Override
    public void onClick(View view) {
        int position = recyclerView.getChildPosition(view);
        Log.d(TAG, "Clicked item on position " + position);

        Day day = loadPosition(position);
        setCurrent(day, position);

        showTimePicker();
    }

    private void showTimePicker() {
        TimePickerFragment fragment = new TimePickerFragment();

        fragment.setOnTimeSetListener(this);

        // Preset time
        Clock clock = new SystemClock(); // TODO Solve dependency on clock
        Calendar now = clock.now();

        GlobalManager globalManager = new GlobalManager(getActivity());
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
    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
        day.setState(Day.STATE_ENABLED);
        day.setHour(hourOfDay);
        day.setMinute(minute);

        save(day);
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
            String timeText = Localization.timeToString(day.getHourX(), day.getMinuteX(), getActivity(), clock());
            headerTitle = getResources().getString(R.string.menu_day_header, timeText, dayOfWeekText, dateText);
        } else {
            headerTitle = getResources().getString(R.string.menu_day_header_disabled, dayOfWeekText, dateText);
        }
        menu.setHeaderTitle(headerTitle);

        // Hide irrelevant items

        MenuItem setTime = menu.findItem(R.id.day_set_time);
        MenuItem disable = menu.findItem(R.id.day_disable);
        MenuItem revert = menu.findItem(R.id.day_revert);
        MenuItem dismiss = menu.findItem(R.id.day_dismiss);
        MenuItem snooze = menu.findItem(R.id.day_snooze);

//        if (position == 0) {
            GlobalManager globalManager = new GlobalManager(getActivity());
            int state = globalManager.getState(day.getDateTime());
//            if (state != GlobalManager.STATE_UNDEFINED) {
                if (state == GlobalManager.STATE_FUTURE) {
                    disable.setVisible(day.isEnabled());
                    revert.setVisible(day.getState() != Day.STATE_DEFAULT);
                    dismiss.setVisible(position == 0 && day.isEnabled() && globalManager.afterNearFuture(day.getDateTime()));
                    snooze.setVisible(false);
                } else if (state == GlobalManager.STATE_RINGING) {
                    disable.setVisible(false);
                    revert.setVisible(false);
                    dismiss.setVisible(true);
                    snooze.setVisible(true);
                } else if (state == GlobalManager.STATE_SNOOZED) {
                    disable.setVisible(false);
                    revert.setVisible(false);
                    dismiss.setVisible(true);
                    snooze.setVisible(false);
                } else if (state == GlobalManager.STATE_DISMISSED_BEFORE_RINGING || state == GlobalManager.STATE_DISMISSED) {
                    disable.setVisible(false);
                    revert.setVisible(false);
                    dismiss.setVisible(false);
                    snooze.setVisible(false);
                } else {
                    throw new IllegalArgumentException("Unexpected argument " + state);
                }
//            } else {
//                disable.setVisible(day.isEnabled());
//                revert.setVisible(day.getState() != Day.STATE_DEFAULT);
//                dismiss.setVisible(false);
//                snooze.setVisible(false);
//            }
//        } else {
//            disable.setVisible(day.isEnabled());
//            revert.setVisible(day.getState() != Day.STATE_DEFAULT);
//            dismiss.setVisible(false);
//            snooze.setVisible(false);
//        }
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
                day.setState(Day.STATE_DEFAULT);
                save(day);
                break;

            case R.id.day_dismiss:
                Log.i(TAG, "Dismiss");
                Context context = getActivity();
                GlobalManager globalManager = new GlobalManager(context);
                globalManager.onDismissBeforeRinging();
                break;

            case R.id.day_snooze:
                Log.i(TAG, "Snooze");
                Context context2 = getActivity();
                GlobalManager globalManager2 = new GlobalManager(context2);
                globalManager2.onSnooze();
                break;
        }
        return super.onContextItemSelected(item);
    }

}
