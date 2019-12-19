package cz.jaro.alarmmorning;

import android.content.Context;
import android.hardware.SensorEvent;

public interface RingInterface {

    Context getContextI();

    void actOnEvent(String action, String sensorName);

    void addSensorRecordToHistory(String sensorName, SensorEvent event, boolean isFiring);
}
