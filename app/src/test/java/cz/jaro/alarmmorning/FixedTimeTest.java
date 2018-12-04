package cz.jaro.alarmmorning;

import android.annotation.SuppressLint;
import android.util.ArraySet;
import android.view.View;

import org.junit.After;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadow.api.Shadow;
import org.robolectric.shadows.ShadowApplication;
import org.robolectric.util.ReflectionHelpers;

import java.lang.reflect.Field;
import java.util.GregorianCalendar;
import java.util.List;

import cz.jaro.alarmmorning.checkalarmtime.CheckAlarmTime;
import cz.jaro.alarmmorning.clock.Clock;
import cz.jaro.alarmmorning.clock.FixedClock;
import cz.jaro.alarmmorning.holiday.HolidayHelper;
import cz.jaro.alarmmorning.model.DayTest;
import cz.jaro.alarmmorning.nighttimebell.NighttimeBell;
import cz.jaro.alarmmorning.shadows.ShadowGlobalManager;

import static cz.jaro.alarmmorning.model.DayTest.DAY;
import static cz.jaro.alarmmorning.model.DayTest.MONTH;
import static cz.jaro.alarmmorning.model.DayTest.YEAR;
import static org.robolectric.Robolectric.flushBackgroundThreadScheduler;
import static org.robolectric.Robolectric.flushForegroundThreadScheduler;
import static org.robolectric.shadows.ShadowApplication.runBackgroundTasks;
import static org.robolectric.shadows.ShadowLooper.runUiThreadTasksIncludingDelayedTasks;

/**
 * The parent class of all tests which supports setting a particular fixed time.
 * <p>
 * The tests use Robolectric to mock access to database and context (needed by holidays).
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 21, shadows = {ShadowGlobalManager.class})
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public abstract class FixedTimeTest {

    protected GlobalManager globalManager;
    protected ShadowGlobalManager shadowGlobalManager;

    @Before
    public void before() {
        // Disable Google Analytics
        ShadowApplication.getInstance().declareActionUnbindable("com.google.android.gms.analytics.service.START");

        globalManager = GlobalManager.getInstance();

        shadowGlobalManager = Shadow.extract(globalManager);
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
        resetSingleton(SystemAlarmClock.class, "instance");
        resetSingleton(SystemNotification.class, "instance");
        resetSingleton(CheckAlarmTime.class, "instance");
        resetSingleton(NighttimeBell.class, "instance");
        resetSingleton(HolidayHelper.class, "instance");

        // TODO The following is a workaround for a Robolectric bug. See https://github.com/robolectric/robolectric/issues/2068
        // https://github.com/robolectric/robolectric/issues/1700
        // https://github.com/robolectric/robolectric/issues/2068
        // https://github.com/robolectric/robolectric/issues/2584
        try {
            resetBackgroundThread();
        } catch (Exception e) {
            throw new Error(e);
        }
        resetWindowManager();
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

    public void finishThreads() {
        runBackgroundTasks();
        flushForegroundThreadScheduler();
        flushBackgroundThreadScheduler();
        runUiThreadTasksIncludingDelayedTasks();
    }

    // https://github.com/robolectric/robolectric/pull/1741
    private void resetBackgroundThread() throws Exception {
        final Class<?> btclass = Class.forName("com.android.internal.os.BackgroundThread");
        final Object backgroundThreadSingleton = ReflectionHelpers.getStaticField(btclass, "sInstance");
        if (backgroundThreadSingleton != null) {
            btclass.getMethod("quit").invoke(backgroundThreadSingleton);
            ReflectionHelpers.setStaticField(btclass, "sInstance", null);
            ReflectionHelpers.setStaticField(btclass, "sHandler", null);
        }
    }

    // https://github.com/robolectric/robolectric/issues/2068#issue-109132096
    @SuppressLint("NewApi")
    private void resetWindowManager() {
        final Class<?> clazz = ReflectionHelpers.loadClass(this.getClass().getClassLoader(), "android.view.WindowManagerGlobal");
        final Object instance = ReflectionHelpers.callStaticMethod(clazz, "getInstance");

        // We essentially duplicate what's in {@link WindowManagerGlobal#closeAll} with what's below.
        // The closeAll method has a bit of a bug where it's iterating through the "roots" but
        // bases the number of objects to iterate through by the number of "views." This can result in
        // an {@link java.lang.IndexOutOfBoundsException} being thrown.
        final Object lock = ReflectionHelpers.getField(instance, "mLock");

        final List<Object> roots = ReflectionHelpers.getField(instance, "mRoots");
        //noinspection SynchronizationOnLocalVariableOrMethodParameter
        synchronized (lock) {
            for (int i = 0; i < roots.size(); i++) {
                ReflectionHelpers.callInstanceMethod(instance, "removeViewLocked",
                        ReflectionHelpers.ClassParameter.from(int.class, i),
                        ReflectionHelpers.ClassParameter.from(boolean.class, false));
            }
        }

        // Views will still be held by this array. We need to clear it out to ensure
        // everything is released.
        final ArraySet<View> dyingViews = ReflectionHelpers.getField(instance, "mDyingViews");
        dyingViews.clear();
    }

    private Clock clock() {
        // Time at which the test is executed
        return new FixedClock(new GregorianCalendar(YEAR, MONTH, DAY, DayTest.HOUR, DayTest.MINUTE));
    }
}