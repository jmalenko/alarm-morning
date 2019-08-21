package cz.jaro.alarmmorning.sensor;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.util.Log;

import java.util.Arrays;

import cz.jaro.alarmmorning.GlobalManager;
import cz.jaro.alarmmorning.RingInterface;
import cz.jaro.alarmmorning.SettingsActivity;

/**
 * Provides detection of device shake.
 */
public class Shake extends SensorEventDetector {

    private static final String TAG = GlobalManager.createLogTag(Shake.class);

    private static final int FORCE_THRESHOLD = 50;
    private static final long SHAKE_DURATION = 500 * 1000000; // in nanoseconds

    private float mLastX = -1.0f, mLastY = -1.0f, mLastZ = -1.0f;
    private long mLastEventTime;
    private long mShakeStartTime;

    public Shake(RingInterface ringInterface) {
        super(ringInterface, "shake", Sensor.TYPE_ACCELEROMETER, SettingsActivity.PREF_ACTION_ON_SHAKE);
    }

    protected boolean isFiring(SensorEvent event) {
        Log.v(TAG, "isFiring(values=" + Arrays.toString(event.values) + ")");

        float x = event.values[0];
        float y = event.values[1];
        float z = event.values[2];

        boolean result = false;

        long timeBetweenEvents = event.timestamp - mLastEventTime;
        float force = (Math.abs(x - mLastX) + Math.abs(y - mLastY) + Math.abs(z - mLastZ)) / timeBetweenEvents * 1000000000;
        if (FORCE_THRESHOLD < force) {
            if (mShakeStartTime == 0)
                mShakeStartTime = event.timestamp;

            long duration = event.timestamp - mShakeStartTime;
            if (SHAKE_DURATION <= duration) {
                result = true;
            }
        } else {
            mShakeStartTime = 0;
        }

        mLastEventTime = event.timestamp;
        mLastX = x;
        mLastY = y;
        mLastZ = z;

        return result;
    }

}
