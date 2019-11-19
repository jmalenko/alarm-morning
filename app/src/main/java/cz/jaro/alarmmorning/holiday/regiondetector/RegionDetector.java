package cz.jaro.alarmmorning.holiday.regiondetector;

import android.content.Context;

/**
 * Detects the device location.
 * <p>
 * Currently used to detect only the country.
 */
public abstract class RegionDetector {

    private Context context;
    private OnRegionChangeListener onRegionChangeListener;

    /**
     * Constructor that takes the Context.
     *
     * @param context Context.
     */
    RegionDetector(Context context) {
        this.context = context;
    }

    /**
     * Get the Context.
     *
     * @return Context
     */
    public Context getContext() {
        return context;
    }

    /**
     * Set the Context.
     *
     * @param context Context
     */
    public void setContext(Context context) {
        this.context = context;
    }

    /**
     * Detect the device region.
     */
    public abstract void detect();

    /**
     * Call this method after the region is detected.
     *
     * @param countryCode  County code
     * @param regionObject Region
     * @return true to keep detecting the region changes
     */
    protected boolean callChangeListener(String countryCode, Object regionObject) {
        return onRegionChangeListener == null || onRegionChangeListener.onRegionChange(this, countryCode, regionObject);
    }

    /**
     * Sets the callback to be invoked when the region is changed by the user.
     *
     * @param onRegionChangeListener The callback to be invoked.
     */
    public void setOnRegionChangeListener(OnRegionChangeListener onRegionChangeListener) {
        this.onRegionChangeListener = onRegionChangeListener;
    }

    /**
     * Interface definition for a callback to be invoked when the region has been changed.
     */
    public interface OnRegionChangeListener {
        /**
         * Called when the region has been changed.
         *
         * @param regionDetector Region detector
         * @param countryCode    Country code
         * @param region         Region
         * @return true to keep detecting the region changes
         */
        boolean onRegionChange(RegionDetector regionDetector, String countryCode, Object region);
    }

}
