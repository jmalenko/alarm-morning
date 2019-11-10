package cz.jaro.alarmmorning.model;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

import cz.jaro.alarmmorning.Analytics;
import cz.jaro.alarmmorning.GlobalManager;

import static cz.jaro.alarmmorning.model.AlarmDataSource1Test.DAY;
import static cz.jaro.alarmmorning.model.AlarmDataSource1Test.HOUR_DAY;
import static cz.jaro.alarmmorning.model.AlarmDataSource1Test.MINUTE_DAY;
import static cz.jaro.alarmmorning.model.AlarmDataSource1Test.MONTH;
import static cz.jaro.alarmmorning.model.AlarmDataSource1Test.YEAR;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Tests with a database preset to particular scenario: there are several Days and defaults are off.
 */
@RunWith(AndroidJUnit4.class)
public class AlarmDataSource3Test {

    private static final int DAYS = 20;

    private GlobalManager globalManager;

    @Before
    public void before() {
        globalManager = GlobalManager.getInstance();
        globalManager.reset();

        for (int i = 0; i < DAYS; i++) {
            Day day = new Day();
            day.setState(Day.STATE_ENABLED);
            day.setDate(new GregorianCalendar(YEAR, MONTH, DAY + i));
            day.setHourDay(HOUR_DAY);
            day.setMinuteDay(MINUTE_DAY);

            Analytics analytics = new Analytics(Analytics.Channel.Activity, Analytics.ChannelName.Calendar);

            globalManager.modifyDayAlarm(day, analytics);
        }
    }

    @Test
    public void preConditions() {
        assertNotNull(globalManager);
    }

    @Test
    public void getAlarmsInPeriod_empty() {
        Calendar from = new GregorianCalendar(YEAR, MONTH - 1, DAY);
        Calendar to = new GregorianCalendar(YEAR, MONTH - 1, DAY + 10);
        List<AppAlarm> appAlarms = globalManager.getAlarmsInPeriod(from, to);

        assertTrue(appAlarms.isEmpty());
    }

    @Test
    public void getAlarmsInPeriod_empty2() {
        Calendar from = new GregorianCalendar(YEAR, MONTH, DAY, HOUR_DAY - 2, MINUTE_DAY);
        Calendar to = new GregorianCalendar(YEAR, MONTH, DAY, HOUR_DAY - 1, MINUTE_DAY);
        List<AppAlarm> appAlarms = globalManager.getAlarmsInPeriod(from, to);

        assertTrue(appAlarms.isEmpty());
    }

    @Test
    public void getAlarmsInPeriod_empty3() {
        Calendar from = new GregorianCalendar(YEAR, MONTH, DAY - 1, 0, 0);
        Calendar to = new GregorianCalendar(YEAR, MONTH, DAY, 0, 0);
        List<AppAlarm> appAlarms = globalManager.getAlarmsInPeriod(from, to);

        assertTrue(appAlarms.isEmpty());
    }

    @Test
    public void getAlarmsInPeriod_one_day() {
        Calendar from = new GregorianCalendar(YEAR, MONTH, DAY, 0, 0);
        Calendar to = new GregorianCalendar(YEAR, MONTH, DAY + 1, 0, 0);
        List<AppAlarm> appAlarms = globalManager.getAlarmsInPeriod(from, to);

        assertEquals(appAlarms.size(), 1);

        Calendar nextAlarm = appAlarms.get(0).getDateTime();
        assertEquals(YEAR, nextAlarm.get(Calendar.YEAR));
        assertEquals(MONTH, nextAlarm.get(Calendar.MONTH));
        assertEquals(DAY, nextAlarm.get(Calendar.DAY_OF_MONTH));
        assertEquals(HOUR_DAY, nextAlarm.get(Calendar.HOUR_OF_DAY));
        assertEquals(MINUTE_DAY, nextAlarm.get(Calendar.MINUTE));
        assertEquals(0, nextAlarm.get(Calendar.SECOND));
        assertEquals(0, nextAlarm.get(Calendar.MILLISECOND));
    }

