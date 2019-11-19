package cz.jaro.alarmmorning.nighttimebell;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import cz.jaro.alarmmorning.MyLog;
import cz.jaro.alarmmorning.WakeLocker;

/**
 * This receiver is called when the night time bell should be played.
 */
public class NighttimeBellAlarmReceiver extends BroadcastReceiver {

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

        MyLog.v("onReceive()");

        NighttimeBell nighttimeBell = NighttimeBell.getInstance(context);
        nighttimeBell.onReceive(context, intent);

        WakeLocker.release();
    }
}
