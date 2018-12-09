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
import java.util.Iterator;
import java.util.Set;

import cz.jaro.alarmmorning.clock.Clock;
import cz.jaro.alarmmorning.model.AppAlarm;
import cz.jaro.alarmmorning.model.OneTimeAlarm;
import cz.jaro.alarmmorning.receivers.AlarmReceiver;

import static cz.jaro.alarmmorning.calendar.CalendarUtils.beginningOfTomorrow;

/**
 * The SystemAlarm handles the "waking up of the app at a specified time to perform an action".
 */
public class SystemAlarm {

    private static final String TAG = SystemAlarm.class.getSimpleName();

    private static SystemAlarm instance;

    private Context context;
    private AlarmManager alarmManager;

    private PendingIntent operation;

    /**
     * Action meaning: Set system alarm for next action. (Currently, all alarms are unset in the next {@link GlobalManager#HORIZON_DAYS} days.)
     */
    public static final String ACTION_SET_SYSTEM_ALARM = "SET_SYSTEM_ALARM";

    /**
     * Action meaning: The next alarm is near (in 2 hours.)
     */
    public static final String ACTION_RING_IN_NEAR_FUTURE = "RING_IN_NEAR_FUTURE";

    /**
     * Action meaning: The alarm tim of an early dismissed alarm.
     */
    public static final String ACTION_ALARM_TIME_OF_EARLY_DISMISSED_ALARM = "ALARM_TIME_OF_EARLY_DISMISSED_ALARM";

    /**
     * Action meaning: Start ringing.
     */
    public static final String ACTION_RING = "RING";

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

        Intent intent = new Intent(context, AlarmReceiver.class);
        intent.setAction(nextAction.action);

        operation = PendingIntent.getBroadcast(context, 1, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        setSystemAlarm(alarmManager, nextAction.time, operation);
    }

    /**
     * Register system alarm that works reliably - triggers on a specific time, regardles the Android version, and whether the devicee is asleep (in low-power
     * idle mode).
     *
     * @param alarmManager alarmManager
     * @param time         time
     * @param operation    operation
     */
    static public void setSystemAlarm(AlarmManager alarmManager, Calendar time, PendingIntent operation) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            alarmManager.set(AlarmManager.RTC_WAKEUP, time.getTimeInMillis(), operation);
        } else if (Build.VERSION_CODES.KITKAT <= Build.VERSION.SDK_INT && Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, time.getTimeInMillis(), operation);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, time.getTimeInMillis(), operation);
        }
    }

    private void registerSystemAlarm(String action, Calendar time, Calendar alarmTime, Long oneTimeAlarmId) {
        NextAction nextAction = new NextAction(action, time, alarmTime, oneTimeAlarmId);
        registerSystemAlarm(nextAction);
    }

    private void saveNextAction(NextAction nextAction) {
        Log.d(TAG, "setNextAction()");

        GlobalManager globalManager = GlobalManager.getInstance();
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

            GlobalManager globalManager = GlobalManager.getInstance();
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
        NextAction nextAction = calcNextAction();
        registerSystemAlarm(nextAction);
    }

    /**
     * Returns the next action (after current time) and system alarm according to the data in database.
     *
     * @return the next action and system alarm
     */
    protected NextAction calcNextAction() {
        Log.d(TAG, "calcNextAction()");

        GlobalManager globalManager = GlobalManager.getInstance();
        Clock clock = globalManager.clock();

        return calcNextAction(clock);
    }

    /**
     * Returns the next action after time <code>clock</code> and system alarm according to the data in database.
     *
     * @return the next action and system alarm
     */
    protected NextAction calcNextAction(Clock clock) {
        Log.d(TAG, "calcNextAction(clock=" + clock.now().getTime().toString() + ")");

        Calendar now = clock.now();

        GlobalManager globalManager = GlobalManager.getInstance();
        AppAlarm appAlarm = globalManager.getNextAlarm(clock, null);

        if (appAlarm == null) {
            Calendar resetTime = getResetTime(now);

            return new NextAction(ACTION_SET_SYSTEM_ALARM, resetTime, null, null);
        } else {
            Calendar alarmTime = appAlarm.getDateTime();
            Long oneTimeAlarmId = appAlarm instanceof OneTimeAlarm ? ((OneTimeAlarm) appAlarm).getId() : null;

            if (useNearFutureTime()) {
                Calendar nearFutureTime = getNearFutureTime(alarmTime);

                if (now.before(nearFutureTime)) {
                    return new NextAction(ACTION_RING_IN_NEAR_FUTURE, nearFutureTime, alarmTime, oneTimeAlarmId);
                }
            }

            return new NextAction(ACTION_RING, alarmTime, alarmTime, oneTimeAlarmId);
        }
    }

    protected boolean nextActionShouldChange() {
        Log.v(TAG, "nextActionShouldChange()");

        NextAction nextAction = calcNextAction();

        GlobalManager globalManager = GlobalManager.getInstance();
        NextAction nextActionPersisted = globalManager.getNextAction();

        return !nextActionPersisted.equals(nextAction);
    }

    public static Calendar getResetTime(Calendar now) {
        Calendar resetTime = beginningOfTomorrow(now);
        return resetTime;
    }

    public boolean useNearFutureTime() {
        return useNearFutureTime(context);
    }

    public static boolean useNearFutureTime(Context context) {
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

        GlobalManager globalManager = GlobalManager.getInstance();

        switch (action) {
            case ACTION_SET_SYSTEM_ALARM:
                globalManager.onAlarmTimeOfEarlyDismissedAlarm();
                break;
            case ACTION_RING_IN_NEAR_FUTURE:
                globalManager.onNearFuture();
                break;
            case ACTION_ALARM_TIME_OF_EARLY_DISMISSED_ALARM:
                globalManager.onAlarmTimeOfEarlyDismissedAlarm();
                break;
            case ACTION_RING:
                globalManager.onRing();
                break;
            default:
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

        GlobalManager globalManager = GlobalManager.getInstance();
        Clock clock = globalManager.clock();

        AppAlarm appAlarm = globalManager.getNextAlarm(clock, null);
        assert appAlarm != null;
        Calendar alarmTime = appAlarm.getDateTime();
        assert alarmTime != null;
        Long oneTimeAlarmId = appAlarm instanceof OneTimeAlarm ? ((OneTimeAlarm) appAlarm).getId() : null;

        registerSystemAlarm(ACTION_RING, alarmTime, alarmTime, oneTimeAlarmId);
    }

    public void onDismissBeforeRinging() {
        Log.d(TAG, "onDismissBeforeRinging()");

        cancelSystemAlarm();

        GlobalManager globalManager = GlobalManager.getInstance();

        // Find the nearest dismissed alarm
        Calendar alarmTime = null;
        Calendar now = globalManager.clock().now();
        Set<Long> dismissedAlarms = globalManager.getDismissedAlarms();
        for (Iterator<Long> iterator = dismissedAlarms.iterator(); iterator.hasNext(); ) {
            long dismissedAlarm = iterator.next();

            Calendar dismissedAlarmCalendar = new GregorianCalendar();
            dismissedAlarmCalendar.setTimeInMillis(dismissedAlarm);
            if (now.before(dismissedAlarmCalendar)) {
                if (alarmTime == null || dismissedAlarmCalendar.before(alarmTime)) {
                    alarmTime = dismissedAlarmCalendar;
                }
            }
        }

        AppAlarm alarmOfRingingAlarm = globalManager.getNextActionAlarm();
        Long oneTimeAlarmId = alarmOfRingingAlarm instanceof OneTimeAlarm ? ((OneTimeAlarm) alarmOfRingingAlarm).getId() : null;

        registerSystemAlarm(ACTION_ALARM_TIME_OF_EARLY_DISMISSED_ALARM, alarmTime, alarmTime, oneTimeAlarmId);
    }

    public static Calendar min(Calendar a, Calendar b) {
        return a.before(b) ? a : b;
    }

    public void onAlarmTimeOfEarlyDismissedAlarm() {
        Log.d(TAG, "onAlarmTimeOfEarlyDismissedAlarm()");

        initialize();
    }

    public void onRing() {
        Log.d(TAG, "onRing()");

        initialize();
    }

    public void onSnooze(Calendar ringAfterSnoozeTime) {
        Log.d(TAG, "onSnooze()");

        cancelSystemAlarm();

        GlobalManager globalManager = GlobalManager.getInstance();
        Calendar alarmTimeOfRingingAlarm = globalManager.getAlarmTimeOfRingingAlarm();

        AppAlarm alarmOfRingingAlarm = globalManager.getNextActionAlarm();
        Long oneTimeAlarmId = alarmOfRingingAlarm instanceof OneTimeAlarm ? ((OneTimeAlarm) alarmOfRingingAlarm).getId() : null;

        registerSystemAlarm(ACTION_RING, ringAfterSnoozeTime, alarmTimeOfRingingAlarm, oneTimeAlarmId);
    }

}

