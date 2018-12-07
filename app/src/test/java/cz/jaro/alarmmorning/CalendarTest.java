package cz.jaro.alarmmorning;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.junit.After;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.fakes.RoboMenuItem;
import org.robolectric.shadows.ShadowActivity;
import org.robolectric.shadows.ShadowAlarmManager;
import org.robolectric.shadows.ShadowAppWidgetManager;
import org.robolectric.shadows.ShadowDrawable;
import org.robolectric.shadows.ShadowNotification;
import org.robolectric.shadows.ShadowNotificationManager;
import org.robolectric.shadows.ShadowPendingIntent;
import org.robolectric.shadows.ShadowTimePickerDialog;
import org.robolectric.shadows.ShadowViewGroup;

import java.util.Calendar;
import java.util.GregorianCalendar;

import cz.jaro.alarmmorning.clock.FixedClock;
import cz.jaro.alarmmorning.graphics.SlideButton;
import cz.jaro.alarmmorning.model.Day;
import cz.jaro.alarmmorning.model.Defaults;
import cz.jaro.alarmmorning.receivers.AlarmReceiver;
import cz.jaro.alarmmorning.receivers.NotificationReceiver;
import cz.jaro.alarmmorning.receivers.VoidReceiver;
import cz.jaro.alarmmorning.shadows.ShadowGlobalManager;
import cz.jaro.alarmmorning.wizard.Wizard;

import static cz.jaro.alarmmorning.GlobalManager.HORIZON_DAYS;
import static cz.jaro.alarmmorning.model.AlarmDbHelper.DEFAULT_ALARM_HOUR;
import static cz.jaro.alarmmorning.model.AlarmDbHelper.DEFAULT_ALARM_MINUTE;
import static cz.jaro.alarmmorning.model.DayTest.DAY;
import static cz.jaro.alarmmorning.model.DayTest.HOUR_DEFAULT;
import static cz.jaro.alarmmorning.model.DayTest.MINUTE_DEFAULT;
import static cz.jaro.alarmmorning.model.DayTest.MONTH;
import static cz.jaro.alarmmorning.model.DayTest.YEAR;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.robolectric.Robolectric.buildActivity;

