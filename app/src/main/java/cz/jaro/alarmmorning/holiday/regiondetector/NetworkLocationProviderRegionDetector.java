package cz.jaro.alarmmorning.holiday.regiondetector;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.LocationManager;

import androidx.core.app.ActivityCompat;

/**
 * Detects the region from Network location provider.
 */
public class NetworkLocationProviderRegionDetector extends LocationProviderRegionDetector {

    public NetworkLocationProviderRegionDetector(Context context) {
        super(context, LocationManager.NETWORK_PROVIDER);
    }

    boolean checkPermissionOK() {
        return ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

}
