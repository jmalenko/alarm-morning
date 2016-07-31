package cz.jaro.alarmmorning;

import android.app.TimePickerDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.ContextMenu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TimePicker;

import java.util.ArrayList;
import java.util.List;

import cz.jaro.alarmmorning.graphics.RecyclerViewWithContextMenu;
import cz.jaro.alarmmorning.graphics.SimpleDividerItemDecoration;
import cz.jaro.alarmmorning.model.AlarmDataSource;
import cz.jaro.alarmmorning.model.Defaults;

public class DefaultsActivity extends AppCompatActivity implements View.OnCreateContextMenuListener, TimePickerDialog.OnTimeSetListener, View.OnClickListener {

    private static final String TAG = DefaultsActivity.class.getSimpleName();

    private DefaultsAdapter adapter;
    private RecyclerView recyclerView;

    private AlarmDataSource dataSource;

    private Defaults defaults;
    private List<Integer> otherWeekdaysWithTheSameAlarmTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_defaults);

        // Set a Toolbar to replace the ActionBar.
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        recyclerView = (RecyclerView) findViewById(R.id.defaults_recycler_view);

        // use a linear layout manager
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);

        // specify an adapter
        adapter = new DefaultsAdapter(this);
        recyclerView.setAdapter(adapter);

        // item separator
        recyclerView.addItemDecoration(new SimpleDividerItemDecoration(this));

        registerForContextMenu(recyclerView);

        dataSource = new AlarmDataSource(this);
        dataSource.open();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        dataSource.close();
    }

    public Defaults loadPosition(int position) {
        int dayOfWeek = positionToDayOfWeek(position);
        Defaults defaults = dataSource.loadDefault(dayOfWeek);
        return defaults;
    }

    private int positionToDayOfWeek(int position) {
        return AlarmDataSource.allDaysOfWeek[position];
    }

    private void save(Defaults defaults) {
        GlobalManager globalManager = new GlobalManager(this);
        globalManager.saveAlarmTimeDefault(defaults, dataSource);

        adapter.notifyDataSetChanged();
    }

    /*
     * On click
     * ========
     */

    @Override
    public void onClick(View view) {
        int position = recyclerView.getChildPosition(view);
        Log.d(TAG, "Clicked item on position " + position);

        defaults = loadPosition(position);
        showTimePicker();
    }

    private void showTimePicker() {
        TimePickerFragment fragment = new TimePickerFragment();

        fragment.setOnTimeSetListener(this);

        // Preset time
        Bundle bundle = new Bundle();
        bundle.putInt(TimePickerFragment.HOURS, defaults.getHour());
        bundle.putInt(TimePickerFragment.MINUTES, defaults.getMinute());
        fragment.setArguments(bundle);

        fragment.show(getFragmentManager(), "timePicker");
    }

    @Override
    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
        saveThisAndOthers(Defaults.STATE_ENABLED, hourOfDay, minute);
    }

    private void saveThisAndOthers(int state, int hour, int minute) {
        calculateChangeOtherDays();

        defaults.setState(state);
        defaults.setHour(hour);
        defaults.setMinute(minute);

        save(defaults);

        showDialogChangeOtherDays();
    }

    /*
     * On context menu
     * ===============
     */

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        int position = ((RecyclerViewWithContextMenu.RecyclerViewContextMenuInfo) menuInfo).position;
        Log.d(TAG, "Long clicked item on position " + position);

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.default_context_menu, menu);

        // Set header

        defaults = loadPosition(position);

        String dayOfWeekText = Localization.dayOfWeekToStringShort(getResources(), defaults.getDayOfWeek());

        String headerTitle;
        if (defaults.isEnabled()) {
            String timeText;
            if (defaults.isEnabled()) {
                timeText = Localization.timeToString(defaults.getHour(), defaults.getMinute(), this);
            } else {
                timeText = getResources().getString(R.string.alarm_unset);
            }
            headerTitle = getResources().getString(R.string.menu_default_header, timeText, dayOfWeekText);
        } else {
            headerTitle = getResources().getString(R.string.menu_default_header_disabled, dayOfWeekText);
        }
        menu.setHeaderTitle(headerTitle);

        // Hide irrelevant items

        MenuItem disable = menu.findItem(R.id.default_disable);
        disable.setVisible(defaults.isEnabled());
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.default_set_time:
                Log.d(TAG, "Set time");
                showTimePicker();
                break;

            case R.id.default_disable:
                Log.d(TAG, "Disable");
                saveThisAndOthers(Defaults.STATE_DISABLED, defaults.getHour(), defaults.getMinute());
                break;
        }
        return super.onContextItemSelected(item);
    }

    /*
     * Change other days
     * =================
     */

    /**
     * Create list of weekdays to change.
     */
    private void calculateChangeOtherDays() {
        // TODO Localization - sort weekdays in natural order

        otherWeekdaysWithTheSameAlarmTime = new ArrayList<>();

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        boolean askPreference = preferences.getBoolean(SettingsActivity.PREF_ASK_TO_CHANGE_OTHER_WEEKDAYS_WIT_THE_SAME_ALARM_TIME, SettingsActivity.PREF_ASK_TO_CHANGE_OTHER_WEEKDAYS_WIT_THE_SAME_ALARM_TIME_DEFAULT);

        if (!askPreference)
            return;

        for (int dayOfWeek : AlarmDataSource.allDaysOfWeek) {
            Defaults defaults2 = dataSource.loadDefault(dayOfWeek);
            boolean sameAlarmTime = defaults2.getState() == defaults.getState() &&
                    defaults2.getHour() == defaults.getHour() &&
                    defaults2.getMinute() == defaults.getMinute();
            if (sameAlarmTime && defaults2.getDayOfWeek() != defaults.getDayOfWeek()) {
                otherWeekdaysWithTheSameAlarmTime.add(defaults2.getDayOfWeek());
            }
        }
    }

    private void showDialogChangeOtherDays() {
        if (!otherWeekdaysWithTheSameAlarmTime.isEmpty()) {
            String days = Localization.daysOfWeekToString(otherWeekdaysWithTheSameAlarmTime, getResources());
            String timeText;
            if (defaults.isEnabled()) {
                timeText = Localization.timeToString(defaults.getHour(), defaults.getMinute(), this);
            } else {
                timeText = getResources().getString(R.string.alarm_unset);
            }
            String title = getResources().getString(R.string.change_others_title, timeText, days);

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(title)
                    .setMessage(getString(R.string.change_others_message))
                    .setPositiveButton(getString(R.string.change_others_yes), dialogClickListener)
                    .setNegativeButton(getString(R.string.change_others_no), dialogClickListener)
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
                        Defaults defaults2 = defaults.clone();
                        defaults2.setDayOfWeek(dayOfWeek);

                        save(defaults2);
                    }
                    break;

                case DialogInterface.BUTTON_NEGATIVE:
                    // Nothing
                    break;
            }
        }
    };

}
