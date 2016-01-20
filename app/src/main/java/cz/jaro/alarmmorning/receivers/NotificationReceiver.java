package cz.jaro.alarmmorning.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import cz.jaro.alarmmorning.GlobalManager;

/**
 * Handles the actions in notification.
 */
public class NotificationReceiver extends BroadcastReceiver {

    private static final String TAG = NotificationReceiver.class.getSimpleName();

    public static final String ACTION_DISMISS = "cz.jaro.alarmmorning.intent.action.DISMISS_ALARM";
    public static final String ACTION_SNOOZE = "cz.jaro.alarmmorning.intent.action.SNOOZE_ALARM";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        Log.d(TAG, "onReceive() action=" + action);

        if (action == ACTION_DISMISS) {
            dissmissFromNotification(context);
        } else if (action == ACTION_SNOOZE) {
            snoozeFromNotification(context);
        }

    }

    private void dissmissFromNotification(Context context) {
        Log.d(TAG, "dissmissFromNotification()");
        Log.i(TAG, "Dismiss");

        GlobalManager globalManager = new GlobalManager(context);
        if (globalManager.isValid()) {
            int state = globalManager.getState();

            if (state == GlobalManager.STATE_FUTURE) {
                globalManager.setState(GlobalManager.STATE_DISMISSED_BEFORE_RINGING);
                globalManager.onDismissBeforeRinging();
            } else if (state == GlobalManager.STATE_RINGING || state == GlobalManager.STATE_SNOOZED) {
                globalManager.setState(GlobalManager.STATE_DISMISSED);
                globalManager.onDismiss();
            } else {
                throw new IllegalArgumentException();
            }
        } else {
            throw new IllegalArgumentException();
        }
    }

    private void snoozeFromNotification(Context context) {
        Log.d(TAG, "snoozeFromNotification()");
        Log.i(TAG, "Snooze");

        GlobalManager globalManager = new GlobalManager(context);
        globalManager.onSnooze();
    }
}
