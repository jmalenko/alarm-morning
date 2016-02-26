package cz.jaro.alarmmorning;

import android.content.res.Resources;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.Calendar;

import cz.jaro.alarmmorning.clock.Clock;
import cz.jaro.alarmmorning.clock.SystemClock;
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
        Log.v(TAG, "Replacing position " + position);

        Day day = fragment.loadPosition(position);
        Calendar date = day.getDateTime();

        Resources res = fragment.getResources();

        int dayOfWeek = date.get(Calendar.DAY_OF_WEEK);
        String dayOfWeekText = Localization.dayOfWeekToString(dayOfWeek, clock());
        viewHolder.getTextDayOfWeek().setText(dayOfWeekText);

        String dateText = Localization.dateToStringVeryShort(date.getTime());
        viewHolder.getTextDate().setText(dateText);

        String timeText;
        if (day.isEnabled()) {
            timeText = Localization.timeToString(day.getHourX(), day.getMinuteX(), fragment.activityInterface.getContextI(), clock());
        } else {
            timeText = res.getString(R.string.alarm_unset);
        }
        viewHolder.getTextTime().setText(timeText);

        boolean enabled = true;
        if (position == 0) {
            GlobalManager globalManager = new GlobalManager(fragment.activityInterface.getContextI());
            int state = globalManager.getState(day.getDateTime());
            if (state != GlobalManager.STATE_UNDEFINED) {
                enabled = state != GlobalManager.STATE_DISMISSED_BEFORE_RINGING && state != GlobalManager.STATE_DISMISSED;
            } else {
                enabled = !day.isPassed(clock());
            }
        }
        viewHolder.getTextTime().setEnabled(enabled);

        String stateText;
        if (position == 0) {
            GlobalManager globalManager = new GlobalManager(fragment.activityInterface.getContextI());
            int state = globalManager.getState(day.getDateTime());
            if (state != GlobalManager.STATE_UNDEFINED) {
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
                    throw new IllegalArgumentException("Unexpected argument " + state);
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
        if (fragment.positionWithNextAlarm(position)) {
            long diff = day.getTimeToRing(clock());

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
        viewHolder.getTextComment().setText(messageText);
    }

    // TODO Solve dependency on clock
    private Clock clock() {
        return new SystemClock();
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