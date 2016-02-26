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

    private SystemNotification(Context context) {
        this.context = context;
    }

    public static SystemNotification getInstance(Context context) {
        if (instance == null) {
            instance = new SystemNotification(context);
        }
        return instance;
    }

    private NotificationCompat.Builder buildNotification(Context context) {
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

    private void showNotification(Context context, NotificationCompat.Builder mBuilder) {
        Log.d(TAG, "showNotification()");
        NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.notify(NOTIFICATION_ID, mBuilder.build());
    }

    private void hideNotification(Context context) {
        Log.d(TAG, "hideNotification()");
        NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.cancel(NOTIFICATION_ID);
    }

    /*
     * Events
     * ======
     */

    public void onAlarmSet(Context context) {
        Log.d(TAG, "onAlarmSet()");

        hideNotification(context);
    }

    protected void onNearFuture(Context context) {
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
        dismissIntent.setAction(NotificationReceiver.ACTION_DISMISS_BEFORE_RINGING);
        PendingIntent dismissPendingIntent = PendingIntent.getBroadcast(context, 1, dismissIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        mBuilder.addAction(R.drawable.ic_alarm_off_white, dismissText, dismissPendingIntent);

        showNotification(context, mBuilder);
    }

    public void onDismissBeforeRinging(Context context) {
        Log.d(TAG, "onDismissBeforeRinging()");

        hideNotification(context);
    }

    protected void onRing(Context context) {
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

    public void onDismiss(Context context) {
        Log.d(TAG, "onDismiss()");

        hideNotification(context);
    }

    public void onSnooze(Context context, Calendar ringAfterSnoozeTime) {
        Log.d(TAG, "onSnooze()");

        NotificationCompat.Builder mBuilder = buildNotification(context);

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

        showNotification(context, mBuilder);
    }

    public void onAlarmCancel(Context context) {
        Log.d(TAG, "onAlarmCancel()");

        hideNotification(context);
    }

}
