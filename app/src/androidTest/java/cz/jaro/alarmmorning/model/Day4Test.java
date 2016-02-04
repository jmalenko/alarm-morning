package cz.jaro.alarmmorning.model;

import android.test.AndroidTestCase;
import android.test.RenamingDelegatingContext;

import java.util.GregorianCalendar;

import cz.jaro.alarmmorning.clock.FixedClock;

import static cz.jaro.alarmmorning.model.AlarmDataSourceTest.DAY;
import static cz.jaro.alarmmorning.model.AlarmDataSourceTest.HOUR_DAY;
import static cz.jaro.alarmmorning.model.AlarmDataSourceTest.MINUTE_DAY;
import static cz.jaro.alarmmorning.model.AlarmDataSourceTest.MONTH;
import static cz.jaro.alarmmorning.model.AlarmDataSourceTest.YEAR;

public class Day4Test extends AndroidTestCase {

    private AlarmDataSource dataSource;

    private Day day0;
    private Day day1;
    private Day day2;
    RenamingDelegatingContext context;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        context = new RenamingDelegatingContext(getContext(), "test_");
        dataSource = new AlarmDataSource(context);
        dataSource.open();

        day0 = new Day();
        day0.setState(Day.STATE_ENABLED);
        day0.setDate(new GregorianCalendar(YEAR, MONTH, DAY));
        day0.setHour(HOUR_DAY);
        day0.setMinute(MINUTE_DAY);
        dataSource.saveDay(day0);

        // day1 = day0 + 8 days
        day1 = new Day();
        day1.setState(Day.STATE_ENABLED);
        day1.setDate(new GregorianCalendar(YEAR, MONTH, DAY + 1));
        day1.setHour(HOUR_DAY + 1);
        day1.setMinute(MINUTE_DAY + 1);
        dataSource.saveDay(day1);

        // day2 = day0 + 8 days
        day2 = new Day();
        day2.setState(Day.STATE_ENABLED);
        day2.setDate(new GregorianCalendar(YEAR, MONTH, DAY + 2));
        day2.setHour(HOUR_DAY + 2);
        day2.setMinute(MINUTE_DAY + 2);
        dataSource.saveDay(day2);
    }

    @Override
    public void tearDown() throws Exception {
        dataSource.close();
        super.tearDown();
    }

    public void testPreConditions() {
        assertNotNull(dataSource);
    }

    public void test_before0() {
        FixedClock clock = new FixedClock(new GregorianCalendar(YEAR, MONTH, DAY, HOUR_DAY, MINUTE_DAY)).addMinute(-1);

        assertEquals(day0.isNextAlarm(context, clock), true);
        assertEquals(day1.isNextAlarm(context, clock), false);
        assertEquals(day2.isNextAlarm(context, clock), false);
    }

    public void test_before1() {
        FixedClock clock = new FixedClock(new GregorianCalendar(YEAR, MONTH, DAY + 1, HOUR_DAY + 1, MINUTE_DAY + 1)).addMinute(-1);

        assertEquals(day0.isNextAlarm(context, clock), false);
        assertEquals(day1.isNextAlarm(context, clock), true);
        assertEquals(day2.isNextAlarm(context, clock), false);
    }

    public void test_before2() {
        FixedClock clock = new FixedClock(new GregorianCalendar(YEAR, MONTH, DAY + 2, HOUR_DAY + 2, MINUTE_DAY + 2)).addMinute(-1);

        assertEquals(day0.isNextAlarm(context, clock), false);
        assertEquals(day1.isNextAlarm(context, clock), false);
        assertEquals(day2.isNextAlarm(context, clock), true);
    }

    public void test_after2() {
        FixedClock clock = new FixedClock(new GregorianCalendar(YEAR, MONTH, DAY + 2, HOUR_DAY + 2, MINUTE_DAY + 2)).addMinute(1);

        // set all defaults to disabled
        for (int dayOfWeek : AlarmDataSource.allDaysOfWeek) {
            Defaults defaults = new Defaults();
            defaults.setDayOfWeek(dayOfWeek);
            defaults.setState(Defaults.STATE_DISABLED);
            defaults.setHour(1);
            defaults.setMinute(2);
            dataSource.saveDefault(defaults);
        }

        assertEquals(day0.isNextAlarm(context, clock), false);
        assertEquals(day1.isNextAlarm(context, clock), false);
        assertEquals(day2.isNextAlarm(context, clock), false);
    }
}
