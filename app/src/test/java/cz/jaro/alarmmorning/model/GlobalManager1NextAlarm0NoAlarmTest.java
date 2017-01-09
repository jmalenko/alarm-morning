package cz.jaro.alarmmorning.model;

import org.junit.Test;

import java.util.Calendar;

import cz.jaro.alarmmorning.Analytics;
import cz.jaro.alarmmorning.FixedTimeTest;
import cz.jaro.alarmmorning.GlobalManager;
import cz.jaro.alarmmorning.clock.Clock;
import cz.jaro.alarmmorning.clock.FixedClock;

import static cz.jaro.alarmmorning.model.DayTest.HOUR_DAY;
import static cz.jaro.alarmmorning.model.DayTest.HOUR_DEFAULT;
import static cz.jaro.alarmmorning.model.DayTest.MINUTE_DAY;
import static cz.jaro.alarmmorning.model.DayTest.MINUTE_DEFAULT;
import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertNull;

/**
 * Tests when no alarm should ring. Holiday is not defined.
 */
public class GlobalManager1NextAlarm0NoAlarmTest extends FixedTimeTest {

    static final int RANGE = 40; // Note: Large values result in longer run times

    @Test
    public void t00_preconditions() {
        assertTrue("Range includes horizon", GlobalManager.HORIZON_DAYS < RANGE);
    }

    @Test
    public void t10_noAlarmNow() {
        Clock clock = globalManager.clock();
        Calendar nextAlarm = globalManager.getNextAlarm(clock);

        assertNull(nextAlarm);
    }

    @Test
    public void t11_noAlarmAnytime() {
        Calendar now = globalManager.clock().now();
        for (int i = -RANGE; i <= RANGE; i++) {
            Calendar date = (Calendar) now.clone();
            date.add(Calendar.DATE, i);

            FixedClock clock = new FixedClock(date);

            Calendar nextAlarm = globalManager.getNextAlarm(clock);

            String str = " on " + date.getTime();

            assertNull("There should be no alarm" + str, nextAlarm);
        }
    }

    private void insertDisabledValues() {
        // Days
        for (int i = -RANGE; i <= RANGE; i++) {
            Calendar now = globalManager.clock().now();
            Calendar date = (Calendar) now.clone();
            date.add(Calendar.DATE, i);

            Day day = new Day();
            day.setDate(date);
            day.setState(Day.STATE_RULE);
            day.setHour(HOUR_DAY);
            day.setMinute(MINUTE_DAY);

            Defaults defaults = new Defaults();
            int dayOfWeek = date.get(Calendar.DAY_OF_WEEK);
            defaults.setDayOfWeek(dayOfWeek);
            defaults.setState(Defaults.STATE_DISABLED);
            defaults.setHour(HOUR_DEFAULT);
            defaults.setMinute(MINUTE_DEFAULT);

            day.setDefaults(defaults);

            Analytics analytics = new Analytics(Analytics.Channel.Test, Analytics.ChannelName.Calendar);

            globalManager.saveDay(day, analytics);
        }

        // Defaults
        for (int dayOfWeek : AlarmDataSource.allDaysOfWeek) {
            Defaults defaults = new Defaults();
            defaults.setDayOfWeek(dayOfWeek);
            defaults.setState(Defaults.STATE_DISABLED);
            defaults.setHour(HOUR_DEFAULT);
            defaults.setMinute(MINUTE_DEFAULT);

            Analytics analytics = new Analytics(Analytics.Channel.Test, Analytics.ChannelName.Defaults);

            globalManager.saveDefault(defaults, analytics);
        }
    }

    @Test
    public void t20_noAlarmAnytimeWithFullDatabase() {
        insertDisabledValues();

        Calendar now = globalManager.clock().now();
        for (int i = -RANGE; i <= RANGE; i++) {
            Calendar date = (Calendar) now.clone();
            date.add(Calendar.DATE, i);

            FixedClock clock = new FixedClock(date);

            Calendar nextAlarm = globalManager.getNextAlarm(clock);

            String str = " on " + date.getTime();

            assertNull("There should be no alarm" + str, nextAlarm);
        }
    }

}