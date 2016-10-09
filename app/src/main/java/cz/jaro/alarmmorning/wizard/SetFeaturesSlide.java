package cz.jaro.alarmmorning.wizard;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.widget.CheckBox;

import cz.jaro.alarmmorning.R;
import cz.jaro.alarmmorning.SettingsActivity;

public class SetFeaturesSlide extends BaseFragment {

    private static final String TAG = SetFeaturesSlide.class.getSimpleName();

    // TODO When run from Settings - preset from current settings

    @Override
    protected int getContentLayoutId() {
        return R.layout.fragment_wizard_features;
    }

    @Override
    public void onSlideDeselected() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        SharedPreferences.Editor editor = preferences.edit();

        CheckBox onCheckAlarmTime = (CheckBox) getView().findViewById(R.id.onCheckAlarmTime);
        editor.putBoolean(SettingsActivity.PREF_CHECK_ALARM_TIME, onCheckAlarmTime.isChecked());
        if (onCheckAlarmTime.isChecked()) {
            editor.putString(SettingsActivity.PREF_CHECK_ALARM_TIME_AT, SettingsActivity.PREF_CHECK_ALARM_TIME_AT_DEFAULT);
            editor.putInt(SettingsActivity.PREF_CHECK_ALARM_TIME_GAP, SettingsActivity.PREF_CHECK_ALARM_TIME_GAP_DEFAULT);
        }

        CheckBox onPlayNighttimeBell = (CheckBox) getView().findViewById(R.id.onPlayNighttimeBell);
        editor.putBoolean(SettingsActivity.PREF_NIGHTTIME_BELL, onPlayNighttimeBell.isChecked());
        if (onPlayNighttimeBell.isChecked()) {
            editor.putString(SettingsActivity.PREF_NIGHTTIME_BELL_AT, SettingsActivity.PREF_NIGHTTIME_BELL_AT_DEFAULT);
        }

        editor.commit();
    }

    @Override
    protected String getTitle() {
        return getString(R.string.wizard_set_features_title);
    }

    @Override
    protected String getDescriptionTop() {
        return getString(R.string.wizard_set_features_description);
    }

    @Override
    public int getDefaultBackgroundColor() {
        return getResources().getColor(R.color.Blue_Grey_700);
    }
}