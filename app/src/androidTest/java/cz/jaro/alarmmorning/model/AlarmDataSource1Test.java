package cz.jaro.alarmmorning.model;

import android.support.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Calendar;
import java.util.GregorianCalendar;

import cz.jaro.alarmmorning.Analytics;
import cz.jaro.alarmmorning.GlobalManager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Tests without the need for resetting the database.
 */
@RunWith(AndroidJUnit4.class)
public class AlarmDataSource1Test {

    // February 2016 starts with Monday
    public static final int YEAR = 2016;
    public static final int MONTH = Calendar.FEBRUARY;
    public static final int DAY = 1;
    public static final int DAY_OF_WEEK = Calendar.MONDAY;

    public static final int HOUR_DAY = 8;
    public static final int MINUTE_DAY = 1;

    public static final int HOUR_DEFAULT = 7;
    public static final int MINUTE_DEFAULT = 0;

    private GlobalManager globalManager;
    private Analytics analytics;

    @Before
    public void before() throws Exception {
        globalManager = GlobalManager.getInstance();
        analytics = new Analytics(Analytics.Channel.Activity, Analytics.ChannelName.Calendar);
    }

    @Test
    public void defaults_2writes() {
        // save 1st object

        Defaults defaults1a = new Defaults();
        defaults1a.setState(Defaults.STATE_ENABLED);
        defaults1a.setDayOfWeek(DAY_OF_WEEK);
        defaults1a.setHour(HOUR_DEFAULT);
        defaults1a.setMinute(MINUTE_DEFAULT);

        globalManager.modifyDefault(defaults1a, analytics);

        Defaults defaults1b = globalManager.loadDefault(defaults1a.getDayOfWeek());

        assertEquals(defaults1a.getState(), defaults1b.getState());
        assertEquals(defaults1a.getDayOfWeek(), defaults1b.getDayOfWeek());
        assertEquals(defaults1a.getHour(), defaults1b.getHour());
        assertEquals(defaults1a.getMinute(), defaults1b.getMinute());

        // save 2nd object

        Defaults defaults2a = new Defaults();
        defaults2a.setState(Defaults.STATE_DISABLED);
        defaults2a.setDayOfWeek(DAY_OF_WEEK + 1);
        defaults2a.setHour(HOUR_DEFAULT + 1);
        defaults2a.setMinute(MINUTE_DEFAULT + 1);

        analytics = new Analytics(Analytics.Channel.Activity, Analytics.ChannelName.Calendar);

        globalManager.modifyDefault(defaults2a, analytics);

        Defaults defaults2b = globalManager.loadDefault(defaults2a.getDayOfWeek());

        assertEquals(defaults2a.getState(), defaults2b.getState());
        assertEquals(defaults2a.getDayOfWeek(), defaults2b.getDayOfWeek());
        assertEquals(defaults2a.getHour(), defaults2b.getHour());
        assertEquals(defaults2a.getMinute(), defaults2b.getMinute());
    }

    @Test
    public void day_2writes() {
        // save 1st object

        Day day1a = new Day();
        day1a.setState(Defaults.STATE_ENABLED);
        day1a.setDate(new GregorianCalendar(YEAR, MONTH, DAY));
        day1a.setHourDay(HOUR_DEFAULT);
        day1a.setMinuteDay(MINUTE_DEFAULT);

        globalManager.modifyDayAlarm(day1a, analytics);

        Day day1b = globalManager.loadDay(day1a.getDate());

        assertEquals(day1a.getState(), day1b.getState());
        assertEquals(day1a.getDate(), day1b.getDate());
        assertEquals(day1a.getHourDay(), day1b.getHourDay());
        assertEquals(day1a.getMinuteDay(), day1b.getMinuteDay());
        assertNotNull(day1b.getDefaults());

        // save 2nd object

        Day day2a = new Day();
        day2a.setState(Defaults.STATE_DISABLED);
        day2a.setDate(new GregorianCalendar(YEAR, MONTH, DAY + 1));
        day2a.setHourDay(HOUR_DEFAULT + 1);
        day2a.setMinuteDay(MINUTE_DEFAULT + 1);

        Analytics analytics2 = new Analytics(Analytics.Channel.Activity, Analytics.ChannelName.Calendar);

        globalManager.modifyDayAlarm(day2a, analytics2);

        Day day2b = globalManager.loadDay(day2a.getDate());

        assertEquals(day2a.getState(), day2b.getState());
        assertEquals(day2a.getDate(), day2b.getDate());
        assertEquals(day2a.getHourDay(), day2b.getHourDay());
        assertEquals(day2a.getMinuteDay(), day2b.getMinuteDay());
        assertNotNull(day2b.getDefaults());
    }

    @Test
    public void day_load_notStored() {
        Calendar dateWithoutRecord = new GregorianCalendar(YEAR - 1, MONTH, DAY);
        Day day = globalManager.loadDay(dateWithoutRecord);

        assertEquals(Day.STATE_RULE, day.getState());
        assertEquals(dateWithoutRecord.getTime().toString(), day.getDate().getTime().toString());
        assertEquals(dateWithoutRecord, day.getDate());
        assertEquals(Day.VALUE_UNSET, day.getHourDay());
        assertEquals(Day.VALUE_UNSET, day.getMinuteDay());
        assertNotNull(day.getDefaults());
    }
}
