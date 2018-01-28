package cz.jaro.alarmmorning;

import android.app.Activity;
import android.app.DatePickerDialog;
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
import android.view.inputmethod.InputMethodManager;
import android.widget.DatePicker;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.GregorianCalendar;
import java.util.List;

import cz.jaro.alarmmorning.clock.Clock;
import cz.jaro.alarmmorning.graphics.RecyclerViewWithContextMenu;
import cz.jaro.alarmmorning.graphics.SimpleDividerItemDecoration;
import cz.jaro.alarmmorning.graphics.TimePickerDialogWithDisable;
import cz.jaro.alarmmorning.model.AppAlarm;
import cz.jaro.alarmmorning.model.Day;
import cz.jaro.alarmmorning.model.Defaults;
import cz.jaro.alarmmorning.model.OneTimeAlarm;

import static cz.jaro.alarmmorning.GlobalManager.HORIZON_DAYS;
import static cz.jaro.alarmmorning.calendar.CalendarUtils.addDaysClone;
import static cz.jaro.alarmmorning.calendar.CalendarUtils.beginningOfToday;
import static cz.jaro.alarmmorning.calendar.CalendarUtils.onTheSameDate;

/**
 * Fragment that appears in the "content_frame".
 */
public class CalendarFragment extends Fragment implements View.OnClickListener, TimePickerDialogWithDisable.OnTimeSetWithDisableListener, DatePickerDialog.OnDateSetListener {

    private static final String TAG = CalendarFragment.class.getSimpleName();

    private CalendarAdapter adapter;

    private RecyclerView recyclerView;

    private ArrayList<AppAlarm> items; // Kept in order: 1. date, 2. on a particular date, the Day is first and the one time alarms follow in natural order

    private Calendar today;

    private int menuAction; // type of the action
    private int positionAction; // position of the item wit which the action is performed (via context menu of via click)
    private static final int POSITION_UNSET = -1; // constant representing "positionAction of the next alarm" when no next alarm exists

    private List<Integer> positionNextAlarm; // positionAction of the next alarm

    private final HandlerOnClockChange handler = new HandlerOnClockChange();

    // TODO Improve architecture of actions (adding, removing, changing when snoozed) with one-time alarm
    private boolean oneTimeAlarmJustRemoved; // used for an ugly hack to know that a one-time alarm was removed (and showing a relevantn toast)
    private OneTimeAlarm oneTimeAlarmJustAdded; // used for an ugly hack to know that (and which) one-time alarm was added (and showing a relevantn toast)

    private CalendarAdapter.CalendarViewHolder editingNameViewHolder; // Reference to the currently edited name of a one-time alarm

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

        loadItems();

