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
    private static final String[] allColumnsDefaults = {AlarmDbHelper.COLUMN_DEFAULTS_ID, AlarmDbHelper.COLUMN_DEFAULTS_DAY_OF_WEEK, AlarmDbHelper.COLUMN_DEFAULTS_STATE, AlarmDbHelper.COLUMN_DEFAULTS_HOURS, AlarmDbHelper.COLUMN_DEFAULTS_MINUTES};
    private static final String[] allColumnsDay = {AlarmDbHelper.COLUMN_DAY_ID, AlarmDbHelper.COLUMN_DAY_DATE, AlarmDbHelper.COLUMN_DAY_STATE, AlarmDbHelper.COLUMN_DAY_HOURS, AlarmDbHelper.COLUMN_DAY_MINUTES};

    public static final int DEFAULT_STATE_UNSET = 0;
    public static final int DEFAULT_STATE_SET = 1;

    public static final int DAY_STATE_DEFAULT = 0;
    public static final int DAY_STATE_SET = 1;
    public static final int DAY_STATE_UNSET = 2;

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

    public int saveDefault(Defaults defaults) {
        ContentValues values = new ContentValues();
        values.put(AlarmDbHelper.COLUMN_DEFAULTS_STATE, defaults.getState());
        values.put(AlarmDbHelper.COLUMN_DEFAULTS_HOURS, defaults.getHours());
        values.put(AlarmDbHelper.COLUMN_DEFAULTS_MINUTES, defaults.getMinutes());

        int rowAffected = database.update(AlarmDbHelper.TABLE_DEFAULTS, values, AlarmDbHelper.COLUMN_DEFAULTS_DAY_OF_WEEK + " = " + defaults.getDayOfWeek(), null);
        return rowAffected;
    }

    public Day loadDay(GregorianCalendar date) {
        Day day;

        String dateText = dateToText(date);

        Cursor cursor = database.query(AlarmDbHelper.TABLE_DAY, allColumnsDay, AlarmDbHelper.COLUMN_DAY_DATE + " = \"" + dateText + "\"", null, null, null, null);
        if (cursor.getCount() == 0) {
            day = new Day();
            day.setDate(date);
            day.setState(DAY_STATE_DEFAULT);
            day.setHours(VALUE_UNSET);
            day.setMinutes(VALUE_UNSET);
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
        values.put(AlarmDbHelper.COLUMN_DAY_HOURS, day.getHours());
        values.put(AlarmDbHelper.COLUMN_DAY_MINUTES, day.getMinutes());

        long id = database.insertWithOnConflict(AlarmDbHelper.TABLE_DAY, null, values, SQLiteDatabase.CONFLICT_REPLACE);

        printDB();
    }

    private String dateToText(GregorianCalendar date) {
        return iso8601Format.format(date.getTime());
    }

    private GregorianCalendar textToDate(String dateText) {
        GregorianCalendar date = new GregorianCalendar();
        try {
            Date date2 = iso8601Format.parse(dateText);
            date.setTimeInMillis(date2.getTime());
            return date;
        } catch (ParseException e) {
            Log.w(TAG, "Unable to parse date from string: " + dateText);
            throw new RuntimeException("Invalid date format", e);
        }
    }

    public GregorianCalendar getNextAlarm(GregorianCalendar currentTime) {
        GregorianCalendar date = new GregorianCalendar();

        for (int daysInAdvance = 0; daysInAdvance < HORIZON_DAYS; daysInAdvance++, date.add(Calendar.DATE, 1)) {
            Day day = loadDay(date);
            if (day.getState() == DAY_STATE_UNSET) {
                continue;
            }

            GregorianCalendar alarmTime = day.getDate();
            if (day.getHours() == AlarmDataSource.VALUE_UNSET) {
                int dayOfWeek = alarmTime.get(Calendar.DAY_OF_WEEK);
                Defaults defaults = loadDefault(dayOfWeek);
                day.setDefaults(defaults);

                alarmTime.set(Calendar.HOUR_OF_DAY, defaults.getHours());
                alarmTime.set(Calendar.MINUTE, defaults.getMinutes());
            } else {
                alarmTime.set(Calendar.HOUR_OF_DAY, day.getHours());
                alarmTime.set(Calendar.MINUTE, day.getMinutes());
            }
            alarmTime.set(Calendar.SECOND, 0);
            alarmTime.set(Calendar.MILLISECOND, 0);

            if (alarmTime.getTime().before(currentTime.getTime())) {
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
        defaults.setHours(cursor.getInt(3));
        defaults.setMinutes(cursor.getInt(4));
        return defaults;
    }

    private Day cursorToDay(Cursor cursor) {
        Day day = new Day();
        day.setId(cursor.getLong(0));

        String dateText = cursor.getString(1);
        GregorianCalendar date = textToDate(dateText);
        day.setDate(date);

        day.setState(cursor.getInt(2));
        day.setHours(cursor.getInt(3));
        day.setMinutes(cursor.getInt(4));
        return day;
    }

    private void printDB() {
        Cursor cursor = database.query(AlarmDbHelper.TABLE_DEFAULTS, allColumnsDefaults, null, null, null, null, null);
        Log.d(TAG, "Table Defaults, rows " + cursor.getCount());

        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            Defaults defaults = cursorToDefaults(cursor);
            Log.d(TAG, " " + defaults.getId() + " | " + defaults.getDayOfWeek() + " | " + defaults.getState() + " | " + defaults.getHours() + " | " + defaults.getMinutes());
            cursor.moveToNext();
        }
        cursor.close();

        cursor = database.query(AlarmDbHelper.TABLE_DAY, allColumnsDay, null, null, null, null, null);
        Log.d(TAG, "Table Day, rows " + cursor.getCount());

        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            Day day = cursorToDay(cursor);
            Log.d(TAG, " " + day.getId() + " | " + dateToText(day.getDate()) + " | " + day.getState() + " | " + day.getHours() + " | " + day.getMinutes());
            cursor.moveToNext();
        }
        cursor.close();
    }
}
