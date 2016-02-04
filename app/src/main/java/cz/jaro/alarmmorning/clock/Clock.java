package cz.jaro.alarmmorning.clock;

import java.util.Calendar;

/**
 * Interface for getting current clock (date and time).
 * <p/>
 * This interface exists to support unit testing.
 */
public interface Clock {

    /**
     * @return current clock (date and time)
     */
    Calendar now();

}
