/*
    Alarm Morning
    Copyright (C) 2015  Jaromir Malenko

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package cz.jaro.alarmmorning;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import cz.jaro.alarmmorning.graphics.SimpleDividerItemDecoration;
import cz.jaro.alarmmorning.model.AlarmDataSource;
import cz.jaro.alarmmorning.model.AlarmDbHelper;
import cz.jaro.alarmmorning.receivers.AlarmReceiver;


public class CalendarActivity extends Activity {

    private String[] menuItems;
    private DrawerLayout drawerLayout;
    private ListView drawerList;

    private ActionBarDrawerToggle drawerToggle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calendar);

        menuItems = getResources().getStringArray(R.array.menu_items_array);
        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawerList = (ListView) findViewById(R.id.left_drawer);

        // set a custom shadow that overlays the main content when the drawer opens
        drawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);

        // set up the drawer's list view with items and click listener
        drawerList.setAdapter(new ArrayAdapter<String>(this, R.layout.drawer_list_item, menuItems));
        drawerList.setOnItemClickListener(new DrawerItemClickListener());

        ActionBar ab = getActionBar();
        ab.setDisplayHomeAsUpEnabled(true);
        ab.setHomeButtonEnabled(true);

        drawerToggle = new ActionBarDrawerToggle(this, drawerLayout, R.drawable.ic_drawer, R.string.drawer_open, R.string.drawer_close) {
            public void onDrawerClosed(View view) {
                getActionBar().setTitle(R.string.title_activity_calendar);
            }

            public void onDrawerOpened(View drawerView) {
                getActionBar().setTitle(R.string.app_name);
            }
        };
        drawerLayout.setDrawerListener(drawerToggle);

        if (savedInstanceState == null) {
            Fragment fragment = new CalendarFragment();
            // Insert the fragment by replacing any existing fragment
            FragmentManager fragmentManager = getFragmentManager();
            fragmentManager.beginTransaction().replace(R.id.content_frame, fragment).commit();

            drawerList.setItemChecked(0, true);
        }
    }

    private class DrawerItemClickListener implements ListView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView parent, View view, int position, long id) {
            switch (position) {
                case 0:
                    drawerLayout.closeDrawer(drawerList);
                    return;

                case 1:
                    Intent intent = new Intent(CalendarActivity.this, DefaultsActivity.class);
                    startActivity(intent);
                    return;

                case 2:
                    intent = new Intent(CalendarActivity.this, SettingsActivity.class);
                    startActivity(intent);
                    return;

                case 3:
                    // TODO remove after testing
                    Context context = CalendarActivity.this;
                    intent = new Intent(context, AlarmReceiver.class);

                    Toast.makeText(context, "Alarm will go off in 5 seconds", Toast.LENGTH_SHORT).show();

                    long time = new GregorianCalendar().getTimeInMillis() + 5000;

                    AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
                    PendingIntent operation = PendingIntent.getBroadcast(context, 1, intent, PendingIntent.FLAG_UPDATE_CURRENT);
                    alarmManager.set(AlarmManager.RTC_WAKEUP, time, operation);

                    return;

                case 4:
                    // TODO remove after testing
                    context = CalendarActivity.this;
                    SystemAlarm systemAlarm = SystemAlarm.getInstance(context);
                    Long timeX = systemAlarm.getTime();
                    Toast.makeText(context, timeX != null ? "Next alarm is at " + new Date(timeX).toString() : "No alarm defined", Toast.LENGTH_SHORT).show();


                    Toast.makeText(context, "Current time is " + new GregorianCalendar().getTime().toString(), Toast.LENGTH_SHORT).show();

                    //long a = alarmTime.getTime().getTime() / 1000;
                    long a = timeX / 1000;
//                    Toast.makeText(context, "Alarm time is " + a, Toast.LENGTH_SHORT).show();

                    long b = new GregorianCalendar().getTime().getTime() / 1000;

//                    Toast.makeText(context, "Current time is " + b, Toast.LENGTH_SHORT).show();

                    Toast.makeText(context, "Will ring in " + (a - b) + " s", Toast.LENGTH_SHORT).show();
                    return;

                case 5:
                    // TODO remove after testing

                    context = CalendarActivity.this;
                    AlarmDbHelper dbHelper = new AlarmDbHelper(context);
                    SQLiteDatabase database = dbHelper.getWritableDatabase();
                    database.delete(AlarmDbHelper.TABLE_DEFAULTS, null, null);

                    ContentValues values;

                    values = new ContentValues();
                    values.put(AlarmDbHelper.COLUMN_DEFAULTS_DAY_OF_WEEK, Calendar.MONDAY);
                    values.put(AlarmDbHelper.COLUMN_DEFAULTS_STATE, AlarmDataSource.DEFAULT_STATE_SET);
                    values.put(AlarmDbHelper.COLUMN_DEFAULTS_HOURS, 7);
                    values.put(AlarmDbHelper.COLUMN_DEFAULTS_MINUTES, 1);
                    database.insert(AlarmDbHelper.TABLE_DEFAULTS, null, values);

                    values = new ContentValues();
                    values.put(AlarmDbHelper.COLUMN_DEFAULTS_DAY_OF_WEEK, Calendar.TUESDAY);
                    values.put(AlarmDbHelper.COLUMN_DEFAULTS_STATE, AlarmDataSource.DEFAULT_STATE_SET);
                    values.put(AlarmDbHelper.COLUMN_DEFAULTS_HOURS, 7);
                    values.put(AlarmDbHelper.COLUMN_DEFAULTS_MINUTES, 2);
                    database.insert(AlarmDbHelper.TABLE_DEFAULTS, null, values);

                    values = new ContentValues();
                    values.put(AlarmDbHelper.COLUMN_DEFAULTS_DAY_OF_WEEK, Calendar.WEDNESDAY);
                    values.put(AlarmDbHelper.COLUMN_DEFAULTS_STATE, AlarmDataSource.DEFAULT_STATE_SET);
                    values.put(AlarmDbHelper.COLUMN_DEFAULTS_HOURS, 7);
                    values.put(AlarmDbHelper.COLUMN_DEFAULTS_MINUTES, 3);
                    database.insert(AlarmDbHelper.TABLE_DEFAULTS, null, values);

                    values = new ContentValues();
                    values.put(AlarmDbHelper.COLUMN_DEFAULTS_DAY_OF_WEEK, Calendar.THURSDAY);
                    values.put(AlarmDbHelper.COLUMN_DEFAULTS_STATE, AlarmDataSource.DEFAULT_STATE_SET);
                    values.put(AlarmDbHelper.COLUMN_DEFAULTS_HOURS, 7);
                    values.put(AlarmDbHelper.COLUMN_DEFAULTS_MINUTES, 4);
                    database.insert(AlarmDbHelper.TABLE_DEFAULTS, null, values);

                    values = new ContentValues();
                    values.put(AlarmDbHelper.COLUMN_DEFAULTS_DAY_OF_WEEK, Calendar.FRIDAY);
                    values.put(AlarmDbHelper.COLUMN_DEFAULTS_STATE, AlarmDataSource.DEFAULT_STATE_SET);
                    values.put(AlarmDbHelper.COLUMN_DEFAULTS_HOURS, 7);
                    values.put(AlarmDbHelper.COLUMN_DEFAULTS_MINUTES, 5);
                    database.insert(AlarmDbHelper.TABLE_DEFAULTS, null, values);

                    values = new ContentValues();
                    values.put(AlarmDbHelper.COLUMN_DEFAULTS_DAY_OF_WEEK, Calendar.SATURDAY);
                    values.put(AlarmDbHelper.COLUMN_DEFAULTS_STATE, AlarmDataSource.DEFAULT_STATE_UNSET);
                    values.put(AlarmDbHelper.COLUMN_DEFAULTS_HOURS, 8);
                    values.put(AlarmDbHelper.COLUMN_DEFAULTS_MINUTES, 0);
                    database.insert(AlarmDbHelper.TABLE_DEFAULTS, null, values);

                    values = new ContentValues();
                    values.put(AlarmDbHelper.COLUMN_DEFAULTS_DAY_OF_WEEK, Calendar.SUNDAY);
                    values.put(AlarmDbHelper.COLUMN_DEFAULTS_STATE, AlarmDataSource.DEFAULT_STATE_UNSET);
                    values.put(AlarmDbHelper.COLUMN_DEFAULTS_HOURS, 9);
                    values.put(AlarmDbHelper.COLUMN_DEFAULTS_MINUTES, 0);
                    database.insert(AlarmDbHelper.TABLE_DEFAULTS, null, values);

                    database.delete(AlarmDbHelper.TABLE_DAY, null, null);

                    recreate();
                    return;
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // The action bar home/up action should open or close the drawer.
        // ActionBarDrawerToggle will take care of this.
        if (drawerToggle.onOptionsItemSelected(item)) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * When using the ActionBarDrawerToggle, you must call it during onPostCreate() and onConfigurationChanged()...
     */

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        drawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // Pass any configuration change to the drawer toggls
        drawerToggle.onConfigurationChanged(newConfig);
    }

    /**
     * Fragment that appears in the "content_frame"
     */
    public static class CalendarFragment extends Fragment {

        private RecyclerView recyclerView;
        private CalendarAdapter adapter;
        private RecyclerView.LayoutManager layoutManager;

        public CalendarFragment() {
            // Empty constructor required for fragment subclasses
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_calendar, container, false);

            recyclerView = (RecyclerView) rootView.findViewById(R.id.calendar_recycler_view);

            // use a linear layout manager
            layoutManager = new LinearLayoutManager(getActivity());
            recyclerView.setLayoutManager(layoutManager);

            // specify an adapter
            CalendarActivity calendarActivity = (CalendarActivity) getActivity();
            adapter = new CalendarAdapter(calendarActivity);
            recyclerView.setAdapter(adapter);

            // item separator
            recyclerView.addItemDecoration(new SimpleDividerItemDecoration(getActivity()));

            return rootView;
        }

        @Override
        public void onResume() {
            super.onResume();
            adapter.onResume();
        }
    }

}