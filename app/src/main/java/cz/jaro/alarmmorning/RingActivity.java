package cz.jaro.alarmmorning;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.hardware.SensorEvent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.view.KeyEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import net.mabboud.android_tone_player.ContinuousBuzzer;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
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
import static java.lang.System.currentTimeMillis;


/**
 * Activity that is displayed while the alarm fires.
 * <p>
 * Activity must be started with {@link GlobalManager#PERSIST_ALARM_TYPE} and {@link GlobalManager#PERSIST_ALARM_ID} extras that define the alarm.
 */
public class RingActivity extends AppCompatActivity implements RingInterface {

    public static final String ACTION_HIDE_ACTIVITY = "cz.jaro.alarmmorning.intent.action.HIDE_ACTIVITY";

    private static final int ALARM_MANAGER_STREAM = AudioManager.STREAM_ALARM;

    public static final int SOUND_METER_DELAY_MILLIS = 100; // Get the sound volume every 100 ms [in milliseconds]. Note that Android provides 1. the amplitude of the intensity and 2. "the maximum volume in the entire previous period" is returned by system call rather than just "the current volume".

    // The silence is detected when
    public static final long SOUND_METER_SILENCE_DURATION = 10000; // in the last 10 seconds [in milliseconds]
    public static final long SOUND_METER_SILENCE_START_AFTER = 5000; // that start 5 seconds [in milliseconds] after the ringing started (if there alarm is not increasing) or once the alarm volume reacheed the target volume
    public static final double SOUND_METER_SILENCE_PERCENTAGE_LIMIT = 0.1; // there is less than 10% [range 0..1] of
    public static final int SOUND_METER_SILENCE_LIMIT = 10; // "loud intervals" i.e. interval with (maximum) volume above 15 dB [in dB]

    // The clap is detected when
    public static final int SOUND_METER_CLAP_PERIODS = 10; // in the last 10 intervals (of length SOUND_METER_DELAY_MILLIS),
    public static final int SOUND_METER_CLAP_MIN_COUNT = 2; // at least 2 of the intervals are exceptionally loud; The exceptionally loud intervals may not be adjacent.
    public static final int SOUND_METER_CLAP_MIN_DIFFERENCE = 5; // The exceptionally loud volume must be at least 5 dB louder than mean.
    public static final int SOUND_METER_CLAP_MIN_ABS = 10; // The exceptionally loud volume must be at least 10 dB loud.

    public static final String SENSOR_NAME__MOVE = "Move";
    public static final String SENSOR_NAME__FLIP = "Flip";
    public static final String SENSOR_NAME__SHAKE = "Shake";
    public static final String SENSOR_NAME__PROXIMITY = "Proximity";
    public static final String SENSOR_NAME__CLAP = "Clap";
    public static final String SENSOR_NAME__SILENCE = "Silence";
    public static final String SENSOR_NAME__KEY = "Key";

    private AppAlarm appAlarm;

    private static final String LAST_RINGING_START_TIME = "last_ringing_start_time";
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

    private FlashlightBlinker flashlightBlinker;
    private boolean isFlashlight;
    private static final long[] FLASHLIGHT_PATTERN = {
            0, // No delay
            100, 900, // Second 1
            100, 100, 100, 700, // Second 2
            100, 100, 100, 100, 100, 500, // Second 3
            100, 100, // Repeat from here
            100, 100,
            400, 200
    }; // Round to whole second
    private static final int FLASHLIGHT_REPEAT = 13;

    private boolean isSoundMeter;
    private SoundMeter soundMeter;
    private Handler soundMeterHandler;
    private RecentList<SoundMeterRecord> soundMeterHistory = new RecentList<>(SoundMeterRecord.class);
    private ContinuousBuzzer tonePlayer;

    private TextView mutedTextView;
    private boolean isMuted;
    private Calendar muteStart;
    private int mutedSecondsLeft;
    public static final int MUTE_SECONDS = 10;

    private TextView snoozeTimeTextView;

    private Set<SensorEventDetector> sensorEventDetectors;
    private JSONObject sensorsHistoryJSON = new JSONObject();

    private boolean actionPerformed = false; // User performed and action (snooze or dismiss) in the activity. That includes auto-snooze and auto-actions.

    private LocalBroadcastManager bManager;
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
            MyLog.d("onReceive() action=" + action);

