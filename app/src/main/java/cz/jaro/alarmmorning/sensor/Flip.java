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
 * Provides detection of device flip.
 */
public class Flip extends SensorEventDetector {

    private static final String TAG = Flip.class.getSimpleName();

    public Flip(RingInterface ringInterface) {
        super(ringInterface, "flip", Sensor.TYPE_ACCELEROMETER, SettingsFragment.PREF_ACTION_ON_FLIP);
    }

    protected boolean isFiring(SensorEvent event) {
        Log.v(TAG, "isFiring(values=" + Arrays.toString(event.values) + ")");

        float z = event.values[2];

        boolean result = z < 0;

        TextView textView = (TextView) ringInterface.findViewByIdI(R.id.flip);
        textView.setText("Flip is " + (result ? "down" : "up") + ". value="+ String.format("%.2f", z) +  (result ? " FLIPPED" : ""));

        return result;
    }

}
