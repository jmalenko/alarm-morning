package cz.jaro.alarmmorning.model;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import cz.jaro.alarmmorning.MyLog;

/**
 * Represent changes of the database model.
 * <p/>
 * The changes are represented as a difference (forward and backward patches) between two subsequent versions.
 * Invariant: the database version V has applied exactly the patches 0..V.
 */
public class AlarmDbHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "alarms.db";

    static final String TABLE_DEFAULTS = "defaults";
    static final String COLUMN_DEFAULTS_ID = "_id";
    static final String COLUMN_DEFAULTS_DAY_OF_WEEK = "day_of_week";
    static final String COLUMN_DEFAULTS_STATE = "state";
    static final String COLUMN_DEFAULTS_HOUR = "hour";
    static final String COLUMN_DEFAULTS_MINUTE = "minute";

    static final String TABLE_DAY = "day";
    static final String COLUMN_DAY_ID = "_id";
    static final String COLUMN_DAY_DATE = "date";
    static final String COLUMN_DAY_STATE = "state";
    static final String COLUMN_DAY_HOUR = "hour";
    static final String COLUMN_DAY_MINUTE = "minute";

    static final String TABLE_ONETIMEALARM = "one_time_alarm";
    static final String COLUMN_ONETIMEALARM_ID = "_id";
    static final String COLUMN_ONETIMEALARM_ALARM_TIME = "alarm_time"; // Note: the alarm time is always stored in UTC timezone (irrespective of default locale)
    static final String COLUMN_ONETIMEALARM_NAME = "name";

    /**
     * Hour of default alarm time. Used to initialize configuration.
     */
    public static final int DEFAULT_ALARM_HOUR = 7;

    /**
     * Minute of default alarm time. Used to initialize configuration.
     */
    public static final int DEFAULT_ALARM_MINUTE = 0;

    AlarmDbHelper(Context context) {
        super(context, DATABASE_NAME, null, PATCHES.length);
    }

    @Override
    public void onCreate(SQLiteDatabase database) {
        MyLog.d("Creating database");
        for (Patch PATCH : PATCHES) {
            PATCH.apply(database);
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        MyLog.d("Upgrading database from " + oldVersion + " to " + newVersion);
        for (int i = oldVersion; i < newVersion; i++) {
            MyLog.d("Applying patch " + i);
            PATCHES[i].apply(db);
        }
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        MyLog.d("Downgrading database from " + oldVersion + " to " + newVersion);
        for (int i = oldVersion; i > newVersion; i--) {
            MyLog.d("Reverting patch " + i);
            PATCHES[i - 1].revert(db);
        }
    }

    /**
     * Represents changes of database model.
     */
    private abstract static class Patch {

        /**
         * Change the database forward.
         *
         * @param db database
         */
        public abstract void apply(SQLiteDatabase db);

        /**
         * Change the database backward.
         *
         * @param db database
         */
        public abstract void revert(SQLiteDatabase db);
    }

    /**
     * Set of patches.
     */
    private static final Patch[] PATCHES = new Patch[]{
            new Patch() { // Version 0
                public void apply(SQLiteDatabase database) {
                    String DATABASE_CREATE_TABLE_DEFAULTS = "create table " + TABLE_DEFAULTS + "(" +
                            COLUMN_DEFAULTS_ID + " integer primary key autoincrement, " +
                            COLUMN_DEFAULTS_DAY_OF_WEEK + " integer unique not null," +
                            COLUMN_DEFAULTS_STATE + " integer not null," +
                            COLUMN_DEFAULTS_HOUR + " integer," +
                            COLUMN_DEFAULTS_MINUTE + " integer" +
                            ");";
                    String DATABASE_CREATE_TABLE_DAY = "create table " + TABLE_DAY + "(" +
                            COLUMN_DAY_ID + " integer primary key autoincrement, " +
                            COLUMN_DAY_DATE + " text unique not null," +
                            COLUMN_DAY_STATE + " integer not null," +
                            COLUMN_DAY_HOUR + " integer," +
                            COLUMN_DAY_MINUTE + " integer" +
                            ");";

                    // Create tables
                    database.execSQL(DATABASE_CREATE_TABLE_DEFAULTS);
                    database.execSQL(DATABASE_CREATE_TABLE_DAY);

                    // Initialize
                    resetDefaults(database);
                }

                public void revert(SQLiteDatabase database) {
                    database.execSQL("DROP TABLE IF EXISTS " + TABLE_DEFAULTS);
                    database.execSQL("DROP TABLE IF EXISTS " + TABLE_DAY);
                }
            },
            new Patch() { // Version 1
                public void apply(SQLiteDatabase database) {
                    String CREATE_TABLE_ALARM = "CREATE TABLE " + TABLE_ONETIMEALARM + "(" +
                            COLUMN_ONETIMEALARM_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                            COLUMN_ONETIMEALARM_ALARM_TIME + " INT," +
                            COLUMN_ONETIMEALARM_NAME + " TEXT" +
                            ")";

                    // Create table
                    database.execSQL(CREATE_TABLE_ALARM);
                }

                public void revert(SQLiteDatabase database) {
                    String DROP_TABLE_ALARM = "DROP TABLE IF EXISTS " + TABLE_ONETIMEALARM;

                    // Drop table
                    database.execSQL(DROP_TABLE_ALARM);
                }
            }
    };

    static void resetDefaults(SQLiteDatabase database) {
        ContentValues values = new ContentValues();
        values.put(COLUMN_DEFAULTS_STATE, Defaults.STATE_DISABLED);
        values.put(COLUMN_DEFAULTS_HOUR, DEFAULT_ALARM_HOUR);
        values.put(COLUMN_DEFAULTS_MINUTE, DEFAULT_ALARM_MINUTE);
        for (int dayOfWeek : AlarmDataSource.allDaysOfWeek) {
            values.put(COLUMN_DEFAULTS_DAY_OF_WEEK, dayOfWeek);
            database.insertWithOnConflict(TABLE_DEFAULTS, null, values, SQLiteDatabase.CONFLICT_REPLACE);
        }
    }

}