            if (action.equals(ACTION_HIDE_ACTIVITY)) {
                if (blink == null) { // The action (snooze, dismiss) was NOT started by this activity
                    shutdown();
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        MyLog.d("onCreate()");
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

        SlideButton dismissButton = findViewById(R.id.dismissButton);
        dismissButton.setOnClickListener(this::onDismiss);

        snoozeTimeTextView = findViewById(R.id.snoozeTimeTextView);

        JoyButton snoozeButton = findViewById(R.id.snoozeButton);
        snoozeButton.setOnJoyClickListener(new JoyButton.OnJoyClickListener() {
            @Override
            public void onDown(View v) {
                MyLog.v("onDown()");
                snoozeTimeTextView.setVisibility(View.VISIBLE);

                int minutes = calcSnoozeMinutes(0, 0, true);
                snoozeTimeTextView.setText(getResources().getQuantityString(R.plurals.snooze_time_text, minutes, minutes));
            }

            @Override
            public void onMove(View v, float dx, float dy, boolean click) {
                MyLog.v("onMove(dx=" + dx + ", dy=" + dy + ", click=" + click + ")");
                int minutes = calcSnoozeMinutes(dx, dy, click);
                snoozeTimeTextView.setText(getResources().getQuantityString(R.plurals.snooze_time_text, minutes, minutes));
            }

            @Override
            public void onUp(View v, float dx, float dy, boolean click) {
                MyLog.v("onUp(dx=" + dx + ", dy=" + dy + ", click=" + click + ")");
                snoozeTimeTextView.setVisibility(View.INVISIBLE);

                int minutes = calcSnoozeMinutes(dx, dy, click);
                doSnooze(minutes, false);
            }

            @Override
            public void onCancel(View v) {
                MyLog.v("onCancel()");
                snoozeTimeTextView.setVisibility(View.INVISIBLE);
            }
        });
        snoozeButton.setOnClickListener(this::onSnooze);

        startAll();
    }

    private int calcSnoozeMinutes(float dx, float dy, boolean click) {
        int minutes;

        if (click) {
            // Use default snooze time
            minutes = (int) SharedPreferencesHelper.load(SettingsActivity.PREF_SNOOZE_TIME, SettingsActivity.PREF_SNOOZE_TIME_DEFAULT);
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
        MyLog.v("onSaveInstanceState()");
        super.onSaveInstanceState(savedInstanceState);

        savedInstanceState.putString(PERSIST_ALARM_TYPE, appAlarm.getClass().getSimpleName());
        savedInstanceState.putString(PERSIST_ALARM_ID, appAlarm.getPersistenceId());
        savedInstanceState.putLong(LAST_RINGING_START_TIME, lastRingingStartTime.getTimeInMillis());
    }

    @Override
    public void onPause() {
        MyLog.v("onPause()");

        // Problem: When starting activity, the activity lifecycle calls are as follows: onCreate, onStart, onResume, onPause, onStop, onStart, onResume
        // Source: https://stackoverflow.com/questions/25369909/onpause-and-onstop-called-immediately-after-starting-activity

        super.onPause();

        focusDuringOnPause = hasWindowFocus();
    }

    @Override
    protected void onStop() {
        MyLog.v("onStop()");

        super.onStop();

        if (focusDuringOnPause) {
            // Normal scenario
            MyLog.d("onStop: Normal scenario");

            if (!actionPerformed) {
                doSnooze(false);
            }
        } else {
            // Activity was started when screen was off / screen was on with keyguard displayed

            MyLog.v("onStop: Exceptional scenario - Activity was started when screen was off / screen was on with keyguard displayed");
            // Do nothing
        }
    }

    @Override
    protected void onDestroy() {
        MyLog.v("onDestroy()");
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
        MyLog.d("doDismiss(autoDismiss=" + autoDismiss + ")");
        MyLog.i("Dismiss");

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
        MyLog.v("doSnooze(autoSnooze=" + autoSnooze + ")");

        int minutes = (int) SharedPreferencesHelper.load(SettingsActivity.PREF_SNOOZE_TIME, SettingsActivity.PREF_SNOOZE_TIME_DEFAULT);

        doSnooze(minutes, autoSnooze);
    }

    private void doSnooze(int minutes, boolean autoSnooze) {
        MyLog.v("doSnooze(minutes=" + minutes + ", autoSnooze=" + autoSnooze + ")");
        MyLog.i("Snooze for " + minutes + " minutes");

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
        MyLog.d("doMute()");

        if (muteAvailable()) {
            MyLog.i("Mute");
            startMute();
        } else {
            MyLog.d("Not muting because it had been not muted");
        }
    }

    private void initMute() {
        mutedTextView = findViewById(R.id.muted);

        muteStart = null;
    }

    private boolean muteAvailable() {
        MyLog.d("muteAvailable()");
        return muteStart == null;
    }

    private void startMute() {
        MyLog.d("startMute()");

        isMuted = true;
        GlobalManager globalManager = GlobalManager.getInstance();
        muteStart = globalManager.clock().now();
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
            MyLog.d("run()");

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
        MyLog.d("updateMute()");

        Resources res = getResources();
        String muteText = res.getString(R.string.muted, mutedSecondsLeft);

        mutedTextView.setText(muteText);
    }

    private void stopMute() {
        MyLog.d("stopMute()");

        if (isMuted) {
            MyLog.i("Unmute");
            isMuted = false;

            unmuteVibrate();
            unmuteSound();

            mutedTextView.setVisibility(View.INVISIBLE);

            handlerMute.removeCallbacks(runnableMute);
        }
    }

    private void startAll() {
        MyLog.d("startAll()");

        if (!isRinging) {
            MyLog.i("Start ringing");

            isRinging = true;

            startSound();
            startVibrate();
            startFlashlight();

            initMute();
            startSensors();
            startSoundMeter();

            startContent();
        }
    }

    /**
     * The wake lock that was acquired on system notification in {@link AlarmReceiver#onReceive(Context, Intent)} must be released.
     */
    private void stopAll() {
        MyLog.d("stopAll()");

        if (isRinging) {
            MyLog.i("Stop ringing");

            isRinging = false;

            stopContent();

            stopSoundMeter();
            stopSensors();
            stopMute();

            stopFlashlight();
            stopVibrate();
            stopSound();

            // allow device sleep
            WakeLocker.release();

            // Save sensors history

            // Sound meter
            Analytics analytics = new Analytics(this, Analytics.Event.End, Analytics.Channel.Activity, Analytics.ChannelName.Ring);
            analytics.set(Analytics.Param.General_key, Analytics.GENERAL_KEY__SOUND_METER_HISTORY);
            analytics.set(Analytics.Param.General_value, soundMeterHistory2JSONString());
            analytics.setConfigurationInfo();
            analytics.save();

            // Other sensors
            analytics = new Analytics(this, Analytics.Event.End, Analytics.Channel.Activity, Analytics.ChannelName.Ring);
            analytics.set(Analytics.Param.General_key, Analytics.GENERAL_KEY__SENSORS_HISTORY);
            analytics.set(Analytics.Param.General_value, sensorsHistoryJSON.toString());
            analytics.setConfigurationInfo();
            analytics.save();
        }
    }

    private void startContent() {
        MyLog.d("startContent()");

        handlerContent.start();
    }

    private final HandlerOnClockChange handlerContent = new HandlerOnClockChange(this::updateContent, Calendar.MINUTE);

    private void stopContent() {
        MyLog.d("stopContent()");

        handlerContent.stop();
    }

    private void updateContent() {
        MyLog.d("updateContent()");

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
        MyLog.v("checkAutoActions()");

        // Note: Do auto-dismiss before auto-snooze

        // Check auto dismiss
        boolean doAutoDismiss = doAutoDismiss(appAlarm);
        if (doAutoDismiss) {
            MyLog.d("Auto-dismiss");
            doDismiss(true);
            return true;
        }

        // Check auto snooze
        boolean doAutoSnooze = doAutoSnooze();
        if (doAutoSnooze) {
            MyLog.d("Auto-snooze");
            doSnooze(true);
            return true;
        }

        return false;
    }

    public static boolean doAutoDismiss(AppAlarm appAlarm) {
        MyLog.v("doAutoDismiss()");
        GlobalManager globalManager = GlobalManager.getInstance();
        Calendar now = globalManager.clock().now();

        // Check auto dismiss
        boolean autoDismiss = (boolean) SharedPreferencesHelper.load(SettingsActivity.PREF_AUTO_DISMISS, SettingsActivity.PREF_AUTO_DISMISS_DEFAULT);
        if (autoDismiss) {
            long autoDismissTime = (int) SharedPreferencesHelper.load(SettingsActivity.PREF_AUTO_DISMISS_TIME, SettingsActivity.PREF_AUTO_DISMISS_TIME_DEFAULT);

            Calendar alarmTime = appAlarm.getDateTime();
            // The following a little hack to account for the fact that when the code is scheduled at particular time, the code that saves the current time is executed few milliseconds later. This applies to both now and lastRingingStartTime bellow variables. We just trim the milliseconds (assumes that the execution happens within one second).
            long diffFromAlarmTime = (now.getTimeInMillis() / 1000 - alarmTime.getTimeInMillis() / 1000) / 60; // in minutes

            boolean doAutoDismiss = autoDismissTime <= diffFromAlarmTime;

            MyLog.v("Auto-dismiss check " + doAutoDismiss + ": auto-dismiss time (" + autoDismissTime + " min) <= time since the alarm time (" + diffFromAlarmTime + " min)");

            return doAutoDismiss;
        }
        return false;
    }

    private boolean doAutoSnooze() {
        MyLog.v("doAutoSnooze()");
        GlobalManager globalManager = GlobalManager.getInstance();
        Calendar now = globalManager.clock().now();

        boolean autoSnooze = (boolean) SharedPreferencesHelper.load(SettingsActivity.PREF_AUTO_SNOOZE, SettingsActivity.PREF_AUTO_SNOOZE_DEFAULT);
        if (autoSnooze) {
            long autoSnoozeTime = (int) SharedPreferencesHelper.load(SettingsActivity.PREF_AUTO_SNOOZE_TIME, SettingsActivity.PREF_AUTO_SNOOZE_TIME_DEFAULT);

            long diffFromLastRingingStartTime = (now.getTimeInMillis() / 1000 - lastRingingStartTime.getTimeInMillis() / 1000) / 60; // in minutes

            boolean doAutoSnooze = autoSnoozeTime <= diffFromLastRingingStartTime;

            MyLog.v("Auto-snooze check " + doAutoSnooze + ": auto-snooze time (" + autoSnoozeTime + " min) <= time since the start of this ringing (" + diffFromLastRingingStartTime + " min)");

            return doAutoSnooze;
        }
        return false;
    }

    private Uri getRingtoneUri() {
        Uri ringtoneUri;

        if (SharedPreferencesHelper.contains(SettingsActivity.PREF_RINGTONE)) {
            String ringtonePreference = (String) SharedPreferencesHelper.load(SettingsActivity.PREF_RINGTONE, SettingsActivity.PREF_RINGTONE_DEFAULT);
            ringtoneUri = ringtonePreference.equals(SettingsActivity.PREF_RINGTONE_DEFAULT) ? null : Uri.parse(ringtonePreference);
        } else {
            ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
        }

        return ringtoneUri;
    }

    private boolean playSound() {
        Uri ringtoneUri = getRingtoneUri();
        return ringtoneUri != null && !ringtoneUri.toString().isEmpty();
    }

    private void startSound() {
        MyLog.d("startSound()");

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
                MyLog.d("Unable to play ringtone as media", e);
            }

            if (soundMethod == 0) {
                try {
                    startSoundAsRingtone(ringtoneUri);
                    soundMethod = 2;
                } catch (Exception e) {
                    MyLog.d("Unable to play ringtone as ringtone", e);
                }
            }

            if (soundMethod == 0) MyLog.e("Unable to play ringtone");
        } else {
            MyLog.w("Sound is intentionally not playing (disabled in settings)");
        }
    }

