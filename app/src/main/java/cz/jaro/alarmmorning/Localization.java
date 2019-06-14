package cz.jaro.alarmmorning;

import android.content.Context;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.util.DisplayMetrics;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import cz.jaro.alarmmorning.calendar.CalendarUtils;

/**
 * This class contains all the localization related features.
 */
public class Localization {

    /**
     * Converts the day of week identifier to abbreviated string.
     *
     * @param dayOfWeek identifier of the day of week. Use identifiers from {@code Calendar} class, like {@link Calendar#SUNDAY}.
     * @return the short name of the day of week
     */
    public static String dayOfWeekToStringShort(Resources resources, int dayOfWeek) {
        int resId = dayOfWeekToShortResourceId(dayOfWeek);
        String currentValue = getValue(resources, resId);
        if (currentValue != null) {
            return currentValue;
        } else {
            return dayOfWeekToShortStringAlgo(dayOfWeek);
        }
    }

    public static int dayOfWeekToShortResourceId(int dayOfWeek) {
        switch (dayOfWeek) {
            case Calendar.SUNDAY:
                return R.string.sunday_short;
            case Calendar.MONDAY:
                return R.string.monday_short;
            case Calendar.TUESDAY:
                return R.string.tuesday_short;
            case Calendar.WEDNESDAY:
                return R.string.wednesday_short;
            case Calendar.THURSDAY:
                return R.string.thursday_short;
            case Calendar.FRIDAY:
                return R.string.friday_short;
            case Calendar.SATURDAY:
                return R.string.saturday_short;
            default:
                throw new IllegalArgumentException("Unexpected argument " + dayOfWeek);
        }
    }

    /**
     * Converts the day of week identifier to abbreviated string. Uses an algorithm to derive the result from the system locale.
     *
     * @param dayOfWeek identifier of the day of week. Use identifiers from {@code Calendar} class, like {@link Calendar#SUNDAY}.
     * @return the short name of the day of week
     */
    private static String dayOfWeekToShortStringAlgo(int dayOfWeek) {
        Calendar date = Calendar.getInstance();
        date.set(Calendar.DAY_OF_WEEK, dayOfWeek);

        /*
        Example of SimpleDateFormat(pattern) parameter effects:
            Pattern parameter       en_US locale            cs_CZ locale
            --------------------------------------------------------------
            no parameter            2/25/17 1:30 PM         24.02.17 13:30
            "E"                     Mon                     po
            "EEEE"                  Monday                  pondělí
        */
        SimpleDateFormat sdf = new SimpleDateFormat("E");
        return sdf.format(date.getTime());
    }

    /**
     * Converts the day of week identifier to string.
     *
     * @param dayOfWeek identifier of the day of week. Use identifiers from {@code Calendar} class, like {@link Calendar#SUNDAY}.
     * @return the name of the day of week
     */
    public static String dayOfWeekToString(Resources resources, int dayOfWeek) {
        int resId = dayOfWeekToResourceId(dayOfWeek);
        String currentValue = getValue(resources, resId);
        if (currentValue != null) {
            return currentValue;
        } else {
            return dayOfWeekToStringAlgo(dayOfWeek);
        }
    }

    public static int dayOfWeekToResourceId(int dayOfWeek) {
        switch (dayOfWeek) {
            case Calendar.SUNDAY:
                return R.string.sunday;
            case Calendar.MONDAY:
                return R.string.monday;
            case Calendar.TUESDAY:
                return R.string.tuesday;
            case Calendar.WEDNESDAY:
                return R.string.wednesday;
            case Calendar.THURSDAY:
                return R.string.thursday;
            case Calendar.FRIDAY:
                return R.string.friday;
            case Calendar.SATURDAY:
                return R.string.saturday;
            default:
                throw new IllegalArgumentException("Unexpected argument " + dayOfWeek);
        }
    }

    /**
     * Converts the day of week identifier to string. Uses an algorithm to derive the result from the system locale.
     *
     * @param dayOfWeek identifier of the day of week. Use identifiers from {@code Calendar} class, like {@link Calendar#SUNDAY}.
     * @return the name of the day of week
     */
    private static String dayOfWeekToStringAlgo(int dayOfWeek) {
        Calendar date = Calendar.getInstance();
        date.set(Calendar.DAY_OF_WEEK, dayOfWeek);
        SimpleDateFormat sdf = new SimpleDateFormat("EEEE");
        return sdf.format(date.getTime());
    }

