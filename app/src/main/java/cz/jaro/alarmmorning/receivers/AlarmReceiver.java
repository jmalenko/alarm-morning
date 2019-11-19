package cz.jaro.alarmmorning.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import cz.jaro.alarmmorning.MyLog;
import cz.jaro.alarmmorning.RingActivity;
import cz.jaro.alarmmorning.SystemAlarm;
import cz.jaro.alarmmorning.WakeLocker;

/**
 * This receiver is called on the system alarm.
 */
public class AlarmReceiver extends BroadcastReceiver {

    /**
     * The wake lock must be received when the used ends the RingActivity. That is done in {@link RingActivity#stopAll()}
     *
     * @param context Context
     * @param intent  Intent
     */
    @SuppressWarnings("JavadocReference")
    @Override
    public void onReceive(Context context, Intent intent) {
        // Prevent device sleep
        WakeLocker.acquire(context);

        MyLog.v("onReceive()");

        SystemAlarm systemAlarm = SystemAlarm.getInstance(context);
        systemAlarm.onSystemAlarm(intent);

        if (!intent.getAction().equals(SystemAlarm.ACTION_RING)) {
            WakeLocker.release();
        }
    }
}
