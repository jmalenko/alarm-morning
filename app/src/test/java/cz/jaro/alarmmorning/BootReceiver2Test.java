package cz.jaro.alarmmorning;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.TextView;

import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowAlarmManager;
import org.robolectric.shadows.ShadowNotificationManager;

import java.util.Calendar;
import java.util.GregorianCalendar;

import cz.jaro.alarmmorning.app.AlarmMorningAppTest;
import cz.jaro.alarmmorning.app.CalendarWithOneTimeAlarmTest;
import cz.jaro.alarmmorning.checkalarmtime.CheckAlarmTime;
import cz.jaro.alarmmorning.checkalarmtime.CheckAlarmTimeAlarmReceiver;
import cz.jaro.alarmmorning.clock.Clock;
import cz.jaro.alarmmorning.clock.FixedClock;
import cz.jaro.alarmmorning.model.AppAlarm;
import cz.jaro.alarmmorning.model.DayTest;
import cz.jaro.alarmmorning.model.OneTimeAlarm;
import cz.jaro.alarmmorning.nighttimebell.NighttimeBell;
import cz.jaro.alarmmorning.nighttimebell.NighttimeBellAlarmReceiver;
import cz.jaro.alarmmorning.receivers.AlarmReceiver;
import cz.jaro.alarmmorning.receivers.VoidReceiver;
import cz.jaro.alarmmorning.shadows.ShadowGlobalManager;

import static cz.jaro.alarmmorning.app.AlarmMorningAppTest.assertNotification;
import static cz.jaro.alarmmorning.app.AlarmMorningAppTest.assertNotificationCount;
import static cz.jaro.alarmmorning.app.CalendarWithOneTimeAlarmTest.ONE_TIME_ALARM_HOUR;
import static cz.jaro.alarmmorning.app.CalendarWithOneTimeAlarmTest.ONE_TIME_ALARM_MINUTE;
import static cz.jaro.alarmmorning.model.DayTest.DAY;
import static cz.jaro.alarmmorning.model.DayTest.MONTH;
import static cz.jaro.alarmmorning.model.DayTest.YEAR;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.robolectric.Robolectric.buildActivity;

/**
 * Tests of Boot Receiver, specifically proper resuming.
 */
