package cz.jaro.alarmmorning.app;

import android.app.Activity;
import android.app.Notification;
import android.content.Intent;
import android.view.View;

import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.robolectric.Robolectric;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowActivity;

import java.util.Calendar;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.List;

import cz.jaro.alarmmorning.Analytics;
import cz.jaro.alarmmorning.GlobalManager;
import cz.jaro.alarmmorning.R;
import cz.jaro.alarmmorning.RingActivity;
import cz.jaro.alarmmorning.SystemAlarm;
import cz.jaro.alarmmorning.clock.FixedClock;
import cz.jaro.alarmmorning.model.OneTimeAlarm;
import cz.jaro.alarmmorning.receivers.AlarmReceiver;
import cz.jaro.alarmmorning.receivers.NotificationReceiver;
import cz.jaro.alarmmorning.shadows.ShadowGlobalManager;

import static cz.jaro.alarmmorning.GlobalManager.HORIZON_DAYS;
import static cz.jaro.alarmmorning.app.CalendarWithOneTimeAlarmTest.ONE_TIME_ALARM_HOUR;
import static cz.jaro.alarmmorning.app.CalendarWithOneTimeAlarmTest.ONE_TIME_ALARM_MINUTE;
import static cz.jaro.alarmmorning.model.DayTest.DAY;
import static cz.jaro.alarmmorning.model.DayTest.HOUR;
import static cz.jaro.alarmmorning.model.DayTest.MONTH;
import static cz.jaro.alarmmorning.model.DayTest.YEAR;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

/**
 * Tests of alarm management in UI.
 * <p>
 * Two one-time alarms are used.
 */
