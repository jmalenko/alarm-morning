package cz.jaro.alarmmorning.voice;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.provider.AlarmClock;
import android.util.Log;

import java.util.Calendar;

import cz.jaro.alarmmorning.AlarmMorningActivity;
import cz.jaro.alarmmorning.Analytics;
import cz.jaro.alarmmorning.GlobalManager;
import cz.jaro.alarmmorning.Localization;
import cz.jaro.alarmmorning.R;
import cz.jaro.alarmmorning.checkalarmtime.SetTimeActivity;
import cz.jaro.alarmmorning.clock.Clock;
import cz.jaro.alarmmorning.graphics.Blink;

import static cz.jaro.alarmmorning.calendar.CalendarUtils.roundDown;
import static cz.jaro.alarmmorning.model.Day.VALUE_UNSET;

/**
 * Set alarm time. The alarm time is passed as an argument in intent. The activity interacts with the user via voice; visual is static.
 */
public class SetAlarmByVoiceActivity extends Activity {

    private static final String TAG = SetAlarmByVoiceActivity.class.getSimpleName();

    private Calendar alarmTime;

    private int hour;
    private int minute;
    private int seconds;
    private boolean skip_ui;
    private boolean ok;

    // TODO Google Now allows cancelling the timer (that was just set).

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        hour = VALUE_UNSET;
        minute = VALUE_UNSET;
        ok = true;

        Intent intent = getIntent();
        if (intent == null) {
            Log.w(TAG, "Intent is null");
            ok = false;
        } else if (AlarmClock.ACTION_SET_ALARM.equals(intent.getAction())) {
            hour = readParam(intent, AlarmClock.EXTRA_HOUR);
            minute = readParam(intent, AlarmClock.EXTRA_MINUTES);
            skip_ui = intent.getBooleanExtra(AlarmClock.EXTRA_SKIP_UI, false);
        } else if (AlarmClock.ACTION_SET_TIMER.equals(intent.getAction())) {
            // If the extra is not specified then show all timers. (This is not mentioned in the reference.)
            if (!intent.hasExtra(AlarmClock.EXTRA_LENGTH)) {
                Intent calendarIntent = new Intent(this, AlarmMorningActivity.class);
                startActivity(calendarIntent);
                finish();
                return;
            }
            seconds = readParam(intent, AlarmClock.EXTRA_LENGTH);
            skip_ui = intent.getBooleanExtra(AlarmClock.EXTRA_SKIP_UI, false);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!isVoiceInteraction()) {
                Log.w(TAG, "Not voice interaction");
                ok = false;
            }
        }

        // Save alarm time
        if (ok) {
            saveTime();
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
                ok = false;
                return VALUE_UNSET;
            }
        } else {
            Log.w(TAG, "Extra missing: " + extraName);
            ok = false;
            return VALUE_UNSET;
        }
    }

    private void saveTime() {
        GlobalManager globalManager = GlobalManager.getInstance();
        Clock clock = globalManager.clock();
        Calendar now = clock.now();

        // Calculate alarm time
        alarmTime = (Calendar) now.clone();

        Intent intent = getIntent();
        switch (intent.getAction()) {
            case AlarmClock.ACTION_SET_ALARM:
                alarmTime.set(Calendar.HOUR_OF_DAY, hour);
                alarmTime.set(Calendar.MINUTE, minute);
                if (alarmTime.before(now)) {
                    alarmTime.add(Calendar.DATE, 1);
                }
                break;
            case AlarmClock.ACTION_SET_TIMER:
                if (120 <= seconds) {
                    alarmTime.add(Calendar.SECOND, seconds);
                } else {
                    alarmTime.add(Calendar.SECOND, seconds);
                    if (alarmTime.get(Calendar.SECOND) <= 30) {
                        if (alarmTime.before(now)) {
                            alarmTime.add(Calendar.MINUTE, 1);
                        }
                    } else {
                        alarmTime.add(Calendar.MINUTE, 1);
                    }
                }
                break;
        }
        roundDown(alarmTime, Calendar.SECOND);

        // Save
        Analytics analytics = new Analytics(Analytics.Channel.External, Analytics.ChannelName.Voice);

        SetTimeActivity.save(this, alarmTime, analytics, false);
    }
}
