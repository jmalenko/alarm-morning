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
import java.security.InvalidParameterException;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Set;

import cz.jaro.alarmmorning.calendar.CalendarEvent;
import cz.jaro.alarmmorning.calendar.CalendarHelper;
import cz.jaro.alarmmorning.calendar.CalendarUtils;
import cz.jaro.alarmmorning.graphics.Blink;
import cz.jaro.alarmmorning.graphics.JoyButton;
import cz.jaro.alarmmorning.graphics.SlideButton;
import cz.jaro.alarmmorning.model.AppAlarm;
import cz.jaro.alarmmorning.model.OneTimeAlarm;
import cz.jaro.alarmmorning.receivers.AlarmReceiver;
import cz.jaro.alarmmorning.sensor.Flip;
import cz.jaro.alarmmorning.sensor.Move;
import cz.jaro.alarmmorning.sensor.Proximity;
import cz.jaro.alarmmorning.sensor.SensorEventDetector;
import cz.jaro.alarmmorning.sensor.Shake;

import static cz.jaro.alarmmorning.GlobalManager.PERSIST_ALARM_ID;
import static cz.jaro.alarmmorning.GlobalManager.PERSIST_ALARM_TYPE;
import static cz.jaro.alarmmorning.calendar.CalendarUtils.endOfToday;
import static cz.jaro.alarmmorning.calendar.CalendarUtils.onTheSameDate;
import static cz.jaro.alarmmorning.calendar.CalendarUtils.onTheSameMinute;


/**
 * Activity that is displayed while the alarm fires.
 * <p>
 * Activity must be started with {@link GlobalManager#PERSIST_ALARM_TYPE} and {@link GlobalManager#PERSIST_ALARM_ID} extras that define the alarm.
 */
public class RingActivity extends Activity implements RingInterface {

    private static final String TAG = GlobalManager.createLogTag(RingActivity.class);

    public static final String ACTION_HIDE_ACTIVITY = "cz.jaro.alarmmorning.intent.action.HIDE_ACTIVITY";

    private AppAlarm appAlarm;

    public static final String LAST_RINGING_START_TIME = "last_ringing_start_time";
    private Calendar lastRingingStartTime;

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

    private TextView snoozeTimeTextView;

    private Set<SensorEventDetector> sensorEventDetectors;

    private boolean actionPerformed = false; // User performed and action (snooze or dismiss) in the activity. That includes auto-snooze and auto-actions.

    LocalBroadcastManager bManager;
    private static final IntentFilter b_intentFilter;

    private boolean focusDuringOnPause;

    private Blink blink;

    static {
        b_intentFilter = new IntentFilter();
        b_intentFilter.addAction(ACTION_HIDE_ACTIVITY);
    }

    private final BroadcastReceiver bReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d(TAG, "onReceive() action=" + action);

