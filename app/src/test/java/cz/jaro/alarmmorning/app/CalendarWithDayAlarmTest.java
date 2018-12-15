package cz.jaro.alarmmorning.app;

import android.app.Activity;
import android.app.Notification;
import android.content.Intent;
import android.view.View;

import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowActivity;

import java.util.Calendar;
import java.util.GregorianCalendar;

import cz.jaro.alarmmorning.Analytics;
import cz.jaro.alarmmorning.R;
import cz.jaro.alarmmorning.RingActivity;
import cz.jaro.alarmmorning.SystemAlarm;
import cz.jaro.alarmmorning.clock.FixedClock;
import cz.jaro.alarmmorning.model.Day;
import cz.jaro.alarmmorning.model.Defaults;
import cz.jaro.alarmmorning.receivers.AlarmReceiver;
import cz.jaro.alarmmorning.receivers.NotificationReceiver;
import cz.jaro.alarmmorning.shadows.ShadowGlobalManager;

import static cz.jaro.alarmmorning.GlobalManager.HORIZON_DAYS;
import static cz.jaro.alarmmorning.model.AlarmDbHelper.DEFAULT_ALARM_HOUR;
import static cz.jaro.alarmmorning.model.AlarmDbHelper.DEFAULT_ALARM_MINUTE;
import static cz.jaro.alarmmorning.model.DayTest.DAY;
import static cz.jaro.alarmmorning.model.DayTest.HOUR_DEFAULT;
import static cz.jaro.alarmmorning.model.DayTest.MINUTE_DEFAULT;
import static cz.jaro.alarmmorning.model.DayTest.MONTH;
import static cz.jaro.alarmmorning.model.DayTest.YEAR;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

