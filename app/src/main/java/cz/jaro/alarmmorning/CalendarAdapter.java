package cz.jaro.alarmmorning;

import android.content.res.Resources;
import android.graphics.Rect;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.TouchDelegate;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Calendar;

import cz.jaro.alarmmorning.calendar.CalendarUtils;
import cz.jaro.alarmmorning.model.AppAlarm;
import cz.jaro.alarmmorning.model.Day;
import cz.jaro.alarmmorning.model.OneTimeAlarm;

/**
 * Provide views to RecyclerView with data from mDataSet.
 */
public class CalendarAdapter extends RecyclerView.Adapter<CalendarAdapter.CalendarViewHolder> {

    private final CalendarFragment fragment;

    /**
     * Initialize the Adapter.
     *
     * @param fragment Fragment that contains the widget that uses this adapter
     */
    public CalendarAdapter(CalendarFragment fragment) {
        this.fragment = fragment;
    }

    /**
     * Create new views (invoked by the layout manager)
     */
    @NonNull
    @Override
    public CalendarViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.calendar_row_item, viewGroup, false);
        return new CalendarViewHolder(view);
    }

    /**
     * Replaces the contents of a view (invoked by the layout manager)
     */
    @Override
    public void onBindViewHolder(@NonNull CalendarViewHolder viewHolder, final int position) {
        MyLog.v("onBindViewHolder(position=" + position);

        AppAlarm appAlarm = fragment.loadPosition(position);
        Day day = null;
        OneTimeAlarm oneTimeAlarm = null;
        if (appAlarm instanceof Day) {
            day = (Day) appAlarm;
        } else if (appAlarm instanceof OneTimeAlarm) {
            oneTimeAlarm = (OneTimeAlarm) appAlarm;
        } else {
            throw new IllegalArgumentException("Unexpected class " + appAlarm.getClass());
        }

        // Set listeners
        viewHolder.itemView.setOnClickListener(fragment);

        if (appAlarm instanceof OneTimeAlarm) {
            LinearLayout headerView = viewHolder.getHeaderDate();
            headerView.setOnClickListener(v -> fragment.onSetDate(viewHolder.itemView));
        }

        // Set appearance

        Calendar date = appAlarm.getDateTime();
        int dayOfWeek = date.get(Calendar.DAY_OF_WEEK);
        Resources res = fragment.getResources();

        String dayOfWeekText;
        boolean showDate = appAlarm instanceof Day ||
                position == 0 ||
                !CalendarUtils.onTheSameDate(date, fragment.loadPosition(position - 1).getDateTime());
        if (showDate) {
            dayOfWeekText = Localization.dayOfWeekToStringShort(res, dayOfWeek);
        } else {
            dayOfWeekText = "";
        }
        viewHolder.getTextDayOfWeek().setText(dayOfWeekText);

        String dateText;
        if (showDate) {
            dateText = Localization.dateToStringVeryShort(res, date.getTime());
        } else {
            dateText = "";
        }
        viewHolder.getTextDate().setText(dateText);

        int backgroundColor;
        com.ibm.icu.util.Calendar c = com.ibm.icu.util.Calendar.getInstance();
        int dayOfWeekType = c.getDayOfWeekType(dayOfWeek);
        if (dayOfWeekType == com.ibm.icu.util.Calendar.WEEKEND) {
            backgroundColor = res.getColor(R.color.weekend);
        } else {
            backgroundColor = res.getColor(R.color.primary_dark);
        }
        viewHolder.getTextDayOfWeek().setBackgroundColor(backgroundColor);
        viewHolder.getTextDate().setBackgroundColor(backgroundColor);

        String timeText;
        if (!(appAlarm instanceof Day) || day.isEnabled()) {
            timeText = Localization.timeToString(appAlarm.getHour(), appAlarm.getMinute(), fragment.getActivity());
        } else {
            timeText = res.getString(R.string.alarm_unset);
        }
        viewHolder.getTextTime().setText(timeText);

        GlobalManager globalManager = GlobalManager.getInstance();
        int state = globalManager.getState(appAlarm);

        boolean enabled;
        enabled = (appAlarm instanceof Day && !day.isEnabled()) || (state != GlobalManager.STATE_DISMISSED_BEFORE_RINGING && state != GlobalManager.STATE_DISMISSED);
        viewHolder.getTextTime().setEnabled(enabled);

        String stateText;
        if (appAlarm instanceof Day && day.isHoliday() && day.getState() == Day.STATE_RULE) {
            stateText = res.getString(R.string.holiday);
        } else if (appAlarm instanceof Day && !day.isEnabled()) {
            stateText = day.sameAsDefault() && !day.isHoliday() ? "" : res.getString(R.string.alarm_state_changed);
        } else {
            if (state == GlobalManager.STATE_FUTURE) {
                if (appAlarm instanceof Day)
                    stateText = day.sameAsDefault() && !day.isHoliday() ? "" : res.getString(R.string.alarm_state_changed);
                else
                    stateText = "";
            } else if (state == GlobalManager.STATE_DISMISSED_BEFORE_RINGING) {
                if (appAlarm.isPassed(fragment.clock()))
                    stateText = res.getString(R.string.alarm_state_passed);
                else
                    stateText = res.getString(R.string.alarm_state_dismissed_before_ringing);
            } else if (state == GlobalManager.STATE_RINGING) {
                stateText = res.getString(R.string.alarm_state_ringing);
            } else if (state == GlobalManager.STATE_SNOOZED) {
                Calendar ringAfterSnoozeTime = globalManager.loadRingAfterSnoozeTime();
                String ringAfterSnoozeTimeText = Localization.timeToString(ringAfterSnoozeTime.get(Calendar.HOUR_OF_DAY), ringAfterSnoozeTime.get(Calendar.MINUTE), fragment.getActivity());
                stateText = res.getString(R.string.alarm_state_snoozed, ringAfterSnoozeTimeText);
            } else if (state == GlobalManager.STATE_DISMISSED) {
                stateText = res.getString(R.string.alarm_state_passed);
            } else {
                throw new IllegalArgumentException("Unexpected argument " + state);
            }
        }
        viewHolder.getTextState().setText(stateText);

        if (appAlarm instanceof Day) {
            viewHolder.getTextName().setVisibility(View.GONE);
        } else {
            String name = oneTimeAlarm.getName();

            viewHolder.getTextName().setVisibility(View.VISIBLE);
            viewHolder.getTextName().setText(name == null ? "" : name);

            viewHolder.getTextName().setHint(" "); // This is required on my Samsung Galaxy S9. Otherwise, when the name is empty, it the touch (to start renaming) doesn't work.

            // Increase touch area
            View delegate = viewHolder.getTextName();
            final View parent = (View) delegate.getParent();
            // Post in the parent's message queue to make sure the parent lays out its children before we call getHitRect()
            parent.post(() -> {
                final Rect r = new Rect();
                delegate.getHitRect(r);

                RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) delegate.getLayoutParams();
                int gap = params.rightMargin;

                r.top -= gap; // Align touch area the right edge of activity (display)
                r.bottom += gap;
                r.left -= 2 * gap;
                r.right += gap;

                parent.setTouchDelegate(new TouchDelegate(r, delegate));
            });

            // Listeners for setting name
            viewHolder.getTextName().setOnFocusChangeListener((v, hasFocus) -> {
                if (hasFocus) {
                    fragment.onEditNameBegin(viewHolder);
                }
            });

            viewHolder.getTextName().setOnEditorActionListener((v, actionId, event) -> {
                if (actionId == EditorInfo.IME_ACTION_DONE
                        || (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                    fragment.onEditNameEnd();

                    return true;
                }
                return false;
            });
        }

        String commentText;
        if (appAlarm instanceof Day && day.isHoliday() && day.getState() == Day.STATE_RULE) {
            commentText = day.getHolidayDescription();
        } else {
            if (fragment.isPositionWithNextAlarm(position) &&
                    !(appAlarm instanceof OneTimeAlarm && (state == GlobalManager.STATE_DISMISSED_BEFORE_RINGING || state == GlobalManager.STATE_DISMISSED))) {
                commentText = getDurationToAlarm(appAlarm);
            } else {
                commentText = "";
            }
        }
        viewHolder.getTextComment().setText(commentText);
    }

    @NonNull
    String getDurationToAlarm(AppAlarm appAlarm) {
        long diff = appAlarm.getTimeToRing(fragment.clock());

        return formatTimeDifference(diff, false, fragment.getResources());
    }

    @NonNull
    public static String formatTimeDifference(long diff, boolean trimZeroSubunits, Resources res) {
        String messageText;

        TimeDifference timeDifference = new TimeDifference(diff);
        String sign = timeDifference.isNegative() ? res.getString(R.string.negative_sign) : "";

        if (timeDifference.days > 0) {
            if (timeDifference.hours != 0 || !trimZeroSubunits)
                messageText = sign + res.getString(R.string.time_to_ring_message_days_and_hours, timeDifference.days, timeDifference.hours);
            else
                messageText = sign + res.getString(R.string.time_to_ring_message_days, timeDifference.days);
        } else if (timeDifference.hours > 0) {
            if (timeDifference.minutes != 0 || !trimZeroSubunits)
                messageText = sign + res.getString(R.string.time_to_ring_message_hours_and_minutes, timeDifference.hours, timeDifference.minutes);
            else
                messageText = sign + res.getString(R.string.time_to_ring_message_hours, timeDifference.hours);
        } else if (timeDifference.minutes > 0) {
            if (timeDifference.seconds != 0 || !trimZeroSubunits)
                messageText = sign + res.getString(R.string.time_to_ring_message_minutes_and_seconds, timeDifference.minutes, timeDifference.seconds);
            else
                messageText = sign + res.getString(R.string.time_to_ring_message_minutes, timeDifference.minutes);
        } else {
            messageText = sign + res.getString(R.string.time_to_ring_message_seconds, timeDifference.seconds);
        }

        return messageText;
    }

    /**
     * Return the size of the dataset (invoked by the layout manager)
     */
    @Override
    public int getItemCount() {
        return fragment.getItemCount();
    }

    /**
     * Provide a reference to the type of views that you are using (custom ViewHolder)
     */
    public static class CalendarViewHolder extends RecyclerView.ViewHolder implements View.OnLongClickListener {
        private final TextView textDayOfWeek;
        private final TextView textDate;
        private final TextView textTime;
        private final TextView textState;
        private final EditText textName;
        private final TextView textComment;
        private final LinearLayout headerDate;

        CalendarViewHolder(View view) {
            super(view);

            textDayOfWeek = view.findViewById(R.id.textDayOfWeekCal);
            textDate = view.findViewById(R.id.textDate);
            textTime = view.findViewById(R.id.textTimeCal);
            textState = view.findViewById(R.id.textState);
            textName = view.findViewById(R.id.textName);
            textComment = view.findViewById(R.id.textComment);
            headerDate = view.findViewById(R.id.headerDate);

            view.setOnLongClickListener(this);
        }

        TextView getTextDayOfWeek() {
            return textDayOfWeek;
        }

        TextView getTextDate() {
            return textDate;
        }

        TextView getTextTime() {
            return textTime;
        }

        TextView getTextState() {
            return textState;
        }

        public EditText getTextName() {
            return textName;
        }

        TextView getTextComment() {
            return textComment;
        }

        LinearLayout getHeaderDate() {
            return headerDate;
        }

        @Override
        public boolean onLongClick(View view) {
            itemView.showContextMenu();
            return true;
        }
    }
}