package cz.jaro.alarmmorning;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.util.Calendar;

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
 * 2. There is no way to get the alarm broadcast registered by this app (in the minimal state described above). Therefore, the reference must be kept here (to
 * allow its cancellation when user set earlier alarm). That is realized by methods {@link #getAlarmTime()} and {@link #setAlarmTime(long)} which use the value
 * stored in {@code SharedPreferences}.
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
     */

    private static final String TAG = GlobalManager.class.getSimpleName();

    public static final String TODAY_ALARM_STATE = "today_alarm_state";
    private static final String TODAY_ALARM_TIME = "today_alarm_time";

    public static final int STATE_UNDEFINED = 0;
    public static final int STATE_FUTURE = 1;
    public static final int STATE_RINGING = 2;
    public static final int STATE_SNOOZED = 3;
    public static final int STATE_DISMISSED = 4;
    public static final int STATE_DISMISSED_BEFORE_RINGING = 5;

    private static final long TIME_UNDEFINED = -1;

    private Context context;

    // TODO Resume ringing if the app was upgraded while ringing
    // TODO Resume ringing if the operating system restarted while ringing

    public GlobalManager(Context context) {
        this.context = context;
    }

    /*
     * State of the app
     * ================
     */

    public int getState() {
        Log.v(TAG, "getState()");
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        int state = preferences.getInt(TODAY_ALARM_STATE, STATE_UNDEFINED);

        Log.v(TAG, "   state=" + state);
        return state;
    }

    public void setState(int state) {
        Log.d(TAG, "setState(state=" + state + ")");

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt(TODAY_ALARM_STATE, state);
        editor.commit();

        Clock clock = new SystemClock(); // TODO change
        Calendar alarmTime = AlarmDataSource.getNextAlarm(context, clock);
        if (alarmTime != null) {
            setAlarmTime(alarmTime.getTimeInMillis());
        }
    }

    private long getAlarmTime() {
        Log.v(TAG, "getAlarmTime()");
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        long alarmTime = preferences.getLong(TODAY_ALARM_TIME, TIME_UNDEFINED);

        return alarmTime;
    }

    private void setAlarmTime(long alarmTime) {
        Log.d(TAG, "setAlarmTime(alarmTime=" + alarmTime + ")");
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putLong(TODAY_ALARM_TIME, alarmTime);
        editor.commit();
    }

    public boolean isValid() {
        Clock clock = new SystemClock(); // TODO change

        long alarmTime1 = getAlarmTime();

        Calendar alarmTime = AlarmDataSource.getNextAlarm(context, clock);
        if (alarmTime == null) {
            return alarmTime1 == TIME_UNDEFINED;
        } else {
            long alarmTime2 = alarmTime.getTimeInMillis();

            return alarmTime1 == alarmTime2;
        }
    }

    // TODO Fix: when ringing continues to next day
    // TODO Fix: when ringing overlaps with next alarm

    public Day getDay() {
        Log.d(TAG, "getDay()");
        AlarmDataSource dataSource = new AlarmDataSource(context);
        dataSource.open();

        Clock clock = new SystemClock();
        Calendar date = CalendarAdapter.getToday(clock);
        Day day = dataSource.loadDayDeep(date);

        dataSource.close();

        return day;
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
        systemAlarm.onAlarmSet();

        SystemNotification systemNotification = SystemNotification.getInstance(context);
        systemNotification.onAlarmSet(context);
    }

    public void onNearFuture() {
        Log.d(TAG, "onNearFuture()");

        setState(STATE_FUTURE);

        SystemAlarm systemAlarm = SystemAlarm.getInstance(context);
        systemAlarm.onNearFuture();

        SystemNotification systemNotification = SystemNotification.getInstance(context);
        systemNotification.onNearFuture(context);
    }

    public void onDismissBeforeRinging() {
        Log.d(TAG, "onDismissBeforeRinging()");

        setState(STATE_DISMISSED_BEFORE_RINGING);

        SystemAlarm systemAlarm = SystemAlarm.getInstance(context);
        systemAlarm.onDismissBeforeRinging();

        SystemNotification systemNotification = SystemNotification.getInstance(context);
        systemNotification.onDismissBeforeRinging(context);

        updateCalendarActivityOnDismissBeforeChange(context);
    }

    public void onAlarmTimeOfEarlyDismissedAlarm() {
        Log.d(TAG, "onAlarmTimeOfEarlyDismissedAlarm()");

        SystemAlarm systemAlarm = SystemAlarm.getInstance(context);
        systemAlarm.onAlarmTimeOfEarlyDismissedAlarm();

        updateCalendarActivityOnAlarmTimeOfEarlyDismissedAlarm(context);
    }

    public void onRing() {
        Log.d(TAG, "onRing()");

        setState(STATE_RINGING);

        SystemAlarm systemAlarm = SystemAlarm.getInstance(context);
        systemAlarm.onRing();

        SystemNotification systemNotification = SystemNotification.getInstance(context);
        systemNotification.onRing(context);

        startRingingActivity(context);
    }

    public void onDismiss() {
        Log.d(TAG, "onDismiss()");

        setState(STATE_DISMISSED);

        SystemNotification systemNotification = SystemNotification.getInstance(context);
        systemNotification.onDismiss(context);

        hideRingingActivity(context);
    }

    public void onSnooze() {
        Log.d(TAG, "onSnooze()");

        setState(STATE_SNOOZED);

        Calendar ringAfterSnoozeTime = ringAfterSnoozeTime();

        SystemAlarm systemAlarm = SystemAlarm.getInstance(context);
        systemAlarm.onSnooze(ringAfterSnoozeTime);

        SystemNotification systemNotification = SystemNotification.getInstance(context);
        systemNotification.onSnooze(context, ringAfterSnoozeTime);

        hideRingingActivity(context);
    }

    /*
     * Actions
     * =======
     */

    public Calendar ringAfterSnoozeTime() {
        Log.d(TAG, "ringAfterSnoozeTime()");

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        int snoozeTime = preferences.getInt(SettingsFragment.PREF_SNOOZE_TIME, SettingsFragment.PREF_SNOOZE_TIME_DEFAULT);

        Clock clock = new SystemClock(); // TODO change
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
        context.startActivity(ringIntent);
    }

    private void hideRingingActivity(Context context) {
        Log.d(TAG, "hideRingingActivity()");

        Intent hideIntent = new Intent();
        hideIntent.setClassName("cz.jaro.alarmmorning", "cz.jaro.alarmmorning.RingActivity");
        hideIntent.setAction(RingActivity.ACTION_HIDE_ACTIVITY);
        LocalBroadcastManager.getInstance(context).sendBroadcast(hideIntent);
    }

    private void updateCalendarActivityOnAlarmTimeOfEarlyDismissedAlarm(Context context) {
        Log.d(TAG, "updateCalendarActivityOnAlarmTimeOfEarlyDismissedAlarm()");

        Intent hideIntent = new Intent();
        hideIntent.setClassName("cz.jaro.alarmmorning", "cz.jaro.alarmmorning.AlarmMorningActivity");
        hideIntent.setAction(AlarmMorningActivity.ACTION_ALARM_TIME_OF_EARLY_DISMEISSED_ALARM);
        LocalBroadcastManager.getInstance(context).sendBroadcast(hideIntent);
    }

    private void updateCalendarActivityOnDismissBeforeChange(Context context) {
        Log.d(TAG, "updateCalendarActivityOnDismissBeforeChange()");

        Intent hideIntent = new Intent();
        hideIntent.setClassName("cz.jaro.alarmmorning", "cz.jaro.alarmmorning.AlarmMorningActivity");
        hideIntent.setAction(AlarmMorningActivity.ACTION_DISMISS_BEFORE_RINGING);
        LocalBroadcastManager.getInstance(context).sendBroadcast(hideIntent);
    }

}