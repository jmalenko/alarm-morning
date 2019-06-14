package cz.jaro.alarmmorning.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import cz.jaro.alarmmorning.Analytics;
import cz.jaro.alarmmorning.GlobalManager;
import cz.jaro.alarmmorning.WakeLocker;
import cz.jaro.alarmmorning.checkalarmtime.CheckAlarmTime;
import cz.jaro.alarmmorning.nighttimebell.NighttimeBell;

/**
 * This receiver is called by the operating system on the system start (boot).
 */
public class BootReceiver extends BroadcastReceiver {

    private static final String TAG = BroadcastReceiver.class.getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {
        WakeLocker.acquire(context);

        if (intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)) {
            Log.v(TAG, "onReceive()");

            new Analytics(context, Analytics.Event.Start, Analytics.Channel.External, Analytics.ChannelName.Boot).setConfigurationInfo().save();

            Log.i(TAG, "Setting alarm on boot");
            GlobalManager globalManager = GlobalManager.getInstance();
            globalManager.forceSetAlarm();

            Log.i(TAG, "Starting CheckAlarmTime on boot");
            CheckAlarmTime checkAlarmTime = CheckAlarmTime.getInstance(context);
            checkAlarmTime.checkAndRegister();

            Log.i(TAG, "Starting NighttimeBell on boot");
            NighttimeBell nighttimeBell = NighttimeBell.getInstance(context);
            nighttimeBell.checkAndRegister();
        }

        WakeLocker.release();
    }

}