            if (action.equals(ACTION_HIDE_ACTIVITY)) {
                if (blink == null) { // The action (snooze, dismiss) was NOT started by this activity
                    shutdown();
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
            decorView.setOnSystemUiVisibilityChangeListener(visibility -> {
                if ((visibility & View.SYSTEM_UI_FLAG_FULLSCREEN) == 0) {
                    decorView.setSystemUiVisibility(flags);
                }
            });
        }

        String alarmType;
        String alarmId;
        GlobalManager globalManager = GlobalManager.getInstance();

        if (savedInstanceState != null) {
            alarmType = savedInstanceState.getString(PERSIST_ALARM_TYPE);
            alarmId = savedInstanceState.getString(PERSIST_ALARM_ID);

            long lastRingingStartTimeMS = savedInstanceState.getLong(LAST_RINGING_START_TIME);
            lastRingingStartTime = CalendarUtils.newGregorianCalendar(lastRingingStartTimeMS);
        } else {
            Intent intent = getIntent();
            alarmType = intent.getStringExtra(PERSIST_ALARM_TYPE);
            alarmId = intent.getStringExtra(PERSIST_ALARM_ID);

            lastRingingStartTime = globalManager.clock().now();
        }
        appAlarm = globalManager.load(alarmType, alarmId);

        if (appAlarm == null) {
            // TODO This should not happen, but it does sometimes (see crash reporting). Therefore we log everything. Possible explanation is that the users
            // Activity manually started without specifying the extra.
            StringBuilder str = new StringBuilder();
            str.append("Stack trace:\n");
            str.append(Log.getStackTraceString(new IllegalStateException("Alarm time should not be null")));
            str.append("\n");
            str.append("Debug info:\n");
            str.append("savedInstanceState is" + (savedInstanceState == null ? "" : " not") + " null");
            str.append("\n");
            str.append("\n");
            str.append("Analytics:\n");
            str.append(this.toString());
            str.append("\n");
            str.append("\n");
            str.append("Analytics with configuration info\n");
            Analytics analytics = new Analytics(this, Analytics.Event.Ring, Analytics.Channel.External, Analytics.ChannelName.Ring);
            analytics.setConfigurationInfo();
            str.append(analytics.toString());
            str.append("\n");
            str.append("\n");
            str.append("Database dump:\n");
            str.append(globalManager.dumpDB());

            Crashlytics.log(str.toString());
            throw new NullPointerException("Cannot get the alarm object");
        }

        SlideButton dismissButton = (SlideButton) findViewById(R.id.dismissButton);
        dismissButton.setOnClickListener(this::onDismiss);

        snoozeTimeTextView = findViewById(R.id.snoozeTimeTextView);

        JoyButton snoozeButton = findViewById(R.id.snoozeButton);
        snoozeButton.setOnJoyClickListener(new JoyButton.OnJoyClickListener() {
            @Override
            public void onDown(View v) {
                Log.v(TAG, "onDown()");
                snoozeTimeTextView.setVisibility(View.VISIBLE);

                int minutes = calcSnoozeMinutes(0, 0, true);
                snoozeTimeTextView.setText(getResources().getQuantityString(R.plurals.snooze_time_text, minutes, minutes));
            }

            @Override
            public void onMove(View v, float dx, float dy, boolean click) {
                Log.v(TAG, "onMove(dx=" + dx + ", dy=" + dy + ", click=" + click + ")");
                int minutes = calcSnoozeMinutes(dx, dy, click);
                snoozeTimeTextView.setText(getResources().getQuantityString(R.plurals.snooze_time_text, minutes, minutes));
            }

            @Override
            public void onUp(View v, float dx, float dy, boolean click) {
                Log.v(TAG, "onUp(dx=" + dx + ", dy=" + dy + ", click=" + click + ")");
                snoozeTimeTextView.setVisibility(View.INVISIBLE);

                int minutes = calcSnoozeMinutes(dx, dy, click);
                doSnooze(minutes, false);
            }

            @Override
            public void onCancel(View v) {
                Log.v(TAG, "onCancel()");
                snoozeTimeTextView.setVisibility(View.INVISIBLE);
            }
        });

        startAll();
    }

    private int calcSnoozeMinutes(float dx, float dy, boolean click) {
        int minutes;

        if (click) {
            // Use default snooze time
            Context context = AlarmMorningApplication.getAppContext();
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
            minutes = preferences.getInt(SettingsActivity.PREF_SNOOZE_TIME, SettingsActivity.PREF_SNOOZE_TIME_DEFAULT);
        } else {
            // Calculate the snooze time from the button position
            double minutesD = positionDeltaToMinutes(dx, dy);
            minutes = (int) Math.round(minutesD);
            if (minutes < 1)
                minutes = 1;
            if (minutes > 15) {
                minutes -= minutes % 5;
            }
        }

        return minutes;
    }

    /**
     * Convert from cartesian coordinates to angle in polar coordinates.
     * <br>
     * Restriction: both dx and dy cannot be zero at the same time.
     *
     * @param dx Delta along the x axis.
     * @param dy Delta along the y axis.
     * @return Angle in radians, in mathematical sense (0 = x-axis, counterclockwise). In interval 0 .. 2*Pi.
     */
    static double positionDeltaToAngle(float dx, float dy) {
        double angle;
        if (dx == 0) {
            if (0 < dy) angle = Math.PI * 3 / 2;
            else if (0 > dy) angle = Math.PI / 2;
            else throw new InvalidParameterException("Cannot calculate angle when there was no move");
        } else {
            if (0 <= dx) {
                if (0 < dy)
                    angle = 2 * Math.PI - Math.atan(dy / dx);
                else if (0 > dy)
                    angle = Math.atan(-dy / dx);
                else
                    angle = 0;
            } else {
                if (0 < dy)
                    angle = Math.PI + Math.atan(dy / -dx);
                else
                    angle = Math.PI - Math.atan(dy / dx);
            }
        }
        return angle;
    }

