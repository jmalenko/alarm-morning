package cz.jaro.alarmmorning.shadows;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

import java.util.GregorianCalendar;

import cz.jaro.alarmmorning.GlobalManager;
import cz.jaro.alarmmorning.clock.Clock;
import cz.jaro.alarmmorning.clock.FixedClock;
import cz.jaro.alarmmorning.model.DayTest;

import static cz.jaro.alarmmorning.model.DayTest.DAY;
import static cz.jaro.alarmmorning.model.DayTest.MONTH;
import static cz.jaro.alarmmorning.model.DayTest.YEAR;

@Implements(GlobalManager.class)
public class ShadowGlobalManager {

//    @RealObject
//    private GlobalManager globalManager;

    // TODO Hotfix Robolectric - call setClock() of this shadow from each test. I haven't found a way to get this shadow from a test.

//    private Clock clock;

    @Implementation
    public Clock clock() {
        FixedClock clock = new FixedClock(new GregorianCalendar(YEAR, MONTH, DAY, DayTest.HOUR, DayTest.MINUTE));
        return clock;
//        return clock != null ? clock : new SystemClock();
    }

//    public void setClock(Clock clock) {
//        this.clock = clock;
//    }
}

