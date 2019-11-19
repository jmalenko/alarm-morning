package cz.jaro.alarmmorning.wizard;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.Nullable;

import cz.jaro.alarmmorning.R;

public class SetWelcomeSlide extends BaseFragment {

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);

        // Set color of bottom description
        if (Wizard.loadWizardFinished()) {
            TextView description2View = view.findViewById(R.id.description2);
            description2View.setTextColor(getResources().getColor(R.color.Deep_Orange_200));
        }

        return view;
    }

    @Override
    protected String getTitle() {
        return getString(R.string.wizard_welcome_title);
    }

    @Override
    protected String getDescriptionTop() {
        return getString(R.string.wizard_welcome_description);
    }

    @Override
    protected String getDescriptionBottom() {
        if (Wizard.loadWizardFinished()) {
            return getString(R.string.wizard_welcome_overwrite_warning);
        } else {
            return null;
        }
    }

    @Override
    public int getDefaultBackgroundColor() {
        return getResources().getColor(R.color.Blue_Grey_550);
    }
}