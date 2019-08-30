package cz.jaro.alarmmorning.model;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.util.Log;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import cz.jaro.alarmmorning.GlobalManager;
import cz.jaro.alarmmorning.calendar.CalendarUtils;

/**
 * Store objects to a database.
 */
public class AlarmDataSource {
    private static final String TAG = GlobalManager.createLogTag(AlarmDataSource.class);

    private final SimpleDateFormat iso8601Format = new SimpleDateFormat("yyyy-MM-dd", Locale.US);

    private SQLiteDatabase database;
    private final AlarmDbHelper dbHelper;

    // Table fields
    private static final String[] allColumnsDefaults = {AlarmDbHelper.COLUMN_DEFAULTS_ID, AlarmDbHelper.COLUMN_DEFAULTS_DAY_OF_WEEK, AlarmDbHelper.COLUMN_DEFAULTS_STATE, AlarmDbHelper.COLUMN_DEFAULTS_HOUR, AlarmDbHelper.COLUMN_DEFAULTS_MINUTE};
    private static final String[] allColumnsDay = {AlarmDbHelper.COLUMN_DAY_ID, AlarmDbHelper.COLUMN_DAY_DATE, AlarmDbHelper.COLUMN_DAY_STATE, AlarmDbHelper.COLUMN_DAY_HOUR, AlarmDbHelper.COLUMN_DAY_MINUTE};
    private static final String[] allColumnsOneTimeAlarm = {AlarmDbHelper.COLUMN_ONETIMEALARM_ID, AlarmDbHelper.COLUMN_ONETIMEALARM_ALARM_TIME, AlarmDbHelper.COLUMN_ONETIMEALARM_NAME};

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
     * Retrieve a {@code Day} object from the database, together with the {@link Day#defaults} referenced object.
     *
     * @param date identifier of the object
     * @return The retrieved object
     */
    @SuppressWarnings("JavadocReference")
    public Day loadDay(Calendar date) {
        Day day = loadDayShallow(date);

        if (day == null) {
            day = new Day();
            day.setDate(CalendarUtils.beginningOfToday(date));
            day.setState(Day.STATE_RULE);
            day.setHourDay(Day.VALUE_UNSET);
            day.setMinuteDay(Day.VALUE_UNSET);
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
        }
        cursor.close();

        return day;
    }

    /**
     * Store the {@code Day} object in the database.
     * <p/>
     * Note that the referenced {@link Day#defaults} object is not stored.
     *
     * @param day object to be stored
     */
    @SuppressWarnings("JavadocReference")
    public void saveDay(Day day) {
        String dateText = dateToText(day.getDate());

        ContentValues values = new ContentValues();
        values.put(AlarmDbHelper.COLUMN_DAY_DATE, dateText);
        values.put(AlarmDbHelper.COLUMN_DAY_STATE, day.getState());
        values.put(AlarmDbHelper.COLUMN_DAY_HOUR, day.getHourDay());
        values.put(AlarmDbHelper.COLUMN_DAY_MINUTE, day.getMinuteDay());

        long id = database.insertWithOnConflict(AlarmDbHelper.TABLE_DAY, null, values, SQLiteDatabase.CONFLICT_REPLACE);
    }

    /**
     * Retrieve a {@code OneTimeAlarm} object from the database.
     *
     * @param id identifier of the object
     * @return The retrieved object
     */
    public OneTimeAlarm loadOneTimeAlarm(long id) {
        OneTimeAlarm oneTimeAlarm;

        Cursor cursor = database.query(AlarmDbHelper.TABLE_ONETIMEALARM, allColumnsOneTimeAlarm, AlarmDbHelper.COLUMN_ONETIMEALARM_ID + " = ?", new String[]{String.valueOf(id)}, null, null, null);
        if (cursor.getCount() == 0) {
            oneTimeAlarm = null;
        } else {
            cursor.moveToFirst();
            oneTimeAlarm = cursorToOneTimeAlarm(cursor);
        }
        cursor.close();

        return oneTimeAlarm;
    }

    /**
     * Retrieve a set of {@code OneTimeAlarm}s objects from the database.
     *
     * @param from If not null, then only the objects with alarm time on or after {@code from} are retrieved.
     * @return The list of retrieved objects
     */
    public List<OneTimeAlarm> loadOneTimeAlarms(Calendar from) {
        List<OneTimeAlarm> oneTimeAlarms = new ArrayList<>();

        String selection;
        String[] selectionArgs;
        if (from == null) {
            selection = null;
            selectionArgs = null;
        } else {
            long fromMS = calendarToMilliseconds(from);

            selection = "? <= " + AlarmDbHelper.COLUMN_ONETIMEALARM_ALARM_TIME;
            selectionArgs = new String[]{String.valueOf(fromMS)};
        }

        Cursor cursor = database.query(AlarmDbHelper.TABLE_ONETIMEALARM, allColumnsOneTimeAlarm, selection, selectionArgs, null, null, null);
        for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
            OneTimeAlarm oneTimeAlarm = cursorToOneTimeAlarm(cursor);
            oneTimeAlarms.add(oneTimeAlarm);
        }
        cursor.close();

        return oneTimeAlarms;
    }

