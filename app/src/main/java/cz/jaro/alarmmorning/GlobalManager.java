package cz.jaro.alarmmorning;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.util.Calendar;

import cz.jaro.alarmmorning.model.AlarmDataSource;
import cz.jaro.alarmmorning.model.Day;

/**
 * Created by jmalenko on 19.1.2016.
 */
public class GlobalManager {

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

        Calendar alarmTime = AlarmDataSource.getNextAlarm(context);
        setAlarmTime(alarmTime.getTimeInMillis());
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
        Calendar alarmTime = AlarmDataSource.getNextAlarm(context);
        long alarmTime1 = alarmTime.getTimeInMillis();

        long alarmTime2 = getAlarmTime();

        return alarmTime1 == alarmTime2;
    }

    // TODO Fix: when ringing continues to next day
    // TODO Fix: when ringing overlaps with next alarm

    public Day getDay() {
        Log.d(TAG, "getDay()");
        AlarmDataSource datasource = new AlarmDataSource(context);
        datasource.open();

        Calendar date = CalendarAdapter.getToday();
        Day day = datasource.loadDay(date);

        datasource.close();

        return day;
    }

    /**
     * Do the following on each event:
     * 1. Set state
     * 2. Register next system alarm
     * 3. Handle notification
     * 4. Handle ring activity
     * 5. Handle calendar activity
     */
    public void onNearFuture() {
        Log.d(TAG, "onNearFuture()");

        setState(STATE_FUTURE);

        SystemNotification.onNearFuture(context);
    }

    public void onDismissBeforeRinging() {
        Log.d(TAG, "onDismissBeforeRinging()");

        setState(STATE_DISMISSED_BEFORE_RINGING);

        SystemAlarm systemAlarm = SystemAlarm.getInstance(context);
        systemAlarm.onDismissBeforeRinging();

        SystemNotification.onDismissBeforeRinging(context);

        updateCalendarActivityOnDismissBeforeChange(context);
    }

    public void onRing() {
        Log.d(TAG, "onRing()");

        setState(STATE_RINGING);

        SystemNotification.onRing(context);

        startRingingActivity(context);
    }

    public void onDismiss() {
        Log.d(TAG, "onDismiss()");

        setState(STATE_DISMISSED);

        SystemNotification.onDismiss(context);

        hideRingingActivity(context);
    }

    public void onSnooze() {
        Log.d(TAG, "onSnooze()");

        setState(STATE_SNOOZED);

        SystemAlarm systemAlarm = SystemAlarm.getInstance(context);
        Calendar ringAfterSnoozeTime = systemAlarm.onSnooze();

        SystemNotification.onSnooze(context, ringAfterSnoozeTime);

        hideRingingActivity(context);
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

    private void updateCalendarActivityOnDismissBeforeChange(Context context) {
        Log.d(TAG, "updateCalendarActivity()");

        Intent hideIntent = new Intent();
        hideIntent.setClassName("cz.jaro.alarmmorning", "cz.jaro.alarmmorning.AlarmMorningActivity");
        hideIntent.setAction(AlarmMorningActivity.ACTION_DISMISS_BEFORE_RINGING);
        LocalBroadcastManager.getInstance(context).sendBroadcast(hideIntent);
    }

}