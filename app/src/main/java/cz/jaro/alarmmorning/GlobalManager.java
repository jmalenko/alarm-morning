package cz.jaro.alarmmorning;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.RemoteViews;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.Set;

import cz.jaro.alarmmorning.clock.Clock;
import cz.jaro.alarmmorning.clock.SystemClock;
import cz.jaro.alarmmorning.model.AlarmDataSource;
import cz.jaro.alarmmorning.model.Day;
import cz.jaro.alarmmorning.model.DayFilter;
import cz.jaro.alarmmorning.model.Defaults;

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

    private static final int STATE_UNDEFINED = 0;
    public static final int STATE_FUTURE = 1;
    public static final int STATE_RINGING = 2;
    public static final int STATE_SNOOZED = 3;
    public static final int STATE_DISMISSED = 4;
    public static final int STATE_DISMISSED_BEFORE_RINGING = 5;

    private Context context;

    // TODO User early dismisses the alarm and then sets the alarm to the same day and time => fix time to next alarm and other things

    public GlobalManager(Context context) {
        this.context = context;
    }

    protected Day getDayWithNextAlarmToRing() {
        Log.v(TAG, "getDayWithNextAlarm()");

        AlarmDataSource dataSource = new AlarmDataSource(context);
        dataSource.open();

        Clock clock = new SystemClock(); // TODO Solve dependency on clock

        Day day = dataSource.getNextAlarm(clock, new DayFilter() {
            @Override
            public boolean match(Day day) {
//                GlobalManager globalManager = new GlobalManager(context);
//                int state = globalManager.getState(day.getDateTime());
                int state = getState(day.getDateTime());
//                if (state != GlobalManager.STATE_UNDEFINED) {
                if (state != GlobalManager.STATE_DISMISSED_BEFORE_RINGING && state != GlobalManager.STATE_DISMISSED) {
                    return true;
                }
//                } else {
//                    return true;
//                }
                return false;
            }
        });

        dataSource.close();

        return day;
    }

    /**
     * Used for cancelling the not dismissed old alarms.
     * <pre>
     *                                                                    Time
     * ----------+---------------+---------------+---------------------------->
     *           |               |               |
     *           |               |               \- Alarm time on day D=1
     *           |               \- Start of "alarm in near future" period
     *           \- Alarm time on day D=0
     * </pre>
     *
     * @return true if the current alarm state {@link #STATE_RINGING} or {@link #STATE_SNOOZED}
     */
    private boolean isRingingOrSnoozed() {
//        Calendar alarmTimeOfRingingAlarm = getAlarmTimeOfRingingAlarm();
//        int state = getState(alarmTimeOfRingingAlarm);
        int state = getState();
        return state == STATE_RINGING || state == STATE_SNOOZED;
    }

    public boolean isRinging() {
        Log.d(TAG, "isRinging()");
        int state = getState();
        return state == STATE_RINGING;
    }

    private boolean afterNearFuture() {
        NextAction nextAction = getNextAction();
        if (nextAction.alarmTime != null) {
            return afterNearFuture(nextAction.alarmTime);
        } else {
            return false;
        }
    }

    public boolean afterNearFuture(Calendar alarmTime) {
        Clock clock = new SystemClock(); // TODO Solve dependency on clock
        Calendar now = clock.now();

        Calendar nearFutureTime = SystemAlarm.getNearFutureTime(context, alarmTime);
        return now.after(nearFutureTime);
    }

    /*
     * Persistence
     * ===========
     *
     * Keep the following information:<br>
     * - about the next SystemAlarm<br>
     * - about the last alarm<br>
     * - about the dismissed alarms. The alarms for yesterday and before are pruned. The dismissed alarm can be both today and tomorrow.<br>
     * The record contains both date and time.
     */

    /*
     * Contains info about the next SystemAlarm.
     */
    private static final String PERSIST_ACTION = "persist_system_alarm_action";
    private static final String PERSIST_TIME = "persist_system_alarm_time";
    private static final String PERSIST_ALARM_TIME = "persist_alarm_time";

    /*
     * Contains info about the last alarm. Last alarm is the one that rans, possibly is snoozed and was dismissed or cancelled.
     */
    private static final String PERSIST_STATE = "persist_state";
    private static final String PERSIST_STATE_OF_ALARM_TIME = "persist_state_of_alarm_time";

    /*
     * Contains info about the dismissed alarms.
     */
    private static final String PERSIST_DISMISSED = "persist_dismissed";

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

    private int getState() {
        Log.v(TAG, "getState()");
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);

        int state = preferences.getInt(PERSIST_STATE, STATE_UNDEFINED);

        return state;
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

    public Set<Long> getDismissedAlarms() {
        Log.v(TAG, "getDismissedAlarm()");

        try {
            JSONArray dismissedAlarmsJSON = JSONSharedPreferences.loadJSONArray(context, PERSIST_DISMISSED);
            Set<Long> dismissedAlarms = jsonToSet(dismissedAlarmsJSON);
            return dismissedAlarms;
        } catch (JSONException e) {
            Log.w(TAG, "Error getting dismissed alarms", e);
            return new HashSet<Long>();
        }
    }

    public void addDismissedAlarm(Calendar alarmTime) {
        Log.v(TAG, "addDismissedAlarm(alarmtime=" + alarmTime.getTime() + ")");

        Set<Long> dismissedAlarms = getDismissedAlarms();

        // TODO Remove elements for yesterday and before

        dismissedAlarms.add(alarmTime.getTimeInMillis());

        Log.e(TAG, "   there are " + dismissedAlarms.size() + " dismissed alarms at");
        for (long alarmTime2 : dismissedAlarms) {
            Log.e(TAG, "      " + new Date(alarmTime2) + ")");
        }

        JSONSharedPreferences.saveJSONArray(context, PERSIST_DISMISSED, new JSONArray(dismissedAlarms));
    }

    /**
     * Algorithm:<br>
     *     1. if the alarmTime is for last alarm then return the state of last alarm (same as {@link #getState()})<br>
     *     2. if the alarmTime is in the set of dismissed alarms then return {@link #STATE_DISMISSED}<br>
     *     3. if the alarmTime is in past then return {@link #STATE_DISMISSED}<br>
     *     4. if the alarmTime is in future then return {@link #STATE_FUTURE}
     *
     * @param alarmTime
     * @return
     */
    public int getState(Calendar alarmTime) {
        Log.v(TAG, "getState(alarmTime=" + alarmTime.getTime() + ")");

        // Condition 1
        Calendar stateAlarmTime = getAlarmTimeOfRingingAlarm();

        Log.d(TAG, "   saved state alarm time is " + stateAlarmTime.getTime());

        if (stateAlarmTime.equals(alarmTime)) {
            Log.i(TAG, "   using saved state alarm time");
            return getState();
        }

        // Condition 2
        Set<Long> dismissedAlarms = getDismissedAlarms();
        if (dismissedAlarms.contains(alarmTime)) {
            Log.i(TAG, "   is among dismissed => DISMISSED");
            return STATE_DISMISSED;
        }

        // Condition 3
        Clock clock = new SystemClock(); // // TODO Solve dependency on clock

        if (alarmTime.before(clock.now())) {
            Log.i(TAG, "   is in past => DISMISSED");
            return STATE_DISMISSED;
        } else {
            // Condition 4
            Log.i(TAG, "   is in future => FUTURE");
            return STATE_FUTURE;
        }
    }

    private Set<Long> jsonToSet(JSONArray jsonArray) throws JSONException {
        Set<Long> set = new HashSet<Long>(jsonArray.length());
        for (int i = 0; i < jsonArray.length(); i++) {
            set.add(jsonArray.getLong(i));
        }
        return set;
    }

    /*
     * Exernal events
     * ==============
     */

    /**
     * This method registers system alarm. If a system alarm is registered, it is canceled first.
     * <p/>
     * This method should be called on external events. Such events are application start after booting or upgrading, time (and time zone) change.
     * <p/>
     * This method should NOT be called when user sets the alarm time. Instead, call {@link #onAlarmSet()}.
     */
    public void forceSetAlarm() {
        Log.d(TAG, "forceSetAlarm()");

        SystemAlarm systemAlarm = SystemAlarm.getInstance(context);

        SystemAlarmClock systemAlarmClock = SystemAlarmClock.getInstance(context);
        systemAlarmClock.onAlarmSet();

        updateWidget(context);

        if (systemAlarm.nextActionShouldChange()) {
            NextAction nextAction = systemAlarm.calcNextAction();
            NextAction nextActionPersisted = getNextAction();

            Log.w(TAG, "The next system alarm changed while the app was not running.\n" + "   Persisted is action=" + nextActionPersisted.action + ", time=" + nextActionPersisted.time + ", alarmTime" + nextActionPersisted.alarmTime + "\n" + "   Current is   action=" + nextAction.action + ", time=" + nextAction.time + ", alarmTime" + nextAction.alarmTime); // More precisely: ... while the app was not running (e.g. because it was being upgraded or the device was  off)

            SystemNotification systemNotification = SystemNotification.getInstance(context);
            systemNotification.notifySkippedAlarms();
        }

        if (isRingingOrSnoozed()) {
            Log.w(TAG, "Previous alarm was not correctly dismissed. Resuming ringing.");

            onRing();
        } else {
            onAlarmSetNew(systemAlarm);
        }
    }


    /*
     * Events
     * ======
     *
     * Do the following on each event:
     * 1. Set state
     * 2. Register next system alarm
     * 3. Handle notification
     * 4. Handle system alarm clock
     * 5. Handle widget
     * 6. Handle ring activity
     * 7. Handle calendar activity
     */

    /**
     * @param day
     * @param dataSource
     */
    public void saveAlarmTime(Day day, AlarmDataSource dataSource) {
        Log.d(TAG, "saveAlarmTime()");

        if (day.getState() == Day.STATE_DISABLED)
            Log.i(TAG, "Disable alarm on " + day.getDateTime().getTime());
        else if (day.getState() == Day.STATE_ENABLED)
            Log.i(TAG, "Set alarm on " + day.getDateTime().getTime());
        else
            Log.i(TAG, "Reverting alarm to default on " + day.getDateTime().getTime());

        dataSource.saveDay(day);

        onAlarmSet();
    }

    public void saveAlarmTimeDefault(Defaults defaults, AlarmDataSource dataSource) {
        Log.d(TAG, "saveAlarmTimeDefault()");

        String dayOfWeekText = Localization.dayOfWeekToStringShort(context.getResources(), defaults.getDayOfWeek());
        if (defaults.getState() == Defaults.STATE_ENABLED)
            Log.i(TAG, "Set alarm at " + defaults.getHour() + ":" + defaults.getMinute() + " on " + dayOfWeekText);
        else
            Log.i(TAG, "Disabling alarm on " + dayOfWeekText);

        dataSource.saveDefault(defaults);

        onAlarmSet();
    }

    /**
     * This event is triggered when the user sets the alarm. This change may be on any day and may or may not change the next alarm time. This also includes
     * disabling an alarm.
     */
    private void onAlarmSet() {
        Log.d(TAG, "onAlarmSet()");

        SystemAlarm systemAlarm = SystemAlarm.getInstance(context);

        if (systemAlarm.nextActionShouldChange()) {
            // cancel the current alarm
            onAlarmCancel();

            onAlarmSetNew(systemAlarm);
        }

        updateCalendarActivity(context, AlarmMorningActivity.ACTION_ALARM_SET);
    }

    private void onAlarmSetNew(SystemAlarm systemAlarm) {
        Log.d(TAG, "onAlarmSetNew()");

        // register next system alarm
        systemAlarm.onAlarmSet();

        SystemAlarmClock systemAlarmClock = SystemAlarmClock.getInstance(context);
        systemAlarmClock.onAlarmSet();

        updateWidget(context);

        NextAction nextAction = systemAlarm.calcNextAction();
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
    }

    public void onNearFuture() {
        Log.d(TAG, "onNearFuture()");

        onNearFuture(true);
    }

    private void onNearFuture(boolean callSystemAlarm) {
        Log.d(TAG, "onNearFuture(callSystemAlarm=" + callSystemAlarm + ")");

        if (isRingingOrSnoozed()) {
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
        addDismissedAlarm(getAlarmTimeOfRingingAlarm());

        SystemAlarm systemAlarm = SystemAlarm.getInstance(context);
        systemAlarm.onDismissBeforeRinging();

        SystemNotification systemNotification = SystemNotification.getInstance(context);
        systemNotification.onDismissBeforeRinging();

        SystemAlarmClock systemAlarmClock = SystemAlarmClock.getInstance(context);
        systemAlarmClock.onDismissBeforeRinging();

        updateWidget(context);

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

        boolean isNew = getNextAction().time.equals(getNextAction().alarmTime); // otherwise the alarm is resumed after snoozing

        if (isRingingOrSnoozed() && isNew) {
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

        if (isNew) {
            SystemAlarmClock systemAlarmClock = SystemAlarmClock.getInstance(context);
            systemAlarmClock.onRing();

            updateWidget(context);
        }

        startRingingActivity(context);

        updateCalendarActivity(context, AlarmMorningActivity.ACTION_RING);
    }

    public void onDismiss() {
        Log.d(TAG, "onDismiss()");

        if (getState() == STATE_SNOOZED) {
            SystemAlarm systemAlarm = SystemAlarm.getInstance(context);
            systemAlarm.onAlarmSet();
        }

        setState(STATE_DISMISSED, getAlarmTimeOfRingingAlarm());
        addDismissedAlarm(getAlarmTimeOfRingingAlarm());

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

    private void updateWidget(Context context) {
        Log.d(TAG, "updateWidget()");

        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_layout);
        WidgetProvider.updateContent(context, views);
        appWidgetManager.updateAppWidget(new ComponentName(context, WidgetProvider.class), views);
    }

    protected void startRingingActivity(Context context) {
        Log.d(TAG, "startRingingActivity()");

        Intent ringIntent = new Intent(context, RingActivity.class);
//        ringIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        ringIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        ringIntent.putExtra(RingActivity.ALARM_TIME, getAlarmTimeOfRingingAlarm());
        context.startActivity(ringIntent);
    }

    private void updateRingingActivity(Context context, String action) {
        Log.d(TAG, "updateRingingActivity(action=" + action + ")");

        Intent hideIntent = new Intent(context, RingActivity.class);
        hideIntent.setAction(action);
        LocalBroadcastManager.getInstance(context).sendBroadcast(hideIntent);
    }

    private void updateCalendarActivity(Context context, String action) {
        Log.d(TAG, "updateCalendarActivity(action=" + action + ")");

        Intent intent = new Intent(context, AlarmMorningActivity.class);
        intent.setAction(action);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }

}