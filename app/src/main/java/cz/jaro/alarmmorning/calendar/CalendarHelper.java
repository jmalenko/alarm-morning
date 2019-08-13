package cz.jaro.alarmmorning.calendar;

import android.Manifest;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.provider.CalendarContract;
import android.util.Log;

import java.util.Calendar;

import androidx.core.content.ContextCompat;
import cz.jaro.alarmmorning.GlobalManager;
import cz.jaro.alarmmorning.Localization;

/**
 * This class provides unifed access to calendar.
 */
public class CalendarHelper {

    private static final String TAG = GlobalManager.createLogTag(CalendarHelper.class);

    // Projection for calendar instances
    public static final String[] INSTANCE_PROJECTION = new String[]{
            CalendarContract.Instances.EVENT_ID,        // 0
            CalendarContract.Instances.BEGIN,           // 1
            CalendarContract.Instances.TITLE,           // 2
            CalendarContract.Instances.EVENT_LOCATION,  // 3
            CalendarContract.Instances.ALL_DAY          // 4
    };

    // The indices for the projection array above
    public static final int PROJECTION_ID_INDEX = 0;
    public static final int PROJECTION_BEGIN_INDEX = 1;
    public static final int PROJECTION_TITLE_INDEX = 2;
    public static final int PROJECTION_LOCATION_INDEX = 3;
    public static final int PROJECTION_ALL_DAY_INDEX = 4;

    private final Context context;

    public CalendarHelper(Context context) {
        this.context = context;
    }

    /**
     * Find the first event that starts between {@code from} and {@code to}.
     *
     * @param from Beginning of the interval in which the returned event starts
     * @param to   End of the interval in which the returned event starts
     * @return calendar event
     */
    public CalendarEvent find(Calendar from, Calendar to) {
        return find(from, to, null);
    }

    /**
     * Find the earliest event that<br/>
     * 1. starts between {@code from} and {@code to}, and<br/>
     * 2. matches {@code filter}.
     *
     * @param from   Beginning of the interval in which the returned event starts
     * @param to     End of the interval in which the returned event starts
     * @param filter Filter
     * @return calendar event
     */
    public CalendarEvent find(Calendar from, Calendar to, CalendarEventFilter filter) {
        Log.d(TAG, "Find the earliest calendar item that starts between " + from.getTime() + " and " + to.getTime());

        int permissionCheck = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR);
        if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
            // Construct the query with the desired date range.
            Uri.Builder builder = CalendarContract.Instances.CONTENT_URI.buildUpon();
            ContentUris.appendId(builder, from.getTimeInMillis()); // Event happens in this interval
            ContentUris.appendId(builder, to.getTimeInMillis());

            String sortOrder = CalendarContract.Instances.BEGIN + ", " + CalendarContract.Instances.END;

            // Submit the query
            ContentResolver cr = context.getContentResolver();
            Cursor cur = cr.query(builder.build(), INSTANCE_PROJECTION, null, null, sortOrder);

            if (cur != null) {
                CalendarEvent eventEarliest = null;

                while (cur.moveToNext()) {
                    CalendarEvent event = load(cur);

                    String timeStr = Localization.timeToString(event.begin.getTime(), context);
                    Log.v(TAG, "   " + timeStr + " " + event.title + "    " + event.location);

                    if (!event.begin.before(from)) { // event starts on or after startOfTomorrow
                        if (filter == null || filter.match(event)) { // match filter
                            if (eventEarliest == null || event.getBegin().before(eventEarliest.getBegin())) {
                                eventEarliest = event;
                            }
                        }
                    }
                }

                if (eventEarliest != null) {
                    String timeStr = Localization.timeToString(eventEarliest.begin.getTime(), context);
                    Log.d(TAG, "Found event: " + timeStr + " " + eventEarliest.getTitle());
                    return eventEarliest;
                }

                Log.d(TAG, "Calendar query returned no events that match criteria");
            } else {
                Log.d(TAG, "Calendar query returned null");
            }
        } else {
            Log.d(TAG, "Permission to read calendar not granted");
        }
        return null;
    }

    CalendarEvent load(Cursor cur) {
        CalendarEvent event = new CalendarEvent();

        long eventID = cur.getLong(PROJECTION_ID_INDEX);
        event.setEventID(eventID);

        long beginMS = cur.getLong(PROJECTION_BEGIN_INDEX);
        Calendar begin = Calendar.getInstance();
        begin.setTimeInMillis(beginMS);
        event.setBegin(begin);

        String title = cur.getString(PROJECTION_TITLE_INDEX);
        event.setTitle(title);

        String location = cur.getString(PROJECTION_LOCATION_INDEX);
        event.setLocation(location);

        int allDayInt = cur.getInt(PROJECTION_ALL_DAY_INDEX);
        boolean allDayBool = allDayInt == 1;
        event.setAllDay(allDayBool);

        return event;
    }

}
