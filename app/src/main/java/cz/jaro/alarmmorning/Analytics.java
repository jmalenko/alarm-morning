package cz.jaro.alarmmorning;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.telephony.TelephonyManager;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.Task;
import com.google.firebase.analytics.FirebaseAnalytics;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.security.SecureRandom;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.TimeZone;
import java.util.concurrent.Callable;

import cz.jaro.alarmmorning.calendar.CalendarUtils;
import cz.jaro.alarmmorning.clock.Clock;
import cz.jaro.alarmmorning.clock.SystemClock;
import cz.jaro.alarmmorning.holiday.HolidayHelper;
import cz.jaro.alarmmorning.model.AlarmDataSource;
import cz.jaro.alarmmorning.model.AppAlarm;
import cz.jaro.alarmmorning.model.Day;
import cz.jaro.alarmmorning.model.Defaults;
import cz.jaro.alarmmorning.model.OneTimeAlarm;
import cz.jaro.alarmmorning.wizard.Wizard;
import de.galgtonold.jollydayandroid.Holiday;

import static cz.jaro.alarmmorning.calendar.CalendarUtils.dayOfWeekToString;

/**
 * This class supports logging for analytics.
 * <p>
 * For efficiency, everything is implemented as static.
 */
public class Analytics {

    private static final String PREF_USER_ID = "user_id";
    private static final int USER_ID_LENGTH = 12;

    private static final String DATETIME_FORMAT_UTC = "yyyy-MM-dd HH:mm:ss.SSS";
    private static final String DATE_FORMAT = "yyyy-MM-dd";
    private static final String TIME_FORMAT = "HH:mm";

    public static final String DISABLED = "Disabled";

    public static final String DISMISS__BEFORE = "Before";
    public static final String DISMISS__AFTER = "After";
    public static final String DISMISS__AUTO = "Auto";

    public static final String DAY_OF_WEEK_TYPE__WEEKDAY = "Weekday";
    public static final String DAY_OF_WEEK_TYPE__WEEKEND = "Weekend";
    public static final String DAY_OF_WEEK_TYPE__WEEKEND_ONSET = "Weekend onset";
    public static final String DAY_OF_WEEK_TYPE__WEEKEND_CEASE = "Weekend cease";

    public static final String ALARM_STATE__SET_TO_SPECIFIC = "Set to specific";
    public static final String ALARM_STATE__SET_TO_DEFAULT = "Set to default";

    public static final String CHECK_ALARM_TIME_ACTION__SHOW_NOTIFICATION = "Show notification";
    public static final String CHECK_ALARM_TIME_ACTION__DON_T_SHOW_NOTIFICATION = "Don't show notification";
    public static final String CHECK_ALARM_TIME_ACTION__NO_APPOINTMENT = "No appointment";

    public static final String CHECK_ALARM_TIME_METHOD__QUICK = "Quick";
    public static final String CHECK_ALARM_TIME_METHOD__DIALOG = "Dialog";

    public static final String TARGET_WIZARD = "Wizard";

    public static final String TARGET_MENU_ACTION_CLOSE = "Menu action: Open";
    public static final String TARGET_MENU_ACTION_OPEN = "Menu action: Close";
    public static final String TARGET_MENU_CALENDAR = "Calendar";
    public static final String TARGET_MENU_DEFAULTS = "Defaults";
    public static final String TARGET_MENU_SETTINGS = "Settings";
    public static final String TARGET_MENU_WEBSITE = "Website";
    public static final String TARGET_MENU_USER_GUIDE = "User guide";
    public static final String TARGET_MENU_CHANGE_LOG = "Change log";
    public static final String TARGET_MENU_REPORT_BUG = "Report bug";
    public static final String TARGET_MENU_TRANSLATE = "Translate";
    public static final String TARGET_MENU_RATE = "Rate";
    public static final String TARGET_MENU_DONATE = "Donate";
    public static final String TARGET_MENU_ADD_ONE_TIME_ALARM = "Add one time alarm";

    private Task<Location> locationTask;
    private boolean shouldSave = false; // Save the event after the asynchronous locationTask task finishes

    public enum Param {
        Version,
        User_ID,
        Datetime_UTC,
        Datetime,
        Channel,
        Channel_name,

        Alarm_date,
        Alarm_time,
        Day_of_week,
        Day_of_week_type,
        Alarm_state,
        Default_alarm_time,
        Alarm_id,
        Snooze_count,

        Alarm_time_old,
        Skipped_alarm_times,
        Dismiss_type,

        Location_time,
        Location_latitude,
        Location_longitude,
        Location_accuracy,
        Location_altitude,
        Location_bearing,
        Location_speed,
        Location_verticalAccuracyMeters,
        Location_bearingAccuracyDegrees,
        Location_speedAccuracyMetersPerSecond,

        Check_alarm_time_action,
        Check_alarm_time_gap,
        Check_alarm_time_method,

        Appointment_begin,
        Appointment_title,
        Appointment_location,

        Preference_key,
        Preference_value,

        Target,

        Configuration
    }

    public enum Event {
        Set_default,

        Set_alarm,
        Ring,
        Snooze,
        Dismiss,

        Skipped_alarm,

        /**
         * When a notification or widget is clicked.
         */
        Click,

        /**
         * Use just before calling {@link NotificationManager#notify(int, Notification)}.
         */
        Show,

