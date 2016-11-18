package cz.jaro.alarmmorning;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.util.Log;

import com.google.firebase.analytics.FirebaseAnalytics;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.Random;
import java.util.TimeZone;

import cz.jaro.alarmmorning.clock.Clock;
import cz.jaro.alarmmorning.clock.SystemClock;
import cz.jaro.alarmmorning.model.AlarmDataSource;
import cz.jaro.alarmmorning.model.Day;
import cz.jaro.alarmmorning.model.Defaults;

/**
 * This class supports logging for analytics.
 * <p>
 * For efficiency, everything is implemented as static.
 */
public class Analytics {
    private static final String TAG = Analytics.class.getSimpleName();

    private static final String PREF_USER_ID = "user_id";
    private static final String PREF_USER_ID_UNSET = "";
    private static final int USER_ID_LENGTH = 12;

    private static final String DATE_FORMAT_UTC = "yyyy-MM-dd HH:mm:ss.SSS";
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

    public static final String TARGET__WIZARD = "Wizard";

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

        Configuration;
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
        Widget
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
        String utcTime = calendarToUTC(now);
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

    public Analytics setDay(Day day) {
        Calendar alarmTime = day.getDateTime();

        String alarmDateString = calendarToDate(alarmTime);
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

        String alarmStateString;
        switch (day.getState()) {
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
                throw new IllegalArgumentException("Unexpected day state: " + day.getState());
        }
        mPayload.putString(Param.Alarm_state.name(), alarmStateString);

        setDefaults(day.getDefaults());

        return this;
    }

    public Analytics setDayOld(Day day) {
        // Load currently stored alarm time
        AlarmDataSource dataSource = new AlarmDataSource(mContext);
        dataSource.open();

        Day dayOld = dataSource.loadDayDeep(day.getDate());

        dataSource.close();

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
        if (defaults.isEnabled()) {
            String defaultAlarmTimeString = getDefaultsAlarmTimeString(defaults);
            mPayload.putString(Param.Default_alarm_time.name(), defaultAlarmTimeString);
        } else {
            mPayload.putString(Param.Default_alarm_time.name(), DISABLED);
        }

        return this;
    }

    public Analytics setDefaultsAll(Defaults defaults) {
        int dayOfWeek = defaults.getDayOfWeek();

        GregorianCalendar calendar = new GregorianCalendar(1, 2, 2016); // February 2016 starts with Monday
        calendar.add(Calendar.DATE, dayOfWeek + 5);
        String dayOfWeekString = calendar.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.SHORT, Locale.US);
        mPayload.putString(Param.Day_of_week.name(), dayOfWeekString);

        com.ibm.icu.util.Calendar c = com.ibm.icu.util.Calendar.getInstance();
        int dayOfWeekType = c.getDayOfWeekType(dayOfWeek);
        String dayOfWeekTypeString = dayOfWeekType == com.ibm.icu.util.Calendar.WEEKEND ? DAY_OF_WEEK_TYPE__WEEKEND : DAY_OF_WEEK_TYPE__WEEKDAY;
        mPayload.putString(Param.Day_of_week_type.name(), dayOfWeekTypeString);

        setDefaults(defaults);

        // Load currently stored alarm time
        AlarmDataSource dataSource = new AlarmDataSource(mContext);
        dataSource.open();

        Defaults defaultsOld = dataSource.loadDefault(dayOfWeek);

        dataSource.close();

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

