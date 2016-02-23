package cz.jaro.alarmmorning.sensor;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorManager;
import android.util.Log;
import android.widget.TextView;

import java.util.Arrays;

import cz.jaro.alarmmorning.R;
import cz.jaro.alarmmorning.RingInterface;
import cz.jaro.alarmmorning.SettingsFragment;

/**
 * Provides detection of device shake.
 */
public class Shake extends SensorEventDetector {

    private static final String TAG = Shake.class.getSimpleName();

    private static final int FORCE_THRESHOLD = 100;
    private static final int TIME_THRESHOLD = 100*1000000; // in nanoseconds
    private static final int SHAKE_TIMEOUT = 500*1000000;
    private static final int SHAKE_DURATION = 1000*1000000;
    private static final int SHAKE_COUNT = 3;

    private float mLastX = -1.0f, mLastY = -1.0f, mLastZ = -1.0f;
    private long mLastTime;
    private int mShakeCount = 0;
    private long mLastShake;
    private long mLastForce;
    private float maxSpeed;

    public Shake(RingInterface ringInterface) {
        super(ringInterface, "shake", Sensor.TYPE_ACCELEROMETER, SettingsFragment.PREF_ACTION_ON_SHAKE);
    }

    protected boolean isFiring(SensorEvent event) {
        Log.v(TAG, "isFiring(values=" + Arrays.toString(event.values) + ")");

        float x = event.values[0];
        float y = event.values[1];
        float z = event.values[2];

        boolean result = false;

        long now = event.timestamp;

        if (now - mLastForce > SHAKE_TIMEOUT) {
            mShakeCount = 0;
//            TextView textView = (TextView) ringInterface.findViewByIdI(R.id.shake);
//            textView.setText("Shake timeout of" + (now - mLastForce) + "\n" + textView.getText());
        }

        if ((now - mLastTime) > TIME_THRESHOLD) {
            long diff = now - mLastTime;
            //        float speed = Math.abs(x + y + z - mLastX - mLastY - mLastZ) / diff * 10000;
            float speed = Math.abs(x * x + y * y + z * z - mLastX * mLastX - mLastY * mLastY - mLastZ * mLastZ) / diff * 10000000;
            maxSpeed = Math.max(maxSpeed, speed);  // TODO remove
            if (speed > FORCE_THRESHOLD) {
                if (++mShakeCount >= SHAKE_COUNT && now - mLastShake > SHAKE_DURATION) {
                    mLastShake = now;
                    mShakeCount = 0;
                    result = true;
                }
                mLastForce = now;
            }

            TextView textView = (TextView) ringInterface.findViewByIdI(R.id.shake);
//            textView.setText("Shake speed=" + String.format("%.2f", speed) + ", count=" + mShakeCount + ", maxSpeed=" + String.format("%.2f", maxSpeed) + "." + (result ? " SHAKEN" : ""));
//            textView.setText("Shake speed=" + String.format("%.2f", speed) + ", count=" + mShakeCount + "." + (result ? " SHAKEN" : ""));

            mLastTime = now;
            mLastX = x;
            mLastY = y;
            mLastZ = z;
        }


        return result;
    }

//    public void onSensorChanged(int sensor, float[] values)
//    {
//        if (sensor != SensorManager.SENSOR_ACCELEROMETER) return;
//        long now = System.currentTimeMillis();
//
//        if ((now - mLastForce) > SHAKE_TIMEOUT) {
//            mShakeCount = 0;
//        }
//
//        if ((now - mLastTime) > TIME_THRESHOLD) {
//            long diff = now - mLastTime;
//            float speed = Math.abs(values[SensorManager.DATA_X] + values[SensorManager.DATA_Y] + values[SensorManager.DATA_Z] - mLastX - mLastY - mLastZ) / diff * 10000;
//            if (speed > FORCE_THRESHOLD) {
//                if ((++mShakeCount >= SHAKE_COUNT) && (now - mLastShake > SHAKE_DURATION)) {
//                    mLastShake = now;
//                    mShakeCount = 0;
//                    if (mShakeListener != null) {
//                        mShakeListener.onShake();
//                    }
//                }
//                mLastForce = now;
//            }
//            mLastTime = now;
//            mLastX = values[SensorManager.DATA_X];
//            mLastY = values[SensorManager.DATA_Y];
//            mLastZ = values[SensorManager.DATA_Z];
//        }
//    }

}