/**
 * Tests of alarm management in UI.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 21, shadows = {ShadowGlobalManager.class})
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class CalendarTest extends FixedTimeTest {
    Context context;

    AlarmManager alarmManager;
    ShadowAlarmManager shadowAlarmManager;

    NotificationManager notificationManager;
    ShadowNotificationManager shadowNotificationManager;

    AppWidgetManager appWidgetManager;
    ShadowAppWidgetManager shadowAppWidgetManager;

    Activity activity;
    ShadowActivity shadowActivity;

    // Items in CalendarFragment of AlarmMorningActivity
    RecyclerView recyclerView;

    View item;

    TextView textDate;
    TextView textDoW;
    TextView textTime;
    TextView textState;
    EditText textName;
    TextView textComment;
    LinearLayout headerDate;

    // Items in RingActivity
    TextView textAlarmTime;
    TextView textOneTimeAlarmName;
    TextView textNextCalendar;
    TextView textMuted;

    ImageButton snoozeButton;
    SlideButton dismissButton;

    @Before
    public void before() {
        super.before();

        AlarmMorningActivityTest.saveWizardPreference(!Wizard.PREF_WIZARD_DEFAULT);

        context = RuntimeEnvironment.application.getApplicationContext();

        AlarmMorningActivityTest.setLocale(context, "en", "US");

        alarmManager = (AlarmManager) RuntimeEnvironment.application.getSystemService(Context.ALARM_SERVICE);
        shadowAlarmManager = Shadows.shadowOf(alarmManager);

        notificationManager = (NotificationManager) RuntimeEnvironment.application.getSystemService(Context.NOTIFICATION_SERVICE);
        shadowNotificationManager = Shadows.shadowOf(notificationManager);

        appWidgetManager = AppWidgetManager.getInstance(context);
        shadowAppWidgetManager = Shadows.shadowOf(appWidgetManager);

        AppWidgetProviderInfo appWidgetProviderInfo = new AppWidgetProviderInfo();
        appWidgetProviderInfo.provider = new ComponentName(context, WidgetProvider.class);
        shadowAppWidgetManager.addInstalledProvider(appWidgetProviderInfo);
    }

    @After
    public void after() {
        super.after();

        // Close ring activity
        if (activity instanceof RingActivity) {
            RingActivity ringActivity = (RingActivity) this.activity;
            ringActivity.shutdown();
        }

        // Cancel all notifications
        notificationManager.cancelAll();
    }

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
        shadowGlobalManager.setClock(new FixedClock(new GregorianCalendar(YEAR, MONTH, DAY, HOUR_DEFAULT + 2, MINUTE_DEFAULT, 59)));
        assertWidget(R.drawable.ic_alarm_white, "8:01 AM", "Tomorrow");

        // Shift clock
        shadowGlobalManager.setClock(new FixedClock(new GregorianCalendar(YEAR, MONTH, DAY, HOUR_DEFAULT + 2, MINUTE_DEFAULT + 1)));
        // FIXME Notification test assertWidget(R.drawable.ic_alarm_white, "8:01 AM", null);
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
        // FIXME Notification test assertWidget(R.drawable.ic_alarm_off_white, "No alarm", null);
    }

    @Test
    public void t540_setTimeWhileSnoozed() {
        prepareUntilSnooze();

        // Context menu
        startActivityCalendar();

        loadItemAtPosition(0);

        item.performLongClick();

        clickContextMenu(R.id.action_day_set_time);

        // Set time
        // FIXME maybe use calendar_setDayAlarm(0, DEFAULT_ALARM_HOUR , DEFAULT_ALARM_MINUTE, DEFAULT_ALARM_HOUR + 6, DEFAULT_ALARM_MINUTE);
        picker_setTime(DEFAULT_ALARM_HOUR, DEFAULT_ALARM_MINUTE + 30, DEFAULT_ALARM_HOUR + 6, DEFAULT_ALARM_MINUTE);

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
        // FIXME Notification test assertWidget(R.drawable.ic_alarm_white, "1:00 PM", null);
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
        // FIXME Notification test assertWidget(R.drawable.ic_alarm_off_white, "No alarm", null);
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

    public static void setNearFuturePeriodPreferenceToZero(Context context) {
        setNearFuturePeriodPreference(context, 0);
    }

    public static void setNearFuturePeriodPreference(Context context, int minutes) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt(SettingsActivity.PREF_NEAR_FUTURE_TIME, minutes);
        editor.commit();
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

    private int calendar_setDayAlarm(int itemPosition, int hourCheck, int minuteCheck, int hour, int minute) {
        // Start activity
        startActivityCalendar();

        int itemCount = recyclerView.getChildCount();

        // Click on item
        loadItemAtPosition(itemPosition);

        item.performClick();

        // Set time
        picker_setTime(hourCheck, minuteCheck, hour, minute);

        refreshRecyclerView();

        return itemCount;
    }

    private void startActivityRing(Calendar alarmTime) {
        Intent ringIntent = new Intent(context, RingActivity.class);
        ringIntent.putExtra(RingActivity.ALARM_TIME, alarmTime);

        activity = buildActivity(RingActivity.class, ringIntent).setup().get();
        shadowActivity = Shadows.shadowOf(activity);

        AlarmMorningActivityTest.setLocale(activity, "en", "US");

        textDate = (TextView) activity.findViewById(R.id.date);
        textTime = (TextView) activity.findViewById(R.id.time);
        textAlarmTime = (TextView) activity.findViewById(R.id.alarmTime);
        textOneTimeAlarmName = (TextView) activity.findViewById(R.id.oneTimeAlarmName);
        textNextCalendar = (TextView) activity.findViewById(R.id.nextCalendar);
        textMuted = (TextView) activity.findViewById(R.id.muted);

        snoozeButton = (ImageButton) activity.findViewById(R.id.snoozeButton);
        dismissButton = (SlideButton) activity.findViewById(R.id.dismissButton);
    }

    private void startActivityCalendar() {
        activity = Robolectric.setupActivity(AlarmMorningActivity.class);
        shadowActivity = Shadows.shadowOf(activity);

        recyclerView = (RecyclerView) shadowActivity.findViewById(R.id.calendar_recycler_view);

        refreshRecyclerView();
    }

    private void refreshRecyclerView() {
        refreshRecyclerView(recyclerView);
    }

    public static void refreshRecyclerView(RecyclerView recyclerView) {
        // Hack: RecyclerView needs to be measured and laid out manually in Robolectric.
        // Source: http://stackoverflow.com/questions/27052866/android-robolectric-click-recyclerview-item
        recyclerView.measure(0, 0);
        recyclerView.layout(0, 0, 100, 10000);
    }

    void loadItemAtPosition(int position) {
        item = recyclerView.getChildAt(position);

        textDate = (TextView) item.findViewById(R.id.textDate);
        textDoW = (TextView) item.findViewById(R.id.textDayOfWeekCal);
        textTime = (TextView) item.findViewById(R.id.textTimeCal);
        textState = (TextView) item.findViewById(R.id.textState);
        textName = (EditText) item.findViewById(R.id.textName);
        textComment = (TextView) item.findViewById(R.id.textComment);
        headerDate = (LinearLayout) item.findViewById(R.id.headerDate);
    }

    void clickContextMenu(int id) {
        clickContextMenu(recyclerView, id);
    }

    public static void clickContextMenu(RecyclerView recyclerView, int id) {
        ShadowViewGroup shadowViewGroup = Shadows.shadowOf(recyclerView);
        android.app.Fragment calendarFragment = (CalendarFragment) shadowViewGroup.getOnCreateContextMenuListener();
        final RoboMenuItem contextMenuItem = new RoboMenuItem(id);

        // TODO Check that the context menu contains this id and that this id is is visible (not yet easily supported by Roboletric)

        // Select the context menu item
        calendarFragment.onContextItemSelected(contextMenuItem);
    }

    void picker_setTime(int hourCheck, int minuteCheck, int hour, int minute) {
        // Time picker
        TimePickerFragment timePickerFragment = (TimePickerFragment) activity.getFragmentManager().findFragmentByTag("timePicker");

        TimePickerDialog dialog = (TimePickerDialog) timePickerFragment.getDialog();
        ShadowTimePickerDialog shadowDialog = Shadows.shadowOf(dialog);

        // Check presets
        assertThat("Preset hour", shadowDialog.getHourOfDay(), is(hourCheck));
        assertThat("Preset minute", shadowDialog.getMinute(), is(minuteCheck));

        // Change the time
        dialog.updateTime(hour, minute);

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick();
    }

    void assertCalendarItem(int position, String date, String dow, String time, String state, String comment) {
        assertCalendarItem(position, date, dow, time, state, null, comment);
    }

    void assertCalendarItem(int position, String date, String dow, String time, String state, String name, String comment) {
        loadItemAtPosition(position);
        assertThat("Date", textDate.getText(), is(date));
        assertThat("DoW", textDoW.getText(), is(dow));
        assertThat("Time", textTime.getText(), is(time));
        assertThat("State", textState.getText(), is(state));
        if (name == null)
            assertThat("Name visibility", textName.getVisibility(), is(View.GONE));
        else {
            assertThat("Name visibility", textName.getVisibility(), is(View.VISIBLE));
            assertThat("Name", textName.getText().toString(), is(name));
        }
        assertThat("Comment", textComment.getText(), is(comment));
    }

    void consumeNextScheduledAlarm() {
        consumeNextScheduledAlarm(shadowAlarmManager);
    }

    public static void consumeNextScheduledAlarm(ShadowAlarmManager shadowAlarmManager) {
        assertThat("There is a scheduled alarm", 0 < shadowAlarmManager.getScheduledAlarms().size(), is(true));
        shadowAlarmManager.getNextScheduledAlarm();
    }

    private void assertSystemAlarm(int year, int month, int day, int hour, int minute, String action) {
        assertSystemAlarm(context, shadowAlarmManager, year, month, day, hour, minute, action);
    }

    public static void assertSystemAlarm(Context context, ShadowAlarmManager shadowAlarmManager, int year, int month, int day, int hour, int minute, String action) {
        ShadowAlarmManager.ScheduledAlarm nextScheduledAlarm = shadowAlarmManager.getNextScheduledAlarm();

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
        ShadowPendingIntent shadowOperation = Shadows.shadowOf(operation);

        Intent expectedIntent = new Intent(context, AlarmReceiver.class);

        assertThat("Broadcast", shadowOperation.isBroadcastIntent(), is(true));
        assertThat("Intent count", shadowOperation.getSavedIntents().length, is(1));
        assertThat("Class", shadowOperation.getSavedIntents()[0].getComponent(), is(expectedIntent.getComponent()));
        assertThat("Action", shadowOperation.getSavedIntent().getAction(), is(action));
    }

    private void assertSystemAlarmCount(int count) {
        assertSystemAlarmCount(shadowAlarmManager, count);
    }

    public static void assertSystemAlarmCount(ShadowAlarmManager shadowAlarmManager, int count) {
        assertThat("Alarm count", shadowAlarmManager.getScheduledAlarms().size(), is(count));
    }

    private void assertSystemAlarmClock(int year, int month, int day, int hour, int minute) {
        assertSystemAlarmClock(context, alarmManager, shadowAlarmManager, year, month, day, hour, minute);
    }

    public static void assertSystemAlarmClock(Context context, AlarmManager alarmManager, ShadowAlarmManager shadowAlarmManager,
                                              int year, int month, int day, int hour, int minute) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            AlarmManager.AlarmClockInfo alarmClockInfo = alarmManager.getNextAlarmClock();

            Calendar time = GregorianCalendar.getInstance();
            time.setTimeInMillis(alarmClockInfo.getTriggerTime());
            assertThat("Year", time.get(Calendar.YEAR), is(year));
            assertThat("Month", time.get(Calendar.MONTH), is(month));
            assertThat("Date", time.get(Calendar.DAY_OF_MONTH), is(day));
            assertThat("Hour", time.get(Calendar.HOUR_OF_DAY), is(hour));
            assertThat("Minute", time.get(Calendar.MINUTE), is(minute));
            assertThat("Second", time.get(Calendar.SECOND), is(0));
            assertThat("Millisecond", time.get(Calendar.MILLISECOND), is(0));

            // Show intent
            PendingIntent showIntent = alarmClockInfo.getShowIntent();
            ShadowPendingIntent shadowShowIntent = Shadows.shadowOf(showIntent);

            Intent expectedShowIntent = new Intent(context, AlarmMorningActivity.class);

            assertThat("Broadcast", shadowShowIntent.isBroadcastIntent(), is(true));
            assertThat("Intent count", shadowShowIntent.getSavedIntents().length, is(1));
            assertThat("Class", shadowShowIntent.getSavedIntents()[0].getComponent(), is(expectedShowIntent.getComponent()));
            assertNull("Action", shadowShowIntent.getSavedIntent().getAction());

            // Operation intent
            PendingIntent operation = shadowAlarmManager.getNextScheduledAlarm().operation;
            ShadowPendingIntent shadowIntent = Shadows.shadowOf(operation);

            Intent expectedIntent = new Intent(context, VoidReceiver.class);

            assertThat("Broadcast", shadowIntent.isBroadcastIntent(), is(true));
            assertThat("Intent count", shadowIntent.getSavedIntents().length, is(1));
            assertThat("Class", shadowIntent.getSavedIntents()[0].getComponent(), is(expectedIntent.getComponent()));
            assertNull("Action", shadowIntent.getSavedIntent().getAction());
        }
    }

    private void assertSystemAlarmClockNone() {
        assertSystemAlarmClockNone(alarmManager);
    }

    public static void assertSystemAlarmClockNone(AlarmManager alarmManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            AlarmManager.AlarmClockInfo alarmClockInfo = alarmManager.getNextAlarmClock();
            assertNull(alarmClockInfo);
        }
    }

    private void assertNotificationCount(int count) {
        assertNotificationCount(shadowNotificationManager, count);
    }

    public static void assertNotificationCount(ShadowNotificationManager shadowNotificationManager, int count) {
        assertThat("Notification count", shadowNotificationManager.size(), is(count));
    }

    public static void assertNotification(Notification notification, String bigContentTitle, String bigContentText) {
        ShadowNotification shadowNotification = Shadows.shadowOf(notification);

        assertThat("Notification title", shadowNotification.getBigContentTitle(), is(bigContentTitle));
        assertThat("Notification text", shadowNotification.getBigContentText(), is(bigContentText));
    }

    public static void assertNotificationActionCount(Notification notification, int count) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
            assertThat("Notification action count", notification.actions.length, is(count));
        }
    }

    private void assertNotificationAction(Notification notification, int index, String title, String actionString) {
        assertNotificationAction(context, notification, index, title, actionString);
    }

    public static void assertNotificationAction(Context context, Notification notification, int index, String title, String actionString) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
            Notification.Action actionButton = notification.actions[index];
            assertThat("Notification action title", actionButton.title, is(title));

            PendingIntent intent1 = actionButton.actionIntent;
            ShadowPendingIntent shadowIntent1 = Shadows.shadowOf(intent1);

            Intent expectedIntent = new Intent(context, NotificationReceiver.class);

            assertThat("Broadcast", shadowIntent1.isBroadcastIntent(), is(true));
            assertThat("Intent count", shadowIntent1.getSavedIntents().length, is(1));
            assertThat("Class", shadowIntent1.getSavedIntents()[0].getComponent(), is(expectedIntent.getComponent()));
            assertThat("Action", shadowIntent1.getSavedIntents()[0].getAction(), is(actionString));
        }
    }

    private void assertWidget(int iconResId, String time, String date) {
        assertWidget(context, shadowAppWidgetManager, iconResId, time, date);
    }

    public static void assertWidget(Context context, ShadowAppWidgetManager shadowAppWidgetManager, int iconResId, String time, String date) {
        int widgetId = shadowAppWidgetManager.createWidget(WidgetProvider.class, R.layout.widget_layout);
        View view = shadowAppWidgetManager.getViewFor(widgetId);

        ImageView widgetIcon = (ImageView) view.findViewById(R.id.icon);
        ShadowDrawable shadowWidgetIconDrawable = Shadows.shadowOf(widgetIcon.getDrawable());
        TextView widgetTime = (TextView) view.findViewById(R.id.alarm_time);
        TextView widgetDate = (TextView) view.findViewById(R.id.alarm_date);

        assertThat("Widget icon", shadowWidgetIconDrawable.getCreatedFromResId(), is(iconResId));
        assertThat("Text visibility", widgetTime.getText().toString(), is(time));
        if (date == null) {
            assertThat("Date visibility", widgetDate.getVisibility(), is(View.GONE));
        } else {
            assertThat("Date visibility", widgetDate.getVisibility(), is(View.VISIBLE));
            assertThat("Date text", widgetDate.getText().toString(), is(date));
        }
    }

}
