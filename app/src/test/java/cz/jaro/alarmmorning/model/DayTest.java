package cz.jaro.alarmmorning.model;

import org.junit.Before;
import org.junit.Test;

import java.util.Calendar;
import java.util.GregorianCalendar;

import cz.jaro.alarmmorning.clock.Clock;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

/**
 * This tests do not depend on {@link Clock}.
 */
public class DayTest {

    // February 2016 starts with Monday
    public static final int YEAR = 2016;
    public static final int MONTH = Calendar.FEBRUARY;
    public static final int DAY = 1;
    public static final int DAY_OF_WEEK = Calendar.MONDAY;

    public static final int HOUR_DAY = 8;
    public static final int MINUTE_DAY = 1;

    public static final int HOUR_DEFAULT = 7;
    public static final int MINUTE_DEFAULT = 0;

    private Defaults defaults;
    private Day day;

    @Before
    public void before() {
        defaults = new Defaults();
        defaults.setDayOfWeek(DAY_OF_WEEK);
        defaults.setHour(HOUR_DEFAULT);
        defaults.setMinute(MINUTE_DEFAULT);

        day = new Day();
        GregorianCalendar date = new GregorianCalendar(YEAR, MONTH, DAY);
        day.setDate(date);
        day.setHour(HOUR_DAY);
        day.setMinute(MINUTE_DAY);
        day.setDefaults(defaults);
    }

    @Test
    public void DefaultDisabledDayDefault() {
        defaults.setState(AlarmDataSource.DEFAULT_STATE_DISABLED);
        day.setState(AlarmDataSource.DAY_STATE_DEFAULT);

        assertThat(day.isEnabled(), is(false));
        assertThat(day.getHourX(), is(HOUR_DEFAULT));
        assertThat(day.getMinuteX(), is(MINUTE_DEFAULT));
        assertThat(day.sameAsDefault(), is(true));

        day.reverse();
        assertThat(day.getState(), is(AlarmDataSource.DAY_STATE_ENABLED));
    }

    @Test
    public void DefaultDisabledDayEnabled() {
        defaults.setState(AlarmDataSource.DEFAULT_STATE_DISABLED);
        day.setState(AlarmDataSource.DAY_STATE_ENABLED);

        assertThat(day.isEnabled(), is(true));
        assertThat(day.getHourX(), is(HOUR_DAY));
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
        assertThat(day.getHourX(), is(HOUR_DAY));
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
        assertThat(day.getHourX(), is(HOUR_DEFAULT));
        assertThat(day.getMinuteX(), is(MINUTE_DEFAULT));
        assertThat(day.sameAsDefault(), is(true));

        day.reverse();
        assertThat(day.getState(), is(AlarmDataSource.DAY_STATE_DISABLED));
    }

    @Test
    public void DefaultEnabledDayEnabled() {
        defaults.setState(AlarmDataSource.DEFAULT_STATE_ENABLED);
        day.setState(AlarmDataSource.DAY_STATE_ENABLED);

        assertThat(day.isEnabled(), is(true));
        assertThat(day.getHourX(), is(HOUR_DAY));
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
        assertThat(day.getHourX(), is(HOUR_DAY));
        assertThat(day.getMinuteX(), is(1));
        assertThat(day.sameAsDefault(), is(false));

        day.reverse();
        assertThat(day.getState(), is(AlarmDataSource.DAY_STATE_ENABLED));
    }

}