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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import cz.jaro.alarmmorning.clock.Clock;
import cz.jaro.alarmmorning.clock.SystemClock;
import cz.jaro.alarmmorning.model.AlarmDataSource;
import cz.jaro.alarmmorning.model.Day;
import cz.jaro.alarmmorning.model.DayFilter;
import cz.jaro.alarmmorning.model.Defaults;

/**
 * The GlobalManager keeps the state of the application and communicates with the application components.
 * <p>
 * The <b>state of the application</b> must be kept somewhere. Reasons:
 * <p>
 * 1. After booting, only the  receiver of next {@link SystemAlarm} is registered. This is the "minimal state of interaction with operating system" and the
 * operating system / user may bring the app to this state (e.g. operating system by destroying the activity if it is in the background, user cancelling the
 * notification).
 * <p>
 * 2. There is no way to get the system alarm broadcast registered by this app (in the minimal state described above). Therefore, the reference must be kept
 * here (to allow its cancellation when user set earlier alarm). That is realized by methods {@link #getNextAction()}} and {@link #setNextAction(NextAction)}
 * which use the value stored in {@code SharedPreferences}.
 * <p/>
 * The <b>communication among application components</b>:
 * <p>
 * When the user makes an action in one component, the other components must act accordingly. Also, the time events must be handled (alarm rings till next
 * alarm; if there are no alarm until horizon (because the user disabled all the alarms), check again just before the horizon (as the defaults may make an alarm
 * appear).
 * <p/>
 * The <b>components of the application</b> are:
 * <p>
 * 1. State of the alarm ("was it dismissed?")
 * <p>
 * 2. The registered System alarm
 * <p>
 * 3. Notification
 * <p>
 * 4. Ring activity (when alarm is ringing)
 * <p>
 * 5. App activity
 */
public class GlobalManager {

    public static final int HORIZON_DAYS = 30;

    /*
     * Technical information
     * =====================
     *
     * The GlobalManager keeps the following atributes by storing them in SharedPreferences:
     * 1. State of the alarm. Typically, this is the "state of the today's alarm", but if the alarm keeps ringing until the alarm time of the next alarm (on the
     * next (or later) day), this is the state of such an alarm.
     * 2. Time of next system alarm. This is used for cancelling the system alarm.
     *
     * Communication with modules
     * ==========================
     *
     * Activities (both AlarmMorningActivity and RingActivity)
     * Everything the activities need to persist is persisted in GlobalManager.
     * Reason: the BroadcastReceiver (an persistence at the module level) cannot be used since the activity may not be running.
     *
     * Singleton and Context managements was inspired by https://nfrolov.wordpress.com/2014/08/16/android-sqlitedatabase-locking-and-multi-threading/
     */

    private static final String TAG = GlobalManager.class.getSimpleName();

    private static final int STATE_UNDEFINED = 0;
    public static final int STATE_FUTURE = 1;
    public static final int STATE_RINGING = 2;
    public static final int STATE_SNOOZED = 3;
    public static final int STATE_DISMISSED = 4;
    public static final int STATE_DISMISSED_BEFORE_RINGING = 5;

    private static final int RECENT_PERIOD = 30; // minutes

    private static GlobalManager instance;

    private AlarmDataSource dataSource;

    private GlobalManager() {
        Context context = AlarmMorningApplication.getAppContext();
        dataSource = new AlarmDataSource(context);
        dataSource.open();
    }

    public static synchronized GlobalManager getInstance() {
        if (instance == null)
            instance = new GlobalManager();
        return instance;
    }

    public Clock clock() {
        return new SystemClock();
    }

