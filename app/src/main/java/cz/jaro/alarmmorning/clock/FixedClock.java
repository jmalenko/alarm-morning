package cz.jaro.alarmmorning.clock;

import java.util.Calendar;

/**
 * Represents a fixed clock (date and time).
 * <p/>
 * The value of fixed clock is set initially set. Then, each call of {@link #now()} returns this fixed time.
 * <p/>
 * The clock may be shifted by {@link #addMinute(int)}.
 * <p/>
 * This class is used in unit tests of methods considering time.
 */
public class FixedClock implements Clock {

    private Calendar clock;

    /**
     * @param clock the value to which the clock is set
     */
    public FixedClock(Calendar clock) {
        this.clock = clock;
    }

    public FixedClock setClock(Calendar clock) {
        this.clock = clock;
        return this;
    }

    /**
     * @return fixed clock (date and time)
     */
    @Override
    public Calendar now() {
        return (Calendar) clock.clone();
    }

    /**
     * Shifts clock forward by {@code minutes} minutes.
     *
     * @param minutes number of minutes added do the clock. May be negative.
     */
    public FixedClock addMinute(int minutes) {
        clock.add(Calendar.MINUTE, minutes);
        return this;
    }

}