class NextAction {
    String action;
    Calendar time;
    Calendar alarmTime;
    Long oneTimeAlarmId;

    public NextAction(String action, Calendar time, Calendar alarmTime, Long oneTimeAlarmId) {
        this.action = action;
        this.time = time;
        this.alarmTime = alarmTime;
        this.oneTimeAlarmId = oneTimeAlarmId;
    }

    /**
     * Compares this NextAction to the specified object.  The result is {@code true} if and only if the argument is not {@code null} and is a {@code NextAction}
     * object that represents the same action, time and alarm time.
     * <p>
     * Implementation note: the standard equality of Calendar is not used, because it also considers fields irrelevant for alarm time, which is simply a time
     * from epoch (specifically, the firstDayOfWeek, minimalDaysInFirstWeek, zone and lenient fields are compared)
     *
     * @param o The object to compare this {@code NextAction} against
     * @return {@code true} if the given object represents a {@code NextAction} equivalent to this nextAction, {@code false} otherwise
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        NextAction that = (NextAction) o;

        if (action != null ? !action.equals(that.action) : that.action != null) return false;
        if (time != null ? !time.equals(that.time) : that.time != null) return false;
        if (alarmTime != null ? !alarmTime.equals(that.alarmTime) : that.alarmTime != null) return false;
        return oneTimeAlarmId != null ? oneTimeAlarmId.equals(that.oneTimeAlarmId) : that.oneTimeAlarmId == null;
    }

    @Override
    public int hashCode() {
        int result = action != null ? action.hashCode() : 0;
        result = 31 * result + (time != null ? time.hashCode() : 0);
        result = 31 * result + (alarmTime != null ? alarmTime.hashCode() : 0);
        result = 31 * result + (oneTimeAlarmId != null ? oneTimeAlarmId.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "action=" + action +
                ", time=" + time.getTime() +
                ", alarmTime=" + (alarmTime != null ? alarmTime.getTime() : "null") +
                ", oneTimeAlarmId=" + (oneTimeAlarmId != null ? oneTimeAlarmId.toString() : "null");
    }

}

