package cz.jaro.alarmmorning.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import cz.jaro.alarmmorning.SystemAlarm;
import cz.jaro.alarmmorning.WakeLocker;

/**
 * This receiver os called by the operating system when the alarm should fire.
 */
public class AlarmReceiver extends BroadcastReceiver {

    private static final String TAG = AlarmReceiver.class.getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {
        // Prevent device sleep
        WakeLocker.acquire(context);

        Log.d(TAG, "onReceive()");

        // Register next alarm
        Log.i(TAG, "Setting next alarm");
        SystemAlarm systemAlarm = SystemAlarm.getInstance(context);
        systemAlarm.setAlarm();

        // Display this alarm
        Log.i(TAG, "Showing ring activity");
        Intent ringIntent = new Intent();
        ringIntent.setClassName("cz.jaro.alarmmorning", "cz.jaro.alarmmorning.RingActivity");
        ringIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        context.startActivity(ringIntent);
    }

}
