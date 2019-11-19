package cz.jaro.alarmmorning.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import cz.jaro.alarmmorning.AlarmMorningActivity;
import cz.jaro.alarmmorning.Analytics;
import cz.jaro.alarmmorning.GlobalManager;
import cz.jaro.alarmmorning.MyLog;
import cz.jaro.alarmmorning.RingActivity;
import cz.jaro.alarmmorning.model.AppAlarm;

import static cz.jaro.alarmmorning.GlobalManager.PERSIST_ALARM_ID;
import static cz.jaro.alarmmorning.GlobalManager.PERSIST_ALARM_TYPE;

/**
 * This receiver handles the actions with the notification.
 */
public class NotificationReceiver extends BroadcastReceiver {

    public static final String ACTION_CLICK_NOTIFICATION = "cz.jaro.alarmmorning.intent.action.CLICK_NOTIFICATION";
    public static final String ACTION_DELETE_NOTIFICATION = "cz.jaro.alarmmorning.intent.action.DELETE_NOTIFICATION";
    public static final String ACTION_DISMISS_BEFORE_RINGING = "cz.jaro.alarmmorning.intent.action.DISMISS_ALARM_BEFORE_RINGING";
    public static final String ACTION_DISMISS = "cz.jaro.alarmmorning.intent.action.DISMISS_ALARM";
    public static final String ACTION_SNOOZE = "cz.jaro.alarmmorning.intent.action.SNOOZE_ALARM";

    public static final String EXTRA_ACTIVITY = "EXTRA_ACTIVITY";

    public static final String EXTRA_ACTIVITY__RING = "Ring";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        MyLog.v("onReceive() action=" + action);

        AppAlarm appAlarm = null;
        GlobalManager globalManager = GlobalManager.getInstance();
        String alarmType = intent.getStringExtra(PERSIST_ALARM_TYPE);
        String alarmId = intent.getStringExtra(PERSIST_ALARM_ID);
        if (alarmType != null && alarmId != null)
            appAlarm = globalManager.load(alarmType, alarmId);

        switch (action) {
            case ACTION_CLICK_NOTIFICATION: {
                Analytics analytics = new Analytics(context, Analytics.Event.Click, Analytics.Channel.Notification, Analytics.ChannelName.Alarm);
                analytics.setAppAlarm(globalManager.getNextAlarmToRing());
                analytics.save();

                String activity = intent.getStringExtra(NotificationReceiver.EXTRA_ACTIVITY);
                if (activity == null) {
                    Intent calendarIntent = new Intent(context, AlarmMorningActivity.class);
                    calendarIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(calendarIntent);
                } else if (activity.equals(NotificationReceiver.EXTRA_ACTIVITY__RING)) {
                    Intent ringIntent = new Intent(context, RingActivity.class);
                    ringIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    intent.putExtra(PERSIST_ALARM_TYPE, appAlarm.getClass().getSimpleName());
                    intent.putExtra(PERSIST_ALARM_ID, appAlarm.getPersistenceId());
                    context.startActivity(ringIntent);
                } else {
                    throw new IllegalArgumentException("Unexpected argument " + activity);
                }
                break;
            }
            case ACTION_DELETE_NOTIFICATION: {
                Analytics analytics = new Analytics(context, Analytics.Event.Hide, Analytics.Channel.Notification, Analytics.ChannelName.Alarm);
                analytics.setAppAlarm(appAlarm);
                analytics.save();

                deleteNotification();
                break;
            }
            case ACTION_DISMISS_BEFORE_RINGING: {
                MyLog.i("Dismiss");

                Analytics analytics = new Analytics(Analytics.Channel.Notification, Analytics.ChannelName.Alarm);

                globalManager.onDismissBeforeRinging(appAlarm, analytics);
                break;
            }
            case ACTION_DISMISS: {
                MyLog.i("Dismiss");

                Analytics analytics = new Analytics(Analytics.Channel.Notification, Analytics.ChannelName.Alarm);

                globalManager.onDismiss(appAlarm, analytics);
                break;
            }
            case ACTION_SNOOZE: {
                MyLog.i("Snooze");

                Analytics analytics = new Analytics(Analytics.Channel.Notification, Analytics.ChannelName.Alarm);

                globalManager.onSnooze(appAlarm, analytics);
                break;
            }
        }
    }

    private void deleteNotification() {
        MyLog.v("deleteNotification()");
        MyLog.i("Delete notification");
    }

}
