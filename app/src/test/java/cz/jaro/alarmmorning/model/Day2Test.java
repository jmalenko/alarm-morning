package cz.jaro.alarmmorning.model;

import org.junit.Before;
import org.junit.Test;

import java.util.GregorianCalendar;

import cz.jaro.alarmmorning.clock.Clock;
import cz.jaro.alarmmorning.clock.FixedClock;

import static cz.jaro.alarmmorning.model.DayTest.DAY;
import static cz.jaro.alarmmorning.model.DayTest.HOUR_DAY;
import static cz.jaro.alarmmorning.model.DayTest.MINUTE_DAY;
import static cz.jaro.alarmmorning.model.DayTest.MONTH;
import static cz.jaro.alarmmorning.model.DayTest.YEAR;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

/**
 * This tests depend on {@link Clock}.
 */
public class Day2Test {

    private Day day;
    private FixedClock clock;

    @Before
    public void before() {
        clock = new FixedClock(new GregorianCalendar(YEAR, MONTH, DAY, HOUR_DAY, MINUTE_DAY));

        day = new Day();
        day.setState(Day.STATE_ENABLED);
        day.setDate(new GregorianCalendar(YEAR, MONTH, DAY));
        day.setHourDay(HOUR_DAY);
        day.setMinuteDay(MINUTE_DAY);
    }

    @Test
    public void beforeX() {
        clock.addMinute(-1);

        assertThat(day.isPassed(clock), is(false));
        assertThat(day.getTimeToRing(clock), is(60000L));
    }

    @Test
    public void on() {
        assertThat(day.isPassed(clock), is(true));
        assertThat(day.getTimeToRing(clock), is(0L));
    }

    @Test
    public void after() {
        clock.addMinute(1);

        assertThat(day.isPassed(clock), is(true));
        assertThat(day.getTimeToRing(clock), is(-60000L));
    }

}