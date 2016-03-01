package cz.jaro.alarmmorning;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import cz.jaro.alarmmorning.clock.Clock;
import cz.jaro.alarmmorning.clock.SystemClock;
import cz.jaro.alarmmorning.model.AlarmDataSource;
import cz.jaro.alarmmorning.model.Day;

/**
 * The GlobalManager keeps the state of the application and communicates with the application components.
 * <p/>
 * The <b>state of the application</b> must be kept somewhere. Reasons:<br>
 * 1. After booting, only the  receiver of next {@link SystemAlarm} is registered. This is the "minimal state of interaction with operating system" and the
 * operating system / user may bring the app to this state (e.g. operating system by destroying the activity if it is in the background, user cancelling the
 * notification).<br>
 * 2. There is no way to get the system alarm broadcast registered by this app (in the minimal state described above). Therefore, the reference must be kept
 * here (to allow its cancellation when user set earlier alarm). That is realized by methods {@link #getNextAction()}} and {@link
 * #setNextAction(NextAction)} which use the value stored in {@code SharedPreferences}.
 * <p/>
 * The <b>communication among application components</b> must communicate.<br>
 * Then the user makes an action in one component, the other components must act accordingly. Also, the time events must be handled (alarm rings till next
 * alarm; if there are no alarm until horizon (because the user disabled all the alarms), check again just before the horizon (as the defaults may make an
 * alarm
 * appear).
 * <p/>
 * The <b>components of the application</b> are:<br>
 * 1. State of the alarm ("was it dimissed?")<br>
 * 2. The registered System alarm<br>
 * 3. Notification<br>
 * 4. Ring activity (when alarm is ringing)<br>
 * 5. App activity<br>
 */
public class GlobalManager {

    /*
     * Technical information
     * =====================
     *
     * The GlobalManager keeps the following atributes by storing them in SharedPreferences:
     * 1. State of the alarm. Typically, this is the "state of the today's alarm", but if the alarm keeps ringing until the alarm time of the next alarm (on the
     * next (or later) day), this is the state of such an alarm.
     * 2. Time of next system alarm. This is used for cancelling the system alarm.
     *
     * Communication with mdules
     * =========================
     *
     * Activities (both AlarmMorningActivity and RingActivity)
     * Everything the activities need to persist is persisted in GlobalManager.
     * Reason: the BroadcastReceiver (an persistence at the module level) cannot be used since the activity may not be running.
     */

    private static final String TAG = GlobalManager.class.getSimpleName();

    public static final int STATE_UNDEFINED = 0;
    public static final int STATE_FUTURE = 1;
    public static final int STATE_RINGING = 2;
    public static final int STATE_SNOOZED = 3;
    public static final int STATE_DISMISSED = 4;
    public static final int STATE_DISMISSED_BEFORE_RINGING = 5;

    private Context context;

    // TODO Resume ringing if the app was upgraded while ringing
    // TODO Resume ringing if the operating system restarted while ringing
    // TODO On start, show notification with number of alarms that were skipped
    // TODO User early dimisses the alarm and then sets the alarm to the same day and time => fix time to next alarm and other things

    public GlobalManager(Context context) {
        this.context = context;
    }

    public Day getDayWithNextAlarm() {
        Log.d(TAG, "getDayWithNextAlarm()");
        AlarmDataSource dataSource = new AlarmDataSource(context);
        dataSource.open();

        Clock clock = new SystemClock();
        Calendar today = CalendarFragment.getToday(clock);
        Day day = dataSource.loadDayDeep(today);

        dataSource.close();

        return day;
    }

    private boolean isRinging() {
        Calendar alarmTimeOfRingingAlarm = getAlarmTimeOfRingingAlarm();
        int state = getState(alarmTimeOfRingingAlarm);
        return state == GlobalManager.STATE_RINGING || state == GlobalManager.STATE_SNOOZED;
    }

    private boolean afterNearFuture() {
        Clock clock = new SystemClock(); // // TODO Solve dependency on clock
        Calendar now = clock.now();

        NextAction nextAction = getNextAction();
        Calendar nearFutureTime = SystemAlarm.getNearFutureTime(context, nextAction.alarmTime);

        return now.after(nearFutureTime);
    }

