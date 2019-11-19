package cz.jaro.alarmmorning.wizard;

import cz.jaro.alarmmorning.R;

public class SetDoneSlide extends BaseFragment {

    @Override
    protected String getTitle() {
        return getString(R.string.wizard_done_title);
    }

    @Override
    protected String getDescriptionBottom() {
        return getString(R.string.wizard_done_description);
    }

    @Override
    public int getDefaultBackgroundColor() {
        return getResources().getColor(R.color.Blue_Grey_900);
    }
}