package cz.jaro.alarmmorning.model;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.util.Log;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;

/**
 * Store objects to a database.
 */
public class AlarmDataSource {
    private static final String TAG = AlarmDataSource.class.getSimpleName();

    private final SimpleDateFormat iso8601Format = new SimpleDateFormat("yyyy-MM-dd", Locale.US);

    private SQLiteDatabase database;
    private AlarmDbHelper dbHelper;

    // Table fields
    private static final String[] allColumnsDefaults = {AlarmDbHelper.COLUMN_DEFAULTS_ID, AlarmDbHelper.COLUMN_DEFAULTS_DAY_OF_WEEK, AlarmDbHelper.COLUMN_DEFAULTS_STATE, AlarmDbHelper.COLUMN_DEFAULTS_HOUR, AlarmDbHelper.COLUMN_DEFAULTS_MINUTE};
    private static final String[] allColumnsDay = {AlarmDbHelper.COLUMN_DAY_ID, AlarmDbHelper.COLUMN_DAY_DATE, AlarmDbHelper.COLUMN_DAY_STATE, AlarmDbHelper.COLUMN_DAY_HOUR, AlarmDbHelper.COLUMN_DAY_MINUTE};

    public static final int[] allDaysOfWeek = new int[]{Calendar.SUNDAY, Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY, Calendar.THURSDAY, Calendar.FRIDAY, Calendar.SATURDAY};

    /**
     * Initialize the object.
     *
     * @param context Context
     */
    public AlarmDataSource(Context context) {
        dbHelper = new AlarmDbHelper(context);
    }

    /**
     * Open the database for writing.
     *
     * @throws SQLiteException if the database cannot be opened for writing
     */
    public void open() throws SQLiteException {
        database = dbHelper.getWritableDatabase();
    }

    /**
     * Close the open database.
     * <p>
     * Note: The database should be never called. Source: https://nfrolov.wordpress.com/2014/08/16/android-sqlitedatabase-locking-and-multi-threading/
     */
    public void close() {
        dbHelper.close();
    }

    /**
     * Retrieve a {@code Defaults} object from the database.
     *
     * @param dayOfWeek identifier of the object
     * @return The retrieved object
     */
    public Defaults loadDefault(int dayOfWeek) {
        Cursor cursor = database.query(AlarmDbHelper.TABLE_DEFAULTS, allColumnsDefaults, AlarmDbHelper.COLUMN_DEFAULTS_DAY_OF_WEEK + " = " + dayOfWeek, null, null, null, null);
        // there always is a row (because it was created when the database was created)
        cursor.moveToFirst();
        Defaults defaults = cursorToDefaults(cursor);
        cursor.close();

        return defaults;
    }

    /**
     * Store the {@code Defaults} object in the database.
     *
     * @param defaults object to be stored
     */
    public void saveDefault(Defaults defaults) {
        ContentValues values = new ContentValues();
        values.put(AlarmDbHelper.COLUMN_DEFAULTS_DAY_OF_WEEK, defaults.getDayOfWeek());//
        values.put(AlarmDbHelper.COLUMN_DEFAULTS_STATE, defaults.getState());
        values.put(AlarmDbHelper.COLUMN_DEFAULTS_HOUR, defaults.getHour());
        values.put(AlarmDbHelper.COLUMN_DEFAULTS_MINUTE, defaults.getMinute());

        database.insertWithOnConflict(AlarmDbHelper.TABLE_DEFAULTS, null, values, SQLiteDatabase.CONFLICT_REPLACE);
    }

    /**
     * Retrieve a {@code Day} object from the database, together with the {@link Day#defaults} referenced object..
     *
     * @param date identifier of the object
     * @return The retrieved object
     */
    public Day loadDay(Calendar date) {
        Day day = loadDayShallow(date);

        if (day == null) {
            day = new Day();
            day.setDate(new GregorianCalendar(date.get(Calendar.YEAR), date.get(Calendar.MONTH), date.get(Calendar.DAY_OF_MONTH)));
            day.setState(Day.STATE_RULE);
            day.setHour(Day.VALUE_UNSET);
            day.setMinute(Day.VALUE_UNSET);
        }

        int dayOfWeek = date.get(Calendar.DAY_OF_WEEK);
        Defaults defaults = loadDefault(dayOfWeek);
        day.setDefaults(defaults);

        return day;
    }

