package cz.jaro.alarmmorning;

import org.junit.Test;

import java.security.InvalidParameterException;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * Test Ring RingActivity.
 */
public class RingActivityTest extends FixedTimeTest {

    @Test(expected = InvalidParameterException.class)
    public void positionDeltaToAngle_noMove() {
        RingActivity.positionDeltaToAngle(0, 0);
    }

    @Test
    public void positionDeltaToAngle() {
        assertThat("Clock 0", RingActivity.positionDeltaToAngle(0, -10), is(Math.PI * 2 / 4));
        assertThat("Clock 3", RingActivity.positionDeltaToAngle(10, 0), is(Math.PI * 0 / 4));
        assertThat("Clock 6", RingActivity.positionDeltaToAngle(0, 10), is(Math.PI * 6 / 4));
        assertThat("Clock 9", RingActivity.positionDeltaToAngle(-10, -0), is(Math.PI * 4 / 4));

        assertThat("Clock 1.5", RingActivity.positionDeltaToAngle(10, -10), is(Math.PI * 1 / 4));
        assertThat("Clock 4.5", RingActivity.positionDeltaToAngle(10, 10), is(Math.PI * 7 / 4));
        assertThat("Clock 7.5", RingActivity.positionDeltaToAngle(-10, 10), is(Math.PI * 5 / 4));
        assertThat("Clock 10.5", RingActivity.positionDeltaToAngle(-10, -10), is(Math.PI * 3 / 4));

        double actual, expected;

        actual = RingActivity.positionDeltaToAngle(4, -7);
        expected = Math.PI * 2 / 6;
        assertTrue("Clock 1", Math.abs(actual - expected) < 0.1);

        actual = RingActivity.positionDeltaToAngle(7, -4);
        expected = Math.PI * 1 / 6;
        assertTrue("Clock 2", Math.abs(actual - expected) < 0.1);

        actual = RingActivity.positionDeltaToAngle(7, 4);
        expected = Math.PI * 11 / 6;
        assertTrue("Clock 4", Math.abs(actual - expected) < 0.1);

        actual = RingActivity.positionDeltaToAngle(4, 7);
        expected = Math.PI * 10 / 6;
        assertTrue("Clock 5", Math.abs(actual - expected) < 0.1);

        actual = RingActivity.positionDeltaToAngle(-4, 7);
        expected = Math.PI * 8 / 6;
        assertTrue("Clock 7", Math.abs(actual - expected) < 0.1);

        actual = RingActivity.positionDeltaToAngle(-7, 4);
        expected = Math.PI * 7 / 6;
        assertTrue("Clock 8", Math.abs(actual - expected) < 0.1);

        actual = RingActivity.positionDeltaToAngle(-7, -4);
        expected = Math.PI * 5 / 6;
        assertTrue("Clock 10", Math.abs(actual - expected) < 0.1);

        actual = RingActivity.positionDeltaToAngle(-4, -7);
        expected = Math.PI * 4 / 6;
        assertTrue("Clock 11", Math.abs(actual - expected) < 0.1);

        // Around zero
        actual = RingActivity.positionDeltaToAngle(100, -1);
        expected = Math.PI * 1 / 180;
        assertTrue("Clock 0.1", Math.abs(actual - expected) < 0.1);

        actual = RingActivity.positionDeltaToAngle(100, 1);
        expected = Math.PI * 359 / 180;
        assertTrue("Clock 11.9", Math.abs(actual - expected) < 0.1);
    }

    @Test(expected = InvalidParameterException.class)
    public void positionDeltaToMinutes_noMove() {
        RingActivity.positionDeltaToMinutes(0, 0);
    }

    @Test
    public void positionDeltaToMinutes() {
        assertThat("Clock 0", RingActivity.positionDeltaToMinutes(0, -10), is(0.0));
        assertThat("Clock 3", RingActivity.positionDeltaToMinutes(10, 0), is(15.0));
        assertThat("Clock 6", RingActivity.positionDeltaToMinutes(0, 10), is(30.0));
        assertThat("Clock 9", RingActivity.positionDeltaToMinutes(-10, 0), is(45.0));

        assertThat("Clock 1.5", RingActivity.positionDeltaToMinutes(10, -10), is(7.5));
        assertThat("Clock 4.5", RingActivity.positionDeltaToMinutes(10, 10), is(22.5));
        assertThat("Clock 7.5", RingActivity.positionDeltaToMinutes(-10, 10), is(37.5));
        assertThat("Clock 10.5", RingActivity.positionDeltaToMinutes(-10, -10), is(52.5));
    }

}