    /**
     * Converts the list of days of week identifiers to string.
     *
     * @param daysOfWeek list of day of week identifiers
     * @param res        resources
     * @return string with the names of days. The can be simply inserted into a sentence.
     */
    public static String daysOfWeekToString(List<Integer> daysOfWeek, Resources res) {
        StringBuilder title;

        int index = daysOfWeek.size();

        if (index == 0) {
            title = new StringBuilder();
        } else {
            title = new StringBuilder(Localization.dayOfWeekToString(res, daysOfWeek.get(--index)));

            if (0 < index) {
                title.insert(0, Localization.dayOfWeekToString(res, daysOfWeek.get(--index))
                        + res.getString(R.string.list_separator_last));
            }

            while (0 < index) {
                title.insert(0, Localization.dayOfWeekToString(res, daysOfWeek.get(--index))
                        + res.getString(R.string.list_separator));
            }
        }
        return title.toString();
    }

    /**
     * Converts the time (in a day) to text.
     *
     * @param hours   hour
     * @param minutes minute
     * @param context context
     * @return the time as string
     */
    public static String timeToString(int hours, int minutes, Context context) {
        Calendar cal = CalendarUtils.newGregorianCalendar();
        cal.set(Calendar.HOUR_OF_DAY, hours);
        cal.set(Calendar.MINUTE, minutes);
        return timeToString(cal.getTime(), context);
    }

    /**
     * Converts the time (in a day) to text.
     *
     * @param date    time moment
     * @param context context
     * @return the time as string
     */
    public static String timeToString(Date date, Context context) {
        String currentValue = getValue(context.getResources(), R.string.time_format);
        if (currentValue != null) {
            return timeToStringFormat(date, currentValue);
        } else {
            return timeToStringAlgo(date);
        }
    }

    /**
     * Converts the time to text by using the specified format.
     *
     * @param date   time moment
     * @param format time format
     * @return the time as string
     */
    private static String timeToStringFormat(Date date, String format) {
        SimpleDateFormat sdf = new SimpleDateFormat(format);
        return sdf.format(date);
    }

    /**
     * Converts the time (in a day) to text. Uses an algorithm to derive the result from the system locale.
     *
     * @param date time moment
     * @return the time as string
     */
    private static String timeToStringAlgo(Date date) {
        /*
        Example of DateFormat.getTimeInstance(style) parameter effects:
            Style parameter         en_US locale                                        cs_CZ locale
            -----------------------------------------------------------------------------------------------------------------
            FULL                    7:00:00 AM Central European Standard Time           7:00:00 Středoevropský standardní čas
            LONG                    7:00:00 AM CET                                      7:00:00 SEČ
            MEDIUM)                 7:00:00 AM                                          7:00:00
            SHORT                   7:00 AM                                             7:00
        */

        /*
        // TODO Test on Galaxy Samsung S7: There is bug in Android that makes the running apps incorrectly thing it is in 12-hour mode after changing time, although the system is set to use 24-hour mode. The fix is to kill (by swiping in the list of running apps) and start the app. We don't hack the app to handle this situation.
        This was fixed on Nov 22, 2016 in Android. When the minSdkVersion passes this date, this comment (and error) can be removed
        Source: https://code.google.com/p/android/issues/detail?id=181201#c28
        */

        DateFormat dateFormat = DateFormat.getTimeInstance(DateFormat.SHORT);
        return dateFormat.format(date);
    }

    /**
     * Converts a date to text in <i>very</i> short format.
     * <p>
     * Sample: 11/21 (for November 21 2016).
     *
     * @param resources resources
     * @param date      date
     * @return the date as string
     */
    public static String dateToStringVeryShort(Resources resources, Date date) {
        String currentValue = getValue(resources, R.string.date_format_very_short);
        if (currentValue != null) {
            return timeToStringFormat(date, currentValue);
        } else {
            return dateToStringVeryShortAlgo(date);
        }
    }

