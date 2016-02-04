package cz.jaro.alarmmorning.model;

import org.junit.Before;
import org.junit.Test;

import java.util.Calendar;
import java.util.GregorianCalendar;

import cz.jaro.alarmmorning.clock.Clock;
import cz.jaro.alarmmorning.clock.FixedClock;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class Day2Test {

    private Defaults defaults;
    private Day day;

    @Before
    public void before() {
        defaults = new Defaults();
        defaults.setDayOfWeek(Calendar.MONDAY);
        defaults.setHour(7);
        defaults.setMinute(0);

//        day = new Day();
        Clock clock = new FixedClock(new GregorianCalendar(2016, 2, 1, 8, 1));
        day = new Day(clock);
        day.setDate(new GregorianCalendar(2016, 2, 1)); // February 2016 starts with Monday
        day.setHour(8);
        day.setMinute(1);
        day.setDefaults(defaults);
    }

    @Test
    public void DefaultDisabledDayDefault() {
        defaults.setState(AlarmDataSource.DEFAULT_STATE_DISABLED);
        day.setState(AlarmDataSource.DAY_STATE_DEFAULT);

        assertThat(day.isEnabled(), is(false));
        assertThat(day.getHourX(), is(7));
        assertThat(day.getMinuteX(), is(0));
        assertThat(day.sameAsDefault(), is(true));

        day.reverse();
        assertThat(day.getState(), is(AlarmDataSource.DAY_STATE_ENABLED));

        // isPassed
        // isNextAlarm
        // getTimeToRing
    }

    @Test
    public void DefaultDisabledDayEnabled() {
        defaults.setState(AlarmDataSource.DEFAULT_STATE_DISABLED);
        day.setState(AlarmDataSource.DAY_STATE_ENABLED);

        assertThat(day.isEnabled(), is(true));
        assertThat(day.getHourX(), is(8));
        assertThat(day.getMinuteX(), is(1));
        assertThat(day.sameAsDefault(), is(false));

        day.reverse();
        assertThat(day.getState(), is(AlarmDataSource.DAY_STATE_DISABLED));
    }

    @Test
    public void DefaultDisabledDayDisabled() {
        defaults.setState(AlarmDataSource.DEFAULT_STATE_DISABLED);
        day.setState(AlarmDataSource.DAY_STATE_DISABLED);

        assertThat(day.isEnabled(), is(false));
        assertThat(day.getHourX(), is(8));
        assertThat(day.getMinuteX(), is(1));
        assertThat(day.sameAsDefault(), is(true));

        day.reverse();
        assertThat(day.getState(), is(AlarmDataSource.DAY_STATE_ENABLED));
    }

    @Test
    public void DefaultEnabledDayDefault() {
        defaults.setState(AlarmDataSource.DEFAULT_STATE_ENABLED);
        day.setState(AlarmDataSource.DAY_STATE_DEFAULT);

        assertThat(day.isEnabled(), is(true));
        assertThat(day.getHourX(), is(7));
        assertThat(day.getMinuteX(), is(0));
        assertThat(day.sameAsDefault(), is(true));

        day.reverse();
        assertThat(day.getState(), is(AlarmDataSource.DAY_STATE_DISABLED));
    }

    @Test
    public void DefaultEnabledDayEnabled() {
        defaults.setState(AlarmDataSource.DEFAULT_STATE_ENABLED);
        day.setState(AlarmDataSource.DAY_STATE_ENABLED);

        assertThat(day.isEnabled(), is(true));
        assertThat(day.getHourX(), is(8));
        assertThat(day.getMinuteX(), is(1));
        assertThat(day.sameAsDefault(), is(false));

        day.reverse();
        assertThat(day.getState(), is(AlarmDataSource.DAY_STATE_DISABLED));
    }

    @Test
    public void DefaultEnabledDayDisabled() {
        defaults.setState(AlarmDataSource.DEFAULT_STATE_ENABLED);
        day.setState(AlarmDataSource.DAY_STATE_DISABLED);

        assertThat(day.isEnabled(), is(false));
        assertThat(day.getHourX(), is(8));
        assertThat(day.getMinuteX(), is(1));
        assertThat(day.sameAsDefault(), is(false));

        day.reverse();
        assertThat(day.getState(), is(AlarmDataSource.DAY_STATE_ENABLED));
    }

    @Test
    public void isPsssed() {
        day.setState(AlarmDataSource.DAY_STATE_ENABLED);

        assertThat(day.day.isPassed(), is(true));
    }

}