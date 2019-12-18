package cz.jaro.alarmmorning;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import java.util.Calendar;
import java.util.Objects;
import java.util.Set;

import cz.jaro.alarmmorning.clock.Clock;
import cz.jaro.alarmmorning.model.AppAlarm;
import cz.jaro.alarmmorning.receivers.AlarmReceiver;

import static cz.jaro.alarmmorning.GlobalManager.PERSIST_ALARM_ID;
import static cz.jaro.alarmmorning.GlobalManager.PERSIST_ALARM_TYPE;
import static cz.jaro.alarmmorning.GlobalManager.STATE_DISMISSED;
import static cz.jaro.alarmmorning.GlobalManager.STATE_DISMISSED_BEFORE_RINGING;
import static cz.jaro.alarmmorning.GlobalManager.getNearFutureTime;
import static cz.jaro.alarmmorning.GlobalManager.getResetTime;
import static cz.jaro.alarmmorning.GlobalManager.useNearFutureTime;

/**
 * The SystemAlarm handles the "waking up of the app at a specified time to perform an action".
 */
public class SystemAlarm {

    private static SystemAlarm instance;

    private final Context context;
    private final AlarmManager alarmManager;

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
            instance = new SystemAlarm(context.getApplicationContext());
        }
        return instance;
    }

    private void registerSystemAlarm(NextAction nextAction) {
        MyLog.i("Setting system alarm at " + nextAction.time.getTime().toString() + " with action " + nextAction.action);

        saveNextAction(nextAction);

        Intent intent = new Intent(context, AlarmReceiver.class);
        intent.setAction(nextAction.action);
        if (nextAction.appAlarm != null) {
            intent.putExtra(PERSIST_ALARM_TYPE, nextAction.appAlarm.getClass().getSimpleName());
            intent.putExtra(PERSIST_ALARM_ID, nextAction.appAlarm.getPersistenceId());
        }

        operation = PendingIntent.getBroadcast(context, 1, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        setSystemAlarm(alarmManager, nextAction.time, operation);
    }

    /**
     * Register system alarm that works reliably - triggers on a specific time, regardless the Android version, and whether the device is asleep (in low-power
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

    private void registerSystemAlarm(String action, Calendar time, AppAlarm appAlarm) {
        NextAction nextAction = new NextAction(action, time, appAlarm);
        registerSystemAlarm(nextAction);
    }

    private void saveNextAction(NextAction nextAction) {
        MyLog.d("setNextAction()");

        GlobalManager globalManager = GlobalManager.getInstance();
        globalManager.setNextAction(nextAction);
    }

    private void cancel() {
        MyLog.d("cancel()");
        if (operation != null) {
            // Method 1: standard
            MyLog.d("Cancelling current system alarm");
            operation.cancel();
        } else {
            // Method 2: try to recreate the operation
            MyLog.d("Recreating operation when cancelling system alarm");

            try {
                GlobalManager globalManager = GlobalManager.getInstance();
                NextAction nextAction = globalManager.getNextAction();

                Intent intent2 = new Intent(context, AlarmReceiver.class);
                intent2.setAction(nextAction.action);

                PendingIntent operation = PendingIntent.getBroadcast(context, 1, intent2, PendingIntent.FLAG_NO_CREATE);

                if (operation != null) {
                    operation.cancel();
                }
            } catch (IllegalArgumentException e) {
                MyLog.d("Unable to get the action for cancelling", e);
            }
        }
    }

    private void reset() {
        MyLog.v("reset()");

        cancel();

        register();
    }

    /*
     * Persistence
     * ===========
     */

    private void register() {
        NextAction nextAction = calcNextAction();
        registerSystemAlarm(nextAction);
    }

    /**
     * Returns the next action (after current time) and system alarm according to the data in database.
     *
     * @return the next action and system alarm
     */
    NextAction calcNextAction() {
        MyLog.d("calcNextAction()");

        GlobalManager globalManager = GlobalManager.getInstance();
        Clock clock = globalManager.clock();

        return calcNextAction(clock);
    }

    /**
     * Returns the next action after time <code>clock</code> and system alarm according to the data in database.
     *
     * @return the next action and system alarm
     */
    private NextAction calcNextAction(Clock clock) {
        MyLog.d("calcNextAction(clock=" + clock.now().getTime().toString() + ")");

        Calendar now = clock.now();

        GlobalManager globalManager = GlobalManager.getInstance();
        AppAlarm nextAlarmToRingWithoutCurrent = globalManager.getNextAlarm(clock, appAlarm -> {
            MyLog.v("   checking filter condition for " + appAlarm);
            int state = globalManager.getState(appAlarm);
            return state != STATE_DISMISSED_BEFORE_RINGING && state != STATE_DISMISSED;
        });

        AppAlarm nearestDismissedAlarm = findNearestDismissedAlarm(clock);

        if (nextAlarmToRingWithoutCurrent == null) {
            Calendar resetTime = getResetTime(now);

            if (nearestDismissedAlarm == null || resetTime.before(nearestDismissedAlarm.getDateTime()))
                return new NextAction(ACTION_SET_SYSTEM_ALARM, resetTime, null);
            else
                return new NextAction(ACTION_ALARM_TIME_OF_EARLY_DISMISSED_ALARM, nearestDismissedAlarm.getDateTime(), nearestDismissedAlarm);
        } else {
            if (useNearFutureTime()) {
                Calendar nearFutureTime = getNearFutureTime(nextAlarmToRingWithoutCurrent.getDateTime());

                if (now.before(nearFutureTime)) {
                    if (nearestDismissedAlarm == null || nearFutureTime.before(nearestDismissedAlarm.getDateTime()))
                        return new NextAction(ACTION_RING_IN_NEAR_FUTURE, nearFutureTime, nextAlarmToRingWithoutCurrent);
                    else
                        return new NextAction(ACTION_ALARM_TIME_OF_EARLY_DISMISSED_ALARM, nearestDismissedAlarm.getDateTime(), nearestDismissedAlarm);
                }
            }

            if (nearestDismissedAlarm == null || nextAlarmToRingWithoutCurrent.getDateTime().before(nearestDismissedAlarm.getDateTime()))
                return new NextAction(ACTION_RING, nextAlarmToRingWithoutCurrent.getDateTime(), nextAlarmToRingWithoutCurrent);
            else
                return new NextAction(ACTION_ALARM_TIME_OF_EARLY_DISMISSED_ALARM, nearestDismissedAlarm.getDateTime(), nearestDismissedAlarm);
        }
    }

    private AppAlarm findNearestDismissedAlarm(Clock clock) {
        GlobalManager globalManager = GlobalManager.getInstance();
        Calendar now = clock.now();

        AppAlarm nearestDismissedAlarm = null;

        Set<AppAlarm> dismissedAlarms = globalManager.getDismissedAlarms();
        for (AppAlarm dismissedAlarm : dismissedAlarms) {
            if (now.before(dismissedAlarm.getDateTime())) {
                if (nearestDismissedAlarm == null || dismissedAlarm.getDateTime().before(nearestDismissedAlarm.getDateTime())) {
                    nearestDismissedAlarm = dismissedAlarm;
                }
            }
        }

        return nearestDismissedAlarm;
    }

    boolean nextActionShouldChange() {
        MyLog.v("nextActionShouldChange()");

        NextAction nextAction = calcNextAction();

        try { // When running tests for the 1st time, there is no persisted action
            GlobalManager globalManager = GlobalManager.getInstance();
            NextAction nextActionPersisted = globalManager.getNextAction();

            return !nextActionPersisted.equals(nextAction);
        } catch (IllegalArgumentException e) {
            return true;
        }
    }

    public void onSystemAlarm(Intent intent) {
        String action = intent.getAction();

        MyLog.i("Acting on system alarm. action=" + action);

        AppAlarm appAlarm = null;
        GlobalManager globalManager = GlobalManager.getInstance();
        String alarmType = intent.getStringExtra(PERSIST_ALARM_TYPE);
        String alarmId = intent.getStringExtra(PERSIST_ALARM_ID);
        if (alarmType != null && alarmId != null)
            appAlarm = globalManager.load(alarmType, alarmId);

        switch (action) {
            case ACTION_SET_SYSTEM_ALARM:
                globalManager.onDateChange();
                break;
            case ACTION_RING_IN_NEAR_FUTURE:
                globalManager.onNearFuture(appAlarm);
                break;
            case ACTION_ALARM_TIME_OF_EARLY_DISMISSED_ALARM:
                globalManager.onAlarmTimeOfEarlyDismissedAlarm(appAlarm);
                break;
            case ACTION_RING:
                globalManager.onRing(appAlarm);
                break;
            default:
                throw new IllegalArgumentException("Unexpected argument " + action);
        }
    }

    /*
     * Events
     * ======
     */

    void onAlarmSet() {
        MyLog.d("onAlarmSet()");

        reset();
    }

    void onNearFuture(AppAlarm appAlarm) {
        MyLog.d("onNearFuture()");

        registerSystemAlarm(ACTION_RING, appAlarm.getDateTime(), appAlarm);
    }

    void onDismissBeforeRinging() {
        MyLog.d("onDismissBeforeRinging()");

        reset();
    }

    void onAlarmTimeOfEarlyDismissedAlarm() {
        MyLog.d("onAlarmTimeOfEarlyDismissedAlarm()");

        register();
    }

    void onRing() {
        MyLog.d("onRing()");

        register();
    }

    void onSnooze(Calendar ringAfterSnoozeTime) {
        MyLog.d("onSnooze()");

        cancel();

        GlobalManager globalManager = GlobalManager.getInstance();
        AppAlarm ringingAlarm = globalManager.getRingingAlarm();

        registerSystemAlarm(ACTION_RING, ringAfterSnoozeTime, ringingAlarm);
    }

    void onDateChange() {
        MyLog.d("onDateChanged()");

        register();
    }

}

class NextAction {
    final String action;
    final Calendar time;
    final AppAlarm appAlarm;

    NextAction(String action, Calendar time, AppAlarm appAlarm) {
        this.action = action;
        this.time = time;
        this.appAlarm = appAlarm;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        NextAction that = (NextAction) o;

        if (!Objects.equals(action, that.action)) return false;
        if (!Objects.equals(time, that.time)) return false;
        return Objects.equals(appAlarm, that.appAlarm);
    }

    @Override
    public int hashCode() {
        int result = action != null ? action.hashCode() : 0;
        result = 31 * result + (time != null ? time.hashCode() : 0);
        result = 31 * result + (appAlarm != null ? appAlarm.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "action=" + action +
                ", time=" + time.getTime() +
                ", appAlarm=" + (appAlarm != null ? appAlarm : "null");
    }

}