    /**
     * Retrieve the set of all {@code OneTimeAlarm}s objects from the database.
     *
     * @return The list of all retrieved objects
     */
    public List<OneTimeAlarm> loadOneTimeAlarms() {
        return loadOneTimeAlarms(null);
    }

    /**
     * Store the {@code OneTimeAlarm} object in the database.
     * <p>
     * If the object is inserted into the database, then the {@link OneTimeAlarm#id} is set to a new id.
     *
     * @param oneTimeAlarm object to be stored
     */
    @SuppressWarnings("JavadocReference")
    public void saveOneTimeAlarm(OneTimeAlarm oneTimeAlarm) {
        ContentValues values = new ContentValues();
        values.put(AlarmDbHelper.COLUMN_ONETIMEALARM_ALARM_TIME, oneTimeAlarm.getAlarmTime());
        values.put(AlarmDbHelper.COLUMN_ONETIMEALARM_NAME, oneTimeAlarm.getName());

        if (oneTimeAlarm.getId() == 0) {
            // Insert
            long newID = database.insertWithOnConflict(AlarmDbHelper.TABLE_ONETIMEALARM, null, values, SQLiteDatabase.CONFLICT_REPLACE);
            oneTimeAlarm.setId(newID);
        } else {
            // Update
            long numberOfAffectedRows = database.updateWithOnConflict(AlarmDbHelper.TABLE_ONETIMEALARM, values, AlarmDbHelper.COLUMN_ONETIMEALARM_ID + " = " + oneTimeAlarm.getId(), null, SQLiteDatabase.CONFLICT_REPLACE);
        }
    }

    /**
     * Delete the {@code OneTimeAlarm} object from the database.
     *
     * @param oneTimeAlarm object to be deleted.
     */
    public void deleteOneTimeAlarm(OneTimeAlarm oneTimeAlarm) {
        database.delete(AlarmDbHelper.TABLE_ONETIMEALARM, AlarmDbHelper.COLUMN_ONETIMEALARM_ID + " = ?", new String[]{String.valueOf(oneTimeAlarm.getId())});
    }

    /**
     * Remove one-time alarms with alarm times before or equal to {@code to}.
     *
     * @param to The one-time alarms with alarm times before or equal this argument will be removed.
     * @return The number of affected rows
     */
    public int deleteOneTimeAlarmsOlderThan(Calendar to) {
        String selection;
        String[] selectionArgs;
        if (to == null) {
            selection = null;
            selectionArgs = null;
        } else {
            long toMS = calendarToMilliseconds(to);

            selection = AlarmDbHelper.COLUMN_ONETIMEALARM_ALARM_TIME + " <= ?";
            selectionArgs = new String[]{String.valueOf(toMS)};
        }

        return database.delete(AlarmDbHelper.TABLE_ONETIMEALARM, selection, selectionArgs);
    }