    /**
     * Retrieve a {@code Day} object from the database.
     *
     * @param date identifier of the object
     * @return The retrieved object
     */
    private Day loadDayShallow(Calendar date) {
        Day day;

        String dateText = dateToText(date);

        Cursor cursor = database.query(AlarmDbHelper.TABLE_DAY, allColumnsDay, AlarmDbHelper.COLUMN_DAY_DATE + " = \"" + dateText + "\"", null, null, null, null);
        if (cursor.getCount() == 0) {
            day = null;
        } else {
            cursor.moveToFirst();
            day = cursorToDay(cursor);
            cursor.close();
        }

        return day;
    }

    /**
     * Store the {@code Day} object in the database.
     * <p/>
     * Note that the referenced {@link Day#defaults} object is not stored.
     *
     * @param day object to be stored
     */
    public void saveDay(Day day) {
        String dateText = dateToText(day.getDate());

        ContentValues values = new ContentValues();
        values.put(AlarmDbHelper.COLUMN_DAY_DATE, dateText);
        values.put(AlarmDbHelper.COLUMN_DAY_STATE, day.getState());
        values.put(AlarmDbHelper.COLUMN_DAY_HOUR, day.getHour());
        values.put(AlarmDbHelper.COLUMN_DAY_MINUTE, day.getMinute());

        long id = database.insertWithOnConflict(AlarmDbHelper.TABLE_DAY, null, values, SQLiteDatabase.CONFLICT_REPLACE);
    }

    private String dateToText(Calendar date) {
        return iso8601Format.format(date.getTime());
    }

    private Calendar textToDate(String dateText) {
        try {
            Calendar date = new GregorianCalendar();
            Date date2 = iso8601Format.parse(dateText);
            date.setTimeInMillis(date2.getTime());
            return date;
        } catch (ParseException e) {
            Log.e(TAG, "Unable to parse date from string: " + dateText);
            throw new RuntimeException("Invalid date format", e);
        }
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

    private String DBtoString() {
        StringBuilder str = new StringBuilder();

        Cursor cursor = database.query(AlarmDbHelper.TABLE_DEFAULTS, allColumnsDefaults, null, null, null, null, null);
        str.append("Table Defaults, rows ").append(cursor.getCount()).append("\n");
        str.append("id|DoW|Sta| H | M \n");
        str.append("--+---+---+---+-- \n");

        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            Defaults defaults = cursorToDefaults(cursor);
            str.
                    append(" ").append(defaults.getId()).
                    append(" | ").append(defaults.getDayOfWeek()).
                    append(" | ").append(defaults.getState()).
                    append(" | ").append(defaults.getHour()).
                    append(" | ").append(defaults.getMinute()).append("\n");
            cursor.moveToNext();
        }
        cursor.close();

        cursor = database.query(AlarmDbHelper.TABLE_DAY, allColumnsDay, null, null, null, null, null);
        str.append("Table Day, rows ").append(cursor.getCount()).append("\n");
        str.append("id| Date       |Sta| H | M \n");
        str.append("--+------------+---+---+---\n");

        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            Day day = cursorToDay(cursor);
            str.
                    append(" ").append(day.getId()).
                    append(" | ").append(dateToText(day.getDate())).
                    append(" | ").append(day.getState()).
                    append(" | ").append(day.getHour()).
                    append(" | ").append(day.getMinute()).append("\n");
            cursor.moveToNext();
        }
        cursor.close();

        return str.toString();
    }

    /**
     * Reset the database to the initial state.
     * <p>
     * Deletes all Days and disables the Defaults.
     */
    public void resetDatabase() {
        deleteAllDays();
        AlarmDbHelper.resetDefaults(database);
    }

    private void deleteAllDays() {
        database.delete(AlarmDbHelper.TABLE_DAY, null, null);
    }

}
