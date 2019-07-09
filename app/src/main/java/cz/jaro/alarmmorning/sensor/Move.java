package cz.jaro.alarmmorning.sensor;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.util.Log;

import java.util.Arrays;

import cz.jaro.alarmmorning.GlobalManager;
import cz.jaro.alarmmorning.RingInterface;
import cz.jaro.alarmmorning.SettingsActivity;

/**
 * Provides detection of device move.
 */
public class Move extends SensorEventDetector {

    private static final String TAG = GlobalManager.createLogTag(Move.class);

    private boolean mInitialized;
    private double mAccelCurrent;
    private double mAccel;

    public Move(RingInterface ringInterface) {
        super(ringInterface, "move", Sensor.TYPE_ACCELEROMETER, SettingsActivity.PREF_ACTION_ON_MOVE);
    }

    protected boolean isFiring(SensorEvent event) {
        Log.v(TAG, "isFiring(values=" + Arrays.toString(event.values) + ")");

        float x = event.values[0];
        float y = event.values[1];
        float z = event.values[2];

        mAccelCurrent = Math.sqrt(x * x + y * y + z * z);

        if (!mInitialized) {
            mInitialized = true;
            return false;
        } else {
            double mAccelLast = mAccelCurrent;
            double delta = mAccelCurrent - mAccelLast;
            mAccel = mAccel * 0.9f + delta;

            boolean result = 1 < Math.abs(mAccel);

            return result;
        }
    }
}
