package cz.jaro.alarmmorning.sensor;

import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.preference.PreferenceManager;
import android.util.Log;

import java.util.Arrays;

import cz.jaro.alarmmorning.RingInterface;
import cz.jaro.alarmmorning.SettingsFragment;

/**
 * SensorEventDetector implements sensor handling. Contains common features of all sensors.
 */
public abstract class SensorEventDetector implements SensorEventListener {

    private static final String TAG = SensorEventDetector.class.getSimpleName();

    protected final RingInterface ringInterface;
    private final String name;
    private final int type;
    private final String preferenceName;

    SensorManager mSensorManager;
    Sensor mSensor;

    private boolean isUsed;
    private boolean isAvailable;
    private boolean isFiringSet;
    private boolean isFiring;

    public SensorEventDetector(RingInterface ringInterface, String name, int type, String preferenceName) {
        this.ringInterface = ringInterface;
        this.name = name;
        this.type = type;
        this.preferenceName = preferenceName;
    }

    public boolean start() {
        Log.v(TAG, "start()");

        isUsed = use();
        if (isUsed) {

            mSensorManager = (SensorManager) ringInterface.getContextI().getSystemService(Context.SENSOR_SERVICE);
            mSensor = mSensorManager.getDefaultSensor(type);

            isAvailable = mSensor != null;
            if (isAvailable) {
                Log.i(TAG, "Sensor " + name + " is used");
                mSensorManager.registerListener(this, mSensor, SensorManager.SENSOR_DELAY_NORMAL);
                isFiringSet = false;
                return true;
            } else {
                Log.d(TAG, "Sensor " + name + " is not available");
            }
        } else {
            Log.d(TAG, "Sensor " + name + " is not used");
        }
        return false;
    }

    public void stop() {
        Log.v(TAG, "stop()");

        if (isUsed && isAvailable) {
            isUsed = false;
            mSensorManager.unregisterListener(this);
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        Log.v(TAG, "onSensorChanged(event.values=" + Arrays.toString(event.values) + ") sensor=" + name);

        if (isAccurate(event.accuracy)) {
            boolean isFiringNew = isFiring(event);

            if (isFiringSet) {
                if (!isFiring && isFiringNew) {
                    onFire();
                }
            }

            isFiring = isFiringNew;
            isFiringSet = true;
        } else {
            Log.d(TAG, "Ignoring because sensor " + name + " is not accurate");
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // nothing
    }

    private boolean isAccurate(int accuracy) {
        return accuracy == SensorManager.SENSOR_STATUS_ACCURACY_LOW || accuracy == SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM || accuracy == SensorManager.SENSOR_STATUS_ACCURACY_HIGH;
    }

    private String getActionFromPreference() {
        Log.v(TAG, "getActionFromPreference()");
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(ringInterface.getContextI());
        return preferences.getString(preferenceName, SettingsFragment.PREF_ACTION_DEFAULT);
    }

    protected boolean use() {
        Log.v(TAG, "use()");
        String action = getActionFromPreference();

        return !action.equals(SettingsFragment.PREF_ACTION_NOTHING);
    }

    void onFire() {
        Log.v(TAG, "onFire()");

        String action = getActionFromPreference();

//        ringInterface.actOnEvent(action); // FIXME: 19.2.2016
        ringInterface.actOnEvent(name);
    }

    /**
     * Detects whether an event happened based on the current value of sensor.
     *
     * @param event array of sensor values
     * @return true iff the sensor event is detected
     */
    abstract protected boolean isFiring(SensorEvent event);

}
