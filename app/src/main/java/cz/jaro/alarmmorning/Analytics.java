package cz.jaro.alarmmorning;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;

import com.google.firebase.analytics.FirebaseAnalytics;

import java.io.Serializable;
import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.Random;

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
    private static final String TAG = AlarmMorningActivity.class.getSimpleName();

    private static final String PREF_USER_ID = "user_id";
    private static final String PREF_USER_ID_UNSET = "";
    private static final int USER_ID_LENGTH = 12;

    private static final String DATE_FORMAT_UTC = "yyyy-MM-dd HH:mm:ss.SSS";
    private static final String DATE_FORMAT = "yyyy-MM-dd";
    private static final String TIME_FORMAT = "HH:mm";

    public static final String DISABLED = "Disabled";

    public static final String DAY_OF_WEEK_TYPE__WORKDAY = "Workday";
    public static final String DAY_OF_WEEK_TYPE__WEEKEND = "Weekend";

    public static final String ALARM_STATE__SET_TO_SPECIFIC = "Set to specific";
    public static final String ALARM_STATE__SET_TO_DEFAULT = "Set to default";

    public static final String CHECK_ALARM_TIME_ACTION__SHOW_NOTIFICATION = "Show notification";
    public static final String CHECK_ALARM_TIME_ACTION__DON_T_SHOW_NOTIFICATION = "Don't show notification";
    public static final String CHECK_ALARM_TIME_ACTION__NO_APPOINTMENT = "No appointment";

    public static final String CHECK_ALARM_TIME_METHOD__QUICK = "Quick";
    public static final String CHECK_ALARM_TIME_METHOD__DIALOG = "Dialog";

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
        Default_alarm_time, // TODO

        Alarm_time_old,

        Check_alarm_time_action,
        Check_alarm_time_gap,
        Check_alarm_time_method,

        Appointment_begin,
        Appointment_title,
        Appointment_location,

        Preference_key,
        Preference_value;

        @Override
        public String toString() {
            return name().replace('_', ' ');
        }
    }

    public enum Event {
        Set_default, // TODO

        Set_alarm,
        Ring, // TODO
        Snooze, // TODO
        Dismiss, // TODO
        Auto_dismiss, // TODO

        Skipped_alarm, // TODO

        Add, // TODO
        Click,
        Remove, // TODO

        /**
         * Use just before calling {@link NotificationManager#notify(int, Notification)} }
         */
        Show,

        /**
         * Use when a notification is cancelled
         */
        Hide,

        Play_nighttime_bell,

        Boot, // TODO
        Upgrade, // TODO

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
        Widget // TODO
    }

    public enum ChannelName {
        Calendar,
        Defaults, // TODO
        Settings, // TODO
        Ring, // TODO
        Wizard, // TODO
        Check_alarm_time,
        Nighttime_bell,
        Statistics; // TODO

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
            Defaults defaults2 = day != null ? day.getDefaults() : defaults;

            int hour = defaults2.getHour();
            int minute = defaults2.getMinute();

            Calendar now = now();
            Calendar defaultAlarmTime = (Calendar) now.clone();
            defaultAlarmTime.set(Calendar.HOUR_OF_DAY, hour);
            defaultAlarmTime.set(Calendar.MINUTE, minute);

            String defaultAlarmTimeString = calendarToTime(defaultAlarmTime);
            mPayload.putString(Param.Default_alarm_time.name(), defaultAlarmTimeString);
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
        String dayOfWeekTypeString = dayOfWeekType == com.ibm.icu.util.Calendar.WEEKEND ? DAY_OF_WEEK_TYPE__WEEKEND : DAY_OF_WEEK_TYPE__WORKDAY;
        mPayload.putString(Param.Day_of_week_type.name(), dayOfWeekTypeString);

        String alarmStateString;
        switch (day.getState()) {
            case Day.STATE_DISABLED:
                alarmStateString = DISABLED;
                break;
            case Day.STATE_ENABLED:
                alarmStateString = ALARM_STATE__SET_TO_SPECIFIC;
                break;
            case Day.STATE_DEFAULT:
                alarmStateString = ALARM_STATE__SET_TO_DEFAULT;
                break;
            default:
                throw new IllegalArgumentException("Unexpected day state: " + day.getState());
        }
        mPayload.putString(Param.Alarm_state.name(), alarmStateString);

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
                padLeft(Param.Default_alarm_time, 5) + " | " +

                padLeft(Param.Alarm_time_old, 5) + " | " +

                padRight(Param.Check_alarm_time_action, 23) + " | " +
                padRight(Param.Check_alarm_time_gap, 3) + " | " +
                padRight(Param.Check_alarm_time_method, 6) + " | " +

                padLeft(Param.Appointment_begin, 5) + " | " +
                padRight(Param.Appointment_title, 20) + " | " +
                padRight(Param.Appointment_location, 20) + " | " +

                padRight(Param.Preference_key, 30) + " | " +
                padRight(Param.Preference_value, 10) + " | " +

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
