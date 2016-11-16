package cz.jaro.alarmmorning.holiday;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.preference.PreferenceManager;

import org.joda.time.LocalDate;

import java.util.Calendar;
import java.util.Set;

import cz.jaro.alarmmorning.R;
import cz.jaro.alarmmorning.SettingsActivity;
import de.jollyday.Holiday;
import de.jollyday.HolidayCalendar;
import de.jollyday.HolidayManager;
import de.jollyday.util.ResourceUtil;

/**
 * Helper class to work with Holidays.
 */
public class HolidayHelper {

    static public String getHolidayPreference(Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        String holidayPreference = preferences.getString(SettingsActivity.PREF_HOLIDAY, SettingsActivity.PREF_HOLIDAY_DEFAULT);
        return holidayPreference;
    }

    static public boolean useHoliday(Context context) {
        return useHoliday(getHolidayPreference(context));
    }

    static public boolean useHoliday(String value) {
        return !value.equals(SettingsActivity.PREF_HOLIDAY_NONE);
    }

    static public HolidayCalendar getHolidayCalendar(Context context) {
        if (useHoliday(context)) {
            String holidayPreference = getHolidayPreference(context);
            for (HolidayCalendar c : HolidayCalendar.values()) {
                if (c.getId().equals(holidayPreference)) {
                    return c;
                }
            }
            throw new IllegalStateException("Cannot find HolidayCalendar with id " + holidayPreference);
        } else {
            return null;
        }
    }

    static public String preferenceToDisplayName(Context context, String holidayPreference) {
        if (useHoliday(holidayPreference)) {
            for (HolidayCalendar c : HolidayCalendar.values()) {
                if (c.getId().equals(holidayPreference)) {
                    ResourceUtil resourceUtil = new ResourceUtil();
                    return resourceUtil.getCountryDescription(c.getId());
                }
            }
            throw new IllegalStateException("Cannot find HolidayCalendar with id " + holidayPreference);
        } else {
            Resources res = context.getResources();
            return res.getString(R.string.holidays_none);
        }
    }

    public static String getHolidayName(HolidayManager holidayManager, Calendar date) {
        int year = date.get(Calendar.YEAR);
        Set<Holiday> holidays = holidayManager.getHolidays(year);
        for (Holiday h : holidays) {
            if (compareDates(date, h.getDate())) {
                return h.getDescription();
            }
        }
        throw new IllegalStateException("Cannot find holiday for date " + date);
    }

    private static boolean compareDates(Calendar date1, LocalDate date2) {
        int year1 = date1.get(Calendar.YEAR);
        int month1 = date1.get(Calendar.MONTH) + 1;
        int day1 = date1.get(Calendar.DAY_OF_MONTH);

        int year2 = date2.getYear();
        int month2 = date2.getMonthOfYear();
        int day2 = date2.getDayOfMonth();

        return year1 == year2
                && month1 == month2
                && day1 == day2;
    }

}