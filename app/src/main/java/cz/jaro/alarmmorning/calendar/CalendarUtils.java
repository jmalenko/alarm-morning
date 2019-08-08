package cz.jaro.alarmmorning.calendar;

import java.text.DateFormatSymbols;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Locale;

/**
 * Helper methods for {@link Calendar} objects.
 */
public class CalendarUtils {

    private static Calendar add(int field, Calendar date, int amount) {
        date.add(field, amount);
        return date;
    }

    /**
     * Add <code>amount</code> of years to <code>date</code>.
     *
     * @param date   Date.
     * @param amount Amount of years.
     * @return The date object (with added years).
     */
    public static Calendar addYears(Calendar date, int amount) {
        add(Calendar.YEAR, date, amount);
        return date;
    }

    /**
     * Returns new {@link Calendar} object with date that is obtained by adding <code>amount</code> of years to <code>date</code>.
     *
     * @param source Date.
     * @param amount Amount of years.
     * @return New date object.
     */
    public static Calendar addYearsClone(Calendar source, int amount) {
        Calendar date = (Calendar) source.clone();
        return addYears(date, amount);
    }

    /**
     * Add one year to <code>date</code>.
     *
     * @param date Date.
     * @return The date object (with added year).
     */
    public static Calendar addYear(Calendar date) {
        return addYears(date, 1);
    }

    /**
     * Add <code>amount</code> of months to <code>date</code>.
     *
     * @param date   Date.
     * @param amount Amount of months.
     * @return The date object (with added months).
     */
    public static Calendar addMonths(Calendar date, int amount) {
        add(Calendar.MONTH, date, amount);
        return date;
    }

    /**
     * Returns new {@link Calendar} object with date that is obtained by adding <code>amount</code> of months to <code>date</code>.
     *
     * @param source Date.
     * @param amount Amount of months.
     * @return New date object.
     */
    public static Calendar addMonthsClone(Calendar source, int amount) {
        Calendar date = (Calendar) source.clone();
        return addMonths(date, amount);
    }

    /**
     * Add one month to <code>date</code>.
     *
     * @param date Date.
     * @return The date object (with added month).
     */
    public static Calendar addMonth(Calendar date) {
        return addMonths(date, 1);
    }

    /**
     * Add <code>amount</code> of days to <code>date</code>.
     *
     * @param date   Date.
     * @param amount Amount of days.
     * @return The date object (with added days).
     */
    public static Calendar addDays(Calendar date, int amount) {
        add(Calendar.DATE, date, amount);
        return date;
    }

    /**
     * Returns new {@link Calendar} object with date that is obtained by adding <code>amount</code> of days to <code>date</code>.
     *
     * @param source Date.
     * @param amount Amount of days.
     * @return New date object.
     */
    public static Calendar addDaysClone(Calendar source, int amount) {
        Calendar date = (Calendar) source.clone();
        return addDays(date, amount);
    }

    /**
     * Add one day to <code>date</code>.
     *
     * @param date Date.
     * @return The date object (with added day).
     */
    public static Calendar addDay(Calendar date) {
        return addDays(date, 1);
    }

    /**
     * Add <code>amount</code> of hours to <code>date</code>.
     *
     * @param date   Date.
     * @param amount Amount of hours.
     * @return The date object (with added hours).
     */
    public static Calendar addHours(Calendar date, int amount) {
        add(Calendar.HOUR_OF_DAY, date, amount);
        return date;
    }

    /**
     * Returns new {@link Calendar} object with date that is obtained by adding <code>amount</code> of hours to <code>date</code>.
     *
     * @param source Date.
     * @param amount Amount of hours.
     * @return New date object.
     */
    public static Calendar addHoursClone(Calendar source, int amount) {
        Calendar date = (Calendar) source.clone();
        return addHours(date, amount);
    }

    /**
     * Add one hour to <code>date</code>.
     *
     * @param date Date.
     * @return The date object (with added hour).
     */
    public static Calendar addHour(Calendar date) {
        return addHours(date, 1);
    }

