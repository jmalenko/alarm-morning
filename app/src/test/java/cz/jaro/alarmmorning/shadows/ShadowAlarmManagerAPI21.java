package cz.jaro.alarmmorning.shadows;

import android.app.AlarmManager;
import android.app.PendingIntent;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.shadows.ShadowAlarmManager;

// TODO Hotfix - This class exists only because Robolectric supports API level 21 and we are using methods introduced afterwards. Remove this class when Robolectric supports implemented methods.

/**
 * Shadow for {@link android.app.AlarmManager}.
 */
@SuppressWarnings({"UnusedDeclaration"})
@Implements(AlarmManager.class)
public class ShadowAlarmManagerAPI21 extends ShadowAlarmManager {

    private AlarmManager.AlarmClockInfo nextAlarmClock;

    @Implementation
    public void setAlarmClock(AlarmManager.AlarmClockInfo info, PendingIntent operation) {
        this.nextAlarmClock = info;
    }

    @Implementation
    public AlarmManager.AlarmClockInfo getNextAlarmClock() {
        return nextAlarmClock;
    }

}
