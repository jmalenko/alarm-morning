package cz.jaro.alarmmorning;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.TimePickerDialog;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
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
import org.robolectric.shadows.ShadowAppWidgetManager;
import org.robolectric.shadows.ShadowDatePickerDialog;
import org.robolectric.shadows.ShadowNotificationManager;
import org.robolectric.shadows.ShadowTimePickerDialog;
import org.robolectric.shadows.ShadowViewGroup;

import java.util.Calendar;
import java.util.GregorianCalendar;

import cz.jaro.alarmmorning.clock.FixedClock;
import cz.jaro.alarmmorning.graphics.SlideButton;
import cz.jaro.alarmmorning.model.DayTest;
import cz.jaro.alarmmorning.model.OneTimeAlarm;
import cz.jaro.alarmmorning.receivers.AlarmReceiver;
import cz.jaro.alarmmorning.shadows.ShadowAlarmManagerAPI21;
import cz.jaro.alarmmorning.shadows.ShadowGlobalManager;
import cz.jaro.alarmmorning.wizard.Wizard;

import static cz.jaro.alarmmorning.model.DayTest.DAY;
import static cz.jaro.alarmmorning.model.DayTest.MONTH;
import static cz.jaro.alarmmorning.model.DayTest.YEAR;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.robolectric.Robolectric.buildActivity;

/**
 * Tests of alarm management in UI such that only one one-time alarm is used.
 */
