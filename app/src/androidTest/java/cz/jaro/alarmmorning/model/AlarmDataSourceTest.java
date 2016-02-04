package cz.jaro.alarmmorning.model;

import android.test.AndroidTestCase;
import android.test.RenamingDelegatingContext;

import java.util.Calendar;
import java.util.GregorianCalendar;

public class AlarmDataSourceTest extends AndroidTestCase {

    // February 2016 starts with Monday
    public static final int YEAR = 2016;
    public static final int MONTH = Calendar.FEBRUARY;
    public static final int DAY = 1;
    public static final int DAY_OF_WEEK = Calendar.MONDAY;

    public static final int HOUR_DAY = 8;
    public static final int MINUTE_DAY = 1;

    public static final int HOUR_DEFAULT = 7;
    public static final int MINUTE_DEFAULT = 0;

    private AlarmDataSource dataSource;
    private Defaults defaults;
    private Day day;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        RenamingDelegatingContext context = new RenamingDelegatingContext(getContext(), "test_");
        dataSource = new AlarmDataSource(context);
        dataSource.open();

        defaults = new Defaults();
        defaults.setState(Defaults.STATE_ENABLED);
        defaults.setDayOfWeek(DAY_OF_WEEK);
        defaults.setHour(HOUR_DEFAULT);
        defaults.setMinute(MINUTE_DEFAULT);

        day = new Day();
        day.setState(Day.STATE_ENABLED);
        day.setDate(new GregorianCalendar(YEAR, MONTH, DAY));
        day.setHour(HOUR_DAY);
        day.setMinute(MINUTE_DAY);
        day.setDefaults(defaults);
    }

    @Override
    public void tearDown() throws Exception {
        dataSource.close();
        super.tearDown();
    }

    public void testPreConditions() {
        assertNotNull(dataSource);
    }

    public void test_Defaults_2writes() {
        // save 1st object

        Defaults defaults1a = new Defaults();
        defaults1a.setState(Defaults.STATE_ENABLED);
        defaults1a.setDayOfWeek(DAY_OF_WEEK);
        defaults1a.setHour(HOUR_DEFAULT);
        defaults1a.setMinute(MINUTE_DEFAULT);

        dataSource.saveDefault(defaults1a);

        Defaults defaults1b = dataSource.loadDefault(defaults1a.getDayOfWeek());

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

        dataSource.saveDefault(defaults2a);

        Defaults defaults2b = dataSource.loadDefault(defaults2a.getDayOfWeek());

        assertEquals(defaults2a.getState(), defaults2b.getState());
        assertEquals(defaults2a.getDayOfWeek(), defaults2b.getDayOfWeek());
        assertEquals(defaults2a.getHour(), defaults2b.getHour());
        assertEquals(defaults2a.getMinute(), defaults2b.getMinute());
    }

    public void test_Day_2writes() {
        // save 1st object

        Day day1a = new Day();
        day1a.setState(Defaults.STATE_ENABLED);
        day1a.setDate(new GregorianCalendar(YEAR, MONTH, DAY));
        day1a.setHour(HOUR_DEFAULT);
        day1a.setMinute(MINUTE_DEFAULT);

        dataSource.saveDay(day1a);

        Day day1b = dataSource.loadDayDeep(day1a.getDate());

        assertEquals(day1a.getState(), day1b.getState());
        assertEquals(day1a.getDate(), day1b.getDate());
        assertEquals(day1a.getHour(), day1b.getHour());
        assertEquals(day1a.getMinute(), day1b.getMinute());
        assertNotNull(day1b.getDefaults());

        // save 2nd object

        Day day2a = new Day();
        day2a.setState(Defaults.STATE_DISABLED);
        day2a.setDate(new GregorianCalendar(YEAR, MONTH, DAY + 1));
        day2a.setHour(HOUR_DEFAULT + 1);
        day2a.setMinute(MINUTE_DEFAULT + 1);

        dataSource.saveDay(day2a);

        Day day2b = dataSource.loadDayDeep(day2a.getDate());

        assertEquals(day2a.getState(), day2b.getState());
        assertEquals(day2a.getDate(), day2b.getDate());
        assertEquals(day2a.getHour(), day2b.getHour());
        assertEquals(day2a.getMinute(), day2b.getMinute());
        assertNotNull(day2b.getDefaults());
    }

    public void test_Day_load_notStored() {
        Calendar dateWithoutRecord = new GregorianCalendar(YEAR - 1, MONTH, DAY);
        Day day = dataSource.loadDayDeep(dateWithoutRecord);

        assertEquals(Day.STATE_DEFAULT, day.getState());
        assertEquals(dateWithoutRecord.getTime().toString(), day.getDate().getTime().toString());
        assertEquals(dateWithoutRecord, day.getDate());
        assertEquals(Day.VALUE_UNSET, day.getHour());
        assertEquals(Day.VALUE_UNSET, day.getMinute());
        assertNotNull(day.getDefaults());
    }
}
