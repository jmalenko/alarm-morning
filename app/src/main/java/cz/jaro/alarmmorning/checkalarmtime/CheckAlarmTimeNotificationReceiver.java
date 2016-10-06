package cz.jaro.alarmmorning.checkalarmtime;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.util.Calendar;

import static cz.jaro.alarmmorning.model.Day.VALUE_UNSET;

/**
 * This receiver handles the actions with the notification for "check alarm time" function.
 * <p>
 * The "new alarm time" parameter is passed in the {@link #EXTRA_NEW_ALARM_TIME} extra.
 */
public class CheckAlarmTimeNotificationReceiver extends BroadcastReceiver {

    private static final String TAG = CheckAlarmTimeNotificationReceiver.class.getSimpleName();

    public static final String ACTION_CHECK_ALARM_TIME_SET_TO = "cz.jaro.alarmmorning.intent.action.SET_TO";
    public static final String ACTION_CHECK_ALARM_TIME_SET_DIALOG = "cz.jaro.alarmmorning.intent.action.SET_DIALOG";

    public static final String EXTRA_NEW_ALARM_TIME = "new_alarm_time";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        Log.v(TAG, "onReceive() action=" + action);

        // Read parameters from extra
        long newAlarmTimeLong = intent.getLongExtra(EXTRA_NEW_ALARM_TIME, VALUE_UNSET);
        if (newAlarmTimeLong == VALUE_UNSET) {
            throw new IllegalArgumentException("The " + EXTRA_NEW_ALARM_TIME + " extra must be set");
        }

        if (action == ACTION_CHECK_ALARM_TIME_SET_TO) {
            Calendar newAlarmTime = Calendar.getInstance();
            newAlarmTime.setTimeInMillis(newAlarmTimeLong);

            Log.i(TAG, "Set alarm time to " + newAlarmTime.getTime());

            SetTimeActivity.save(context, newAlarmTime);
        } else if (action == ACTION_CHECK_ALARM_TIME_SET_DIALOG) {
            Log.i(TAG, "Show dialog to set alarm time");

            // Start activity
            Intent dialogIntent = new Intent(context, SetTimeActivity.class);
            dialogIntent.putExtra(CheckAlarmTimeNotificationReceiver.EXTRA_NEW_ALARM_TIME, newAlarmTimeLong);
            dialogIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            dialogIntent.addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
            dialogIntent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
            dialogIntent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
            context.startActivity(dialogIntent);

            // Collapse Android notification tray
            Intent it = new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
            context.sendBroadcast(it);
        } else {
            throw new IllegalArgumentException("Unexpected argument " + action);
        }

        // Hide notification
        CheckAlarmTime checkAlarmTime = CheckAlarmTime.getInstance(context);
        checkAlarmTime.hideNotification();
    }
}