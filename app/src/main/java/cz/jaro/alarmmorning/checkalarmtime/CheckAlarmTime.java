package cz.jaro.alarmmorning.checkalarmtime;

import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import java.util.Calendar;

import cz.jaro.alarmmorning.Analytics;
import cz.jaro.alarmmorning.GlobalManager;
import cz.jaro.alarmmorning.Localization;
import cz.jaro.alarmmorning.R;
import cz.jaro.alarmmorning.SettingsActivity;
import cz.jaro.alarmmorning.SystemAlarm;
import cz.jaro.alarmmorning.calendar.CalendarEvent;
import cz.jaro.alarmmorning.calendar.CalendarEventFilter;
import cz.jaro.alarmmorning.calendar.CalendarHelper;
import cz.jaro.alarmmorning.clock.Clock;
import cz.jaro.alarmmorning.graphics.TimePreference;
import cz.jaro.alarmmorning.model.Day;

import static cz.jaro.alarmmorning.Analytics.CHECK_ALARM_TIME_ACTION__DON_T_SHOW_NOTIFICATION;
import static cz.jaro.alarmmorning.Analytics.CHECK_ALARM_TIME_ACTION__NO_APPOINTMENT;
import static cz.jaro.alarmmorning.Analytics.CHECK_ALARM_TIME_ACTION__SHOW_NOTIFICATION;
import static cz.jaro.alarmmorning.calendar.CalendarUtils.beginningOfTomorrow;
import static cz.jaro.alarmmorning.calendar.CalendarUtils.justBeforeNoonTomorrow;
import static cz.jaro.alarmmorning.calendar.CalendarUtils.roundDown;

/**
 * This class implements the "check alarm time" feature. Specifically, the following check is (typically) run in the evening: compare the alarm time and time of
 * the beginning of the first meeting on the next day. It also assumes the gap (in minutes), that must be between alarm time and the beginning of the first
 * meeting. The latest alarm time is calculated as "beginning of the first meeting" minus the gap. If there is no alarm before the latest alarm time, then a
 * reminder (notification) appears which informs the user about this situation and allows for a quick change of the alarm time. In real life, this is useful
 * because you don't ever forget to set the alarm time even if you have the meeting in the calendar.
 */
public class CheckAlarmTime {

    // TODO Monitor the calendar instance that caused the alarm time change. Handle actions (time change, delete) accordingly.
    // TODO Monitor new calendar instances that could cause the alarm time change. Handle accordingly.

    private static final String TAG = SystemAlarm.class.getSimpleName();

    private static final int NOTIFICATION_ID = 1;

    private static CheckAlarmTime instance;

    private Context context;
    private AlarmManager alarmManager;

    private PendingIntent operation;

    /**
     * Action meaning: Check alarm time of the next alarm: compare it with the 1st calendar instance. If it is too close, offer the user to quickly change the
     * alarm time.
     */
    public static final String ACTION_CHECK_ALARM_TIME = "CHECK_ALARM_TIME";

    /**
     * Action meaning: Automatically hide the notification (on a specified time). Note: the notification may have been already dismissed by the user.
     */
    public static final String ACTION_AUTO_HIDE_NOTIFICATION = "AUTO_HIDE_NOTIFICATION";

    private CheckAlarmTime(Context context) {
        this.context = context;
        alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
    }

    public static CheckAlarmTime getInstance(Context context) {
        if (instance == null) {
            instance = new CheckAlarmTime(context);
        }
        return instance;
    }

    public void checkAndRegisterCheckAlarmTime() {
        Log.v(TAG, "checkAndRegisterCheckAlarmTime()");

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        boolean checkAlarmTimePreference = preferences.getBoolean(SettingsActivity.PREF_CHECK_ALARM_TIME, SettingsActivity.PREF_CHECK_ALARM_TIME_DEFAULT);

        if (checkAlarmTimePreference) {
            register();
        }
    }