        return rootView;
    }

    public int getItemCount() {
        return items.size();
    }

    private void loadItems() {
        GlobalManager globalManager = GlobalManager.getInstance();
        Calendar now = clock().now();

        items = new ArrayList<>();

        // Add days
        for (int i = 0; i < HORIZON_DAYS; i++) {
            Calendar date = addDaysClone(now, i);
            Day day = globalManager.loadDay(date);
            items.add(day);
        }

        // Add one time alarms
        Calendar beginningOfToday = beginningOfToday(now);
        List<OneTimeAlarm> oneTimeAlarms = globalManager.loadOneTimeAlarms(beginningOfToday);
        items.addAll(oneTimeAlarms);

        // Sort
        Collections.sort(items, new Comparator<AppAlarm>() {
            public int compare(AppAlarm appAlarm, AppAlarm appAlarm2) {
                Calendar c1 = appAlarm.getDateTime();
                Calendar c2 = appAlarm2.getDateTime();
                // On a particular date, Day should be first
                if (onTheSameDate(c1, c2) && (appAlarm instanceof Day || appAlarm2 instanceof Day)) {
                    return appAlarm instanceof Day ? -1 : 1;
                }
                // Natural order
                return c1.before(c2)
                        ? -1
                        : c2.before(c1) ? 1 : 0;
            }
        });
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

        // In case the one-time alarm was removed, show "Alarm was removed" toast instead of "Alarm is off"
        if (oneTimeAlarmJustRemoved) {
            String toastText = getResources().getString(R.string.time_to_ring_toast_removed);
            Toast.makeText(getActivity(), toastText, Toast.LENGTH_LONG).show();

            oneTimeAlarmJustRemoved = false;

            return;
        }
        // In case the one-time alarm was added, show toast with proper time to ring
        if (oneTimeAlarmJustAdded != null) {
            String toastText = formatToastText(oneTimeAlarmJustAdded);
            Toast.makeText(getActivity(), toastText, Toast.LENGTH_LONG).show();

            oneTimeAlarmJustAdded = null;

            return;
        }

        adapter.notifyItemChanged(positionAction);
        updatePositionNextAlarm();

        if (positionAction != POSITION_UNSET) { // otherwise the alarm is set because a default alarm time was set. (The DefaultsActivity is running and I don't know why CalendarFragment gets the broadcast message...)
            AppAlarm appAlarmAtPosition = loadPosition(positionAction);
            String toastText = formatToastText(appAlarmAtPosition);
            Toast.makeText(getActivity(), toastText, Toast.LENGTH_LONG).show();
        }
    }

    public void onDismissBeforeRinging() {
        Log.d(TAG, "onDismissBeforeRinging()");
        for (int pos : positionNextAlarm) {
            adapter.notifyItemChanged(pos);
        }
    }

    public void onAlarmTimeOfEarlyDismissedAlarm() {
        Log.d(TAG, "onAlarmTimeOfEarlyDismissedAlarm()");
        for (int pos : positionNextAlarm) {
            adapter.notifyItemChanged(pos);
        }
    }

    public void onRing() {
        Log.d(TAG, "onRing()");
        for (int pos : positionNextAlarm) {
            adapter.notifyItemChanged(pos);
        }
    }

    public void onDismiss() {
        Log.d(TAG, "onDismiss()");
        updatePositionNextAlarm();
    }

    public void onSnooze() {
        Log.d(TAG, "onSnooze()");
        for (int pos : positionNextAlarm) {
            adapter.notifyItemChanged(pos);
        }
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
        LinearLayoutManager linearLayoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
        for (int pos : positionNextAlarm) {
            // Cannot use adapter.notifyItemChanged(pos) as this would break editing name of the one-time alarm that is also the next alarm to ring.
            // Therefore we need to update the TextView with time to alarm.

            AppAlarm appAlarmAtPosition = loadPosition(pos);

            int childAt = pos - linearLayoutManager.findFirstVisibleItemPosition();
            if (0 <= childAt && childAt < recyclerView.getChildCount()) {
                RelativeLayout itemView = (RelativeLayout) recyclerView.getChildAt(childAt);
                TextView textComment = (TextView) itemView.findViewById(R.id.textComment);

                String messageText = adapter.getTimeToAlarm(appAlarmAtPosition);
                textComment.setText(messageText);
            }
        }

        // Shift items when date changes
        Calendar today2 = getToday(clock());

        if (!today.equals(today2)) {
            int diffInDays = -1;
            for (int i = 1; i < items.size(); i++) {
                AppAlarm appAlarm = loadPosition(i);
                if (!(appAlarm instanceof Day))
                    continue;
                Day day = (Day) appAlarm;
                Calendar date = day.getDate();
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

    private List<Integer> calcPositionNextAlarm() {
        GlobalManager globalManager = GlobalManager.getInstance();
        AppAlarm nextAlarmToRing = globalManager.getNextAlarmToRing();

        return nextAlarmToRing == null ? new ArrayList<>() : alarmTimeToPosition(nextAlarmToRing);
    }

    private List<Integer> alarmTimeToPosition(AppAlarm appAlarm) {
        List<Integer> positions = new ArrayList<>();

        for (int pos = 0; pos < items.size(); pos++) {
            AppAlarm appAlarm2 = loadPosition(pos);
            if (appAlarm.getDateTime().equals(appAlarm2.getDateTime())) {
                positions.add(pos);
            }
        }

        return positions;
    }

    private void updatePositionNextAlarm() {
        List<Integer> newPositionNextAlarm = calcPositionNextAlarm();

        boolean same = positionNextAlarm.containsAll(newPositionNextAlarm) && newPositionNextAlarm.containsAll(positionNextAlarm);
        if (!same) {
            Log.d(TAG, "Next alarm is at positionAction " + newPositionNextAlarm);

            List<Integer> oldPositionNextAlarm = positionNextAlarm;
            positionNextAlarm = newPositionNextAlarm;

            for (int pos : oldPositionNextAlarm) {
                adapter.notifyItemChanged(pos);
            }
            for (int pos : newPositionNextAlarm) {
                adapter.notifyItemChanged(pos);
            }
        }
    }

    public boolean isPositionWithNextAlarm(int position) {
        return positionNextAlarm.contains(position);
    }

    private void save(AppAlarm appAlarm) {
        Analytics analytics = new Analytics(Analytics.Channel.Activity, Analytics.ChannelName.Calendar);
        GlobalManager globalManager = GlobalManager.getInstance();

        if (appAlarm instanceof Day) {
            Day day = (Day) appAlarm;
            globalManager.saveDay(day, analytics);
        } else if (appAlarm instanceof OneTimeAlarm) {
            OneTimeAlarm oneTimeAlarm = (OneTimeAlarm) appAlarm;
            globalManager.saveOneTimeAlarm(oneTimeAlarm, analytics);

            // Reorder
            int positionNew = resetPositionOnSave(oneTimeAlarm, positionAction);
            adapter.notifyItemChanged(positionAction);
            // FUTURE Blink the item
            adapter.notifyItemMoved(positionAction, positionNew);
        } else {
            throw new IllegalArgumentException("Unexpected class " + appAlarm.getClass());
        }

        adapter.notifyItemChanged(positionAction);
        updatePositionNextAlarm();
    }

    private void add(OneTimeAlarm oneTimeAlarm) {
        Analytics analytics = new Analytics(Analytics.Channel.Activity, Analytics.ChannelName.Calendar);
        GlobalManager globalManager = GlobalManager.getInstance();

        globalManager.saveOneTimeAlarm(oneTimeAlarm, analytics);

        // Update activity
        updatePositionNextAlarm();

        int pos = findPosition(oneTimeAlarm);
        items.add(pos, oneTimeAlarm);
        adapter.notifyItemInserted(pos);

        oneTimeAlarmJustAdded = oneTimeAlarm;
    }

    private void remove(OneTimeAlarm oneTimeAlarm) {
        Analytics analytics = new Analytics(Analytics.Channel.Activity, Analytics.ChannelName.Calendar);
        GlobalManager globalManager = GlobalManager.getInstance();

        globalManager.removeOneTimeAlarm(oneTimeAlarm, analytics);

        // Update activity

        items.remove(positionAction);

        adapter.notifyItemRangeRemoved(positionAction, 1);
        updatePositionNextAlarm();

        oneTimeAlarmJustRemoved = true;
    }

    private int findPosition(OneTimeAlarm oneTimeAlarm) {
        Calendar c = oneTimeAlarm.getDateTime();
        int pos = 0;

        Calendar beginningOfToday = beginningOfToday(clock().now());
        if (c.before(beginningOfToday))
            return pos;

        // Find the Day on the dame date
        while (true) {
            if (pos == items.size())
                break;

            Calendar c2 = items.get(pos).getDateTime();

            if (onTheSameDate(c, c2))
                break;

            pos++;
        }
        // Assert: pos is the index of the respective Day
        // Increase positionAction
        pos++;
        // Use natural order. If there are several alarms at the same same, put it at the end o such alarms.
        while (true) {
            if (pos == items.size())
                break;

            Calendar c2 = items.get(pos).getDateTime();

            if (!onTheSameDate(c, c2))
                break;

            if (!c2.before(c))
                break;

            pos++;
        }

        return pos;
    }

    private int resetPositionOnSave(OneTimeAlarm oneTimeAlarm, int positionOld) {
        items.remove(positionOld);
        int positionNew = findPosition(oneTimeAlarm);
        // TODO If (there are several alarms set at the same time and) the alarm time did not change, then leave the item at the original positionAction
        items.add(positionNew, oneTimeAlarm);
        return positionNew;
    }

    private String formatToastText(AppAlarm appAlarm) {
        return formatToastText(getResources(), clock(), appAlarm);
    }

    static public String formatToastText(Resources res, Clock clock, AppAlarm appAlarm) {
        String toastText;

        if (appAlarm instanceof Day && !((Day) appAlarm).isEnabled()) {
            toastText = res.getString(R.string.time_to_ring_toast_off);
        } else {
            long diff = appAlarm.getTimeToRing(clock);

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

    public AppAlarm loadPosition(int position) {
        return items.get(position);
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
        positionAction = recyclerView.getChildAdapterPosition(view);
        Log.d(TAG, "Clicked item on positionAction " + positionAction);

        menuAction = R.id.day_set_time;
        showTimePicker();
    }

    private void showTimePicker() {
        TimePickerFragment fragment = new TimePickerFragment();

        fragment.setOnTimeSetListener(this);

        // Preset time
        Calendar now = clock().now();

        GlobalManager globalManager = GlobalManager.getInstance();
        AppAlarm appAlarmAtPosition = loadPosition(positionAction);
        int state = globalManager.getState(appAlarmAtPosition.getDateTime());
        boolean presetNap = appAlarmAtPosition instanceof Day && menuAction == R.id.day_set_time && (
                positionAction == 0 && (((Day) appAlarmAtPosition).isEnabled()
                        ? (state == GlobalManager.STATE_SNOOZED || state == GlobalManager.STATE_DISMISSED || state == GlobalManager.STATE_DISMISSED_BEFORE_RINGING)
                        : now.after(appAlarmAtPosition.getDateTime()))
        );

        Bundle bundle = new Bundle();
        if (presetNap) {
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
            int napTime = preferences.getInt(SettingsActivity.PREF_NAP_TIME, SettingsActivity.PREF_NAP_TIME_DEFAULT);

            now.add(Calendar.MINUTE, napTime);

            bundle.putInt(TimePickerFragment.HOURS, now.get(Calendar.HOUR_OF_DAY));
            bundle.putInt(TimePickerFragment.MINUTES, now.get(Calendar.MINUTE));
        } else {
            if (menuAction == R.id.day_add_alarm) {
                // If today, then preset to the next hour (so the alarm rings in next hour).
                // If tomorrow or later, set to the current hour and zero minutes (so it coincides with the possible appointment that is currently in progress).
                // (There are other options too: 1. use now + nap time, or 2. use previously set time for one-time alarm (in case the user manually enters a
                // recurrent alarm))
                int hour = now.get(Calendar.HOUR_OF_DAY);
                if (onTheSameDate(appAlarmAtPosition.getDate(), today)) {
                    if (hour + 1 < 24) hour++;
                }
                bundle.putInt(TimePickerFragment.HOURS, hour);
                bundle.putInt(TimePickerFragment.MINUTES, 0);
            } else if (menuAction == R.id.day_set_time) {
                if (appAlarmAtPosition instanceof Day) {
                    Day day = (Day) appAlarmAtPosition;
                    bundle.putInt(TimePickerFragment.HOURS, day.getHourX());
                    bundle.putInt(TimePickerFragment.MINUTES, day.getMinuteX());
                } else {
                    OneTimeAlarm oneTimeAlarm = (OneTimeAlarm) appAlarmAtPosition;
                    bundle.putInt(TimePickerFragment.HOURS, oneTimeAlarm.getHour());
                    bundle.putInt(TimePickerFragment.MINUTES, oneTimeAlarm.getMinute());
                }
            } else {
                throw new IllegalArgumentException("Unexpected action " + menuAction);
            }
        }
        fragment.setArguments(bundle);

        fragment.show(getFragmentManager(), "timePicker");
    }

    @Override
    public void onTimeSetWithDisable(TimePicker view, boolean disable, int hourOfDay, int minute) {
        if (view.isShown()) {
            AppAlarm appAlarmAtPosition = loadPosition(positionAction);
            if (disable) {
                if (appAlarmAtPosition instanceof Day) {
                    Day day = (Day) appAlarmAtPosition;
                    day.setState(Day.STATE_DISABLED);
                    save(appAlarmAtPosition);
                    return;
                } else {
                    switch (menuAction) {
                        case R.id.day_set_time:
                            OneTimeAlarm oneTimeAlarm = (OneTimeAlarm) appAlarmAtPosition;
                            remove(oneTimeAlarm);
                            break;

                        case R.id.day_add_alarm:
                            // do nothing (setting time of a (to be) newly created alarm)
                    }
                    return;
                }
            } else {
                switch (menuAction) {
                    case R.id.day_set_time:
                        if (appAlarmAtPosition instanceof Day) {
                            Day day = (Day) appAlarmAtPosition;
                            day.setState(Day.STATE_ENABLED);
                        }
                        appAlarmAtPosition.setHour(hourOfDay);
                        appAlarmAtPosition.setMinute(minute);

                        save(appAlarmAtPosition);
                        return;

                    case R.id.day_add_alarm:
                        OneTimeAlarm oneTimeAlarm = new OneTimeAlarm();
                        oneTimeAlarm.setDate(appAlarmAtPosition.getDate());
                        oneTimeAlarm.setHour(hourOfDay);
                        oneTimeAlarm.setMinute(minute);

                        add(oneTimeAlarm);
                        return;
                }
            }
        }
    }

    /*
     * On set date (for one-time alarms)
     * =================================
     */

    public void onSetDate(View view) {
        positionAction = recyclerView.getChildAdapterPosition(view);
        Log.d(TAG, "Set date for item on positionAction " + positionAction);

        showDatePicker();
    }

    private void showDatePicker() {
        DatePickerFragment fragment = new DatePickerFragment();

        fragment.setOnDateSetListener(this);

        OneTimeAlarm oneTimeAlarmAtPosition = (OneTimeAlarm) loadPosition(positionAction);

        Bundle bundle = new Bundle();

        Calendar date = oneTimeAlarmAtPosition.getDate();
        bundle.putInt(DatePickerFragment.YEAR, date.get(Calendar.YEAR));
        bundle.putInt(DatePickerFragment.MONTH, date.get(Calendar.MONTH));
        bundle.putInt(DatePickerFragment.DAY, date.get(Calendar.DATE));

        fragment.setArguments(bundle);

        fragment.show(getFragmentManager(), "datePicker");
    }

    @Override
    public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
        if (view.isShown()) {
            OneTimeAlarm oneTimeAlarmAtPosition = (OneTimeAlarm) loadPosition(positionAction);

            Calendar date = new GregorianCalendar(year, month, dayOfMonth);
            oneTimeAlarmAtPosition.setDate(date);

            save(oneTimeAlarmAtPosition);
        }
    }

    /*
     * On set name (for one-time alarms)
     * =================================
     */

    public void onEditNameBegin(CalendarAdapter.CalendarViewHolder viewholder) {
        editingNameViewHolder = viewholder;
    }

    /**
     * @return Return true if this event was consumed.
     */
    public boolean onEditNameEnd() {
        if (editingNameViewHolder != null) {
            onNameSet(editingNameViewHolder);

            editingNameViewHolder = null;

            return true;
        } else {
            return false;
        }
    }

    void onNameSet(CalendarAdapter.CalendarViewHolder viewHolder) {
        positionAction = recyclerView.getChildAdapterPosition(viewHolder.itemView);
        Log.d(TAG, "Set name for item on positionAction " + positionAction);

        // Save

        OneTimeAlarm oneTimeAlarmAtPosition = (OneTimeAlarm) loadPosition(positionAction);
        oneTimeAlarmAtPosition.setName(viewHolder.getTextName().getText().toString());

        Analytics analytics = new Analytics(Analytics.Channel.Activity, Analytics.ChannelName.Calendar);

        GlobalManager globalManager = GlobalManager.getInstance();
        globalManager.saveOneTimeAlarmName(oneTimeAlarmAtPosition, analytics);

        // Change user interface

        CalendarFragment.hideSoftKeyboard(getActivity());
        viewHolder.getTextName().clearFocus();
    }

    public static void hideSoftKeyboard(Activity activity) {
        InputMethodManager inputMethodManager = (InputMethodManager) activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(activity.getCurrentFocus().getWindowToken(), 0);
    }

    /*
     * On context menu
     * ===============
     */

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        positionAction = ((RecyclerViewWithContextMenu.RecyclerViewContextMenuInfo) menuInfo).position;
        Log.d(TAG, "Long clicked item on positionAction " + positionAction);

        MenuInflater inflater = getActivity().getMenuInflater();
        inflater.inflate(R.menu.day_context_menu, menu);

        AppAlarm appAlarm = loadPosition(positionAction);
        Day day = null;
        OneTimeAlarm oneTimeAlarm = null;
        if (appAlarm instanceof Day) {
            day = (Day) appAlarm;
        } else if (appAlarm instanceof OneTimeAlarm) {
            oneTimeAlarm = (OneTimeAlarm) appAlarm;
        } else {
            throw new IllegalArgumentException("Unexpected class " + appAlarm.getClass());
        }

        // Set header

        Calendar date = appAlarm.getDateTime();
        int dayOfWeek = date.get(Calendar.DAY_OF_WEEK);
        String dayOfWeekText = Localization.dayOfWeekToStringShort(getResources(), dayOfWeek);

        String dateText = Localization.dateToStringVeryShort(getResources(), date.getTime());

        String headerTitle;
        if (appAlarm instanceof Day) {
            if (day.isEnabled()) {
                String timeText = Localization.timeToString(day.getHourX(), day.getMinuteX(), getActivity());
                headerTitle = getResources().getString(R.string.menu_day_header, timeText, dayOfWeekText, dateText);
            } else {
                headerTitle = getResources().getString(R.string.menu_day_header_disabled, dayOfWeekText, dateText);
            }
        } else {
            String timeText = Localization.timeToString(oneTimeAlarm.getHour(), oneTimeAlarm.getMinute(), getActivity());
            headerTitle = getResources().getString(R.string.menu_day_header, timeText, dayOfWeekText, dateText);
        }
        menu.setHeaderTitle(headerTitle);

        // Set title of revert item
        if (appAlarm instanceof Day) {
            MenuItem revertItem = menu.findItem(R.id.day_revert);
            Defaults defaults = day.getDefaults();
            String timeText;
            if (defaults.isEnabled() && !day.isHoliday()) {
                timeText = Localization.timeToString(defaults.getHour(), defaults.getMinute(), getActivity());
            } else {
                timeText = getResources().getString(R.string.alarm_unset);
            }
            revertItem.setTitle(getString(R.string.action_revert, timeText));
        }

        // Hide irrelevant items

        MenuItem setTime = menu.findItem(R.id.day_set_time);
        MenuItem disable = menu.findItem(R.id.day_disable);
        MenuItem revert = menu.findItem(R.id.day_revert);
        MenuItem dismiss = menu.findItem(R.id.day_dismiss);
        MenuItem snooze = menu.findItem(R.id.day_snooze);
        MenuItem addAlarm = menu.findItem(R.id.day_add_alarm);

        GlobalManager globalManager = GlobalManager.getInstance();
        int state = globalManager.getState(appAlarm.getDateTime());

        switch (state) {
            case GlobalManager.STATE_FUTURE:
                if (appAlarm instanceof Day) {
                    if (positionNextAlarm.contains(positionAction) && day.isEnabled() && globalManager.afterNearFuture(appAlarm.getDateTime())) {
                        disable.setVisible(false);
                        revert.setVisible(false);
                        dismiss.setVisible(true);
                    } else {
                        disable.setVisible(true);
                        revert.setVisible(day.getState() != Day.STATE_RULE);
                        dismiss.setVisible(false);
                    }
                } else {
                    if (positionNextAlarm.contains(positionAction) && globalManager.afterNearFuture(appAlarm.getDateTime())) {
                        dismiss.setVisible(true);
                    } else {
                        dismiss.setVisible(false);
                    }
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

        // Fix visibility for one time alarm
        if (appAlarm instanceof OneTimeAlarm) {
            disable.setVisible(false);
            revert.setVisible(false);
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        Day day = null;
        OneTimeAlarm oneTimeAlarm = null;
        AppAlarm appAlarmAtPosition = loadPosition(positionAction);
        if (appAlarmAtPosition instanceof Day) {
            day = (Day) appAlarmAtPosition;
        } else if (appAlarmAtPosition instanceof OneTimeAlarm) {
            oneTimeAlarm = (OneTimeAlarm) appAlarmAtPosition;
        } else {
            throw new IllegalArgumentException("Unexpected class " + appAlarmAtPosition.getClass());
        }

        switch (item.getItemId()) {
            case R.id.day_set_time:
            case R.id.day_add_alarm:
                Log.d(TAG, "Set time");
                menuAction = item.getItemId();
                showTimePicker();
                break;

            case R.id.day_disable:
                Log.d(TAG, "Disable");
                if (appAlarmAtPosition instanceof Day) {
                    day.setState(Day.STATE_DISABLED);
                    save(day);
                } else {
                    remove(oneTimeAlarm);
                }
                break;

            case R.id.day_revert:
                Log.i(TAG, "Revert");
                if (appAlarmAtPosition instanceof Day) {
                    day.setState(Day.STATE_RULE);
                    save(day);
                }
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
