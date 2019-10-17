package cz.jaro.alarmmorning.model;

import android.content.Context;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.GregorianCalendar;
import java.util.List;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import cz.jaro.alarmmorning.AlarmMorningApplication;

import static cz.jaro.alarmmorning.model.AlarmDataSource1Test.DAY;
import static cz.jaro.alarmmorning.model.AlarmDataSource1Test.MONTH;
import static cz.jaro.alarmmorning.model.AlarmDataSource1Test.YEAR;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Tests with a database with only one-time alarms.
 */
@RunWith(AndroidJUnit4.class)
public class AlarmDataSource4OneTimeAlarmTest {

    public static final int HOUR_ONE_TIME_ALARM = 15;
    public static final int MINUTE_ONE_TIME_ALARM = 0;

    private AlarmDataSource dataSource;

    @Before
    public void before() {
        Context context = AlarmMorningApplication.getAppContext();
        dataSource = new AlarmDataSource(context);
        dataSource.open();

        dataSource.resetDatabase();
    }

    @Test
    public void oneTimeAlarm_0_empty() {
        List<OneTimeAlarm> oneTimeAlarms = dataSource.loadOneTimeAlarms(null);

        assertEquals(oneTimeAlarms.size(), 0);
    }

    @Test
    public void oneTimeAlarm_1_add() {
        // Add

        OneTimeAlarm oneTimeAlarm1 = new OneTimeAlarm();
        oneTimeAlarm1.setDate(new GregorianCalendar(YEAR, MONTH, DAY));
        oneTimeAlarm1.setHour(HOUR_ONE_TIME_ALARM);
        oneTimeAlarm1.setHour(MINUTE_ONE_TIME_ALARM);

        dataSource.saveOneTimeAlarm(oneTimeAlarm1);

        // Check

        List<OneTimeAlarm> oneTimeAlarms = dataSource.loadOneTimeAlarms(null);

        assertEquals(oneTimeAlarms.size(), 1);

        OneTimeAlarm oneTimeAlarm1b = oneTimeAlarms.get(0);

        assertNotNull(oneTimeAlarm1b);

        assertEquals(oneTimeAlarm1.getDate().getTime().toString(), oneTimeAlarm1b.getDate().getTime().toString());
        assertEquals(oneTimeAlarm1.getHour(), oneTimeAlarm1b.getHour());
        assertEquals(oneTimeAlarm1.getMinute(), oneTimeAlarm1b.getMinute());
    }

    @Test
    public void oneTimeAlarm_1_add_2writes() {
        // Add

        OneTimeAlarm oneTimeAlarm1 = new OneTimeAlarm();
        oneTimeAlarm1.setDate(new GregorianCalendar(YEAR, MONTH, DAY));
        oneTimeAlarm1.setHour(HOUR_ONE_TIME_ALARM);
        oneTimeAlarm1.setHour(MINUTE_ONE_TIME_ALARM);

        dataSource.saveOneTimeAlarm(oneTimeAlarm1);

        // Load

        List<OneTimeAlarm> oneTimeAlarms = dataSource.loadOneTimeAlarms(null);
        oneTimeAlarm1 = oneTimeAlarms.get(0);

        // 2nd add (= change)

        oneTimeAlarm1.setDate(new GregorianCalendar(YEAR + 1, MONTH + 1, DAY + 1));
        oneTimeAlarm1.setHour(HOUR_ONE_TIME_ALARM + 1);
        oneTimeAlarm1.setHour(MINUTE_ONE_TIME_ALARM + 1);

        dataSource.saveOneTimeAlarm(oneTimeAlarm1);

        // Check

        List<OneTimeAlarm> oneTimeAlarms2 = dataSource.loadOneTimeAlarms(null);

        assertEquals(oneTimeAlarms2.size(), 1);

        OneTimeAlarm oneTimeAlarm1b = oneTimeAlarms2.get(0);

        assertNotNull(oneTimeAlarm1b);

        assertEquals(oneTimeAlarm1.getDate().getTime().toString(), oneTimeAlarm1b.getDate().getTime().toString());
        assertEquals(oneTimeAlarm1.getHour(), oneTimeAlarm1b.getHour());
        assertEquals(oneTimeAlarm1.getMinute(), oneTimeAlarm1b.getMinute());
    }

