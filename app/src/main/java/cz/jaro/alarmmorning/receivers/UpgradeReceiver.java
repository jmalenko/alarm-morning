package cz.jaro.alarmmorning.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import cz.jaro.alarmmorning.GlobalManager;
import cz.jaro.alarmmorning.WakeLocker;

/**
 * This receiver is called by the operating system on the app upgrade.
 */
public class UpgradeReceiver extends BroadcastReceiver {

    private static final String TAG = UpgradeReceiver.class.getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {
        WakeLocker.acquire(context);

        Log.v(TAG, "onReceive()");
        Log.i(TAG, "Setting alarm on update");

        GlobalManager globalManager = new GlobalManager(context);
        globalManager.forceSetAlarm();

        WakeLocker.release();
    }

}