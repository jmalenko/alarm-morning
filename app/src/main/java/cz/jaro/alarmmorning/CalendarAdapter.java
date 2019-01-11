package cz.jaro.alarmmorning;

import android.content.res.Resources;
import android.graphics.Rect;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
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

import java.util.Calendar;

import cz.jaro.alarmmorning.calendar.CalendarUtils;
import cz.jaro.alarmmorning.model.AppAlarm;
import cz.jaro.alarmmorning.model.Day;
import cz.jaro.alarmmorning.model.OneTimeAlarm;

/**
 * Provide views to RecyclerView with data from mDataSet.
 */
public class CalendarAdapter extends RecyclerView.Adapter<CalendarAdapter.CalendarViewHolder> {

    private static final String TAG = CalendarAdapter.class.getSimpleName();

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
    @Override
    public CalendarViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.calendar_row_item, viewGroup, false);
        return new CalendarViewHolder(view);
    }

    /**
     * Replaces the contents of a view (invoked by the layout manager)
     */
    @Override
    public void onBindViewHolder(CalendarViewHolder viewHolder, final int position) {
        Log.v(TAG, "onBindViewHolder(position=" + position);

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
            headerView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    fragment.onSetDate(viewHolder.itemView);
                }
            });
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
        if (!(appAlarm instanceof Day) || day.isEnabled()) {
            // FIXME Use 24 hour time format on my phone
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
                stateText = res.getString(R.string.alarm_state_snoozed);
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
            viewHolder.getTextName().setText(name == null || name.isEmpty() ? "" : name);

            // Increase touch area
            View delegate = viewHolder.getTextName();
            final View parent = (View) delegate.getParent();
            parent.post(new Runnable() {
                // Post in the parent's message queue to make sure the parent lays out its children before we call getHitRect()
                public void run() {
                    final Rect r = new Rect();
                    delegate.getHitRect(r);
                    RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) delegate.getLayoutParams();
                    r.top -= params.rightMargin;
                    r.bottom += params.rightMargin;
                    r.left -= params.rightMargin;
                    r.right += params.rightMargin;
                    parent.setTouchDelegate(new TouchDelegate(r, delegate));
                }
            });

            // Listeners for setting name
            viewHolder.getTextName().setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View v, boolean hasFocus) {
                    if (hasFocus) {
                        fragment.onEditNameBegin(viewHolder);
                    }
                }
            });

            viewHolder.getTextName().setOnEditorActionListener(new TextView.OnEditorActionListener() {
                @Override
                public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                    if (actionId == EditorInfo.IME_ACTION_DONE) {
                        fragment.onEditNameEnd();

                        return true;
                    }
                    return false;
                }
            });
        }

        String commentText;
        if (appAlarm instanceof Day && day.isHoliday() && day.getState() == Day.STATE_RULE) {
            commentText = day.getHolidayDescription();
        } else {
            if (fragment.isPositionWithNextAlarm(position) &&
                    !(appAlarm instanceof OneTimeAlarm && (state == GlobalManager.STATE_DISMISSED_BEFORE_RINGING || state == GlobalManager.STATE_DISMISSED))) {
                commentText = getTimeToAlarm(appAlarm);
            } else {
                commentText = "";
            }
        }
        viewHolder.getTextComment().setText(commentText);
    }

    @NonNull
    String getTimeToAlarm(AppAlarm appAlarm) {
        long diff = appAlarm.getTimeToRing(fragment.clock());

        String messageText = formatTimeDifference(diff, fragment.getResources());
        return messageText;
    }

    @NonNull
    public static String formatTimeDifference(long diff, Resources res) {
        String messageText;

        TimeDifference timeDifference = new TimeDifference(diff);
        String sign = timeDifference.isNegative() ? res.getString(R.string.negative_sign) : "";

        if (timeDifference.days > 0) {
            messageText = sign + res.getString(R.string.time_to_ring_message_days, timeDifference.days, timeDifference.hours);
        } else if (timeDifference.hours > 0) {
            messageText = sign + res.getString(R.string.time_to_ring_message_hours, timeDifference.hours, timeDifference.minutes);
        } else if (timeDifference.minutes > 0) {
            messageText = sign + res.getString(R.string.time_to_ring_message_minutes, timeDifference.minutes, timeDifference.seconds);
        } else {
            messageText = sign + res.getString(R.string.time_to_ring_message_seconds, timeDifference.seconds, sign);
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

        public CalendarViewHolder(View view) {
            super(view);

            textDayOfWeek = (TextView) view.findViewById(R.id.textDayOfWeekCal);
            textDate = (TextView) view.findViewById(R.id.textDate);
            textTime = (TextView) view.findViewById(R.id.textTimeCal);
            textState = (TextView) view.findViewById(R.id.textState);
            textName = (EditText) view.findViewById(R.id.textName);
            textComment = (TextView) view.findViewById(R.id.textComment);
            headerDate = (LinearLayout) view.findViewById(R.id.headerDate);

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

        public EditText getTextName() {
            return textName;
        }

        public TextView getTextComment() {
            return textComment;
        }

        public LinearLayout getHeaderDate() {
            return headerDate;
        }

        @Override
        public boolean onLongClick(View view) {
            itemView.showContextMenu();
            return true;
        }
    }
}