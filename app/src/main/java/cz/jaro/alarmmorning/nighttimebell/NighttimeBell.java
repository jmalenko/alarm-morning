package cz.jaro.alarmmorning.nighttimebell;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.widget.Toast;

import java.util.Calendar;

import cz.jaro.alarmmorning.Analytics;
import cz.jaro.alarmmorning.MyLog;
import cz.jaro.alarmmorning.R;
import cz.jaro.alarmmorning.SettingsActivity;
import cz.jaro.alarmmorning.SharedPreferencesHelper;
import cz.jaro.alarmmorning.SystemAlarm;
import cz.jaro.alarmmorning.checkalarmtime.CheckAlarmTime;

/**
 * This class implements the "Nighttime bell" also known as "taps bugle call". Specifically, a sound is played every day at a specified time to remind the
 * people about the time.
 */
public class NighttimeBell {

    private static NighttimeBell instance;

    private final Context context;
    private final AlarmManager alarmManager;

    private PendingIntent operation;

    /**
     * Action meaning: Play the sound.
     */
    public static final String ACTION_PLAY = "PLAY_NIGHTTIME_BELL";

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

    public boolean isEnabled() {
        MyLog.v("isEnabled()");

        return (boolean) SharedPreferencesHelper.load(SettingsActivity.PREF_NIGHTTIME_BELL, SettingsActivity.PREF_NIGHTTIME_BELL_DEFAULT);
    }

    public void checkAndRegister() {
        MyLog.v("checkAndRegister()");

        if (isEnabled()) {
            register();
        }
    }

    public void register() {
        MyLog.d("register()");

        String nighttimeBellAtPreference = (String) SharedPreferencesHelper.load(SettingsActivity.PREF_NIGHTTIME_BELL_AT, SettingsActivity.PREF_NIGHTTIME_BELL_AT_DEFAULT);

        register(nighttimeBellAtPreference);
    }

    private void register(String stringValue) {
        MyLog.d("register(stringValue=)" + stringValue);

        Calendar playNighttimeBellAt = CheckAlarmTime.calcNextOccurence(stringValue);

        String action = ACTION_PLAY;
        MyLog.i("Setting system alarm at " + playNighttimeBellAt.getTime().toString() + " with action " + action);

        Intent intent = new Intent(context, NighttimeBellAlarmReceiver.class);
        intent.setAction(action);

        operation = PendingIntent.getBroadcast(context, 1, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        SystemAlarm.setSystemAlarm(alarmManager, playNighttimeBellAt, operation);
    }

    public void unregister() {
        MyLog.d("unregister()");

        if (operation != null) {
            // Method 1: standard
            MyLog.d("Cancelling current system alarm");
            operation.cancel();
        } else {
            // Method 2: try to recreate the operation
            MyLog.d("Recreating operation when cancelling system alarm");

            Intent intent2 = new Intent(context, NighttimeBellAlarmReceiver.class);
            intent2.setAction(ACTION_PLAY);

            PendingIntent operation2 = PendingIntent.getBroadcast(context, 1, intent2, PendingIntent.FLAG_NO_CREATE);

            if (operation2 != null) {
                operation2.cancel();
            }
        }
    }

    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        MyLog.i("Acting on CheckAlarmTime. action=" + action);

        if (action.equals(ACTION_PLAY)) {
            // The condition is needed for cases we are unable to unregister a system alarm.
            if (!isEnabled()) return;

            onPlay(context);
        } else {
            throw new IllegalArgumentException("Unexpected argument " + action);
        }
    }

    private void onPlay(Context context) {
        MyLog.d("onPlay()");
        new Analytics(context, Analytics.Event.Play_nighttime_bell, Analytics.Channel.Time, Analytics.ChannelName.Nighttime_bell).save();

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
                AssetFileDescriptor afd = context.getResources().openRawResourceFd(R.raw.church_clock_strikes_3);
                mediaPlayer.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength()); // play this file only
                afd.close();
            }

            mediaPlayer.setOnCompletionListener(mp -> {
                MyLog.d("onCompletion");
                mp.release();
            });
            mediaPlayer.setAudioStreamType(AudioManager.STREAM_ALARM);
            mediaPlayer.prepare();
            mediaPlayer.start();
        } catch (Exception e) {
            MyLog.e("Unable to play nighttime bell ringtone as media", e);
        }
    }

    /**
     *
     * @return Ringtone Uri. Return null if the default value is set (meaning: play the Raw file instead of a ringtone).
     */
    private Uri getRingtoneUri() {
        Uri ringtoneUri;

        if (SharedPreferencesHelper.contains(SettingsActivity.PREF_NIGHTTIME_BELL_RINGTONE)) {
            String ringtonePreference = (String) SharedPreferencesHelper.load(SettingsActivity.PREF_NIGHTTIME_BELL_RINGTONE, SettingsActivity.PREF_NIGHTTIME_BELL_RINGTONE_DEFAULT);
            ringtoneUri = ringtonePreference.equals(SettingsActivity.PREF_NIGHTTIME_BELL_RINGTONE_DEFAULT) ? null : Uri.parse(ringtonePreference);
        } else {
            ringtoneUri = null;
        }

        return ringtoneUri;
    }

}