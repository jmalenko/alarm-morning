package cz.jaro.alarmmorning.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import cz.jaro.alarmmorning.GlobalManager;

/**
 * This receiver does nothing.
 */
public class VoidReceiver extends BroadcastReceiver {

    private static final String TAG = GlobalManager.createLogTag(VoidReceiver.class);

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.e(TAG, "onReceive()");
        // nothing
    }

}
