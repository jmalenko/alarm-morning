package cz.jaro.alarmmorning.model;

import java.util.Calendar;
import java.util.GregorianCalendar;

import cz.jaro.alarmmorning.Localization;

/**
 * Created by jmalenko on 18.12.2015.
 */
public class Day {

    private long id;

    GregorianCalendar date;

    /**
     * 0 = as default
     * 1 = set (to particular time)
     * 2 = unset (disabled)
     */
    private int state;

    private int hours;

    private int minutes;

    private Defaults defaults;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public GregorianCalendar getDate() {
        return date;
    }

    public void setDate(GregorianCalendar date) {
        this.date = date;
    }

    public int getState() {
        return state;
    }

    public void setState(int state) {
        this.state = state;
    }

    public int getHours() {
        return hours;
    }

    public void setHours(int hours) {
        this.hours = hours;
    }

    public int getMinutes() {
        return minutes;
    }

    public void setMinutes(int minutes) {
        this.minutes = minutes;
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

    private boolean isEnabled() {
        return state == AlarmDataSource.DAY_STATE_SET ||
                (state == AlarmDataSource.DAY_STATE_DEFAULT && defaults.isEnabled());
    }

    private int getHoursX() {
        if (hours == AlarmDataSource.VALUE_UNSET)
            return defaults.getHours();
        else
            return hours;
    }

    private int getMinutesX() {
        if (minutes == AlarmDataSource.VALUE_UNSET)
            return defaults.getMinutes();
        else
            return minutes;
    }

    private Calendar getDateTime() {
        Calendar alarm = (Calendar) date.clone();

        alarm.set(Calendar.HOUR, getHoursX());
        alarm.set(Calendar.MINUTE, getMinutesX());

        return alarm;
    }

}
