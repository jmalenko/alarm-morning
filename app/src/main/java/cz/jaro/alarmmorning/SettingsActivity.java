package cz.jaro.alarmmorning;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.RingtonePreference;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.SurfaceView;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ListView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NavUtils;
import androidx.core.content.ContextCompat;

import cz.jaro.alarmmorning.checkalarmtime.CheckAlarmTime;
import cz.jaro.alarmmorning.graphics.AppCompatPreferenceActivity;
import cz.jaro.alarmmorning.graphics.RelativeTimePreference;
import cz.jaro.alarmmorning.graphics.TimePreference;
import cz.jaro.alarmmorning.holiday.HolidayHelper;
import cz.jaro.alarmmorning.nighttimebell.NighttimeBell;
import cz.jaro.alarmmorning.wizard.Wizard;

public class SettingsActivity extends AppCompatPreferenceActivity {

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
     * Flash.
     */
    public static final String PREF_FLASHLIGHT = "pref_flashlight";

    /**
     * Value is in minutes.
     */
    public static final String PREF_SNOOZE_TIME = "pref_snooze_time";

    /**
     * Value is boolean.
     */
    public static final String PREF_AUTO_SNOOZE = "pref_auto_snooze";

    /**
     * Value is in minutes.
     */
    public static final String PREF_AUTO_SNOOZE_TIME = "pref_auto_snooze_time";

    /**
     * Value is boolean.
     */
    public static final String PREF_AUTO_DISMISS = "pref_auto_dismiss";

    /**
     * Value is in minutes.
     */
    public static final String PREF_AUTO_DISMISS_TIME = "pref_auto_dismiss_time";

    /**
     * Value is in minutes.
     */
    public static final String PREF_NEAR_FUTURE_TIME = "pref_near_future_time";

    /**
     * Value is in minutes.
     */
    public static final String PREF_NAP_ENABLED = "pref_nap_enabled";
    public static final String PREF_NAP_TIME = "pref_nap_time";

    public static final String PREF_ACTION_ON_BUTTON = "pref_action_on_button";
    public static final String PREF_ACTION_ON_MOVE = "pref_action_on_move";
    public static final String PREF_ACTION_ON_FLIP = "pref_action_on_flip";
    public static final String PREF_ACTION_ON_SHAKE = "pref_action_on_shake";
    public static final String PREF_ACTION_ON_PROXIMITY = "pref_action_on_proximity";
    public static final String PREF_ACTION_ON_CLAP = "pref_action_on_clap";

    public static final String PREF_ACTION_NOTHING = "0";
    public static final String PREF_ACTION_MUTE = "1";
    public static final String PREF_ACTION_SNOOZE = "2";
    public static final String PREF_ACTION_DISMISS = "3";

    public static final String PREF_CHECK_ALARM_TIME = "pref_check_alarm_time";
    public static final String PREF_CHECK_ALARM_TIME_AT = "pref_check_alarm_time_at";
    public static final String PREF_CHECK_ALARM_TIME_GAP = "pref_check_alarm_time_gap";

    public static final String PREF_NIGHTTIME_BELL = "pref_nighttime_bell";
    public static final String PREF_NIGHTTIME_BELL_AT = "pref_nighttime_bell_at";
    public static final String PREF_NIGHTTIME_BELL_RINGTONE = "pref_nighttime_bell_ringtone";

    public static final String PREF_RELIABILITY_CHECK_ENABLED = "pref_reliability_check_enabled";
    public static final String PREF_TEST_PANIC = "pref_test_panic";

    public static final String PREF_HOLIDAY = "pref_holiday";

    public static final String PREF_START_WIZARD = "pref_start_wizard";

