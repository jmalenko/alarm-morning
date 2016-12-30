package cz.jaro.alarmmorning.model;

import android.support.annotation.NonNull;

import org.junit.After;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.lang.reflect.Field;
import java.util.Calendar;
import java.util.GregorianCalendar;

import cz.jaro.alarmmorning.Analytics;
import cz.jaro.alarmmorning.BuildConfig;
import cz.jaro.alarmmorning.GlobalManager;
import cz.jaro.alarmmorning.SettingsActivity;
import cz.jaro.alarmmorning.clock.FixedClock;
import cz.jaro.alarmmorning.shadows.ShadowAlarmManagerAPI21;

import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertNull;

/**
 * Tests when no alarm should ring. Holiday is not defined.
 * <p>
 * The tests use Robolectric to mock access to database and context (needed by holidays).
 */
@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class, sdk = 21, shadows = {ShadowAlarmManagerAPI21.class})
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class GlobalManager1NextAlarm0NoAlarmTest {

    private GlobalManager globalManager;

    // February 2016 starts with Monday
    public static final int YEAR = 2016;
    public static final int MONTH = Calendar.FEBRUARY;
    public static final int DAY = 1;
    public static final int HOUR = 1;
    public static final int MINUTE = 0;

    public static final int HOUR_DAY = 8;
    public static final int MINUTE_DAY = 1;

    public static final int HOUR_DEFAULT = AlarmDbHelper.DEFAULT_ALARM_HOUR;
    public static final int MINUTE_DEFAULT = AlarmDbHelper.DEFAULT_ALARM_MINUTE;

    public static final int RANGE = 40; // Note: Large values result in longer run times

    @Before
    public void before() {
        globalManager = GlobalManager.getInstance();

        globalManager.resetDatabase();

        // Holiday
        globalManager.saveHoliday(SettingsActivity.PREF_HOLIDAY_NONE);
    }

    /**
     * This must be done to clean up after a test. Otherwise we get <code>java.lang.RuntimeException: java.util.concurrent.ExecutionException: java.lang
     * .IllegalStateException: Illegal connection pointer 1. Current pointers for thread</code>
     * <p>
     * The reason is that the own SQLiteOpenHelper is a singleton. Between tests all instances should be reset or you will get strange side effects like this.
     * For me it works to set the static variable null per reflection. Here an example
     */
    @After
    public void after() {
        resetSingleton(GlobalManager.class, "instance");
    }

    /**
     * Sets a static field to null.
     *
     * @param clazz     Class
     * @param fieldName The static variable name which holds the singleton instance
     */
    public static void resetSingleton(Class clazz, String fieldName) {
        Field instance;
        try {
            instance = clazz.getDeclaredField(fieldName);
            instance.setAccessible(true);
            instance.set(null, null);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void t00_preconditions() {
        assertTrue("Range includes horizon", GlobalManager.HORIZON_DAYS < RANGE);
    }

    @Test
    public void t10_noAlarmNow() {
        FixedClock clock = clockTest();
        Calendar nextAlarm = globalManager.getNextAlarm(clock);

        assertNull(nextAlarm);
    }

    @NonNull
    public static FixedClock clockTest() {
        GregorianCalendar date = new GregorianCalendar(YEAR, MONTH, DAY);
        return new FixedClock(date);
    }

    @Test
    public void t11_noAlarmAnytime() {
        Calendar now = clockTest().now();
        for (int i = -RANGE; i <= RANGE; i++) {
            Calendar date = (Calendar) now.clone();
            date.add(Calendar.DATE, i);

            FixedClock clock = new FixedClock(date);

            Calendar nextAlarm = globalManager.getNextAlarm(clock);

            String str = " on " + date.getTime();

            assertNull("There should be no alarm" + str, nextAlarm);
        }
    }

    private void insertDisabledValues() {
        // Days
        for (int i = -RANGE; i <= RANGE; i++) {
            Calendar now = clockTest().now();
            Calendar date = (Calendar) now.clone();
            date.add(Calendar.DATE, i);

            Day day = new Day();
            day.setDate(date);
            day.setState(Day.STATE_RULE);
            day.setHour(HOUR_DAY);
            day.setMinute(MINUTE_DAY);

            Defaults defaults = new Defaults();
            int dayOfWeek = date.get(Calendar.DAY_OF_WEEK);
            defaults.setDayOfWeek(dayOfWeek);
            defaults.setState(Defaults.STATE_DISABLED);
            defaults.setHour(HOUR_DEFAULT);
            defaults.setMinute(MINUTE_DEFAULT);

            day.setDefaults(defaults);

            Analytics analytics = new Analytics(Analytics.Channel.Test, Analytics.ChannelName.Calendar);

            globalManager.saveDay(day, analytics);
        }

        // Defaults
        for (int dayOfWeek : AlarmDataSource.allDaysOfWeek) {
            Defaults defaults = new Defaults();
            defaults.setDayOfWeek(dayOfWeek);
            defaults.setState(Defaults.STATE_DISABLED);
            defaults.setHour(HOUR_DEFAULT);
            defaults.setMinute(MINUTE_DEFAULT);

            Analytics analytics = new Analytics(Analytics.Channel.Test, Analytics.ChannelName.Defaults);

            globalManager.saveDefault(defaults, analytics);
        }
    }

    @Test
    public void t20_noAlarmAnytimeWithFullDatabase() {
        insertDisabledValues();

        Calendar now = clockTest().now();
        for (int i = -RANGE; i <= RANGE; i++) {
            Calendar date = (Calendar) now.clone();
            date.add(Calendar.DATE, i);

            FixedClock clock = new FixedClock(date);

            Calendar nextAlarm = globalManager.getNextAlarm(clock);

            String str = " on " + date.getTime();

            assertNull("There should be no alarm" + str, nextAlarm);
        }
    }

}