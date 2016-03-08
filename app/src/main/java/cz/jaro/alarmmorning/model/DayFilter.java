package cz.jaro.alarmmorning.model;

/**
 * Filters a Day.
 */
public interface DayFilter {

    /**
     * Check whether a Day matches a filter of not.
     *
     * @param day Day
     * @return true if day matches the filer
     */
    boolean match(Day day);

}
