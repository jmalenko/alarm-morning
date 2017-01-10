package cz.jaro.alarmmorning.model;

import org.junit.Test;

import java.util.Calendar;
import java.util.GregorianCalendar;

import cz.jaro.alarmmorning.Analytics;
import cz.jaro.alarmmorning.FixedTimeTest;
import cz.jaro.alarmmorning.clock.Clock;
import cz.jaro.alarmmorning.clock.FixedClock;

import static cz.jaro.alarmmorning.calendar.CalendarUtils.beginningOfTomorrow;
import static cz.jaro.alarmmorning.calendar.CalendarUtils.nextSameDayOfWeek;
import static cz.jaro.alarmmorning.model.GlobalManager1NextAlarm0NoAlarmTest.RANGE;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

/**
 * Tests when there is only one default enabled. Holiday is not defined.
 */
public class GlobalManager1NextAlarm2DefaultTest extends FixedTimeTest {

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

        Clock clock = globalManager.clock();

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

        Calendar tomorrowBeginning = beginningOfTomorrow(date);
        Calendar nextSameDayOfWeek = nextSameDayOfWeek(tomorrowBeginning, dayOfWeek());

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

        Calendar nextSameDayOfWeekDay = nextSameDayOfWeek(date, dayOfWeek());

        assertThat("Year", nextAlarm.get(Calendar.YEAR), is(nextSameDayOfWeekDay.get(Calendar.YEAR)));
        assertThat("Month", nextAlarm.get(Calendar.MONTH), is(nextSameDayOfWeekDay.get(Calendar.MONTH)));
        assertThat("Date", nextAlarm.get(Calendar.DAY_OF_MONTH), is(nextSameDayOfWeekDay.get(Calendar.DAY_OF_MONTH)));
        assertThat("Hour", nextAlarm.get(Calendar.HOUR_OF_DAY), is(DayTest.HOUR_DEFAULT));
        assertThat("Minute", nextAlarm.get(Calendar.MINUTE), is(DayTest.MINUTE_DEFAULT));
    }

    @Test
    public void t40_everyDay() {
        setDefaultAlarm();

        Calendar now = globalManager.clock().now();
        for (int i = -RANGE; i <= RANGE; i++) {
            Calendar date = (Calendar) now.clone();
            date.add(Calendar.DATE, i);

            FixedClock clock = new FixedClock(date);

            Calendar nextAlarm = globalManager.getNextAlarm(clock);

            Calendar nextSameDayOfWeekDay = nextSameDayOfWeek(date, dayOfWeek());
            String str = " on " + date.getTime();

            assertThat("Year" + str, nextAlarm.get(Calendar.YEAR), is(nextSameDayOfWeekDay.get(Calendar.YEAR)));
            assertThat("Month" + str, nextAlarm.get(Calendar.MONTH), is(nextSameDayOfWeekDay.get(Calendar.MONTH)));
            assertThat("Date" + str, nextAlarm.get(Calendar.DAY_OF_MONTH), is(nextSameDayOfWeekDay.get(Calendar.DAY_OF_MONTH)));
            assertThat("Hour" + str, nextAlarm.get(Calendar.HOUR_OF_DAY), is(DayTest.HOUR_DEFAULT));
            assertThat("Minute" + str, nextAlarm.get(Calendar.MINUTE), is(DayTest.MINUTE_DEFAULT));
        }
    }

    static int dayOfWeek() {
        Calendar dateDefault = new GregorianCalendar(DayTest.YEAR, DayTest.MONTH, DayTest.DAY, DayTest.HOUR, DayTest.MINUTE);
        return dateDefault.get(Calendar.DAY_OF_WEEK);
    }
}