    /**
     * This method registers system alarm. If a system alarm is registered, it is canceled first.
     * <p/>
     * This method should be called on external events. Such events are application start after booting or upgrading, time (and time zone) change.
     * <p/>
     * This method should NOT be called when user sets the alarm time. Instead, call {@link #onAlarmSet()}.
     */
    public void forceSetAlarm() {
        Log.d(TAG, "forceSetAlarm()");

        onAlarmSet();
    }

    /*
     * Persistence
     * ===========
     */

    private static final String PERSIST_ACTION = "persist_system_alarm_action";
    private static final String PERSIST_TIME = "persist_system_alarm_time";
    private static final String PERSIST_ALARM_TIME = "persist_alarm_time";

    private static final String PERSIST_STATE = "persist_state";
    private static final String PERSIST_STATE_OF_ALARM_TIME = "persist_state_of_alarm_time";

    private static final String ACTION_UNDEFINED = "";
    private static final long TIME_UNDEFINED = -1;

    protected NextAction getNextAction() {
        Log.v(TAG, "getNextAction()");

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        String action = preferences.getString(PERSIST_ACTION, ACTION_UNDEFINED);
        long timeInMS = preferences.getLong(PERSIST_TIME, TIME_UNDEFINED);
        long alarmTimeInMS = preferences.getLong(PERSIST_ALARM_TIME, TIME_UNDEFINED);

        Calendar time = new GregorianCalendar();
        time.setTime(new Date(timeInMS));

        Calendar alarmTime = null;
        if (alarmTimeInMS != -1) {
            alarmTime = new GregorianCalendar();
            alarmTime.setTime(new Date(alarmTimeInMS));
        }

        NextAction nextAction = new NextAction(action, time, alarmTime);
        return nextAction;
    }

    public void setNextAction(NextAction nextAction) {
        Log.v(TAG, "setNextAction(action=" + nextAction.action + ", time=" + nextAction.time.getTime() + ", alarmTime=" + (nextAction.alarmTime != null ? nextAction.alarmTime.getTime() : "null") + ")");

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = preferences.edit();

        editor.putString(PERSIST_ACTION, nextAction.action);
        editor.putLong(PERSIST_TIME, nextAction.time.getTimeInMillis());
        editor.putLong(PERSIST_ALARM_TIME, nextAction.alarmTime != null ? nextAction.alarmTime.getTimeInMillis() : -1);

        editor.commit();
    }

    public int getState(Calendar date) {
        Log.v(TAG, "getState()");

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);

        long stateAlarmTimeInMS = preferences.getLong(PERSIST_STATE_OF_ALARM_TIME, TIME_UNDEFINED);
        Calendar stateAlarmTime = new GregorianCalendar();
        stateAlarmTime.setTime(new Date(stateAlarmTimeInMS));

        Log.v(TAG, "   comparing state alarm time " + stateAlarmTime.getTime() + " with " + date.getTime());

