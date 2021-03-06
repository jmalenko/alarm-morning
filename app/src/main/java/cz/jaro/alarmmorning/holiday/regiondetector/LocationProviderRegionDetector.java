package cz.jaro.alarmmorning.holiday.regiondetector;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import cz.jaro.alarmmorning.MyLog;

/**
 * Detects the region from Location Provider.
 */
public class LocationProviderRegionDetector extends RegionDetector {

    private final String provider;

    public LocationProviderRegionDetector(Context context, String provider) {
        super(context);
        this.provider = provider;
    }

    public void detect() {
        LocationManager locationManager = (LocationManager) getContext().getSystemService(Context.LOCATION_SERVICE);

        LocationListener locationListener = new LocationListener() {
            public void onLocationChanged(Location location) {
                MyLog.v("onLocationChanged()");
                useNewLocation(location);
            }

            public void onStatusChanged(String provider, int status, Bundle extras) {
                MyLog.v("onStatusChanged()");
            }

            public void onProviderEnabled(String provider) {
                MyLog.v("onProviderEnabled()");
            }

            public void onProviderDisabled(String provider) {
                MyLog.v("onProviderDisabled()");
            }
        };

        try {
            Location location = locationManager.getLastKnownLocation(provider);
            if (location != null) {
                useNewLocation(location);
            }

            locationManager.requestLocationUpdates(provider, 0, 0, locationListener);
        } catch (SecurityException e) {
            MyLog.w("Location permission missing", e);
        } catch (RuntimeException e) {
            MyLog.w("Non-specific exception", e);
        }
    }

    boolean checkPermissionOK() {
        return true;
    }

    private void useNewLocation(Location location) {
        MyLog.d("useNewLocation()");

        try {
            double latitude = location.getLatitude();
            double longitude = location.getLongitude();
            String countryCode = getCountryCode(latitude, longitude);
            callChangeListener(countryCode, location);
        } catch (IOException e) {
            MyLog.w("Cannot get address from coordinates", e);
        }
    }

    String getCountryCode(double lat, double lng) throws IOException {
        if (!Geocoder.isPresent()) {
            MyLog.e("Geocoder not present");
            return null;
        }
        Geocoder geocoder = new Geocoder(getContext(), Locale.getDefault());
        List<Address> addresses = geocoder.getFromLocation(lat, lng, 1);
        Address obj = addresses.get(0);

//        String address = obj.getAddressLine(0);
//        address = address + "\n" + obj.getLatitude() + " " + obj.getLongitude();
//        address = address + "\n" + obj.getCountryName();
//        address = address + "\n" + obj.getCountryCode();
//        address = address + "\n" + obj.getAdminArea(); // State
//        address = address + "\n" + obj.getPostalCode();
//        address = address + "\n" + obj.getSubAdminArea(); // City
//        address = address + "\n" + obj.getLocality();
//        address = address + "\n" + obj.getSubThoroughfare();

        return obj.getCountryCode();
    }

}
