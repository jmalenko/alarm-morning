package cz.jaro.alarmmorning.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import cz.jaro.alarmmorning.SystemAlarm;
import cz.jaro.alarmmorning.WakeLocker;

/**
 * Hanfdles situations when time changes (time was explicitly set) or time zone changes.
 */
public class TimeChangedReceiver extends BroadcastReceiver {

    private static final String TAG = TimeChangedReceiver.class.getName();

    @Override
    public void onReceive(Context context, Intent intent) {
        WakeLocker.acquire(context);

        Log.d(TAG, "onReceive()");
        Log.i(TAG, "Setting alarm on time change");

        SystemAlarm systemAlarm = SystemAlarm.getInstance(context);
        systemAlarm.setAlarm();

        WakeLocker.release();
    }

}
