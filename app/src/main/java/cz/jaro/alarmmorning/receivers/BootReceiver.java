package cz.jaro.alarmmorning.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import cz.jaro.alarmmorning.SystemAlarm;
import cz.jaro.alarmmorning.WakeLocker;

/**
 * Created by jmalenko on 21.12.2015.
 */
public class BootReceiver extends BroadcastReceiver {

    private static final String TAG = BroadcastReceiver.class.getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {
        WakeLocker.acquire(context);

        Log.d(TAG, "onReceive()");
        Log.i(TAG, "Setting alarm on boot");

        SystemAlarm systemAlarm = SystemAlarm.getInstance(context);
        systemAlarm.setAlarm();

        WakeLocker.release();
    }

}