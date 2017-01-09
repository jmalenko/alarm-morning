package cz.jaro.alarmmorning.model;

import org.junit.After;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.Calendar;
import java.util.GregorianCalendar;

import cz.jaro.alarmmorning.Analytics;
import cz.jaro.alarmmorning.BuildConfig;
import cz.jaro.alarmmorning.GlobalManager;
import cz.jaro.alarmmorning.clock.Clock;
import cz.jaro.alarmmorning.clock.FixedClock;
import cz.jaro.alarmmorning.shadows.ShadowAlarmManagerAPI21;

import static cz.jaro.alarmmorning.model.GlobalManager1NextAlarm0NoAlarmTest.RANGE;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

/**
 * Tests when there is only one day alarm enabled. Holiday is not defined.
 */
@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class, sdk = 21, shadows = {ShadowAlarmManagerAPI21.class})
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class GlobalManager1NextAlarm1DayTest {

    private GlobalManager globalManager;

    @Before
    public void before() {
        globalManager = GlobalManager.getInstance();

        globalManager.resetDatabase();
        globalManager.resetSettings();
    }

    @After
    public void after() {
        GlobalManager1NextAlarm0NoAlarmTest.resetSingleton(GlobalManager.class, "instance");
    }

    private void setAlarmToToday() {
        Calendar date = new GregorianCalendar(DayTest.YEAR, DayTest.MONTH, DayTest.DAY, DayTest.HOUR, DayTest.MINUTE);

        Day day = new Day();
        day.setDate(date);
        day.setState(Day.STATE_ENABLED);
        day.setHour(DayTest.HOUR_DAY);
        day.setMinute(DayTest.MINUTE_DAY);

        Defaults defaults = new Defaults();
        int dayOfWeek = date.get(Calendar.DAY_OF_WEEK);
        defaults.setDayOfWeek(dayOfWeek);
        defaults.setState(Defaults.STATE_DISABLED);

        day.setDefaults(defaults);

        Analytics analytics = new Analytics(Analytics.Channel.Test, Analytics.ChannelName.Calendar);

        globalManager.saveDay(day, analytics);
    }

    @Test
    public void t10_alarmToday() {
        setAlarmToToday();

        Clock clock = GlobalManager1NextAlarm0NoAlarmTest.clockTest();
        Calendar nextAlarm = globalManager.getNextAlarm(clock);

        assertThat("Year", nextAlarm.get(Calendar.YEAR), is(DayTest.YEAR));
        assertThat("Month", nextAlarm.get(Calendar.MONTH), is(DayTest.MONTH));
        assertThat("Date", nextAlarm.get(Calendar.DAY_OF_MONTH), is(DayTest.DAY));
        assertThat("Hour", nextAlarm.get(Calendar.HOUR_OF_DAY), is(DayTest.HOUR_DAY));
        assertThat("Minute", nextAlarm.get(Calendar.MINUTE), is(DayTest.MINUTE_DAY));
    }

    @Test
    public void t11_justBeforeAlarm() {
        setAlarmToToday();

        Calendar date = new GregorianCalendar(DayTest.YEAR, DayTest.MONTH, DayTest.DAY, DayTest.HOUR_DAY, DayTest.MINUTE_DAY);
        date.add(Calendar.SECOND, -1); // 1 second before alarm
        FixedClock clock = new FixedClock(date);

        Calendar nextAlarm = globalManager.getNextAlarm(clock);

        assertThat("Year", nextAlarm.get(Calendar.YEAR), is(DayTest.YEAR));
        assertThat("Month", nextAlarm.get(Calendar.MONTH), is(DayTest.MONTH));
        assertThat("Date", nextAlarm.get(Calendar.DAY_OF_MONTH), is(DayTest.DAY));
        assertThat("Hour", nextAlarm.get(Calendar.HOUR_OF_DAY), is(DayTest.HOUR_DAY));
        assertThat("Minute", nextAlarm.get(Calendar.MINUTE), is(DayTest.MINUTE_DAY));
    }

    @Test
    public void t21_yesterday() {
        setAlarmToToday();

        Calendar date = new GregorianCalendar(DayTest.YEAR, DayTest.MONTH, DayTest.DAY, DayTest.HOUR, DayTest.MINUTE);
        date.add(Calendar.DAY_OF_MONTH, -1);
        FixedClock clock = new FixedClock(date);

        Calendar nextAlarm = globalManager.getNextAlarm(clock);

        assertThat("Year", nextAlarm.get(Calendar.YEAR), is(DayTest.YEAR));
        assertThat("Month", nextAlarm.get(Calendar.MONTH), is(DayTest.MONTH));
        assertThat("Date", nextAlarm.get(Calendar.DAY_OF_MONTH), is(DayTest.DAY));
        assertThat("Hour", nextAlarm.get(Calendar.HOUR_OF_DAY), is(DayTest.HOUR_DAY));
        assertThat("Minute", nextAlarm.get(Calendar.MINUTE), is(DayTest.MINUTE_DAY));
    }

    @Test
    public void t22_beforeTodayFar() {
        setAlarmToToday();

        Calendar date = new GregorianCalendar(DayTest.YEAR, DayTest.MONTH, DayTest.DAY, DayTest.HOUR, DayTest.MINUTE);
        date.add(Calendar.DAY_OF_MONTH, -GlobalManager1NextAlarm0NoAlarmTest.RANGE);
        FixedClock clock = new FixedClock(date);

        Calendar nextAlarm = globalManager.getNextAlarm(clock);

        assertNull(nextAlarm);
    }

    @Test
    public void t23_before() {
        setAlarmToToday();

        Calendar now = GlobalManager1NextAlarm0NoAlarmTest.clockTest().now();
        for (int i = -RANGE; i <= 0; i++) {
            Calendar date = (Calendar) now.clone();
            date.add(Calendar.DATE, i);

            FixedClock clock = new FixedClock(date);

            Calendar nextAlarm = globalManager.getNextAlarm(clock);

            String str = " on " + date.getTime();

            if (i <= -GlobalManager.HORIZON_DAYS) {
                assertNull("There should be no alarm in distant past" + str, nextAlarm);
            } else {
                assertThat("Year" + str, nextAlarm.get(Calendar.YEAR), is(DayTest.YEAR));
                assertThat("Month" + str, nextAlarm.get(Calendar.MONTH), is(DayTest.MONTH));
                assertThat("Date" + str, nextAlarm.get(Calendar.DAY_OF_MONTH), is(DayTest.DAY));
                assertThat("Hour" + str, nextAlarm.get(Calendar.HOUR_OF_DAY), is(DayTest.HOUR_DAY));
                assertThat("Minute" + str, nextAlarm.get(Calendar.MINUTE), is(DayTest.MINUTE_DAY));
            }
        }
    }

    @Test
    public void t31_justAfterAlarm() {
        setAlarmToToday();

        Calendar date = new GregorianCalendar(DayTest.YEAR, DayTest.MONTH, DayTest.DAY, DayTest.HOUR_DAY, DayTest.MINUTE_DAY);
        date.add(Calendar.SECOND, 1); // 1 second after alarm
        FixedClock clock = new FixedClock(date);

        Calendar nextAlarm = globalManager.getNextAlarm(clock);

        assertNull(nextAlarm);
    }

    @Test
    public void t32_after() {
        setAlarmToToday();

        Calendar now = GlobalManager1NextAlarm0NoAlarmTest.clockTest().now();
        for (int i = 1; i <= RANGE; i++) {
            Calendar date = (Calendar) now.clone();
            date.add(Calendar.DATE, i);

            FixedClock clock = new FixedClock(date);

            Calendar nextAlarm = globalManager.getNextAlarm(clock);

            assertNull(nextAlarm);
        }
    }

}