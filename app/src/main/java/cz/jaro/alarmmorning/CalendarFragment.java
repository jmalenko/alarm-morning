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
    private int positionAction; // position of the item with which the action is performed (via context menu or via click)
    private static final int POSITION_UNSET = -1; // constant representing "positionAction of the next alarm" when no next alarm exists

    private List<Integer> positionNextAlarm; // position of the next alarm

    private final HandlerOnClockChange handler = new HandlerOnClockChange();

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

        Collections.sort(items);
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
     * Events relevant to next alarm
     * =============================
     */

    public void onAlarmSet(AppAlarm appAlarm) {
        Log.d(TAG, "onAlarmSet(appAparm = " + appAlarm + ")");

        updatePositionOfNextAlarm();
    }

    public void onDismissBeforeRinging(AppAlarm appAlarm) {
        Log.d(TAG, "onDismissBeforeRinging(appAparm = " + appAlarm + ")");

        int pos = getPosition(appAlarm); // TODO Maybe this needs to happen in every similar method
        adapter.notifyItemChanged(pos);

        updatePositionOfNextAlarm();
    }

    public void onAlarmTimeOfEarlyDismissedAlarm(AppAlarm appAlarm) {
        Log.d(TAG, "onAlarmTimeOfEarlyDismissedAlarm(appAparm = " + appAlarm + ")");
        invalidateItemsWithNextAlarm();
    }

    public void onRing(AppAlarm appAlarm) {
        Log.d(TAG, "onRing(appAparm = " + appAlarm + ")");
        invalidateItemsWithNextAlarm();
    }

    public void onDismiss(AppAlarm appAlarm) {
        Log.d(TAG, "onDismiss(appAparm = " + appAlarm + ")");
        updatePositionOfNextAlarm();
    }

    public void onSnooze(AppAlarm appAlarm) {
        Log.d(TAG, "onSnooze(appAparm = " + appAlarm + ")");
        invalidateItemsWithNextAlarm();
    }

    public void onCancel(AppAlarm appAlarm) {
        Log.d(TAG, "onCancel(appAparm = " + appAlarm + ")");
        updatePositionOfNextAlarm();
    }

    /*
     * Events relevant to alarm management
     * ===================================
     */

    public void onModifyDayAlarm(Day day) {
        Log.d(TAG, "modifyDayAlarm(day = " + day + ")");

        int pos = getPosition(day);
        adapter.notifyItemChanged(pos);

        // Update position of next alarms
        positionNextAlarm = calcPositionNextAlarm();

        // Show toast
        String toastText = formatToastText(day);
        Toast.makeText(getActivity(), toastText, Toast.LENGTH_LONG).show();
    }

    public void onCreateOneTimeAlarm(OneTimeAlarm oneTimeAlarm) {
        Log.d(TAG, "onCreateOneTimeAlarm(oneTimeAlarm = " + oneTimeAlarm + ")");

        loadItems(); // TODO Don't load everything again, use findPosition()

        int pos = getPosition(oneTimeAlarm);
        adapter.notifyItemInserted(pos);

        // Update position of next alarms
        List<Integer> positionNextAlarmNew = new ArrayList<>();
        for (int i = 0; i < positionNextAlarm.size(); i++) {
            positionNextAlarmNew.add(positionNextAlarm.get(i) + 1);
            Integer p = positionNextAlarm.get(i);
            if (p < pos)
                positionNextAlarmNew.add(i, p);
            else
                positionNextAlarmNew.add(i, p + 1);
        }
        positionNextAlarm = positionNextAlarmNew;

        // Update position of next action
        if (positionAction < pos) { /* nothing */ } else
            positionAction++;

        // Show toast
        String toastText = formatToastText(oneTimeAlarm);
        Toast.makeText(getActivity(), toastText, Toast.LENGTH_LONG).show();

        // FUTURE Blink the item
    }

    public void onDeleteOneTimeAlarm(long oneTimeAlarmId) {
        Log.d(TAG, "onDeleteOneTimeAlarm(oneTimeAlarmId = " + oneTimeAlarmId + ")");

        // Get the OneTimeAlarm object
        OneTimeAlarm oneTimeAlarm = null;
        for (AppAlarm appAlarm : items) {
            if (appAlarm instanceof OneTimeAlarm
                    && ((OneTimeAlarm) appAlarm).getId() == oneTimeAlarmId) {
                oneTimeAlarm = (OneTimeAlarm) appAlarm;
                break;
            }
        }

        if (oneTimeAlarm == null) {
            Log.w(TAG, "Cannot find the alarm to delete" + oneTimeAlarmId);
            throw new IllegalArgumentException("Cannot find the alarm to delete " + oneTimeAlarmId);
        }

        // Update the fragment
        int pos = getPosition(oneTimeAlarm);
        if (pos < 0) {
            Log.e(TAG, "Cannot get alarm position");
            throw new IllegalArgumentException("Cannot get alarm position");
        }

        items.remove(pos);
        recyclerView.removeViewAt(pos); // TODO This line should not be necessary, but the (one-time alarm) tests fail without this line. However some comments stress that his line implies an annoying glitch in the real usage by user. Look at https://stackoverflow.com/questions/31367599/how-to-update-recyclerview-adapter-data . Maybe remove this line and add the 4th line: adapter.notifyItemRangeChanged(pos, items.size());
        adapter.notifyItemRemoved(pos);

        // Updating positionNextAlarm and positionAction is not necessary now as immediately onAlarmSet() is called (which does this)
    }

    public void onModifyOneTimeAlarmDateTime(OneTimeAlarm oneTimeAlarm) {
        Log.d(TAG, "onModifyOneTimeAlarmDateTime(oneTimeAlarm = " + oneTimeAlarm + ")");

        changePosition(oneTimeAlarm);
    }

    private void changePosition(OneTimeAlarm oneTimeAlarm) {
        int pos1 = getPosition(oneTimeAlarm);
        loadItems();
        int pos2 = getPosition(oneTimeAlarm);

        // If the item does not exist (e.g. because its date was changed to past)
        if (pos2 == -1) {
            adapter.notifyItemRemoved(pos1);
            adapter.notifyDataSetChanged();

            List<Integer> positionNextAlarmNew = new ArrayList<>();
            for (int i = 0; i < positionNextAlarm.size(); i++) {
                int pos = positionNextAlarm.get(i);
                int posNew;

                if (pos < pos1)
                    posNew = pos;
                else
                    posNew = pos - 1;

                positionNextAlarmNew.add(i, posNew);
            }
            positionNextAlarm = positionNextAlarmNew;

            if (pos1 <= positionAction)
                positionAction--;

            return;
        }

        adapter.notifyItemChanged(pos1);
        adapter.notifyItemMoved(pos1, pos2);

        List<Integer> positionNextAlarmNew = new ArrayList<>();
        for (int i = 0; i < positionNextAlarm.size(); i++) {
            int pos = positionNextAlarm.get(i);
            int posNew;

            if (pos1 == pos)
                posNew = pos2;
            else if (pos2 == pos)
                posNew = pos1;
            else if (pos1 < pos && pos < pos2) {
                if (pos1 < pos2)
                    posNew = pos - 1;
                else
                    posNew = pos + 1;
            } else
                posNew = pos;

            positionNextAlarmNew.add(i, posNew);
        }
        positionNextAlarm = positionNextAlarmNew;

        if (pos1 == positionAction)
            positionAction = pos2;
        else if (pos2 == positionAction)
            positionAction = pos1;
        else if (pos1 < positionAction && positionAction < pos2) {
            if (pos1 < pos2)
                positionAction--;
            else
                positionAction++;
        }
    }

    public void onModifyOneTimeAlarmName(OneTimeAlarm oneTimeAlarm) {
        Log.d(TAG, "onModifyOneTimeAlarmName(oneTimeAlarm = " + oneTimeAlarm + ")");

        // Note: Deviation from the "act when notified from GlobalManager" pattern.
        // 1. The name can be only changed in the CalendarFragment.
        // 2. The name in the CalendarFragment will change when user enters a new name. (This is the deviation.)
        // 3. The name in the CalendarFragment will change again when the CalendarFragment is notified from GlobalManager.

        int pos = getPosition(oneTimeAlarm);

        if (pos == -1) {
            Log.w(TAG, "Alarm name changed of an non-existent alarm");
            return;
        }

        // To prevent updating of the whole item, we update only the name
        RelativeLayout itemView = (RelativeLayout) recyclerView.getChildAt(pos);
        TextView textName = (TextView) itemView.findViewById(R.id.textName);
        textName.setText(oneTimeAlarm.getName());
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

                String messageText = adapter.getDurationToAlarm(appAlarmAtPosition);
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
     * Other
     * =====
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

    private void updatePositionOfNextAlarm() {
        List<Integer> newPositionNextAlarm = calcPositionNextAlarm();

        boolean same = positionNextAlarm.containsAll(newPositionNextAlarm) && newPositionNextAlarm.containsAll(positionNextAlarm);
        if (!same) {
            Log.d(TAG, "Next alarm is at positionAction " + newPositionNextAlarm);

            List<Integer> oldPositionNextAlarm = positionNextAlarm;
            positionNextAlarm = newPositionNextAlarm;

            // TODO Optimization: if an item is both in oldPositionNextAlarm and newPositionNextAlarm then call this item notifyItemChanged() only once
            for (int pos : oldPositionNextAlarm) {
                adapter.notifyItemChanged(pos);
            }
            for (int pos : newPositionNextAlarm) {
                adapter.notifyItemChanged(pos);
            }
        }
    }

    private void invalidateItemsWithNextAlarm() {
        for (int pos : positionNextAlarm) {
            adapter.notifyItemChanged(pos);
        }
    }

    public boolean isPositionWithNextAlarm(int position) {
        return positionNextAlarm.contains(position);
    }

    private int getPosition(AppAlarm appAlarm) {
        for (int i = 0; i < items.size(); i++) {
            AppAlarm appAlarm2 = items.get(i);
            if (appAlarm2 instanceof Day
                    && appAlarm instanceof Day
                    && onTheSameDate(((Day) appAlarm2).getDateTime(), ((Day) appAlarm).getDateTime()))
                return i;
            if (appAlarm2 instanceof OneTimeAlarm
                    && appAlarm instanceof OneTimeAlarm
                    && ((OneTimeAlarm) appAlarm2).getId() == ((OneTimeAlarm) appAlarm).getId()) {
                return i;
            }
        }
        return -1;
    }

    private void modifyDayAlarm(Day day) {
        Analytics analytics = new Analytics(Analytics.Channel.Activity, Analytics.ChannelName.Calendar);
        GlobalManager globalManager = GlobalManager.getInstance();

        globalManager.modifyDayAlarm(day, analytics);
    }

    private void createOneTimeAlarm(OneTimeAlarm oneTimeAlarm) {
        Analytics analytics = new Analytics(Analytics.Channel.Activity, Analytics.ChannelName.Calendar);
        GlobalManager globalManager = GlobalManager.getInstance();

        globalManager.createOneTimeAlarm(oneTimeAlarm, analytics);
    }

    private void deleteOneTimeAlarm(OneTimeAlarm oneTimeAlarm) {
        Analytics analytics = new Analytics(Analytics.Channel.Activity, Analytics.ChannelName.Calendar);
        GlobalManager globalManager = GlobalManager.getInstance();

        globalManager.deleteOneTimeAlarm(oneTimeAlarm, analytics);
    }

    private void modifyOneTimeAlarmDateTime(OneTimeAlarm oneTimeAlarm) {
        Analytics analytics = new Analytics(Analytics.Channel.Activity, Analytics.ChannelName.Calendar);
        GlobalManager globalManager = GlobalManager.getInstance();

        globalManager.modifyOneTimeAlarmDateTime(oneTimeAlarm, analytics);
    }

    private void modifyOneTimeAlarmName(OneTimeAlarm oneTimeAlarm) {
        Analytics analytics = new Analytics(Analytics.Channel.Activity, Analytics.ChannelName.Calendar);
        GlobalManager globalManager = GlobalManager.getInstance();

        globalManager.modifyOneTimeAlarmName(oneTimeAlarm, analytics);
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
                TimeDifference timeDifference = new TimeDifference(diff);
                if (timeDifference.days > 0) {
                    toastText = res.getString(R.string.time_to_ring_toast_days, timeDifference.days, timeDifference.hours, timeDifference.minutes);
                } else if (timeDifference.hours > 0) {
                    toastText = res.getString(R.string.time_to_ring_toast_hours, timeDifference.hours, timeDifference.minutes);
                } else if (timeDifference.minutes > 0) {
                    toastText = res.getString(R.string.time_to_ring_toast_minutes, timeDifference.minutes, timeDifference.seconds);
                } else {
                    toastText = res.getString(R.string.time_to_ring_toast_seconds, timeDifference.seconds);
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
        loadItems();
        adapter.notifyDataSetChanged();
    }

    public void onAddOneTimeAlarm() {
        positionAction = 0;
        menuAction = R.id.action_day_add_alarm;
        showTimePicker();
    }

    /*
     * On click
     * ========
     */

    @Override
    public void onClick(View view) {
        positionAction = recyclerView.getChildAdapterPosition(view);
        Log.d(TAG, "Clicked item on positionAction " + positionAction);

        menuAction = R.id.action_day_set_time;
        showTimePicker();
    }

    private void showTimePicker() {
        TimePickerFragment fragment = new TimePickerFragment();

        fragment.setOnTimeSetListener(this);

        // Preset time
        Calendar now = clock().now();

        GlobalManager globalManager = GlobalManager.getInstance();
        AppAlarm appAlarmAtPosition = loadPosition(positionAction);
        int state = globalManager.getState(appAlarmAtPosition);
        boolean presetNap = appAlarmAtPosition instanceof Day && menuAction == R.id.action_day_set_time && (
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
            if (menuAction == R.id.action_day_add_alarm) {
                // If today, then preset to the next hour (so the alarm rings in next hour).
                // If tomorrow or later, set to the current hour and zero minutes (so it coincides with the possible appointment that is currently in progress).
                // (There are other options too: 1. use now + nap time, or 2. use previously set time for one-time alarm (in case the user manually enters a
                // recurrent alarm))
                int hour = now.get(Calendar.HOUR_OF_DAY);
                if (onTheSameDate(appAlarmAtPosition.getDate(), today)) {
                    hour = (hour + 1) % 24;
                }
                bundle.putInt(TimePickerFragment.HOURS, hour);
                bundle.putInt(TimePickerFragment.MINUTES, 0);
            } else if (menuAction == R.id.action_day_set_time) {
                bundle.putInt(TimePickerFragment.HOURS, appAlarmAtPosition.getHour());
                bundle.putInt(TimePickerFragment.MINUTES, appAlarmAtPosition.getMinute());
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
                    modifyDayAlarm(day);
                    return;
                } else {
                    switch (menuAction) {
                        case R.id.action_day_set_time:
                            OneTimeAlarm oneTimeAlarm = (OneTimeAlarm) appAlarmAtPosition;
                            deleteOneTimeAlarm(oneTimeAlarm);
                            break;

                        case R.id.action_day_add_alarm:
                            // do nothing (setting time of a (to be) newly created alarm)
                    }
                    return;
                }
            } else {
                switch (menuAction) {
                    case R.id.action_day_set_time:
                        if (appAlarmAtPosition instanceof Day) {
                            Day day = (Day) appAlarmAtPosition;
                            day.setState(Day.STATE_ENABLED);
                        }

                        appAlarmAtPosition.setHour(hourOfDay);
                        appAlarmAtPosition.setMinute(minute);

                        if (appAlarmAtPosition instanceof Day) {
                            Day day = (Day) appAlarmAtPosition;
                            modifyDayAlarm(day);
                        } else {
                            OneTimeAlarm oneTimeAlarm = (OneTimeAlarm) appAlarmAtPosition;
                            modifyOneTimeAlarmDateTime(oneTimeAlarm);
                        }
                        return;

                    case R.id.action_day_add_alarm:
                        OneTimeAlarm oneTimeAlarm = new OneTimeAlarm();
                        oneTimeAlarm.setDate(appAlarmAtPosition.getDate());
                        oneTimeAlarm.setHour(hourOfDay);
                        oneTimeAlarm.setMinute(minute);

                        // If in the past then shift to tomorrow
                        // Note: we do this even if the "set one time alarm" action started from the context menu on today AND the time is in the past
                        Calendar now = clock().now();
                        if (oneTimeAlarm.getDateTime().before(now)) {
                            Calendar date = oneTimeAlarm.getDate();
                            date.add(Calendar.DATE, 1);
                            oneTimeAlarm.setDate(date);
                        }

                        createOneTimeAlarm(oneTimeAlarm);
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

            modifyOneTimeAlarmDateTime(oneTimeAlarmAtPosition);
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

        modifyOneTimeAlarmName(oneTimeAlarmAtPosition);

        // Change user interface

        CalendarFragment.hideSoftKeyboard(getActivity());
        viewHolder.getTextName().clearFocus();
    }

    public static void hideSoftKeyboard(Activity activity) {
        InputMethodManager inputMethodManager = (InputMethodManager) activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(activity.getWindow().getDecorView().getWindowToken(), 0);
    }

    /*
     * On context menu
     * ===============
     */

    /**
     * Note: in the context menu, regarding the one time alarms:
     *
     * <ul>Exactly one of the Dismiss and Disable options is available for the future one-time alarms. The Disable is available if and only if the alarm is in
     * the near period, otherwise the Disable is available.</ul>
     *
     * <ul>The Disable option will actually delete the one-time alarm.</ul>
     *
     * @param menu     Menu
     * @param v        View
     * @param menuInfo MenuInfo
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
        if (!(appAlarm instanceof Day) || day.isEnabled()) {
            String timeText = Localization.timeToString(appAlarm.getHour(), appAlarm.getMinute(), getActivity());
            headerTitle = getResources().getString(R.string.menu_day_header, timeText, dayOfWeekText, dateText);
        } else {
            headerTitle = getResources().getString(R.string.menu_day_header_disabled, dayOfWeekText, dateText);
        }
        menu.setHeaderTitle(headerTitle);

        // Set title of revert item
        if (appAlarm instanceof Day) {
            MenuItem revertItem = menu.findItem(R.id.action_day_revert);
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

        MenuItem setTime = menu.findItem(R.id.action_day_set_time);
        MenuItem disable = menu.findItem(R.id.action_day_disable);
        MenuItem revert = menu.findItem(R.id.action_day_revert);
        MenuItem dismiss = menu.findItem(R.id.action_day_dismiss);
        MenuItem snooze = menu.findItem(R.id.action_day_snooze);
        MenuItem addAlarm = menu.findItem(R.id.action_day_add_alarm);

        GlobalManager globalManager = GlobalManager.getInstance();
        int state = globalManager.getState(appAlarm);

        switch (state) {
            case GlobalManager.STATE_FUTURE:
                boolean nextAndAfterNear = positionNextAlarm.contains(positionAction) && globalManager.afterBeginningOfNearFuturePeriod(appAlarm.getDateTime());
                if (nextAndAfterNear) {
                    disable.setVisible(false);
                    revert.setVisible(false);
                    dismiss.setVisible(true);
                } else {
                    disable.setVisible(!(appAlarm instanceof Day) || day.isEnabled());
                    revert.setVisible(appAlarm instanceof Day && day.getState() != Day.STATE_RULE);
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
            case R.id.action_day_set_time:
            case R.id.action_day_add_alarm:
                Log.d(TAG, "Set time");
                menuAction = item.getItemId();
                showTimePicker();
                break;

            case R.id.action_day_disable:
                Log.d(TAG, "Disable");
                if (appAlarmAtPosition instanceof Day) {
                    day.setState(Day.STATE_DISABLED);
                    modifyDayAlarm(day);
                } else {
                    deleteOneTimeAlarm(oneTimeAlarm);
                }
                break;

            case R.id.action_day_revert:
                Log.i(TAG, "Revert");
                if (appAlarmAtPosition instanceof Day) {
                    day.setState(Day.STATE_RULE);
                    modifyDayAlarm(day);
                }
                break;

            case R.id.action_day_dismiss:
                Log.i(TAG, "Dismiss");

                Analytics analytics1 = new Analytics(Analytics.Channel.Activity, Analytics.ChannelName.Calendar);

                GlobalManager globalManager1 = GlobalManager.getInstance();
                globalManager1.onDismissAny(appAlarmAtPosition, analytics1);

                break;

            case R.id.action_day_snooze:
                Log.i(TAG, "Snooze");

                Analytics analytics2 = new Analytics(Analytics.Channel.Activity, Analytics.ChannelName.Calendar);

                GlobalManager globalManager2 = GlobalManager.getInstance();
                globalManager2.onSnooze(appAlarmAtPosition, analytics2);
                break;
        }
        return super.onContextItemSelected(item);
    }

}
