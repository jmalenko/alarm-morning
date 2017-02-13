package cz.jaro.alarmmorning.graphics;

import android.app.TimePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.View;
import android.widget.TimePicker;

import cz.jaro.alarmmorning.R;

/**
 * A dialog that prompts the user for the time of day using a {@link TimePicker} and with a button that allows to set "no time" (effectively disabling the
 * alarm).
 */
public class TimePickerDialogWithDisable extends TimePickerDialog {

    private final OnTimeSetWithDisableListener mTimeSetWithDisableListener;
    private TimePicker mTimePicker;

    /**
     * The callback interface used to indicate the user is done filling in the time (e.g. they clicked on the 'OK' or 'Disable' button).
     */
    public interface OnTimeSetWithDisableListener {
        /**
         * Called when the user is done setting a new time and the dialog has closed.
         *
         * @param view      the mTimePicker associated with this listener
         * @param disable   if the time was set
         * @param hourOfDay the hour that was set
         * @param minute    the minute that was set
         */
        void onTimeSetWithDisable(TimePicker view, boolean disable, int hourOfDay, int minute);
    }

    /**
     * Creates a new time picker dialog.
     *
     * @param context      the parent context
     * @param listener     the listener to call when the time is set
     * @param hourOfDay    the initial hour
     * @param minute       the initial minute
     * @param is24HourView whether this is a 24 hour mTimePicker or AM/PM
     */
    public TimePickerDialogWithDisable(Context context, OnTimeSetWithDisableListener listener, int hourOfDay, int minute, boolean is24HourView) {
        this(context, 0, listener, hourOfDay, minute, is24HourView);
    }

    /**
     * Creates a new time picker dialog with the specified theme.
     * <p>
     * The theme is overlaid on top of the theme of the parent {@code context}. If {@code themeResId} is 0, the dialog will be inflated using the theme
     * specified by the {@link android.R.attr#timePickerDialogTheme android:timePickerDialogTheme} attribute on the parent {@code context}'s theme.
     *
     * @param context      the parent context
     * @param themeResId   the resource ID of the theme to apply to this dialog
     * @param listener     the listener to call when the time is set
     * @param hourOfDay    the initial hour
     * @param minute       the initial minute
     * @param is24HourView Whether this is a 24 hour mTimePicker, or AM/PM.
     */
    public TimePickerDialogWithDisable(Context context, int themeResId, OnTimeSetWithDisableListener listener, int hourOfDay, int minute, boolean is24HourView) {
        super(context, themeResId, null, hourOfDay, minute, is24HourView);

        mTimeSetWithDisableListener = listener;

        final Context themeContext = getContext();
        setButton(BUTTON_POSITIVE, themeContext.getString(R.string.action_set), this);
        setButton(BUTTON_NEGATIVE, null, this); // Effectively sets the button to gone (not visible)
        // TODO Hiding the negative button violates the Material Design guidelines, but I believe users will be fine as the dialog (in TimePickerFragment) is
        // "cancelled on touch outside"
        setButton(BUTTON_NEUTRAL, themeContext.getString(R.string.action_disable), this);
    }

    /**
     * Set the mTimePicker to display in that dialog.
     */
    public void setView(View view) {
        this.mTimePicker = (TimePicker) view;
        super.setView(view);
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        switch (which) {
            case BUTTON_NEUTRAL:
                if (mTimeSetWithDisableListener != null) {
                    mTimeSetWithDisableListener.onTimeSetWithDisable(mTimePicker, true, mTimePicker.getCurrentHour(), mTimePicker.getCurrentMinute());
                }
                break;
            case BUTTON_POSITIVE:
                if (mTimeSetWithDisableListener != null) {
                    mTimeSetWithDisableListener.onTimeSetWithDisable(mTimePicker, false, mTimePicker.getCurrentHour(), mTimePicker.getCurrentMinute());
                }
                break;
            case BUTTON_NEGATIVE:
                cancel();
                break;
        }
    }

}