    public static final String PREF_RINGTONE_DEFAULT = "content://settings/system/alarm_alert";
    public static final int PREF_VOLUME_DEFAULT = 8;
    public static final boolean PREF_VOLUME_INCREASING_DEFAULT = true;
    public static final boolean PREF_VIBRATE_DEFAULT = true;
    public static final boolean PREF_FLASHLIGHT_DEFAULT = false;
    public static final int PREF_SNOOZE_TIME_DEFAULT = 10;
    public static final boolean PREF_AUTO_SNOOZE_DEFAULT = true;
    public static final int PREF_AUTO_SNOOZE_TIME_DEFAULT = 5;
    public static final boolean PREF_AUTO_DISMISS_DEFAULT = true;
    public static final int PREF_AUTO_DISMISS_TIME_DEFAULT = 120;
    public static final String PREF_ACTION_DEFAULT = PREF_ACTION_NOTHING;
    public static final int PREF_NEAR_FUTURE_TIME_DEFAULT = 120;
    public static final boolean PREF_NAP_ENABLED_DEFAULT = false;
    public static final int PREF_NAP_TIME_DEFAULT = 30;
    public static final boolean PREF_CHECK_ALARM_TIME_DEFAULT = true;
    public static final String PREF_CHECK_ALARM_TIME_AT_DEFAULT = "22:00";
    public static final int PREF_CHECK_ALARM_TIME_GAP_DEFAULT = 60;
    public static final boolean PREF_NIGHTTIME_BELL_DEFAULT = true;
    public static final String PREF_NIGHTTIME_BELL_AT_DEFAULT = "22:00";
    public static final String PREF_NIGHTTIME_BELL_RINGTONE_DEFAULT = "raw://church_clock_strikes_3";
    public static final boolean PREF_RELIABILITY_CHECK_ENABLED_DEFAULT = true;
    public static final String PREF_HOLIDAY_NONE = HolidayHelper.PATH_TOP;
    public static final String PREF_HOLIDAY_DEFAULT = PREF_HOLIDAY_NONE;

    public static final int PREF_VOLUME_MAX = 10;

    public static final String SETTING_ACTION__NOTHING = "Nothing";
    public static final String SETTING_ACTION__MUTE = "Mute";
    public static final String SETTING_ACTION__SNOOZE = "Snooze";
    public static final String SETTING_ACTION__DISMISS = "Dismiss";

    private static final int REQUEST_CODE_WIZARD = 1;

