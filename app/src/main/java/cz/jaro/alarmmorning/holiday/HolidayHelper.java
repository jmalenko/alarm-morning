package cz.jaro.alarmmorning.holiday;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.preference.PreferenceManager;

import org.joda.time.LocalDate;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import cz.jaro.alarmmorning.CalendarFragment;
import cz.jaro.alarmmorning.GlobalManager;
import cz.jaro.alarmmorning.R;
import cz.jaro.alarmmorning.SettingsActivity;
import cz.jaro.alarmmorning.clock.SystemClock;
import de.jollyday.Holiday;
import de.jollyday.HolidayCalendar;
import de.jollyday.HolidayManager;
import de.jollyday.util.ResourceUtil;

/**
 * Helper class to work with Holidays.
 */
public class HolidayHelper {

    static HolidayHelper instance;
    Context context;

    HolidayHelper() {
        context = findContext();
    }

    public static HolidayHelper getInstance() {
        if (instance == null) {
            instance = new HolidayHelper();
        }
        return instance;
    }

    public String getHolidayPreference() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        String holidayPreference = preferences.getString(SettingsActivity.PREF_HOLIDAY, SettingsActivity.PREF_HOLIDAY_DEFAULT);
        return holidayPreference;
    }

    public boolean useHoliday() {
        return useHoliday(getHolidayPreference());
    }

    public boolean useHoliday(String value) {
        return !value.equals(SettingsActivity.PREF_HOLIDAY_NONE);
    }

    public HolidayCalendar getHolidayCalendar() {
        if (useHoliday()) {
            String holidayPreference = getHolidayPreference();
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

    /**
     * Get list of {@link HolidayCalendar}s.
     *
     * @return list of {@link HolidayCalendar}s, sorted alphabetically with respect to current Locale
     */
    public List<HolidayCalendar> listHolidayCalendars() {
        List<HolidayCalendar> holidayCalendars = new ArrayList<HolidayCalendar>();
        for (HolidayCalendar c : HolidayCalendar.values()) {
            holidayCalendars.add(c);
        }

        // Sort (with respect to Localization)
        ResourceUtil resourceUtil = new ResourceUtil();

        Collator coll = Collator.getInstance();
        coll.setStrength(Collator.PRIMARY);

        Collections.sort(holidayCalendars, new Comparator<HolidayCalendar>() {
            @Override
            public int compare(HolidayCalendar lhs, HolidayCalendar rhs) {
                String lshCountry = resourceUtil.getCountryDescription(lhs.getId()).intern();
                String rshCountry = resourceUtil.getCountryDescription(rhs.getId()).intern();
                return coll.compare(lshCountry, rshCountry);
            }
        });

        return holidayCalendars;
    }

    /**
     * Get list of {@link Holiday}s, from today (including) till the same day next year (excluding), sorted by date.
     *
     * @return list of {@link Holiday}s
     */
    public List<Holiday> listHolidays() {
        HolidayCalendar holidayCalendar = HolidayHelper.getInstance().getHolidayCalendar();
        HolidayManager holidayManager = HolidayManager.getInstance(holidayCalendar);

        // Add all the holidays this year and next year
        int year = new SystemClock().now().get(Calendar.YEAR);

        Set<Holiday> holidays = holidayManager.getHolidays(year);
        Set<Holiday> holidaysY1 = holidayManager.getHolidays(year + 1);
        holidays.addAll(holidaysY1);

        GlobalManager globalManager = new GlobalManager(context);
        Calendar from = CalendarFragment.getToday(globalManager.clock());
        Calendar to = (Calendar) from.clone();
        to.add(Calendar.YEAR, 1);

        // Filter: keep only holidays in next year
        Iterator<Holiday> it = holidays.iterator();
        while (it.hasNext()) {
            Holiday h = it.next();

            if (h.getDate().toDate().before(from.getTime()) || !h.getDate().toDate().before(to.getTime())) {
                it.remove();
            }
        }

        // Sort
        List<Holiday> orderedHolidays = new ArrayList<Holiday>(holidays);
        Collections.sort(orderedHolidays, new Comparator<Holiday>() {
            @Override
            public int compare(Holiday lhs, Holiday rhs) {
                return lhs.getDate().compareTo(rhs.getDate());
            }
        });

        return orderedHolidays;
    }

    public String preferenceToDisplayName(String holidayPreference) {
        if (useHoliday(holidayPreference)) {
            ResourceUtil resourceUtil = new ResourceUtil();
            for (HolidayCalendar c : HolidayCalendar.values()) {
                if (c.getId().equals(holidayPreference)) {
                    return resourceUtil.getCountryDescription(c.getId());
                }
            }
            throw new IllegalStateException("Cannot find HolidayCalendar with id " + holidayPreference);
        } else {
            Resources res = context.getResources();
            return res.getString(R.string.holidays_none);
        }
    }

    public String getHolidayDescription(Calendar date) {
        if (useHoliday()) {
            HolidayCalendar holidayCalendar = HolidayHelper.getInstance().getHolidayCalendar();
            HolidayManager holidayManager = HolidayManager.getInstance(holidayCalendar);

            int year = date.get(Calendar.YEAR);
            Set<Holiday> holidays = holidayManager.getHolidays(year);
            for (Holiday h : holidays) {
                if (compareDates(date, h.getDate())) {
                    return h.getDescription();
                }
            }
            throw new IllegalStateException("Cannot find holiday for date " + date);
        } else {
            return null;
        }
    }

    private boolean compareDates(Calendar date1, LocalDate date2) {
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

    public boolean isHoliday(Calendar date) {
        if (useHoliday()) {
            HolidayCalendar holidayCalendar = HolidayHelper.getInstance().getHolidayCalendar();
            HolidayManager holidayManager = HolidayManager.getInstance(holidayCalendar);

            return holidayManager.isHoliday(date);
        } else {
            return false;
        }
    }

    /**
     * This s hack that allows getting a {@link Context} such that it's not passed as a method argument. The <code>Context</code> is needed to read holiday
     * settings from {@link SharedPreferences} and clock from {@link GlobalManager}.
     *
     * @return Context
     */
    private Context findContext() {
        try {
            Application application = (Application) Class.forName("android.app.ActivityThread")
                    .getMethod("currentApplication").invoke(null, (Object[]) null);
            Context context = application.getApplicationContext();
            return context;
        } catch (Exception e) {
            throw new IllegalStateException("Cannot get context", e);
        }
    }
}