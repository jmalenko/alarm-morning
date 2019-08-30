package cz.jaro.alarmmorning.app;

import android.app.Activity;
import android.app.Notification;
import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;

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
import cz.jaro.alarmmorning.model.AppAlarm;
import cz.jaro.alarmmorning.model.OneTimeAlarm;
import cz.jaro.alarmmorning.receivers.AlarmReceiver;
import cz.jaro.alarmmorning.receivers.NotificationReceiver;
import cz.jaro.alarmmorning.shadows.ShadowGlobalManager;

import static cz.jaro.alarmmorning.GlobalManager.HORIZON_DAYS;
import static cz.jaro.alarmmorning.model.DayTest.DAY;
import static cz.jaro.alarmmorning.model.DayTest.HOUR;
import static cz.jaro.alarmmorning.model.DayTest.MINUTE;
import static cz.jaro.alarmmorning.model.DayTest.MONTH;
import static cz.jaro.alarmmorning.model.DayTest.YEAR;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

/**
 * Tests of alarm management in UI.
 * <p>
 * One one-time alarm is used.
 */
@Config(shadows = {ShadowGlobalManager.class})
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class CalendarWithOneTimeAlarmTest extends AlarmMorningAppTest {
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

    public static final int ONE_TIME_ALARM_HOUR = HOUR + 3;
    public static final int ONE_TIME_ALARM_MINUTE = 30;

    @Test
    public void t100_addAlarm() {
        // Consume the alarm with action ACTION_SET_SYSTEM_ALARM
        consumeNextScheduledAlarm();

        // Add one-time alarm
        int oldCount = calendar_addOneTimeAlarm(0, HOUR + 1, 0, ONE_TIME_ALARM_HOUR, ONE_TIME_ALARM_MINUTE);

        // Check calendar
        int newCount = recyclerView.getChildCount();
        assertThat("The number of items increased by one", newCount - oldCount, is(1));

        assertCalendarItem(0, "2/1", "Mon", "Off", "", null, ""); // Today
        assertCalendarItem(1, "", "", "4:30 AM", "", "", "3h 30m"); // The one-time alarm
        assertCalendarItem(2, "2/2", "Tue", "Off", "", null, ""); // Tomorrow

        // Check system alarm
        assertSystemAlarmCount(2);
        assertSystemAlarm(YEAR, MONTH, DAY, ONE_TIME_ALARM_HOUR - 2 /* PREF_NEAR_FUTURE_TIME_DEFAULT / 60 */, ONE_TIME_ALARM_MINUTE, SystemAlarm.ACTION_RING_IN_NEAR_FUTURE); // Check system alarm
        assertSystemAlarmClock(YEAR, MONTH, DAY, ONE_TIME_ALARM_HOUR, ONE_TIME_ALARM_MINUTE); // Check system alarm clock

        // Check notification
        assertNotificationCount(0);

        // Check widget
        assertWidget(R.drawable.ic_alarm_white, "4:30 AM", null);
    }

    @Test
    public void t101_addAlarmToPast() {
        // Consume the alarm with action ACTION_SET_SYSTEM_ALARM
        consumeNextScheduledAlarm();

        // Add one-time alarm
        Calendar past = globalManager.clock().now();
        past.add(Calendar.MINUTE, -1);
        int hourOfDay = past.get(Calendar.HOUR_OF_DAY);
        int minuteOfHour = past.get(Calendar.MINUTE);

        int oldCount = calendar_addOneTimeAlarm(0, HOUR + 1, 0, hourOfDay, minuteOfHour);

        // Check calendar
        int newCount = recyclerView.getChildCount();
        assertThat("The number of items increased by one", newCount - oldCount, is(1));

        assertCalendarItem(0, "2/1", "Mon", "Off", "", null, ""); // Today
        assertCalendarItem(1, "2/2", "Tue", "Off", "", null, ""); // Tomorrow
        assertCalendarItem(2, "", "", "12:59 AM", "", "", "23h 59m"); // The one-time alarm
        assertCalendarItem(3, "2/3", "Wed", "Off", "", null, ""); // The day after tomorrow

        // Check system alarm
        assertSystemAlarmCount(2);
        assertSystemAlarm(YEAR, MONTH, DAY, hourOfDay - 2 /* PREF_NEAR_FUTURE_TIME_DEFAULT / 60 */ + 24, minuteOfHour, SystemAlarm.ACTION_RING_IN_NEAR_FUTURE); // Check system alarm
        assertSystemAlarmClock(YEAR, MONTH, DAY + 1, hourOfDay, minuteOfHour); // Check system alarm clock

        // Check notification
        assertNotificationCount(0);

        // Check widget
        assertWidget(R.drawable.ic_alarm_white, "12:59 AM", "Tomorrow");
    }

    @Test
    public void t102_addAlarmWithZeroAdvancePeriod() {
        // Set the preference to zero
        CalendarWithDayAlarmTest.setNearFuturePeriodPreferenceToZero(context);

        // Consume the alarm with action ACTION_SET_SYSTEM_ALARM
        consumeNextScheduledAlarm();

        // Add one-time alarm
        int oldCount = calendar_addOneTimeAlarm(0, HOUR + 1, 0, ONE_TIME_ALARM_HOUR, ONE_TIME_ALARM_MINUTE);

        // Check calendar
        int newCount = recyclerView.getChildCount();
        assertThat("The number of items increased by one", newCount - oldCount, is(1));

        assertCalendarItem(0, "2/1", "Mon", "Off", "", null, ""); // Today
        assertCalendarItem(1, "", "", "4:30 AM", "", "", "3h 30m"); // The one-time alarm
        assertCalendarItem(2, "2/2", "Tue", "Off", "", null, ""); // Tomorrow

        // Check system alarm
        assertSystemAlarmCount(2);
        assertSystemAlarm(YEAR, MONTH, DAY, ONE_TIME_ALARM_HOUR, ONE_TIME_ALARM_MINUTE, SystemAlarm.ACTION_RING);
        assertSystemAlarmClock(YEAR, MONTH, DAY, ONE_TIME_ALARM_HOUR, ONE_TIME_ALARM_MINUTE);

        // Check notification
        assertNotificationCount(0);

        // Check widget
        assertWidget(R.drawable.ic_alarm_white, "4:30 AM", null);
    }

    @Test
    public void t103_addAlarmToTomorrow() {
        // Consume the alarm with action ACTION_SET_SYSTEM_ALARM
        consumeNextScheduledAlarm();

        // Add one-time alarm
        int oldCount = calendar_addOneTimeAlarm(1, HOUR, 0, ONE_TIME_ALARM_HOUR, ONE_TIME_ALARM_MINUTE);

        // Check calendar
        int newCount = recyclerView.getChildCount();
        assertThat("The number of items increased by one", newCount - oldCount, is(1));

        assertCalendarItem(0, "2/1", "Mon", "Off", "", null, ""); // Today
        assertCalendarItem(1, "2/2", "Tue", "Off", "", null, ""); // Tomorrow
        assertCalendarItem(2, "", "", "4:30 AM", "", "", "1d 3h"); // The one-time alarm
        assertCalendarItem(3, "2/3", "Wed", "Off", "", null, ""); // The day after tomorrow

        // Check system alarm
        assertSystemAlarmCount(2);
        assertSystemAlarm(YEAR, MONTH, DAY + 1, ONE_TIME_ALARM_HOUR - 2, ONE_TIME_ALARM_MINUTE, SystemAlarm.ACTION_RING_IN_NEAR_FUTURE);
        assertSystemAlarmClock(YEAR, MONTH, DAY + 1, ONE_TIME_ALARM_HOUR, ONE_TIME_ALARM_MINUTE);

        // Check notification
        assertNotificationCount(0);

        // Check widget
        assertWidget(R.drawable.ic_alarm_white, "4:30 AM", "Tomorrow");
    }

    @Test
    public void t210_dismiss_ofDistantAlarm() {
        prepareCreate();

        // Consume the alarm with action ACTION_RING_IN_NEAR_FUTURE
        consumeNextScheduledAlarm();

        // Start Activity
        startActivityCalendar();
        int oldCount = recyclerView.getChildCount();

        // Context menu
        loadItemAtPosition(1);

        item.performLongClick();

        clickContextMenu(R.id.action_day_dismiss);

        refreshRecyclerView();

        // Check calendar
        int newCount = recyclerView.getChildCount();
        assertThat("The change of number of items", newCount - oldCount, is(0));

        assertCalendarItem(0, "2/1", "Mon", "Off", "", null, ""); // Today
        assertCalendarItem(1, "", "", "4:30 AM", "Dismissed before ringing", "", ""); // The one-time alarm
        assertCalendarItem(2, "2/2", "Tue", "Off", "", null, ""); // Tomorrow

        // Check system alarm
        assertSystemAlarmCount(1);
        assertSystemAlarm(YEAR, MONTH, DAY, ONE_TIME_ALARM_HOUR, ONE_TIME_ALARM_MINUTE, SystemAlarm.ACTION_ALARM_TIME_OF_EARLY_DISMISSED_ALARM); // Check system alarm
        assertSystemAlarmClockNone();

        // Check notification
        assertNotificationCount(0);

        // Check widget
        assertWidget(R.drawable.ic_alarm_off_white, "No alarm", null);
    }

    @Test
    public void t220_disable_ofDistantAlarm() {
        prepareCreate();

        // Consume the alarm with action ACTION_RING_IN_NEAR_FUTURE
        consumeNextScheduledAlarm();

        // Start Activity
        startActivityCalendar();

        int oldCount = recyclerView.getChildCount();

        // Context menu
        loadItemAtPosition(1);

        item.performLongClick();

        clickContextMenu(R.id.action_day_disable);

        refreshRecyclerView();

        // Check calendar
        int newCount = recyclerView.getChildCount();
        assertThat("The change of number of items", newCount - oldCount, is(-1));

        assertCalendarItem(0, "2/1", "Mon", "Off", "", null, ""); // Today
        assertCalendarItem(1, "2/2", "Tue", "Off", "", null, ""); // Tomorrow

        // Check system alarm
        assertSystemAlarm(YEAR, MONTH, DAY + 1, 0, 0, SystemAlarm.ACTION_SET_SYSTEM_ALARM);
        assertSystemAlarmClockNone();

        // Check notification
        assertNotificationCount(0);

        // Check widget
        assertWidget(R.drawable.ic_alarm_off_white, "No alarm", null);
    }

    // Set alarm to an alarm time of a previously dismissed alarm
    @Test
    public void t221_addAlarm_AtTheTimeOfAPreviouslyDismissedAlarm() {
        t220_disable_ofDistantAlarm();

        // Save day
        setAlarmToToday();

        refreshRecyclerView();

        // Check calendar
        assertThat("The number of items", recyclerView.getChildCount(), is(HORIZON_DAYS + 1));

        assertCalendarItem(0, "2/1", "Mon", "Off", "", null, ""); // Today
        assertCalendarItem(1, "", "", "4:30 AM", "", "", "3h 30m"); // The one-time alarm
        assertCalendarItem(2, "2/2", "Tue", "Off", "", null, ""); // Tomorrow

        // Check system alarm
        assertSystemAlarmCount(2);
        assertSystemAlarm(YEAR, MONTH, DAY, ONE_TIME_ALARM_HOUR - 2, ONE_TIME_ALARM_MINUTE, SystemAlarm.ACTION_RING_IN_NEAR_FUTURE);
        assertSystemAlarmClock(YEAR, MONTH, DAY, ONE_TIME_ALARM_HOUR, ONE_TIME_ALARM_MINUTE);

        // Check notification
        assertNotificationCount(0);

        // Check widget
        assertWidget(R.drawable.ic_alarm_white, "4:30 AM", null);
    }

    private int shared_t240_setTime_ofDistantAlarm(int hour, int minute) {
        prepareCreate();

        // Consume the alarm with action ACTION_RING_IN_NEAR_FUTURE
        consumeNextScheduledAlarm();

        return calendar_setTime(1, ONE_TIME_ALARM_HOUR, ONE_TIME_ALARM_MINUTE, hour, minute);
    }

    @Test
    public void t240_setTime_ofDistantAlarm_inPast() {
        int oldCount = shared_t240_setTime_ofDistantAlarm(HOUR, MINUTE - 1);

        // Check calendar
        int newCount = recyclerView.getChildCount();
        assertThat("The number of items did not change", newCount - oldCount, is(0));

        assertCalendarItem(0, "2/1", "Mon", "Off", "", null, ""); // Today
        assertCalendarItem(1, "", "", "12:59 AM", "Passed", "", ""); // The one-time alarm
        assertCalendarItem(2, "2/2", "Tue", "Off", "", null, ""); // Tomorrow

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
    public void t241_setTime_ofDistantAlarm_inNearPeriod() {
        int oldCount = shared_t240_setTime_ofDistantAlarm(HOUR + 1, MINUTE + 1);

        // Check calendar
        int newCount = recyclerView.getChildCount();
        assertThat("The number of items did not change", newCount - oldCount, is(0));

        assertCalendarItem(0, "2/1", "Mon", "Off", "", null, ""); // Today
        assertCalendarItem(1, "", "", "2:01 AM", "", "", "1h 1m"); // The one-time alarm
        assertCalendarItem(2, "2/2", "Tue", "Off", "", null, ""); // Tomorrow

        // Check system alarm
        assertSystemAlarmCount(2);
        assertSystemAlarm(YEAR, MONTH, DAY, HOUR + 1, MINUTE + 1, SystemAlarm.ACTION_RING);
        assertSystemAlarmClock(YEAR, MONTH, DAY, HOUR + 1, MINUTE + 1);

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
    public void t242_setTime_ofDistantAlarm_afterNearPeriod() {
        int oldCount = shared_t240_setTime_ofDistantAlarm(ONE_TIME_ALARM_HOUR + 1, ONE_TIME_ALARM_MINUTE + 1);

        // Check calendar
        int newCount = recyclerView.getChildCount();
        assertThat("The number of items did not change", newCount - oldCount, is(0));

        assertCalendarItem(0, "2/1", "Mon", "Off", "", null, ""); // Today
        assertCalendarItem(1, "", "", "5:31 AM", "", "", "4h 31m"); // The one-time alarm
        assertCalendarItem(2, "2/2", "Tue", "Off", "", null, ""); // Tomorrow

        // Check system alarm
        assertSystemAlarmCount(2);
        assertSystemAlarm(YEAR, MONTH, DAY, ONE_TIME_ALARM_HOUR + 1 - 2, ONE_TIME_ALARM_MINUTE + 1, SystemAlarm.ACTION_RING_IN_NEAR_FUTURE);
        assertSystemAlarmClock(YEAR, MONTH, DAY, ONE_TIME_ALARM_HOUR + 1, ONE_TIME_ALARM_MINUTE + 1);

        // Check notification
        assertNotificationCount(0);

        // Check widget
        assertWidget(R.drawable.ic_alarm_white, "5:31 AM", null);
    }

    private int shared_t250_setDate_ofDistantAlarm(int year, int month, int day) {
        prepareCreate();

        // Consume the alarm with action ACTION_RING_IN_NEAR_FUTURE
        consumeNextScheduledAlarm();

        // Set date
        return calendar_setDate(1, YEAR, MONTH, DAY, year, month, day);
    }

    @Test
    public void t250_setDate_ofDistantAlarm_inPast() {
        int oldCount = shared_t250_setDate_ofDistantAlarm(YEAR, MONTH, DAY - 1);

        // Check calendar
        int newCount = recyclerView.getChildCount();
        assertThat("The number of items decreased by one", newCount - oldCount, is(-1));

        assertCalendarItem(0, "2/1", "Mon", "Off", "", null, ""); // Today
        assertCalendarItem(1, "2/2", "Tue", "Off", "", null, ""); // Tomorrow

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
    public void t251_setDate_ofDistantAlarm_inFuture() {
        int oldCount = shared_t250_setDate_ofDistantAlarm(YEAR, MONTH, DAY + 1);

        // Check calendar
        int newCount = recyclerView.getChildCount();
        assertThat("The number of items did not change", newCount - oldCount, is(0));

        assertCalendarItem(0, "2/1", "Mon", "Off", "", null, ""); // Today
        assertCalendarItem(1, "2/2", "Tue", "Off", "", null, ""); // Tomorrow
        assertCalendarItem(2, "", "", "4:30 AM", "", "", "1d 3h"); // The one-time alarm
        assertCalendarItem(3, "2/3", "Wed", "Off", "", null, ""); // The day after tomorrow

        // Check system alarm
        assertSystemAlarmCount(2);
        assertSystemAlarm(YEAR, MONTH, DAY + 1, ONE_TIME_ALARM_HOUR - 2, ONE_TIME_ALARM_MINUTE, SystemAlarm.ACTION_RING_IN_NEAR_FUTURE);
        assertSystemAlarmClock(YEAR, MONTH, DAY + 1, ONE_TIME_ALARM_HOUR, ONE_TIME_ALARM_MINUTE);

        // Check notification
        assertNotificationCount(0);

        // Check widget
        assertWidget(R.drawable.ic_alarm_white, "4:30 AM", "Tomorrow");
    }

    @Test
    public void t260_setName_ofDistantAlarm() {
        prepareCreate();

        // Consume the alarm with action ACTION_RING_IN_NEAR_FUTURE
        consumeNextScheduledAlarm();

        // Start activity
        startActivityCalendar();

        int oldCount = recyclerView.getChildCount();

        // Change name
        loadItemAtPosition(1);

        textName.requestFocus();

        // Show soft keyboard
        final InputMethodManager inputMethodManager = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        inputMethodManager.showSoftInput(textName, InputMethodManager.SHOW_IMPLICIT);

        textName.setText("Flowers");

        // Click Done in the soft keyboard
        textName.onEditorAction(EditorInfo.IME_ACTION_DONE);

        refreshRecyclerView();

        // Check calendar
        int newCount = recyclerView.getChildCount();
        assertThat("The number of items did not change", newCount - oldCount, is(0));

        assertCalendarItem(0, "2/1", "Mon", "Off", "", null, ""); // Today
        assertCalendarItem(1, "", "", "4:30 AM", "", "Flowers", "3h 30m"); // The one-time alarm
        assertCalendarItem(2, "2/2", "Tue", "Off", "", null, ""); // Tomorrow

        // Check system alarm
        assertSystemAlarmCount(1);
        assertSystemAlarmClock(YEAR, MONTH, DAY, ONE_TIME_ALARM_HOUR, ONE_TIME_ALARM_MINUTE);
        assertSystemAlarmClockNone();

        // Check notification
        assertNotificationCount(0);

        // Check widget
        assertWidget(R.drawable.ic_alarm_white, "4:30 AM", null);
    }

    @Test
    public void t300_onNear() {
        prepareUntilNear();

        // Check calendar
        // Nothing to check in the calendar

        // Check system alarm
        assertSystemAlarmCount(1);
        assertSystemAlarm(YEAR, MONTH, DAY, ONE_TIME_ALARM_HOUR, ONE_TIME_ALARM_MINUTE, SystemAlarm.ACTION_RING);
        assertSystemAlarmClockNone();

        // Check notification
        assertNotificationCount(1);

        Notification notification = shadowNotificationManager.getAllNotifications().get(0);
        assertNotification(notification, "Alarm at 4:30 AM", "Touch to view all alarms");
        assertNotificationActionCount(notification, 1);
        assertNotificationAction(notification, 0, "Dismiss", NotificationReceiver.ACTION_DISMISS_BEFORE_RINGING);

        // Check widget
        assertWidget(R.drawable.ic_alarm_white, "4:30 AM", null);
    }

    @Test
    public void t301_onNear_namedAlarm() {
        t260_setName_ofDistantAlarm();

        // Shift clock
        shadowGlobalManager.setClock(new FixedClock(new GregorianCalendar(YEAR, MONTH, DAY, ONE_TIME_ALARM_HOUR - 2, ONE_TIME_ALARM_MINUTE)));

        // Call the receiver
        Intent intent = new Intent();
        intent.setAction(SystemAlarm.ACTION_RING_IN_NEAR_FUTURE);
        AppAlarm appAlarm = globalManager.loadOneTimeAlarms().get(0);
        intent.putExtra(GlobalManager.PERSIST_ALARM_TYPE, appAlarm.getClass().getSimpleName());
        intent.putExtra(GlobalManager.PERSIST_ALARM_ID, appAlarm.getPersistenceId());
        AlarmReceiver alarmReceiver = new AlarmReceiver();
        alarmReceiver.onReceive(context, intent);

        // Shift clock
        shadowGlobalManager.setClock(new FixedClock(new GregorianCalendar(YEAR, MONTH, DAY, ONE_TIME_ALARM_HOUR - 2, ONE_TIME_ALARM_MINUTE, 10)));

        // Check calendar
        // Nothing to check in the calendar

        // Check system alarm
        assertSystemAlarmCount(1);
        assertSystemAlarm(YEAR, MONTH, DAY, ONE_TIME_ALARM_HOUR, ONE_TIME_ALARM_MINUTE, SystemAlarm.ACTION_RING);
        assertSystemAlarmClockNone();

        // Check notification
        assertNotificationCount(1);

        Notification notification = shadowNotificationManager.getAllNotifications().get(0);
        assertNotification(notification, "Alarm at 4:30 AM – Flowers", "Touch to view all alarms");
        assertNotificationActionCount(notification, 1);
        assertNotificationAction(notification, 0, "Dismiss", NotificationReceiver.ACTION_DISMISS_BEFORE_RINGING);

        // Check widget
        assertWidget(R.drawable.ic_alarm_white, "4:30 AM", null);
    }

    @Test
    public void t310_dismiss_whileInNearPeriod() {
        prepareUntilNear();

        // Consume the alarm with action ACTION_RING
        consumeNextScheduledAlarm();

        // Start activity
        startActivityCalendar();

        int oldCount = recyclerView.getChildCount();

        // Context menu
        loadItemAtPosition(1);

        item.performLongClick();

        clickContextMenu(R.id.action_day_dismiss);

        refreshRecyclerView();

        // Check calendar
        int newCount = recyclerView.getChildCount();
        assertThat("The number of items did not change", newCount - oldCount, is(0));

        assertCalendarItem(0, "2/1", "Mon", "Off", "", null, ""); // Today
        assertCalendarItem(1, "", "", "4:30 AM", "Dismissed before ringing", "", ""); // The one-time alarm
        assertCalendarItem(2, "2/2", "Tue", "Off", "", null, ""); // Tomorrow

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
    public void t320_disable_whileInNearPeriod() {
        prepareUntilNear();

        // Consume the alarm with action ACTION_RING
        consumeNextScheduledAlarm();

        // Start activity
        startActivityCalendar();

        int oldCount = recyclerView.getChildCount();

        // Context menu
        loadItemAtPosition(1);

        item.performLongClick();

        clickContextMenu(R.id.action_day_disable);

        refreshRecyclerView();

        // Check calendar
        int newCount = recyclerView.getChildCount();
        assertThat("The change of number of items", newCount - oldCount, is(-1));

        assertCalendarItem(0, "2/1", "Mon", "Off", "", null, ""); // Today
        assertCalendarItem(1, "2/2", "Tue", "Off", "", null, ""); // Tomorrow

        // Check system alarm
        assertSystemAlarm(YEAR, MONTH, DAY + 1, 0, 0, SystemAlarm.ACTION_SET_SYSTEM_ALARM);

        // Check system alarm clock
        assertSystemAlarmClockNone();

        // Check notification
        assertNotificationCount(0);

        // Check widget
        assertWidget(R.drawable.ic_alarm_off_white, "No alarm", null);
    }

    private int shared_t340_setTime_whileInNearPeriod(int hour, int minute) {
        prepareUntilNear();

        // Consume the alarm with action ACTION_RING
        consumeNextScheduledAlarm();

        return calendar_setTime(1, ONE_TIME_ALARM_HOUR, ONE_TIME_ALARM_MINUTE, hour, minute);
    }

    @Test
    public void t340_setTime_whileInNearPeriod_inPast() {
        int oldCount = shared_t340_setTime_whileInNearPeriod(ONE_TIME_ALARM_HOUR - 2, ONE_TIME_ALARM_MINUTE - 1);

        // Check calendar
        int newCount = recyclerView.getChildCount();
        assertThat("The number of items did not change", newCount - oldCount, is(0));

        assertCalendarItem(0, "2/1", "Mon", "Off", "", null, ""); // Today
        assertCalendarItem(1, "", "", "2:29 AM", "Passed", "", ""); // The one-time alarm
        assertCalendarItem(2, "2/2", "Tue", "Off", "", null, ""); // Tomorrow

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
    public void t341_setTime_whileInNearPeriod_inNearPeriod() {
        int oldCount = shared_t340_setTime_whileInNearPeriod(ONE_TIME_ALARM_HOUR - 1, ONE_TIME_ALARM_MINUTE - 1);

        // Check calendar
        int newCount = recyclerView.getChildCount();
        assertThat("The number of items did not change", newCount - oldCount, is(0));

        assertCalendarItem(0, "2/1", "Mon", "Off", "", null, ""); // Today
        assertCalendarItem(1, "", "", "3:29 AM", "", "", "58m 50s"); // The one-time alarm
        assertCalendarItem(2, "2/2", "Tue", "Off", "", null, ""); // Tomorrow

        // Check system alarm
        assertSystemAlarmCount(2);
        assertSystemAlarm(YEAR, MONTH, DAY, ONE_TIME_ALARM_HOUR - 1, ONE_TIME_ALARM_MINUTE - 1, SystemAlarm.ACTION_RING);
        assertSystemAlarmClock(YEAR, MONTH, DAY, ONE_TIME_ALARM_HOUR - 1, ONE_TIME_ALARM_MINUTE - 1);

        // Check notification
        assertNotificationCount(1);

        Notification notification = shadowNotificationManager.getAllNotifications().get(0);
        assertNotification(notification, "Alarm at 3:29 AM", "Touch to view all alarms");
        assertNotificationActionCount(notification, 1);
        assertNotificationAction(notification, 0, "Dismiss", NotificationReceiver.ACTION_DISMISS_BEFORE_RINGING);

        // Check widget
        assertWidget(R.drawable.ic_alarm_white, "3:29 AM", null);
    }

    @Test
    public void t342_setTime_whileInNearPeriod_afterNearPeriod() {
        int oldCount = shared_t340_setTime_whileInNearPeriod(ONE_TIME_ALARM_HOUR + 1, ONE_TIME_ALARM_MINUTE + 1);

        // Check calendar
        int newCount = recyclerView.getChildCount();
        assertThat("The number of items did not change", newCount - oldCount, is(0));

        assertCalendarItem(0, "2/1", "Mon", "Off", "", null, ""); // Today
        assertCalendarItem(1, "", "", "5:31 AM", "", "", "3h 0m"); // The one-time alarm
        assertCalendarItem(2, "2/2", "Tue", "Off", "", null, ""); // Tomorrow

        // Check system alarm
        assertSystemAlarmCount(2);
        assertSystemAlarm(YEAR, MONTH, DAY, ONE_TIME_ALARM_HOUR + 1 - 2, ONE_TIME_ALARM_MINUTE + 1, SystemAlarm.ACTION_RING_IN_NEAR_FUTURE);
        assertSystemAlarmClock(YEAR, MONTH, DAY, ONE_TIME_ALARM_HOUR + 1, ONE_TIME_ALARM_MINUTE + 1);

        // Check notification
        assertNotificationCount(0);

        // Check widget
        assertWidget(R.drawable.ic_alarm_white, "5:31 AM", null);
    }

    private int shared_t350_setDate_whileInNearPeriod(int year, int month, int day) {
        prepareUntilNear();

        // Consume the alarm with action ACTION_RING
        consumeNextScheduledAlarm();

        // Set date
        return calendar_setDate(1, YEAR, MONTH, DAY, year, month, day);
    }

    @Test
    public void t350_setDate_whileInNearPeriod_inPast() {
        int oldCount = shared_t350_setDate_whileInNearPeriod(YEAR, MONTH, DAY - 1);

        // Check calendar
        int newCount = recyclerView.getChildCount();
        assertThat("The number of items decreased by one", newCount - oldCount, is(-1));

        assertCalendarItem(0, "2/1", "Mon", "Off", "", null, ""); // Today
        assertCalendarItem(1, "2/2", "Tue", "Off", "", null, ""); // Tomorrow

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
    public void t351_setDate_whileInNearPeriod_inFuture() {
        int oldCount = shared_t350_setDate_whileInNearPeriod(YEAR, MONTH, DAY + 1);

        // Check calendar
        int newCount = recyclerView.getChildCount();
        assertThat("The number of items did not change", newCount - oldCount, is(0));

        assertCalendarItem(0, "2/1", "Mon", "Off", "", null, ""); // Today
        assertCalendarItem(1, "2/2", "Tue", "Off", "", null, ""); // Tomorrow
        assertCalendarItem(2, "", "", "4:30 AM", "", "", "1d 1h"); // The one-time alarm
        assertCalendarItem(3, "2/3", "Wed", "Off", "", null, ""); // The day after tomorrow

        // Check system alarm
        assertSystemAlarmCount(2);
        assertSystemAlarm(YEAR, MONTH, DAY + 1, ONE_TIME_ALARM_HOUR - 2, ONE_TIME_ALARM_MINUTE, SystemAlarm.ACTION_RING_IN_NEAR_FUTURE);
        assertSystemAlarmClock(YEAR, MONTH, DAY + 1, ONE_TIME_ALARM_HOUR, ONE_TIME_ALARM_MINUTE);

        // Check notification
        assertNotificationCount(0);

        // Check widget
        assertWidget(R.drawable.ic_alarm_white, "4:30 AM", "Tomorrow");
    }

    @Test
    public void t360_setName_whileInNearPeriod() {
        prepareUntilNear();

        // Consume the alarm with action ACTION_RING
        consumeNextScheduledAlarm();

        // Start activity
        startActivityCalendar();
        int oldCount = recyclerView.getChildCount();

        // Change name
        loadItemAtPosition(1);

        textName.requestFocus();

        // Show soft keyboard
        final InputMethodManager inputMethodManager = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        inputMethodManager.showSoftInput(textName, InputMethodManager.SHOW_IMPLICIT);

        textName.setText("Flowers");

        // Click Done in the soft keyboard
        textName.onEditorAction(EditorInfo.IME_ACTION_DONE);

        refreshRecyclerView();

        // Check calendar
        int newCount = recyclerView.getChildCount();
        assertThat("The number of items did not change", newCount - oldCount, is(0));

        assertCalendarItem(0, "2/1", "Mon", "Off", "", null, ""); // Today
        assertCalendarItem(1, "", "", "4:30 AM", "", "Flowers", "1h 59m"); // The one-time alarm
        assertCalendarItem(2, "2/2", "Tue", "Off", "", null, ""); // Tomorrow

        // Check system alarm
        assertSystemAlarmCount(0);
        assertSystemAlarmClockNone();

        // Check notification
        assertNotificationCount(1);

        Notification notification = shadowNotificationManager.getAllNotifications().get(0);
        assertNotification(notification, "Alarm at 4:30 AM – Flowers", "Touch to view all alarms");
        assertNotificationActionCount(notification, 1);
        assertNotificationAction(notification, 0, "Dismiss", NotificationReceiver.ACTION_DISMISS_BEFORE_RINGING);

        // Check widget
        assertWidget(R.drawable.ic_alarm_white, "4:30 AM", null);
    }

    @Test
    public void t400_onRing() {
        prepareUntilRing();

        // Check that ringing started
        Activity activity = Robolectric.setupActivity(Activity.class);
        ShadowActivity shadowActivity = Shadows.shadowOf(activity);

        Intent intentNext = shadowActivity.peekNextStartedActivity();
        Intent expectedIntentNext = new Intent(context, RingActivity.class);

        assertThat("Intent component", intentNext.getComponent(), is(expectedIntentNext.getComponent()));

        // Check system alarm
        assertSystemAlarmCount(1);
        assertSystemAlarm(YEAR, MONTH, DAY + 1, 0, 0, SystemAlarm.ACTION_SET_SYSTEM_ALARM);
        assertSystemAlarmClockNone();

        // Check notification
        assertNotificationCount(1);

        Notification notification = shadowNotificationManager.getAllNotifications().get(0);
        assertNotification(notification, "Alarm at 4:30 AM", "Ringing");
        assertNotificationActionCount(notification, 2);
        assertNotificationAction(notification, 0, "Dismiss", NotificationReceiver.ACTION_DISMISS);
        assertNotificationAction(notification, 1, "Snooze", NotificationReceiver.ACTION_SNOOZE);

        // Check widget
        assertWidget(R.drawable.ic_alarm_white, "4:30 AM", null);

        // Check ring activity
        startActivityRing(globalManager.loadOneTimeAlarms().get(0));

        assertThat("Date visibility", textDate.getVisibility(), is(View.VISIBLE));
        assertThat("Time visibility", textTime.getVisibility(), is(View.VISIBLE));
        assertThat("Alarm time visibility", textAlarmTime.getVisibility(), is(View.INVISIBLE));
        assertThat("Alarm name visibility", textOneTimeAlarmName.getVisibility(), is(View.GONE));
        assertThat("Calendar visibility", textNextCalendar.getVisibility(), is(View.GONE));
        assertThat("Muted visibility", textMuted.getVisibility(), is(View.INVISIBLE));

        assertThat("Date", textDate.getText(), is("Monday, February 1"));
        assertThat("Time", textTime.getText(), is("4:30 AM"));

        // Check calendar
        startActivityCalendar();

        int count = recyclerView.getChildCount();
        assertThat("The number of items", count, is(HORIZON_DAYS + 1));

        assertCalendarItem(0, "2/1", "Mon", "Off", "", null, ""); // Today
        assertCalendarItem(1, "", "", "4:30 AM", "Ringing", "", "-10s"); // The one-time alarm
        assertCalendarItem(2, "2/2", "Tue", "Off", "", null, ""); // Tomorrow
    }

    @Test
    public void t401_onRingWithSecondAlarmTomorrow() {
        // Consume the alarm with action ACTION_SET_SYSTEM_ALARM
        consumeNextScheduledAlarm();

        // Add one-time alarm on tomorrow
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
        assertSystemAlarm(YEAR, MONTH, DAY + 1, ONE_TIME_ALARM_HOUR - 1, ONE_TIME_ALARM_MINUTE + 1, SystemAlarm.ACTION_RING_IN_NEAR_FUTURE);
        assertSystemAlarmClock(YEAR, MONTH, DAY + 1, ONE_TIME_ALARM_HOUR + 1, ONE_TIME_ALARM_MINUTE + 1);

        // Check notification
        assertNotificationCount(1);

        Notification notification = shadowNotificationManager.getAllNotifications().get(0);
        assertNotification(notification, "Alarm at 4:30 AM", "Ringing");
        assertNotificationActionCount(notification, 2);
        assertNotificationAction(notification, 0, "Dismiss", NotificationReceiver.ACTION_DISMISS);
        assertNotificationAction(notification, 1, "Snooze", NotificationReceiver.ACTION_SNOOZE);

        // Check widget
        assertWidget(R.drawable.ic_alarm_white, "4:30 AM", null);

        // Context menu - dismiss today's alarm
        startActivityCalendar();
        loadItemAtPosition(0);
        item.performLongClick();
        clickContextMenu(R.id.action_day_dismiss);
        refreshRecyclerView();

        // Shift clock by just under 2 hours after alarm
        shadowGlobalManager.setClock(new FixedClock(new GregorianCalendar(YEAR, MONTH, DAY, ONE_TIME_ALARM_HOUR + 1 + 2, ONE_TIME_ALARM_MINUTE, 59)));
        assertWidget(R.drawable.ic_alarm_white, "5:31 AM", "Tomorrow");

        // Shift clock to two hours after the alarm
        shadowGlobalManager.setClock(new FixedClock(new GregorianCalendar(YEAR, MONTH, DAY, ONE_TIME_ALARM_HOUR + 1 + 2, ONE_TIME_ALARM_MINUTE + 1)));
        assertWidget(R.drawable.ic_alarm_white, "5:31 AM", null);
    }

    @Test
    public void t410_dismiss_whileRinging_RingActivity() {
        prepareUntilRing();

        // Start ring activity
        startActivityRing(globalManager.loadOneTimeAlarms().get(0));

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
    public void t411_dismiss_whileRinging_CalendarActivity() {
        prepareUntilRing();

        // Start activity
        startActivityCalendar();

        int oldCount = recyclerView.getChildCount();

        // Context menu
        loadItemAtPosition(1);

        item.performLongClick();

        clickContextMenu(R.id.action_day_dismiss);

        refreshRecyclerView();

        // Check calendar
        int newCount = recyclerView.getChildCount();
        assertThat("The change of number of items", newCount - oldCount, is(0));

        assertCalendarItem(0, "2/1", "Mon", "Off", "", null, ""); // Today
        assertCalendarItem(1, "", "", "4:30 AM", "Passed", "", ""); // The one-time alarm
        assertCalendarItem(2, "2/2", "Tue", "Off", "", null, ""); // Tomorrow

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
    public void t420_disable_whileRinging() {
        prepareUntilRing();

        // Start activity
        startActivityCalendar();
        int oldCount = recyclerView.getChildCount();

        // Context menu
        loadItemAtPosition(1);

        item.performLongClick();

        clickContextMenu(R.id.action_day_disable);

        refreshRecyclerView();

        // Check calendar
        int newCount = recyclerView.getChildCount();
        assertThat("The change of number of items", newCount - oldCount, is(-1));

        assertCalendarItem(0, "2/1", "Mon", "Off", "", null, ""); // Today
        assertCalendarItem(1, "2/2", "Tue", "Off", "", null, ""); // Tomorrow

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
    public void t430_snooze_whileRinging_RingActivity() {
        prepareUntilRing();

        // Consume the alarm with action ACTION_SET_SYSTEM_ALARM
        consumeNextScheduledAlarm();

        // Start ring activity
        startActivityRing(globalManager.loadOneTimeAlarms().get(0));

        snoozeButton.performClick();

        // Start Calendar
        startActivityCalendar();

        assertCalendarItem(0, "2/1", "Mon", "Off", "", null, ""); // Today
        assertCalendarItem(1, "", "", "4:30 AM", "Snoozed until 4:40 AM", "", "-10s"); // The one-time alarm
        assertCalendarItem(2, "2/2", "Tue", "Off", "", null, ""); // Tomorrow

        // Check system alarm
        assertSystemAlarmCount(1);
        assertSystemAlarm(YEAR, MONTH, DAY, ONE_TIME_ALARM_HOUR, ONE_TIME_ALARM_MINUTE + 10, SystemAlarm.ACTION_RING);
        assertSystemAlarmClockNone();

        // Check notification
        assertNotificationCount(1);

        Notification notification = shadowNotificationManager.getAllNotifications().get(0);
        assertNotification(notification, "Alarm at 4:30 AM", "Snoozed until 4:40 AM");
        assertNotificationActionCount(notification, 1);
        assertNotificationAction(notification, 0, "Dismiss", NotificationReceiver.ACTION_DISMISS);

        // Check widget
        assertWidget(R.drawable.ic_alarm_white, "4:30 AM", null);
    }

    @Test
    public void t431_snooze_whileRinging_CalendarActivity() {
        prepareUntilRing();

        // Consume the alarm with action ACTION_SET_SYSTEM_ALARM
        consumeNextScheduledAlarm();

        // Start activity
        startActivityCalendar();
        int oldCount = recyclerView.getChildCount();

        // Context menu
        loadItemAtPosition(1);

        item.performLongClick();

        clickContextMenu(R.id.action_day_snooze);

        refreshRecyclerView();

        // Check calendar
        int newCount = recyclerView.getChildCount();
        assertThat("The change of number of items", newCount - oldCount, is(0));

        assertCalendarItem(0, "2/1", "Mon", "Off", "", null, ""); // Today
        assertCalendarItem(1, "", "", "4:30 AM", "Snoozed until 4:40 AM", "", "-10s"); // The one-time alarm
        assertCalendarItem(2, "2/2", "Tue", "Off", "", null, ""); // Tomorrow

        // Check system alarm
        assertSystemAlarmCount(1);
        assertSystemAlarm(YEAR, MONTH, DAY, ONE_TIME_ALARM_HOUR, ONE_TIME_ALARM_MINUTE + 10, SystemAlarm.ACTION_RING);
        assertSystemAlarmClockNone();

        // Check notification
        assertNotificationCount(1);

        Notification notification = shadowNotificationManager.getAllNotifications().get(0);
        assertNotification(notification, "Alarm at 4:30 AM", "Snoozed until 4:40 AM");
        assertNotificationActionCount(notification, 1);
        assertNotificationAction(notification, 0, "Dismiss", NotificationReceiver.ACTION_DISMISS);

        // Check widget
        assertWidget(R.drawable.ic_alarm_white, "4:30 AM", null);
    }

    private int shared_t440_setTime_whileRinging(int hour, int minute) {
        prepareUntilRing();

        // Consume the alarm with action ACTION_SET_SYSTEM_ALARM
        consumeNextScheduledAlarm();

        return calendar_setTime(1, ONE_TIME_ALARM_HOUR, ONE_TIME_ALARM_MINUTE, hour, minute);
    }

    @Test
    public void t440_setTime_whileRinging_inPast() {
        int oldCount = shared_t440_setTime_whileRinging(ONE_TIME_ALARM_HOUR, ONE_TIME_ALARM_MINUTE - 1);

        // Check calendar
        int newCount = recyclerView.getChildCount();
        assertThat("The number of items did not change", newCount - oldCount, is(0));

        assertCalendarItem(0, "2/1", "Mon", "Off", "", null, ""); // Today
        assertCalendarItem(1, "", "", "4:29 AM", "Passed", "", ""); // The one-time alarm
        assertCalendarItem(2, "2/2", "Tue", "Off", "", null, ""); // Tomorrow

        // Check system alarm
        assertSystemAlarmCount(0);
        assertSystemAlarmClockNone();

        // Check notification
        assertNotificationCount(0);

        // Check widget
        assertWidget(R.drawable.ic_alarm_off_white, "No alarm", null);
    }

    @Test
    public void t441_setTime_whileRinging_inNearPeriod() {
        int oldCount = shared_t440_setTime_whileRinging(ONE_TIME_ALARM_HOUR + 1, ONE_TIME_ALARM_MINUTE + 1);

        // Check calendar
        int newCount = recyclerView.getChildCount();
        assertThat("The change of number of items", newCount - oldCount, is(0));

        assertCalendarItem(0, "2/1", "Mon", "Off", "", null, ""); // Today
        assertCalendarItem(1, "", "", "5:31 AM", "", "", "1h 0m"); // The one-time alarm
        assertCalendarItem(2, "2/2", "Tue", "Off", "", null, ""); // Tomorrow

        // Check system alarm
        assertSystemAlarmCount(2);
        assertSystemAlarm(YEAR, MONTH, DAY, ONE_TIME_ALARM_HOUR + 1, ONE_TIME_ALARM_MINUTE + 1, SystemAlarm.ACTION_RING);
        assertSystemAlarmClock(YEAR, MONTH, DAY, ONE_TIME_ALARM_HOUR + 1, ONE_TIME_ALARM_MINUTE + 1);

//        // Check notification
        assertNotificationCount(1);

        Notification notification = shadowNotificationManager.getAllNotifications().get(0);
        assertNotification(notification, "Alarm at 5:31 AM", "Touch to view all alarms");
        assertNotificationActionCount(notification, 1);
        assertNotificationAction(notification, 0, "Dismiss", NotificationReceiver.ACTION_DISMISS_BEFORE_RINGING);

        // Check widget
        assertWidget(R.drawable.ic_alarm_white, "5:31 AM", null);
    }

    @Test
    public void t442_setTime_whileRinging_afterNearPeriod() {
        int oldCount = shared_t440_setTime_whileRinging(ONE_TIME_ALARM_HOUR + 5, ONE_TIME_ALARM_MINUTE + 5);

        // Check calendar
        int newCount = recyclerView.getChildCount();
        assertThat("The change of number of items", newCount - oldCount, is(0));

        assertCalendarItem(0, "2/1", "Mon", "Off", "", null, ""); // Today
        assertCalendarItem(1, "", "", "9:35 AM", "", "", "5h 4m"); // The one-time alarm
        assertCalendarItem(2, "2/2", "Tue", "Off", "", null, ""); // Tomorrow

        // Check system alarm
        assertSystemAlarmCount(2);
        assertSystemAlarm(YEAR, MONTH, DAY, ONE_TIME_ALARM_HOUR + 5 - 2, ONE_TIME_ALARM_MINUTE + 5, SystemAlarm.ACTION_RING_IN_NEAR_FUTURE);
        assertSystemAlarmClock(YEAR, MONTH, DAY, ONE_TIME_ALARM_HOUR + 5, ONE_TIME_ALARM_MINUTE + 5);

        // Check notification
        assertNotificationCount(0);
        assertSystemAlarmClockNone();

        // Check widget
        assertWidget(R.drawable.ic_alarm_white, "9:35 AM", null);
    }

    private int shared_t450_setDate_whileRinging(int year, int month, int day) {
        prepareUntilRing();

        // Consume the alarm with action ACTION_RING
        consumeNextScheduledAlarm();

        // Set date
        return calendar_setDate(1, YEAR, MONTH, DAY, year, month, day);
    }

    @Test
    public void t450_setDate_whileRinging_inPast() {
        int oldCount = shared_t450_setDate_whileRinging(YEAR, MONTH, DAY - 1);

        // Check calendar
        int newCount = recyclerView.getChildCount();
        assertThat("The number of items decreased by one", newCount - oldCount, is(-1));

        assertCalendarItem(0, "2/1", "Mon", "Off", "", null, ""); // Today
        assertCalendarItem(1, "2/2", "Tue", "Off", "", null, ""); // Tomorrow

        // Check system alarm
        assertSystemAlarmCount(0);
        assertSystemAlarmClockNone();

        // Check notification
        assertNotificationCount(0);

        // Check widget
        assertWidget(R.drawable.ic_alarm_off_white, "No alarm", null);
    }

    @Test
    public void t451_setDate_whileRinging_inFuture() {
        int oldCount = shared_t450_setDate_whileRinging(YEAR, MONTH, DAY + 1);

        // Check calendar
        int newCount = recyclerView.getChildCount();
        assertThat("The change of number of items", newCount - oldCount, is(0));

        assertCalendarItem(0, "2/1", "Mon", "Off", "", null, ""); // Today
        assertCalendarItem(1, "2/2", "Tue", "Off", "", null, ""); // Tomorrow
        assertCalendarItem(2, "", "", "4:30 AM", "", "", "23h 59m"); // The one-time alarm
        assertCalendarItem(3, "2/3", "Wed", "Off", "", null, ""); // The day after tomorrow

        // Check system alarm
        assertSystemAlarmCount(2);
        assertSystemAlarm(YEAR, MONTH, DAY + 1, ONE_TIME_ALARM_HOUR - 2, ONE_TIME_ALARM_MINUTE, SystemAlarm.ACTION_RING_IN_NEAR_FUTURE);
        assertSystemAlarmClock(YEAR, MONTH, DAY + 1, ONE_TIME_ALARM_HOUR, ONE_TIME_ALARM_MINUTE);

        // Check notification
        assertNotificationCount(0);

        // Check widget
        assertWidget(R.drawable.ic_alarm_white, "4:30 AM", "Tomorrow");
    }

    @Test
    public void t460_setName_whileRinging() {
        prepareUntilRing();

        // Consume the alarm with action ACTION_SET_SYSTEM_ALARM
        consumeNextScheduledAlarm();

        // Start activity
        startActivityCalendar();

        int oldCount = recyclerView.getChildCount();

        // Change name
        loadItemAtPosition(1);

        textName.requestFocus();

        // Show soft keyboard
        final InputMethodManager inputMethodManager = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        inputMethodManager.showSoftInput(textName, InputMethodManager.SHOW_IMPLICIT);

        textName.setText("Flowers");

        // Click Done in the soft keyboard
        textName.onEditorAction(EditorInfo.IME_ACTION_DONE);

        refreshRecyclerView();

        // Check calendar
        int newCount = recyclerView.getChildCount();
        assertThat("The number of items did not change", newCount - oldCount, is(0));

        assertCalendarItem(0, "2/1", "Mon", "Off", "", null, ""); // Today
        assertCalendarItem(1, "", "", "4:30 AM", "Ringing", "Flowers", "-10s"); // The one-time alarm
        assertCalendarItem(2, "2/2", "Tue", "Off", "", null, ""); // Tomorrow

        // Check system alarm
        assertSystemAlarmCount(0);
        assertSystemAlarmClockNone();

        // Check notification
        assertNotificationCount(1);

        Notification notification = shadowNotificationManager.getAllNotifications().get(0);
        assertNotification(notification, "Alarm at 4:30 AM – Flowers", "Ringing");
        assertNotificationActionCount(notification, 2);
        assertNotificationAction(notification, 0, "Dismiss", NotificationReceiver.ACTION_DISMISS);
        assertNotificationAction(notification, 1, "Snooze", NotificationReceiver.ACTION_SNOOZE);

        // Check widget
        assertWidget(R.drawable.ic_alarm_white, "4:30 AM", null);
    }

    @Test
    public void t510_dismiss_whileSnoozed() {
        prepareUntilSnooze();

        // Consume the alarm with action ACTION_RING
        consumeNextScheduledAlarm();

        // Start activity
        startActivityCalendar();

        int oldCount = recyclerView.getChildCount();

        // Context menu
        loadItemAtPosition(1);

        item.performLongClick();

        clickContextMenu(R.id.action_day_dismiss);

        // Check calendar
        int newCount = recyclerView.getChildCount();
        assertThat("The change of number of items", newCount - oldCount, is(0));

        assertCalendarItem(0, "2/1", "Mon", "Off", "", null, ""); // Today
        assertCalendarItem(1, "", "", "4:30 AM", "Passed", "", ""); // The one-time alarm
        assertCalendarItem(2, "2/2", "Tue", "Off", "", null, ""); // Tomorrow

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
    public void t520_disable_whileSnoozed() {
        prepareUntilSnooze();

        // Consume the alarm with action ACTION_RING
        consumeNextScheduledAlarm();

        // Start activity
        startActivityCalendar();

        int oldCount = recyclerView.getChildCount();

        // Context menu
        loadItemAtPosition(1);

        item.performLongClick();

        clickContextMenu(R.id.action_day_disable);

        refreshRecyclerView();

        // Check calendar
        int newCount = recyclerView.getChildCount();
        assertThat("The change of number of items", newCount - oldCount, is(-1));

        assertCalendarItem(0, "2/1", "Mon", "Off", "", null, ""); // Today
        assertCalendarItem(1, "2/2", "Tue", "Off", "", null, ""); // Tomorrow

        // Check system alarm
        assertSystemAlarmCount(1);
        assertSystemAlarm(YEAR, MONTH, DAY + 1, 0, 0, SystemAlarm.ACTION_SET_SYSTEM_ALARM);
        assertSystemAlarmClockNone();

        // Check notification
        assertNotificationCount(0);

        // Check widget
        assertWidget(R.drawable.ic_alarm_off_white, "No alarm", null);
    }

    private int shared_t540_setTime_whileSnoozed(int hour, int minute) {
        prepareUntilSnooze();

        // Consume the alarm with action ACTION_RING
        consumeNextScheduledAlarm();

        return calendar_setTime(1, ONE_TIME_ALARM_HOUR, ONE_TIME_ALARM_MINUTE, hour, minute);
    }

    @Test
    public void t540_setTime_whileSnoozed_inPast() {
        int oldCount = shared_t540_setTime_whileSnoozed(ONE_TIME_ALARM_HOUR, ONE_TIME_ALARM_MINUTE - 1);

        // Check calendar
        int newCount = recyclerView.getChildCount();
        assertThat("The number of items did not change", newCount - oldCount, is(0));

        assertCalendarItem(0, "2/1", "Mon", "Off", "", null, ""); // Today
        assertCalendarItem(1, "", "", "4:29 AM", "Passed", "", ""); // The one-time alarm
        assertCalendarItem(2, "2/2", "Tue", "Off", "", null, ""); // Tomorrow

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
    public void t541_setTime_whileSnoozed_inNearPeriod() {
        int oldCount = shared_t540_setTime_whileSnoozed(ONE_TIME_ALARM_HOUR + 1, ONE_TIME_ALARM_MINUTE + 1);

        // Check calendar
        int newCount = recyclerView.getChildCount();
        assertThat("The change of number of items", newCount - oldCount, is(0));

        assertCalendarItem(0, "2/1", "Mon", "Off", "", null, ""); // Today
        assertCalendarItem(1, "", "", "5:31 AM", "", "", "1h 0m"); // The one-time alarm
        assertCalendarItem(2, "2/2", "Tue", "Off", "", null, ""); // Tomorrow

        // Check system alarm
        assertSystemAlarmCount(2);
        assertSystemAlarm(YEAR, MONTH, DAY, ONE_TIME_ALARM_HOUR + 1, ONE_TIME_ALARM_MINUTE + 1, SystemAlarm.ACTION_RING);
        assertSystemAlarmClock(YEAR, MONTH, DAY, ONE_TIME_ALARM_HOUR + 1, ONE_TIME_ALARM_MINUTE + 1);

        // Check notification
        assertNotificationCount(1);

        Notification notification = shadowNotificationManager.getAllNotifications().get(0);
        assertNotification(notification, "Alarm at 5:31 AM", "Touch to view all alarms");
        assertNotificationActionCount(notification, 1);
        assertNotificationAction(notification, 0, "Dismiss", NotificationReceiver.ACTION_DISMISS_BEFORE_RINGING);

        // Check widget
        assertWidget(R.drawable.ic_alarm_white, "5:31 AM", null);
    }

    @Test
    public void t542_setTime_whileSnoozed_afterNearPeriod() {
        int oldCount = shared_t440_setTime_whileRinging(ONE_TIME_ALARM_HOUR + 5, ONE_TIME_ALARM_MINUTE + 5);

        // Check calendar
        int newCount = recyclerView.getChildCount();
        assertThat("The change of number of items", newCount - oldCount, is(0));

        assertCalendarItem(0, "2/1", "Mon", "Off", "", null, ""); // Today
        assertCalendarItem(1, "", "", "9:35 AM", "", "", "5h 4m"); // The one-time alarm
        assertCalendarItem(2, "2/2", "Tue", "Off", "", null, ""); // Tomorrow

        // Check system alarm
        assertSystemAlarmCount(2);
        assertSystemAlarm(YEAR, MONTH, DAY, ONE_TIME_ALARM_HOUR - 2 + 5, ONE_TIME_ALARM_MINUTE + 5, SystemAlarm.ACTION_RING_IN_NEAR_FUTURE);
        assertSystemAlarmClock(YEAR, MONTH, DAY, ONE_TIME_ALARM_HOUR + 5, ONE_TIME_ALARM_MINUTE + 5);

        // Check notification
        assertNotificationCount(0);

        // Check widget
        assertWidget(R.drawable.ic_alarm_white, "9:35 AM", null);
    }

    private int shared_t550_setDate_whileSnoozed(int year, int month, int day) {
        prepareUntilSnooze();

        // Consume the alarm with action ACTION_RING
        consumeNextScheduledAlarm();

        // Set date
        return calendar_setDate(1, YEAR, MONTH, DAY, year, month, day);
    }

    @Test
    public void t550_setDate_whileSnoozed_inPast() {
        int oldCount = shared_t550_setDate_whileSnoozed(YEAR, MONTH, DAY - 1);

        // Check calendar
        int newCount = recyclerView.getChildCount();
        assertThat("The number of items decreased by one", newCount - oldCount, is(-1));

        assertCalendarItem(0, "2/1", "Mon", "Off", "", null, ""); // Today
        assertCalendarItem(1, "2/2", "Tue", "Off", "", null, ""); // Tomorrow

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
    public void t551_setDate_whileSnoozed_inFuture() {
        int oldCount = shared_t550_setDate_whileSnoozed(YEAR, MONTH, DAY + 1);

        // Check calendar
        int newCount = recyclerView.getChildCount();
        assertThat("The change of number of items", newCount - oldCount, is(0));

        assertCalendarItem(0, "2/1", "Mon", "Off", "", null, ""); // Today
        assertCalendarItem(1, "2/2", "Tue", "Off", "", null, ""); // Tomorrow
        assertCalendarItem(2, "", "", "4:30 AM", "", "", "23h 59m"); // The one-time alarm
        assertCalendarItem(3, "2/3", "Wed", "Off", "", null, ""); // The day after tomorrow

        // Check system alarm
        assertSystemAlarmCount(2);
        assertSystemAlarm(YEAR, MONTH, DAY + 1, ONE_TIME_ALARM_HOUR - 2, ONE_TIME_ALARM_MINUTE, SystemAlarm.ACTION_RING_IN_NEAR_FUTURE);
        assertSystemAlarmClock(YEAR, MONTH, DAY + 1, ONE_TIME_ALARM_HOUR, ONE_TIME_ALARM_MINUTE);

        // Check notification
        assertNotificationCount(0);

        // Check widget
        assertWidget(R.drawable.ic_alarm_white, "4:30 AM", "Tomorrow");
    }

    @Test
    public void t560_setName_whileSnoozed() {
        prepareUntilSnooze();

        // Consume the alarm with action ACTION_RING
        consumeNextScheduledAlarm();

        // Start activity
        startActivityCalendar();

        int oldCount = recyclerView.getChildCount();

        // Change name
        loadItemAtPosition(1);

        textName.requestFocus();

        // Show soft keyboard
        final InputMethodManager inputMethodManager = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        inputMethodManager.showSoftInput(textName, InputMethodManager.SHOW_IMPLICIT);

        textName.setText("Flowers");

        // Click Done in the soft keyboard
        textName.onEditorAction(EditorInfo.IME_ACTION_DONE);

        refreshRecyclerView();

        // Check calendar
        int newCount = recyclerView.getChildCount();
        assertThat("The number of items did not change", newCount - oldCount, is(0));

        assertCalendarItem(0, "2/1", "Mon", "Off", "", null, ""); // Today
        assertCalendarItem(1, "", "", "4:30 AM", "Snoozed until 4:40 AM", "Flowers", "-20s"); // The one-time alarm
        assertCalendarItem(2, "2/2", "Tue", "Off", "", null, ""); // Tomorrow

        // Check system alarm
        assertSystemAlarmCount(0);
        assertSystemAlarmClockNone();

        // Check notification
        assertNotificationCount(1);

        Notification notification = shadowNotificationManager.getAllNotifications().get(0);
        assertNotification(notification, "Alarm at 4:30 AM – Flowers", "Snoozed until 4:40 AM");
        assertNotificationActionCount(notification, 1);
        assertNotificationAction(notification, 0, "Dismiss", NotificationReceiver.ACTION_DISMISS);

        // Check widget
        assertWidget(R.drawable.ic_alarm_white, "4:30 AM", null);
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
        AppAlarm appAlarm = globalManager.loadOneTimeAlarms().get(0);
        intent.putExtra(GlobalManager.PERSIST_ALARM_TYPE, appAlarm.getClass().getSimpleName());
        intent.putExtra(GlobalManager.PERSIST_ALARM_ID, appAlarm.getPersistenceId());
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
        List<OneTimeAlarm> oneTimeAlarms = globalManager.loadOneTimeAlarms();
        Collections.sort(oneTimeAlarms);

        Intent intent = new Intent();
        intent.setAction(SystemAlarm.ACTION_RING);
        AppAlarm appAlarm = oneTimeAlarms.get(0);
        intent.putExtra(GlobalManager.PERSIST_ALARM_TYPE, appAlarm.getClass().getSimpleName());
        intent.putExtra(GlobalManager.PERSIST_ALARM_ID, appAlarm.getPersistenceId());
        AlarmReceiver alarmReceiver = new AlarmReceiver();
        alarmReceiver.onReceive(context, intent);

        // Shift clock
        shadowGlobalManager.setClock(new FixedClock(new GregorianCalendar(YEAR, MONTH, DAY, ONE_TIME_ALARM_HOUR, ONE_TIME_ALARM_MINUTE, 10)));
    }

    private void prepareUntilSnooze() {
        prepareUntilRing();

        // Consume the alarm with action ACTION_SET_SYSTEM_ALARM
        consumeNextScheduledAlarm();

        // Start ring activity
        startActivityRing(globalManager.loadOneTimeAlarms().get(0));

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

    private void setAlarm(Calendar date) {
        OneTimeAlarm oneTimeAlarm = new OneTimeAlarm();
        oneTimeAlarm.setDate(date);
        oneTimeAlarm.setHour(date.get(Calendar.HOUR_OF_DAY));
        oneTimeAlarm.setMinute(date.get(Calendar.MINUTE));

        Analytics analytics = new Analytics(Analytics.Channel.Test, Analytics.ChannelName.Calendar);

        globalManager.createOneTimeAlarm(oneTimeAlarm, analytics);
    }

}
