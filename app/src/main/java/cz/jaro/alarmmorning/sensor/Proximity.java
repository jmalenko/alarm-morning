package cz.jaro.alarmmorning.sensor;

import android.hardware.Sensor;
import android.hardware.SensorEvent;

import java.util.Arrays;

import cz.jaro.alarmmorning.MyLog;
import cz.jaro.alarmmorning.RingInterface;
import cz.jaro.alarmmorning.SettingsActivity;

/**
 * Provides detection of proximity a near object to the device.
 */
public class Proximity extends SensorEventDetector {

    public static final double DIFF_MIN = 0.5;

    private boolean mInitialized;
    private float mMax;

    public Proximity(RingInterface ringInterface) {
        super(ringInterface, "proximity", Sensor.TYPE_PROXIMITY, SettingsActivity.PREF_ACTION_ON_PROXIMITY);
    }

    protected boolean isFiring(SensorEvent event) {
        MyLog.v("isFiring(values=" + Arrays.toString(event.values) + ")");

        float distance = event.values[0];

        boolean result = false;

        if (!mInitialized) {
            mInitialized = true;
            mMax = distance;
        } else {
            mMax = Math.max(mMax, distance);
            float diffMax = mMax - distance;
            result = distance < mSensor.getMaximumRange() && DIFF_MIN < diffMax;
        }
        return result;
    }

}
