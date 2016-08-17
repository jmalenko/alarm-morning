package cz.jaro.alarmmorning.wizard;

import cz.jaro.alarmmorning.R;

public class SetWelcomeSlide extends BaseFragment {

    private static final String TAG = SetWelcomeSlide.class.getSimpleName();

    @Override
    protected String getTitle() {
        return getString(R.string.wizard_welcome_title);
    }

    @Override
    protected String getDescriptionBottom() {
        return getString(R.string.wizard_welcome_description);
    }

    @Override
    public int getDefaultBackgroundColor() {
        return getResources().getColor(R.color.Blue_Grey_500);
    }
}