package cz.jaro.alarmmorning.checkalarmtime;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import cz.jaro.alarmmorning.WakeLocker;

/**
 * This receiver is called when the alarm time should be checked.
 */
public class CheckAlarmTimeAlarmReceiver extends BroadcastReceiver {

    private static final String TAG = CheckAlarmTimeAlarmReceiver.class.getSimpleName();

    /**
     * Delegate to CheckAlarmTime class.
     *
     * @param context
     * @param intent
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        // Prevent device sleep
        WakeLocker.acquire(context);

        Log.v(TAG, "onReceive()");

        CheckAlarmTime checkAlarmTime = CheckAlarmTime.getInstance(context);
        checkAlarmTime.onReceive(intent);

        WakeLocker.release();
    }
}
