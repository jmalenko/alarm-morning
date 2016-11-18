package cz.jaro.alarmmorning.graphics;

import android.content.Context;
import android.content.res.TypedArray;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.List;

import cz.jaro.alarmmorning.R;
import cz.jaro.alarmmorning.SettingsActivity;
import cz.jaro.alarmmorning.holiday.HolidayAdapter;
import cz.jaro.alarmmorning.holiday.HolidayHelper;
import de.jollyday.Holiday;

/**
 * The Preference representing a HolidayCalendar.
 * <p/>
 * Realized as a spinner dialog with none item.
 * <p/>
 * The HolidayCalendar ID is stored (or {@link cz.jaro.alarmmorning.SettingsActivity#PREF_HOLIDAY_NONE} when no HolidayCalendar is selected).
 */
public class HolidayPreference extends DialogPreference implements AdapterView.OnItemSelectedListener {

    private Spinner holidaySpinner;
    private HolidayAdapter holidayAdapter;

    private LinearLayout listOfHolidays;
    private TextView listOfHolidaysDetails;

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

        holidaySpinner.setOnItemSelectedListener(this);


        // TODO Spinner for subcountry holidayManager.getCalendarHierarchy()

        listOfHolidays = (LinearLayout) view.findViewById(R.id.listOfHolidays);
        listOfHolidaysDetails = (TextView) view.findViewById(R.id.listOfHolidaysDetails);

        // Update view
        int position = holidayAdapter.getPositionForId(mSelectedValue);
        holidaySpinner.setSelection(position);
        updateListOfHolidays(position);
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

    private void updateListOfHolidays(int position) {
        String holidayCalendarId = holidayAdapter.positionToPreferenceString(position);

        HolidayHelper holidayHelper = HolidayHelper.getInstance();
        if (holidayHelper.useHoliday(holidayCalendarId)) {
            List<Holiday> holidays = holidayHelper.listHolidays(holidayCalendarId);

            StringBuffer str = new StringBuffer();

            for (Holiday h : holidays) {
                if (str.length() > 0)
                    str.append('\n');
                str.append(h.toString());
            }

            listOfHolidaysDetails.setText(str);
            listOfHolidays.setVisibility(View.VISIBLE);
        } else {
            listOfHolidays.setVisibility(View.GONE);
        }
    }


    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        updateListOfHolidays(position);
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        // Nothing
    }
}