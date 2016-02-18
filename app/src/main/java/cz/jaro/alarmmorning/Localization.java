package cz.jaro.alarmmorning;

import android.content.Context;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

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
        // TODO Remove dependency on clock
        Calendar date = clock.now();
        date.set(Calendar.DAY_OF_WEEK, dayOfWeek);
        SimpleDateFormat sdf = new SimpleDateFormat("E");
        sdf.setCalendar(date);
        return sdf.format(date.getTime());
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
