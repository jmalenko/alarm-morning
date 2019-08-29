package cz.jaro.alarmmorning.wizard;

import android.content.Context;
import android.widget.CheckBox;

import cz.jaro.alarmmorning.Analytics;
import cz.jaro.alarmmorning.GlobalManager;
import cz.jaro.alarmmorning.Localization;
import cz.jaro.alarmmorning.R;
import cz.jaro.alarmmorning.SettingsActivity;
import cz.jaro.alarmmorning.SharedPreferencesHelper;
import cz.jaro.alarmmorning.graphics.TimePreference;

/**
 * When run repeatedly, behaves as run for the first time.
 */
public class SetFeaturesSlide extends BaseFragment {

    private static final String TAG = GlobalManager.createLogTag(SetFeaturesSlide.class);

    @Override
    protected int getContentLayoutId() {
        return R.layout.fragment_wizard_features;
    }

    @Override
    public void onSlideDeselected() {

        CheckBox onCheckAlarmTime = getView().findViewById(R.id.onCheckAlarmTime);
        SharedPreferencesHelper.save(SettingsActivity.PREF_CHECK_ALARM_TIME, onCheckAlarmTime.isChecked());
        if (onCheckAlarmTime.isChecked()) {
            savePreference(getContext(), SettingsActivity.PREF_CHECK_ALARM_TIME_AT, SettingsActivity.PREF_CHECK_ALARM_TIME_AT_DEFAULT);
            savePreference(getContext(), SettingsActivity.PREF_CHECK_ALARM_TIME_GAP, SettingsActivity.PREF_CHECK_ALARM_TIME_GAP_DEFAULT);
        }

        CheckBox onPlayNighttimeBell = getView().findViewById(R.id.onPlayNighttimeBell);
        SharedPreferencesHelper.save(SettingsActivity.PREF_NIGHTTIME_BELL, onPlayNighttimeBell.isChecked());
        if (onPlayNighttimeBell.isChecked()) {
            savePreference(getContext(), SettingsActivity.PREF_NIGHTTIME_BELL_AT, SettingsActivity.PREF_NIGHTTIME_BELL_AT_DEFAULT);
        }
    }

    protected static void savePreference(Context context, String pref, String value) {
        analytics(context, pref, value);

        SharedPreferencesHelper.save(pref, value);
    }

    protected static void savePreference(Context context, String pref, int value) {
        analytics(context, pref, value);

        SharedPreferencesHelper.save(pref, value);
    }

    protected static void analytics(Context context, String pref, Object value) {
        Analytics analytics = new Analytics(context, Analytics.Event.Change_setting, Analytics.Channel.Activity, Analytics.ChannelName.Wizard);
        analytics.set(Analytics.Param.Preference_key, pref);
        analytics.set(Analytics.Param.Preference_value, String.valueOf(value));
        analytics.save();
    }

    @Override
    protected String getTitle() {
        return getString(R.string.wizard_set_features_title);
    }

    @Override
    protected String getDescriptionTop() {
        int hour = TimePreference.getHour(SettingsActivity.PREF_CHECK_ALARM_TIME_AT_DEFAULT);
        int minute = TimePreference.getMinute(SettingsActivity.PREF_CHECK_ALARM_TIME_AT_DEFAULT);
        String timeText = Localization.timeToString(hour, minute, getContext());
        return getResources().getString(R.string.wizard_set_features_description, timeText);
    }

    @Override
    public int getDefaultBackgroundColor() {
        return getResources().getColor(R.color.Blue_Grey_750);
    }
}