    static double positionDeltaToMinutes(float dx, float dy) {
        if (dx == 0 && dy == 0) throw new InvalidParameterException("Cannot calculate minutes when the was no move");

        double angle = positionDeltaToAngle(dx, dy);

        double minutes = Math.toDegrees(-angle + Math.PI / 2);
        if (minutes < 0) minutes += 360;
        minutes /= 6;
        return minutes;
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        // The settings must be reset after the user interacts with UI
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

        savedInstanceState.putString(PERSIST_ALARM_TYPE, appAlarm.getClass().getSimpleName());
        savedInstanceState.putString(PERSIST_ALARM_ID, appAlarm.getPersistenceId());
        savedInstanceState.putLong(LAST_RINGING_START_TIME, lastRingingStartTime.getTimeInMillis());
    }

    @Override
    public void onPause() {
        Log.v(TAG, "onPause()");

        // Problem: When starting activity, the activity lifecycle calls are as follows: onCreate, onStart, onResume, onPause, onStop, onStart, onResume
        // Source: https://stackoverflow.com/questions/25369909/onpause-and-onstop-called-immediately-after-starting-activity

        super.onPause();

        focusDuringOnPause = hasWindowFocus();
    }

    @Override
    protected void onStop() {
        Log.v(TAG, "onStop()");

        super.onStop();

        if (focusDuringOnPause) {
            // Normal scenario
            Log.d(TAG, "onStop: Normal scenario");

            if (!actionPerformed) {
                doSnooze(false);
            }
        } else {
            // Activity was started when screen was off / screen was on with keyguard displayed

            Log.v(TAG, "onStop: Exceptional scenario - Activity was started when screen was off / screen was on with keyguard displayed");
            // Do nothing
        }
    }

    @Override
    protected void onDestroy() {
        Log.v(TAG, "onDestroy()");
        super.onDestroy();

        bManager.unregisterReceiver(bReceiver);
    }

    public void shutdown() {
        stopAll();
        finish();
    }

    public void onDismiss(View view) {
        doDismiss(false);
    }

    public void onSnooze(View view) {
        doSnooze(false);
    }

    private void doDismiss(boolean autoDismiss) {
        Log.d(TAG, "doDismiss(autoDismiss=" + autoDismiss + ")");
        Log.i(TAG, "Dismiss");

        stopAll();

        actionPerformed = true;

        Analytics analytics = autoDismiss ?
                new Analytics(Analytics.Channel.Time, Analytics.ChannelName.Ring) :
                new Analytics(Analytics.Channel.Activity, Analytics.ChannelName.Ring);

        blink = new Blink(this);

        GlobalManager globalManager = GlobalManager.getInstance();
        globalManager.onDismiss(appAlarm, analytics);

        blink.setMessageText(R.string.blink_dismiss);
        blink.initiateFinish();
    }

    private void doSnooze(boolean autoSnooze) {
        Log.v(TAG, "doSnooze(autoSnooze=" + autoSnooze + ")");

        Context context = AlarmMorningApplication.getAppContext();
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        int minutes = preferences.getInt(SettingsActivity.PREF_SNOOZE_TIME, SettingsActivity.PREF_SNOOZE_TIME_DEFAULT);

        doSnooze(minutes, autoSnooze);
    }

    private void doSnooze(int minutes, boolean autoSnooze) {
        Log.v(TAG, "doSnooze(minutes=" + minutes + ", autoSnooze=" + autoSnooze + ")");
        Log.i(TAG, "Snooze for " + minutes + " minutes");

        stopAll();

        actionPerformed = true;

        Analytics analytics = autoSnooze ?
                new Analytics(Analytics.Channel.Time, Analytics.ChannelName.Ring) :
                new Analytics(Analytics.Channel.Activity, Analytics.ChannelName.Ring);

        blink = new Blink(this);

        GlobalManager globalManager = GlobalManager.getInstance();
        Calendar ringAfterSnoozeTime = globalManager.onSnooze(appAlarm, minutes, analytics);

        blink.setMessageText(R.string.blink_snooze);
        String timeStr = Localization.timeToString(ringAfterSnoozeTime.getTime(), getBaseContext());
        blink.setTimeText(timeStr);
        blink.initiateFinish();
    }

