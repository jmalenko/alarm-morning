package cz.jaro.alarmmorning.graphics;

import android.app.AlertDialog;
import android.content.Context;
import android.content.res.TypedArray;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.SeekBar;

import cz.jaro.alarmmorning.R;

/**
 * Created by jmalenko on 22.1.2016.
 */
public class VolumePreference extends DialogPreference {
    private static final int MAX_VALUE = 100;

    private int mSelectedValue;
    private final int mMaxValue;
    private SeekBar mSeekBar;

    public VolumePreference(final Context context, final AttributeSet attrs) {
        super(context, attrs);

        final TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.IntegerPreference);

        mMaxValue = a.getInt(R.styleable.IntegerPreference_maxValue, MAX_VALUE);

        a.recycle();
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
    protected void onPrepareDialogBuilder(final AlertDialog.Builder builder) {
        super.onPrepareDialogBuilder(builder);

        mSeekBar = new SeekBar(this.getContext());
        mSeekBar.setMax(mMaxValue);
        mSeekBar.setProgress(mSelectedValue);
        mSeekBar.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        final LinearLayout linearLayout = new LinearLayout(this.getContext());
        linearLayout.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        linearLayout.setGravity(Gravity.CENTER);
        linearLayout.addView(mSeekBar);

        builder.setView(linearLayout);
    }

    @Override
    protected void onDialogClosed(final boolean positiveResult) {
        super.onDialogClosed(positiveResult);

        if (positiveResult && mSeekBar != null) {
            final int selectedValue = mSeekBar.getProgress();

            if (this.callChangeListener(selectedValue)) {
                mSelectedValue = selectedValue;

                this.persistInt(mSelectedValue);
            }
        }
    }

}