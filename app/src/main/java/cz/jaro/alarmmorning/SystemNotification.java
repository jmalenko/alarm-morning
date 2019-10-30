package cz.jaro.alarmmorning;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Build;
import android.util.Log;

import java.util.Calendar;
import java.util.List;
import java.util.NoSuchElementException;

import androidx.core.app.NotificationCompat;
import cz.jaro.alarmmorning.clock.Clock;
import cz.jaro.alarmmorning.model.AppAlarm;
import cz.jaro.alarmmorning.model.Day;
import cz.jaro.alarmmorning.model.OneTimeAlarm;
import cz.jaro.alarmmorning.receivers.NotificationReceiver;

import static cz.jaro.alarmmorning.GlobalManager.PERSIST_ALARM_ID;
import static cz.jaro.alarmmorning.GlobalManager.PERSIST_ALARM_TYPE;
import static cz.jaro.alarmmorning.GlobalManager.getNearFutureTime;
import static cz.jaro.alarmmorning.GlobalManager.useNearFutureTime;
import static cz.jaro.alarmmorning.calendar.CalendarUtils.onTheSameDate;

/**
 * SystemNotification manages the notification about the alarm.
 */
public class SystemNotification {

    private static final String TAG = GlobalManager.createLogTag(SystemNotification.class);

    private static SystemNotification instance;
    private final Context context;

    public static final String CHANNEL_ID = "MAIN_NOTIFICATION";
    private static final int NOTIFICATION_ID = 0;
    private static int NOTIFICATION_ERROR_ID = NOTIFICATION_ID;

    private static final String PERSIST_NOTIFICATION_ALARM_TYPE = "PERSIST_NOTIFICATION_ALARM_TYPE";
    private static final String PERSIST_NOTIFICATION_DAY_ALARM_DATE = "PERSIST_NOTIFICATION_DAY_ALARM_DATE";
    private static final String PERSIST_NOTIFICATION_ONE_TIME_ALARM_ID = "PERSIST_NOTIFICATION_ONE_TIME_ALARM_ID";

    private SystemNotification(Context context) {
        this.context = context;
    }

    public static SystemNotification getInstance(Context context) {
        if (instance == null) {
            instance = new SystemNotification(context);
        }
        return instance;
    }

