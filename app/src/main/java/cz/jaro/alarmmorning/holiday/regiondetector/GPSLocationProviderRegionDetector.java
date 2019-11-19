package cz.jaro.alarmmorning.holiday.regiondetector;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.LocationManager;

import androidx.core.app.ActivityCompat;

/**
 * Detects the region from GPS location provider.
 */
public class GPSLocationProviderRegionDetector extends LocationProviderRegionDetector {

    public GPSLocationProviderRegionDetector(Context context) {
        super(context, LocationManager.GPS_PROVIDER);
    }

    boolean checkPermissionOK() {
        return ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

}
