package cz.jaro.alarmmorning.shadows;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.annotation.RealObject;

import cz.jaro.alarmmorning.GlobalManager;
import cz.jaro.alarmmorning.clock.Clock;
import cz.jaro.alarmmorning.clock.SystemClock;

@Implements(GlobalManager.class)
public class ShadowGlobalManager {

    @RealObject
    private GlobalManager globalManager;

    private Clock clock;

    @Implementation
    public Clock clock() {
        return clock != null ? clock : new SystemClock();
    }

    public void setClock(Clock clock) {
        this.clock = clock;
    }
}

