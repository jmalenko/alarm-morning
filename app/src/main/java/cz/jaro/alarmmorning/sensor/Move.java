package cz.jaro.alarmmorning.sensor;

import android.hardware.Sensor;
import android.hardware.SensorEvent;

import java.text.DecimalFormat;
import java.util.Arrays;

import cz.jaro.alarmmorning.MyLog;
import cz.jaro.alarmmorning.RingInterface;
import cz.jaro.alarmmorning.SettingsActivity;

/**
 * Provides detection of device move.
 */
public class Move extends SensorEventDetector {

    private double[] gravity = new double[3];

    public Move(RingInterface ringInterface) {
        super(ringInterface, "move", Sensor.TYPE_ACCELEROMETER, SettingsActivity.PREF_ACTION_ON_MOVE);
    }

    protected boolean isFiring(SensorEvent event) {
        MyLog.v("isFiring(values=" + Arrays.toString(event.values) + ")");

        final double alpha = 0.8;
        gravity[0] = alpha * gravity[0] + (1 - alpha) * event.values[0];
        gravity[1] = alpha * gravity[1] + (1 - alpha) * event.values[1];
        gravity[2] = alpha * gravity[2] + (1 - alpha) * event.values[2];

        double[] linear_acceleration = new double[3];
        linear_acceleration[0] = event.values[0] - gravity[0];
        linear_acceleration[1] = event.values[1] - gravity[1];
        linear_acceleration[2] = event.values[2] - gravity[2];

        double acceleration_size = size3(linear_acceleration);

        // The detected gravity must be around 9.81
        final double gravity_size_real = 9.81;
        final double delta = 2;
        double gravity_size = size3(gravity);
        boolean gravity_ok = gravity_size_real - delta < gravity_size && gravity_size < gravity_size_real + delta;

        DecimalFormat f = new DecimalFormat("#0.00");
        MyLog.v("Acceleration=" + f.format(acceleration_size) +
                ", gravity_size=" + f.format(gravity_size) +
                ", gravity=[" + f.format(gravity[0]) + ", " + f.format(gravity[1]) + ", " + f.format(gravity[2]) + "]");

        // The "real" acceleration must be high enough
        final double acceleration_size_min = 4;
        return gravity_ok && acceleration_size_min < acceleration_size;
    }

    public static double size3(double[] vector) {
        return Math.sqrt(vector[0] * vector[0] +
                vector[1] * vector[1] +
                vector[2] * vector[2]
        );
    }
}