    private final static int MY_PERMISSIONS_REQUEST_CAMERA = 1;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupActionBar();

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preferences);

        bindPreferenceSummaryToValue(findPreference(PREF_RINGTONE));
        bindPreferenceSummaryToValue(findPreference(PREF_VOLUME));
        bindPreferenceSummaryToValue(findPreference(PREF_VOLUME_INCREASING));
        bindPreferenceSummaryToValue(findPreference(PREF_VIBRATE));
        bindPreferenceSummaryToValue(findPreference(PREF_FLASHLIGHT));
        bindPreferenceSummaryToValue(findPreference(PREF_SNOOZE_TIME));
        bindPreferenceSummaryToValue(findPreference(PREF_AUTO_SNOOZE_TIME));
        bindPreferenceSummaryToValue(findPreference(PREF_AUTO_DISMISS_TIME));
        bindPreferenceSummaryToValue(findPreference(PREF_ACTION_ON_BUTTON));
        bindPreferenceSummaryToValue(findPreference(PREF_ACTION_ON_MOVE));
        bindPreferenceSummaryToValue(findPreference(PREF_ACTION_ON_FLIP));
        bindPreferenceSummaryToValue(findPreference(PREF_ACTION_ON_SHAKE));
        bindPreferenceSummaryToValue(findPreference(PREF_ACTION_ON_PROXIMITY));
        bindPreferenceSummaryToValue(findPreference(PREF_ACTION_ON_CLAP));
        bindPreferenceSummaryToValue(findPreference(PREF_CHECK_ALARM_TIME));
        bindPreferenceSummaryToValue(findPreference(PREF_CHECK_ALARM_TIME_AT));
        bindPreferenceSummaryToValue(findPreference(PREF_CHECK_ALARM_TIME_GAP));
        bindPreferenceSummaryToValue(findPreference(PREF_NIGHTTIME_BELL));
        bindPreferenceSummaryToValue(findPreference(PREF_NIGHTTIME_BELL_AT));
        bindPreferenceSummaryToValue(findPreference(PREF_NIGHTTIME_BELL_RINGTONE));
        bindPreferenceSummaryToValue(findPreference(PREF_NEAR_FUTURE_TIME));
        bindPreferenceSummaryToValue(findPreference(PREF_NAP_ENABLED));
        bindPreferenceSummaryToValue(findPreference(PREF_NAP_TIME));
        bindPreferenceSummaryToValue(findPreference(PREF_RELIABILITY_CHECK_ENABLED));
        bindPreferenceSummaryToValue(findPreference(PREF_HOLIDAY));

        // Start/stop services

        Preference prefCheckAlarmTime = findPreference(PREF_CHECK_ALARM_TIME);
        prefCheckAlarmTime.setOnPreferenceChangeListener((preference, newValue) -> {
            analytics(preference, newValue);

            boolean boolValue = (boolean) newValue;
            CheckAlarmTime checkAlarmTime = CheckAlarmTime.getInstance(this);
            if (boolValue) {
                MyLog.i("Starting CheckAlarmTime");
                checkAlarmTime.register();
            } else {
                MyLog.i("Stopping CheckAlarmTime");
                checkAlarmTime.unregister();
            }
            return true;
        });

        Preference prefNighttimeBell = findPreference(PREF_NIGHTTIME_BELL);
        prefNighttimeBell.setOnPreferenceChangeListener((preference, newValue) -> {
            analytics(preference, newValue);

            boolean boolValue = (boolean) newValue;
            NighttimeBell nighttimeBell = NighttimeBell.getInstance(this);
            if (boolValue) {
                MyLog.i("Starting NighttimeBell");
                nighttimeBell.register();
            } else {
                MyLog.i("Stopping NighttimeBell");
                nighttimeBell.unregister();
            }
            return true;
        });

        // Click handlers

        Preference prefStartWizard = findPreference(PREF_START_WIZARD);
        prefStartWizard.setOnPreferenceClickListener(preference -> {
            Analytics analytics = new Analytics(preference.getContext(), Analytics.Event.Start, Analytics.Channel.Activity, Analytics.ChannelName.Settings);
            analytics.set(Analytics.Param.Target, Analytics.TARGET_WIZARD);
            analytics.save();

            Intent intent = new Intent(this, Wizard.class);
            startActivityForResult(intent, REQUEST_CODE_WIZARD);

            return true;
        });

        Preference prefTestPanic = findPreference(PREF_TEST_PANIC);
        prefTestPanic.setOnPreferenceClickListener(preference -> {
            Analytics analytics = new Analytics(preference.getContext(), Analytics.Event.Start, Analytics.Channel.Activity, Analytics.ChannelName.Settings);
            analytics.set(Analytics.Param.Target, Analytics.TARGET_TEST_PANIC);
            analytics.save();

            // The SurfaceView (that shows the camera) must be visible for the flashlight to work...
            // Moreover, to add it to dialog, it must be (according to my experiments) included in a layout.
            LinearLayout layout = new LinearLayout(this);
            layout.setOrientation(LinearLayout.VERTICAL);
            layout.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));

            SurfaceView surfaceView = new SurfaceView(this);
            layout.addView(surfaceView);

            surfaceView.getLayoutParams().width = 1; // in pixels
            surfaceView.getLayoutParams().height = 1;

            // Start the panic
            RingActivity ringActivity = new RingActivity();
            ringActivity.testSilenceDetectedStart(this, surfaceView);

            // Show dialog with stop button
            AlertDialog alertDialog = new AlertDialog.Builder(this).create();
            alertDialog.setTitle(getString(R.string.test_panic_title));
            alertDialog.setMessage(getString(R.string.test_panic_message));
            alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, getString(R.string.test_panic_button), (dialog, which) -> {
                ringActivity.testSilenceDetectedStop(this);
                dialog.dismiss();
            });
            alertDialog.setView(layout);
            alertDialog.show();

            return true;
        });

        Preference prefFlashlight = findPreference(PREF_FLASHLIGHT);
        prefFlashlight.setOnPreferenceClickListener(preference -> {
            int permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA);
            if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, MY_PERMISSIONS_REQUEST_CAMERA);
            }
            return true;
        });

        // Change handlers are handled in sBindPreferenceSummaryToValueListener.action(...)
    }

    /**
     * Set up the {@link android.app.ActionBar}, if the API is available.
     */
    private void setupActionBar() {
        Toolbar toolbar;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            ViewGroup root = (ViewGroup) findViewById(android.R.id.list).getParent().getParent().getParent();
            toolbar = (Toolbar) LayoutInflater.from(this).inflate(R.layout.toolbar, root, false);
            root.addView(toolbar, 0);
        } else {
            ViewGroup root = findViewById(android.R.id.content);
            ListView content = (ListView) root.getChildAt(0);
            root.removeAllViews();
            toolbar = (Toolbar) LayoutInflater.from(this).inflate(R.layout.toolbar, root, false);
            int height;
            TypedValue tv = new TypedValue();
            if (getTheme().resolveAttribute(R.attr.actionBarSize, tv, true)) {
                height = TypedValue.complexToDimensionPixelSize(tv.data, getResources().getDisplayMetrics());
            } else {
                height = toolbar.getHeight();
            }
            content.setPadding(0, height, 0, 0);
            root.addView(content);
            root.addView(toolbar);
        }
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            if (!super.onMenuItemSelected(featureId, item)) {
                NavUtils.navigateUpFromSameTask(this);
            }
            return true;
        }
        return super.onMenuItemSelected(featureId, item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_WIZARD) {
            if (resultCode == RESULT_OK) {
                finish();
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    /**
     * A preference value change listener that updates the preference's summary to reflect its new value.
     */
    private static final OnPreferenceChangeListenerWithAnalytics sBindPreferenceSummaryToValueListener = new OnPreferenceChangeListenerWithAnalytics() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object value) {
            analytics(preference, value);
            action(preference, value);
            updateSummary(preference, value);
            return true;
        }

        void action(Preference preference, Object value) {
            String stringValue = value.toString();
            String key = preference.getKey();

            if (key.equals(PREF_HOLIDAY)) {
                GlobalManager globalManager = GlobalManager.getInstance();
                globalManager.saveHoliday(stringValue);
            }
        }

        public boolean updateSummary(Preference preference, Object value) {
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
                    } else if (stringValue.equals(PREF_NIGHTTIME_BELL_RINGTONE_DEFAULT)) {
                        // Use string from resources for a raw ringtone
                        String name = preference.getContext().getResources().getString(R.string.alarmtone_title_church_bell);
                        preference.setSummary(name);
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
                String summaryText = res.getString(R.string.pref_summary_volume, volume);

                preference.setSummary(summaryText);
            } else if (preference instanceof RelativeTimePreference || key.equals(PREF_SNOOZE_TIME)) {
                int intValue = (int) value;
                int hours = RelativeTimePreference.valueToHour(intValue);
                int minutes = RelativeTimePreference.valueToMinute(intValue);

                Context context = preference.getContext();
                Resources res = context.getResources();
                String summaryText = hours == 0 ?
                        res.getQuantityString(R.plurals.time_minute, minutes, minutes) :
                        res.getString(R.string.time_hour_min, res.getQuantityString(R.plurals.time_hour, hours, hours), res.getQuantityString(R.plurals.time_minute, minutes, minutes));

                preference.setSummary(summaryText);
            } else if (preference instanceof TimePreference) {
                int hours = TimePreference.getHour(stringValue);
                int minutes = TimePreference.getMinute(stringValue);

                Context context = preference.getContext();
                Resources res = context.getResources();
                String timeText = Localization.timeToString(hours, minutes, context);
                String summaryText = res.getString(R.string.pref_summary_time_preference, timeText);

                preference.setSummary(summaryText);
            } else if (key.equals(PREF_ACTION_ON_BUTTON) || key.equals(PREF_ACTION_ON_MOVE) || key.equals(PREF_ACTION_ON_FLIP) || key.equals(PREF_ACTION_ON_SHAKE) || key.equals(PREF_ACTION_ON_PROXIMITY) || key.equals(PREF_ACTION_ON_CLAP)) {
                int intValue = Integer.valueOf(stringValue);

                Context context = preference.getContext();
                Resources res = context.getResources();
                CharSequence summaryText = res.getTextArray(R.array.actionArray)[intValue];

                preference.setSummary(summaryText);
            } else if (key.equals(PREF_HOLIDAY)) {
                String summaryText = HolidayHelper.getInstance().preferenceToDisplayName(stringValue);

                preference.setSummary(summaryText);
            } else {
                // For all other preferences, set the summary to the value's simple string representation.
                preference.setSummary(stringValue);
            }

            return true;
        }
    };

    private static void analytics(Preference preference, Object value) {
        String stringValue = value.toString();
        String key = preference.getKey();

        Analytics analytics = new Analytics(preference.getContext(), Analytics.Event.Change_setting, Analytics.Channel.Activity, Analytics.ChannelName.Settings);
        analytics.set(Analytics.Param.Preference_key, key);
        analytics.set(Analytics.Param.Preference_value, stringValue);
        analytics.save();
    }

    public static int getRealVolume(double volumePreference, int maxVolume) {
        return (int) Math.ceil(((volumePreference / SettingsActivity.PREF_VOLUME_MAX) * maxVolume));
    }

    public static String actionCodeToString(String action) {
        switch (action) {
            case PREF_ACTION_NOTHING:
                return SETTING_ACTION__NOTHING;
            case PREF_ACTION_MUTE:
                return SETTING_ACTION__MUTE;
            case PREF_ACTION_SNOOZE:
                return SETTING_ACTION__SNOOZE;
            case PREF_ACTION_DISMISS:
                return SETTING_ACTION__DISMISS;
            default:
                throw new IllegalArgumentException("Unexpected argument " + action);
        }
    }