    static private long calendarToMilliseconds(Calendar calendar) {
        TimeZone utcTZ = TimeZone.getTimeZone(OneTimeAlarm.UTC);
        Calendar toUTC = Calendar.getInstance(utcTZ);
        CalendarUtils.copyAllFields(calendar, toUTC);
        return toUTC.getTimeInMillis();
    }

    private String dateToText(Calendar date) {
        return iso8601Format.format(date.getTime());
    }

    private Calendar textToDate(String dateText) {
        try {
            Date date = iso8601Format.parse(dateText);
            return CalendarUtils.newGregorianCalendar(date.getTime());
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
        day.setHourDay(cursor.getInt(3));
        day.setMinuteDay(cursor.getInt(4));
        return day;
    }

    private OneTimeAlarm cursorToOneTimeAlarm(Cursor cursor) {
        OneTimeAlarm oneTimeAlarm = new OneTimeAlarm();

        oneTimeAlarm.setId(cursor.getLong(0));
        oneTimeAlarm.setAlarmTime(cursor.getLong(1));
        oneTimeAlarm.setName(cursor.getString(2));

        return oneTimeAlarm;
    }

    public String dumpDB() {
        StringBuilder str = new StringBuilder();

        // Table Defaults
        Cursor cursor = database.query(AlarmDbHelper.TABLE_DEFAULTS, allColumnsDefaults, null, null, null, null, null);
        str.append("Table Defaults, rows ").append(cursor.getCount()).append("\n");
        str.append("id |DoW|Sta| H | M \n");
        str.append("---+---+---+---+-- \n");

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

        // Table Day
        cursor = database.query(AlarmDbHelper.TABLE_DAY, allColumnsDay, null, null, null, null, null);
        str.append("Table Day, rows ").append(cursor.getCount()).append("\n");
        str.append("id | Date       |Sta| H | M \n");
        str.append("---+------------+---+---+---\n");

        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            Day day = cursorToDay(cursor);
            str.
                    append(" ").append(day.getId()).
                    append(" | ").append(dateToText(day.getDate())).
                    append(" | ").append(day.getState()).
                    append(" | ").append(day.getHourDay()).
                    append(" | ").append(day.getMinuteDay()).append("\n");
            cursor.moveToNext();
        }
        cursor.close();

        // Table OneTimeAlarm
        cursor = database.query(AlarmDbHelper.TABLE_ONETIMEALARM, allColumnsOneTimeAlarm, null, null, null, null, null);
        str.append("Table OneTimeAlarm, rows ").append(cursor.getCount()).append("\n");
        str.append("id | Date       | H | M \n");
        str.append("---+------------+---+---\n");

        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            OneTimeAlarm oneTimeAlarm = cursorToOneTimeAlarm(cursor);
            str.
                    append(" ").append(oneTimeAlarm.getId()).
                    append(" | ").append(dateToText(oneTimeAlarm.getDateTime())).
                    append(" | ").append(oneTimeAlarm.getDateTime().get(Calendar.HOUR_OF_DAY)).
                    append(" | ").append(oneTimeAlarm.getDateTime().get(Calendar.MINUTE)).append("\n");
            cursor.moveToNext();
        }
        cursor.close();

        return str.toString();
    }

    /**
     * Reset the database to the initial state.
     * <p>
     * Deletes all Days and disables the Defaults, deletes all one-time alarms.
     */
    public void resetDatabase() {
        deleteAllDays();
        AlarmDbHelper.resetDefaults(database);

        deleteAllOneTimeAlarms();
    }

    private void deleteAllDays() {
        database.delete(AlarmDbHelper.TABLE_DAY, null, null);
    }

    private void deleteAllOneTimeAlarms() {
        database.delete(AlarmDbHelper.TABLE_ONETIMEALARM, null, null);
    }

}
