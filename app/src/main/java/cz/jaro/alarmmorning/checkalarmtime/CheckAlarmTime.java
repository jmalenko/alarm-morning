package cz.jaro.alarmmorning.checkalarmtime;

import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Build;
import android.text.Html;
import android.text.SpannableString;
import android.util.Log;

import java.util.Calendar;
import java.util.NoSuchElementException;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import cz.jaro.alarmmorning.Analytics;
import cz.jaro.alarmmorning.GlobalManager;
import cz.jaro.alarmmorning.Localization;
import cz.jaro.alarmmorning.R;
import cz.jaro.alarmmorning.SettingsActivity;
import cz.jaro.alarmmorning.SharedPreferencesHelper;
import cz.jaro.alarmmorning.SystemAlarm;
import cz.jaro.alarmmorning.SystemNotification;
import cz.jaro.alarmmorning.calendar.CalendarEvent;
import cz.jaro.alarmmorning.calendar.CalendarEventFilter;
import cz.jaro.alarmmorning.calendar.CalendarHelper;
import cz.jaro.alarmmorning.clock.Clock;
import cz.jaro.alarmmorning.graphics.TimePreference;
import cz.jaro.alarmmorning.model.Day;
import cz.jaro.alarmmorning.model.Defaults;

import static cz.jaro.alarmmorning.Analytics.CHECK_ALARM_TIME_ACTION__DON_T_SHOW_NOTIFICATION;
import static cz.jaro.alarmmorning.Analytics.CHECK_ALARM_TIME_ACTION__NO_APPOINTMENT;
import static cz.jaro.alarmmorning.Analytics.CHECK_ALARM_TIME_ACTION__SHOW_NOTIFICATION;
import static cz.jaro.alarmmorning.calendar.CalendarUtils.beginningOfToday;
import static cz.jaro.alarmmorning.calendar.CalendarUtils.beginningOfTomorrow;
import static cz.jaro.alarmmorning.calendar.CalendarUtils.justBeforeNoonToday;
import static cz.jaro.alarmmorning.calendar.CalendarUtils.onTheSameDate;
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

    private final Context context;
    private final AlarmManager alarmManager;

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

    /*
     * Contains info about the current check.
     */
    static final String PERSIST__CHECK_ALARM_TIME__NOTIFICATION_EVENT_BEGIN = "PERSIST__CHECK_ALARM_TIME__NOTIFICATION_EVENT_BEGIN";
    static final String PERSIST__CHECK_ALARM_TIME__NOTIFICATION_ACTION = "PERSIST__CHECK_ALARM_TIME__NOTIFICATION_ACTION";

    static final String PERSIST__CHECK_ALARM_TIME__NOTIFICATION_ACTION__NOTIFICATION_NOT_DISPLAYED = "NOTIFICATION_NOT_DISPLAYED";
    static final String PERSIST__CHECK_ALARM_TIME__NOTIFICATION_ACTION__NOTIFICATION_DISPLAYED = "NOTIFICATION_DISPLAYED";
    static final String PERSIST__CHECK_ALARM_TIME__NOTIFICATION_ACTION__DELETED = "DELETED";
    static final String PERSIST__CHECK_ALARM_TIME__NOTIFICATION_ACTION__SET_TO_DEFAULT = "SET_TO_DEFAULT";
    static final String PERSIST__CHECK_ALARM_TIME__NOTIFICATION_ACTION__SET_TO_CUSTOM = "SET_TO_CUSTOM";
    static final String PERSIST__CHECK_ALARM_TIME__NOTIFICATION_ACTION__AUTO_HIDDEN = "AUTO_HIDDEN";

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

        return (boolean) SharedPreferencesHelper.load(SettingsActivity.PREF_CHECK_ALARM_TIME, SettingsActivity.PREF_CHECK_ALARM_TIME_DEFAULT);
    }

    public void checkAndRegister() {
        Log.v(TAG, "checkAndRegister()");

        if (isEnabled()) {
            register();
        }
    }

    public void register() {
        Log.v(TAG, "register()");

        String checkAlarmTimeAtPreference = (String) SharedPreferencesHelper.load(SettingsActivity.PREF_CHECK_ALARM_TIME_AT, SettingsActivity.PREF_CHECK_ALARM_TIME_AT_DEFAULT);

        register(checkAlarmTimeAtPreference);

        // Do the check if the time is after the check time
        boolean betweenCheckAlarmTimeAndAlarmTime = isBetweenCheckAlarmTimeAndAlarmTime();
        if (betweenCheckAlarmTimeAndAlarmTime) {
            Log.i(TAG, "Doing the check immediately after registering");
            doCheckAlarmTime();
        }
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
        Log.v(TAG, "unregister()");

        if (operation != null) {
            // Method 1: standard
            Log.d(TAG, "Cancelling current system alarm for Check Alarm Time");
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

        // Hide the notification if displayed
        if (isNotificationVisible()) {
            hideNotification();
        }

        // Cleanup the preferences
        SharedPreferencesHelper.remove(PERSIST__CHECK_ALARM_TIME__NOTIFICATION_EVENT_BEGIN);
        SharedPreferencesHelper.remove(PERSIST__CHECK_ALARM_TIME__NOTIFICATION_ACTION);
    }

    private void registerNotificationDismiss(Calendar time) {
        Log.v(TAG, "registerNotificationDismiss()");

        String action = ACTION_AUTO_HIDE_NOTIFICATION;
        Log.i(TAG, "Setting system alarm at " + time.getTime().toString() + " with action " + action);

        Intent intentDismissNotification = new Intent(context, CheckAlarmTimeAlarmReceiver.class);
        intentDismissNotification.setAction(action);

        operationDismissNotification = PendingIntent.getBroadcast(context, 1, intentDismissNotification, PendingIntent.FLAG_UPDATE_CURRENT);

        alarmManager.set(AlarmManager.RTC, time.getTimeInMillis(), operationDismissNotification);
    }

    private void unregisterNotificationDismiss() {
        Log.v(TAG, "unregisterNotificationDismiss()");

        if (operationDismissNotification != null) {
            // Method 1: standard
            Log.d(TAG, "Cancelling current system alarm for notification dismiss");
            operationDismissNotification.cancel();
        } else {
            // Method 2: try to recreate the operation
            Log.d(TAG, "Recreating operation when cancelling system alarm");

            Intent intentDismissNotification2 = new Intent(context, CheckAlarmTimeAlarmReceiver.class);
            intentDismissNotification2.setAction(ACTION_AUTO_HIDE_NOTIFICATION);

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
                onCalendarUpdated();
                Log.v(TAG, "data = " + intent.getData());
                break;
            default:
                throw new IllegalArgumentException("Unexpected argument " + action);
        }
    }

    private void onCheckAlarmTime() {
        Log.v(TAG, "onCheckAlarmTime()");

        // Register for tomorrow
        register();

        // Do the check
        doCheckAlarmTime();
    }

    private void doCheckAlarmTime() {
        MorningInfo morningInfo = new MorningInfo(context);
        doCheckAlarmTime(morningInfo);

        // Save analytics
        morningInfo.analytics.save();
    }

    void doCheckAlarmTime(MorningInfo morningInfo) {
        Log.v(TAG, "doCheckAlarmTime(morningInfo=" + morningInfo + ")");
        if (morningInfo.attentionNeeded) {
            showNotification(morningInfo.day, morningInfo.targetAlarmTime, morningInfo.event);
            registerNotificationDismiss(morningInfo.alarmTime);

            SharedPreferencesHelper.save(PERSIST__CHECK_ALARM_TIME__NOTIFICATION_EVENT_BEGIN, Analytics.calendarToDatetimeStringUTC(morningInfo.event.getBegin()));
            SharedPreferencesHelper.save(PERSIST__CHECK_ALARM_TIME__NOTIFICATION_ACTION,
                    morningInfo.attentionNeeded
                            ? PERSIST__CHECK_ALARM_TIME__NOTIFICATION_ACTION__NOTIFICATION_DISPLAYED
                            : PERSIST__CHECK_ALARM_TIME__NOTIFICATION_ACTION__NOTIFICATION_NOT_DISPLAYED);
        }
    }

    private void onAutoHideNotification() {
        Log.v(TAG, "onAutoHideNotification()");

        new Analytics(context, Analytics.Event.Hide, Analytics.Channel.Time, Analytics.ChannelName.Check_alarm_time).save();

        SharedPreferencesHelper.save(PERSIST__CHECK_ALARM_TIME__NOTIFICATION_ACTION, PERSIST__CHECK_ALARM_TIME__NOTIFICATION_ACTION__AUTO_HIDDEN);

        hideNotification();
    }

    /**
     * Note that Android broadcast an intent "something happened with calendar" (specifically the PROVIDER_CHANGED action). However this intent doesn't have any
     * attributes describing what happened. Therefore all the calendar events must be checked.
     */
    void onCalendarUpdated() {
        Log.v(TAG, "onCalendarUpdated()");
        boolean betweenCheckAlarmTimeAndAlarmTime = isBetweenCheckAlarmTimeAndAlarmTime();
        if (betweenCheckAlarmTimeAndAlarmTime) {
            boolean notificationVisible = isNotificationVisible();

            Calendar notificationEventBegin;
            String notificationAction;
            try {
                String notificationEventBeginStr = (String) SharedPreferencesHelper.load(PERSIST__CHECK_ALARM_TIME__NOTIFICATION_EVENT_BEGIN);
                notificationEventBegin = Analytics.datetimeUTCStringToCalendar(notificationEventBeginStr);
                notificationAction = (String) SharedPreferencesHelper.load(PERSIST__CHECK_ALARM_TIME__NOTIFICATION_ACTION);
            } catch (NoSuchElementException e) {
                notificationEventBegin = null;
                notificationAction = null;
            }

            Log.v(TAG, "notificationEventBegin=" + (notificationEventBegin != null ? Analytics.calendarToDatetimeStringUTC(notificationEventBegin) : "null"));
            Log.v(TAG, "notificationAction=" + notificationAction);

            MorningInfo morningInfo = new MorningInfo(context);
            Calendar eventBegin = morningInfo.event != null ? morningInfo.event.getBegin() : null;

            Log.v(TAG, "morningInfo=" + morningInfo);

            // Check for an earlier event

            boolean checkForAnEarlierEvent = morningInfo.attentionNeeded
                    && (notificationEventBegin == null || (!onTheSameDate(notificationEventBegin, morningInfo.day.getDate()) || (eventBegin != null && eventBegin.before(notificationEventBegin))));
            Log.d(TAG, "Check for an earlier event = " + checkForAnEarlierEvent);
            if (checkForAnEarlierEvent) {
                Log.i(TAG, "Updating notification because of an earlier event");

                if (notificationVisible) {
                    unregisterNotificationDismiss();
                }
                showNotification(morningInfo.day, morningInfo.targetAlarmTime, morningInfo.event);
                registerNotificationDismiss(morningInfo.alarmTime);

                SharedPreferencesHelper.save(PERSIST__CHECK_ALARM_TIME__NOTIFICATION_EVENT_BEGIN, Analytics.calendarToDatetimeStringUTC(morningInfo.event.getBegin()));
                SharedPreferencesHelper.save(PERSIST__CHECK_ALARM_TIME__NOTIFICATION_ACTION, PERSIST__CHECK_ALARM_TIME__NOTIFICATION_ACTION__NOTIFICATION_DISPLAYED);
            }

            // Check for a deleted event (that triggered setting the alarm)

            boolean checkForADeletedEvent = notificationEventBegin != null && notificationAction != null && (eventBegin == null || notificationEventBegin.before(eventBegin));
            Log.d(TAG, "Check for a deleted event (that triggered setting the alarm) = " + checkForADeletedEvent);
            if (checkForADeletedEvent) {
                if (notificationAction.equals(PERSIST__CHECK_ALARM_TIME__NOTIFICATION_ACTION__NOTIFICATION_DISPLAYED)) {
                    Log.i(TAG, "Updating notification because of a deleted event");

                    hideNotification();

                    if (morningInfo.attentionNeeded) {
                        showNotification(morningInfo.day, morningInfo.targetAlarmTime, morningInfo.event);
                        registerNotificationDismiss(morningInfo.alarmTime);
                        SharedPreferencesHelper.save(PERSIST__CHECK_ALARM_TIME__NOTIFICATION_EVENT_BEGIN, Analytics.calendarToDatetimeStringUTC(morningInfo.event.getBegin()));
                        SharedPreferencesHelper.save(PERSIST__CHECK_ALARM_TIME__NOTIFICATION_ACTION, PERSIST__CHECK_ALARM_TIME__NOTIFICATION_ACTION__NOTIFICATION_DISPLAYED);
                    }
                } else if (notificationAction.equals(PERSIST__CHECK_ALARM_TIME__NOTIFICATION_ACTION__SET_TO_CUSTOM)
                        || notificationAction.equals(PERSIST__CHECK_ALARM_TIME__NOTIFICATION_ACTION__SET_TO_DEFAULT)) {
                    Log.i(TAG, "Showing notification because of a deleted event");

                    showNotification(morningInfo.day, morningInfo.targetAlarmTime, morningInfo.event, true);
                    registerNotificationDismiss(morningInfo.alarmTime);

                    Calendar newNotificationEventBegin = morningInfo.event != null ? morningInfo.event.getBegin() : justBeforeNoonToday(calcMorningDate()); // Note: relative to morning

                    SharedPreferencesHelper.save(PERSIST__CHECK_ALARM_TIME__NOTIFICATION_EVENT_BEGIN, Analytics.calendarToDatetimeStringUTC(newNotificationEventBegin));
                    SharedPreferencesHelper.save(PERSIST__CHECK_ALARM_TIME__NOTIFICATION_ACTION, PERSIST__CHECK_ALARM_TIME__NOTIFICATION_ACTION__NOTIFICATION_DISPLAYED);
                }
            }

            // Otherwise hide (e.g. a meeting that triggered the notification
            boolean hide = !checkForADeletedEvent && !morningInfo.attentionNeeded && notificationVisible;
            Log.d(TAG, "Check for hide notification =  = " + hide);
            if (hide) {
                Log.i(TAG, "Hiding notification");
                hideNotification();
            }
        }
    }

    /**
     * Checks that the current time is after the check alarm time and before the following noon.
     *
     * @return True iff the current time is after the check alarm time and before the following noon.
     */
    private boolean isBetweenCheckAlarmTimeAndAlarmTime() {
        return calcMorningDate() != null;
    }

    /**
     * @return The date on which the alarm should be compared to the earliest event. (Note: the actual check is usually done in the evening of previous day.)
     */
    static public Calendar calcMorningDate() {
        Log.v(TAG, "calcMorningDate()");
        String checkAlarmTimeAtPreference = (String) SharedPreferencesHelper.load(SettingsActivity.PREF_CHECK_ALARM_TIME_AT, SettingsActivity.PREF_CHECK_ALARM_TIME_AT_DEFAULT);

        GlobalManager globalManager = GlobalManager.getInstance();
        Clock clock = globalManager.clock();
        Calendar now = clock.now();

        Calendar checkAlarmTimeAtToday = calcTodaysOccurence(checkAlarmTimeAtPreference);

        Log.d(TAG, "checkAlarmTimeAtToday=" + checkAlarmTimeAtToday.getTime());

        if (!now.before(checkAlarmTimeAtToday)) {
            Calendar beginningOfTomorrow = beginningOfTomorrow(now);
            Log.d(TAG, "calcMorningDate returns " + beginningOfTomorrow.getTime());
            return beginningOfTomorrow;
        }

        Calendar todayNoon = justBeforeNoonToday(now);
        if (now.before(todayNoon)) {
            Calendar beginningOfToday = beginningOfToday(now);
            Log.d(TAG, "calcMorningDate returns " + beginningOfToday.getTime());
            return beginningOfToday;
        }

        Log.d(TAG, "calcMorningDate returns null");
        return null;
    }

    private boolean isNotificationVisible() {
        Intent intent = new Intent(context, CheckAlarmTimeNotificationReceiver.class);
        intent.setAction(CheckAlarmTimeNotificationReceiver.ACTION_CHECK_ALARM_TIME_CLICK);
        PendingIntent test = PendingIntent.getBroadcast(context, REQUEST_CODE, intent, PendingIntent.FLAG_NO_CREATE);
        return test != null;
    }

    private void showNotification(Day day, Calendar targetAlarmTime, CalendarEvent event) {
        showNotification(day, targetAlarmTime, event, false);
    }

    private void showNotification(Day day, Calendar targetAlarmTime, CalendarEvent event, boolean originalEventDeleted) {
        Log.v(TAG, "showNotification()");
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

        String contentText = "";
        String bodyText = "";
        if (originalEventDeleted) {
            contentText += res.getString(R.string.notification_check_text_event_deleted) + " ";
            bodyText += res.getString(R.string.notification_check_text_event_deleted) + "<br>";
        }
        if (event != null) {
            String meetingTimeText = Localization.timeToString(event.getBegin().get(Calendar.HOUR_OF_DAY), event.getBegin().get(Calendar.MINUTE), context);
            if (event.getLocation() != null && !event.getLocation().isEmpty()) {
                contentText += res.getString(R.string.notification_check_text_with_location, meetingTimeText, event.getTitle(), event.getLocation());
                bodyText += res.getString(R.string.notification_check_text_with_location, meetingTimeText, event.getTitle(), event.getLocation());
            } else {
                contentText += res.getString(R.string.notification_check_text_without_location, meetingTimeText, event.getTitle());
                bodyText += res.getString(R.string.notification_check_text_without_location, meetingTimeText, event.getTitle());
            }
        } else {
            contentText += res.getString(R.string.notification_check_no_event);
            bodyText += res.getString(R.string.notification_check_no_event);
        }
        mBuilder.setContentText(contentText);

        SpannableString formattedBody = new SpannableString(
                Build.VERSION.SDK_INT < Build.VERSION_CODES.N
                        ? Html.fromHtml(bodyText)
                        : Html.fromHtml(bodyText, Html.FROM_HTML_MODE_LEGACY)
        );
        mBuilder.setStyle(new NotificationCompat.BigTextStyle().bigText(formattedBody).setBigContentTitle(contentTitle));

        Intent intent = new Intent(context, CheckAlarmTimeNotificationReceiver.class);
        intent.setAction(CheckAlarmTimeNotificationReceiver.ACTION_CHECK_ALARM_TIME_CLICK);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, REQUEST_CODE, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        mBuilder.setContentIntent(pendingIntent);

        Intent deleteIntent = new Intent(context, CheckAlarmTimeNotificationReceiver.class);
        deleteIntent.setAction(CheckAlarmTimeNotificationReceiver.ACTION_CHECK_ALARM_TIME_DELETE);
        PendingIntent deletePendingIntent = PendingIntent.getBroadcast(context, REQUEST_CODE, deleteIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        mBuilder.setDeleteIntent(deletePendingIntent);

        if (event != null) {
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
        } else {
            // Time from default if relevant (different from current alarm time and default is enabled)
            Defaults defaults = day.getDefaults();
            if (!day.sameAsDefault() && defaults.isEnabled()) {
                GlobalManager globalManager = GlobalManager.getInstance();
                Clock clock = globalManager.clock();
                Calendar now = clock.now();

                Calendar defaultAlarmTime = beginningOfTomorrow(now);
                defaultAlarmTime.set(Calendar.HOUR_OF_DAY, defaults.getHour());
                defaultAlarmTime.set(Calendar.MINUTE, defaults.getMinute());

                String targetTimeText = Localization.timeToString(defaults.getHour(), defaults.getMinute(), context);
                String setText = res.getString(R.string.notification_check_text_set_at, targetTimeText);
                Intent setIntent = new Intent(context, CheckAlarmTimeNotificationReceiver.class);
                setIntent.setAction(CheckAlarmTimeNotificationReceiver.ACTION_CHECK_ALARM_TIME_SET_TO);
                setIntent.putExtra(CheckAlarmTimeNotificationReceiver.EXTRA_NEW_ALARM_TIME, defaultAlarmTime.getTimeInMillis());
                PendingIntent setPendingIntent = PendingIntent.getBroadcast(context, REQUEST_CODE, setIntent, PendingIntent.FLAG_UPDATE_CURRENT);
                mBuilder.addAction(R.drawable.ic_alarm_on_white, setText, setPendingIntent);
            }

            String disableText = res.getString(R.string.action_disable);
            Intent disableIntent = new Intent(context, CheckAlarmTimeNotificationReceiver.class);
            disableIntent.setAction(CheckAlarmTimeNotificationReceiver.ACTION_CHECK_ALARM_TIME_DISABLE);
            disableIntent.putExtra(CheckAlarmTimeNotificationReceiver.EXTRA_NEW_ALARM_TIME, day.getDate().getTimeInMillis());
            PendingIntent setPendingIntent = PendingIntent.getBroadcast(context, REQUEST_CODE, disableIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            mBuilder.addAction(R.drawable.ic_alarm_on_white, disableText, setPendingIntent);
        }

        NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.notify(NOTIFICATION_ID, mBuilder.build());
    }

    private void hideNotificationOnly() {
        Log.v(TAG, "hideNotificationOnly()");
        NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.cancel(NOTIFICATION_ID);
    }

    void hideNotification() {
        Log.d(TAG, "Hide notification");
        unregisterNotificationDismiss();
        hideNotificationOnly();
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
        Log.v(TAG, "onAlarmSet()");

        if (isBetweenCheckAlarmTimeAndAlarmTime()) {
            MorningInfo morningInfo = new MorningInfo(context);

            // If the alarm time was changed to a time long enough before the first meeting and notification exists, then hide the notification
            if (!morningInfo.attentionNeeded && isNotificationVisible()) {
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
        Log.v(TAG, "init()");

        // Note: This is usually executed in the evening (during the regular check). But it is also executed in the morning of the day on wh:
        // 1. The user enables the check (in the Settings) between modnight nad noon
        // 2. There is an update of calendar events

        // Find first calendar event
        GlobalManager globalManager = GlobalManager.getInstance();

        Calendar morningStart = CheckAlarmTime.calcMorningDate();
        Log.v(TAG, "morningStart=" + morningStart.getTime());

        Calendar morningNoon = justBeforeNoonToday(morningStart); // Note: relative to morningStart
        Log.v(TAG, "morningNoon=" + morningNoon.getTime());

        // Load tomorrow's alarm time
        day = globalManager.loadDay(morningStart);
        alarmTime = day.getDateTime();
        Log.v(TAG, "alarmTime=" + alarmTime.getTime() + ", enabled=" + day.isEnabled());

        // Load gap
        checkAlarmTimeGap = (int) SharedPreferencesHelper.load(SettingsActivity.PREF_CHECK_ALARM_TIME_GAP, SettingsActivity.PREF_CHECK_ALARM_TIME_GAP_DEFAULT);
        Log.v(TAG, "checkAlarmTimeGap=" + checkAlarmTimeGap + " minutes");

        analytics = new Analytics(context, Analytics.Event.Show, Analytics.Channel.Time, Analytics.ChannelName.Check_alarm_time);
        analytics.setDay(day);
        analytics.set(Analytics.Param.Check_alarm_time_gap, checkAlarmTimeGap);
        // TODO Analytics - add device location

        event = getEarliestEvent(morningStart, morningNoon);

        if (event != null) {
            targetAlarmTime = (Calendar) event.getBegin().clone();
            targetAlarmTime.add(Calendar.MINUTE, -checkAlarmTimeGap);
            Log.v(TAG, "targetAlarmTime=" + targetAlarmTime.getTime());

            // TODO The alarm time must be on the same date. If it's before that then shift the alarm time to midnight. Task: Support alarm on the previous day.
            if (targetAlarmTime.before(morningStart)) {
                targetAlarmTime = morningStart;
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
//                ", analytics=" + analytics +
//                ", event=" + event +
                ", event begin=" + (event != null ? Analytics.calendarToDatetimeStringUTC(event.getBegin()) : "null") +
                ", alarmTime=" + Analytics.calendarToDatetimeStringUTC(alarmTime) +
                ", targetAlarmTime=" + (targetAlarmTime != null ? Analytics.calendarToDatetimeStringUTC(targetAlarmTime) : "null") +
                ", attentionNeeded=" + attentionNeeded +
                '}';
    }
}