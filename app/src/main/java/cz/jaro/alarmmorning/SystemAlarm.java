package cz.jaro.alarmmorning;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.util.Calendar;
import java.util.GregorianCalendar;

import cz.jaro.alarmmorning.model.AlarmDataSource;
import cz.jaro.alarmmorning.receivers.AlarmReceiver;

/**
 * Created by jmalenko on 22.12.2015.
 */
public class SystemAlarm {

    private static final String TAG = SystemAlarm.class.getName();

    private static SystemAlarm instance;

    private Context context;
    private AlarmManager alarmManager;

    private Long time;
    private PendingIntent operation;

    private SystemAlarm(Context context) {
        this.context = context;
        alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
    }

    public static SystemAlarm getInstance(Context context) {
        if (instance == null) {
            instance = new SystemAlarm(context);
        }
        return instance;
    }


    public void setAlarm() {
        AlarmDataSource datasource = new AlarmDataSource(context);
        datasource.open();

        Calendar now = new GregorianCalendar();
        Calendar alarmTime = datasource.getNextAlarm(now);

        datasource.close();

        if (alarmTime == null) {
            if (operation != null) {
                Log.i(TAG, "Cancelling previous alarm");
                operation.cancel();
            }
            Log.i(TAG, "No alarm scheduled");
            return;
        }

        if (operation != null) {
            if (time == alarmTime.getTimeInMillis()) {
                Log.i(TAG, "Scheduled alarm does not change at " + alarmTime.getTime().toString());
                return;
            } else {
                Log.i(TAG, "Cancelling previous alarm");
                operation.cancel();
            }
        }

        Log.i(TAG, "Setting alarm at " + alarmTime.getTime().toString());

        Intent ringIntent = new Intent(context, AlarmReceiver.class);

        time = alarmTime.getTimeInMillis();
        operation = PendingIntent.getBroadcast(context, 1, ringIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        alarmManager.set(AlarmManager.RTC_WAKEUP, time, operation);

        // requires API level 21 or higher
//        PendingIntent showIntent = PendingIntent.getBroadcast(context, 1, ringIntent, PendingIntent.FLAG_UPDATE_CURRENT);
//        AlarmManager.AlarmClockInfo alarmClockInfo = new AlarmManager.AlarmClockInfo(alarmTime.getTimeInMillis(), showIntent);
//        alarmManager.setAlarmClock(alarmClockInfo, operation);
    }

    public long getTime() {
        return time;
    }

    //    // requires API level 21 or hi
//    public static long getNextGlobalAlarm(Context context) {
//        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
//        AlarmManager.AlarmClockInfo alarm = alarmManager.getNextAlarmClock();
//        long time = alarm.getTriggerTime();
//        return time;
//    }

}
