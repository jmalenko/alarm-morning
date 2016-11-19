package cz.jaro.alarmmorning.holiday;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.preference.PreferenceManager;
import android.util.Log;

import org.joda.time.LocalDate;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cz.jaro.alarmmorning.CalendarFragment;
import cz.jaro.alarmmorning.GlobalManager;
import cz.jaro.alarmmorning.R;
import cz.jaro.alarmmorning.SettingsActivity;
import cz.jaro.alarmmorning.clock.SystemClock;
import de.jollyday.CalendarHierarchy;
import de.jollyday.Holiday;
import de.jollyday.HolidayCalendar;
import de.jollyday.HolidayManager;
import de.jollyday.util.ResourceUtil;

/**
 * Helper class to work with Holidays.
 */
public class HolidayHelper {

    private static final String TAG = HolidayHelper.class.getSimpleName();

    private static HolidayHelper instance;
    private Context context;

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
        return !value.equals(SettingsActivity.PREF_HOLIDAY_NONE) && !value.isEmpty();
    }

    public HolidayCalendar getHolidayCalendar() {
        String holidayPreference = getHolidayPreference();
        return getHolidayCalendar(holidayPreference);
    }

    public HolidayCalendar getHolidayCalendar(String path) {
        String[] ids = path.split("\\.");

        if (useHoliday()) {
            for (HolidayCalendar c : HolidayCalendar.values()) {
                if (c.getId().equals(ids[0])) {
                    return c;
                }
            }
            throw new IllegalStateException("Cannot find HolidayCalendar with id \"" + path + "\"");
        } else {
            return null;
        }
    }

    /**
     * Get list of {@link Holiday}s, from today (including) till the same day next year (excluding), sorted by date.
     *
     * @param path Region path
     * @return list of {@link Holiday}s
     */
    public List<Holiday> listHolidays(String path) {
        HolidayCalendar holidayCalendar = HolidayHelper.getInstance().getHolidayCalendar(path);
        return listHolidays(holidayCalendar, path);
    }

    /**
     * Get list of {@link Holiday}s, from today (including) till the same day next year (excluding), sorted by date.
     *
     * @return list of {@link Holiday}s
     */
    public List<Holiday> listHolidays() {
        HolidayCalendar holidayCalendar = HolidayHelper.getInstance().getHolidayCalendar();
        return listHolidays(holidayCalendar, getHolidayPreference());
    }

    /**
     * Get list of {@link Holiday}s, from today (including) till the same day next year (excluding), sorted by date.
     *
     * @param holidayCalendar HolidayCalendar to use
     * @return list of {@link Holiday}s
     */
    private List<Holiday> listHolidays(HolidayCalendar holidayCalendar, String path) {
        HolidayManager holidayManager = HolidayManager.getInstance(holidayCalendar);

        String[] ids = path.split("\\.");
        String[] subRegions = Arrays.copyOfRange(ids, 1, ids.length);

        // Add all the holidays this year and next year
        Set<Holiday> holidays = new HashSet<>();
        int year = new SystemClock().now().get(Calendar.YEAR);
        for (int y = year; y <= year + 1; y++) {
            Set<Holiday> holidaysY;
            holidaysY = holidayManager.getHolidays(y, subRegions);
            holidays.addAll(holidaysY);
        }

        // Filter: keep only holidays in next year
        GlobalManager globalManager = new GlobalManager(context);
        Calendar from = CalendarFragment.getToday(globalManager.clock());
        Calendar to = (Calendar) from.clone();
        to.add(Calendar.YEAR, 1);

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

    public String preferenceToDisplayName(String path) {
        if (useHoliday(path)) {
            StringBuffer displayName = new StringBuffer();

            String[] ids = path.split("\\.");

            // Country
            ResourceUtil resourceUtil = new ResourceUtil();
            displayName.append(resourceUtil.getCountryDescription(ids[0]));

            // Subregions
            HolidayManager holidayManager = HolidayManager.getInstance(ids[0]);
            CalendarHierarchy calendarHierarchy = holidayManager.getCalendarHierarchy();

            Map<String, CalendarHierarchy> children = calendarHierarchy.getChildren();
            for (int i = 1; i < ids.length; i++) {
                displayName.append(" – ");
                displayName.append(children.get(ids[i]).getDescription());

                children = children.get(ids[i]).getChildren();
            }

            return displayName.toString();
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

    public int pathLength(String path) {
        Log.v(TAG, "pathLength(parentPath=" + path + ")");
        int length;
        if (path.isEmpty()) length = 0;
        else length = path.split("\\.").length;
        Log.v(TAG, "pathLength=" + length);
        return length;
    }

    public String pathPart(String path, int index) {
        Log.d(TAG, "pathPart(parentPath=" + path + ", index=" + index + ")");
        String part = path.split("\\.")[index - 1];
        Log.v(TAG, "pathPart=" + part);
        return part;
    }

    public String pathPrefix(String path, int level) {
        Log.d(TAG, "pathPrefix(parentPath=" + path + ", level=" + level + ")");

        if (level == 0) {
            Log.v(TAG, "pathPrefix=\"\"");
            return "";
        }

        int pos = -1;
        int counter = 0;
        while ((pos = path.indexOf(".", pos + 1)) != -1) {
            counter++;
            if (counter == level) {
                String prefix = path.substring(0, pos);
                Log.v(TAG, "pathPrefix=" + prefix);
                return prefix;
            }
        }

        Log.v(TAG, "pathPrefix=" + path);
        return path;
    }

    public List<Region> list() {
        return list("");
    }

    public boolean hasSubregions(String path) {
        return !list(path).isEmpty();
    }

    public List<Region> list(String path) {
        Log.v(TAG, "List region " + path);
        List<Region> list;

        if (path.equals("") || path.equals(SettingsActivity.PREF_HOLIDAY_NONE)) {
            list = listTopCountries();
        } else {
            list = listSubregions(path);
        }

        // Sort
        Collator coll = Collator.getInstance();
        coll.setStrength(Collator.PRIMARY);

        Collections.sort(list, (lhs, rhs) -> coll.compare(lhs.description, rhs.description));

        return list;
    }

    private List<Region> listTopCountries() {
        List<Region> list = new ArrayList<>();
        ResourceUtil resourceUtil = new ResourceUtil();

        for (HolidayCalendar c : HolidayCalendar.values()) {
            Region region = new Region();
            region.parentPath = "";
            region.id = c.getId();
            region.description = resourceUtil.getCountryDescription(c.getId());

            list.add(region);
        }

        return list;
    }

    private Map<String, CalendarHierarchy> pathInfo(String path) {
        Log.d(TAG, "pathInfo(path=" + path + ")");
        ResourceUtil resourceUtil = new ResourceUtil();

        String[] ids = path.split("\\.");

        Log.v(TAG, " " + ids[0] + " " + resourceUtil.getCountryDescription(ids[0]));

        HolidayManager holidayManager = HolidayManager.getInstance(ids[0]);

        CalendarHierarchy calendarHierarchy = holidayManager.getCalendarHierarchy();
        Map<String, CalendarHierarchy> children = calendarHierarchy.getChildren();

        for (int i = 1; i < ids.length; i++) {
            Log.v(TAG, " " + ids[i] + " " + children.get(ids[i]).getDescription());

            children = children.get(ids[i]).getChildren();
        }

        return children;
    }

    private List<Region> listSubregions(String path) {
        List<Region> list = new ArrayList<>();

        Map<String, CalendarHierarchy> children = pathInfo(path);

        for (Map.Entry<String, CalendarHierarchy> entry : children.entrySet()) {
            String key = entry.getKey();
            CalendarHierarchy value = entry.getValue();

            Log.v(TAG, "  " + key + " " + value.getDescription());

            Region region = new Region();
            region.parentPath = path;
            region.id = key;
            region.description = value.getDescription();

            list.add(region);
        }

        return list;
    }

}

class Region {

    String parentPath;
    String id;
    String description;

    boolean isTop() {
        return parentPath == "";
    }

    String getFullPath() {
        if (isTop()) {
            return id;
        } else {
            return parentPath + "." + id;
        }
    }
}