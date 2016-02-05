package cz.jaro.alarmmorning;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import java.util.Calendar;
import java.util.GregorianCalendar;

import cz.jaro.alarmmorning.clock.Clock;
import cz.jaro.alarmmorning.clock.SystemClock;
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
            // Method 1: standard
            Log.d(TAG, "Cancelling current system alarm");
            operation.cancel();
        }
        /* else { // TODO recreate the operation
            // method 2: try to recreate the operation
            Intent intent2 = new Intent(context, AlarmReceiver.class);
            intent2.setAction(action);
            PendingIntent operation2 = PendingIntent.getBroadcast(context, 1, intent2, PendingIntent.FLAG_NO_CREATE);
            if (operation2 != null) {
                operation2.cancel();
            }
        }*/
    }

    private void initialize() {
        Log.d(TAG, "initialize()");

        Clock clock = new SystemClock(); // TODO change

        Calendar alarmTime = AlarmDataSource.getNextAlarm(context, clock);

        if (alarmTime == null) {
            Calendar now = new SystemClock().now();

            Calendar resetTime = new GregorianCalendar(now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH));
            resetTime.add(Calendar.DAY_OF_MONTH, AlarmDataSource.HORIZON_DAYS - 1);

            registerSystemAlarm(ACTION_SET_SYSTEM_ALARM, resetTime);
        } else {
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
            int nearFutureMinutes = preferences.getInt(SettingsFragment.PREF_NEAR_FUTURE_TIME, SettingsFragment.PREF_NEAR_FUTURE_TIME_DEFAULT);

            Calendar nearFutureTime = (Calendar) alarmTime.clone();
            nearFutureTime.add(Calendar.MINUTE, -nearFutureMinutes);

            Calendar now = clock.now();

            if (now.before(nearFutureTime)) {
                registerSystemAlarm(ACTION_RING_IN_NEAR_FUTURE, nearFutureTime);
            } else {
                registerSystemAlarm(ACTION_RING, alarmTime);

                GlobalManager globalManager = new GlobalManager(context);
                globalManager.onNearFuture();
            }
        }
    }

//    public void setSystemAlarm() {
//        Log.d(TAG, "setSystemAlarm()");
//
//        Clock clock = new SystemClock(); // TODO change
//        Calendar alarmTime = AlarmDataSource.getNextAlarm(context, clock);
//        if (alarmTime.getTimeInMillis() != time) {
//            GlobalManager globalManager = new GlobalManager(context);
//            globalManager.onAlarmSet();
//        }
//    }

    /**
     * This method registers system alarm. If a system alarm is registered, it is canceled first.
     * <p/>
     * This method should be called on external events. Such events are application start after booting or upgrading, time (and time zone) change.
     * <p/>
     * This method should NOT be called when user sets the alarm time. Instead, call {@link GlobalManager#onAlarmSet()}.
     */
    public void setSystemAlarm() {
        Log.d(TAG, "setSystemAlarm()");

        // there may be a registered alarm when this method is called from TimeChangedReceiver or TimeZoneChangedReceiver.
        cancelSystemAlarm();

        initialize();
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

        GlobalManager globalManager = new GlobalManager(context);

        if (action.equals(ACTION_SET_SYSTEM_ALARM)) {
            globalManager.onAlarmTimeOfEarlyDismissedAlarm();
        } else if (action.equals(ACTION_RING_IN_NEAR_FUTURE)) {
            globalManager.onNearFuture();
        } else if (action.equals(ACTION_RING)) {
            globalManager.onRing();
        } else {
            throw new IllegalArgumentException();
        }
    }

    /*
     * Events
     * ======
     */

    public void onAlarmSet() {
        Log.d(TAG, "onAlarmSet()");

        Clock clock = new SystemClock(); // TODO change
        Calendar alarmTime = AlarmDataSource.getNextAlarm(context, clock);
        if (alarmTime.getTimeInMillis() != time) {
            cancelSystemAlarm();

            initialize();
        }
    }

    public void onNearFuture() {
        Log.d(TAG, "onNearFuture()");

        Clock clock = new SystemClock(); // TODO change
        Calendar alarmTime = AlarmDataSource.getNextAlarm(context, clock);
        assert alarmTime != null;
        registerSystemAlarm(ACTION_RING, alarmTime);
    }

    public void onDismissBeforeRinging() {
        Log.d(TAG, "onDismissBeforeRinging()");

        initialize();
    }

    public void onAlarmTimeOfEarlyDismissedAlarm() {
        Log.d(TAG, "onAlarmTimeOfEarlyDismissedAlarm()");

        initialize();
    }

    public void onRing() {
        Log.d(TAG, "onRing()");

        initialize();
    }

    public void onDismiss() {
        Log.d(TAG, "onDismiss()");
    }

    public void onSnooze(Calendar ringAfterSnoozeTime) {
        Log.d(TAG, "onSnooze()");

        cancelSystemAlarm();
        registerSystemAlarm(ACTION_RING, ringAfterSnoozeTime);
    }

}
