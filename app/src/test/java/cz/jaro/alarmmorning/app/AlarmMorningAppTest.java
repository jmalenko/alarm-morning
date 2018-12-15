package cz.jaro.alarmmorning.app;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
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
import org.robolectric.shadows.ShadowDatePickerDialog;
import org.robolectric.shadows.ShadowDrawable;
import org.robolectric.shadows.ShadowNotification;
import org.robolectric.shadows.ShadowNotificationManager;
import org.robolectric.shadows.ShadowPendingIntent;
import org.robolectric.shadows.ShadowTimePickerDialog;
import org.robolectric.shadows.ShadowViewGroup;

import java.util.Calendar;
import java.util.GregorianCalendar;

import cz.jaro.alarmmorning.AlarmMorningActivity;
import cz.jaro.alarmmorning.AlarmMorningActivityTest;
import cz.jaro.alarmmorning.CalendarFragment;
import cz.jaro.alarmmorning.DatePickerFragment;
import cz.jaro.alarmmorning.FixedTimeTest;
import cz.jaro.alarmmorning.R;
import cz.jaro.alarmmorning.RingActivity;
import cz.jaro.alarmmorning.SettingsActivity;
import cz.jaro.alarmmorning.TimePickerFragment;
import cz.jaro.alarmmorning.WidgetProvider;
import cz.jaro.alarmmorning.graphics.SlideButton;
import cz.jaro.alarmmorning.receivers.AlarmReceiver;
import cz.jaro.alarmmorning.receivers.NotificationReceiver;
import cz.jaro.alarmmorning.receivers.VoidReceiver;
import cz.jaro.alarmmorning.shadows.ShadowGlobalManager;
import cz.jaro.alarmmorning.wizard.Wizard;

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
public class AlarmMorningAppTest extends FixedTimeTest {
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

    public static void setNearFuturePeriodPreferenceToZero(Context context) {
        setNearFuturePeriodPreference(context, 0);
    }

    public static void setNearFuturePeriodPreference(Context context, int minutes) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt(SettingsActivity.PREF_NEAR_FUTURE_TIME, minutes);
        editor.commit();
    }

    int calendar_setDayAlarm(int itemPosition, int hourCheck, int minuteCheck, int hour, int minute) {
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

    void startActivityRing(Calendar alarmTime) {
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

    void startActivityCalendar() {
        activity = Robolectric.setupActivity(AlarmMorningActivity.class);
        shadowActivity = Shadows.shadowOf(activity);

        recyclerView = (RecyclerView) shadowActivity.findViewById(R.id.calendar_recycler_view);

        refreshRecyclerView();
    }

    void refreshRecyclerView() {
        refreshRecyclerView(recyclerView);
    }

    public static void refreshRecyclerView(RecyclerView recyclerView) {
        // XXX Workaround Robolectric - RecyclerView needs to be measured and laid out manually in Robolectric.
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

    void assertSystemAlarm(int year, int month, int day, int hour, int minute, String action) {
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

    void assertSystemAlarmCount(int count) {
        assertSystemAlarmCount(shadowAlarmManager, count);
    }

    public static void assertSystemAlarmCount(ShadowAlarmManager shadowAlarmManager, int count) {
        assertThat("Alarm count", shadowAlarmManager.getScheduledAlarms().size(), is(count));
    }

    void assertSystemAlarmClock(int year, int month, int day, int hour, int minute) {
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

    void assertSystemAlarmClockNone() {
        assertSystemAlarmClockNone(alarmManager);
    }

    public static void assertSystemAlarmClockNone(AlarmManager alarmManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            AlarmManager.AlarmClockInfo alarmClockInfo = alarmManager.getNextAlarmClock();
            assertNull(alarmClockInfo);
        }
    }

    void assertNotificationCount(int count) {
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            assertThat("Notification action count", notification.actions.length, is(count));
        }
    }

    void assertNotificationAction(Notification notification, int index, String title, String actionString) {
        assertNotificationAction(context, notification, index, title, actionString);
    }

    public static void assertNotificationAction(Context context, Notification notification, int index, String title, String actionString) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
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

    void assertWidget(int iconResId, String time, String date) {
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
        assertThat("Time text", widgetTime.getText().toString(), is(time));
        if (date == null) {
            assertThat("Date visibility", widgetDate.getVisibility(), is(View.GONE));
        } else {
            assertThat("Date visibility", widgetDate.getVisibility(), is(View.VISIBLE));
            assertThat("Date text", widgetDate.getText().toString(), is(date));
        }
    }

    private void picker_setDate(int yearCheck, int monthCheck, int dayCheck, int year, int month, int day) {
        DatePickerFragment datePickerFragment = (DatePickerFragment) activity.getFragmentManager().findFragmentByTag("datePicker");

        DatePickerDialog dialog = (DatePickerDialog) datePickerFragment.getDialog();
        ShadowDatePickerDialog shadowDialog = Shadows.shadowOf(dialog);

        // Check presets
        assertThat("Preset year", shadowDialog.getYear(), is(yearCheck));
        assertThat("Preset month", shadowDialog.getMonthOfYear(), is(monthCheck));
        assertThat("Preset day", shadowDialog.getDayOfMonth(), is(dayCheck));

        // Change the date
        dialog.updateDate(year, month, day);

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick();
    }

    int calendar_setDate(int itemPosition, int yearCheck, int monthCheck, int dayCheck, int year, int month, int day) {
        // Start activity
        startActivityCalendar();

        int itemCount = recyclerView.getChildCount();

        // Context menu
        loadItemAtPosition(itemPosition);

        headerDate.performClick();

        // Set date
        picker_setDate(yearCheck, monthCheck, dayCheck, year, month, day);

        refreshRecyclerView();

        return itemCount;
    }

    int calendar_setTime(int itemPosition, int hourCheck, int minuteCheck, int hour, int minute) {
        // Start activity
        startActivityCalendar();

        int itemCount = recyclerView.getChildCount();

        // Context menu
        loadItemAtPosition(itemPosition);

        item.performClick();

        // Set time
        picker_setTime(hourCheck, minuteCheck, hour, minute);

        refreshRecyclerView();

        return itemCount;
    }

    int calendar_addOneTimeAlarm(int itemPosition, int hourCheck, int minuteCheck, int hour, int minute) {
        // Start activity
        startActivityCalendar();

        int itemCount = recyclerView.getChildCount();

        // Context menu
        loadItemAtPosition(itemPosition);

        item.performLongClick(); // Show context menu

        clickContextMenu(R.id.action_day_add_alarm); // Select the item in context menu

        // Set time
        picker_setTime(hourCheck, minuteCheck, hour, minute);

        refreshRecyclerView();

        return itemCount;
    }
}
