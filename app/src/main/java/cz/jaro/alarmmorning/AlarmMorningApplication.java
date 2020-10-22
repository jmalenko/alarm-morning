package cz.jaro.alarmmorning;

import android.app.Application;
import android.content.Context;

import com.google.firebase.crashlytics.FirebaseCrashlytics;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;

// Inspired by https://nfrolov.wordpress.com/2014/07/12/android-using-context-statically-and-in-singletons/

/**
 * Maintains the application state.
 */
public class AlarmMorningApplication extends Application {

    private static Context appContext;

    @Override
    public void onCreate() {
        MyLog.v("onCreate()");
        super.onCreate();
        appContext = this;

        // Add values to crash logs

        // Database dump
        GlobalManager globalManager = GlobalManager.getInstance();
        FirebaseCrashlytics crashlytics = FirebaseCrashlytics.getInstance();
        crashlytics.setCustomKey("database_dump", globalManager.dumpDB());

        // Setup data for Crashlytics
        Analytics analytics = new Analytics();
        analytics.setContext(appContext);
        JSONObject configuration = analytics.createConfiguration();

        Iterator<?> keys = configuration.keys();
        while (keys.hasNext()) {
            String key = (String) keys.next();
            try {
                Object value = configuration.get(key);
                crashlytics.setCustomKey(key, value.toString());
            } catch (JSONException e) {
                MyLog.w("Cannot get value for key " + key);
            }
        }
    }

    public static Context getAppContext() {
        return appContext;
    }
}