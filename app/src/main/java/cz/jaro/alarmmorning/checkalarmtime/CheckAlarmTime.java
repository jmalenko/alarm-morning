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
import cz.jaro.alarmmorning.SystemNotification;
import cz.jaro.alarmmorning.calendar.CalendarEvent;
import cz.jaro.alarmmorning.calendar.CalendarEventFilter;
import cz.jaro.alarmmorning.calendar.CalendarHelper;
import cz.jaro.alarmmorning.calendar.CalendarUtils;
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

    private static final String TAG = GlobalManager.createLogTag(CheckAlarmTime.class);

    private static final int NOTIFICATION_ID = 1;
    private static final int REQUEST_CODE = 1;

    private static CheckAlarmTime instance;

    private Context context;
    private AlarmManager alarmManager;

    private PendingIntent operation;
    private PendingIntent operationDismissNotification;

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

    public boolean isEnabled() {
        Log.v(TAG, "isEnabled()");

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        boolean checkAlarmTimePreference = preferences.getBoolean(SettingsActivity.PREF_CHECK_ALARM_TIME, SettingsActivity.PREF_CHECK_ALARM_TIME_DEFAULT);

        return checkAlarmTimePreference;
    }

    public void checkAndRegister() {
        Log.v(TAG, "checkAndRegister()");

        if (isEnabled()) {
            register();
        }
    }

    public void register() {
        Log.d(TAG, "register()");

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        String checkAlarmTimeAtPreference = preferences.getString(SettingsActivity.PREF_CHECK_ALARM_TIME_AT, SettingsActivity.PREF_CHECK_ALARM_TIME_AT_DEFAULT);

        register(checkAlarmTimeAtPreference);
    }

    private void register(String checkAlarmTimeAtPreference) {
        Log.d(TAG, "register(checkAlarmTimeAtPreference=" + checkAlarmTimeAtPreference);

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            Calendar checkAlarmTimeAt = calcNextOccurence(checkAlarmTimeAtPreference);

            String action = ACTION_CHECK_ALARM_TIME;
            Log.i(TAG, "Setting system alarm at " + checkAlarmTimeAt.getTime().toString() + " with action " + action);

            Intent intent = new Intent(context, CheckAlarmTimeAlarmReceiver.class);
            intent.setAction(action);

            operation = PendingIntent.getBroadcast(context, 1, intent, PendingIntent.FLAG_UPDATE_CURRENT);

            SystemAlarm.setSystemAlarm(alarmManager, checkAlarmTimeAt, operation);
        } else {
            CalendarEventChangeReceiverAsJob.schedule(context);
        }
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

        operationDismissNotification = PendingIntent.getBroadcast(context, 1, intentDismissNotification, PendingIntent.FLAG_UPDATE_CURRENT);

        alarmManager.set(AlarmManager.RTC, time.getTimeInMillis(), operationDismissNotification);
    }

    private void unregisterNotificationDismiss(Calendar time) {
        Log.d(TAG, "unregisterNotificationDismiss()");

        if (operationDismissNotification != null) {
            // Method 1: standard
            Log.d(TAG, "Cancelling current system alarm");
            operationDismissNotification.cancel();
        } else {
            // Method 2: try to recreate the operation
            Log.d(TAG, "Recreating operation when cancelling system alarm");

            String action = ACTION_AUTO_HIDE_NOTIFICATION;
            Intent intentDismissNotification2 = new Intent(context, CheckAlarmTimeAlarmReceiver.class);
            intentDismissNotification2.setAction(action);

            PendingIntent operationDismissNotification2 = PendingIntent.getBroadcast(context, 1, intentDismissNotification2, PendingIntent.FLAG_NO_CREATE);

            if (operationDismissNotification2 != null) {
                operationDismissNotification2.cancel();
            }
        }
    }

    public void onReceive(Intent intent) {
        String action = intent.getAction();

        Log.i(TAG, "Acting on CheckAlarmTime. action=" + action);

        switch (action) {
            case ACTION_CHECK_ALARM_TIME:
                // The condition is needed for cases we are unable to unregister a system alarm.
                if (!isEnabled()) return;

                onCheckAlarmTime();
                break;
            case ACTION_AUTO_HIDE_NOTIFICATION:
                onAutoHideNotification();
                break;
            case Intent.ACTION_PROVIDER_CHANGED:
                // TODO May not work in Oreo and later. Solution is at https://stackoverflow.com/questions/49616809/oreo-calendar-changes
                // TODO Intent is received even when calendar is not changed.
                onCalendarUpdated();
                Log.v(TAG, "data = " + intent.getData());
                break;
            default:
                throw new IllegalArgumentException("Unexpected argument " + action);
        }
    }

    private void onCheckAlarmTime() {
        Log.d(TAG, "onCheckAlarmTime()");

        // Register for tomorrow
        register();

        MorningInfo morningInfo = new MorningInfo(context);
        onCheckAlarmTime(morningInfo);

        // Save analytics
        morningInfo.analytics.save();
    }

    void onCheckAlarmTime(MorningInfo morningInfo) {
        Log.d(TAG, "onCheckAlarmTime(morningInfo=" + morningInfo + ")");
        if (morningInfo.attentionNeeded) {
            showNotification(morningInfo.day, morningInfo.targetAlarmTime, morningInfo.checkAlarmTimeGap, morningInfo.event);
            registerNotificationDismiss(morningInfo.alarmTime);
        }
    }

    private void onAutoHideNotification() {
        Log.d(TAG, "onAutoHideNotification()");
        new Analytics(context, Analytics.Event.Hide, Analytics.Channel.Time, Analytics.ChannelName.Check_alarm_time).save();

        hideNotification();
    }

    /**
     * Note that Android broadcast and Intent "something happed with calendar" (specifically the PROVIDER_CHANGED action). However this intent doesn't have any
     * attributes describing what happened. Therefore all the calendar events must be checked.
     */
    private void onCalendarUpdated() {
        boolean betweenCheckAlarmTimeAndAlarmTime = isBetweenCheckAlarmTimeAndAlarmTime();
        if (betweenCheckAlarmTimeAndAlarmTime) {
            boolean notificationVisible = isNotificationVisible();
            MorningInfo morningInfo = new MorningInfo(context);
            if (morningInfo.attentionNeeded) {
                if (notificationVisible) {
                    // TODO Update notification if the target alarm time changed
                    boolean targetAlarmTimeChanged = true;
                    if (targetAlarmTimeChanged) {
                        showNotification(morningInfo.day, morningInfo.targetAlarmTime, morningInfo.checkAlarmTimeGap, morningInfo.event);
                    }
                } else {
                    // TODO Show notification but ignore if the user already dismissed the notification about this event (maybe only duration changed)
                    boolean dismissedNotification = false;
                    if (!dismissedNotification) {
                        showNotification(morningInfo.day, morningInfo.targetAlarmTime, morningInfo.checkAlarmTimeGap, morningInfo.event);
                        registerNotificationDismiss(morningInfo.alarmTime);
                    }
                }
            } else {
                if (notificationVisible) {
                    // Hide notification
                    unregisterNotificationDismiss(morningInfo.targetAlarmTime);
                    hideNotification();
                }
            }
        }
    }

    /**
     * Checks that the current time is after the check alarm time and before the alarm time tomorrow.
     *
     * @return True iff the current time is after the check alarm time and before the alarm time tomorrow. If the alarm is disabled tomorrow, then return true.
     */
    private boolean isBetweenCheckAlarmTimeAndAlarmTime() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        String checkAlarmTimeAtPreference = preferences.getString(SettingsActivity.PREF_CHECK_ALARM_TIME_AT, SettingsActivity.PREF_CHECK_ALARM_TIME_AT_DEFAULT);

        GlobalManager globalManager = GlobalManager.getInstance();
        Clock clock = globalManager.clock();
        Calendar now = clock.now();

        Calendar checkAlarmTimeAtLast = calcLastOccurence(checkAlarmTimeAtPreference);
        Calendar checkAlarmTimeAtToday = calcTodaysOccurence(checkAlarmTimeAtPreference);

        Calendar date = now.before(checkAlarmTimeAtToday) ? CalendarUtils.beginningOfToday(now) : CalendarUtils.beginningOfTomorrow(now);
        Day day = globalManager.loadDay(date);

        Log.v(TAG, !day.isEnabled() ? "Alarm is disabled on " + day.getDate().getTime() : "Now " + now.getTime() + " is in period between " + checkAlarmTimeAtLast.getTime() + " and " + day.getDateTime().getTime());
        boolean res = !day.isEnabled() || (now.after(checkAlarmTimeAtLast) && now.before(day.getDateTime()));
        Log.d(TAG, "isBetweenCheckAlarmTimeAndAlarmTime() returns " + res);
        return res;
    }

    private boolean isNotificationVisible() {
        Intent notificationIntent = new Intent(context, CheckAlarmTimeNotificationReceiver.class);
        PendingIntent test = PendingIntent.getBroadcast(context, REQUEST_CODE, notificationIntent, PendingIntent.FLAG_NO_CREATE);
        return test != null;
    }

    private void showNotification(Day day, Calendar targetAlarmTime, int checkAlarmTimeGapPreference, CalendarEvent event) {
        Log.d(TAG, "showNotification()");
        Resources res = context.getResources();

        String contentTitle;
        if (day.isEnabled()) {
            String timeText = Localization.timeToString(day.getHour(), day.getMinute(), context);
            contentTitle = res.getString(R.string.notification_check_title_currently_set, timeText);
        } else {
            contentTitle = res.getString(R.string.notification_check_title_currently_unset);
        }

        SystemNotification.createNotificationChannel(context);
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context, SystemNotification.CHANNEL_ID)
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
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, REQUEST_CODE, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        mBuilder.setContentIntent(pendingIntent);

        Intent deleteIntent = new Intent(context, CheckAlarmTimeNotificationReceiver.class);
        deleteIntent.setAction(CheckAlarmTimeNotificationReceiver.ACTION_CHECK_ALARM_TIME_DELETE);
        PendingIntent deletePendingIntent = PendingIntent.getBroadcast(context, REQUEST_CODE, deleteIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        mBuilder.setDeleteIntent(deletePendingIntent);

        String targetTimeText = Localization.timeToString(targetAlarmTime.get(Calendar.HOUR_OF_DAY), targetAlarmTime.get(Calendar.MINUTE), context);
        String setText = res.getString(R.string.notification_check_text_set_at, targetTimeText);
        Intent setIntent = new Intent(context, CheckAlarmTimeNotificationReceiver.class);
        setIntent.setAction(CheckAlarmTimeNotificationReceiver.ACTION_CHECK_ALARM_TIME_SET_TO);
        setIntent.putExtra(CheckAlarmTimeNotificationReceiver.EXTRA_NEW_ALARM_TIME, targetAlarmTime.getTimeInMillis());
        PendingIntent setPendingIntent = PendingIntent.getBroadcast(context, REQUEST_CODE, setIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        mBuilder.addAction(R.drawable.ic_alarm_on_white, setText, setPendingIntent);

        String dialogText = res.getString(R.string.notification_check_text_set_dialog);
        Intent dialogIntent = new Intent(context, CheckAlarmTimeNotificationReceiver.class);
        dialogIntent.setAction(CheckAlarmTimeNotificationReceiver.ACTION_CHECK_ALARM_TIME_SET_DIALOG);
        dialogIntent.putExtra(CheckAlarmTimeNotificationReceiver.EXTRA_NEW_ALARM_TIME, targetAlarmTime.getTimeInMillis());
        PendingIntent dialogPendingIntent = PendingIntent.getBroadcast(context, REQUEST_CODE, dialogIntent, PendingIntent.FLAG_UPDATE_CURRENT);
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
        GlobalManager globalManager = GlobalManager.getInstance();
        Clock clock = globalManager.clock();
        Calendar now = clock.now();

        Calendar time = calcTodaysOccurence(timePreference);

        // If in the past, then shift to tomorrow
        if (time.before(now)) {
            time.add(Calendar.DAY_OF_MONTH, 1);
        }

        return time;
    }

    /**
     * Calculate the date and time of the last event that is occurring daily at {@code timePreference}.
     *
     * @param timePreference a string representation of {@link TimePreference} value
     * @return Calendar with next event
     */
    @NonNull
    static public Calendar calcLastOccurence(String timePreference) {
        GlobalManager globalManager = GlobalManager.getInstance();
        Clock clock = globalManager.clock();
        Calendar now = clock.now();

        Calendar time = calcTodaysOccurence(timePreference);

        // If in the future, then shift to yesterday
        if (now.before(time)) {
            time.add(Calendar.DAY_OF_MONTH, -1);
        }

        return time;
    }

    /**
     * Calculate the date and time of the today's event that is occurring daily at {@code timePreference}.
     *
     * @param timePreference a string representation of {@link TimePreference} value
     * @return Calendar with next event
     */
    @NonNull
    static public Calendar calcTodaysOccurence(String timePreference) {
        GlobalManager globalManager = GlobalManager.getInstance();
        Clock clock = globalManager.clock();
        Calendar now = clock.now();

        int hours = TimePreference.getHour(timePreference);
        int minutes = TimePreference.getMinute(timePreference);

        Calendar time = (Calendar) now.clone();
        time.set(Calendar.HOUR_OF_DAY, hours);
        time.set(Calendar.MINUTE, minutes);
        roundDown(time, Calendar.SECOND);

        return time;
    }

    /*
     * Events
     * ======
     */

    public void onAlarmSet() {
        Log.d(TAG, "onAlarmSet()");

        MorningInfo morningInfo = new MorningInfo(context);

        // If the alarm time was changed to a time long enough before the first meeting and notification exists, then hide the notification
        if (!morningInfo.attentionNeeded && isNotificationVisible()) {
            unregisterNotificationDismiss(morningInfo.targetAlarmTime);
            hideNotification();

            // Modify and save analytics
            Analytics analytics = new Analytics(context, Analytics.Event.Hide, Analytics.Channel.External, Analytics.ChannelName.Check_alarm_time);
            analytics.set(Analytics.Param.Check_alarm_time_action, CHECK_ALARM_TIME_ACTION__DON_T_SHOW_NOTIFICATION);
            analytics.setDay(morningInfo.day);
            analytics.set(Analytics.Param.Check_alarm_time_gap, morningInfo.checkAlarmTimeGap);
            analytics.set(Analytics.Param.Appointment_begin, morningInfo.event != null ? Analytics.calendarToTime(morningInfo.event.getBegin()) : "No event");
            analytics.set(Analytics.Param.Appointment_title, morningInfo.event != null ? morningInfo.event.getTitle() : "");
            analytics.set(Analytics.Param.Appointment_location, morningInfo.event != null ? morningInfo.event.getLocation() : "");
            analytics.save();
        }
    }

}

