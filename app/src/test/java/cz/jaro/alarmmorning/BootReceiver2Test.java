package cz.jaro.alarmmorning;

import android.app.AlarmManager;
import android.content.Context;

import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowAlarmManager;

import java.util.GregorianCalendar;

import cz.jaro.alarmmorning.app.AlarmMorningAppTest;
import cz.jaro.alarmmorning.app.CalendarWithOneTimeAlarmTest;
import cz.jaro.alarmmorning.checkalarmtime.CheckAlarmTime;
import cz.jaro.alarmmorning.checkalarmtime.CheckAlarmTimeAlarmReceiver;
import cz.jaro.alarmmorning.clock.FixedClock;
import cz.jaro.alarmmorning.model.DayTest;
import cz.jaro.alarmmorning.model.OneTimeAlarm;
import cz.jaro.alarmmorning.nighttimebell.NighttimeBell;
import cz.jaro.alarmmorning.nighttimebell.NighttimeBellAlarmReceiver;
import cz.jaro.alarmmorning.receivers.AlarmReceiver;
import cz.jaro.alarmmorning.receivers.VoidReceiver;
import cz.jaro.alarmmorning.shadows.ShadowGlobalManager;

import static cz.jaro.alarmmorning.app.CalendarWithOneTimeAlarmTest.ONE_TIME_ALARM_HOUR;
import static cz.jaro.alarmmorning.app.CalendarWithOneTimeAlarmTest.ONE_TIME_ALARM_MINUTE;
import static cz.jaro.alarmmorning.model.DayTest.DAY;
import static cz.jaro.alarmmorning.model.DayTest.MONTH;
import static cz.jaro.alarmmorning.model.DayTest.YEAR;

/**
 * Tests of Boot Receiver, specifically proper resuming.
 */
