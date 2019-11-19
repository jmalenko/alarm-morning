package cz.jaro.alarmmorning.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import cz.jaro.alarmmorning.MyLog;

/**
 * This receiver does nothing.
 */
public class VoidReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        MyLog.e("onReceive()");
        // nothing
    }

}
