package cz.jaro.alarmmorning.holiday;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.util.Log;

import org.joda.time.LocalDate;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cz.jaro.alarmmorning.GlobalManager;
import cz.jaro.alarmmorning.R;
import cz.jaro.alarmmorning.SettingsActivity;
import de.jollyday.CalendarHierarchy;
import de.jollyday.Holiday;
import de.jollyday.HolidayCalendar;
import de.jollyday.HolidayManager;
import de.jollyday.util.ResourceUtil;

import static cz.jaro.alarmmorning.calendar.CalendarUtils.beginningOfToday;
import static cz.jaro.alarmmorning.holiday.HolidayHelper.PATH_TOP;

/**
 * Helper class to work with Holidays.
 */
public class HolidayHelper {

    private static final String TAG = HolidayHelper.class.getSimpleName();

    public static final String PATH_TOP = "";
    public static final String PATH_SEPARATOR = ".";

    public static final String STRING_PATH_SEPARATOR = " – ";

    private static HolidayHelper instance;
    private Context context;

    private HolidayHelper() {
        context = findContext();
    }

    public static HolidayHelper getInstance() {
        if (instance == null) {
            instance = new HolidayHelper();
        }
        return instance;
    }

    /**
     * Check whether holidays are enabaled by user.
     *
     * @return true if the user enabled holidays
     */
    public boolean useHoliday() {
        GlobalManager globalManager = GlobalManager.getInstance();
        return useHoliday(globalManager.loadHoliday());
    }

    public boolean useHoliday(String value) {
        return !value.equals(SettingsActivity.PREF_HOLIDAY_NONE);
    }

    private HolidayCalendar getHolidayCalendar() {
        GlobalManager globalManager = GlobalManager.getInstance();
        String holidayPreference = globalManager.loadHoliday();
        return getHolidayCalendar(holidayPreference);
    }

