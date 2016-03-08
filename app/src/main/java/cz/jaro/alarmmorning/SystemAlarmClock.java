package cz.jaro.alarmmorning;

import android.annotation.TargetApi;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import java.util.Calendar;
import java.util.GregorianCalendar;

import cz.jaro.alarmmorning.clock.Clock;
import cz.jaro.alarmmorning.clock.SystemClock;
import cz.jaro.alarmmorning.model.AlarmDataSource;
import cz.jaro.alarmmorning.model.Day;
import cz.jaro.alarmmorning.receivers.VoidReceiver;

/**
 * The SystemAlarmClock handles the Android Alarm Clock features.
 */
public class SystemAlarmClock {

    private static final String TAG = SystemAlarmClock.class.getSimpleName();

    private static final int REQUEST_CODE_INTENT_ACTION = 1;
    private static final int REQUEST_CODE_INTENT_SHOW = 2;

    private static SystemAlarmClock instance;

    private Context context;
    private AlarmManager alarmManager;

    private SystemAlarmClock(Context context) {
        this.context = context;
        alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
    }

    public static SystemAlarmClock getInstance(Context context) {
        if (instance == null) {
            instance = new SystemAlarmClock(context);
        }
        return instance;
    }

    /*
     * Events
     * ======
     */

    public void onAlarmSet() {
        Log.d(TAG, "onAlarmSet()");

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            reset();
        } else {
            Log.d(TAG, "The system alarm clock is not supported on this Android version");
        }
    }

    public void onDismissBeforeRinging() {
        Log.d(TAG, "onDismissBeforeRinging()");

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            reset();
        } else {
            Log.d(TAG, "The system alarm clock is not supported on this Android version");
        }
    }

    public void onRing() {
        Log.d(TAG, "onRing()");

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            reset();
        } else {
            Log.d(TAG, "The system alarm clock is not supported on this Android version");
        }
    }

    public void onAlarmCancel() {
        Log.d(TAG, "onAlarmCancel()");

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            reset();
        } else {
            Log.d(TAG, "The system alarm clock is not supported on this Android version");
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void reset() {
        Log.v(TAG, "reset()");

        cancel();

        register();

        print();
    }

    private void cancel() {
        Log.v(TAG, "cancel()");

        Intent intent = new Intent(context, VoidReceiver.class);
        PendingIntent operation = PendingIntent.getBroadcast(context, REQUEST_CODE_INTENT_ACTION, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        alarmManager.cancel(operation);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void register() {
        Log.v(TAG, "register()");
        Day day = calcNextAlarm();

        if (day != null) {
            Calendar alarmTime = day.getDateTime();

            Intent intent = new Intent(context, VoidReceiver.class);
            PendingIntent operation = PendingIntent.getBroadcast(context, REQUEST_CODE_INTENT_ACTION, intent, PendingIntent.FLAG_UPDATE_CURRENT);

            Intent showIntent = new Intent(context, AlarmMorningActivity.class);
            PendingIntent showOperation = PendingIntent.getBroadcast(context, REQUEST_CODE_INTENT_SHOW, showIntent, PendingIntent.FLAG_UPDATE_CURRENT);

            AlarmManager.AlarmClockInfo alarmClockInfo = new AlarmManager.AlarmClockInfo(alarmTime.getTimeInMillis(), showOperation);
            alarmManager.setAlarmClock(alarmClockInfo, operation);
        } else {
            Log.v(TAG, "   no next alarm");
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void print() {
        AlarmManager.AlarmClockInfo alarmClockInfo = alarmManager.getNextAlarmClock();
        if (alarmClockInfo == null) {
            Log.v(TAG, "   system alarm clock is not set");
        } else {
            Calendar t = new GregorianCalendar();
            t.setTimeInMillis(alarmClockInfo.getTriggerTime());
            Log.v(TAG, "   system alarm clock is at " + t.getTime());
        }
    }

    // TODO Solve dependency on clock
    private Clock clock() {
        return new SystemClock();
    }

    // TODO Refactoring - similar to CalendarFragment.calcPositionNextAlarm()
    private Day calcNextAlarm() {
        Calendar today = clock().now();

        AlarmDataSource dataSource = new AlarmDataSource(context);
        dataSource.open();

        for (int position = 0; position < AlarmDataSource.HORIZON_DAYS; position++) {

            Calendar date = CalendarFragment.addDays(today, position);

            Day day = dataSource.loadDayDeep(date);

            if (day.isEnabled() && !day.isPassed(clock())) {
                GlobalManager globalManager = new GlobalManager(context);
                int state = globalManager.getState(day.getDateTime());
                if (state != GlobalManager.STATE_UNDEFINED) {
                    if (state != GlobalManager.STATE_DISMISSED_BEFORE_RINGING && state != GlobalManager.STATE_DISMISSED) {
                        dataSource.close();
                        return day;
                    }
                } else {
                    dataSource.close();
                    return day;
                }
            }
        }

        dataSource.close();
        return null;
    }

}
