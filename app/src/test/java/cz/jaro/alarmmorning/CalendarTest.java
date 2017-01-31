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
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import org.junit.After;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.robolectric.Robolectric;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.fakes.RoboMenuItem;
import org.robolectric.shadows.ShadowActivity;
import org.robolectric.shadows.ShadowAlarmManager;
import org.robolectric.shadows.ShadowAppWidgetManager;
import org.robolectric.shadows.ShadowNotificationManager;
import org.robolectric.shadows.ShadowPendingIntent;
import org.robolectric.shadows.ShadowTimePickerDialog;
import org.robolectric.shadows.ShadowViewGroup;

import java.util.Calendar;
import java.util.GregorianCalendar;

import cz.jaro.alarmmorning.clock.FixedClock;
import cz.jaro.alarmmorning.graphics.SlideButton;
import cz.jaro.alarmmorning.model.Day;
import cz.jaro.alarmmorning.model.DayTest;
import cz.jaro.alarmmorning.model.Defaults;
import cz.jaro.alarmmorning.receivers.AlarmReceiver;
import cz.jaro.alarmmorning.receivers.NotificationReceiver;
import cz.jaro.alarmmorning.receivers.VoidReceiver;
import cz.jaro.alarmmorning.shadows.ShadowAlarmManagerAPI21;
import cz.jaro.alarmmorning.shadows.ShadowGlobalManager;
import cz.jaro.alarmmorning.wizard.Wizard;

import static cz.jaro.alarmmorning.model.AlarmDbHelper.DEFAULT_ALARM_HOUR;
import static cz.jaro.alarmmorning.model.AlarmDbHelper.DEFAULT_ALARM_MINUTE;
import static cz.jaro.alarmmorning.model.DayTest.DAY;
import static cz.jaro.alarmmorning.model.DayTest.MONTH;
import static cz.jaro.alarmmorning.model.DayTest.YEAR;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

/**
 * Tests of alarm management in UI.
 */
