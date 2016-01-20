package cz.jaro.alarmmorning.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import cz.jaro.alarmmorning.SystemAlarm;
import cz.jaro.alarmmorning.WakeLocker;

/**
 * This receiver is called by the operating system on the system alarm.
 */
public class AlarmReceiver extends BroadcastReceiver {

    private static final String TAG = AlarmReceiver.class.getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {
        // Prevent device sleep
        WakeLocker.acquire(context);

        Log.d(TAG, "onReceive()");

        SystemAlarm systemAlarm = SystemAlarm.getInstance(context);
        systemAlarm.onSystemAlarm(context, intent);
    }
}
