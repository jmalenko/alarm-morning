package cz.jaro.alarmmorning;

import android.app.Dialog;
import android.app.DialogFragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import cz.jaro.alarmmorning.graphics.TimePickerDialogWithDisable;

/**
 * A dialog for choosing time.
 */
public class TimePickerFragment extends DialogFragment {

    public static final String HOURS = "hours";
    public static final String MINUTES = "minutes";

    private TimePickerDialogWithDisable.OnTimeSetWithDisableListener onTimeSetListener;

    public void setOnTimeSetListener(TimePickerDialogWithDisable.OnTimeSetWithDisableListener onTimeSetListener) {
        this.onTimeSetListener = onTimeSetListener;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);
        getDialog().setCanceledOnTouchOutside(true);

        // XXX Workaround for Samsung Galaxy S7 - without this, the title is disapled with curently selected time. The title is duplicate as there are large buttons for hours and minutes in the dialog.
        getDialog().setTitle(null);

        return view;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        int hours = getArguments().getInt(HOURS);
        int minutes = getArguments().getInt(MINUTES);

        return new TimePickerDialogWithDisable(getActivity(), onTimeSetListener, hours, minutes, DateFormat.is24HourFormat(getActivity()));
    }

}
