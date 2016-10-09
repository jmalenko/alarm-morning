package cz.jaro.alarmmorning.wizard;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TimePicker;

import cz.jaro.alarmmorning.GlobalManager;
import cz.jaro.alarmmorning.R;
import cz.jaro.alarmmorning.model.AlarmDataSource;
import cz.jaro.alarmmorning.model.Defaults;

/**
 * Rules:
 * <p/>
 * 1. Use the median alarm time of enabled workdays.
 * <p/>
 * 2. If there is no enabled alarm on a workday, then use the system alarm clock time.
 * <p/>
 * 3. If system alarm clock time is not defined, use calendar: find all the first non-all-day meetings in past 30 days, take median and subtract 1 hour.
 * <p/>
 * 4. If there are no such meetings, use 7:00 am.
 */

public class SetTimeSlide extends BaseFragment implements TimePicker.OnTimeChangedListener {

    private static final String TAG = SetTimeSlide.class.getSimpleName();

    // TODO When run from Settings - solve that the workdays may have different alarm times. (Probably show checkbox "overload")

    int hourOfDay;
    int minute;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);

        // TODO Preset time
        hourOfDay = 7;
        minute = 0;

        TimePicker picker = (TimePicker) view.findViewById(R.id.timePicker2);
        picker.setCurrentHour(hourOfDay);
        picker.setCurrentMinute(minute);
        picker.setIs24HourView(DateFormat.is24HourFormat(getActivity()));
        picker.setOnTimeChangedListener(this);

        // Make the whole TimePicker visible
        ScrollView scrollView = (ScrollView) view.findViewById(R.id.content_frame2);
        LinearLayout.LayoutParams params1 = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        scrollView.setLayoutParams(params1);

        return view;
    }

    @Override
    protected int getContentLayoutId() {
        return R.layout.fragment_wizard_time;
    }

    @Override
    public void onTimeChanged(TimePicker view, int hourOfDay, int minute) {
        this.hourOfDay = hourOfDay;
        this.minute = minute;
    }

    @Override
    public void onSlideDeselected() {
        AlarmDataSource dataSource = new AlarmDataSource(getContext());
        dataSource.open();

        GlobalManager globalManager = new GlobalManager(getContext());

        Defaults defaults = new Defaults();
        defaults.setHour(hourOfDay);
        defaults.setMinute(minute);
        for (int i = 0; i < AlarmDataSource.allDaysOfWeek.length; i++) {
            int dayOfWeek = AlarmDataSource.allDaysOfWeek[i];
            defaults.setDayOfWeek(dayOfWeek);

            com.ibm.icu.util.Calendar c = com.ibm.icu.util.Calendar.getInstance();
            int dayOfWeekType = c.getDayOfWeekType(dayOfWeek);
            boolean isWeekend = dayOfWeekType == com.ibm.icu.util.Calendar.WEEKEND;
            defaults.setState(isWeekend ? Defaults.STATE_DISABLED : Defaults.STATE_ENABLED);

            globalManager.saveAlarmTimeDefault(defaults, dataSource);
        }

        dataSource.close();
    }

    @Override
    protected String getTitle() {
        return getString(R.string.wizard_set_time_title);
    }

    @Override
    protected String getDescriptionTop() {
        return getString(R.string.wizard_set_time_description_top);
    }

    @Override
    protected String getDescriptionBottom() {
        return getString(R.string.wizard_set_time_description_bottom);
    }

    @Override
    public int getDefaultBackgroundColor() {
        return getResources().getColor(R.color.Blue_Grey_600);
    }
}