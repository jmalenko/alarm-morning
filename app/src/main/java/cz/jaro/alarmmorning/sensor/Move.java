package cz.jaro.alarmmorning.sensor;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.util.Log;
import android.widget.TextView;

import java.util.Arrays;

import cz.jaro.alarmmorning.R;
import cz.jaro.alarmmorning.RingInterface;
import cz.jaro.alarmmorning.SettingsFragment;

/**
 * Provides detection of device move.
 */
public class Move extends SensorEventDetector {

    private static final String TAG = Move.class.getSimpleName();

    private boolean mInitialized;
    private double mAccelCurrent;
    private double mAccel;

    public Move(RingInterface ringInterface) {
        super(ringInterface, "move", Sensor.TYPE_ACCELEROMETER, SettingsFragment.PREF_ACTION_ON_SHAKE);
    }

    protected boolean isFiring(SensorEvent event) {
        Log.v(TAG, "isFiring(values=" + Arrays.toString(event.values) + ")");

        float x = event.values[0];
        float y = event.values[1];
        float z = event.values[2];

        if (!mInitialized) {
            mInitialized = true;
            mAccelCurrent = Math.sqrt(x * x + y * y + z * z);
            return false;
        } else {
            double mAccelLast = mAccelCurrent;
            mAccelCurrent = Math.sqrt(x * x + y * y + z * z);
            double delta = mAccelCurrent - mAccelLast;
            mAccel = mAccel * 0.9f + delta;

            boolean result = 1 < Math.abs(mAccel);

            TextView textView = (TextView) ringInterface.findViewByIdI(R.id.move);
            textView.setText("Move mAccel=" + String.format("%.2f", mAccel) + ", mAccelCurrent=" + String.format("%.2f", mAccelCurrent) + "." + (result ? " MOVED" : ""));

            return result;
        }
    }
}
