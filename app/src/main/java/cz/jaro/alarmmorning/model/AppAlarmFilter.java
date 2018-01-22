package cz.jaro.alarmmorning.model;

/**
 * Filters an {@link AppAlarm}.
 */
public interface AppAlarmFilter {

    /**
     * Check whether a Day matches a filter of not.
     *
     * @param appAlarm Alarm
     * @return true if the appAlarm matches the filer
     */
    boolean match(AppAlarm appAlarm);

}