@Config(shadows = {ShadowGlobalManager.class})
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class BootReceiver2Test extends FixedTimeTest {

    private Context context;
    private ShadowAlarmManager shadowAlarmManager;
    private CalendarWithOneTimeAlarmTest test;

    private OneTimeAlarm alarm1;

    private static final int ONE_TIME_ALARM_HOUR2 = 10;

    @Before
    public void before() {
        super.before();

        context = RuntimeEnvironment.application.getApplicationContext();

        AlarmManager alarmManager = (AlarmManager) RuntimeEnvironment.application.getSystemService(Context.ALARM_SERVICE);
        shadowAlarmManager = Shadows.shadowOf(alarmManager);

        // Prepare dependency on another test (and consume the system alarms)
        test = new CalendarWithOneTimeAlarmTest();
        test.before();

        assertSystemAlarmCount(1);
        assertAndConsumeSystemAlarm(DayTest.YEAR, DayTest.MONTH, DayTest.DAY + 1, 0, 0, AlarmReceiver.class, SystemAlarm.ACTION_SET_SYSTEM_ALARM);

        // Add alarm 1
        alarm1 = test.setAlarm(new GregorianCalendar(YEAR, MONTH, DAY, ONE_TIME_ALARM_HOUR, ONE_TIME_ALARM_MINUTE));

        assertSystemAlarmCount(2);
        assertAndConsumeSystemAlarm(DayTest.YEAR, DayTest.MONTH, DayTest.DAY, ONE_TIME_ALARM_HOUR - 2, ONE_TIME_ALARM_MINUTE, AlarmReceiver.class, SystemAlarm.ACTION_RING_IN_NEAR_FUTURE);
        assertAndConsumeSystemAlarm(DayTest.YEAR, DayTest.MONTH, DayTest.DAY, ONE_TIME_ALARM_HOUR, ONE_TIME_ALARM_MINUTE, VoidReceiver.class, null);

        // Add alarm 2
        test.setAlarm(new GregorianCalendar(YEAR, MONTH, DAY, ONE_TIME_ALARM_HOUR2, ONE_TIME_ALARM_MINUTE));

        assertSystemAlarmCount(0);
    }

    @Test
    public void t100_farBefore1stAlarm() {
        shadowGlobalManager.setClock(new FixedClock(new GregorianCalendar(YEAR, MONTH, DAY, ONE_TIME_ALARM_HOUR - 3, ONE_TIME_ALARM_MINUTE, 0)));
        reboot();

        assertSystemAlarmCount(4);

        assertAndConsumeSystemAlarm(DayTest.YEAR, DayTest.MONTH, DayTest.DAY, ONE_TIME_ALARM_HOUR - 2, ONE_TIME_ALARM_MINUTE, AlarmReceiver.class, SystemAlarm.ACTION_RING_IN_NEAR_FUTURE);
        assertAndConsumeSystemAlarm(DayTest.YEAR, DayTest.MONTH, DayTest.DAY, ONE_TIME_ALARM_HOUR, ONE_TIME_ALARM_MINUTE, VoidReceiver.class, null); // SystemAlarmClock
        assertAndConsumeSystemAlarm(DayTest.YEAR, DayTest.MONTH, DayTest.DAY, 22, 0, CheckAlarmTimeAlarmReceiver.class, CheckAlarmTime.ACTION_CHECK_ALARM_TIME);
        assertAndConsumeSystemAlarm(DayTest.YEAR, DayTest.MONTH, DayTest.DAY, 22, 0, NighttimeBellAlarmReceiver.class, NighttimeBell.ACTION_PLAY);

        checkNoActivity();
    }

    @Test
    public void t110_justBefore1stAlarm() {
        shadowGlobalManager.setClock(new FixedClock(new GregorianCalendar(YEAR, MONTH, DAY, ONE_TIME_ALARM_HOUR, ONE_TIME_ALARM_MINUTE - 1, 0)));
        reboot();

        assertSystemAlarmCount(4);

        assertAndConsumeSystemAlarm(DayTest.YEAR, DayTest.MONTH, DayTest.DAY, ONE_TIME_ALARM_HOUR, ONE_TIME_ALARM_MINUTE, AlarmReceiver.class, SystemAlarm.ACTION_RING);
        assertAndConsumeSystemAlarm(DayTest.YEAR, DayTest.MONTH, DayTest.DAY, ONE_TIME_ALARM_HOUR, ONE_TIME_ALARM_MINUTE, VoidReceiver.class, null); // SystemAlarmClock
        assertAndConsumeSystemAlarm(DayTest.YEAR, DayTest.MONTH, DayTest.DAY, 22, 0, CheckAlarmTimeAlarmReceiver.class, CheckAlarmTime.ACTION_CHECK_ALARM_TIME);
        assertAndConsumeSystemAlarm(DayTest.YEAR, DayTest.MONTH, DayTest.DAY, 22, 0, NighttimeBellAlarmReceiver.class, NighttimeBell.ACTION_PLAY);

        checkNoActivity();
    }

    @Test
    public void t120_justAfter1stAlarm() {
        shadowGlobalManager.setClock(new FixedClock(new GregorianCalendar(YEAR, MONTH, DAY, ONE_TIME_ALARM_HOUR, ONE_TIME_ALARM_MINUTE + 1, 0)));
        reboot();

        assertSystemAlarmCount(4);

        assertAndConsumeSystemAlarm(DayTest.YEAR, DayTest.MONTH, DayTest.DAY, ONE_TIME_ALARM_HOUR2 - 2, ONE_TIME_ALARM_MINUTE, AlarmReceiver.class, SystemAlarm.ACTION_RING_IN_NEAR_FUTURE);
        assertAndConsumeSystemAlarm(DayTest.YEAR, DayTest.MONTH, DayTest.DAY, ONE_TIME_ALARM_HOUR2, ONE_TIME_ALARM_MINUTE, VoidReceiver.class, null); // SystemAlarmClock
        assertAndConsumeSystemAlarm(DayTest.YEAR, DayTest.MONTH, DayTest.DAY, 22, 0, CheckAlarmTimeAlarmReceiver.class, CheckAlarmTime.ACTION_CHECK_ALARM_TIME);
        assertAndConsumeSystemAlarm(DayTest.YEAR, DayTest.MONTH, DayTest.DAY, 22, 0, NighttimeBellAlarmReceiver.class, NighttimeBell.ACTION_PLAY);

        checkSkippedNotification(1);

        checkRingingActivity();
    }

    @Test
    public void t130_farAfter1stAlarm() {
        shadowGlobalManager.setClock(new FixedClock(new GregorianCalendar(YEAR, MONTH, DAY, ONE_TIME_ALARM_HOUR, ONE_TIME_ALARM_MINUTE + 31, 0)));
        reboot();

        assertSystemAlarmCount(4);

        assertAndConsumeSystemAlarm(DayTest.YEAR, DayTest.MONTH, DayTest.DAY, ONE_TIME_ALARM_HOUR2 - 2, ONE_TIME_ALARM_MINUTE, AlarmReceiver.class, SystemAlarm.ACTION_RING_IN_NEAR_FUTURE);
        assertAndConsumeSystemAlarm(DayTest.YEAR, DayTest.MONTH, DayTest.DAY, ONE_TIME_ALARM_HOUR2, ONE_TIME_ALARM_MINUTE, VoidReceiver.class, null); // SystemAlarmClock
        assertAndConsumeSystemAlarm(DayTest.YEAR, DayTest.MONTH, DayTest.DAY, 22, 0, CheckAlarmTimeAlarmReceiver.class, CheckAlarmTime.ACTION_CHECK_ALARM_TIME);
        assertAndConsumeSystemAlarm(DayTest.YEAR, DayTest.MONTH, DayTest.DAY, 22, 0, NighttimeBellAlarmReceiver.class, NighttimeBell.ACTION_PLAY);

        checkSkippedNotification(1);

        checkNoActivity();
    }

    @Test
    public void t140_justBefore2ndAlarm() {
        shadowGlobalManager.setClock(new FixedClock(new GregorianCalendar(YEAR, MONTH, DAY, ONE_TIME_ALARM_HOUR2, ONE_TIME_ALARM_MINUTE - 1, 0)));
        reboot();

        assertSystemAlarmCount(4);

        assertAndConsumeSystemAlarm(DayTest.YEAR, DayTest.MONTH, DayTest.DAY, ONE_TIME_ALARM_HOUR2, ONE_TIME_ALARM_MINUTE, AlarmReceiver.class, SystemAlarm.ACTION_RING);
        assertAndConsumeSystemAlarm(DayTest.YEAR, DayTest.MONTH, DayTest.DAY, ONE_TIME_ALARM_HOUR2, ONE_TIME_ALARM_MINUTE, VoidReceiver.class, null); // SystemAlarmClock
        assertAndConsumeSystemAlarm(DayTest.YEAR, DayTest.MONTH, DayTest.DAY, 22, 0, CheckAlarmTimeAlarmReceiver.class, CheckAlarmTime.ACTION_CHECK_ALARM_TIME);
        assertAndConsumeSystemAlarm(DayTest.YEAR, DayTest.MONTH, DayTest.DAY, 22, 0, NighttimeBellAlarmReceiver.class, NighttimeBell.ACTION_PLAY);

        checkSkippedNotification(1);

        checkNoActivity();
    }

    @Test
    public void t150_justAfter2ndAlarm() {
        shadowGlobalManager.setClock(new FixedClock(new GregorianCalendar(YEAR, MONTH, DAY, ONE_TIME_ALARM_HOUR2, ONE_TIME_ALARM_MINUTE + 1, 0)));
        reboot();

        assertSystemAlarmCount(3);

        assertAndConsumeSystemAlarm(DayTest.YEAR, DayTest.MONTH, DayTest.DAY, 22, 0, CheckAlarmTimeAlarmReceiver.class, CheckAlarmTime.ACTION_CHECK_ALARM_TIME);
        assertAndConsumeSystemAlarm(DayTest.YEAR, DayTest.MONTH, DayTest.DAY, 22, 0, NighttimeBellAlarmReceiver.class, NighttimeBell.ACTION_PLAY);
        assertAndConsumeSystemAlarm(DayTest.YEAR, DayTest.MONTH, DayTest.DAY + 1, 0, 0, AlarmReceiver.class, SystemAlarm.ACTION_SET_SYSTEM_ALARM);

        checkSkippedNotification(2);

        checkRingingActivity();
    }

    @Test
    public void t160_farAfter2ndAlarm() {
        shadowGlobalManager.setClock(new FixedClock(new GregorianCalendar(YEAR, MONTH, DAY, ONE_TIME_ALARM_HOUR2, ONE_TIME_ALARM_MINUTE + 31, 0)));
        reboot();

        assertSystemAlarmCount(3);

        assertAndConsumeSystemAlarm(DayTest.YEAR, DayTest.MONTH, DayTest.DAY, 22, 0, CheckAlarmTimeAlarmReceiver.class, CheckAlarmTime.ACTION_CHECK_ALARM_TIME);
        assertAndConsumeSystemAlarm(DayTest.YEAR, DayTest.MONTH, DayTest.DAY, 22, 0, NighttimeBellAlarmReceiver.class, NighttimeBell.ACTION_PLAY);
        assertAndConsumeSystemAlarm(DayTest.YEAR, DayTest.MONTH, DayTest.DAY + 1, 0, 0, AlarmReceiver.class, SystemAlarm.ACTION_SET_SYSTEM_ALARM);

        checkSkippedNotification(2);

        checkNoActivity();
    }

    @Test
    public void t200_ringing_justBefore2ndAlarm() {
        t120_justAfter1stAlarm();
        shadowGlobalManager.setClock(new FixedClock(new GregorianCalendar(YEAR, MONTH, DAY, ONE_TIME_ALARM_HOUR2, ONE_TIME_ALARM_MINUTE - 1, 0)));
        reboot();

        assertSystemAlarmCount(4);

        assertAndConsumeSystemAlarm(DayTest.YEAR, DayTest.MONTH, DayTest.DAY, ONE_TIME_ALARM_HOUR2, ONE_TIME_ALARM_MINUTE, AlarmReceiver.class, SystemAlarm.ACTION_RING);
        assertAndConsumeSystemAlarm(DayTest.YEAR, DayTest.MONTH, DayTest.DAY, ONE_TIME_ALARM_HOUR2, ONE_TIME_ALARM_MINUTE, VoidReceiver.class, null); // SystemAlarmClock
        assertAndConsumeSystemAlarm(DayTest.YEAR, DayTest.MONTH, DayTest.DAY, 22, 0, CheckAlarmTimeAlarmReceiver.class, CheckAlarmTime.ACTION_CHECK_ALARM_TIME);
        assertAndConsumeSystemAlarm(DayTest.YEAR, DayTest.MONTH, DayTest.DAY, 22, 0, NighttimeBellAlarmReceiver.class, NighttimeBell.ACTION_PLAY);

        checkSkippedNotification(0);

        checkRingingActivity();
    }

    @Test
    public void t210_ringing_justAfter2ndAlarm() {
        t120_justAfter1stAlarm();
        shadowGlobalManager.setClock(new FixedClock(new GregorianCalendar(YEAR, MONTH, DAY, ONE_TIME_ALARM_HOUR2, ONE_TIME_ALARM_MINUTE + 1, 0)));
        reboot();

        assertSystemAlarmCount(3);

        assertAndConsumeSystemAlarm(DayTest.YEAR, DayTest.MONTH, DayTest.DAY, 22, 0, CheckAlarmTimeAlarmReceiver.class, CheckAlarmTime.ACTION_CHECK_ALARM_TIME);
        assertAndConsumeSystemAlarm(DayTest.YEAR, DayTest.MONTH, DayTest.DAY, 22, 0, NighttimeBellAlarmReceiver.class, NighttimeBell.ACTION_PLAY);
        assertAndConsumeSystemAlarm(DayTest.YEAR, DayTest.MONTH, DayTest.DAY + 1, 0, 0, AlarmReceiver.class, SystemAlarm.ACTION_SET_SYSTEM_ALARM);
        checkSkippedNotification(2);

        checkRingingActivity();
    }

    @Test
    public void t220_ringing_farAfter2ndAlarm() {
        t120_justAfter1stAlarm();
        shadowGlobalManager.setClock(new FixedClock(new GregorianCalendar(YEAR, MONTH, DAY, ONE_TIME_ALARM_HOUR2, ONE_TIME_ALARM_MINUTE + 31, 0)));
        reboot();

        assertSystemAlarmCount(3);

        assertAndConsumeSystemAlarm(DayTest.YEAR, DayTest.MONTH, DayTest.DAY, 22, 0, CheckAlarmTimeAlarmReceiver.class, CheckAlarmTime.ACTION_CHECK_ALARM_TIME);
        assertAndConsumeSystemAlarm(DayTest.YEAR, DayTest.MONTH, DayTest.DAY, 22, 0, NighttimeBellAlarmReceiver.class, NighttimeBell.ACTION_PLAY);
        assertAndConsumeSystemAlarm(DayTest.YEAR, DayTest.MONTH, DayTest.DAY + 1, 0, 0, AlarmReceiver.class, SystemAlarm.ACTION_SET_SYSTEM_ALARM);

        checkSkippedNotification(2);

        checkNoActivity();
    }

    @Test
    public void t300_snoozed_inOneMinute() {
        t120_justAfter1stAlarm();
        snooze();

        shadowGlobalManager.setClock(new FixedClock(new GregorianCalendar(YEAR, MONTH, DAY, ONE_TIME_ALARM_HOUR, ONE_TIME_ALARM_MINUTE + 2, 0)));
        reboot();

        assertSystemAlarmCount(3);

        assertAndConsumeSystemAlarm(DayTest.YEAR, DayTest.MONTH, DayTest.DAY, ONE_TIME_ALARM_HOUR, ONE_TIME_ALARM_MINUTE + 11, AlarmReceiver.class, SystemAlarm.ACTION_RING);
        assertAndConsumeSystemAlarm(DayTest.YEAR, DayTest.MONTH, DayTest.DAY, 22, 0, CheckAlarmTimeAlarmReceiver.class, CheckAlarmTime.ACTION_CHECK_ALARM_TIME);
        assertAndConsumeSystemAlarm(DayTest.YEAR, DayTest.MONTH, DayTest.DAY, 22, 0, NighttimeBellAlarmReceiver.class, NighttimeBell.ACTION_PLAY);

        checkSkippedNotification(1);

        checkNoActivity();
    }

    @Test
    public void t310_snoozed_justBefore2ndAlarm() {
        t120_justAfter1stAlarm();
        snooze();

        shadowGlobalManager.setClock(new FixedClock(new GregorianCalendar(YEAR, MONTH, DAY, ONE_TIME_ALARM_HOUR2, ONE_TIME_ALARM_MINUTE - 1, 0)));
        reboot();

        assertSystemAlarmCount(3);

        assertAndConsumeSystemAlarm(DayTest.YEAR, DayTest.MONTH, DayTest.DAY, ONE_TIME_ALARM_HOUR2, ONE_TIME_ALARM_MINUTE, AlarmReceiver.class, SystemAlarm.ACTION_RING);
        assertAndConsumeSystemAlarm(DayTest.YEAR, DayTest.MONTH, DayTest.DAY, 22, 0, CheckAlarmTimeAlarmReceiver.class, CheckAlarmTime.ACTION_CHECK_ALARM_TIME);
        assertAndConsumeSystemAlarm(DayTest.YEAR, DayTest.MONTH, DayTest.DAY, 22, 0, NighttimeBellAlarmReceiver.class, NighttimeBell.ACTION_PLAY);

        checkSkippedNotification(1);

        checkRingingActivity();
    }

    @Test
    public void t320_snoozed_justAfter2ndAlarm() {
        t120_justAfter1stAlarm();
        snooze();

        shadowGlobalManager.setClock(new FixedClock(new GregorianCalendar(YEAR, MONTH, DAY, ONE_TIME_ALARM_HOUR2, ONE_TIME_ALARM_MINUTE + 1, 0)));
        reboot();

        assertSystemAlarmCount(3);

        assertAndConsumeSystemAlarm(DayTest.YEAR, DayTest.MONTH, DayTest.DAY, 22, 0, CheckAlarmTimeAlarmReceiver.class, CheckAlarmTime.ACTION_CHECK_ALARM_TIME);
        assertAndConsumeSystemAlarm(DayTest.YEAR, DayTest.MONTH, DayTest.DAY, 22, 0, NighttimeBellAlarmReceiver.class, NighttimeBell.ACTION_PLAY);
        assertAndConsumeSystemAlarm(DayTest.YEAR, DayTest.MONTH, DayTest.DAY + 1, 0, 0, AlarmReceiver.class, SystemAlarm.ACTION_SET_SYSTEM_ALARM);

        checkSkippedNotification(2);

        checkRingingActivity();
    }

    @Test
    public void t330_snoozed_farAfter2ndAlarm() {
        t120_justAfter1stAlarm();
        snooze();

        shadowGlobalManager.setClock(new FixedClock(new GregorianCalendar(YEAR, MONTH, DAY, ONE_TIME_ALARM_HOUR2, ONE_TIME_ALARM_MINUTE + 31, 0)));
        reboot();

        assertSystemAlarmCount(3);

        assertAndConsumeSystemAlarm(DayTest.YEAR, DayTest.MONTH, DayTest.DAY, 22, 0, CheckAlarmTimeAlarmReceiver.class, CheckAlarmTime.ACTION_CHECK_ALARM_TIME);
        assertAndConsumeSystemAlarm(DayTest.YEAR, DayTest.MONTH, DayTest.DAY, 22, 0, NighttimeBellAlarmReceiver.class, NighttimeBell.ACTION_PLAY);
        assertAndConsumeSystemAlarm(DayTest.YEAR, DayTest.MONTH, DayTest.DAY + 1, 0, 0, AlarmReceiver.class, SystemAlarm.ACTION_SET_SYSTEM_ALARM);

        checkSkippedNotification(1);

        checkNoActivity();
    }


    @Test
    public void t400_dismissed_justBefore2ndAlarm() {
        t120_justAfter1stAlarm();
        dismiss();

        shadowGlobalManager.setClock(new FixedClock(new GregorianCalendar(YEAR, MONTH, DAY, ONE_TIME_ALARM_HOUR2, ONE_TIME_ALARM_MINUTE - 1, 0)));
        reboot();

        assertSystemAlarmCount(4);

        assertAndConsumeSystemAlarm(DayTest.YEAR, DayTest.MONTH, DayTest.DAY, ONE_TIME_ALARM_HOUR2, ONE_TIME_ALARM_MINUTE, AlarmReceiver.class, SystemAlarm.ACTION_RING);
        assertAndConsumeSystemAlarm(DayTest.YEAR, DayTest.MONTH, DayTest.DAY, ONE_TIME_ALARM_HOUR2, ONE_TIME_ALARM_MINUTE, VoidReceiver.class, null); // SystemAlarmClock
        assertAndConsumeSystemAlarm(DayTest.YEAR, DayTest.MONTH, DayTest.DAY, 22, 0, CheckAlarmTimeAlarmReceiver.class, CheckAlarmTime.ACTION_CHECK_ALARM_TIME);
        assertAndConsumeSystemAlarm(DayTest.YEAR, DayTest.MONTH, DayTest.DAY, 22, 0, NighttimeBellAlarmReceiver.class, NighttimeBell.ACTION_PLAY);

        checkSkippedNotification(1);

        checkNoActivity();
    }

    @Test
    public void t410_dismissed_justAfter2ndAlarm() {
        t120_justAfter1stAlarm();
        dismiss();

        shadowGlobalManager.setClock(new FixedClock(new GregorianCalendar(YEAR, MONTH, DAY, ONE_TIME_ALARM_HOUR2, ONE_TIME_ALARM_MINUTE + 1, 0)));
        reboot();

        assertSystemAlarmCount(3);

        assertAndConsumeSystemAlarm(DayTest.YEAR, DayTest.MONTH, DayTest.DAY, 22, 0, CheckAlarmTimeAlarmReceiver.class, CheckAlarmTime.ACTION_CHECK_ALARM_TIME);
        assertAndConsumeSystemAlarm(DayTest.YEAR, DayTest.MONTH, DayTest.DAY, 22, 0, NighttimeBellAlarmReceiver.class, NighttimeBell.ACTION_PLAY);
        assertAndConsumeSystemAlarm(DayTest.YEAR, DayTest.MONTH, DayTest.DAY + 1, 0, 0, AlarmReceiver.class, SystemAlarm.ACTION_SET_SYSTEM_ALARM);

        checkSkippedNotification(1);

        checkRingingActivity();
    }

    @Test
    public void t420_dismissed_farAfter2ndAlarm() {
        t120_justAfter1stAlarm();
        dismiss();

        shadowGlobalManager.setClock(new FixedClock(new GregorianCalendar(YEAR, MONTH, DAY, ONE_TIME_ALARM_HOUR2, ONE_TIME_ALARM_MINUTE + 31, 0)));
        reboot();

        assertSystemAlarmCount(3);

        assertAndConsumeSystemAlarm(DayTest.YEAR, DayTest.MONTH, DayTest.DAY, 22, 0, CheckAlarmTimeAlarmReceiver.class, CheckAlarmTime.ACTION_CHECK_ALARM_TIME);
        assertAndConsumeSystemAlarm(DayTest.YEAR, DayTest.MONTH, DayTest.DAY, 22, 0, NighttimeBellAlarmReceiver.class, NighttimeBell.ACTION_PLAY);
        assertAndConsumeSystemAlarm(DayTest.YEAR, DayTest.MONTH, DayTest.DAY + 1, 0, 0, AlarmReceiver.class, SystemAlarm.ACTION_SET_SYSTEM_ALARM);

        checkSkippedNotification(1);

        checkNoActivity();
    }

    public void reboot() {
        BootReceiverTest.reboot(context);
    }

    private void snooze() {
        Analytics analytics = new Analytics(Analytics.Channel.Activity, Analytics.ChannelName.Ring);

        int minutes = (int) SharedPreferencesHelper.load(SettingsActivity.PREF_SNOOZE_TIME, SettingsActivity.PREF_SNOOZE_TIME_DEFAULT);

        GlobalManager globalManager = GlobalManager.getInstance();
        globalManager.onSnooze(alarm1, minutes, analytics);

        // Assert
        assertSystemAlarmCount(1);

        assertAndConsumeSystemAlarm(DayTest.YEAR, DayTest.MONTH, DayTest.DAY, ONE_TIME_ALARM_HOUR, ONE_TIME_ALARM_MINUTE + 11, AlarmReceiver.class, SystemAlarm.ACTION_RING);
    }

    private void dismiss() {
        Analytics analytics = new Analytics(Analytics.Channel.Activity, Analytics.ChannelName.Ring);

        GlobalManager globalManager = GlobalManager.getInstance();
        globalManager.onDismiss(alarm1, analytics);
    }

    void assertSystemAlarmCount(int count) {
        AlarmMorningAppTest.assertSystemAlarmCount(shadowAlarmManager, count);
    }

    private void assertAndConsumeSystemAlarm(int year, int month, int day, int hour, int minute, Class<?> cls, String action) {
        BootReceiverTest.assertSystemAlarm(context, shadowAlarmManager, year, month, day, hour, minute, cls, action);
        BootReceiverTest.consumeSystemAlarm(shadowAlarmManager);
    }

    private void checkRingingActivity() {
        CalendarWithOneTimeAlarmTest.checkActivity(context, RingActivity.class);
        CalendarWithOneTimeAlarmTest.consumeActivity();
        // TODO Check alarm time
    }

    private void checkNoActivity() {
        CalendarWithOneTimeAlarmTest.checkNoActivity();
    }

    private void checkSkippedNotification(int count) {
        // TODO Implement
    }
}
