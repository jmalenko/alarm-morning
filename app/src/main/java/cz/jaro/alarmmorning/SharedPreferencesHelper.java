package cz.jaro.alarmmorning;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.util.NoSuchElementException;
import java.util.Set;

public class SharedPreferencesHelper {

    public static void save(String key, Object value) {
        MyLog.v("save(key=" + key + ", value=" + value + ")");

        Context context = AlarmMorningApplication.getAppContext();
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = preferences.edit();

        if (value instanceof String) {
            editor.putString(key, (String) value);
        } else if (value instanceof Set<?>) {
            try {
                editor.putStringSet(key, (Set<String>) value);
            } catch (ClassCastException e) {
                MyLog.e("Cannot cast Set<?> to Set<String>");
                throw e;
            }
        } else if (value instanceof Integer) {
            editor.putInt(key, (int) value);
        } else if (value instanceof Long) {
            editor.putLong(key, (long) value);
        } else if (value instanceof Boolean) {
            editor.putBoolean(key, (boolean) value);
        } else if (value instanceof Float) {
            editor.putFloat(key, (float) value);
        }

        editor.apply();
    }

    public static Object load(String key) throws NoSuchElementException {
        MyLog.v("load(key=" + key + ")");

        Context context = AlarmMorningApplication.getAppContext();
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);

        // Check that the key exists

        if (!preferences.contains(key)) {
            throw new NoSuchElementException("The key " + key + " is not contained in the SharedPreferences");
        }

        // Try to lead each type
        // Note; the default can be anything as we are sure the key exists
        try {
            return preferences.getString(key, null);
        } catch (ClassCastException e) {
            // Intentionally empty
        }

        try {
            return preferences.getStringSet(key, null);
        } catch (ClassCastException e) {
            // Intentionally empty
        }

        try {
            return preferences.getInt(key, 0);
        } catch (ClassCastException e) {
            // Intentionally empty
        }

        try {
            return preferences.getLong(key, 0);
        } catch (ClassCastException e) {
            // Intentionally empty
        }

        try {
            return preferences.getBoolean(key, false);
        } catch (ClassCastException e) {
            // Intentionally empty
        }

        try {
            return preferences.getFloat(key, 0);
        } catch (ClassCastException e) {
            // Intentionally empty
        }

        // The record does NOT exist
        throw new IllegalStateException("This should not happen");
    }

    public static Object load(String key, Object defaultValue) {
        try {
            return load(key);
        } catch (NoSuchElementException e) {
            return defaultValue;
        }
    }

    public static void remove(String key) {
        MyLog.v("remove(key=" + key + ")");

        Context context = AlarmMorningApplication.getAppContext();
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = preferences.edit();

        editor.remove(key);

        editor.apply();
    }

    public static boolean contains(String key) {
        MyLog.v("contains(key=" + key + ")");

        Context context = AlarmMorningApplication.getAppContext();
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);

        return preferences.contains(key);
    }

    public static void clear() {
        MyLog.v("clear()");

        Context context = AlarmMorningApplication.getAppContext();
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = preferences.edit();

        editor.clear();

        editor.apply();
    }
}
