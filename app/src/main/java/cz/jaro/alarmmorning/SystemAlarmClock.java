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

        reset();
    }

    public void onDismissBeforeRinging() {
        Log.d(TAG, "onDismissBeforeRinging()");

        reset();
    }

    public void onRing() {
        Log.d(TAG, "onRing()");

        reset();
    }

    public void onAlarmCancel() {
        Log.d(TAG, "onAlarmCancel()");

        reset();
    }

    private void reset() {
        Log.v(TAG, "reset()");

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            cancel();

            register();

            print();
        } else {
            Log.d(TAG, "The system alarm clock is not supported on this Android version");
        }
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

        GlobalManager globalManager = GlobalManager.getInstance();
        Day day = globalManager.getDayWithNextAlarmToRing();

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
}
