package cz.jaro.alarmmorning.model;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by jmalenko on 16.12.2015.
 */

public class AlarmDbHelper extends SQLiteOpenHelper {

    private static final String TAG = AlarmDbHelper.class.getSimpleName();

    private static final String DATABASE_NAME = "alarms.db";

    public static final String TABLE_DEFAULTS = "defaults";
    public static final String COLUMN_DEFAULTS_ID = "_id";
    public static final String COLUMN_DEFAULTS_DAY_OF_WEEK = "day_of_week";
    public static final String COLUMN_DEFAULTS_STATE = "state";
    public static final String COLUMN_DEFAULTS_HOUR = "hour";
    public static final String COLUMN_DEFAULTS_MINUTE = "minute";

    public static final String TABLE_DAY = "day";
    public static final String COLUMN_DAY_ID = "_id";
    public static final String COLUMN_DAY_DATE = "date";
    public static final String COLUMN_DAY_STATE = "state";
    public static final String COLUMN_DAY_HOUR = "hour";
    public static final String COLUMN_DAY_MINUTE = "minute";

    public AlarmDbHelper(Context context) {
        super(context, DATABASE_NAME, null, PATCHES.length);
    }

    @Override
    public void onCreate(SQLiteDatabase database) {
        for (Patch PATCH : PATCHES) {
            PATCH.apply(database);
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        for (int i = oldVersion; i < newVersion; i++) {
            PATCHES[i].apply(db);
        }
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        for (int i = oldVersion; i > newVersion; i--) {
            PATCHES[i - 1].revert(db);
        }
    }

    private abstract static class Patch {
        public abstract void apply(SQLiteDatabase db);

        public abstract void revert(SQLiteDatabase db);
    }

    private static final Patch[] PATCHES = new Patch[]{new Patch() {
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
            ContentValues values = new ContentValues();
            values.put(COLUMN_DEFAULTS_STATE, AlarmDataSource.DEFAULT_STATE_DISABLED);
            values.put(COLUMN_DEFAULTS_HOUR, 7);
            values.put(COLUMN_DEFAULTS_MINUTE, 0);
            for (int dayOfWeek : AlarmDataSource.allDaysOfWeek) {
                values.put(COLUMN_DEFAULTS_DAY_OF_WEEK, dayOfWeek);
                database.insert(TABLE_DEFAULTS, null, values);
            }
        }

        public void revert(SQLiteDatabase database) {
            database.execSQL("DROP TABLE IF EXISTS " + TABLE_DEFAULTS);
            database.execSQL("DROP TABLE IF EXISTS " + TABLE_DAY);
        }
    }};

}