        if (stateAlarmTime.equals(date)) {
            int state = preferences.getInt(PERSIST_STATE, STATE_UNDEFINED);

            Log.v(TAG, "   state=" + state);
            return state;
        } else {
            Log.v(TAG, "   state=" + STATE_UNDEFINED + " because the persisted day is for another alarm time");
            return STATE_UNDEFINED;
        }
    }

    public Calendar getAlarmTimeOfRingingAlarm() {
        Log.v(TAG, "getAlarmTimeOfRingingAlarm()");

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);

        long stateAlarmTimeInMS = preferences.getLong(PERSIST_STATE_OF_ALARM_TIME, TIME_UNDEFINED);
        Calendar stateAlarmTime = new GregorianCalendar();
        stateAlarmTime.setTime(new Date(stateAlarmTimeInMS));

        return stateAlarmTime;
    }

    public void setState(int state, Calendar alarmTime) {
        Log.v(TAG, "setState(state=" + state + ", alarmTime=" + alarmTime.getTime() + ")");

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = preferences.edit();

        editor.putInt(PERSIST_STATE, state);
        editor.putLong(PERSIST_STATE_OF_ALARM_TIME, alarmTime.getTimeInMillis());

        editor.commit();
    }

    /*
     * Events
     * ======
     *
     * Do the following on each event:
     * 1. Set state
     * 2. Register next system alarm
     * 3. Handle notification
     * 4. Handle ring activity
     * 5. Handle calendar activity
     */

    /**
     * This event is triggered when the user sets the alarm. This change may be on any day and may or may not change the next alarm time. This also includes
     * disabling an alarm.
     */
    public void onAlarmSet() {
        Log.d(TAG, "onAlarmSet()");

        SystemAlarm systemAlarm = SystemAlarm.getInstance(context);
        NextAction nextAction = systemAlarm.nextAction();

        Log.d(TAG, "next action is action=" + nextAction.action + ", time=" + nextAction.time.getTime().toString());

        if (systemAlarm.nextActionShouldChange(nextAction)) {
            // cancel the current alarm
            onAlarmCancel();

            // register next system alarm
            systemAlarm.onAlarmSet();

            if (nextAction.action.equals(SystemAlarm.ACTION_SET_SYSTEM_ALARM)) {
                // nothing
            } else if (nextAction.action.equals(SystemAlarm.ACTION_RING_IN_NEAR_FUTURE)) {
                // nothing
            } else if (nextAction.action.equals(SystemAlarm.ACTION_RING)) {
                if (systemAlarm.useNearFutureTime()) {
                    onNearFuture(false);
                }
            } else {
                throw new IllegalArgumentException("Unexpected argument " + nextAction);
            }

            updateCalendarActivity(context, AlarmMorningActivity.ACTION_ALARM_SET);
        }
    }

    public void onNearFuture() {
        Log.d(TAG, "onNearFuture()");

        onNearFuture(true);
    }

    private void onNearFuture(boolean callSystemAlarm) {
        Log.d(TAG, "onNearFuture(callSystemAlarm=" + callSystemAlarm + ")");

        if (isRinging()) {
            Log.i(TAG, "The previous alarm is still ringing. Ignoring this event.");

            if (callSystemAlarm) {
                SystemAlarm systemAlarm = SystemAlarm.getInstance(context);
                systemAlarm.onNearFuture();
            }

            return;
        }

        setState(STATE_FUTURE, getNextAction().alarmTime);

        if (callSystemAlarm) {
            SystemAlarm systemAlarm = SystemAlarm.getInstance(context);
            systemAlarm.onNearFuture();
        }

        SystemNotification systemNotification = SystemNotification.getInstance(context);
        systemNotification.onNearFuture();
    }

    public void onDismissBeforeRinging() {
        Log.d(TAG, "onDismissBeforeRinging()");

        setState(STATE_DISMISSED_BEFORE_RINGING, getAlarmTimeOfRingingAlarm());

        SystemAlarm systemAlarm = SystemAlarm.getInstance(context);
        systemAlarm.onDismissBeforeRinging();

        SystemNotification systemNotification = SystemNotification.getInstance(context);
        systemNotification.onDismissBeforeRinging();

        updateCalendarActivity(context, AlarmMorningActivity.ACTION_DISMISS_BEFORE_RINGING);
    }

    public void onAlarmTimeOfEarlyDismissedAlarm() {
        Log.d(TAG, "onAlarmTimeOfEarlyDismissedAlarm()");

        SystemAlarm systemAlarm = SystemAlarm.getInstance(context);
        systemAlarm.onAlarmTimeOfEarlyDismissedAlarm();

        updateCalendarActivity(context, AlarmMorningActivity.ACTION_ALARM_TIME_OF_EARLY_DISMISSED_ALARM);
    }

    public void onRing() {
        Log.d(TAG, "onRing()");

        if (isRinging()) {
            Log.i(TAG, "The previous alarm is still ringing. Cancelling it.");

            onAlarmCancel();

            SystemNotification systemNotification = SystemNotification.getInstance(context);
            systemNotification.notifyCancelledAlarm();

            setState(STATE_RINGING, getNextAction().alarmTime);
        } else {
            setState(STATE_RINGING, getAlarmTimeOfRingingAlarm());
        }

        SystemAlarm systemAlarm = SystemAlarm.getInstance(context);
        systemAlarm.onRing();

        SystemNotification systemNotification = SystemNotification.getInstance(context);
        systemNotification.onRing();

        startRingingActivity(context);

        updateCalendarActivity(context, AlarmMorningActivity.ACTION_RING);
    }

    public void onDismiss() {
        Log.d(TAG, "onDismiss()");

        setState(STATE_DISMISSED, getAlarmTimeOfRingingAlarm());

        SystemNotification systemNotification = SystemNotification.getInstance(context);
        systemNotification.onDismiss();

        updateRingingActivity(context, RingActivity.ACTION_HIDE_ACTIVITY);

        updateCalendarActivity(context, AlarmMorningActivity.ACTION_DISMISS);

        if (afterNearFuture()) {
            Log.i(TAG, "Immediately starting \"alarm in near future\" period.");

            onNearFuture(false);
        }
    }

    public void onSnooze() {
        Log.d(TAG, "onSnooze()");

        setState(STATE_SNOOZED, getAlarmTimeOfRingingAlarm());

        Clock clock = new SystemClock(); // TODO Solve dependency on clock
        Calendar ringAfterSnoozeTime = getRingAfterSnoozeTime(clock);

        SystemAlarm systemAlarm = SystemAlarm.getInstance(context);
        systemAlarm.onSnooze(ringAfterSnoozeTime, getAlarmTimeOfRingingAlarm());

        SystemNotification systemNotification = SystemNotification.getInstance(context);
        systemNotification.onSnooze(ringAfterSnoozeTime);

        updateRingingActivity(context, RingActivity.ACTION_HIDE_ACTIVITY);

        updateCalendarActivity(context, AlarmMorningActivity.ACTION_SNOOZE);
    }

    /**
     * This event occurs when there is an alarm with state after {@link #STATE_FUTURE} and before state {@link #STATE_DISMISSED} (or {@link
     * #STATE_DISMISSED_BEFORE_RINGING}), that has to be cancelled because an earlier alarm was set by the user.
     */
    public void onAlarmCancel() {
        Log.d(TAG, "onAlarmCancel()");

        SystemNotification systemNotification = SystemNotification.getInstance(context);
        systemNotification.onAlarmCancel();

        updateRingingActivity(context, RingActivity.ACTION_HIDE_ACTIVITY);
    }

    /*
     * Actions
     * =======
     */

    public Calendar getRingAfterSnoozeTime(Clock clock) {
        Log.d(TAG, "getRingAfterSnoozeTime()");

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        int snoozeTime = preferences.getInt(SettingsActivity.PREF_SNOOZE_TIME, SettingsActivity.PREF_SNOOZE_TIME_DEFAULT);

        Calendar ringAfterSnoozeTime = clock.now();
        ringAfterSnoozeTime.add(Calendar.MINUTE, snoozeTime);
        ringAfterSnoozeTime.set(Calendar.SECOND, 0);
        ringAfterSnoozeTime.set(Calendar.MILLISECOND, 0);

        return ringAfterSnoozeTime;
    }

    private void startRingingActivity(Context context) {
        Log.d(TAG, "startRingingActivity()");

        Intent ringIntent = new Intent();
        ringIntent.setClassName("cz.jaro.alarmmorning", "cz.jaro.alarmmorning.RingActivity");
//        ringIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        ringIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        ringIntent.putExtra(RingActivity.ALARM_TIME, getAlarmTimeOfRingingAlarm());
        context.startActivity(ringIntent);
    }

    private void updateRingingActivity(Context context, String action) {
        Log.d(TAG, "updateRingingActivity(action=" + action + ")");

        Intent hideIntent = new Intent();
        hideIntent.setClassName("cz.jaro.alarmmorning", "cz.jaro.alarmmorning.RingActivity");
        hideIntent.setAction(action);
        LocalBroadcastManager.getInstance(context).sendBroadcast(hideIntent);
    }

    private void updateCalendarActivity(Context context, String action) {
        Log.d(TAG, "updateCalendarActivity(action=" + action + ")");

        Intent intent = new Intent();
        intent.setClassName("cz.jaro.alarmmorning", "cz.jaro.alarmmorning.AlarmMorningActivity");
        intent.setAction(action);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }

}