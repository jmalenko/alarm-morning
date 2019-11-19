package cz.jaro.alarmmorning;

import android.annotation.TargetApi;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import java.util.Calendar;

import cz.jaro.alarmmorning.calendar.CalendarUtils;
import cz.jaro.alarmmorning.model.AppAlarm;
import cz.jaro.alarmmorning.receivers.VoidReceiver;

/**
 * The SystemAlarmClock handles the Android Alarm Clock features.
 */
public class SystemAlarmClock {

    private static final int REQUEST_CODE_INTENT_ACTION = 1;
    private static final int REQUEST_CODE_INTENT_SHOW = 2;

    private static SystemAlarmClock instance;

    private final Context context;
    private final AlarmManager alarmManager;

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

    void onAlarmSet() {
        MyLog.d("onAlarmSet()");

        reset();
    }

    void onDismissBeforeRinging() {
        MyLog.d("onDismissBeforeRinging()");

        reset();
    }

    void onRing() {
        MyLog.d("onRing()");

        reset();
    }

    void onAlarmCancel() {
        MyLog.d("onAlarmCancel()");

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            cancel();
        }
    }

    /*
     * Other
     * =====
     */

    private void reset() {
        MyLog.v("reset()");

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            cancel();

            register();

            print();
        } else {
            MyLog.d("The system alarm clock is not supported on this Android version");
        }
    }

    private void cancel() {
        MyLog.v("cancel()");

        Intent intent = new Intent(context, VoidReceiver.class);
        PendingIntent operation = PendingIntent.getBroadcast(context, REQUEST_CODE_INTENT_ACTION, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        alarmManager.cancel(operation);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void register() {
        MyLog.v("register()");

        GlobalManager globalManager = GlobalManager.getInstance();
        AppAlarm nextAlarm = globalManager.getNextAlarm();

        if (nextAlarm != null) {
            Calendar alarmTime = nextAlarm.getDateTime();

            Intent intent = new Intent(context, VoidReceiver.class);
            PendingIntent operation = PendingIntent.getBroadcast(context, REQUEST_CODE_INTENT_ACTION, intent, PendingIntent.FLAG_UPDATE_CURRENT);

            Intent showIntent = new Intent(context, AlarmMorningActivity.class);
            PendingIntent showOperation = PendingIntent.getBroadcast(context, REQUEST_CODE_INTENT_SHOW, showIntent, PendingIntent.FLAG_UPDATE_CURRENT);

            AlarmManager.AlarmClockInfo alarmClockInfo = new AlarmManager.AlarmClockInfo(alarmTime.getTimeInMillis(), showOperation);
            alarmManager.setAlarmClock(alarmClockInfo, operation);
        } else {
            MyLog.v("   no next alarm");
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void print() {
        AlarmManager.AlarmClockInfo alarmClockInfo = alarmManager.getNextAlarmClock();
        if (alarmClockInfo == null) {
            MyLog.v("   system alarm clock is not set");
        } else {
            Calendar t = CalendarUtils.newGregorianCalendar(alarmClockInfo.getTriggerTime());
            MyLog.v("   system alarm clock is at " + t.getTime());
        }
    }
}
