package cz.jaro.alarmmorning.voice;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.provider.AlarmClock;
import android.util.Log;

import java.util.Calendar;

import cz.jaro.alarmmorning.Analytics;
import cz.jaro.alarmmorning.GlobalManager;
import cz.jaro.alarmmorning.Localization;
import cz.jaro.alarmmorning.R;
import cz.jaro.alarmmorning.checkalarmtime.SetTimeActivity;
import cz.jaro.alarmmorning.clock.Clock;
import cz.jaro.alarmmorning.graphics.Blink;

import static cz.jaro.alarmmorning.model.Day.VALUE_UNSET;

/**
 * Set alarm time. The alarm time is passed as an argument in intent. The activity interacts with the user via voice; visual is static.
 */
public class SetAlarmByVoiceActivity extends Activity {

    private static final String TAG = SetAlarmByVoiceActivity.class.getSimpleName();

    private Calendar alarmTime;

    private int hour;
    private int minute;
    private boolean ok;

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
            // Read parameters
            if (intent.hasExtra(AlarmClock.EXTRA_HOUR)) {
                hour = intent.getIntExtra(AlarmClock.EXTRA_HOUR, VALUE_UNSET);
                if (hour == VALUE_UNSET) {
                    Log.w(TAG, "Invalid hour value \"" + hour + "\"");
                    ok = false;
                }
            } else {
                Log.w(TAG, "Extra missing: hour");
                ok = false;
            }

            if (intent.hasExtra(AlarmClock.EXTRA_MINUTES)) {
                minute = intent.getIntExtra(AlarmClock.EXTRA_MINUTES, VALUE_UNSET);
                if (minute == VALUE_UNSET) {
                    Log.w(TAG, "Invalid minute value \"" + hour + "\"");
                    ok = false;
                }
            } else {
                Log.w(TAG, "Extra missing: minute");
                ok = false;
            }
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

        Blink blink = new Blink(this);

        if (ok) {
            // Speak the action through the App Indexing API
//            Thing alarm = new Thing.Builder()
//                    .setName("Alarm for 4:00 PM")
//                    .setDescription("Alarm set for 4:00 PM, with the 'Argon' ringtone"
//                            + " and vibrate turned on.")
//                    .setUrl(APP_URI)
//                    .build();
//
//            Action setAlarmAction = new Action.Builder(Action.TYPE_ADD)
//                    .setObject(alarm)
//                    .setActionStatus(Action.STATUS_TYPE_COMPLETED)
//                    .build();
//
//            AppIndex.AppIndexApi.end(mClient, setAlarmAction);

            // Blink and finish activity
            blink.setMessageText(R.string.blink_set);
            String timeStr = Localization.timeToString(alarmTime.getTime(), getBaseContext());
            blink.setTimeText(timeStr);
        } else {
            blink.setMessageText(R.string.blink_invalid);
        }

        blink.initiateFinish();
    }

    private void saveTime() {
        GlobalManager globalManager = new GlobalManager(getBaseContext());
        Clock clock = globalManager.clock();
        Calendar now = clock.now();

        // Calculate alarm time
        alarmTime = (Calendar) now.clone();
        alarmTime.set(Calendar.HOUR_OF_DAY, hour);
        alarmTime.set(Calendar.MINUTE, minute);
        if (alarmTime.before(now)) {
            alarmTime.add(Calendar.DATE, 1);
        }

        // Save
        Analytics analytics = new Analytics(Analytics.Channel.External, Analytics.ChannelName.Voice);

        SetTimeActivity.save(this, alarmTime, analytics, false);
    }
}
