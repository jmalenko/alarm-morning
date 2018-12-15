package cz.jaro.alarmmorning.app;

import android.app.Notification;
import android.content.Intent;

import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.robolectric.annotation.Config;

import java.util.Calendar;
import java.util.GregorianCalendar;

import cz.jaro.alarmmorning.R;
import cz.jaro.alarmmorning.SystemAlarm;
import cz.jaro.alarmmorning.clock.FixedClock;
import cz.jaro.alarmmorning.receivers.AlarmReceiver;
import cz.jaro.alarmmorning.receivers.NotificationReceiver;
import cz.jaro.alarmmorning.shadows.ShadowGlobalManager;

import static cz.jaro.alarmmorning.GlobalManager.HORIZON_DAYS;
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

    final int ONE_TIME_ALARM_HOUR = HOUR + 3;
    final int ONE_TIME_ALARM_MINUTE = 30;

    private void shared_t105_addTwoAlarms_BothInNearPeriod() {
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
    public void t105a_addTwoAlarms_inNearPeriod_order12() {
        // Consume the alarm with action ACTION_SET_SYSTEM_ALARM
        consumeNextScheduledAlarm();

        // Add one-time alarms
        int oldCount = calendar_addOneTimeAlarm(0, HOUR + 1, 0, HOUR + 1, 2);
        calendar_addOneTimeAlarm(0, HOUR + 1, 0, HOUR + 1, 1);

        // Check calendar
        int newCount = recyclerView.getChildCount();
        assertThat("The number of items increased by one", newCount - oldCount, is(2));

        shared_t105_addTwoAlarms_BothInNearPeriod();
    }

    @Test
    public void t105b_addTwoAlarms_inNearPeriod_order21() {
        // Consume the alarm with action ACTION_SET_SYSTEM_ALARM
        consumeNextScheduledAlarm();

        // Add one-time alarms
        int oldCount = calendar_addOneTimeAlarm(0, HOUR + 1, 0, HOUR + 1, 1);
        calendar_addOneTimeAlarm(0, HOUR + 1, 0, HOUR + 1, 2);

        // Check calendar
        int newCount = recyclerView.getChildCount();
        assertThat("The number of items increased by one", newCount - oldCount, is(2));

        shared_t105_addTwoAlarms_BothInNearPeriod();
    }

    private void shared_t106_addTwoAlarms_afterNearPeriod() {
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
    public void t106a_addTwoAlarms_afterNearPeriod_order12() {
        // Consume the alarm with action ACTION_SET_SYSTEM_ALARM
        consumeNextScheduledAlarm();

        // Add one-time alarms
        int oldCount = calendar_addOneTimeAlarm(0, HOUR + 1, 0, ONE_TIME_ALARM_HOUR, ONE_TIME_ALARM_MINUTE + 1);
        calendar_addOneTimeAlarm(0, HOUR + 1, 0, ONE_TIME_ALARM_HOUR, ONE_TIME_ALARM_MINUTE + 2);

        // Check calendar
        int newCount = recyclerView.getChildCount();
        assertThat("The number of items increased by one", newCount - oldCount, is(2));

        shared_t106_addTwoAlarms_afterNearPeriod();
    }

    @Test
    public void t106b_addTwoAlarms_afterNearFuture_order21() {
        // Consume the alarm with action ACTION_SET_SYSTEM_ALARM
        consumeNextScheduledAlarm();

        // Add one-time alarms
        int oldCount = calendar_addOneTimeAlarm(0, HOUR + 1, 0, ONE_TIME_ALARM_HOUR, ONE_TIME_ALARM_MINUTE + 2);
        calendar_addOneTimeAlarm(0, HOUR + 1, 0, ONE_TIME_ALARM_HOUR, ONE_TIME_ALARM_MINUTE + 1);

        // Check calendar
        int newCount = recyclerView.getChildCount();
        assertThat("The number of items increased by one", newCount - oldCount, is(2));

        shared_t106_addTwoAlarms_afterNearPeriod();
    }

    @Test
    private void shared_t211_dismiss_ofDistantTwoAlarms_noneAlarm() {
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
    public void t211a_dismiss_ofDistantTwoAlarms_order12() {
        t106a_addTwoAlarms_afterNearPeriod_order12();

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
        shared_t211_dismiss_ofDistantTwoAlarms_noneAlarm();
    }

    // There are two alarms at T+1 and T+2; dismiss the T+2 alarm, then dismiss T+1 alarm.
    @Test
    public void t211b_dismiss_ofDistantTwoAlarms_order21() {
        t106a_addTwoAlarms_afterNearPeriod_order12();

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
        shared_t211_dismiss_ofDistantTwoAlarms_noneAlarm();
    }

    private void prepareCreate() {
        // Consume the alarm with action ACTION_SET_SYSTEM_ALARM
        consumeNextScheduledAlarm();

        // Save day
        setAlarmToToday();
    }

    private void prepareUntilNear() {
        prepareCreate();

        // Consume the alarm with action ACTION_RING_IN_NEAR_FUTURE
        consumeNextScheduledAlarm();
        // Consume the alarm involving System Alarm Clock
        consumeNextScheduledAlarm();

        // Shift clock
        shadowGlobalManager.setClock(new FixedClock(new GregorianCalendar(YEAR, MONTH, DAY, ONE_TIME_ALARM_HOUR - 2, ONE_TIME_ALARM_MINUTE)));

        // Call the receiver
        Intent intent = new Intent();
        intent.setAction(SystemAlarm.ACTION_RING_IN_NEAR_FUTURE);
        AlarmReceiver alarmReceiver = new AlarmReceiver();
        alarmReceiver.onReceive(context, intent);

        // Shift clock
        shadowGlobalManager.setClock(new FixedClock(new GregorianCalendar(YEAR, MONTH, DAY, ONE_TIME_ALARM_HOUR - 2, ONE_TIME_ALARM_MINUTE, 10)));
    }

    private void prepareUntilRing() {
        prepareUntilNear();

        // Consume the alarm with action ACTION_RING
        consumeNextScheduledAlarm();

        // Shift clock
        shadowGlobalManager.setClock(new FixedClock(new GregorianCalendar(YEAR, MONTH, DAY, ONE_TIME_ALARM_HOUR, ONE_TIME_ALARM_MINUTE)));

        // Call the receiver
        Intent intent = new Intent();
        intent.setAction(SystemAlarm.ACTION_RING);
        AlarmReceiver alarmReceiver2 = new AlarmReceiver();
        alarmReceiver2.onReceive(context, intent);

        // Shift clock
        shadowGlobalManager.setClock(new FixedClock(new GregorianCalendar(YEAR, MONTH, DAY, ONE_TIME_ALARM_HOUR, ONE_TIME_ALARM_MINUTE, 10)));
    }

    private void prepareUntilSnooze() {
        prepareUntilRing();

        // Consume the alarm with action ACTION_SET_SYSTEM_ALARM
        consumeNextScheduledAlarm();

        // Start ring activity
        Calendar alarmTime = new GregorianCalendar(YEAR, MONTH, DAY, ONE_TIME_ALARM_HOUR, ONE_TIME_ALARM_MINUTE);
        startActivityRing(alarmTime);

        snoozeButton.performClick();

        // Shift clock
        shadowGlobalManager.setClock(new FixedClock(new GregorianCalendar(YEAR, MONTH, DAY, ONE_TIME_ALARM_HOUR, ONE_TIME_ALARM_MINUTE, 20)));
    }

    private void setAlarmToToday() {
        Calendar date = new GregorianCalendar(YEAR, MONTH, DAY, ONE_TIME_ALARM_HOUR, ONE_TIME_ALARM_MINUTE);
        setAlarm(date);
    }

    private void setAlarmToTomorrow() {
        Calendar date = new GregorianCalendar(YEAR, MONTH, DAY + 1, ONE_TIME_ALARM_HOUR + 1, ONE_TIME_ALARM_MINUTE + 1);
        setAlarm(date);
    }

}
