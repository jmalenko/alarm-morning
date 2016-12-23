package cz.jaro.alarmmorning;

import android.app.TimePickerDialog;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
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

    private Defaults defaults;
    private List<Integer> otherWeekdaysWithTheSameAlarmTime;

    protected int firstDayOfWeek;

    private Snackbar snackbar;

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

        com.ibm.icu.util.Calendar c = com.ibm.icu.util.Calendar.getInstance();
        firstDayOfWeek = c.getFirstDayOfWeek(); // ICU4J library encodes days as 1 = Sunday, ... , 7 = Saturday
        // This app uses 0 = Sunday, ... , 6 = Saturday
        firstDayOfWeek--;
        Log.v(TAG, "First day of week is " + firstDayOfWeek);
    }

    public Defaults loadPosition(int position) {
        GlobalManager globalManager = GlobalManager.getInstance();
        int dayOfWeek = positionToDayOfWeek(position);
        Defaults defaults = globalManager.loadDefault(dayOfWeek);
        return defaults;
    }

    private int positionToDayOfWeek(int position) {
        return AlarmDataSource.allDaysOfWeek[(firstDayOfWeek + position) % AlarmDataSource.allDaysOfWeek.length];
    }

    private void save(Defaults defaults) {
        Analytics analytics = new Analytics(Analytics.Channel.Activity, Analytics.ChannelName.Defaults);

        GlobalManager globalManager = GlobalManager.getInstance();
        globalManager.saveDefault(defaults, analytics);

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
        // Hide snackbar
        if (snackbar != null && snackbar.isShownOrQueued()) {
            snackbar.dismiss();
        }

        // Show time picker
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
        if (view.isShown()) {
            saveThisAndOthers(Defaults.STATE_ENABLED, hourOfDay, minute);
        }
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
        GlobalManager globalManager = GlobalManager.getInstance();

        otherWeekdaysWithTheSameAlarmTime = new ArrayList<>();

        for (int i = 0; i < AlarmDataSource.allDaysOfWeek.length; i++) {
            int j = (firstDayOfWeek + i) % AlarmDataSource.allDaysOfWeek.length;
            int dayOfWeek = AlarmDataSource.allDaysOfWeek[j];
            Defaults defaults2 = globalManager.loadDefault(dayOfWeek);
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

            snackbar = Snackbar.make(getCurrentFocus(), title, Snackbar.LENGTH_INDEFINITE);
            snackbar.setAction(getString(R.string.change_others_yes), snackbarClickListener);
            snackbar.getView().setBackgroundColor(ContextCompat.getColor(this, R.color.primary_dark));
            snackbar.show();
        }
    }

    View.OnClickListener snackbarClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            for (int dayOfWeek : otherWeekdaysWithTheSameAlarmTime) {
                Defaults defaults2 = defaults.clone();
                defaults2.setDayOfWeek(dayOfWeek);

                save(defaults2);
            }
        }
    };

}
