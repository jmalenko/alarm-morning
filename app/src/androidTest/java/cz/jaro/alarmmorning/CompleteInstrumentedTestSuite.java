package cz.jaro.alarmmorning;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import cz.jaro.alarmmorning.model.AlarmDataSource1Test;
import cz.jaro.alarmmorning.model.AlarmDataSource2Test;
import cz.jaro.alarmmorning.model.AlarmDataSource3Test;
import cz.jaro.alarmmorning.model.AlarmDataSource4OneTimeAlarmTest;
import cz.jaro.alarmmorning.model.Day4Test;
import cz.jaro.alarmmorning.nighttimebell.CustomAlarmToneTest;

/**
 * Complete set of instrumented tests.
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({
        AlarmDataSource1Test.class,
        AlarmDataSource2Test.class,
        AlarmDataSource3Test.class,
        AlarmDataSource4OneTimeAlarmTest.class,

        Day4Test.class,

        LocalizationTest.class,

        CustomAlarmToneTest.class
})
public class CompleteInstrumentedTestSuite {
}
