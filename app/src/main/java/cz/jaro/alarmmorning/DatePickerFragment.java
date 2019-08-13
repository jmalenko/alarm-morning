package cz.jaro.alarmmorning;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.Nullable;

/**
 * A dialog for choosing date.
 */
public class DatePickerFragment extends DialogFragment {

    public static final String YEAR = "year";
    public static final String MONTH = "month";
    public static final String DAY = "day";

    private DatePickerDialog.OnDateSetListener onDateSetListener;

    public void setOnDateSetListener(DatePickerDialog.OnDateSetListener onDateSetListener) {
        this.onDateSetListener = onDateSetListener;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);
        getDialog().setCanceledOnTouchOutside(true);
        return view;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        int year = getArguments().getInt(YEAR);
        int month = getArguments().getInt(MONTH);
        int day = getArguments().getInt(DAY);

        return new DatePickerDialog(getActivity(), onDateSetListener, year, month, day);
    }
}