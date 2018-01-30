package cz.jaro.alarmmorning.model;

import android.support.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.GregorianCalendar;

import cz.jaro.alarmmorning.Analytics;
import cz.jaro.alarmmorning.GlobalManager;
import cz.jaro.alarmmorning.clock.FixedClock;

import static cz.jaro.alarmmorning.model.AlarmDataSource1Test.DAY;
import static cz.jaro.alarmmorning.model.AlarmDataSource1Test.HOUR_DAY;
import static cz.jaro.alarmmorning.model.AlarmDataSource1Test.MINUTE_DAY;
import static cz.jaro.alarmmorning.model.AlarmDataSource1Test.MONTH;
import static cz.jaro.alarmmorning.model.AlarmDataSource1Test.YEAR;
import static junit.framework.Assert.assertEquals;

@RunWith(AndroidJUnit4.class)
public class Day4Test {

    private GlobalManager globalManager;

    private Day day0;
    private Day day1;
    private Day day2;

    @Before
    public void before() throws Exception {
        globalManager = GlobalManager.getInstance();

        day0 = new Day();
        day0.setState(Day.STATE_ENABLED);
        day0.setDate(new GregorianCalendar(YEAR, MONTH, DAY));
        day0.setHourDay(HOUR_DAY);
        day0.setMinuteDay(MINUTE_DAY);

        Analytics analytics = new Analytics(Analytics.Channel.Activity, Analytics.ChannelName.Calendar);

        globalManager.saveDay(day0, analytics);

        // day1 = day0 + 8 days
        day1 = new Day();
        day1.setState(Day.STATE_ENABLED);
        day1.setDate(new GregorianCalendar(YEAR, MONTH, DAY + 1));
        day1.setHourDay(HOUR_DAY + 1);
        day1.setMinuteDay(MINUTE_DAY + 1);

        analytics = new Analytics(Analytics.Channel.Activity, Analytics.ChannelName.Calendar);

        globalManager.saveDay(day1, analytics);

        // day2 = day0 + 8 days
        day2 = new Day();
        day2.setState(Day.STATE_ENABLED);
        day2.setDate(new GregorianCalendar(YEAR, MONTH, DAY + 2));
        day2.setHourDay(HOUR_DAY + 2);
        day2.setMinuteDay(MINUTE_DAY + 2);

        analytics = new Analytics(Analytics.Channel.Activity, Analytics.ChannelName.Calendar);

        globalManager.saveDay(day2, analytics);
    }

    @Test
    public void before0() {
        FixedClock clock = new FixedClock(new GregorianCalendar(YEAR, MONTH, DAY, HOUR_DAY, MINUTE_DAY)).addMinute(-1);

        assertEquals(day0.isNextAlarm(clock), true);
        assertEquals(day1.isNextAlarm(clock), false);
        assertEquals(day2.isNextAlarm(clock), false);
    }

    @Test
    public void before1() {
        FixedClock clock = new FixedClock(new GregorianCalendar(YEAR, MONTH, DAY + 1, HOUR_DAY + 1, MINUTE_DAY + 1)).addMinute(-1);

        assertEquals(day0.isNextAlarm(clock), false);
        assertEquals(day1.isNextAlarm(clock), true);
        assertEquals(day2.isNextAlarm(clock), false);
    }

    @Test
    public void before2() {
        FixedClock clock = new FixedClock(new GregorianCalendar(YEAR, MONTH, DAY + 2, HOUR_DAY + 2, MINUTE_DAY + 2)).addMinute(-1);

        assertEquals(day0.isNextAlarm(clock), false);
        assertEquals(day1.isNextAlarm(clock), false);
        assertEquals(day2.isNextAlarm(clock), true);
    }

    @Test
    public void after2() {
        FixedClock clock = new FixedClock(new GregorianCalendar(YEAR, MONTH, DAY + 2, HOUR_DAY + 2, MINUTE_DAY + 2)).addMinute(1);

        // set all defaults to disabled
        for (int dayOfWeek : AlarmDataSource.allDaysOfWeek) {
            Defaults defaults = new Defaults();
            defaults.setDayOfWeek(dayOfWeek);
            defaults.setState(Defaults.STATE_DISABLED);
            defaults.setHour(1);
            defaults.setMinute(2);

            Analytics analytics = new Analytics(Analytics.Channel.Activity, Analytics.ChannelName.Calendar);

            globalManager.saveDefault(defaults, analytics);
        }

        assertEquals(day0.isNextAlarm(clock), false);
        assertEquals(day1.isNextAlarm(clock), false);
        assertEquals(day2.isNextAlarm(clock), false);
    }
}
