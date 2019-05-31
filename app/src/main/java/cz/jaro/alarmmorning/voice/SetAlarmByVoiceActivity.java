package cz.jaro.alarmmorning.voice;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.provider.AlarmClock;
import android.util.Log;

import java.util.Calendar;
import java.util.TimeZone;

import cz.jaro.alarmmorning.AlarmMorningActivity;
import cz.jaro.alarmmorning.Analytics;
import cz.jaro.alarmmorning.CalendarAdapter;
import cz.jaro.alarmmorning.GlobalManager;
import cz.jaro.alarmmorning.Localization;
import cz.jaro.alarmmorning.R;
import cz.jaro.alarmmorning.calendar.CalendarUtils;
import cz.jaro.alarmmorning.clock.Clock;
import cz.jaro.alarmmorning.graphics.Blink;
import cz.jaro.alarmmorning.model.OneTimeAlarm;

import static cz.jaro.alarmmorning.calendar.CalendarUtils.roundDown;
import static cz.jaro.alarmmorning.model.Day.VALUE_UNSET;
import static cz.jaro.alarmmorning.model.OneTimeAlarm.UTC;

/**
 * Set alarm time. The alarm time is passed as an argument in intent. The activity interacts with the user via voice; visual is static.
 * <p>
 * Examples of the voice commands explicitly supported by this app:
 * <ul>
 * <li>Set an alarm for [time, e.g. 6 a.m.]</li>
 * <li>Set an alarm for [time] (with) labeled [name, e.g. dentist]</li>
 * <li>Set an alarm for [amount of time, e.g. 20 minutes] from now</li>
 * <li>Set an alarm in [amount of time]</li>
 * <li>Set an alarm in [amount of time] labeled [name]</li>
 * <li>Set a timer for [amount of time]</li>
 * <li>Set a timer for [amount of time] labeled [name]</li>
 * <li>Wake me up in [time]</li>
 * <li>Wake me up at [amount of time] [days]</li>
 * </ul>
 * <p>
 * Examples of the voice commands supported by Google Voice. Note that Google Voice says "You can do that in the app" and starts the app (into the calendar):*
 * <ul>
 * <li>Show me my alarms</li>
 * <li>Show me my timers</li>
 * <li>My alarms</li>
 * <li>When is my next alarm?</li>
 * <li>Snooze the alarm</li>
 * <li>Dismiss the alarm</li>
 * <li>Turn off all alarms</li>
 * <li>Turn off all timers</li>
 * <li>Stop the alarm</li>
 * <li>Cancel the alarm</li>
 * </ul>
 * <p>
 * Examples of the unsupported voice commands:
 * <ul>
 * <li>Set a repeating alarm for [time] on [days, e.g. Monday and Friday, everyday]</li>
 * <li>Set a repeating alarm at [time] for [days]</li>
 * </ul>
 */
public class SetAlarmByVoiceActivity extends Activity {

    private static final String TAG = SetAlarmByVoiceActivity.class.getSimpleName();

    private Calendar alarmTime;

    private boolean skip_ui;
    private boolean ok;
    private Calendar now;

    private static int ROUND_IF_LESS_THAN__SECONDS = 120;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        GlobalManager globalManager = GlobalManager.getInstance();
        Clock clock = globalManager.clock();
        now = clock.now();

        String name = null;

        ok = true;

        Intent intent = getIntent();
        if (intent == null) {
            Log.w(TAG, "Intent is null");
            ok = false;
        } else if (AlarmClock.ACTION_SET_ALARM.equals(intent.getAction())) {
            skip_ui = intent.getBooleanExtra(AlarmClock.EXTRA_SKIP_UI, false);

            int hour = readParam(intent, AlarmClock.EXTRA_HOUR);
            int minute = readParam(intent, AlarmClock.EXTRA_MINUTES);
            name = intent.getStringExtra(AlarmClock.EXTRA_MESSAGE);

            setAlarmTime(hour, minute);
        } else if (AlarmClock.ACTION_SET_TIMER.equals(intent.getAction())) {
            // If the extra is not specified then show all timers. (This is not mentioned in the reference.)
            if (!intent.hasExtra(AlarmClock.EXTRA_LENGTH)) {
                Intent calendarIntent = new Intent(this, AlarmMorningActivity.class);
                startActivity(calendarIntent);
                finish();
                return;
            }

            skip_ui = intent.getBooleanExtra(AlarmClock.EXTRA_SKIP_UI, false);

            int seconds = readParam(intent, AlarmClock.EXTRA_LENGTH);
            name = intent.getStringExtra(AlarmClock.EXTRA_MESSAGE);

            setAlarmTime(seconds);

            if (name == null) {
                long diff = alarmTime.getTimeInMillis() - now.getTimeInMillis();
                name = CalendarAdapter.formatTimeDifference(diff, getResources());
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!isVoiceInteraction()) {
                Log.w(TAG, "Not voice interaction");
                ok = false;
            }
        }

