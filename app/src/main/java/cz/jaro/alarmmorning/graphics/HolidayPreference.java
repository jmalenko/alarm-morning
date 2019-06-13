package cz.jaro.alarmmorning.graphics;

import android.content.Context;
import android.content.res.TypedArray;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.View;

import cz.jaro.alarmmorning.SettingsActivity;
import de.galgtonold.jollydayandroid.HolidayCalendar;

/**
 * The Preference representing a {@link HolidayCalendar}.
 * <p/>
 * Realized as a spinner dialog with none item.
 * <p/>
 * The HolidayCalendar path is stored (or {@link cz.jaro.alarmmorning.SettingsActivity#PREF_HOLIDAY_NONE} when no HolidayCalendar is selected). The
 * HolidayCalendar path are a dot-separated IDs of regions. The IDs correspond to to the IDs used in Jollyday library, Eg. "DE.BY.AG" stands for "Germany –
 * Bavaria – Munich".
 */
public class HolidayPreference extends DialogPreference {

    private static final String TAG = HolidayPreference.class.getSimpleName();

    private HolidaySelector mHolidaySelector;

    private String mSelectedValue;

    public HolidayPreference(final Context context, final AttributeSet attrs) {
        super(context, attrs);

        setPositiveButtonText(context.getString(android.R.string.ok));
        setNegativeButtonText(context.getString(android.R.string.cancel));
    }

    @Override
    protected void onSetInitialValue(final boolean restoreValue, final Object defaultValue) {
        final String strDefaultValue = defaultValue instanceof String ? (String) defaultValue : SettingsActivity.PREF_HOLIDAY_NONE;
        mSelectedValue = restoreValue ? this.getPersistedString(strDefaultValue) : strDefaultValue;
    }

    @Override
    protected View onCreateDialogView() {
        mHolidaySelector = new HolidaySelector(getContext());

        return mHolidaySelector;
    }

    @Override
    protected Object onGetDefaultValue(final TypedArray a, final int index) {
        return a.getString(index);
    }

    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);

        mHolidaySelector.setPath(mSelectedValue);
    }

    @Override
    protected void onDialogClosed(final boolean positiveResult) {
        super.onDialogClosed(positiveResult);

        if (positiveResult) {
            String selectedValue = mHolidaySelector.getPath();

            if (callChangeListener(selectedValue)) {
                mSelectedValue = selectedValue;

                persistString(mSelectedValue);
            }
        }
    }

}