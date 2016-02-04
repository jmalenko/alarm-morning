package cz.jaro.alarmmorning.model;

import org.junit.Before;
import org.junit.Test;

import java.util.Calendar;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class DefaultsTest {

    private Defaults defaults;

    @Before
    public void before() {
        defaults = new Defaults();
        defaults.setDayOfWeek(Calendar.MONDAY);
        defaults.setHour(7);
        defaults.setMinute(0);
    }

    @Test
    public void reverse_fromDisabled() {
        defaults.setState(Defaults.STATE_DISABLED);

        defaults.reverse();

        assertThat(defaults.getState(), is(Defaults.STATE_ENABLED));

        defaults.reverse();

        assertThat(defaults.getState(), is(Defaults.STATE_DISABLED));
    }

    @Test
    public void reverse_fromEnabled() {
        defaults.setState(Defaults.STATE_ENABLED);

        defaults.reverse();

        assertThat(defaults.getState(), is(Defaults.STATE_DISABLED));
    }

    @Test
    public void reverse_multiple() {
        Defaults defaults = new Defaults();
        defaults.setState(Defaults.STATE_ENABLED);

        defaults.reverse();
        defaults.reverse();

        assertThat(defaults.getState(), is(Defaults.STATE_ENABLED));
    }

}
