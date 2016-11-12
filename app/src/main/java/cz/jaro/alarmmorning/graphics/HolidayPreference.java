package cz.jaro.alarmmorning.graphics;

import android.content.Context;
import android.content.res.TypedArray;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Spinner;

import cz.jaro.alarmmorning.R;
import cz.jaro.alarmmorning.SettingsActivity;
import cz.jaro.alarmmorning.holiday.HolidayAdapter;

/**
 * The Preference representing a HolidayCalendar.
 * <p/>
 * Realized as a spinner dialog with none item.
 * <p/>
 * The HolidayCalendar ID is stored (or {@link cz.jaro.alarmmorning.SettingsActivity#PREF_HOLIDAY_NONE} when no HolidayCalendar is selected).
 */
public class HolidayPreference extends DialogPreference {

    private Spinner holidaySpinner;
    HolidayAdapter holidayAdapter;

    private String mSelectedValue;

    public HolidayPreference(final Context context, final AttributeSet attrs) {
        super(context, attrs);

        setDialogLayoutResource(R.layout.fragment_wizard_holiday_region);
    }

    @Override
    protected void onSetInitialValue(final boolean restoreValue, final Object defaultValue) {
        final String strDefaultValue = defaultValue instanceof String ? (String) defaultValue : SettingsActivity.PREF_HOLIDAY_NONE;
        mSelectedValue = restoreValue ? this.getPersistedString(strDefaultValue) : strDefaultValue;
    }

    @Override
    protected Object onGetDefaultValue(final TypedArray a, final int index) {
        return a.getString(index);
    }

    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);

        holidaySpinner = (Spinner) view.findViewById(R.id.countrySpinner);

        holidayAdapter = new HolidayAdapter(getContext(), R.layout.simple_spinner_item);
        holidayAdapter.setDropDownViewResource(R.layout.simple_spinner_dropdown_item);
        holidaySpinner.setAdapter(holidayAdapter);

        holidaySpinner.setSelection(holidayAdapter.getPositionForId(mSelectedValue));

        // TODO Spinner for subcountry

        // TODO Show list of holidays
    }

    @Override
    protected void onDialogClosed(final boolean positiveResult) {
        super.onDialogClosed(positiveResult);

        if (positiveResult) {
            int position = holidaySpinner.getSelectedItemPosition();
            String selectedValue = holidayAdapter.positionToPreferenceString(position);

            if (callChangeListener(selectedValue)) {
                mSelectedValue = selectedValue;

                persistString(mSelectedValue);
            }
        }
    }

}