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
import static org.junit.Assert.assertThat;

/**
 * Tests when there is only one default enabled. Holiday is not defined.
 */
@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class, sdk = 21, shadows = {ShadowAlarmManagerAPI21.class})
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class GlobalManager1NextAlarm2DefaultTest {

    private GlobalManager globalManager;

    @Before
    public void before() {
        globalManager = GlobalManager.getInstance();
        globalManager.reset();
    }

    @After
    public void after() {
        GlobalManager1NextAlarm0NoAlarmTest.resetSingleton(GlobalManager.class, "instance");
    }

    private void setDefaultAlarm() {
        Calendar date = new GregorianCalendar(DayTest.YEAR, DayTest.MONTH, DayTest.DAY, DayTest.HOUR, DayTest.MINUTE);
        int dayOfWeek = date.get(Calendar.DAY_OF_WEEK);

        Defaults defaults = new Defaults();
        defaults.setDayOfWeek(dayOfWeek);
        defaults.setState(Defaults.STATE_ENABLED);
        defaults.setHour(DayTest.HOUR_DEFAULT);
        defaults.setMinute(DayTest.MINUTE_DEFAULT);

        Analytics analytics = new Analytics(Analytics.Channel.Test, Analytics.ChannelName.Defaults);

        globalManager.saveDefault(defaults, analytics);
    }

    @Test
    public void t10_alarmToday() {
        setDefaultAlarm();

        Clock clock = GlobalManager1NextAlarm0NoAlarmTest.clockTest();

        Calendar nextAlarm = globalManager.getNextAlarm(clock);

        assertThat("Year", nextAlarm.get(Calendar.YEAR), is(DayTest.YEAR));
        assertThat("Month", nextAlarm.get(Calendar.MONTH), is(DayTest.MONTH));
        assertThat("Date", nextAlarm.get(Calendar.DAY_OF_MONTH), is(DayTest.DAY));
        assertThat("Hour", nextAlarm.get(Calendar.HOUR_OF_DAY), is(DayTest.HOUR_DEFAULT));
        assertThat("Minute", nextAlarm.get(Calendar.MINUTE), is(DayTest.MINUTE_DEFAULT));
    }

    @Test
    public void t11_justBeforeAlarm() {
        setDefaultAlarm();

        Calendar date = new GregorianCalendar(DayTest.YEAR, DayTest.MONTH, DayTest.DAY, DayTest.HOUR_DEFAULT, DayTest.MINUTE_DEFAULT);
        date.add(Calendar.SECOND, -1);
        FixedClock clock = new FixedClock(date);

        Calendar nextAlarm = globalManager.getNextAlarm(clock);

        assertThat("Year", nextAlarm.get(Calendar.YEAR), is(DayTest.YEAR));
        assertThat("Month", nextAlarm.get(Calendar.MONTH), is(DayTest.MONTH));
        assertThat("Date", nextAlarm.get(Calendar.DAY_OF_MONTH), is(DayTest.DAY));
        assertThat("Hour", nextAlarm.get(Calendar.HOUR_OF_DAY), is(DayTest.HOUR_DEFAULT));
        assertThat("Minute", nextAlarm.get(Calendar.MINUTE), is(DayTest.MINUTE_DEFAULT));
    }

    @Test
    public void t21_yesterday() {
        setDefaultAlarm();

        Calendar date = new GregorianCalendar(DayTest.YEAR, DayTest.MONTH, DayTest.DAY, DayTest.HOUR, DayTest.MINUTE);
        date.add(Calendar.DAY_OF_MONTH, -1);
        FixedClock clock = new FixedClock(date);

        Calendar nextAlarm = globalManager.getNextAlarm(clock);

        assertThat("Year", nextAlarm.get(Calendar.YEAR), is(DayTest.YEAR));
        assertThat("Month", nextAlarm.get(Calendar.MONTH), is(DayTest.MONTH));
        assertThat("Date", nextAlarm.get(Calendar.DAY_OF_MONTH), is(DayTest.DAY));
        assertThat("Hour", nextAlarm.get(Calendar.HOUR_OF_DAY), is(DayTest.HOUR_DEFAULT));
        assertThat("Minute", nextAlarm.get(Calendar.MINUTE), is(DayTest.MINUTE_DEFAULT));
    }

    @Test
    public void t31_justAfterAlarm() {
        setDefaultAlarm();

        Calendar date = new GregorianCalendar(DayTest.YEAR, DayTest.MONTH, DayTest.DAY, DayTest.HOUR_DEFAULT, DayTest.MINUTE_DEFAULT);
        date.add(Calendar.SECOND, 1);
        FixedClock clock = new FixedClock(date);

        Calendar nextAlarm = globalManager.getNextAlarm(clock);

        Calendar tomorrowBeginning = (Calendar) date.clone();
        tomorrowBeginning.add(Calendar.DATE, 1);
        tomorrowBeginning.set(Calendar.HOUR, 0);
        tomorrowBeginning.set(Calendar.MINUTE, 0);
        tomorrowBeginning.set(Calendar.SECOND, 0);
        tomorrowBeginning.set(Calendar.MILLISECOND, 0);
        Calendar nextSameDayOfWeek = findNextSameDayOfWeek(tomorrowBeginning, dayOfWeek());

        assertThat("Year", nextAlarm.get(Calendar.YEAR), is(nextSameDayOfWeek.get(Calendar.YEAR)));
        assertThat("Month", nextAlarm.get(Calendar.MONTH), is(nextSameDayOfWeek.get(Calendar.MONTH)));
        assertThat("Date", nextAlarm.get(Calendar.DAY_OF_MONTH), is(nextSameDayOfWeek.get(Calendar.DAY_OF_MONTH)));
        assertThat("Hour", nextAlarm.get(Calendar.HOUR_OF_DAY), is(DayTest.HOUR_DEFAULT));
        assertThat("Minute", nextAlarm.get(Calendar.MINUTE), is(DayTest.MINUTE_DEFAULT));
    }

    @Test
    public void t32_tomorrow() {
        setDefaultAlarm();

        Calendar date = new GregorianCalendar(DayTest.YEAR, DayTest.MONTH, DayTest.DAY, DayTest.HOUR, DayTest.MINUTE);
        date.add(Calendar.DAY_OF_MONTH, 1);
        FixedClock clock = new FixedClock(date);

        Calendar nextAlarm = globalManager.getNextAlarm(clock);

        Calendar nextSameDayOfWeekDay = findNextSameDayOfWeek(date, dayOfWeek());

        assertThat("Year", nextAlarm.get(Calendar.YEAR), is(nextSameDayOfWeekDay.get(Calendar.YEAR)));
        assertThat("Month", nextAlarm.get(Calendar.MONTH), is(nextSameDayOfWeekDay.get(Calendar.MONTH)));
        assertThat("Date", nextAlarm.get(Calendar.DAY_OF_MONTH), is(nextSameDayOfWeekDay.get(Calendar.DAY_OF_MONTH)));
        assertThat("Hour", nextAlarm.get(Calendar.HOUR_OF_DAY), is(DayTest.HOUR_DEFAULT));
        assertThat("Minute", nextAlarm.get(Calendar.MINUTE), is(DayTest.MINUTE_DEFAULT));
    }

    @Test
    public void t40_everyDay() {
        setDefaultAlarm();

        Calendar now = GlobalManager1NextAlarm0NoAlarmTest.clockTest().now();
        for (int i = -RANGE; i <= RANGE; i++) {
            Calendar date = (Calendar) now.clone();
            date.add(Calendar.DATE, i);

            FixedClock clock = new FixedClock(date);

            Calendar nextAlarm = globalManager.getNextAlarm(clock);

            Calendar nextSameDayOfWeekDay = findNextSameDayOfWeek(date, dayOfWeek());
            String str = " on " + date.getTime();

            assertThat("Year" + str, nextAlarm.get(Calendar.YEAR), is(nextSameDayOfWeekDay.get(Calendar.YEAR)));
            assertThat("Month" + str, nextAlarm.get(Calendar.MONTH), is(nextSameDayOfWeekDay.get(Calendar.MONTH)));
            assertThat("Date" + str, nextAlarm.get(Calendar.DAY_OF_MONTH), is(nextSameDayOfWeekDay.get(Calendar.DAY_OF_MONTH)));
            assertThat("Hour" + str, nextAlarm.get(Calendar.HOUR_OF_DAY), is(DayTest.HOUR_DEFAULT));
            assertThat("Minute" + str, nextAlarm.get(Calendar.MINUTE), is(DayTest.MINUTE_DEFAULT));
        }
    }

    /**
     * Finds the first day, starting with <code>date</code>, such that it's day of week equals <<code>dayOfWeek</code>.
     *
     * @param date      Date to start with
     * @param dayOfWeek Day of Week
     * @return Date of the next day
     */
    public static Calendar findNextSameDayOfWeek(Calendar date, int dayOfWeek) {
        Calendar res = (Calendar) date.clone();
        while (res.get(Calendar.DAY_OF_WEEK) != dayOfWeek)
            res.add(Calendar.DATE, 1);
        return res;
    }

    static int dayOfWeek() {
        Calendar dateDefault = new GregorianCalendar(DayTest.YEAR, DayTest.MONTH, DayTest.DAY, DayTest.HOUR, DayTest.MINUTE);
        return dateDefault.get(Calendar.DAY_OF_WEEK);
    }
}