    public Day getDayWithNextAlarmToRing() {
        Log.v(TAG, "getDayWithNextAlarmToRing()");

        Day day;
        if (isRingingOrSnoozed()) {
            Log.v(TAG, "   loading the ringing or snoozed alarm");
            day = dataSource.loadDay(clock().now());
        } else {
            day = getNextAlarm(clock(), day1 -> {
                Log.v(TAG, "   checking filter condition for " + day1.getDateTime().getTime());
                int state = getState(day1.getDateTime());
                return state != GlobalManager.STATE_DISMISSED_BEFORE_RINGING && state != GlobalManager.STATE_DISMISSED;
            });
        }

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

    public boolean isDismissedAny() {
        Log.d(TAG, "isDismissedAny()");
        int state = getState();
        return state == STATE_DISMISSED || state == STATE_DISMISSED_BEFORE_RINGING;
    }

    private boolean afterNearFuture() {
        NextAction nextAction = getNextAction();
        return nextAction.alarmTime != null && afterNearFuture(nextAction.alarmTime);
    }

    public boolean afterNearFuture(Calendar alarmTime) {
        Context context = AlarmMorningApplication.getAppContext();
        Calendar now = clock().now();

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

        Context context = AlarmMorningApplication.getAppContext();
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

        Context context = AlarmMorningApplication.getAppContext();
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = preferences.edit();

        editor.putString(PERSIST_ACTION, nextAction.action);
        editor.putLong(PERSIST_TIME, nextAction.time.getTimeInMillis());
        editor.putLong(PERSIST_ALARM_TIME, nextAction.alarmTime != null ? nextAction.alarmTime.getTimeInMillis() : -1);

        editor.commit();
    }

    private int getState() {
        Log.v(TAG, "getState()");
        Context context = AlarmMorningApplication.getAppContext();
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);

        int state = preferences.getInt(PERSIST_STATE, STATE_UNDEFINED);

        return state;
    }

    public Calendar getAlarmTimeOfRingingAlarm() {
        Log.v(TAG, "getAlarmTimeOfRingingAlarm()");

        Context context = AlarmMorningApplication.getAppContext();
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);

        long stateAlarmTimeInMS = preferences.getLong(PERSIST_STATE_OF_ALARM_TIME, TIME_UNDEFINED);
        Calendar stateAlarmTime = new GregorianCalendar();
        stateAlarmTime.setTime(new Date(stateAlarmTimeInMS));

