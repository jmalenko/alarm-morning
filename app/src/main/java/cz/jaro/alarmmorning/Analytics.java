package cz.jaro.alarmmorning;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.google.android.gms.ads.identifier.AdvertisingIdClient;
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

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
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
    private static final String TAG = GlobalManager.createLogTag(Analytics.class);

    private static final String PREF_USER_ID = "user_id";
    private static final String PREF_USER_ID_UNSET = "";
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
    private Bundle mPayload;

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

    public Context getContext() {
        return mContext;
    }

    public void setContext(Context context) {
        if (mContext != null) throw new IllegalStateException("Context is already set");

        mContext = context;

        String userId = getUserId();
        mPayload.putString(Param.User_ID.name(), userId);

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

    public Analytics setDefaults(Defaults defaults) {
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

    @NonNull
    public JSONObject createConfiguration() {
        JSONObject conf = new JSONObject();

        try {
            // Settings
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(mContext);

            JSONObject confPreferences = new JSONObject();
            String ringtonePreference = preferences.getString(SettingsActivity.PREF_RINGTONE, SettingsActivity.PREF_RINGTONE_DEFAULT);
            confPreferences.put(SettingsActivity.PREF_RINGTONE, ringtonePreference);

            int volumePreference = preferences.getInt(SettingsActivity.PREF_VOLUME, SettingsActivity.PREF_VOLUME_DEFAULT);
            int volume = SettingsActivity.getRealVolume(volumePreference, 100);
            confPreferences.put(SettingsActivity.PREF_VOLUME, volume);

            boolean increasing = preferences.getBoolean(SettingsActivity.PREF_VOLUME_INCREASING, SettingsActivity.PREF_VOLUME_INCREASING_DEFAULT);
            confPreferences.put(SettingsActivity.PREF_VOLUME_INCREASING, increasing);

            boolean vibratePreference = preferences.getBoolean(SettingsActivity.PREF_VIBRATE, SettingsActivity.PREF_VIBRATE_DEFAULT);
            confPreferences.put(SettingsActivity.PREF_VIBRATE, vibratePreference);

            int snoozeTime = preferences.getInt(SettingsActivity.PREF_SNOOZE_TIME, SettingsActivity.PREF_SNOOZE_TIME_DEFAULT);
            confPreferences.put(SettingsActivity.PREF_SNOOZE_TIME, snoozeTime);

            boolean autoSnooze = preferences.getBoolean(SettingsActivity.PREF_AUTO_SNOOZE, SettingsActivity.PREF_AUTO_SNOOZE_DEFAULT);
            confPreferences.put(SettingsActivity.PREF_AUTO_SNOOZE, autoSnooze);

            int autoSnoozeMinutes = preferences.getInt(SettingsActivity.PREF_AUTO_SNOOZE_TIME, SettingsActivity.PREF_AUTO_SNOOZE_TIME_DEFAULT);
            confPreferences.put(SettingsActivity.PREF_AUTO_SNOOZE_TIME, autoSnoozeMinutes);

            boolean autoDismiss = preferences.getBoolean(SettingsActivity.PREF_AUTO_DISMISS, SettingsActivity.PREF_AUTO_DISMISS_DEFAULT);
            confPreferences.put(SettingsActivity.PREF_AUTO_DISMISS, autoDismiss);

            int autoDismissMinutes = preferences.getInt(SettingsActivity.PREF_AUTO_DISMISS_TIME, SettingsActivity.PREF_AUTO_DISMISS_TIME_DEFAULT);
            confPreferences.put(SettingsActivity.PREF_AUTO_DISMISS_TIME, autoDismissMinutes);

            int nearFutureMinutes = preferences.getInt(SettingsActivity.PREF_NEAR_FUTURE_TIME, SettingsActivity.PREF_NEAR_FUTURE_TIME_DEFAULT);
            confPreferences.put(SettingsActivity.PREF_NEAR_FUTURE_TIME, nearFutureMinutes);

            boolean napTimeEnabled = preferences.getBoolean(SettingsActivity.PREF_NAP_ENABLED, SettingsActivity.PREF_NAP_ENABLED_DEFAULT);
            confPreferences.put(SettingsActivity.PREF_NAP_ENABLED, napTimeEnabled);

            int napTime = preferences.getInt(SettingsActivity.PREF_NAP_TIME, SettingsActivity.PREF_NAP_TIME_DEFAULT);
            confPreferences.put(SettingsActivity.PREF_NAP_TIME, napTime);

            String buttonActionPreference = preferences.getString(SettingsActivity.PREF_ACTION_ON_BUTTON, SettingsActivity.PREF_ACTION_DEFAULT);
            confPreferences.put(SettingsActivity.PREF_ACTION_ON_BUTTON, SettingsActivity.actionCodeToString(buttonActionPreference));

            String moveActionPreference = preferences.getString(SettingsActivity.PREF_ACTION_ON_BUTTON, SettingsActivity.PREF_ACTION_DEFAULT);
            confPreferences.put(SettingsActivity.PREF_ACTION_ON_MOVE, SettingsActivity.actionCodeToString(moveActionPreference));

            String flipActionPreference = preferences.getString(SettingsActivity.PREF_ACTION_ON_BUTTON, SettingsActivity.PREF_ACTION_DEFAULT);
            confPreferences.put(SettingsActivity.PREF_ACTION_ON_FLIP, SettingsActivity.actionCodeToString(flipActionPreference));

            String shakeActionPreference = preferences.getString(SettingsActivity.PREF_ACTION_ON_BUTTON, SettingsActivity.PREF_ACTION_DEFAULT);
            confPreferences.put(SettingsActivity.PREF_ACTION_ON_SHAKE, SettingsActivity.actionCodeToString(shakeActionPreference));

            String proximityActionPreference = preferences.getString(SettingsActivity.PREF_ACTION_ON_BUTTON, SettingsActivity.PREF_ACTION_DEFAULT);
            confPreferences.put(SettingsActivity.PREF_ACTION_ON_PROXIMITY, SettingsActivity.actionCodeToString(proximityActionPreference));

            boolean checkAlarmTimePreference = preferences.getBoolean(SettingsActivity.PREF_CHECK_ALARM_TIME, SettingsActivity.PREF_CHECK_ALARM_TIME_DEFAULT);
            confPreferences.put(SettingsActivity.PREF_CHECK_ALARM_TIME, checkAlarmTimePreference);

            String checkAlarmTimeAtPreference = preferences.getString(SettingsActivity.PREF_CHECK_ALARM_TIME_AT, SettingsActivity.PREF_CHECK_ALARM_TIME_AT_DEFAULT);
            confPreferences.put(SettingsActivity.PREF_CHECK_ALARM_TIME_AT, checkAlarmTimeAtPreference);

            int checkAlarmTimeGap = preferences.getInt(SettingsActivity.PREF_CHECK_ALARM_TIME_GAP, SettingsActivity.PREF_CHECK_ALARM_TIME_GAP_DEFAULT);
            confPreferences.put(SettingsActivity.PREF_CHECK_ALARM_TIME_GAP, checkAlarmTimeGap);

            boolean nighttimeBellPreference = preferences.getBoolean(SettingsActivity.PREF_NIGHTTIME_BELL, SettingsActivity.PREF_NIGHTTIME_BELL_DEFAULT);
            confPreferences.put(SettingsActivity.PREF_NIGHTTIME_BELL, nighttimeBellPreference);

            String nighttimeBellAtPreference = preferences.getString(SettingsActivity.PREF_NIGHTTIME_BELL_AT, SettingsActivity.PREF_NIGHTTIME_BELL_AT_DEFAULT);
            confPreferences.put(SettingsActivity.PREF_NIGHTTIME_BELL_AT, nighttimeBellAtPreference);

            String nighttimeBellRingtonePreference = preferences.getString(SettingsActivity.PREF_NIGHTTIME_BELL_RINGTONE, SettingsActivity.PREF_NIGHTTIME_BELL_RINGTONE_DEFAULT);
            confPreferences.put(SettingsActivity.PREF_NIGHTTIME_BELL_RINGTONE, nighttimeBellRingtonePreference);

            conf.put("preferences", confPreferences);

            // System
            JSONObject confSystem = new JSONObject();

            confSystem.put("build_brand", Build.BRAND);
            confSystem.put("build_device", Build.DEVICE);
            confSystem.put("build_display", Build.DISPLAY);
            confSystem.put("build_fingerprint", Build.FINGERPRINT);
            confSystem.put("build_manufacturer", Build.MANUFACTURER);
            confSystem.put("build_model", Build.MODEL);
            confSystem.put("build_product", Build.PRODUCT);
            confSystem.put("build_hardware", Build.HARDWARE);
            confSystem.put("build_host", Build.HOST);
            confSystem.put("build_id", Build.ID);
            confSystem.put("build_user", Build.USER);
            confSystem.put("build_board", Build.BOARD);
            confSystem.put("build_serial", Build.SERIAL);

            confSystem.put("build_version_release", Build.VERSION.RELEASE);
            confSystem.put("build_version_sdk_int", Build.VERSION.SDK_INT);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                confSystem.put("build_version_base_os", Build.VERSION.BASE_OS);
            }

            confSystem.put("buildConfig_application_id", BuildConfig.APPLICATION_ID);
            confSystem.put("buildConfig_build_type", BuildConfig.BUILD_TYPE);
            confSystem.put("buildConfig_debug", BuildConfig.DEBUG);
            confSystem.put("buildConfig_flavor", BuildConfig.FLAVOR);
            confSystem.put("buildConfig_version_code", BuildConfig.VERSION_CODE);
            confSystem.put("buildConfig_version_name", BuildConfig.VERSION_NAME);

            Configuration configuration = mContext.getResources().getConfiguration();
            confSystem.put("configuration_mcc", configuration.mcc);
            confSystem.put("configuration_mnc", configuration.mnc);
            confSystem.put("configuration_uiMode", configuration.uiMode);
            confSystem.put("configuration_locale", configuration.locale);

            confSystem.put("settingsSystem_time_12_24", Settings.System.getString(mContext.getContentResolver(), Settings.System.TIME_12_24));

            try {
                AdvertisingIdClient.Info idInfo = AdvertisingIdClient.getAdvertisingIdInfo(getContext());
                confSystem.put("google_advertising_id", idInfo.getId());
            } catch (Exception e) {
                Log.v(TAG, "Cannot get advertising id", e);
            }

            confSystem.put("settings_secure_android_id", Settings.Secure.getString(getContext().getContentResolver(), Settings.Secure.ANDROID_ID));
            confSystem.put("Settings.Secure.default_input_method", Settings.Secure.getString(getContext().getContentResolver(), Settings.Secure.DEFAULT_INPUT_METHOD));

            try {
                // The commented lines require new permissions or requires a higher API level

                final TelephonyManager tm = (TelephonyManager) getContext().getSystemService(Context.TELEPHONY_SERVICE);
                confSystem.put("telephonyManager_simCountryIso", tm.getSimCountryIso());
                confSystem.put("telephonyManager_simOperator", tm.getSimOperator());
                confSystem.put("telephonyManager_simOperatorName", tm.getSimOperatorName());
//                confSystem.put("telephonyManager_simSerialNumber", tm.getSimSerialNumber());

                confSystem.put("telephonyManager_networkCountryIso", tm.getNetworkCountryIso());
                confSystem.put("telephonyManager_networkOperator", tm.getNetworkOperator());
                confSystem.put("telephonyManager_networkOperatorName", tm.getNetworkOperatorName());
                confSystem.put("telephonyManager_networkType", tm.getNetworkType());

                confSystem.put("telephonyManager_phoneType", tm.getPhoneType());

//                confSystem.put("telephonyManager_voiceMailNumber", tm.getVoiceMailNumber());
//                confSystem.put("telephonyManager_voiceNetworkType", tm.getVoiceNetworkType());
//                confSystem.put("telephonyManager_voiceMailAlphaTag", tm.getVoiceMailAlphaTag());

//                confSystem.put("telephonyManager_dataEnabled", tm.isDataEnabled());
//                confSystem.put("telephonyManager_smsCapable", tm.isSmsCapable());
//                confSystem.put("telephonyManager_voiceCapable", tm.isVoiceCapable());

//                confSystem.put("telephonyManager_deviceId", tm.getDeviceId());
//                confSystem.put("telephonyManager_imei", tm.getImei());
//                confSystem.put("telephonyManager_meid", tm.getMeid());
//                confSystem.put("telephonyManager_deviceSoftwareVersion", tm.getDeviceSoftwareVersion());
//                confSystem.put("telephonyManager_line1Number", tm.getLine1Number());
//                confSystem.put("telephonyManager_nai", tm.getNai());
//                confSystem.put("telephonyManager_subscriberId", tm.getSubscriberId());
            } catch (Exception e) {
                Log.v(TAG, "Cannot get data from telephony manager", e);
            }

            try {
                PackageInfo pInfo = getContext().getPackageManager().getPackageInfo(this.getClass().getPackage().getName(), 0);
                confSystem.put("packageInfo_versionName", pInfo.versionName);
            } catch (PackageManager.NameNotFoundException e) {
                Log.v(TAG, "Cannot get data from package info", e);
            }

            // TODO Other sources of data for analytics
            /*
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

            Locale locale = Locale.getDefault();
            confLocale.put("locale", locale.toString());

            confLocale.put("locale_Country", locale.getCountry());
            confLocale.put("locale_Language", locale.getLanguage());
            confLocale.put("locale_Variant", locale.getVariant());

            confLocale.put("locale_DisplayCountry", locale.getDisplayCountry());
            confLocale.put("locale_DisplayLanguage", locale.getDisplayLanguage());
            confLocale.put("locale_DisplayVariant", locale.getDisplayVariant());

            confLocale.put("locale_DisplayName", locale.getDisplayName());
            confLocale.put("locale_ISO3Country", locale.getISO3Country());
            confLocale.put("locale_ISO3Language", locale.getISO3Language());

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                confLocale.put("locale_Script", locale.getScript());
                confLocale.put("locale_DisplayScript", locale.getDisplayScript());
            }

            TimeZone timeZone = TimeZone.getDefault();
            confLocale.put("timeZone_ID", timeZone.getID());
            confLocale.put("timeZone_DisplayName", timeZone.getDisplayName());
            confLocale.put("timeZone_RawOffset", timeZone.getRawOffset());
            confLocale.put("timeZone_DSTSavings", timeZone.getDSTSavings());

            conf.put("locale", confLocale);

            // Calendar
            JSONObject confCalendar = new JSONObject();

            com.ibm.icu.util.Calendar cal = com.ibm.icu.util.Calendar.getInstance();
            for (int dayOfWeek : AlarmDataSource.allDaysOfWeek) {
                String dayOfWeekText = Localization.dayOfWeekToStringShort(mContext.getResources(), dayOfWeek);
                int dayOfWeekType = cal.getDayOfWeekType(dayOfWeek);
                confCalendar.put(dayOfWeekText, dayOfWeekTypeToString(dayOfWeekType));
            }

            conf.put("dayOfWeekType", confCalendar);

            // Holidays
            JSONObject confHoliday = new JSONObject();

            HolidayHelper holidayHelper = HolidayHelper.getInstance();
            if (holidayHelper.useHoliday()) {
                List<Holiday> holidays = holidayHelper.listHolidays();

                for (Holiday h : holidays) {
                    confHoliday.put(String.valueOf(h.getDate()), h.getDescription());
                }
            }

            conf.put("holiday", confHoliday);

            // Permissions
            JSONObject confPermissions = new JSONObject();

            for (String permission : Wizard.allPermissions) {
                int permissionCheck = ContextCompat.checkSelfPermission(getContext(), permission);
                confPermissions.put(permission, permissionCheckToString(permissionCheck));
            }

            conf.put("permission", confPermissions);
        } catch (JSONException e) {
            Log.w(TAG, "Cannot create configuration record", e);
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
        // Validity checks
        if (mPayload.getInt(Param.Version.name()) == 0) throw new IllegalStateException("Analytics record is not valid: Version is null");
        if (mPayload.getString(Param.User_ID.name()) == null) throw new IllegalStateException("Analytics record is not valid: User_ID is null");
        if (mPayload.getString(Param.Datetime.name()) == null) throw new IllegalStateException("Analytics record is not valid: Datetime is null");
        if (mPayload.getString(Param.Datetime_UTC.name()) == null) throw new IllegalStateException("Analytics record is not valid: Datetime_UTC is null");
        if (mEvent == null) throw new IllegalStateException("Analytics record is not valid: Event is null");
        if (mPayload.getString(Param.Channel.name()) == null) throw new IllegalStateException("Analytics record is not valid: Channel is null");
        if (mPayload.getString(Param.Channel_name.name()) == null) throw new IllegalStateException("Analytics record is not valid: Channel_name is null");

        if (mFirebaseAnalytics == null) throw new IllegalStateException("Analytics is null");

        mFirebaseAnalytics.logEvent(mEvent.name(), mPayload);
        Log.i(TAG, toString());
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
        Calendar now = clock.now();
        return now;
    }

    private String getUserId() {
        String userId;

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(mContext);
        if (preferences.contains(PREF_USER_ID)) {
            userId = preferences.getString(PREF_USER_ID, PREF_USER_ID_UNSET);
        } else {
            char[] CHARSET_AZ_09 = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789".toCharArray();
            userId = randomString(CHARSET_AZ_09, USER_ID_LENGTH);

            Log.d(TAG, "Created user_id=" + userId);

            SharedPreferences.Editor editor = preferences.edit();
            editor.putString(PREF_USER_ID, userId);
            editor.apply();
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
