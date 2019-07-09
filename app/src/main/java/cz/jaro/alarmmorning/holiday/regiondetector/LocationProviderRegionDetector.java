package cz.jaro.alarmmorning.holiday.regiondetector;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;

import java.util.List;
import java.util.Locale;

import cz.jaro.alarmmorning.GlobalManager;

/**
 * Detects the region from Location Provider.
 */
public class LocationProviderRegionDetector extends RegionDetector {

    private static final String TAG = GlobalManager.createLogTag(LocationProviderRegionDetector.class);

    private String provider;

    public LocationProviderRegionDetector(Context context, String provider) {
        super(context);
        this.provider = provider;
    }

    public void detect() {
        LocationManager locationManager = (LocationManager) getContext().getSystemService(Context.LOCATION_SERVICE);

        LocationListener locationListener = new LocationListener() {
            public void onLocationChanged(Location location) {
                Log.v(TAG, "onLocationChanged()");
                useNewLocation(location);
            }

            public void onStatusChanged(String provider, int status, Bundle extras) {
                Log.v(TAG, "onStatusChanged()");
            }

            public void onProviderEnabled(String provider) {
                Log.v(TAG, "onProviderEnabled()");
            }

            public void onProviderDisabled(String provider) {
                Log.v(TAG, "onProviderDisabled()");
            }
        };

        try {
            Location location = locationManager.getLastKnownLocation(provider);
            if (location != null) {
                useNewLocation(location);
            }

            locationManager.requestLocationUpdates(provider, 0, 0, locationListener);
        } catch (SecurityException e) {
            Log.w(TAG, "Location permission missing", e);
        } catch (RuntimeException e) {
            Log.w(TAG, e);
        }
    }

    boolean checkPermissionOK() {
        return true;
    }

    private void useNewLocation(Location location) {
        Log.d(TAG, "useNewLocation()");

        double latitude = location.getLatitude();
        double longitude = location.getLongitude();
        String countryCode = getCountryCode(latitude, longitude);

        if (countryCode != null) {
            callChangeListener(countryCode, location);
        }
    }

    String getCountryCode(double lat, double lng) {
        if (!Geocoder.isPresent()) {
            Log.e(TAG, "Geocoder not present");
            return null;
        }
        Geocoder geocoder = new Geocoder(getContext(), Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(lat, lng, 1);
            Address obj = addresses.get(0);

//            String address = obj.getAddressLine(0);
//            address = address + "\n" + obj.getLatitude() + " " + obj.getLongitude();
//            address = address + "\n" + obj.getCountryName();
//            address = address + "\n" + obj.getCountryCode();
//            address = address + "\n" + obj.getAdminArea(); // State
//            address = address + "\n" + obj.getPostalCode();
//            address = address + "\n" + obj.getSubAdminArea(); // City
//            address = address + "\n" + obj.getLocality();
//            address = address + "\n" + obj.getSubThoroughfare();

            return obj.getCountryCode();
        } catch (Exception e) {
            Log.w(TAG, "Cannot get address from coordinates", e);
            return null;
        }
    }

}