    @Test
    public void getAlarmsInPeriod_one_minute() {
        Calendar from = new GregorianCalendar(YEAR, MONTH, DAY, HOUR_DAY, MINUTE_DAY - 1);
        Calendar to = new GregorianCalendar(YEAR, MONTH, DAY, HOUR_DAY, MINUTE_DAY + 1);
        List<AppAlarm> appAlarms = globalManager.getAlarmsInPeriod(from, to);

        assertEquals(appAlarms.size(), 1);

        Calendar nextAlarm = appAlarms.get(0).getDateTime();
        assertEquals(YEAR, nextAlarm.get(Calendar.YEAR));
        assertEquals(MONTH, nextAlarm.get(Calendar.MONTH));
        assertEquals(DAY, nextAlarm.get(Calendar.DAY_OF_MONTH));
        assertEquals(HOUR_DAY, nextAlarm.get(Calendar.HOUR_OF_DAY));
        assertEquals(MINUTE_DAY, nextAlarm.get(Calendar.MINUTE));
        assertEquals(0, nextAlarm.get(Calendar.SECOND));
        assertEquals(0, nextAlarm.get(Calendar.MILLISECOND));
    }

    @Test
    public void getAlarmsInPeriod_one_leftBorder() {
        Calendar from = new GregorianCalendar(YEAR, MONTH, DAY, HOUR_DAY, MINUTE_DAY);
        Calendar to = new GregorianCalendar(YEAR, MONTH, DAY, HOUR_DAY, MINUTE_DAY + 1);
        List<AppAlarm> appAlarms = globalManager.getAlarmsInPeriod(from, to);

        assertEquals(appAlarms.size(), 1);

        Calendar nextAlarm = appAlarms.get(0).getDateTime();
        assertEquals(YEAR, nextAlarm.get(Calendar.YEAR));
        assertEquals(MONTH, nextAlarm.get(Calendar.MONTH));
        assertEquals(DAY, nextAlarm.get(Calendar.DAY_OF_MONTH));
        assertEquals(HOUR_DAY, nextAlarm.get(Calendar.HOUR_OF_DAY));
        assertEquals(MINUTE_DAY, nextAlarm.get(Calendar.MINUTE));
        assertEquals(0, nextAlarm.get(Calendar.SECOND));
        assertEquals(0, nextAlarm.get(Calendar.MILLISECOND));
    }

    @Test
    public void getAlarmsInPeriod_one_rightBorder() {
        Calendar from = new GregorianCalendar(YEAR, MONTH, DAY, HOUR_DAY, MINUTE_DAY - 1);
        Calendar to = new GregorianCalendar(YEAR, MONTH, DAY, HOUR_DAY, MINUTE_DAY);
        List<AppAlarm> appAlarms = globalManager.getAlarmsInPeriod(from, to);

        assertEquals(appAlarms.size(), 1);

        Calendar nextAlarm = appAlarms.get(0).getDateTime();
        assertEquals(YEAR, nextAlarm.get(Calendar.YEAR));
        assertEquals(MONTH, nextAlarm.get(Calendar.MONTH));
        assertEquals(DAY, nextAlarm.get(Calendar.DAY_OF_MONTH));
        assertEquals(HOUR_DAY, nextAlarm.get(Calendar.HOUR_OF_DAY));
        assertEquals(MINUTE_DAY, nextAlarm.get(Calendar.MINUTE));
        assertEquals(0, nextAlarm.get(Calendar.SECOND));
        assertEquals(0, nextAlarm.get(Calendar.MILLISECOND));
    }

    @Test
    public void getAlarmsInPeriod_two_day() {
        Calendar from = new GregorianCalendar(YEAR, MONTH, DAY, 0, 0);
        Calendar to = new GregorianCalendar(YEAR, MONTH, DAY + 2, 0, 0);
        List<AppAlarm> appAlarms = globalManager.getAlarmsInPeriod(from, to);

        assertEquals(appAlarms.size(), 2);

        Calendar nextAlarm = appAlarms.get(0).getDateTime();
        assertEquals(YEAR, nextAlarm.get(Calendar.YEAR));
        assertEquals(MONTH, nextAlarm.get(Calendar.MONTH));
        assertEquals(DAY, nextAlarm.get(Calendar.DAY_OF_MONTH));
        assertEquals(HOUR_DAY, nextAlarm.get(Calendar.HOUR_OF_DAY));
        assertEquals(MINUTE_DAY, nextAlarm.get(Calendar.MINUTE));
        assertEquals(0, nextAlarm.get(Calendar.SECOND));
        assertEquals(0, nextAlarm.get(Calendar.MILLISECOND));

        Calendar nextAlarm2 = appAlarms.get(1).getDateTime();
        assertEquals(YEAR, nextAlarm2.get(Calendar.YEAR));
        assertEquals(MONTH, nextAlarm2.get(Calendar.MONTH));
        assertEquals(DAY + 1, nextAlarm2.get(Calendar.DAY_OF_MONTH));
        assertEquals(HOUR_DAY, nextAlarm2.get(Calendar.HOUR_OF_DAY));
        assertEquals(MINUTE_DAY, nextAlarm2.get(Calendar.MINUTE));
        assertEquals(0, nextAlarm2.get(Calendar.SECOND));
        assertEquals(0, nextAlarm2.get(Calendar.MILLISECOND));
    }

