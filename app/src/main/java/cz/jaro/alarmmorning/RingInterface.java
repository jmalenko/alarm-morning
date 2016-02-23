package cz.jaro.alarmmorning;

import android.content.Context;
import android.view.View;

public interface RingInterface {

    Context getContextI();

    View findViewByIdI(int id);

    void actOnEvent(String action);

}
