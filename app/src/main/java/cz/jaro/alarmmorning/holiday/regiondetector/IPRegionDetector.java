package cz.jaro.alarmmorning.holiday.regiondetector;

import android.content.Context;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import cz.jaro.alarmmorning.GlobalManager;

/**
 * Detects the region from IP address. Uses an internet server to get the information.
 */
public class IPRegionDetector extends RegionDetector {

    private static final String TAG = GlobalManager.createLogTag(IPRegionDetector.class);

    private static final String URL = "http://ip-api.com/json";
    public static final String COUNTRY_CODE = "countryCode";

    public IPRegionDetector(Context context) {
        super(context);
    }

    public void detect() {
        try {
            String content = URLHelper.readURL(URL);

            JSONObject json = new JSONObject(content);
            String countryCode = json.getString(IPRegionDetector.COUNTRY_CODE);

            callChangeListener(countryCode, json);
        } catch (IOException e) {
            Log.w(TAG, "Cannot read URL content", e);
        } catch (JSONException e) {
            Log.w(TAG, "Error parsing JSON", e);
        }
    }

}
