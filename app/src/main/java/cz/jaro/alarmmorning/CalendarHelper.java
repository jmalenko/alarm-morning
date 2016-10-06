package cz.jaro.alarmmorning;

import android.Manifest;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.provider.CalendarContract;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import java.util.Calendar;

/**
 * This class provides unifed access to calendar.
 */
public class CalendarHelper {

    private static final String TAG = CalendarHelper.class.getSimpleName();

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
     * Find the first event that starts after {@code from} and before {@code to}.
     *
     * @param from Beginning of the interval in which the returned event starts
     * @param to   End of the interval in which the returned event starts
     * @return calendar event
     */
    public CalendarEvent find(Calendar from, Calendar to) {
        Log.d(TAG, "Find the first calendar that starts between " + from.getTime() + " and " + to.getTime());

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
                while (cur.moveToNext()) {
                    CalendarEvent event = CalendarEvent.load(cur);

                    String timeStr = Localization.timeToString(event.begin.getTime(), context);
                    Log.v(TAG, "   " + timeStr + " " + event.title + "    " + event.location);

                    if (!event.begin.before(from)) { // event starts on or after startOfTomorrow
                        Log.d(TAG, "Found");
                        return event;
                    }
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
}