    /**
     * Add <code>amount</code> of minutes to <code>date</code>.
     *
     * @param date   Date.
     * @param amount Amount of minutes.
     * @return The date object (with added minutes).
     */
    public static Calendar addMinutes(Calendar date, int amount) {
        add(Calendar.MINUTE, date, amount);
        return date;
    }

    /**
     * Returns new {@link Calendar} object with date that is obtained by adding <code>amount</code> of minutes to <code>date</code>.
     *
     * @param source Date.
     * @param amount Amount of minutes.
     * @return New date object.
     */
    public static Calendar addMinutesClone(Calendar source, int amount) {
        Calendar date = (Calendar) source.clone();
        return addMinutes(date, amount);
    }

    /**
     * Add one minute to <code>date</code>.
     *
     * @param date Date.
     * @return The date object (with added minute).
     */
    public static Calendar addMinute(Calendar date) {
        return addMinutes(date, 1);
    }

    /**
     * Add <code>amount</code> of seconds to <code>date</code>.
     *
     * @param date   Date.
     * @param amount Amount of seconds.
     * @return The date object (with added seconds).
     */
    public static Calendar addSeconds(Calendar date, int amount) {
        add(Calendar.SECOND, date, amount);
        return date;
    }

    /**
     * Returns new {@link Calendar} object with date that is obtained by adding <code>amount</code> of seconds to <code>date</code>.
     *
     * @param source Date.
     * @param amount Amount of seconds.
     * @return New date object.
     */
    public static Calendar addSecondsClone(Calendar source, int amount) {
        Calendar date = (Calendar) source.clone();
        return addSeconds(date, amount);
    }

    /**
     * Add one second to <code>date</code>.
     *
     * @param date Date.
     * @return The date object (with added second).
     */
    public static Calendar addSecond(Calendar date) {
        return addSeconds(date, 1);
    }

    /**
     * Add <code>amount</code> of milliseconds to <code>date</code>.
     *
     * @param date   Date.
     * @param amount Amount of milliseconds.
     * @return The date object (with added milliseconds).
     */
    public static Calendar addMilliSeconds(Calendar date, int amount) {
        add(Calendar.MILLISECOND, date, amount);
        return date;
    }

    /**
     * Returns new {@link Calendar} object with date that is obtained by adding <code>amount</code> of milliseconds to <code>date</code>.
     *
     * @param source Date.
     * @param amount Amount of milliseconds.
     * @return New date object.
     */
    public static Calendar addMilliSecondsClone(Calendar source, int amount) {
        Calendar date = (Calendar) source.clone();
        return addMilliSeconds(date, amount);
    }

    /**
     * Add one millisecond to <code>date</code>.
     *
     * @param date Date.
     * @return The date object (with added millisecond).
     */
    public static Calendar addMilliSecond(Calendar date) {
        return addMilliSeconds(date, 1);
    }

    /**
     * Returns new {@link Calendar} object with date that is the first day, starting with <code>date</code>, such that it's day of week equals
     * <code>dayOfWeek</code>.
     *
     * @param date      Date to start with
     * @param dayOfWeek Day of Week
     * @return Date of the next day
     */
    public static Calendar nextSameDayOfWeek(Calendar date, int dayOfWeek) {
        Calendar nextSameDayOfWeek = (Calendar) date.clone();
        while (nextSameDayOfWeek.get(Calendar.DAY_OF_WEEK) != dayOfWeek)
            nextSameDayOfWeek.add(Calendar.DATE, 1);
        return nextSameDayOfWeek;
    }

    /**
     * Returns the short name of a <code>dayOfWeek</code> in US English.
     *
     * @param dayOfWeek Day of week.
     * @return Name of the day of week.
     */
    static public String dayOfWeekToString(int dayOfWeek) {
        DateFormatSymbols symbols = new DateFormatSymbols(Locale.US);
        String[] dayNames = symbols.getShortWeekdays();
        return dayNames[dayOfWeek];
    }

    /**
     * Returns new {@link Calendar} object with date and time that is the midnight at the beginning of the day with <code>now</code>.
     *
     * @param now Date.
     * @return Date and time of the midnight at the beginning of <code>now</code>.
     */
    public static Calendar beginningOfToday(Calendar now) {
        Calendar beginningOfToday = (Calendar) now.clone();

        roundDown(beginningOfToday, Calendar.HOUR_OF_DAY);

        return beginningOfToday;
    }