class MorningInfo {
    private static final String TAG = GlobalManager.createLogTag(MorningInfo.class);

    private Context context;

    Day day;
    int checkAlarmTimeGap;
    Analytics analytics;
    CalendarEvent event;
    Calendar alarmTime;
    Calendar targetAlarmTime;
    boolean attentionNeeded;

    MorningInfo(Context context) {
        this.context = context;
        init();
    }

    MorningInfo() {  // The empty constructor is here for testing with Mockito
    }

    public void setContext(Context context) {
        this.context = context;
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
    public void init() {
        Log.d(TAG, "onCheckAlarmTime()");

        // Find first calendar event
        GlobalManager globalManager = GlobalManager.getInstance();
        Clock clock = globalManager.clock();
        Calendar now = clock.now();

        Calendar tomorrowStart = beginningOfTomorrow(now);
        Log.v(TAG, "tomorrowStart=" + tomorrowStart.getTime());

        Calendar tomorrowNoon = justBeforeNoonTomorrow(now);
        Log.v(TAG, "tomorrowNoon=" + tomorrowNoon.getTime());

        // Load tomorrow's alarm time
        day = globalManager.loadDay(tomorrowStart);
        alarmTime = day.getDateTime();
        Log.v(TAG, "alarmTime=" + alarmTime.getTime() + ", enabled=" + day.isEnabled());

        // Load gap
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        checkAlarmTimeGap = preferences.getInt(SettingsActivity.PREF_CHECK_ALARM_TIME_GAP, SettingsActivity.PREF_CHECK_ALARM_TIME_GAP_DEFAULT);
        Log.v(TAG, "checkAlarmTimeGap=" + checkAlarmTimeGap + " minutes");

        analytics = new Analytics(context, Analytics.Event.Show, Analytics.Channel.Time, Analytics.ChannelName.Check_alarm_time);
        analytics.setDay(day);
        analytics.set(Analytics.Param.Check_alarm_time_gap, checkAlarmTimeGap);
        // TODO Analytics - add device location

        event = getEarliestEvent(tomorrowStart, tomorrowNoon);

        if (event != null) {
            targetAlarmTime = (Calendar) event.getBegin().clone();
            targetAlarmTime.add(Calendar.MINUTE, -checkAlarmTimeGap);
            Log.v(TAG, "targetAlarmTime=" + targetAlarmTime.getTime());

            // TODO Currently, the alarm time must be on the same date. Support alarm on the previous day.
            if (targetAlarmTime.before(tomorrowStart)) {
                targetAlarmTime = tomorrowStart;
                Log.v(TAG, "adjusted targetAlarmTime to " + targetAlarmTime.getTime());
            }

            analytics.set(Analytics.Param.Appointment_begin, Analytics.calendarToTime(event.getBegin()));
            analytics.set(Analytics.Param.Appointment_title, event.getTitle());
            analytics.set(Analytics.Param.Appointment_location, event.getLocation());

            attentionNeeded = !day.isEnabled() || targetAlarmTime.before(alarmTime);
            Log.v(TAG, "attentionNeeded=" + attentionNeeded);
            if (attentionNeeded) {
                Log.d(TAG, "Appointment that needs and earlier alarm time found");
                analytics.set(Analytics.Param.Check_alarm_time_action, CHECK_ALARM_TIME_ACTION__SHOW_NOTIFICATION);
            } else {
                analytics.set(Analytics.Param.Check_alarm_time_action, CHECK_ALARM_TIME_ACTION__DON_T_SHOW_NOTIFICATION);
            }
        } else {
            attentionNeeded = false;
            targetAlarmTime = null;

            analytics.set(Analytics.Param.Check_alarm_time_action, CHECK_ALARM_TIME_ACTION__NO_APPOINTMENT);
        }
    }

    CalendarEvent getEarliestEvent(Calendar tomorrowStart, Calendar tomorrowNoon) {
        if (tomorrowStart == null || tomorrowNoon == null) // Needed for mock initialization
            return null;

        CalendarHelper calendarHelper = new CalendarHelper(context);
        CalendarEventFilter notAllDay = new CalendarEventFilter() {
            @Override
            public boolean match(CalendarEvent event) {
                return !event.getAllDay();
            }
        };
        return calendarHelper.find(tomorrowStart, tomorrowNoon, notAllDay);
    }

    @Override
    public String toString() {
        return "MorningInfo{" +
                ", day=" + day +
                ", checkAlarmTimeGap=" + checkAlarmTimeGap +
                ", analytics=" + analytics +
                ", event=" + event +
                ", alarmTime=" + alarmTime +
                ", targetAlarmTime=" + targetAlarmTime +
                ", attentionNeeded=" + attentionNeeded +
                '}';
    }
}