    public Analytics setConfigurationInfo() {
        JSONObject conf = new JSONObject();

        try {
            // Settings
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(mContext);

            String ringtonePreference = preferences.getString(SettingsActivity.PREF_RINGTONE, SettingsActivity.PREF_RINGTONE_DEFAULT);
            conf.put(SettingsActivity.PREF_RINGTONE, ringtonePreference);

            int volumePreference = preferences.getInt(SettingsActivity.PREF_VOLUME, SettingsActivity.PREF_VOLUME_DEFAULT);
            int volume = SettingsActivity.getRealVolume(volumePreference, 100);
            conf.put(SettingsActivity.PREF_VOLUME, volume);

            boolean increasing = preferences.getBoolean(SettingsActivity.PREF_VOLUME_INCREASING, SettingsActivity.PREF_VOLUME_INCREASING_DEFAULT);
            conf.put(SettingsActivity.PREF_VOLUME_INCREASING, increasing);

            boolean vibratePreference = preferences.getBoolean(SettingsActivity.PREF_VIBRATE, SettingsActivity.PREF_VIBRATE_DEFAULT);
            conf.put(SettingsActivity.PREF_VIBRATE, vibratePreference);

            int snoozeTime = preferences.getInt(SettingsActivity.PREF_SNOOZE_TIME, SettingsActivity.PREF_SNOOZE_TIME_DEFAULT);
            conf.put(SettingsActivity.PREF_SNOOZE_TIME, snoozeTime);

            int nearFutureMinutes = preferences.getInt(SettingsActivity.PREF_NEAR_FUTURE_TIME, SettingsActivity.PREF_NEAR_FUTURE_TIME_DEFAULT);
            conf.put(SettingsActivity.PREF_NEAR_FUTURE_TIME, nearFutureMinutes);

            int napTime = preferences.getInt(SettingsActivity.PREF_NAP_TIME, SettingsActivity.PREF_NAP_TIME_DEFAULT);
            conf.put(SettingsActivity.PREF_NAP_TIME, napTime);

            String buttonActionPreference = preferences.getString(SettingsActivity.PREF_ACTION_ON_BUTTON, SettingsActivity.PREF_ACTION_DEFAULT);
            conf.put(SettingsActivity.PREF_ACTION_ON_BUTTON, SettingsActivity.actionCodeToString(buttonActionPreference));

            String moveActionPreference = preferences.getString(SettingsActivity.PREF_ACTION_ON_BUTTON, SettingsActivity.PREF_ACTION_DEFAULT);
            conf.put(SettingsActivity.PREF_ACTION_ON_MOVE, SettingsActivity.actionCodeToString(moveActionPreference));

            String flipActionPreference = preferences.getString(SettingsActivity.PREF_ACTION_ON_BUTTON, SettingsActivity.PREF_ACTION_DEFAULT);
            conf.put(SettingsActivity.PREF_ACTION_ON_FLIP, SettingsActivity.actionCodeToString(flipActionPreference));

            String shakeActionPreference = preferences.getString(SettingsActivity.PREF_ACTION_ON_BUTTON, SettingsActivity.PREF_ACTION_DEFAULT);
            conf.put(SettingsActivity.PREF_ACTION_ON_SHAKE, SettingsActivity.actionCodeToString(shakeActionPreference));

            String proximityActionPreference = preferences.getString(SettingsActivity.PREF_ACTION_ON_BUTTON, SettingsActivity.PREF_ACTION_DEFAULT);
            conf.put(SettingsActivity.PREF_ACTION_ON_PROXIMITY, SettingsActivity.actionCodeToString(proximityActionPreference));

            boolean checkAlarmTimePreference = preferences.getBoolean(SettingsActivity.PREF_CHECK_ALARM_TIME, SettingsActivity.PREF_CHECK_ALARM_TIME_DEFAULT);
            conf.put(SettingsActivity.PREF_CHECK_ALARM_TIME, checkAlarmTimePreference);

            String checkAlarmTimeAtPreference = preferences.getString(SettingsActivity.PREF_CHECK_ALARM_TIME_AT, SettingsActivity.PREF_CHECK_ALARM_TIME_AT_DEFAULT);
            conf.put(SettingsActivity.PREF_CHECK_ALARM_TIME_AT, checkAlarmTimeAtPreference);

            int checkAlarmTimeGap = preferences.getInt(SettingsActivity.PREF_CHECK_ALARM_TIME_GAP, SettingsActivity.PREF_CHECK_ALARM_TIME_GAP_DEFAULT);
            conf.put(SettingsActivity.PREF_CHECK_ALARM_TIME_GAP, checkAlarmTimeGap);

            boolean nighttimeBellPreference = preferences.getBoolean(SettingsActivity.PREF_NIGHTTIME_BELL, SettingsActivity.PREF_NIGHTTIME_BELL_DEFAULT);
            conf.put(SettingsActivity.PREF_NIGHTTIME_BELL, nighttimeBellPreference);

            String nighttimeBellAtPreference = preferences.getString(SettingsActivity.PREF_NIGHTTIME_BELL_AT, SettingsActivity.PREF_NIGHTTIME_BELL_AT_DEFAULT);
            conf.put(SettingsActivity.PREF_NIGHTTIME_BELL_AT, nighttimeBellAtPreference);

            String nighttimeBellRingtonePreference = preferences.getString(SettingsActivity.PREF_NIGHTTIME_BELL_RINGTONE, SettingsActivity.PREF_NIGHTTIME_BELL_RINGTONE_DEFAULT);
            conf.put(SettingsActivity.PREF_NIGHTTIME_BELL_RINGTONE, nighttimeBellRingtonePreference);

            // Environment
            conf.put("build_brand", Build.BRAND);
            conf.put("build_device", Build.DEVICE);
            conf.put("build_display", Build.DISPLAY);
            conf.put("build_fingerprint", Build.FINGERPRINT);
            conf.put("build_manufacturer", Build.MANUFACTURER);
            conf.put("build_model", Build.MODEL);
            conf.put("build_product", Build.PRODUCT);
            conf.put("build_hardware", Build.HARDWARE);
            conf.put("build_host", Build.HOST);
            conf.put("build_id", Build.ID);
            conf.put("build_user", Build.USER);
            conf.put("build_board", Build.BOARD);
            conf.put("build_serial", Build.SERIAL);

            conf.put("build_version_release", Build.VERSION.RELEASE);
            conf.put("build_version_sdk_int", Build.VERSION.SDK_INT);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                conf.put("build_version_base_os", Build.VERSION.BASE_OS);
            }

            conf.put("buildConfig_application_id", BuildConfig.APPLICATION_ID);
            conf.put("buildConfig_build_type", BuildConfig.BUILD_TYPE);
            conf.put("buildConfig_debug", BuildConfig.DEBUG);
            conf.put("buildConfig_flavor", BuildConfig.FLAVOR);
            conf.put("buildConfig_version_code", BuildConfig.VERSION_CODE);
            conf.put("buildConfig_version_name", BuildConfig.VERSION_NAME);

            Configuration configuration = mContext.getResources().getConfiguration();
            conf.put("configuration_mcc", configuration.mcc);
            conf.put("configuration_mnc", configuration.mnc);
            conf.put("configuration_uiMode", configuration.uiMode);
            conf.put("configuration_locale", configuration.locale);

            conf.put("settingsSystem_time_12_24", Settings.System.getString(mContext.getContentResolver(), Settings.System.TIME_12_24));
            conf.put("settingsSystem_sys_prop_setting_version", Settings.System.getString(mContext.getContentResolver(), Settings.System.SYS_PROP_SETTING_VERSION));

            Locale locale = Locale.getDefault();
            conf.put("locale", locale.toString());

            conf.put("locale_Country", locale.getCountry());
            conf.put("locale_Language", locale.getLanguage());
            conf.put("locale_Variant", locale.getVariant());

//            conf.put("locale_DisplayCountry", locale.getDisplayCountry());
//            conf.put("locale_DisplayLanguage", locale.getDisplayLanguage());
//            conf.put("locale_DisplayVariant", locale.getDisplayVariant());
//
//            conf.put("locale_DisplayName", locale.getDisplayName());
//            conf.put("locale_ISO3Country", locale.getISO3Country());
//            conf.put("locale_ISO3Language", locale.getISO3Language());
//
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
//                conf.put("locale_Script", locale.getScript());
//                conf.put("locale_DisplayScript", locale.getDisplayScript());
//            }

            TimeZone timeZone = TimeZone.getDefault();
            conf.put("timeZone_ID", timeZone.getID());
            conf.put("timeZone_DisplayName", timeZone.getDisplayName());
            conf.put("timeZone_RawOffset", timeZone.getRawOffset());
            conf.put("timeZone_DSTSavings", timeZone.getDSTSavings());

            com.ibm.icu.util.Calendar cal = com.ibm.icu.util.Calendar.getInstance();
            for (int dayOfWeek : AlarmDataSource.allDaysOfWeek) {
                String dayOfWeekText = Localization.dayOfWeekToStringShort(mContext.getResources(), dayOfWeek);
                int dayOfWeekType = cal.getDayOfWeekType(dayOfWeek);
                conf.put("dayOfWeekType_" + dayOfWeek + "_" + dayOfWeekText, dayOfWeekTypeToString(dayOfWeekType));
            }
        } catch (JSONException e) {
            Log.w(TAG, "Cannot create configuration record", e);
        }

        mPayload.putSerializable(Param.Configuration.name(), conf.toString());

        return this;
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
            editor.commit();
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

    public static String calendarToUTC(Calendar calendar) {
        SimpleDateFormat sdfTime = new SimpleDateFormat(DATE_FORMAT_UTC);
        return sdfTime.format(calendar.getTime());
    }

    public static String calendarToDate(Calendar calendar) {
        SimpleDateFormat sdfTime = new SimpleDateFormat(DATE_FORMAT);
        return sdfTime.format(calendar.getTime());
    }

    public static String calendarToTime(Calendar calendar) {
        SimpleDateFormat sdfTime = new SimpleDateFormat(TIME_FORMAT);
        return sdfTime.format(calendar.getTime());
    }
}