    /**
     * Returns new {@link Calendar} object with date and time that is the last millisecond of the day with <code>now</code>.
     *
     * @param now Date.
     * @return Date and time of the the last millisecond of day with <code>now</code>.
     */
    public static Calendar endOfToday(Calendar now) {
        Calendar endOfToday = (Calendar) now.clone();

        endOfToday.add(Calendar.DATE, 1);
        roundDown(endOfToday, Calendar.HOUR_OF_DAY);
        endOfToday.add(Calendar.MILLISECOND, -1);

        return endOfToday;
    }

    /**
     * Returns new {@link Calendar} object with date and time that is the midnight at the beginning of the day after the day with <code>now</code>. (Alternative
     * definition: returns the midnight at the end of the day with <code>now</code>.)
     *
     * @param now Date.
     * @return Date and time of the midnight at the beginning of <code>now</code>.
     */
    public static Calendar beginningOfTomorrow(Calendar now) {
        Calendar beginningOfTomorrow = (Calendar) now.clone();

        beginningOfTomorrow.add(Calendar.DATE, 1);
        roundDown(beginningOfTomorrow, Calendar.HOUR_OF_DAY);

        return beginningOfTomorrow;
    }

    /**
     * Returns new {@link Calendar} object with date and time that is the last millisecond before the noon on tomorrow.
     *
     * @param now Date.
     * @return Date and time of the the last millisecond before noon on the day with <code>now</code>.
     */
    public static Calendar justBeforeNoonTomorrow(Calendar now) {
        Calendar justBeforeNoonTomorrow = beginningOfTomorrow(now);

        justBeforeNoonTomorrow.set(Calendar.HOUR_OF_DAY, 12);
        addMilliSeconds(justBeforeNoonTomorrow, -1);

        return justBeforeNoonTomorrow;
    }

    /**
     * Returns new {@link Calendar} object with date and time that is the last millisecond before the noon today.
     *
     * @param now Date.
     * @return Date and time of the the last millisecond before noon on the day with <code>now</code>.
     */
    public static Calendar justBeforeNoonToday(Calendar now) {
        Calendar justBeforeNoonToday = beginningOfToday(now);

        justBeforeNoonToday.set(Calendar.HOUR_OF_DAY, 12);
        addMilliSeconds(justBeforeNoonToday, -1);

        return justBeforeNoonToday;
    }

    /**
     * Zeroes all the fields, starting with <code>field</code> (including <code>field</code>). The field {@link Calendar#DATE} is set to one (instead of zero).
     * <p>
     * E.g. for field parameter {@link Calendar#DATE} zeroes all the following: {@link Calendar#HOUR_OF_DAY}, {@link Calendar#MINUTE}, {@link Calendar#SECOND},
     * {@link Calendar#MILLISECOND}.
     *
     * @param date  Date.
     * @param field Field (same as in {@link Calendar}).
     * @return The date object (with zeroed fields).
     */
    public static Calendar roundDown(Calendar date, int field) {
        switch (field) {
            case Calendar.YEAR:
                date.set(Calendar.YEAR, 0);
            case Calendar.MONTH:
                date.set(Calendar.MONTH, 0);
            case Calendar.WEEK_OF_YEAR:
                date.set(Calendar.WEEK_OF_YEAR, 0);
            case Calendar.DATE:
                date.set(Calendar.DATE, 1);
            case Calendar.HOUR_OF_DAY:
                date.set(Calendar.HOUR_OF_DAY, 0);
            case Calendar.MINUTE:
                date.set(Calendar.MINUTE, 0);
            case Calendar.SECOND:
                date.set(Calendar.SECOND, 0);
            case Calendar.MILLISECOND:
                date.set(Calendar.MILLISECOND, 0);
        }
        return date;
    }

