package cz.jaro.alarmmorning;

import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.os.Handler;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.IOException;


/**
 * Blink the camera flashlight
 */
public class FlashlightBlinker {

    private final Context context;

    private Camera camera;
    private SurfaceHolder surfaceHolder;
    private final Handler handler = new Handler();

    private long[] pattern;
    private int repeat;
    private int index = 0; // position (in the pattern array) to act on next

    public FlashlightBlinker(Context context) {
        this.context = context;
    }

    boolean hasFlashlightBlinker() {
        return context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH);
    }

    /**
     * Blink the flashlight with a given pattern.
     *
     * <p>
     * Pass in an array of ints that are the durations for which to turn on or off
     * the flashlight in milliseconds.  The first value indicates the number of milliseconds
     * to wait before turning the flashlight on.  The next value indicates the number of milliseconds
     * for which to keep the flashlight on before turning it off.  Subsequent values alternate
     * between durations in milliseconds to turn the flashlight off or to turn the vibrator on.
     * </p><p>
     * To cause the pattern to repeat, pass the index into the pattern array at which
     * to start the repeat, or -1 to disable repeating.
     * </p>
     *
     * @param pattern an array of longs of times for which to turn the vibrator on or off.
     * @param repeat  the index into pattern at which to repeat, or -1 if
     *                you don't want to repeat.
     */
    public void blink(long[] pattern, int repeat, SurfaceView preview) {
        this.pattern = pattern;
        this.repeat = repeat;

        // Initialize - on some devices (including my Samsung Galaxy S9) the preview must be shown for flashlight to work.
        // Source: https://stackoverflow.com/a/9379765/5726150

        try {
            surfaceHolder = preview.getHolder();
            surfaceHolder.addCallback(new SurfaceHolder.Callback() {
                @Override
                public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
                    // Nothing
                }

                @Override
                public void surfaceCreated(SurfaceHolder holder) {
                    surfaceHolder = holder;
                    try {
                        camera.setPreviewDisplay(surfaceHolder);
                    } catch (IOException e) {
                        MyLog.e("Cannot set preview display", e);
                    }
                }

                @Override
                public void surfaceDestroyed(SurfaceHolder holder) {
                    surfaceHolder = null;
                }
            });

            camera = Camera.open();
            camera.setPreviewDisplay(surfaceHolder);
            camera.startPreview();
        } catch (IOException e) {
            MyLog.e("Cannot set preview display", e);
            return;
        }

        // Start blinking
        opposite();
    }

    public void cancel() {
        if (isOn())
            off();

        // Note: handler.removeCallbacks(this::opposite); doen't work. Source: https://stackoverflow.com/questions/26546043/android-handler-removecallbacks-not-working
        handler.removeCallbacksAndMessages(null);

        camera.stopPreview();
        camera.release();
    }

    /**
     * <p>Return athe current state of the flashlight. The better interpretation is "return the state that the flashlight should be now (based on the index in the pattern)".</p>
     * <p>Note: This may return a wrong value at the beginning when the flashlight may be really on (because the user turned it on mannually), but this method will return false. After the flashlight is turned on once, the return value corresponds to the reality.</p>
     *
     * @return Whether the flashlight is currently on.
     */
    private boolean isOn() {
        return index % 2 == 0;
    }

    private void opposite() {
        if (index == pattern.length) {
            if (repeat == -1) {
                if (isOn()) {
                    off();
                    return;
                }
            } else {
                index = repeat;
            }
        }
        long delay = pattern[index];

        if (isOn()) {
            off();
        } else {
            on();
        }

        index++;

        handler.postDelayed(this::opposite, delay);
    }

    private void on() {
        Camera.Parameters params = camera.getParameters();
        params.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
        camera.setParameters(params);
    }

    private void off() {
        Camera.Parameters params = camera.getParameters();
        params.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
        camera.setParameters(params);
    }
}