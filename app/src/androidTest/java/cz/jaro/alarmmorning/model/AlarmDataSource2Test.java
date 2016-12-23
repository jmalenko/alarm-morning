package cz.jaro.alarmmorning.model;

import android.test.AndroidTestCase;

import java.util.Calendar;
import java.util.GregorianCalendar;

import cz.jaro.alarmmorning.GlobalManager;
import cz.jaro.alarmmorning.clock.FixedClock;

import static cz.jaro.alarmmorning.model.AlarmDataSourceTest.DAY;
import static cz.jaro.alarmmorning.model.AlarmDataSourceTest.HOUR_DAY;
import static cz.jaro.alarmmorning.model.AlarmDataSourceTest.MINUTE_DAY;
import static cz.jaro.alarmmorning.model.AlarmDataSourceTest.MONTH;
import static cz.jaro.alarmmorning.model.AlarmDataSourceTest.YEAR;

public class AlarmDataSource2Test extends AndroidTestCase {

    private GlobalManager globalManager;

    private Day day0;
    private Day day1;
    private Day day2;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        globalManager = GlobalManager.getInstance();

        day0 = new Day();
        day0.setState(Day.STATE_ENABLED);
        day0.setDate(new GregorianCalendar(YEAR, MONTH, DAY));
        day0.setHour(HOUR_DAY);
        day0.setMinute(MINUTE_DAY);
        globalManager.saveDay(day0);

        // day1 = day0 + 8 days
        day1 = new Day();
        day1.setState(Day.STATE_ENABLED);
        day1.setDate(new GregorianCalendar(YEAR, MONTH, DAY + 8));
        day1.setHour(HOUR_DAY + 1);
        day1.setMinute(MINUTE_DAY + 1);
        globalManager.saveDay(day1);

