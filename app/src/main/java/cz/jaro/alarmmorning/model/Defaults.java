package cz.jaro.alarmmorning.model;

/**
 * Created by jmalenko on 16.12.2015.
 */
public class Defaults {

    private long id;

    /**
     * 0 = unset
     * 1 = set (to particular time)
     */
    private int state;

    /**
     * The dey of week is represented by the same number as in Calendar.
     *
     * @see java.util.Calendar
     */
    private int dayOfWeek;

    private int hours;

    private int minutes;

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

    public boolean isEnabled() {
        return state == AlarmDataSource.DEFAULT_STATE_SET;
    }
}