    private void doMute() {
        Log.d(TAG, "doMute()");

        if (muteAvailable()) {
            Log.i(TAG, "Mute");
            startMute();
        } else {
            Log.d(TAG, "Not muting because it had been not muted");
        }
    }

    private void initMute() {
        mutedTextView = findViewById(R.id.muted);

        mutedInPast = false;
    }

    private boolean muteAvailable() {
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

    private final Handler handlerMute = new Handler();
    private final Runnable runnableMute = new Runnable() {
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

            startSound();
            startVibrate();

            initMute();
            startSensors();

            startContent();
        }
    }

    /**
     * The wake lock that was acquired on system notification in {@link AlarmReceiver#onReceive(Context, Intent)} must be released.
     */
    private void stopAll() {
        Log.d(TAG, "stopAll()");

        if (isRinging) {
            Log.i(TAG, "Stop ringing");

            isRinging = false;

            stopContent();

            stopSensors();
            stopMute();

            stopVibrate();
            stopSound();

            // allow device sleep
            WakeLocker.release();
        }
    }

    private void startContent() {
        Log.d(TAG, "startContent()");

        handlerContent.start();
    }

    private final HandlerOnClockChange handlerContent = new HandlerOnClockChange(this::updateContent, Calendar.MINUTE);

    private void stopContent() {
        Log.d(TAG, "stopContent()");

        handlerContent.stop();
    }

    private void updateContent() {
        Log.d(TAG, "updateContent()");

        if (checkAutoActions())
            return;

        GlobalManager globalManager = GlobalManager.getInstance();
        Calendar now = globalManager.clock().now();

        Resources res = getResources();
        String currentDateString = Localization.dateToStringFull(res, now.getTime());
        TextView dateView = findViewById(R.id.date);
        dateView.setText(currentDateString);

        String currentTimeString = Localization.timeToString(now.getTime(), this);
        TextView timeView = findViewById(R.id.time);
        timeView.setText(currentTimeString);

        TextView alarmTimeView = findViewById(R.id.alarmTime);
        Calendar alarmTime = appAlarm.getDateTime();
        if (onTheSameMinute(alarmTime, now)) {
            alarmTimeView.setVisibility(View.INVISIBLE);
        } else {
            String alarmTimeText;
            String timeStr = Localization.timeToString(alarmTime.getTime(), getBaseContext());
            if (onTheSameDate(alarmTime, now)) {
                alarmTimeText = res.getString(R.string.alarm_was_set_to_today, timeStr);
            } else {
                String dateStr = Localization.dateToStringFull(res, alarmTime.getTime());
                alarmTimeText = res.getString(R.string.alarm_was_set_to_nontoday, timeStr, dateStr);
            }
            alarmTimeView.setText(alarmTimeText);
            alarmTimeView.setVisibility(View.VISIBLE);
        }

        TextView nameView = findViewById(R.id.oneTimeAlarmName);
        if (appAlarm instanceof OneTimeAlarm && ((OneTimeAlarm) appAlarm).getName() != null) {
            OneTimeAlarm oneTimeAlarm = (OneTimeAlarm) appAlarm;
            nameView.setText(oneTimeAlarm.getName());
            nameView.setVisibility(View.VISIBLE);
        } else {
            nameView.setVisibility(View.GONE);
        }

        /*
        Show the next calendar entry, which is one or more entries defined by the following rules:
          1. Entry starts on or after the alarm time
          2. Entry starts today (before 11:59:59 pm)
          3. Show the entry that starts earliest from now
        As there alarm may ring (or be snoozed) for a longer period, consider also the following rule:
          4. Show the entry that occurs now
        */
        TextView nextCalendarView = findViewById(R.id.nextCalendar);

        Calendar endOfToday = endOfToday(now); // last milisecond in today

        CalendarHelper calendarHelper = new CalendarHelper(this);
        CalendarEvent event = calendarHelper.find(now, endOfToday);

        if (event != null) {
            String timeStr = Localization.timeToString(event.getBegin().getTime(), getBaseContext());

            String nextCalendarText = event.getLocation() != null && !event.getLocation().isEmpty() ?
                    res.getString(R.string.next_calendar_with_location, timeStr, event.getTitle(), event.getLocation()) :
                    res.getString(R.string.next_calendar_without_location, timeStr, event.getTitle());

            nextCalendarView.setText(nextCalendarText);
            nextCalendarView.setVisibility(View.VISIBLE);
        } else {
            nextCalendarView.setVisibility(View.GONE);
        }
    }

