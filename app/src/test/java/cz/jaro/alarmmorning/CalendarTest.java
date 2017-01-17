package cz.jaro.alarmmorning;

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
import android.widget.TextView;

import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.robolectric.Robolectric;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowActivity;
import org.robolectric.shadows.ShadowAlarmManager;
import org.robolectric.shadows.ShadowPendingIntent;
import org.robolectric.shadows.ShadowTimePickerDialog;

import java.util.Calendar;
import java.util.GregorianCalendar;

import cz.jaro.alarmmorning.model.DayTest;
import cz.jaro.alarmmorning.receivers.AlarmReceiver;
import cz.jaro.alarmmorning.shadows.ShadowAlarmManagerAPI21;
import cz.jaro.alarmmorning.shadows.ShadowGlobalManager;
import cz.jaro.alarmmorning.wizard.Wizard;

import static cz.jaro.alarmmorning.model.AlarmDbHelper.DEFAULT_ALARM_HOUR;
import static cz.jaro.alarmmorning.model.AlarmDbHelper.DEFAULT_ALARM_MINUTE;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

/**
 * Tests of UI.
 */
@Config(constants = BuildConfig.class, sdk = 21, shadows = {ShadowAlarmManagerAPI21.class, ShadowGlobalManager.class})
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class CalendarTest extends FixedTimeTest {

    private Context context;
    private ShadowAlarmManager shadowAlarmManager;

    @Before
    public void before() {
        super.before();

        AlarmMorningActivityTest.saveWizardPreference(!Wizard.PREF_WIZARD_DEFAULT);

        context = RuntimeEnvironment.application.getApplicationContext();

        AlarmManager alarmManager = (AlarmManager) RuntimeEnvironment.application.getSystemService(Context.ALARM_SERVICE);
        shadowAlarmManager = Shadows.shadowOf(alarmManager);
    }

    @Test
    public void t00_noAlarmIsScheduled() {
        // Check system alarm
        assertThat(shadowAlarmManager.getScheduledAlarms().size(), is(1));

        ShadowAlarmManager.ScheduledAlarm nextScheduledAlarm = shadowAlarmManager.peekNextScheduledAlarm();

        assertThat(nextScheduledAlarm.type, is(AlarmManager.RTC_WAKEUP));

        Calendar time = GregorianCalendar.getInstance();
        time.setTimeInMillis(nextScheduledAlarm.triggerAtTime);
        assertThat("Year", time.get(Calendar.YEAR), is(DayTest.YEAR));
        assertThat("Month", time.get(Calendar.MONTH), is(DayTest.MONTH));
        assertThat("Date", time.get(Calendar.DAY_OF_MONTH), is(DayTest.DAY + 1));
        assertThat("Hour", time.get(Calendar.HOUR_OF_DAY), is(0));
        assertThat("Minute", time.get(Calendar.MINUTE), is(0));
        assertThat("Second", time.get(Calendar.SECOND), is(0));
        assertThat("Millisecond", time.get(Calendar.MILLISECOND), is(0));

        PendingIntent operation = nextScheduledAlarm.operation;
        ShadowPendingIntent shadowPendingIntent = Shadows.shadowOf(operation);

        Intent expectedIntent = new Intent(context, AlarmReceiver.class);

        assertThat(shadowPendingIntent.isBroadcastIntent(), is(true));
        assertThat(shadowPendingIntent.getSavedIntents().length, is(1));
        assertThat(shadowPendingIntent.getSavedIntents()[0].getComponent(), is(expectedIntent.getComponent()));
        assertThat(shadowPendingIntent.getSavedIntent().getAction(), is(SystemAlarm.ACTION_SET_SYSTEM_ALARM));
    }

    @Test
    @Config(qualifiers = "en")
    public void t10_setAlarm() {
        // Consume the alarm with action ACTION_SET_SYSTEM_ALARM
        ShadowAlarmManager.ScheduledAlarm nextScheduledAlarm1 = shadowAlarmManager.getNextScheduledAlarm();
        assertThat(nextScheduledAlarm1.type, is(AlarmManager.RTC_WAKEUP));

        // Click in calendar
        AlarmMorningActivity activity = Robolectric.setupActivity(AlarmMorningActivity.class);
        ShadowActivity shadowActivity = Shadows.shadowOf(activity);

        AlarmMorningActivityTest.setLocale(activity, "en", "US");

        RecyclerView recyclerView = (RecyclerView) shadowActivity.findViewById(R.id.calendar_recycler_view);

        // Hack: RecyclerView needs to be measured and layed out manually in Robolectric.
        // Source: http://stackoverflow.com/questions/27052866/android-robolectric-click-recyclerview-item
        recyclerView.measure(0, 0);
        recyclerView.layout(0, 0, 100, 10000);

        View item = recyclerView.getChildAt(0);

        TextView textDate = (TextView) item.findViewById(R.id.textDate);
        TextView textTime = (TextView) item.findViewById(R.id.textTimeCal);
        TextView textState = (TextView) item.findViewById(R.id.textState);

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
//        assertThat(textTime.getText(), is("07:00")); // TODO Fix test
        assertThat(textState.getText(), is("Changed"));

        // Check system alarm
        assertThat(shadowAlarmManager.getScheduledAlarms().size(), is(1));

        ShadowAlarmManager.ScheduledAlarm nextScheduledAlarm = shadowAlarmManager.peekNextScheduledAlarm();

        assertThat(nextScheduledAlarm.type, is(AlarmManager.RTC_WAKEUP));

        Calendar time = GregorianCalendar.getInstance();
        time.setTimeInMillis(nextScheduledAlarm.triggerAtTime);
        assertThat("Year", time.get(Calendar.YEAR), is(DayTest.YEAR));
        assertThat("Month", time.get(Calendar.MONTH), is(DayTest.MONTH));
        assertThat("Date", time.get(Calendar.DAY_OF_MONTH), is(DayTest.DAY));
        assertThat("Hour", time.get(Calendar.HOUR_OF_DAY), is(DEFAULT_ALARM_HOUR - 2));
        assertThat("Minute", time.get(Calendar.MINUTE), is(DEFAULT_ALARM_MINUTE));
        assertThat("Second", time.get(Calendar.SECOND), is(0));
        assertThat("Millisecond", time.get(Calendar.MILLISECOND), is(0));

        PendingIntent operation = nextScheduledAlarm.operation;
        ShadowPendingIntent shadowPendingIntent = Shadows.shadowOf(operation);

        Intent expectedIntent = new Intent(context, AlarmReceiver.class);

        assertThat(shadowPendingIntent.isBroadcastIntent(), is(true));
        assertThat(shadowPendingIntent.getSavedIntents().length, is(1));
        assertThat(shadowPendingIntent.getSavedIntents()[0].getComponent(), is(expectedIntent.getComponent()));
        assertThat(shadowPendingIntent.getSavedIntent().getAction(), is(SystemAlarm.ACTION_RING_IN_NEAR_FUTURE));
    }

    @Test
    @Config(qualifiers = "en")
    public void t11_setAlarmTwice() {
        ShadowAlarmManager.ScheduledAlarm nextScheduledAlarm1 = shadowAlarmManager.getNextScheduledAlarm();
        assertThat(nextScheduledAlarm1.type, is(AlarmManager.RTC_WAKEUP));

        // Click in calendar
        AlarmMorningActivity activity = Robolectric.setupActivity(AlarmMorningActivity.class);
        ShadowActivity shadowActivity = Shadows.shadowOf(activity);

        AlarmMorningActivityTest.setLocale(activity, "en", "US");

        RecyclerView recyclerView = (RecyclerView) shadowActivity.findViewById(R.id.calendar_recycler_view);

        // Hack: RecyclerView needs to be measured and layed out manually in Robolectric.
        // Source: http://stackoverflow.com/questions/27052866/android-robolectric-click-recyclerview-item
        recyclerView.measure(0, 0);
        recyclerView.layout(0, 0, 100, 10000);

        View item = recyclerView.getChildAt(0);

        TextView textDate = (TextView) item.findViewById(R.id.textDate);
        TextView textTime = (TextView) item.findViewById(R.id.textTimeCal);
        TextView textState = (TextView) item.findViewById(R.id.textState);

//        assertThat(textDate.getText(), is("2/1")); // TODO Fix test
        assertThat(textTime.getText(), is("Off"));
        assertThat(textState.getText(), is(""));

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
        assertThat(shadowAlarmManager.getScheduledAlarms().size(), is(1));

        ShadowAlarmManager.ScheduledAlarm nextScheduledAlarm = shadowAlarmManager.peekNextScheduledAlarm();

        assertThat(nextScheduledAlarm.type, is(AlarmManager.RTC_WAKEUP));

        Calendar time = GregorianCalendar.getInstance();
        time.setTimeInMillis(nextScheduledAlarm.triggerAtTime);
        assertThat("Year", time.get(Calendar.YEAR), is(DayTest.YEAR));
        assertThat("Month", time.get(Calendar.MONTH), is(DayTest.MONTH));
        assertThat("Date", time.get(Calendar.DAY_OF_MONTH), is(DayTest.DAY));
        assertThat("Hour", time.get(Calendar.HOUR_OF_DAY), is(DEFAULT_ALARM_HOUR));
        assertThat("Minute", time.get(Calendar.MINUTE), is(DEFAULT_ALARM_MINUTE + 2));
        assertThat("Second", time.get(Calendar.SECOND), is(0));
        assertThat("Millisecond", time.get(Calendar.MILLISECOND), is(0));

        PendingIntent operation = nextScheduledAlarm.operation;
        ShadowPendingIntent shadowPendingIntent = Shadows.shadowOf(operation);

        Intent expectedIntent = new Intent(context, AlarmReceiver.class);

        assertThat(shadowPendingIntent.isBroadcastIntent(), is(true));
        assertThat(shadowPendingIntent.getSavedIntents().length, is(1));
        assertThat(shadowPendingIntent.getSavedIntents()[0].getComponent(), is(expectedIntent.getComponent()));
        assertThat(shadowPendingIntent.getSavedIntent().getAction(), is(SystemAlarm.ACTION_RING_IN_NEAR_FUTURE));
    }

    @Test
    @Config(qualifiers = "en")
    public void t12_setAlarmToPast() {
        ShadowAlarmManager.ScheduledAlarm nextScheduledAlarm1 = shadowAlarmManager.peekNextScheduledAlarm();
        assertThat(nextScheduledAlarm1.type, is(AlarmManager.RTC_WAKEUP));

        // Click in calendar
        AlarmMorningActivity activity = Robolectric.setupActivity(AlarmMorningActivity.class);
        ShadowActivity shadowActivity = Shadows.shadowOf(activity);

        AlarmMorningActivityTest.setLocale(activity, "en", "US");

        RecyclerView recyclerView = (RecyclerView) shadowActivity.findViewById(R.id.calendar_recycler_view);

        // Hack: RecyclerView needs to be measured and layed out manually in Robolectric.
        // Source: http://stackoverflow.com/questions/27052866/android-robolectric-click-recyclerview-item
        recyclerView.measure(0, 0);
        recyclerView.layout(0, 0, 100, 10000);

        View item = recyclerView.getChildAt(0);

        TextView textDate = (TextView) item.findViewById(R.id.textDate);
        TextView textTime = (TextView) item.findViewById(R.id.textTimeCal);
        TextView textState = (TextView) item.findViewById(R.id.textState);

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

        Calendar now = globalManager.clock().now();
        now.add(Calendar.MINUTE, -1);
        dialog.updateTime(now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE));

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick();

        // Check calendar
//        assertThat(textDate.getText(), is("2/1")); // TODO Fix test
//        assertThat(textTime.getText(), is("00:59")); // TODO Fix test
        assertThat(textState.getText(), is("Passed"));

        // Check system alarm
        assertThat(shadowAlarmManager.getScheduledAlarms().size(), is(1));

        ShadowAlarmManager.ScheduledAlarm nextScheduledAlarm = shadowAlarmManager.peekNextScheduledAlarm();

        assertThat(nextScheduledAlarm.type, is(AlarmManager.RTC_WAKEUP));

        Calendar time = GregorianCalendar.getInstance();
        time.setTimeInMillis(nextScheduledAlarm.triggerAtTime);
        assertThat("Year", time.get(Calendar.YEAR), is(DayTest.YEAR));
        assertThat("Month", time.get(Calendar.MONTH), is(DayTest.MONTH));
        assertThat("Date", time.get(Calendar.DAY_OF_MONTH), is(DayTest.DAY + 1));
        assertThat("Hour", time.get(Calendar.HOUR_OF_DAY), is(0));
        assertThat("Minute", time.get(Calendar.MINUTE), is(0));
        assertThat("Second", time.get(Calendar.SECOND), is(0));
        assertThat("Millisecond", time.get(Calendar.MILLISECOND), is(0));

        PendingIntent operation = nextScheduledAlarm.operation;
        ShadowPendingIntent shadowPendingIntent = Shadows.shadowOf(operation);

        Intent expectedIntent = new Intent(context, AlarmReceiver.class);

        assertThat(shadowPendingIntent.isBroadcastIntent(), is(true));
        assertThat(shadowPendingIntent.getSavedIntents().length, is(1));
        assertThat(shadowPendingIntent.getSavedIntents()[0].getComponent(), is(expectedIntent.getComponent()));
        assertThat(shadowPendingIntent.getSavedIntent().getAction(), is(SystemAlarm.ACTION_SET_SYSTEM_ALARM));
    }

    @Test
    @Config(qualifiers = "en")
    public void t13_setAlarmWithZeroAdvancePeriod() {
        // Set the preference to zero
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt(SettingsActivity.PREF_NEAR_FUTURE_TIME, 0);
        editor.commit();

        // Set alarm
        ShadowAlarmManager.ScheduledAlarm nextScheduledAlarm1 = shadowAlarmManager.getNextScheduledAlarm();
        assertThat(nextScheduledAlarm1.type, is(AlarmManager.RTC_WAKEUP));

        // Click in calendar
        AlarmMorningActivity activity = Robolectric.setupActivity(AlarmMorningActivity.class);
        ShadowActivity shadowActivity = Shadows.shadowOf(activity);

        AlarmMorningActivityTest.setLocale(activity, "en", "US");

        RecyclerView recyclerView = (RecyclerView) shadowActivity.findViewById(R.id.calendar_recycler_view);

        // Hack: RecyclerView needs to be measured and layed out manually in Robolectric.
        // Source: http://stackoverflow.com/questions/27052866/android-robolectric-click-recyclerview-item
        recyclerView.measure(0, 0);
        recyclerView.layout(0, 0, 100, 10000);

        View item = recyclerView.getChildAt(0);

        TextView textDate = (TextView) item.findViewById(R.id.textDate);
        TextView textTime = (TextView) item.findViewById(R.id.textTimeCal);
        TextView textState = (TextView) item.findViewById(R.id.textState);

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
        assertThat(shadowAlarmManager.getScheduledAlarms().size(), is(1));

        ShadowAlarmManager.ScheduledAlarm nextScheduledAlarm = shadowAlarmManager.peekNextScheduledAlarm();

        assertThat(nextScheduledAlarm.type, is(AlarmManager.RTC_WAKEUP));

        Calendar time = GregorianCalendar.getInstance();
        time.setTimeInMillis(nextScheduledAlarm.triggerAtTime);
        assertThat("Year", time.get(Calendar.YEAR), is(DayTest.YEAR));
        assertThat("Month", time.get(Calendar.MONTH), is(DayTest.MONTH));
        assertThat("Date", time.get(Calendar.DAY_OF_MONTH), is(DayTest.DAY));
        assertThat("Hour", time.get(Calendar.HOUR_OF_DAY), is(DEFAULT_ALARM_HOUR));
        assertThat("Minute", time.get(Calendar.MINUTE), is(DEFAULT_ALARM_MINUTE));
        assertThat("Second", time.get(Calendar.SECOND), is(0));
        assertThat("Millisecond", time.get(Calendar.MILLISECOND), is(0));

        PendingIntent operation = nextScheduledAlarm.operation;
        ShadowPendingIntent shadowPendingIntent = Shadows.shadowOf(operation);

        Intent expectedIntent = new Intent(context, AlarmReceiver.class);

        assertThat(shadowPendingIntent.isBroadcastIntent(), is(true));
        assertThat(shadowPendingIntent.getSavedIntents().length, is(1));
        assertThat(shadowPendingIntent.getSavedIntents()[0].getComponent(), is(expectedIntent.getComponent()));
        assertThat(shadowPendingIntent.getSavedIntent().getAction(), is(SystemAlarm.ACTION_RING));
    }

    @Test
    @Config(qualifiers = "en")
    public void t14_setAlarmTomorrow() {
        ShadowAlarmManager.ScheduledAlarm nextScheduledAlarm1 = shadowAlarmManager.getNextScheduledAlarm();
        assertThat(nextScheduledAlarm1.type, is(AlarmManager.RTC_WAKEUP));

        // Click in calendar
        AlarmMorningActivity activity = Robolectric.setupActivity(AlarmMorningActivity.class);
        ShadowActivity shadowActivity = Shadows.shadowOf(activity);

        AlarmMorningActivityTest.setLocale(activity, "en", "US");

        RecyclerView recyclerView = (RecyclerView) shadowActivity.findViewById(R.id.calendar_recycler_view);

        // Hack: RecyclerView needs to be measured and layed out manually in Robolectric.
        // Source: http://stackoverflow.com/questions/27052866/android-robolectric-click-recyclerview-item
        recyclerView.measure(0, 0);
        recyclerView.layout(0, 0, 100, 10000);

        View item = recyclerView.getChildAt(1);

        TextView textDate = (TextView) item.findViewById(R.id.textDate);
        TextView textTime = (TextView) item.findViewById(R.id.textTimeCal);
        TextView textState = (TextView) item.findViewById(R.id.textState);

//        assertThat(textDate.getText(), is("2/2")); // TODO Fix test
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
        assertThat(shadowAlarmManager.getScheduledAlarms().size(), is(1));

        ShadowAlarmManager.ScheduledAlarm nextScheduledAlarm = shadowAlarmManager.peekNextScheduledAlarm();

        assertThat(nextScheduledAlarm.type, is(AlarmManager.RTC_WAKEUP));

        Calendar time = GregorianCalendar.getInstance();
        time.setTimeInMillis(nextScheduledAlarm.triggerAtTime);
        assertThat("Year", time.get(Calendar.YEAR), is(DayTest.YEAR));
        assertThat("Month", time.get(Calendar.MONTH), is(DayTest.MONTH));
        assertThat("Date", time.get(Calendar.DAY_OF_MONTH), is(DayTest.DAY + 1));
        assertThat("Hour", time.get(Calendar.HOUR_OF_DAY), is(DEFAULT_ALARM_HOUR - 2));
        assertThat("Minute", time.get(Calendar.MINUTE), is(DEFAULT_ALARM_MINUTE));
        assertThat("Second", time.get(Calendar.SECOND), is(0));
        assertThat("Millisecond", time.get(Calendar.MILLISECOND), is(0));

        PendingIntent operation = nextScheduledAlarm.operation;
        ShadowPendingIntent shadowPendingIntent = Shadows.shadowOf(operation);

        Intent expectedIntent = new Intent(context, AlarmReceiver.class);

        assertThat(shadowPendingIntent.isBroadcastIntent(), is(true));
        assertThat(shadowPendingIntent.getSavedIntents().length, is(1));
        assertThat(shadowPendingIntent.getSavedIntents()[0].getComponent(), is(expectedIntent.getComponent()));
        assertThat(shadowPendingIntent.getSavedIntent().getAction(), is(SystemAlarm.ACTION_RING_IN_NEAR_FUTURE));
    }
}