    public void register() {
        Log.d(TAG, "register()");

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        String checkAlarmTimeAtPreference = preferences.getString(SettingsActivity.PREF_CHECK_ALARM_TIME_AT, SettingsActivity.PREF_CHECK_ALARM_TIME_AT_DEFAULT);

        register(checkAlarmTimeAtPreference);

    }

    private void register(String stringValue) {
        Log.d(TAG, "register(stringValue=)" + stringValue);

        Calendar checkAlarmTimeAt = calcNextOccurence(stringValue);

        String action = ACTION_CHECK_ALARM_TIME;
        Log.i(TAG, "Setting system alarm at " + checkAlarmTimeAt.getTime().toString() + " with action " + action);

        Intent intent = new Intent(context, CheckAlarmTimeAlarmReceiver.class);
        intent.setAction(action);

        operation = PendingIntent.getBroadcast(context, 1, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        SystemAlarm.setSystemAlarm(alarmManager, checkAlarmTimeAt, operation);
    }

    public void unregister() {
        Log.d(TAG, "unregister()");

        if (operation != null) {
            // Method 1: standard
            Log.d(TAG, "Cancelling current system alarm");
            operation.cancel();
        } else {
            // Method 2: try to recreate the operation
            Log.d(TAG, "Recreating operation when cancelling system alarm");

            Intent intent2 = new Intent(context, CheckAlarmTimeAlarmReceiver.class);
            intent2.setAction(ACTION_CHECK_ALARM_TIME);

            PendingIntent operation2 = PendingIntent.getBroadcast(context, 1, intent2, PendingIntent.FLAG_NO_CREATE);

            if (operation2 != null) {
                operation2.cancel();
            }
        }
    }

    public void reregister(String stringValue) {
        unregister();
        register(stringValue);
    }

    private void registerNotificationDismiss(Calendar time) {
        Log.d(TAG, "registerNotificationDismiss()");

        String action = ACTION_AUTO_HIDE_NOTIFICATION;
        Log.i(TAG, "Setting system alarm at " + time.getTime().toString() + " with action " + action);

        Intent intentDismissNotification = new Intent(context, CheckAlarmTimeAlarmReceiver.class);
        intentDismissNotification.setAction(action);

        PendingIntent operationDismissNotification = PendingIntent.getBroadcast(context, 1, intentDismissNotification, PendingIntent.FLAG_UPDATE_CURRENT);

        alarmManager.set(AlarmManager.RTC, time.getTimeInMillis(), operationDismissNotification);
    }

    public void onReceive(Intent intent) {
        String action = intent.getAction();

        Log.i(TAG, "Acting on CheckAlarmTime. action=" + action);

        switch (action) {
            case ACTION_CHECK_ALARM_TIME:
                onCheckAlarmTime();
                break;
            case ACTION_AUTO_HIDE_NOTIFICATION:
                onAutoHideNotification();
                break;
            default:
                throw new IllegalArgumentException("Unexpected argument " + action);
        }
    }

    /**
     * Compare the alarm time for tomorrow with the first meeting tomorrow. If the alarm time is nut sufficiently long before the meeting, then offer user to
     * quickly change the alarm time.
     * <p>
     * The first meeting tomorrow is defined as follows:
     * <p>
     * 1. Entry appears tomorrow in the morning (between midnight and noon).
     * <p>
     * 2. Entry starts on the day
     * <p>
     * 3. Entry is not all-day
     */
    private void onCheckAlarmTime() {
        Log.d(TAG, "onCheckAlarmTime()");

        // Register for tomorrow
        register();

        // Find first calendar event
        GlobalManager globalManager = GlobalManager.getInstance();
        Clock clock = globalManager.clock();
        Calendar now = clock.now();

        Calendar tomorrowStart = beginningOfTomorrow(now);
        Log.v(TAG, "tomorrowStart=" + tomorrowStart.getTime());

        Calendar tomorrowNoon = justBeforeNoonTomorrow(now);
        Log.v(TAG, "tomorrowNoon=" + tomorrowNoon.getTime());

        // Load tomorrow's alarm time
        Day day = globalManager.loadDay(tomorrowStart);
        Calendar alarmTime = day.getDateTime();
        Log.v(TAG, "alarmTime=" + alarmTime.getTime());

        // Load gap
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        int checkAlarmTimeGap = preferences.getInt(SettingsActivity.PREF_CHECK_ALARM_TIME_GAP, SettingsActivity.PREF_CHECK_ALARM_TIME_GAP_DEFAULT);
        Log.v(TAG, "checkAlarmTimeGap=" + checkAlarmTimeGap + " minutes");

        Analytics analytics = new Analytics(context, Analytics.Event.Show, Analytics.Channel.Time, Analytics.ChannelName.Check_alarm_time);
        analytics.setDay(day);
        analytics.set(Analytics.Param.Check_alarm_time_gap, checkAlarmTimeGap);
        // TODO Analytics - add device location

        CalendarHelper calendarHelper = new CalendarHelper(context);
        CalendarEventFilter notAllDay = new CalendarEventFilter() {
            @Override
            public boolean match(CalendarEvent event) {
                return !event.getAllDay();
            }
        };
        CalendarEvent event = calendarHelper.find(tomorrowStart, tomorrowNoon, notAllDay);

        if (event != null) {
            Calendar targetAlarmTime = (Calendar) event.getBegin().clone();
            targetAlarmTime.add(Calendar.MINUTE, -checkAlarmTimeGap);
            Log.v(TAG, "      targetAlarmTime=" + targetAlarmTime.getTime());

            // TODO Currently, the alarm time must be on the same date. Support alarm on the previous day.
            if (targetAlarmTime.before(tomorrowStart)) {
                targetAlarmTime = tomorrowStart;
                Log.v(TAG, "      adjusted targetAlarmTime to " + targetAlarmTime.getTime());
            }

            analytics.set(Analytics.Param.Appointment_begin, Analytics.calendarToTime(event.getBegin()));
            analytics.set(Analytics.Param.Appointment_title, event.getTitle());
            analytics.set(Analytics.Param.Appointment_location, event.getLocation());

            if (!day.isEnabled() || targetAlarmTime.before(alarmTime)) {
                Log.d(TAG, "Appointment that needs and earlier alarm time found");
                analytics.set(Analytics.Param.Check_alarm_time_action, CHECK_ALARM_TIME_ACTION__SHOW_NOTIFICATION);

                showNotification(day, targetAlarmTime, checkAlarmTimeGap, event);
                registerNotificationDismiss(targetAlarmTime);
            } else {
                analytics.set(Analytics.Param.Check_alarm_time_action, CHECK_ALARM_TIME_ACTION__DON_T_SHOW_NOTIFICATION);
            }
        } else {
            analytics.set(Analytics.Param.Check_alarm_time_action, CHECK_ALARM_TIME_ACTION__NO_APPOINTMENT);
        }

        analytics.save();
    }

    private void onAutoHideNotification() {
        Log.d(TAG, "onAutoHideNotification()");
        new Analytics(context, Analytics.Event.Hide, Analytics.Channel.Time, Analytics.ChannelName.Check_alarm_time).save();

        hideNotification();
    }

    private void showNotification(Day day, Calendar targetAlarmTime, int checkAlarmTimeGapPreference, CalendarEvent event) {
        Log.d(TAG, "showNotification()");
        Resources res = context.getResources();

        String contentTitle;
        if (day.isEnabled()) {
            String timeText = Localization.timeToString(day.getHourX(), day.getMinuteX(), context);
            contentTitle = res.getString(R.string.notification_check_title_currently_set, timeText);
        } else {
            contentTitle = res.getString(R.string.notification_check_title_currently_unset);
        }

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context)
                .setSmallIcon(R.drawable.ic_alarm_white)
                .setContentTitle(contentTitle)
                .setPriority(NotificationCompat.PRIORITY_MAX);

        String meetingTimeText = Localization.timeToString(event.getBegin().get(Calendar.HOUR_OF_DAY), event.getBegin().get(Calendar.MINUTE), context);
        String contentText;
        if (event.getLocation() != null && !event.getLocation().isEmpty()) {
            contentText = res.getString(R.string.notification_check_text_with_location, meetingTimeText, event.getTitle(), event.getLocation());
        } else {
            contentText = res.getString(R.string.notification_check_text_without_location, meetingTimeText, event.getTitle());
        }
        mBuilder.setContentText(contentText);

        Intent intent = new Intent(context, CheckAlarmTimeNotificationReceiver.class);
        intent.setAction(CheckAlarmTimeNotificationReceiver.ACTION_CHECK_ALARM_TIME_CLICK);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 1, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        mBuilder.setContentIntent(pendingIntent);

        Intent deleteIntent = new Intent(context, CheckAlarmTimeNotificationReceiver.class);
        deleteIntent.setAction(CheckAlarmTimeNotificationReceiver.ACTION_CHECK_ALARM_TIME_DELETE);
        PendingIntent deletePendingIntent = PendingIntent.getBroadcast(context, 1, deleteIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        mBuilder.setDeleteIntent(deletePendingIntent);

        String targetTimeText = Localization.timeToString(targetAlarmTime.get(Calendar.HOUR_OF_DAY), targetAlarmTime.get(Calendar.MINUTE), context);
        String setText = res.getString(R.string.notification_check_text_set_at, targetTimeText);
        Intent setIntent = new Intent(context, CheckAlarmTimeNotificationReceiver.class);
        setIntent.setAction(CheckAlarmTimeNotificationReceiver.ACTION_CHECK_ALARM_TIME_SET_TO);
        setIntent.putExtra(CheckAlarmTimeNotificationReceiver.EXTRA_NEW_ALARM_TIME, targetAlarmTime.getTimeInMillis());
        PendingIntent setPendingIntent = PendingIntent.getBroadcast(context, 1, setIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        mBuilder.addAction(R.drawable.ic_alarm_on_white, setText, setPendingIntent);

        String dialogText = res.getString(R.string.notification_check_text_set_dialog);
        Intent dialogIntent = new Intent(context, CheckAlarmTimeNotificationReceiver.class);
        dialogIntent.setAction(CheckAlarmTimeNotificationReceiver.ACTION_CHECK_ALARM_TIME_SET_DIALOG);
        dialogIntent.putExtra(CheckAlarmTimeNotificationReceiver.EXTRA_NEW_ALARM_TIME, targetAlarmTime.getTimeInMillis());
        PendingIntent dialogPendingIntent = PendingIntent.getBroadcast(context, 1, dialogIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        mBuilder.addAction(R.drawable.ic_alarm_white, dialogText, dialogPendingIntent);

        NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.notify(NOTIFICATION_ID, mBuilder.build());
    }

    public void hideNotification() {
        Log.d(TAG, "hideNotification()");
        NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.cancel(NOTIFICATION_ID);
    }

    /**
     * Calculate the date and time of the next event that is occurring daily at {@code timePreference}.
     *
     * @param timePreference a string representation of {@link TimePreference} value
     * @return Calendar with next event
     */
    @NonNull
    static public Calendar calcNextOccurence(String timePreference) {
        int hours = TimePreference.getHour(timePreference);
        int minutes = TimePreference.getMinute(timePreference);

        GlobalManager globalManager = GlobalManager.getInstance();
        Clock clock = globalManager.clock();
        Calendar now = clock.now();

        Calendar time = (Calendar) now.clone();
        time.set(Calendar.HOUR_OF_DAY, hours);
        time.set(Calendar.MINUTE, minutes);
        roundDown(time, Calendar.SECOND);

        // If in the past, then shift to tomorrow
        if (time.before(now)) {
            time.add(Calendar.DAY_OF_MONTH, 1);
        }

        return time;
    }

}