package cz.jaro.alarmmorning.calendar;

/**
 * Abstract class that supports filtering of {@link CalendarEvent}'s.
 */
public abstract class CalendarEventFilter {

    /**
     * Decide whether an {@code event} satisfies a condition. The condition is implemented in the body of this method.
     *
     * @param event
     * @return true if the {@code event} matches match
     */
    public abstract boolean match(CalendarEvent event);

}
