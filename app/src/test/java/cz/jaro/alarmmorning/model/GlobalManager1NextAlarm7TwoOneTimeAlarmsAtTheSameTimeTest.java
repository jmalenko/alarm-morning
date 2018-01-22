package cz.jaro.alarmmorning.model;

import org.junit.Before;
import org.junit.Test;

import java.util.Calendar;
import java.util.GregorianCalendar;

import cz.jaro.alarmmorning.Analytics;
import cz.jaro.alarmmorning.FixedTimeTest;
import cz.jaro.alarmmorning.clock.Clock;
import cz.jaro.alarmmorning.clock.FixedClock;

import static cz.jaro.alarmmorning.model.GlobalManager1NextAlarm0NoAlarmTest.RANGE;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

/**
 * Tests when there are two one-time alarms at the same time.
 * <p>
 * Note, that the behavior should be the same as with one alarm. On he technical level, this class is a copy of
 * {@link GlobalManager1NextAlarm5OneTimeAlarmTest} class, but with two alarms.
 */
public class GlobalManager1NextAlarm7TwoOneTimeAlarmsAtTheSameTimeTest extends FixedTimeTest {

    private void setAlarmToToday() {
        Calendar date = new GregorianCalendar(DayTest.YEAR, DayTest.MONTH, DayTest.DAY, DayTest.HOUR, DayTest.MINUTE);

        OneTimeAlarm oneTimeAlarm = new OneTimeAlarm();
        oneTimeAlarm.setDate(date);
        oneTimeAlarm.setHour(DayTest.HOUR_DAY);
        oneTimeAlarm.setMinute(DayTest.MINUTE_DAY);

        Analytics analytics = new Analytics(Analytics.Channel.Test, Analytics.ChannelName.Calendar);

        globalManager.saveOneTimeAlarm(oneTimeAlarm, analytics);
    }

    @Before
    public void before() {
        super.before();

        setAlarmToToday();
        setAlarmToToday();
    }

    @Test
    public void t10_alarmToday() {
        Clock clock = globalManager.clock();
        Calendar nextAlarm = globalManager.getNextAlarm(clock);

        assertThat("Year", nextAlarm.get(Calendar.YEAR), is(DayTest.YEAR));
        assertThat("Month", nextAlarm.get(Calendar.MONTH), is(DayTest.MONTH));
        assertThat("Date", nextAlarm.get(Calendar.DAY_OF_MONTH), is(DayTest.DAY));
        assertThat("Hour", nextAlarm.get(Calendar.HOUR_OF_DAY), is(DayTest.HOUR_DAY));
        assertThat("Minute", nextAlarm.get(Calendar.MINUTE), is(DayTest.MINUTE_DAY));
    }

    @Test
    public void t11_justBeforeAlarm() {
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
        Calendar date = new GregorianCalendar(DayTest.YEAR, DayTest.MONTH, DayTest.DAY, DayTest.HOUR, DayTest.MINUTE);
        date.add(Calendar.DAY_OF_MONTH, -RANGE);
        FixedClock clock = new FixedClock(date);

        Calendar nextAlarm = globalManager.getNextAlarm(clock);

        // Horizon doesn't apply to one-time alarms
        assertThat("Year", nextAlarm.get(Calendar.YEAR), is(DayTest.YEAR));
        assertThat("Month", nextAlarm.get(Calendar.MONTH), is(DayTest.MONTH));
        assertThat("Date", nextAlarm.get(Calendar.DAY_OF_MONTH), is(DayTest.DAY));
        assertThat("Hour", nextAlarm.get(Calendar.HOUR_OF_DAY), is(DayTest.HOUR_DAY));
        assertThat("Minute", nextAlarm.get(Calendar.MINUTE), is(DayTest.MINUTE_DAY));
    }

    @Test
    public void t23_before() {
        Calendar now = globalManager.clock().now();
        for (int i = -RANGE; i <= 0; i++) {
            Calendar date = (Calendar) now.clone();
            date.add(Calendar.DATE, i);

            FixedClock clock = new FixedClock(date);

            Calendar nextAlarm = globalManager.getNextAlarm(clock);

            String str = " on " + date.getTime();

            // Horizon doesn't apply to one-time alarms
            assertThat("Year" + str, nextAlarm.get(Calendar.YEAR), is(DayTest.YEAR));
            assertThat("Month" + str, nextAlarm.get(Calendar.MONTH), is(DayTest.MONTH));
            assertThat("Date" + str, nextAlarm.get(Calendar.DAY_OF_MONTH), is(DayTest.DAY));
            assertThat("Hour" + str, nextAlarm.get(Calendar.HOUR_OF_DAY), is(DayTest.HOUR_DAY));
            assertThat("Minute" + str, nextAlarm.get(Calendar.MINUTE), is(DayTest.MINUTE_DAY));
        }
    }

    @Test
    public void t31_justAfterAlarm() {
        Calendar date = new GregorianCalendar(DayTest.YEAR, DayTest.MONTH, DayTest.DAY, DayTest.HOUR_DAY, DayTest.MINUTE_DAY);
        date.add(Calendar.SECOND, 1); // 1 second after alarm
        FixedClock clock = new FixedClock(date);

        Calendar nextAlarm = globalManager.getNextAlarm(clock);

        assertNull(nextAlarm);
    }

    @Test
    public void t32_after() {
        Calendar now = globalManager.clock().now();
        for (int i = 1; i <= RANGE; i++) {
            Calendar date = (Calendar) now.clone();
            date.add(Calendar.DATE, i);

            FixedClock clock = new FixedClock(date);

            Calendar nextAlarm = globalManager.getNextAlarm(clock);

            assertNull(nextAlarm);
        }
    }

}