    private NotificationCompat.Builder buildNotification(AppAlarm appAlarm) {
        Resources res = context.getResources();
        String timeText = Localization.timeToString(appAlarm.getHour(), appAlarm.getMinute(), context);

        String contentTitle = appAlarm instanceof OneTimeAlarm && ((OneTimeAlarm) appAlarm).getName() != null && !((OneTimeAlarm) appAlarm).getName().isEmpty()
                ? res.getString(R.string.notification_title_with_name, timeText, ((OneTimeAlarm) appAlarm).getName())
                : res.getString(R.string.notification_title, timeText);

        createNotificationChannel(context);
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_alarm_white)
                .setContentTitle(contentTitle)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_SERVICE);

        Intent deleteIntent = new Intent(context, NotificationReceiver.class);
        deleteIntent.setAction(NotificationReceiver.ACTION_DELETE_NOTIFICATION);
        deleteIntent.putExtra(PERSIST_ALARM_TYPE, appAlarm.getClass().getSimpleName());
        deleteIntent.putExtra(PERSIST_ALARM_ID, appAlarm.getPersistenceId());
        PendingIntent deletePendingIntent = PendingIntent.getBroadcast(context, 1, deleteIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        mBuilder.setDeleteIntent(deletePendingIntent);

        return mBuilder;
    }

    public static void createNotificationChannel(Context context) {
        // Create the NotificationChannel, but only on API 26+ because the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String name = context.getString(R.string.channel_name);
            String descriptionText = context.getString(R.string.channel_description);
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(descriptionText);

            // Register the channel with the system
            NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private void showNotification(NotificationCompat.Builder mBuilder, AppAlarm appAlarm) {
        Log.d(TAG, "showNotification()");
        NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.notify(NOTIFICATION_ID, mBuilder.build());

        persist(appAlarm);
    }

    private void hideNotification() {
        Log.d(TAG, "hideNotification()");
        NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.cancel(NOTIFICATION_ID);

        persistRemove();
    }

    /*
     * Events relevant to next alarm
     * =============================
     */

    void onNearFuture(AppAlarm appAlarm) {
        Log.d(TAG, "onNearFuture(appAlarm=" + appAlarm + ")");

        NotificationCompat.Builder mBuilder = buildNotification(appAlarm);

        Resources res = context.getResources();
        String contentText = res.getString(R.string.notification_text_future);
        mBuilder.setContentText(contentText);

        Intent intent = new Intent(context, NotificationReceiver.class);
        intent.setAction(NotificationReceiver.ACTION_CLICK_NOTIFICATION);
        intent.putExtra(PERSIST_ALARM_TYPE, appAlarm.getClass().getSimpleName());
        intent.putExtra(PERSIST_ALARM_ID, appAlarm.getPersistenceId());
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 1, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        mBuilder.setContentIntent(pendingIntent);

        String dismissText = res.getString(R.string.action_dismiss);
        Intent dismissIntent = new Intent(context, NotificationReceiver.class);
        dismissIntent.setAction(NotificationReceiver.ACTION_DISMISS_BEFORE_RINGING);
        dismissIntent.putExtra(PERSIST_ALARM_TYPE, appAlarm.getClass().getSimpleName());
        dismissIntent.putExtra(PERSIST_ALARM_ID, appAlarm.getPersistenceId());
        PendingIntent dismissPendingIntent = PendingIntent.getBroadcast(context, 1, dismissIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        mBuilder.addAction(R.drawable.ic_alarm_off_white, dismissText, dismissPendingIntent);

        showNotification(mBuilder, appAlarm);
    }

    void onDismissBeforeRinging(AppAlarm appAlarm) {
        Log.d(TAG, "onDismissBeforeRinging(appAlarm=" + appAlarm + ")");

        if (currentlyDisplayedNotificationIsAbout(appAlarm)) {
            hideNotification();
        }
    }

    private boolean currentlyDisplayedNotificationIsAbout(AppAlarm appAlarm) {
        try {
            String retrieveAlarmType = (String) SharedPreferencesHelper.load(PERSIST_NOTIFICATION_ALARM_TYPE);

            if (appAlarm instanceof Day) {
                Day day = (Day) appAlarm;
                if (retrieveAlarmType.equals("Day")) {
                    String dateStr = (String) SharedPreferencesHelper.load(PERSIST_NOTIFICATION_DAY_ALARM_DATE);
                    Calendar retrieveDayAlarmDate = Analytics.dateStringToCalendar(dateStr);
                    return onTheSameDate(day.getDate(), retrieveDayAlarmDate);
                }
            } else if (appAlarm instanceof OneTimeAlarm) {
                OneTimeAlarm oneTimeAlarm = (OneTimeAlarm) appAlarm;
                if (retrieveAlarmType.equals("OneTimeAlarm")) {
                    long retrieveOneTimeAlarmId = (long) SharedPreferencesHelper.load(PERSIST_NOTIFICATION_ONE_TIME_ALARM_ID);
                    return oneTimeAlarm.getId() == retrieveOneTimeAlarmId;
                }
            } else {
                throw new IllegalArgumentException("Unexpected class " + appAlarm.getClass());
            }
        } catch (NoSuchElementException e) {
            Log.v(TAG, "The persisted data is incomplete", e);
            return false;
        }

        return false;
    }

    void onRing(AppAlarm appAlarm) {
        Log.d(TAG, "onRing(appAlarm=" + appAlarm + ")");

        NotificationCompat.Builder mBuilder = buildNotification(appAlarm);

        Resources res = context.getResources();
        String contentText = res.getString(R.string.notification_text_ringing);
        mBuilder.setContentText(contentText);

        Intent intent = new Intent(context, NotificationReceiver.class);
        intent.setAction(NotificationReceiver.ACTION_CLICK_NOTIFICATION);
        intent.putExtra(NotificationReceiver.EXTRA_ACTIVITY, NotificationReceiver.EXTRA_ACTIVITY__RING);
        intent.putExtra(PERSIST_ALARM_TYPE, appAlarm.getClass().getSimpleName());
        intent.putExtra(PERSIST_ALARM_ID, appAlarm.getPersistenceId());
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 1, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        mBuilder.setContentIntent(pendingIntent);

        String dismissText = res.getString(R.string.action_dismiss);
        Intent dismissIntent = new Intent(context, NotificationReceiver.class);
        dismissIntent.setAction(NotificationReceiver.ACTION_DISMISS);
        dismissIntent.putExtra(PERSIST_ALARM_TYPE, appAlarm.getClass().getSimpleName());
        dismissIntent.putExtra(PERSIST_ALARM_ID, appAlarm.getPersistenceId());
        PendingIntent dismissPendingIntent = PendingIntent.getBroadcast(context, 1, dismissIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        mBuilder.addAction(R.drawable.ic_alarm_off_white, dismissText, dismissPendingIntent);

        String snoozeText = res.getString(R.string.action_snooze);
        Intent snoozeIntent = new Intent(context, NotificationReceiver.class);
        snoozeIntent.setAction(NotificationReceiver.ACTION_SNOOZE);
        snoozeIntent.putExtra(PERSIST_ALARM_TYPE, appAlarm.getClass().getSimpleName());
        snoozeIntent.putExtra(PERSIST_ALARM_ID, appAlarm.getPersistenceId());
        PendingIntent snoozePendingIntent = PendingIntent.getBroadcast(context, 1, snoozeIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        mBuilder.addAction(R.drawable.ic_snooze_white, snoozeText, snoozePendingIntent);

        showNotification(mBuilder, appAlarm);
    }

    void onDismiss(AppAlarm appAlarm) {
        Log.d(TAG, "onDismiss(appAlarm=" + appAlarm + ")");

        hideNotification();
    }

    void onSnooze(AppAlarm appAlarm, Calendar ringAfterSnoozeTime) {
        Log.d(TAG, "onSnooze(appAlarm=" + appAlarm + ")");

        NotificationCompat.Builder mBuilder = buildNotification(appAlarm);

        Resources res = context.getResources();
        String ringAfterSnoozeTimeText = Localization.timeToString(ringAfterSnoozeTime.get(Calendar.HOUR_OF_DAY), ringAfterSnoozeTime.get(Calendar.MINUTE), context);
        String contentText = res.getString(R.string.notification_text_snoozed, ringAfterSnoozeTimeText);
        mBuilder.setContentText(contentText);

        Intent intent = new Intent(context, NotificationReceiver.class);
        intent.setAction(NotificationReceiver.ACTION_CLICK_NOTIFICATION);
        intent.putExtra(PERSIST_ALARM_TYPE, appAlarm.getClass().getSimpleName());
        intent.putExtra(PERSIST_ALARM_ID, appAlarm.getPersistenceId());
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 1, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        mBuilder.setContentIntent(pendingIntent);

        String dismissText = res.getString(R.string.action_dismiss);
        Intent dismissIntent = new Intent(context, NotificationReceiver.class);
        dismissIntent.setAction(NotificationReceiver.ACTION_DISMISS);
        dismissIntent.putExtra(PERSIST_ALARM_TYPE, appAlarm.getClass().getSimpleName());
        dismissIntent.putExtra(PERSIST_ALARM_ID, appAlarm.getPersistenceId());
        PendingIntent dismissPendingIntent = PendingIntent.getBroadcast(context, 1, dismissIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        mBuilder.addAction(R.drawable.ic_alarm_off_white, dismissText, dismissPendingIntent);

        showNotification(mBuilder, appAlarm);
    }

    void onAlarmCancel(AppAlarm appAlarm) {
        Log.d(TAG, "onAlarmCancel(appAlarm=" + appAlarm + ")");

        hideNotification();
    }

    /*
     * Events relevant to alarm management
     * ===================================
     */

    void onDeleteOneTimeAlarm(OneTimeAlarm oneTimeAlarm) {
        Log.d(TAG, "onDismissBeforeRinging(appAlarm=" + oneTimeAlarm + ")");

        if (currentlyDisplayedNotificationIsAbout(oneTimeAlarm)) {
            hideNotification();

            // Possibly display a following notification
            GlobalManager globalManager = GlobalManager.getInstance();
            AppAlarm nextAlarm = globalManager.getNextAlarm();
            if (nextAlarm != null) {
                Clock clock = globalManager.clock();
                Calendar now = clock.now();

                Calendar alarmTime = nextAlarm.getDateTime();

                if (useNearFutureTime()) {
                    Calendar nearFutureTime = getNearFutureTime(alarmTime);

                    if (nearFutureTime.before(now)) {
                        onNearFuture(nextAlarm);
                    }
                }
            }
        }
    }

    void onModifyOneTimeAlarmDateTime(OneTimeAlarm oneTimeAlarm) {
        Log.d(TAG, "onModifyOneTimeAlarmDateTime(oneTimeAlarm = " + oneTimeAlarm + ")");

        if (currentlyDisplayedNotificationIsAbout(oneTimeAlarm)) {
            GlobalManager globalManager = GlobalManager.getInstance();

            if (!globalManager.inNearFuturePeriod(oneTimeAlarm.getDateTime())) {
                hideNotification();
            }
        }
    }

    void onModifyOneTimeAlarmName(OneTimeAlarm oneTimeAlarm) {
        Log.d(TAG, "onModifyOneTimeAlarmName(oneTimeAlarm = " + oneTimeAlarm + ")");

        if (currentlyDisplayedNotificationIsAbout(oneTimeAlarm)) {
            hideNotification();

            // Display a new notification
            GlobalManager globalManager = GlobalManager.getInstance();
            Clock clock = globalManager.clock();
            Calendar now = clock.now();

            Calendar alarmTime = oneTimeAlarm.getDateTime();
            Calendar nearFutureTime = getNearFutureTime(alarmTime);

            if (now.before(nearFutureTime)) {
                // Nothing
            } else if (now.before(alarmTime)) {
                if (useNearFutureTime()) {
                    onNearFuture(oneTimeAlarm);
                }
            } else {
                int state = globalManager.getState(oneTimeAlarm);
                switch (state) {
                    case GlobalManager.STATE_RINGING:
                        onRing(oneTimeAlarm);
                        break;
                    case GlobalManager.STATE_SNOOZED:
                        Calendar ringAfterSnoozeTime = globalManager.getRingAfterSnoozeTime(globalManager.clock());
                        onSnooze(oneTimeAlarm, ringAfterSnoozeTime);
                        break;
                }
            }
        }
    }

    /*
     * Other
     * =====
     */

    void notifyCancelledAlarm(AppAlarm appAlarm) {
        Log.d(TAG, "notifyCancelledAlarm(appAlarm=" + appAlarm + ")");

        NotificationCompat.Builder mBuilder = buildNotification(appAlarm); // TODO The title shows just the time. Task: On mignight, after the notification was displayed, update the notification such that the title includes both time and date

        Resources res = context.getResources();
        String contentText = res.getString(R.string.notification_text_cancelled);
        mBuilder.setContentText(contentText);

        Intent intent = new Intent(context, NotificationReceiver.class);
        intent.setAction(NotificationReceiver.ACTION_CLICK_NOTIFICATION);
        intent.putExtra(PERSIST_ALARM_TYPE, appAlarm.getClass().getSimpleName());
        intent.putExtra(PERSIST_ALARM_ID, appAlarm.getPersistenceId());
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 1, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        mBuilder.setContentIntent(pendingIntent);

        NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.notify(++NOTIFICATION_ERROR_ID, mBuilder.build());
    }

    /**
     * Shows a notification with the skipped alarm times.
     *
     * @param skippedAlarms The count of skipped alarm times.
     */
    void notifySkippedAlarms(List<AppAlarm> skippedAlarms) {
        Log.d(TAG, "notifySkippedAlarms()");

        Resources res = context.getResources();
        String contentTitle = res.getString(R.string.app_name);
        String contentText = res.getString(R.string.notification_text_skipped,
                res.getString(R.string.notification_text_skipped_count, skippedAlarms.size()),
                Localization.appAlarmsToString(skippedAlarms, context));
        Log.v(TAG, contentText);

        createNotificationChannel(context);
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_alarm_white)
                .setContentTitle(contentTitle)
                .setContentText(contentText);

        Intent intent = new Intent(context, AlarmMorningActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 1, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        mBuilder.setContentIntent(pendingIntent);

        NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.notify(++NOTIFICATION_ERROR_ID, mBuilder.build());
    }

    private void persist(AppAlarm appAlarm) {
        Log.v(TAG, "persist(appAlarm=" + appAlarm + ")");

        SharedPreferencesHelper.save(PERSIST_NOTIFICATION_ALARM_TYPE, appAlarm.getClass().getSimpleName());
        if (appAlarm instanceof Day) {
            Day day = (Day) appAlarm;
            SharedPreferencesHelper.save(PERSIST_NOTIFICATION_DAY_ALARM_DATE, Analytics.calendarToStringDate(day.getDate()));
        } else if (appAlarm instanceof OneTimeAlarm) {
            OneTimeAlarm oneTimeAlarm = (OneTimeAlarm) appAlarm;
            SharedPreferencesHelper.save(PERSIST_NOTIFICATION_ONE_TIME_ALARM_ID, oneTimeAlarm.getId());
        } else {
            throw new IllegalArgumentException("Unexpected class " + appAlarm.getClass());
        }
    }

    private void persistRemove() {
        Log.v(TAG, "persistRemove()");

        SharedPreferencesHelper.remove(PERSIST_NOTIFICATION_ALARM_TYPE);
        SharedPreferencesHelper.remove(PERSIST_NOTIFICATION_DAY_ALARM_DATE);
        SharedPreferencesHelper.remove(PERSIST_NOTIFICATION_ONE_TIME_ALARM_ID);
    }

}
