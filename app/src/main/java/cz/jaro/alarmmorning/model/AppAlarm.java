package cz.jaro.alarmmorning.model;

import java.util.Calendar;

import androidx.annotation.NonNull;
import cz.jaro.alarmmorning.GlobalManager;
import cz.jaro.alarmmorning.clock.Clock;

import static cz.jaro.alarmmorning.calendar.CalendarUtils.onTheSameDate;

/**
 * Represents an alarm time.
 */
public abstract class AppAlarm implements Comparable {

    /**
     * Return the date and time of the alarm.
     *
     * @return alarm date and time
     */
    abstract public Calendar getDateTime();

    /**
     * Return the date of the alarm.
     *
     * @return alarm date
     */
    abstract public Calendar getDate();

    /**
     * Set the date of the alarm.
     *
     * @param date alarm date
     */
    abstract public void setDate(Calendar date);

    /**
     * Return the hour of the alarm.
     *
     * @return alarm hour
     */
    abstract public int getHour();

    /**
     * Set the hour of the alarm.
     *
     * @param hour alarm hour
     */
    abstract public void setHour(int hour);

    /**
     * Return the minute of the alarm.
     *
     * @return alarm minute
     */
    abstract public int getMinute();

    /**
     * Set the minute of the alarm.
     *
     * @param minute alarm minute
     */
    abstract public void setMinute(int minute);

    public abstract String getPersistenceId();

    /**
     * Check if the alarm time is passed, e.g. if the alarm time was in past (or the alarm is now).
     * <p>
     * Note: the part "or the alarm is now" of definition is defined for ease of testing: in tests, on the alarm time, we intend to check that the
     * system alarm for next alarm is registered. In reality (with real time) there would be a slight delay between the alarm time and setting the next
     * system alarm (which calls this method).
     *
     * @return true if the alarm time is passed
     */
    public boolean isPassed(Clock clock) {
        Calendar now = clock.now();
        return !getDateTime().after(now);
    }

    /**
     * Checks if this is the next alarm. Specifically: there is no other alarm between now and alarm time.
     *
     * @param clock clock
     * @return true if this is the next alarm
     */
    public boolean isNextAlarm(Clock clock) {
        Calendar alarmTime1 = getDateTime();

        GlobalManager globalManager = GlobalManager.getInstance();
        Calendar alarmTime2 = globalManager.getNextAlarm(clock);

        return alarmTime1.equals(alarmTime2);
    }

    /**
     * Returns the time to alarm time.
     *
     * @return milliseconds between now and the alarm time. The negative value means that alarm time was in past.
     */
    public long getTimeToRing(Clock clock) {
        Calendar alarmTime1 = getDateTime();
        Calendar now = clock.now();

        return alarmTime1.getTimeInMillis() - now.getTimeInMillis();
    }

    /**
     * Compares the alarm times of two alarms.
     * <p>
     * Note: the alarm states (enabled/disabled) are ignored.
     *
     * @param o AppAlarm to compare with
     * @return -1 if this alarm time is before the alarm time of o, 0 if the alarm time are at the same time, 1 otherwise.
     */
    @Override
    public int compareTo(@NonNull Object o) {
        if (!(o instanceof AppAlarm))
            throw new ClassCastException("Cannot compare ApPAlarm with non-AppAlarm");

        AppAlarm o1 = this;
        AppAlarm o2 = (AppAlarm) o;

        Calendar c1 = o1.getDateTime();
        Calendar c2 = o2.getDateTime();

        // On a particular date, Day should be first
        if (onTheSameDate(c1, c2) && (o1 instanceof Day || o2 instanceof Day)) {
            return o1 instanceof Day ? -1 : 1;
        }

        // Natural ordering
        return c1.before(c2)
                ? -1
                : c2.before(c1) ? 1 : 0;
    }
}