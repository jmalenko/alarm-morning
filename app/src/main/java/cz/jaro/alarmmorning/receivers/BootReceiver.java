package cz.jaro.alarmmorning.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import cz.jaro.alarmmorning.Analytics;
import cz.jaro.alarmmorning.GlobalManager;
import cz.jaro.alarmmorning.MyLog;
import cz.jaro.alarmmorning.WakeLocker;
import cz.jaro.alarmmorning.checkalarmtime.CheckAlarmTime;
import cz.jaro.alarmmorning.nighttimebell.NighttimeBell;

/**
 * This receiver is called by the operating system on the system start (boot).
 */
public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        WakeLocker.acquire(context);

        String action = intent.getAction();
        MyLog.v("onReceive(action=" + action + ")");

        if (action.equals(Intent.ACTION_BOOT_COMPLETED)) {
            MyLog.i("Starting after boot");

            new Analytics(context, Analytics.Event.Start, Analytics.Channel.External, Analytics.ChannelName.Boot).setConfigurationInfo().save();

            MyLog.i("Setting alarm on boot");
            GlobalManager globalManager = GlobalManager.getInstance();
            globalManager.firstSetAlarm();

            MyLog.i("Starting CheckAlarmTime on boot");
            CheckAlarmTime checkAlarmTime = CheckAlarmTime.getInstance(context);
            checkAlarmTime.checkAndRegister();

            MyLog.i("Starting NighttimeBell on boot");
            NighttimeBell nighttimeBell = NighttimeBell.getInstance(context);
            nighttimeBell.checkAndRegister();
        }

        WakeLocker.release();
    }

}