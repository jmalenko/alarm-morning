package cz.jaro.alarmmorning.sensor;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.util.Log;

import java.util.Arrays;

import cz.jaro.alarmmorning.GlobalManager;
import cz.jaro.alarmmorning.RingInterface;
import cz.jaro.alarmmorning.SettingsActivity;

/**
 * Provides detection of device flip.
 */
public class Flip extends SensorEventDetector {

    private static final String TAG = GlobalManager.createLogTag(Flip.class);

    private final static float THRESHOLD = 5; // when the device is still facing up and flips, the value goes from 9.81 do -9.81

    private boolean mInitialized;
    private boolean mUp;

    public Flip(RingInterface ringInterface) {
        super(ringInterface, "flip", Sensor.TYPE_ACCELEROMETER, SettingsActivity.PREF_ACTION_ON_FLIP);
    }

    protected boolean isFiring(SensorEvent event) {
        Log.v(TAG, "isFiring(values=" + Arrays.toString(event.values) + ")");

        float z = event.values[2];

        boolean result = false;

        if (!mInitialized) {
            if (THRESHOLD < z) {
                mInitialized = true;
                mUp = true;
            } else if (z < -THRESHOLD) {
                mInitialized = true;
                mUp = false;
            }
        } else {
            if (THRESHOLD < z) {
                result = !mUp;
                mUp = true;
            } else if (z < -THRESHOLD) {
                result = mUp;
                mUp = false;
            }
        }

        return result;
    }

}
