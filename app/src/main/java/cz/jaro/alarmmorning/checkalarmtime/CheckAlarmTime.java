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

import cz.jaro.alarmmorning.AlarmMorningActivity;
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
import cz.jaro.alarmmorning.model.AlarmDataSource;
import cz.jaro.alarmmorning.model.Day;

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

    private Intent intent;
    private PendingIntent operation;

    private Intent intentDismissNotification;
    private PendingIntent operationDismissNotification;

    /**
     * Action meaning: Check alarm time of the next alarm: compare it with the 1st calendar instance. If it is too close, offer the user to quickly change the
     * alarm time.
     */
    protected static final String ACTION_CHECK_ALARM_TIME = "CHECK_ALARM_TIME";

    /**
     * Action meaning: Dismiss the notification. Note: the notification may have been already dismissed by the user.
     */
    protected static final String ACTION_DISMISS_NOTIFICATION = "DISMISS_NOTIFICATION";

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
            registerCheckAlarmTime();
        }
    }

    public void registerCheckAlarmTime() {
        Log.d(TAG, "registerCheckAlarmTime()");

        Calendar checkAlarmTimeAt = calcCheckAlarmTimeAt();

        String action = ACTION_CHECK_ALARM_TIME;
        Log.i(TAG, "Setting system alarm at " + checkAlarmTimeAt.getTime().toString() + " with action " + action);

        intent = new Intent(context, CheckAlarmTimeAlarmReceiver.class);
        intent.setAction(action);

        operation = PendingIntent.getBroadcast(context, 1, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        alarmManager.set(AlarmManager.RTC, checkAlarmTimeAt.getTimeInMillis(), operation);
    }

    public void unregisterCheckAlarmTime() {
        Log.d(TAG, "unregisterCheckAlarmTime()");

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

    private void registerNotificationDismiss(Calendar time) {
        Log.d(TAG, "registerNotificationDismiss()");

        String action = ACTION_DISMISS_NOTIFICATION;
        Log.i(TAG, "Setting system alarm at " + time.getTime().toString() + " with action " + action);

        intentDismissNotification = new Intent(context, CheckAlarmTimeAlarmReceiver.class);
        intentDismissNotification.setAction(action);

        operationDismissNotification = PendingIntent.getBroadcast(context, 1, intentDismissNotification, PendingIntent.FLAG_UPDATE_CURRENT);

        alarmManager.set(AlarmManager.RTC, time.getTimeInMillis(), operationDismissNotification);
    }

    public void onReceive(Intent intent) {
        String action = intent.getAction();

        Log.i(TAG, "Acting on CheckAlarmTime. action=" + action);

        if (action.equals(ACTION_CHECK_ALARM_TIME)) {
            onCheckAlarmTime();
        } else if (action.equals(ACTION_DISMISS_NOTIFICATION)) {
            onDismissNotification();
        } else {
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
        registerCheckAlarmTime();

        // Find first calendar event
        GlobalManager globalManager = new GlobalManager(context);
        Clock clock = globalManager.clock();

        Calendar tomorrowStart = clock.now();
        tomorrowStart.add(Calendar.DATE, 1);
        tomorrowStart.set(Calendar.HOUR_OF_DAY, 0);
        tomorrowStart.set(Calendar.MINUTE, 0);
        tomorrowStart.set(Calendar.SECOND, 0);
        tomorrowStart.set(Calendar.MILLISECOND, 0);
        Log.v(TAG, "tomorrowStart=" + tomorrowStart.getTime());

        Calendar tomorrowNoon = (Calendar) tomorrowStart.clone();
        tomorrowNoon.add(Calendar.HOUR_OF_DAY, 12);
        tomorrowNoon.add(Calendar.MILLISECOND, -1);
        Log.v(TAG, "tomorrowNoon=" + tomorrowNoon.getTime());

        // Load tomorrow's alarm time
        AlarmDataSource dataSource = new AlarmDataSource(context);
        dataSource.open();

        Day day = dataSource.loadDayDeep(tomorrowStart);
        Calendar alarmTime = day.getDateTime();
        Log.v(TAG, "alarmTime=" + alarmTime.getTime());

        dataSource.close();

        // Load gap
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        int checkAlarmTimeGap = preferences.getInt(SettingsActivity.PREF_CHECK_ALARM_TIME_GAP, SettingsActivity.PREF_CHECK_ALARM_TIME_GAP_DEFAULT);
        Log.v(TAG, "checkAlarmTimeGap=" + checkAlarmTimeGap + " minutes");

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

            if (!day.isEnabled() || targetAlarmTime.before(alarmTime)) {
                Log.d(TAG, "Appointment that needs and earlier alarm time found");

                showNotification(day, targetAlarmTime, checkAlarmTimeGap, event);
                registerNotificationDismiss(targetAlarmTime);
            }
        }
    }

    private void onDismissNotification() {
        Log.d(TAG, "onDismissNotification()");
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
                .setAutoCancel(true);

        String meetingTimeText = Localization.timeToString(event.getBegin().get(Calendar.HOUR_OF_DAY), event.getBegin().get(Calendar.MINUTE), context);
        String contentText;
        if (event.getLocation() != null) {
            contentText = res.getString(R.string.notification_check_text_with_location, meetingTimeText, event.getTitle(), event.getLocation());
        } else {
            contentText = res.getString(R.string.notification_check_text_without_location, meetingTimeText, event.getTitle());
        }
        mBuilder.setContentText(contentText);

        Intent intent = new Intent(context, AlarmMorningActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 1, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        mBuilder.setContentIntent(pendingIntent);

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

    @NonNull
    private Calendar calcCheckAlarmTimeAt() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        String checkAlarmTimeAtPreference = preferences.getString(SettingsActivity.PREF_CHECK_ALARM_TIME_AT, SettingsActivity.PREF_CHECK_ALARM_TIME_AT_DEFAULT);

        return calcNextOccurence(context, checkAlarmTimeAtPreference);
    }

    /**
     * Calculate the date and time of the next event that is occuring daily at {@code time}.
     *
     * @param context context
     * @param time    a string representation of {@link TimePreference} value
     * @return
     */
    @NonNull
    static public Calendar calcNextOccurence(Context context, String time) {
        int hours = TimePreference.getHour(time);
        int minutes = TimePreference.getMinute(time);

        GlobalManager globalManager = new GlobalManager(context);
        Clock clock = globalManager.clock();
        Calendar now = clock.now();

        Calendar checkAlarmTimeAt = now;
        checkAlarmTimeAt.set(Calendar.HOUR_OF_DAY, hours);
        checkAlarmTimeAt.set(Calendar.MINUTE, minutes);
        checkAlarmTimeAt.set(Calendar.SECOND, 0);
        checkAlarmTimeAt.set(Calendar.MILLISECOND, 0);

        // If in the past, then shift to tomorrow
        if (checkAlarmTimeAt.before(now)) {
            checkAlarmTimeAt.add(Calendar.DAY_OF_MONTH, 1);
        }

        return checkAlarmTimeAt;
    }
}