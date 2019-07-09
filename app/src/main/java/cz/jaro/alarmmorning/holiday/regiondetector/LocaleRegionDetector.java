package cz.jaro.alarmmorning.holiday.regiondetector;

import android.content.Context;

import java.util.Locale;

import cz.jaro.alarmmorning.GlobalManager;

/**
 * Detects the region from Locale.
 */
public class LocaleRegionDetector extends RegionDetector {

    private static final String TAG = GlobalManager.createLogTag(LocaleRegionDetector.class);

    public LocaleRegionDetector(Context context) {
        super(context);
    }

    public void detect() {
        Locale locale = Locale.getDefault();
        String country = locale.getCountry();
        callChangeListener(country, locale);
    }

}
