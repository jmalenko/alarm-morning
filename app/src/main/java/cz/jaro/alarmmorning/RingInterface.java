package cz.jaro.alarmmorning;

import android.content.Context;

public interface RingInterface {

    Context getContextI();

    void actOnEvent(String action);

}
