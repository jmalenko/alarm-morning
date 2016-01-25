package cz.jaro.alarmmorning;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import java.io.IOException;
import java.util.Calendar;


/**
 * Activity that is displayed while the alarm fires.
 */
public class RingActivity extends Activity {

    private static final String TAG = RingActivity.class.getSimpleName();

    public static final String ACTION_HIDE_ACTIVITY = "cz.jaro.alarmmorning.intent.action.HIDE_ACTIVITY";

    private Ringtone ringtone;
    private MediaPlayer mediaPlayer;
    private AudioManager audioManager;

    private boolean isRinging;

    private boolean isPlaying;
    private int soundMethod;

    private int previousVolume;
    private int volume;
    private int maxVolume;
    private int increasingVolumePercentage;

    private Vibrator vibrator;
    private boolean isVibrating;

    LocalBroadcastManager bManager;
    private static IntentFilter b_intentFilter;

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
                stopAll();

                finish();
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

        startAll();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        bManager.unregisterReceiver(bReceiver);
    }

    public void onDismiss(View view) {
        Log.d(TAG, "onDismiss()");
        Log.i(TAG, "Dismiss");

        stopAll();

        Context context = view.getContext();
        GlobalManager globalManager = new GlobalManager(context);
        globalManager.onDismiss();

        finish();
    }

    public void onSnooze(View view) {
        Log.d(TAG, "onSnooze()");
        Log.i(TAG, "Snooze");

        stopAll();

        Context context = view.getContext();
        GlobalManager globalManager = new GlobalManager(context);
        globalManager.onSnooze();

        finish();
    }

    private void startAll() {
        Log.d(TAG, "startAll()");

        if (!isRinging) {
            Log.i(TAG, "Start ringing");

            isRinging = true;

            updateContent();

            startSound();
            startVibrate();
        }
    }

    public void stopAll() {
        Log.d(TAG, "stopAll()");

        if (isRinging) {
            Log.i(TAG, "Stop ringing");

            isRinging = false;

            stopVibrate();
            stopSound();

            // allow device sleep
            WakeLocker.release();
        }
    }

    private void updateContent() {
        Log.d(TAG, "updateContent()");
        Calendar now = Calendar.getInstance();
        String currentTimeString = Localization.timeToString(now.getTime(), this);

        TextView timeView = (TextView) findViewById(R.id.fullscreen_content);
        timeView.setText(currentTimeString);
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
                Log.d(TAG, "Unable to play ringtone as media");
            }

            if (soundMethod == 0) {
                try {
                    startSoundAsRingtone(ringtoneUri);
                    soundMethod = 2;
                } catch (Exception e) {
                    Log.d(TAG, "Unable to play ringtone as ringtone");
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

        boolean increasing = preferences.getBoolean(SettingsActivity.PREF_VOLUME_INCREASING, SettingsActivity.PREF_VOLUME_INCREASING_DEFAULT);

        if (increasing) {
            increasingVolumePercentage = 0;
            runnable.run();
        } else {
            audioManager.setStreamVolume(AudioManager.STREAM_ALARM, volume, 0);
        }
    }

    /**
     * @return Whether the increasing reached the final volume.
     */
    private boolean updateIncreasingVolume() {
        Log.v(TAG, "increasing volume");

        increasingVolumePercentage++;
        Log.v(TAG, "volume percentage = " + increasingVolumePercentage);
        float ratio = (float) increasingVolumePercentage / 100;
        int tempVolume = (int) Math.ceil(ratio * maxVolume);
        Log.v(TAG, "current volume = " + tempVolume);

        if (volume <= tempVolume) {
            Log.v(TAG, "reached final volume");
            audioManager.setStreamVolume(AudioManager.STREAM_ALARM, volume, 0);
            return true;
        } else {
            audioManager.setStreamVolume(AudioManager.STREAM_ALARM, tempVolume, 0);
            return false;
        }
    }

    private Handler handler = new Handler();
    private Runnable runnable = new Runnable() {
        @Override
        public void run() {
            boolean end = updateIncreasingVolume();

            if (!end) {
                handler.postDelayed(this, 1000);
            }
        }
    };

    private void stopVolume() {
        Log.d(TAG, "stopVolume()");

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

                long[] pattern = {0, 500, 1000};

                vibrator.vibrate(pattern, 0);
            } else {
                Log.w(TAG, "The device cannot vibrate");
            }
        }
    }

    private void stopVibrate() {
        Log.d(TAG, "stopVibrate()");

        if (isVibrating) {
            vibrator.cancel();
        }
    }

}
