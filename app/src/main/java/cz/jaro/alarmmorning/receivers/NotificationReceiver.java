package cz.jaro.alarmmorning.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import cz.jaro.alarmmorning.AlarmMorningActivity;
import cz.jaro.alarmmorning.Analytics;
import cz.jaro.alarmmorning.GlobalManager;
import cz.jaro.alarmmorning.RingActivity;
import cz.jaro.alarmmorning.model.AppAlarm;

/**
 * This receiver handles the actions with the notification.
 */
public class NotificationReceiver extends BroadcastReceiver {

    private static final String TAG = NotificationReceiver.class.getSimpleName();

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

        Log.v(TAG, "onReceive() action=" + action);

        GlobalManager globalManager = GlobalManager.getInstance();

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
                    intent.putExtra(RingActivity.ALARM_TIME, intent.getSerializableExtra(RingActivity.ALARM_TIME));
                    intent.putExtra(RingActivity.ALARM_NAME, intent.getSerializableExtra(RingActivity.ALARM_NAME));
                    context.startActivity(ringIntent);
                } else {
                    throw new IllegalArgumentException("Unexpected argument " + activity);
                }
                break;
            }
            case ACTION_DELETE_NOTIFICATION: {
                Analytics analytics = new Analytics(context, Analytics.Event.Hide, Analytics.Channel.Notification, Analytics.ChannelName.Alarm);
                analytics.setAppAlarm(globalManager.getNextAlarmToRing());
                analytics.save();

                deleteNotification();
                break;
            }
            case ACTION_DISMISS_BEFORE_RINGING: {
                Log.i(TAG, "Dismiss");

                AppAlarm appAlarm = globalManager.getAlarmOfRingingAlarm();
                Analytics analytics = new Analytics(Analytics.Channel.Notification, Analytics.ChannelName.Alarm);

                globalManager.onDismissBeforeRinging(appAlarm, analytics);
                break;
            }
            case ACTION_DISMISS: {
                Log.i(TAG, "Dismiss");

                Analytics analytics = new Analytics(Analytics.Channel.Notification, Analytics.ChannelName.Alarm);

                globalManager.onDismiss(analytics);
                break;
            }
            case ACTION_SNOOZE: {
                Log.i(TAG, "Snooze");

                Analytics analytics = new Analytics(Analytics.Channel.Notification, Analytics.ChannelName.Alarm);

                globalManager.onSnooze(analytics);
                break;
            }
        }
    }

    private void deleteNotification() {
        Log.v(TAG, "deleteNotification()");
        Log.i(TAG, "Delete notification");
    }

}
