package cz.jaro.alarmmorning.checkalarmtime;

import android.app.TimePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.format.DateFormat;
import android.util.Log;
import android.widget.TimePicker;
import android.widget.Toast;

import java.util.Calendar;

import cz.jaro.alarmmorning.Analytics;
import cz.jaro.alarmmorning.CalendarFragment;
import cz.jaro.alarmmorning.GlobalManager;
import cz.jaro.alarmmorning.model.AlarmDataSource;
import cz.jaro.alarmmorning.model.Day;

import static cz.jaro.alarmmorning.Analytics.CHECK_ALARM_TIME_METHOD__DIALOG;
import static cz.jaro.alarmmorning.RingActivity.ALARM_TIME;
import static cz.jaro.alarmmorning.checkalarmtime.CheckAlarmTimeNotificationReceiver.EXTRA_NEW_ALARM_TIME;
import static cz.jaro.alarmmorning.model.Day.VALUE_UNSET;

/**
 * This activity shows a time picker dialog above the current activity. The picked time is stored as alarm time.
 * <p>
 * The "default time" parameter should be pssed in the {@link CheckAlarmTimeNotificationReceiver#EXTRA_NEW_ALARM_TIME} extra.
 */
public class SetTimeActivity extends AppCompatActivity implements TimePickerDialog.OnTimeSetListener {

    private static final String TAG = SetTimeActivity.class.getSimpleName();

    Calendar newAlarmTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.v(TAG, "onCreate()");
        super.onCreate(savedInstanceState);

        // Read parameters from extra
        long newAlarmTimeLong = getIntent().getLongExtra(EXTRA_NEW_ALARM_TIME, VALUE_UNSET);
        if (newAlarmTimeLong != VALUE_UNSET) {
            newAlarmTime = Calendar.getInstance();
            newAlarmTime.setTimeInMillis(newAlarmTimeLong);
        }

        if (savedInstanceState != null) {
            newAlarmTime = (Calendar) savedInstanceState.getSerializable(ALARM_TIME);
        }

        if (newAlarmTime == null) {
            throw new IllegalStateException("New alarm time is not set");
        }
    }

    @Override
    protected void onResume() {
        Log.v(TAG, "onResume()");
        super.onResume();

        int hourOfDay = newAlarmTime.get(Calendar.HOUR_OF_DAY);
        int minute = newAlarmTime.get(Calendar.MINUTE);

        TimePickerDialog timePickerDialog = new TimePickerDialog(this, this, hourOfDay, minute, DateFormat.is24HourFormat(this));
        timePickerDialog.setOnDismissListener(new DialogInterface.OnDismissListener() { // Not that the Cancel button is handled via OnDismissListener
            public void onDismiss(DialogInterface dialog) {
                finish();
            }
        });
        timePickerDialog.show();
    }

    @Override
    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
        if (view.isShown()) {
            Log.v(TAG, "onTimeSet()");
            Calendar saveAlarmTime = (Calendar) newAlarmTime.clone();
            saveAlarmTime.set(Calendar.HOUR_OF_DAY, hourOfDay);
            saveAlarmTime.set(Calendar.MINUTE, minute);

            Analytics analytics = new Analytics().set(Analytics.Param.Check_alarm_time_method, CHECK_ALARM_TIME_METHOD__DIALOG);
            analytics.setChannel(Analytics.Channel.Notification);
            analytics.setChannelName(Analytics.ChannelName.Check_alarm_time);

            save(this, saveAlarmTime, analytics);

            finish();
        }
    }

    public static void save(Context context, Calendar saveAlarmTime, Analytics analytics) {
        save(context, saveAlarmTime, analytics, true);
    }

    public static void save(Context context, Calendar saveAlarmTime, Analytics analytics, boolean showToast) {
        AlarmDataSource dataSource = new AlarmDataSource(context);
        dataSource.open();

        // Load
        Day day = dataSource.loadDayDeep(saveAlarmTime);

        // Update
        day.setState(Day.STATE_ENABLED);
        day.setHour(saveAlarmTime.get(Calendar.HOUR_OF_DAY));
        day.setMinute(saveAlarmTime.get(Calendar.MINUTE));

        // Save
        GlobalManager globalManager = new GlobalManager(context);
        globalManager.saveAlarmTime(day, dataSource, analytics);

        dataSource.close();

        // Show toast
        if (showToast) {
            String toastText = CalendarFragment.formatToastText(context.getResources(), globalManager.clock(), day);
            Toast.makeText(context, toastText, Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        Log.v(TAG, "onSaveInstanceState()");
        super.onSaveInstanceState(savedInstanceState);

        savedInstanceState.putSerializable(ALARM_TIME, newAlarmTime);
    }

}
