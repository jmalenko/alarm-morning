package cz.jaro.alarmmorning.graphics;

import android.content.Context;
import android.content.res.TypedArray;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.NumberPicker;

import cz.jaro.alarmmorning.R;

/**
 * The Preference representing a time difference.
 * <p>
 * The user uses two number pickers: one for hours and send for minutes.
 * <p>
 * The value is stored in minutes. Default value is 0.
 */
public class RelativeTimePreference extends DialogPreference {
    private static final int MAX_HOUR = 24;

    private int mSelectedValue;
    private final int mMaxHour;
    private NumberPicker mHourPicker;
    private NumberPicker mMinutePicker;

    public RelativeTimePreference(final Context context, final AttributeSet attrs) {
        super(context, attrs);

        final TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.RelativeTimePreference);

        mMaxHour = a.getInt(R.styleable.RelativeTimePreference_maxHour, RelativeTimePreference.MAX_HOUR);

        a.recycle();

        setDialogLayoutResource(R.layout.relative_time_picker);
    }

    @Override
    protected void onSetInitialValue(final boolean restoreValue, final Object defaultValue) {
        final int intDefaultValue = defaultValue instanceof Integer ? (int) defaultValue : 0;
        mSelectedValue = restoreValue ? this.getPersistedInt(intDefaultValue) : intDefaultValue;
    }

    @Override
    protected Object onGetDefaultValue(final TypedArray a, final int index) {
        return a.getInteger(index, 0);
    }

    @Override
    protected void onBindDialogView(View view) {
        mHourPicker = (NumberPicker) view.findViewById(R.id.hour);
        mHourPicker.setMaxValue(mMaxHour);
        mHourPicker.setValue(valueToHour(mSelectedValue));
        mHourPicker.setWrapSelectorWheel(false);

        mMinutePicker = (NumberPicker) view.findViewById(R.id.minute);
        mMinutePicker.setMaxValue(59);
        mMinutePicker.setValue(valueToMinute(mSelectedValue));

        super.onBindDialogView(view);
    }

    @Override
    protected void onDialogClosed(final boolean positiveResult) {
        super.onDialogClosed(positiveResult);

        if (positiveResult) {
            final int selectedValue = hourAndMinuteToValue(mHourPicker.getValue(), mMinutePicker.getValue());

            if (this.callChangeListener(selectedValue)) {
                mSelectedValue = selectedValue;

                this.persistInt(mSelectedValue);
            }
        }
    }

    private static int valueToHour(int value) {
        return value / 60;
    }

    private static int valueToMinute(int value) {
        return value % 60;
    }

    private static int hourAndMinuteToValue(int hour, int minute) {
        return 60 * hour + minute;
    }

}
