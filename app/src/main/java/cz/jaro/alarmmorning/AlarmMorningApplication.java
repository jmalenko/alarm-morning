package cz.jaro.alarmmorning;

import android.app.Application;
import android.content.Context;

// Inspired by https://nfrolov.wordpress.com/2014/07/12/android-using-context-statically-and-in-singletons/

/**
 * Maintains the application state.
 */
public class AlarmMorningApplication extends Application {

    private static Context appContext;

    @Override
    public void onCreate() {
        super.onCreate();
        appContext = this;
    }

    public static Context getAppContext() {
        return appContext;
    }
}