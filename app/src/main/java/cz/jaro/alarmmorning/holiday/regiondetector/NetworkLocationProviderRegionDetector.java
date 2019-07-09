package cz.jaro.alarmmorning.holiday.regiondetector;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.support.v4.app.ActivityCompat;

import cz.jaro.alarmmorning.GlobalManager;

/**
 * Detects the region from Network location provider.
 */
public class NetworkLocationProviderRegionDetector extends LocationProviderRegionDetector {

    private static final String TAG = GlobalManager.createLogTag(NetworkLocationProviderRegionDetector.class);

    public NetworkLocationProviderRegionDetector(Context context) {
        super(context, LocationManager.NETWORK_PROVIDER);
    }

    boolean checkPermissionOK() {
        return ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

}
