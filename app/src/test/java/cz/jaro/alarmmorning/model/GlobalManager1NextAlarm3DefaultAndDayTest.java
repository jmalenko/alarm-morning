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
import cz.jaro.alarmmorning.clock.FixedClock;
import cz.jaro.alarmmorning.shadows.ShadowAlarmManagerAPI21;

import static cz.jaro.alarmmorning.model.GlobalManager1NextAlarm2DefaultTest.dayOfWeek;
import static cz.jaro.alarmmorning.model.GlobalManager1NextAlarm2DefaultTest.findNextSameDayOfWeek;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

/**
 * Tests when there is one default enabled and a day that disables the alarm (which was set by default). Holiday is not defined.
 */
@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class, sdk = 21, shadows = {ShadowAlarmManagerAPI21.class})
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class GlobalManager1NextAlarm3DefaultAndDayTest {

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

    private void setAlarmDisabledToday() {
        Calendar date = new GregorianCalendar(DayTest.YEAR, DayTest.MONTH, DayTest.DAY, DayTest.HOUR, DayTest.MINUTE);

        Day day = new Day();
        day.setDate(date);
        day.setState(Day.STATE_DISABLED);
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
    public void t10_justBeforeAlarmWithoutDay() {
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
    public void t10_justBeforeAlarmWithDay() {
        setDefaultAlarm();
        setAlarmDisabledToday();

        Calendar date = new GregorianCalendar(DayTest.YEAR, DayTest.MONTH, DayTest.DAY, DayTest.HOUR_DEFAULT, DayTest.MINUTE_DEFAULT);
        date.add(Calendar.SECOND, -1);
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

}