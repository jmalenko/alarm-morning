package cz.jaro.alarmmorning;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import java.io.IOException;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Set;

import cz.jaro.alarmmorning.calendar.CalendarEvent;
import cz.jaro.alarmmorning.calendar.CalendarHelper;
import cz.jaro.alarmmorning.clock.Clock;
import cz.jaro.alarmmorning.graphics.Blink;
import cz.jaro.alarmmorning.graphics.SlideButton;
import cz.jaro.alarmmorning.receivers.AlarmReceiver;
import cz.jaro.alarmmorning.sensor.Flip;
import cz.jaro.alarmmorning.sensor.Move;
import cz.jaro.alarmmorning.sensor.Proximity;
import cz.jaro.alarmmorning.sensor.SensorEventDetector;
import cz.jaro.alarmmorning.sensor.Shake;


/**
 * Activity that is displayed while the alarm fires.
 */
public class RingActivity extends Activity implements RingInterface {

    private static final String TAG = RingActivity.class.getSimpleName();

    public static final String ACTION_HIDE_ACTIVITY = "cz.jaro.alarmmorning.intent.action.HIDE_ACTIVITY";

    public static final String ALARM_TIME = "ALARM_TIME";

    private Calendar mAlarmTime;

    private Ringtone ringtone;
    private MediaPlayer mediaPlayer;
    private AudioManager audioManager;

    private boolean isRinging;

    private boolean isPlaying;
    private int soundMethod;

    private int previousVolume;
    private int volume;
    private int maxVolume;
    private boolean increasing;
    private int increasingVolumePercentage;

    private Vibrator vibrator;
    private boolean isVibrating;
    private static final long[] VIBRATOR_PATTERN = {0, 500, 1000};

    private TextView mutedTextView;
    private boolean isMuted;
    private boolean mutedInPast;
    private int mutedSecondsLeft;
    public static final int MUTE_SECONDS = 10;

    private Set<SensorEventDetector> sensorEventDetectors;

    LocalBroadcastManager bManager;
    private static IntentFilter b_intentFilter;

    private SlideButton dismissButton;

    private Blink blink;

    static {
        b_intentFilter = new IntentFilter();
        b_intentFilter.addAction(ACTION_HIDE_ACTIVITY);
    }

    private BroadcastReceiver bReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d(TAG, "onReceive() action=" + action);

            if (action.equals(ACTION_HIDE_ACTIVITY)) {
                if (blink == null) { // The action (snooze, dismiss) was NOT started by this activity
                    stopAll();

                    finish();
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate()");
        super.onCreate(savedInstanceState);

        // skip keyguard
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);

        setContentView(R.layout.activity_ring);

        bManager = LocalBroadcastManager.getInstance(this);
        bManager.registerReceiver(bReceiver, b_intentFilter);

        // This work only for android 4.4+
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            final int flags = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
            getWindow().getDecorView().setSystemUiVisibility(flags);

            // Code below is to handle presses of Volume up or Volume down.
            // Without this, after pressing volume buttons, the navigation bar will show up and won't hide.
            final View decorView = getWindow().getDecorView();
            decorView.setOnSystemUiVisibilityChangeListener(new View.OnSystemUiVisibilityChangeListener() {
                @Override
                public void onSystemUiVisibilityChange(int visibility) {
                    if ((visibility & View.SYSTEM_UI_FLAG_FULLSCREEN) == 0) {
                        decorView.setSystemUiVisibility(flags);
                    }
                }
            });
        }

        if (savedInstanceState != null) {
            mAlarmTime = (Calendar) savedInstanceState.getSerializable(ALARM_TIME);
        }

        Calendar alarmTime = (Calendar) getIntent().getSerializableExtra(ALARM_TIME);
        if (alarmTime != null) {
            this.mAlarmTime = alarmTime;
        }