@Config(constants = BuildConfig.class, sdk = 21, shadows = {ShadowAlarmManagerAPI21.class, ShadowGlobalManager.class})
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class CalendarTest extends FixedTimeTest {

    private Context context;

    private AlarmManager alarmManager;
    private ShadowAlarmManagerAPI21 shadowAlarmManager;

    private NotificationManager notificationManager;
    private ShadowNotificationManager shadowNotificationManager;

    private AppWidgetManager appWidgetManager;
    private ShadowAppWidgetManager shadowAppWidgetManager;

    private Activity activity;
    private ShadowActivity shadowActivity;

    // Items in CalendarFragment of AlarmMorningActivity
    private RecyclerView recyclerView;

    private View item;

    private TextView textDate;
    private TextView textTime;
    private TextView textState;

    // Items in RingActivity
    private TextView textAlarmTime;
    private TextView textNextCalendar;
    private TextView textMuted;

    private ImageButton snoozeButton;
    private SlideButton dismissButton;

    @Before
    public void before() {
        super.before();

        AlarmMorningActivityTest.saveWizardPreference(!Wizard.PREF_WIZARD_DEFAULT);

        context = RuntimeEnvironment.application.getApplicationContext();

        alarmManager = (AlarmManager) RuntimeEnvironment.application.getSystemService(Context.ALARM_SERVICE);
        shadowAlarmManager = (ShadowAlarmManagerAPI21) Shadows.shadowOf(alarmManager);

        notificationManager = (NotificationManager) RuntimeEnvironment.application.getSystemService(Context.NOTIFICATION_SERVICE);
        shadowNotificationManager = Shadows.shadowOf(notificationManager);

        appWidgetManager = AppWidgetManager.getInstance(context);
        shadowAppWidgetManager = Shadows.shadowOf(appWidgetManager);

        AppWidgetProviderInfo appWidgetProviderInfo = new AppWidgetProviderInfo();
        shadowAppWidgetManager.addInstalledProvider(appWidgetProviderInfo);

        globalManager.forceSetAlarm();
    }

    @After
    public void after() {
        super.after();

        // Close ring activity
        if (activity instanceof RingActivity) {
            RingActivity ringActivity = (RingActivity) this.activity;
            ringActivity.shutdown();
        }

        // Cancell all notifications
        notificationManager.cancelAll();
    }

    @Test
    public void t00_prerequisities() {
        assertThat(shadowAppWidgetManager.getInstalledProviders().size(), is(1));
    }

    @Test
    public void t01_noAlarmIsScheduled() {
        // Check system alarm
        assertSystemAlarm(YEAR, MONTH, DAY + 1, 0, 0, SystemAlarm.ACTION_SET_SYSTEM_ALARM);

        // Check system alarm clock
        assertSystemAlarmClockNone();

        // Check notification
        assertThat(shadowNotificationManager.size(), is(0));

        // Check widget
        // TODO Test widget
    }

    @Test
    @Config(qualifiers = "en")
    public void t10_setAlarm() {
        // Consume the alarm with action ACTION_SET_SYSTEM_ALARM
        consumeNextScheduledAlarm();

        // Click in calendar
        startActivityCalendar();
        loadItemAtPosition(0);

//        assertThat(textDate.getText(), is("2/1")); // TODO Fix test
        assertThat(textTime.getText(), is("Off"));
        assertThat(textState.getText(), is(""));

        item.performClick();

        // Click the time picker
        TimePickerFragment fragment = (TimePickerFragment) activity.getFragmentManager().findFragmentByTag("timePicker");

        TimePickerDialog dialog = (TimePickerDialog) fragment.getDialog();
        ShadowTimePickerDialog shadowDialog = Shadows.shadowOf(dialog);

        assertThat(shadowDialog.getHourOfDay(), is(DEFAULT_ALARM_HOUR));
        assertThat(shadowDialog.getMinute(), is(DEFAULT_ALARM_MINUTE));

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick();

        // Check calendar
//        assertThat(textDate.getText(), is("2/1")); // TODO Fix test
//        assertThat(textTime.getText(), is("07:00")); // TODO Fix test
        assertThat(textState.getText(), is("Changed"));

        // Check system alarm
        assertSystemAlarm(YEAR, MONTH, DAY, DEFAULT_ALARM_HOUR - 2, DEFAULT_ALARM_MINUTE, SystemAlarm.ACTION_RING_IN_NEAR_FUTURE);

        // Check system alarm clock
        assertSystemAlarmClock(YEAR, MONTH, DAY, DEFAULT_ALARM_HOUR, DEFAULT_ALARM_MINUTE);

        // Check notification
        assertThat(shadowNotificationManager.size(), is(0));
    }

    @Test
    @Config(qualifiers = "en")
    public void t11_setAlarmTwice() {
        // Consume the alarm with action ACTION_SET_SYSTEM_ALARM
        consumeNextScheduledAlarm();

        // Click in calendar
        startActivityCalendar();
        loadItemAtPosition(0);

        // 1st: set
        item.performClick();

        // Click the time picker
        TimePickerFragment fragment = (TimePickerFragment) activity.getFragmentManager().findFragmentByTag("timePicker");

        TimePickerDialog dialog = (TimePickerDialog) fragment.getDialog();
        ShadowTimePickerDialog shadowDialog = Shadows.shadowOf(dialog);

//        assertThat(textDate.getText(), is("2/1")); // TODO Fix test
        assertThat(shadowDialog.getHourOfDay(), is(DEFAULT_ALARM_HOUR));
        assertThat(shadowDialog.getMinute(), is(DEFAULT_ALARM_MINUTE));

        dialog.updateTime(DEFAULT_ALARM_HOUR + 1, DEFAULT_ALARM_MINUTE + 1);

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick();

        // 2nd: set
        item.performClick();

        // Click the time picker
        fragment = (TimePickerFragment) activity.getFragmentManager().findFragmentByTag("timePicker");

        dialog = (TimePickerDialog) fragment.getDialog();
        shadowDialog = Shadows.shadowOf(dialog);

//        assertThat(textDate.getText(), is("2/1")); // TODO Fix test
        assertThat(shadowDialog.getHourOfDay(), is(DEFAULT_ALARM_HOUR + 1));
        assertThat(shadowDialog.getMinute(), is(DEFAULT_ALARM_MINUTE + 1));

        dialog.updateTime(DEFAULT_ALARM_HOUR + 2, DEFAULT_ALARM_MINUTE + 2);

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick();

        // Check calendar
//        assertThat(textDate.getText(), is("2/1")); // TODO Fix test
//        assertThat(textTime.getText(), is("09:02")); // TODO Fix test
        assertThat(textState.getText(), is("Changed"));

        // Check system alarm
        assertSystemAlarm(YEAR, MONTH, DAY, DEFAULT_ALARM_HOUR, DEFAULT_ALARM_MINUTE + 2, SystemAlarm.ACTION_RING_IN_NEAR_FUTURE);

        // Check system alarm clock
        assertSystemAlarmClock(YEAR, MONTH, DAY, DEFAULT_ALARM_HOUR + 2, DEFAULT_ALARM_MINUTE + 2);

        // Check notification
        assertThat(shadowNotificationManager.size(), is(0));
    }

    @Test
    @Config(qualifiers = "en")
    public void t12_setAlarmToPast() {
        // Consume the alarm with action ACTION_SET_SYSTEM_ALARM
        consumeNextScheduledAlarm();

        // Click in calendar
        startActivityCalendar();
        loadItemAtPosition(0);
        item.performClick();

        // Click the time picker
        TimePickerFragment fragment = (TimePickerFragment) activity.getFragmentManager().findFragmentByTag("timePicker");

        TimePickerDialog dialog = (TimePickerDialog) fragment.getDialog();
        ShadowTimePickerDialog shadowDialog = Shadows.shadowOf(dialog);

        assertThat(shadowDialog.getHourOfDay(), is(DEFAULT_ALARM_HOUR));
        assertThat(shadowDialog.getMinute(), is(DEFAULT_ALARM_MINUTE));

        Calendar now = globalManager.clock().now();
        now.add(Calendar.MINUTE, -1);
        dialog.updateTime(now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE));

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick();

        // Check calendar
//        assertThat(textDate.getText(), is("2/1")); // TODO Fix test
//        assertThat(textTime.getText(), is("00:59")); // TODO Fix test
        assertThat(textState.getText(), is("Passed"));

        // Check system alarm
        assertThat(shadowAlarmManager.getScheduledAlarms().size(), is(0));
        // The alarm that was consumed at the beginning of this method didn't change

        // Check system alarm clock
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            AlarmManager.AlarmClockInfo alarmClockInfo = alarmManager.getNextAlarmClock();
            assertNull(alarmClockInfo);
        }

        // Check notification
        assertThat(shadowNotificationManager.size(), is(0));
    }

    @Test
    @Config(qualifiers = "en")
    public void t13_setAlarmWithZeroAdvancePeriod() {
        // Set the preference to zero
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt(SettingsActivity.PREF_NEAR_FUTURE_TIME, 0);
        editor.commit();

        // Consume the alarm with action ACTION_SET_SYSTEM_ALARM
        consumeNextScheduledAlarm();

        // Click in calendar
        startActivityCalendar();
        loadItemAtPosition(0);
        item.performClick();

        // Click the time picker
        TimePickerFragment fragment = (TimePickerFragment) activity.getFragmentManager().findFragmentByTag("timePicker");

        TimePickerDialog dialog = (TimePickerDialog) fragment.getDialog();
        ShadowTimePickerDialog shadowDialog = Shadows.shadowOf(dialog);

        assertThat(shadowDialog.getHourOfDay(), is(DEFAULT_ALARM_HOUR));
        assertThat(shadowDialog.getMinute(), is(DEFAULT_ALARM_MINUTE));

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick();

        // Check calendar
//        assertThat(textDate.getText(), is("2/1")); // TODO Fix test
//        assertThat(textTime.getText(), is("07:00")); // TODO Fix test
        assertThat(textState.getText(), is("Changed"));

        // Check system alarm
        assertSystemAlarm(YEAR, MONTH, DAY, DEFAULT_ALARM_HOUR, DEFAULT_ALARM_MINUTE, SystemAlarm.ACTION_RING);

        // Check system alarm clock
        assertSystemAlarmClock(YEAR, MONTH, DAY, DEFAULT_ALARM_HOUR, DEFAULT_ALARM_MINUTE);

        // Check notification
        assertThat(shadowNotificationManager.size(), is(0));
    }

    @Test
    @Config(qualifiers = "en")
    public void t14_setAlarmTomorrow() {
        // Consume the alarm with action ACTION_SET_SYSTEM_ALARM
        consumeNextScheduledAlarm();

        // Click in calendar
        startActivityCalendar();
        loadItemAtPosition(1);
        item.performClick();

        // Click the time picker
        TimePickerFragment fragment = (TimePickerFragment) activity.getFragmentManager().findFragmentByTag("timePicker");

        TimePickerDialog dialog = (TimePickerDialog) fragment.getDialog();
        ShadowTimePickerDialog shadowDialog = Shadows.shadowOf(dialog);

        assertThat(shadowDialog.getHourOfDay(), is(DEFAULT_ALARM_HOUR));
        assertThat(shadowDialog.getMinute(), is(DEFAULT_ALARM_MINUTE));

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick();

        // Check calendar
//        assertThat(textDate.getText(), is("2/1")); // TODO Fix test
//        assertThat(textTime.getText(), is("07:00")); // TODO Fix test
        assertThat(textState.getText(), is("Changed"));

        // Check system alarm
        assertSystemAlarm(YEAR, MONTH, DAY + 1, DEFAULT_ALARM_HOUR - 2, DEFAULT_ALARM_MINUTE, SystemAlarm.ACTION_RING_IN_NEAR_FUTURE);

        // Check system alarm clock
        assertSystemAlarmClock(YEAR, MONTH, DAY + 1, DEFAULT_ALARM_HOUR, DEFAULT_ALARM_MINUTE);

        // Check notification
        assertThat(shadowNotificationManager.size(), is(0));
    }

    @Test
    public void t20_onNear() {
        // Consume the alarm with action ACTION_SET_SYSTEM_ALARM
        consumeNextScheduledAlarm();

        // Save day
        setAlarmToToday();

        // Consume the alarm with action ACTION_RING_IN_NEAR_FUTURE
        consumeNextScheduledAlarm();

        // Shift clock
        shadowGlobalManager.setClock(new FixedClock(new GregorianCalendar(YEAR, MONTH, DAY, DayTest.HOUR_DEFAULT - 2, DayTest.MINUTE_DEFAULT)));
        globalManager.onNearFuture();

        // Call the receiver
        Intent intent = new Intent();
        intent.setAction(SystemAlarm.ACTION_RING_IN_NEAR_FUTURE);
        AlarmReceiver alarmReceiver = new AlarmReceiver();
        alarmReceiver.onReceive(context, intent);

        // Check system alarm
        assertSystemAlarm(YEAR, MONTH, DAY, DEFAULT_ALARM_HOUR, DEFAULT_ALARM_MINUTE, SystemAlarm.ACTION_RING);

        // Check system alarm clock
        assertSystemAlarmClock(YEAR, MONTH, DAY, DEFAULT_ALARM_HOUR, DEFAULT_ALARM_MINUTE);

        // Check notification
        // TODO Fails when run with other test (fine when run as the only test)
//        assertThat(shadowNotificationManager.size(), is(1));
//
//        Notification notification = shadowNotificationManager.getAllNotifications().get(0);
//        ShadowNotification shadowNotification = Shadows.shadowOf(notification);
//
//        //assertThat(shadowNotification.getBigContentTitle(), is("Alarm at 7:00")); // TODO Fix test
//        assertThat(shadowNotification.getBigContentText(), is("Touch to view all alarms"));
//        assertNotificationActionCount(notification, 1);
//        assertNotificationAction(notification, 0, "Dismiss", NotificationReceiver.ACTION_DISMISS_BEFORE_RINGING);
    }

    @Test
    @Config(qualifiers = "en")
    public void t21_dismissBeforeRinging() {
        // Consume the alarm with action ACTION_SET_SYSTEM_ALARM
        consumeNextScheduledAlarm();

        // Save day
        setAlarmToToday();

        // Consume the alarm with action ACTION_RING_IN_NEAR_FUTURE
        consumeNextScheduledAlarm();

        // Shift clock
        shadowGlobalManager.setClock(new FixedClock(new GregorianCalendar(YEAR, MONTH, DAY, DayTest.HOUR_DEFAULT - 2, DayTest.MINUTE_DEFAULT, 1))); // 1 second after near time
        globalManager.onNearFuture();

        // Consume the alarm with action ACTION_RING
        consumeNextScheduledAlarm();

        // Click in calendar
        startActivityCalendar();
        loadItemAtPosition(0);
        item.performLongClick();

        // TODO Test that the context menu contains the "Dismiss" item (not yet easily supported by Roboletric)

        // Click context menu
        clickContextMenu(R.id.day_dismiss);

        // Check calendar
//        assertThat(textDate.getText(), is("2/1")); // TODO Fix test
//        assertThat(textTime.getText(), is("07:00")); // TODO Fix test
        assertThat(textState.getText(), is("Dismissed before ringing"));

        // Check system alarm
        assertSystemAlarm(YEAR, MONTH, DAY, DEFAULT_ALARM_HOUR, DEFAULT_ALARM_MINUTE, SystemAlarm.ACTION_ALARM_TIME_OF_EARLY_DISMISSED_ALARM);

        // Check system alarm clock
        assertSystemAlarmClockNone();

        // Check notification
        assertThat(shadowNotificationManager.size(), is(0));
    }

    @Test
    public void t30_onRing() {
        // Consume the alarm with action ACTION_SET_SYSTEM_ALARM
        consumeNextScheduledAlarm();

        // Save day
        setAlarmToToday();

        // Consume the alarm with action ACTION_RING_IN_NEAR_FUTURE
        consumeNextScheduledAlarm();

        // Shift clock - must go through onNearFuture() as it calls globalManager.setNextAction()
        shadowGlobalManager.setClock(new FixedClock(new GregorianCalendar(YEAR, MONTH, DAY, DayTest.HOUR_DEFAULT - 2, DayTest.MINUTE_DEFAULT)));
        globalManager.onNearFuture(); // FIXME this should NOT be necessary. Create test with zero advance period (in which case the onNearFuture() is not called

        // Consume the alarm with action ACTION_RING
        consumeNextScheduledAlarm();

        // Shift clock
        shadowGlobalManager.setClock(new FixedClock(new GregorianCalendar(YEAR, MONTH, DAY, DayTest.HOUR_DEFAULT, DayTest.MINUTE_DEFAULT)));

        Activity activity = Robolectric.setupActivity(Activity.class);
        ShadowActivity shadowActivity = Shadows.shadowOf(activity);

        // Call the receiver
        Intent intent = new Intent();
        intent.setAction(SystemAlarm.ACTION_RING);
        AlarmReceiver alarmReceiver = new AlarmReceiver();
        alarmReceiver.onReceive(context, intent);

        // Check that ringing started
        Intent intentNext = shadowActivity.peekNextStartedActivity();
        Intent expectedIntentNext = new Intent(context, RingActivity.class);

        assertThat(intentNext.getComponent(), is(expectedIntentNext.getComponent()));

        // Check system alarm
        assertSystemAlarm(YEAR, MONTH, DAY + 1, 0, 0, SystemAlarm.ACTION_SET_SYSTEM_ALARM);

        // Check system alarm clock
        assertSystemAlarmClockNone();

        // Check notification
        // TODO Fails when run with other test (fine when run as the only test)
//        assertThat(shadowNotificationManager.size(), is(1));
//
//        Notification notification = shadowNotificationManager.getAllNotifications().get(0);
//        ShadowNotification shadowNotification = Shadows.shadowOf(notification);
//
////        assertThat(shadowNotification.getBigContentTitle(), is("Alarm at 7:00")); // TODO Fix test
//        assertThat(shadowNotification.getBigContentText(), is("Ringing"));
//        assertNotificationActionCount(notification, 2);
//        assertNotificationAction(notification, 0, "Dismiss", NotificationReceiver.ACTION_DISMISS);
//        assertNotificationAction(notification, 1, "Snooze", NotificationReceiver.ACTION_SNOOZE);

        // Start ring activity
        Calendar alarmTime = new GregorianCalendar(YEAR, MONTH, DAY, DayTest.HOUR_DEFAULT, DayTest.MINUTE_DEFAULT);
        startActivityRing(alarmTime);

        // Check appearance
        assertThat(textDate.getVisibility(), is(View.VISIBLE));
        assertThat(textTime.getVisibility(), is(View.VISIBLE));
        assertThat(textAlarmTime.getVisibility(), is(View.INVISIBLE));
        assertThat(textNextCalendar.getVisibility(), is(View.GONE));
        assertThat(textMuted.getVisibility(), is(View.INVISIBLE));

//        assertThat(textDate.getText(), is("Monday, February 1")); // TODO Fix test
//        assertThat(textTime.getText(), is("07:00")); // TODO Fix test
    }

    @Test
    public void t31_onRingWithTomorrow() {
        // Consume the alarm with action ACTION_SET_SYSTEM_ALARM
        consumeNextScheduledAlarm();

        // Save day
        setAlarmToToday();
        setAlarmToTomorrow();

        // Consume the alarm with action ACTION_RING_IN_NEAR_FUTURE
        consumeNextScheduledAlarm();

        // Shift clock - must go through onNearFuture() as it calls globalManager.setNextAction()
        shadowGlobalManager.setClock(new FixedClock(new GregorianCalendar(YEAR, MONTH, DAY, DayTest.HOUR_DEFAULT - 2, DayTest.MINUTE_DEFAULT)));
        globalManager.onNearFuture(); // FIXME this should NOT be necessary. Create test with zero advance period (in which case the onNearFuture() is not called

        // Consume the alarm with action ACTION_RING
        consumeNextScheduledAlarm();

        // Shift clock
        shadowGlobalManager.setClock(new FixedClock(new GregorianCalendar(YEAR, MONTH, DAY, DayTest.HOUR_DEFAULT, DayTest.MINUTE_DEFAULT, 1))); // 1 second after alarm
        globalManager.onRing();

        Activity activity = Robolectric.setupActivity(Activity.class);
        ShadowActivity shadowActivity = Shadows.shadowOf(activity);

        // Call the receiver
        Intent intent = new Intent();
        intent.setAction(SystemAlarm.ACTION_RING);
        AlarmReceiver alarmReceiver = new AlarmReceiver();
        alarmReceiver.onReceive(context, intent);

        // Check that ringing started
        Intent intentNext = shadowActivity.peekNextStartedActivity();
        Intent expectedIntentNext = new Intent(context, RingActivity.class);

        assertThat(intentNext.getComponent(), is(expectedIntentNext.getComponent()));

        // Check system alarm
        assertSystemAlarm(YEAR, MONTH, DAY + 1, DEFAULT_ALARM_HOUR - 1, DEFAULT_ALARM_MINUTE + 1, SystemAlarm.ACTION_RING_IN_NEAR_FUTURE);

        // Check system alarm clock
        assertSystemAlarmClock(YEAR, MONTH, DAY + 1, DEFAULT_ALARM_HOUR + 1, DEFAULT_ALARM_MINUTE + 1);

        // Check notification
        // TODO Fails when run with other test (fine when run as the only test)
//        assertThat(shadowNotificationManager.size(), is(1));
//
//        Notification notification = shadowNotificationManager.getAllNotifications().get(0);
//        ShadowNotification shadowNotification = Shadows.shadowOf(notification);
//
////        assertThat(shadowNotification.getBigContentTitle(), is("Alarm at 7:00")); // TODO Fix test
//        assertThat(shadowNotification.getBigContentText(), is("Ringing"));
//        assertNotificationActionCount(notification, 2);
//        assertNotificationAction(notification, 0, "Dismiss", NotificationReceiver.ACTION_DISMISS);
//        assertNotificationAction(notification, 1, "Snooze", NotificationReceiver.ACTION_SNOOZE);
    }

    @Test
    @Config(qualifiers = "en")
    public void t32_dismissWhileRinging() {
        // Consume the alarm with action ACTION_SET_SYSTEM_ALARM
        consumeNextScheduledAlarm();

        // Save day
        setAlarmToToday();

        // Consume the alarm with action ACTION_RING_IN_NEAR_FUTURE
        consumeNextScheduledAlarm();

        // Shift clock - must go through onNearFuture() as it calls globalManager.setNextAction()
        shadowGlobalManager.setClock(new FixedClock(new GregorianCalendar(YEAR, MONTH, DAY, DayTest.HOUR_DEFAULT - 2, DayTest.MINUTE_DEFAULT)));
        globalManager.onNearFuture(); // FIXME this should NOT be necessary. Create test with zero advance period (in which case the onNearFuture() is not called

        // Consume the alarm with action ACTION_RING
        consumeNextScheduledAlarm();

        // Shift clock
        shadowGlobalManager.setClock(new FixedClock(new GregorianCalendar(YEAR, MONTH, DAY, DayTest.HOUR_DEFAULT, DayTest.MINUTE_DEFAULT, 1))); // 1 second after alarm
        globalManager.onRing();

        Calendar alarmTime = new GregorianCalendar(YEAR, MONTH, DAY, DayTest.HOUR_DEFAULT, DayTest.MINUTE_DEFAULT);
        startActivityRing(alarmTime);

        dismissButton.performClick();

        // Check system alarm
        assertSystemAlarm(YEAR, MONTH, DAY + 1, 0, 0, SystemAlarm.ACTION_SET_SYSTEM_ALARM);

        // Check system alarm clock
        assertSystemAlarmClockNone();

        // Check notification
        assertThat(shadowNotificationManager.size(), is(0));
    }

    @Test
    @Config(qualifiers = "en")
    public void t33_snoozeWhileRinging() {
        // Consume the alarm with action ACTION_SET_SYSTEM_ALARM
        consumeNextScheduledAlarm();

        // Save day
        setAlarmToToday();

        // Consume the alarm with action ACTION_RING_IN_NEAR_FUTURE
        consumeNextScheduledAlarm();

        // Shift clock - must go through onNearFuture() as it calls globalManager.setNextAction()
        shadowGlobalManager.setClock(new FixedClock(new GregorianCalendar(YEAR, MONTH, DAY, DayTest.HOUR_DEFAULT - 2, DayTest.MINUTE_DEFAULT)));
        globalManager.onNearFuture(); // FIXME this should NOT be necessary. Create test with zero advance period (in which case the onNearFuture() is not called

        // Consume the alarm with action ACTION_RING
        consumeNextScheduledAlarm();

        // Shift clock
        shadowGlobalManager.setClock(new FixedClock(new GregorianCalendar(YEAR, MONTH, DAY, DayTest.HOUR_DEFAULT, DayTest.MINUTE_DEFAULT, 1))); // 1 second after alarm
        globalManager.onRing();

        // Consume the alarm with action ACTION_SET_SYSTEM_ALARM
        consumeNextScheduledAlarm();

        // Start ring activity
        Calendar alarmTime = new GregorianCalendar(YEAR, MONTH, DAY, DayTest.HOUR_DEFAULT, DayTest.MINUTE_DEFAULT);
        startActivityRing(alarmTime);

        snoozeButton.performClick();

        // Check system alarm
        assertSystemAlarm(YEAR, MONTH, DAY, DEFAULT_ALARM_HOUR, DEFAULT_ALARM_MINUTE + 10, SystemAlarm.ACTION_RING);

        // Check system alarm clock
        assertSystemAlarmClockNone();

        // Check notification
        // TODO Fails when run with other test (fine when run as the only test)
//        assertThat(shadowNotificationManager.size(), is(1));
//
//        Notification notification = shadowNotificationManager.getAllNotifications().get(0);
//        ShadowNotification shadowNotification = Shadows.shadowOf(notification);
//
////        assertThat(shadowNotification.getBigContentTitle(), is("Alarm at 7:00")); // TODO Fix test
////        assertThat(shadowNotification.getBigContentText(), is("Snoozed till 07:10"));  // TODO Fix test
//        assertNotificationActionCount(notification, 1);
//        assertNotificationAction(notification, 0, "Dismiss", NotificationReceiver.ACTION_DISMISS);
    }

    @Test
    public void t40_dismissWhileSnoozed() {
        // Consume the alarm with action ACTION_SET_SYSTEM_ALARM
        consumeNextScheduledAlarm();

        // Save day
        setAlarmToToday();

        // Consume the alarm with action ACTION_RING_IN_NEAR_FUTURE
        consumeNextScheduledAlarm();

        // Shift clock - must go through onNearFuture() as it calls globalManager.setNextAction()
        shadowGlobalManager.setClock(new FixedClock(new GregorianCalendar(YEAR, MONTH, DAY, DayTest.HOUR_DEFAULT - 2, DayTest.MINUTE_DEFAULT)));
        globalManager.onNearFuture(); // FIXME this should NOT be necessary. Create test with zero advance period (in which case the onNearFuture() is not called

        // Consume the alarm with action ACTION_RING
        consumeNextScheduledAlarm();

        // Shift clock - must go through onNearFuture() as it calls globalManager.setNextAction()
        shadowGlobalManager.setClock(new FixedClock(new GregorianCalendar(YEAR, MONTH, DAY, DayTest.HOUR_DEFAULT, DayTest.MINUTE_DEFAULT)));
        globalManager.onRing();

        // Consume the alarm with action ACTION_RING
        consumeNextScheduledAlarm();

        // Shift clock
        shadowGlobalManager.setClock(new FixedClock(new GregorianCalendar(YEAR, MONTH, DAY, DayTest.HOUR_DEFAULT, DayTest.MINUTE_DEFAULT, 1))); // 1 second after alarm

        Analytics analytics = new Analytics(Analytics.Channel.Test, Analytics.ChannelName.Calendar);
        globalManager.onSnooze(analytics);

        // Consume the alarm with action ACTION_RING
        consumeNextScheduledAlarm();

        // Click in calendar
        startActivityCalendar();
        loadItemAtPosition(0);
        item.performLongClick();

        // TODO Test that the context menu contains the "Dismiss" item (not yet easily supported by Roboletric)

        // Click context menu
        clickContextMenu(R.id.day_dismiss);

        // Check calendar
//        assertThat(textDate.getText(), is("2/1")); // TODO Fix test
//        assertThat(textTime.getText(), is("07:00")); // TODO Fix test
        assertThat(textState.getText(), is("Passed"));

        // Check system alarm
        assertSystemAlarm(YEAR, MONTH, DAY + 1, 0, 0, SystemAlarm.ACTION_SET_SYSTEM_ALARM);

        // Check system alarm clock
        assertSystemAlarmClockNone();

        // Check notification
        assertThat(shadowNotificationManager.size(), is(0));
    }

    @Test
    public void t41_setTimeWhileSnoozed() {
        // Consume the alarm with action ACTION_SET_SYSTEM_ALARM
        consumeNextScheduledAlarm();

        // Save day
        setAlarmToToday();

        // Consume the alarm with action ACTION_RING_IN_NEAR_FUTURE
        consumeNextScheduledAlarm();

        // Shift clock
        shadowGlobalManager.setClock(new FixedClock(new GregorianCalendar(YEAR, MONTH, DAY, DayTest.HOUR_DEFAULT, DayTest.MINUTE_DEFAULT, 1))); // 1 second after alarm

        // We must some internal variables about the current alarm before calling onSnooze()
        Calendar alarmTime = new GregorianCalendar(YEAR, MONTH, DAY, DayTest.HOUR_DEFAULT, DayTest.MINUTE_DEFAULT);
        globalManager.setState(GlobalManager.STATE_RINGING, alarmTime);

        Analytics analytics = new Analytics(Analytics.Channel.Test, Analytics.ChannelName.Calendar);
        globalManager.onSnooze(analytics);

        // Consume the alarm with action ACTION_RING
        consumeNextScheduledAlarm();

        // Click in calendar
        startActivityCalendar();
        loadItemAtPosition(0);
        item.performLongClick();

        // TODO Test that the context menu contains the "Set time" item (not yet easily supported by Roboletric)

        // Click context menu
        clickContextMenu(R.id.day_set_time);

        // Click the time picker
        TimePickerFragment fragment = (TimePickerFragment) activity.getFragmentManager().findFragmentByTag("timePicker");

        TimePickerDialog dialog = (TimePickerDialog) fragment.getDialog();
        ShadowTimePickerDialog shadowDialog = Shadows.shadowOf(dialog);

        dialog.updateTime(DEFAULT_ALARM_HOUR + 6, DEFAULT_ALARM_MINUTE);
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick();

        // Check calendar
//        assertThat(textDate.getText(), is("2/1")); // TODO Fix test
//        assertThat(textTime.getText(), is("07:00")); // TODO Fix test
        assertThat(textState.getText(), is("Changed"));

        // Check system alarm
        assertSystemAlarm(YEAR, MONTH, DAY, DEFAULT_ALARM_HOUR + 4, DEFAULT_ALARM_MINUTE, SystemAlarm.ACTION_RING_IN_NEAR_FUTURE);

        // Check system alarm clock
        assertSystemAlarmClock(YEAR, MONTH, DAY, DEFAULT_ALARM_HOUR + 6, DEFAULT_ALARM_MINUTE);

        // Check notification
        assertThat(shadowNotificationManager.size(), is(0));
    }

    /**
     * Note: this action is not possible in UI.
     */
    @Test
    public void t42_revertWhileSnoozed() {
        // Consume the alarm with action ACTION_SET_SYSTEM_ALARM
        consumeNextScheduledAlarm();

        // Save day
        setAlarmToToday();

        // Consume the alarm with action ACTION_RING_IN_NEAR_FUTURE
        consumeNextScheduledAlarm();

        // Shift clock
        shadowGlobalManager.setClock(new FixedClock(new GregorianCalendar(YEAR, MONTH, DAY, DayTest.HOUR_DEFAULT, DayTest.MINUTE_DEFAULT, 1))); // 1 second after alarm

        // We must some internal variables about the current alarm before calling onSnooze()
        Calendar alarmTime = new GregorianCalendar(YEAR, MONTH, DAY, DayTest.HOUR_DEFAULT, DayTest.MINUTE_DEFAULT);
        globalManager.setState(GlobalManager.STATE_RINGING, alarmTime);

        Analytics analytics = new Analytics(Analytics.Channel.Test, Analytics.ChannelName.Calendar);
        globalManager.onSnooze(analytics);

        // Consume the alarm with action ACTION_RING
        consumeNextScheduledAlarm();

        // Click in calendar
        startActivityCalendar();
        loadItemAtPosition(0);
        item.performLongClick();

        // TODO Test that the context menu contains the "Revert" item (not yet easily supported by Roboletric)

        // Click context menu
        clickContextMenu(R.id.day_revert);

        // Check calendar
//        assertThat(textDate.getText(), is("2/1")); // TODO Fix test
//        assertThat(textTime.getText(), is("07:00")); // TODO Fix test
//        assertThat(textState.getText(), is("Passed"));

        // Check system alarm
        assertSystemAlarm(YEAR, MONTH, DAY + 1, 0, 0, SystemAlarm.ACTION_SET_SYSTEM_ALARM);

        // Check system alarm clock
        assertSystemAlarmClockNone();

        // Check notification
        assertThat(shadowNotificationManager.size(), is(0));
    }

    /**
     * Note: this action is not possible in UI.
     */
    @Test
    public void t43_disableWhileSnoozed() {
        // Consume the alarm with action ACTION_SET_SYSTEM_ALARM
        consumeNextScheduledAlarm();

        // Save day
        setAlarmToToday();

        // Consume the alarm with action ACTION_RING_IN_NEAR_FUTURE
        consumeNextScheduledAlarm();

        // Shift clock
        shadowGlobalManager.setClock(new FixedClock(new GregorianCalendar(YEAR, MONTH, DAY, DayTest.HOUR_DEFAULT, DayTest.MINUTE_DEFAULT, 1))); // 1 second after alarm

        // We must some internal variables about the current alarm before calling onSnooze()
        Calendar alarmTime = new GregorianCalendar(YEAR, MONTH, DAY, DayTest.HOUR_DEFAULT, DayTest.MINUTE_DEFAULT);
        globalManager.setState(GlobalManager.STATE_RINGING, alarmTime);

        Analytics analytics = new Analytics(Analytics.Channel.Test, Analytics.ChannelName.Calendar);
        globalManager.onSnooze(analytics);

        // Consume the alarm with action ACTION_RING
        consumeNextScheduledAlarm();

        // Click in calendar
        startActivityCalendar();
        loadItemAtPosition(0);
        item.performLongClick();

        // TODO Test that the context menu contains the "Disable" item (not yet easily supported by Roboletric)

        // Click context menu
        clickContextMenu(R.id.day_disable);

        // Check system alarm
        assertSystemAlarm(YEAR, MONTH, DAY + 1, 0, 0, SystemAlarm.ACTION_SET_SYSTEM_ALARM);

        // Check system alarm clock
        assertSystemAlarmClockNone();

        // Check notification
        assertThat(shadowNotificationManager.size(), is(0));
    }

    private void consumeNextScheduledAlarm() {
        assertThat(shadowAlarmManager.getScheduledAlarms().size(), is(1));
        shadowAlarmManager.getNextScheduledAlarm();
    }

    private void setAlarmToToday() {
        Calendar date = new GregorianCalendar(YEAR, MONTH, DAY, DayTest.HOUR_DEFAULT, DayTest.MINUTE_DEFAULT);
        setAlarm(date);
    }

    private void setAlarmToTomorrow() {
        Calendar date = new GregorianCalendar(YEAR, MONTH, DAY + 1, DayTest.HOUR_DEFAULT + 1, DayTest.MINUTE_DEFAULT + 1);
        setAlarm(date);
    }

    private void setAlarm(Calendar date) {
        Day day = new Day();
        day.setDate(date);
        day.setState(Day.STATE_ENABLED);
        day.setHour(date.get(Calendar.HOUR_OF_DAY));
        day.setMinute(date.get(Calendar.MINUTE));

        Defaults defaults = new Defaults();
        int dayOfWeek = date.get(Calendar.DAY_OF_WEEK);
        defaults.setDayOfWeek(dayOfWeek);
        defaults.setState(Defaults.STATE_DISABLED);

        day.setDefaults(defaults);

        Analytics analytics = new Analytics(Analytics.Channel.Test, Analytics.ChannelName.Calendar);

        globalManager.saveDay(day, analytics);
    }

    private void startActivityRing(Calendar alarmTime) {
        Intent ringIntent = new Intent(context, RingActivity.class);
        ringIntent.putExtra(RingActivity.ALARM_TIME, alarmTime);

        activity = Robolectric.buildActivity(RingActivity.class).withIntent(ringIntent).setup().get();
        shadowActivity = Shadows.shadowOf(activity);

        AlarmMorningActivityTest.setLocale(activity, "en", "US");

        textDate = (TextView) activity.findViewById(R.id.date);
        textTime = (TextView) activity.findViewById(R.id.time);
        textAlarmTime = (TextView) activity.findViewById(R.id.alarmTime);
        textNextCalendar = (TextView) activity.findViewById(R.id.nextCalendar);
        textMuted = (TextView) activity.findViewById(R.id.muted);

        snoozeButton = (ImageButton) activity.findViewById(R.id.snoozeButton);
        dismissButton = (SlideButton) activity.findViewById(R.id.dismissButton);
    }

    private void startActivityCalendar() {
        activity = Robolectric.setupActivity(AlarmMorningActivity.class);
        shadowActivity = Shadows.shadowOf(activity);

        AlarmMorningActivityTest.setLocale(activity, "en", "US");

        recyclerView = (RecyclerView) shadowActivity.findViewById(R.id.calendar_recycler_view);

        // Hack: RecyclerView needs to be measured and layed out manually in Robolectric.
        // Source: http://stackoverflow.com/questions/27052866/android-robolectric-click-recyclerview-item
        recyclerView.measure(0, 0);
        recyclerView.layout(0, 0, 100, 10000);
    }

    private void loadItemAtPosition(int position) {
        item = recyclerView.getChildAt(position);

        textDate = (TextView) item.findViewById(R.id.textDate);
        textTime = (TextView) item.findViewById(R.id.textTimeCal);
        textState = (TextView) item.findViewById(R.id.textState);
    }

    private void clickItem() {
        item.performClick();
    }

    private void clickContextMenu(int id) {
        ShadowViewGroup shadowViewGroup = Shadows.shadowOf(recyclerView);
        android.app.Fragment calendarFragment = (CalendarFragment) shadowViewGroup.getOnCreateContextMenuListener();
        final RoboMenuItem contextMenuItem = new RoboMenuItem(id);
        calendarFragment.onContextItemSelected(contextMenuItem);
    }

    private void assertSystemAlarm(int year, int month, int day, int hour, int minute, String action) {
        assertThat("Alarm count", shadowAlarmManager.getScheduledAlarms().size(), is(1));

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
        ShadowPendingIntent shadowOperation = Shadows.shadowOf(operation);

        Intent expectedIntent = new Intent(context, AlarmReceiver.class);

        assertThat("Broadcast", shadowOperation.isBroadcastIntent(), is(true));
        assertThat("Intent count", shadowOperation.getSavedIntents().length, is(1));
        assertThat("Class", shadowOperation.getSavedIntents()[0].getComponent(), is(expectedIntent.getComponent()));
        assertThat("Action", shadowOperation.getSavedIntent().getAction(), is(action));
    }

    private void assertSystemAlarmClock(int year, int month, int day, int hour, int minute) {
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
            PendingIntent operation = shadowAlarmManager.getNextAlarmClockOperation();
            ShadowPendingIntent shadowIntent = Shadows.shadowOf(operation);

            Intent expectedIntent = new Intent(context, VoidReceiver.class);

            assertThat("Broadcast", shadowIntent.isBroadcastIntent(), is(true));
            assertThat("Intent count", shadowIntent.getSavedIntents().length, is(1));
            assertThat("Class", shadowIntent.getSavedIntents()[0].getComponent(), is(expectedIntent.getComponent()));
            assertNull("Action", shadowIntent.getSavedIntent().getAction());
        }
    }

    private void assertSystemAlarmClockNone() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            AlarmManager.AlarmClockInfo alarmClockInfo = alarmManager.getNextAlarmClock();
            assertNull(alarmClockInfo);
        }
    }

    private void assertNotificationActionCount(Notification notification, int count) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
            assertThat(notification.actions.length, is(count));
        }
    }

    private void assertNotificationAction(Notification notification, int index, String title, String actionString) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
            Notification.Action actionButton = notification.actions[index];
            assertThat(actionButton.title, is(title));

            PendingIntent intent1 = actionButton.actionIntent;
            ShadowPendingIntent shadowIntent1 = Shadows.shadowOf(intent1);

            Intent expectedIntent = new Intent(context, NotificationReceiver.class);

            assertThat("Broadcast", shadowIntent1.isBroadcastIntent(), is(true));
            assertThat("Intent count", shadowIntent1.getSavedIntents().length, is(1));
            assertThat("Class", shadowIntent1.getSavedIntents()[0].getComponent(), is(expectedIntent.getComponent()));
            assertThat("Action", shadowIntent1.getSavedIntents()[0].getAction(), is(actionString));
        }
    }

}
