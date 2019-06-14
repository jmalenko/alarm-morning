package cz.jaro.alarmmorning;

import android.content.Context;
import android.os.PowerManager;

/**
 * The WakeLocker prevents the device from sleeping.
 * <p/>
 * First, call {@link #acquire(Context)} to prevent the device from sleeping. Second, perform the action. Third, call {@link #release()} to resume the
 * standard behavior.
 */
public abstract class WakeLocker {
    private static final String APP_TAG = "alarm-morning:mywakelogtag";

    private static PowerManager.WakeLock wakeLock;

    /**
     * Prevent the device from sleeping.
     *
     * @param context context
     */
    public static void acquire(Context context) {
        if (wakeLock != null) wakeLock.release();

        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        wakeLock = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK |
                PowerManager.ACQUIRE_CAUSES_WAKEUP |
                PowerManager.ON_AFTER_RELEASE, APP_TAG);
        wakeLock.acquire();
    }

    /**
     * Resume resume the standard behavior regarding sleeping.
     */
    public static void release() {
        if (wakeLock != null) wakeLock.release();
        wakeLock = null;
    }
}
