package cz.jaro.alarmmorning.model;

import android.content.Context;

import java.util.Calendar;
import java.util.GregorianCalendar;

/**
 * Created by jmalenko on 18.12.2015.
 */
public class Day {

    private long id;

    Calendar date;

    /**
     * 0 = as default
     * 1 = set (to particular time)
     * 2 = unset (disabled)
     */
    private int state;

    private int hour;

    private int minute;

    private Defaults defaults;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public Calendar getDate() {
        return date;
    }

    public void setDate(Calendar date) {
        this.date = date;
    }

    public int getState() {
        return state;
    }

    public void setState(int state) {
        this.state = state;
    }

    public int getHour() {
        return hour;
    }

    public void setHour(int hour) {
        this.hour = hour;
    }

    public int getMinute() {
        return minute;
    }

    public void setMinute(int minute) {
        this.minute = minute;
    }

    public Defaults getDefaults() {
        return defaults;
    }

    public void setDefaults(Defaults defaults) {
        this.defaults = defaults;
    }

    public boolean isPassed() {
        Calendar now = new GregorianCalendar();
        if (isEnabled()) {
            if (getDateTime().before(now)) {
                return true;
            }
        }
        return false;
    }

    public boolean isEnabled() {
        return state == AlarmDataSource.DAY_STATE_ENABLED ||
                (state == AlarmDataSource.DAY_STATE_DEFAULT && defaults.isEnabled());
    }

    public int getHourX() {
        if (hour == AlarmDataSource.VALUE_UNSET || state == AlarmDataSource.DAY_STATE_DEFAULT)
            return defaults.getHour();
        else
            return hour;
    }

    public int getMinuteX() {
        if (minute == AlarmDataSource.VALUE_UNSET || state == AlarmDataSource.DAY_STATE_DEFAULT)
            return defaults.getMinute();
        else
            return minute;
    }

    public Calendar getDateTime() {
        Calendar alarmTime = (Calendar) date.clone();

        alarmTime.set(Calendar.HOUR_OF_DAY, getHourX());
        alarmTime.set(Calendar.MINUTE, getMinuteX());
        alarmTime.set(Calendar.SECOND, 0);
        alarmTime.set(Calendar.MILLISECOND, 0);

        return alarmTime;
    }

    public void reverse() {
        switch (getState()) {
            case AlarmDataSource.DAY_STATE_DEFAULT:
                setState(defaults.isEnabled() ? AlarmDataSource.DAY_STATE_DISABLED : AlarmDataSource.DAY_STATE_ENABLED);
                break;

            case AlarmDataSource.DAY_STATE_ENABLED:
                setState(AlarmDataSource.DAY_STATE_DISABLED);
                break;

            case AlarmDataSource.DAY_STATE_DISABLED:
                setState(AlarmDataSource.DAY_STATE_ENABLED);
                break;
        }
    }

    public boolean isNextAlarm(Context context) {
        Calendar alarmTime1 = getDateTime();

        Calendar alarmTime2 = AlarmDataSource.getNextAlarm(context);

        return alarmTime1.equals(alarmTime2);
    }

    public long getTimeToRing() {
        Calendar alarmTime1 = getDateTime();
        Calendar now = new GregorianCalendar();

        return alarmTime1.getTimeInMillis() - now.getTimeInMillis();
    }
}
