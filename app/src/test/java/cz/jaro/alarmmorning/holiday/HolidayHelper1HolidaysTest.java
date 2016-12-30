package cz.jaro.alarmmorning.holiday;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

import cz.jaro.alarmmorning.BuildConfig;
import de.jollyday.Holiday;

import static org.junit.Assert.assertEquals;

/**
 * Test the Czech holidays (in HolidayHelper class).
 */
@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class, manifest = "app/src/main/AndroidManifest.xml", sdk = 21)
public class HolidayHelper1HolidaysTest {

    public static final List<Calendar> holidays_Czech;
    static {
        holidays_Czech = new ArrayList<>();
        // 2016
        holidays_Czech.add(new GregorianCalendar(2016, Calendar.JANUARY, 1));
        holidays_Czech.add(new GregorianCalendar(2016, Calendar.MARCH, 27));
        holidays_Czech.add(new GregorianCalendar(2016, Calendar.MARCH, 28));
        holidays_Czech.add(new GregorianCalendar(2016, Calendar.MAY, 1));
        holidays_Czech.add(new GregorianCalendar(2016, Calendar.MAY, 8));
        holidays_Czech.add(new GregorianCalendar(2016, Calendar.JULY, 5));
        holidays_Czech.add(new GregorianCalendar(2016, Calendar.JULY, 6));
        holidays_Czech.add(new GregorianCalendar(2016, Calendar.SEPTEMBER, 28));
        holidays_Czech.add(new GregorianCalendar(2016, Calendar.OCTOBER, 28));
        holidays_Czech.add(new GregorianCalendar(2016, Calendar.NOVEMBER, 17));
        holidays_Czech.add(new GregorianCalendar(2016, Calendar.DECEMBER, 24));
        holidays_Czech.add(new GregorianCalendar(2016, Calendar.DECEMBER, 25));
        holidays_Czech.add(new GregorianCalendar(2016, Calendar.DECEMBER, 26));
        // 2017
        holidays_Czech.add(new GregorianCalendar(2017, Calendar.JANUARY, 1));
        holidays_Czech.add(new GregorianCalendar(2017, Calendar.APRIL, 16));
        holidays_Czech.add(new GregorianCalendar(2017, Calendar.APRIL, 17));
        holidays_Czech.add(new GregorianCalendar(2017, Calendar.MAY, 1));
        holidays_Czech.add(new GregorianCalendar(2017, Calendar.MAY, 8));
        holidays_Czech.add(new GregorianCalendar(2017, Calendar.JULY, 5));
        holidays_Czech.add(new GregorianCalendar(2017, Calendar.JULY, 6));
        holidays_Czech.add(new GregorianCalendar(2017, Calendar.SEPTEMBER, 28));
        holidays_Czech.add(new GregorianCalendar(2017, Calendar.OCTOBER, 28));
        holidays_Czech.add(new GregorianCalendar(2017, Calendar.NOVEMBER, 17));
        holidays_Czech.add(new GregorianCalendar(2017, Calendar.DECEMBER, 24));
        holidays_Czech.add(new GregorianCalendar(2017, Calendar.DECEMBER, 25));
        holidays_Czech.add(new GregorianCalendar(2017, Calendar.DECEMBER, 26));
    }

    private HolidayHelper holidayHelper;

    @Before
    public void before() {
        holidayHelper = HolidayHelper.getInstance();
    }

    @Test
    public void listHolidays() {
        List<Holiday> holidays = holidayHelper.listHolidays(HolidayHelperTest.CZ);
        for (Holiday holiday : holidays) {
            Calendar date = Calendar.getInstance();
            date.setTime(holiday.getDate().toDate());

            if (!isHoliday(date, holidays_Czech)) {
                throw new AssertionError("There should be no holiday on " + date.getTime().toString());
            }
        }

        // make sure there is no other holiday
        assertEquals("Count of holidays", holidays.size(), 13);
    }

    static public boolean isHoliday(Calendar date, List<Calendar> holidays) {
        for (Calendar d : holidays) {
            if (sameDate(d, date))
                return true;
        }
        return false;
    }

    static public boolean sameDate(Calendar d1, Calendar d2) {
        return d1.get(Calendar.YEAR) == d2.get(Calendar.YEAR)
                && d1.get(Calendar.MONTH) == d2.get(Calendar.MONTH)
                && d1.get(Calendar.DAY_OF_MONTH) == d2.get(Calendar.DAY_OF_MONTH);
    }
}