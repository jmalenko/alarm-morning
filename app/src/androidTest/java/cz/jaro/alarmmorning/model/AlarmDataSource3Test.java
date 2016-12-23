package cz.jaro.alarmmorning.model;

import android.test.AndroidTestCase;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

import cz.jaro.alarmmorning.Analytics;
import cz.jaro.alarmmorning.GlobalManager;

import static cz.jaro.alarmmorning.model.AlarmDataSourceTest.DAY;
import static cz.jaro.alarmmorning.model.AlarmDataSourceTest.HOUR_DAY;
import static cz.jaro.alarmmorning.model.AlarmDataSourceTest.MINUTE_DAY;
import static cz.jaro.alarmmorning.model.AlarmDataSourceTest.MONTH;
import static cz.jaro.alarmmorning.model.AlarmDataSourceTest.YEAR;

public class AlarmDataSource3Test extends AndroidTestCase {

    private static final int DAYS = 20;

    private GlobalManager globalManager;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        globalManager = GlobalManager.getInstance();
        Analytics analytics = new Analytics(Analytics.Channel.Activity, Analytics.ChannelName.Calendar);

        for (int i = 0; i < DAYS; i++) {
            Day day = new Day();
            day.setState(Day.STATE_ENABLED);
            day.setDate(new GregorianCalendar(YEAR, MONTH, DAY + i));
            day.setHour(HOUR_DAY);
            day.setMinute(MINUTE_DAY);
            globalManager.saveDay(day, analytics);
        }
    }

    public void testPreConditions() {
        assertNotNull(globalManager);
    }

    public void test_getAlarmsInPeriod_empty() {
        Calendar from = new GregorianCalendar(YEAR, MONTH - 1, DAY);
        Calendar to = new GregorianCalendar(YEAR, MONTH - 1, DAY + 10);
        List<Calendar> alarmTimes = globalManager.getAlarmsInPeriod(from, to);

        assertEquals(alarmTimes.isEmpty(), true);
    }

    public void test_getAlarmsInPeriod_empty2() {
        Calendar from = new GregorianCalendar(YEAR, MONTH, DAY, HOUR_DAY - 2, MINUTE_DAY);
        Calendar to = new GregorianCalendar(YEAR, MONTH, DAY, HOUR_DAY - 1, MINUTE_DAY);
        List<Calendar> alarmTimes = globalManager.getAlarmsInPeriod(from, to);

        assertEquals(alarmTimes.isEmpty(), true);
    }

    public void test_getAlarmsInPeriod_empty3() {
        Calendar from = new GregorianCalendar(YEAR, MONTH, DAY - 1, 0, 0);
        Calendar to = new GregorianCalendar(YEAR, MONTH, DAY, 0, 0);
        List<Calendar> alarmTimes = globalManager.getAlarmsInPeriod(from, to);

        assertEquals(alarmTimes.isEmpty(), true);
    }

    public void test_getAlarmsInPeriod_one_day() {
        Calendar from = new GregorianCalendar(YEAR, MONTH, DAY, 0, 0);
        Calendar to = new GregorianCalendar(YEAR, MONTH, DAY + 1, 0, 0);
        List<Calendar> alarmTimes = globalManager.getAlarmsInPeriod(from, to);

        assertEquals(alarmTimes.size(), 1);

        Calendar nextAlarm = alarmTimes.get(0);
        assertEquals(YEAR, nextAlarm.get(Calendar.YEAR));
        assertEquals(MONTH, nextAlarm.get(Calendar.MONTH));
        assertEquals(DAY, nextAlarm.get(Calendar.DAY_OF_MONTH));
        assertEquals(HOUR_DAY, nextAlarm.get(Calendar.HOUR_OF_DAY));
        assertEquals(MINUTE_DAY, nextAlarm.get(Calendar.MINUTE));
        assertEquals(0, nextAlarm.get(Calendar.SECOND));
        assertEquals(0, nextAlarm.get(Calendar.MILLISECOND));
    }

    public void test_getAlarmsInPeriod_one_minute() {
        Calendar from = new GregorianCalendar(YEAR, MONTH, DAY, HOUR_DAY, MINUTE_DAY - 1);
        Calendar to = new GregorianCalendar(YEAR, MONTH, DAY, HOUR_DAY, MINUTE_DAY + 1);
        List<Calendar> alarmTimes = globalManager.getAlarmsInPeriod(from, to);

        assertEquals(alarmTimes.size(), 1);

        Calendar nextAlarm = alarmTimes.get(0);
        assertEquals(YEAR, nextAlarm.get(Calendar.YEAR));
        assertEquals(MONTH, nextAlarm.get(Calendar.MONTH));
        assertEquals(DAY, nextAlarm.get(Calendar.DAY_OF_MONTH));
        assertEquals(HOUR_DAY, nextAlarm.get(Calendar.HOUR_OF_DAY));
        assertEquals(MINUTE_DAY, nextAlarm.get(Calendar.MINUTE));
        assertEquals(0, nextAlarm.get(Calendar.SECOND));
        assertEquals(0, nextAlarm.get(Calendar.MILLISECOND));
    }

    public void test_getAlarmsInPeriod_one_leftBorder() {
        Calendar from = new GregorianCalendar(YEAR, MONTH, DAY, HOUR_DAY, MINUTE_DAY);
        Calendar to = new GregorianCalendar(YEAR, MONTH, DAY, HOUR_DAY, MINUTE_DAY + 1);
        List<Calendar> alarmTimes = globalManager.getAlarmsInPeriod(from, to);

        assertEquals(alarmTimes.size(), 1);

        Calendar nextAlarm = alarmTimes.get(0);
        assertEquals(YEAR, nextAlarm.get(Calendar.YEAR));
        assertEquals(MONTH, nextAlarm.get(Calendar.MONTH));
        assertEquals(DAY, nextAlarm.get(Calendar.DAY_OF_MONTH));
        assertEquals(HOUR_DAY, nextAlarm.get(Calendar.HOUR_OF_DAY));
        assertEquals(MINUTE_DAY, nextAlarm.get(Calendar.MINUTE));
        assertEquals(0, nextAlarm.get(Calendar.SECOND));
        assertEquals(0, nextAlarm.get(Calendar.MILLISECOND));
    }

    public void test_getAlarmsInPeriod_one_rightBorder() {
        Calendar from = new GregorianCalendar(YEAR, MONTH, DAY, HOUR_DAY, MINUTE_DAY - 1);
        Calendar to = new GregorianCalendar(YEAR, MONTH, DAY, HOUR_DAY, MINUTE_DAY);
        List<Calendar> alarmTimes = globalManager.getAlarmsInPeriod(from, to);

        assertEquals(alarmTimes.size(), 1);

        Calendar nextAlarm = alarmTimes.get(0);
        assertEquals(YEAR, nextAlarm.get(Calendar.YEAR));
        assertEquals(MONTH, nextAlarm.get(Calendar.MONTH));
        assertEquals(DAY, nextAlarm.get(Calendar.DAY_OF_MONTH));
        assertEquals(HOUR_DAY, nextAlarm.get(Calendar.HOUR_OF_DAY));
        assertEquals(MINUTE_DAY, nextAlarm.get(Calendar.MINUTE));
        assertEquals(0, nextAlarm.get(Calendar.SECOND));
        assertEquals(0, nextAlarm.get(Calendar.MILLISECOND));
    }

    public void test_getAlarmsInPeriod_two_day() {
        Calendar from = new GregorianCalendar(YEAR, MONTH, DAY, 0, 0);
        Calendar to = new GregorianCalendar(YEAR, MONTH, DAY + 2, 0, 0);
        List<Calendar> alarmTimes = globalManager.getAlarmsInPeriod(from, to);

        assertEquals(alarmTimes.size(), 2);

        Calendar nextAlarm = alarmTimes.get(0);
        assertEquals(YEAR, nextAlarm.get(Calendar.YEAR));
        assertEquals(MONTH, nextAlarm.get(Calendar.MONTH));
        assertEquals(DAY, nextAlarm.get(Calendar.DAY_OF_MONTH));
        assertEquals(HOUR_DAY, nextAlarm.get(Calendar.HOUR_OF_DAY));
        assertEquals(MINUTE_DAY, nextAlarm.get(Calendar.MINUTE));
        assertEquals(0, nextAlarm.get(Calendar.SECOND));
        assertEquals(0, nextAlarm.get(Calendar.MILLISECOND));

        Calendar nextAlarm2 = alarmTimes.get(1);
        assertEquals(YEAR, nextAlarm2.get(Calendar.YEAR));
        assertEquals(MONTH, nextAlarm2.get(Calendar.MONTH));
        assertEquals(DAY + 1, nextAlarm2.get(Calendar.DAY_OF_MONTH));
        assertEquals(HOUR_DAY, nextAlarm2.get(Calendar.HOUR_OF_DAY));
        assertEquals(MINUTE_DAY, nextAlarm2.get(Calendar.MINUTE));
        assertEquals(0, nextAlarm2.get(Calendar.SECOND));
        assertEquals(0, nextAlarm2.get(Calendar.MILLISECOND));
    }

    public void test_getAlarmsInPeriod_three_day() {
        Calendar from = new GregorianCalendar(YEAR, MONTH, DAY, 0, 0);
        Calendar to = new GregorianCalendar(YEAR, MONTH, DAY + 3, 0, 0);
        List<Calendar> alarmTimes = globalManager.getAlarmsInPeriod(from, to);

        assertEquals(alarmTimes.size(), 3);

        Calendar nextAlarm = alarmTimes.get(0);
        assertEquals(YEAR, nextAlarm.get(Calendar.YEAR));
        assertEquals(MONTH, nextAlarm.get(Calendar.MONTH));
        assertEquals(DAY, nextAlarm.get(Calendar.DAY_OF_MONTH));
        assertEquals(HOUR_DAY, nextAlarm.get(Calendar.HOUR_OF_DAY));
        assertEquals(MINUTE_DAY, nextAlarm.get(Calendar.MINUTE));
        assertEquals(0, nextAlarm.get(Calendar.SECOND));
        assertEquals(0, nextAlarm.get(Calendar.MILLISECOND));

        Calendar nextAlarm2 = alarmTimes.get(1);
        assertEquals(YEAR, nextAlarm2.get(Calendar.YEAR));
        assertEquals(MONTH, nextAlarm2.get(Calendar.MONTH));
        assertEquals(DAY + 1, nextAlarm2.get(Calendar.DAY_OF_MONTH));
        assertEquals(HOUR_DAY, nextAlarm2.get(Calendar.HOUR_OF_DAY));
        assertEquals(MINUTE_DAY, nextAlarm2.get(Calendar.MINUTE));
        assertEquals(0, nextAlarm2.get(Calendar.SECOND));
        assertEquals(0, nextAlarm2.get(Calendar.MILLISECOND));

        Calendar nextAlarm3 = alarmTimes.get(2);
        assertEquals(YEAR, nextAlarm3.get(Calendar.YEAR));
        assertEquals(MONTH, nextAlarm3.get(Calendar.MONTH));
        assertEquals(DAY + 2, nextAlarm3.get(Calendar.DAY_OF_MONTH));
        assertEquals(HOUR_DAY, nextAlarm3.get(Calendar.HOUR_OF_DAY));
        assertEquals(MINUTE_DAY, nextAlarm3.get(Calendar.MINUTE));
        assertEquals(0, nextAlarm3.get(Calendar.SECOND));
        assertEquals(0, nextAlarm3.get(Calendar.MILLISECOND));
    }

    public void test_getAlarmsInPeriod_all() {
        Calendar from = new GregorianCalendar(YEAR - 1, MONTH, DAY);
        Calendar to = new GregorianCalendar(YEAR + 1, MONTH, DAY);
        List<Calendar> alarmTimes = globalManager.getAlarmsInPeriod(from, to);

        assertEquals(alarmTimes.size(), DAYS);

        for (int i = 0; i < DAYS; i++) {
            Calendar nextAlarm = alarmTimes.get(i);
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
