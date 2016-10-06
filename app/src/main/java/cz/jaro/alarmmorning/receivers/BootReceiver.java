package cz.jaro.alarmmorning.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import cz.jaro.alarmmorning.GlobalManager;
import cz.jaro.alarmmorning.WakeLocker;
import cz.jaro.alarmmorning.checkalarmtime.CheckAlarmTime;

/**
 * This receiver is called by the operating system on the system start (boot).
 */
public class BootReceiver extends BroadcastReceiver {

    private static final String TAG = BroadcastReceiver.class.getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {
        WakeLocker.acquire(context);

        Log.v(TAG, "onReceive()");

        Log.i(TAG, "Setting alarm on boot");
        GlobalManager globalManager = new GlobalManager(context);
        globalManager.forceSetAlarm();

        Log.i(TAG, "Starting CheckAlarmTime on boot");
        CheckAlarmTime checkAlarmTime = CheckAlarmTime.getInstance(context);
        checkAlarmTime.checkAndRegisterCheckAlarmTime();

        WakeLocker.release();
    }

}