package cz.jaro.alarmmorning.wizard;

import android.app.AlarmManager;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TimePicker;

import com.ibm.icu.util.Calendar;

import java.util.ArrayList;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.List;

import androidx.annotation.Nullable;
import cz.jaro.alarmmorning.Analytics;
import cz.jaro.alarmmorning.GlobalManager;
import cz.jaro.alarmmorning.R;
import cz.jaro.alarmmorning.SettingsActivity;
import cz.jaro.alarmmorning.calendar.CalendarEvent;
import cz.jaro.alarmmorning.calendar.CalendarEventFilter;
import cz.jaro.alarmmorning.calendar.CalendarHelper;
import cz.jaro.alarmmorning.calendar.CalendarUtils;
import cz.jaro.alarmmorning.clock.Clock;
import cz.jaro.alarmmorning.graphics.RelativeTimePreference;
import cz.jaro.alarmmorning.model.AlarmDataSource;
import cz.jaro.alarmmorning.model.AlarmDbHelper;
import cz.jaro.alarmmorning.model.Defaults;

import static cz.jaro.alarmmorning.calendar.CalendarUtils.addDaysClone;
import static cz.jaro.alarmmorning.calendar.CalendarUtils.beginningOfToday;
import static cz.jaro.alarmmorning.calendar.CalendarUtils.endOfToday;

/**
 * Rules:
 * <p/>
 * 1. Use the median alarm time of enabled workdays (of defaults).
 * <p/>
 * 2. If there is no enabled alarm on a workday, then use the system alarm clock time.
 * <p/>
 * 3. If system alarm clock time is not defined, use calendar: find all the first non-all-day meetings in past 30 days, take median and subtract 1 hour.
 * <p/>
 * 4. If there are no such meetings, use 7:00 am.
 * <p/>
 * Alternatives to concider: Get all alarms saved in the alarm application.
 */
public class SetTimeSlide extends BaseFragment implements TimePicker.OnTimeChangedListener {

    private static final String TAG = GlobalManager.createLogTag(SetTimeSlide.class);

