package cz.jaro.alarmmorning;

import android.app.FragmentManager;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.TimePicker;

import java.util.ArrayList;
import java.util.List;

import cz.jaro.alarmmorning.clock.Clock;
import cz.jaro.alarmmorning.clock.SystemClock;
import cz.jaro.alarmmorning.model.AlarmDataSource;
import cz.jaro.alarmmorning.model.Defaults;

/**
 * Provide views to RecyclerView with data from mDataSet.
 */
public class DefaultsAdapter extends RecyclerView.Adapter<DefaultsAdapter.DefaultViewHolder> implements TimePickerDialog.OnTimeSetListener {

    private static final String TAG = DefaultsAdapter.class.getSimpleName();

    private final ActivityInterface activityInterface;

    private Defaults changingDefaults;
    private List<Integer> otherWeekdaysWithTheSameAlarmTime;

    private AlarmDataSource dataSource;

    /**
     * Initialize the Adapter.
     *
     * @param activityInterface
     */
    public DefaultsAdapter(ActivityInterface activityInterface) {
        this.activityInterface = activityInterface;

        dataSource = new AlarmDataSource(activityInterface.getContextI());
        dataSource.open();
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

        Defaults defaults = dataSource.loadDefault(dayOfWeek);

        // Show current alarm

        Clock clock = new SystemClock();
        String dayOfWeekText = Localization.dayOfWeekToString(defaults.getDayOfWeek(), clock);
        viewHolder.getTextDayOfWeek().setText(dayOfWeekText);

        String timeText;
        if (defaults.isEnabled()) {
            timeText = Localization.timeToString(defaults.getHour(), defaults.getMinute(), activityInterface.getContextI(), clock);
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
        calcultateChangeOtherDays();

        changingDefaults.setState(Defaults.STATE_ENABLED);
        changingDefaults.setHour(hourOfDay);
        changingDefaults.setMinute(minute);

        save(changingDefaults);

        showDialogToChangeOtherDays();
    }

    private void calcultateChangeOtherDays() {
        // TODO Localization - sort weekdays in natural order

        // Create list of weekdays to change
        otherWeekdaysWithTheSameAlarmTime = new ArrayList<>();
        for (int dayOfWeek : AlarmDataSource.allDaysOfWeek) {
            Defaults defaults = dataSource.loadDefault(dayOfWeek);
            boolean sameAlarmTime = defaults.getState() == changingDefaults.getState() &&
                    defaults.getHour() == changingDefaults.getHour() &&
                    defaults.getMinute() == changingDefaults.getMinute();
            if (sameAlarmTime && defaults.getDayOfWeek() != changingDefaults.getDayOfWeek()) {
                otherWeekdaysWithTheSameAlarmTime.add(defaults.getDayOfWeek());
            }
        }
    }

    private void showDialogToChangeOtherDays() {
        if (!otherWeekdaysWithTheSameAlarmTime.isEmpty()) {
            Clock clock = new SystemClock(); // // TODO Solve dependency on clock
            // TODO Localization - full names od days (not abbreviations)
            String days = Localization.daysOfWeekToString(otherWeekdaysWithTheSameAlarmTime, activityInterface.getResourcesI(), clock);
            ;
            String timeText;
            if (changingDefaults.isEnabled()) {
                timeText = Localization.timeToString(changingDefaults.getHour(), changingDefaults.getMinute(), activityInterface.getContextI(), clock);
            } else {
                timeText = activityInterface.getResourcesI().getString(R.string.alarm_unset);
            }
            String title = String.format(activityInterface.getResourcesI().getString(R.string.change_others_title), timeText, days);

            AlertDialog.Builder builder = new AlertDialog.Builder(activityInterface.getContextI());
            builder.setTitle(title)
                    .setMessage(activityInterface.getContextI().getString(R.string.change_others_message))
                    .setPositiveButton(activityInterface.getContextI().getString(R.string.change_others_yes), dialogClickListener)
                    .setNegativeButton(activityInterface.getContextI().getString(R.string.change_others_no), dialogClickListener)
                    .show();
        }
    }

    DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            switch (which) {
                case DialogInterface.BUTTON_POSITIVE:
                    // Yes button clicked
                    for (int dayOfWeek : otherWeekdaysWithTheSameAlarmTime) {
                        Defaults defaults = changingDefaults.clone();
                        defaults.setDayOfWeek(dayOfWeek);

                        save(defaults);
                    }
                    break;

                case DialogInterface.BUTTON_NEGATIVE:
                    // Nothing
                    break;
            }
        }
    };

    public void onLongClick() {
        changingDefaults.reverse();

        save(changingDefaults);
    }

    private void save(Defaults defaults) {
        dataSource.saveDefault(defaults);

        refresh();
    }

    private void refresh() {
        notifyDataSetChanged();

        Context context = activityInterface.getContextI();
        GlobalManager globalManager = new GlobalManager(context);
        globalManager.onAlarmSet();
    }

    public void onDestroy() {
        dataSource.close();
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