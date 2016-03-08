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

import cz.jaro.alarmmorning.Localization;
import cz.jaro.alarmmorning.clock.Clock;
import cz.jaro.alarmmorning.clock.SystemClock;

/**
 * Store objects to a database.
 */
public class AlarmDataSource {
    private static final String TAG = AlarmDataSource.class.getSimpleName();

    private final SimpleDateFormat iso8601Format = new SimpleDateFormat("yyyy-MM-dd");

    private SQLiteDatabase database;
    private AlarmDbHelper dbHelper;

    // Table fields
    private static final String[] allColumnsDefaults = {AlarmDbHelper.COLUMN_DEFAULTS_ID, AlarmDbHelper.COLUMN_DEFAULTS_DAY_OF_WEEK, AlarmDbHelper.COLUMN_DEFAULTS_STATE, AlarmDbHelper.COLUMN_DEFAULTS_HOUR, AlarmDbHelper.COLUMN_DEFAULTS_MINUTE};
    private static final String[] allColumnsDay = {AlarmDbHelper.COLUMN_DAY_ID, AlarmDbHelper.COLUMN_DAY_DATE, AlarmDbHelper.COLUMN_DAY_STATE, AlarmDbHelper.COLUMN_DAY_HOUR, AlarmDbHelper.COLUMN_DAY_MINUTE};


    public static final int[] allDaysOfWeek = new int[]{Calendar.SUNDAY, Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY, Calendar.THURSDAY, Calendar.FRIDAY, Calendar.SATURDAY};

    public static final int HORIZON_DAYS = 30;

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
        Clock clock = new SystemClock(); // TODO Solve dependency on clock
        String dayOfWeekText = Localization.dayOfWeekToString(defaults.getDayOfWeek(), clock);
        if (defaults.getState() == Defaults.STATE_ENABLED)
            Log.i(TAG, "Set alarm at " + defaults.getHour() + ":" + defaults.getMinute() + " on " + dayOfWeekText);
        else
            Log.i(TAG, "Disabling alarm on " + dayOfWeekText);

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
    public Day loadDayDeep(Calendar date) {
        Day day = loadDay(date);

        if (day == null) {
            day = new Day();
            day.setDate(new GregorianCalendar(date.get(Calendar.YEAR), date.get(Calendar.MONTH), date.get(Calendar.DAY_OF_MONTH)));
            day.setState(Day.STATE_DEFAULT);
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
     * <p/>
     *
     * @param date identifier of the object
     * @return The retrieved object
     */
    private Day loadDay(Calendar date) {
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
        if (day.getState() == Day.STATE_DISABLED)
            Log.i(TAG, "Disable alarm on " + day.getDateTime().getTime());
        else if (day.getState() == Day.STATE_ENABLED)
            Log.i(TAG, "Set alarm on " + day.getDateTime().getTime());
        else
            Log.i(TAG, "Reverting alarm to default on " + day.getDateTime().getTime());

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

    /**
     * Return the alarm time.
     *
     * @param clock clock
     * @return next alarm time
     */
    public Calendar getNextAlarm(Clock clock) {
        Day day = getNextAlarm(clock, null);
        if (day != null) {
            Calendar alarmTime = day.getDateTime();
            Log.v(TAG, "Next alarm is at " + alarmTime.getTime().toString());
            return alarmTime;
        } else {
            Log.v(TAG, "Next alarm is never");
            return null;
        }
    }

    /**
     * Return the nearest Day with alarm such that the Day matches the filter. The filter that such a Day is enabled and not in past is also checked.
     *
     * @param clock clock
     * @return nearest Day with alarm
     */
    public Day getNextAlarm(Clock clock, DayFilter filter) {
        Calendar date = clock.now();

        for (int daysInAdvance = 0; daysInAdvance < HORIZON_DAYS; daysInAdvance++, date.add(Calendar.DATE, 1)) {
            Day day = loadDayDeep(date);

            if (!day.isEnabled()) {
                continue;
            }

            if (day.isPassed(clock)) {
                continue;
            }

            if (filter != null && !filter.match(day)) {
                continue;
            }

            Log.v(TAG, "   The day that satisfies filter is " + day.getDate().getTime());
            return day;
        }

        Log.v(TAG, "Next alarm is never");
        return null;
    }

    /**
     * Return the alarm time of the next alarm.
     * <p/>
     * This is a helper method that opens the database.
     *
     * @param context context
     * @param clock   clock
     * @return next alarm time
     */
    public static Calendar getNextAlarm(Context context, Clock clock) {
        AlarmDataSource datasource = new AlarmDataSource(context);
        datasource.open();

        Calendar alarmTime = datasource.getNextAlarm(clock);

        datasource.close();

        return alarmTime;
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
        StringBuffer str = new StringBuffer();

        Cursor cursor = database.query(AlarmDbHelper.TABLE_DEFAULTS, allColumnsDefaults, null, null, null, null, null);
        str.append("Table Defaults, rows " + cursor.getCount() + "\n");
        str.append("id|DoW|Sta| H | M \n");
        str.append("--+---+---+---+-- \n");

        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            Defaults defaults = cursorToDefaults(cursor);
            str.append(" " + defaults.getId() + " | " + defaults.getDayOfWeek() + " | " + defaults.getState() + " | " + defaults.getHour() + " | " + defaults.getMinute() + "\n");
            cursor.moveToNext();
        }
        cursor.close();

        cursor = database.query(AlarmDbHelper.TABLE_DAY, allColumnsDay, null, null, null, null, null);
        str.append("Table Day, rows " + cursor.getCount() + "\n");
        str.append("id| Date       |Sta| H | M \n");
        str.append("--+------------+---+---+---\n");

        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            Day day = cursorToDay(cursor);
            str.append(" " + day.getId() + " | " + dateToText(day.getDate()) + " | " + day.getState() + " | " + day.getHour() + " | " + day.getMinute() + "\n");
            cursor.moveToNext();
        }
        cursor.close();

        return str.toString();
    }
}