        dismissButton = (SlideButton) findViewById(R.id.dismissButton);
        dismissButton.setSlideButtonListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onDismiss(view);
            }
        });

        startAll();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        // The seetings must be reset after the user interacts with UI
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT && hasFocus) {
            final int flags = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
            getWindow().getDecorView().setSystemUiVisibility(flags);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        Log.v(TAG, "onSaveInstanceState()");
        super.onSaveInstanceState(savedInstanceState);

        savedInstanceState.putSerializable(ALARM_TIME, mAlarmTime);
    }

    @Override
    protected void onDestroy() {
        Log.v(TAG, "onDestroy()");
        super.onDestroy();

        bManager.unregisterReceiver(bReceiver);
    }

    public void onDismiss(View view) {
        Log.i(TAG, "Dismiss");
        doDismiss(view.getContext());
    }

    public void onSnooze(View view) {
        Log.i(TAG, "Snooze");
        doSnooze(view.getContext());
    }

    public void doDismiss(Context context) {
        Log.d(TAG, "doDismiss()");

        stopAll();

        Analytics analytics = new Analytics(Analytics.Channel.Activity, Analytics.ChannelName.Ring);

        blink = new Blink(this);

        GlobalManager globalManager = GlobalManager.getInstance();
        globalManager.onDismiss(analytics);

        blink.setMessageText(R.string.blink_dismiss);
        blink.initiateFinish();
    }

    public void doSnooze(Context context) {
        Log.d(TAG, "doSnooze()");

        stopAll();

        Analytics analytics = new Analytics(Analytics.Channel.Activity, Analytics.ChannelName.Ring);

        blink = new Blink(this);

        GlobalManager globalManager = GlobalManager.getInstance();
        Calendar ringAfterSnoozeTime = globalManager.onSnooze(analytics);

        blink.setMessageText(R.string.blink_snooze);
        String timeStr = Localization.timeToString(ringAfterSnoozeTime.getTime(), getBaseContext());
        blink.setTimeText(timeStr);
        blink.initiateFinish();
    }

    public void doMute() {
        Log.d(TAG, "doMute()");

        if (muteAvailable()) {
            Log.i(TAG, "Mute");
            startMute();
        } else {
            Log.d(TAG, "Not muting because it had been mutedTextView");
        }
    }

    private void initMute() {
        mutedTextView = (TextView) findViewById(R.id.muted);

        mutedInPast = false;
    }

    public boolean muteAvailable() {
        Log.d(TAG, "muteAvailable()");
        return !mutedInPast;
    }

    private void startMute() {
        Log.d(TAG, "startMute()");

        isMuted = true;
        mutedInPast = true;
        mutedSecondsLeft = MUTE_SECONDS + 1;

        muteSound();
        muteVibrate();

        mutedTextView.setVisibility(View.VISIBLE);
        updateMute();

        runnableMute.run();
    }

    private Handler handlerMute = new Handler();
    private Runnable runnableMute = new Runnable() {
        @Override
        public void run() {
            Log.d(TAG, "run()");

            mutedSecondsLeft--;
            if (mutedSecondsLeft > 0) {
                updateMute();

                handlerContent.postDelayed(this, 1000);
            } else {
                stopMute();
            }
        }
    };

    private void updateMute() {
        Log.d(TAG, "updateMute()");

        Resources res = getResources();
        String muteText = res.getString(R.string.muted, mutedSecondsLeft);

        mutedTextView.setText(muteText);
    }

    private void stopMute() {
        Log.d(TAG, "stopMute()");

        if (isMuted) {
            Log.i(TAG, "Unmute");
            isMuted = false;

            unmuteVibrate();
            unmuteSound();

            mutedTextView.setVisibility(View.INVISIBLE);

            handlerMute.removeCallbacks(runnableMute);
        }
    }

    private void startAll() {
        Log.d(TAG, "startAll()");

        if (!isRinging) {
            Log.i(TAG, "Start ringing");

            isRinging = true;

            startContent();

            startSound();
            startVibrate();

            initMute();
            startSensors();
        }
    }

    /**
     * The wake lock that was acquired on system notification in {@link AlarmReceiver#onReceive(Context, Intent)} must be released.
     */
    public void stopAll() {
        Log.d(TAG, "stopAll()");

        if (isRinging) {
            Log.i(TAG, "Stop ringing");

            isRinging = false;

            stopSensors();
            stopMute();

            stopVibrate();
            stopSound();

            stopContent();

            // allow device sleep
            WakeLocker.release();
        }
    }

    private void startContent() {
        Log.d(TAG, "startContent()");

        handlerContent.start();
    }

    private Runnable runnableContent = new Runnable() {
        @Override
        public void run() {
            updateContent();
        }
    };
    private HandlerOnClockChange handlerContent = new HandlerOnClockChange(runnableContent, Calendar.MINUTE);

    private void stopContent() {
        Log.d(TAG, "stopContent()");

        handlerContent.stop();
    }

    private void updateContent() {
        Log.d(TAG, "updateContent()");
        Calendar now = clock().now();

        Resources res = getResources();
        String currentDateString = Localization.dateToStringFull(res, now.getTime());
        TextView dateView = (TextView) findViewById(R.id.date);
        dateView.setText(currentDateString);

        String currentTimeString = Localization.timeToString(now.getTime(), this);
        TextView timeView = (TextView) findViewById(R.id.time);
        timeView.setText(currentTimeString);

        TextView alarmTimeView = (TextView) findViewById(R.id.alarmTime);
        if (onTheSameMinute(mAlarmTime, now)) {
            alarmTimeView.setVisibility(View.INVISIBLE);
        } else {
            String alarmTimeText;
            String timeStr = Localization.timeToString(mAlarmTime.getTime(), getBaseContext());
            if (onTheSameDate(mAlarmTime, now)) {
                alarmTimeText = res.getString(R.string.alarm_was_set_to_today, timeStr);
            } else {
                String dateStr = Localization.dateToStringFull(res, mAlarmTime.getTime());
                alarmTimeText = res.getString(R.string.alarm_was_set_to_nontoday, timeStr, dateStr);
            }
            alarmTimeView.setText(alarmTimeText);
            alarmTimeView.setVisibility(View.VISIBLE);
        }

        /*
        Show the next calendar entry, which is one or more entries defined by the following rules:
          1. Entry starts on or after the alarm time
          2. Entry starts today (before 11:59:59 pm)
          3. Show the entry that starts earliest from now
        As there alarm may ring (or be snoozed) for a longer period, consider also the following rule:
          4. Show the entry that occurs now
        */
        TextView nextCalendarView = (TextView) findViewById(R.id.nextCalendar);

        Calendar endOfToday = Calendar.getInstance(); // last milisecond in today
        endOfToday.add(Calendar.DATE, 1);
        endOfToday.set(Calendar.HOUR_OF_DAY, 0);
        endOfToday.set(Calendar.MINUTE, 0);
        endOfToday.set(Calendar.SECOND, 0);
        endOfToday.set(Calendar.MILLISECOND, 0);
        endOfToday.add(Calendar.MILLISECOND, -1);

        CalendarHelper calendarHelper = new CalendarHelper(this);
        CalendarEvent event = calendarHelper.find(now, endOfToday);

        if (event != null) {
            String timeStr = Localization.timeToString(event.getBegin().getTime(), getBaseContext());

            String nextCalendarText = event.getLocation() != null && !event.getLocation().isEmpty()?
                    res.getString(R.string.next_calendar_with_location, timeStr, event.getTitle(), event.getLocation()) :
                    res.getString(R.string.next_calendar_without_location, timeStr, event.getTitle());

            nextCalendarView.setText(nextCalendarText);
            nextCalendarView.setVisibility(View.VISIBLE);
        } else {
            nextCalendarView.setVisibility(View.GONE);
        }
    }

    public static boolean onTheSameDate(Calendar cal1, Calendar cal2) {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR);
    }

    public static boolean onTheSameMinute(Calendar cal1, Calendar cal2) {
        return onTheSameDate(cal1, cal2) &&
                cal1.get(Calendar.HOUR_OF_DAY) == cal2.get(Calendar.HOUR_OF_DAY) &&
                cal1.get(Calendar.MINUTE) == cal2.get(Calendar.MINUTE);
    }

    private Uri getRingtoneUri() {
        Uri ringtoneUri;
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getBaseContext());

        if (preferences.contains(SettingsActivity.PREF_RINGTONE)) {
            String ringtonePreference = preferences.getString(SettingsActivity.PREF_RINGTONE, SettingsActivity.PREF_RINGTONE_DEFAULT);
            ringtoneUri = ringtonePreference.equals(SettingsActivity.PREF_RINGTONE_DEFAULT) ? null : Uri.parse(ringtonePreference);
        } else {
            ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
        }

        return ringtoneUri;
    }

    private boolean playSound() {
        Uri ringtoneUri = getRingtoneUri();
        return ringtoneUri != null;
    }

    private void startSound() {
        Log.d(TAG, "startSound()");

        isPlaying = false;

        if (playSound()) {
            isPlaying = true;

            startVolume();

            Uri ringtoneUri = getRingtoneUri();

            soundMethod = 0;

            try {
                startSoundAsMedia(ringtoneUri);
                soundMethod = 1;
            } catch (Exception e) {
                Log.d(TAG, "Unable to play ringtone as media", e);
            }

            if (soundMethod == 0) {
                try {
                    startSoundAsRingtone(ringtoneUri);
                    soundMethod = 2;
                } catch (Exception e) {
                    Log.d(TAG, "Unable to play ringtone as ringtone", e);
                }
            }

            if (soundMethod == 0) Log.e(TAG, "Unable to play ringtone");
        } else {
            Log.w(TAG, "Sound is intentionally not playing");
        }
    }

    private void startSoundAsRingtone(Uri ringtoneUri) {
        Log.d(TAG, "startSoundAsRingtone()");
        ringtone = RingtoneManager.getRingtone(getApplicationContext(), ringtoneUri);
        ringtone.play();
    }

    private void stopSoundAsRingtone() {
        Log.d(TAG, "stopSoundAsRingtone()");
        ringtone.stop();
    }

    private void pauseSoundAsRingtone() {
        Log.d(TAG, "pauseSoundAsRingtone()");
        ringtone.stop();
    }

    private void resumeSoundAsRingtone() {
        Log.d(TAG, "resumeSoundAsRingtone()");
        ringtone.play();
    }

    private void startSoundAsMedia(Uri ringtoneUri) throws IOException {
        Log.d(TAG, "startSoundAsMedia()");

        mediaPlayer = new MediaPlayer();
        mediaPlayer.setDataSource(this, ringtoneUri);

        mediaPlayer.setAudioStreamType(AudioManager.STREAM_ALARM);
        mediaPlayer.setLooping(true); // repeat sound
        mediaPlayer.prepare();
        mediaPlayer.start();
    }

    private void stopSoundAsMedia() {
        Log.d(TAG, "stopSoundAsMedia()");
        mediaPlayer.stop();
    }

    private void pauseSoundAsMedia() {
        Log.d(TAG, "pauseSoundAsMedia()");
        mediaPlayer.pause();
    }

    private void resumeSoundAsMedia() {
        Log.d(TAG, "resumeSoundAsMedia()");
        mediaPlayer.start();
    }

    private void stopSound() {
        Log.d(TAG, "stopSound()");

        if (isPlaying) {
            isPlaying = false;

            switch (soundMethod) {
                case 1:
                    stopSoundAsMedia();
                    break;
                case 2:
                    stopSoundAsRingtone();
                    break;
            }

            stopVolume();
        }
    }

    private void muteSound() {
        Log.d(TAG, "muteSound()");

        if (isPlaying) {
            switch (soundMethod) {
                case 1:
                    pauseSoundAsMedia();
                    break;
                case 2:
                    pauseSoundAsRingtone();
                    break;
            }
        }
    }

    private void unmuteSound() {
        Log.d(TAG, "unmuteSound()");

        if (isPlaying) {
            switch (soundMethod) {
                case 1:
                    resumeSoundAsMedia();
                    break;
                case 2:
                    resumeSoundAsRingtone();
                    break;
            }
        }
    }

    private void startVolume() {
        Log.d(TAG, "startVolume()");

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        int volumePreference = preferences.getInt(SettingsActivity.PREF_VOLUME, SettingsActivity.PREF_VOLUME_DEFAULT);

        if (volumePreference == 0)
            Log.w(TAG, "Volume is set to 0");

        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM);

        previousVolume = audioManager.getStreamVolume(AudioManager.STREAM_ALARM);
        Log.v(TAG, "previous volume= " + previousVolume);

        volume = SettingsActivity.getRealVolume(volumePreference, maxVolume);

        Log.v(TAG, "preference volume = " + volumePreference);
        Log.v(TAG, "max volume= " + maxVolume);
        Log.v(TAG, "volume = " + volume);

        increasing = preferences.getBoolean(SettingsActivity.PREF_VOLUME_INCREASING, SettingsActivity.PREF_VOLUME_INCREASING_DEFAULT);

        if (increasing) {
            increasingVolumePercentage = 0;
            runnableVolume.run();
        } else {
            audioManager.setStreamVolume(AudioManager.STREAM_ALARM, volume, 0);
        }
    }

    /**
     * @return Whether the increasing reached the final volume.
     */
    private boolean updateIncreasingVolume() {
        Log.v(TAG, "updateIncreasingVolume");

        if (isMuted)
            return false;

        increasingVolumePercentage++;
        Log.v(TAG, "   volume percentage = " + increasingVolumePercentage);
        float ratio = (float) increasingVolumePercentage / 100;
        int tempVolume = (int) Math.ceil(ratio * maxVolume);
        Log.v(TAG, "   current volume = " + tempVolume);

        if (volume <= tempVolume) {
            Log.v(TAG, "reached final volume");
            audioManager.setStreamVolume(AudioManager.STREAM_ALARM, volume, 0);
            return true;
        } else {
            audioManager.setStreamVolume(AudioManager.STREAM_ALARM, tempVolume, 0);
            return false;
        }
    }

    private Handler handlerVolume = new Handler();
    private Runnable runnableVolume = new Runnable() {
        @Override
        public void run() {
            boolean end = updateIncreasingVolume();

            if (!end) {
                handlerVolume.postDelayed(this, 1000);
            }
        }
    };

    private void stopVolume() {
        Log.d(TAG, "stopVolume()");

        if (increasing) {
            handlerVolume.removeCallbacks(runnableVolume);
        }

        audioManager.setStreamVolume(AudioManager.STREAM_ALARM, previousVolume, 0);
    }

    private void startVibrate() {
        Log.d(TAG, "startVibrate()");

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        boolean vibratePreference = preferences.getBoolean(SettingsActivity.PREF_VIBRATE, SettingsActivity.PREF_VIBRATE_DEFAULT);

        isVibrating = false;

        if (vibratePreference) {
            vibrator = (Vibrator) getBaseContext().getSystemService(Context.VIBRATOR_SERVICE);

            if (vibrator.hasVibrator()) {
                isVibrating = true;

                vibrator.vibrate(VIBRATOR_PATTERN, 0);

                // To continue vibrating when screen goes off
                IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_OFF);
                registerReceiver(vibrateReceiver, filter);
            } else {
                Log.w(TAG, "The device cannot vibrate");
            }
        }
    }

    public BroadcastReceiver vibrateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
                vibrator.vibrate(VIBRATOR_PATTERN, 0);
            }
        }
    };

    private void stopVibrate() {
        Log.d(TAG, "stopVibrate()");

        if (isVibrating) {
            vibrator.cancel();

            unregisterReceiver(vibrateReceiver);
        }
    }

    private void muteVibrate() {
        Log.d(TAG, "muteVibrate()");

        if (isVibrating) {
            vibrator.cancel();

            unregisterReceiver(vibrateReceiver);
        }
    }

    private void unmuteVibrate() {
        Log.d(TAG, "unmuteVibrate()");

        if (isVibrating) {
            vibrator.vibrate(VIBRATOR_PATTERN, 0);

            // To continue vibrating when screen goes off
            IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_OFF);
            registerReceiver(vibrateReceiver, filter);
        }
    }

    @Override
    public boolean onKeyDown(int keycode, KeyEvent e) {
        Log.d(TAG, "onKeyDown(keycode=" + keycode + ")");

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        String buttonActionPreference = preferences.getString(SettingsActivity.PREF_ACTION_ON_BUTTON, SettingsActivity.PREF_ACTION_DEFAULT);

        switch (buttonActionPreference) {
            case SettingsActivity.PREF_ACTION_DEFAULT:
                if (keycode == KeyEvent.KEYCODE_BACK) {
                    Log.d(TAG, "Doing nothing on back key.");
                    return true;
                } else {
                    Log.d(TAG, "Doing nothing. Let the operating system handles tke key.");
                    return super.onKeyDown(keycode, e);
                }

            default:
                actOnEvent(buttonActionPreference);
                return true;
        }
    }

    private void startSensors() {
        Log.v(TAG, "startSensors()");
        sensorEventDetectors = new HashSet<>();

        sensorEventDetectors.add(new Flip(this));
        sensorEventDetectors.add(new Move(this));
        sensorEventDetectors.add(new Shake(this));
        sensorEventDetectors.add(new Proximity(this));

        for (SensorEventDetector sensorEventDetector : sensorEventDetectors) {
            sensorEventDetector.start();
        }
    }

    private void stopSensors() {
        Log.v(TAG, "stopSensors()");

        for (SensorEventDetector sensorEventDetector : sensorEventDetectors) {
            sensorEventDetector.stop();
        }
    }

    public Clock clock() {
        GlobalManager globalManager = GlobalManager.getInstance();
        return globalManager.clock();
    }

    @Override
    public Context getContextI() {
        return this;
    }

    @Override
    public void actOnEvent(String action) {
        switch (action) {
            case SettingsActivity.PREF_ACTION_DEFAULT:
                Log.d(TAG, "Doing nothing");
                return;

            case SettingsActivity.PREF_ACTION_MUTE:
                doMute();
                return;

            case SettingsActivity.PREF_ACTION_SNOOZE:
                Log.i(TAG, "Snooze");
                doSnooze(getBaseContext());
                return;

            case SettingsActivity.PREF_ACTION_DISMISS:
                Log.i(TAG, "Dismiss");
                doDismiss(getBaseContext());
                return;

            default:
                throw new IllegalArgumentException("Unexpected argument " + action);
        }
    }
}


