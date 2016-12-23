package cz.jaro.alarmmorning;

import android.content.res.Resources;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import cz.jaro.alarmmorning.model.AlarmDataSource;
import cz.jaro.alarmmorning.model.Defaults;

/**
 * Provide views to RecyclerView with data from mDataSet.
 */
public class DefaultsAdapter extends RecyclerView.Adapter<DefaultsAdapter.DefaultViewHolder> {

    private final DefaultsActivity activity;

    /**
     * Initialize the Adapter.
     *
     * @param activity Activity that contains the widget that uses this adapter
     */
    public DefaultsAdapter(DefaultsActivity activity) {
        this.activity = activity;
    }

    /**
     * Create new views (invoked by the layout manager)
     */
    @Override
    public DefaultViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.defaults_row_item, viewGroup, false);

        view.setOnClickListener(activity);

        return new DefaultViewHolder(view);
    }

    /**
     * Replaces the contents of a view (invoked by the layout manager).
     */
    @Override
    public void onBindViewHolder(DefaultViewHolder viewHolder, final int position) {
        Defaults defaults = activity.loadPosition(position); // positions start from 0
        Resources res = activity.getResources();

        String dayOfWeekText = Localization.dayOfWeekToStringShort(res, defaults.getDayOfWeek());
        viewHolder.getTextDayOfWeek().setText(dayOfWeekText);

        int backgroundColor;
        com.ibm.icu.util.Calendar c = com.ibm.icu.util.Calendar.getInstance();
        int dayOfWeekType = c.getDayOfWeekType(((activity.firstDayOfWeek + position) % AlarmDataSource.allDaysOfWeek.length) + 1); // ICU4J library encodes days as 1 = Sunday, ... , 7 = Saturday
        switch (dayOfWeekType) {
            case com.ibm.icu.util.Calendar.WEEKEND:
                backgroundColor = res.getColor(R.color.weekend);
                break;

            default:
                backgroundColor = res.getColor(R.color.primary_dark);
        }
        viewHolder.getTextDayOfWeek().setBackgroundColor(backgroundColor);

        String timeText;
        if (defaults.isEnabled()) {
            timeText = Localization.timeToString(defaults.getHour(), defaults.getMinute(), activity);
        } else {
            timeText = activity.getResources().getString(R.string.alarm_unset);
        }
        viewHolder.getTextTime().setText(timeText);
    }

    /**
     * Return the size of the dataset (invoked by the layout manager)
     */
    @Override
    public int getItemCount() {
        return AlarmDataSource.allDaysOfWeek.length;
    }


    /**
     * Provide a reference to the type of views that you are using (custom ViewHolder)
     */
    public static class DefaultViewHolder extends RecyclerView.ViewHolder implements View.OnLongClickListener {

        private final TextView textDayOfWeek;
        private final TextView textTime;

        public DefaultViewHolder(View view) {
            super(view);

            textDayOfWeek = (TextView) view.findViewById(R.id.textDayOfWeek);
            textTime = (TextView) view.findViewById(R.id.textTime);

            view.setOnLongClickListener(this);
        }

        public TextView getTextDayOfWeek() {
            return textDayOfWeek;
        }

        public TextView getTextTime() {
            return textTime;
        }

        @Override
        public boolean onLongClick(View view) {
            itemView.showContextMenu();
            return true;
        }
    }

}