package cz.jaro.alarmmorning;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.util.Calendar;

import cz.jaro.alarmmorning.model.AlarmDataSource;
import cz.jaro.alarmmorning.receivers.AlarmReceiver;

/**
 * Created by jmalenko on 22.12.2015.
 */
public class SystemAlarm {

    private static final String TAG = SystemAlarm.class.getSimpleName();

    private static SystemAlarm instance;

    private Context context;
    private AlarmManager alarmManager;

    private Long time;
    private Intent intent;
    private PendingIntent operation;

    /**
     * Action meaning: Set system alarm for next action. (Currently, all alarm are unset in the next {@link AlarmDataSource#HORIZON_DAYS} days.)
     */
    static private String ACTION_SET_SYSTEM_ALARM = "SET_SYSTEM_ALARM";

    /**
     * Action meaning: Shout notification about the next alarm. (The next alarm time is in 2 hours.)
     */
    static private String ACTION_RING_IN_NEAR_FUTURE = "RING_IN_NEAR_FUTURE";

    /**
     * Action meaning: Start ringing.
     */
    static private String ACTION_RING = "RING";

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


    protected void registerSystemAlarm(String action, Calendar time) {
        Log.i(TAG, "Setting system alarm at " + time.getTime().toString());

        this.time = time.getTimeInMillis();

        intent = new Intent(context, AlarmReceiver.class);
        intent.setAction(action);

        operation = PendingIntent.getBroadcast(context, 1, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        alarmManager.set(AlarmManager.RTC_WAKEUP, this.time, operation);
    }

    private void cancelSystemAlarm() {
        if (operation != null) {
            Log.d(TAG, "Cancelling current alarm");
            operation.cancel();
        }
    }

    private void initialize() {
        Log.d(TAG, "initialize()");

        Calendar alarmTime = AlarmDataSource.getNextAlarm(context);

        if (alarmTime == null) {
            // TODO Fix: when no alarm is set [in the horizon]. This happens when a default time is set and all the days in calendar are changed to unset. This must me fixed everywhere AlarmDataSource.getNextAlarm() is called.
            registerSystemAlarm(ACTION_SET_SYSTEM_ALARM, null);
        } else {
            Calendar now = Calendar.getInstance();

            // TODO Create preference "Near future is X;Y before alarm time"
            final int NEAR_FUTURE_HOUR = 0;
            final int NEAR_FUTURE_MINUTE = 1;

            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
            int nearFutureMinutes = preferences.getInt(SettingsFragment.PREF_NEAR_FUTURE_TIME, SettingsFragment.PREF_NEAR_FUTURE_TIME_DEFAULT);

            Calendar nearFutureTime = subtractHour(alarmTime, nearFutureMinutes);

            if (now.before(nearFutureTime)) {
                registerSystemAlarm(ACTION_RING_IN_NEAR_FUTURE, nearFutureTime);
            } else {
                alarmTime = AlarmDataSource.getNextAlarm(context);

                registerSystemAlarm(ACTION_RING, alarmTime);

                GlobalManager globalManager = new GlobalManager(context);
                globalManager.onNearFuture();
            }
        }
    }

    public void setSystemAlarm() {
        Log.d(TAG, "setSystemAlarm()");

        cancelSystemAlarm();
        initialize();
    }

    private static Calendar subtractHour(Calendar time, int minute) {
        Calendar date = (Calendar) time.clone();
        date.add(Calendar.MINUTE, -minute);
        return date;
    }


//    public void setAlarmOld() {
//        Log.d(TAG, "setAlarmOld()");
//
//        Calendar alarmTime = AlarmDataSource.getNextAlarm(context);
//
//        if (alarmTime == null) {
//            if (operation != null) {
//                Log.i(TAG, "Cancelling current alarm");
//                operation.cancel();
//            }
//            Log.i(TAG, "No alarm scheduled");
//            return;
//        }
//
//        if (operation != null) {
//            if (time == alarmTime.getTimeInMillis()) {
//                Log.i(TAG, "Scheduled alarm does not change at " + alarmTime.getTime().toString());
//                return;
//            } else {
//                Log.i(TAG, "Cancelling current alarm");
//                operation.cancel();
//            }
//        }
//
//        Log.i(TAG, "Setting alarm at " + alarmTime.getTime().toString());
//
//        Intent ringIntent = new Intent(context, AlarmReceiver.class);
//
//        time = alarmTime.getTimeInMillis();
//        operation = PendingIntent.getBroadcast(context, 1, ringIntent, PendingIntent.FLAG_UPDATE_CURRENT);
//        alarmManager.set(AlarmManager.RTC_WAKEUP, time, operation);
//
//        // requires API level 21 or higher
////        PendingIntent showIntent = PendingIntent.getBroadcast(context, 1, ringIntent, PendingIntent.FLAG_UPDATE_CURRENT);
////        AlarmManager.AlarmClockInfo alarmClockInfo = new AlarmManager.AlarmClockInfo(alarmTime.getTimeInMillis(), showIntent);
////        alarmManager.setAlarmClock(alarmClockInfo, operation);
//    }

//    public long getTime() {
//        return time;
//    }

    //    // requires API level 21 or hi
//    public static long getNextGlobalAlarm(Context context) {
//        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
//        AlarmManager.AlarmClockInfo alarm = alarmManager.getNextAlarmClock();
//        long time = alarm.getTriggerTime();
//        return time;
//    }

    public void onSystemAlarm(Context context, Intent intent) {
        String action = intent.getAction();

        Log.i(TAG, "Acting on system alarm. action=" + action);

        if (action == ACTION_SET_SYSTEM_ALARM) {
            initialize();

            // switch today from "dismissed in future" to "passed"
            Intent hideIntent = new Intent();
            hideIntent.setClassName("cz.jaro.alarmmorning", "cz.jaro.alarmmorning.CalendarActivity");
            hideIntent.setAction(CalendarActivity.ACTION_UPDATE_TODAY);
            LocalBroadcastManager.getInstance(context).sendBroadcast(hideIntent);
        } else if (action == ACTION_RING_IN_NEAR_FUTURE) {
            Log.i(TAG, "Near future");

            Calendar alarmTime = AlarmDataSource.getNextAlarm(context);
            registerSystemAlarm(ACTION_RING, alarmTime);

            GlobalManager globalManager = new GlobalManager(context);
            globalManager.onNearFuture();
        } else if (action == ACTION_RING) {
            Log.i(TAG, "Ring");

            initialize();

            GlobalManager globalManager = new GlobalManager(context);
            globalManager.onRing();
        } else {
            throw new IllegalArgumentException();
        }
    }

    public void onDismissBeforeRinging() {
        Log.d(TAG, "onDismissBeforeRinging()");

        Calendar alarmTime = AlarmDataSource.getNextAlarm(context);

        cancelSystemAlarm();
        registerSystemAlarm(ACTION_SET_SYSTEM_ALARM, alarmTime);
    }

    public Calendar onSnooze() {
        Log.d(TAG, "onSnooze()");

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        int snoozeTime = preferences.getInt(SettingsFragment.PREF_SNOOZE_TIME, SettingsFragment.PREF_SNOOZE_TIME_DEFAULT);

        Calendar ringAfterSnoozeTime = Calendar.getInstance();
        ringAfterSnoozeTime.add(Calendar.MINUTE, snoozeTime);
        ringAfterSnoozeTime.set(Calendar.SECOND, 0);
        ringAfterSnoozeTime.set(Calendar.MILLISECOND, 0);

        cancelSystemAlarm();
        registerSystemAlarm(ACTION_RING, ringAfterSnoozeTime);

        return ringAfterSnoozeTime;
    }

}
