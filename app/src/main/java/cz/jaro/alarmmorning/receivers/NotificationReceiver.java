package cz.jaro.alarmmorning.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import cz.jaro.alarmmorning.GlobalManager;

/**
 * This receiver handles the actions with the notification.
 */
public class NotificationReceiver extends BroadcastReceiver {

    private static final String TAG = NotificationReceiver.class.getSimpleName();

    public static final String ACTION_DELETE_NOTIFICATION = "cz.jaro.alarmmorning.intent.action.DELETE_NOTIFICATION";
    public static final String ACTION_DISMISS_BEFORE_RINGING = "cz.jaro.alarmmorning.intent.action.DISMISS_ALARM_BEFORE_RINGING";
    public static final String ACTION_DISMISS = "cz.jaro.alarmmorning.intent.action.DISMISS_ALARM";
    public static final String ACTION_SNOOZE = "cz.jaro.alarmmorning.intent.action.SNOOZE_ALARM";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        Log.d(TAG, "onReceive() action=" + action);

        if (action == ACTION_DELETE_NOTIFICATION) {
            deleteNotification(context);
        }
        if (action == ACTION_DISMISS_BEFORE_RINGING) {
            GlobalManager globalManager = new GlobalManager(context);
            globalManager.onDismissBeforeRinging();
        } else if (action == ACTION_DISMISS) {
            GlobalManager globalManager = new GlobalManager(context);
            globalManager.onDismiss();
        } else if (action == ACTION_SNOOZE) {
            GlobalManager globalManager = new GlobalManager(context);
            globalManager.onSnooze();
        }
    }

    private void deleteNotification(Context context) {
        Log.d(TAG, "deleteNotification()");
        Log.i(TAG, "Delete notification");
    }

}
