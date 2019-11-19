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
 * This receiver is called by the operating system on time change (time was explicitly set), date change or time zone change.
 */
public class TimeChangedReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        WakeLocker.acquire(context);

        MyLog.v("onReceive(action=" + intent.getAction() + ")");
        MyLog.i("Starting after time changed");

        new Analytics(context, Analytics.Event.Start, Analytics.Channel.External, Analytics.ChannelName.TimeChange).setConfigurationInfo().save();

        MyLog.i("Setting alarm on time change");
        GlobalManager globalManager = GlobalManager.getInstance();
        globalManager.firstSetAlarm();

        MyLog.i("Setting CheckAlarmTime on time change");
        CheckAlarmTime checkAlarmTime = CheckAlarmTime.getInstance(context);
        checkAlarmTime.checkAndRegister();

        MyLog.i("Setting NighttimeBell on time change");
        NighttimeBell nighttimeBell = NighttimeBell.getInstance(context);
        nighttimeBell.checkAndRegister();

        WakeLocker.release();
    }

}
