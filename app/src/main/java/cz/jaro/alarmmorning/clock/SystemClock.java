package cz.jaro.alarmmorning.clock;

import java.util.Calendar;

/**
 * Represents current system time. Each call of {@link #now()} returns the (non-decreasing) system time.
 */
public class SystemClock implements Clock {

    /**
     * @return current system clock (date and time)
     */
    @Override
    public Calendar now() {
        return Calendar.getInstance();
    }

}