        // Save alarm time
        if (ok) {
            // Round to whole minute if set less or equal to 2 minutes in the future
            if (ROUND_IF_LESS_THAN__SECONDS * 1000 <= alarmTime.getTimeInMillis() - now.getTimeInMillis()) {
                roundDown(alarmTime, Calendar.SECOND);
            } else {
                roundDown(alarmTime, Calendar.MILLISECOND);
            }

            // Save
            Analytics analytics = new Analytics(Analytics.Channel.External, Analytics.ChannelName.Voice);

            addOneTimeAlarm(alarmTime, name, analytics);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (!skip_ui) {
            Blink blink = new Blink(this);

            if (ok) {
                // Speak the action through the App Indexing API
//                Thing alarm = new Thing.Builder()
//                        .setName("Alarm for 4:00 PM")
//                        .setDescription("Alarm set for 4:00 PM, with the 'Argon' ringtone"
//                                + " and vibrate turned on.")
//                        .setUrl(APP_URI)
//                        .build();
//
//                Action setAlarmAction = new Action.Builder(Action.TYPE_ADD)
//                        .setObject(alarm)
//                        .setActionStatus(Action.STATUS_TYPE_COMPLETED)
//                        .build();
//
//                AppIndex.AppIndexApi.end(mClient, setAlarmAction);

                // Blink and finish activity
                blink.setMessageText(R.string.blink_set);
                String timeStr = Localization.timeToString(alarmTime.getTime(), getBaseContext());
                blink.setTimeText(timeStr);
            } else {
                blink.setMessageText(R.string.blink_invalid);
            }

            blink.initiateFinish();
        } else {
            finish();
        }
    }

    private int readParam(Intent intent, String extraName) {
        if (intent.hasExtra(extraName)) {
            int value = intent.getIntExtra(extraName, VALUE_UNSET);
            if (value != VALUE_UNSET) {
                return value;
            } else {
                Log.w(TAG, "Invalid extra value \"" + extraName + "\"");
                ok = false;
                return VALUE_UNSET;
            }
        } else {
            Log.w(TAG, "Extra missing: " + extraName);
            ok = false;
            return VALUE_UNSET;
        }
    }

    /**
     * Sets the alarm time to hour:minute. The date is chosen such that the alram time is the earliest possible, now or in future.
     *
     * @param hour   Hour
     * @param minute Minute
     */
    private void setAlarmTime(int hour, int minute) {
        // Adjust alarm time from no
        alarmTime = (Calendar) now.clone();

        alarmTime.set(Calendar.HOUR_OF_DAY, hour);
        alarmTime.set(Calendar.MINUTE, minute);
        if (alarmTime.before(now)) {
            alarmTime.add(Calendar.DATE, 1);
        }
    }

    /**
     * Sets the alarm time to seconds from now.
     *
     * @param seconds Seconds
     */
    private void setAlarmTime(int seconds) {
        // Adjust alarm time from no
        alarmTime = (Calendar) now.clone();

        alarmTime.add(Calendar.SECOND, seconds);
    }

    public static void addOneTimeAlarm(Calendar saveAlarmTime, String name, Analytics analytics) {
        GlobalManager globalManager = GlobalManager.getInstance();

        // Convert alarm time to UTC
        TimeZone utcTZ = TimeZone.getTimeZone(UTC);
        Calendar alarmTimeUTC = Calendar.getInstance(utcTZ);

        CalendarUtils.copyAllFields(saveAlarmTime, alarmTimeUTC);

        // Create one-time alarm
        OneTimeAlarm oneTimeAlarm = new OneTimeAlarm();
        oneTimeAlarm.setAlarmTime(alarmTimeUTC);
        oneTimeAlarm.setName(name);

        // Save
        globalManager.createOneTimeAlarm(oneTimeAlarm, analytics);
    }

}
