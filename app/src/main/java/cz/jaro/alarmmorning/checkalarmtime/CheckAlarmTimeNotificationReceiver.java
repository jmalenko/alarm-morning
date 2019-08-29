package cz.jaro.alarmmorning.checkalarmtime;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.util.Calendar;

import cz.jaro.alarmmorning.AlarmMorningActivity;
import cz.jaro.alarmmorning.Analytics;
import cz.jaro.alarmmorning.GlobalManager;
import cz.jaro.alarmmorning.SharedPreferencesHelper;
import cz.jaro.alarmmorning.calendar.CalendarUtils;

import static cz.jaro.alarmmorning.Analytics.CHECK_ALARM_TIME_METHOD__QUICK;
import static cz.jaro.alarmmorning.model.Day.VALUE_UNSET;

/**
 * This receiver handles the actions with the notification for "check alarm time" function.
 * <p>
 * The "new alarm time" parameter is passed in the {@link #EXTRA_NEW_ALARM_TIME} extra.
 */
public class CheckAlarmTimeNotificationReceiver extends BroadcastReceiver {

    private static final String TAG = GlobalManager.createLogTag(CheckAlarmTimeNotificationReceiver.class);

    public static final String ACTION_CHECK_ALARM_TIME_CLICK = "cz.jaro.alarmmorning.intent.action.CLICK";
    public static final String ACTION_CHECK_ALARM_TIME_DELETE = "cz.jaro.alarmmorning.intent.action.DELETE";
    public static final String ACTION_CHECK_ALARM_TIME_SET_TO = "cz.jaro.alarmmorning.intent.action.SET_TO";
    public static final String ACTION_CHECK_ALARM_TIME_SET_DIALOG = "cz.jaro.alarmmorning.intent.action.SET_DIALOG";
    public static final String ACTION_CHECK_ALARM_TIME_DISABLE = "cz.jaro.alarmmorning.intent.action.DISABLE";

    public static final String EXTRA_NEW_ALARM_TIME = "new_alarm_time";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        Log.v(TAG, "onReceive() action=" + action);

        switch (action) {
            case ACTION_CHECK_ALARM_TIME_CLICK:
                new Analytics(context, Analytics.Event.Click, Analytics.Channel.Notification, Analytics.ChannelName.Check_alarm_time).save();

                Intent calendarIntent = new Intent(context, AlarmMorningActivity.class);
                calendarIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(calendarIntent);
                break;
            case ACTION_CHECK_ALARM_TIME_DELETE:
                new Analytics(context, Analytics.Event.Hide, Analytics.Channel.Notification, Analytics.ChannelName.Check_alarm_time).save();

                SharedPreferencesHelper.save(CheckAlarmTime.PERSIST__CHECK_ALARM_TIME__NOTIFICATION_ACTION, CheckAlarmTime.PERSIST__CHECK_ALARM_TIME__NOTIFICATION_ACTION__DELETED);
                break;
            case ACTION_CHECK_ALARM_TIME_SET_TO: {
                // Read parameters from extra
                long newAlarmTimeLong = intent.getLongExtra(EXTRA_NEW_ALARM_TIME, VALUE_UNSET);
                if (newAlarmTimeLong == VALUE_UNSET) {
                    throw new IllegalArgumentException("The " + EXTRA_NEW_ALARM_TIME + " extra must be set");
                }

                Calendar newAlarmTime = CalendarUtils.newGregorianCalendar(newAlarmTimeLong);

                Log.i(TAG, "Set alarm time to " + newAlarmTime.getTime());
                Analytics analytics = new Analytics(Analytics.Channel.Notification, Analytics.ChannelName.Check_alarm_time).set(Analytics.Param.Check_alarm_time_method, CHECK_ALARM_TIME_METHOD__QUICK);

                SetTimeActivity.updateDay(context, newAlarmTime, analytics);

                SharedPreferencesHelper.save(CheckAlarmTime.PERSIST__CHECK_ALARM_TIME__NOTIFICATION_ACTION, CheckAlarmTime.PERSIST__CHECK_ALARM_TIME__NOTIFICATION_ACTION__SET_TO_DEFAULT);
                break;
            }
            case ACTION_CHECK_ALARM_TIME_DISABLE: {
                // Read parameters from extra
                long dayDateLong = intent.getLongExtra(EXTRA_NEW_ALARM_TIME, VALUE_UNSET);
                if (dayDateLong == VALUE_UNSET) {
                    throw new IllegalArgumentException("The " + EXTRA_NEW_ALARM_TIME + " extra must be set");
                }

                Calendar dayDate = CalendarUtils.newGregorianCalendar(dayDateLong);

                Log.i(TAG, "Disabling alarm on " + dayDate.getTime());
                Analytics analytics = new Analytics(Analytics.Channel.Notification, Analytics.ChannelName.Check_alarm_time).set(Analytics.Param.Check_alarm_time_method, CHECK_ALARM_TIME_METHOD__QUICK);

                SetTimeActivity.updateDay(context, true, dayDate, analytics, true);

                SharedPreferencesHelper.save(CheckAlarmTime.PERSIST__CHECK_ALARM_TIME__NOTIFICATION_ACTION, CheckAlarmTime.PERSIST__CHECK_ALARM_TIME__NOTIFICATION_ACTION__SET_TO_DEFAULT);
                break;
            }
            case ACTION_CHECK_ALARM_TIME_SET_DIALOG: {
                // Read parameters from extra
                long newAlarmTimeLong = intent.getLongExtra(EXTRA_NEW_ALARM_TIME, VALUE_UNSET);
                if (newAlarmTimeLong == VALUE_UNSET) {
                    throw new IllegalArgumentException("The " + EXTRA_NEW_ALARM_TIME + " extra must be set");
                }

                Log.i(TAG, "Show dialog to set alarm time");

                // Start activity
                Intent dialogIntent = new Intent(context, SetTimeActivity.class);
                dialogIntent.putExtra(CheckAlarmTimeNotificationReceiver.EXTRA_NEW_ALARM_TIME, newAlarmTimeLong);
                dialogIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                dialogIntent.addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
                dialogIntent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
                dialogIntent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                context.startActivity(dialogIntent);

                // Preference CheckAlarmTime.PERSIST__CHECK_ALARM_TIME__NOTIFICATION_ACTION is set in the activity after a sucessful completition.

                // Collapse Android notification tray
                Intent it = new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
                context.sendBroadcast(it);
                break;
            }
            default:
                throw new IllegalArgumentException("Unexpected argument " + action);
        }

        // Hide notification
        CheckAlarmTime checkAlarmTime = CheckAlarmTime.getInstance(context);
        checkAlarmTime.hideNotification();
    }
}