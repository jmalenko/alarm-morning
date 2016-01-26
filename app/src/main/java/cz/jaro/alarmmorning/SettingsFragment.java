package cz.jaro.alarmmorning;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.RingtonePreference;
import android.text.TextUtils;

public class SettingsFragment extends PreferenceFragment {

    /**
     * Value is ringtone URI.
     */
    public static final String PREF_RINGTONE = "pref_ringtone";

    /**
     * Value is 0 .. PREF_VOLUME_MAX. Percents of max volume.
     */
    public static final String PREF_VOLUME = "pref_volume";

    /**
     * Increase volume by 1% every second, until the volume is reached.
     */
    public static final String PREF_VOLUME_INCREASING = "pref_volume_increasing";

    /**
     * Vibrate.
     */
    public static final String PREF_VIBRATE = "pref_vibrate";

    /**
     * Value is in minutes.
     */
    public static final String PREF_SNOOZE_TIME = "pref_snooze_time";

    /**
     * Value is in minutes.
     */
    public static final String PREF_NEAR_FUTURE_TIME = "pref_near_future_time";

    public static final String PREF_RINGTONE_DEFAULT = "";
    public static final int PREF_VOLUME_DEFAULT = 8;
    public static final boolean PREF_VOLUME_INCREASING_DEFAULT = true;
    public static final boolean PREF_VIBRATE_DEFAULT = true;
    public static final int PREF_SNOOZE_TIME_DEFAULT = 10;
    public static final int PREF_NEAR_FUTURE_TIME_DEFAULT = 120;

    public static final int PREF_VOLUME_MAX = 10;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preferences);

        bindPreferenceSummaryToValue(findPreference(PREF_RINGTONE));
        bindPreferenceSummaryToValue(findPreference(PREF_VOLUME));
        bindPreferenceSummaryToValue(findPreference(PREF_SNOOZE_TIME));
        bindPreferenceSummaryToValue(findPreference(PREF_NEAR_FUTURE_TIME));
    }

    /**
     * A preference value change listener that updates the preference's summary to reflect its new value.
     */
    private static Preference.OnPreferenceChangeListener sBindPreferenceSummaryToValueListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object value) {
            String stringValue = value.toString();
            String key = preference.getKey();

            if (preference instanceof RingtonePreference) {
                // For ringtone preferences, look up the correct display value using RingtoneManager.
                if (TextUtils.isEmpty(stringValue)) {
                    // Empty values correspond to 'silent' (no ringtone).
                    preference.setSummary(R.string.pref_ringtone_silent);
                } else {
                    Ringtone ringtone = RingtoneManager.getRingtone(preference.getContext(), Uri.parse(stringValue));

                    if (ringtone == null) {
                        // Clear the summary if there was a lookup error.
                        preference.setSummary(null);
                    } else {
                        // Set the summary to reflect the new ringtone display name.
                        String name = ringtone.getTitle(preference.getContext());
                        preference.setSummary(name);
                    }
                }
            } else if (key.equals(PREF_VOLUME)) {
                int intValue = (int) value;
                int volume = getRealVolume(intValue, 100);

                Context context = preference.getContext();
                Resources res = context.getResources();
                String summaryText = String.format(res.getString(R.string.pref_summary_volume), volume);

                preference.setSummary(summaryText);
            } else if (key.equals(PREF_SNOOZE_TIME)) {
                int intValue = (int) value;

                Context context = preference.getContext();
                Resources res = context.getResources();
                String summaryText = String.format(res.getString(R.string.pref_summary_snooze_time), intValue);

                preference.setSummary(summaryText);
            } else if (key.equals(PREF_NEAR_FUTURE_TIME)) {
                int intValue = (int) value;
                int hours = minutesToHour(intValue);
                int minutes = minutesToMinute(intValue);

                Context context = preference.getContext();
                Resources res = context.getResources();
                String summaryText = String.format(res.getString(R.string.pref_summary_near_future_time), hours, minutes);

                preference.setSummary(summaryText);
            } else {
                // For all other preferences, set the summary to the value's simple string representation.
                preference.setSummary(stringValue);
            }
            return true;
        }
    };

    private static int minutesToHour(int minutes) {
        return minutes / 60;
    }

    private static int minutesToMinute(int minutes) {
        return minutes % 60;
    }

    private static int hourAndMinuteToMinutes(int hour, int minute) {
        return 60 * hour + minute;
    }

    public static int getRealVolume(double volumePreference, int maxVolume) {
        return (int) Math.ceil(((volumePreference / SettingsFragment.PREF_VOLUME_MAX) * maxVolume));
    }

    /**
     * Binds a preference's summary to its value. More specifically, when the preference's value is changed, its summary (line of text below the preference
     * title) is updated to reflect the value. The summary is also immediately updated upon calling this method. The exact display format is dependent on the
     * type of preference.
     *
     * @see #sBindPreferenceSummaryToValueListener
     */
    private static void bindPreferenceSummaryToValue(Preference preference) {
        // Set the listener to watch for value changes.
        preference.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);

        // Trigger the listener immediately with the preference's current value.
        String key = preference.getKey();
        SharedPreferences defaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(preference.getContext());
        Object newValue;
        if (key.equals(PREF_RINGTONE)) {
            newValue = defaultSharedPreferences.getString(preference.getKey(), PREF_RINGTONE_DEFAULT);
        } else if (key.equals(PREF_VOLUME)) {
            newValue = defaultSharedPreferences.getInt(preference.getKey(), PREF_VOLUME_DEFAULT);
        } else if (key.equals(PREF_SNOOZE_TIME)) {
            newValue = defaultSharedPreferences.getInt(preference.getKey(), PREF_SNOOZE_TIME_DEFAULT);
        } else if (key.equals(PREF_NEAR_FUTURE_TIME)) {
            newValue = defaultSharedPreferences.getInt(preference.getKey(), PREF_NEAR_FUTURE_TIME_DEFAULT);
        } else {
            throw new IllegalArgumentException();
        }
        sBindPreferenceSummaryToValueListener.onPreferenceChange(preference, newValue);
    }

}