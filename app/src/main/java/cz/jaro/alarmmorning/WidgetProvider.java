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
import cz.jaro.alarmmorning.clock.SystemClock;
import cz.jaro.alarmmorning.model.Day;

/**
 * The widget.
 */
public class WidgetProvider extends AppWidgetProvider {

    private static final String TAG = WidgetProvider.class.getSimpleName();

    public static final int HIDE_TOMORROW_HOURS = -22;

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
        Intent intent = new Intent(context, AlarmMorningActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);
        views.setOnClickPendingIntent(R.id.widget_layout, pendingIntent);

        // Set content
        GlobalManager globalManager = new GlobalManager(context);
        Day day = globalManager.getDayWithNextAlarmToRing();

        int iconSrcId;
        String timeText;
        String dateText = null;

        if (day != null) {
            Calendar alarmTime = day.getDateTime();

            iconSrcId = R.drawable.ic_alarm_white;

            timeText = Localization.timeToString(alarmTime.getTime(), context);

            Clock clock = new SystemClock(); // TODO Solve dependency on clock
            Calendar now = clock.now();

            if (!RingActivity.onTheSameDate(now, alarmTime)) {
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

    private static boolean inTomorrow(Calendar cal1, Calendar cal2) {
        Calendar date = (Calendar) cal1.clone();
        date.add(Calendar.DATE, 1);
        return RingActivity.onTheSameDate(date, cal2);
    }

    private static boolean inNextWeek(Calendar cal1, Calendar cal2) {
        Calendar date = (Calendar) cal1.clone();
        for (int i = 0; i < 6; i++) {
            date.add(Calendar.DATE, 1);
            if (RingActivity.onTheSameDate(date, cal2)) {
                return true;
            }
        }
        return false;
    }

    /*
     * Update on day change
     * ====================
     */

    private Context context;
    private Runnable runnable = new Runnable() {
        @Override
        public void run() {
            onDateChange();
        }
    };
    private HandlerOnClockChange handler = new HandlerOnClockChange(runnable, Calendar.HOUR_OF_DAY);

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