    private void startSoundAsRingtone(Uri ringtoneUri) {
        MyLog.d("startSoundAsRingtone()");
        ringtone = RingtoneManager.getRingtone(this, ringtoneUri);
        ringtone.play();
    }

    private void stopSoundAsRingtone() {
        MyLog.d("stopSoundAsRingtone()");
        ringtone.stop();
    }

    private void pauseSoundAsRingtone() {
        MyLog.d("pauseSoundAsRingtone()");
        ringtone.stop();
    }

    private void resumeSoundAsRingtone() {
        MyLog.d("resumeSoundAsRingtone()");
        ringtone.play();
    }

    private void startSoundAsMedia(Uri ringtoneUri) throws IOException {
        MyLog.d("startSoundAsMedia()");

        mediaPlayer = new MediaPlayer();
        mediaPlayer.setAudioStreamType(ALARM_MANAGER_STREAM);
        mediaPlayer.setDataSource(this, ringtoneUri);
        mediaPlayer.prepare();
        mediaPlayer.setLooping(true);
        mediaPlayer.start();
    }

    private void stopSoundAsMedia() {
        MyLog.d("stopSoundAsMedia()");
        mediaPlayer.stop();
    }

    private void pauseSoundAsMedia() {
        MyLog.d("pauseSoundAsMedia()");
        mediaPlayer.pause();
    }