        // day2 = day0 + 16 days
        day2 = new Day();
        day2.setState(Day.STATE_ENABLED);
        day2.setDate(new GregorianCalendar(YEAR, MONTH, DAY + 16));
        day2.setHour(HOUR_DAY + 2);
        day2.setMinute(MINUTE_DAY + 2);
        globalManager.saveDay(day2);
    }

    public void testPreConditions() {
        assertNotNull(globalManager);
        assertTrue("", 8 <= GlobalManager.HORIZON_DAYS);
    }

    public void test_before0() {
        FixedClock clock = new FixedClock(new GregorianCalendar(YEAR, MONTH, DAY, HOUR_DAY, MINUTE_DAY)).addMinute(-1);
        Calendar nextAlarm = globalManager.getNextAlarm(clock);

        assertEquals("next alarm on " + clock.now().getTime().toString(), day0.getDateTime().getTime().toString(), nextAlarm.getTime().toString());
        assertEquals(YEAR, nextAlarm.get(Calendar.YEAR));
        assertEquals(MONTH, nextAlarm.get(Calendar.MONTH));
        assertEquals(DAY, nextAlarm.get(Calendar.DAY_OF_MONTH));
        assertEquals(HOUR_DAY, nextAlarm.get(Calendar.HOUR_OF_DAY));
        assertEquals(MINUTE_DAY, nextAlarm.get(Calendar.MINUTE));
        assertEquals(0, nextAlarm.get(Calendar.SECOND));
        assertEquals(0, nextAlarm.get(Calendar.MILLISECOND));
    }

    public void test_after0() {
        FixedClock clock = new FixedClock(new GregorianCalendar(YEAR, MONTH, DAY, HOUR_DAY, MINUTE_DAY)).addMinute(1);
        Calendar nextAlarm = globalManager.getNextAlarm(clock);

        assertEquals("next alarm on " + clock.now().getTime().toString(), day1.getDateTime().getTime().toString(), nextAlarm.getTime().toString());
        assertEquals(YEAR, nextAlarm.get(Calendar.YEAR));
        assertEquals(MONTH, nextAlarm.get(Calendar.MONTH));
        assertEquals(DAY + 8, nextAlarm.get(Calendar.DAY_OF_MONTH));
        assertEquals(HOUR_DAY + 1, nextAlarm.get(Calendar.HOUR_OF_DAY));
        assertEquals(MINUTE_DAY + 1, nextAlarm.get(Calendar.MINUTE));
        assertEquals(0, nextAlarm.get(Calendar.SECOND));
        assertEquals(0, nextAlarm.get(Calendar.MILLISECOND));
    }

    public void test_before1() {
        FixedClock clock = new FixedClock(new GregorianCalendar(YEAR, MONTH, DAY + 8, HOUR_DAY + 1, MINUTE_DAY + 1)).addMinute(-1);
        Calendar nextAlarm = globalManager.getNextAlarm(clock);

        assertEquals("next alarm on " + clock.now().getTime().toString(), day1.getDateTime().getTime().toString(), nextAlarm.getTime().toString());
        assertEquals(YEAR, nextAlarm.get(Calendar.YEAR));
        assertEquals(MONTH, nextAlarm.get(Calendar.MONTH));
        assertEquals(DAY + 8, nextAlarm.get(Calendar.DAY_OF_MONTH));
        assertEquals(HOUR_DAY + 1, nextAlarm.get(Calendar.HOUR_OF_DAY));
        assertEquals(MINUTE_DAY + 1, nextAlarm.get(Calendar.MINUTE));
        assertEquals(0, nextAlarm.get(Calendar.SECOND));
        assertEquals(0, nextAlarm.get(Calendar.MILLISECOND));
    }

    public void test_after1() {
        FixedClock clock = new FixedClock(new GregorianCalendar(YEAR, MONTH, DAY + 8, HOUR_DAY + 1, MINUTE_DAY + 1)).addMinute(1);
        Calendar nextAlarm = globalManager.getNextAlarm(clock);

        assertEquals("next alarm on " + clock.now().getTime().toString(), day2.getDateTime().getTime().toString(), nextAlarm.getTime().toString());
        assertEquals(YEAR, nextAlarm.get(Calendar.YEAR));
        assertEquals(MONTH, nextAlarm.get(Calendar.MONTH));
        assertEquals(DAY + 16, nextAlarm.get(Calendar.DAY_OF_MONTH));
        assertEquals(HOUR_DAY + 2, nextAlarm.get(Calendar.HOUR_OF_DAY));
        assertEquals(MINUTE_DAY + 2, nextAlarm.get(Calendar.MINUTE));
        assertEquals(0, nextAlarm.get(Calendar.SECOND));
        assertEquals(0, nextAlarm.get(Calendar.MILLISECOND));
    }

    public void test_before2() {
        FixedClock clock = new FixedClock(new GregorianCalendar(YEAR, MONTH, DAY + 16, HOUR_DAY + 2, MINUTE_DAY + 2)).addMinute(-1);
        Calendar nextAlarm = globalManager.getNextAlarm(clock);

        assertEquals("next alarm on " + clock.now().getTime().toString(), day2.getDateTime().getTime().toString(), nextAlarm.getTime().toString());
        assertEquals(YEAR, nextAlarm.get(Calendar.YEAR));
        assertEquals(MONTH, nextAlarm.get(Calendar.MONTH));
        assertEquals(DAY + 16, nextAlarm.get(Calendar.DAY_OF_MONTH));
        assertEquals(HOUR_DAY + 2, nextAlarm.get(Calendar.HOUR_OF_DAY));
        assertEquals(MINUTE_DAY + 2, nextAlarm.get(Calendar.MINUTE));
        assertEquals(0, nextAlarm.get(Calendar.SECOND));
        assertEquals(0, nextAlarm.get(Calendar.MILLISECOND));
    }

    public void test_after2() {
        // set all defaults to disabled
        for (int dayOfWeek : AlarmDataSource.allDaysOfWeek) {
            Defaults defaults = new Defaults();
            defaults.setDayOfWeek(dayOfWeek);
            defaults.setState(Defaults.STATE_DISABLED);
            defaults.setHour(1);
            defaults.setMinute(2);
            globalManager.saveDefault(defaults);
        }

        FixedClock clock = new FixedClock(new GregorianCalendar(YEAR, MONTH, DAY + 16, HOUR_DAY + 2, MINUTE_DAY + 2)).addMinute(1);
        Calendar nextAlarm = globalManager.getNextAlarm(clock);

        assertNull(nextAlarm);
    }

}
