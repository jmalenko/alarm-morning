package cz.jaro.alarmmorning.checkalarmtime;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import cz.jaro.alarmmorning.GlobalManager;

/**
 * This receiver is called when a calendar event is created, modified or deleted.
 */
public class CalendarEventChangeReceiver extends BroadcastReceiver {

    private static final String TAG = GlobalManager.createLogTag(CalendarEventChangeReceiver.class);

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.v(TAG, "onReceive()");

        CheckAlarmTime checkAlarmTime = CheckAlarmTime.getInstance(context);
        checkAlarmTime.onReceive(intent);
    }
}
