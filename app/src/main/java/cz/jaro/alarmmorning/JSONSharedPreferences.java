package cz.jaro.alarmmorning;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class JSONSharedPreferences {
    private static final String PREFIX = "json_";

    public static void saveJSONObject(String key, JSONObject object) {
        SharedPreferencesHelper.save(JSONSharedPreferences.PREFIX + key, object.toString());
    }

    public static void saveJSONArray(String key, JSONArray array) {
        SharedPreferencesHelper.save(JSONSharedPreferences.PREFIX + key, array.toString());
    }

    public static JSONObject loadJSONObject(String key) throws JSONException {
        return new JSONObject((String) SharedPreferencesHelper.load(JSONSharedPreferences.PREFIX + key, "{}"));
    }

    public static JSONArray loadJSONArray(String key) throws JSONException {
        return new JSONArray((String) SharedPreferencesHelper.load(JSONSharedPreferences.PREFIX + key, "[]"));
    }

    public static void remove(String key) {
        SharedPreferencesHelper.remove(JSONSharedPreferences.PREFIX + key);
    }
}