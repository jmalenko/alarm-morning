import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import cz.jaro.alarmmorning.AlarmMorningActivity0Test;
import cz.jaro.alarmmorning.AlarmMorningActivityTest;
import cz.jaro.alarmmorning.BootReceiverTest;
import cz.jaro.alarmmorning.CalendarTest;
import cz.jaro.alarmmorning.CalendarWithOneTimeAlarmTest;
import cz.jaro.alarmmorning.clock.FixedClockTest;
import cz.jaro.alarmmorning.clock.SystemClockTest;
import cz.jaro.alarmmorning.holiday.HolidayHelper1HolidaysTest;
import cz.jaro.alarmmorning.holiday.HolidayHelperTest;
import cz.jaro.alarmmorning.model.Day2Test;
import cz.jaro.alarmmorning.model.DayTest;
import cz.jaro.alarmmorning.model.DefaultsTest;
import cz.jaro.alarmmorning.model.GlobalManager1NextAlarm0NoAlarmTest;
import cz.jaro.alarmmorning.model.GlobalManager1NextAlarm1DayTest;
import cz.jaro.alarmmorning.model.GlobalManager1NextAlarm2DefaultTest;
import cz.jaro.alarmmorning.model.GlobalManager1NextAlarm3DefaultAndDayTest;
import cz.jaro.alarmmorning.model.GlobalManager1NextAlarm4HolidaysTest;
import cz.jaro.alarmmorning.model.GlobalManager1NextAlarm5OneTimeAlarmTest;
import cz.jaro.alarmmorning.model.GlobalManager1NextAlarm6TwoOneTimeAlarmsTest;
import cz.jaro.alarmmorning.model.GlobalManager1NextAlarm7TwoOneTimeAlarmsAtTheSameTimeTest;

/**
 * Complete set of instrumented tests.
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({
        DayTest.class,
        Day2Test.class,
        DefaultsTest.class,

        SystemClockTest.class,
        FixedClockTest.class,

        BootReceiverTest.class,

        HolidayHelper1HolidaysTest.class,
        HolidayHelperTest.class,

        GlobalManager1NextAlarm0NoAlarmTest.class,
        GlobalManager1NextAlarm1DayTest.class,
        GlobalManager1NextAlarm2DefaultTest.class,
        GlobalManager1NextAlarm3DefaultAndDayTest.class,
        GlobalManager1NextAlarm4HolidaysTest.class,
        GlobalManager1NextAlarm5OneTimeAlarmTest.class,
        GlobalManager1NextAlarm6TwoOneTimeAlarmsTest.class,
        GlobalManager1NextAlarm7TwoOneTimeAlarmsAtTheSameTimeTest.class,

        CalendarTest.class,
        CalendarWithOneTimeAlarmTest.class,

        AlarmMorningActivity0Test.class,
        AlarmMorningActivityTest.class
})
public class CompleteUnitTestSuite {
}
