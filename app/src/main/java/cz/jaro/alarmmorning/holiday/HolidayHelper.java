package cz.jaro.alarmmorning.holiday;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.preference.PreferenceManager;

import cz.jaro.alarmmorning.R;
import cz.jaro.alarmmorning.SettingsActivity;
import de.jollyday.HolidayCalendar;
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
}