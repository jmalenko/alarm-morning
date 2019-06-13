package cz.jaro.alarmmorning.graphics;

import android.content.Context;
import android.content.res.TypedArray;
import android.preference.DialogPreference;
import android.text.format.DateFormat;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TimePicker;

/**
 * The Preference representing time (absolute time in a day).
 */
public class TimePreference extends DialogPreference {
    private int mHour = 0;
    private int mMinute = 0;
    private TimePicker mTimePicker = null;

    public TimePreference(Context context, AttributeSet attrs) {
        super(context, attrs);

        setPositiveButtonText(context.getString(android.R.string.ok));
        setNegativeButtonText(context.getString(android.R.string.cancel));
    }

    @Override
    protected void onSetInitialValue(boolean restoreValue, Object defaultValue) {
        String time;

        if (restoreValue) {
            if (defaultValue == null) {
                time = getPersistedString("00:00");
            } else {
                time = getPersistedString(defaultValue.toString());
            }
        } else {
            time = defaultValue.toString();
        }

        mHour = getHour(time);
        mMinute = getMinute(time);
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        return a.getString(index);
    }

    @Override
    protected View onCreateDialogView() {
        mTimePicker = new TimePicker(getContext());
        mTimePicker.setIs24HourView(DateFormat.is24HourFormat(getContext()));

        return mTimePicker;
    }

    @Override
    protected void onBindDialogView(View v) {
        super.onBindDialogView(v);

        mTimePicker.setCurrentHour(mHour);
        mTimePicker.setCurrentMinute(mMinute);
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);

        if (positiveResult) {
            int hour = mTimePicker.getCurrentHour();
            int minute = mTimePicker.getCurrentMinute();

            String time = hour + ":" + minute;

            if (callChangeListener(time)) {
                mHour = hour;
                mMinute = minute;

                persistString(time);
            }
        }
    }

    public static int getHour(String time) {
        String[] pieces = time.split(":");
        return Integer.parseInt(pieces[0]);
    }

    public static int getMinute(String time) {
        String[] pieces = time.split(":");
        return Integer.parseInt(pieces[1]);
    }
}