@Config(constants = BuildConfig.class, sdk = 21, shadows = {ShadowAlarmManagerAPI21.class, ShadowGlobalManager.class})
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class CalendarWithOneTimeAlarmTest extends FixedTimeTest {

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
    private TextView textDoW;
    private TextView textTime;
    private TextView textState;
    private EditText textName;
    private TextView textComment;
    private LinearLayout headerDate;

    // Items in RingActivity
    private TextView textAlarmTime;
    private TextView textOneTimeAlarmName;
    private TextView textNextCalendar;
    private TextView textMuted;

    private ImageButton snoozeButton;
    private SlideButton dismissButton;

    private final int ONE_TIME_ALARM_HOUR = DayTest.HOUR + 3;
    private final int ONE_TIME_ALARM_MINUTE = 30;

    @Before
    public void before() {
        super.before();

        AlarmMorningActivityTest.saveWizardPreference(!Wizard.PREF_WIZARD_DEFAULT);

        context = RuntimeEnvironment.application.getApplicationContext();

        AlarmMorningActivityTest.setLocale(context, "en", "US");

        alarmManager = (AlarmManager) RuntimeEnvironment.application.getSystemService(Context.ALARM_SERVICE);
        shadowAlarmManager = (ShadowAlarmManagerAPI21) Shadows.shadowOf(alarmManager);

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
        assertNotificationCount(0);

        // Check widget
        assertWidget(R.drawable.ic_alarm_off_white, "No alarm", null);
    }

    @Test
    public void t10_addAlarm() {
        // Consume the alarm with action ACTION_SET_SYSTEM_ALARM
        consumeNextScheduledAlarm();

        // Click in calendar
        startActivityCalendar();

        int oldCount = recyclerView.getChildCount();
        loadItemAtPosition(0);

        assertThat(textDate.getText(), is("2/1"));
        assertThat(textDoW.getText(), is("Mon"));
        assertThat(textTime.getText(), is("Off"));
        assertThat(textState.getText(), is(""));
        assertThat(textName.getVisibility(), is(View.GONE));
        assertThat(textComment.getText(), is(""));

        item.performLongClick();

        // TODO Test that the context menu contains the "Add alarm" item (not yet easily supported by Roboletric)

        // Click context menu
        clickContextMenu(R.id.day_add_alarm);

        // Click the time picker
        TimePickerFragment timePickerFragment = (TimePickerFragment) activity.getFragmentManager().findFragmentByTag("timePicker");

        TimePickerDialog dialog = (TimePickerDialog) timePickerFragment.getDialog();
        ShadowTimePickerDialog shadowDialog = Shadows.shadowOf(dialog);

        // Check presets
        assertThat(shadowDialog.getHourOfDay(), is(DayTest.HOUR + 1));
        assertThat(shadowDialog.getMinute(), is(0));

        // Change the preset time
        dialog.updateTime(ONE_TIME_ALARM_HOUR, ONE_TIME_ALARM_MINUTE);

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick();

        // Hack: RecyclerView needs to be measured and laid out manually in Robolectric.
        // Source: http://stackoverflow.com/questions/27052866/android-robolectric-click-recyclerview-item
        recyclerView.measure(0, 0);
        recyclerView.layout(0, 0, 100, 10000);

        // Check calendar
        int newCount = recyclerView.getChildCount();
        assertThat("The number of items increased by one", newCount - oldCount, is(1));

        // Check calendar - the Day item is the same
        loadItemAtPosition(0);
        assertThat(textDate.getText(), is("2/1"));
        assertThat(textDoW.getText(), is("Mon"));
        assertThat(textTime.getText(), is("Off"));
        assertThat(textState.getText(), is(""));
        assertThat(textName.getVisibility(), is(View.GONE));
        assertThat(textComment.getText(), is(""));

        // Check calendar - the newly added item for one-time alarm
        loadItemAtPosition(1);
        assertThat(textDate.getText(), is(""));
        assertThat(textDoW.getText(), is(""));
        assertThat(textTime.getText(), is("4:30 AM"));
        assertThat(textState.getText(), is(""));
        assertThat(textName.getVisibility(), is(View.VISIBLE));
        assertThat(textName.getText().toString(), is(""));
        assertThat(textComment.getText(), is("3h 30m"));

        // Check system alarm
        assertSystemAlarm(YEAR, MONTH, DAY, ONE_TIME_ALARM_HOUR - 2, ONE_TIME_ALARM_MINUTE, SystemAlarm.ACTION_RING_IN_NEAR_FUTURE);

        // Check system alarm clock
        assertSystemAlarmClock(YEAR, MONTH, DAY, ONE_TIME_ALARM_HOUR, ONE_TIME_ALARM_MINUTE);

        // Check notification
        assertNotificationCount(0);

        // Check widget
        assertWidget(R.drawable.ic_alarm_white, "04:30", "Mon");
    }

    @Test
    public void t11_addAlarmToPast() {
        // Consume the alarm with action ACTION_SET_SYSTEM_ALARM
        consumeNextScheduledAlarm();

        // Click in calendar
        startActivityCalendar();
        int oldCount = recyclerView.getChildCount();
        loadItemAtPosition(0);
        item.performLongClick();
        clickContextMenu(R.id.day_add_alarm);

        // Click the time picker
        TimePickerFragment fragment = (TimePickerFragment) activity.getFragmentManager().findFragmentByTag("timePicker");

        TimePickerDialog dialog = (TimePickerDialog) fragment.getDialog();
        ShadowTimePickerDialog shadowDialog = Shadows.shadowOf(dialog);

        Calendar past = globalManager.clock().now();
        past.add(Calendar.MINUTE, -1);
        dialog.updateTime(past.get(Calendar.HOUR_OF_DAY), past.get(Calendar.MINUTE));

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick();

        // Check calendar
        int newCount = recyclerView.getChildCount();
        assertThat("The number of items increased by one", newCount - oldCount, is(1));

        // Check calendar - the Day item is the same
        loadItemAtPosition(0);
        assertThat(textDate.getText(), is("2/1"));
        assertThat(textDoW.getText(), is("Mon"));
        assertThat(textTime.getText(), is("Off"));
        assertThat(textState.getText(), is(""));
        assertThat(textName.getVisibility(), is(View.GONE));
        assertThat(textComment.getText(), is(""));

        // Check calendar - the newly added item for one-time alarm
        loadItemAtPosition(1);
        assertThat(textDate.getText(), is(""));
        assertThat(textDoW.getText(), is(""));
        assertThat(textTime.getText(), is("12:59 AM"));
        assertThat(textState.getText(), is("Passed"));
        assertThat(textName.getVisibility(), is(View.VISIBLE));
        assertThat(textName.getText().toString(), is(""));
        assertThat(textComment.getText(), is(""));

        // Check system alarm
        // The alarm that was consumed at the beginning of this method didn't change
        assertThat(shadowAlarmManager.getScheduledAlarms().size(), is(0));

        // Check system alarm clock
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            AlarmManager.AlarmClockInfo alarmClockInfo = alarmManager.getNextAlarmClock();
            assertNull(alarmClockInfo);
        }

        // Check notification
        assertNotificationCount(0);

        // Check widget
        assertWidget(R.drawable.ic_alarm_off_white, "No alarm", null);
    }

    @Test
    public void t12_addAlarmWithZeroAdvancePeriod() {
        // Set the preference to zero
        CalendarTest.setNearFuturePeriodPreferenceToZero(context);

        // Consume the alarm with action ACTION_SET_SYSTEM_ALARM
        consumeNextScheduledAlarm();

        // Click in calendar
        startActivityCalendar();
        int oldCount = recyclerView.getChildCount();
        loadItemAtPosition(0);
        item.performLongClick();
        clickContextMenu(R.id.day_add_alarm);

        // Click the time picker
        TimePickerFragment fragment = (TimePickerFragment) activity.getFragmentManager().findFragmentByTag("timePicker");

        TimePickerDialog dialog = (TimePickerDialog) fragment.getDialog();
        ShadowTimePickerDialog shadowDialog = Shadows.shadowOf(dialog);

        dialog.updateTime(ONE_TIME_ALARM_HOUR, ONE_TIME_ALARM_MINUTE);

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick();

        // Check calendar
        int newCount = recyclerView.getChildCount();
        assertThat("The number of items increased by one", newCount - oldCount, is(1));

        // Check calendar - the Day item is the same
        loadItemAtPosition(0);
        assertThat(textDate.getText(), is("2/1"));
        assertThat(textDoW.getText(), is("Mon"));
        assertThat(textTime.getText(), is("Off"));
        assertThat(textState.getText(), is(""));
        assertThat(textName.getVisibility(), is(View.GONE));
        assertThat(textComment.getText(), is(""));

        // Check calendar - the newly added item for one-time alarm
        loadItemAtPosition(1);
        assertThat(textDate.getText(), is(""));
        assertThat(textDoW.getText(), is(""));
        assertThat(textTime.getText(), is("4:30 AM"));
        assertThat(textState.getText(), is(""));
        assertThat(textName.getVisibility(), is(View.VISIBLE));
        assertThat(textName.getText().toString(), is(""));
        assertThat(textComment.getText(), is("3h 30m"));

        // Check system alarm
        assertSystemAlarm(YEAR, MONTH, DAY, ONE_TIME_ALARM_HOUR, ONE_TIME_ALARM_MINUTE, SystemAlarm.ACTION_RING);

        // Check system alarm clock
        assertSystemAlarmClock(YEAR, MONTH, DAY, ONE_TIME_ALARM_HOUR, ONE_TIME_ALARM_MINUTE);

        // Check notification
        assertNotificationCount(0);

        // Check widget
        assertWidget(R.drawable.ic_alarm_white, "4:30 AM", "Mon");
    }

    @Test
    public void t13_addAlarmToTomorrow() {
        // Consume the alarm with action ACTION_SET_SYSTEM_ALARM
        consumeNextScheduledAlarm();

        // Click in calendar
        startActivityCalendar();
        int oldCount = recyclerView.getChildCount();
        loadItemAtPosition(1);
        item.performLongClick();

        // Click context menu
        clickContextMenu(R.id.day_add_alarm);

        // Click the time picker
        TimePickerFragment timePickerFragment = (TimePickerFragment) activity.getFragmentManager().findFragmentByTag("timePicker");

        TimePickerDialog dialog = (TimePickerDialog) timePickerFragment.getDialog();
        ShadowTimePickerDialog shadowDialog = Shadows.shadowOf(dialog);

        // Check presets
        assertThat(shadowDialog.getHourOfDay(), is(DayTest.HOUR));
        assertThat(shadowDialog.getMinute(), is(0));

        // Change the preset time
        dialog.updateTime(ONE_TIME_ALARM_HOUR, ONE_TIME_ALARM_MINUTE);

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick();

        // Hack: RecyclerView needs to be measured and laid out manually in Robolectric.
        // Source: http://stackoverflow.com/questions/27052866/android-robolectric-click-recyclerview-item
        recyclerView.measure(0, 0);
        recyclerView.layout(0, 0, 100, 10000);

        // Check calendar
        int newCount = recyclerView.getChildCount();
        assertThat("The number of items increased by one", newCount - oldCount, is(1));

        // Check calendar - the Day for today item is the same
        loadItemAtPosition(0);
        assertThat(textDate.getText(), is("2/1"));
        assertThat(textDoW.getText(), is("Mon"));
        assertThat(textTime.getText(), is("Off"));
        assertThat(textState.getText(), is(""));
        assertThat(textName.getVisibility(), is(View.GONE));
        assertThat(textComment.getText(), is(""));

        // Check calendar - the Day for tomorrow item is the same
        loadItemAtPosition(1);
        assertThat(textDate.getText(), is("2/2"));
        assertThat(textDoW.getText(), is("Tue"));
        assertThat(textTime.getText(), is("Off"));
        assertThat(textState.getText(), is(""));
        assertThat(textName.getVisibility(), is(View.GONE));
        assertThat(textComment.getText(), is(""));

        // Check calendar - the newly added item for one-time alarm
        loadItemAtPosition(2);
        assertThat(textDate.getText(), is(""));
        assertThat(textDoW.getText(), is(""));
        assertThat(textTime.getText(), is("4:30 AM"));
        assertThat(textState.getText(), is(""));
        assertThat(textName.getVisibility(), is(View.VISIBLE));
        assertThat(textName.getText().toString(), is(""));
        assertThat(textComment.getText(), is("1d 3h"));

        // Check system alarm
        assertSystemAlarm(YEAR, MONTH, DAY + 1, ONE_TIME_ALARM_HOUR - 2, ONE_TIME_ALARM_MINUTE, SystemAlarm.ACTION_RING_IN_NEAR_FUTURE);

        // Check system alarm clock
        assertSystemAlarmClock(YEAR, MONTH, DAY + 1, ONE_TIME_ALARM_HOUR, ONE_TIME_ALARM_MINUTE);

        // Check notification
        assertNotificationCount(0);

        // Check widget
        assertWidget(R.drawable.ic_alarm_white, "04:30", "Tue");
    }

    @Test
    public void t14_addTwoAlarmsAtTheSameTime() {
        // Consume the alarm with action ACTION_SET_SYSTEM_ALARM
        consumeNextScheduledAlarm();

        // Click in calendar
        startActivityCalendar();

        int oldCount = recyclerView.getChildCount();
        loadItemAtPosition(0);

        // 1st: set

        item.performLongClick();

        // Click context menu
        clickContextMenu(R.id.day_add_alarm);

        // Click the time picker
        TimePickerFragment timePickerFragment = (TimePickerFragment) activity.getFragmentManager().findFragmentByTag("timePicker");

        TimePickerDialog dialog = (TimePickerDialog) timePickerFragment.getDialog();
        ShadowTimePickerDialog shadowDialog = Shadows.shadowOf(dialog);

        // Check presets
        assertThat(shadowDialog.getHourOfDay(), is(DayTest.HOUR + 1));
        assertThat(shadowDialog.getMinute(), is(0));

        // Change the preset time
        dialog.updateTime(ONE_TIME_ALARM_HOUR, ONE_TIME_ALARM_MINUTE);

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick();

        // Hack: RecyclerView needs to be measured and laid out manually in Robolectric.
        // Source: http://stackoverflow.com/questions/27052866/android-robolectric-click-recyclerview-item
        recyclerView.measure(0, 0);
        recyclerView.layout(0, 0, 100, 10000);

        // 2nd: set
        loadItemAtPosition(0);

        item.performLongClick();

        // Click context menu
        clickContextMenu(R.id.day_add_alarm);

        // Click the time picker
        timePickerFragment = (TimePickerFragment) activity.getFragmentManager().findFragmentByTag("timePicker");

        dialog = (TimePickerDialog) timePickerFragment.getDialog();
        shadowDialog = Shadows.shadowOf(dialog);

        // Check presets
        assertThat(shadowDialog.getHourOfDay(), is(DayTest.HOUR + 1));
        assertThat(shadowDialog.getMinute(), is(0));

        // Change the preset time
        dialog.updateTime(ONE_TIME_ALARM_HOUR, ONE_TIME_ALARM_MINUTE);

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick();

        // Hack: RecyclerView needs to be measured and laid out manually in Robolectric.
        // Source: http://stackoverflow.com/questions/27052866/android-robolectric-click-recyclerview-item
        recyclerView.measure(0, 0);
        recyclerView.layout(0, 0, 100, 10000);

        // Check calendar
        int newCount = recyclerView.getChildCount();
        assertThat("The number of items increased by one", newCount - oldCount, is(2));

        // Check calendar - the Day item is the same
        loadItemAtPosition(0);
        assertThat(textDate.getText(), is("2/1"));
        assertThat(textDoW.getText(), is("Mon"));
        assertThat(textTime.getText(), is("Off"));
        assertThat(textState.getText(), is(""));
        assertThat(textName.getVisibility(), is(View.GONE));
        assertThat(textComment.getText(), is(""));

        // Check calendar - the 1st newly added item for one-time alarm
        loadItemAtPosition(1);
        assertThat(textDate.getText(), is(""));
        assertThat(textDoW.getText(), is(""));
        assertThat(textTime.getText(), is("4:30 AM"));
        assertThat(textState.getText(), is(""));
        assertThat(textName.getVisibility(), is(View.VISIBLE));
        assertThat(textName.getText().toString(), is(""));
        assertThat(textComment.getText(), is("3h 30m"));

        // Check calendar - the 2nd newly added item for one-time alarm
        loadItemAtPosition(2);
        assertThat(textDate.getText(), is(""));
        assertThat(textDoW.getText(), is(""));
        assertThat(textTime.getText(), is("4:30 AM"));
        assertThat(textState.getText(), is(""));
        assertThat(textName.getVisibility(), is(View.VISIBLE));
        assertThat(textName.getText().toString(), is(""));
        assertThat(textComment.getText(), is("3h 30m"));

        // Check system alarm
        assertSystemAlarm(YEAR, MONTH, DAY, ONE_TIME_ALARM_HOUR - 2, ONE_TIME_ALARM_MINUTE, SystemAlarm.ACTION_RING_IN_NEAR_FUTURE);

        // Check system alarm clock
        assertSystemAlarmClock(YEAR, MONTH, DAY, ONE_TIME_ALARM_HOUR, ONE_TIME_ALARM_MINUTE);

        // Check notification
        assertNotificationCount(0);

        // Check widget
        assertWidget(R.drawable.ic_alarm_white, "04:30", "Mon");
    }

    @Test
    public void t15_addTwoAlarms_BothInNearPeriod_SecondIsBefore() {
        // Consume the alarm with action ACTION_SET_SYSTEM_ALARM
        consumeNextScheduledAlarm();

        // Click in calendar
        startActivityCalendar();

        int oldCount = recyclerView.getChildCount();
        loadItemAtPosition(0);

        // 1st: set

        item.performLongClick();

        // Click context menu
        clickContextMenu(R.id.day_add_alarm);

        // Click the time picker
        TimePickerFragment timePickerFragment = (TimePickerFragment) activity.getFragmentManager().findFragmentByTag("timePicker");

        TimePickerDialog dialog = (TimePickerDialog) timePickerFragment.getDialog();
        ShadowTimePickerDialog shadowDialog = Shadows.shadowOf(dialog);

        // Check presets
        assertThat(shadowDialog.getHourOfDay(), is(DayTest.HOUR + 1));
        assertThat(shadowDialog.getMinute(), is(0));

        // Change the preset time
        dialog.updateTime(DayTest.HOUR + 1, 2);

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick();

        // Hack: RecyclerView needs to be measured and laid out manually in Robolectric.
        // Source: http://stackoverflow.com/questions/27052866/android-robolectric-click-recyclerview-item
        recyclerView.measure(0, 0);
        recyclerView.layout(0, 0, 100, 10000);

        // 2nd: set
        loadItemAtPosition(0);

        item.performLongClick();

        // Click context menu
        clickContextMenu(R.id.day_add_alarm);

        // Click the time picker
        timePickerFragment = (TimePickerFragment) activity.getFragmentManager().findFragmentByTag("timePicker");

        dialog = (TimePickerDialog) timePickerFragment.getDialog();
        shadowDialog = Shadows.shadowOf(dialog);

        // Check presets
        assertThat(shadowDialog.getHourOfDay(), is(DayTest.HOUR + 1));
        assertThat(shadowDialog.getMinute(), is(0));

        // Change the preset time
        dialog.updateTime(DayTest.HOUR + 1, 1);

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick();

        // Hack: RecyclerView needs to be measured and laid out manually in Robolectric.
        // Source: http://stackoverflow.com/questions/27052866/android-robolectric-click-recyclerview-item
        recyclerView.measure(0, 0);
        recyclerView.layout(0, 0, 100, 10000);

        // Check calendar
        int newCount = recyclerView.getChildCount();
        assertThat("The number of items increased by one", newCount - oldCount, is(2));

        // Check calendar - the Day item is the same
        loadItemAtPosition(0);
        assertThat(textDate.getText(), is("2/1"));
        assertThat(textDoW.getText(), is("Mon"));
        assertThat(textTime.getText(), is("Off"));
        assertThat(textState.getText(), is(""));
        assertThat(textName.getVisibility(), is(View.GONE));
        assertThat(textComment.getText(), is(""));

        // Check calendar - the 1st newly added item for one-time alarm
        loadItemAtPosition(1);
        assertThat(textDate.getText(), is(""));
        assertThat(textDoW.getText(), is(""));
        assertThat(textTime.getText(), is("2:01 AM"));
        assertThat(textState.getText(), is(""));
        assertThat(textName.getVisibility(), is(View.VISIBLE));
        assertThat(textName.getText().toString(), is(""));
        assertThat(textComment.getText(), is("1h 1m"));

        // Check calendar - the 2nd newly added item for one-time alarm
        loadItemAtPosition(2);
        assertThat(textDate.getText(), is(""));
        assertThat(textDoW.getText(), is(""));
        assertThat(textTime.getText(), is("2:02 AM"));
        assertThat(textState.getText(), is(""));
        assertThat(textName.getVisibility(), is(View.VISIBLE));
        assertThat(textName.getText().toString(), is(""));
        assertThat(textComment.getText(), is(""));

        // Check system alarm
        assertSystemAlarm(YEAR, MONTH, DAY, DayTest.HOUR + 1, 1, SystemAlarm.ACTION_RING);

        // Check system alarm clock
        assertSystemAlarmClock(YEAR, MONTH, DAY, DayTest.HOUR + 1, 1);

        // Check notification
//        assertNotificationCount(1);
//
//        Notification notification = shadowNotificationManager.getAllNotifications().get(0);
//        assertNotification(notification, "Alarm at 2:01 AM", "Touch to view all alarms");
//        assertNotificationActionCount(notification, 1);
//        assertNotificationAction(notification, 0, "Dismiss", NotificationReceiver.ACTION_DISMISS_BEFORE_RINGING);

        // Check widget
        assertWidget(R.drawable.ic_alarm_white, "02:01", "Mon");
    }

    @Test
    public void t16_setAlarmOfDistantAlarm() {
        prepareCreate();

        // Consume the alarm with action ACTION_RING_IN_NEAR_FUTURE
        consumeNextScheduledAlarm();

        startActivityCalendar();

        // Hack: RecyclerView needs to be measured and laid out manually in Robolectric.
        // Source: http://stackoverflow.com/questions/27052866/android-robolectric-click-recyclerview-item
        recyclerView.measure(0, 0);
        recyclerView.layout(0, 0, 100, 10000);

        int oldCount = recyclerView.getChildCount();

        // 2nd: set
        loadItemAtPosition(1);

        item.performLongClick();

        // TODO Test that the context menu contains the "Set time" item (not yet easily supported by Roboletric)
        // TODO Test that the context menu contains the "Add alarm" item (not yet easily supported by Roboletric)

        // Click context menu
        clickContextMenu(R.id.day_set_time);

        // Click the time picker
        TimePickerFragment timePickerFragment = (TimePickerFragment) activity.getFragmentManager().findFragmentByTag("timePicker");

        TimePickerDialog dialog = (TimePickerDialog) timePickerFragment.getDialog();
        ShadowTimePickerDialog shadowDialog = Shadows.shadowOf(dialog);

        // Check presets
        assertThat(shadowDialog.getHourOfDay(), is(ONE_TIME_ALARM_HOUR));
        assertThat(shadowDialog.getMinute(), is(ONE_TIME_ALARM_MINUTE));

        // Change the preset time
        dialog.updateTime(ONE_TIME_ALARM_HOUR + 1, ONE_TIME_ALARM_MINUTE + 1);

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick();

        // Hack: RecyclerView needs to be measured and laid out manually in Robolectric.
        // Source: http://stackoverflow.com/questions/27052866/android-robolectric-click-recyclerview-item
        recyclerView.measure(0, 0);
        recyclerView.layout(0, 0, 100, 10000);

        // Check calendar
        int newCount = recyclerView.getChildCount();
        assertThat("The number of items did not change", newCount - oldCount, is(0));

        // Check calendar - the Day item is the same
        loadItemAtPosition(0);
        assertThat(textDate.getText(), is("2/1"));
        assertThat(textDoW.getText(), is("Mon"));
        assertThat(textTime.getText(), is("Off"));
        assertThat(textState.getText(), is(""));
        assertThat(textName.getVisibility(), is(View.GONE));
        assertThat(textComment.getText(), is(""));

        // Check calendar - the newly added item for one-time alarm
        loadItemAtPosition(1);
        assertThat(textDate.getText(), is(""));
        assertThat(textDoW.getText(), is(""));
        assertThat(textTime.getText(), is("5:31 AM"));
        assertThat(textState.getText(), is(""));
        assertThat(textName.getVisibility(), is(View.VISIBLE));
        assertThat(textName.getText().toString(), is(""));
        assertThat(textComment.getText(), is("4h 31m"));

        // Check system alarm
        assertSystemAlarm(YEAR, MONTH, DAY, ONE_TIME_ALARM_HOUR + 1 - 2, ONE_TIME_ALARM_MINUTE + 1, SystemAlarm.ACTION_RING_IN_NEAR_FUTURE);

        // Check system alarm clock
        assertSystemAlarmClock(YEAR, MONTH, DAY, ONE_TIME_ALARM_HOUR + 1, ONE_TIME_ALARM_MINUTE + 1);

        // Check notification
        assertNotificationCount(0);

        // Check widget
        assertWidget(R.drawable.ic_alarm_white, "05:31", "Mon");
    }

    @Test
    public void t17_setDateOfDistantAlarm() {
        prepareCreate();

        // Consume the alarm with action ACTION_RING_IN_NEAR_FUTURE
        consumeNextScheduledAlarm();

        // Click in calendar
        startActivityCalendar();
        loadItemAtPosition(1);
        headerDate.performClick();

        // Click the date picker
        DatePickerFragment datePickerFragment = (DatePickerFragment) activity.getFragmentManager().findFragmentByTag("datePicker");

        DatePickerDialog dialog = (DatePickerDialog) datePickerFragment.getDialog();
        ShadowDatePickerDialog shadowDialog = Shadows.shadowOf(dialog);

        // Check presets
        assertThat(shadowDialog.getYear(), is(YEAR));
        assertThat(shadowDialog.getMonthOfYear(), is(MONTH));
        assertThat(shadowDialog.getDayOfMonth(), is(DAY));

        // Change the preset time
        dialog.updateDate(YEAR, MONTH, DAY + 1);

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick();

        // Hack: RecyclerView needs to be measured and laid out manually in Robolectric.
        // Source: http://stackoverflow.com/questions/27052866/android-robolectric-click-recyclerview-item
        recyclerView.measure(0, 0);
        recyclerView.layout(0, 0, 100, 10000);

        // Check calendar - the Day item is the same
        loadItemAtPosition(0);
        assertThat(textDate.getText(), is("2/1"));
        assertThat(textDoW.getText(), is("Mon"));
        assertThat(textTime.getText(), is("Off"));
        assertThat(textState.getText(), is(""));
        assertThat(textName.getVisibility(), is(View.GONE));
        assertThat(textComment.getText(), is(""));

        // Check calendar - the Day item is the same
        loadItemAtPosition(1);
        assertThat(textDate.getText(), is("2/2"));
        assertThat(textDoW.getText(), is("Tue"));
        assertThat(textTime.getText(), is("Off"));
        assertThat(textState.getText(), is(""));
        assertThat(textName.getVisibility(), is(View.GONE));
        assertThat(textComment.getText(), is(""));

        // Check calendar - the newly added item for one-time alarm
        loadItemAtPosition(2);
        assertThat(textDate.getText(), is(""));
        assertThat(textDoW.getText(), is(""));
        assertThat(textTime.getText(), is("4:30 AM"));
        assertThat(textState.getText(), is(""));
        assertThat(textName.getVisibility(), is(View.VISIBLE));
        assertThat(textName.getText().toString(), is(""));
        assertThat(textComment.getText(), is("1d 3h"));

        // Check system alarm
        assertSystemAlarm(YEAR, MONTH, DAY + 1, ONE_TIME_ALARM_HOUR - 2, ONE_TIME_ALARM_MINUTE, SystemAlarm.ACTION_RING_IN_NEAR_FUTURE);

        // Check system alarm clock
        assertSystemAlarmClock(YEAR, MONTH, DAY + 1, ONE_TIME_ALARM_HOUR, ONE_TIME_ALARM_MINUTE);

        // Check notification
        assertNotificationCount(0);

        // Check widget
        assertWidget(R.drawable.ic_alarm_white, "04:30", "Tue");
    }

    @Test
    public void t18_setNameOfDistantAlarm() {
        prepareCreate();

        // Consume the alarm with action ACTION_RING_IN_NEAR_FUTURE
        consumeNextScheduledAlarm();

        // Click in calendar
        startActivityCalendar();
        loadItemAtPosition(1);
        textName.performClick();

        textName.setText("Flowers");

        // Click Done in the soft keyboard
        textName.onEditorAction(EditorInfo.IME_ACTION_DONE);

        // Hack: RecyclerView needs to be measured and laid out manually in Robolectric.
        // Source: http://stackoverflow.com/questions/27052866/android-robolectric-click-recyclerview-item
        recyclerView.measure(0, 0);
        recyclerView.layout(0, 0, 100, 10000);

        // Check calendar - the Day item is the same
        loadItemAtPosition(0);
        assertThat(textDate.getText(), is("2/1"));
        assertThat(textDoW.getText(), is("Mon"));
        assertThat(textTime.getText(), is("Off"));
        assertThat(textState.getText(), is(""));
        assertThat(textName.getVisibility(), is(View.GONE));
        assertThat(textComment.getText(), is(""));

        // Check calendar - the newly added item for one-time alarm
        loadItemAtPosition(1);
        assertThat(textDate.getText(), is(""));
        assertThat(textDoW.getText(), is(""));
        assertThat(textTime.getText(), is("4:30 AM"));
        assertThat(textState.getText(), is(""));
        assertThat(textName.getVisibility(), is(View.VISIBLE));
        assertThat(textName.getText().toString(), is("Flowers"));
        assertThat(textComment.getText(), is("3h 30m"));

        // Check system alarm
        assertSystemAlarmNone();

        // Check system alarm clock
        assertSystemAlarmClock(YEAR, MONTH, DAY, ONE_TIME_ALARM_HOUR, ONE_TIME_ALARM_MINUTE);

        // Check notification
        assertNotificationCount(0);

        // Check widget
        assertWidget(R.drawable.ic_alarm_white, "04:30", "Mon");
    }

    @Test
    public void t20_onNear() {
        prepareUntilNear();

        // Check system alarm
        assertSystemAlarm(YEAR, MONTH, DAY, ONE_TIME_ALARM_HOUR, ONE_TIME_ALARM_MINUTE, SystemAlarm.ACTION_RING);

        // Check system alarm clock
        assertSystemAlarmClock(YEAR, MONTH, DAY, ONE_TIME_ALARM_HOUR, ONE_TIME_ALARM_MINUTE);

        // Check notification
//        assertNotificationCount(1);
//
//        Notification notification = shadowNotificationManager.getAllNotifications().get(0);
//        assertNotification(notification, "Alarm at 4:30 AM", "Touch to view all alarms");
//        assertNotificationActionCount(notification, 1);
//        assertNotificationAction(notification, 0, "Dismiss", NotificationReceiver.ACTION_DISMISS_BEFORE_RINGING);

        // Check widget
        assertWidget(R.drawable.ic_alarm_white, "4:30 AM", "Mon");
    }

    @Test
    public void t21_dismissWhileInNearPeriod() {
        prepareUntilNear();

        // Consume the alarm with action ACTION_RING
        consumeNextScheduledAlarm();

        // Click in calendar
        startActivityCalendar();
        loadItemAtPosition(1);
        item.performLongClick();

        // TODO Test that the context menu contains the "Dismiss" item (not yet easily supported by Roboletric)

        // Click context menu
        clickContextMenu(R.id.day_dismiss);

        // Check calendar - the Day item is the same
        loadItemAtPosition(0);
        assertThat(textDate.getText(), is("2/1"));
        assertThat(textDoW.getText(), is("Mon"));
        assertThat(textTime.getText(), is("Off"));
        assertThat(textState.getText(), is(""));
        assertThat(textName.getVisibility(), is(View.GONE));
        assertThat(textComment.getText(), is(""));

        // Check calendar - the newly added item for one-time alarm
        loadItemAtPosition(1);
        assertThat(textDate.getText(), is(""));
        assertThat(textDoW.getText(), is(""));
        assertThat(textTime.getText(), is("4:30 AM"));
        assertThat(textState.getText(), is("Dismissed before ringing"));
        assertThat(textName.getVisibility(), is(View.VISIBLE));
        assertThat(textName.getText().toString(), is(""));
        // FIXME assertThat(textComment.getText(), is(""));

        // Check system alarm
        assertSystemAlarm(YEAR, MONTH, DAY, ONE_TIME_ALARM_HOUR, ONE_TIME_ALARM_MINUTE, SystemAlarm.ACTION_ALARM_TIME_OF_EARLY_DISMISSED_ALARM);

        // Check system alarm clock
        assertSystemAlarmClockNone();

        // Check notification
        assertNotificationCount(0);

        // Check widget
        assertWidget(R.drawable.ic_alarm_off_white, "No alarm", null);
    }

    @Test
    public void t22_setTimeWhileInNearPeriod() {
        prepareUntilNear();

        // Consume the alarm with action ACTION_RING
        consumeNextScheduledAlarm();

        // Click in calendar
        startActivityCalendar();
        loadItemAtPosition(1);
        item.performClick();

        // Click the time picker
        TimePickerFragment timePickerFragment = (TimePickerFragment) activity.getFragmentManager().findFragmentByTag("timePicker");

        TimePickerDialog dialog = (TimePickerDialog) timePickerFragment.getDialog();
        ShadowTimePickerDialog shadowDialog = Shadows.shadowOf(dialog);

        // Check presets
        assertThat(shadowDialog.getHourOfDay(), is(ONE_TIME_ALARM_HOUR));
        assertThat(shadowDialog.getMinute(), is(ONE_TIME_ALARM_MINUTE));

        // Change the preset time
        dialog.updateTime(ONE_TIME_ALARM_HOUR + 1, ONE_TIME_ALARM_MINUTE + 1);

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick();

        // Hack: RecyclerView needs to be measured and laid out manually in Robolectric.
        // Source: http://stackoverflow.com/questions/27052866/android-robolectric-click-recyclerview-item
        recyclerView.measure(0, 0);
        recyclerView.layout(0, 0, 100, 10000);

        // Check calendar - the Day item is the same
        loadItemAtPosition(0);
        assertThat(textDate.getText(), is("2/1"));
        assertThat(textDoW.getText(), is("Mon"));
        assertThat(textTime.getText(), is("Off"));
        assertThat(textState.getText(), is(""));
        assertThat(textName.getVisibility(), is(View.GONE));
        assertThat(textComment.getText(), is(""));

        // Check calendar - the one-time alarm
        loadItemAtPosition(1);
        assertThat(textDate.getText(), is(""));
        assertThat(textDoW.getText(), is(""));
        assertThat(textTime.getText(), is("5:31 AM"));
        assertThat(textState.getText(), is(""));
        assertThat(textName.getVisibility(), is(View.VISIBLE));
        assertThat(textName.getText().toString(), is(""));
        assertThat(textComment.getText(), is("3h 0m"));

        // Check system alarm
        assertSystemAlarm(YEAR, MONTH, DAY, ONE_TIME_ALARM_HOUR + 1 - 2, ONE_TIME_ALARM_MINUTE + 1, SystemAlarm.ACTION_RING_IN_NEAR_FUTURE);

        // Check system alarm clock
        assertSystemAlarmClock(YEAR, MONTH, DAY, ONE_TIME_ALARM_HOUR + 1, ONE_TIME_ALARM_MINUTE + 1);

        // Check notification
        assertNotificationCount(0);

        // Check widget
        assertWidget(R.drawable.ic_alarm_white, "05:31", "Mon");
    }

    @Test
    public void t23_setDateWhileInNearPeriod() {
        prepareUntilNear();

        // Consume the alarm with action ACTION_RING
        consumeNextScheduledAlarm();

        // Click in calendar
        startActivityCalendar();
        loadItemAtPosition(1);
        headerDate.performClick();

        // Click the date picker
        DatePickerFragment datePickerFragment = (DatePickerFragment) activity.getFragmentManager().findFragmentByTag("datePicker");

        DatePickerDialog dialog = (DatePickerDialog) datePickerFragment.getDialog();
        ShadowDatePickerDialog shadowDialog = Shadows.shadowOf(dialog);

        // Check presets
        assertThat(shadowDialog.getYear(), is(YEAR));
        assertThat(shadowDialog.getMonthOfYear(), is(MONTH));
        assertThat(shadowDialog.getDayOfMonth(), is(DAY));

        // Change the preset time
        dialog.updateDate(YEAR, MONTH, DAY + 1);

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick();

        // Hack: RecyclerView needs to be measured and laid out manually in Robolectric.
        // Source: http://stackoverflow.com/questions/27052866/android-robolectric-click-recyclerview-item
        recyclerView.measure(0, 0);
        recyclerView.layout(0, 0, 100, 10000);

        // Check calendar - the Day item is the same
        loadItemAtPosition(0);
        assertThat(textDate.getText(), is("2/1"));
        assertThat(textDoW.getText(), is("Mon"));
        assertThat(textTime.getText(), is("Off"));
        assertThat(textState.getText(), is(""));
        assertThat(textName.getVisibility(), is(View.GONE));
        assertThat(textComment.getText(), is(""));

        // Check calendar - the Day item is the same
        loadItemAtPosition(1);
        assertThat(textDate.getText(), is("2/2"));
        assertThat(textDoW.getText(), is("Tue"));
        assertThat(textTime.getText(), is("Off"));
        assertThat(textState.getText(), is(""));
        assertThat(textName.getVisibility(), is(View.GONE));
        assertThat(textComment.getText(), is(""));

        // Check calendar - the newly added item for one-time alarm
        loadItemAtPosition(2);
        assertThat(textDate.getText(), is(""));
        assertThat(textDoW.getText(), is(""));
        assertThat(textTime.getText(), is("4:30 AM"));
        assertThat(textState.getText(), is(""));
        assertThat(textName.getVisibility(), is(View.VISIBLE));
        assertThat(textName.getText().toString(), is(""));
        assertThat(textComment.getText(), is("1d 1h"));

        // Check system alarm
        assertSystemAlarm(YEAR, MONTH, DAY + 1, ONE_TIME_ALARM_HOUR - 2, ONE_TIME_ALARM_MINUTE, SystemAlarm.ACTION_RING_IN_NEAR_FUTURE);

        // Check system alarm clock
        assertSystemAlarmClock(YEAR, MONTH, DAY + 1, ONE_TIME_ALARM_HOUR, ONE_TIME_ALARM_MINUTE);

        // Check notification
        assertNotificationCount(0);

        // Check widget
        assertWidget(R.drawable.ic_alarm_white, "04:30", "Tue");
    }

    @Test
    public void t24_setNameWhileInNearPeriod() {
        prepareUntilNear();

        // Consume the alarm with action ACTION_RING
        consumeNextScheduledAlarm();

        // Click in calendar
        startActivityCalendar();
        loadItemAtPosition(1);
        textName.performClick();

        textName.setText("Flowers");

        // Click Done in the soft keyboard
        textName.onEditorAction(EditorInfo.IME_ACTION_DONE);

        // Hack: RecyclerView needs to be measured and laid out manually in Robolectric.
        // Source: http://stackoverflow.com/questions/27052866/android-robolectric-click-recyclerview-item
        recyclerView.measure(0, 0);
        recyclerView.layout(0, 0, 100, 10000);

        // Check calendar - the Day item is the same
        loadItemAtPosition(0);
        assertThat(textDate.getText(), is("2/1"));
        assertThat(textDoW.getText(), is("Mon"));
        assertThat(textTime.getText(), is("Off"));
        assertThat(textState.getText(), is(""));
        assertThat(textName.getVisibility(), is(View.GONE));
        assertThat(textComment.getText(), is(""));

        // Check calendar - the newly added item for one-time alarm
        loadItemAtPosition(1);
        assertThat(textDate.getText(), is(""));
        assertThat(textDoW.getText(), is(""));
        assertThat(textTime.getText(), is("4:30 AM"));
        assertThat(textState.getText(), is(""));
        assertThat(textName.getVisibility(), is(View.VISIBLE));
        assertThat(textName.getText().toString(), is("Flowers"));
        assertThat(textComment.getText(), is("1h 59m"));

        // Check system alarm
        assertSystemAlarmNone();

        // Check system alarm clock
        assertSystemAlarmClock(YEAR, MONTH, DAY, ONE_TIME_ALARM_HOUR, ONE_TIME_ALARM_MINUTE);

        // Check notification
        assertNotificationCount(0);

        // Check widget
        assertWidget(R.drawable.ic_alarm_white, "04:30", "Mon");
    }

    @Test
    public void t30_onRing() {
        prepareUntilRing();

        // Check that ringing started
        Activity activity = Robolectric.setupActivity(Activity.class);
        ShadowActivity shadowActivity = Shadows.shadowOf(activity);

        Intent intentNext = shadowActivity.peekNextStartedActivity();
        Intent expectedIntentNext = new Intent(context, RingActivity.class);

        assertThat(intentNext.getComponent(), is(expectedIntentNext.getComponent()));

        // Check system alarm
        assertSystemAlarm(YEAR, MONTH, DAY + 1, 0, 0, SystemAlarm.ACTION_SET_SYSTEM_ALARM);

        // Check system alarm clock
        assertSystemAlarmClockNone();

        // Check notification
//        assertNotificationCount(1);
//
//        Notification notification = shadowNotificationManager.getAllNotifications().get(0);
//        assertNotification(notification, "Alarm at 4:30 AM", "Ringing");
//        assertNotificationActionCount(notification, 2);
//        assertNotificationAction(notification, 0, "Dismiss", NotificationReceiver.ACTION_DISMISS);
//        assertNotificationAction(notification, 1, "Snooze", NotificationReceiver.ACTION_SNOOZE);

        // Start ring activity
        Calendar alarmTime = new GregorianCalendar(YEAR, MONTH, DAY, ONE_TIME_ALARM_HOUR, ONE_TIME_ALARM_MINUTE);
        startActivityRing(alarmTime);

        // Check appearance in Ring activity
        assertThat(textDate.getVisibility(), is(View.VISIBLE));
        assertThat(textTime.getVisibility(), is(View.VISIBLE));
        assertThat(textAlarmTime.getVisibility(), is(View.INVISIBLE));
        assertThat(textOneTimeAlarmName.getVisibility(), is(View.GONE));
        assertThat(textNextCalendar.getVisibility(), is(View.GONE));
        assertThat(textMuted.getVisibility(), is(View.INVISIBLE));

        assertThat(textDate.getText(), is("Monday, February 1"));
        assertThat(textTime.getText(), is("4:30 AM"));
        assertThat(textOneTimeAlarmName.getVisibility(), is(View.GONE));

        // Check widget
        assertWidget(R.drawable.ic_alarm_off_white, "No alarm", null);
    }

    @Test
    public void t31_onRingWithTomorrow() {
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
        assertSystemAlarm(YEAR, MONTH, DAY + 1, ONE_TIME_ALARM_HOUR - 1, ONE_TIME_ALARM_MINUTE + 1, SystemAlarm.ACTION_RING_IN_NEAR_FUTURE);

        // Check system alarm clock
        assertSystemAlarmClock(YEAR, MONTH, DAY + 1, ONE_TIME_ALARM_HOUR + 1, ONE_TIME_ALARM_MINUTE + 1);

        // Check notification
//        assertNotificationCount(1);
//
//        Notification notification = shadowNotificationManager.getAllNotifications().get(0);
//        assertNotification(notification, "Alarm at 4:30 AM", "Ringing");
//        assertNotificationActionCount(notification, 2);
//        assertNotificationAction(notification, 0, "Dismiss", NotificationReceiver.ACTION_DISMISS);
//        assertNotificationAction(notification, 1, "Snooze", NotificationReceiver.ACTION_SNOOZE);

        // Check widget
        assertWidget(R.drawable.ic_alarm_white, "05:31", "Tomorrow");

        // Shift clock by just under 2 hours
        shadowGlobalManager.setClock(new FixedClock(new GregorianCalendar(YEAR, MONTH, DAY, ONE_TIME_ALARM_HOUR + 2, ONE_TIME_ALARM_MINUTE, 59)));
        assertWidget(R.drawable.ic_alarm_white, "05:31", "Tomorrow");

        // Shift clock
        shadowGlobalManager.setClock(new FixedClock(new GregorianCalendar(YEAR, MONTH, DAY, ONE_TIME_ALARM_HOUR + 2, ONE_TIME_ALARM_MINUTE + 1)));
        assertWidget(R.drawable.ic_alarm_white, "05:31", null);
    }

    @Test
    public void t32_dismissWhileRinging() {
        prepareUntilRing();

        // Start ring activity
        Calendar alarmTime = new GregorianCalendar(YEAR, MONTH, DAY, ONE_TIME_ALARM_HOUR, ONE_TIME_ALARM_MINUTE, 10);
        startActivityRing(alarmTime);

        dismissButton.performClick();

        // Check system alarm
        assertSystemAlarm(YEAR, MONTH, DAY + 1, 0, 0, SystemAlarm.ACTION_SET_SYSTEM_ALARM);

        // Check system alarm clock
        assertSystemAlarmClockNone();

        // Check notification
        assertNotificationCount(0);

        // Check widget
        assertWidget(R.drawable.ic_alarm_off_white, "No alarm", null);
    }

    @Test
    public void t33_snoozeWhileRinging() {
        prepareUntilRing();

        // Consume the alarm with action ACTION_SET_SYSTEM_ALARM
        consumeNextScheduledAlarm();

        // Start ring activity
        Calendar alarmTime = new GregorianCalendar(YEAR, MONTH, DAY, ONE_TIME_ALARM_HOUR, ONE_TIME_ALARM_MINUTE, 10);
        startActivityRing(alarmTime);

        snoozeButton.performClick();

        // Start Calendar
        startActivityCalendar();

        // Check calendar - the Day item is the same
        loadItemAtPosition(0);
        assertThat(textDate.getText(), is("2/1"));
        assertThat(textDoW.getText(), is("Mon"));
        assertThat(textTime.getText(), is("Off"));
        assertThat(textState.getText(), is(""));
        assertThat(textName.getVisibility(), is(View.GONE));
        assertThat(textComment.getText(), is(""));

        // Check calendar - the newly added item for one-time alarm
        loadItemAtPosition(1);
        assertThat(textDate.getText(), is(""));
        assertThat(textDoW.getText(), is(""));
        assertThat(textTime.getText(), is("4:30 AM"));
        assertThat(textState.getText(), is("Snoozed"));
        assertThat(textName.getVisibility(), is(View.VISIBLE));
        assertThat(textName.getText().toString(), is(""));
        // FIXME assertThat(textComment.getText(), is("-0m 10s"));

        // Check system alarm
        assertSystemAlarm(YEAR, MONTH, DAY, ONE_TIME_ALARM_HOUR, ONE_TIME_ALARM_MINUTE + 10, SystemAlarm.ACTION_RING);

        // Check system alarm clock
        assertSystemAlarmClockNone();

        // Check notification
//        assertNotificationCount(1);
//
//        Notification notification = shadowNotificationManager.getAllNotifications().get(0);
//        assertNotification(notification, "Alarm at 4:30 AM", "Snoozed till 4:40 AM");
//        assertNotificationActionCount(notification, 1);
//        assertNotificationAction(notification, 0, "Dismiss", NotificationReceiver.ACTION_DISMISS);

        // Check widget
        assertWidget(R.drawable.ic_alarm_off_white, "No alarm", null);
    }

    @Test
    public void t34_setTimeWhileRinging() {
        prepareUntilRing();

        // Consume the alarm with action ACTION_SET_SYSTEM_ALARM
        consumeNextScheduledAlarm();

        // Click in calendar
        startActivityCalendar();
        loadItemAtPosition(1);
        item.performClick();

        // Click the time picker
        TimePickerFragment timePickerFragment = (TimePickerFragment) activity.getFragmentManager().findFragmentByTag("timePicker");

        TimePickerDialog dialog = (TimePickerDialog) timePickerFragment.getDialog();
        ShadowTimePickerDialog shadowDialog = Shadows.shadowOf(dialog);

        // Check presets
        assertThat(shadowDialog.getHourOfDay(), is(ONE_TIME_ALARM_HOUR));
        assertThat(shadowDialog.getMinute(), is(ONE_TIME_ALARM_MINUTE));

        // Change the preset time
        dialog.updateTime(ONE_TIME_ALARM_HOUR + 1, ONE_TIME_ALARM_MINUTE + 1);

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick();

        // Hack: RecyclerView needs to be measured and laid out manually in Robolectric.
        // Source: http://stackoverflow.com/questions/27052866/android-robolectric-click-recyclerview-item
        recyclerView.measure(0, 0);
        recyclerView.layout(0, 0, 100, 10000);

        // Check calendar - the Day item is the same
        loadItemAtPosition(0);
        assertThat(textDate.getText(), is("2/1"));
        assertThat(textDoW.getText(), is("Mon"));
        assertThat(textTime.getText(), is("Off"));
        assertThat(textState.getText(), is(""));
        assertThat(textName.getVisibility(), is(View.GONE));
        assertThat(textComment.getText(), is(""));

        // Check calendar - the one-time alarm
        loadItemAtPosition(1);
        assertThat(textDate.getText(), is(""));
        assertThat(textDoW.getText(), is(""));
        assertThat(textTime.getText(), is("5:31 AM"));
        assertThat(textState.getText(), is(""));
        assertThat(textName.getVisibility(), is(View.VISIBLE));
        assertThat(textName.getText().toString(), is(""));
        assertThat(textComment.getText(), is("1h 0m"));

        // Check system alarm
        assertSystemAlarm(YEAR, MONTH, DAY, ONE_TIME_ALARM_HOUR + 1, ONE_TIME_ALARM_MINUTE + 1, SystemAlarm.ACTION_RING);

        // Check system alarm clock
        assertSystemAlarmClock(YEAR, MONTH, DAY, ONE_TIME_ALARM_HOUR + 1, ONE_TIME_ALARM_MINUTE + 1);

        // Check notification
        assertNotificationCount(0);

        // Check widget
        assertWidget(R.drawable.ic_alarm_white, "05:31", "Mon");
    }

    @Test
    public void t35_setDateWhileRinging() {
        prepareUntilRing();

        // Consume the alarm with action ACTION_SET_SYSTEM_ALARM
        consumeNextScheduledAlarm();

        // Click in calendar
        startActivityCalendar();
        loadItemAtPosition(1);
        headerDate.performClick();

        // Click the date picker
        DatePickerFragment datePickerFragment = (DatePickerFragment) activity.getFragmentManager().findFragmentByTag("datePicker");

        DatePickerDialog dialog = (DatePickerDialog) datePickerFragment.getDialog();
        ShadowDatePickerDialog shadowDialog = Shadows.shadowOf(dialog);

        // Check presets
        assertThat(shadowDialog.getYear(), is(YEAR));
        assertThat(shadowDialog.getMonthOfYear(), is(MONTH));
        assertThat(shadowDialog.getDayOfMonth(), is(DAY));

        // Change the preset time
        dialog.updateDate(YEAR, MONTH, DAY + 1);

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick();

        // Hack: RecyclerView needs to be measured and laid out manually in Robolectric.
        // Source: http://stackoverflow.com/questions/27052866/android-robolectric-click-recyclerview-item
        recyclerView.measure(0, 0);
        recyclerView.layout(0, 0, 100, 10000);

        // Check calendar - the Day item is the same
        loadItemAtPosition(0);
        assertThat(textDate.getText(), is("2/1"));
        assertThat(textDoW.getText(), is("Mon"));
        assertThat(textTime.getText(), is("Off"));
        assertThat(textState.getText(), is(""));
        assertThat(textName.getVisibility(), is(View.GONE));
        assertThat(textComment.getText(), is(""));

        // Check calendar - the Day item is the same
        loadItemAtPosition(1);
        assertThat(textDate.getText(), is("2/2"));
        assertThat(textDoW.getText(), is("Tue"));
        assertThat(textTime.getText(), is("Off"));
        assertThat(textState.getText(), is(""));
        assertThat(textName.getVisibility(), is(View.GONE));
        assertThat(textComment.getText(), is(""));

        // Check calendar - the newly added item for one-time alarm
        loadItemAtPosition(2);
        assertThat(textDate.getText(), is(""));
        assertThat(textDoW.getText(), is(""));
        assertThat(textTime.getText(), is("4:30 AM"));
        assertThat(textState.getText(), is(""));
        assertThat(textName.getVisibility(), is(View.VISIBLE));
        assertThat(textName.getText().toString(), is(""));
        // FIXME assertThat(textComment.getText(), is("1d 3h"));

        // Check system alarm
        assertSystemAlarm(YEAR, MONTH, DAY + 1, ONE_TIME_ALARM_HOUR - 2, ONE_TIME_ALARM_MINUTE, SystemAlarm.ACTION_RING_IN_NEAR_FUTURE);

        // Check system alarm clock
        assertSystemAlarmClock(YEAR, MONTH, DAY + 1, ONE_TIME_ALARM_HOUR, ONE_TIME_ALARM_MINUTE);

        // Check notification
        assertNotificationCount(0);

        // Check widget
        assertWidget(R.drawable.ic_alarm_white, "04:30", "Tue");
    }

    @Test
    public void t36_setNameWhileRinging() {
        prepareUntilRing();

        // Consume the alarm with action ACTION_SET_SYSTEM_ALARM
        consumeNextScheduledAlarm();

        // Click in calendar
        startActivityCalendar();
        loadItemAtPosition(1);
        textName.performClick();

        textName.setText("Flowers");

        // Click Done in the soft keyboard
        textName.onEditorAction(EditorInfo.IME_ACTION_DONE);

        // Hack: RecyclerView needs to be measured and laid out manually in Robolectric.
        // Source: http://stackoverflow.com/questions/27052866/android-robolectric-click-recyclerview-item
        recyclerView.measure(0, 0);
        recyclerView.layout(0, 0, 100, 10000);

        // Check calendar - the Day item is the same
        loadItemAtPosition(0);
        assertThat(textDate.getText(), is("2/1"));
        assertThat(textDoW.getText(), is("Mon"));
        assertThat(textTime.getText(), is("Off"));
        assertThat(textState.getText(), is(""));
        assertThat(textName.getVisibility(), is(View.GONE));
        assertThat(textComment.getText(), is(""));

        // Check calendar - the newly added item for one-time alarm
        loadItemAtPosition(1);
        assertThat(textDate.getText(), is(""));
        assertThat(textDoW.getText(), is(""));
        assertThat(textTime.getText(), is("4:30 AM"));
        assertThat(textState.getText(), is("Ringing"));
        assertThat(textName.getVisibility(), is(View.VISIBLE));
        assertThat(textName.getText().toString(), is("Flowers"));
        // FIXME assertThat(textComment.getText(), is("-0m 10s"));

        // Check system alarm
        assertSystemAlarmNone();

        // Check system alarm clock
        assertSystemAlarmClockNone();

        // Check notification
        assertNotificationCount(0);

        // Check widget
        assertWidget(R.drawable.ic_alarm_white, "04:30", "Tue");
    }

    @Test
    public void t40_dismissWhileSnoozed() {
        // Consume the alarm with action ACTION_SET_SYSTEM_ALARM
        prepareUntilSnooze();

        // Click in calendar
        startActivityCalendar();
        loadItemAtPosition(1);
        item.performLongClick();

        // TODO Test that the context menu contains the "Dismiss" item (not yet easily supported by Roboletric)

        // Click context menu
        clickContextMenu(R.id.day_dismiss);

        // Check calendar - the Day item is the same
        loadItemAtPosition(0);
        assertThat(textDate.getText(), is("2/1"));
        assertThat(textDoW.getText(), is("Mon"));
        assertThat(textTime.getText(), is("Off"));
        assertThat(textState.getText(), is(""));
        assertThat(textName.getVisibility(), is(View.GONE));
        assertThat(textComment.getText(), is(""));

        // Check calendar - the newly added item for one-time alarm
        loadItemAtPosition(1);
        assertThat(textDate.getText(), is(""));
        assertThat(textDoW.getText(), is(""));
        assertThat(textTime.getText(), is("4:30 AM"));
        assertThat(textState.getText(), is("Passed"));
        assertThat(textName.getVisibility(), is(View.VISIBLE));
        assertThat(textName.getText().toString(), is(""));
        assertThat(textComment.getText(), is(""));

        // Check system alarm
        assertSystemAlarm(YEAR, MONTH, DAY + 1, 0, 0, SystemAlarm.ACTION_SET_SYSTEM_ALARM);

        // Check system alarm clock
        assertSystemAlarmClockNone();

        // Check notification
        assertNotificationCount(0);

        // Check widget
        assertWidget(R.drawable.ic_alarm_off_white, "No alarm", null);
    }

    @Test
    public void t41_setTimeWhileSnoozed() {
        prepareUntilSnooze();

        // Click in calendar
        startActivityCalendar();
        loadItemAtPosition(1);
        item.performLongClick();

        // TODO Test that the context menu contains the "Set time" item (not yet easily supported by Roboletric)

        // Click context menu
        clickContextMenu(R.id.day_set_time);

        // Click the time picker
        TimePickerFragment fragment = (TimePickerFragment) activity.getFragmentManager().findFragmentByTag("timePicker");

        TimePickerDialog dialog = (TimePickerDialog) fragment.getDialog();
        ShadowTimePickerDialog shadowDialog = Shadows.shadowOf(dialog);

        dialog.updateTime(ONE_TIME_ALARM_HOUR + 6, ONE_TIME_ALARM_MINUTE + 6);
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick();

        // Check calendar - the Day item is the same
        loadItemAtPosition(0);
        assertThat(textDate.getText(), is("2/1"));
        assertThat(textDoW.getText(), is("Mon"));
        assertThat(textTime.getText(), is("Off"));
        assertThat(textState.getText(), is(""));
        assertThat(textName.getVisibility(), is(View.GONE));
        assertThat(textComment.getText(), is(""));

        // Check calendar - the newly added item for one-time alarm
        loadItemAtPosition(1);
        assertThat(textDate.getText(), is(""));
        assertThat(textDoW.getText(), is(""));
        assertThat(textTime.getText(), is("10:36 AM"));
        assertThat(textState.getText(), is(""));
        assertThat(textName.getVisibility(), is(View.VISIBLE));
        assertThat(textName.getText().toString(), is(""));
        // FIXME assertThat(textComment.getText(), is("2h 6m"));

        // Check system alarm
        assertSystemAlarm(YEAR, MONTH, DAY, ONE_TIME_ALARM_HOUR - 2 + 6, ONE_TIME_ALARM_MINUTE + 6, SystemAlarm.ACTION_RING_IN_NEAR_FUTURE);

        // Check system alarm clock
        assertSystemAlarmClock(YEAR, MONTH, DAY, ONE_TIME_ALARM_HOUR + 6, ONE_TIME_ALARM_MINUTE + 6);

        // Check notification
        assertNotificationCount(0);

        // Check widget
        assertWidget(R.drawable.ic_alarm_white, "10:36", "Mon");
    }

    @Test
    public void t42_setDateWhileSnoozed() {
        prepareUntilSnooze();

        // Click in calendar
        startActivityCalendar();
        loadItemAtPosition(1);
        headerDate.performClick();

        // Click the date picker
        DatePickerFragment datePickerFragment = (DatePickerFragment) activity.getFragmentManager().findFragmentByTag("datePicker");

        DatePickerDialog dialog = (DatePickerDialog) datePickerFragment.getDialog();
        ShadowDatePickerDialog shadowDialog = Shadows.shadowOf(dialog);

        // Check presets
        assertThat(shadowDialog.getYear(), is(YEAR));
        assertThat(shadowDialog.getMonthOfYear(), is(MONTH));
        assertThat(shadowDialog.getDayOfMonth(), is(DAY));

        // Change the preset time
        dialog.updateDate(YEAR, MONTH, DAY + 1);

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick();

        // Hack: RecyclerView needs to be measured and laid out manually in Robolectric.
        // Source: http://stackoverflow.com/questions/27052866/android-robolectric-click-recyclerview-item
        recyclerView.measure(0, 0);
        recyclerView.layout(0, 0, 100, 10000);

        // Check calendar - the Day item is the same
        loadItemAtPosition(0);
        assertThat(textDate.getText(), is("2/1"));
        assertThat(textDoW.getText(), is("Mon"));
        assertThat(textTime.getText(), is("Off"));
        assertThat(textState.getText(), is(""));
        assertThat(textName.getVisibility(), is(View.GONE));
        assertThat(textComment.getText(), is(""));

        // Check calendar - the Day item is the same
        loadItemAtPosition(1);
        assertThat(textDate.getText(), is("2/2"));
        assertThat(textDoW.getText(), is("Tue"));
        assertThat(textTime.getText(), is("Off"));
        assertThat(textState.getText(), is(""));
        assertThat(textName.getVisibility(), is(View.GONE));
        assertThat(textComment.getText(), is(""));

        // Check calendar - the newly added item for one-time alarm
        loadItemAtPosition(2);
        assertThat(textDate.getText(), is(""));
        assertThat(textDoW.getText(), is(""));
        assertThat(textTime.getText(), is("4:30 AM"));
        assertThat(textState.getText(), is(""));
        assertThat(textName.getVisibility(), is(View.VISIBLE));
        assertThat(textName.getText().toString(), is(""));
        // FIXME assertThat(textComment.getText(), is("1d 3h"));

        // Check system alarm
        assertSystemAlarm(YEAR, MONTH, DAY + 1, ONE_TIME_ALARM_HOUR - 2, ONE_TIME_ALARM_MINUTE, SystemAlarm.ACTION_RING_IN_NEAR_FUTURE);

        // Check system alarm clock
        assertSystemAlarmClock(YEAR, MONTH, DAY + 1, ONE_TIME_ALARM_HOUR, ONE_TIME_ALARM_MINUTE);

        // Check notification
        assertNotificationCount(0);

        // Check widget
        assertWidget(R.drawable.ic_alarm_white, "04:30", "Tue");
    }

    @Test
    public void t43_setNameWhileSnoozed() {
        prepareUntilSnooze();

        // Click in calendar
        startActivityCalendar();
        loadItemAtPosition(1);
        textName.performClick();

        textName.setText("Flowers");

        // Click Done in the soft keyboard
        textName.onEditorAction(EditorInfo.IME_ACTION_DONE);

        // Hack: RecyclerView needs to be measured and laid out manually in Robolectric.
        // Source: http://stackoverflow.com/questions/27052866/android-robolectric-click-recyclerview-item
        recyclerView.measure(0, 0);
        recyclerView.layout(0, 0, 100, 10000);

        // Check calendar - the Day item is the same
        loadItemAtPosition(0);
        assertThat(textDate.getText(), is("2/1"));
        assertThat(textDoW.getText(), is("Mon"));
        assertThat(textTime.getText(), is("Off"));
        assertThat(textState.getText(), is(""));
        assertThat(textName.getVisibility(), is(View.GONE));
        assertThat(textComment.getText(), is(""));

        // Check calendar - the newly added item for one-time alarm
        loadItemAtPosition(1);
        assertThat(textDate.getText(), is(""));
        assertThat(textDoW.getText(), is(""));
        assertThat(textTime.getText(), is("4:30 AM"));
        assertThat(textState.getText(), is("Snoozed"));
        assertThat(textName.getVisibility(), is(View.VISIBLE));
        assertThat(textName.getText().toString(), is("Flowers"));
        // FIXME assertThat(textComment.getText(), is("-0m 20s"));

        // Check system alarm
        assertSystemAlarmNone();

        // Check system alarm clock
        assertSystemAlarmClockNone();

        // Check notification
        assertNotificationCount(0);

        // Check widget
        assertWidget(R.drawable.ic_alarm_white, "04:30", "Tue");
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

        // Shift clock
        shadowGlobalManager.setClock(new FixedClock(new GregorianCalendar(YEAR, MONTH, DAY, ONE_TIME_ALARM_HOUR - 2, ONE_TIME_ALARM_MINUTE)));

        // Call the receiver
        Intent intent = new Intent();
        intent.setAction(SystemAlarm.ACTION_RING_IN_NEAR_FUTURE);
        AlarmReceiver alarmReceiver = new AlarmReceiver();
        alarmReceiver.onReceive(context, intent);

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

        // Consume the alarm with action ACTION_RING
        consumeNextScheduledAlarm();

        shadowGlobalManager.setClock(new FixedClock(new GregorianCalendar(YEAR, MONTH, DAY, ONE_TIME_ALARM_HOUR, ONE_TIME_ALARM_MINUTE, 20)));
    }

    private void consumeNextScheduledAlarm() {
        assertThat(shadowAlarmManager.getScheduledAlarms().size(), is(1));
        shadowAlarmManager.getNextScheduledAlarm();
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

        globalManager.saveOneTimeAlarm(oneTimeAlarm, analytics);
    }

    private void startActivityRing(Calendar alarmTime) {
        Intent ringIntent = new Intent(context, RingActivity.class);
        ringIntent.putExtra(RingActivity.ALARM_TIME, alarmTime);

        activity = buildActivity(RingActivity.class).withIntent(ringIntent).setup().get();
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

        // Hack: RecyclerView needs to be measured and laid out manually in Robolectric.
        // Source: http://stackoverflow.com/questions/27052866/android-robolectric-click-recyclerview-item
        recyclerView.measure(0, 0);
        recyclerView.layout(0, 0, 100, 10000);
    }

    private void loadItemAtPosition(int position) {
        item = recyclerView.getChildAt(position);

        textDate = (TextView) item.findViewById(R.id.textDate);
        textDoW = (TextView) item.findViewById(R.id.textDayOfWeekCal);
        textTime = (TextView) item.findViewById(R.id.textTimeCal);
        textState = (TextView) item.findViewById(R.id.textState);
        textName = (EditText) item.findViewById(R.id.textName);
        textComment = (TextView) item.findViewById(R.id.textComment);
        headerDate = (LinearLayout) item.findViewById(R.id.headerDate);
    }

    private void clickContextMenu(int id) {
        ShadowViewGroup shadowViewGroup = Shadows.shadowOf(recyclerView);
        android.app.Fragment calendarFragment = (CalendarFragment) shadowViewGroup.getOnCreateContextMenuListener();
        final RoboMenuItem contextMenuItem = new RoboMenuItem(id);
        calendarFragment.onContextItemSelected(contextMenuItem);
    }

    private void assertSystemAlarm(int year, int month, int day, int hour, int minute, String action) {
        CalendarTest.assertSystemAlarm(context, shadowAlarmManager, year, month, day, hour, minute, action);
    }

    private void assertSystemAlarmNone() {
        CalendarTest.assertSystemAlarmNone(shadowAlarmManager);
    }

    private void assertSystemAlarmClock(int year, int month, int day, int hour, int minute) {
        CalendarTest.assertSystemAlarmClock(context, alarmManager, shadowAlarmManager, year, month, day, hour, minute);
    }

    private void assertSystemAlarmClockNone() {
        CalendarTest.assertSystemAlarmClockNone(alarmManager);
    }

    private void assertNotificationCount(int count) {
        CalendarTest.assertNotificationCount(shadowNotificationManager, count);
    }

    private void assertNotification(Notification notification, String bigContentTitle, String bigContentText) {
        CalendarTest.assertNotification(notification, bigContentTitle, bigContentText);
    }

    private void assertNotificationActionCount(Notification notification, int count) {
        CalendarTest.assertNotificationActionCount(notification, count);
    }

    private void assertNotificationAction(Notification notification, int index, String title, String actionString) {
        CalendarTest.assertNotificationAction(context, notification, index, title, actionString);
    }

    private void assertWidget(int iconResId, String time, String date) {
        CalendarTest.assertWidget(context, shadowAppWidgetManager, iconResId, time, date);
    }

}
