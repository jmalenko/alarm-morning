package cz.jaro.alarmmorning;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;
import android.util.Log;

import java.util.Calendar;
import java.util.GregorianCalendar;

import cz.jaro.alarmmorning.clock.Clock;
import cz.jaro.alarmmorning.model.AlarmDataSource;
import cz.jaro.alarmmorning.receivers.AlarmReceiver;

/**
 * The SystemAlarm handles the "waking up of the app at a specified time to perform an action".
 */
public class SystemAlarm {

    private static final String TAG = SystemAlarm.class.getSimpleName();

    private static SystemAlarm instance;

    private Context context;
    private AlarmManager alarmManager;

    private Intent intent;
    private PendingIntent operation;

    /**
     * Action meaning: Set system alarm for next action. (Currently, all alarms are unset in the next {@link AlarmDataSource#HORIZON_DAYS} days.)
     */
    protected static final String ACTION_SET_SYSTEM_ALARM = "SET_SYSTEM_ALARM";

    /**
     * Action meaning: The next alarm is near (in 2 hours.)
     */
    protected static final String ACTION_RING_IN_NEAR_FUTURE = "RING_IN_NEAR_FUTURE";

    /**
     * Action meaning: The alarm tim of an early dismissed alarm.
     */
    protected static final String ACTION_ALARM_TIME_OF_EARLY_DISMISSED_ALARM = "ALARM_TIME_OF_EARLY_DISMISSED_ALARM";

    /**
     * Action meaning: Start ringing.
     */
    public static String ACTION_RING = "RING";

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

