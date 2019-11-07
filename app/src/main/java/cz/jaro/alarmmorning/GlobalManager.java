package cz.jaro.alarmmorning;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.RemoteViews;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

import androidx.annotation.VisibleForTesting;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import cz.jaro.alarmmorning.calendar.CalendarUtils;
import cz.jaro.alarmmorning.checkalarmtime.CheckAlarmTime;
import cz.jaro.alarmmorning.clock.Clock;
import cz.jaro.alarmmorning.clock.SystemClock;
import cz.jaro.alarmmorning.holiday.HolidayHelper;
import cz.jaro.alarmmorning.model.AlarmDataSource;
import cz.jaro.alarmmorning.model.AppAlarm;
import cz.jaro.alarmmorning.model.AppAlarmFilter;
import cz.jaro.alarmmorning.model.Day;
import cz.jaro.alarmmorning.model.Defaults;
import cz.jaro.alarmmorning.model.OneTimeAlarm;

import static cz.jaro.alarmmorning.SystemAlarm.ACTION_ALARM_TIME_OF_EARLY_DISMISSED_ALARM;
import static cz.jaro.alarmmorning.SystemAlarm.ACTION_RING_IN_NEAR_FUTURE;
import static cz.jaro.alarmmorning.SystemAlarm.ACTION_SET_SYSTEM_ALARM;
import static cz.jaro.alarmmorning.calendar.CalendarUtils.addDay;
import static cz.jaro.alarmmorning.calendar.CalendarUtils.addMilliSeconds;
import static cz.jaro.alarmmorning.calendar.CalendarUtils.addMinutesClone;
import static cz.jaro.alarmmorning.calendar.CalendarUtils.beginningOfToday;
import static cz.jaro.alarmmorning.calendar.CalendarUtils.beginningOfTomorrow;
import static cz.jaro.alarmmorning.calendar.CalendarUtils.onTheSameDate;
import static cz.jaro.alarmmorning.calendar.CalendarUtils.roundDown;

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
 * 4. Widget
 * <p>
 * 5. Ring activity (when alarm is ringing)
 * <p>
 * 6. Calendar activity
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

    private static final String TAG = createLogTag(GlobalManager.class);

    private static final int STATE_UNDEFINED = 0;
    public static final int STATE_FUTURE = 1;
    public static final int STATE_RINGING = 2;
    public static final int STATE_SNOOZED = 3;
    public static final int STATE_DISMISSED = 4;
    public static final int STATE_DISMISSED_BEFORE_RINGING = 5;

    private static final String STRING_STATE_UNDEFINED = "Undefined";
    private static final String STRING_STATE_FUTURE = "Future";
    private static final String STRING_STATE_RINGING = "Ringing";
    private static final String STRING_STATE_SNOOZED = "Snoozed";
    private static final String STRING_STATE_DISMISSED = "Dismissed";
    private static final String STRING_STATE_DISMISSED_BEFORE_RINGING = "Dismissed before ringing";

    private static final String STRING_PERSIST_TYPE = "Type";
    private static final String STRING_PERSIST_ID = "Id";
    private static final String NULL = "null";

    private static final int RECENT_PERIOD = 30; // minutes

    private static GlobalManager instance;

    private final AlarmDataSource dataSource;

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

    /**
     * Excludes today's alarm if it was dismissed (both before and after alarm time). Includes today's alarm that is ringing or snoozed.
     *
     * @return Day with next alarm.
     */
    public AppAlarm getNextAlarmToRing() {
        Log.v(TAG, "getNextAlarmToRing()");

        AppAlarm appAlarm;
        if (isRingingOrSnoozed()) {
            Log.v(TAG, "   loading the ringing or snoozed alarm");

            appAlarm = getRingingAlarm();
        } else {
            appAlarm = getNextAlarm(clock(), appAlarm2 -> {
                Log.v(TAG, "   checking filter condition for " + appAlarm2);
                int state = getState(appAlarm2);
                return state != STATE_DISMISSED_BEFORE_RINGING && state != STATE_DISMISSED;
            });
        }

        return appAlarm;
    }

    /**
     * Excludes today's alarm if it was dismissed (both before and after alarm time) or is ringing or is snoozed.
     *
     * @return Day with next alarm.
     */
    public AppAlarm getNextAlarm() {
        Log.v(TAG, "getNextAlarm()");

        return getNextAlarm(clock(), appAlarm2 -> {
            Log.v(TAG, "   checking filter condition for " + appAlarm2);
            int state = getState(appAlarm2);
            return state != STATE_DISMISSED_BEFORE_RINGING && state != STATE_DISMISSED && state != STATE_RINGING && state != STATE_SNOOZED;
        });
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
        int state = getState();
        return state == STATE_RINGING || state == STATE_SNOOZED;
    }

    public boolean isRinging() {
        int state = getState();
        return state == STATE_RINGING;
    }

    private boolean isDismissedAny() {
        int state = getState();
        return state == STATE_DISMISSED || state == STATE_DISMISSED_BEFORE_RINGING;
    }

    public static Calendar getResetTime(Calendar now) {
        return beginningOfTomorrow(now);
    }

    public static boolean useNearFutureTime() {
        int nearFutureMinutes = (int) SharedPreferencesHelper.load(SettingsActivity.PREF_NEAR_FUTURE_TIME, SettingsActivity.PREF_NEAR_FUTURE_TIME_DEFAULT);

        return 0 < nearFutureMinutes;
    }

    public static Calendar getNearFutureTime(Calendar alarmTime) {
        int nearFutureMinutes = (int) SharedPreferencesHelper.load(SettingsActivity.PREF_NEAR_FUTURE_TIME, SettingsActivity.PREF_NEAR_FUTURE_TIME_DEFAULT);

        Calendar nearFutureTime = (Calendar) alarmTime.clone();
        nearFutureTime.add(Calendar.MINUTE, -nearFutureMinutes);

        return nearFutureTime;
    }

    private boolean afterBeginningOfNearFuturePeriod() {
        try {
            NextAction nextAction = getNextAction();
            return nextAction.appAlarm != null && afterBeginningOfNearFuturePeriod(nextAction.appAlarm.getDateTime());
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    boolean afterBeginningOfNearFuturePeriod(Calendar alarmTime) {
        Calendar now = clock().now();

        Calendar nearFutureTime = getNearFutureTime(alarmTime);
        return now.after(nearFutureTime);
    }

    boolean inNearFuturePeriod(Calendar alarmTime) {
        Calendar now = clock().now();

        Calendar nearFutureTime = getNearFutureTime(alarmTime);
        return now.after(nearFutureTime) && now.before(alarmTime);
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
    public static final String PERSIST_ALARM_TYPE = "persist_alarm_type";
    public static final String PERSIST_ALARM_ID = "persist_alarm_id";

    /*
     * Contains info about the last alarm. Last alarm is the one that runs, possibly is snoozed and was dismissed or cancelled.
     */
    private static final String PERSIST_LAST_STATE = "persist_last_state";
    private static final String PERSIST_LAST_ALARM_TYPE = "persist_last_alarm_type";
    private static final String PERSIST_LAST_ALARM_ID = "persist_last_alarm_id";
    private static final String PERSIST_LAST_RING_AFTER_SNOOZE_TIME = "persist_last_ring_after_snooze_time";
    private static final String PERSIST_LAST_SNOOZE_COUNT = "persist_last_snooze_count";

    /*
     * Contains info about the dismissed alarms.
     */
    private static final String PERSIST_DISMISSED = "persist_dismissed_2"; // There vas a change in format in versionCode = 15, and we don't want to use one key with different formats. Therefore we use the suffix number.

    // Persisted next action
    // =====================

    NextAction getNextAction() throws IllegalArgumentException {
        Log.v(TAG, "getNextAction()");

        try {
            String action = (String) SharedPreferencesHelper.load(PERSIST_ACTION);

            long timeInMS = (long) SharedPreferencesHelper.load(PERSIST_TIME);
            Calendar time = CalendarUtils.newGregorianCalendar(timeInMS);

            String alarmType = (String) SharedPreferencesHelper.load(PERSIST_ALARM_TYPE);
            AppAlarm appAlarm;
            if (alarmType.equals(NULL)) {
                appAlarm = null;
            } else {
                String alarmId = (String) SharedPreferencesHelper.load(PERSIST_ALARM_ID);
                appAlarm = load(alarmType, alarmId);
            }

            return new NextAction(action, time, appAlarm);
        } catch (NoSuchElementException e) {
            throw new IllegalArgumentException("The persisted data is incomplete", e);
        }
    }

    void setNextAction(NextAction nextAction) {
        Log.v(TAG, "setNextAction(action=" + nextAction.action + ", time=" + nextAction.time.getTime() + ", appAlarm=" + nextAction.appAlarm + ")");

        SharedPreferencesHelper.save(PERSIST_ACTION, nextAction.action);
        SharedPreferencesHelper.save(PERSIST_TIME, nextAction.time.getTimeInMillis());
        if (nextAction.appAlarm == null) {
            SharedPreferencesHelper.save(PERSIST_ALARM_TYPE, NULL);
        } else {
            SharedPreferencesHelper.save(PERSIST_ALARM_TYPE, nextAction.appAlarm.getClass().getSimpleName());
            SharedPreferencesHelper.save(PERSIST_ALARM_ID, nextAction.appAlarm.getPersistenceId());
        }
    }

    // Persisted state
    // ===============

    /**
     * Returns the state of the persisted alarm.
     * <p>
     * Warning: this method does not consider the set of dismissed alarms. In practice, the logic should also consider {@link #isDismissedAlarm(AppAlarm)}.
     *
     * @return State
     */
    private int getState() {
        Log.v(TAG, "getState()");

        try {
            return (int) SharedPreferencesHelper.load(PERSIST_LAST_STATE);
        } catch (NoSuchElementException e) {
            return STATE_UNDEFINED;
        }
    }

    public AppAlarm getRingingAlarm() {
        Log.v(TAG, "getRingingAlarm()");

        try {
            String alarmType = (String) SharedPreferencesHelper.load(PERSIST_LAST_ALARM_TYPE);
            String alarmId = (String) SharedPreferencesHelper.load(PERSIST_LAST_ALARM_ID);

            return load(alarmType, alarmId);
        } catch (NoSuchElementException e) {
            Log.v(TAG, "The persisted data is incomplete", e);
            return null;
        } catch (IllegalArgumentException e) {
            Log.v(TAG, "Cannot load ringing alarm", e);
            return null;
        }
    }

    private void setState(int state, AppAlarm appAlarm) {
        Log.v(TAG, "setState(state=" + state + " (" + stateToString(state) + "), appAlarm=" + appAlarm + ")");

        SharedPreferencesHelper.save(PERSIST_LAST_STATE, state);
        SharedPreferencesHelper.save(PERSIST_LAST_ALARM_TYPE, appAlarm.getClass().getSimpleName());
        if (appAlarm instanceof Day) {
            Day day = (Day) appAlarm;
            SharedPreferencesHelper.save(PERSIST_LAST_ALARM_ID, Analytics.calendarToStringDate(day.getDate()));
        } else if (appAlarm instanceof OneTimeAlarm) {
            OneTimeAlarm oneTimeAlarm = (OneTimeAlarm) appAlarm;
            SharedPreferencesHelper.save(PERSIST_LAST_ALARM_ID, String.valueOf(oneTimeAlarm.getId()));
        } else {
            throw new IllegalArgumentException("Unexpected class " + appAlarm.getClass());
        }
    }

    private void saveRingAfterSnoozeTime(Calendar ringAfterSnoozeTime) {
        Log.v(TAG, "saveRingAfterSnoozeTime(ringAfterSnoozeTime=" + ringAfterSnoozeTime + ")");

        String ringAfterSnoozeTimeStr = Analytics.calendarToDatetimeStringUTC(ringAfterSnoozeTime);

        SharedPreferencesHelper.save(PERSIST_LAST_RING_AFTER_SNOOZE_TIME, ringAfterSnoozeTimeStr);
    }

    public Calendar loadRingAfterSnoozeTime() {
        Log.v(TAG, "loadRingAfterSnoozeTime()");

        String ringAfterSnoozeTimeStr = (String) SharedPreferencesHelper.load(PERSIST_LAST_RING_AFTER_SNOOZE_TIME);

        return Analytics.datetimeUTCStringToCalendar(ringAfterSnoozeTimeStr);
    }

    private void zeroSnoozeCount() {
        Log.v(TAG, "zeroSnoozeCount()");

        saveSnoozeCount(0);
    }

    private long increaseSnoozeCount() {
        Log.v(TAG, "increaseSnoozeCount()");

        long snoozeCount = loadSnoozeCount();
        snoozeCount++;

        saveSnoozeCount(snoozeCount);

        return snoozeCount;
    }

    private void saveSnoozeCount(long snoozeCount) {
        Log.v(TAG, "saveSnoozeCount()");

        SharedPreferencesHelper.save(PERSIST_LAST_SNOOZE_COUNT, snoozeCount);

    }

    private long loadSnoozeCount() {
        Log.v(TAG, "loadSnoozeCount()");

        return (long) SharedPreferencesHelper.load(PERSIST_LAST_SNOOZE_COUNT);
    }

    /**
     * Returns the alarm times of dismissed alarms.
     *
     * @return Alarm times of dismissed alarms.
     */
    public Set<AppAlarm> getDismissedAlarms() {
        Log.v(TAG, "getDismissedAlarm()");

        try {
            JSONArray jsonArray = JSONSharedPreferences.loadJSONArray(PERSIST_DISMISSED);

            Set<AppAlarm> dismissedAlarms = new HashSet<>(jsonArray.length());
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject obj = (JSONObject) jsonArray.get(i);

                String alarmType = obj.getString(STRING_PERSIST_TYPE);
                String alarmId = obj.getString(STRING_PERSIST_ID);

                AppAlarm dimissedAlarm = load(alarmType, alarmId);

                if (dimissedAlarm != null) // This  is needed as an dissabled one-time alarm may already be removed
                    dismissedAlarms.add(dimissedAlarm);
            }

            return dismissedAlarms;
        } catch (JSONException e) {
            Log.w(TAG, "Error getting dismissed alarms", e);
            return new HashSet<>();
        }
    }

    /**
     * Checks whether the there is dismissed alarm at a particular alarm time.
     * <p>
     * This is determined based on the set of dismissed alarm times.
     *
     * @param appAlarm An alarm
     * @return True if an alarm was dismissed at the alarm time.
     */
    public boolean isDismissedAlarm(AppAlarm appAlarm) {
        Set<AppAlarm> dismissedAlarms = getDismissedAlarms();
        return dismissedAlarms.contains(appAlarm);
    }

    interface SetOperation {
        void modify(Set<AppAlarm> dismissedAlarms);
    }

    private static <T extends Comparable<? super T>> List<T> asSortedList(Collection<T> c) {
        List<T> list = new ArrayList<>(c);
        java.util.Collections.sort(list);
        return list;
    }

    private void modifyDismissedAlarm(SetOperation setOperation) {
        Log.v(TAG, "modifyDismissedAlarm()");

        Set<AppAlarm> dismissedAlarms = getDismissedAlarms();

        // Remove elements for yesterday and before
        Calendar beginningOfToday = beginningOfToday(clock().now());
        for (Iterator<AppAlarm> iterator = dismissedAlarms.iterator(); iterator.hasNext(); ) {
            AppAlarm dismissedAlarm = iterator.next();
            if (dismissedAlarm.getDateTime().before(beginningOfToday)) {
                Log.d(TAG, "Removing an old dismissed alarm from the set of dismissed alarms: " + dismissedAlarm);
                iterator.remove();
            }
        }

        // Modify set
        setOperation.modify(dismissedAlarms);

        // Log
        List<AppAlarm> sortedDismissedAlarms = asSortedList(dismissedAlarms);
        Log.v(TAG, "There are " + dismissedAlarms.size() + " dismissed alarms at");
        for (AppAlarm appAlarm : sortedDismissedAlarms) {
            Log.v(TAG, "   " + appAlarm);
        }

        try {
            // Create the object to persist
            JSONArray jsonArray = new JSONArray();
            for (AppAlarm dismissedAlarm : sortedDismissedAlarms) {
                JSONObject obj = new JSONObject();
                obj.put(STRING_PERSIST_TYPE, dismissedAlarm.getClass().getSimpleName());
                obj.put(STRING_PERSIST_ID, dismissedAlarm.getPersistenceId());

                jsonArray.put(jsonArray.length(), obj);
            }

            // Save
            JSONSharedPreferences.saveJSONArray(PERSIST_DISMISSED, jsonArray);
        } catch (JSONException e) {
            Log.w(TAG, "Cannot convert to JSON, therefore cannot store dismissed alarms", e);
        }
    }

    public void addDismissedAlarm(AppAlarm appAlarm) {
        Log.v(TAG, "addDismissedAlarm(appAlarm=" + appAlarm + ")");

        modifyDismissedAlarm(dismissedAlarms ->
                dismissedAlarms.add(appAlarm)
        );
    }

    private void removeDismissedAlarm(AppAlarm appAlarm) {
        Log.v(TAG, "removeDismissedAlarm(appAlarm=" + appAlarm + ")");

        modifyDismissedAlarm(dismissedAlarms -> {
                    if (dismissedAlarms.contains(appAlarm)) {
                        Log.d(TAG, "Removing a dismissed alarm from the set of dismissed alarms: " + appAlarm);
                        dismissedAlarms.remove(appAlarm);
                    }
                }
        );
    }

    /**
     * Decides the state of the alarm.
     * <p>
     * Algorithm: For the given alarm...
     * <p>
     * 1. if the alarm is the ringing alarm then return the state of last alarm (same as {@link #getState()}); but if the alarm was dismissed then
     * return {@link #STATE_DISMISSED} or {@link #STATE_DISMISSED_BEFORE_RINGING} depending on the current time.
     * <p>
     * 2. if the alarm is in the set of dismissed alarms then return {@link #STATE_DISMISSED} or {@link #STATE_DISMISSED_BEFORE_RINGING} depending on the
     * current time.
     * <p>
     * 3. if the alarm is in the past then return {@link #STATE_DISMISSED}
     * <p>
     * 4. if the alarm is in the future then return {@link #STATE_FUTURE}
     *
     * @param appAlarm An alarm
     * @return The state of the alarm
     */
    public int getState(AppAlarm appAlarm) {
        Log.v(TAG, "getState(appAlarm=" + appAlarm + ")");

        // Condition 1
        AppAlarm ringingAlarm = getRingingAlarm();

        Log.v(TAG, "   saved alarm time is " + ringingAlarm);

        if (ringingAlarm != null &&
                (appAlarm instanceof Day && ringingAlarm instanceof Day && (ringingAlarm.getDate()).equals(appAlarm.getDate())) ||
                (appAlarm instanceof OneTimeAlarm && ringingAlarm instanceof OneTimeAlarm && (((OneTimeAlarm) ringingAlarm).getId() == ((OneTimeAlarm) appAlarm).getId()))
        ) {
            if (isDismissedAlarm(appAlarm)) {
                if (appAlarm.getDateTime().before(clock().now())) {
                    Log.v(TAG, "   is ringing & is among dismissed & is in past => DISMISSED");
                    return STATE_DISMISSED;
                } else {
                    Log.v(TAG, "   is ringing & is among dismissed is in future => STATE_DISMISSED_BEFORE_RINGING");
                    return STATE_DISMISSED_BEFORE_RINGING;
                }
            }

            Log.v(TAG, "   using saved state alarm time");

            return getState();
        }

        // Condition 2
        Set<AppAlarm> dismissedAlarms = getDismissedAlarms();
        if (dismissedAlarms.contains(appAlarm)) {
            if (appAlarm.getDateTime().before(clock().now())) {
                Log.v(TAG, "   is among dismissed & is in past => DISMISSED");
                return STATE_DISMISSED;
            } else {
                Log.v(TAG, "   is among dismissed is in future => STATE_DISMISSED_BEFORE_RINGING");
                return STATE_DISMISSED_BEFORE_RINGING;
            }
        }

        // Condition 3
        if (appAlarm.getDateTime().before(clock().now())) {
            Log.v(TAG, "   is in past => DISMISSED");
            return STATE_DISMISSED;
        } else {
            // Condition 4
            Log.v(TAG, "   is in future => FUTURE");
            return STATE_FUTURE;
        }
    }

    /*
     * External events
     * ===============
     */

    /**
     * This method registers system alarm. If a system alarm is registered, it is canceled first.
     * <p/>
     * This method should be called on external events. Such events are application start after booting or upgrading, time (and time zone) change.
     * <p/>
     * This method should NOT be called when user sets the alarm time. Instead, call {@link #onAlarmSet()}.
     */
    public void firstSetAlarm() {
        Log.d(TAG, "firstSetAlarm()");

        Context context = AlarmMorningApplication.getAppContext();
        SystemAlarm systemAlarm = SystemAlarm.getInstance(context);
        Calendar now = clock().now();

        // Step 1: Do all the logic

        AppAlarm ringingAlarm = getRingingAlarm();
        boolean isRingingOrSnoozed = ringingAlarm != null && isRingingOrSnoozed();
        boolean isSnoozed = !isRinging();

        List<AppAlarm> alarmsInPeriod; // between current alarm (excluding it) and now (including it)
        try {
            NextAction nextActionPersisted = getNextAction();

            Calendar from = (Calendar) nextActionPersisted.appAlarm.getDateTime().clone();
            if (nextActionPersisted.action.equals(SystemAlarm.ACTION_RING)) // If the current alarm is ringing then it is handled in the ringingAlarm varible above. Here we want to include the "ring in near future" alarm.
                addMilliSeconds(from, 1);

            alarmsInPeriod = getAlarmsInPeriod(from, now);
        } catch (IllegalArgumentException | NullPointerException e) {
            alarmsInPeriod = new ArrayList<>();
        }

        boolean beforeAutoDismissTime = alarmsInPeriod.isEmpty()
                && isRingingOrSnoozed
                && !RingActivity.doAutoDismiss(ringingAlarm);
        boolean ringingAlarmWillNormallyResumeAfterSnoozing = beforeAutoDismissTime
                && isSnoozed
                && now.before(loadRingAfterSnoozeTime());
        boolean isRingingAlarmSkipped = isRingingOrSnoozed && !ringingAlarmWillNormallyResumeAfterSnoozing;

        // Step 2: Show notification
        boolean sbowNotification = isRingingAlarmSkipped || !alarmsInPeriod.isEmpty();
        if (sbowNotification) {
            List<AppAlarm> notificationAlarms = new ArrayList<>();
            if (isRingingAlarmSkipped)
                notificationAlarms.add(ringingAlarm);
            notificationAlarms.addAll(alarmsInPeriod);

            Log.i(TAG, "There were " + notificationAlarms.size() + " skipped alarms");

            JSONArray skippedAlarmsJSON = new JSONArray();
            for (AppAlarm skippedAlarm : notificationAlarms) {
                try {
                    JSONObject conf = new JSONObject();
                    conf.put("alarmTime", skippedAlarm.getDateTime().getTime().toString());
                    if (skippedAlarm instanceof OneTimeAlarm) {
                        OneTimeAlarm oneTimeAlarm = (OneTimeAlarm) skippedAlarm;
                        conf.put("name", oneTimeAlarm.getName());
                    }
                    skippedAlarmsJSON.put(conf);
                } catch (JSONException e) {
                    skippedAlarmsJSON.put("Error: " + e.getMessage());
                }
            }
            Analytics analytics = new Analytics(context, Analytics.Event.Skipped_alarm, Analytics.Channel.Time, Analytics.ChannelName.Alarm);
            analytics.set(Analytics.Param.Skipped_alarm_times, skippedAlarmsJSON.toString());
            analytics.save();

            SystemNotification systemNotification = SystemNotification.getInstance(context);
            systemNotification.notifySkippedAlarms(notificationAlarms);
        }

        // Step 3: Resume ringing, schedule system alarm

        // Resume if the ringing alarm is still active (ringing or snoozed).
        // This covers the case when the device restarts while alarm is ringing.
        boolean resumeRingingAlarm = alarmsInPeriod.isEmpty() && beforeAutoDismissTime;
        if (resumeRingingAlarm) {
            if (isRinging()) {
                Log.i(TAG, "Resuming ringing the ringing alarm");
                onRing(ringingAlarm, true);
            } else { // is snoozed
                if (!ringingAlarmWillNormallyResumeAfterSnoozing) {
                    Log.i(TAG, "Resuming ringing the snoozed alarm as it's after the snooze time");
                    onRing(ringingAlarm, true);
                } else {
                    Log.i(TAG, "Resuming: The snoozed alarm will ring after the snooze time");

                    Calendar ringAfterSnoozeTime = loadRingAfterSnoozeTime();

                    systemAlarm.onSnooze(ringAfterSnoozeTime);

                    AppAlarm nextAlarmToRing = getNextAlarmToRing();
                    SystemNotification systemNotification = SystemNotification.getInstance(context);
                    systemNotification.onSnooze(nextAlarmToRing, ringAfterSnoozeTime);
                }
            }
            return;
        }

        // Resume if the last alarm (e.g. the one that was scheduled as last) is recent.
        // This covers the case then the device was off at the alarm time, but the device (and app) started shortly afterwards.
        boolean resumeLastAlarm = !alarmsInPeriod.isEmpty();
        if (resumeLastAlarm) {
            AppAlarm lastAlarm = alarmsInPeriod.get(alarmsInPeriod.size() - 1);
            boolean lastAlarmIsRecent = inRecentPast(lastAlarm.getDateTime());
            if (lastAlarmIsRecent) {
                Log.i(TAG, "Resuming ringing as the last alarm is recent");

                onRing(lastAlarm, true);

                return;
            }
        }

        // Set next (future) alarm
        Log.i(TAG, "Resuming: setting next alarm");
        onAlarmSetNew(systemAlarm);
    }

    void onDateChange() {
        Log.d(TAG, "onDateChanged()");

        Context context = AlarmMorningApplication.getAppContext();

        SystemAlarm systemAlarm = SystemAlarm.getInstance(context);
        systemAlarm.onDateChange();
    }

    /**
     * Check if the time is in past {@code minutes} minutes.
     *
     * @param time time
     * @return true if the time is in the period &lt;now - minutes ; now&gt;
     */
    private boolean inRecentPast(Calendar time) {
        Calendar now = clock().now();

        Calendar from = addMinutesClone(now, -GlobalManager.RECENT_PERIOD);

        return time.after(from) && time.before(now);
    }

    /*
     * Database (storing & loading objects)
     * ====================================
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

    public Defaults loadDefault(int dayOfWeek) {
        return dataSource.loadDefault(dayOfWeek);
    }

    /**
     * Returns the one-time alarm with a given identifier.
     *
     * @param id Id of the one-time alarm
     * @return Return one-time alarm with the given Id.
     */
    private OneTimeAlarm loadOneTimeAlarm(Long id) {
        return dataSource.loadOneTimeAlarm(id);
    }

    /**
     * Returns the list of one-time alarms.
     *
     * @return List of one-time alarms.
     */
    public List<OneTimeAlarm> loadOneTimeAlarms() {
        return dataSource.loadOneTimeAlarms();
    }

    /**
     * Returns the list of one-time alarms.
     *
     * @param from If not null, then only one-time alarms with alarm time on or after {@code from} are returned.
     * @param to   If not null, then only one-time alarms with alarm time before {@code to} are returned.
     * @return List of one-time alarms.
     */
    public List<OneTimeAlarm> loadOneTimeAlarms(Calendar from, Calendar to) {
        return dataSource.loadOneTimeAlarms(from, to);
    }

    private void save(OneTimeAlarm oneTimeAlarm, Analytics analytics) {
        Context context = AlarmMorningApplication.getAppContext();
        analytics.setContext(context);
        analytics.setEvent(Analytics.Event.Set_alarm);
        analytics.setOneTimeAlarm(oneTimeAlarm);
        analytics.save();

        Log.i(TAG, "Save one-time alarm at " + oneTimeAlarm.getDateTime().getTime().toString());

        dataSource.saveOneTimeAlarm(oneTimeAlarm);

        // Delete old one-time alarms
        Calendar to = clock().now();
        CalendarUtils.addDays(to, -7);
        dataSource.deleteOneTimeAlarmsOlderThan(to);
    }

    private void remove(OneTimeAlarm oneTimeAlarm, Analytics analytics) {
        Context context = AlarmMorningApplication.getAppContext();
        analytics.setContext(context);
        analytics.setEvent(Analytics.Event.Dismiss);
        analytics.setOneTimeAlarm(oneTimeAlarm);
        analytics.save();

        Log.i(TAG, "Delete one-time alarm at " + oneTimeAlarm.getDateTime());

        dataSource.deleteOneTimeAlarm(oneTimeAlarm);
    }

    /*
     * Events relevant to next alarm
     * =============================
     */

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
            try {
                onAlarmCancel(getNextAction().appAlarm);
            } catch (IllegalArgumentException e) {
                Log.d(TAG, "There is no persisted alarm to cancel", e);
            }

            onAlarmSetNew(systemAlarm);
        }

        CheckAlarmTime checkAlarmTime = CheckAlarmTime.getInstance(context);
        checkAlarmTime.onAlarmSet();

        updateCalendarActivity(context, AlarmMorningActivity.ACTION_ALARM_SET, null);
    }

    private void onAlarmSetNew(SystemAlarm systemAlarm) {
        Log.d(TAG, "onAlarmSetNew()");

        Context context = AlarmMorningApplication.getAppContext();

        // register next system alarm
        systemAlarm.onAlarmSet();

        SystemAlarmClock systemAlarmClock = SystemAlarmClock.getInstance(context);
        systemAlarmClock.onAlarmSet();

        updateWidget(context);

        NextAction nextAction = systemAlarm.calcNextAction();
        switch (nextAction.action) {
            case ACTION_SET_SYSTEM_ALARM:
            case ACTION_RING_IN_NEAR_FUTURE:
            case ACTION_ALARM_TIME_OF_EARLY_DISMISSED_ALARM:
                // nothing
                break;
            case SystemAlarm.ACTION_RING:
                if (useNearFutureTime()) {
                    onNearFuture(nextAction.appAlarm, false, true);
                }
                break;
            default:
                throw new IllegalArgumentException("Unexpected argument " + nextAction);
        }
    }

    public void onNearFuture(AppAlarm appAlarm) {
        Log.d(TAG, "onNearFuture(appAlarm=" + appAlarm + ")");

        onNearFuture(appAlarm, true, false);
    }

    /**
     * Translate state to {@link #STATE_FUTURE}.
     *
     * @param callSystemAlarm If true then call {@link SystemAlarm#onNearFuture(AppAlarm)}, otherwise ship the call.
     * @param force           If false then considers a ringing or snoozed alarm: if there is a ringing or snoozed alarm then do nothing.
     */
    private void onNearFuture(AppAlarm appAlarm, boolean callSystemAlarm, boolean force) {
        Log.d(TAG, "onNearFuture(callSystemAlarm=" + callSystemAlarm + ")");

        Context context = AlarmMorningApplication.getAppContext();

        Analytics analytics = new Analytics(context, Analytics.Event.Show, Analytics.Channel.Notification, Analytics.ChannelName.Alarm);
        analytics.setAppAlarm(appAlarm);
        analytics.save();

        if (!force && isRingingOrSnoozed()) {
            Log.i(TAG, "The previous alarm is still ringing. Ignoring this event.");

            if (callSystemAlarm) {
                SystemAlarm systemAlarm = SystemAlarm.getInstance(context);
                systemAlarm.onNearFuture(appAlarm);
            }

            return;
        }

        setState(STATE_FUTURE, appAlarm);

        if (callSystemAlarm) {
            SystemAlarm systemAlarm = SystemAlarm.getInstance(context);
            systemAlarm.onNearFuture(appAlarm);
        }

        SystemNotification systemNotification = SystemNotification.getInstance(context);
        systemNotification.onNearFuture(appAlarm);
    }

    /**
     * Dismiss the alarm.
     * <p>
     * Automatically chooses between calling {@link #onDismiss(AppAlarm, Analytics)} and {@link #onDismissBeforeRinging(AppAlarm, Analytics)}.
     *
     * @param appAlarm  The alarm to be dismissed
     * @param analytics Analytics
     */
    public void onDismissAny(AppAlarm appAlarm, Analytics analytics) {
        Log.d(TAG, "onDismissAny(appAlarm=" + appAlarm + ")");

        Calendar now = clock().now();

        if (now.after(appAlarm.getDateTime())) {
            onDismiss(appAlarm, analytics);
        } else {
            onDismissBeforeRinging(appAlarm, analytics);
        }
    }

    public void onDismissBeforeRinging(AppAlarm appAlarm, Analytics analytics) {
        Log.d(TAG, "onDismissBeforeRinging(appAlarm=" + appAlarm + ")");

        setState(STATE_DISMISSED_BEFORE_RINGING, appAlarm);
        addDismissedAlarm(appAlarm);

        Context context = AlarmMorningApplication.getAppContext();

        analytics.setContext(context);
        analytics.setEvent(Analytics.Event.Dismiss);
        analytics.set(Analytics.Param.Dismiss_type, Analytics.DISMISS__BEFORE);
        analytics.setAppAlarm(appAlarm);
        analytics.save();

        SystemAlarm systemAlarm = SystemAlarm.getInstance(context);
        systemAlarm.onDismissBeforeRinging();

        SystemAlarmClock systemAlarmClock = SystemAlarmClock.getInstance(context);
        systemAlarmClock.onDismissBeforeRinging();

        SystemNotification systemNotification = SystemNotification.getInstance(context);
        systemNotification.onDismissBeforeRinging(appAlarm);

        updateWidget(context);

        updateCalendarActivity(context, AlarmMorningActivity.ACTION_DISMISS_BEFORE_RINGING, appAlarm);

        // Translate to STATE_FUTURE if in the near future
        AppAlarm nextAlarmToRing = getNextAlarmToRing();
        if (nextAlarmToRing != null && afterBeginningOfNearFuturePeriod(nextAlarmToRing.getDateTime())) {
            Log.i(TAG, "Immediately starting \"alarm in near future\" period.");

            onNearFuture(nextAlarmToRing, false, false);
        }
    }

    public void onAlarmTimeOfEarlyDismissedAlarm(AppAlarm appAlarm) {
        Log.d(TAG, "onAlarmTimeOfEarlyDismissedAlarm(appAlarm=" + appAlarm + ")");

        Context context = AlarmMorningApplication.getAppContext();

        SystemAlarm systemAlarm = SystemAlarm.getInstance(context);
        systemAlarm.onAlarmTimeOfEarlyDismissedAlarm();

        updateCalendarActivity(context, AlarmMorningActivity.ACTION_ALARM_TIME_OF_EARLY_DISMISSED_ALARM, appAlarm);

        // translate to STATE_FUTURE if in the near future
        AppAlarm nextAlarmToRing = getNextAlarmToRing();
        if (nextAlarmToRing != null && afterBeginningOfNearFuturePeriod(nextAlarmToRing.getDateTime())) {
            Log.i(TAG, "Immediately starting \"alarm in near future\" period.");

            onNearFuture(nextAlarmToRing, false, false);
        }
    }

    public void onRing(AppAlarm appAlarm) {
        onRing(appAlarm, false);
    }

    private void onRing(AppAlarm appAlarm, boolean ignoreCancelledAlarm) {
        Log.d(TAG, "onRing(appAlarm=" + appAlarm + ")");

        Context context = AlarmMorningApplication.getAppContext();

        boolean isNew; // This is the first time the alarm rings, otherwise the alarm is resumed after snoozing
        try {
            NextAction nextAction = getNextAction();
            isNew = !nextAction.time.after(nextAction.appAlarm.getDateTime());
        } catch (IllegalArgumentException e) {
            Log.d(TAG, "Cannot get nextAction", e);
            isNew = true;
        }

        if (isRingingOrSnoozed() && isNew && !ignoreCancelledAlarm) { // Another alarm is still ringing
            Log.i(TAG, "The previous alarm is still ringing. Cancelling it.");

            AppAlarm ringingAlarm = getRingingAlarm();

            setState(STATE_DISMISSED, ringingAlarm);

            SystemNotification systemNotification = SystemNotification.getInstance(context);
            systemNotification.notifyCancelledAlarm(ringingAlarm);
        }

        setState(STATE_RINGING, appAlarm);

        if (isNew) {
            zeroSnoozeCount();
        }

        Analytics analytics = new Analytics(context, Analytics.Event.Ring, Analytics.Channel.Time, Analytics.ChannelName.Alarm);
        analytics.setAppAlarm(appAlarm);
        analytics.set(Analytics.Param.Snooze_count, loadSnoozeCount());
        analytics.save();

        SystemAlarm systemAlarm = SystemAlarm.getInstance(context);
        systemAlarm.onRing();

        SystemNotification systemNotification = SystemNotification.getInstance(context);
        systemNotification.onRing(appAlarm);

        if (isNew) {
            SystemAlarmClock systemAlarmClock = SystemAlarmClock.getInstance(context);
            systemAlarmClock.onRing();

            updateWidget(context);
        }

        // If there is another alarm scheduled at the same time, then cancel it
        List<OneTimeAlarm> oneTimeAlarms = loadOneTimeAlarms();
        for (OneTimeAlarm oneTimeAlarm : oneTimeAlarms) {
            if (!appAlarm.equals(oneTimeAlarm) && oneTimeAlarm.getDateTime().equals(appAlarm.getDateTime())) {
                addDismissedAlarm(oneTimeAlarm);
                systemNotification.notifyCancelledAlarm(oneTimeAlarm);
            }
        }

        startRingingActivity(context);

        updateCalendarActivity(context, AlarmMorningActivity.ACTION_RING, appAlarm);
    }

    public void onDismiss(AppAlarm appAlarm, Analytics analytics) {
        Log.d(TAG, "onDismiss()");

        Context context = AlarmMorningApplication.getAppContext();

        analytics.setContext(context);
        analytics.setEvent(Analytics.Event.Dismiss);
        analytics.set(Analytics.Param.Dismiss_type, Analytics.DISMISS__AFTER);
        analytics.setAppAlarm(appAlarm);
        analytics.save();

        if (getState(appAlarm) == STATE_SNOOZED) {
            SystemAlarm systemAlarm = SystemAlarm.getInstance(context);
            systemAlarm.onAlarmSet();
        }

        setState(STATE_DISMISSED, appAlarm);
        addDismissedAlarm(appAlarm);

        SystemNotification systemNotification = SystemNotification.getInstance(context);
        systemNotification.onDismiss(appAlarm);

        updateWidget(context);

        updateRingingActivity(context, RingActivity.ACTION_HIDE_ACTIVITY, appAlarm);

        updateCalendarActivity(context, AlarmMorningActivity.ACTION_DISMISS, appAlarm);

        // translate to STATE_FUTURE if in the near future
        if (afterBeginningOfNearFuturePeriod()) {
            Log.i(TAG, "Immediately starting \"alarm in near future\" period.");

            NextAction nextAction = getNextAction();
            onNearFuture(nextAction.appAlarm, false, false);
        }
    }

    /**
     * @param appAlarm  Alarm to be snoozed.
     * @param analytics Analytics with filled {@link Analytics.Channel} and {@link Analytics.ChannelName} fields. Other fields will be filled by this method.
     * @return Time when the alarm will ring again
     */
    public Calendar onSnooze(AppAlarm appAlarm, Analytics analytics) {
        Log.d(TAG, "onSnooze()");

        int snoozeTime = (int) SharedPreferencesHelper.load(SettingsActivity.PREF_SNOOZE_TIME, SettingsActivity.PREF_SNOOZE_TIME_DEFAULT);

        return onSnooze(appAlarm, snoozeTime, analytics);
    }

    /**
     * @param appAlarm  Alarm to be snoozed.
     * @param minutes   For how many minutes will the alarm be snoozed.
     * @param analytics Analytics with filled {@link Analytics.Channel} and {@link Analytics.ChannelName} fields. Other fields will be filled by this method.
     * @return Time when the alarm will ring again
     */
    public Calendar onSnooze(AppAlarm appAlarm, int minutes, Analytics analytics) {
        Log.d(TAG, "onSnooze(appAlarm=" + appAlarm.getPersistenceId() + ", minutes=" + minutes + ")");

        Context context = AlarmMorningApplication.getAppContext();

        AppAlarm nextAlarmToRing = getNextAlarmToRing();

        analytics.setContext(context);
        analytics.setEvent(Analytics.Event.Snooze);
        analytics.setAppAlarm(nextAlarmToRing);
        analytics.set(Analytics.Param.Snooze_count, increaseSnoozeCount());
        analytics.save();

        setState(STATE_SNOOZED, getRingingAlarm());

        Calendar ringAfterSnoozeTime = getRingAfterSnoozeTime(clock(), minutes);
        saveRingAfterSnoozeTime(ringAfterSnoozeTime);

        SystemAlarm systemAlarm = SystemAlarm.getInstance(context);
        systemAlarm.onSnooze(ringAfterSnoozeTime);

        SystemNotification systemNotification = SystemNotification.getInstance(context);
        systemNotification.onSnooze(nextAlarmToRing, ringAfterSnoozeTime);

        updateRingingActivity(context, RingActivity.ACTION_HIDE_ACTIVITY, appAlarm);

        updateCalendarActivity(context, AlarmMorningActivity.ACTION_SNOOZE, appAlarm);

        return ringAfterSnoozeTime;
    }

    /**
     * This event occurs when there is an alarm with state after {@link #STATE_FUTURE} and before state {@link #STATE_DISMISSED} (or {@link
     * #STATE_DISMISSED_BEFORE_RINGING}), that has to be cancelled because an earlier alarm was set by the user.
     */
    private void onAlarmCancel(AppAlarm appAlarm) {
        Log.d(TAG, "onAlarmCancel()");

        Context context = AlarmMorningApplication.getAppContext();

        if (isRingingOrSnoozed()) {
            /* Note for Analytics - Situation: Alarm at 9:00 is snoozed and set at 10:00. Currently the events are in the order:
                 1. Set alarm to 10:00.
                 2. Dismiss alarm at 9:00.
               Naturally, order should be switched. But it is implemented this way. */
            Analytics analytics = new Analytics(context, Analytics.Event.Dismiss, Analytics.Channel.External, Analytics.ChannelName.Alarm);
            analytics.set(Analytics.Param.Dismiss_type, Analytics.DISMISS__AUTO);
            analytics.setAppAlarm(appAlarm);
            analytics.save();
        }

        SystemNotification systemNotification = SystemNotification.getInstance(context);
        systemNotification.onAlarmCancel(appAlarm);

        SystemAlarmClock systemAlarmClock = SystemAlarmClock.getInstance(context);
        systemAlarmClock.onAlarmCancel();

        updateRingingActivity(context, RingActivity.ACTION_HIDE_ACTIVITY, appAlarm);

        updateCalendarActivity(context, AlarmMorningActivity.ACTION_CANCEL, appAlarm);
    }

    /*
     * Actions relevant to alarm management
     * ====================================
     */

    public void modifyDayAlarm(Day day, Analytics analytics) {
        Log.d(TAG, "modifyDayAlarm()");

        Context context = AlarmMorningApplication.getAppContext();
        analytics.setContext(context);
        analytics.setEvent(Analytics.Event.Set_alarm);
        analytics.setDay(day);
        analytics.setDayOld(day);
        analytics.save();

        switch (day.getState()) {
            case Day.STATE_DISABLED:
                Log.i(TAG, "Disable alarm on " + day);
                break;
            case Day.STATE_ENABLED:
                Log.i(TAG, "Set alarm on " + day);
                break;
            case Day.STATE_RULE:
                Log.i(TAG, "Reverting alarm to default on " + day);
                break;
            default:
                throw new IllegalArgumentException("Unexpected state " + day.getState());
        }

        // If the modified alarm is ringing or snoozed, then adjust state
        if (isRingingOrSnoozed()) {
            AppAlarm nextAlarmToRing = getNextAlarmToRing();
            if (nextAlarmToRing instanceof Day) {
                Day dayOld = (Day) nextAlarmToRing;
                if (onTheSameDate(day.getDate(), dayOld.getDate())) {
                    Calendar now = clock().now();
                    if (now.before(day.getDateTime())) {
                        setState(STATE_FUTURE, dayOld);
                    } else {
                        setState(STATE_DISMISSED, dayOld);
                        addDismissedAlarm(dayOld);
                    }
                }
            }
        }

        dataSource.saveDay(day);

        updateCalendarActivity(context, AlarmMorningActivity.EVENT_MODIFY_DAY_ALARM, day);

        onAlarmSet();
    }

    public void modifyDefault(Defaults defaults, Analytics analytics) {
        Log.d(TAG, "modifyDefault()");

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
     * Create a new one-time alarm
     *
     * @param oneTimeAlarm One-time alarm to create.
     * @param analytics    Analytics
     */
    public void createOneTimeAlarm(OneTimeAlarm oneTimeAlarm, Analytics analytics) {
        Log.d(TAG, "createOneTimeAlarm()");

        save(oneTimeAlarm, analytics);

        Context context = AlarmMorningApplication.getAppContext();
        updateCalendarActivity(context, AlarmMorningActivity.EVENT_CREATE_ONE_TIME_ALARM, oneTimeAlarm);

        onAlarmSet();
    }

    /**
     * Delete an one-time alarm.
     *
     * @param oneTimeAlarm One-time alarm to delete.
     * @param analytics    Analytics
     */
    public void deleteOneTimeAlarm(OneTimeAlarm oneTimeAlarm, Analytics analytics) {
        Log.d(TAG, "deleteOneTimeAlarm()");

        // If the modified alam is ringing or snooze, then dismiss it
        if (isRingingOrSnoozed()) {
            AppAlarm nextAlarmToRing = getNextAlarmToRing();
            if (nextAlarmToRing instanceof OneTimeAlarm) {
                OneTimeAlarm oneTimeAlarmOld = (OneTimeAlarm) nextAlarmToRing;
                if (oneTimeAlarm.getId() == oneTimeAlarmOld.getId()) {
                    Log.i(TAG, "Dismissing (before actual delete) the ringing one-time alarm at " + oneTimeAlarm.getDateTime());
                    setState(STATE_DISMISSED, oneTimeAlarm);
                    addDismissedAlarm(oneTimeAlarm);
                }
            }
        }

        remove(oneTimeAlarm, analytics);

        Context context = AlarmMorningApplication.getAppContext();

        SystemNotification systemNotification = SystemNotification.getInstance(context);
        systemNotification.onDeleteOneTimeAlarm(oneTimeAlarm);

        updateWidget(context);

        updateCalendarActivity(context, AlarmMorningActivity.EVENT_DELETE_ONE_TIME_ALARM, oneTimeAlarm);

        onAlarmSet();
    }

    /**
     * Modify the date or time of an one-time alarm.
     *
     * @param oneTimeAlarm The modified one-time alarm.
     * @param analytics    Analytics
     */
    public void modifyOneTimeAlarmDateTime(OneTimeAlarm oneTimeAlarm, Analytics analytics) {
        Log.d(TAG, "modifyOneTimeAlarmDateTime()");

        setState(STATE_FUTURE, oneTimeAlarm);

        save(oneTimeAlarm, analytics);

        removeDismissedAlarm(oneTimeAlarm);

        Context context = AlarmMorningApplication.getAppContext();

        SystemNotification systemNotification = SystemNotification.getInstance(context);
        systemNotification.onModifyOneTimeAlarmDateTime(oneTimeAlarm);

        updateCalendarActivity(context, AlarmMorningActivity.EVENT_MODIFY_ONE_TIME_ALARM_DATETIME, oneTimeAlarm);

        onAlarmSet();

        // If the the datetime changed to past then properly dismiss the alarm
        Calendar now = clock().now();
        if (oneTimeAlarm.getDateTime().before(now)) {
            Analytics analytics2 = new Analytics(Analytics.Channel.Time, Analytics.ChannelName.Alarm);
            onDismiss(oneTimeAlarm, analytics2);
        }
    }

    /**
     * Modify the name of an one-time alarm.
     *
     * @param oneTimeAlarm The modified one-time alarm.
     * @param analytics    Analytics
     */
    public void modifyOneTimeAlarmName(OneTimeAlarm oneTimeAlarm, Analytics analytics) {
        Log.d(TAG, "modifyOneTimeAlarmName()");

        save(oneTimeAlarm, analytics);

        Context context = AlarmMorningApplication.getAppContext();

        SystemNotification systemNotification = SystemNotification.getInstance(context);
        systemNotification.onModifyOneTimeAlarmName(oneTimeAlarm);

        updateCalendarActivity(context, AlarmMorningActivity.EVENT_MODIFY_ONE_TIME_ALARM_NAME, oneTimeAlarm);
    }

    /*
     * Actions
     * =======
     */

    public Calendar getRingAfterSnoozeTime(Clock clock) {
        Log.d(TAG, "getRingAfterSnoozeTime()");

        int snoozeTime = (int) SharedPreferencesHelper.load(SettingsActivity.PREF_SNOOZE_TIME, SettingsActivity.PREF_SNOOZE_TIME_DEFAULT);

        return getRingAfterSnoozeTime(clock, snoozeTime);
    }

    private Calendar getRingAfterSnoozeTime(Clock clock, int minutes) {
        Log.d(TAG, "getRingAfterSnoozeTime(minutes=" + minutes + ")");

        Calendar ringAfterSnoozeTime = addMinutesClone(clock.now(), minutes);
        roundDown(ringAfterSnoozeTime, Calendar.SECOND);

        return ringAfterSnoozeTime;
    }

    private void updateWidget(Context context) {
        Log.d(TAG, "updateWidget()");

        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_layout);
        WidgetProvider.updateContent(context, views);
        appWidgetManager.updateAppWidget(new ComponentName(context, WidgetProvider.class), views);
    }

    void startRingingActivity(Context context) {
        Log.d(TAG, "startRingingActivity()");

        Intent ringIntent = new Intent(context, RingActivity.class);
        ringIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        AppAlarm ringingAlarm = getRingingAlarm();
        ringIntent.putExtra(PERSIST_ALARM_TYPE, ringingAlarm.getClass().getSimpleName());
        ringIntent.putExtra(PERSIST_ALARM_ID, ringingAlarm.getPersistenceId());
        context.startActivity(ringIntent);
    }

    private void updateRingingActivity(Context context, String action, AppAlarm appAlarm) {
        Log.d(TAG, "updateRingingActivity(action=" + action + ")");

        Intent intent = new Intent(context, RingActivity.class);
        intent.setAction(action);
        if (appAlarm != null) {
            intent.putExtra(PERSIST_ALARM_TYPE, appAlarm.getClass().getSimpleName());
            intent.putExtra(PERSIST_ALARM_ID, appAlarm.getPersistenceId());
        }
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }

    private void updateCalendarActivity(Context context, String action, AppAlarm appAlarm) {
        Log.d(TAG, "updateCalendarActivity(action=" + action + ", appAlarm = " + appAlarm + ")");

        Intent intent = new Intent(context, AlarmMorningActivity.class);
        intent.setAction(action);
        if (appAlarm != null) {
            intent.putExtra(PERSIST_ALARM_TYPE, appAlarm.getClass().getSimpleName());
            intent.putExtra(PERSIST_ALARM_ID, appAlarm.getPersistenceId());
        }
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }

    /**
     * Return the identifier of a region that is used to determine holidays.
     *
     * @return the path identifier of a region
     */
    public String loadHoliday() {
        Log.v(TAG, "loadHoliday()");

        String holidayPreference;
        try {
            holidayPreference = (String) SharedPreferencesHelper.load(SettingsActivity.PREF_HOLIDAY);
        } catch (NoSuchElementException e) {
            holidayPreference = SettingsActivity.PREF_HOLIDAY_DEFAULT;
        }

        // If the region path does not exist (because the library stopped supporting it or it disappeared) then use the first existing super-region
        HolidayHelper holidayHelper = HolidayHelper.getInstance();
        holidayPreference = holidayHelper.findExistingParent(holidayPreference);

        return holidayPreference;
    }

    /**
     * Save the identifier of the region that is used to determine holidays.
     *
     * @param holidayPreference the path identifier of a region
     */
    public void saveHoliday(String holidayPreference) {
        Log.d(TAG, "saveHoliday(holidayPreference=" + holidayPreference + ")");

        // Save
        SharedPreferencesHelper.save(SettingsActivity.PREF_HOLIDAY, holidayPreference);

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
        AppAlarm appAlarm = getNextAlarm(clock, null);
        if (appAlarm != null) {
            Calendar alarmTime = appAlarm.getDateTime();
            Log.v(TAG, "Next alarm is at " + alarmTime.getTime().toString());
            return alarmTime;
        } else {
            Log.v(TAG, "Next alarm is never");
            return null;
        }
    }

    /**
     * Return the nearest {@link AppAlarm} with alarm such it matches the filter. Only the Days that are enabled and not in the past are considered, and all the
     * one-time alarms that are not in the past are considered.
     * <p>
     * Note that there may be multiple alarms at the same date and time (one Day alarm + several one-time alarms). Only one (the first found)
     * alarm is returned. This is OK because the important thing is the date and time of next alarm (and not the count or type).
     *
     * @param clock Clock
     * @return Nearest alarm. Return null if the is no Day alarm in the next {@link #HORIZON_DAYS} days or no one-time alarm in the (unlimited) future.
     */
    public AppAlarm getNextAlarm(Clock clock, AppAlarmFilter filter) {
        Day day = getNextAlarmDay(clock, filter);
        OneTimeAlarm oneTimeAlarm = getNextAlarmOneTimeAlarm(clock, filter);

        AppAlarm getNextAlarm;
        if (day == null) {
            getNextAlarm = oneTimeAlarm;
        } else {
            if (oneTimeAlarm == null) {
                getNextAlarm = day;
            } else {
                getNextAlarm = day.getDateTime().before(oneTimeAlarm.getDateTime()) ? day : oneTimeAlarm;
            }
        }

        if (getNextAlarm != null) {
            Log.v(TAG, "   Next alarm is at " + getNextAlarm.getDateTime().getTime().toString());
        } else {
            Log.v(TAG, "   Next alarm is never");
        }
        return getNextAlarm;
    }

    /**
     * Return the nearest Day with alarm such that the Day matches the filter. The filter that such a Day is enabled and not in past is also checked.
     *
     * @param clock clock
     * @return nearest Day with alarm. Return null if the is no alarm in the next {@link #HORIZON_DAYS} days.
     */
    private Day getNextAlarmDay(Clock clock, AppAlarmFilter filter) {
        Calendar date = clock.now();

        for (int daysInAdvance = 0; daysInAdvance < HORIZON_DAYS; daysInAdvance++, addDay(date)) {
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

            Log.v(TAG, "   The day that satisfies filter is " + day);
            return day;
        }

        Log.v(TAG, "   No next alarm defined by Days and Defaults");
        return null;
    }

    private OneTimeAlarm getNextAlarmOneTimeAlarm(Clock clock, AppAlarmFilter filter) {
        OneTimeAlarm nextOneTimeAlarm = null;

        Calendar now = clock.now();
        List<OneTimeAlarm> oneTimeAlarms = loadOneTimeAlarms(now, null);

        for (OneTimeAlarm oneTimeAlarm : oneTimeAlarms) {
            if (oneTimeAlarm.isPassed(clock)) {
                continue;
            }

            if (filter != null && !filter.match(oneTimeAlarm)) {
                continue;
            }

            if (nextOneTimeAlarm == null || oneTimeAlarm.getDateTime().before(nextOneTimeAlarm.getDateTime())) {
                nextOneTimeAlarm = oneTimeAlarm;
            }
        }

        if (nextOneTimeAlarm == null) {
            Log.v(TAG, "   No next alarm defined by one-time alarms");
        } else {
            Log.v(TAG, "   The one-time alarm that satisfies filter is " + nextOneTimeAlarm.getDateTime().getTime());
        }

        return nextOneTimeAlarm;
    }

    /**
     * Return the alarm times in the specified period.
     *
     * @param from beginning of the period
     * @param to   end of the period
     * @return alarm times, including the borders
     */
    public List<AppAlarm> getAlarmsInPeriod(Calendar from, Calendar to) {
        Log.d(TAG, "getAlarmsInPeriod(from=" + from.getTime() + ", to=" + to.getTime() + ")");

        List<AppAlarm> appAlarms = new ArrayList<>();

        if (!from.before(to))
            return appAlarms;

        // Add day alarms

        for (Calendar date = (Calendar) from.clone(); date.before(to); addDay(date)) {
            Day day = loadDay(date);

            if (!day.isEnabled()) {
                continue;
            }

            // Handle the alarm times on the first (alarm time before beginning of period) and last day (after the end of period)
            Calendar alarmTime = day.getDateTime();
            if (alarmTime.before(from))
                continue;
            if (to.before(alarmTime))
                continue;

            appAlarms.add(day);
        }

        // Add one-time alarms
        List<OneTimeAlarm> oneTimeAlarms = loadOneTimeAlarms(from, to);
        appAlarms.addAll(oneTimeAlarms);

        return appAlarms;
    }

    public AppAlarm load(String alarmType, String alarmId) throws IllegalArgumentException {
        if (alarmType.equals(Day.class.getSimpleName())) {
            Calendar date = Analytics.dateStringToCalendar(alarmId);
            return loadDay(date);
        } else if (alarmType.equals(OneTimeAlarm.class.getSimpleName())) {
            return loadOneTimeAlarm(Long.decode(alarmId));
        } else {
            throw new IllegalArgumentException("Unexpected class " + alarmType);
        }
    }

    private static String stateToString(int state) {
        switch (state) {
            case STATE_UNDEFINED:
                return STRING_STATE_UNDEFINED;
            case STATE_FUTURE:
                return STRING_STATE_FUTURE;
            case STATE_RINGING:
                return STRING_STATE_RINGING;
            case STATE_SNOOZED:
                return STRING_STATE_SNOOZED;
            case STATE_DISMISSED:
                return STRING_STATE_DISMISSED;
            case STATE_DISMISSED_BEFORE_RINGING:
                return STRING_STATE_DISMISSED_BEFORE_RINGING;
            default:
                throw new IllegalStateException("Unsupported state " + state);
        }
    }

    /**
     * Reset all the data (database and settings (including the data stored by GlobalManager)) to the initial state.
     */
    @VisibleForTesting
    public void reset() {
        resetDatabase();
        resetSettings();
        onAlarmSet();
    }

    /**
     * Reset the database to the initial state.
     */
    private void resetDatabase() {
        dataSource.resetDatabase();
    }

    /**
     * Reset the settings to the defaults and remove the data stored by GlobalManager.
     * <p>
     * Note: this just rests the settings, but the subsequent action is not triggered by this method. E.g. when resetting the holiday, the alarm is not reset
     * (in case the next day was holiday).
     */
    private void resetSettings() {
        Context context = AlarmMorningApplication.getAppContext();

        // Clear preferences
        SharedPreferencesHelper.clear();

        // Set defaults
        PreferenceManager.setDefaultValues(context, R.xml.preferences, true);
    }

    /**
     * Returns the content of the database.
     *
     * @return Content of the database.
     */
    String dumpDB() {
        return dataSource.dumpDB();
    }

    public static String createLogTag(Class c) {
        return c.getSimpleName() + "[AM]";
    }

}