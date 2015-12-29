package cz.jaro.alarmmorning;

import android.app.Dialog;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.app.TimePickerDialog;
import android.content.Context;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import java.util.Calendar;
import java.util.GregorianCalendar;

import cz.jaro.alarmmorning.model.AlarmDataSource;
import cz.jaro.alarmmorning.model.Day;
import cz.jaro.alarmmorning.model.Defaults;

/**
 * Provide views to RecyclerView with data from mDataSet.
 */
public class CalendarAdapter extends RecyclerView.Adapter<CalendarAdapter.CalendarViewHolder> implements TimePickerDialog.OnTimeSetListener {

    private CalendarActivity calendarActivity;

    private GregorianCalendar today;

    private AlarmDataSource datasource;

    private Day changingDay;

    /**
     * Initialize the Adapter.
     */
    public CalendarAdapter(CalendarActivity calendarActivity) {
        datasource = new AlarmDataSource(calendarActivity);
        datasource.open();

        this.calendarActivity = calendarActivity;
    }

    public void onResume() {
        today = new GregorianCalendar();
    }

    /**
     * Create new views (invoked by the layout manager)
     */
    @Override
    public CalendarViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        View v = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.calendar_row_item, viewGroup, false);

        return new CalendarViewHolder(v, calendarActivity.getFragmentManager(), this);
    }

    /**
     * Replaces the contents of a view (invoked by the layout manager)
     */
    @Override
    public void onBindViewHolder(CalendarViewHolder viewHolder, final int position) {
        GregorianCalendar date = addDays(today, position);

        int dayOfWeek = date.get(Calendar.DAY_OF_WEEK);
        String dayOfWeekText = Localization.dayOfWeekToString(dayOfWeek);
        viewHolder.getTextDayOfWeek().setText(dayOfWeekText);

        String dateText = Localization.dateToString(date.getTime());
        viewHolder.getTextDate().setText(dateText);

        Day day = datasource.loadDay(date);
        String timeText;
        if (day.getState() == AlarmDataSource.DAY_STATE_DEFAULT) {
            Defaults defaults = day.getDefaults();
            if (defaults.getState() == AlarmDataSource.DEFAULT_STATE_SET) {
                timeText = Localization.timeToString(defaults.getHours(), defaults.getMinutes(), calendarActivity);
            } else {
                timeText = calendarActivity.getResources().getString(R.string.alarm_unset);
            }
        } else if (day.getState() == AlarmDataSource.DAY_STATE_SET) {
            if (day.getHours() == AlarmDataSource.VALUE_UNSET) {
                Defaults defaults = day.getDefaults();
                timeText = Localization.timeToString(defaults.getHours(), defaults.getMinutes(), calendarActivity);
            } else {
                timeText = Localization.timeToString(day.getHours(), day.getMinutes(), calendarActivity);
            }
        } else { // day.getState() == AlarmDataSource.DAY_STATE_UNSET
            timeText = calendarActivity.getResources().getString(R.string.alarm_unset);
        }
        viewHolder.getTextTime().setText(timeText);

        String stateText;
        switch (day.getState()) {
            case AlarmDataSource.DAY_STATE_DEFAULT:
                stateText = "";
                break;
            default:
                stateText = calendarActivity.getResources().getString(R.string.alarm_state_changed);
                break;
        }
        viewHolder.getTextState().setText(stateText);

//        viewHolder.getTextComment().setText("Alarm will sound in 8:25. This is a long text.");

        viewHolder.setDay(day);
    }

    /**
     * Return the size of the dataset (invoked by the layout manager)
     */
    @Override
    public int getItemCount() {
        return AlarmDataSource.HORIZON_DAYS;
    }

    public static GregorianCalendar addDays(GregorianCalendar today, int numberOfDays) {
        GregorianCalendar date = (GregorianCalendar) today.clone();
        date.add(Calendar.DATE, numberOfDays);
        return date;
    }

    @Override
    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
        // Save data
        changingDay.setState(AlarmDataSource.DAY_STATE_SET);
        changingDay.setHours(hourOfDay);
        changingDay.setMinutes(minute);

        datasource.saveDay(changingDay);

        notifyDataSetChanged();

        Context context = calendarActivity.getBaseContext();
        SystemAlarm systemAlarm = SystemAlarm.getInstance(context);
        systemAlarm.setAlarm();
    }

    public void onLongClick() {
        // Save data
        if (changingDay.getState() == AlarmDataSource.DAY_STATE_DEFAULT) {
            Defaults defaults = changingDay.getDefaults();
            if (defaults.getState() == AlarmDataSource.DEFAULT_STATE_SET) {
                changingDay.setState(AlarmDataSource.DAY_STATE_UNSET);
            } else { // defaults.getState() == AlarmDataSource.DEFAULT_STATE_UNSET
                changingDay.setState(AlarmDataSource.DAY_STATE_SET);
            }
        } else if (changingDay.getState() == AlarmDataSource.DAY_STATE_SET) {
            changingDay.setState(AlarmDataSource.DAY_STATE_UNSET);
        } else { // changingDay.getState() == AlarmDataSource.DAY_STATE_UNSET
            changingDay.setState(AlarmDataSource.DAY_STATE_SET);
        }

        datasource.saveDay(changingDay);

        notifyDataSetChanged();

        Context context = calendarActivity.getBaseContext();
        SystemAlarm systemAlarm = SystemAlarm.getInstance(context);
        systemAlarm.setAlarm();
    }

    public void setChangingDay(Day changingDay) {
        this.changingDay = changingDay;
    }

    /**
     * Provide a reference to the type of views that you are using (custom ViewHolder)
     */
    public static class CalendarViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnLongClickListener {
        private final FragmentManager fragmentManager;
        private final CalendarAdapter calendarAdapter;

        private Day day;

        private final TextView textDayOfWeek;
        private final TextView textDate;
        private final TextView textTime;
        private final TextView textState;
        private final TextView textComment;

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

        @Override
        public void onClick(View view) {
            calendarAdapter.setChangingDay(day);

            TimePickerFragment fragment = new TimePickerFragment();

            fragment.setCalendarViewHolder(this);

            // Preset current time
            Bundle bundle = new Bundle();
            if (day.getState() == AlarmDataSource.DAY_STATE_DEFAULT) {
                bundle.putInt(TimePickerFragment.HOURS, day.getDefaults().getHours());
                bundle.putInt(TimePickerFragment.MINUTES, day.getDefaults().getMinutes());
            } else {
                if (day.getHours() == AlarmDataSource.VALUE_UNSET) {
                    Defaults defaults = day.getDefaults();
                    bundle.putInt(TimePickerFragment.HOURS, defaults.getHours());
                    bundle.putInt(TimePickerFragment.MINUTES, defaults.getMinutes());
                } else {
                    bundle.putInt(TimePickerFragment.HOURS, day.getHours());
                    bundle.putInt(TimePickerFragment.MINUTES, day.getMinutes());
                }
            }
            fragment.setArguments(bundle);

            fragment.show(fragmentManager, "timePicker");
        }

        @Override
        public boolean onLongClick(View view) {
            calendarAdapter.setChangingDay(day);
            calendarAdapter.onLongClick();
            return true;
        }
    }

    public static class TimePickerFragment extends DialogFragment {

        public static final String HOURS = "hours";
        public static final String MINUTES = "minutes";

        private CalendarViewHolder calendarViewHolder;

        public void setCalendarViewHolder(CalendarViewHolder calendarViewHolder) {
            this.calendarViewHolder = calendarViewHolder;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            int hours = getArguments().getInt(HOURS);
            int minutes = getArguments().getInt(MINUTES);

            TimePickerDialog timePickerDialog = new TimePickerDialog(getActivity(), calendarViewHolder.calendarAdapter, hours, minutes, DateFormat.is24HourFormat(getActivity()));

            return timePickerDialog;
        }

    }

}