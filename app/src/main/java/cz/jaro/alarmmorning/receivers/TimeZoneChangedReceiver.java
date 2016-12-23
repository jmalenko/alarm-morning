package cz.jaro.alarmmorning.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import cz.jaro.alarmmorning.Analytics;
import cz.jaro.alarmmorning.GlobalManager;
import cz.jaro.alarmmorning.WakeLocker;

/**
 * This receiver is called by the operating system on time zone change.
 */
public class TimeZoneChangedReceiver extends BroadcastReceiver {

    private static final String TAG = TimeChangedReceiver.class.getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {
        WakeLocker.acquire(context);

        Log.v(TAG, "onReceive()");

        new Analytics(context, Analytics.Event.Start, Analytics.Channel.External, Analytics.ChannelName.TimeZoneChange).setConfigurationInfo().save();

        Log.i(TAG, "Setting alarm on time zone change");
        GlobalManager globalManager = GlobalManager.getInstance();
        globalManager.forceSetAlarm();

        WakeLocker.release();
    }

}
