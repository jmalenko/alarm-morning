package cz.jaro.alarmmorning;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import java.util.Calendar;

import cz.jaro.alarmmorning.model.Day;
import cz.jaro.alarmmorning.receivers.NotificationReceiver;

/**
 * Created by ext93831 on 18.1.2016.
 */
public class SystemNotification {

    private static final String TAG = SystemNotification.class.getSimpleName();

    private static final int NOTIFICATION_ID = 0;

    private static NotificationCompat.Builder buildNotification(Context context) {
        GlobalManager globalManager = new GlobalManager(context);
        Day day = globalManager.getDay();

        Resources res = context.getResources();
        String timeText = Localization.timeToString(day.getHourX(), day.getMinuteX(), context);
        String contentTitle = String.format(res.getString(R.string.notification_title), timeText);

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context)
                .setSmallIcon(R.drawable.ic_alarm_on_white)
                .setContentTitle(contentTitle);

        return mBuilder;
    }

    private static void showNotification(Context context, NotificationCompat.Builder mBuilder) {
        Log.d(TAG, "showNotification()");
        NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.notify(NOTIFICATION_ID, mBuilder.build());
    }

    private static void hideNotification(Context context) {
        Log.d(TAG, "hideNotification()");
        NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.cancel(NOTIFICATION_ID);
    }

    protected static void onNearFuture(Context context) {
        Log.d(TAG, "onNearFuture()");

        NotificationCompat.Builder mBuilder = buildNotification(context);

        Resources res = context.getResources();
        String contentText = res.getString(R.string.notification_text_future);
        mBuilder.setContentText(contentText);

        Intent intent = new Intent(context, AlarmMorningActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 1, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        mBuilder.setContentIntent(pendingIntent);

        String dismissText = res.getString(R.string.action_dismiss);
        Intent dismissIntent = new Intent(context, NotificationReceiver.class);
        dismissIntent.setAction(NotificationReceiver.ACTION_DISMISS);
        PendingIntent dismissPendingIntent = PendingIntent.getBroadcast(context, 1, dismissIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        mBuilder.addAction(R.drawable.ic_alarm_off_white, dismissText, dismissPendingIntent);

        showNotification(context, mBuilder);
    }

    public static void onDismissBeforeRinging(Context context) {
        Log.d(TAG, "onDismissBeforeRinging()");

        hideNotification(context);
    }

    protected static void onRing(Context context) {
        Log.d(TAG, "onRing()");

        NotificationCompat.Builder mBuilder = buildNotification(context);

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

        showNotification(context, mBuilder);
    }

    protected static void onSnooze(Context context, Calendar ringAfterSnoozeTime) {
        Log.d(TAG, "onSnooze()");

        NotificationCompat.Builder mBuilder = buildNotification(context);

        Resources res = context.getResources();
        String ringAfterSnoozeTimeText = Localization.timeToString(ringAfterSnoozeTime.get(Calendar.HOUR_OF_DAY), ringAfterSnoozeTime.get(Calendar.MINUTE), context);
        String contentText = String.format(res.getString(R.string.notification_text_snoozed), ringAfterSnoozeTimeText);
        mBuilder.setContentText(contentText);

        Intent intent = new Intent(context, AlarmMorningActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 1, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        mBuilder.setContentIntent(pendingIntent);

        String dismissText = res.getString(R.string.action_dismiss);
        Intent dismissIntent = new Intent(context, NotificationReceiver.class);
        dismissIntent.setAction(NotificationReceiver.ACTION_DISMISS);
        PendingIntent dismissPendingIntent = PendingIntent.getBroadcast(context, 1, dismissIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        mBuilder.addAction(R.drawable.ic_alarm_off_white, dismissText, dismissPendingIntent);

        showNotification(context, mBuilder);
    }


    public static void onDismiss(Context context) {
        Log.d(TAG, "onDismiss()");

        hideNotification(context);
    }

}
