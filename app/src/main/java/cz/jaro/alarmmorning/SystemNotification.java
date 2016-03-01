package cz.jaro.alarmmorning;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import java.util.Calendar;

import cz.jaro.alarmmorning.clock.Clock;
import cz.jaro.alarmmorning.clock.SystemClock;
import cz.jaro.alarmmorning.model.Day;
import cz.jaro.alarmmorning.receivers.NotificationReceiver;

/**
 * Created by ext93831 on 18.1.2016.
 */
public class SystemNotification {

    private static final String TAG = SystemNotification.class.getSimpleName();

    private static SystemNotification instance;
    private Context context;

    private static final int NOTIFICATION_ID = 0;
    private static int NOTIFICATION_ERROR_ID = NOTIFICATION_ID;

    private SystemNotification(Context context) {
        this.context = context;
    }

    public static SystemNotification getInstance(Context context) {
        if (instance == null) {
            instance = new SystemNotification(context);
        }
        return instance;
    }

    private NotificationCompat.Builder buildNotification() {
        GlobalManager globalManager = new GlobalManager(context);
        Day day = globalManager.getDayWithNextAlarm();

        Resources res = context.getResources();
        Clock clock = new SystemClock(); // TODO Solve dependency on clock
        String timeText = Localization.timeToString(day.getHourX(), day.getMinuteX(), context, clock);
        String contentTitle = res.getString(R.string.notification_title, timeText);

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context)
                .setSmallIcon(R.drawable.ic_alarm_white)
                .setContentTitle(contentTitle);

        Intent deleteIntent = new Intent(context, NotificationReceiver.class);
        deleteIntent.setAction(NotificationReceiver.ACTION_DELETE_NOTIFICATION);
        PendingIntent deletePendingIntent = PendingIntent.getBroadcast(context, 1, deleteIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        mBuilder.setDeleteIntent(deletePendingIntent);

        return mBuilder;
    }

    private void showNotification(NotificationCompat.Builder mBuilder) {
        Log.d(TAG, "showNotification()");
        NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.notify(NOTIFICATION_ID, mBuilder.build());
    }

    private void hideNotification() {
        Log.d(TAG, "hideNotification()");
        NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.cancel(NOTIFICATION_ID);
    }

    /*
     * Events
     * ======
     */

    public void onAlarmSet() {
        Log.d(TAG, "onAlarmSet()");

        hideNotification();
    }

