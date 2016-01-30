package cz.jaro.alarmmorning.model;

/**
 * Represents the alarm clock setting for a particular weekday.
 */
public class Defaults {

    private long id;

    /**
     * 0 = unset
     * 1 = set (to particular time)
     */
    private int state;

    /**
     * The dey of week is represented by the same number as in {@link java.util.Calendar}. Specifically, the identifier of Monday is {@link java.util.Calendar#MONDAY}
     */
    private int dayOfWeek;

    private int hour;

    private int minute;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public int getDayOfWeek() {
        return dayOfWeek;
    }

    public void setDayOfWeek(int dayOfWeek) {
        this.dayOfWeek = dayOfWeek;
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

    /**
     * Check if the alarm is enabled on this weekday.
     *
     * @return true if alarm is enabled
     */
    public boolean isEnabled() {
        return state == AlarmDataSource.DEFAULT_STATE_ENABLED;
    }

    /**
     * Switches the alarm: if it's enabled, then it's set to disabled. And vice versa.
     */
    public void reverse() {
        if (isEnabled()) {
            setState(AlarmDataSource.DEFAULT_STATE_DISABLED);
        } else {
            setState(AlarmDataSource.DEFAULT_STATE_ENABLED);
        }
    }
}