    /**
     * Returns the field that is just smaller than a specified <code>field</code>.
     * <p>
     * E.g. for field parameter {@link Calendar#DATE} returns {@link Calendar#HOUR_OF_DAY}.
     *
     * @param field Field (same as in {@link Calendar}).
     * @return The field that is just smaller the parameter <code>field</code>.
     */
    public static int subField(int field) {
        switch (field) {
            case Calendar.YEAR:
                return Calendar.MONTH;
            case Calendar.MONTH:
                return Calendar.WEEK_OF_YEAR;
            case Calendar.WEEK_OF_YEAR:
                return Calendar.DATE;
            case Calendar.DATE:
                return Calendar.HOUR_OF_DAY;
            case Calendar.HOUR_OF_DAY:
                return Calendar.MINUTE;
            case Calendar.MINUTE:
                return Calendar.SECOND;
            case Calendar.SECOND:
                return Calendar.MILLISECOND;
            default:
                throw new IllegalArgumentException("Unsupported field " + field);

        }
    }

    /**
     * Check if two calendars (date and time) are on the same day.
     *
     * @param cal1 Calendar 1.
     * @param cal2 Calendar 2.
     * @return True if both calendars are on the same day.
     */
    public static boolean onTheSameDate(Calendar cal1, Calendar cal2) {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR);
    }

    /**
     * Check if two calendars (date and time) are on the same day, hour and minute.
     *
     * @param cal1 Calendar 1.
     * @param cal2 Calendar 2.
     * @return True if both calendars are on the same day, hour and minute.
     */
    public static boolean onTheSameMinute(Calendar cal1, Calendar cal2) {
        return onTheSameDate(cal1, cal2) &&
                cal1.get(Calendar.HOUR_OF_DAY) == cal2.get(Calendar.HOUR_OF_DAY) &&
                cal1.get(Calendar.MINUTE) == cal2.get(Calendar.MINUTE);
    }

    /**
     * Check if a calendar (date and time) occurs tomorrow.
     *
     * @param now Current time. The tomorrow date is obtained from this parameter by adding one day.
     * @param cal Calendar to check.
     * @return True if <code>cal</code> occurs tomorrow.
     */
    public static boolean inTomorrow(Calendar now, Calendar cal) {
        Calendar date = (Calendar) now.clone();
        date.add(Calendar.DATE, 1);
        return onTheSameDate(date, cal);
    }

    /**
     * Check if a calendar (date and time) occurs next week (today or the next 6 days).
     *
     * @param now Current time.
     * @param cal Calendar to check.
     * @return True if <code>cal</code> occurs tomorrow.
     */
    public static boolean inNextWeek(Calendar now, Calendar cal) {
        Calendar date = (Calendar) now.clone();
        for (int i = 0; i < 6; i++) {
            date.add(Calendar.DATE, 1);
            if (onTheSameDate(date, cal)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Creates a calendar from milliseconds (since epoch).
     *
     * @param milliseconds milliseconds since epoch
     * @return Gregorian calendar
     */
    public static GregorianCalendar newGregorianCalendar(long milliseconds) {
        GregorianCalendar calendar = new GregorianCalendar();
        calendar.setTimeInMillis(milliseconds);
        return calendar;
    }

    /**
     * Creates a calendar representing today (the beginning midnight).
     *
     * @return Gregorian calendar
     */
    public static Calendar newGregorianCalendar() {
        return beginningOfToday(new GregorianCalendar());
    }

    /**
     * Copies all fields from source calendar to target calendar.
     * <p>
     * This method is useful for setting all the fields of the target calendar in a different time zone such that all the fields should be the same as in the
     * source calendar.
     *
     * @param source source calendar
     * @param target target calendar
     */
    public static void copyAllFields(Calendar source, Calendar target) {
        target.set(Calendar.YEAR, source.get(Calendar.YEAR));
        target.set(Calendar.MONTH, source.get(Calendar.MONTH));
        target.set(Calendar.DATE, source.get(Calendar.DATE));
        target.set(Calendar.HOUR_OF_DAY, source.get(Calendar.HOUR_OF_DAY));
        target.set(Calendar.MINUTE, source.get(Calendar.MINUTE));
        target.set(Calendar.SECOND, source.get(Calendar.SECOND));
        target.set(Calendar.MILLISECOND, source.get(Calendar.MILLISECOND));
    }
}