    private void resumeSoundAsMedia() {
        MyLog.d("resumeSoundAsMedia()");
        mediaPlayer.start();
    }

    private void stopSound() {
        MyLog.d("stopSound()");

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
        MyLog.d("muteSound()");

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
        MyLog.d("unmuteSound()");

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
        MyLog.d("startVolume()");

        int volumePreference = (int) SharedPreferencesHelper.load(SettingsActivity.PREF_VOLUME, SettingsActivity.PREF_VOLUME_DEFAULT);

        if (volumePreference == 0)
            MyLog.w("Volume is set to 0");

        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        // My observations:
        // Without this line, with the wired headphones connected, the alarm sounds from the device.
        // Without this line, with the Bluetooth headphones connected, the alarm sounds from the headphones.
        // With this line, with the wired headphones connected, the alarm sounds from the device.
        // With this line, with the Bluetooth headphones connected, the alarm sounds from the device.
        audioManager.setSpeakerphoneOn(true); // Always use loudspeaker

        previousVolume = audioManager.getStreamVolume(ALARM_MANAGER_STREAM);
        MyLog.v("previous volume= " + previousVolume);

        maxVolume = audioManager.getStreamMaxVolume(ALARM_MANAGER_STREAM);
        volume = SettingsActivity.getRealVolume(volumePreference, maxVolume);

        MyLog.v("preference volume = " + volumePreference);
        MyLog.v("max volume= " + maxVolume);
        MyLog.v("volume = " + volume);

        increasing = (boolean) SharedPreferencesHelper.load(SettingsActivity.PREF_VOLUME_INCREASING, SettingsActivity.PREF_VOLUME_INCREASING_DEFAULT);

        if (increasing) {
            increasingVolumePercentage = 0;
            runnableVolume.run();
        } else {
            audioManager.setStreamVolume(ALARM_MANAGER_STREAM, volume, 0);
        }
    }

