package cz.jaro.alarmmorning.wizard;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.widget.CheckBox;

import cz.jaro.alarmmorning.Analytics;
import cz.jaro.alarmmorning.GlobalManager;
import cz.jaro.alarmmorning.R;
import cz.jaro.alarmmorning.SettingsActivity;

/**
 * When run repeatedly, behaves as run for the first time.
 */
public class SetActionsSlide extends BaseFragment {

    private static final String TAG = GlobalManager.createLogTag(SetActionsSlide.class);

    @Override
    protected int getContentLayoutId() {
        return R.layout.fragment_wizard_actions;
    }

    @Override
    public void onSlideDeselected() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        SharedPreferences.Editor editor = preferences.edit();

        CheckBox onMoveCheckBox = getView().findViewById(R.id.onMoveCheckBox);
        if (onMoveCheckBox.isChecked()) {
            saveActionPreference(editor, SettingsActivity.PREF_ACTION_ON_MOVE, SettingsActivity.PREF_ACTION_MUTE);
        }

        CheckBox onFlipCheckBox = getView().findViewById(R.id.onFlipCheckBox);
        if (onFlipCheckBox.isChecked()) {
            saveActionPreference(editor, SettingsActivity.PREF_ACTION_ON_FLIP, SettingsActivity.PREF_ACTION_SNOOZE);
        }

        CheckBox onShakeCheckBox = getView().findViewById(R.id.onShakeCheckBox);
        if (onShakeCheckBox.isChecked()) {
            saveActionPreference(editor, SettingsActivity.PREF_ACTION_ON_SHAKE, SettingsActivity.PREF_ACTION_DISMISS);
        }

        editor.apply();
    }

    private void saveActionPreference(SharedPreferences.Editor editor, String pref, String value) {
        Analytics analytics = new Analytics(getContext(), Analytics.Event.Change_setting, Analytics.Channel.Activity, Analytics.ChannelName.Wizard);
        analytics.set(Analytics.Param.Preference_key, pref);
        analytics.set(Analytics.Param.Preference_value, SettingsActivity.actionCodeToString(value));
        analytics.save();

        editor.putString(pref, value);
    }

    @Override
    protected String getTitle() {
        return getString(R.string.wizard_set_actions_title);
    }

    @Override
    protected String getDescriptionBottom() {
        return getString(R.string.wizard_set_actions_description);
    }

    @Override
    public int getDefaultBackgroundColor() {
        return getResources().getColor(R.color.Blue_Grey_700);
    }
}