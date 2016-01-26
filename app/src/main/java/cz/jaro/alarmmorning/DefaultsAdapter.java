package cz.jaro.alarmmorning;

import android.app.FragmentManager;
import android.app.TimePickerDialog;
import android.content.Context;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
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

    private final ActivityInterface activityInterface;

    private Defaults changingDefaults;

    private AlarmDataSource datasource;

    /**
     * Initialize the Adapter.
     *
     * @param activityInterface
     */
    public DefaultsAdapter(ActivityInterface activityInterface) {
        this.activityInterface = activityInterface;

        datasource = new AlarmDataSource(activityInterface.getContextI());
        datasource.open();
    }

    /**
     * Create new views (invoked by the layout manager)
     */
    @Override
    public DefaultViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.defaults_row_item, viewGroup, false);

        return new DefaultViewHolder(view, activityInterface.getFragmentManagerI(), this);
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
        if (defaults.isEnabled()) {
            timeText = Localization.timeToString(defaults.getHour(), defaults.getMinute(), activityInterface.getContextI());
        } else {
            timeText = activityInterface.getResourcesI().getString(R.string.alarm_unset);
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
        changingDefaults.setState(AlarmDataSource.DEFAULT_STATE_ENABLED);
        changingDefaults.setHour(hourOfDay);
        changingDefaults.setMinute(minute);

        save(changingDefaults);
    }

    public void onLongClick() {
        changingDefaults.reverse();

        save(changingDefaults);
    }

    private void save(Defaults defaults) {
        datasource.saveDefault(defaults);

        refresh();
    }

    private void refresh() {
        notifyDataSetChanged();

        Context context = activityInterface.getContextI();
        SystemAlarm systemAlarm = SystemAlarm.getInstance(context);
        systemAlarm.setSystemAlarm();
    }

    public void onDestroy() {
        datasource.close();
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

            fragment.setOnTimeSetListener(defaultsAdapter);

            // Preset time
            Bundle bundle = new Bundle();
            bundle.putInt(TimePickerFragment.HOURS, defaults.getHour());
            bundle.putInt(TimePickerFragment.MINUTES, defaults.getMinute());
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

}