    /**
     * Converts a date to text in <i>very</i> short format. Uses an algorithm to derive the result from the system locale.
     * <p>
     * Contains month and day of month only.
     *
     * @param date date
     * @return the date as string
     */
    private static String dateToStringVeryShortAlgo(Date date) {
        /*
        Example of DateFormat.getDateInstance(style) parameter effects:
            Style parameter         en_US locale                    cs_CZ locale
            ------------------------------------------------------------------------------
            FULL                    Monday, February 1, 2016        pondělí 1. února, 2016
            LONG                    February 1, 2016                1. února, 2016
            MEDIUM)                 Feb 1, 2016                     1. 2. 2016
            SHORT                   2/1/16                          01.02.16
        */
        SimpleDateFormat dateFormat = (SimpleDateFormat) DateFormat.getDateInstance(DateFormat.SHORT);

        // Trick: use regex to trim off all y's and any non-alphabetic characters before and after
        dateFormat.applyPattern(dateFormat.toPattern().replaceAll("[^\\p{Alpha}]*y+[^\\p{Alpha}]*", ""));

        return dateFormat.format(date);
    }

    /**
     * Converts a date to text in full format.
     * <p>
     * Sample: Monday, November 21 2016.
     *
     * @param resources resources
     * @param date      date
     * @return the date as string
     */
    public static String dateToStringFull(Resources resources, Date date) {
        String currentValue = getValue(resources, R.string.date_format_full);
        if (currentValue != null) {
            return timeToStringFormat(date, currentValue);
        } else {
            return dateToStringFullAlgo(date);
        }
    }

    /**
     * Converts a date to text in full format. Uses an algorithm to derive the result from the system locale.
     * <p>
     * Contains month and day of month, optionally contains day of week. Does not contain the year.
     *
     * @param date date
     * @return the date as string
     */
    private static String dateToStringFullAlgo(Date date) {
        SimpleDateFormat dateFormat = (SimpleDateFormat) DateFormat.getDateInstance(DateFormat.FULL);

        // Trick: use regex to trim off all y's and any non-alphabetic characters before and after
        dateFormat.applyPattern(dateFormat.toPattern().replaceAll("[^\\p{Alpha}]*y+[^\\p{Alpha}]*", ""));

        return dateFormat.format(date);
    }

    /**
     * Converts the date time (date and time) to text.
     *
     * @param dateTime date with time
     * @param context  context
     * @return the time as string
     */
    public static String dateTimeToString(Calendar dateTime, Context context) {
        Resources resources = context.getResources();

        String dateText = Localization.dateToStringVeryShort(resources, dateTime.getTime());
        String timeText = Localization.timeToString(dateTime.getTime(), context);
        String dateTimeText = resources.getString(R.string.datetime_format, timeText, dateText);

        return dateTimeText;
    }

    /**
     * Converts the list of datetimes to string.
     *
     * @param dateTimes list of datetimes
     * @param context   context
     * @return string with the datetimes. The can be simply inserted into a sentence.
     */
    public static String dateTimesToString(List<Calendar> dateTimes, Context context) {
        Resources resources = context.getResources();

        StringBuilder dateTimesText = new StringBuilder();

        for (int index = 0; index < dateTimes.size(); index++) {
            if (0 < index) {
                dateTimesText.append(index < dateTimes.size() - 1 ?
                        resources.getString(R.string.list_separator) :
                        resources.getString(R.string.list_separator_last));
            }
            Calendar dateTime = dateTimes.get(index);
            dateTimesText.append(Localization.dateTimeToString(dateTime, context));
        }

        return dateTimesText.toString();
    }

    /**
     * Gets the value from resources. Considers both the default (English) locale and the current locale.
     *
     * @param resources resources
     * @param resId     resource id
     * @return the value from current locale if the value in the current locale is different from the value in in the default locale; otherwise returns null.
     */
    private static String getValue(Resources resources, int resId) {
        AssetManager assets = resources.getAssets();
        DisplayMetrics metrics = resources.getDisplayMetrics();
        Configuration config = new Configuration(resources.getConfiguration());
        config.locale = Locale.US;
        new Resources(assets, metrics, config); // sets the resources in resources

        String defaultValue = resources.getString(resId);

        config.locale = Locale.getDefault();
        new Resources(assets, metrics, config);

        String currentValue = resources.getString(resId);

        boolean same = defaultValue.equals(currentValue);
        return same ? null : currentValue;
    }

}
