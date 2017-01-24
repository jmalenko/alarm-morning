package cz.jaro.alarmmorning;

import org.junit.After;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.internal.ShadowExtractor;

import java.lang.reflect.Field;
import java.util.GregorianCalendar;

import cz.jaro.alarmmorning.checkalarmtime.CheckAlarmTime;
import cz.jaro.alarmmorning.clock.Clock;
import cz.jaro.alarmmorning.clock.FixedClock;
import cz.jaro.alarmmorning.model.DayTest;
import cz.jaro.alarmmorning.nighttimebell.NighttimeBell;
import cz.jaro.alarmmorning.shadows.ShadowAlarmManagerAPI21;
import cz.jaro.alarmmorning.shadows.ShadowGlobalManager;

import static cz.jaro.alarmmorning.model.DayTest.DAY;
import static cz.jaro.alarmmorning.model.DayTest.MONTH;
import static cz.jaro.alarmmorning.model.DayTest.YEAR;

/**
 * The parent class of all tests which supports setting a particular fixed time.
 * <p>
 * The tests use Robolectric to mock access to database and context (needed by holidays).
 */
@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class, sdk = 21, shadows = {ShadowAlarmManagerAPI21.class, ShadowGlobalManager.class})
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class FixedTimeTest {

    protected GlobalManager globalManager;
    protected ShadowGlobalManager shadowGlobalManager;

    @Before
    public void before() {
        globalManager = GlobalManager.getInstance();

        shadowGlobalManager = (ShadowGlobalManager) ShadowExtractor.extract(globalManager);
        shadowGlobalManager.setClock(clock());

        globalManager.reset();
    }

    /**
     * This must be done to clean up after a test. Otherwise we get <code>java.lang.RuntimeException: java.util.concurrent.ExecutionException: java.lang
     * .IllegalStateException: Illegal connection pointer 1. Current pointers for thread</code>. The reason is that the own SQLiteOpenHelper (and other
     * classes) is a singleton. Between tests all instances should be reset, otherwise we get strange side effects like this.
     */
    @After
    public void after() {
        // Reset all singletons
        resetSingleton(GlobalManager.class, "instance");
        resetSingleton(SystemAlarm.class, "instance");
        resetSingleton(CheckAlarmTime.class, "instance");
        resetSingleton(SystemAlarmClock.class, "instance");
        resetSingleton(NighttimeBell.class, "instance");
    }

    /**
     * Sets a static field to null.
     *
     * @param clazz     Class
     * @param fieldName The static variable name which holds the singleton instance
     */
    private static void resetSingleton(Class clazz, String fieldName) {
        Field instance;
        try {
            instance = clazz.getDeclaredField(fieldName);
            instance.setAccessible(true);
            instance.set(null, null);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    Clock clock() {
        return new FixedClock(new GregorianCalendar(YEAR, MONTH, DAY, DayTest.HOUR, DayTest.MINUTE));
    }
}