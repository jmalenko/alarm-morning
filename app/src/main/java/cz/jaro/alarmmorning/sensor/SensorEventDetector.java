package cz.jaro.alarmmorning.sensor;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import java.util.Arrays;

import cz.jaro.alarmmorning.MyLog;
import cz.jaro.alarmmorning.RingInterface;
import cz.jaro.alarmmorning.SettingsActivity;
import cz.jaro.alarmmorning.SharedPreferencesHelper;

/**
 * SensorEventDetector implements sensor handling. Contains common features of all sensors.
 */
public abstract class SensorEventDetector implements SensorEventListener {

    private final RingInterface ringInterface;
    private final String name;
    private final int type;
    private final String preferenceName;

    private SensorManager mSensorManager;
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

    /**
     * @return true if the detector started properly
     */
    public boolean start() {
        MyLog.v("start()");

        isUsed = use();
        if (isUsed) {

            mSensorManager = (SensorManager) ringInterface.getContextI().getSystemService(Context.SENSOR_SERVICE);
            mSensor = mSensorManager.getDefaultSensor(type);

            isAvailable = mSensor != null;
            if (isAvailable) {
                MyLog.i("Sensor " + name + " is used");
                mSensorManager.registerListener(this, mSensor, SensorManager.SENSOR_DELAY_NORMAL);
                isFiringSet = false;
                return true;
            } else {
                MyLog.d("Sensor " + name + " is not available");
            }
        } else {
            MyLog.d("Sensor " + name + " is not used");
        }
        return false;
    }

    public void stop() {
        MyLog.v("stop()");

        if (isUsed && isAvailable) {
            isUsed = false;
            mSensorManager.unregisterListener(this);
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        MyLog.v("onSensorChanged(event.values=" + Arrays.toString(event.values) + ") sensor=" + name);

        if (isAccurate(event.accuracy)) {
            boolean isFiringNew = isFiring(event);

            ringInterface.addSensorRecordToHistory(name, event, isFiring);

            if (isFiringSet) { // We must have two events
                if (!isFiring && isFiringNew) { // The previous event isn't firing and this event is
                    onFire();
                }
            }

            isFiring = isFiringNew;
            isFiringSet = true;
        } else {
            MyLog.d("Ignoring because sensor " + name + " is not accurate");
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
        MyLog.v("getActionFromPreference()");
        return (String) SharedPreferencesHelper.load(preferenceName, SettingsActivity.PREF_ACTION_DEFAULT);
    }

    private boolean use() {
        MyLog.v("use()");
        String action = getActionFromPreference();

        return !action.equals(SettingsActivity.PREF_ACTION_NOTHING);
    }

    private void onFire() {
        MyLog.v("onFire()");

        MyLog.i("Sensor " + name + " fired");
        String action = getActionFromPreference();
        ringInterface.actOnEvent(action, name);
    }

    /**
     * Detects whether an event happened based on the current value of sensor.
     *
     * @param event array of sensor values
     * @return true iff the sensor event is detected
     */
    abstract protected boolean isFiring(SensorEvent event);

}
