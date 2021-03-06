package cz.jaro.alarmmorning.app;

import android.app.AlarmManager;
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
import android.os.Build;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

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
import cz.jaro.alarmmorning.BootReceiverTest;
import cz.jaro.alarmmorning.CalendarFragment;
import cz.jaro.alarmmorning.DatePickerFragment;
import cz.jaro.alarmmorning.FixedTimeTest;
import cz.jaro.alarmmorning.GlobalManager;
import cz.jaro.alarmmorning.R;
import cz.jaro.alarmmorning.RingActivity;
import cz.jaro.alarmmorning.SettingsActivity;
import cz.jaro.alarmmorning.SharedPreferencesHelper;
import cz.jaro.alarmmorning.TimePickerFragment;
import cz.jaro.alarmmorning.WidgetProvider;
import cz.jaro.alarmmorning.graphics.SlideButton;
import cz.jaro.alarmmorning.model.AppAlarm;
import cz.jaro.alarmmorning.receivers.AlarmReceiver;
import cz.jaro.alarmmorning.receivers.NotificationReceiver;
import cz.jaro.alarmmorning.receivers.VoidReceiver;
import cz.jaro.alarmmorning.shadows.ShadowGlobalManager;
import cz.jaro.alarmmorning.wizard.Wizard;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
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

    AppCompatActivity activity;
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
            RingActivity ringActivity = (RingActivity) activity;
            ringActivity.shutdown();
        }

        // Cancel all notifications
        notificationManager.cancelAll();
    }

    public static void setNearFuturePeriodPreferenceToZero(Context context) {
        setNearFuturePeriodPreference(context, 0);
    }

    public static void setNearFuturePeriodPreference(Context context, int minutes) {
        SharedPreferencesHelper.save(SettingsActivity.PREF_NEAR_FUTURE_TIME, minutes);
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

    void startActivityRing(AppAlarm appAlarm) {
        Intent ringIntent = new Intent(context, RingActivity.class);
        ringIntent.putExtra(GlobalManager.PERSIST_ALARM_TYPE, appAlarm.getClass().getSimpleName());
        ringIntent.putExtra(GlobalManager.PERSIST_ALARM_ID, appAlarm.getPersistenceId());

        activity = buildActivity(RingActivity.class, ringIntent).setup().get();
        shadowActivity = Shadows.shadowOf(activity);

        AlarmMorningActivityTest.setLocale(activity, "en", "US");

        textDate = activity.findViewById(R.id.date);
        textTime = activity.findViewById(R.id.time);
        textAlarmTime = activity.findViewById(R.id.alarmTime);
        textOneTimeAlarmName = activity.findViewById(R.id.oneTimeAlarmName);
        textNextCalendar = activity.findViewById(R.id.nextCalendar);
        textMuted = activity.findViewById(R.id.muted);

        snoozeButton = activity.findViewById(R.id.snoozeButton);
        dismissButton = activity.findViewById(R.id.dismissButton);
    }

    void startActivityCalendar() {
        activity = Robolectric.setupActivity(AlarmMorningActivity.class);
        shadowActivity = Shadows.shadowOf(activity);

        recyclerView = activity.findViewById(R.id.calendar_recycler_view);

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

        textDate = item.findViewById(R.id.textDate);
        textDoW = item.findViewById(R.id.textDayOfWeekCal);
        textTime = item.findViewById(R.id.textTimeCal);
        textState = item.findViewById(R.id.textState);
        textName = item.findViewById(R.id.textName);
        textComment = item.findViewById(R.id.textComment);
        headerDate = item.findViewById(R.id.headerDate);
    }

    void clickContextMenu(int id) {
        clickContextMenu(recyclerView, id);
    }

    public static void clickContextMenu(RecyclerView recyclerView, int id) {
        final MenuItem contextMenuItem = new RoboMenuItem(id);

        // Check that the context menu contains this id and that this id is visible
        assertTrue("MenuItem visible", contextMenuItem.isVisible());

        // Select the context menu item
        ShadowViewGroup shadowViewGroup = Shadows.shadowOf(recyclerView);
        Fragment calendarFragment = (CalendarFragment) shadowViewGroup.getOnCreateContextMenuListener();
        calendarFragment.onContextItemSelected(contextMenuItem);
    }

    void picker_setTime(int hourCheck, int minuteCheck, int hour, int minute) {
        // Time picker
        TimePickerFragment timePickerFragment = (TimePickerFragment) activity.getSupportFragmentManager().findFragmentByTag("timePicker");

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
        if (name == null)
            assertThat("Name visibility", textName.getVisibility(), is(View.GONE));
        else {
            assertThat("Name visibility", textName.getVisibility(), is(View.VISIBLE));
            assertThat("Name", textName.getText().toString(), is(name));
        }
        assertThat("State", textState.getText(), is(state));
        assertThat("Comment", textComment.getText(), is(comment));
    }

    void consumeNextScheduledAlarm() {
        BootReceiverTest.consumeSystemAlarm(shadowAlarmManager);
    }

    void assertAndConsumeSystemAlarm(int year, int month, int day, int hour, int minute, String action) {
        BootReceiverTest.assertSystemAlarm(context, shadowAlarmManager, year, month, day, hour, minute, AlarmReceiver.class, action);
        BootReceiverTest.consumeSystemAlarm(shadowAlarmManager);
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
            assertIntent(context, alarmClockInfo.getShowIntent(), AlarmMorningActivity.class, null);

            // Operation intent
            assertIntent(context, shadowAlarmManager.getNextScheduledAlarm().operation, VoidReceiver.class, null);
        }
    }

    private static void assertIntent(Context context, PendingIntent intent, Class<?> cls, String action) {
        ShadowPendingIntent shadowIntent = Shadows.shadowOf(intent);

        Intent expectedIntent = new Intent(context, cls);

        assertThat("Broadcast", shadowIntent.isBroadcastIntent(), is(true));
        assertThat("Intent count", shadowIntent.getSavedIntents().length, is(1));
        assertThat("Class", shadowIntent.getSavedIntents()[0].getComponent(), is(expectedIntent.getComponent()));
        if (action == null)
            assertNull("Action", shadowIntent.getSavedIntent().getAction());
        else
            assertThat("Action", shadowIntent.getSavedIntents()[0].getAction(), is(action));
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

    public static void assertNotification(Notification notification, String contentTitle, String contentText) {
        ShadowNotification shadowNotification = Shadows.shadowOf(notification);

        assertThat("Notification title", shadowNotification.getContentTitle(), is(contentTitle));
        assertThat("Notification text", shadowNotification.getContentText(), is(contentText));
    }

    public static void assertNotificationActionCount(Notification notification, int count) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            assertThat("Notification action count", notification.actions != null ? notification.actions.length : 0, is(count));
        }
    }

    void assertNotificationAction(Notification notification, int index, String title, String actionString) {
        assertNotificationAction(context, notification, index, title, actionString);
    }

    public static void assertNotificationAction(Context context, Notification notification, int index, String title, String actionString) {
        assertNotificationAction(context, notification, index, title, actionString, NotificationReceiver.class);
    }

    public static void assertNotificationAction(Context context, Notification notification, int index, String title, String actionString, Class expectedIntentClass) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            Notification.Action actionButton = notification.actions[index];
            assertThat("Notification action title", actionButton.title, is(title));

            assertIntent(context, actionButton.actionIntent, expectedIntentClass, actionString);
        }
    }

    void assertWidget(int iconResId, String time, String date) {
        assertWidget(context, shadowAppWidgetManager, iconResId, time, date);
    }

    public static void assertWidget(Context context, ShadowAppWidgetManager shadowAppWidgetManager, int iconResId, String time, String date) {
        int widgetId = shadowAppWidgetManager.createWidget(WidgetProvider.class, R.layout.widget_layout);
        View view = shadowAppWidgetManager.getViewFor(widgetId);

        ImageView widgetIcon = view.findViewById(R.id.icon);
        ShadowDrawable shadowWidgetIconDrawable = Shadows.shadowOf(widgetIcon.getDrawable());
        TextView widgetTime = view.findViewById(R.id.alarm_time);
        TextView widgetDate = view.findViewById(R.id.alarm_date);

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
        DatePickerFragment datePickerFragment = (DatePickerFragment) activity.getSupportFragmentManager().findFragmentByTag("datePicker");

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
