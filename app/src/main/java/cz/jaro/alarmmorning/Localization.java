package cz.jaro.alarmmorning;

import android.content.Context;
import android.content.res.Resources;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import cz.jaro.alarmmorning.clock.Clock;

/**
 * This class contains all the localization related features.
 */
public class Localization {

    /**
     * Converts the day of week identifier to string.
     *
     * @param dayOfWeek identifier of the day of week. Use identifiers from {@code Calendar} class, like {@link Calendar#SUNDAY}.
     * @param clock     clock
     * @return the name of the day of week
     */
    public static String dayOfWeekToString(int dayOfWeek, Clock clock) {
        // TODO Localization - rename to ...Short (usein calendar) and create ...Long (use in sentende, dialogue to change other days with the same alarm time
        // TODO Remove dependency on clock

        Calendar date = clock.now();
        date.set(Calendar.DAY_OF_WEEK, dayOfWeek);
        SimpleDateFormat sdf = new SimpleDateFormat("E");
        sdf.setCalendar(date);
        return sdf.format(date.getTime());
    }

    /**
     * Converts the day of week identifier to string.
     *
     * @param dayOfWeek identifier of the day of week. Use identifiers from {@code Calendar} class, like {@link Calendar#SUNDAY}.
     * @param res       resources
     * @return the name of the day of week
     */
    public static String dayOfWeekToString2(int dayOfWeek, Resources res) {
        // TODO Localization - consider using values in LOCALE - day names, month names, formats of calendar days...
        switch (dayOfWeek) {
            case Calendar.SUNDAY:
                return res.getString(R.string.sunday_short);
            case Calendar.MONDAY:
                return res.getString(R.string.monday_short);
            case Calendar.TUESDAY:
                return res.getString(R.string.tuesday_short);
            case Calendar.WEDNESDAY:
                return res.getString(R.string.wednesday_short);
            case Calendar.THURSDAY:
                return res.getString(R.string.thursday_short);
            case Calendar.FRIDAY:
                return res.getString(R.string.friday_short);
            case Calendar.SATURDAY:
                return res.getString(R.string.saturday_short);
            default:
                throw new IllegalArgumentException("Unexpected argument " + dayOfWeek);
        }
    }

    /**
     * Converts the list of days of week identifiers to string.
     *
     * @param daysOfWeek list of day of week identifiers
     * @param res        resources
     * @param clock      clock
     * @return string with the names of days. The can be simly inserted into a sentence.
     */
    public static String daysOfWeekToString(List<Integer> daysOfWeek, Resources res, Clock clock) {
        String title;

        int index = daysOfWeek.size();

        if (index == 0) {
            title = "";
        } else {
            title = Localization.dayOfWeekToString(daysOfWeek.get(--index), clock);

            if (0 < index) {
                title = Localization.dayOfWeekToString(daysOfWeek.get(--index), clock)
                        + res.getString(R.string.list_separator_last)
                        + title;
            }

            while (0 < index) {
                title = Localization.dayOfWeekToString(daysOfWeek.get(--index), clock)
                        + res.getString(R.string.list_separator)
                        + title;
            }
        }
        return title;
    }

    /**
     * Converts the time (in a day) to text.
     *
     * @param hours   hour
     * @param minutes minute
     * @param context context
     * @param clock   clock
     * @return the time as string
     */
    public static String timeToString(int hours, int minutes, Context context, Clock clock) {
        Calendar date = clock.now();
        date.set(Calendar.HOUR_OF_DAY, hours);
        date.set(Calendar.MINUTE, minutes);
        return timeToString(date.getTime(), context);
    }

    /**
     * Converts the time (in a day) to text.
     *
     * @param date    time moment
     * @param context context
     * @return the time as string
     */
    public static String timeToString(Date date, Context context) {
        java.text.DateFormat dateFormat = android.text.format.DateFormat.getTimeFormat(context);
        return dateFormat.format(date);
    }

    /**
     * Converts a date to text in <i>very</i> short format.
     *
     * @param date date
     * @return the date as string
     */
    public static String dateToStringVeryShort(Date date) {
        SimpleDateFormat dateFormat = (SimpleDateFormat) java.text.DateFormat.getDateInstance(java.text.DateFormat.SHORT);
        // Trick: use regex to trim off all y's and any non-alphabetic characters before and after
        dateFormat.applyPattern(dateFormat.toPattern().replaceAll("[^\\p{Alpha}]*y+[^\\p{Alpha}]*", ""));
        return dateFormat.format(date);
    }

    /**
     * Converts a date to text in full format.
     *
     * @param date date
     * @return the date as string
     */
    public static String dateToStringFull(Date date) {
        SimpleDateFormat dateFormat = (SimpleDateFormat) java.text.DateFormat.getDateInstance(DateFormat.FULL);
        return dateFormat.format(date);
    }

}
