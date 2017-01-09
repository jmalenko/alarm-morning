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
import cz.jaro.alarmmorning.SettingsActivity;
import cz.jaro.alarmmorning.clock.FixedClock;
import cz.jaro.alarmmorning.shadows.ShadowAlarmManagerAPI21;

import static cz.jaro.alarmmorning.holiday.HolidayHelper1HolidaysTest.holidays_Czech;
import static cz.jaro.alarmmorning.holiday.HolidayHelper1HolidaysTest.isHoliday;
import static cz.jaro.alarmmorning.holiday.HolidayHelper1HolidaysTest.sameDate;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

/**
 * Tests when there is holiday defined.
 */
@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class, sdk = 21, shadows = {ShadowAlarmManagerAPI21.class})
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class GlobalManager1NextAlarm4HolidaysTest {

    private GlobalManager globalManager;

    @Before
    public void before() {
        globalManager = GlobalManager.getInstance();
        globalManager.reset();

        // Defaults
        for (int dayOfWeek : AlarmDataSource.allDaysOfWeek) {
            Defaults defaults = new Defaults();
            defaults.setDayOfWeek(dayOfWeek);
            defaults.setState(Defaults.STATE_ENABLED);
            defaults.setHour(DayTest.HOUR_DEFAULT);
            defaults.setMinute(DayTest.MINUTE_DEFAULT);

            Analytics analytics = new Analytics(Analytics.Channel.Test, Analytics.ChannelName.Defaults);

            globalManager.saveDefault(defaults, analytics);
        }
    }

    @After
    public void after() {
        GlobalManager1NextAlarm0NoAlarmTest.resetSingleton(GlobalManager.class, "instance");
    }

    @Test
    public void t10_everyDayWIthoutHolidays() {
        globalManager.saveHoliday(SettingsActivity.PREF_HOLIDAY_NONE);

        Calendar from = new GregorianCalendar(DayTest.YEAR, Calendar.JANUARY, 1);
        Calendar to = new GregorianCalendar(DayTest.YEAR + 1, Calendar.JANUARY, 1);
        for (Calendar date = (Calendar) from.clone(); date.before(to); date.add(Calendar.DATE, 1)) {

            FixedClock clock = new FixedClock(date);

            Calendar nextAlarm = globalManager.getNextAlarm(clock);

            String str = " on " + date.getTime();

            assertThat("Year" + str, nextAlarm.get(Calendar.YEAR), is(date.get(Calendar.YEAR)));
            assertThat("Month" + str, nextAlarm.get(Calendar.MONTH), is(date.get(Calendar.MONTH)));
            assertThat("Date" + str, nextAlarm.get(Calendar.DAY_OF_MONTH), is(date.get(Calendar.DAY_OF_MONTH)));
            assertThat("Hour" + str, nextAlarm.get(Calendar.HOUR_OF_DAY), is(DayTest.HOUR_DEFAULT));
            assertThat("Minute" + str, nextAlarm.get(Calendar.MINUTE), is(DayTest.MINUTE_DEFAULT));
        }
    }

    @Test
    public void t10a_NewYear_CZ() {
        globalManager.saveHoliday("CZ");

        Calendar date = new GregorianCalendar(DayTest.YEAR, Calendar.JANUARY, 1, DayTest.HOUR, DayTest.MINUTE);
        FixedClock clock = new FixedClock(date);

        Calendar nextAlarm = globalManager.getNextAlarm(clock);

        assertThat("Year", nextAlarm.get(Calendar.YEAR), is(DayTest.YEAR));
        assertThat("Month", nextAlarm.get(Calendar.MONTH), is(Calendar.JANUARY));
        assertThat("Date", nextAlarm.get(Calendar.DAY_OF_MONTH), is(2));
        assertThat("Hour", nextAlarm.get(Calendar.HOUR_OF_DAY), is(DayTest.HOUR_DEFAULT));
        assertThat("Minute", nextAlarm.get(Calendar.MINUTE), is(DayTest.MINUTE_DEFAULT));
    }

    @Test
    public void t10b_EasterSunday_CZ() {
        globalManager.saveHoliday("CZ");

        Calendar date = new GregorianCalendar(DayTest.YEAR, Calendar.MARCH, 27, DayTest.HOUR, DayTest.MINUTE);
        FixedClock clock = new FixedClock(date);

        Calendar nextAlarm = globalManager.getNextAlarm(clock);

        assertThat("Year", nextAlarm.get(Calendar.YEAR), is(DayTest.YEAR));
        assertThat("Month", nextAlarm.get(Calendar.MONTH), is(Calendar.MARCH));
        assertThat("Date", nextAlarm.get(Calendar.DAY_OF_MONTH), is(29));
        assertThat("Hour", nextAlarm.get(Calendar.HOUR_OF_DAY), is(DayTest.HOUR_DEFAULT));
        assertThat("Minute", nextAlarm.get(Calendar.MINUTE), is(DayTest.MINUTE_DEFAULT));
    }

    @Test
    public void t10c_EasterMonday_CZ() {
        globalManager.saveHoliday("CZ");

        Calendar date = new GregorianCalendar(DayTest.YEAR, Calendar.MARCH, 28, DayTest.HOUR, DayTest.MINUTE);
        FixedClock clock = new FixedClock(date);

        Calendar nextAlarm = globalManager.getNextAlarm(clock);

        assertThat("Year", nextAlarm.get(Calendar.YEAR), is(DayTest.YEAR));
        assertThat("Month", nextAlarm.get(Calendar.MONTH), is(Calendar.MARCH));
        assertThat("Date", nextAlarm.get(Calendar.DAY_OF_MONTH), is(29));
        assertThat("Hour", nextAlarm.get(Calendar.HOUR_OF_DAY), is(DayTest.HOUR_DEFAULT));
        assertThat("Minute", nextAlarm.get(Calendar.MINUTE), is(DayTest.MINUTE_DEFAULT));
    }

    @Test
    public void t10d_LabourDay_CZ() {
        globalManager.saveHoliday("CZ");

        Calendar date = new GregorianCalendar(DayTest.YEAR, Calendar.MAY, 1, DayTest.HOUR, DayTest.MINUTE);
        FixedClock clock = new FixedClock(date);

        Calendar nextAlarm = globalManager.getNextAlarm(clock);

        assertThat("Year", nextAlarm.get(Calendar.YEAR), is(DayTest.YEAR));
        assertThat("Month", nextAlarm.get(Calendar.MONTH), is(Calendar.MAY));
        assertThat("Date", nextAlarm.get(Calendar.DAY_OF_MONTH), is(2));
        assertThat("Hour", nextAlarm.get(Calendar.HOUR_OF_DAY), is(DayTest.HOUR_DEFAULT));
        assertThat("Minute", nextAlarm.get(Calendar.MINUTE), is(DayTest.MINUTE_DEFAULT));
    }

    @Test
    public void t11_everyDayWIthHolidays_CZ() {
        globalManager.saveHoliday("CZ");

        Calendar from = new GregorianCalendar(DayTest.YEAR, Calendar.JANUARY, 1);
        Calendar to = new GregorianCalendar(DayTest.YEAR + 1, Calendar.JANUARY, 1);
        for (Calendar date = (Calendar) from.clone(); date.before(to); date.add(Calendar.DATE, 1)) {
            FixedClock clock = new FixedClock(date);

            Calendar nextAlarm = globalManager.getNextAlarm(clock);

            String str = " on " + date.getTime();

            if (isHoliday(date, holidays_Czech)) {
                if (sameDate(nextAlarm, date)) {
                    throw new AssertionError("Alarm is on the same date although it's holiday on " + nextAlarm.getTime().toString());
                }
                if (nextAlarm.before(date)) {
                    throw new AssertionError("Alarm on " + nextAlarm.getTime() + " is before current time on " + nextAlarm.getTime().toString());
                }
            } else {
                assertThat("Year" + str, nextAlarm.get(Calendar.YEAR), is(date.get(Calendar.YEAR)));
                assertThat("Month" + str, nextAlarm.get(Calendar.MONTH), is(date.get(Calendar.MONTH)));
                assertThat("Date" + str, nextAlarm.get(Calendar.DAY_OF_MONTH), is(date.get(Calendar.DAY_OF_MONTH)));
            }
            assertThat("Hour" + str, nextAlarm.get(Calendar.HOUR_OF_DAY), is(DayTest.HOUR_DEFAULT));
            assertThat("Minute" + str, nextAlarm.get(Calendar.MINUTE), is(DayTest.MINUTE_DEFAULT));
        }
    }

}