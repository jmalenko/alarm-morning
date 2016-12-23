package cz.jaro.alarmmorning.wizard;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;

import cz.jaro.alarmmorning.GlobalManager;
import cz.jaro.alarmmorning.R;
import cz.jaro.alarmmorning.SettingsActivity;
import cz.jaro.alarmmorning.graphics.HolidaySelector;
import cz.jaro.alarmmorning.holiday.HolidayHelper;
import de.jollyday.Holiday;

public class SetHolidaySlide extends BaseFragment {

    private static final String TAG = SetHolidaySlide.class.getSimpleName();

    private HolidaySelector holidaySelector;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);

        holidaySelector = (HolidaySelector) view.findViewById(R.id.holidaySelector);
        holidaySelector.setListVisibility(View.GONE);

        // Preset
        String holidayPreference;
        if (Wizard.loadWizardFinished(getContext())) {
            GlobalManager globalManager = GlobalManager.getInstance();
            holidayPreference = globalManager.loadHoliday();
        } else {
            holidayPreference = SettingsActivity.PREF_HOLIDAY_NONE;
        }
        holidaySelector.setPath(holidayPreference);

        return view;
    }

    @Override
    protected int getContentLayoutId() {
        return R.layout.fragment_wizard_holiday;
    }

    @Override
    public void onSlideDeselected() {
        Log.v(TAG, "onSlideDeselected()");

        String holidayCalendarPreferenceString = holidaySelector.getPath();

        // Analytics
        SetFeaturesSlide.analytics(getContext(), SettingsActivity.PREF_HOLIDAY, holidayCalendarPreferenceString);

        // Save holiday
        GlobalManager globalManager = GlobalManager.getInstance();
        globalManager.saveHoliday(holidayCalendarPreferenceString);

        // Debug log
        HolidayHelper holidayHelper = HolidayHelper.getInstance();
        if (holidayHelper.useHoliday()) {
            List<Holiday> holidays = holidayHelper.listHolidays();

            Log.d(TAG, "Holidays in " + holidayCalendarPreferenceString + " in next year");
            for (Holiday h : holidays) {
                Log.v(TAG, "   " + h.toString());
            }
        }
    }

    @Override
    protected String getTitle() {
        return getString(R.string.wizard_set_holidays_title);
    }

    @Override
    protected String getDescriptionTop() {
        return getString(R.string.wizard_set_holidays_description);
    }

    @Override
    public int getDefaultBackgroundColor() {
        return getResources().getColor(R.color.Blue_Grey_650);
    }

}