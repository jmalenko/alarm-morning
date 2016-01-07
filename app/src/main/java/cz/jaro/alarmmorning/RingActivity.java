package cz.jaro.alarmmorning;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import java.io.IOException;
import java.util.Calendar;
import java.util.GregorianCalendar;


/**
 * Activity that is displayed while the alarm fires.
 */
public class RingActivity extends Activity {

    private static final String TAG = RingActivity.class.getName();

    private Ringtone ringtone;
    private MediaPlayer mediaPlayer;
    private AudioManager audioManager;
    private int previousVolume;
    private boolean isRinging;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate()");
        super.onCreate(savedInstanceState);

        // skip keyguard
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);

        setContentView(R.layout.activity_ring);

        startRinging();
    }

    public void onDismiss(View view) {
        Log.d(TAG, "onDismiss()");
        stopRinging();

        finish();
    }

    private void startRinging() {
        Log.d(TAG, "startRinging()");

        if (!isRinging) {
            Log.i(TAG, "Start ringing");

            isRinging = true;

            updateContent();

            startSound();
        }
    }

    public void stopRinging() {
        Log.d(TAG, "stopRinging()");

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
        Calendar now = new GregorianCalendar();
        String currentTimeString = Localization.timeToString(now.getTime(), this);

        TextView timeView = (TextView) findViewById(R.id.fullscreen_content);
        timeView.setText(currentTimeString);
    }

    private void startSoundAsRingtone(Uri ringtoneUri) {
        Log.d(TAG, "startSoundAsRingtone()");
        ringtone = RingtoneManager.getRingtone(getApplicationContext(), ringtoneUri);
        ringtone.play();
    }

    private void startSound() {
        Log.d(TAG, "startSound()");
        final String VALUE_UNSET = "";

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        String ringtonePreference = preferences.getString("pref_ringtone", VALUE_UNSET);
        Uri ringtoneUri = ringtonePreference.equals(VALUE_UNSET) ? RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM) : Uri.parse(ringtonePreference);

        mediaPlayer = new MediaPlayer();
        try {
            mediaPlayer.setDataSource(this, ringtoneUri);
            audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

            // set max volume
            previousVolume = audioManager.getStreamVolume(AudioManager.STREAM_ALARM);
            int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM);
            audioManager.setStreamVolume(AudioManager.STREAM_ALARM, maxVolume, 0);

            mediaPlayer.setAudioStreamType(AudioManager.STREAM_ALARM);
            mediaPlayer.setLooping(true); // repeat sound
            mediaPlayer.prepare();
            mediaPlayer.start();
        } catch (IOException e) {
            Log.w(TAG, "Unable to play ringtone as media");
            startSoundAsRingtone(ringtoneUri);
        }
    }

    private void stopSound() {
        Log.d(TAG, "stopSound()");
        if (ringtone != null) {
            ringtone.stop();
        }
        mediaPlayer.stop();

        audioManager.setStreamVolume(AudioManager.STREAM_ALARM, previousVolume, 0);
    }

}
