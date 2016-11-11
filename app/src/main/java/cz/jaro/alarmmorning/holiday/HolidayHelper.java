package cz.jaro.alarmmorning.holiday;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import cz.jaro.alarmmorning.SettingsActivity;
import de.jollyday.HolidayCalendar;

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
        return getHolidayPreference(context) != SettingsActivity.PREF_HOLIDAY_NONE;
    }

    static public HolidayCalendar getHolidayCalendar(Context context) {
        if (useHoliday(context)) {
            String holidayPreference = getHolidayPreference(context);
            for (HolidayCalendar c : HolidayCalendar.values()) {
                if (c.getId().equals(holidayPreference)) {
                    return c;
                }
            }
            throw new IllegalStateException("Cannot find HolidayCalendar givent it's id");
        } else {
            return null;
        }
    }
}