        /**
         * Use when a notification is cancelled, eg. in {@link NotificationManager#cancel(int)}.
         */
        Hide,

        /**
         * Use in {@link WidgetProvider#onEnabled(Context)}.
         */
        Add,

        /**
         * Use in {@link WidgetProvider#onDisabled(Context)}.
         */
        Remove,

        Play_nighttime_bell,

        Start,

        Change_setting;

        @Override
        public String toString() {
            return name().replace('_', ' ');
        }
    }

    public enum Channel {
        Activity,
        Notification,
        Time,
        External,
        Widget,
        Test
    }

    public enum ChannelName {
        Calendar,
        Defaults,
        Settings,
        Ring,
        Wizard,

        Alarm,

        Widget_alarm_time,

        Check_alarm_time,
        Nighttime_bell,

        Voice,

        Boot,
        Upgrade,
        TimeZoneChange,
        TimeChange;

        @Override
        public String toString() {
            return name().replace('_', ' ');
        }
    }

    private Context mContext;
    private FirebaseAnalytics mFirebaseAnalytics;
    private Event mEvent;
    private final Bundle mPayload;

    public Analytics() {
        mPayload = new Bundle();

        int versionCode = BuildConfig.VERSION_CODE;
        mPayload.putInt(Param.Version.name(), versionCode);

        Calendar now = now();
        String utcTime = calendarToDatetimeStringUTC(now);
        mPayload.putString(Param.Datetime_UTC.name(), utcTime);

        mPayload.putString(Param.Datetime.name(), now.getTime().toString());
    }

    public Analytics(Channel channel, ChannelName channelName) {
        this(null, channel, channelName);
    }

    public Analytics(Context context, Event event, Channel channel, ChannelName channelName) {
        this(context, event, channel, channelName, null, null);
    }

    public Analytics(Event event, Channel channel, ChannelName channelName) {
        this(null, event, channel, channelName, null, null);
    }

    public Analytics(Context context, Event event, Channel channel, ChannelName channelName, Day day, Defaults defaults) {
        this();

        if (context != null) {
            setContext(context);
        }

        setEvent(event);

        setChannel(channel);

        setChannelName(channelName);

        if (day != null) {
            setDay(day);
        }

        if (day != null || defaults != null) {
            setDefaults(day != null ? day.getDefaults() : defaults);
        }
    }

    private Context getContext() {
        return mContext;
    }

    public void setContext(Context context) {
        if (mContext != null) throw new IllegalStateException("Context is already set");

        mContext = context;

        String userId = getUserId();
        mPayload.putString(Param.User_ID.name(), userId);

        setLocation();

        mFirebaseAnalytics = FirebaseAnalytics.getInstance(context);
    }

    public Event getEvent() {
        return mEvent;
    }

    public void setEvent(Event event) {
        if (mEvent != null) throw new IllegalStateException("Event is already set");

        mEvent = event;
    }

    public Analytics setChannel(Channel channel) {
        if (mPayload.getString(Param.Channel.name()) != null) throw new IllegalStateException("Channel is already set");

        mPayload.putString(Param.Channel.name(), channel.toString());
        return this;
    }

    public Analytics setChannelName(ChannelName channelName) {
        if (mPayload.getString(Param.Channel_name.name()) != null) throw new IllegalStateException("Channel_name is already set");

        mPayload.putString(Param.Channel_name.name(), channelName.toString());
        return this;
    }

    public Analytics setAppAlarm(AppAlarm appAlarm) {
        if (appAlarm instanceof Day) {
            Day day = (Day) appAlarm;
            return setDay(day);
        } else if (appAlarm instanceof OneTimeAlarm) {
            OneTimeAlarm oneTimeAlarm = (OneTimeAlarm) appAlarm;
            return setOneTimeAlarm(oneTimeAlarm);
        } else {
            throw new IllegalArgumentException("Unexpected class " + appAlarm.getClass());
        }
    }

    public Analytics setDay(Day day) {
        Calendar alarmTime = day.getDateTime();

        String alarmDateString = calendarToStringDate(alarmTime);
        mPayload.putString(Param.Alarm_date.name(), alarmDateString);

        String alarmTimeString = calendarToTime(alarmTime);
        mPayload.putString(Param.Alarm_time.name(), alarmTimeString);

        String dayOfWeekString = alarmTime.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.SHORT, Locale.US);
        mPayload.putString(Param.Day_of_week.name(), dayOfWeekString);

        com.ibm.icu.util.Calendar c = com.ibm.icu.util.Calendar.getInstance();
        int dayOfWeek = alarmTime.get(Calendar.DAY_OF_WEEK);
        int dayOfWeekType = c.getDayOfWeekType(dayOfWeek);
        String dayOfWeekTypeString = dayOfWeekType == com.ibm.icu.util.Calendar.WEEKEND ? DAY_OF_WEEK_TYPE__WEEKEND : DAY_OF_WEEK_TYPE__WEEKDAY;
        mPayload.putString(Param.Day_of_week_type.name(), dayOfWeekTypeString);

        String alarmStateString = dayStateToString(day.getState());
        mPayload.putString(Param.Alarm_state.name(), alarmStateString);

        setDefaults(day.getDefaults());

