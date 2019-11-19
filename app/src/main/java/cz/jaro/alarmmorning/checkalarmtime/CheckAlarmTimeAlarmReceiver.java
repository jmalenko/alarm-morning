package cz.jaro.alarmmorning.checkalarmtime;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import cz.jaro.alarmmorning.MyLog;
import cz.jaro.alarmmorning.WakeLocker;

/**
 * This receiver is called when the alarm time should be checked.
 */
public class CheckAlarmTimeAlarmReceiver extends BroadcastReceiver {

    /**
     * Delegate to CheckAlarmTime class.
     *
     * @param context Context
     * @param intent  Intent
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        // Prevent device sleep
        WakeLocker.acquire(context);

        MyLog.v("onReceive()");

        CheckAlarmTime checkAlarmTime = CheckAlarmTime.getInstance(context);
        checkAlarmTime.onReceive(intent);

        WakeLocker.release();
    }
}
