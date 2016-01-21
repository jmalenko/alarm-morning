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
        }
    }

    public void stopAll() {
        Log.d(TAG, "stopAll()");

        if (isRinging) {
            Log.i(TAG, "Stop ringing");

            isRinging = false;

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
            final String VALUE_UNSET = "";
            String ringtonePreference = preferences.getString(SettingsActivity.PREF_RINGTONE, VALUE_UNSET);
            ringtoneUri = ringtonePreference.equals(VALUE_UNSET) ? null : Uri.parse(ringtonePreference);
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

    private void startSoundAsMedia(Uri ringtoneUri) throws IOException {
        Log.d(TAG, "startSoundAsMedia()");

        mediaPlayer = new MediaPlayer();
        mediaPlayer.setDataSource(this, ringtoneUri);

        mediaPlayer.setAudioStreamType(AudioManager.STREAM_ALARM);
        mediaPlayer.setLooping(true); // repeat sound
        mediaPlayer.prepare();
        mediaPlayer.start();
    }

    private void stopSound() {
        Log.d(TAG, "stopSound()");

        if (isPlaying) {
            isPlaying = false;

            switch (soundMethod) {
                case 1:
                    mediaPlayer.stop();
                    break;
                case 2:
                    ringtone.stop();
                    break;
            }

            stopVolume();
        }
    }

    private void startVolume() {
        Log.d(TAG, "startVolume()");

        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        previousVolume = audioManager.getStreamVolume(AudioManager.STREAM_ALARM);

        // set max volume
        int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM);
        audioManager.setStreamVolume(AudioManager.STREAM_ALARM, maxVolume, 0);
    }

    private void stopVolume() {
        Log.d(TAG, "stopVolume()");

        audioManager.setStreamVolume(AudioManager.STREAM_ALARM, previousVolume, 0);
    }
}
