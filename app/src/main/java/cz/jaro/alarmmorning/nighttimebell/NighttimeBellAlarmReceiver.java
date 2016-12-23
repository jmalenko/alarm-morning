package cz.jaro.alarmmorning.nighttimebell;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import cz.jaro.alarmmorning.WakeLocker;

/**
 * This receiver is called when the night time bell should be played.
 */
public class NighttimeBellAlarmReceiver extends BroadcastReceiver {

    private static final String TAG = NighttimeBellAlarmReceiver.class.getSimpleName();

    /**
     * Delegate to NighttimeBell class.
     *
     * @param context Context
     * @param intent  Intent
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        // Prevent device sleep
        WakeLocker.acquire(context);

        Log.v(TAG, "onReceive()");

        NighttimeBell nighttimeBell = NighttimeBell.getInstance(context);
        nighttimeBell.onReceive(context, intent);

        WakeLocker.release();
    }
}