    /**
     * @return Whether the increasing reached the final volume.
     */
    private boolean updateIncreasingVolume() {
        MyLog.v("updateIncreasingVolume");

        if (isMuted)
            return false;

        increasingVolumePercentage++;
        MyLog.v("   volume percentage = " + increasingVolumePercentage);
        float ratio = (float) increasingVolumePercentage / 100;
        int tempVolume = (int) Math.ceil(ratio * maxVolume);
        MyLog.v("   current volume = " + tempVolume);

        if (volume <= tempVolume) {
            MyLog.v("reached final volume");
            audioManager.setStreamVolume(ALARM_MANAGER_STREAM, volume, 0);
            return true;
        } else {
            audioManager.setStreamVolume(ALARM_MANAGER_STREAM, tempVolume, 0);
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
        MyLog.d("stopVolume()");

        if (increasing) {
            handlerVolume.removeCallbacks(runnableVolume);
        }

        audioManager.setStreamVolume(ALARM_MANAGER_STREAM, previousVolume, 0);
    }

    private void startVibrate() {
        MyLog.d("startVibrate()");

        boolean vibratePreference = (boolean) SharedPreferencesHelper.load(SettingsActivity.PREF_VIBRATE, SettingsActivity.PREF_VIBRATE_DEFAULT);
        if (vibratePreference) {
            startVibrateForce(this);
        }
    }

    private void startVibrateForce(Context context) {
        MyLog.v("startVibrateForce()");

        isVibrating = false;

        vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);

        if (vibrator.hasVibrator()) {
            isVibrating = true;

            vibrator.vibrate(VIBRATOR_PATTERN, 0);

            // To continue vibrating when screen goes off
            IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_OFF);
            context.registerReceiver(vibrateReceiver, filter);
        } else {
            MyLog.w("The device cannot vibrate");
        }
    }

    private final BroadcastReceiver vibrateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
                vibrator.vibrate(VIBRATOR_PATTERN, 0);
            }
        }
    };

    private void stopVibrate() {
        MyLog.d("stopVibrate()");
        stopVibrate(this);
    }

    private void stopVibrate(Context context) {
        MyLog.v("stopVibrate()");

        if (isVibrating) {
            vibrator.cancel();

            context.unregisterReceiver(vibrateReceiver);
        }
    }

    private void muteVibrate() {
        MyLog.d("muteVibrate()");

        if (isVibrating) {
            vibrator.cancel();

            unregisterReceiver(vibrateReceiver);
        }
    }

    private void unmuteVibrate() {
        MyLog.d("unmuteVibrate()");

        if (isVibrating) {
            vibrator.vibrate(VIBRATOR_PATTERN, 0);

            // To continue vibrating when screen goes off
            IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_OFF);
            registerReceiver(vibrateReceiver, filter);
        }
    }

    private void startFlashlight() {
        MyLog.d("startFlashlight()");

        boolean flashPreference = (boolean) SharedPreferencesHelper.load(SettingsActivity.PREF_FLASHLIGHT, SettingsActivity.PREF_FLASHLIGHT_DEFAULT);
        if (flashPreference) {
            startFlashlightForce();
        }
    }

    private void startFlashlightForce() {
        startFlashlightForce(this, findViewById(R.id.cameraPreview));
    }

    private void startFlashlightForce(Context context, SurfaceView surfaceView) {
        MyLog.v("startFlashlightForce()");

        isFlashlight = false;

        int permissionCheck = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA);
        if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
            flashlightBlinker = new FlashlightBlinker(context);

            if (flashlightBlinker.hasFlashlightBlinker()) {
                flashlightBlinker.blink(FLASHLIGHT_PATTERN, FLASHLIGHT_REPEAT, surfaceView);

                isFlashlight = true;
            } else {
                MyLog.w("The device doesn't have a flashlight blinker");
            }
        } else {
            MyLog.w("The CAMERA permission is not granted");
            // It doesn't make sense to ask for permission while ringing
        }
    }

    private void stopFlashlight() {
        MyLog.d("stopFlashlight()");

        if (isFlashlight) {
            flashlightBlinker.cancel();

            isFlashlight = false;
        }
    }

    private void startSoundMeter() {
        MyLog.d("startSoundMeter()");

        boolean enableSoundMeter = enableSoundMeter();
        if (enableSoundMeter) {
            int permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO);
            if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
                try {
                    MyLog.i("Sensor soundMeter is used");

                    soundMeter = new SoundMeter();
                    soundMeter.start();

                    soundMeterHandler = new Handler();
                    soundMeterHandler.postDelayed(this::updateSoundMeter, SOUND_METER_DELAY_MILLIS);

                    isSoundMeter = true;
                    return;
                } catch (RuntimeException | IOException e) {
                    MyLog.w("Cannot start sound meter", e);
                }
            } else {
                MyLog.w("The RECORD_AUDIO permission is not granted");
                // It doesn't make sense to ask for permission while ringing
            }
        }

        MyLog.d("Sensor soundMeter is not used");
    }

    private void stopSoundMeter() {
        MyLog.d("stopSoundMeter()");

        if (isSoundMeter) {
            soundMeterHandler.removeCallbacks(this::updateSoundMeter);

            try {
                soundMeter.stop();
            } catch (RuntimeException e) {
                MyLog.w("Cannot stop sound meter", e);
            }

            if (tonePlayer != null)
                stopSoundBuzzer();

            isSoundMeter = false;
        }
    }

    private void updateSoundMeter() {
        MyLog.v("updateSoundMeter()");

        soundMeterHandler.postDelayed(this::updateSoundMeter, SOUND_METER_DELAY_MILLIS);

        // Store measurement
        long currentTimeMillis = currentTimeMillis();

        double amplitude;
        try {
            amplitude = soundMeter.getMaxAmplitude();
        } catch (NullPointerException e) {
            MyLog.v("Unable to get amplitude. Exiting.");
            return;
        }
        if (amplitude == 0) {// Sometimes, first few measures have 0 amplitude (= -infinity dB). Skip these.
            MyLog.v("Amplitude is zero. Exiting.");
            return;
        }

        double dB = amplitudeToDecibel(amplitude);
//        MyLog.v("Amplitude=" + amplitude + " = " + dB + " dB");

        SoundMeterRecord soundMeterRecord = new SoundMeterRecord(currentTimeMillis, dB);
        soundMeterHistory.add(soundMeterRecord);

        // Delete old records from history - commented to enable saving all the measurements to analytics
//        long maxLength = Math.max(
//                SOUND_METER_SILENCE_DURATION / SOUND_METER_DELAY_MILLIS, // silence detection
//                SOUND_METER_CLAP_PERIODS // clap detection
//        );
//        while (maxLength < soundMeterHistory.size())
//            soundMeterHistory.removeFirst();

        // Check actions

        if (isReliabilityCheckEnabled())
            checkSilence();

        if (onClapActionEnabled())
            checkClapGesture();
    }

    /**
     * Detect silence, typically because the ringtone doesn't play (due to various reasons).
     *
     * @return True if silence detected.
     */
    private boolean checkSilence() {
        MyLog.v("checkSilence()");

        long currentTimeMillis = currentTimeMillis();

        boolean skipBecauseMuted = muteStart != null && currentTimeMillis - muteStart.getTimeInMillis() < MUTE_SECONDS * 1000 + SOUND_METER_SILENCE_DURATION;
        if (skipBecauseMuted) {
            MyLog.v("Skipping the silence check because the ringtone is or was recently muted");
            return false;
        }

        long detectSoundStartMS = lastRingingStartTime.getTimeInMillis(); // In milliseconds, start of applying this logics, from activity start adjusted for the initial silent period
        if (increasing) {
            int volumePreference = (int) SharedPreferencesHelper.load(SettingsActivity.PREF_VOLUME, SettingsActivity.PREF_VOLUME_DEFAULT);
            // volumePreference range is 0 .. SettingsActivity.PREF_VOLUME_MAX
            // skip (add appropriate number of seconds) the period during which the alarm volume is increasing
            detectSoundStartMS += volumePreference * 10 * 1000;
        }
        detectSoundStartMS += SOUND_METER_SILENCE_START_AFTER; // Always add few seconds

        Calendar detectSoundStart = Calendar.getInstance();
        detectSoundStart.setTimeInMillis(detectSoundStartMS);
        MyLog.v("detectSoundStartMS=" + detectSoundStartMS + " ms = " + detectSoundStart.getTime());

        if (detectSoundStartMS < currentTimeMillis - SOUND_METER_SILENCE_DURATION) {
            int count_above_limit = 0;
            int count_total = 0;

            for (SoundMeterRecord soundMeterRecord : soundMeterHistory) {
                if (detectSoundStartMS < soundMeterRecord.timestamp) {
                    if (currentTimeMillis - SOUND_METER_SILENCE_DURATION < soundMeterRecord.timestamp) {
                        if (SOUND_METER_SILENCE_LIMIT < soundMeterRecord.dB) {
                            MyLog.v("  " + soundMeterRecord.dB + "   is above limit");
                            count_above_limit++;
                        } else {
                            MyLog.v("  " + soundMeterRecord.dB);
                        }
                        count_total++;
                    }
                }
            }

            double ratio = count_above_limit / (float) count_total;
            boolean isSilence = ratio < SOUND_METER_SILENCE_PERCENTAGE_LIMIT;

            MyLog.d(String.format(Locale.US, "Silence: %s, ratio=%f < %f, %d/%d above limit of %d dB", isSilence ? "yes" : "no", ratio, SOUND_METER_SILENCE_PERCENTAGE_LIMIT, count_above_limit, count_total, SOUND_METER_SILENCE_LIMIT));

            if (isSilence) {
                doSilenceDetected();
                return true;
            }
        } else {
            MyLog.v("Skipping the silence check because we are too cloe to start");
        }

        return false;
    }

    private void doSilenceDetected() {
        MyLog.i("Silence detected");

        Analytics analytics = new Analytics(this, Analytics.Event.Gesture, Analytics.Channel.Activity, Analytics.ChannelName.Ring);
        analytics.set(Analytics.Param.Sensor_name, SENSOR_NAME__SILENCE);
        analytics.set(Analytics.Param.General_key, Analytics.GENERAL_KEY__SOUND_METER_HISTORY);
        analytics.set(Analytics.Param.General_value, soundMeterHistory2JSONString());
        analytics.setConfigurationInfo();
        analytics.save();

        // Show the message
//        TextView silenceDetectedView = findViewById(R.id.silenceDetected);
//        silenceDetectedView.setVisibility(View.VISIBLE);
//
//        // Do panic
//
//        // Let the standard ringtone keep ringing, but adjust it...
//        // Stop increasing
//        handlerVolume.removeCallbacks(runnableVolume);
//        // Set volume to maximum
//        if (audioManager != null)
//            audioManager.setStreamVolume(ALARM_MANAGER_STREAM, maxVolume, 0); // Set volume to max
//
//        if (!isFlashlight)
//            startFlashlightForce();
//
//        if (!isVibrating)
//            startVibrateForce(this);
//
//        startSoundBuzzer();
    }

    public void testSilenceDetectedStart(Context context, SurfaceView surfaceView) {
        MyLog.v("testSilenceDetectedStart()");

        startFlashlightForce(context, surfaceView);
        startVibrateForce(context);
        startSoundBuzzer();
    }

    public void testSilenceDetectedStop(Context context) {
        MyLog.v("testSilenceDetectedStop()");

        stopFlashlight();
        stopVibrate(context);
        stopSoundBuzzer();
    }

    private void startSoundBuzzer() {
        MyLog.v("startSoundBuzzer()");

        // Play tone
        // Source: https://stackoverflow.com/questions/2413426/playing-an-arbitrary-tone-with-android/3731075
        tonePlayer = new ContinuousBuzzer();
        tonePlayer.setPausePeriodSeconds(1);
        tonePlayer.setPauseTimeInMs(350);
        tonePlayer.setVolume(100); // volume values are from 0-100
        tonePlayer.setToneFreqInHz(800);
        tonePlayer.play();
    }

    private void stopSoundBuzzer() {
        MyLog.v("stopSoundBuzzer()");

        tonePlayer.stop();
    }

    /**
     * Detect the loud sound, typically clapping by hands.
     *
     * @return True if loud sound (clapping, whistling) detected.
     */
    private boolean checkClapGesture() {
        MyLog.v("checkClapGesture()");

        if (SOUND_METER_CLAP_PERIODS <= soundMeterHistory.size()) {

            // Get samples

            MyLog.v("All measurements");
            List<Double> values = new ArrayList<>();
            for (int i = 0; i < SOUND_METER_CLAP_PERIODS; i++) {
                double dB = soundMeterHistory.get(soundMeterHistory.size() - 1 - i).dB;
                MyLog.v("  " + dB);
                values.add(dB);
            }

            Collections.sort(values);

            // Remove SOUND_METER_CLAP_MIN_COUNT biggest values
            MyLog.v("Removing " + SOUND_METER_CLAP_MIN_COUNT + " biggest measurements");
            for (int i = values.size() - SOUND_METER_CLAP_MIN_COUNT; i < values.size(); i++) {
                MyLog.v("  " + values.get(i));
            }

            // Get statistic description

            double[] valuesForStatistics = new double[SOUND_METER_CLAP_PERIODS - SOUND_METER_CLAP_MIN_COUNT];

            MyLog.v("Remaining measurements (ordered)");
            for (int i = 0; i < valuesForStatistics.length; i++) {
                MyLog.v("  " + values.get(i));
                valuesForStatistics[i] = values.get(i);
            }

            double mean = Statistics.mean(valuesForStatistics);
            double stdDev = Statistics.stdDev(valuesForStatistics);

            // Count the intervals with volume above limit

            double limit = Math.max(mean + Math.max(3 * stdDev, SOUND_METER_CLAP_MIN_DIFFERENCE), SOUND_METER_CLAP_MIN_ABS);

            for (int i = 0; i < SOUND_METER_CLAP_PERIODS; i++) { // Restore values
                double dB = soundMeterHistory.get(soundMeterHistory.size() - 1 - i).dB;
                values.set(i, dB);
            }

            int count = 0;
            boolean isPreviousOutlier = false;
            for (double dB : values) {
                boolean isCurrentOutlier = limit < dB;
                if (isCurrentOutlier && !isPreviousOutlier)
                    count++;
                isPreviousOutlier = isCurrentOutlier;
            }

            boolean clapDetected = SOUND_METER_CLAP_MIN_COUNT <= count;

            MyLog.d(String.format("Clap: %s, %d/%d outliers, limit=%f, mean=%f, stdDev=%f", clapDetected ? "yes" : "no", count, SOUND_METER_CLAP_MIN_COUNT, limit, mean, stdDev));

            if (clapDetected) {
                doClapDetected();
                return true;
            }
        }

        return false;
    }

    private void doClapDetected() {
        MyLog.i("Clap (loud sound) detected");

        String action = (String) SharedPreferencesHelper.load(SettingsActivity.PREF_ACTION_ON_CLAP, SettingsActivity.PREF_ACTION_DEFAULT);
        actOnEvent(action, SENSOR_NAME__CLAP);
    }

    /**
     * Convert the amplitute to decibels. Empirically, this is equals to the reality; however, in practice the hardware used on Android phones has low quality and is not exact.
     *
     * @return Intensity of a sound, in decibels.
     */
    public double amplitudeToDecibel(double amplitude) {
        // About the amplitude interpretation and relation to decibels: https://stackoverflow.com/questions/10655703/what-does-androids-getmaxamplitude-function-for-the-mediarecorder-actually-gi
        return 20 * Math.log10(amplitude / 2700.0);
    }

    public static boolean enableSoundMeter() {
        return isReliabilityCheckEnabled() || onClapActionEnabled();
    }

    private static boolean isReliabilityCheckEnabled() {
        return (boolean) SharedPreferencesHelper.load(SettingsActivity.PREF_RELIABILITY_CHECK_ENABLED, SettingsActivity.PREF_RELIABILITY_CHECK_ENABLED_DEFAULT);
    }

    private static boolean onClapActionEnabled() {
        String onClapAction = (String) SharedPreferencesHelper.load(SettingsActivity.PREF_ACTION_ON_CLAP, SettingsActivity.PREF_ACTION_DEFAULT);
        return !onClapAction.equals(SettingsActivity.PREF_ACTION_NOTHING);
    }

    private String soundMeterHistory2JSONString() {
        JSONObject o = new JSONObject();

        for (SoundMeterRecord soundMeterRecord : soundMeterHistory) {
            String key = String.valueOf(soundMeterRecord.timestamp);
            double value = soundMeterRecord.dB;
            try {
                o.put(key, value);
            } catch (JSONException e) {
                MyLog.v("Cannot put the key " + key + " to the JSON object (with value " + value + ")", e);
            }
        }

        return o.toString();
    }

    @Override
    public boolean onKeyDown(int keycode, KeyEvent e) {
        MyLog.d("onKeyDown(keycode=" + keycode + ")");

        String buttonActionPreference = (String) SharedPreferencesHelper.load(SettingsActivity.PREF_ACTION_ON_BUTTON, SettingsActivity.PREF_ACTION_DEFAULT);

        if (SettingsActivity.PREF_ACTION_DEFAULT.equals(buttonActionPreference)) {
            if (keycode == KeyEvent.KEYCODE_BACK) {
                MyLog.d("Doing nothing on back key.");
                return true;
            } else {
                MyLog.d("Doing nothing. Let the operating system handles tke key.");
                return super.onKeyDown(keycode, e);
            }
        } else {
            MyLog.i("Act on key press detected");
            actOnEvent(buttonActionPreference, SENSOR_NAME__KEY);
            return true;
        }
    }

    private void startSensors() {
        MyLog.d("startSensors()");
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
        MyLog.d("stopSensors()");

        for (SensorEventDetector sensorEventDetector : sensorEventDetectors) {
            sensorEventDetector.stop();
        }
    }

    @Override
    public Context getContextI() {
        return this;
    }

    @Override
    public void actOnEvent(String action, String sensorName) {
        MyLog.v("actOnEvent(action=" + SettingsActivity.actionCodeToString(action) + ")");

        // Save analytics
        Analytics analytics = new Analytics(this, Analytics.Event.Gesture, Analytics.Channel.Activity, Analytics.ChannelName.Ring);
        analytics.set(Analytics.Param.Action, action);
        analytics.setConfigurationInfo();

        if (sensorName.equals(SENSOR_NAME__KEY)) {
            analytics.set(Analytics.Param.Sensor_name, SENSOR_NAME__KEY);
        } else if (sensorName.equals(SENSOR_NAME__CLAP)) {
            analytics.set(Analytics.Param.Sensor_name, SENSOR_NAME__CLAP);
            analytics.set(Analytics.Param.General_key, Analytics.GENERAL_KEY__SOUND_METER_HISTORY);
            analytics.set(Analytics.Param.General_value, soundMeterHistory2JSONString());
        } else if (sensorName.equals(SENSOR_NAME__PROXIMITY) || sensorName.equals(SENSOR_NAME__FLIP) || sensorName.equals(SENSOR_NAME__MOVE) || sensorName.equals(SENSOR_NAME__SHAKE)) {
            analytics.set(Analytics.Param.Sensor_name, sensorName);

            analytics.set(Analytics.Param.General_key, sensorName.equals(SENSOR_NAME__PROXIMITY)
                    ? Analytics.GENERAL_KEY__PROXIMITY_HISTORY
                    : Analytics.GENERAL_KEY__ACCELEROMETER_HISTORY
            );

            JSONObject sensorData;
            try {
                sensorData = sensorsHistoryJSON.getJSONObject(sensorName);
            } catch (JSONException e) {
                sensorData = new JSONObject();
            }
            analytics.set(Analytics.Param.General_value, sensorData.toString());
        } else {
            throw new IllegalArgumentException("Unexpected sensor " + sensorName);
        }

        analytics.save();

        // Do action

        switch (action) {
            case SettingsActivity.PREF_ACTION_DEFAULT:
                MyLog.d("Doing nothing");
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

    @Override
    public void addSensorRecordToHistory(String sensorName, SensorEvent event, boolean isFiring) {
        JSONObject sensorData;
        try {
            sensorData = sensorsHistoryJSON.getJSONObject(sensorName);
        } catch (JSONException e) {
            try {
                sensorData = new JSONObject();
                sensorData.put("sensor", event.sensor.toString());
                sensorsHistoryJSON.put(sensorName, sensorData);
            } catch (JSONException e2) {
                MyLog.w("Cannot save sensor data to history", e2);
                return;
            }
        }

        try {
            JSONObject o = new JSONObject();

            JSONArray valuesJSON = new JSONArray();
            for (double d : event.values)
                valuesJSON.put(d);
            o.put("values", valuesJSON);

            o.put("accuracy", event.accuracy);

            o.put("isFiring", isFiring);

            String key = String.valueOf(event.timestamp);
            sensorData.put(key, o);
        } catch (JSONException e) {
            MyLog.w("Cannot save sensor data to history", e);
        }
    }
}

class SoundMeterRecord {
    long timestamp; // in milliseconds
    double dB;

    SoundMeterRecord(long timestamp, double dB) {
        this.timestamp = timestamp;
        this.dB = dB;
    }
}