    protected void onNearFuture() {
        Log.d(TAG, "onNearFuture()");

        NotificationCompat.Builder mBuilder = buildNotification();

        Resources res = context.getResources();
        String contentText = res.getString(R.string.notification_text_future);
        mBuilder.setContentText(contentText);

        Intent intent = new Intent(context, AlarmMorningActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 1, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        mBuilder.setContentIntent(pendingIntent);

        String dismissText = res.getString(R.string.action_dismiss);
        Intent dismissIntent = new Intent(context, NotificationReceiver.class);
        dismissIntent.setAction(NotificationReceiver.ACTION_DISMISS_BEFORE_RINGING);
        PendingIntent dismissPendingIntent = PendingIntent.getBroadcast(context, 1, dismissIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        mBuilder.addAction(R.drawable.ic_alarm_off_white, dismissText, dismissPendingIntent);

        showNotification(mBuilder);
    }

    public void onDismissBeforeRinging() {
        Log.d(TAG, "onDismissBeforeRinging()");

        hideNotification();
    }

    protected void onRing() {
        Log.d(TAG, "onRing()");

        NotificationCompat.Builder mBuilder = buildNotification();

        Resources res = context.getResources();
        String contentText = res.getString(R.string.notification_text_ringing);
        mBuilder.setContentText(contentText);

        Intent intent = new Intent(context, RingActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 1, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        mBuilder.setContentIntent(pendingIntent);

        String dismissText = res.getString(R.string.action_dismiss);
        Intent dismissIntent = new Intent(context, NotificationReceiver.class);
        dismissIntent.setAction(NotificationReceiver.ACTION_DISMISS);
        PendingIntent dismissPendingIntent = PendingIntent.getBroadcast(context, 1, dismissIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        mBuilder.addAction(R.drawable.ic_alarm_off_white, dismissText, dismissPendingIntent);

        String snoozeText = res.getString(R.string.action_snooze);
        Intent snoozeIntent = new Intent(context, NotificationReceiver.class);
        snoozeIntent.setAction(NotificationReceiver.ACTION_SNOOZE);
        PendingIntent snoozePendingIntent = PendingIntent.getBroadcast(context, 1, snoozeIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        mBuilder.addAction(R.drawable.ic_snooze_white, snoozeText, snoozePendingIntent);

        showNotification(mBuilder);
    }

    public void onDismiss() {
        Log.d(TAG, "onDismiss()");

        hideNotification();
    }

    public void onSnooze(Calendar ringAfterSnoozeTime) {
        Log.d(TAG, "onSnooze()");

        NotificationCompat.Builder mBuilder = buildNotification();

        Resources res = context.getResources();
        Clock clock = new SystemClock(); // TODO Solve dependency on clock
        String ringAfterSnoozeTimeText = Localization.timeToString(ringAfterSnoozeTime.get(Calendar.HOUR_OF_DAY), ringAfterSnoozeTime.get(Calendar.MINUTE), context, clock);
        String contentText = res.getString(R.string.notification_text_snoozed, ringAfterSnoozeTimeText);
        mBuilder.setContentText(contentText);

        Intent intent = new Intent(context, AlarmMorningActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 1, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        mBuilder.setContentIntent(pendingIntent);

        String dismissText = res.getString(R.string.action_dismiss);
        Intent dismissIntent = new Intent(context, NotificationReceiver.class);
        dismissIntent.setAction(NotificationReceiver.ACTION_DISMISS);
        PendingIntent dismissPendingIntent = PendingIntent.getBroadcast(context, 1, dismissIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        mBuilder.addAction(R.drawable.ic_alarm_off_white, dismissText, dismissPendingIntent);

        showNotification(mBuilder);
    }

    public void onAlarmCancel() {
        Log.d(TAG, "onAlarmCancel()");

        hideNotification();
    }

    public void notifyCancelledAlarm() {
        Log.d(TAG, "notifyCancelledAlarm()");

        NotificationCompat.Builder mBuilder = buildNotification();

        GlobalManager globalManager = new GlobalManager(context);
        Calendar alarmTime = globalManager.getAlarmTimeOfRingingAlarm();

        Resources res = context.getResources();
        Clock clock = new SystemClock(); // TODO Solve dependency on clock
        String timeText = Localization.timeToString(alarmTime.get(Calendar.HOUR_OF_DAY), alarmTime.get(Calendar.MINUTE), context, clock);
        String dateText = Localization.dateToStringVeryShort(alarmTime.getTime());
        String contentTitle = res.getString(R.string.notification_title_long, timeText, dateText);
        mBuilder.setContentTitle(contentTitle);

        String contentText = res.getString(R.string.notification_text_cancelled);
        mBuilder.setContentText(contentText);

        Intent intent = new Intent(context, AlarmMorningActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 1, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        mBuilder.setContentIntent(pendingIntent);

        NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.notify(++NOTIFICATION_ERROR_ID, mBuilder.build());
    }

    public void notifySkippedAlarms() {
        Log.d(TAG, "notifySkippedAlarms()");

        Resources res = context.getResources();
        String contentTitle = res.getString(R.string.app_name);
        String contentText = res.getString(R.string.notification_text_skipped);

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context)
                .setSmallIcon(R.drawable.ic_alarm_white)
                .setContentTitle(contentTitle)
                .setContentText(contentText);

        Intent intent = new Intent(context, AlarmMorningActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 1, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        mBuilder.setContentIntent(pendingIntent);

        NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.notify(++NOTIFICATION_ERROR_ID, mBuilder.build());
    }

}
