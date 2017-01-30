package cz.jaro.alarmmorning.shadows;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Intent;

import org.robolectric.Shadows;
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

    private AlarmManager.AlarmClockInfo alarmClockInfo;
    private PendingIntent operation;

    @Implementation
    public void setAlarmClock(AlarmManager.AlarmClockInfo alarmClockInfo, PendingIntent operation) {
        this.alarmClockInfo = alarmClockInfo;
        this.operation = operation;
    }

    @Implementation
    public AlarmManager.AlarmClockInfo getNextAlarmClock() {
        return alarmClockInfo;
    }

    @Implementation
    public void cancel(PendingIntent pendingIntent) {
        if (operation != null) {
            final Intent intentTypeToRemove = Shadows.shadowOf(pendingIntent).getSavedIntent();
            final Intent alarmIntent = Shadows.shadowOf(operation).getSavedIntent();
            if (intentTypeToRemove.filterEquals(alarmIntent)) {
                alarmClockInfo = null;
                operation = null;
                return;
            }
        }

        super.cancel(pendingIntent);
    }

    public PendingIntent getNextAlarmClockOperation() {
        return operation;
    }
}