    /**
     * @return True when an auto action is triggered.
     */
    private boolean checkAutoActions() {
        Log.v(TAG, "checkAutoActions()");
        Context context = this;
        GlobalManager globalManager = GlobalManager.getInstance();
        Calendar now = globalManager.clock().now();

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);

        // Note: Do auto-dismiss before auto-snooze

        // Check auto dismiss
        boolean autoDismiss = preferences.getBoolean(SettingsActivity.PREF_AUTO_DISMISS, SettingsActivity.PREF_AUTO_DISMISS_DEFAULT);
        if (autoDismiss) {
            long autoDismissTime = preferences.getInt(SettingsActivity.PREF_AUTO_DISMISS_TIME, SettingsActivity.PREF_AUTO_DISMISS_TIME_DEFAULT);

            Calendar alarmTime = appAlarm.getDateTime();
            // The following a little hack to account for the fact that when the code is scheduled at particular time, the code that saves the current time is executed few milliseconds later. This applies to both now and lastRingingStartTime bellow variables. We just trim the milliseconds (assumes that the execution happens within one second).
            long diffFromAlarmTime = (now.getTimeInMillis() / 1000 - alarmTime.getTimeInMillis() / 1000) / 60; // in minutes

            boolean doAutoDismiss = autoDismissTime <= diffFromAlarmTime;

            Log.v(TAG, "Auto-dismiss check " + doAutoDismiss + ": auto-dismiss time (" + autoDismissTime + " min) <= time since the alarm time (" + diffFromAlarmTime + " min)");

            if (doAutoDismiss) {
                Log.d(TAG, "Auto-dismiss");
                doDismiss(true);
                return true;
            }
        }

        // Check auto snooze
        boolean autoSnooze = preferences.getBoolean(SettingsActivity.PREF_AUTO_SNOOZE, SettingsActivity.PREF_AUTO_SNOOZE_DEFAULT);
        if (autoSnooze) {
            long autoSnoozeTime = preferences.getInt(SettingsActivity.PREF_AUTO_SNOOZE_TIME, SettingsActivity.PREF_AUTO_SNOOZE_TIME_DEFAULT);

            long diffFromLastRingingStartTime = (now.getTimeInMillis() / 1000 - lastRingingStartTime.getTimeInMillis() / 1000) / 60; // in minutes

            boolean doAutoSnooze = autoSnoozeTime <= diffFromLastRingingStartTime;

            Log.v(TAG, "Auto-snooze check " + doAutoSnooze + ": auto-snooze time (" + autoSnoozeTime + " min) <= time since the start of this ringing (" + diffFromLastRingingStartTime + " min)");

            if (doAutoSnooze) {
                Log.d(TAG, "Auto-snooze");
                doSnooze(true);
                return true;
            }
        }

        return false;
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

    private final Handler handlerVolume = new Handler();
    private final Runnable runnableVolume = new Runnable() {
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

    public final BroadcastReceiver vibrateReceiver = new BroadcastReceiver() {
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

        if (SettingsActivity.PREF_ACTION_DEFAULT.equals(buttonActionPreference)) {
            if (keycode == KeyEvent.KEYCODE_BACK) {
                Log.d(TAG, "Doing nothing on back key.");
                return true;
            } else {
                Log.d(TAG, "Doing nothing. Let the operating system handles tke key.");
                return super.onKeyDown(keycode, e);
            }
        } else {
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

    @Override
    public Context getContextI() {
        return this;
    }

    @Override
    public void actOnEvent(String action) {
        Log.v(TAG, "actOnEvent(action=" + action + ")");
        switch (action) {
            case SettingsActivity.PREF_ACTION_DEFAULT:
                Log.d(TAG, "Doing nothing");
                return;

            case SettingsActivity.PREF_ACTION_MUTE:
                doMute();
                return;

            case SettingsActivity.PREF_ACTION_SNOOZE:
                doSnooze(false);
                return;

            case SettingsActivity.PREF_ACTION_DISMISS:
                doDismiss(false);
                return;

            default:
                throw new IllegalArgumentException("Unexpected argument " + action);
        }
    }
}
