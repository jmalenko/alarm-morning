package cz.jaro.alarmmorning.clock;

import org.junit.Before;
import org.junit.Test;

import java.util.Calendar;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class SystemClockTest {

    SystemClock systemClock;

    @Before
    public void before() {
        systemClock = new SystemClock();
    }

    @Test
    public void now() {
        Calendar calendar1 = Calendar.getInstance();

        Calendar now = systemClock.now();

        Calendar calendar2 = Calendar.getInstance();

        // all the calls may return the same smallest representable time interval
        assertThat(calendar1.after(now), is(false));
        assertThat(now.after(calendar2), is(false));
    }

}
