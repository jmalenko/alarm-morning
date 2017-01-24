package cz.jaro.alarmmorning;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

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
import cz.jaro.alarmmorning.shadows.ShadowAlarmManagerAPI21;
import cz.jaro.alarmmorning.shadows.ShadowGlobalManager;
import cz.jaro.alarmmorning.wizard.Wizard;

import static cz.jaro.alarmmorning.model.AlarmDbHelper.DEFAULT_ALARM_HOUR;
import static cz.jaro.alarmmorning.model.AlarmDbHelper.DEFAULT_ALARM_MINUTE;
import static cz.jaro.alarmmorning.model.DayTest.DAY;
import static cz.jaro.alarmmorning.model.DayTest.MONTH;
import static cz.jaro.alarmmorning.model.DayTest.YEAR;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

/**
 * Tests of alarm management in UI.
 */
@Config(constants = BuildConfig.class, sdk = 21, shadows = {ShadowAlarmManagerAPI21.class, ShadowGlobalManager.class})
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class CalendarTest extends FixedTimeTest {

    private Context context;
    private ShadowAlarmManager shadowAlarmManager;

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

        AlarmManager alarmManager = (AlarmManager) RuntimeEnvironment.application.getSystemService(Context.ALARM_SERVICE);
        shadowAlarmManager = Shadows.shadowOf(alarmManager);

        globalManager.forceSetAlarm();
    }

    @Test
    public void t00_noAlarmIsScheduled() {
        // Check system alarm
        assertSystemAlarm(DayTest.YEAR, DayTest.MONTH, DayTest.DAY + 1, 0, 0, SystemAlarm.ACTION_SET_SYSTEM_ALARM);
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
        assertSystemAlarm(DayTest.YEAR, DayTest.MONTH, DayTest.DAY, DEFAULT_ALARM_HOUR - 2, DEFAULT_ALARM_MINUTE, SystemAlarm.ACTION_RING_IN_NEAR_FUTURE);
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
        assertSystemAlarm(DayTest.YEAR, DayTest.MONTH, DayTest.DAY, DEFAULT_ALARM_HOUR, DEFAULT_ALARM_MINUTE + 2, SystemAlarm.ACTION_RING_IN_NEAR_FUTURE);
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
        assertSystemAlarm(DayTest.YEAR, DayTest.MONTH, DayTest.DAY, DEFAULT_ALARM_HOUR, DEFAULT_ALARM_MINUTE, SystemAlarm.ACTION_RING);
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
        assertSystemAlarm(DayTest.YEAR, DayTest.MONTH, DayTest.DAY + 1, DEFAULT_ALARM_HOUR - 2, DEFAULT_ALARM_MINUTE, SystemAlarm.ACTION_RING_IN_NEAR_FUTURE);
    }

    @Test
    public void t20_onNear() {
        // Consume the alarm with action ACTION_SET_SYSTEM_ALARM
        consumeNextScheduledAlarm();

        // Shift clock
        shadowGlobalManager.setClock(new FixedClock(new GregorianCalendar(YEAR, MONTH, DAY, DayTest.HOUR_DEFAULT - 2, DayTest.MINUTE_DEFAULT)));
        // Save day
        setAlarmToToday();

        // Call the receiver
        Intent intent = new Intent();
        intent.setAction(SystemAlarm.ACTION_RING_IN_NEAR_FUTURE);
        AlarmReceiver alarmReceiver = new AlarmReceiver();
        alarmReceiver.onReceive(context, intent);

        // Check system alarm
        assertSystemAlarm(DayTest.YEAR, DayTest.MONTH, DayTest.DAY, DEFAULT_ALARM_HOUR, DEFAULT_ALARM_MINUTE, SystemAlarm.ACTION_RING);

    }

    @Test
    @Config(qualifiers = "en")
    public void t21_dismissBeforeRinging() {
        // Consume the alarm with action ACTION_SET_SYSTEM_ALARM
        consumeNextScheduledAlarm();

        // Save day
        setAlarmToToday();
        // Shift clock
        shadowGlobalManager.setClock(new FixedClock(new GregorianCalendar(YEAR, MONTH, DAY, DayTest.HOUR_DEFAULT - 2, DayTest.MINUTE_DEFAULT, 10))); // 10 seconds after near time
        // Set state
        Calendar alarmTime = new GregorianCalendar(YEAR, MONTH, DAY, DayTest.HOUR_DEFAULT, DayTest.MINUTE_DEFAULT);
        globalManager.setState(GlobalManager.STATE_FUTURE, alarmTime);

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
        assertSystemAlarm(DayTest.YEAR, DayTest.MONTH, DayTest.DAY, DEFAULT_ALARM_HOUR, DEFAULT_ALARM_MINUTE, SystemAlarm.ACTION_ALARM_TIME_OF_EARLY_DISMISSED_ALARM);
    }

    @Test
    public void t30_onRing() {
        // Consume the alarm with action ACTION_SET_SYSTEM_ALARM
        consumeNextScheduledAlarm();

        // Shift clock
        shadowGlobalManager.setClock(new FixedClock(new GregorianCalendar(YEAR, MONTH, DAY, DayTest.HOUR_DEFAULT, DayTest.MINUTE_DEFAULT)));
        // Save day
        setAlarmToToday();

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
        assertSystemAlarm(DayTest.YEAR, DayTest.MONTH, DayTest.DAY + 1, 0, 0, SystemAlarm.ACTION_SET_SYSTEM_ALARM);
    }

    @Test
    @Config(qualifiers = "en")
    public void t31_dismissWhileRinging() {
        // Consume the alarm with action ACTION_SET_SYSTEM_ALARM
        consumeNextScheduledAlarm();

        // Save day
        setAlarmToToday();
        // Shift clock
        shadowGlobalManager.setClock(new FixedClock(new GregorianCalendar(YEAR, MONTH, DAY, DayTest.HOUR_DEFAULT, DayTest.MINUTE_DEFAULT, 10))); // 10 seconds after alarm
        // Set state
        Calendar alarmTime = new GregorianCalendar(YEAR, MONTH, DAY, DayTest.HOUR_DEFAULT, DayTest.MINUTE_DEFAULT);
        globalManager.setState(GlobalManager.STATE_RINGING, alarmTime);

        // Consume the alarm with action ACTION_RING
        consumeNextScheduledAlarm();

        // Start ring activity
        startActivityRing(alarmTime);

        // Check appearance
        assertThat(textDate.getVisibility(), is(View.VISIBLE));
        assertThat(textTime.getVisibility(), is(View.VISIBLE));
        assertThat(textAlarmTime.getVisibility(), is(View.INVISIBLE));
        assertThat(textNextCalendar.getVisibility(), is(View.GONE));
        assertThat(textMuted.getVisibility(), is(View.INVISIBLE));

//        assertThat(textDate.getText(), is("Monday, February 1")); // TODO Fix test
//        assertThat(textTime.getText(), is("07:00")); // TODO Fix test

        dismissButton.performClick();

        // Check system alarm
        assertThat(shadowAlarmManager.getScheduledAlarms().size(), is(0));
        // The alarm that was consumed at the beginning of this method didn't change

        // Cleanup: close activity
        ((RingActivity) activity).shutdown();
    }

    @Test
    @Config(qualifiers = "en")
    public void t32_snoozeWhileRinging() {
        // Consume the alarm with action ACTION_SET_SYSTEM_ALARM
        consumeNextScheduledAlarm();

        // Save day
        setAlarmToToday();
        // Shift clock
        shadowGlobalManager.setClock(new FixedClock(new GregorianCalendar(YEAR, MONTH, DAY, DayTest.HOUR_DEFAULT, DayTest.MINUTE_DEFAULT, 10))); // 10 seconds after alarm
        // Set state
        Calendar alarmTime = new GregorianCalendar(YEAR, MONTH, DAY, DayTest.HOUR_DEFAULT, DayTest.MINUTE_DEFAULT);
        globalManager.setState(GlobalManager.STATE_RINGING, alarmTime);

        // Consume the alarm with action ACTION_RING
        consumeNextScheduledAlarm();

        // Start ring activity
        startActivityRing(alarmTime);

        snoozeButton.performClick();

        // Check system alarm
        assertSystemAlarm(DayTest.YEAR, DayTest.MONTH, DayTest.DAY, DEFAULT_ALARM_HOUR, DEFAULT_ALARM_MINUTE + 10, SystemAlarm.ACTION_RING);

        // Cleanup: close activity
        ((RingActivity) activity).shutdown();
    }

    @Test
    public void t40_dismissWhileSnoozed() {
        // Consume the alarm with action ACTION_SET_SYSTEM_ALARM
        consumeNextScheduledAlarm();

        // Save day
        setAlarmToToday();
        // Shift clock
        shadowGlobalManager.setClock(new FixedClock(new GregorianCalendar(YEAR, MONTH, DAY, DayTest.HOUR_DEFAULT, DayTest.MINUTE_DEFAULT, 10))); // 10 seconds after alarm
        // Set state
        Calendar alarmTime = new GregorianCalendar(YEAR, MONTH, DAY, DayTest.HOUR_DEFAULT, DayTest.MINUTE_DEFAULT);
        globalManager.setState(GlobalManager.STATE_SNOOZED, alarmTime);

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
        assertSystemAlarm(DayTest.YEAR, DayTest.MONTH, DayTest.DAY + 1, 0, 0, SystemAlarm.ACTION_SET_SYSTEM_ALARM);
    }

    @Test
    public void t41_setTimeWhileSnoozed() {
        // Consume the alarm with action ACTION_SET_SYSTEM_ALARM
        consumeNextScheduledAlarm();

        // Save day
        setAlarmToToday();
        // Shift clock
        shadowGlobalManager.setClock(new FixedClock(new GregorianCalendar(YEAR, MONTH, DAY, DayTest.HOUR_DEFAULT, DayTest.MINUTE_DEFAULT, 10))); // 10 seconds after alarm
        // Set state
        Calendar alarmTime = new GregorianCalendar(YEAR, MONTH, DAY, DayTest.HOUR_DEFAULT, DayTest.MINUTE_DEFAULT);
        globalManager.setState(GlobalManager.STATE_SNOOZED, alarmTime);

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
        assertSystemAlarm(DayTest.YEAR, DayTest.MONTH, DayTest.DAY, DEFAULT_ALARM_HOUR + 4, DEFAULT_ALARM_MINUTE, SystemAlarm.ACTION_RING_IN_NEAR_FUTURE);
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
        // Shift clock
        shadowGlobalManager.setClock(new FixedClock(new GregorianCalendar(YEAR, MONTH, DAY, DayTest.HOUR_DEFAULT, DayTest.MINUTE_DEFAULT, 10))); // 10 seconds after alarm
        // Set state
        Calendar alarmTime = new GregorianCalendar(YEAR, MONTH, DAY, DayTest.HOUR_DEFAULT, DayTest.MINUTE_DEFAULT);
        globalManager.setState(GlobalManager.STATE_SNOOZED, alarmTime);

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
        assertSystemAlarm(DayTest.YEAR, DayTest.MONTH, DayTest.DAY + 1, 0, 0, SystemAlarm.ACTION_SET_SYSTEM_ALARM);
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
        // Shift clock
        shadowGlobalManager.setClock(new FixedClock(new GregorianCalendar(YEAR, MONTH, DAY, DayTest.HOUR_DEFAULT, DayTest.MINUTE_DEFAULT, 10))); // 10 seconds after alarm
        // Set state
        Calendar alarmTime = new GregorianCalendar(YEAR, MONTH, DAY, DayTest.HOUR_DEFAULT, DayTest.MINUTE_DEFAULT);
        globalManager.setState(GlobalManager.STATE_SNOOZED, alarmTime);

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
        assertSystemAlarm(DayTest.YEAR, DayTest.MONTH, DayTest.DAY + 1, 0, 0, SystemAlarm.ACTION_SET_SYSTEM_ALARM);
    }

    private void consumeNextScheduledAlarm() {
        assertThat(shadowAlarmManager.getScheduledAlarms().size(), is(1));
        shadowAlarmManager.getNextScheduledAlarm();
    }

    private void setAlarmToToday() {
        Calendar date = new GregorianCalendar(DayTest.YEAR, DayTest.MONTH, DayTest.DAY);

        Day day = new Day();
        day.setDate(date);
        day.setState(Day.STATE_ENABLED);
        day.setHour(DayTest.HOUR_DEFAULT);
        day.setMinute(DayTest.MINUTE_DEFAULT);

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
        ShadowPendingIntent shadowPendingIntent = Shadows.shadowOf(operation);

        Intent expectedIntent = new Intent(context, AlarmReceiver.class);

        assertThat("Broadcast", shadowPendingIntent.isBroadcastIntent(), is(true));
        assertThat("Intent count", shadowPendingIntent.getSavedIntents().length, is(1));
        assertThat("Class", shadowPendingIntent.getSavedIntents()[0].getComponent(), is(expectedIntent.getComponent()));
        assertThat("Action", shadowPendingIntent.getSavedIntent().getAction(), is(action));
    }

}