//    @Override
//    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
//        switch (requestCode) {
//            case MY_PERMISSIONS_REQUEST_CAMERA: {
//                // If request is cancelled, the result arrays are empty.
//                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//                    // Permission was granted
//                } else {
//                    // Permission denied
//
//                    // The "revert the setting if the users doesn't immediately grant permission" is a nice feature.
//                    // However, it's not common in Android and we are checking for the permission on calendar start anyway.
//                    // Therefore it's disabled.
//
//                    // Revert the value to false
//                    ((CheckBoxPreference) findPreference(PREF_FLASHLIGHT)).setChecked(false);
//                }
//                return;
//            }
//        }
//    }

    /**
     * Binds a preference's summary to its value. More specifically, when the preference's value is changed, its summary (line of text below the preference
     * title) is updated to reflect the value. The summary is also immediately updated upon calling this method. The exact display format is dependent on the
     * type of preference.
     *
     * @see #sBindPreferenceSummaryToValueListener
     */
    private static void bindPreferenceSummaryToValue(Preference preference) {
        // Set the listener to watch for value changes.
        bindPreferenceChangeListener(preference);

        // Trigger the listener immediately with the preference's current value.
        String key = preference.getKey();
        Object newValue;
        switch (key) {
            case PREF_RINGTONE:
                newValue = SharedPreferencesHelper.load(preference.getKey(), PREF_RINGTONE_DEFAULT);
                break;
            case PREF_VOLUME:
                newValue = SharedPreferencesHelper.load(preference.getKey(), PREF_VOLUME_DEFAULT);
                break;
            case PREF_VOLUME_INCREASING:
                newValue = SharedPreferencesHelper.load(preference.getKey(), PREF_VOLUME_INCREASING_DEFAULT);
                break;
            case PREF_VIBRATE:
                newValue = SharedPreferencesHelper.load(preference.getKey(), PREF_VIBRATE_DEFAULT);
                break;
            case PREF_FLASHLIGHT:
                newValue = SharedPreferencesHelper.load(preference.getKey(), PREF_FLASHLIGHT_DEFAULT);
                break;
            case PREF_SNOOZE_TIME:
                newValue = SharedPreferencesHelper.load(preference.getKey(), PREF_SNOOZE_TIME_DEFAULT);
                break;
            case PREF_AUTO_SNOOZE_TIME:
                newValue = SharedPreferencesHelper.load(preference.getKey(), PREF_AUTO_SNOOZE_TIME_DEFAULT);
                break;
            case PREF_AUTO_DISMISS_TIME:
                newValue = SharedPreferencesHelper.load(preference.getKey(), PREF_AUTO_DISMISS_TIME_DEFAULT);
                break;
            case PREF_ACTION_ON_BUTTON:
            case PREF_ACTION_ON_MOVE:
            case PREF_ACTION_ON_FLIP:
            case PREF_ACTION_ON_SHAKE:
            case PREF_ACTION_ON_PROXIMITY:
            case PREF_ACTION_ON_CLAP:
                newValue = SharedPreferencesHelper.load(preference.getKey(), PREF_ACTION_DEFAULT);
                break;
            case PREF_CHECK_ALARM_TIME:
                newValue = SharedPreferencesHelper.load(preference.getKey(), PREF_CHECK_ALARM_TIME_DEFAULT);
                break;
            case PREF_CHECK_ALARM_TIME_AT:
                newValue = SharedPreferencesHelper.load(preference.getKey(), PREF_CHECK_ALARM_TIME_AT_DEFAULT);
                break;
            case PREF_CHECK_ALARM_TIME_GAP:
                newValue = SharedPreferencesHelper.load(preference.getKey(), PREF_CHECK_ALARM_TIME_GAP_DEFAULT);
                break;
            case PREF_NIGHTTIME_BELL:
                newValue = SharedPreferencesHelper.load(preference.getKey(), PREF_NIGHTTIME_BELL_DEFAULT);
                break;
            case PREF_NIGHTTIME_BELL_AT:
                newValue = SharedPreferencesHelper.load(preference.getKey(), PREF_NIGHTTIME_BELL_AT_DEFAULT);
                break;
            case PREF_NIGHTTIME_BELL_RINGTONE:
                newValue = SharedPreferencesHelper.load(preference.getKey(), PREF_NIGHTTIME_BELL_RINGTONE_DEFAULT);
                break;
            case PREF_NEAR_FUTURE_TIME:
                newValue = SharedPreferencesHelper.load(preference.getKey(), PREF_NEAR_FUTURE_TIME_DEFAULT);
                break;
            case PREF_NAP_ENABLED:
                newValue = SharedPreferencesHelper.load(preference.getKey(), PREF_NAP_ENABLED_DEFAULT);
                break;
            case PREF_NAP_TIME:
                newValue = SharedPreferencesHelper.load(preference.getKey(), PREF_NAP_TIME_DEFAULT);
                break;
            case PREF_RELIABILITY_CHECK_ENABLED:
                newValue = SharedPreferencesHelper.load(preference.getKey(), PREF_RELIABILITY_CHECK_ENABLED_DEFAULT);
                break;
            case PREF_HOLIDAY:
                newValue = SharedPreferencesHelper.load(preference.getKey(), PREF_HOLIDAY_DEFAULT);
                break;
            default:
                throw new IllegalArgumentException("Unexpected argument " + key);
        }
        sBindPreferenceSummaryToValueListener.updateSummary(preference, newValue);
    }

    private static void bindPreferenceChangeListener(Preference preference) {
        preference.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);
    }
}

interface OnPreferenceChangeListenerWithAnalytics extends Preference.OnPreferenceChangeListener {

    boolean updateSummary(Preference preference, Object value);
}