    @Test
    public void getAlarmsInPeriod_three_day() {
        Calendar from = new GregorianCalendar(YEAR, MONTH, DAY, 0, 0);
        Calendar to = new GregorianCalendar(YEAR, MONTH, DAY + 3, 0, 0);
        List<AppAlarm> appAlarms = globalManager.getAlarmsInPeriod(from, to);

        assertEquals(appAlarms.size(), 3);

        Calendar nextAlarm = appAlarms.get(0).getDateTime();
        assertEquals(YEAR, nextAlarm.get(Calendar.YEAR));
        assertEquals(MONTH, nextAlarm.get(Calendar.MONTH));
        assertEquals(DAY, nextAlarm.get(Calendar.DAY_OF_MONTH));
        assertEquals(HOUR_DAY, nextAlarm.get(Calendar.HOUR_OF_DAY));
        assertEquals(MINUTE_DAY, nextAlarm.get(Calendar.MINUTE));
        assertEquals(0, nextAlarm.get(Calendar.SECOND));
        assertEquals(0, nextAlarm.get(Calendar.MILLISECOND));

        Calendar nextAlarm2 = appAlarms.get(1).getDateTime();
        assertEquals(YEAR, nextAlarm2.get(Calendar.YEAR));
        assertEquals(MONTH, nextAlarm2.get(Calendar.MONTH));
        assertEquals(DAY + 1, nextAlarm2.get(Calendar.DAY_OF_MONTH));
        assertEquals(HOUR_DAY, nextAlarm2.get(Calendar.HOUR_OF_DAY));
        assertEquals(MINUTE_DAY, nextAlarm2.get(Calendar.MINUTE));
        assertEquals(0, nextAlarm2.get(Calendar.SECOND));
        assertEquals(0, nextAlarm2.get(Calendar.MILLISECOND));

        Calendar nextAlarm3 = appAlarms.get(2).getDateTime();
        assertEquals(YEAR, nextAlarm3.get(Calendar.YEAR));
        assertEquals(MONTH, nextAlarm3.get(Calendar.MONTH));
        assertEquals(DAY + 2, nextAlarm3.get(Calendar.DAY_OF_MONTH));
        assertEquals(HOUR_DAY, nextAlarm3.get(Calendar.HOUR_OF_DAY));
        assertEquals(MINUTE_DAY, nextAlarm3.get(Calendar.MINUTE));
        assertEquals(0, nextAlarm3.get(Calendar.SECOND));
        assertEquals(0, nextAlarm3.get(Calendar.MILLISECOND));
    }

    @Test
    public void getAlarmsInPeriod_all() {
        Calendar from = new GregorianCalendar(YEAR - 1, MONTH, DAY);
        Calendar to = new GregorianCalendar(YEAR + 1, MONTH, DAY);
        List<AppAlarm> appAlarms = globalManager.getAlarmsInPeriod(from, to);

        assertEquals(appAlarms.size(), DAYS);

        for (int i = 0; i < DAYS; i++) {
            Calendar nextAlarm = appAlarms.get(i).getDateTime();
            assertEquals(YEAR, nextAlarm.get(Calendar.YEAR));
            assertEquals(MONTH, nextAlarm.get(Calendar.MONTH));
            assertEquals(DAY + i, nextAlarm.get(Calendar.DAY_OF_MONTH));
            assertEquals(HOUR_DAY, nextAlarm.get(Calendar.HOUR_OF_DAY));
            assertEquals(MINUTE_DAY, nextAlarm.get(Calendar.MINUTE));
            assertEquals(0, nextAlarm.get(Calendar.SECOND));
            assertEquals(0, nextAlarm.get(Calendar.MILLISECOND));
        }
    }

}
