package cz.jaro.alarmmorning.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import cz.jaro.alarmmorning.GlobalManager;
import cz.jaro.alarmmorning.WakeLocker;

/**
 * This receiver is called by the operating system on time zone change.
 */
public class TimeZoneChangedReceiver extends BroadcastReceiver {

    private static final String TAG = TimeChangedReceiver.class.getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {
        WakeLocker.acquire(context);

        Log.v(TAG, "onReceive()");
        Log.i(TAG, "Setting alarm on time zone change");

        GlobalManager globalManager = new GlobalManager(context);
        globalManager.forceSetAlarm();

        WakeLocker.release();
    }

}
