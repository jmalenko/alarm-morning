package cz.jaro.alarmmorning;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class JSONSharedPreferences {
    private static final String PREFIX = "json_";

    public static void saveJSONObject(Context c, String key, JSONObject object) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(c);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(JSONSharedPreferences.PREFIX + key, object.toString());
        editor.apply();
    }

    public static void saveJSONArray(Context c, String key, JSONArray array) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(c);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(JSONSharedPreferences.PREFIX + key, array.toString());
        editor.apply();
    }

    public static JSONObject loadJSONObject(Context c, String key) throws JSONException {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(c);
        return new JSONObject(settings.getString(JSONSharedPreferences.PREFIX + key, "{}"));
    }

    public static JSONArray loadJSONArray(Context c, String key) throws JSONException {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(c);
        return new JSONArray(settings.getString(JSONSharedPreferences.PREFIX + key, "[]"));
    }

    public static void remove(Context c, String key) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(c);
        if (settings.contains(JSONSharedPreferences.PREFIX + key)) {
            SharedPreferences.Editor editor = settings.edit();
            editor.remove(JSONSharedPreferences.PREFIX + key);
            editor.apply();
        }
    }
}