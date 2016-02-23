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
 * Provides detection of proximity a near object to the device.
 */
public class Proximity extends SensorEventDetector {

    private static final String TAG = Proximity.class.getSimpleName();

    public Proximity(RingInterface ringInterface) {
        super(ringInterface, "proximity", Sensor.TYPE_PROXIMITY, SettingsFragment.PREF_ACTION_ON_PROXIMITY);
    }

    protected boolean isFiring(SensorEvent event) {
        Log.v(TAG, "isFiring(values=" + Arrays.toString(event.values) + ")");

        float distance = event.values[0];

        boolean result = distance < mSensor.getMaximumRange();

        TextView textView = (TextView) ringInterface.findViewByIdI(R.id.proximity);
        textView.setText("Proximity is " + String.format("%.2f", distance) + "." + (result ? " NEAR" : ""));

        return result;
    }

}
