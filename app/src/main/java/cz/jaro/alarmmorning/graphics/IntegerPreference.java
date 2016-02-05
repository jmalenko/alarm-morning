package cz.jaro.alarmmorning.graphics;

import android.app.AlertDialog;
import android.content.Context;
import android.content.res.TypedArray;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.NumberPicker;

import cz.jaro.alarmmorning.R;

/**
 * The Preference representing an integer (a natural number).
 * <p>
 * The user uses a NumberPicker to set the volume..
 * <p>
 * The value is stored as an integer.
 */
public class IntegerPreference extends DialogPreference {
    private static final int MIN_VALUE = 0;
    private static final int MAX_VALUE = 100;
    private static final boolean WRAP_SELECTOR_WHEEL = false;

    private int mSelectedValue;
    private final int mMinValue;
    private final int mMaxValue;
    private final boolean mWrapSelectorWheel;
    private NumberPicker mNumberPicker;

    public IntegerPreference(final Context context, final AttributeSet attrs) {
        super(context, attrs);

        final TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.IntegerPreference);

        mMinValue = a.getInt(R.styleable.IntegerPreference_minValue, IntegerPreference.MIN_VALUE);
        mMaxValue = a.getInt(R.styleable.IntegerPreference_maxValue, IntegerPreference.MAX_VALUE);
        mWrapSelectorWheel = a.getBoolean(R.styleable.IntegerPreference_wrapSelectorWheel, IntegerPreference.WRAP_SELECTOR_WHEEL);

        a.recycle();
    }

    @Override
    protected void onSetInitialValue(final boolean restoreValue, final Object defaultValue) {
        final int intDefaultValue = defaultValue instanceof Integer ? (int) defaultValue : mMinValue;
        mSelectedValue = restoreValue ? this.getPersistedInt(intDefaultValue) : intDefaultValue;
    }

    @Override
    protected Object onGetDefaultValue(final TypedArray a, final int index) {
        return a.getInteger(index, 0);
    }

    @Override
    protected void onPrepareDialogBuilder(final AlertDialog.Builder builder) {
        super.onPrepareDialogBuilder(builder);

        mNumberPicker = new NumberPicker(this.getContext());
        mNumberPicker.setMinValue(mMinValue);
        mNumberPicker.setMaxValue(mMaxValue);
        mNumberPicker.setValue(mSelectedValue);
        mNumberPicker.setWrapSelectorWheel(mWrapSelectorWheel);
        mNumberPicker.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        final LinearLayout linearLayout = new LinearLayout(this.getContext());
        linearLayout.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        linearLayout.setGravity(Gravity.CENTER);
        linearLayout.addView(mNumberPicker);

        builder.setView(linearLayout);
    }

    @Override
    protected void onDialogClosed(final boolean positiveResult) {
        super.onDialogClosed(positiveResult);

        if (positiveResult && mNumberPicker != null) {
            final int selectedValue = mNumberPicker.getValue();

            if (this.callChangeListener(selectedValue)) {
                mSelectedValue = selectedValue;

                this.persistInt(mSelectedValue);
            }
        }
    }

}