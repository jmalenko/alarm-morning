package cz.jaro.alarmmorning;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import java.util.NoSuchElementException;
import java.util.Set;

public class SharedPreferencessHelper {

    private static final String TAG = GlobalManager.createLogTag(SharedPreferencessHelper.class);

    public static void save(String key, Object value) {
        Log.v(TAG, "save(key=" + key + ", value=" + value + ")");

        Context context = AlarmMorningApplication.getAppContext();
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = preferences.edit();

        if (value instanceof String) {
            editor.putString(key, (String) value);
        } else if (value instanceof Set<?>) {
            try {
                editor.putStringSet(key, (Set<String>) value);
            } catch (ClassCastException e) {
                Log.e(TAG, "Cannot cast Set<?> to Set<String>");
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
        Log.v(TAG, "load(key=" + key + ")");

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

    public static void remove(String key) {
        Log.v(TAG, "remove(key=" + key + ")");

        Context context = AlarmMorningApplication.getAppContext();
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = preferences.edit();

        editor.remove(key);

        editor.apply();
    }
}
