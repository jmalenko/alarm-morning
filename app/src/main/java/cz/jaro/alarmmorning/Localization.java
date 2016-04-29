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

import cz.jaro.alarmmorning.clock.Clock;
import cz.jaro.alarmmorning.clock.SystemClock;

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
        Clock clock = new SystemClock(); // TODO Solve dependency on clock
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
        Clock clock = new SystemClock(); // TODO Solve dependency on clock
        Calendar date = clock.now();
        date.set(Calendar.DAY_OF_WEEK, dayOfWeek);
        SimpleDateFormat sdf = new SimpleDateFormat("EEEE");
        sdf.setCalendar(date);
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
        String title;

        int index = daysOfWeek.size();

        if (index == 0) {
            title = "";
        } else {
            title = Localization.dayOfWeekToString(res, daysOfWeek.get(--index));

            if (0 < index) {
                title = Localization.dayOfWeekToString(res, daysOfWeek.get(--index))
                        + res.getString(R.string.list_separator_last)
                        + title;
            }

            while (0 < index) {
                title = Localization.dayOfWeekToString(res, daysOfWeek.get(--index))
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
        String currentValue = getValue(context.getResources(), R.string.time_format);
        if (currentValue != null) {
            return timeToStringFormat(date, currentValue);
        } else {
            return timeToStringAlgo(date, context);
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
     * @param date    time moment
     * @param context context
     * @return the time as string
     */
    private static String timeToStringAlgo(Date date, Context context) {
        java.text.DateFormat dateFormat = android.text.format.DateFormat.getTimeFormat(context);
        return dateFormat.format(date);
    }

    /**
     * Converts a date to text in <i>very</i> short format.
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
     *
     * @param date date
     * @return the date as string
     */
    private static String dateToStringVeryShortAlgo(Date date) {
        SimpleDateFormat dateFormat = (SimpleDateFormat) java.text.DateFormat.getDateInstance(java.text.DateFormat.SHORT);
        // Trick: use regex to trim off all y's and any non-alphabetic characters before and after
        dateFormat.applyPattern(dateFormat.toPattern().replaceAll("[^\\p{Alpha}]*y+[^\\p{Alpha}]*", ""));
        return dateFormat.format(date);
    }

    /**
     * Converts a date to text in full format.
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
     *
     * @param date date
     * @return the date as string
     */
    private static String dateToStringFullAlgo(Date date) {
        SimpleDateFormat dateFormat = (SimpleDateFormat) java.text.DateFormat.getDateInstance(DateFormat.FULL);
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

        String dateTimesText = "";

        for (int index = 0; index < dateTimes.size(); index++) {
            if (0 < index) {
                if (index < dateTimes.size())
                    dateTimesText = dateTimesText + resources.getString(R.string.list_separator);
                else
                    dateTimesText = dateTimesText + resources.getString(R.string.list_separator_last);
            }
            Calendar dateTime = dateTimes.get(index);
            dateTimesText = dateTimesText + Localization.dateTimeToString(dateTime, context);
        }

        return dateTimesText;
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

        boolean changed = defaultValue.equals(currentValue);
        return changed ? null : currentValue;
    }

}