@Config(shadows = {ShadowGlobalManager.class})
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class CalendarWithTwoAlarmsTest extends AlarmMorningAppTest {
    /*
    Overview of times:

       Current time                                Alarm time
       1:00                                        4:30
    ---+-------------------------------------------+--------------------------->
       HOUR                                        ONE_TIME_ALARM_HOUR
       MINUTE                                      ONE_TIME_ALARM_MINUTE

    Derived:

                               Near period starts
                               2:30
    ---+-----------------------+-------------------+--------------------------->

       Tests t1xx (_add) and t2xx (_ofDistantAlarm_) are executed
       1:00
    ---+-----------------------+-------------------+--------------------------->

                                Tests t3xx (_whileInNearPeriod_) are executed
                                2:30:10
    ---+-----------------------+-------------------+--------------------------->

                                                    Tests t4xx (_whileRinging_) are executed
                                                    4:30:10
    ---+-----------------------+-------------------+--------------------------->

                                                     Tests t5xx (_whileSnoozed_) are executed
                                                     4:30:20
    ---+-----------------------+-------------------+--------------------------->

    */

    private void shared_t101_addTwoAlarms_BothInNearPeriod() {
        // Check calendar
        assertCalendarItem(0, "2/1", "Mon", "Off", "", null, ""); // Today
        assertCalendarItem(1, "", "", "2:01 AM", "", "", "1h 1m"); // The 1st one-time alarm
        assertCalendarItem(2, "", "", "2:02 AM", "", "", ""); // The 2nd one-time alarm
        assertCalendarItem(3, "2/2", "Tue", "Off", "", null, ""); // Tomorrow

        // Check system alarm
        assertSystemAlarmCount(2);
        assertSystemAlarm(YEAR, MONTH, DAY, HOUR + 1, 1, SystemAlarm.ACTION_RING);
        assertSystemAlarmClock(YEAR, MONTH, DAY, HOUR + 1, 1);

        // Check notification
        assertNotificationCount(1);

        Notification notification = shadowNotificationManager.getAllNotifications().get(0);
        assertNotification(notification, "Alarm at 2:01 AM", "Touch to view all alarms");
        assertNotificationActionCount(notification, 1);
        assertNotificationAction(notification, 0, "Dismiss", NotificationReceiver.ACTION_DISMISS_BEFORE_RINGING);

        // Check widget
        assertWidget(R.drawable.ic_alarm_white, "2:01 AM", null);
    }

    @Test
    public void t101a_addTwoAlarms_inNearPeriod_order12() {
        // Consume the alarm with action ACTION_SET_SYSTEM_ALARM
        consumeNextScheduledAlarm();

        // Add one-time alarms
        int oldCount = calendar_addOneTimeAlarm(0, HOUR + 1, 0, HOUR + 1, 2);
        calendar_addOneTimeAlarm(0, HOUR + 1, 0, HOUR + 1, 1);

        // Check calendar
        int newCount = recyclerView.getChildCount();
        assertThat("The number of items increased by two", newCount - oldCount, is(2));

        shared_t101_addTwoAlarms_BothInNearPeriod();
    }

    @Test
    public void t101b_addTwoAlarms_inNearPeriod_order21() {
        // Consume the alarm with action ACTION_SET_SYSTEM_ALARM
        consumeNextScheduledAlarm();

        // Add one-time alarms
        int oldCount = calendar_addOneTimeAlarm(0, HOUR + 1, 0, HOUR + 1, 1);
        calendar_addOneTimeAlarm(0, HOUR + 1, 0, HOUR + 1, 2);

        // Check calendar
        int newCount = recyclerView.getChildCount();
        assertThat("The number of items increased by two", newCount - oldCount, is(2));

        shared_t101_addTwoAlarms_BothInNearPeriod();
    }

    private void shared_t102_addTwoAlarms_afterNearPeriod() {
        // Check calendar
        assertCalendarItem(0, "2/1", "Mon", "Off", "", null, ""); // Today
        assertCalendarItem(1, "", "", "4:31 AM", "", "", "3h 31m"); // The 1st one-time alarm
        assertCalendarItem(2, "", "", "4:32 AM", "", "", ""); // The 2nd one-time alarm
        assertCalendarItem(3, "2/2", "Tue", "Off", "", null, ""); // Tomorrow

        // Check system alarm
        assertSystemAlarmCount(2);
        assertSystemAlarm(YEAR, MONTH, DAY, ONE_TIME_ALARM_HOUR - 2, ONE_TIME_ALARM_MINUTE + 1, SystemAlarm.ACTION_RING_IN_NEAR_FUTURE);
        assertSystemAlarmClock(YEAR, MONTH, DAY, ONE_TIME_ALARM_HOUR, ONE_TIME_ALARM_MINUTE + 1);

        // Check notification
        assertNotificationCount(0);

        // Check widget
        assertWidget(R.drawable.ic_alarm_white, "4:31 AM", null);
    }

    @Test
    public void t102a_addTwoAlarms_afterNearPeriod_order12() {
        // Consume the alarm with action ACTION_SET_SYSTEM_ALARM
        consumeNextScheduledAlarm();

        // Add one-time alarms
        int oldCount = calendar_addOneTimeAlarm(0, HOUR + 1, 0, ONE_TIME_ALARM_HOUR, ONE_TIME_ALARM_MINUTE + 1);
        calendar_addOneTimeAlarm(0, HOUR + 1, 0, ONE_TIME_ALARM_HOUR, ONE_TIME_ALARM_MINUTE + 2);

        // Check calendar
        int newCount = recyclerView.getChildCount();
        assertThat("The number of items increased by two", newCount - oldCount, is(2));

        shared_t102_addTwoAlarms_afterNearPeriod();
    }

    @Test
    public void t102b_addTwoAlarms_afterNearFuture_order21() {
        // Consume the alarm with action ACTION_SET_SYSTEM_ALARM
        consumeNextScheduledAlarm();

        // Add one-time alarms
        int oldCount = calendar_addOneTimeAlarm(0, HOUR + 1, 0, ONE_TIME_ALARM_HOUR, ONE_TIME_ALARM_MINUTE + 2);
        calendar_addOneTimeAlarm(0, HOUR + 1, 0, ONE_TIME_ALARM_HOUR, ONE_TIME_ALARM_MINUTE + 1);

        // Check calendar
        int newCount = recyclerView.getChildCount();
        assertThat("The number of items increased by two", newCount - oldCount, is(2));

        shared_t102_addTwoAlarms_afterNearPeriod();
    }

    @Test
    public void t110_addTwoAlarmsAtTheSameTime_viaCalendar() {
        // Consume the alarm with action ACTION_SET_SYSTEM_ALARM
        consumeNextScheduledAlarm();

        // Add one-time alarms
        int oldCount = calendar_addOneTimeAlarm(0, HOUR + 1, 0, ONE_TIME_ALARM_HOUR, ONE_TIME_ALARM_MINUTE);
        calendar_addOneTimeAlarm(0, HOUR + 1, 0, ONE_TIME_ALARM_HOUR, ONE_TIME_ALARM_MINUTE);

        // Check calendar
        int newCount = recyclerView.getChildCount();
        assertThat("The number of items increased by two", newCount - oldCount, is(2));

        assertCalendarItem(0, "2/1", "Mon", "Off", "", null, ""); // Today
        assertCalendarItem(1, "", "", "4:30 AM", "", "", "3h 30m"); // The 1st one-time alarm
        assertCalendarItem(2, "", "", "4:30 AM", "", "", "3h 30m"); // The 2nd one-time alarm
        assertCalendarItem(3, "2/2", "Tue", "Off", "", null, ""); // Tomorrow

        // Check system alarm
        assertSystemAlarmCount(2);
        assertSystemAlarm(YEAR, MONTH, DAY, ONE_TIME_ALARM_HOUR - 2, ONE_TIME_ALARM_MINUTE, SystemAlarm.ACTION_RING_IN_NEAR_FUTURE);
        assertSystemAlarmClock(YEAR, MONTH, DAY, ONE_TIME_ALARM_HOUR, ONE_TIME_ALARM_MINUTE);

        // Check notification
        assertNotificationCount(0);

        // Check widget
        assertWidget(R.drawable.ic_alarm_white, "4:30 AM", null);
    }

    @Test
    public void t111_addTwoAlarmsAtTheSameTime_viaGlobalManager() {
        // Consume the alarm with action ACTION_SET_SYSTEM_ALARM
        consumeNextScheduledAlarm();

        setAlarm(new GregorianCalendar(YEAR, MONTH, DAY, ONE_TIME_ALARM_HOUR, ONE_TIME_ALARM_MINUTE), "One");
        setAlarm(new GregorianCalendar(YEAR, MONTH, DAY, ONE_TIME_ALARM_HOUR, ONE_TIME_ALARM_MINUTE), "Two");

        startActivityCalendar();
        refreshRecyclerView();

        // Check calendar
        assertThat("The number of items increased by two", recyclerView.getChildCount(), is(HORIZON_DAYS + 2));

        assertCalendarItem(0, "2/1", "Mon", "Off", "", null, ""); // Today
        assertCalendarItem(1, "", "", "4:30 AM", "", "One", "3h 30m"); // The 1st one-time alarm
        assertCalendarItem(2, "", "", "4:30 AM", "", "Two", "3h 30m"); // The 2nd one-time alarm
        assertCalendarItem(3, "2/2", "Tue", "Off", "", null, ""); // Tomorrow

        // Check system alarm
        assertSystemAlarmCount(2);
        assertSystemAlarm(YEAR, MONTH, DAY, ONE_TIME_ALARM_HOUR - 2, ONE_TIME_ALARM_MINUTE, SystemAlarm.ACTION_RING_IN_NEAR_FUTURE);
        assertSystemAlarmClock(YEAR, MONTH, DAY, ONE_TIME_ALARM_HOUR, ONE_TIME_ALARM_MINUTE);

        // Check notification
        assertNotificationCount(0);

        // Check widget
        assertWidget(R.drawable.ic_alarm_white, "4:30 AM", null);
    }

    public void shared_t200_dismiss_ofDistantTwoAlarms_noneAlarm() {
        // Check calendar
        assertThat("The number of items hasn't changed", recyclerView.getChildCount(), is(HORIZON_DAYS + 2));
        assertCalendarItem(0, "2/1", "Mon", "Off", "", null, ""); // Today
        assertCalendarItem(1, "", "", "4:31 AM", "Dismissed before ringing", "", ""); // The 1st one-time alarm
        assertCalendarItem(2, "", "", "4:32 AM", "Dismissed before ringing", "", ""); // The 2nd one-time alarm
        assertCalendarItem(3, "2/2", "Tue", "Off", "", null, ""); // Tomorrow

        // Check system alarm
        assertSystemAlarmCount(1);
        assertSystemAlarm(YEAR, MONTH, DAY, ONE_TIME_ALARM_HOUR, ONE_TIME_ALARM_MINUTE + 1, SystemAlarm.ACTION_ALARM_TIME_OF_EARLY_DISMISSED_ALARM);
        assertSystemAlarmClockNone();

        // Check notification
        assertNotificationCount(0);

        // Check widget
        assertWidget(R.drawable.ic_alarm_off_white, "No alarm", null);
    }

    // There are two alarms at T+1 and T+2; dismiss the T+1 alarm, then dismiss T+2 alarm.
    @Test
    public void t200a_dismiss_ofDistantTwoAlarms_order12() {
        t102a_addTwoAlarms_afterNearPeriod_order12();

        // ----------------------------------------------------

        // Dismiss the 1st one-time alarm
        loadItemAtPosition(1);
        item.performLongClick();
        clickContextMenu(R.id.action_day_dismiss);
        refreshRecyclerView();

        // Check calendar
        assertThat("The number of items hasn't changed", recyclerView.getChildCount(), is(HORIZON_DAYS + 2));

        assertCalendarItem(0, "2/1", "Mon", "Off", "", null, ""); // Today
        assertCalendarItem(1, "", "", "4:31 AM", "Dismissed before ringing", "", ""); // The 1st one-time alarm
        assertCalendarItem(2, "", "", "4:32 AM", "", "", "3h 32m"); // The 2nd one-time alarm
        assertCalendarItem(3, "2/2", "Tue", "Off", "", null, ""); // Tomorrow

        // Check system alarm
        assertSystemAlarmCount(2);
        assertSystemAlarm(YEAR, MONTH, DAY, ONE_TIME_ALARM_HOUR - 2, ONE_TIME_ALARM_MINUTE + 2, SystemAlarm.ACTION_RING_IN_NEAR_FUTURE);
        assertSystemAlarmClock(YEAR, MONTH, DAY, ONE_TIME_ALARM_HOUR, ONE_TIME_ALARM_MINUTE + 2);

        // Check notification
        assertNotificationCount(0);

        // Check widget
        assertWidget(R.drawable.ic_alarm_white, "4:32 AM", null);

        // ----------------------------------------------------

        // Dismiss the 2nd alarm
        loadItemAtPosition(2);
        item.performLongClick();
        clickContextMenu(R.id.action_day_dismiss);
        refreshRecyclerView();

        // Checks
        shared_t200_dismiss_ofDistantTwoAlarms_noneAlarm();
    }

    // There are two alarms at T+1 and T+2; dismiss the T+2 alarm, then dismiss T+1 alarm.
    @Test
    public void t200b_dismiss_ofDistantTwoAlarms_order21() {
        t102a_addTwoAlarms_afterNearPeriod_order12();

        // ----------------------------------------------------

        // Dismiss the 2nd alarm
        loadItemAtPosition(2);
        item.performLongClick();
        clickContextMenu(R.id.action_day_dismiss);
        refreshRecyclerView();

        // Check calendar
        assertThat("The number of items hasn't changed", recyclerView.getChildCount(), is(HORIZON_DAYS + 2));

        assertCalendarItem(0, "2/1", "Mon", "Off", "", null, ""); // Today
        assertCalendarItem(1, "", "", "4:31 AM", "", "", "3h 31m"); // The 1st one-time alarm
        assertCalendarItem(2, "", "", "4:32 AM", "Dismissed before ringing", "", ""); // The 2nd one-time alarm
        assertCalendarItem(3, "2/2", "Tue", "Off", "", null, ""); // Tomorrow

        // Check system alarm
        assertSystemAlarmCount(2);
        assertSystemAlarm(YEAR, MONTH, DAY, ONE_TIME_ALARM_HOUR - 2, ONE_TIME_ALARM_MINUTE + 1, SystemAlarm.ACTION_RING_IN_NEAR_FUTURE);
        assertSystemAlarmClock(YEAR, MONTH, DAY, ONE_TIME_ALARM_HOUR, ONE_TIME_ALARM_MINUTE + 1);

        // Check notification
        assertNotificationCount(0);

        // Check widget
        assertWidget(R.drawable.ic_alarm_white, "4:31 AM", null);

        // ----------------------------------------------------

        // Dismiss the 1st one-time alarm
        loadItemAtPosition(1);
        item.performLongClick();
        clickContextMenu(R.id.action_day_dismiss);
        refreshRecyclerView();

        // Checks
        shared_t200_dismiss_ofDistantTwoAlarms_noneAlarm();
    }

    public void shared_t220_dismiss_ofDistantTwoAlarmsAtTheSameTime_noneAlarm() {
        // Check calendar
        assertThat("The number of items hasn't changed", recyclerView.getChildCount(), is(HORIZON_DAYS + 2));
        assertCalendarItem(0, "2/1", "Mon", "Off", "", null, ""); // Today
        assertCalendarItem(1, "", "", "4:30 AM", "Dismissed before ringing", "One", ""); // The 1st one-time alarm
        assertCalendarItem(2, "", "", "4:30 AM", "Dismissed before ringing", "Two", ""); // The 2nd one-time alarm
        assertCalendarItem(3, "2/2", "Tue", "Off", "", null, ""); // Tomorrow

        // Check system alarm
        assertSystemAlarmCount(1);
        assertSystemAlarm(YEAR, MONTH, DAY, ONE_TIME_ALARM_HOUR, ONE_TIME_ALARM_MINUTE, SystemAlarm.ACTION_ALARM_TIME_OF_EARLY_DISMISSED_ALARM);
        assertSystemAlarmClockNone();

        // Check notification
        assertNotificationCount(0);

        // Check widget
        assertWidget(R.drawable.ic_alarm_off_white, "No alarm", null);
    }

    @Test
    public void t210a_dismiss_ofDistantTwoAlarmsAtTheSameTime_order12() {
        // Consume the alarm with action ACTION_SET_SYSTEM_ALARM
        consumeNextScheduledAlarm();

        setAlarm(new GregorianCalendar(YEAR, MONTH, DAY, ONE_TIME_ALARM_HOUR, ONE_TIME_ALARM_MINUTE), "One");
        setAlarm(new GregorianCalendar(YEAR, MONTH, DAY, ONE_TIME_ALARM_HOUR, ONE_TIME_ALARM_MINUTE), "Two");

        startActivityCalendar();
        refreshRecyclerView();

        // ----------------------------------------------------

        // Dismiss the 1st one-time alarm
        loadItemAtPosition(1);
        item.performLongClick();
        clickContextMenu(R.id.action_day_dismiss);
        refreshRecyclerView();

        // Check calendar
        assertThat("The number of items hasn't changed", recyclerView.getChildCount(), is(HORIZON_DAYS + 2));
        assertCalendarItem(0, "2/1", "Mon", "Off", "", null, ""); // Today
        assertCalendarItem(1, "", "", "4:30 AM", "Dismissed before ringing", "One", ""); // The 1st one-time alarm
        assertCalendarItem(2, "", "", "4:30 AM", "", "Two", "3h 30m"); // The 2nd one-time alarm
        assertCalendarItem(3, "2/2", "Tue", "Off", "", null, ""); // Tomorrow

        // Check system alarm
        assertSystemAlarmCount(2);
        assertSystemAlarm(YEAR, MONTH, DAY, ONE_TIME_ALARM_HOUR - 2, ONE_TIME_ALARM_MINUTE, SystemAlarm.ACTION_RING_IN_NEAR_FUTURE);
        assertSystemAlarmClock(YEAR, MONTH, DAY, ONE_TIME_ALARM_HOUR, ONE_TIME_ALARM_MINUTE);

        // Check notification
        assertNotificationCount(0);

        // Check widget
        assertWidget(R.drawable.ic_alarm_white, "4:30 AM", null);

        // ----------------------------------------------------

        // Dismiss the 2nd alarm
        loadItemAtPosition(2);
        item.performLongClick();
        clickContextMenu(R.id.action_day_dismiss);
        refreshRecyclerView();

        // Checks
        shared_t220_dismiss_ofDistantTwoAlarmsAtTheSameTime_noneAlarm();
    }

    @Test
    public void t210b_dismiss_ofDistantTwoAlarmsAtTheSameTime_order21() {
        // Consume the alarm with action ACTION_SET_SYSTEM_ALARM
        consumeNextScheduledAlarm();

        setAlarm(new GregorianCalendar(YEAR, MONTH, DAY, ONE_TIME_ALARM_HOUR, ONE_TIME_ALARM_MINUTE), "One");
        setAlarm(new GregorianCalendar(YEAR, MONTH, DAY, ONE_TIME_ALARM_HOUR, ONE_TIME_ALARM_MINUTE), "Two");

        startActivityCalendar();
        refreshRecyclerView();

        // Dismiss the 1st one-time alarm
        loadItemAtPosition(2);
        item.performLongClick();
        clickContextMenu(R.id.action_day_dismiss);
        refreshRecyclerView();

        // Check calendar
        assertThat("The number of items hasn't changed", recyclerView.getChildCount(), is(HORIZON_DAYS + 2));
        assertCalendarItem(0, "2/1", "Mon", "Off", "", null, ""); // Today
        assertCalendarItem(1, "", "", "4:30 AM", "", "One", "3h 30m"); // The 1st one-time alarm
        assertCalendarItem(2, "", "", "4:30 AM", "Dismissed before ringing", "Two", ""); // The 2nd one-time alarm
        assertCalendarItem(3, "2/2", "Tue", "Off", "", null, ""); // Tomorrow

        // Check system alarm
        assertSystemAlarmCount(2);
        assertSystemAlarm(YEAR, MONTH, DAY, ONE_TIME_ALARM_HOUR - 2, ONE_TIME_ALARM_MINUTE, SystemAlarm.ACTION_RING_IN_NEAR_FUTURE);
        assertSystemAlarmClock(YEAR, MONTH, DAY, ONE_TIME_ALARM_HOUR, ONE_TIME_ALARM_MINUTE);

        // Check notification
        assertNotificationCount(0);

        // Check widget
        assertWidget(R.drawable.ic_alarm_white, "4:30 AM", null);

        // ----------------------------------------------------

        // Dismiss the 2nd alarm
        loadItemAtPosition(1);
        item.performLongClick();
        clickContextMenu(R.id.action_day_dismiss);
        refreshRecyclerView();

        // Checks
        shared_t220_dismiss_ofDistantTwoAlarmsAtTheSameTime_noneAlarm();
    }

    private void shared_t310_dismiss_ofDistantTwoAlarms_noneAlarm() {
        // Check calendar
        assertThat("The number of items hasn't changed", recyclerView.getChildCount(), is(HORIZON_DAYS + 2));
        assertCalendarItem(0, "2/1", "Mon", "Off", "", null, ""); // Today
        assertCalendarItem(1, "", "", "2:01 AM", "Dismissed before ringing", "", ""); // The 1st one-time alarm
        assertCalendarItem(2, "", "", "2:02 AM", "Dismissed before ringing", "", ""); // The 2nd one-time alarm
        assertCalendarItem(3, "2/2", "Tue", "Off", "", null, ""); // Tomorrow

        // Check system alarm
        assertSystemAlarmCount(1);
        assertSystemAlarm(YEAR, MONTH, DAY, HOUR + 1, 1, SystemAlarm.ACTION_ALARM_TIME_OF_EARLY_DISMISSED_ALARM);
        assertSystemAlarmClockNone();

        // Check notification
        assertNotificationCount(0);

        // Check widget
        assertWidget(R.drawable.ic_alarm_off_white, "No alarm", null);
    }

    // There are two alarms at T+1 and T+2; dismiss the T+1 alarm, then dismiss T+2 alarm.
    @Test
    public void t310a_dismiss_ofNearTwoAlarms_order12() {
        t101a_addTwoAlarms_inNearPeriod_order12();

        // ----------------------------------------------------

        // Dismiss the 1st one-time alarm
        loadItemAtPosition(1);
        item.performLongClick();
        clickContextMenu(R.id.action_day_dismiss);
        refreshRecyclerView();

        // Check calendar
        assertThat("The number of items hasn't changed", recyclerView.getChildCount(), is(HORIZON_DAYS + 2));

        assertCalendarItem(0, "2/1", "Mon", "Off", "", null, ""); // Today
        assertCalendarItem(1, "", "", "2:01 AM", "Dismissed before ringing", "", ""); // The 1st one-time alarm
        assertCalendarItem(2, "", "", "2:02 AM", "", "", "1h 2m"); // The 2nd one-time alarm
        assertCalendarItem(3, "2/2", "Tue", "Off", "", null, ""); // Tomorrow

        // Check system alarm
        assertSystemAlarmCount(2);
        assertSystemAlarm(YEAR, MONTH, DAY, HOUR + 1, 1, SystemAlarm.ACTION_ALARM_TIME_OF_EARLY_DISMISSED_ALARM);
        assertSystemAlarmClock(YEAR, MONTH, DAY, HOUR + 1, 2);

        // Check notification
        assertNotificationCount(1);

        Notification notification = shadowNotificationManager.getAllNotifications().get(0);
        assertNotification(notification, "Alarm at 2:02 AM", "Touch to view all alarms");
        assertNotificationActionCount(notification, 1);
        assertNotificationAction(notification, 0, "Dismiss", NotificationReceiver.ACTION_DISMISS_BEFORE_RINGING);

        // Check widget
        assertWidget(R.drawable.ic_alarm_white, "2:02 AM", null);

        // ----------------------------------------------------

        // Dismiss the 2nd alarm
        loadItemAtPosition(2);
        item.performLongClick();
        clickContextMenu(R.id.action_day_dismiss);
        refreshRecyclerView();

        // Checks
        shared_t310_dismiss_ofDistantTwoAlarms_noneAlarm();
    }

    // There are two alarms at T+1 and T+2; dismiss the T+2 alarm, then dismiss T+1 alarm.
    @Test
    public void t310b_dismiss_ofNearTwoAlarms_order21() {
        t101a_addTwoAlarms_inNearPeriod_order12();

        // ----------------------------------------------------

        // Dismiss the 2nd alarm
        loadItemAtPosition(2);
        item.performLongClick();
        clickContextMenu(R.id.action_day_dismiss);
        refreshRecyclerView();

        // Check calendar
        assertThat("The number of items hasn't changed", recyclerView.getChildCount(), is(HORIZON_DAYS + 2));

        assertCalendarItem(0, "2/1", "Mon", "Off", "", null, ""); // Today
        assertCalendarItem(1, "", "", "2:01 AM", "", "", "1h 1m"); // The 1st one-time alarm
        assertCalendarItem(2, "", "", "2:02 AM", "Dismissed before ringing", "", ""); // The 2nd one-time alarm
        assertCalendarItem(3, "2/2", "Tue", "Off", "", null, ""); // Tomorrow

        // Check system alarm
        assertSystemAlarmCount(2);
        assertSystemAlarm(YEAR, MONTH, DAY, HOUR + 1, 1, SystemAlarm.ACTION_RING);
        assertSystemAlarmClock(YEAR, MONTH, DAY, HOUR + 1, 1);

        // Check notification
        assertNotificationCount(1);

        Notification notification = shadowNotificationManager.getAllNotifications().get(0);
        assertNotification(notification, "Alarm at 2:01 AM", "Touch to view all alarms");
        assertNotificationActionCount(notification, 1);
        assertNotificationAction(notification, 0, "Dismiss", NotificationReceiver.ACTION_DISMISS_BEFORE_RINGING);

        // Check widget
        assertWidget(R.drawable.ic_alarm_white, "2:01 AM", null);

        // ----------------------------------------------------

        // Dismiss the 1st one-time alarm
        loadItemAtPosition(1);
        item.performLongClick();
        clickContextMenu(R.id.action_day_dismiss);
        refreshRecyclerView();

        // Checks
        shared_t310_dismiss_ofDistantTwoAlarms_noneAlarm();
    }

    @Test
    public void t400_onRing_withTwoAlarms_PreviousIsNotDismissed() {
        t102a_addTwoAlarms_afterNearPeriod_order12();

        // ----------------------------------------------------

        // Start the first alarm

        // Shift clock
        shadowGlobalManager.setClock(new FixedClock(new GregorianCalendar(YEAR, MONTH, DAY, ONE_TIME_ALARM_HOUR, ONE_TIME_ALARM_MINUTE + 1)));

        // Call the receiver
        List<OneTimeAlarm> oneTimeAlarms = globalManager.loadOneTimeAlarms();
        Collections.sort(oneTimeAlarms);

        Intent intent = new Intent();
        intent.setAction(SystemAlarm.ACTION_RING);
        intent.putExtra(GlobalManager.PERSIST_ALARM_TYPE, oneTimeAlarms.get(0).getClass().getSimpleName());
        intent.putExtra(GlobalManager.PERSIST_ALARM_ID, oneTimeAlarms.get(0).getPersistenceId());
        AlarmReceiver alarmReceiver = new AlarmReceiver();
        alarmReceiver.onReceive(context, intent);

        // Shift clock
        shadowGlobalManager.setClock(new FixedClock(new GregorianCalendar(YEAR, MONTH, DAY, ONE_TIME_ALARM_HOUR, ONE_TIME_ALARM_MINUTE + 1, 10)));

        // ----------------------------------------------------

        // Check that ringing started
        Activity activity = Robolectric.setupActivity(Activity.class);
        ShadowActivity shadowActivity = Shadows.shadowOf(activity);

        Intent intentNext = shadowActivity.peekNextStartedActivity();
        Intent expectedIntentNext = new Intent(context, RingActivity.class);

        assertThat("Intent component", intentNext.getComponent(), is(expectedIntentNext.getComponent()));

        // Check ring activity
        Calendar alarmTime = new GregorianCalendar(YEAR, MONTH, DAY, ONE_TIME_ALARM_HOUR, ONE_TIME_ALARM_MINUTE + 1);
        startActivityRing(globalManager.loadOneTimeAlarms().get(0));

        assertThat("Date visibility", textDate.getVisibility(), is(View.VISIBLE));
        assertThat("Time visibility", textTime.getVisibility(), is(View.VISIBLE));
        assertThat("Alarm time visibility", textAlarmTime.getVisibility(), is(View.INVISIBLE));
        assertThat("Alarm name visibility", textOneTimeAlarmName.getVisibility(), is(View.GONE));
        assertThat("Calendar visibility", textNextCalendar.getVisibility(), is(View.GONE));
        assertThat("Muted visibility", textMuted.getVisibility(), is(View.INVISIBLE));

        assertThat("Date", textDate.getText(), is("Monday, February 1"));
        assertThat("Time", textTime.getText(), is("4:31 AM"));

        // Check calendar
        startActivityCalendar();

        assertThat("The number of items", recyclerView.getChildCount(), is(HORIZON_DAYS + 2));
        assertCalendarItem(0, "2/1", "Mon", "Off", "", null, ""); // Today
        assertCalendarItem(1, "", "", "4:31 AM", "Ringing", "", "-10s"); // The one-time alarm
        assertCalendarItem(2, "", "", "4:32 AM", "", "", ""); // The one-time alarm
        assertCalendarItem(3, "2/2", "Tue", "Off", "", null, ""); // Tomorrow

        // Check system alarm
        assertSystemAlarmCount(2);
        assertSystemAlarm(YEAR, MONTH, DAY, ONE_TIME_ALARM_HOUR, ONE_TIME_ALARM_MINUTE + 2, SystemAlarm.ACTION_RING);
        assertSystemAlarmClock(YEAR, MONTH, DAY, ONE_TIME_ALARM_HOUR, ONE_TIME_ALARM_MINUTE + 2);

        // Check notification
        assertNotificationCount(1);

        Notification notification = shadowNotificationManager.getAllNotifications().get(0);
        assertNotification(notification, "Alarm at 4:31 AM", "Ringing");
        assertNotificationActionCount(notification, 2);
        assertNotificationAction(notification, 0, "Dismiss", NotificationReceiver.ACTION_DISMISS);
        assertNotificationAction(notification, 1, "Snooze", NotificationReceiver.ACTION_SNOOZE);

        // Check widget
        assertWidget(R.drawable.ic_alarm_white, "4:31 AM", null);

        // ----------------------------------------------------

        // Start the second alarm
        // Note: the currently ringing alarm is intentionally NOT dismissed by the user when the 2nd alarm starts.

        // Shift clock
        shadowGlobalManager.setClock(new FixedClock(new GregorianCalendar(YEAR, MONTH, DAY, ONE_TIME_ALARM_HOUR, ONE_TIME_ALARM_MINUTE + 2)));

        // Call the receiver
        Intent intent2 = new Intent();
        intent2.setAction(SystemAlarm.ACTION_RING);
        intent2.putExtra(GlobalManager.PERSIST_ALARM_TYPE, oneTimeAlarms.get(1).getClass().getSimpleName());
        intent2.putExtra(GlobalManager.PERSIST_ALARM_ID, oneTimeAlarms.get(1).getPersistenceId());
        AlarmReceiver alarmReceiver2 = new AlarmReceiver();
        alarmReceiver2.onReceive(context, intent2);

        // Shift clock
        shadowGlobalManager.setClock(new FixedClock(new GregorianCalendar(YEAR, MONTH, DAY, ONE_TIME_ALARM_HOUR, ONE_TIME_ALARM_MINUTE + 2, 10)));

        // ----------------------------------------------------

        // Check that ringing started
        Activity activity2 = Robolectric.setupActivity(Activity.class);
        ShadowActivity shadowActivity2 = Shadows.shadowOf(activity2);

        Intent intentNext2 = shadowActivity2.peekNextStartedActivity();
        Intent expectedIntentNext2 = new Intent(context, RingActivity.class);

        assertThat("Intent component", intentNext2.getComponent(), is(expectedIntentNext2.getComponent()));

        // Check ring activity
        Calendar alarmTime2 = new GregorianCalendar(YEAR, MONTH, DAY, ONE_TIME_ALARM_HOUR, ONE_TIME_ALARM_MINUTE + 2);
        startActivityRing(globalManager.loadOneTimeAlarms().get(1));

        assertThat("Date visibility", textDate.getVisibility(), is(View.VISIBLE));
        assertThat("Time visibility", textTime.getVisibility(), is(View.VISIBLE));
        assertThat("Alarm time visibility", textAlarmTime.getVisibility(), is(View.INVISIBLE));
        assertThat("Alarm name visibility", textOneTimeAlarmName.getVisibility(), is(View.GONE));
        assertThat("Calendar visibility", textNextCalendar.getVisibility(), is(View.GONE));
        assertThat("Muted visibility", textMuted.getVisibility(), is(View.INVISIBLE));

        assertThat("Date", textDate.getText(), is("Monday, February 1"));
        assertThat("Time", textTime.getText(), is("4:32 AM"));

        // Check calendar
        startActivityCalendar();

        assertThat("The number of items", recyclerView.getChildCount(), is(HORIZON_DAYS + 2));
        assertCalendarItem(0, "2/1", "Mon", "Off", "", null, ""); // Today
        assertCalendarItem(1, "", "", "4:31 AM", "Passed", "", ""); // The one-time alarm
        assertCalendarItem(2, "", "", "4:32 AM", "Ringing", "", "-10s"); // The one-time alarm
        assertCalendarItem(3, "2/2", "Tue", "Off", "", null, ""); // Tomorrow

        // Check system alarm
        assertSystemAlarmCount(1);
        assertSystemAlarm(YEAR, MONTH, DAY + 1, 0, 0, SystemAlarm.ACTION_SET_SYSTEM_ALARM);
        assertSystemAlarmClockNone();

        // Check notification
        assertNotificationCount(2);

        Notification notification2 = shadowNotificationManager.getAllNotifications().get(1);
        assertNotification(notification2, "Alarm at 4:31 AM", "Alarm cancelled");
        assertNotificationActionCount(notification2, 0);

        Notification notification3 = shadowNotificationManager.getAllNotifications().get(0);
        assertNotification(notification3, "Alarm at 4:32 AM", "Ringing");
        assertNotificationActionCount(notification3, 2);
        assertNotificationAction(notification3, 0, "Dismiss", NotificationReceiver.ACTION_DISMISS);
        assertNotificationAction(notification3, 1, "Snooze", NotificationReceiver.ACTION_SNOOZE);

        // Check widget
        assertWidget(R.drawable.ic_alarm_white, "4:32 AM", null);

        // ----------------------------------------------------

        // Dismiss the 2nd alarm

        // Start activity
        startActivityCalendar();
        loadItemAtPosition(2);
        item.performLongClick();
        clickContextMenu(R.id.action_day_dismiss);
        refreshRecyclerView();

        // ----------------------------------------------------

        // Check calendar
        assertThat("The number of items", recyclerView.getChildCount(), is(HORIZON_DAYS + 2));
        assertCalendarItem(0, "2/1", "Mon", "Off", "", null, ""); // Today
        assertCalendarItem(1, "", "", "4:31 AM", "Passed", "", ""); // The 1st one-time alarm
        assertCalendarItem(2, "", "", "4:32 AM", "Passed", "", ""); // The 2nd one-time alarm
        assertCalendarItem(3, "2/2", "Tue", "Off", "", null, ""); // Tomorrow

        // Check system alarm
        assertSystemAlarmCount(0);
        assertSystemAlarmClockNone();

        // Check notification
        assertNotificationCount(1);

        Notification notification4 = shadowNotificationManager.getAllNotifications().get(0);
        assertNotification(notification4, "Alarm at 4:31 AM", "Alarm cancelled");
        assertNotificationActionCount(notification4, 0);

        // Check widget
        assertWidget(R.drawable.ic_alarm_off_white, "No alarm", null);
    }

    @Test
    public void t410_onRing_withTwoAlarmsAtTheSameTime() {
        t111_addTwoAlarmsAtTheSameTime_viaGlobalManager();

        // ----------------------------------------------------

        // Start the alarm

        // Shift clock
        shadowGlobalManager.setClock(new FixedClock(new GregorianCalendar(YEAR, MONTH, DAY, ONE_TIME_ALARM_HOUR, ONE_TIME_ALARM_MINUTE)));

        // Call the receiver
        List<OneTimeAlarm> oneTimeAlarms = globalManager.loadOneTimeAlarms();
        Collections.sort(oneTimeAlarms);

        Intent intent = new Intent();
        intent.setAction(SystemAlarm.ACTION_RING);
        intent.putExtra(GlobalManager.PERSIST_ALARM_TYPE, oneTimeAlarms.get(0).getClass().getSimpleName());
        intent.putExtra(GlobalManager.PERSIST_ALARM_ID, oneTimeAlarms.get(0).getPersistenceId());
        AlarmReceiver alarmReceiver = new AlarmReceiver();
        alarmReceiver.onReceive(context, intent);

        // Shift clock
        shadowGlobalManager.setClock(new FixedClock(new GregorianCalendar(YEAR, MONTH, DAY, ONE_TIME_ALARM_HOUR, ONE_TIME_ALARM_MINUTE, 10)));

        // ----------------------------------------------------

        // Check that ringing started
        Activity activity = Robolectric.setupActivity(Activity.class);
        ShadowActivity shadowActivity = Shadows.shadowOf(activity);

        Intent intentNext = shadowActivity.peekNextStartedActivity();
        Intent expectedIntentNext = new Intent(context, RingActivity.class);

        assertThat("Intent component", intentNext.getComponent(), is(expectedIntentNext.getComponent()));

        // Check calendar
        startActivityCalendar();

        assertThat("The number of items", recyclerView.getChildCount(), is(HORIZON_DAYS + 2));
        assertCalendarItem(0, "2/1", "Mon", "Off", "", null, ""); // Today
        assertCalendarItem(1, "", "", "4:30 AM", "Ringing", "One", "-10s"); // The one-time alarm
        assertCalendarItem(2, "", "", "4:30 AM", "Passed", "Two", ""); // The one-time alarm
        assertCalendarItem(3, "2/2", "Tue", "Off", "", null, ""); // Tomorrow

        // Check ring activity
        Calendar alarmTime = new GregorianCalendar(YEAR, MONTH, DAY, ONE_TIME_ALARM_HOUR, ONE_TIME_ALARM_MINUTE);
        startActivityRing(globalManager.loadOneTimeAlarms().get(0));

        assertThat("Date visibility", textDate.getVisibility(), is(View.VISIBLE));
        assertThat("Time visibility", textTime.getVisibility(), is(View.VISIBLE));
        assertThat("Alarm time visibility", textAlarmTime.getVisibility(), is(View.INVISIBLE));
        assertThat("Alarm name visibility", textOneTimeAlarmName.getVisibility(), is(View.VISIBLE));
        assertThat("Calendar visibility", textNextCalendar.getVisibility(), is(View.GONE));
        assertThat("Muted visibility", textMuted.getVisibility(), is(View.INVISIBLE));

        assertThat("Date", textDate.getText(), is("Monday, February 1"));
        assertThat("Time", textTime.getText(), is("4:30 AM"));
        assertThat("Alarm name", textOneTimeAlarmName.getText(), is("One"));

        // Check system alarm
        assertSystemAlarmCount(1);
        assertSystemAlarm(YEAR, MONTH, DAY + 1, 0, 0, SystemAlarm.ACTION_SET_SYSTEM_ALARM);
        assertSystemAlarmClockNone();

        // Check notification
        assertNotificationCount(2);

        Notification notification2 = shadowNotificationManager.getAllNotifications().get(1);
        assertNotification(notification2, "Alarm at 4:30 AM – Two", "Alarm cancelled");
        assertNotificationActionCount(notification2, 0);

        Notification notification = shadowNotificationManager.getAllNotifications().get(0);
        assertNotification(notification, "Alarm at 4:30 AM – One", "Ringing");
        assertNotificationActionCount(notification, 2);
        assertNotificationAction(notification, 0, "Dismiss", NotificationReceiver.ACTION_DISMISS);
        assertNotificationAction(notification, 1, "Snooze", NotificationReceiver.ACTION_SNOOZE);

        // Check widget
        assertWidget(R.drawable.ic_alarm_white, "4:30 AM", null);

        // ----------------------------------------------------

        // Dismiss the alarm

        // We are still in RingActivity
        dismissButton.performClick();

        startActivityCalendar();
        refreshRecyclerView();

        // ----------------------------------------------------

        // Check calendar
        assertThat("The number of items", recyclerView.getChildCount(), is(HORIZON_DAYS + 2));
        assertCalendarItem(0, "2/1", "Mon", "Off", "", null, ""); // Today
        assertCalendarItem(1, "", "", "4:30 AM", "Passed", "One", ""); // The 1st one-time alarm
        assertCalendarItem(2, "", "", "4:30 AM", "Passed", "Two", ""); // The 2nd one-time alarm
        assertCalendarItem(3, "2/2", "Tue", "Off", "", null, ""); // Tomorrow

        // Check system alarm
        assertSystemAlarmCount(0);
        assertSystemAlarmClockNone();

        // Check notification
        assertNotificationCount(1);

        Notification notification3 = shadowNotificationManager.getAllNotifications().get(0);
        assertNotification(notification3, "Alarm at 4:30 AM – Two", "Alarm cancelled");
        assertNotificationActionCount(notification3, 0);

        // Check widget
        assertWidget(R.drawable.ic_alarm_off_white, "No alarm", null);
    }

    private void setAlarm(Calendar datetime, String name) {
        OneTimeAlarm oneTimeAlarm = new OneTimeAlarm();
        oneTimeAlarm.setDate(datetime);
        oneTimeAlarm.setHour(datetime.get(Calendar.HOUR_OF_DAY));
        oneTimeAlarm.setMinute(datetime.get(Calendar.MINUTE));
        oneTimeAlarm.setName(name);

        Analytics analytics = new Analytics(Analytics.Channel.Test, Analytics.ChannelName.Calendar);

        globalManager.createOneTimeAlarm(oneTimeAlarm, analytics);
    }

}
