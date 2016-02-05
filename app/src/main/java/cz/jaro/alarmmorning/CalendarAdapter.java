package cz.jaro.alarmmorning;

import android.app.FragmentManager;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import java.util.Calendar;

import cz.jaro.alarmmorning.clock.Clock;
import cz.jaro.alarmmorning.clock.SystemClock;
import cz.jaro.alarmmorning.model.AlarmDataSource;
import cz.jaro.alarmmorning.model.Day;

/**
 * Provide views to RecyclerView with data from mDataSet.
 */
public class CalendarAdapter extends RecyclerView.Adapter<CalendarAdapter.CalendarViewHolder> implements TimePickerDialog.OnTimeSetListener {

    private static final String TAG = CalendarAdapter.class.getSimpleName();

    private static final int POSITION_UNSET = -1;

    private ActivityInterface activityInterface;
    private Calendar today;
    private AlarmDataSource dataSource;
    private Day changingDay;
    private int changingItem;

    private int positionNextAlarm;

    // TODO Change time of ringing alarm

    // TODO Show not dismissed alarms from previous days

    // TODO Add menu item "Dismiss" in "near future" and when snoozed

    /**
     * Initialize the Adapter.
     *
     * @param activityInterface
     */
    public CalendarAdapter(ActivityInterface activityInterface) {
        this.activityInterface = activityInterface;

        dataSource = new AlarmDataSource(activityInterface.getContextI());
        dataSource.open();

        today = getToday(clock());
        updatePositionNextAlarm();
    }

