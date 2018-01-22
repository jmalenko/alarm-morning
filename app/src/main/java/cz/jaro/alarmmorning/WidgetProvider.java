package cz.jaro.alarmmorning;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

import java.util.Calendar;

import cz.jaro.alarmmorning.clock.Clock;
import cz.jaro.alarmmorning.model.AppAlarm;
import cz.jaro.alarmmorning.receivers.WidgetReceiver;

import static cz.jaro.alarmmorning.calendar.CalendarUtils.inNextWeek;
import static cz.jaro.alarmmorning.calendar.CalendarUtils.inTomorrow;
import static cz.jaro.alarmmorning.calendar.CalendarUtils.onTheSameDate;

/**
 * The widget.
 */
public class WidgetProvider extends AppWidgetProvider {

    private static final String TAG = WidgetProvider.class.getSimpleName();

    /**
     * Hide the tomorrow's alarm time until it is nearer than this number of hours.
     * <p>
     * This is a negative number. Technically: hide until the actual time - alarm time > this constant.
     */
    public static final int HIDE_TOMORROW_HOURS = -22;

    @Override
    public void onEnabled(Context context) {
        super.onEnabled(context);
        new Analytics(context, Analytics.Event.Add, Analytics.Channel.Widget, Analytics.ChannelName.Widget_alarm_time).save();
    }

    @Override
    public void onDisabled(Context context) {
        super.onDisabled(context);
        new Analytics(context, Analytics.Event.Remove, Analytics.Channel.Widget, Analytics.ChannelName.Widget_alarm_time).save();
    }

    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        Log.v(TAG, "onUpdate()");

        // Perform this loop procedure for each App Widget that belongs to this provider
        for (int appWidgetId : appWidgetIds) {
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_layout);

            updateContent(context, views);

            // Tell the AppWidgetManager to perform an update on the current app widget
            appWidgetManager.updateAppWidget(appWidgetId, views);
        }
    }

    public static void updateContent(Context context, RemoteViews views) {
        Log.d(TAG, "updateContent()");

        // Launch activity
        Intent intent = new Intent(context, WidgetReceiver.class);
        intent.setAction(WidgetReceiver.ACTION_WIDGET_CLICK);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 1, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        views.setOnClickPendingIntent(R.id.widget_layout, pendingIntent);

        // Set content
        GlobalManager globalManager = GlobalManager.getInstance();
        AppAlarm nextAlarmToRing = globalManager.getNextAlarmToRing();

        int iconSrcId;
        String timeText;
        String dateText = null;

        if (nextAlarmToRing != null) {
            Calendar alarmTime = nextAlarmToRing.getDateTime();

            iconSrcId = R.drawable.ic_alarm_white;

            timeText = Localization.timeToString(alarmTime.getTime(), context);

            Clock clock = globalManager.clock();
            Calendar now = clock.now();

            if (!onTheSameDate(now, alarmTime)) {
                if (inTomorrow(now, alarmTime)) {
                    Calendar fewHoursBeforeAlarmTime = (Calendar) alarmTime.clone();
                    fewHoursBeforeAlarmTime.add(Calendar.HOUR_OF_DAY, HIDE_TOMORROW_HOURS);
                    if (now.before(fewHoursBeforeAlarmTime)) {
                        dateText = context.getResources().getString(R.string.tomorrow);
                    }
                } else {
                    int dayOfWeek = alarmTime.get(Calendar.DAY_OF_WEEK);
                    String dayOfWeekText = Localization.dayOfWeekToStringShort(context.getResources(), dayOfWeek);
                    if (inNextWeek(now, alarmTime)) {
                        dateText = dayOfWeekText;
                    } else {
                        String calendarText = Localization.dateToStringVeryShort(context.getResources(), alarmTime.getTime());
                        dateText = context.getResources().getString(R.string.widget_alarm_text_later, dayOfWeekText, calendarText);
                    }
                }
            }
        } else {
            iconSrcId = R.drawable.ic_alarm_off_white;
            timeText = context.getResources().getString(R.string.widget_alarm_text_none);
        }
        views.setImageViewResource(R.id.icon, iconSrcId);
        views.setTextViewText(R.id.alarm_time, timeText);
        if (dateText != null) {
            views.setTextViewText(R.id.alarm_date, dateText);
            views.setViewVisibility(R.id.alarm_date, View.VISIBLE);
        } else {
            views.setViewVisibility(R.id.alarm_date, View.GONE);
        }
    }

    /*
     * Update on day change
     * ====================
     */

    private Context context;
    private HandlerOnClockChange handler = new HandlerOnClockChange(this::onDateChange, Calendar.HOUR_OF_DAY);

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.v(TAG, "onReceive(intent=" + intent.getAction() + ")");

        super.onReceive(context, intent);

        if (!handler.isRunning()) {
            this.context = context;
            handler.start();
        }
    }

    public void onDateChange() {
        Log.v(TAG, "onDateChange()");

        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_layout);
        updateContent(context, views);
        appWidgetManager.updateAppWidget(new ComponentName(context, WidgetProvider.class), views);
    }

}
