package cz.jaro.alarmmorning.receivers;

import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import cz.jaro.alarmmorning.AlarmMorningActivity;
import cz.jaro.alarmmorning.Analytics;

/**
 * The widget receiver.
 */
public class WidgetReceiver extends AppWidgetProvider {

    private static final String TAG = WidgetReceiver.class.getSimpleName();

    public static final String ACTION_WIDGET_CLICK = "cz.jaro.alarmmorning.intent.action.WIDGET_CLICK";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        Log.v(TAG, "onReceive() action=" + action);

        if (action.equals(ACTION_WIDGET_CLICK)) {
            new Analytics(context, Analytics.Event.Click, Analytics.Channel.Widget, Analytics.ChannelName.Widget_alarm_time).save();

            Intent calendarIntent = new Intent(context, AlarmMorningActivity.class);
            calendarIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(calendarIntent);
        }
    }

}