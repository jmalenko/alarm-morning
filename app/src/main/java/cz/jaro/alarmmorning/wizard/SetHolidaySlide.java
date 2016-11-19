package cz.jaro.alarmmorning.wizard;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;

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
        holidaySelector.setPath(SettingsActivity.PREF_HOLIDAY_NONE);
        holidaySelector.setListVisibility(View.GONE);

        // TODO Wizard - Preselect holiday calendar

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

        // Save holiday
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        SharedPreferences.Editor editor = preferences.edit();

        SetFeaturesSlide.savePreference(getContext(), editor, SettingsActivity.PREF_HOLIDAY, holidayCalendarPreferenceString);

        editor.commit();

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