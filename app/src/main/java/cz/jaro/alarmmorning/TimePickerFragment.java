package cz.jaro.alarmmorning;

import android.app.Dialog;
import android.app.DialogFragment;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.text.format.DateFormat;

/**
 * A dialog for choosing time.
 */
public class TimePickerFragment extends DialogFragment {

    public static final String HOURS = "hours";
    public static final String MINUTES = "minutes";

    private TimePickerDialog.OnTimeSetListener onTimeSetListener;

    public void setOnTimeSetListener(TimePickerDialog.OnTimeSetListener onTimeSetListener) {
        this.onTimeSetListener = onTimeSetListener;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        int hours = getArguments().getInt(HOURS);
        int minutes = getArguments().getInt(MINUTES);

        return new TimePickerDialog(getActivity(), onTimeSetListener, hours, minutes, DateFormat.is24HourFormat(getActivity()));
    }

}
