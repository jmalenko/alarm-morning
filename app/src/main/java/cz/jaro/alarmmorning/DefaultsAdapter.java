package cz.jaro.alarmmorning;

import android.app.Dialog;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.app.TimePickerDialog;
import android.content.Context;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.TimePicker;

import cz.jaro.alarmmorning.model.AlarmDataSource;
import cz.jaro.alarmmorning.model.Defaults;

/**
 * Provide views to RecyclerView with data from mDataSet.
 */
public class DefaultsAdapter extends RecyclerView.Adapter<DefaultsAdapter.DefaultViewHolder> implements TimePickerDialog.OnTimeSetListener {

    private final DefaultsActivity defaultsActivity;

    private Defaults changingDefaults;

    private AlarmDataSource datasource;

    /**
     * Initialize the Adapter.
     *
     * @param defaultsActivity
     */
    public DefaultsAdapter(DefaultsActivity defaultsActivity) {
        this.defaultsActivity = defaultsActivity;

        datasource = new AlarmDataSource(defaultsActivity);
        datasource.open();
    }

    /**
     * Create new views (invoked by the layout manager)
     */
    @Override
    public DefaultViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.defaults_row_item, viewGroup, false);

        return new DefaultViewHolder(view, defaultsActivity.getFragmentManager(), this);
    }

    /**
     * Replaces the contents of a view (invoked by the layout manager).
     */
    @Override
    public void onBindViewHolder(DefaultViewHolder viewHolder, final int position) {
        int dayOfWeek = positionToDayOfWeek(position);

        Defaults defaults = datasource.loadDefault(dayOfWeek);

        // Show current alarm

        String dayOfWeekText = Localization.dayOfWeekToString(defaults.getDayOfWeek());
        viewHolder.getTextDayOfWeek().setText(dayOfWeekText);

        String timeText;
        if (defaults.getState() == AlarmDataSource.DEFAULT_STATE_SET) {
            timeText = Localization.timeToString(defaults.getHours(), defaults.getMinutes(), defaultsActivity);
        } else {
            timeText = defaultsActivity.getResources().getString(R.string.alarm_unset);
        }
        viewHolder.getTextTime().setText(timeText);

        viewHolder.setDefaults(defaults);
    }

    /**
     * Return the size of the dataset (invoked by the layout manager)
     */
    @Override
    public int getItemCount() {
        return AlarmDataSource.allDaysOfWeek.length;
    }

    private int positionToDayOfWeek(int position) {
        return AlarmDataSource.allDaysOfWeek[position];
    }

    public void setChangingDefaults(Defaults changingDefaults) {
        this.changingDefaults = changingDefaults;
    }

    @Override
    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
        // Save data
        changingDefaults.setState(AlarmDataSource.DEFAULT_STATE_SET);
        changingDefaults.setHours(hourOfDay);
        changingDefaults.setMinutes(minute);

        datasource.saveDefault(changingDefaults);

        notifyDataSetChanged();

        Context context = defaultsActivity.getBaseContext();
        SystemAlarm systemAlarm = SystemAlarm.getInstance(context);
        systemAlarm.setAlarm();
    }

    public void onLongClick() {
        // Save data
        if (changingDefaults.getState() == AlarmDataSource.DEFAULT_STATE_SET) {
            changingDefaults.setState(AlarmDataSource.DEFAULT_STATE_UNSET);
        } else {
            changingDefaults.setState(AlarmDataSource.DEFAULT_STATE_SET);
        }

        datasource.saveDefault(changingDefaults);

        notifyDataSetChanged();

        Context context = defaultsActivity.getBaseContext();
        SystemAlarm systemAlarm = SystemAlarm.getInstance(context);
        systemAlarm.setAlarm();
    }

    /**
     * Provide a reference to the type of views that you are using (custom ViewHolder)
     */
    public static class DefaultViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnLongClickListener {
        private final FragmentManager fragmentManager;
        private final DefaultsAdapter defaultsAdapter;

        private Defaults defaults;

        private final TextView textDayOfWeek;
        private final TextView textTime;

        public DefaultViewHolder(View view, final FragmentManager fragmentManager, DefaultsAdapter defaultsAdapter) {
            super(view);

            this.fragmentManager = fragmentManager;
            this.defaultsAdapter = defaultsAdapter;

            textDayOfWeek = (TextView) view.findViewById(R.id.textDayOfWeek);
            textTime = (TextView) view.findViewById(R.id.textTime);

            view.setOnClickListener(this);
            view.setOnLongClickListener(this);
        }

        public TextView getTextDayOfWeek() {
            return textDayOfWeek;
        }

        public TextView getTextTime() {
            return textTime;
        }

        public void setDefaults(Defaults defaults) {
            this.defaults = defaults;
        }

        @Override
        public void onClick(View view) {
            defaultsAdapter.setChangingDefaults(defaults);

            TimePickerFragment fragment = new TimePickerFragment();

            fragment.setDefaultViewHolder(this);

            // Preset current time
            Bundle bundle = new Bundle();
            bundle.putInt(TimePickerFragment.HOURS, defaults.getHours());
            bundle.putInt(TimePickerFragment.MINUTES, defaults.getMinutes());
            fragment.setArguments(bundle);

            fragment.show(fragmentManager, "timePicker");
        }

        @Override
        public boolean onLongClick(View view) {
            defaultsAdapter.setChangingDefaults(defaults);
            defaultsAdapter.onLongClick();
            return true;
        }
    }

    public static class TimePickerFragment extends DialogFragment {

        public static final String HOURS = "hours";
        public static final String MINUTES = "minutes";

        private DefaultViewHolder defaultViewHolder;

        public void setDefaultViewHolder(DefaultViewHolder defaultViewHolder) {
            this.defaultViewHolder = defaultViewHolder;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            int hours = getArguments().getInt(HOURS);
            int minutes = getArguments().getInt(MINUTES);

            TimePickerDialog timePickerDialog = new TimePickerDialog(getActivity(), defaultViewHolder.defaultsAdapter, hours, minutes, DateFormat.is24HourFormat(getActivity()));

            return timePickerDialog;
        }

    }
}

