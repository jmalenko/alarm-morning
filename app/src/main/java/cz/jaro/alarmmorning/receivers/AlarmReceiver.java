package cz.jaro.alarmmorning.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import cz.jaro.alarmmorning.SystemAlarm;

/**
 * This receiver os called by the operating system when the alarm should fire.
 */
public class AlarmReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        // Register next alarm
        SystemAlarm systemAlarm = SystemAlarm.getInstance(context);
        systemAlarm.setAlarm();

        // Display this alarm
        Intent ringIntent = new Intent();
        ringIntent.setClassName("cz.jaro.alarmmorning", "cz.jaro.alarmmorning.RingActivity");
        ringIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        context.startActivity(ringIntent);
    }

}
