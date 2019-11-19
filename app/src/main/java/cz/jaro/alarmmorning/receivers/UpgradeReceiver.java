package cz.jaro.alarmmorning.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.preference.PreferenceManager;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.Calendar;
import java.util.HashSet;
import java.util.Set;

import cz.jaro.alarmmorning.Analytics;
import cz.jaro.alarmmorning.GlobalManager;
import cz.jaro.alarmmorning.JSONSharedPreferences;
import cz.jaro.alarmmorning.MyLog;
import cz.jaro.alarmmorning.R;
import cz.jaro.alarmmorning.SharedPreferencesHelper;
import cz.jaro.alarmmorning.WakeLocker;
import cz.jaro.alarmmorning.checkalarmtime.CheckAlarmTime;
import cz.jaro.alarmmorning.model.Day;
import cz.jaro.alarmmorning.nighttimebell.CustomAlarmTone;
import cz.jaro.alarmmorning.nighttimebell.NighttimeBell;

import static cz.jaro.alarmmorning.GlobalManager.HORIZON_DAYS;
import static cz.jaro.alarmmorning.calendar.CalendarUtils.addDay;

/**
 * This receiver is called by the operating system on the app upgrade.
 */
public class UpgradeReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        WakeLocker.acquire(context);

        String action = intent.getAction();
        MyLog.v("onReceive(action=" + action + ")");

        if (action.equals(Intent.ACTION_MY_PACKAGE_REPLACED)) {
            MyLog.i("Starting after upgrade");

            new Analytics(context, Analytics.Event.Start, Analytics.Channel.External, Analytics.ChannelName.Upgrade).setConfigurationInfo().save();

            // Update default values of preferences
            PreferenceManager.setDefaultValues(context, R.xml.preferences, false);

            MyLog.i("Updating preferences");
            updateData(context);

            MyLog.i("Setting alarm on update");
            GlobalManager globalManager = GlobalManager.getInstance();
            globalManager.firstSetAlarm();

            MyLog.i("Starting CheckAlarmTime on update");
            CheckAlarmTime checkAlarmTime = CheckAlarmTime.getInstance(context);
            checkAlarmTime.checkAndRegister();

            MyLog.i("Starting NighttimeBell on update");
            NighttimeBell nighttimeBell = NighttimeBell.getInstance(context);
            nighttimeBell.checkAndRegister();

            MyLog.i("Installing files");
            CustomAlarmTone customAlarmTone = new CustomAlarmTone(context);
            customAlarmTone.install();
        }

        WakeLocker.release();
    }

    // Update state - persisted data, preferences, ...
    // ===============================================

    private void updateData(Context context) {
        try {
            PackageInfo pInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            String versionName = pInfo.versionName;
            int versionCode = pInfo.versionCode;

            switch (versionCode) {
                case 11: // versionName "0.91"
                    updateTo11();
                    break;
            }
        } catch (PackageManager.NameNotFoundException e) {
            MyLog.e("Cannot update preferences", e);
        }
    }

    // Update To 11
    // ------------

    private void updateTo11() {
        // There is a change of format of dismissed alarms (from alarm times to type+id).

        // Get old preference
        Set<Long> dismissedAlarms = getDismissedAlarms();

        // Remove old preference
        SharedPreferencesHelper.remove(PERSIST_DISMISSED_1);

        // Migrate the old preference to new preference
        GlobalManager globalManager = GlobalManager.getInstance();
        Calendar date = globalManager.clock().now();
        for (int daysInAdvance = 0; daysInAdvance < HORIZON_DAYS; daysInAdvance++, addDay(date)) {
            Day day = globalManager.loadDay(date);

            if (dismissedAlarms.contains(day.getDateTime().getTimeInMillis())) {
                // Intentionally, we are NOT calling: globalManager.setState(STATE_DISMISSED, day)
                globalManager.addDismissedAlarm(day);
            }
        }
    }

    private static final String PERSIST_DISMISSED_1 = "persist_dismissed";

    private Set<Long> getDismissedAlarms() {
        MyLog.v("getDismissedAlarm()");

        try {
            JSONArray dismissedAlarmsJSON = JSONSharedPreferences.loadJSONArray(PERSIST_DISMISSED_1);
            return jsonToSet(dismissedAlarmsJSON);
        } catch (JSONException e) {
            MyLog.w("Error getting dismissed alarms", e);
            return new HashSet<>();
        }
    }

    private Set<Long> jsonToSet(JSONArray jsonArray) throws JSONException {
        Set<Long> set = new HashSet<>(jsonArray.length());
        for (int i = 0; i < jsonArray.length(); i++) {
            set.add(jsonArray.getLong(i));
        }
        return set;
    }
}