    /**
     * Create new views (invoked by the layout manager)
     */
    @Override
    public CalendarViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        View v = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.calendar_row_item, viewGroup, false);

        return new CalendarViewHolder(v, activityInterface.getFragmentManagerI(), this);
    }

    /**
     * Replaces the contents of a view (invoked by the layout manager)
     */
    @Override
    public void onBindViewHolder(CalendarViewHolder viewHolder, final int position) {
        Calendar date = addDays(today, position);

        Resources res = activityInterface.getResourcesI();

        int dayOfWeek = date.get(Calendar.DAY_OF_WEEK);
        String dayOfWeekText = Localization.dayOfWeekToString(dayOfWeek, clock());
        viewHolder.getTextDayOfWeek().setText(dayOfWeekText);

        String dateText = Localization.dateToString(date.getTime());
        viewHolder.getTextDate().setText(dateText);

        Day day = dataSource.loadDayDeep(date);
        String timeText;
        if (day.isEnabled()) {
            timeText = Localization.timeToString(day.getHourX(), day.getMinuteX(), activityInterface.getContextI(), clock());
        } else {
            timeText = res.getString(R.string.alarm_unset);
        }
        viewHolder.getTextTime().setText(timeText);

        boolean enabled = true;
        if (position == 0) {
            GlobalManager globalManager = new GlobalManager(activityInterface.getContextI());
            if (globalManager.isValid()) {
                int state = globalManager.getState();

                if (state == GlobalManager.STATE_DISMISSED_BEFORE_RINGING || state == GlobalManager.STATE_DISMISSED) {
                    enabled = false;
                }
            } else {
                enabled = !day.isPassed(clock());
            }
        }
        viewHolder.getTextTime().setEnabled(enabled);

        String stateText;
        if (position == 0) {
            GlobalManager globalManager = new GlobalManager(activityInterface.getContextI());
            if (globalManager.isValid()) {
                int state = globalManager.getState();

                if (state == GlobalManager.STATE_FUTURE) {
                    stateText = day.sameAsDefault() ? "" : res.getString(R.string.alarm_state_changed);
                } else if (state == GlobalManager.STATE_DISMISSED_BEFORE_RINGING) {
                    if (day.isPassed(clock()))
                        stateText = res.getString(R.string.alarm_state_passed);
                    else
                        stateText = res.getString(R.string.alarm_state_dismissed_before_ringing);
                } else if (state == GlobalManager.STATE_RINGING) {
                    stateText = res.getString(R.string.alarm_state_ringing);
                } else if (state == GlobalManager.STATE_SNOOZED) {
                    stateText = res.getString(R.string.alarm_state_snoozed);
                } else if (state == GlobalManager.STATE_DISMISSED) {
                    stateText = res.getString(R.string.alarm_state_passed);
                } else {
                    // This is generally an error, because the state should be properly set. However, when upgrading the app (and probably on boot), the activity may become visible BEFORE the receiver that sets the system alarm and state.
                    stateText = "";
                }
            } else {
                if (day.isPassed(clock())) {
                    stateText = res.getString(R.string.alarm_state_passed);
                } else {
                    stateText = day.sameAsDefault() ? "" : res.getString(R.string.alarm_state_changed);
                }
            }
        } else {
            stateText = day.sameAsDefault() ? "" : res.getString(R.string.alarm_state_changed);
        }
        viewHolder.getTextState().setText(stateText);

        String messageText;
        if (position == positionNextAlarm) {
            long diff = day.getTimeToRing(clock());

            TimeDifference timeDifference = TimeDifference.getTimeUnits(diff);

            if (timeDifference.days > 0) {
                messageText = String.format(res.getString(R.string.time_to_ring_message_days), timeDifference.days, timeDifference.hours);
            } else if (timeDifference.hours > 0) {
                messageText = String.format(res.getString(R.string.time_to_ring_message_hours), timeDifference.hours, timeDifference.minutes);
            } else {
                messageText = String.format(res.getString(R.string.time_to_ring_message_minutes), timeDifference.minutes, timeDifference.seconds);
            }
        } else {
            messageText = "";
        }
        viewHolder.getTextComment().setText(messageText);

        viewHolder.setDay(day);
        viewHolder.setPosition(position);
    }

    private Clock clock() {
        return new SystemClock();
    }

    private int calcPositionNextAlarm() {
        for (int position = 0; position < AlarmDataSource.HORIZON_DAYS; position++) {

            Calendar date = addDays(today, position);

            Day day = dataSource.loadDayDeep(date);

            if (day.isEnabled() && !day.isPassed(clock())) {
                return position;
            }
        }
        return POSITION_UNSET;
    }

    protected void updatePositionNextAlarm() {
        int newPositionNextAlarm = calcPositionNextAlarm();

        if (positionNextAlarm != newPositionNextAlarm) {
            Log.d(TAG, "Next alarm is at position " + newPositionNextAlarm);

            int oldPositionNextAlarm = positionNextAlarm;
            positionNextAlarm = newPositionNextAlarm;

            if (oldPositionNextAlarm != POSITION_UNSET)
                notifyItemChanged(oldPositionNextAlarm);
            if (positionNextAlarm != POSITION_UNSET)
                notifyItemChanged(positionNextAlarm);
        }
    }

    /**
     * Return the size of the dataset (invoked by the layout manager)
     */
    @Override
    public int getItemCount() {
        return AlarmDataSource.HORIZON_DAYS;
    }

    public void onResume() {
        Calendar today2 = getToday(clock());

        if (!today.equals(today2)) {
            today = today2;
            notifyDataSetChanged();
        } else {
            notifyItemChanged(positionNextAlarm);
        }
    }

    public void onDestroy() {
        dataSource.close();
    }

    public void onSystemTimeChange() {
        Log.v(TAG, "onSystemTimeChange()");

        // Update time to next alarm
        if (positionNextAlarm != POSITION_UNSET)
            notifyItemChanged(positionNextAlarm);

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
                notifyItemRangeRemoved(0, diffInDays);
            } else {
                notifyDataSetChanged();
            }
        }
    }

    public void onTimeOrTimeZoneChange() {
        Log.d(TAG, "onTimeOrTimeZoneChange()");
        today = getToday(clock());
        notifyDataSetChanged();
    }

    @Override
    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
        changingDay.setState(Day.STATE_ENABLED);
        changingDay.setHour(hourOfDay);
        changingDay.setMinute(minute);

        save(changingDay);
    }

    public void onLongClick() {
        changingDay.reverse();

        save(changingDay);
    }

    private void save(Day day) {
        dataSource.saveDay(day);

        refresh();

        String toastText = formatToastText(day);
        Context context = activityInterface.getContextI();
        Toast.makeText(context, toastText, Toast.LENGTH_LONG).show();
    }

    private void refresh() {
        notifyItemChanged(changingItem);
        updatePositionNextAlarm();

        Context context = activityInterface.getContextI();
        GlobalManager globalManager = new GlobalManager(context);
        globalManager.onAlarmSet();
    }

    private String formatToastText(Day day) {
        Resources res = activityInterface.getResourcesI();
        String toastText;

        if (!day.isEnabled()) {
            toastText = res.getString(R.string.time_to_ring_toast_off);
        } else {
            long diff = day.getTimeToRing(clock());

            if (diff < 0) {
                toastText = res.getString(R.string.time_to_ring_toast_passed);
            } else {
                TimeDifference timeDifference = TimeDifference.getTimeUnits(diff);
                if (timeDifference.days > 0) {
                    toastText = String.format(res.getString(R.string.time_to_ring_toast_days), timeDifference.days, timeDifference.hours, timeDifference.minutes);
                } else if (timeDifference.hours > 0) {
                    toastText = String.format(res.getString(R.string.time_to_ring_toast_hours), timeDifference.hours, timeDifference.minutes);
                } else {
                    toastText = String.format(res.getString(R.string.time_to_ring_toast_minutes), timeDifference.minutes, timeDifference.seconds);
                }
            }
        }
        return toastText;
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

    public void setChangingDay(Day changingDay, int changingItem) {
        this.changingDay = changingDay;
        this.changingItem = changingItem;
    }

    /**
     * Provide a reference to the type of views that you are using (custom ViewHolder)
     */
    public static class CalendarViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnLongClickListener {
        private final FragmentManager fragmentManager;
        private final CalendarAdapter calendarAdapter;
        private final TextView textDayOfWeek;
        private final TextView textDate;
        private final TextView textTime;
        private final TextView textState;
        private final TextView textComment;
        private Day day;
        private int position;

        public CalendarViewHolder(View view, final FragmentManager fragmentManager, CalendarAdapter calendarAdapter) {
            super(view);

            this.fragmentManager = fragmentManager;
            this.calendarAdapter = calendarAdapter;

            textDayOfWeek = (TextView) view.findViewById(R.id.textDayOfWeekCal);
            textDate = (TextView) view.findViewById(R.id.textDate);
            textTime = (TextView) view.findViewById(R.id.textTimeCal);
            textState = (TextView) view.findViewById(R.id.textState);
            textComment = (TextView) view.findViewById(R.id.textComment);

            view.setOnClickListener(this);
            view.setOnLongClickListener(this);

        }

        public TextView getTextDayOfWeek() {
            return textDayOfWeek;
        }

        public TextView getTextDate() {
            return textDate;
        }

        public TextView getTextTime() {
            return textTime;
        }

        public TextView getTextState() {
            return textState;
        }

        public TextView getTextComment() {
            return textComment;
        }

        public void setDay(Day day) {
            this.day = day;
        }

        public void setPosition(int position) {
            this.position = position;
        }

        @Override
        public void onClick(View view) {
            calendarAdapter.setChangingDay(day, position);

            TimePickerFragment fragment = new TimePickerFragment();

            fragment.setOnTimeSetListener(calendarAdapter);

            // Preset time
            Bundle bundle = new Bundle();
            bundle.putInt(TimePickerFragment.HOURS, day.getHourX());
            bundle.putInt(TimePickerFragment.MINUTES, day.getMinuteX());
            fragment.setArguments(bundle);

            fragment.show(fragmentManager, "timePicker");
        }

        @Override
        public boolean onLongClick(View view) {
            calendarAdapter.setChangingDay(day, position);
            calendarAdapter.onLongClick();
            return true;
        }
    }

    private static class TimeDifference {
        long days;
        long hours;
        long minutes;
        long seconds;

        public static TimeDifference getTimeUnits(long diff) {
            TimeDifference timeDifference = new TimeDifference();

            long remaining = diff;
            long length;

            length = 24 * 60 * 60 * 1000;
            timeDifference.days = remaining / length;
            remaining = remaining % length;

            length = 60 * 60 * 1000;
            timeDifference.hours = remaining / length;
            remaining = remaining % length;

            length = 60 * 1000;
            timeDifference.minutes = remaining / length;
            remaining = remaining % length;

            length = 1000;
            timeDifference.seconds = remaining / length;

            return timeDifference;
        }
    }
}