    private HolidayCalendar getHolidayCalendar(String path) {
        if (useHoliday(path)) {
            String[] ids = path.split("\\.");
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
     * @return List of {@link Holiday}s.
     */
    public List<Holiday> listHolidays() {
        GlobalManager globalManager = GlobalManager.getInstance();
        String path = globalManager.loadHoliday();

        return listHolidays(path);
    }

    /**
     * Get list of {@link Holiday}s, from today (including) till the same day next year (excluding), sorted by date.
     *
     * @param path Path identifier of a region
     * @return List of {@link Holiday}s.
     */
    public List<Holiday> listHolidays(String path) {
        GlobalManager globalManager = GlobalManager.getInstance();

        Calendar from = beginningOfToday(globalManager.clock().now());
        Calendar to = (Calendar) from.clone();
        to.add(Calendar.YEAR, 1);

        return listHolidays(path, from, to);
    }

    /**
     * Get list of {@link Holiday}s for the <code>path</code>, between <code>from</code> and <code>to</code> (including), sorted by date.
     *
     * @param path Path identifier of a region
     * @param from Lower end of range range
     * @param to   Upper range of date range
     * @return List of {@link Holiday}s.
     */
    public List<Holiday> listHolidays(String path, Calendar from, Calendar to) {
        HolidayCalendar holidayCalendar = HolidayHelper.getInstance().getHolidayCalendar(path);

        return listHolidays(holidayCalendar, path, from, to);
    }

    /**
     * Get list of {@link Holiday}s, between <code>from</code> and <code>to</code> (including), sorted by date.
     *
     * @param holidayCalendar HolidayCalendar to use
     * @param path            Path identifier of a region
     * @param from            Lower end of range range
     * @param to              Upper range of date range
     * @return List of {@link Holiday}s.
     */
    private List<Holiday> listHolidays(HolidayCalendar holidayCalendar, String path, Calendar from, Calendar to) {
        HolidayManager holidayManager = HolidayManager.getInstance(holidayCalendar);

        String[] ids = path.split("\\.");
        String[] subRegions = Arrays.copyOfRange(ids, 1, ids.length);

        // Add all the holidays the years that include the range
        Set<Holiday> holidays = new HashSet<>();
        for (int y = from.get(Calendar.YEAR); y <= to.get(Calendar.YEAR); y++) {
            Set<Holiday> holidaysY;
            holidaysY = holidayManager.getHolidays(y, subRegions);
            holidays.addAll(holidaysY);
        }

        // Filter: keep only holidays in the range
        Iterator<Holiday> it = holidays.iterator();
        while (it.hasNext()) {
            Holiday h = it.next();

            if (h.getDate().toDate().before(from.getTime()) || !h.getDate().toDate().before(to.getTime())) {
                it.remove();
            }
        }

        // Sort
        List<Holiday> orderedHolidays = new ArrayList<>(holidays);
        Collections.sort(orderedHolidays,
                (lhs, rhs) -> lhs.getDate().compareTo(rhs.getDate()));

        return orderedHolidays;
    }

    /**
     * Formats the region identifier to a string that can be presented to user. Separates the level by a dash. Uses real names of regions and respects locales.
     *
     * @param path path identifier of a region
     * @return string region name presentable to a user
     */
    public String preferenceToDisplayName(String path) {
        if (useHoliday(path)) {
            StringBuilder displayName = new StringBuilder();

            String[] ids = path.split("\\.");

            // Country
            ResourceUtil resourceUtil = new ResourceUtil();
            displayName.append(resourceUtil.getCountryDescription(ids[0]));

            // Subregions
            HolidayManager holidayManager = HolidayManager.getInstance(ids[0]);
            CalendarHierarchy calendarHierarchy = holidayManager.getCalendarHierarchy();

            Map<String, CalendarHierarchy> children = calendarHierarchy.getChildren();
            for (int i = 1; i < ids.length; i++) {
                String id = ids[i];
                CalendarHierarchy child = children.get(id);

                if (child == null) {
                    String prefix = pathPrefix(path, i);
                    throw new IllegalStateException("Prefix " + prefix + " has no id " + id + " in path " + path);
                }


                displayName.append(STRING_PATH_SEPARATOR);
                displayName.append(child.getDescription());

                children = children.get(ids[i]).getChildren();
            }

            return displayName.toString();
        } else {
            Resources res = context.getResources();
            return res.getString(R.string.holidays_none);
        }
    }

    /**
     * Return a holiday name.
     *
     * @param date Date
     * @return holida name
     * @throws IllegalStateException if it's not holiday on the date
     */
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

    /**
     * Check if the date is a holiday.
     *
     * @param date Date
     * @return true if it's holiday on the date
     */
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

    /**
     * Returns the number of levels of a region identifier.
     *
     * @param path path identifier of a region
     * @return number of levels in the path
     */
    public int pathLength(String path) {
        Log.v(TAG, "pathLength(parentPath=" + path + ")");
        int length;
        if (path.isEmpty()) length = 0;
        else length = path.split("\\.").length;
        Log.v(TAG, "pathLength=" + length);
        return length;
    }

    /**
     * Returns an ID of a super-region in a region identifier.
     *
     * @param path  path identifier of a region
     * @param index level of the super-region
     * @return id of the super-region
     */
    public String pathPart(String path, int index) {
        Log.d(TAG, "pathPart(parentPath=" + path + ", index=" + index + ")");
        String part = path.split("\\.")[index - 1];
        Log.v(TAG, "pathPart=" + part);
        return part;
    }

    /**
     * Return a super-region of a region.
     *
     * @param path  path identifier of a region
     * @param level level of the super-region.
     * @return path identifying a region
     */
    public String pathPrefix(String path, int level) {
        Log.d(TAG, "pathPrefix(parentPath=" + path + ", level=" + level + ")");

        if (level == 0) {
            Log.v(TAG, "pathPrefix=\"\"");
            return PATH_TOP;
        }

        int pos = -1;
        int counter = 0;
        while ((pos = path.indexOf(PATH_SEPARATOR, pos + 1)) != -1) {
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

    /**
     * Check if a region identifier is valid.
     *
     * @param path path identifier of a region
     * @return true if a region with such <code>path</code> exists
     */
    public boolean isPathValid(String path) {
        try {
            Map<String, CalendarHierarchy> children = pathInfo(path);
            return true;
        } catch (IllegalStateException e) {
            return false;
        }
    }

    /**
     * Lists the regions at top level.
     *
     * @return list of regions
     */
    public List<Region> list() {
        return list(PATH_TOP);
    }

    /**
     * Checks if a region has subregions.
     *
     * @param path path identifier of a region
     * @return true if the region has subregions
     */
    public boolean hasSubregions(String path) {
        return !list(path).isEmpty();
    }

    /**
     * Lists the subregions of a region identified by <code>path</code>.
     *
     * @param path path identifier of a region
     * @return list of regions
     */
    public List<Region> list(String path) {
        Log.v(TAG, "List region " + path);
        List<Region> list;

        if (path.equals(PATH_TOP)) {
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
            region.parentPath = PATH_TOP;
            region.id = c.getId();
            region.description = resourceUtil.getCountryDescription(c.getId());

            list.add(region);
        }

        return list;
    }

    /**
     * @throws IllegalStateException if the path doesn't exist
     */
    private Map<String, CalendarHierarchy> pathInfo(String path) {
        Log.d(TAG, "pathInfo(path=" + path + ")");
        ResourceUtil resourceUtil = new ResourceUtil();

        String[] ids = path.split("\\.");

        String id = ids[0];
        Log.v(TAG, " " + id + " " + resourceUtil.getCountryDescription(id));

        HolidayManager holidayManager;

        try {
            holidayManager = HolidayManager.getInstance(id);
        } catch (IllegalStateException e) {
            // Country does not exist
            throw new IllegalStateException("Prefix " + id + " does not exist in path " + path);
        }

        CalendarHierarchy calendarHierarchy = holidayManager.getCalendarHierarchy();
        Map<String, CalendarHierarchy> children = calendarHierarchy.getChildren();

        for (int i = 1; i < ids.length; i++) {
            id = ids[i];
            CalendarHierarchy child = children.get(id);

            if (child == null) {
                String prefix = pathPrefix(path, i);
                throw new IllegalStateException("Prefix " + prefix + " has no id " + id + " in path " + path);
            }

            Log.v(TAG, " " + id + " " + child.getDescription());

            children = child.getChildren();
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

/**
 * Region represents a region. Region has an {@link #id} and a path identifying the parent region in {@link #parentPath}.
 */
class Region {

    String parentPath;
    String id;
    String description;

    boolean isTop() {
        return parentPath.equals(PATH_TOP);
    }

    String getFullPath() {
        if (isTop()) {
            return id;
        } else {
            return parentPath + HolidayHelper.PATH_SEPARATOR + id;
        }
    }
}