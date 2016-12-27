package cz.jaro.alarmmorning.holiday.regiondetector;

import android.content.Context;
import android.telephony.TelephonyManager;

/**
 * Detects the region from telephony services.
 */
public class TelephonyRegionDetector extends RegionDetector {

    private static final String TAG = TelephonyRegionDetector.class.getSimpleName();

    public TelephonyRegionDetector(Context context) {
        super(context);
    }

    public void detect() {
        TelephonyManager telephonyManager = (TelephonyManager) getContext().getSystemService(Context.TELEPHONY_SERVICE);

        String countryCode = telephonyManager.getNetworkCountryIso().toUpperCase();
        callChangeListener(countryCode, telephonyManager);
    }

}