    @Test
    public void oneTimeAlarm_1_remove() {
        // Add

        OneTimeAlarm oneTimeAlarm1 = new OneTimeAlarm();
        oneTimeAlarm1.setDate(new GregorianCalendar(YEAR, MONTH, DAY));
        oneTimeAlarm1.setHour(HOUR_ONE_TIME_ALARM);
        oneTimeAlarm1.setHour(MINUTE_ONE_TIME_ALARM);

        dataSource.saveOneTimeAlarm(oneTimeAlarm1);

        // Remove

        List<OneTimeAlarm> oneTimeAlarms = dataSource.loadOneTimeAlarms(null);
        OneTimeAlarm oneTimeAlarm1b = oneTimeAlarms.get(0);

        dataSource.deleteOneTimeAlarm(oneTimeAlarm1b);

        // Check

        List<OneTimeAlarm> oneTimeAlarms2 = dataSource.loadOneTimeAlarms(null);

        assertEquals(oneTimeAlarms2.size(), 0);
    }

    @Test
    public void oneTimeAlarm_2_add() {
        // Add 1

        OneTimeAlarm oneTimeAlarm1 = new OneTimeAlarm();
        oneTimeAlarm1.setDate(new GregorianCalendar(YEAR, MONTH, DAY));
        oneTimeAlarm1.setHour(HOUR_ONE_TIME_ALARM);
        oneTimeAlarm1.setHour(MINUTE_ONE_TIME_ALARM);

        dataSource.saveOneTimeAlarm(oneTimeAlarm1);

        // Add 2

        OneTimeAlarm oneTimeAlarm2 = new OneTimeAlarm();
        oneTimeAlarm2.setDate(new GregorianCalendar(YEAR + 1, MONTH + 1, DAY + 1));
        oneTimeAlarm2.setHour(HOUR_ONE_TIME_ALARM + 1);
        oneTimeAlarm2.setHour(MINUTE_ONE_TIME_ALARM + 1);

        dataSource.saveOneTimeAlarm(oneTimeAlarm2);

        // Check

        List<OneTimeAlarm> oneTimeAlarms = dataSource.loadOneTimeAlarms(null);

        assertEquals(oneTimeAlarms.size(), 2);

        OneTimeAlarm oneTimeAlarm1b = oneTimeAlarms.get(0);
        OneTimeAlarm oneTimeAlarm2b = oneTimeAlarms.get(1);

        // Trick: because loadOneTimeAlarms() returns the alarms unordered, we may need to "fix" the order
        if (oneTimeAlarm1.getHour() != oneTimeAlarm1b.getHour()) {
            oneTimeAlarm1b = oneTimeAlarms.get(1);
            oneTimeAlarm2b = oneTimeAlarms.get(0);
        }

        // Check 1
        assertNotNull(oneTimeAlarm1b);

        assertEquals(oneTimeAlarm1.getDate().getTime().toString(), oneTimeAlarm1b.getDate().getTime().toString());
        assertEquals(oneTimeAlarm1.getHour(), oneTimeAlarm1b.getHour());
        assertEquals(oneTimeAlarm1.getMinute(), oneTimeAlarm1b.getMinute());

        // Check 2
        assertNotNull(oneTimeAlarm2b);

        assertEquals(oneTimeAlarm2.getDate().getTime().toString(), oneTimeAlarm2b.getDate().getTime().toString());
        assertEquals(oneTimeAlarm2.getHour(), oneTimeAlarm2b.getHour());
        assertEquals(oneTimeAlarm2.getMinute(), oneTimeAlarm2b.getMinute());
    }

}