    private int hourOfDay;
    private int minute;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);

        presetTime();

        TimePicker picker = view.findViewById(R.id.timePicker2);
        picker.setCurrentHour(hourOfDay);
        picker.setCurrentMinute(minute);
        picker.setIs24HourView(DateFormat.is24HourFormat(getActivity()));
        picker.setOnTimeChangedListener(this);

        // Make the whole TimePicker visible
        int screenSize = getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK;
        if (screenSize == Configuration.SCREENLAYOUT_SIZE_NORMAL) {
            ScrollView scrollView = view.findViewById(R.id.content_frame2);
            LinearLayout.LayoutParams params1 = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            scrollView.setLayoutParams(params1);
        }

        return view;
    }

    /**
     * When run from Settings, the workdays may have different alarm times. We take median time and use it.
     */
    private void presetTime() {
        if (Wizard.loadWizardFinished()) {
            boolean set;

            set = presetTimeFromDefaults();
            if (!set) {
                set = presetTimeFromSystemAlarmClock();
                if (!set) {
                    set = presetTimeFromCalendar();
                    if (!set) {
                        presetTimeFromConstants();
                    }
                }
            }
        } else {
            presetTimeFromConstants();
        }
        Log.v(TAG, "Preset time " + hourOfDay + ":" + minute);
    }

    private boolean presetTimeFromDefaults() {
        GlobalManager globalManager = GlobalManager.getInstance();

        // Use the median alarm time of enabled workdays (of defaults)
        List<Integer> times = new ArrayList<>();

        for (int i = 0; i < AlarmDataSource.allDaysOfWeek.length; i++) {
            int dayOfWeek = AlarmDataSource.allDaysOfWeek[i];

            Defaults defaults = globalManager.loadDefault(dayOfWeek);

            if (defaults.getState() == Defaults.STATE_DISABLED)
                continue;

            Calendar c = Calendar.getInstance();
            int dayOfWeekType = c.getDayOfWeekType(dayOfWeek);
            boolean isWeekend = dayOfWeekType == Calendar.WEEKEND;

            if (isWeekend)
                continue;

            int time = RelativeTimePreference.hourAndMinuteToValue(defaults.getHour(), defaults.getMinute());
            times.add(time);
        }

        return setTimeFromMedian(times);
    }

    private boolean setTimeFromMedian(List<Integer> times) {
        if (times.size() == 0)
            return false;

        int time = median(times);

        hourOfDay = RelativeTimePreference.valueToHour(time);
        minute = RelativeTimePreference.valueToMinute(time);

        return true;
    }

    private int median(List<Integer> times) {
        log("All", times);

        // Sort
        Collections.sort(times);

        log("Sorted", times);

        // Take median
        int medianIndex = times.size() / 2;

        return times.get(medianIndex);
    }

    private void log(String message, List<Integer> times) {
        StringBuilder str = new StringBuilder();
        for (int time : times) {
            if (0 < str.length())
                str.append(", ");

            int hour = RelativeTimePreference.valueToHour(time);
            int minute = RelativeTimePreference.valueToMinute(time);
            str.append(hour).append(":").append(minute);
        }
        Log.d(TAG, message + ": " + str);
    }

    private boolean presetTimeFromSystemAlarmClock() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            AlarmManager alarmManager = (AlarmManager) getContext().getSystemService(Context.ALARM_SERVICE);
            AlarmManager.AlarmClockInfo nextAlarmClock = alarmManager.getNextAlarmClock();

            if (nextAlarmClock == null)
                return false;

            long alarmTimeInMillis = nextAlarmClock.getTriggerTime();

            GregorianCalendar alarmTime = CalendarUtils.newGregorianCalendar(alarmTimeInMillis);

            hourOfDay = alarmTime.get(java.util.Calendar.HOUR_OF_DAY);
            minute = alarmTime.get(java.util.Calendar.MINUTE);

            return true;
        } else {
            return false;
        }
    }

    private boolean presetTimeFromCalendar() {
        List<Integer> times = new ArrayList<>();

        // Dor each day in the past 30 days
        for (int day = 1; day <= 30; day++) {
            // Find first calendar event
            Context context = getContext();
            GlobalManager globalManager = GlobalManager.getInstance();
            Clock clock = globalManager.clock();

            java.util.Calendar dayIn = addDaysClone(clock.now(), -day);
            java.util.Calendar dayStart = beginningOfToday(dayIn);
            java.util.Calendar dayEnd = endOfToday(dayIn);

            CalendarHelper calendarHelper = new CalendarHelper(context);
            CalendarEventFilter notAllDay = new CalendarEventFilter() {
                @Override
                public boolean match(CalendarEvent event) {
                    return !event.getAllDay();
                }
            };
            CalendarEvent event = calendarHelper.find(dayStart, dayEnd, notAllDay);

            if (event != null) {
                java.util.Calendar targetAlarmTime = (java.util.Calendar) event.getBegin().clone();
                targetAlarmTime.add(java.util.Calendar.MINUTE, -SettingsActivity.PREF_CHECK_ALARM_TIME_GAP_DEFAULT);

                if (!targetAlarmTime.before(dayStart)) {
                    int time = RelativeTimePreference.hourAndMinuteToValue(targetAlarmTime.get(java.util.Calendar.HOUR_OF_DAY), targetAlarmTime.get(java.util.Calendar.MINUTE));
                    times.add(time);
                } else {
                    int time = RelativeTimePreference.hourAndMinuteToValue(0, 0);
                    times.add(time);
                }
            }
        }

        return setTimeFromMedian(times);
    }

    private void presetTimeFromConstants() {
        hourOfDay = AlarmDbHelper.DEFAULT_ALARM_HOUR;
        minute = AlarmDbHelper.DEFAULT_ALARM_MINUTE;
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
        GlobalManager globalManager = GlobalManager.getInstance();

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

            Analytics analytics = new Analytics(Analytics.Channel.Activity, Analytics.ChannelName.Wizard);

            globalManager.modifyDefault(defaults, analytics);
        }
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