    private void registerSystemAlarm(NextAction nextAction) {
        Log.i(TAG, "Setting system alarm at " + nextAction.time.getTime().toString() + " with action " + nextAction.action);

        saveNextAction(nextAction);

        intent = new Intent(context, AlarmReceiver.class);
        intent.setAction(nextAction.action);

        operation = PendingIntent.getBroadcast(context, 1, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            alarmManager.set(AlarmManager.RTC_WAKEUP, nextAction.time.getTimeInMillis(), operation);
        } else if (Build.VERSION_CODES.KITKAT <= Build.VERSION.SDK_INT && Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, nextAction.time.getTimeInMillis(), operation);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, nextAction.time.getTimeInMillis(), operation);
        }
    }

    protected void registerSystemAlarm(String action, Calendar time, Calendar alarmTime) {
        NextAction nextAction = new NextAction(action, time, alarmTime);
        registerSystemAlarm(nextAction);
    }

    private void saveNextAction(NextAction nextAction) {
        Log.d(TAG, "setNextAction()");

        GlobalManager globalManager = new GlobalManager(context);
        globalManager.setNextAction(nextAction);
    }

    private void cancelSystemAlarm() {
        Log.d(TAG, "cancelSystemAlarm()");
        if (operation != null) {
            // Method 1: standard
            Log.d(TAG, "Cancelling current system alarm");
            operation.cancel();
        } else {
            // Method 2: try to recreate the operation
            Log.d(TAG, "Recreating operation when cancelling system alarm");

            GlobalManager globalManager = new GlobalManager(context);
            NextAction nextAction2 = globalManager.getNextAction();

            Intent intent2 = new Intent(context, AlarmReceiver.class);
            intent2.setAction(nextAction2.action);

            PendingIntent operation2 = PendingIntent.getBroadcast(context, 1, intent2, PendingIntent.FLAG_NO_CREATE);

            if (operation2 != null) {
                operation2.cancel();
            }
        }
    }

    /*
     * Persistence
     * ===========
     */

    private void initialize() {
        GlobalManager globalManager = new GlobalManager(context);
        Clock clock = globalManager.clock();

        NextAction nextAction = calcNextAction(clock);
        registerSystemAlarm(nextAction);
    }

    /**
     * Returns the next action and system alarm according to the data in database.
     *
     * @return the next action and system alarm
     */
    protected NextAction calcNextAction(Clock clock) {
        Log.d(TAG, "calcNextAction()");

        Calendar now = clock.now();

        Calendar alarmTime = AlarmDataSource.getNextAlarm(context, clock);

        if (alarmTime == null) {
            Calendar resetTime = getResetTime(now);

            return new NextAction(ACTION_SET_SYSTEM_ALARM, resetTime, alarmTime);
        } else {
            if (useNearFutureTime()) {
                Calendar nearFutureTime = getNearFutureTime(alarmTime);

                if (now.before(nearFutureTime)) {
                    return new NextAction(ACTION_RING_IN_NEAR_FUTURE, nearFutureTime, alarmTime);
                }
            }

            return new NextAction(ACTION_RING, alarmTime, alarmTime);
        }
    }

    protected boolean nextActionShouldChange() {
        Log.v(TAG, "nextActionSh ouldChange()");

        GlobalManager globalManager = new GlobalManager(context);

        Clock clock = globalManager.clock();
        NextAction nextAction = calcNextAction(clock);

        NextAction nextActionPersisted = globalManager.getNextAction();

        return !nextActionPersisted.equals(nextAction);
    }

    public static Calendar getResetTime(Calendar now) {
        Calendar resetTime = new GregorianCalendar(now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH));
        resetTime.add(Calendar.DAY_OF_MONTH, 1);

        return resetTime;
    }

    public boolean useNearFutureTime() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        int nearFutureMinutes = preferences.getInt(SettingsActivity.PREF_NEAR_FUTURE_TIME, SettingsActivity.PREF_NEAR_FUTURE_TIME_DEFAULT);

        return 0 < nearFutureMinutes;
    }

    private Calendar getNearFutureTime(Calendar alarmTime) {
        return getNearFutureTime(context, alarmTime);
    }

    public static Calendar getNearFutureTime(Context context, Calendar alarmTime) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        int nearFutureMinutes = preferences.getInt(SettingsActivity.PREF_NEAR_FUTURE_TIME, SettingsActivity.PREF_NEAR_FUTURE_TIME_DEFAULT);

        Calendar nearFutureTime = (Calendar) alarmTime.clone();
        nearFutureTime.add(Calendar.MINUTE, -nearFutureMinutes);

        return nearFutureTime;
    }

    public void onSystemAlarm(Intent intent) {
        String action = intent.getAction();

        Log.i(TAG, "Acting on system alarm. action=" + action);

        GlobalManager globalManager = new GlobalManager(context);

        if (action.equals(ACTION_SET_SYSTEM_ALARM)) {
            globalManager.onAlarmTimeOfEarlyDismissedAlarm();
        } else if (action.equals(ACTION_RING_IN_NEAR_FUTURE)) {
            globalManager.onNearFuture();
        } else if (action.equals(ACTION_ALARM_TIME_OF_EARLY_DISMISSED_ALARM)) {
            globalManager.onAlarmTimeOfEarlyDismissedAlarm();
        } else if (action.equals(ACTION_RING)) {
            globalManager.onRing();
        } else {
            throw new IllegalArgumentException("Unexpected argument " + action);
        }
    }

    /*
     * Events
     * ======
     */

    public void onAlarmSet() {
        Log.d(TAG, "onAlarmSet()");

        cancelSystemAlarm();

        initialize();
    }

    public void onNearFuture() {
        Log.d(TAG, "onNearFuture()");

        GlobalManager globalManager = new GlobalManager(context);
        Clock clock = globalManager.clock();

        Calendar alarmTime = AlarmDataSource.getNextAlarm(context, clock);
        assert alarmTime != null;
        registerSystemAlarm(ACTION_RING, alarmTime, alarmTime);
    }

    public void onDismissBeforeRinging() {
        Log.d(TAG, "onDismissBeforeRinging()");

        cancelSystemAlarm();

        GlobalManager globalManager = new GlobalManager(context);
        Calendar alarmTime = globalManager.getAlarmTimeOfRingingAlarm();

        registerSystemAlarm(ACTION_ALARM_TIME_OF_EARLY_DISMISSED_ALARM, alarmTime, alarmTime);
    }

    public void onAlarmTimeOfEarlyDismissedAlarm() {
        Log.d(TAG, "onAlarmTimeOfEarlyDismissedAlarm()");

        initialize();
    }

    public void onRing() {
        Log.d(TAG, "onRing()");

        initialize();
    }

    public void onSnooze(Calendar ringAfterSnoozeTime, Calendar alarmTime) {
        Log.d(TAG, "onSnooze()");

        cancelSystemAlarm();

        GlobalManager globalManager = new GlobalManager(context);
        Calendar alarmTimeOfRingingAlarm = globalManager.getAlarmTimeOfRingingAlarm();

        registerSystemAlarm(ACTION_RING, ringAfterSnoozeTime, alarmTimeOfRingingAlarm);
    }

}

class NextAction {
    String action;
    Calendar time;
    Calendar alarmTime;

    public NextAction(String action, Calendar time, Calendar alarmTime) {
        this.action = action;
        this.time = time;
        this.alarmTime = alarmTime;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        NextAction that = (NextAction) o;

        if (action != null ? !action.equals(that.action) : that.action != null) return false;
        if (time != null ? !time.equals(that.time) : that.time != null) return false;
        return !(alarmTime != null ? !alarmTime.equals(that.alarmTime) : that.alarmTime != null);
    }

    @Override
    public int hashCode() {
        int result = action != null ? action.hashCode() : 0;
        result = 31 * result + (time != null ? time.hashCode() : 0);
        result = 31 * result + (alarmTime != null ? alarmTime.hashCode() : 0);
        return result;
    }
}

