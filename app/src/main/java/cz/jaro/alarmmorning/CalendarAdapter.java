package cz.jaro.alarmmorning;

import android.content.res.Resources;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.Calendar;

import cz.jaro.alarmmorning.model.AlarmDataSource;
import cz.jaro.alarmmorning.model.Day;

/**
 * Provide views to RecyclerView with data from mDataSet.
 */
public class CalendarAdapter extends RecyclerView.Adapter<CalendarAdapter.CalendarViewHolder> {

    private static final String TAG = CalendarAdapter.class.getSimpleName();

    private final CalendarFragment fragment;

    /**
     * Initialize the Adapter.
     *
     * @param fragment
     */
    public CalendarAdapter(CalendarFragment fragment) {
        this.fragment = fragment;
    }

    /**
     * Create new views (invoked by the layout manager)
     */
    @Override
    public CalendarViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.calendar_row_item, viewGroup, false);

        view.setOnClickListener(fragment);

        return new CalendarViewHolder(view);
    }

    /**
     * Replaces the contents of a view (invoked by the layout manager)
     */
    @Override
    public void onBindViewHolder(CalendarViewHolder viewHolder, final int position) {
        Log.v(TAG, "onBindViewHolder(position=" + position);

        Day day = fragment.loadPosition(position);
        Calendar date = day.getDateTime();

        Resources res = fragment.getResources();

        int dayOfWeek = date.get(Calendar.DAY_OF_WEEK);
        String dayOfWeekText = Localization.dayOfWeekToStringShort(res, dayOfWeek);
        viewHolder.getTextDayOfWeek().setText(dayOfWeekText);

        String dateText = Localization.dateToStringVeryShort(res, date.getTime());
        viewHolder.getTextDate().setText(dateText);

        int backgroundColor;
        com.ibm.icu.util.Calendar c = com.ibm.icu.util.Calendar.getInstance();
        int dayOfWeekType = c.getDayOfWeekType(dayOfWeek);
        switch (dayOfWeekType) {
            case com.ibm.icu.util.Calendar.WEEKEND:
                backgroundColor = res.getColor(R.color.weekend);
                break;

            default:
                backgroundColor = res.getColor(R.color.primary_dark);
        }
        viewHolder.getTextDayOfWeek().setBackgroundColor(backgroundColor);
        viewHolder.getTextDate().setBackgroundColor(backgroundColor);

        String timeText;
        if (day.isEnabled()) {
            timeText = Localization.timeToString(day.getHourX(), day.getMinuteX(), fragment.getActivity());
        } else {
            timeText = res.getString(R.string.alarm_unset);
        }
        viewHolder.getTextTime().setText(timeText);

        GlobalManager globalManager = new GlobalManager(fragment.getActivity());
        int state = globalManager.getState(day.getDateTime());

        boolean enabled;
        enabled = state != GlobalManager.STATE_DISMISSED_BEFORE_RINGING && state != GlobalManager.STATE_DISMISSED;
        viewHolder.getTextTime().setEnabled(enabled);

        String stateText;
        if (day.isHoliday() && day.getState() == Day.STATE_RULE) {
            stateText = res.getString(R.string.holiday);
        } else {
            if (state == GlobalManager.STATE_FUTURE) {
                stateText = day.sameAsDefault() && !day.isHoliday() ? "" : res.getString(R.string.alarm_state_changed);
            } else if (state == GlobalManager.STATE_DISMISSED_BEFORE_RINGING) {
                if (day.isPassed(fragment.clock()))
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
                throw new IllegalArgumentException("Unexpected argument " + state);
            }
        }
        viewHolder.getTextState().setText(stateText);

        String messageText;
        if (day.isHoliday() && day.getState() == Day.STATE_RULE) {
            messageText = day.holidayName();
        } else {
            if (fragment.positionWithNextAlarm(position)) {
                long diff = day.getTimeToRing(fragment.clock());

                TimeDifference timeDifference = TimeDifference.split(diff);

                if (timeDifference.days > 0) {
                    messageText = res.getString(R.string.time_to_ring_message_days, timeDifference.days, timeDifference.hours);
                } else if (timeDifference.hours > 0) {
                    messageText = res.getString(R.string.time_to_ring_message_hours, timeDifference.hours, timeDifference.minutes);
                } else {
                    messageText = res.getString(R.string.time_to_ring_message_minutes, timeDifference.minutes, timeDifference.seconds);
                }
            } else {
                messageText = "";
            }
        }
        viewHolder.getTextComment().setText(messageText);
    }

    /**
     * Return the size of the dataset (invoked by the layout manager)
     */
    @Override
    public int getItemCount() {
        return AlarmDataSource.HORIZON_DAYS;
    }


    /**
     * Provide a reference to the type of views that you are using (custom ViewHolder)
     */
    public static class CalendarViewHolder extends RecyclerView.ViewHolder implements View.OnLongClickListener {
        private final TextView textDayOfWeek;
        private final TextView textDate;
        private final TextView textTime;
        private final TextView textState;
        private final TextView textComment;

        public CalendarViewHolder(View view) {
            super(view);

            textDayOfWeek = (TextView) view.findViewById(R.id.textDayOfWeekCal);
            textDate = (TextView) view.findViewById(R.id.textDate);
            textTime = (TextView) view.findViewById(R.id.textTimeCal);
            textState = (TextView) view.findViewById(R.id.textState);
            textComment = (TextView) view.findViewById(R.id.textComment);

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

        @Override
        public boolean onLongClick(View view) {
            itemView.showContextMenu();
            return true;
        }
    }


    static class TimeDifference {
        long days;
        long hours;
        long minutes;
        long seconds;

        public static TimeDifference split(long diff) {
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