        return this;
    }

    Analytics setDayOld(Day day) {
        // Load currently stored alarm time
        GlobalManager globalManager = GlobalManager.getInstance();
        Day dayOld = globalManager.loadDay(day.getDate());

        if (dayOld.isEnabled()) {
            Calendar alarmTime = dayOld.getDateTime();

            String alarmTimeString = calendarToTime(alarmTime);
            mPayload.putString(Param.Alarm_time_old.name(), alarmTimeString);
        } else {
            mPayload.putString(Param.Alarm_time_old.name(), DISABLED);
        }
        return this;
    }

    private Analytics setDefaults(Defaults defaults) {
        if (defaults != null && defaults.isEnabled()) {
            String defaultAlarmTimeString = getDefaultsAlarmTimeString(defaults);
            mPayload.putString(Param.Default_alarm_time.name(), defaultAlarmTimeString);
        } else {
            mPayload.putString(Param.Default_alarm_time.name(), DISABLED);
        }

        return this;
    }

    public Analytics setDefaultsAll(Defaults defaults) {
        int dayOfWeek = defaults.getDayOfWeek();

        String dayOfWeekString = dayOfWeekToString(defaults.getDayOfWeek());
        mPayload.putString(Param.Day_of_week.name(), dayOfWeekString);

        com.ibm.icu.util.Calendar c = com.ibm.icu.util.Calendar.getInstance();
        int dayOfWeekType = c.getDayOfWeekType(dayOfWeek);
        String dayOfWeekTypeString = dayOfWeekType == com.ibm.icu.util.Calendar.WEEKEND ? DAY_OF_WEEK_TYPE__WEEKEND : DAY_OF_WEEK_TYPE__WEEKDAY;
        mPayload.putString(Param.Day_of_week_type.name(), dayOfWeekTypeString);

        setDefaults(defaults);

        // Load currently stored alarm time
        GlobalManager globalManager = GlobalManager.getInstance();
        Defaults defaultsOld = globalManager.loadDefault(dayOfWeek);

        if (defaultsOld.isEnabled()) {
            String defaultAlarmTimeString = getDefaultsAlarmTimeString(defaultsOld);
            mPayload.putString(Param.Alarm_time_old.name(), defaultAlarmTimeString);
        } else {
            mPayload.putString(Param.Alarm_time_old.name(), DISABLED);
        }

        return this;
    }

    private String getDefaultsAlarmTimeString(Defaults defaults) {
        int hour = defaults.getHour();
        int minute = defaults.getMinute();

        Calendar now = now();
        Calendar defaultAlarmTime = (Calendar) now.clone();
        defaultAlarmTime.set(Calendar.HOUR_OF_DAY, hour);
        defaultAlarmTime.set(Calendar.MINUTE, minute);

        return calendarToTime(defaultAlarmTime);
    }

    public Analytics setOneTimeAlarm(OneTimeAlarm oneTimeAlarm) {
        Calendar alarmTime = oneTimeAlarm.getDateTime();

        String alarmDateString = calendarToStringDate(alarmTime);
        mPayload.putString(Param.Alarm_date.name(), alarmDateString);

        String alarmTimeString = calendarToTime(alarmTime);
        mPayload.putString(Param.Alarm_time.name(), alarmTimeString);

        String dayOfWeekString = alarmTime.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.SHORT, Locale.US);
        mPayload.putString(Param.Day_of_week.name(), dayOfWeekString);

        com.ibm.icu.util.Calendar c = com.ibm.icu.util.Calendar.getInstance();
        int dayOfWeek = alarmTime.get(Calendar.DAY_OF_WEEK);
        int dayOfWeekType = c.getDayOfWeekType(dayOfWeek);
        String dayOfWeekTypeString = dayOfWeekType == com.ibm.icu.util.Calendar.WEEKEND ? DAY_OF_WEEK_TYPE__WEEKEND : DAY_OF_WEEK_TYPE__WEEKDAY;
        mPayload.putString(Param.Day_of_week_type.name(), dayOfWeekTypeString);

        mPayload.putLong(Param.Alarm_id.name(), oneTimeAlarm.getId());

        return this;
    }

    public Analytics setConfigurationInfo() {
        JSONObject conf = createConfiguration();
        mPayload.putSerializable(Param.Configuration.name(), conf.toString());
        return this;
    }

    private void put(JSONObject conf, String key, Callable callable) {
        try {
            Object result = callable.call();
            conf.put(key, result);
        } catch (Exception e) {
            MyLog.v("Cannot get value for " + key, e);
        }
    }

    @NonNull
    JSONObject createConfiguration() {
        JSONObject conf = new JSONObject();

        try {
            // Settings

            JSONObject confPreferences = new JSONObject();

            put(confPreferences, SettingsActivity.PREF_RINGTONE, () -> SharedPreferencesHelper.load(SettingsActivity.PREF_RINGTONE, SettingsActivity.PREF_RINGTONE_DEFAULT));

            put(confPreferences, SettingsActivity.PREF_VOLUME, () -> {
                int volumePreference = (int) SharedPreferencesHelper.load(SettingsActivity.PREF_VOLUME, SettingsActivity.PREF_VOLUME_DEFAULT);
                return SettingsActivity.getRealVolume(volumePreference, 100);
            });

            put(confPreferences, SettingsActivity.PREF_VOLUME_INCREASING, () -> SharedPreferencesHelper.load(SettingsActivity.PREF_VOLUME_INCREASING, SettingsActivity.PREF_VOLUME_INCREASING_DEFAULT));

            put(confPreferences, SettingsActivity.PREF_VIBRATE, () -> SharedPreferencesHelper.load(SettingsActivity.PREF_VIBRATE, SettingsActivity.PREF_VIBRATE_DEFAULT));

            put(confPreferences, SettingsActivity.PREF_SNOOZE_TIME, () -> SharedPreferencesHelper.load(SettingsActivity.PREF_SNOOZE_TIME, SettingsActivity.PREF_SNOOZE_TIME_DEFAULT));

            put(confPreferences, SettingsActivity.PREF_AUTO_SNOOZE, () -> SharedPreferencesHelper.load(SettingsActivity.PREF_AUTO_SNOOZE, SettingsActivity.PREF_AUTO_SNOOZE_DEFAULT));

            put(confPreferences, SettingsActivity.PREF_AUTO_SNOOZE_TIME, () -> SharedPreferencesHelper.load(SettingsActivity.PREF_AUTO_SNOOZE_TIME, SettingsActivity.PREF_AUTO_SNOOZE_TIME_DEFAULT));

            put(confPreferences, SettingsActivity.PREF_AUTO_DISMISS, () -> SharedPreferencesHelper.load(SettingsActivity.PREF_AUTO_DISMISS, SettingsActivity.PREF_AUTO_DISMISS_DEFAULT));

            put(confPreferences, SettingsActivity.PREF_AUTO_DISMISS_TIME, () -> SharedPreferencesHelper.load(SettingsActivity.PREF_AUTO_DISMISS_TIME, SettingsActivity.PREF_AUTO_DISMISS_TIME_DEFAULT));

            put(confPreferences, SettingsActivity.PREF_NEAR_FUTURE_TIME, () -> SharedPreferencesHelper.load(SettingsActivity.PREF_NEAR_FUTURE_TIME, SettingsActivity.PREF_NEAR_FUTURE_TIME_DEFAULT));

            put(confPreferences, SettingsActivity.PREF_NAP_ENABLED, () -> SharedPreferencesHelper.load(SettingsActivity.PREF_NAP_ENABLED, SettingsActivity.PREF_NAP_ENABLED_DEFAULT));

            put(confPreferences, SettingsActivity.PREF_NAP_TIME, () -> SharedPreferencesHelper.load(SettingsActivity.PREF_NAP_TIME, SettingsActivity.PREF_NAP_TIME_DEFAULT));

            put(confPreferences, SettingsActivity.PREF_ACTION_ON_BUTTON, () -> {
                String buttonActionPreference = (String) SharedPreferencesHelper.load(SettingsActivity.PREF_ACTION_ON_BUTTON, SettingsActivity.PREF_ACTION_DEFAULT);
                return SettingsActivity.actionCodeToString(buttonActionPreference);
            });

            put(confPreferences, SettingsActivity.PREF_ACTION_ON_MOVE, () -> {
                String moveActionPreference = (String) SharedPreferencesHelper.load(SettingsActivity.PREF_ACTION_ON_MOVE, SettingsActivity.PREF_ACTION_DEFAULT);
                return SettingsActivity.actionCodeToString(moveActionPreference);
            });

            put(confPreferences, SettingsActivity.PREF_ACTION_ON_FLIP, () -> {
                String flipActionPreference = (String) SharedPreferencesHelper.load(SettingsActivity.PREF_ACTION_ON_FLIP, SettingsActivity.PREF_ACTION_DEFAULT);
                return SettingsActivity.actionCodeToString(flipActionPreference);
            });

            put(confPreferences, SettingsActivity.PREF_ACTION_ON_SHAKE, () -> {
                String shakeActionPreference = (String) SharedPreferencesHelper.load(SettingsActivity.PREF_ACTION_ON_SHAKE, SettingsActivity.PREF_ACTION_DEFAULT);
                return SettingsActivity.actionCodeToString(shakeActionPreference);
            });

            put(confPreferences, SettingsActivity.PREF_ACTION_ON_PROXIMITY, () -> {
                String proximityActionPreference = (String) SharedPreferencesHelper.load(SettingsActivity.PREF_ACTION_ON_PROXIMITY, SettingsActivity.PREF_ACTION_DEFAULT);
                return SettingsActivity.actionCodeToString(proximityActionPreference);
            });

            put(confPreferences, SettingsActivity.PREF_CHECK_ALARM_TIME, () -> SharedPreferencesHelper.load(SettingsActivity.PREF_CHECK_ALARM_TIME, SettingsActivity.PREF_CHECK_ALARM_TIME_DEFAULT));

            put(confPreferences, SettingsActivity.PREF_CHECK_ALARM_TIME_AT, () -> SharedPreferencesHelper.load(SettingsActivity.PREF_CHECK_ALARM_TIME_AT, SettingsActivity.PREF_CHECK_ALARM_TIME_AT_DEFAULT));

            put(confPreferences, SettingsActivity.PREF_CHECK_ALARM_TIME_GAP, () -> SharedPreferencesHelper.load(SettingsActivity.PREF_CHECK_ALARM_TIME_GAP, SettingsActivity.PREF_CHECK_ALARM_TIME_GAP_DEFAULT));

            put(confPreferences, SettingsActivity.PREF_NIGHTTIME_BELL, () -> SharedPreferencesHelper.load(SettingsActivity.PREF_NIGHTTIME_BELL, SettingsActivity.PREF_NIGHTTIME_BELL_DEFAULT));

            put(confPreferences, SettingsActivity.PREF_NIGHTTIME_BELL_AT, () -> SharedPreferencesHelper.load(SettingsActivity.PREF_NIGHTTIME_BELL_AT, SettingsActivity.PREF_NIGHTTIME_BELL_AT_DEFAULT));

            put(confPreferences, SettingsActivity.PREF_NIGHTTIME_BELL_RINGTONE, () -> SharedPreferencesHelper.load(SettingsActivity.PREF_NIGHTTIME_BELL_RINGTONE, SettingsActivity.PREF_NIGHTTIME_BELL_RINGTONE_DEFAULT));

            conf.put("preferences", confPreferences);

            // System

            JSONObject confSystem = new JSONObject();

            put(confSystem, "build_brand", () -> Build.BRAND);
            put(confSystem, "build_device", () -> Build.DEVICE);
            put(confSystem, "build_display", () -> Build.DISPLAY);
            put(confSystem, "build_fingerprint", () -> Build.FINGERPRINT);
            put(confSystem, "build_manufacturer", () -> Build.MANUFACTURER);
            put(confSystem, "build_model", () -> Build.MODEL);
            put(confSystem, "build_product", () -> Build.PRODUCT);
            put(confSystem, "build_hardware", () -> Build.HARDWARE);
            put(confSystem, "build_host", () -> Build.HOST);
            put(confSystem, "build_id", () -> Build.ID);
            put(confSystem, "build_user", () -> Build.USER);
            put(confSystem, "build_board", () -> Build.BOARD);
            put(confSystem, "build_serial", () -> Build.SERIAL);

            put(confSystem, "build_version_release", () -> Build.VERSION.RELEASE);
            put(confSystem, "build_version_sdk_int", () -> Build.VERSION.SDK_INT);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                put(confSystem, "build_version_base_os", () -> Build.VERSION.BASE_OS);
            }

            put(confSystem, "buildConfig_application_id", () -> BuildConfig.APPLICATION_ID);
            put(confSystem, "buildConfig_build_type", () -> BuildConfig.BUILD_TYPE);
            put(confSystem, "buildConfig_debug", () -> BuildConfig.DEBUG);
            put(confSystem, "buildConfig_flavor", () -> BuildConfig.FLAVOR);
            put(confSystem, "buildConfig_version_code", () -> BuildConfig.VERSION_CODE);
            put(confSystem, "buildConfig_version_name", () -> BuildConfig.VERSION_NAME);

            final Configuration configuration = mContext.getResources().getConfiguration();
            put(confSystem, "configuration_mcc", () -> configuration.mcc);
            put(confSystem, "configuration_mnc", () -> configuration.mnc);
            put(confSystem, "configuration_uiMode", () -> configuration.uiMode);
            put(confSystem, "configuration_locale", () -> configuration.locale);

            put(confSystem, "settingsSystem_time_12_24", () -> Settings.System.getString(mContext.getContentResolver(), Settings.System.TIME_12_24));

//            put(confSystem, "google_advertising_id", () -> { // TODO Sometimes the following error is thrown: IllegalStateException: Calling this from your main thread can lead to deadlock
//                AdvertisingIdClient.Info idInfo = AdvertisingIdClient.getAdvertisingIdInfo(getContext());
//                return idInfo.getId();
//            });

            put(confSystem, "settings_secure_android_id", () -> Settings.Secure.getString(getContext().getContentResolver(), Settings.Secure.ANDROID_ID));
            put(confSystem, "settings_secure_default_input_method", () -> Settings.Secure.getString(getContext().getContentResolver(), Settings.Secure.DEFAULT_INPUT_METHOD));

            final TelephonyManager tm = (TelephonyManager) getContext().getSystemService(Context.TELEPHONY_SERVICE);

            put(confSystem, "telephonyManager_simCountryIso", tm::getSimCountryIso);
            put(confSystem, "telephonyManager_simOperator", tm::getSimOperator);
            put(confSystem, "telephonyManager_simOperatorName", tm::getSimOperatorName);
            // The commented lines require new permissions or requires a higher API level
//            put(confSystem, "telephonyManager_simSerialNumber", tm::getSimSerialNumber);

            put(confSystem, "telephonyManager_networkCountryIso", tm::getNetworkCountryIso);
            put(confSystem, "telephonyManager_networkOperator", tm::getNetworkOperator);
            put(confSystem, "telephonyManager_networkOperatorName", tm::getNetworkOperatorName);
            put(confSystem, "telephonyManager_networkType", tm::getNetworkType);

            put(confSystem, "telephonyManager_phoneType", tm::getPhoneType);

//            put(confSystem, "telephonyManager_voiceMailNumber", tm::getVoiceMailNumber);
//            put(confSystem, "telephonyManager_voiceNetworkType", tm::getVoiceNetworkType);
//            put(confSystem, "telephonyManager_voiceMailAlphaTag", tm::getVoiceMailAlphaTag);
//
//            put(confSystem, "telephonyManager_dataEnabled", tm::isDataEnabled);
//            put(confSystem, "telephonyManager_smsCapable", tm::isSmsCapable);
//            put(confSystem, "telephonyManager_voiceCapable", tm::isVoiceCapable);
//
//            put(confSystem, "telephonyManager_deviceId", tm::getDeviceId);
//            put(confSystem, "telephonyManager_imei", tm::getImei);
//            put(confSystem, "telephonyManager_meid", tm::getMeid);
//            put(confSystem, "telephonyManager_deviceSoftwareVersion", tm::getDeviceSoftwareVersion);
//            put(confSystem, "telephonyManager_line1Number", tm::getLine1Number);
//            put(confSystem, "telephonyManager_nai", tm::getNai);
//            put(confSystem, "telephonyManager_subscriberId", tm::getSubscriberId);

            put(confSystem, "packageInfo_versionName", () -> {
                PackageInfo pInfo = getContext().getPackageManager().getPackageInfo(this.getClass().getPackage().getName(), 0);
                return pInfo.versionName;
            });

            /*
            Other sources of data for analytics:

            Google account - https://stackoverflow.com/questions/2245545/accessing-google-account-id-username-via-android
            Primary e-mail address - https://stackoverflow.com/questions/2112965/how-to-get-the-android-devices-primary-e-mail-address
            Wifi MAC address - https://stackoverflow.com/questions/11705906/programmatically-getting-the-mac-of-an-android-device
            Wifi SSID - https://stackoverflow.com/questions/21391395/get-ssid-when-wifi-is-connected
            Bluetooth MAC address - https://stackoverflow.com/questions/41014764/is-it-possible-to-get-bluetooth-mac-address-in-android-jar-library
            Network connection type - https://stackoverflow.com/questions/2802472/detect-network-connection-type-on-android
            Device serial number - https://stackoverflow.com/questions/11029294/android-how-to-programmatically-access-the-device-serial-number-shown-in-the-av
             */

            conf.put("system", confSystem);

            // Locale

            JSONObject confLocale = new JSONObject();

            final Locale locale = Locale.getDefault();
            put(confLocale, "locale", locale::toString);

            put(confLocale, "locale_Country", locale::getCountry);
            put(confLocale, "locale_Language", locale::getLanguage);
            put(confLocale, "locale_Variant", locale::getVariant);

            put(confLocale, "locale_DisplayCountry", locale::getDisplayCountry);
            put(confLocale, "locale_DisplayLanguage", locale::getDisplayLanguage);
            put(confLocale, "locale_DisplayVariant", locale::getDisplayVariant);

            put(confLocale, "locale_DisplayName", locale::getDisplayName);
            put(confLocale, "locale_ISO3Country", locale::getISO3Country);
            put(confLocale, "locale_ISO3Language", locale::getISO3Language);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                put(confLocale, "locale_Script", locale::getScript);
                put(confLocale, "locale_DisplayScript", locale::getDisplayScript);
            }

            final TimeZone timeZone = TimeZone.getDefault();
            put(confLocale, "timeZone_ID", timeZone::getID);
            put(confLocale, "timeZone_DisplayName", timeZone::getDisplayName);
            put(confLocale, "timeZone_RawOffset", timeZone::getRawOffset);
            put(confLocale, "timeZone_DSTSavings", timeZone::getDSTSavings);

            conf.put("locale", confLocale);

            // Calendar

            JSONObject confCalendar = new JSONObject();

            com.ibm.icu.util.Calendar cal = com.ibm.icu.util.Calendar.getInstance();
            for (int dayOfWeek : AlarmDataSource.allDaysOfWeek) {
                String dayOfWeekText = Localization.dayOfWeekToStringShort(mContext.getResources(), dayOfWeek);
                int dayOfWeekType = cal.getDayOfWeekType(dayOfWeek);
                put(confCalendar, dayOfWeekText, () -> dayOfWeekTypeToString(dayOfWeekType));
            }

            conf.put("dayOfWeekType", confCalendar);

            // Holidays

            JSONObject confHoliday = new JSONObject();

            HolidayHelper holidayHelper = HolidayHelper.getInstance();
            if (holidayHelper.useHoliday()) {
                List<Holiday> holidays = holidayHelper.listHolidays();

                for (Holiday h : holidays) {
                    put(confHoliday, String.valueOf(h.getDate()), h::getDescription);
                }
            }

            conf.put("holiday", confHoliday);

            // Permissions

            JSONObject confPermissions = new JSONObject();

            for (String permission : Wizard.allPermissions) {
                int permissionCheck = ContextCompat.checkSelfPermission(getContext(), permission);
                put(confPermissions, permission, () -> permissionCheckToString(permissionCheck));
            }

            conf.put("permission", confPermissions);
        } catch (JSONException e) {
            MyLog.w("Cannot create configuration record", e);
        }
        return conf;
    }

    public static String dayOfWeekTypeToString(int dayOfWeekType) {
        switch (dayOfWeekType) {
            case com.ibm.icu.util.Calendar.WEEKDAY:
                return DAY_OF_WEEK_TYPE__WEEKDAY;
            case com.ibm.icu.util.Calendar.WEEKEND:
                return DAY_OF_WEEK_TYPE__WEEKEND;
            case com.ibm.icu.util.Calendar.WEEKEND_ONSET:
                return DAY_OF_WEEK_TYPE__WEEKEND_ONSET;
            case com.ibm.icu.util.Calendar.WEEKEND_CEASE:
                return DAY_OF_WEEK_TYPE__WEEKEND_CEASE;
            default:
                throw new IllegalStateException("Unsupported day of week type " + dayOfWeekType);
        }
    }

    public Analytics set(Param param, Serializable s) {
        mPayload.putSerializable(param.name(), s);
        return this;
    }

    public void save() {
        MyLog.v("save()");

        // Validity checks
        if (mPayload.getInt(Param.Version.name()) == 0) throw new IllegalStateException("Analytics record is not valid: Version is null");
        if (mPayload.getString(Param.User_ID.name()) == null) throw new IllegalStateException("Analytics record is not valid: User_ID is null");
        if (mPayload.getString(Param.Datetime.name()) == null) throw new IllegalStateException("Analytics record is not valid: Datetime is null");
        if (mPayload.getString(Param.Datetime_UTC.name()) == null) throw new IllegalStateException("Analytics record is not valid: Datetime_UTC is null");
        if (mEvent == null) throw new IllegalStateException("Analytics record is not valid: Event is null");
        if (mPayload.getString(Param.Channel.name()) == null) throw new IllegalStateException("Analytics record is not valid: Channel is null");
        if (mPayload.getString(Param.Channel_name.name()) == null) throw new IllegalStateException("Analytics record is not valid: Channel_name is null");

        if (mFirebaseAnalytics == null) throw new IllegalStateException("Analytics is null");

        if (locationTask != null && !locationTask.isComplete()) {
            MyLog.v("Saving postponed until the location is acquired");
            shouldSave = true;
            return;
        }

        mFirebaseAnalytics.logEvent(mEvent.name(), mPayload);
        MyLog.i(toString());
    }

    @Override
    public String toString() {
        return padLeft(Param.Datetime_UTC, 23) + " | " +
                padRight(mEvent.toString(), 20) + " | " +
                padRight(Param.Channel, 20) + " | " +
                padRight(Param.Channel_name, 20) + " | " +
                padRight(Param.Alarm_date, 10) + " | " +
                padLeft(Param.Alarm_time, 5) + " | " +
                padRight(Param.Day_of_week, 3) + " | " +
                padRight(Param.Day_of_week_type, 7) + " | " +
                padRight(Param.Alarm_state, 15) + " | " +
                padLeft(Param.Default_alarm_time, 8) + " | " +
                padRight(Param.Alarm_id, 8) + " | " +
                padRight(Param.Snooze_count, 2) + " | " +

                padLeft(Param.Alarm_time_old, 8) + " | " +
                padRight(Param.Skipped_alarm_times, 20) + " | " +
                padRight(Param.Dismiss_type, 6) + " | " +

                padLeft(Param.Location_time, 8) + " | " +
                padLeft(Param.Location_latitude, 8) + " | " +
                padLeft(Param.Location_longitude, 8) + " | " +
                padLeft(Param.Location_accuracy, 8) + " | " +
                padLeft(Param.Location_altitude, 8) + " | " +
                padLeft(Param.Location_bearing, 8) + " | " +
                padLeft(Param.Location_speed, 8) + " | " +
                padLeft(Param.Location_verticalAccuracyMeters, 8) + " | " +
                padLeft(Param.Location_bearingAccuracyDegrees, 8) + " | " +
                padLeft(Param.Location_speedAccuracyMetersPerSecond, 8) + " | " +

                padRight(Param.Check_alarm_time_action, 23) + " | " +
                padRight(Param.Check_alarm_time_gap, 3) + " | " +
                padRight(Param.Check_alarm_time_method, 6) + " | " +

                padLeft(Param.Appointment_begin, 5) + " | " +
                padRight(Param.Appointment_title, 20) + " | " +
                padRight(Param.Appointment_location, 20) + " | " +

                padRight(Param.Preference_key, 30) + " | " +
                padRight(Param.Preference_value, 10) + " | " +

                padRight(Param.Target, 20) + " | " +

                padRight(Param.Configuration, 10) + " | " +

                padLeft(Param.Version, 3) + " | " +
                padRight(Param.User_ID, USER_ID_LENGTH) + " | " +
                padRight(Param.Datetime, 1);
    }

    private String padLeft(Param param, int n) {
        Serializable serializable = mPayload.getSerializable(param.name());
        return padLeft(serializable != null ? serializable.toString() : "", n);
    }

    private String padRight(Param param, int n) {
        Serializable serializable = mPayload.getSerializable(param.name());
        return padRight(serializable != null ? serializable.toString() : "", n);
    }

    public static String padRight(String s, int n) {
        return String.format("%1$-" + n + "s", s);
    }

    public static String padLeft(String s, int n) {
        return String.format("%1$" + n + "s", s);
    }

    private Calendar now() {
        Clock clock = new SystemClock();
        return clock.now();
    }

    private String getUserId() {
        String userId;

        if (SharedPreferencesHelper.contains(PREF_USER_ID)) {
            userId = (String) SharedPreferencesHelper.load(PREF_USER_ID);
        } else {
            char[] CHARSET_AZ_09 = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789".toCharArray();
            userId = randomString(CHARSET_AZ_09, USER_ID_LENGTH);

            MyLog.d("Created user_id=" + userId);

            SharedPreferencesHelper.save(PREF_USER_ID, userId);
        }

        return userId;
    }

    private static String randomString(char[] characterSet, int length) {
        Random random = new SecureRandom();
        char[] result = new char[length];
        for (int i = 0; i < result.length; i++) {
            // picks a random index out of character set
            int randomCharIndex = random.nextInt(characterSet.length);
            result[i] = characterSet[randomCharIndex];
        }
        return new String(result);
    }

    public static String calendarToDatetimeStringUTC(Calendar calendar) {
        SimpleDateFormat sdfTime = new SimpleDateFormat(DATETIME_FORMAT_UTC, Locale.US);
        return sdfTime.format(calendar.getTime());
    }

    public static Calendar datetimeUTCStringToCalendar(String dateString) {
        return stringToCalendar(dateString, DATETIME_FORMAT_UTC);
    }

    public static String calendarToStringDate(Calendar calendar) {
        SimpleDateFormat sdfTime = new SimpleDateFormat(DATE_FORMAT, Locale.US);
        return sdfTime.format(calendar.getTime());
    }

    public static Calendar dateStringToCalendar(String dateString) {
        return stringToCalendar(dateString, DATE_FORMAT);
    }

    private static Calendar stringToCalendar(String dateString, String format) {
        SimpleDateFormat sdfTime = new SimpleDateFormat(format, Locale.US);
        try {
            Date date = sdfTime.parse(dateString);

            Calendar cal = Calendar.getInstance();
            cal.setTime(date);

            return cal;
        } catch (ParseException e) {
            throw new Error("Error while parsing date string", e);
        }
    }

    public static String calendarToTime(Calendar calendar) {
        SimpleDateFormat sdfTime = new SimpleDateFormat(TIME_FORMAT, Locale.US);
        return sdfTime.format(calendar.getTime());
    }

    public static String calendarToTime(int hour, int minute) {
        Calendar calendar = CalendarUtils.newGregorianCalendar();
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);

        return calendarToTime(calendar);
    }

    private void setLocation() {
        try {
            GoogleApiAvailability googleApiAvailability = GoogleApiAvailability.getInstance();
            int available = googleApiAvailability.isGooglePlayServicesAvailable(mContext);
            if (available == ConnectionResult.SUCCESS) {
                FusedLocationProviderClient fusedLocationClient = LocationServices.getFusedLocationProviderClient(mContext);

                locationTask = fusedLocationClient.getLastLocation();
                locationTask.addOnCompleteListener(result -> {
                    if (result.isSuccessful()) {
                        Location location = result.getResult();
                        if (location != null) {
                            set(Param.Location_time, location.getTime());
                            set(Param.Location_latitude, location.getLatitude());
                            set(Param.Location_longitude, location.getLongitude());
                            set(Param.Location_accuracy, location.getAccuracy());
                            if (location.hasAltitude())
                                set(Param.Location_altitude, location.getAltitude());
                            if (location.hasBearing())
                                set(Param.Location_bearing, location.getBearing());
                            if (location.hasSpeed())
                                set(Param.Location_speed, location.getSpeed());
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                if (location.hasVerticalAccuracy())
                                    set(Param.Location_verticalAccuracyMeters, location.getVerticalAccuracyMeters());
                                if (location.hasBearingAccuracy())
                                    set(Param.Location_bearingAccuracyDegrees, location.getBearingAccuracyDegrees());
                                if (location.hasSpeedAccuracy())
                                    set(Param.Location_speedAccuracyMetersPerSecond, location.getSpeedAccuracyMetersPerSecond());
                            }
                            MyLog.d("Location acquired: " + location.getLatitude() + ", " + location.getLongitude());
                        } else {
                            MyLog.d("The location is null.");
                        }
                    }
                    if (shouldSave)
                        save();
                });
            } else {
                MyLog.d("Google API not available for location acquisition. Details: " + new ConnectionResult(available));
            }
        } catch (Exception e) {
            MyLog.w("Exception while getting location", e);
        }
    }

    @NonNull
    public static String dayStateToString(int state) {
        String alarmStateString;
        switch (state) {
            case Day.STATE_DISABLED:
                alarmStateString = DISABLED;
                break;
            case Day.STATE_ENABLED:
                alarmStateString = ALARM_STATE__SET_TO_SPECIFIC;
                break;
            case Day.STATE_RULE:
                alarmStateString = ALARM_STATE__SET_TO_DEFAULT;
                break;
            default:
                throw new IllegalArgumentException("Unexpected day state: " + state);
        }
        return alarmStateString;
    }

    @NonNull
    public static String defaultStateToString(int state) {
        String alarmStateString;
        switch (state) {
            case Defaults.STATE_DISABLED:
                alarmStateString = DISABLED;
                break;
            case Defaults.STATE_ENABLED:
                alarmStateString = ALARM_STATE__SET_TO_SPECIFIC;
                break;
            default:
                throw new IllegalArgumentException("Unexpected defaults state: " + state);
        }
        return alarmStateString;
    }

    public String permissionCheckToString(int permissionCheck) {
        switch (permissionCheck) {
            case PackageManager.PERMISSION_GRANTED:
                return "granted";
            case PackageManager.PERMISSION_DENIED:
                return "denied";
            default:
                throw new IllegalStateException("Unsupported permissionCheck " + permissionCheck);
        }
    }
}