        return stateAlarmTime;
    }

    public void setState(int state, Calendar alarmTime) {
        Log.v(TAG, "setState(state=" + state + ", alarmTime=" + alarmTime.getTime() + ")");

        Context context = AlarmMorningApplication.getAppContext();
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = preferences.edit();

        editor.putInt(PERSIST_STATE, state);
        editor.putLong(PERSIST_STATE_OF_ALARM_TIME, alarmTime.getTimeInMillis());

        editor.commit();
    }

    public Set<Long> getDismissedAlarms() {
        Log.v(TAG, "getDismissedAlarm()");

        try {
            Context context = AlarmMorningApplication.getAppContext();
            JSONArray dismissedAlarmsJSON = JSONSharedPreferences.loadJSONArray(context, PERSIST_DISMISSED);
            Set<Long> dismissedAlarms = jsonToSet(dismissedAlarmsJSON);
            return dismissedAlarms;
        } catch (JSONException e) {
            Log.w(TAG, "Error getting dismissed alarms", e);
            return new HashSet<>();
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

        Context context = AlarmMorningApplication.getAppContext();
        JSONSharedPreferences.saveJSONArray(context, PERSIST_DISMISSED, new JSONArray(dismissedAlarms));
    }

    /**
     * Decides the state of the alarm.
     * <p>
     * Algorithm:
     * <p>
     * 1. if the alarmTime is for last alarm then return the state of last alarm (same as {@link #getState()})
     * <p>
     * 2. if the alarmTime is in the set of dismissed alarms then return {@link #STATE_DISMISSED}
     * <p>
     * 3. if the alarmTime is in past then return {@link #STATE_DISMISSED}
     * <p>
     * 4. if the alarmTime is in future then return {@link #STATE_FUTURE}
     *
     * @param alarmTime Alarm time
     * @return The state of the alarm
     */
    public int getState(Calendar alarmTime) {
        Log.v(TAG, "getState(alarmTime=" + alarmTime.getTime() + ")");

        // Condition 1
        Calendar stateAlarmTime = getAlarmTimeOfRingingAlarm();

        Log.v(TAG, "   saved state alarm time is " + stateAlarmTime.getTime());

        if (stateAlarmTime.equals(alarmTime)) {
            Log.v(TAG, "   using saved state alarm time");
            return getState();
        }

        // Condition 2
        Set<Long> dismissedAlarms = getDismissedAlarms();
        if (dismissedAlarms.contains(alarmTime.getTimeInMillis())) {
            Log.v(TAG, "   is among dismissed => DISMISSED");
            return STATE_DISMISSED;
        }

        // Condition 3
        if (alarmTime.before(clock().now())) {
            Log.v(TAG, "   is in past => DISMISSED");
            return STATE_DISMISSED;
        } else {
            // Condition 4
            Log.v(TAG, "   is in future => FUTURE");
            return STATE_FUTURE;
        }
    }

    private Set<Long> jsonToSet(JSONArray jsonArray) throws JSONException {
        Set<Long> set = new HashSet<>(jsonArray.length());
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

        Context context = AlarmMorningApplication.getAppContext();
        SystemAlarm systemAlarm = SystemAlarm.getInstance(context);

        SystemAlarmClock systemAlarmClock = SystemAlarmClock.getInstance(context);
        systemAlarmClock.onAlarmSet();

        updateWidget(context);

        Calendar lastAlarmTime = null;

        if (systemAlarm.nextActionShouldChange()) {
            NextAction nextAction = systemAlarm.calcNextAction(clock());
            NextAction nextActionPersisted = getNextAction();

            if (!nextActionPersisted.action.equals(ACTION_UNDEFINED)) {
                Log.w(TAG, "The next system alarm changed while the app was not running.\n" +
                        "   Persisted is action=" + nextActionPersisted.action + ", time=" + nextActionPersisted.time.getTime() + ", alarmTime=" + nextActionPersisted.alarmTime.getTime() + "\n" +
                        "   Current is   action=" + nextAction.action + ", time=" + nextAction.time.getTime() + ", alarmTime=" + nextAction.alarmTime.getTime());
                // e.g. because it was being upgraded or the device was off

                List<Calendar> skippedAlarmTimes = new ArrayList<>();

                if (!isDismissedAny()) {
                    skippedAlarmTimes.add(getAlarmTimeOfRingingAlarm());
                }

                Calendar from = nextActionPersisted.alarmTime;
                from.add(Calendar.SECOND, 1);

                List<Calendar> alarmTimes = getAlarmsInPeriod(from, clock().now());

                skippedAlarmTimes.addAll(alarmTimes);

                if (!skippedAlarmTimes.isEmpty()) {
                    Log.i(TAG, "  The following alarm times were skipped: " + Localization.dateTimesToString(skippedAlarmTimes, context));

                    Analytics analytics = new Analytics(context, Analytics.Event.Skipped_alarm, Analytics.Channel.Time, Analytics.ChannelName.Alarm);
                    analytics.set(Analytics.Param.Skipped_alarm_times, skippedAlarmTimes.toString());
                    analytics.save();

                    SystemNotification systemNotification = SystemNotification.getInstance(context);
                    systemNotification.notifySkippedAlarms(skippedAlarmTimes.size());

                    lastAlarmTime = skippedAlarmTimes.get(skippedAlarmTimes.size() - 1);
                }
            }
        }

        // resume if the previous alarm (e.g. the one that was the last ringing one) is still ringing
        if (isRingingOrSnoozed()) {
            Log.w(TAG, "Previous alarm is still ringing");

            if (inRecentPast(getAlarmTimeOfRingingAlarm(), RECENT_PERIOD)) {
                Log.i(TAG, "Resuming ringing as the previous alarm is recent");

                onRing();

                return;
            } else {
                Log.d(TAG, "Not resuming ringing as the previous alarm is not recent");
            }
        }

        // resume if the last alarm (e.g. the one that was scheduled as last) is recent
        if (lastAlarmTime != null && inRecentPast(lastAlarmTime, RECENT_PERIOD)) {
            Log.i(TAG, "Resuming ringing as the last alarm is recent");

            onRing();

            return;
        }

        onAlarmSetNew(systemAlarm);
    }

    /**
     * Check if the time is in past minutes minutes.
     *
     * @param time    time
     * @param minutes minutes
     * @return true if the time is in the period &lt;now - minutes ; now&gt;
     */
    public boolean inRecentPast(Calendar time, int minutes) {
        Calendar now = clock().now();

        Calendar from = (Calendar) now.clone();
        from.add(Calendar.MINUTE, -minutes);

        return time.after(from) && time.before(now);
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

    public Day loadDay(Calendar date) {
        return dataSource.loadDay(date);
    }

    public void saveDay(Day day, Analytics analytics) {
        Log.d(TAG, "saveDay()");

        Context context = AlarmMorningApplication.getAppContext();
        analytics.setContext(context);
        analytics.setEvent(Analytics.Event.Set_alarm);
        analytics.setDay(day);
        analytics.setDayOld(day);
        analytics.save();

        if (day.getState() == Day.STATE_DISABLED)
            Log.i(TAG, "Disable alarm on " + day.getDateTime().getTime());
        else if (day.getState() == Day.STATE_ENABLED)
            Log.i(TAG, "Set alarm on " + day.getDateTime().getTime());
        else
            Log.i(TAG, "Reverting alarm to default on " + day.getDateTime().getTime());

        dataSource.saveDay(day);

        onAlarmSet();
    }

    public Defaults loadDefault(int dayOfWeek) {
        return dataSource.loadDefault(dayOfWeek);
    }

    public void saveDefault(Defaults defaults, Analytics analytics) {
        Log.d(TAG, "saveDefault()");

        Context context = AlarmMorningApplication.getAppContext();
        analytics.setContext(context);
        analytics.setEvent(Analytics.Event.Set_default);
        analytics.setDefaultsAll(defaults);
        analytics.save();

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

        Context context = AlarmMorningApplication.getAppContext();
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

        Context context = AlarmMorningApplication.getAppContext();

        // register next system alarm
        systemAlarm.onAlarmSet();

        SystemAlarmClock systemAlarmClock = SystemAlarmClock.getInstance(context);
        systemAlarmClock.onAlarmSet();

        updateWidget(context);

        NextAction nextAction = systemAlarm.calcNextAction(clock());
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

        Context context = AlarmMorningApplication.getAppContext();

        Analytics analytics = new Analytics(context, Analytics.Event.Show, Analytics.Channel.Notification, Analytics.ChannelName.Alarm);
        analytics.setDay(getDayWithNextAlarmToRing());
        analytics.save();

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

    public void onDismissBeforeRinging(Analytics analytics) {
        Log.d(TAG, "onDismissBeforeRinging()");

        Context context = AlarmMorningApplication.getAppContext();

        analytics.setContext(context);
        analytics.setEvent(Analytics.Event.Dismiss);
        analytics.set(Analytics.Param.Dismiss_type, Analytics.DISMISS__BEFORE);
        analytics.setDay(getDayWithNextAlarmToRing());
        analytics.save();

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

        // translate to STATE_FUTURE if in the near future
        Day dayWithNextAlarmToRing = getDayWithNextAlarmToRing();
        if (afterNearFuture(dayWithNextAlarmToRing.getDateTime())) {
            Log.i(TAG, "Immediately starting \"alarm in near future\" period.");

            // TODO Start "alarm in near future" period. We cannot handle 1. early dismissed alarm till alarm time and 2. next alarm in near period at the same time, because get/setNextAction can store only one of such alarm. Instead this notification will be displayed at "alarm time of early dismissed alarm" (remove it from there once this is fixed).
            Log.w(TAG, "We should show the \"alarm is near\" notification now. This is not supported. Instead, we show this notification at \"alarm time of early dismissed alarm\".");
        }
    }

    public void onAlarmTimeOfEarlyDismissedAlarm() {
        Log.d(TAG, "onAlarmTimeOfEarlyDismissedAlarm()");

        Context context = AlarmMorningApplication.getAppContext();

        SystemAlarm systemAlarm = SystemAlarm.getInstance(context);
        systemAlarm.onAlarmTimeOfEarlyDismissedAlarm();

        updateCalendarActivity(context, AlarmMorningActivity.ACTION_ALARM_TIME_OF_EARLY_DISMISSED_ALARM);

        // translate to STATE_FUTURE if in the near future
        Day dayWithNextAlarmToRing = getDayWithNextAlarmToRing();
        if (afterNearFuture(dayWithNextAlarmToRing.getDateTime())) {
            Log.i(TAG, "Immediately starting \"alarm in near future\" period.");

            onNearFuture(false);
        }
    }

    public void onRing() {
        Log.d(TAG, "onRing()");

        Context context = AlarmMorningApplication.getAppContext();

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

        Analytics analytics = new Analytics(context, Analytics.Event.Ring, Analytics.Channel.Time, Analytics.ChannelName.Alarm);
        analytics.setDay(getDayWithNextAlarmToRing());
        // TODO Analytics - add number of snoozes
        // TODO Analytics - add device location
        analytics.save();

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

    public void onDismiss(Analytics analytics) {
        Log.d(TAG, "onDismiss()");

        Context context = AlarmMorningApplication.getAppContext();

        analytics.setContext(context);
        analytics.setEvent(Analytics.Event.Dismiss);
        analytics.set(Analytics.Param.Dismiss_type, Analytics.DISMISS__AFTER);
        analytics.setDay(getDayWithNextAlarmToRing());
        analytics.save();

        if (getState() == STATE_SNOOZED) {
            SystemAlarm systemAlarm = SystemAlarm.getInstance(context);
            systemAlarm.onAlarmSet();
        }

        setState(STATE_DISMISSED, getAlarmTimeOfRingingAlarm());
        addDismissedAlarm(getAlarmTimeOfRingingAlarm());

        SystemNotification systemNotification = SystemNotification.getInstance(context);
        systemNotification.onDismiss();

        updateWidget(context);

        updateRingingActivity(context, RingActivity.ACTION_HIDE_ACTIVITY);

        updateCalendarActivity(context, AlarmMorningActivity.ACTION_DISMISS);

        // translate to STATE_FUTURE if in the near future
        if (afterNearFuture()) {
            Log.i(TAG, "Immediately starting \"alarm in near future\" period.");

            onNearFuture(false);
        }
    }

    /**
     * @param analytics Analytics with filled {@link Analytics.Channel} and {@link Analytics.ChannelName} fields. Other fields will be filled by this method.
     * @return Time when the alarm will ring again
     */
    public Calendar onSnooze(Analytics analytics) {
        Log.d(TAG, "onSnooze()");

        Context context = AlarmMorningApplication.getAppContext();

        analytics.setContext(context);
        analytics.setEvent(Analytics.Event.Snooze);
        analytics.setDay(getDayWithNextAlarmToRing());
        // TODO Analytics - add number of snoozes
        analytics.save();

        setState(STATE_SNOOZED, getAlarmTimeOfRingingAlarm());

        Calendar ringAfterSnoozeTime = getRingAfterSnoozeTime(clock());

        SystemAlarm systemAlarm = SystemAlarm.getInstance(context);
        systemAlarm.onSnooze(ringAfterSnoozeTime, getAlarmTimeOfRingingAlarm());

        SystemNotification systemNotification = SystemNotification.getInstance(context);
        systemNotification.onSnooze(ringAfterSnoozeTime);

        updateRingingActivity(context, RingActivity.ACTION_HIDE_ACTIVITY);

        updateCalendarActivity(context, AlarmMorningActivity.ACTION_SNOOZE);

        return ringAfterSnoozeTime;
    }

    /**
     * This event occurs when there is an alarm with state after {@link #STATE_FUTURE} and before state {@link #STATE_DISMISSED} (or {@link
     * #STATE_DISMISSED_BEFORE_RINGING}), that has to be cancelled because an earlier alarm was set by the user.
     */
    public void onAlarmCancel() {
        Log.d(TAG, "onAlarmCancel()");

        Context context = AlarmMorningApplication.getAppContext();

        int state = getState();
        if (state == STATE_SNOOZED || state == STATE_RINGING) {
            Analytics analytics = new Analytics(context, Analytics.Event.Dismiss, Analytics.Channel.Time, Analytics.ChannelName.Alarm);
            analytics.set(Analytics.Param.Dismiss_type, Analytics.DISMISS__AUTO);
            analytics.setDay(getDayWithNextAlarmToRing());
            analytics.save();
            // FIXME Analytics - Situation: Alarm at 9:00 is snoozed. Currently the events are in the order 1. Set alarm to 10:00, 2. Dismiss alarm at 9:00. Expected: The order should be switched. Moreover, currently there is time 10:00 with the Dismiss record, which is not correct as the alarm at 9:00 is dismissed.
        }

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

        Context context = AlarmMorningApplication.getAppContext();
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

    /**
     * Return the identifier of a region that is used to determine holidays.
     *
     * @return the path identifier of a region
     */
    public String loadHoliday() {
        Log.v(TAG, "loadHoliday()");

        Context context = AlarmMorningApplication.getAppContext();

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        String holidayPreference = preferences.getString(SettingsActivity.PREF_HOLIDAY, SettingsActivity.PREF_HOLIDAY_DEFAULT);

        // TODO If the region path does not exist (because the library stopped supporting it or it disappeared) then use the first existing super-region

        return holidayPreference;
    }

    /**
     * Save the identifier of the region that is used to determine holidays.
     *
     * @param holidayPreference the path identifier of a region
     */
    public void saveHoliday(String holidayPreference) {
        Log.d(TAG, "saveHoliday(holidayPreference=" + holidayPreference + ")");

        Context context = AlarmMorningApplication.getAppContext();

        // Save
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = preferences.edit();

        editor.putString(SettingsActivity.PREF_HOLIDAY, holidayPreference);

        editor.commit();

        // Reset alarm
        onAlarmSet();
    }

    /**
     * Return the alarm time.
     *
     * @param clock clock
     * @return next alarm time. Return null if the is no alarm in the next {@link #HORIZON_DAYS} days.
     */
    public Calendar getNextAlarm(Clock clock) {
        Day day = getNextAlarm(clock, null);
        if (day != null) {
            Calendar alarmTime = day.getDateTime();
            Log.v(TAG, "Next alarm is at " + alarmTime.getTime().toString());
            return alarmTime;
        } else {
            Log.v(TAG, "Next alarm is never");
            return null;
        }
    }

    /**
     * Return the nearest Day with alarm such that the Day matches the filter. The filter that such a Day is enabled and not in past is also checked.
     *
     * @param clock clock
     * @return nearest Day with alarm. Return null if the is no alarm in the next {@link #HORIZON_DAYS} days.
     */
    public Day getNextAlarm(Clock clock, DayFilter filter) {
        Calendar date = clock.now();

        for (int daysInAdvance = 0; daysInAdvance < HORIZON_DAYS; daysInAdvance++, date.add(Calendar.DATE, 1)) {
            Day day = loadDay(date);

            if (!day.isEnabled()) {
                continue;
            }

            if (day.isPassed(clock)) {
                continue;
            }

            if (filter != null && !filter.match(day)) {
                continue;
            }

            Log.v(TAG, "   The day that satisfies filter is " + day.getDate().getTime());
            return day;
        }

        Log.v(TAG, "Next alarm is never");
        return null;
    }

    /**
     * Return the alarm times in the specified period.
     *
     * @param from beginning of the period
     * @param to   end of the period
     * @return alarm times, including the borders
     */
    public List<Calendar> getAlarmsInPeriod(Calendar from, Calendar to) {
        Log.d(TAG, "getAlarmsInPeriod(from=" + from.getTime() + ", to=" + to.getTime() + ")");

        List<Calendar> alarmTimes = new ArrayList<>();

        for (Calendar date = (Calendar) from.clone(); date.before(to); date.add(Calendar.DATE, 1)) {
            Day day = loadDay(date);

            if (!day.isEnabled()) {
                continue;
            }

            // handle the alarmTimes on the first (alarmTimes before beginning of period) and last day (after the end of period)
            Calendar alarmTime = day.getDateTime();
            if (alarmTime.before(from))
                continue;
            if (to.before(alarmTime))
                continue;

            alarmTimes.add(alarmTime);
        }

        Log.d(TAG, "   There are " + alarmTimes.size() + " alarmTimes");
        for (Calendar alarmTime : alarmTimes) {
            Log.d(TAG, "   " + alarmTime.getTime());
        }

        return alarmTimes;
    }

}