@Config(shadows = {ShadowGlobalManager.class})
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class BootReceiver2Test extends FixedTimeTest {

    private Context context;
    private ShadowAlarmManager shadowAlarmManager;

    private NotificationManager notificationManager;
    private ShadowNotificationManager shadowNotificationManager;

    private CalendarWithOneTimeAlarmTest test;
    private Clock clock;

    private OneTimeAlarm alarm1;
    private OneTimeAlarm alarm2;

    private static final int ONE_TIME_ALARM_HOUR2 = 10;

    @Before
    public void before() {
        super.before();

        context = RuntimeEnvironment.application.getApplicationContext();

        AlarmManager alarmManager = (AlarmManager) RuntimeEnvironment.application.getSystemService(Context.ALARM_SERVICE);
        shadowAlarmManager = Shadows.shadowOf(alarmManager);

        notificationManager = (NotificationManager) RuntimeEnvironment.application.getSystemService(Context.NOTIFICATION_SERVICE);
        shadowNotificationManager = Shadows.shadowOf(notificationManager);

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
        alarm2 = test.setAlarm(new GregorianCalendar(YEAR, MONTH, DAY, ONE_TIME_ALARM_HOUR2, ONE_TIME_ALARM_MINUTE));

        assertSystemAlarmCount(0);
    }

    @Test
    public void t100_farBefore1stAlarm() {
        setClock(new FixedClock(new GregorianCalendar(YEAR, MONTH, DAY, ONE_TIME_ALARM_HOUR - 3, ONE_TIME_ALARM_MINUTE, 0)));
        reboot();

        assertSystemAlarmCount(4);

        assertAndConsumeSystemAlarm(DayTest.YEAR, DayTest.MONTH, DayTest.DAY, ONE_TIME_ALARM_HOUR - 2, ONE_TIME_ALARM_MINUTE, AlarmReceiver.class, SystemAlarm.ACTION_RING_IN_NEAR_FUTURE);
        assertAndConsumeSystemAlarm(DayTest.YEAR, DayTest.MONTH, DayTest.DAY, ONE_TIME_ALARM_HOUR, ONE_TIME_ALARM_MINUTE, VoidReceiver.class, null); // SystemAlarmClock
        assertAndConsumeSystemAlarm(DayTest.YEAR, DayTest.MONTH, DayTest.DAY, 22, 0, CheckAlarmTimeAlarmReceiver.class, CheckAlarmTime.ACTION_CHECK_ALARM_TIME);
        assertAndConsumeSystemAlarm(DayTest.YEAR, DayTest.MONTH, DayTest.DAY, 22, 0, NighttimeBellAlarmReceiver.class, NighttimeBell.ACTION_PLAY);

        checkNoActivity();

        assertNotificationCount(shadowNotificationManager, 0);
    }

    @Test
    public void t110_justBefore1stAlarm() {
        setClock(new FixedClock(new GregorianCalendar(YEAR, MONTH, DAY, ONE_TIME_ALARM_HOUR, ONE_TIME_ALARM_MINUTE - 1, 0)));
        reboot();

        assertSystemAlarmCount(4);

        assertAndConsumeSystemAlarm(DayTest.YEAR, DayTest.MONTH, DayTest.DAY, ONE_TIME_ALARM_HOUR, ONE_TIME_ALARM_MINUTE, AlarmReceiver.class, SystemAlarm.ACTION_RING);
        assertAndConsumeSystemAlarm(DayTest.YEAR, DayTest.MONTH, DayTest.DAY, ONE_TIME_ALARM_HOUR, ONE_TIME_ALARM_MINUTE, VoidReceiver.class, null); // SystemAlarmClock
        assertAndConsumeSystemAlarm(DayTest.YEAR, DayTest.MONTH, DayTest.DAY, 22, 0, CheckAlarmTimeAlarmReceiver.class, CheckAlarmTime.ACTION_CHECK_ALARM_TIME);
        assertAndConsumeSystemAlarm(DayTest.YEAR, DayTest.MONTH, DayTest.DAY, 22, 0, NighttimeBellAlarmReceiver.class, NighttimeBell.ACTION_PLAY);

        checkNoActivity();

        assertNotificationCount(shadowNotificationManager, 1);

        Notification notification = shadowNotificationManager.getAllNotifications().get(0);
        assertNotification(notification, "Alarm at 4:30 AM", "Touch to view all alarms");
    }

    @Test
    public void t120_justAfter1stAlarm() {
        ringWithoutCleanup();

        assertSystemAlarmCount(4);

        assertAndConsumeSystemAlarm(DayTest.YEAR, DayTest.MONTH, DayTest.DAY, ONE_TIME_ALARM_HOUR2 - 2, ONE_TIME_ALARM_MINUTE, AlarmReceiver.class, SystemAlarm.ACTION_RING_IN_NEAR_FUTURE);
        assertAndConsumeSystemAlarm(DayTest.YEAR, DayTest.MONTH, DayTest.DAY, ONE_TIME_ALARM_HOUR2, ONE_TIME_ALARM_MINUTE, VoidReceiver.class, null); // SystemAlarmClock
        assertAndConsumeSystemAlarm(DayTest.YEAR, DayTest.MONTH, DayTest.DAY, 22, 0, CheckAlarmTimeAlarmReceiver.class, CheckAlarmTime.ACTION_CHECK_ALARM_TIME);
        assertAndConsumeSystemAlarm(DayTest.YEAR, DayTest.MONTH, DayTest.DAY, 22, 0, NighttimeBellAlarmReceiver.class, NighttimeBell.ACTION_PLAY);

        checkRingingActivity(alarm1);

        assertNotificationCount(shadowNotificationManager, 2);

        Notification notification = shadowNotificationManager.getAllNotifications().get(0);
        assertNotification(notification, "Alarm at 4:30 AM", "Ringing");
        notification = shadowNotificationManager.getAllNotifications().get(1);
        assertNotification(notification, "Alarm Morning", "There was 1 skipped alarm while the device was off: 4:30 AM");
        notificationManager.cancelAll();
    }

    @Test
    public void t130_farAfter1stAlarm() {
        setClock(new FixedClock(new GregorianCalendar(YEAR, MONTH, DAY, ONE_TIME_ALARM_HOUR, ONE_TIME_ALARM_MINUTE + 31, 0)));
        reboot();

        assertSystemAlarmCount(4);

        assertAndConsumeSystemAlarm(DayTest.YEAR, DayTest.MONTH, DayTest.DAY, ONE_TIME_ALARM_HOUR2 - 2, ONE_TIME_ALARM_MINUTE, AlarmReceiver.class, SystemAlarm.ACTION_RING_IN_NEAR_FUTURE);
        assertAndConsumeSystemAlarm(DayTest.YEAR, DayTest.MONTH, DayTest.DAY, ONE_TIME_ALARM_HOUR2, ONE_TIME_ALARM_MINUTE, VoidReceiver.class, null); // SystemAlarmClock
        assertAndConsumeSystemAlarm(DayTest.YEAR, DayTest.MONTH, DayTest.DAY, 22, 0, CheckAlarmTimeAlarmReceiver.class, CheckAlarmTime.ACTION_CHECK_ALARM_TIME);
        assertAndConsumeSystemAlarm(DayTest.YEAR, DayTest.MONTH, DayTest.DAY, 22, 0, NighttimeBellAlarmReceiver.class, NighttimeBell.ACTION_PLAY);

        checkNoActivity();

        assertNotificationCount(shadowNotificationManager, 1);

        Notification notification = shadowNotificationManager.getAllNotifications().get(0);
        assertNotification(notification, "Alarm Morning", "There was 1 skipped alarm while the device was off: 4:30 AM");
    }

    @Test
    public void t140_justBefore2ndAlarm() {
        setClock(new FixedClock(new GregorianCalendar(YEAR, MONTH, DAY, ONE_TIME_ALARM_HOUR2, ONE_TIME_ALARM_MINUTE - 1, 0)));
        reboot();

        assertSystemAlarmCount(4);

        assertAndConsumeSystemAlarm(DayTest.YEAR, DayTest.MONTH, DayTest.DAY, ONE_TIME_ALARM_HOUR2, ONE_TIME_ALARM_MINUTE, AlarmReceiver.class, SystemAlarm.ACTION_RING);
        assertAndConsumeSystemAlarm(DayTest.YEAR, DayTest.MONTH, DayTest.DAY, ONE_TIME_ALARM_HOUR2, ONE_TIME_ALARM_MINUTE, VoidReceiver.class, null); // SystemAlarmClock
        assertAndConsumeSystemAlarm(DayTest.YEAR, DayTest.MONTH, DayTest.DAY, 22, 0, CheckAlarmTimeAlarmReceiver.class, CheckAlarmTime.ACTION_CHECK_ALARM_TIME);
        assertAndConsumeSystemAlarm(DayTest.YEAR, DayTest.MONTH, DayTest.DAY, 22, 0, NighttimeBellAlarmReceiver.class, NighttimeBell.ACTION_PLAY);

        checkNoActivity();

        assertNotificationCount(shadowNotificationManager, 2);

        Notification notification = shadowNotificationManager.getAllNotifications().get(0);
        assertNotification(notification, "Alarm at 10:30 AM", "Touch to view all alarms");
        notification = shadowNotificationManager.getAllNotifications().get(1);
        assertNotification(notification, "Alarm Morning", "There was 1 skipped alarm while the device was off: 4:30 AM");
    }

    @Test
    public void t150_justAfter2ndAlarm() {
        setClock(new FixedClock(new GregorianCalendar(YEAR, MONTH, DAY, ONE_TIME_ALARM_HOUR2, ONE_TIME_ALARM_MINUTE + 1, 0)));
        reboot();

        assertSystemAlarmCount(3);

        assertAndConsumeSystemAlarm(DayTest.YEAR, DayTest.MONTH, DayTest.DAY, 22, 0, CheckAlarmTimeAlarmReceiver.class, CheckAlarmTime.ACTION_CHECK_ALARM_TIME);
        assertAndConsumeSystemAlarm(DayTest.YEAR, DayTest.MONTH, DayTest.DAY, 22, 0, NighttimeBellAlarmReceiver.class, NighttimeBell.ACTION_PLAY);
        assertAndConsumeSystemAlarm(DayTest.YEAR, DayTest.MONTH, DayTest.DAY + 1, 0, 0, AlarmReceiver.class, SystemAlarm.ACTION_SET_SYSTEM_ALARM);

        checkRingingActivity(alarm2);

        assertNotificationCount(shadowNotificationManager, 2);

        Notification notification = shadowNotificationManager.getAllNotifications().get(0);
        assertNotification(notification, "Alarm at 10:30 AM", "Ringing");
        notification = shadowNotificationManager.getAllNotifications().get(1);
        assertNotification(notification, "Alarm Morning", "There were 2 skipped alarms while the device was off: 4:30 AM and 10:30 AM");
    }

    @Test
    public void t160_farAfter2ndAlarm() {
        setClock(new FixedClock(new GregorianCalendar(YEAR, MONTH, DAY, ONE_TIME_ALARM_HOUR2, ONE_TIME_ALARM_MINUTE + 31, 0)));
        reboot();

        assertSystemAlarmCount(3);

        assertAndConsumeSystemAlarm(DayTest.YEAR, DayTest.MONTH, DayTest.DAY, 22, 0, CheckAlarmTimeAlarmReceiver.class, CheckAlarmTime.ACTION_CHECK_ALARM_TIME);
        assertAndConsumeSystemAlarm(DayTest.YEAR, DayTest.MONTH, DayTest.DAY, 22, 0, NighttimeBellAlarmReceiver.class, NighttimeBell.ACTION_PLAY);
        assertAndConsumeSystemAlarm(DayTest.YEAR, DayTest.MONTH, DayTest.DAY + 1, 0, 0, AlarmReceiver.class, SystemAlarm.ACTION_SET_SYSTEM_ALARM);

        checkNoActivity();

        assertNotificationCount(shadowNotificationManager, 1);

        Notification notification = shadowNotificationManager.getAllNotifications().get(0);
        assertNotification(notification, "Alarm Morning", "There were 2 skipped alarms while the device was off: 4:30 AM and 10:30 AM");
    }

    @Test
    public void t200_ringing_justBefore2ndAlarm() {
        ring();

        setClock(new FixedClock(new GregorianCalendar(YEAR, MONTH, DAY, ONE_TIME_ALARM_HOUR2, ONE_TIME_ALARM_MINUTE - 1, 0)));
        reboot();

        assertSystemAlarmCount(4);

        assertAndConsumeSystemAlarm(DayTest.YEAR, DayTest.MONTH, DayTest.DAY, ONE_TIME_ALARM_HOUR2, ONE_TIME_ALARM_MINUTE, AlarmReceiver.class, SystemAlarm.ACTION_RING);
        assertAndConsumeSystemAlarm(DayTest.YEAR, DayTest.MONTH, DayTest.DAY, ONE_TIME_ALARM_HOUR2, ONE_TIME_ALARM_MINUTE, VoidReceiver.class, null); // SystemAlarmClock
        assertAndConsumeSystemAlarm(DayTest.YEAR, DayTest.MONTH, DayTest.DAY, 22, 0, CheckAlarmTimeAlarmReceiver.class, CheckAlarmTime.ACTION_CHECK_ALARM_TIME);
        assertAndConsumeSystemAlarm(DayTest.YEAR, DayTest.MONTH, DayTest.DAY, 22, 0, NighttimeBellAlarmReceiver.class, NighttimeBell.ACTION_PLAY);

        checkNoActivity();

        assertNotificationCount(shadowNotificationManager, 2);

        Notification notification = shadowNotificationManager.getAllNotifications().get(0);
        assertNotification(notification, "Alarm Morning", "There was 1 skipped alarm while the device was off: 4:30 AM");
        notification = shadowNotificationManager.getAllNotifications().get(1);
        assertNotification(notification, "Alarm at 10:30 AM", "Touch to view all alarms");
    }

    @Test
    public void t210_ringing_justAfter2ndAlarm() {
        ring();
        setClock(new FixedClock(new GregorianCalendar(YEAR, MONTH, DAY, ONE_TIME_ALARM_HOUR2, ONE_TIME_ALARM_MINUTE + 1, 0)));
        reboot();

        assertSystemAlarmCount(3);

        assertAndConsumeSystemAlarm(DayTest.YEAR, DayTest.MONTH, DayTest.DAY, 22, 0, CheckAlarmTimeAlarmReceiver.class, CheckAlarmTime.ACTION_CHECK_ALARM_TIME);
        assertAndConsumeSystemAlarm(DayTest.YEAR, DayTest.MONTH, DayTest.DAY, 22, 0, NighttimeBellAlarmReceiver.class, NighttimeBell.ACTION_PLAY);
        assertAndConsumeSystemAlarm(DayTest.YEAR, DayTest.MONTH, DayTest.DAY + 1, 0, 0, AlarmReceiver.class, SystemAlarm.ACTION_SET_SYSTEM_ALARM);

        checkRingingActivity(alarm2);

        assertNotificationCount(shadowNotificationManager, 2);

        Notification notification = shadowNotificationManager.getAllNotifications().get(0);
        assertNotification(notification, "Alarm Morning", "There were 2 skipped alarms while the device was off: 4:30 AM and 10:30 AM");
        notification = shadowNotificationManager.getAllNotifications().get(1);
        assertNotification(notification, "Alarm at 10:30 AM", "Ringing");
    }

    @Test
    public void t220_ringing_farAfter2ndAlarm() {
        ring();
        setClock(new FixedClock(new GregorianCalendar(YEAR, MONTH, DAY, ONE_TIME_ALARM_HOUR2, ONE_TIME_ALARM_MINUTE + 31, 0)));
        reboot();

        assertSystemAlarmCount(3);

        assertAndConsumeSystemAlarm(DayTest.YEAR, DayTest.MONTH, DayTest.DAY, 22, 0, CheckAlarmTimeAlarmReceiver.class, CheckAlarmTime.ACTION_CHECK_ALARM_TIME);
        assertAndConsumeSystemAlarm(DayTest.YEAR, DayTest.MONTH, DayTest.DAY, 22, 0, NighttimeBellAlarmReceiver.class, NighttimeBell.ACTION_PLAY);
        assertAndConsumeSystemAlarm(DayTest.YEAR, DayTest.MONTH, DayTest.DAY + 1, 0, 0, AlarmReceiver.class, SystemAlarm.ACTION_SET_SYSTEM_ALARM);

        checkNoActivity();

        assertNotificationCount(shadowNotificationManager, 1);

        Notification notification = shadowNotificationManager.getAllNotifications().get(0);
        assertNotification(notification, "Alarm Morning", "There were 2 skipped alarms while the device was off: 4:30 AM and 10:30 AM");
    }

    @Test
    public void t300_snoozed_inOneMinute() {
        snooze();

        setClock(new FixedClock(new GregorianCalendar(YEAR, MONTH, DAY, ONE_TIME_ALARM_HOUR, ONE_TIME_ALARM_MINUTE + 2, 0)));
        reboot();

        assertSystemAlarmCount(3);

        assertAndConsumeSystemAlarm(DayTest.YEAR, DayTest.MONTH, DayTest.DAY, ONE_TIME_ALARM_HOUR, ONE_TIME_ALARM_MINUTE + 11, AlarmReceiver.class, SystemAlarm.ACTION_RING);
        assertAndConsumeSystemAlarm(DayTest.YEAR, DayTest.MONTH, DayTest.DAY, 22, 0, CheckAlarmTimeAlarmReceiver.class, CheckAlarmTime.ACTION_CHECK_ALARM_TIME);
        assertAndConsumeSystemAlarm(DayTest.YEAR, DayTest.MONTH, DayTest.DAY, 22, 0, NighttimeBellAlarmReceiver.class, NighttimeBell.ACTION_PLAY);

        checkNoActivity();

        assertNotificationCount(shadowNotificationManager, 1);

        Notification notification = shadowNotificationManager.getAllNotifications().get(0);
        assertNotification(notification, "Alarm at 4:30 AM", "Snoozed until 4:41 AM");
    }

    @Test
    public void t310_snoozed_justBefore2ndAlarm() {
        snooze();

        setClock(new FixedClock(new GregorianCalendar(YEAR, MONTH, DAY, ONE_TIME_ALARM_HOUR2, ONE_TIME_ALARM_MINUTE - 1, 0)));
        reboot();

        assertSystemAlarmCount(4);

        assertAndConsumeSystemAlarm(DayTest.YEAR, DayTest.MONTH, DayTest.DAY, ONE_TIME_ALARM_HOUR2, ONE_TIME_ALARM_MINUTE, AlarmReceiver.class, SystemAlarm.ACTION_RING);
        assertAndConsumeSystemAlarm(DayTest.YEAR, DayTest.MONTH, DayTest.DAY, ONE_TIME_ALARM_HOUR2, ONE_TIME_ALARM_MINUTE, VoidReceiver.class, null);
        assertAndConsumeSystemAlarm(DayTest.YEAR, DayTest.MONTH, DayTest.DAY, 22, 0, CheckAlarmTimeAlarmReceiver.class, CheckAlarmTime.ACTION_CHECK_ALARM_TIME);
        assertAndConsumeSystemAlarm(DayTest.YEAR, DayTest.MONTH, DayTest.DAY, 22, 0, NighttimeBellAlarmReceiver.class, NighttimeBell.ACTION_PLAY);

        checkNoActivity();

        assertNotificationCount(shadowNotificationManager, 2);

        Notification notification = shadowNotificationManager.getAllNotifications().get(0); // XXX Order of notifications depends on how the tests are run (run all tests in the entire class vs run just this test/method)
        assertNotification(notification, "Alarm Morning", "There was 1 skipped alarm while the device was off: 4:30 AM");
        notification = shadowNotificationManager.getAllNotifications().get(1);
        assertNotification(notification, "Alarm at 10:30 AM", "Touch to view all alarms");
    }

    @Test
    public void t320_snoozed_justAfter2ndAlarm() {
        snooze();

        setClock(new FixedClock(new GregorianCalendar(YEAR, MONTH, DAY, ONE_TIME_ALARM_HOUR2, ONE_TIME_ALARM_MINUTE + 1, 0)));
        reboot();

        assertSystemAlarmCount(3);

        assertAndConsumeSystemAlarm(DayTest.YEAR, DayTest.MONTH, DayTest.DAY, 22, 0, CheckAlarmTimeAlarmReceiver.class, CheckAlarmTime.ACTION_CHECK_ALARM_TIME);
        assertAndConsumeSystemAlarm(DayTest.YEAR, DayTest.MONTH, DayTest.DAY, 22, 0, NighttimeBellAlarmReceiver.class, NighttimeBell.ACTION_PLAY);
        assertAndConsumeSystemAlarm(DayTest.YEAR, DayTest.MONTH, DayTest.DAY + 1, 0, 0, AlarmReceiver.class, SystemAlarm.ACTION_SET_SYSTEM_ALARM);

        checkRingingActivity(alarm2);

        assertNotificationCount(shadowNotificationManager, 2);

        Notification notification = shadowNotificationManager.getAllNotifications().get(0);
        assertNotification(notification, "Alarm Morning", "There were 2 skipped alarms while the device was off: 4:30 AM and 10:30 AM");
        notification = shadowNotificationManager.getAllNotifications().get(1);
        assertNotification(notification, "Alarm at 10:30 AM", "Ringing");
    }

    @Test
    public void t330_snoozed_farAfter2ndAlarm() {
        snooze();

        setClock(new FixedClock(new GregorianCalendar(YEAR, MONTH, DAY, ONE_TIME_ALARM_HOUR2, ONE_TIME_ALARM_MINUTE + 31, 0)));
        reboot();

        assertSystemAlarmCount(3);

        assertAndConsumeSystemAlarm(DayTest.YEAR, DayTest.MONTH, DayTest.DAY, 22, 0, CheckAlarmTimeAlarmReceiver.class, CheckAlarmTime.ACTION_CHECK_ALARM_TIME);
        assertAndConsumeSystemAlarm(DayTest.YEAR, DayTest.MONTH, DayTest.DAY, 22, 0, NighttimeBellAlarmReceiver.class, NighttimeBell.ACTION_PLAY);
        assertAndConsumeSystemAlarm(DayTest.YEAR, DayTest.MONTH, DayTest.DAY + 1, 0, 0, AlarmReceiver.class, SystemAlarm.ACTION_SET_SYSTEM_ALARM);

        checkNoActivity();

        assertNotificationCount(shadowNotificationManager, 1);

        Notification notification = shadowNotificationManager.getAllNotifications().get(0);
        assertNotification(notification, "Alarm Morning", "There were 2 skipped alarms while the device was off: 4:30 AM and 10:30 AM");
    }


    @Test
    public void t400_dismissed_justBefore2ndAlarm() {
        dismiss();

        assertSystemAlarmCount(0);
        assertNotificationCount(shadowNotificationManager, 0);

        setClock(new FixedClock(new GregorianCalendar(YEAR, MONTH, DAY, ONE_TIME_ALARM_HOUR2, ONE_TIME_ALARM_MINUTE - 1, 0)));
        reboot();

        assertSystemAlarmCount(4);

        assertAndConsumeSystemAlarm(DayTest.YEAR, DayTest.MONTH, DayTest.DAY, ONE_TIME_ALARM_HOUR2, ONE_TIME_ALARM_MINUTE, AlarmReceiver.class, SystemAlarm.ACTION_RING);
        assertAndConsumeSystemAlarm(DayTest.YEAR, DayTest.MONTH, DayTest.DAY, ONE_TIME_ALARM_HOUR2, ONE_TIME_ALARM_MINUTE, VoidReceiver.class, null); // SystemAlarmClock
        assertAndConsumeSystemAlarm(DayTest.YEAR, DayTest.MONTH, DayTest.DAY, 22, 0, CheckAlarmTimeAlarmReceiver.class, CheckAlarmTime.ACTION_CHECK_ALARM_TIME);
        assertAndConsumeSystemAlarm(DayTest.YEAR, DayTest.MONTH, DayTest.DAY, 22, 0, NighttimeBellAlarmReceiver.class, NighttimeBell.ACTION_PLAY);

        checkNoActivity();

        assertNotificationCount(shadowNotificationManager, 1);

        Notification notification = shadowNotificationManager.getAllNotifications().get(0);
        assertNotification(notification, "Alarm at 10:30 AM", "Touch to view all alarms");
    }

    @Test
    public void t410_dismissed_justAfter2ndAlarm() {
        dismiss();

        setClock(new FixedClock(new GregorianCalendar(YEAR, MONTH, DAY, ONE_TIME_ALARM_HOUR2, ONE_TIME_ALARM_MINUTE + 1, 0)));
        reboot();

        assertSystemAlarmCount(3);

        assertAndConsumeSystemAlarm(DayTest.YEAR, DayTest.MONTH, DayTest.DAY, 22, 0, CheckAlarmTimeAlarmReceiver.class, CheckAlarmTime.ACTION_CHECK_ALARM_TIME);
        assertAndConsumeSystemAlarm(DayTest.YEAR, DayTest.MONTH, DayTest.DAY, 22, 0, NighttimeBellAlarmReceiver.class, NighttimeBell.ACTION_PLAY);
        assertAndConsumeSystemAlarm(DayTest.YEAR, DayTest.MONTH, DayTest.DAY + 1, 0, 0, AlarmReceiver.class, SystemAlarm.ACTION_SET_SYSTEM_ALARM);

        checkRingingActivity(alarm2);

        assertNotificationCount(shadowNotificationManager, 2);

        Notification notification = shadowNotificationManager.getAllNotifications().get(0);
        assertNotification(notification, "Alarm at 10:30 AM", "Ringing");
        notification = shadowNotificationManager.getAllNotifications().get(1);
        assertNotification(notification, "Alarm Morning", "There was 1 skipped alarm while the device was off: 10:30 AM");
    }

    @Test
    public void t420_dismissed_farAfter2ndAlarm() {
        dismiss();

        setClock(new FixedClock(new GregorianCalendar(YEAR, MONTH, DAY, ONE_TIME_ALARM_HOUR2, ONE_TIME_ALARM_MINUTE + 31, 0)));
        reboot();

        assertSystemAlarmCount(3);

        assertAndConsumeSystemAlarm(DayTest.YEAR, DayTest.MONTH, DayTest.DAY, 22, 0, CheckAlarmTimeAlarmReceiver.class, CheckAlarmTime.ACTION_CHECK_ALARM_TIME);
        assertAndConsumeSystemAlarm(DayTest.YEAR, DayTest.MONTH, DayTest.DAY, 22, 0, NighttimeBellAlarmReceiver.class, NighttimeBell.ACTION_PLAY);
        assertAndConsumeSystemAlarm(DayTest.YEAR, DayTest.MONTH, DayTest.DAY + 1, 0, 0, AlarmReceiver.class, SystemAlarm.ACTION_SET_SYSTEM_ALARM);

        checkNoActivity();

        assertNotificationCount(shadowNotificationManager, 1);

        Notification notification = shadowNotificationManager.getAllNotifications().get(0);
        assertNotification(notification, "Alarm Morning", "There was 1 skipped alarm while the device was off: 10:30 AM");
    }

    public void reboot() {
        BootReceiverTest.reboot(context);
    }

    private void ringWithoutCleanup() {
        setClock(new FixedClock(new GregorianCalendar(YEAR, MONTH, DAY, ONE_TIME_ALARM_HOUR, ONE_TIME_ALARM_MINUTE + 1, 0)));
        reboot();

    }

    private void ring() {
        ringWithoutCleanup();

        BootReceiverTest.consumeSystemAlarms(shadowAlarmManager);
        CalendarWithOneTimeAlarmTest.consumeActivity();
        notificationManager.cancelAll();
    }

    private void snooze() {
        ring();

        Analytics analytics = new Analytics(Analytics.Channel.Activity, Analytics.ChannelName.Ring);

        int minutes = (int) SharedPreferencesHelper.load(SettingsActivity.PREF_SNOOZE_TIME, SettingsActivity.PREF_SNOOZE_TIME_DEFAULT);

        GlobalManager globalManager = GlobalManager.getInstance();
        globalManager.onSnooze(alarm1, minutes, analytics);

        // Assert
        assertSystemAlarmCount(1);

        assertAndConsumeSystemAlarm(DayTest.YEAR, DayTest.MONTH, DayTest.DAY, ONE_TIME_ALARM_HOUR, ONE_TIME_ALARM_MINUTE + minutes + 1, AlarmReceiver.class, SystemAlarm.ACTION_RING);

        checkNoActivity();

        assertNotificationCount(shadowNotificationManager, 1);

        Notification notification = shadowNotificationManager.getAllNotifications().get(0);
        assertNotification(notification, "Alarm at 4:30 AM", "Snoozed until 4:41 AM");

        notificationManager.cancelAll();
    }

    private void dismiss() {
        ring();

        Analytics analytics = new Analytics(Analytics.Channel.Activity, Analytics.ChannelName.Ring);

        GlobalManager globalManager = GlobalManager.getInstance();
        globalManager.onDismiss(alarm1, analytics);

        // Assert
        assertSystemAlarmCount(0);

        checkNoActivity();

        assertNotificationCount(shadowNotificationManager, 0);
    }

    private void setClock(Clock clock) {
        this.clock = clock;
        shadowGlobalManager.setClock(clock);
    }

    private void assertSystemAlarmCount(int count) {
        AlarmMorningAppTest.assertSystemAlarmCount(shadowAlarmManager, count);
    }

    private void assertAndConsumeSystemAlarm(int year, int month, int day, int hour, int minute, Class<?> cls, String action) {
        BootReceiverTest.assertSystemAlarm(context, shadowAlarmManager, year, month, day, hour, minute, cls, action);
        BootReceiverTest.consumeSystemAlarm(shadowAlarmManager);
    }

    private void checkRingingActivity(AppAlarm appAlarm) {
        CalendarWithOneTimeAlarmTest.checkActivity(context, RingActivity.class);
        CalendarWithOneTimeAlarmTest.consumeActivity();

        // Check that the RingActivity shows correct alarm time
        Intent ringIntent = new Intent(context, RingActivity.class);
        ringIntent.putExtra(GlobalManager.PERSIST_ALARM_TYPE, appAlarm.getClass().getSimpleName());
        ringIntent.putExtra(GlobalManager.PERSIST_ALARM_ID, appAlarm.getPersistenceId());

        Activity activity = buildActivity(RingActivity.class, ringIntent).setup().get();

        TextView textTime = activity.findViewById(R.id.time);
        TextView textAlarmTime = activity.findViewById(R.id.alarmTime);

        String timeStr = Localization.timeToString(appAlarm.getDateTime().getTime(), context);
        Calendar now = clock.now();
        if (appAlarm.getHour() == now.get(Calendar.HOUR_OF_DAY) && appAlarm.getMinute() == now.get(Calendar.MINUTE)) {
            assertThat("Time", textTime.getText(), is(timeStr));

            assertThat("Alarm time", textAlarmTime.getVisibility(), is(View.INVISIBLE));
        } else {
            String clockStr = Localization.timeToString(now.getTime(), context);
            assertThat("Time", textTime.getText(), is(clockStr));

            assertThat("Alarm time", textAlarmTime.getVisibility(), is(View.VISIBLE));
            assertThat("Alarm time", textAlarmTime.getText(), is("Alarm was set to " + timeStr));
        }
    }

    private void checkNoActivity() {
        CalendarWithOneTimeAlarmTest.checkActivity(context, null);
    }

}