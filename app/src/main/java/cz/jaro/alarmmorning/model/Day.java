package cz.jaro.alarmmorning.model;

import java.util.Calendar;

import cz.jaro.alarmmorning.Analytics;
import cz.jaro.alarmmorning.clock.Clock;
import cz.jaro.alarmmorning.holiday.HolidayHelper;

import static cz.jaro.alarmmorning.calendar.CalendarUtils.roundDown;

/**
 * Represents the alarm clock setting for a particular date. The default values are inherited from {@link #defaults}, but can be changed (and stored here).
 * All methods are meant "with respect to the particular date".
 * <p/>
 * Terminology: We use a slightly different terminology than standard English.<br>
 * We use "alarm", while English uses "alarm clock".<br>
 * We use "alarm is enabled", while English uses "alarm is on".<br>
 * We use "set alarm at 7:30", while English uses "set alarm for 7:30".
 * We use "alarm rings at 7:30", while English uses "alarm goes off at 7:30".
 * We use "ring the alarm", while English uses "raised the alarm".
 * <p/>
 * The alarm time is a combination of {@link #getDate()}, {@link #getHour()} and {@link#getMinute()}. For convenience, the method {@link #getDateTime()}
 * combines all three items together and returns the alarm time..
 */
public class Day extends AppAlarm {

    /**
     * Value of the {@code state} field indicating the disabled state.
     */
    public static final int STATE_DISABLED = 0;

    /**
     * Value of the {@code state} field indicating the enabled state.
     */
    public static final int STATE_ENABLED = 1;

    /**
     * Value of the {@code state} field indicating the default state.
     */
    public static final int STATE_RULE = 2;

    /**
     * Value of the {@code hour} and {@code minute} fields indicating "value was not set".
     */
    public static final int VALUE_UNSET = -1;

    private long id;

    private Calendar date;

    /**
     * The state of the default.
     * <p/>
     * The states are:<br/>
     * {@link #STATE_DISABLED} = as default<br/>
     * {@link #STATE_ENABLED} = set (to particular time)<br/>
     * {@link #STATE_RULE} = unset (disabled)<br/>
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

    public int getHourDay() {
        return hour;
    }

    public void setHourDay(int hour) {
        this.hour = hour;
    }

    public int getMinuteDay() {
        return minute;
    }

    public void setMinuteDay(int minute) {
        this.minute = minute;
    }

    public Defaults getDefaults() {
        return defaults;
    }

    public void setDefaults(Defaults defaults) {
        this.defaults = defaults;
    }

    /**
     * Check if the alarm time is passed, e.g. if the alarm is enabled and the alarm time was in past (or the alarm is now).
     *
     * @return true if the alarm time is passed
     */
    public boolean isPassed(Clock clock) {
        return isEnabled() && super.isPassed(clock);
    }

    /**
     * Check if the alarm is enabled on this date.
     *
     * @return true if the alarm is enabled
     */
    public boolean isEnabled() {
        return state == STATE_ENABLED ||
                (state == STATE_RULE && !isHoliday() && defaults.isEnabled());
    }

    /**
     * Return the hour of the alarm.
     * <p/>
     * If the alarm is enabled, then use this method to get the hour of alarm time.
     *
     * @return hour of the alarm (that should be used if the alarm is enabled)
     */
    public int getHour() {
        if (!isValid() || state == STATE_RULE)
            return defaults.getHour();
        else
            return hour;
    }

    public void setHour(int hour) {
        setHourDay(hour);
    }

    /**
     * Return the hour of the alarm.
     * <p/>
     * If the alarm is enabled, then use this method to get the hour of alarm time.
     *
     * @return minute of the alarm
     */
    public int getMinute() {
        if (!isValid() || state == STATE_RULE)
            return defaults.getMinute();
        else
            return minute;
    }

    public void setMinute(int minute) {
        setMinuteDay(minute);
    }

    /**
     * Checks whether the data for this object are valid.
     *
     * @return true if the object was populated by data from a database. If false then use the data from {@code defaults}.
     */
    private boolean isValid() {
        return hour != VALUE_UNSET;
    }

    /**
     * Return the alarm time.
     * <p/>
     * If the alarm is enabled, then use this method to get the alarm time.
     *
     * @return alarm time
     */
    public Calendar getDateTime() {
        Calendar alarmTime = (Calendar) date.clone();

        alarmTime.set(Calendar.HOUR_OF_DAY, getHour());
        alarmTime.set(Calendar.MINUTE, getMinute());
        roundDown(alarmTime, Calendar.SECOND);

        return alarmTime;
    }

    /**
     * Switches the alarm:<br>
     * If it's enabled, then it's set to disabled.<br>
     * If it's disabled, then it's set to enabled.<br>
     * If it's set to default and the default is enabled, then it's set to disabled.<br>
     * If it's set to default and the default is disabled, then it's set to enabled.
     */
    public void reverse() {
        switch (getState()) {
            case STATE_RULE:
                setState(defaults.isEnabled() ? STATE_DISABLED : STATE_ENABLED);
                break;

            case STATE_ENABLED:
                setState(STATE_DISABLED);
                break;

            case STATE_DISABLED:
                setState(STATE_ENABLED);
                break;
        }
    }

    /**
     * Check if the alarm is changed on this date.
     * <p/>
     * Note that only the (enabled/disabled) state and time (hour and minute) is compared with default.
     * <p/>
     * Technically, if<br>
     * 1. the default is enabled and
     * 2. the day state uses the time from default (the state is {@link Day#STATE_RULE}) and<br>
     * 3. the day is changed to disabled and<br>
     * 4. back to enabled (to {@link Day#STATE_ENABLED}),<br>
     * then this method returns true. On technical level, it could be argued that the alarm is changed since the state is {@link Day#STATE_ENABLED} and not
     * {@link Day#STATE_RULE}.
     *
     * @return false if the alarm time on this date is changed (compared to default)
     */
    public boolean sameAsDefault() {
        return (state == STATE_ENABLED && getDefaults().getState() == Defaults.STATE_ENABLED && hour == defaults.getHour() && minute == defaults.getMinute()) ||
                (state == STATE_DISABLED && getDefaults().getState() == Defaults.STATE_DISABLED) ||
                state == STATE_RULE;
    }

    public boolean isHoliday() {
        HolidayHelper holidayHelper = HolidayHelper.getInstance();
        return holidayHelper.isHoliday(getDate());
    }

    public String getHolidayDescription() {
        HolidayHelper holidayHelper = HolidayHelper.getInstance();
        return holidayHelper.getHolidayDescription(getDate());
    }

    @Override
    public String toString() {
        StringBuffer str = new StringBuffer();

        str.append(Analytics.calendarToDate(date));
        str.append(" ");
        str.append(Analytics.dayStateToString(state));

        switch (state) {
            case STATE_DISABLED:
                if (isHoliday()) {
                    str.append(": holiday ");
                    str.append(getHolidayDescription());
                }
                break;
            case STATE_ENABLED:
                str.append(" ");
                str.append(Analytics.calendarToTime(getDateTime()));
                break;
            case STATE_RULE:
                str.append(": ");
                str.append(defaults.toString());
                break;
        }

        return str.toString();
    }
}