/**
 * Tests of alarm management in UI.
 * <p>
 * One Day alarm is used in the tests.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 21, shadows = {ShadowGlobalManager.class})
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class CalendarWithDayAlarmTest extends AlarmMorningAppTest {

    @Test
    public void t000_prerequisities() {
        assertThat("Shadow widget manager contains a provider", shadowAppWidgetManager.getInstalledProviders().size(), is(1));
    }

    @Test
    public void t001_noAlarmIsScheduled() {
        // Start activity
        startActivityCalendar();

        // Check calendar
        int count = recyclerView.getChildCount();
        assertThat("The number of items", count, is(HORIZON_DAYS));

        assertCalendarItem(0, "2/1", "Mon", "Off", "", ""); // Today
        assertCalendarItem(1, "2/2", "Tue", "Off", "", ""); // Tomorrow
        assertCalendarItem(2, "2/3", "Wed", "Off", "", ""); // 3rd day
        assertCalendarItem(3, "2/4", "Thu", "Off", "", ""); // 4th day
        assertCalendarItem(4, "2/5", "Fri", "Off", "", ""); // 5th day

        // Check system alarm
        assertSystemAlarmCount(1);
        assertSystemAlarm(YEAR, MONTH, DAY + 1, 0, 0, SystemAlarm.ACTION_SET_SYSTEM_ALARM);

        // Check system alarm clock
        assertSystemAlarmClockNone();

        // Check notification
        assertNotificationCount(0);

        // Check widget
        assertWidget(R.drawable.ic_alarm_off_white, "No alarm", null);
    }

    @Test
    public void t100_setAlarm() {
        // Consume the alarm with action ACTION_SET_SYSTEM_ALARM
        consumeNextScheduledAlarm();

        // Set day alarm
        int oldCount = calendar_setDayAlarm(0, DEFAULT_ALARM_HOUR, DEFAULT_ALARM_MINUTE, DEFAULT_ALARM_HOUR, DEFAULT_ALARM_MINUTE);

        // Check calendar
        int newCount = recyclerView.getChildCount();
        assertThat("The change of number of items", newCount - oldCount, is(0));

        assertCalendarItem(0, "2/1", "Mon", "7:00 AM", "Changed", "6h 0m"); // Today
        assertCalendarItem(1, "2/2", "Tue", "Off", "", ""); // Tomorrow

        // Check system alarm
        assertSystemAlarmCount(2);
        assertSystemAlarm(YEAR, MONTH, DAY, DEFAULT_ALARM_HOUR - 2, DEFAULT_ALARM_MINUTE, SystemAlarm.ACTION_RING_IN_NEAR_FUTURE); // System alarm
        assertSystemAlarmClock(YEAR, MONTH, DAY, DEFAULT_ALARM_HOUR, DEFAULT_ALARM_MINUTE); // System alarm clock

        // Check notification
        assertNotificationCount(0);

        // Check widget
        assertWidget(R.drawable.ic_alarm_white, "7:00 AM", null);
    }

    @Test
    public void t101_changeAlarm() {
        // Consume the alarm with action ACTION_SET_SYSTEM_ALARM
        consumeNextScheduledAlarm();

        // Set day alarms
        int oldCount = calendar_setDayAlarm(0, DEFAULT_ALARM_HOUR, DEFAULT_ALARM_MINUTE, DEFAULT_ALARM_HOUR + 1, DEFAULT_ALARM_MINUTE + 1);
        calendar_setDayAlarm(0, DEFAULT_ALARM_HOUR + 1, DEFAULT_ALARM_MINUTE + 1, DEFAULT_ALARM_HOUR + 2, DEFAULT_ALARM_MINUTE + 2);

        // Check calendar
        int newCount = recyclerView.getChildCount();
        assertThat("The change of number of items", newCount - oldCount, is(0));

        assertCalendarItem(0, "2/1", "Mon", "9:02 AM", "Changed", "8h 2m"); // Today
        assertCalendarItem(1, "2/2", "Tue", "Off", "", ""); // Tomorrow

        // Check system alarm
        assertSystemAlarmCount(2);
        assertSystemAlarm(YEAR, MONTH, DAY, DEFAULT_ALARM_HOUR, DEFAULT_ALARM_MINUTE + 2, SystemAlarm.ACTION_RING_IN_NEAR_FUTURE);
        assertSystemAlarmClock(YEAR, MONTH, DAY, DEFAULT_ALARM_HOUR + 2, DEFAULT_ALARM_MINUTE + 2);

        // Check notification
        assertNotificationCount(0);

        // Check widget
        assertWidget(R.drawable.ic_alarm_white, "9:02 AM", null);
    }

    @Test
    public void t102_setAlarmToPast() {
        // Consume the alarm with action ACTION_SET_SYSTEM_ALARM
        consumeNextScheduledAlarm();

        // Set day alarm
        Calendar now = globalManager.clock().now();
        now.add(Calendar.MINUTE, -1);

        calendar_setDayAlarm(0, DEFAULT_ALARM_HOUR, DEFAULT_ALARM_MINUTE, now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE));

        // Check calendar
        assertCalendarItem(0, "2/1", "Mon", "12:59 AM", "Passed", ""); // Today
        assertCalendarItem(1, "2/2", "Tue", "Off", "", ""); // Tomorrow

        // Check system alarm
        assertSystemAlarmCount(0);
        assertSystemAlarmClockNone();

        // Check notification
        assertNotificationCount(0);

        // Check widget
        assertWidget(R.drawable.ic_alarm_off_white, "No alarm", null);
    }

    @Test
    public void t103_setAlarmWithZeroAdvancePeriod() {
        // Set the preference to zero
        setNearFuturePeriodPreferenceToZero(context);

        // Consume the alarm with action ACTION_SET_SYSTEM_ALARM
        consumeNextScheduledAlarm();

        // Set day alarm
        calendar_setDayAlarm(0, DEFAULT_ALARM_HOUR, DEFAULT_ALARM_MINUTE, DEFAULT_ALARM_HOUR, DEFAULT_ALARM_MINUTE);

        // Check calendar
        assertCalendarItem(0, "2/1", "Mon", "7:00 AM", "Changed", "6h 0m"); // Today
        assertCalendarItem(1, "2/2", "Tue", "Off", "", ""); // Tomorrow

        // Check system alarm
        assertSystemAlarmCount(2);
        assertSystemAlarm(YEAR, MONTH, DAY, DEFAULT_ALARM_HOUR, DEFAULT_ALARM_MINUTE, SystemAlarm.ACTION_RING);
        assertSystemAlarmClock(YEAR, MONTH, DAY, DEFAULT_ALARM_HOUR, DEFAULT_ALARM_MINUTE);

        // Check notification
        assertNotificationCount(0);

        // Check widget
        assertWidget(R.drawable.ic_alarm_white, "7:00 AM", null);
    }

    @Test
    public void t104_setAlarmTomorrow() {
        // Consume the alarm with action ACTION_SET_SYSTEM_ALARM
        consumeNextScheduledAlarm();

        // Set day alarm
        calendar_setDayAlarm(1, DEFAULT_ALARM_HOUR, DEFAULT_ALARM_MINUTE, DEFAULT_ALARM_HOUR, DEFAULT_ALARM_MINUTE);

        // Check calendar
        assertCalendarItem(0, "2/1", "Mon", "Off", "", ""); // Today
        assertCalendarItem(1, "2/2", "Tue", "7:00 AM", "Changed", "1d 6h"); // Tomorrow

        // Check system alarm
        assertSystemAlarmCount(2);
        assertSystemAlarm(YEAR, MONTH, DAY + 1, DEFAULT_ALARM_HOUR - 2, DEFAULT_ALARM_MINUTE, SystemAlarm.ACTION_RING_IN_NEAR_FUTURE);
        assertSystemAlarmClock(YEAR, MONTH, DAY + 1, DEFAULT_ALARM_HOUR, DEFAULT_ALARM_MINUTE);

        // Check notification
        assertNotificationCount(0);

        // Check widget
        assertWidget(R.drawable.ic_alarm_white, "7:00 AM", "Tomorrow");
    }

    @Test
    public void t104_setTime_viaContextMenu() {
        // Consume the alarm with action ACTION_SET_SYSTEM_ALARM
        consumeNextScheduledAlarm();

        // Context menu
        startActivityCalendar();

        int oldCount = recyclerView.getChildCount();

        loadItemAtPosition(0);

        item.performLongClick();

        clickContextMenu(R.id.action_day_set_time);

        // Set time
        picker_setTime(DEFAULT_ALARM_HOUR, DEFAULT_ALARM_MINUTE, DEFAULT_ALARM_HOUR, DEFAULT_ALARM_MINUTE);

        // Check calendar
        int newCount = recyclerView.getChildCount();
        assertThat("The change of number of items", newCount - oldCount, is(0));

        assertCalendarItem(0, "2/1", "Mon", "7:00 AM", "Changed", "6h 0m"); // Today
        assertCalendarItem(1, "2/2", "Tue", "Off", "", ""); // Tomorrow

        // Check system alarm
        assertSystemAlarmCount(2);
        assertSystemAlarm(YEAR, MONTH, DAY, DEFAULT_ALARM_HOUR - 2, DEFAULT_ALARM_MINUTE, SystemAlarm.ACTION_RING_IN_NEAR_FUTURE); // System alarm
        assertSystemAlarmClock(YEAR, MONTH, DAY, DEFAULT_ALARM_HOUR, DEFAULT_ALARM_MINUTE); // System alarm clock

        // Check notification
        assertNotificationCount(0);

        // Check widget
        assertWidget(R.drawable.ic_alarm_white, "7:00 AM", null);
    }

    @Test
    public void t300_onNear() {
        prepareUntilNear();

        // Check system alarm
        assertSystemAlarmCount(1);
        assertSystemAlarm(YEAR, MONTH, DAY, DEFAULT_ALARM_HOUR, DEFAULT_ALARM_MINUTE, SystemAlarm.ACTION_RING);
        assertSystemAlarmClockNone();

        // Check notification
        assertNotificationCount(1);

        Notification notification = shadowNotificationManager.getAllNotifications().get(0);
        assertNotification(notification, "Alarm at 7:00 AM", "Touch to view all alarms");
        assertNotificationActionCount(notification, 1);
        assertNotificationAction(notification, 0, "Dismiss", NotificationReceiver.ACTION_DISMISS_BEFORE_RINGING);

        // Check widget
        assertWidget(R.drawable.ic_alarm_white, "7:00 AM", null);
    }

    @Test
    public void t301_dismissWhileInNearPeriod() {
        prepareUntilNear();

        // Consume the alarm with action ACTION_RING
        consumeNextScheduledAlarm();

        // Context menu
        startActivityCalendar();

        loadItemAtPosition(0);

        item.performLongClick();

        clickContextMenu(R.id.action_day_dismiss);

        refreshRecyclerView();

        // Check calendar
        assertCalendarItem(0, "2/1", "Mon", "7:00 AM", "Dismissed before ringing", ""); // Today
        assertCalendarItem(1, "2/2", "Tue", "Off", "", ""); // Tomorrow

        // Check system alarm
        assertSystemAlarmCount(1);
        assertSystemAlarm(YEAR, MONTH, DAY, DEFAULT_ALARM_HOUR, DEFAULT_ALARM_MINUTE, SystemAlarm.ACTION_ALARM_TIME_OF_EARLY_DISMISSED_ALARM);
        assertSystemAlarmClockNone();

        // Check notification
        assertNotificationCount(0);

        // Check widget
        assertWidget(R.drawable.ic_alarm_off_white, "No alarm", null);
    }

    @Test
    public void t400_onRing() {
        prepareUntilRing();

        // Check that ringing started
        Activity activity = Robolectric.setupActivity(Activity.class);
        ShadowActivity shadowActivity = Shadows.shadowOf(activity);

        Intent intentNext = shadowActivity.peekNextStartedActivity();
        Intent expectedIntentNext = new Intent(context, RingActivity.class);

        assertThat(intentNext.getComponent(), is(expectedIntentNext.getComponent()));

        // Check system alarm
        assertSystemAlarmCount(1);
        assertSystemAlarm(YEAR, MONTH, DAY + 1, 0, 0, SystemAlarm.ACTION_SET_SYSTEM_ALARM);
        assertSystemAlarmClockNone();

        // Check notification
        assertNotificationCount(1);

        Notification notification = shadowNotificationManager.getAllNotifications().get(0);
        assertNotification(notification, "Alarm at 7:00 AM", "Ringing");
        assertNotificationActionCount(notification, 2);
        assertNotificationAction(notification, 0, "Dismiss", NotificationReceiver.ACTION_DISMISS);
        assertNotificationAction(notification, 1, "Snooze", NotificationReceiver.ACTION_SNOOZE);

        // Start ring activity
        Calendar alarmTime = new GregorianCalendar(YEAR, MONTH, DAY, HOUR_DEFAULT, MINUTE_DEFAULT);
        startActivityRing(alarmTime);

        // Check appearance
        assertThat("Date visibility", textDate.getVisibility(), is(View.VISIBLE));
        assertThat("Time visibility", textTime.getVisibility(), is(View.VISIBLE));
        assertThat("Alarm time visibility", textAlarmTime.getVisibility(), is(View.INVISIBLE));
        assertThat("Alarm name visibility", textOneTimeAlarmName.getVisibility(), is(View.GONE));
        assertThat("Calendar visibility", textNextCalendar.getVisibility(), is(View.GONE));
        assertThat("Muted visibility", textMuted.getVisibility(), is(View.INVISIBLE));

        assertThat("Date", textDate.getText(), is("Monday, February 1"));
        assertThat("Time", textTime.getText(), is("7:00 AM"));

        // Check widget
        assertWidget(R.drawable.ic_alarm_white, "7:00 AM", null);
    }

    @Test
    public void t401_onRingWithTomorrow() {
        // Consume the alarm with action ACTION_SET_SYSTEM_ALARM
        consumeNextScheduledAlarm();

        // Save day
        setAlarmToTomorrow();

        prepareUntilRing();

        // Check that ringing started
        Activity activity = Robolectric.setupActivity(Activity.class);
        ShadowActivity shadowActivity = Shadows.shadowOf(activity);

        Intent intentNext = shadowActivity.peekNextStartedActivity();
        Intent expectedIntentNext = new Intent(context, RingActivity.class);

        assertThat(intentNext.getComponent(), is(expectedIntentNext.getComponent()));

        // Check system alarm
        assertSystemAlarmCount(2);
        assertSystemAlarm(YEAR, MONTH, DAY + 1, DEFAULT_ALARM_HOUR - 1, DEFAULT_ALARM_MINUTE + 1, SystemAlarm.ACTION_RING_IN_NEAR_FUTURE);
        assertSystemAlarmClock(YEAR, MONTH, DAY + 1, DEFAULT_ALARM_HOUR + 1, DEFAULT_ALARM_MINUTE + 1);

        // Check notification
        assertNotificationCount(1);

        Notification notification = shadowNotificationManager.getAllNotifications().get(0);
        assertNotification(notification, "Alarm at 7:00 AM", "Ringing");
        assertNotificationActionCount(notification, 2);
        assertNotificationAction(notification, 0, "Dismiss", NotificationReceiver.ACTION_DISMISS);
        assertNotificationAction(notification, 1, "Snooze", NotificationReceiver.ACTION_SNOOZE);

        // Check widget
        assertWidget(R.drawable.ic_alarm_white, "7:00 AM", null);

        // Context menu - dismiss today's alarm
        startActivityCalendar();
        loadItemAtPosition(0);
        item.performLongClick();
        clickContextMenu(R.id.action_day_dismiss);
        refreshRecyclerView();

        // Check widget
        assertWidget(R.drawable.ic_alarm_white, "8:01 AM", "Tomorrow");

        // Shift clock by just under 2 hours
        shadowGlobalManager.setClock(new FixedClock(new GregorianCalendar(YEAR, MONTH, DAY, HOUR_DEFAULT + 1 + 2, MINUTE_DEFAULT, 59)));
        assertWidget(R.drawable.ic_alarm_white, "8:01 AM", "Tomorrow");

        // Shift clock
        shadowGlobalManager.setClock(new FixedClock(new GregorianCalendar(YEAR, MONTH, DAY, HOUR_DEFAULT + 1 + 2, MINUTE_DEFAULT + 1)));
        assertWidget(R.drawable.ic_alarm_white, "8:01 AM", null);
    }

    @Test
    public void t402_dismissWhileRinging() {
        prepareUntilRing();

        // Start ring activity
        Calendar alarmTime = new GregorianCalendar(YEAR, MONTH, DAY, HOUR_DEFAULT, MINUTE_DEFAULT, 10);
        startActivityRing(alarmTime);

        dismissButton.performClick();

        // Check system alarm
        assertSystemAlarmCount(1);
        assertSystemAlarm(YEAR, MONTH, DAY + 1, 0, 0, SystemAlarm.ACTION_SET_SYSTEM_ALARM);
        assertSystemAlarmClockNone();

        // Check notification
        assertNotificationCount(0);

        // Check widget
        assertWidget(R.drawable.ic_alarm_off_white, "No alarm", null);
    }

    @Test
    public void t403_dismissWhileRingingAndNextAlarmIsInNearPeriod() {
        // Section 1: analogous to prepareUntilNear() but with alarms at 23:30 and 1:00

        // Consume the alarm with action ACTION_SET_SYSTEM_ALARM
        consumeNextScheduledAlarm();

        // Set alarms at 23:30 and 1:00
        Calendar todayAlarm = new GregorianCalendar(YEAR, MONTH, DAY, 23, 30);
        setAlarm(todayAlarm);

        Calendar tomorrowAlarm = new GregorianCalendar(YEAR, MONTH, DAY + 1, 1, 0);
        setAlarm(tomorrowAlarm);

        // Consume the alarm with action ACTION_RING_IN_NEAR_FUTURE
        consumeNextScheduledAlarm();

        // Shift clock
        shadowGlobalManager.setClock(new FixedClock(new GregorianCalendar(YEAR, MONTH, DAY, 23 - 2, 30)));

        // Call the receiver
        Intent intent = new Intent();
        intent.setAction(SystemAlarm.ACTION_RING_IN_NEAR_FUTURE);
        AlarmReceiver alarmReceiver = new AlarmReceiver();
        alarmReceiver.onReceive(context, intent);

        shadowGlobalManager.setClock(new FixedClock(new GregorianCalendar(YEAR, MONTH, DAY, 23 - 2, 30, 10)));

        // Section 2: analogous to prepareUntilRing()

        // Consume the alarm with action ACTION_RING
        consumeNextScheduledAlarm();

        // Shift clock
        shadowGlobalManager.setClock(new FixedClock(new GregorianCalendar(YEAR, MONTH, DAY, 23, 30)));

        // Call the receiver
        Intent intent2 = new Intent();
        intent2.setAction(SystemAlarm.ACTION_RING);
        AlarmReceiver alarmReceiver2 = new AlarmReceiver();
        alarmReceiver2.onReceive(context, intent2);

        shadowGlobalManager.setClock(new FixedClock(new GregorianCalendar(YEAR, MONTH, DAY, 23, 30, 10)));

        // Section 3: checks

        // Start ring activity
        Calendar alarmTime = new GregorianCalendar(YEAR, MONTH, DAY, 23, 30, 10);
        startActivityRing(alarmTime);

        dismissButton.performClick();

        // Check system alarm
        assertSystemAlarmCount(2);
        assertSystemAlarm(YEAR, MONTH, DAY + 1, 1, 0, SystemAlarm.ACTION_RING);
        assertSystemAlarmClock(YEAR, MONTH, DAY + 1, 1, 0);

        // Check notification
        assertNotificationCount(1);

        Notification notification = shadowNotificationManager.getAllNotifications().get(0);
        assertNotification(notification, "Alarm at 1:00 AM", "Touch to view all alarms");
        assertNotificationActionCount(notification, 1);
        assertNotificationAction(notification, 0, "Dismiss", NotificationReceiver.ACTION_DISMISS_BEFORE_RINGING);

        // Check widget
        assertWidget(R.drawable.ic_alarm_white, "1:00 AM", null);
    }

    @Test
    public void t430_snoozeWhileRinging() {
        prepareUntilRing();

        // Consume the alarm with action ACTION_SET_SYSTEM_ALARM
        consumeNextScheduledAlarm();

        // Start ring activity
        Calendar alarmTime = new GregorianCalendar(YEAR, MONTH, DAY, HOUR_DEFAULT, MINUTE_DEFAULT, 10);
        startActivityRing(alarmTime);

        snoozeButton.performClick();

        // Start Calendar
        startActivityCalendar();

        // Check calendar
        assertCalendarItem(0, "2/1", "Mon", "7:00 AM", "Snoozed", "–10s"); // Today
        assertCalendarItem(1, "2/2", "Tue", "Off", "", ""); // Tomorrow

        // Check system alarm
        assertSystemAlarmCount(1);
        assertSystemAlarm(YEAR, MONTH, DAY, DEFAULT_ALARM_HOUR, DEFAULT_ALARM_MINUTE + 10, SystemAlarm.ACTION_RING);
        assertSystemAlarmClockNone();

        // Check notification
        assertNotificationCount(1);

        Notification notification = shadowNotificationManager.getAllNotifications().get(0);
        assertNotification(notification, "Alarm at 7:00 AM", "Snoozed till 7:10 AM");
        assertNotificationActionCount(notification, 1);
        assertNotificationAction(notification, 0, "Dismiss", NotificationReceiver.ACTION_DISMISS);

        // Check widget
        assertWidget(R.drawable.ic_alarm_white, "7:00 AM", null);
    }

    @Test
    public void t510_dismissWhileSnoozed() {
        // Consume the alarm with action ACTION_SET_SYSTEM_ALARM
        prepareUntilSnooze();

        // Context menu
        startActivityCalendar();

        loadItemAtPosition(0);

        item.performLongClick();

        clickContextMenu(R.id.action_day_dismiss);

        // Check calendar
        assertCalendarItem(0, "2/1", "Mon", "7:00 AM", "Passed", ""); // Today
        assertCalendarItem(1, "2/2", "Tue", "Off", "", ""); // Tomorrow

        // Check system alarm
        assertSystemAlarmCount(1);
        assertSystemAlarm(YEAR, MONTH, DAY + 1, 0, 0, SystemAlarm.ACTION_SET_SYSTEM_ALARM);
        assertSystemAlarmClockNone();

        // Check notification
        assertNotificationCount(0);

        // Check widget
        assertWidget(R.drawable.ic_alarm_off_white, "No alarm", null);
    }

    /**
     * Note: this action is not possible in UI.
     */
    @Test
    public void t520_disableWhileSnoozed() {
        prepareUntilSnooze();

        // Context menu
        startActivityCalendar();

        loadItemAtPosition(0);

        item.performLongClick();

        clickContextMenu(R.id.action_day_disable);

        // Check calendar
        assertCalendarItem(0, "2/1", "Mon", "Off", "", ""); // Today
        assertCalendarItem(1, "2/2", "Tue", "Off", "", ""); // Tomorrow

        // Check system alarm
        assertSystemAlarmCount(1);
        assertSystemAlarm(YEAR, MONTH, DAY + 1, 0, 0, SystemAlarm.ACTION_SET_SYSTEM_ALARM);
        assertSystemAlarmClockNone();

        // Check notification
        assertNotificationCount(0);

        // Check widget
        assertWidget(R.drawable.ic_alarm_off_white, "No alarm", null);
    }

    @Test
    public void t540_setTimeWhileSnoozed() {
        prepareUntilSnooze();

        // Set day alarm
        calendar_setDayAlarm(0, DEFAULT_ALARM_HOUR, DEFAULT_ALARM_MINUTE + 30, DEFAULT_ALARM_HOUR + 6, DEFAULT_ALARM_MINUTE);

        // Check calendar
        assertCalendarItem(0, "2/1", "Mon", "1:00 PM", "Changed", "5h 59m"); // Today
        assertCalendarItem(1, "2/2", "Tue", "Off", "", ""); // Tomorrow

        // Check system alarm
        assertSystemAlarmCount(2);
        assertSystemAlarm(YEAR, MONTH, DAY, DEFAULT_ALARM_HOUR + 4, DEFAULT_ALARM_MINUTE, SystemAlarm.ACTION_RING_IN_NEAR_FUTURE);
        assertSystemAlarmClock(YEAR, MONTH, DAY, DEFAULT_ALARM_HOUR + 6, DEFAULT_ALARM_MINUTE);

        // Check notification
        assertNotificationCount(0);

        // Check widget
        assertWidget(R.drawable.ic_alarm_white, "1:00 PM", null);
    }

    /**
     * Note: this action is not possible in UI.
     */
    @Test
    public void t570_revertWhileSnoozed() {
        // Consume the alarm with action ACTION_SET_SYSTEM_ALARM
        prepareUntilSnooze();

        // Context menu
        startActivityCalendar();

        loadItemAtPosition(0);

        item.performLongClick();

        clickContextMenu(R.id.action_day_revert);

        // Check calendar
        assertCalendarItem(0, "2/1", "Mon", "Off", "", ""); // Today
        assertCalendarItem(1, "2/2", "Tue", "Off", "", ""); // Tomorrow

        // Check system alarm
        assertSystemAlarmCount(1);
        assertSystemAlarm(YEAR, MONTH, DAY + 1, 0, 0, SystemAlarm.ACTION_SET_SYSTEM_ALARM);
        assertSystemAlarmClockNone();

        // Check notification
        assertNotificationCount(0);

        // Check widget
        assertWidget(R.drawable.ic_alarm_off_white, "No alarm", null);
    }

    private void prepareUntilNear() {
        // Consume the alarm with action ACTION_SET_SYSTEM_ALARM
        consumeNextScheduledAlarm();

        // Save day
        setAlarmToToday();

        // Consume the alarm with action ACTION_RING_IN_NEAR_FUTURE
        consumeNextScheduledAlarm();
        // Consume the alarm involving System Alarm Clock
        consumeNextScheduledAlarm();

        // Shift clock
        shadowGlobalManager.setClock(new FixedClock(new GregorianCalendar(YEAR, MONTH, DAY, HOUR_DEFAULT - 2, MINUTE_DEFAULT)));

        // Call the receiver
        Intent intent = new Intent();
        intent.setAction(SystemAlarm.ACTION_RING_IN_NEAR_FUTURE);
        AlarmReceiver alarmReceiver = new AlarmReceiver();
        alarmReceiver.onReceive(context, intent);

        shadowGlobalManager.setClock(new FixedClock(new GregorianCalendar(YEAR, MONTH, DAY, HOUR_DEFAULT - 2, MINUTE_DEFAULT, 10)));
    }

    private void prepareUntilRing() {
        prepareUntilNear();

        // Consume the alarm with action ACTION_RING
        consumeNextScheduledAlarm();

        // Shift clock
        shadowGlobalManager.setClock(new FixedClock(new GregorianCalendar(YEAR, MONTH, DAY, HOUR_DEFAULT, MINUTE_DEFAULT)));

        // Call the receiver
        Intent intent = new Intent();
        intent.setAction(SystemAlarm.ACTION_RING);
        AlarmReceiver alarmReceiver = new AlarmReceiver();
        alarmReceiver.onReceive(context, intent);

        shadowGlobalManager.setClock(new FixedClock(new GregorianCalendar(YEAR, MONTH, DAY, HOUR_DEFAULT, MINUTE_DEFAULT, 10)));
    }

    private void prepareUntilSnooze() {
        prepareUntilRing();

        // Consume the alarm with action ACTION_SET_SYSTEM_ALARM
        consumeNextScheduledAlarm();

        // Start ring activity
        Calendar alarmTime = new GregorianCalendar(YEAR, MONTH, DAY, HOUR_DEFAULT, MINUTE_DEFAULT);
        startActivityRing(alarmTime);

        snoozeButton.performClick();

        // Consume the alarm with action ACTION_RING
        consumeNextScheduledAlarm();

        shadowGlobalManager.setClock(new FixedClock(new GregorianCalendar(YEAR, MONTH, DAY, HOUR_DEFAULT, MINUTE_DEFAULT, 20)));
    }

    private void setAlarmToToday() {
        Calendar date = new GregorianCalendar(YEAR, MONTH, DAY, HOUR_DEFAULT, MINUTE_DEFAULT);
        setAlarm(date);
    }

    private void setAlarmToTomorrow() {
        Calendar date = new GregorianCalendar(YEAR, MONTH, DAY + 1, HOUR_DEFAULT + 1, MINUTE_DEFAULT + 1);
        setAlarm(date);
    }

    private void setAlarm(Calendar date) {
        Day day = new Day();
        day.setDate(date);
        day.setState(Day.STATE_ENABLED);
        day.setHourDay(date.get(Calendar.HOUR_OF_DAY));
        day.setMinuteDay(date.get(Calendar.MINUTE));

        Defaults defaults = new Defaults();
        int dayOfWeek = date.get(Calendar.DAY_OF_WEEK);
        defaults.setDayOfWeek(dayOfWeek);
        defaults.setState(Defaults.STATE_DISABLED);

        day.setDefaults(defaults);

        Analytics analytics = new Analytics(Analytics.Channel.Test, Analytics.ChannelName.Calendar);

        globalManager.modifyDayAlarm(day, analytics);
    }

}
