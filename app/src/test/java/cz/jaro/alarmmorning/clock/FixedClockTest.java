package cz.jaro.alarmmorning.clock;

import org.junit.Before;
import org.junit.Test;

import java.util.Calendar;
import java.util.GregorianCalendar;

import cz.jaro.alarmmorning.model.AlarmDataSource;
import cz.jaro.alarmmorning.model.Day;
import cz.jaro.alarmmorning.model.Defaults;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class FixedClockTest {

    Calendar now;
    FixedClock fixedClock;

    @Before
    public void before() {
        now = new GregorianCalendar(2016, 2, 1, 5, 0, 0);

        fixedClock = new FixedClock(now);
    }

    @Test
    public void now() {
        assertThat(fixedClock.now(), is(now));
    }

    @Test
    public void addMinute() {
        assertThat(fixedClock.now(), is(now));

        now.add(Calendar.MINUTE, 1);

        fixedClock.addMinute(1);

        assertThat(fixedClock.now(), is(now));
    }

}
