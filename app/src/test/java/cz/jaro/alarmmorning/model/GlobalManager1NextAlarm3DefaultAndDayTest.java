package cz.jaro.alarmmorning.model;

import org.junit.Test;

import java.util.Calendar;
import java.util.GregorianCalendar;

import cz.jaro.alarmmorning.Analytics;
import cz.jaro.alarmmorning.FixedTimeTest;
import cz.jaro.alarmmorning.clock.FixedClock;

import static cz.jaro.alarmmorning.calendar.CalendarUtils.beginningOfTomorrow;
import static cz.jaro.alarmmorning.calendar.CalendarUtils.nextSameDayOfWeek;
import static cz.jaro.alarmmorning.model.GlobalManager1NextAlarm2DefaultTest.dayOfWeek;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

/**
 * Tests when there is one default enabled and a day that disables the alarm (which was set by default). Holiday is not defined.
 */
public class GlobalManager1NextAlarm3DefaultAndDayTest extends FixedTimeTest {

    private void setDefaultAlarm() {
        Calendar date = new GregorianCalendar(DayTest.YEAR, DayTest.MONTH, DayTest.DAY, DayTest.HOUR, DayTest.MINUTE);
        int dayOfWeek = date.get(Calendar.DAY_OF_WEEK);

        Defaults defaults = new Defaults();
        defaults.setDayOfWeek(dayOfWeek);
        defaults.setState(Defaults.STATE_ENABLED);
        defaults.setHour(DayTest.HOUR_DEFAULT);
        defaults.setMinute(DayTest.MINUTE_DEFAULT);

        Analytics analytics = new Analytics(Analytics.Channel.Test, Analytics.ChannelName.Defaults);

        globalManager.modifyDefault(defaults, analytics);
    }

    private void setAlarmDisabledToday() {
        Calendar date = new GregorianCalendar(DayTest.YEAR, DayTest.MONTH, DayTest.DAY, DayTest.HOUR, DayTest.MINUTE);

        Day day = new Day();
        day.setDate(date);
        day.setState(Day.STATE_DISABLED);
        day.setHourDay(DayTest.HOUR_DAY);
        day.setMinuteDay(DayTest.MINUTE_DAY);

        Defaults defaults = new Defaults();
        int dayOfWeek = date.get(Calendar.DAY_OF_WEEK);
        defaults.setDayOfWeek(dayOfWeek);
        defaults.setState(Defaults.STATE_DISABLED);

        day.setDefaults(defaults);

        Analytics analytics = new Analytics(Analytics.Channel.Test, Analytics.ChannelName.Calendar);

        globalManager.modifyDayAlarm(day, analytics);
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

        Calendar tomorrowBeginning = beginningOfTomorrow(date);
        Calendar nextSameDayOfWeek = nextSameDayOfWeek(tomorrowBeginning, dayOfWeek());

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

        Calendar tomorrowBeginning = beginningOfTomorrow(date);
        Calendar nextSameDayOfWeek = nextSameDayOfWeek(tomorrowBeginning, dayOfWeek());

        assertThat("Year", nextAlarm.get(Calendar.YEAR), is(nextSameDayOfWeek.get(Calendar.YEAR)));
        assertThat("Month", nextAlarm.get(Calendar.MONTH), is(nextSameDayOfWeek.get(Calendar.MONTH)));
        assertThat("Date", nextAlarm.get(Calendar.DAY_OF_MONTH), is(nextSameDayOfWeek.get(Calendar.DAY_OF_MONTH)));
        assertThat("Hour", nextAlarm.get(Calendar.HOUR_OF_DAY), is(DayTest.HOUR_DEFAULT));
        assertThat("Minute", nextAlarm.get(Calendar.MINUTE), is(DayTest.MINUTE_DEFAULT));
    }

}