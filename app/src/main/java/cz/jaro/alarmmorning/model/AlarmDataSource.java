package cz.jaro.alarmmorning.model;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

/**
 * Created by jmalenko on 16.12.2015.
 */
public class AlarmDataSource {
    private static final String TAG = AlarmDataSource.class.getName();

    private final SimpleDateFormat iso8601Format = new SimpleDateFormat("yyyy-MM-dd");

    public static final int[] allDaysOfWeek = new int[]{Calendar.SUNDAY, Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY, Calendar.THURSDAY, Calendar.FRIDAY, Calendar.SATURDAY};

    // Database fields
    private SQLiteDatabase database;
    private AlarmDbHelper dbHelper;
    private static final String[] allColumnsDefaults = {AlarmDbHelper.COLUMN_DEFAULTS_ID, AlarmDbHelper.COLUMN_DEFAULTS_DAY_OF_WEEK, AlarmDbHelper.COLUMN_DEFAULTS_STATE, AlarmDbHelper.COLUMN_DEFAULTS_HOUR, AlarmDbHelper.COLUMN_DEFAULTS_MINUTE};
    private static final String[] allColumnsDay = {AlarmDbHelper.COLUMN_DAY_ID, AlarmDbHelper.COLUMN_DAY_DATE, AlarmDbHelper.COLUMN_DAY_STATE, AlarmDbHelper.COLUMN_DAY_HOUR, AlarmDbHelper.COLUMN_DAY_MINUTE};

    public static final int DEFAULT_STATE_DISABLED = 0;
    public static final int DEFAULT_STATE_ENABLED = 1;

    public static final int DAY_STATE_DEFAULT = 0;
    public static final int DAY_STATE_ENABLED = 1;
    public static final int DAY_STATE_DISABLED = 2;

    public static final int VALUE_UNSET = -1;

    public static final int HORIZON_DAYS = 30;

    public AlarmDataSource(Context context) {
        dbHelper = new AlarmDbHelper(context);
    }

    public void open() throws SQLException {
        database = dbHelper.getWritableDatabase();
    }

    public void close() {
        dbHelper.close();
    }

    public Defaults loadDefault(int dayOfWeek) {
        Cursor cursor = database.query(AlarmDbHelper.TABLE_DEFAULTS, allColumnsDefaults, AlarmDbHelper.COLUMN_DEFAULTS_DAY_OF_WEEK + " = " + dayOfWeek, null, null, null, null);
        cursor.moveToFirst();
        Defaults defaults = cursorToDefaults(cursor);
        cursor.close();

        return defaults;
    }

    public void saveDefault(Defaults defaults) {
        ContentValues values = new ContentValues();
        values.put(AlarmDbHelper.COLUMN_DEFAULTS_DAY_OF_WEEK, defaults.getDayOfWeek());
        values.put(AlarmDbHelper.COLUMN_DEFAULTS_STATE, defaults.getState());
        values.put(AlarmDbHelper.COLUMN_DEFAULTS_HOUR, defaults.getHour());
        values.put(AlarmDbHelper.COLUMN_DEFAULTS_MINUTE, defaults.getMinute());

        database.insertWithOnConflict(AlarmDbHelper.TABLE_DEFAULTS, null, values, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public Day loadDay(Calendar date) {
        Day day;

        String dateText = dateToText(date);

        Cursor cursor = database.query(AlarmDbHelper.TABLE_DAY, allColumnsDay, AlarmDbHelper.COLUMN_DAY_DATE + " = \"" + dateText + "\"", null, null, null, null);
        if (cursor.getCount() == 0) {
            day = new Day();
            day.setDate(date);
            day.setState(DAY_STATE_DEFAULT);
            day.setHour(VALUE_UNSET);
            day.setMinute(VALUE_UNSET);
        } else {
            cursor.moveToFirst();
            day = cursorToDay(cursor);
            cursor.close();
        }

        int dayOfWeek = date.get(Calendar.DAY_OF_WEEK);
        Defaults defaults = loadDefault(dayOfWeek);
        day.setDefaults(defaults);
        return day;
    }

    public void saveDay(Day day) {
        String dateText = dateToText(day.getDate());

        ContentValues values = new ContentValues();
        values.put(AlarmDbHelper.COLUMN_DAY_DATE, dateText);
        values.put(AlarmDbHelper.COLUMN_DAY_STATE, day.getState());
        values.put(AlarmDbHelper.COLUMN_DAY_HOUR, day.getHour());
        values.put(AlarmDbHelper.COLUMN_DAY_MINUTE, day.getMinute());

        long id = database.insertWithOnConflict(AlarmDbHelper.TABLE_DAY, null, values, SQLiteDatabase.CONFLICT_REPLACE);

        printDB();
    }

    private String dateToText(Calendar date) {
        return iso8601Format.format(date.getTime());
    }

    private Calendar textToDate(String dateText) {
        Calendar date = new GregorianCalendar();
        try {
            Date date2 = iso8601Format.parse(dateText);
            date.setTimeInMillis(date2.getTime());
            return date;
        } catch (ParseException e) {
            Log.w(TAG, "Unable to parse date from string: " + dateText);
            throw new RuntimeException("Invalid date format", e);
        }
    }

    public Calendar getNextAlarm(Calendar now) {
        Calendar date = new GregorianCalendar();

        for (int daysInAdvance = 0; daysInAdvance < HORIZON_DAYS; daysInAdvance++, date.add(Calendar.DATE, 1)) {
            Day day = loadDay(date);
            if (!day.isEnabled()) {
                continue;
            }

            Calendar alarmTime = day.getDateTime();

            if (alarmTime.getTime().before(now.getTime())) {
                continue;
            }

            Log.d(TAG, "Next alarm is at " + alarmTime.getTime().toString());
            return alarmTime;
        }
        return null;
    }

    private Defaults cursorToDefaults(Cursor cursor) {
        Defaults defaults = new Defaults();
        defaults.setId(cursor.getLong(0));
        defaults.setDayOfWeek(cursor.getInt(1));
        defaults.setState(cursor.getInt(2));
        defaults.setHour(cursor.getInt(3));
        defaults.setMinute(cursor.getInt(4));
        return defaults;
    }

    private Day cursorToDay(Cursor cursor) {
        Day day = new Day();
        day.setId(cursor.getLong(0));

        String dateText = cursor.getString(1);
        Calendar date = textToDate(dateText);
        day.setDate(date);

        day.setState(cursor.getInt(2));
        day.setHour(cursor.getInt(3));
        day.setMinute(cursor.getInt(4));
        return day;
    }

    private void printDB() {
        Cursor cursor = database.query(AlarmDbHelper.TABLE_DEFAULTS, allColumnsDefaults, null, null, null, null, null);
        Log.d(TAG, "Table Defaults, rows " + cursor.getCount());

        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            Defaults defaults = cursorToDefaults(cursor);
            Log.d(TAG, " " + defaults.getId() + " | " + defaults.getDayOfWeek() + " | " + defaults.getState() + " | " + defaults.getHour() + " | " + defaults.getMinute());
            cursor.moveToNext();
        }
        cursor.close();

        cursor = database.query(AlarmDbHelper.TABLE_DAY, allColumnsDay, null, null, null, null, null);
        Log.d(TAG, "Table Day, rows " + cursor.getCount());

        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            Day day = cursorToDay(cursor);
            Log.d(TAG, " " + day.getId() + " | " + dateToText(day.getDate()) + " | " + day.getState() + " | " + day.getHour() + " | " + day.getMinute());
            cursor.moveToNext();
        }
        cursor.close();
    }
}
