package cz.jaro.alarmmorning.nighttimebell;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.AssetFileDescriptor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import java.util.Calendar;

import cz.jaro.alarmmorning.R;
import cz.jaro.alarmmorning.SettingsActivity;
import cz.jaro.alarmmorning.SystemAlarm;
import cz.jaro.alarmmorning.checkalarmtime.CheckAlarmTime;

/**
 * This class implements the "Nighttime bell" also known as "taps bugle call". Specifically, a sound is played every day at a specified time to remind the
 * people about the time.
 */
public class NighttimeBell {

    private static final String TAG = NighttimeBell.class.getSimpleName();

    private static NighttimeBell instance;

    private Context context;
    private AlarmManager alarmManager;

    private Intent intent;
    private PendingIntent operation;

    /**
     * Action meaning: Play the sound.
     */
    protected static final String ACTION_PLAY = "PLAY_NIGHTTIME_BELL";

    private NighttimeBell(Context context) {
        this.context = context;
        alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
    }

    public static NighttimeBell getInstance(Context context) {
        if (instance == null) {
            instance = new NighttimeBell(context);
        }
        return instance;
    }

    public void checkAndRegister() {
        Log.v(TAG, "checkAndRegister()");

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        boolean checkAlarmTimePreference = preferences.getBoolean(SettingsActivity.PREF_NIGHTTIME_BELL, SettingsActivity.PREF_NIGHTTIME_BELL_DEFAULT);

        if (checkAlarmTimePreference) {
            register();
        }
    }

    public void register() {
        Log.d(TAG, "register()");

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        String nighttimeBellAtPreference = preferences.getString(SettingsActivity.PREF_NIGHTTIME_BELL_AT, SettingsActivity.PREF_NIGHTTIME_BELL_AT_DEFAULT);

        register(nighttimeBellAtPreference);
    }

    private void register(String stringValue) {
        Log.d(TAG, "register(stringValue=)" + stringValue);

        Calendar playNighttimeBellAt = CheckAlarmTime.calcNextOccurence(context, stringValue);

        String action = ACTION_PLAY;
        Log.i(TAG, "Setting system alarm at " + playNighttimeBellAt.getTime().toString() + " with action " + action);

        intent = new Intent(context, NighttimeBellAlarmReceiver.class);
        intent.setAction(action);

        operation = PendingIntent.getBroadcast(context, 1, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        SystemAlarm.setSystemAlarm(alarmManager, playNighttimeBellAt, operation);
    }

    public void unregister() {
        Log.d(TAG, "unregister()");

        if (operation != null) {
            // Method 1: standard
            Log.d(TAG, "Cancelling current system alarm");
            operation.cancel();
        } else {
            // Method 2: try to recreate the operation
            Log.d(TAG, "Recreating operation when cancelling system alarm");

            Intent intent2 = new Intent(context, NighttimeBellAlarmReceiver.class);
            intent2.setAction(ACTION_PLAY);

            PendingIntent operation2 = PendingIntent.getBroadcast(context, 1, intent2, PendingIntent.FLAG_NO_CREATE);

            if (operation2 != null) {
                operation2.cancel();
            }
        }
    }

    public void reregister(String stringValue) {
        unregister();
        register(stringValue);
    }

    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        Log.i(TAG, "Acting on CheckAlarmTime. action=" + action);

        if (action.equals(ACTION_PLAY)) {
            onPlay(context);
        } else {
            throw new IllegalArgumentException("Unexpected argument " + action);
        }
    }

    private void onPlay(Context context) {
        Log.d(TAG, "onPlay()");

        // Register for tomorrow
        register();

        // Toast
        String toastText = context.getResources().getString(R.string.nighttime_bell_toast);
        Toast.makeText(context, toastText, Toast.LENGTH_LONG).show();

        // Play
        Uri ringtoneUri = getRingtoneUri();

        try {
            MediaPlayer mediaPlayer;
            mediaPlayer = new MediaPlayer();

            if (ringtoneUri != null) {
                mediaPlayer.setDataSource(context, ringtoneUri);
            } else {
                // Play the Raw file
                AssetFileDescriptor afd = context.getAssets().openFd(context.getResources().getResourceEntryName(R.raw.church_clock_strikes_3) + ".mp3");
                mediaPlayer.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength()); // play this file only
                afd.close();
            }

            mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    Log.d(TAG, "onCompletion");
                    mp.release();
                }
            });
            mediaPlayer.setAudioStreamType(AudioManager.STREAM_NOTIFICATION);
            mediaPlayer.prepare();
            mediaPlayer.start();
        } catch (Exception e) {
            Log.e(TAG, "Unable to play nighttime bell ringtone as media", e);
        }
    }

    /**
     *
     * @return Ringtone Uri. Return null if the default value is set (meaning: play the Raw file instead of a ringtone).
     */
    private Uri getRingtoneUri() {
        Uri ringtoneUri;
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);

        if (preferences.contains(SettingsActivity.PREF_NIGHTTIME_BELL_RINGTONE)) {
            String ringtonePreference = preferences.getString(SettingsActivity.PREF_NIGHTTIME_BELL_RINGTONE, SettingsActivity.PREF_NIGHTTIME_BELL_RINGTONE_DEFAULT);
            ringtoneUri = ringtonePreference.equals(SettingsActivity.PREF_NIGHTTIME_BELL_RINGTONE_DEFAULT) ? null : Uri.parse(ringtonePreference);
        } else {
            ringtoneUri = null;
        }

        return ringtoneUri;
    }

}