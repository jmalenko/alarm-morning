package cz.jaro.alarmmorning.wizard;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.widget.CheckBox;

import cz.jaro.alarmmorning.R;
import cz.jaro.alarmmorning.SettingsActivity;

public class SetActionsSlide extends BaseFragment {

    private static final String TAG = SetActionsSlide.class.getSimpleName();

    // TODO When run from Settings - preset from current settings

    @Override
    protected int getContentLayoutId() {
        return R.layout.fragment_wizard_actions;
    }

    @Override
    public void onSlideDeselected() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        SharedPreferences.Editor editor = preferences.edit();

        CheckBox onMoveCheckBox = (CheckBox) getView().findViewById(R.id.onMoveCheckBox);
        if (onMoveCheckBox.isChecked()) {
            editor.putString(SettingsActivity.PREF_ACTION_ON_MOVE, SettingsActivity.PREF_ACTION_MUTE);
        }

        CheckBox onFlipCheckBox = (CheckBox) getView().findViewById(R.id.onFlipCheckBox);
        if (onFlipCheckBox.isChecked()) {
            editor.putString(SettingsActivity.PREF_ACTION_ON_FLIP, SettingsActivity.PREF_ACTION_SNOOZE);
        }

        CheckBox onShakeCheckBox = (CheckBox) getView().findViewById(R.id.onShakeCheckBox);
        if (onShakeCheckBox.isChecked()) {
            editor.putString(SettingsActivity.PREF_ACTION_ON_SHAKE, SettingsActivity.PREF_ACTION_DISMISS);
        }

        editor.commit();
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