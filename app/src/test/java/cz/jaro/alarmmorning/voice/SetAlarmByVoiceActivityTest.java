package cz.jaro.alarmmorning.voice;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.provider.AlarmClock;

import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowActivity;

import java.util.Calendar;
import java.util.List;

import cz.jaro.alarmmorning.AlarmMorningActivityTest;
import cz.jaro.alarmmorning.FixedTimeTest;
import cz.jaro.alarmmorning.GlobalManager;
import cz.jaro.alarmmorning.model.DayTest;
import cz.jaro.alarmmorning.model.OneTimeAlarm;
import cz.jaro.alarmmorning.shadows.ShadowGlobalManager;

import static cz.jaro.alarmmorning.app.CalendarWithOneTimeAlarmTest.ONE_TIME_ALARM_HOUR;
import static cz.jaro.alarmmorning.app.CalendarWithOneTimeAlarmTest.ONE_TIME_ALARM_MINUTE;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.robolectric.Robolectric.buildActivity;

/**
 * Tests of alarm set by voice
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 21, shadows = {ShadowGlobalManager.class})
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class SetAlarmByVoiceActivityTest extends FixedTimeTest {
    Context context;

    GlobalManager globalManager;

    Activity activity;
    ShadowActivity shadowActivity;

    @Before
    public void before() {
        super.before();

        context = RuntimeEnvironment.application.getApplicationContext();

        AlarmMorningActivityTest.setLocale(context, "en", "US");

        globalManager = GlobalManager.getInstance();
    }

    @Test
    public void precondition() {
        List<OneTimeAlarm> oneTimeAlarms = globalManager.loadOneTimeAlarms();

        assertThat("The number of one-time alarms", oneTimeAlarms.size(), is(0));
    }

    @Test
    public void setAlarm() {
        Intent intent = new Intent(context, SetAlarmByVoiceActivity.class);
        intent.setAction(AlarmClock.ACTION_SET_ALARM);
        intent.putExtra(AlarmClock.EXTRA_HOUR, ONE_TIME_ALARM_HOUR);
        intent.putExtra(AlarmClock.EXTRA_MINUTES, ONE_TIME_ALARM_MINUTE);

        startSetAlarmByVoiceActivity(intent);

        // Check the alarm was added
        List<OneTimeAlarm> oneTimeAlarms = globalManager.loadOneTimeAlarms();

        assertThat("The number of one-time alarms", oneTimeAlarms.size(), is(1));

        OneTimeAlarm oneTimeAlarm = oneTimeAlarms.get(0);

        Calendar cal = oneTimeAlarm.getDateTime();
        assertThat("Year", cal.get(Calendar.YEAR), is(DayTest.YEAR));
        assertThat("Month", cal.get(Calendar.MONTH), is(DayTest.MONTH));
        assertThat("Date", cal.get(Calendar.DAY_OF_MONTH), is(DayTest.DAY));
        assertThat("Hour", cal.get(Calendar.HOUR_OF_DAY), is(ONE_TIME_ALARM_HOUR));
        assertThat("Minute", cal.get(Calendar.MINUTE), is(ONE_TIME_ALARM_MINUTE));
        assertThat("Second", cal.get(Calendar.SECOND), is(0));

        assertNull("Name", oneTimeAlarm.getName());
    }

    @Test
    public void setAlarm_withName() {
        Intent intent = new Intent(context, SetAlarmByVoiceActivity.class);
        intent.setAction(AlarmClock.ACTION_SET_ALARM);
        intent.putExtra(AlarmClock.EXTRA_HOUR, ONE_TIME_ALARM_HOUR);
        intent.putExtra(AlarmClock.EXTRA_MINUTES, ONE_TIME_ALARM_MINUTE);
        intent.putExtra(AlarmClock.EXTRA_MESSAGE, "Tea");

        startSetAlarmByVoiceActivity(intent);

        // Check the alarm was added
        List<OneTimeAlarm> oneTimeAlarms = globalManager.loadOneTimeAlarms();

        assertThat("The number of one-time alarms", oneTimeAlarms.size(), is(1));

        OneTimeAlarm oneTimeAlarm = oneTimeAlarms.get(0);

        Calendar cal = oneTimeAlarm.getDateTime();
        assertThat("Year", cal.get(Calendar.YEAR), is(DayTest.YEAR));
        assertThat("Month", cal.get(Calendar.MONTH), is(DayTest.MONTH));
        assertThat("Date", cal.get(Calendar.DAY_OF_MONTH), is(DayTest.DAY));
        assertThat("Hour", cal.get(Calendar.HOUR_OF_DAY), is(ONE_TIME_ALARM_HOUR));
        assertThat("Minute", cal.get(Calendar.MINUTE), is(ONE_TIME_ALARM_MINUTE));
        assertThat("Second", cal.get(Calendar.SECOND), is(0));

        assertThat("Name", oneTimeAlarm.getName(), is("Tea"));
    }

    @Test
    public void setTimer() {
        Intent intent = new Intent(context, SetAlarmByVoiceActivity.class);
        intent.setAction(AlarmClock.ACTION_SET_TIMER);
        intent.putExtra(AlarmClock.EXTRA_LENGTH, 600);

        startSetAlarmByVoiceActivity(intent);

        // Check the alarm was added
        List<OneTimeAlarm> oneTimeAlarms = globalManager.loadOneTimeAlarms();

        assertThat("The number of one-time alarms", oneTimeAlarms.size(), is(1));

        OneTimeAlarm oneTimeAlarm = oneTimeAlarms.get(0);

        Calendar cal = oneTimeAlarm.getDateTime();
        assertThat("Year", cal.get(Calendar.YEAR), is(DayTest.YEAR));
        assertThat("Month", cal.get(Calendar.MONTH), is(DayTest.MONTH));
        assertThat("Date", cal.get(Calendar.DAY_OF_MONTH), is(DayTest.DAY));
        assertThat("Hour", cal.get(Calendar.HOUR_OF_DAY), is(DayTest.HOUR));
        assertThat("Minute", cal.get(Calendar.MINUTE), is(DayTest.MINUTE + 10));
        assertThat("Second", cal.get(Calendar.SECOND), is(0));

        assertThat("Name", oneTimeAlarm.getName(), is("10m"));
    }

    @Test
    public void setTimer_withName() {
        Intent intent = new Intent(context, SetAlarmByVoiceActivity.class);
        intent.setAction(AlarmClock.ACTION_SET_TIMER);
        intent.putExtra(AlarmClock.EXTRA_LENGTH, 600);
        intent.putExtra(AlarmClock.EXTRA_MESSAGE, "Tea");

        startSetAlarmByVoiceActivity(intent);

        // Check the alarm was added
        List<OneTimeAlarm> oneTimeAlarms = globalManager.loadOneTimeAlarms();

        assertThat("The number of one-time alarms", oneTimeAlarms.size(), is(1));

        OneTimeAlarm oneTimeAlarm = oneTimeAlarms.get(0);

        Calendar cal = oneTimeAlarm.getDateTime();
        assertThat("Year", cal.get(Calendar.YEAR), is(DayTest.YEAR));
        assertThat("Month", cal.get(Calendar.MONTH), is(DayTest.MONTH));
        assertThat("Date", cal.get(Calendar.DAY_OF_MONTH), is(DayTest.DAY));
        assertThat("Hour", cal.get(Calendar.HOUR_OF_DAY), is(DayTest.HOUR));
        assertThat("Minute", cal.get(Calendar.MINUTE), is(DayTest.MINUTE + 10));
        assertThat("Second", cal.get(Calendar.SECOND), is(0));

        assertThat("Name", oneTimeAlarm.getName(), is("Tea"));
    }

    @Test
    public void setTimer_10seconds() {
        Intent intent = new Intent(context, SetAlarmByVoiceActivity.class);
        intent.setAction(AlarmClock.ACTION_SET_TIMER);
        intent.putExtra(AlarmClock.EXTRA_LENGTH, 10);

        startSetAlarmByVoiceActivity(intent);

        // Check the alarm was added
        List<OneTimeAlarm> oneTimeAlarms = globalManager.loadOneTimeAlarms();

        assertThat("The number of one-time alarms", oneTimeAlarms.size(), is(1));

        OneTimeAlarm oneTimeAlarm = oneTimeAlarms.get(0);

        Calendar cal = oneTimeAlarm.getDateTime();
        assertThat("Year", cal.get(Calendar.YEAR), is(DayTest.YEAR));
        assertThat("Month", cal.get(Calendar.MONTH), is(DayTest.MONTH));
        assertThat("Date", cal.get(Calendar.DAY_OF_MONTH), is(DayTest.DAY));
        assertThat("Hour", cal.get(Calendar.HOUR_OF_DAY), is(DayTest.HOUR));
        assertThat("Minute", cal.get(Calendar.MINUTE), is(DayTest.MINUTE));
        assertThat("Second", cal.get(Calendar.SECOND), is(10));

        assertThat("Name", oneTimeAlarm.getName(), is("10s"));
    }

    private void startSetAlarmByVoiceActivity(Intent intent) {
        // Start activity
        activity = buildActivity(SetAlarmByVoiceActivity.class, intent).setup().get();
        shadowActivity = Shadows.shadowOf(activity);

        AlarmMorningActivityTest.setLocale(activity, "en", "US");
    }

}
