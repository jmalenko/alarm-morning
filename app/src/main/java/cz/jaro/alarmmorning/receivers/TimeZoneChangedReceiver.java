package cz.jaro.alarmmorning.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import cz.jaro.alarmmorning.SystemAlarm;
import cz.jaro.alarmmorning.WakeLocker;

/**
 * Created by jmalenko on 11.1.2016.
 */
public class TimeZoneChangedReceiver extends BroadcastReceiver {

    private static final String TAG = TimeChangedReceiver.class.getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {
        WakeLocker.acquire(context);

        Log.d(TAG, "onReceive()");
        Log.i(TAG, "Setting alarm on time zone change");

        SystemAlarm systemAlarm = SystemAlarm.getInstance(context);
        systemAlarm.setAlarm();

        WakeLocker.release();
    }

}
