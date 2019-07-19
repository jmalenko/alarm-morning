package cz.jaro.alarmmorning;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowAlarmManager;
import org.robolectric.shadows.ShadowPendingIntent;

import java.util.Calendar;
import java.util.GregorianCalendar;

import cz.jaro.alarmmorning.checkalarmtime.CheckAlarmTime;
import cz.jaro.alarmmorning.checkalarmtime.CheckAlarmTimeAlarmReceiver;
import cz.jaro.alarmmorning.model.DayTest;
import cz.jaro.alarmmorning.nighttimebell.NighttimeBell;
import cz.jaro.alarmmorning.nighttimebell.NighttimeBellAlarmReceiver;
import cz.jaro.alarmmorning.receivers.AlarmReceiver;
import cz.jaro.alarmmorning.receivers.BootReceiver;
import cz.jaro.alarmmorning.shadows.ShadowGlobalManager;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

/**
 * Tests of Boot Receiver.
 */
@Config(shadows = {ShadowGlobalManager.class})
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class BootReceiverTest extends FixedTimeTest {

    private Context context;
    private ShadowAlarmManager shadowAlarmManager;

    @Before
    public void before() {
        super.before();

        context = RuntimeEnvironment.application.getApplicationContext();

        AlarmManager alarmManager = (AlarmManager) RuntimeEnvironment.application.getSystemService(Context.ALARM_SERVICE);
        shadowAlarmManager = Shadows.shadowOf(alarmManager);

        // Initialize same as after booting
        BroadcastReceiver receiver = new BootReceiver();
        Intent intent = new Intent(Intent.ACTION_BOOT_COMPLETED);
        receiver.onReceive(context, intent);
    }

    @Test
    public void t00_allSystemAlarms() {
        assertThat("Alarm count", shadowAlarmManager.getScheduledAlarms().size(), is(3));

        // System alarm for Check alarm time
        assertSystemAlarm(DayTest.YEAR, DayTest.MONTH, DayTest.DAY, 22, 0, CheckAlarmTimeAlarmReceiver.class, CheckAlarmTime.ACTION_CHECK_ALARM_TIME);
        shadowAlarmManager.getNextScheduledAlarm();

        // System alarm for Nighttime bell
        assertSystemAlarm(DayTest.YEAR, DayTest.MONTH, DayTest.DAY, 22, 0, NighttimeBellAlarmReceiver.class, NighttimeBell.ACTION_PLAY);
        shadowAlarmManager.getNextScheduledAlarm();

        // System alarm for Alarm
        assertSystemAlarm(DayTest.YEAR, DayTest.MONTH, DayTest.DAY + 1, 0, 0, AlarmReceiver.class, SystemAlarm.ACTION_SET_SYSTEM_ALARM);
        shadowAlarmManager.getNextScheduledAlarm();
    }

    private void assertSystemAlarm(int year, int month, int day, int hour, int minute, Class<?> cls, String action) {
        ShadowAlarmManager.ScheduledAlarm nextScheduledAlarm = shadowAlarmManager.peekNextScheduledAlarm();

        assertThat("Type", nextScheduledAlarm.type, is(AlarmManager.RTC_WAKEUP));

        Calendar time = GregorianCalendar.getInstance();
        time.setTimeInMillis(nextScheduledAlarm.triggerAtTime);
        assertThat("Year", time.get(Calendar.YEAR), is(year));
        assertThat("Month", time.get(Calendar.MONTH), is(month));
        assertThat("Date", time.get(Calendar.DAY_OF_MONTH), is(day));
        assertThat("Hour", time.get(Calendar.HOUR_OF_DAY), is(hour));
        assertThat("Minute", time.get(Calendar.MINUTE), is(minute));
        assertThat("Second", time.get(Calendar.SECOND), is(0));
        assertThat("Millisecond", time.get(Calendar.MILLISECOND), is(0));

        PendingIntent operation = nextScheduledAlarm.operation;
        ShadowPendingIntent shadowPendingIntent = Shadows.shadowOf(operation);

        Intent expectedIntent = new Intent(context, cls);

        assertThat("Broadcast", shadowPendingIntent.isBroadcastIntent(), is(true));
        assertThat("Intent count", shadowPendingIntent.getSavedIntents().length, is(1));
        assertThat("Class", shadowPendingIntent.getSavedIntents()[0].getComponent(), is(expectedIntent.getComponent()));
        assertThat("Action", shadowPendingIntent.getSavedIntent().getAction(), is(action));
    }

}
