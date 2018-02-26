package cz.jaro.alarmmorning;

/**
 * Represents the difference between two time points as days+hours+minutes+seconds.
 */
public class TimeDifference {
    long days;
    long hours;
    long minutes;
    long seconds;
    boolean negative;

    /**
     * Constructs the time difference.
     *
     * @param diff Time difference in milliseconds.
     */
    public TimeDifference(long diff) {
        split(diff);
    }

    void split(long diff) {
        negative = diff < 0;

        long remaining = negative ? -diff : diff;
        long length;

        length = 24 * 60 * 60 * 1000;
        days = remaining / length;
        remaining = remaining % length;

        length = 60 * 60 * 1000;
        hours = remaining / length;
        remaining = remaining % length;

        length = 60 * 1000;
        minutes = remaining / length;
        remaining = remaining % length;

        length = 1000;
        seconds = remaining / length;
    }

    public long getDays() {
        return days;
    }

    public long getHours() {
        return hours;
    }

    public long getMinutes() {
        return minutes;
    }

    public long getSeconds() {
        return seconds;
    }

    public boolean isNegative() {
        return negative;
    }

}
