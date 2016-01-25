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
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import cz.jaro.alarmmorning.graphics.SimpleDividerItemDecoration;


public class CalendarActivity extends Activity {

    private static final String TAG = CalendarActivity.class.getSimpleName();

    public static final String ACTION_DISMISS_BEFORE_RINGING = "DISMISS_BEFORE_RINGING";
    public static final String ACTION_UPDATE_TODAY = "UPDATE_TODAY";

    private DrawerLayout drawerLayout;
    private ListView drawerList;
    private ActionBarDrawerToggle drawerToggle;
    private CalendarFragment fragment;

    private static final IntentFilter s_intentFilter;

    LocalBroadcastManager bManager;
    private static IntentFilter b_intentFilter;

    static {
        s_intentFilter = new IntentFilter();
        s_intentFilter.addAction(Intent.ACTION_TIMEZONE_CHANGED);
        s_intentFilter.addAction(Intent.ACTION_TIME_CHANGED);

        b_intentFilter = new IntentFilter();
        b_intentFilter.addAction(ACTION_DISMISS_BEFORE_RINGING);
        b_intentFilter.addAction(ACTION_UPDATE_TODAY);
    }

    private BroadcastReceiver bReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d(TAG, "onReceive() action=" + action);

            if (action.equals(ACTION_DISMISS_BEFORE_RINGING)) {
                fragment.onDismissBeforeRinging();
            } else if (action.equals(ACTION_UPDATE_TODAY)) {
                fragment.onAlarmTimeOfEarlyDismissedAlarm();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calendar);

        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawerList = (ListView) findViewById(R.id.left_drawer);

        // set up the drawer's list view with items and click listener
        String[] menuItems = getResources().getStringArray(R.array.menu_items_array);
        drawerList.setAdapter(new ArrayAdapter<String>(this, R.layout.drawer_list_item, menuItems));
        drawerList.setOnItemClickListener(new DrawerItemClickListener());

        ActionBar ab = getActionBar();
        ab.setDisplayHomeAsUpEnabled(true);
        ab.setDisplayShowHomeEnabled(false);
        ab.setHomeButtonEnabled(true);

        drawerToggle = new ActionBarDrawerToggle(this, drawerLayout, R.string.drawer_open, R.string.drawer_close) {
            @Override
            public void onDrawerClosed(View view) {
                getActionBar().setTitle(R.string.title_activity_calendar);
            }

            @Override
            public void onDrawerOpened(View drawerView) {
                getActionBar().setTitle(R.string.app_name);
            }
        };
        drawerLayout.setDrawerListener(drawerToggle);

        // handler for time nad timezone change
        registerReceiver(timeChangedReceiver, s_intentFilter);

        // handler for local events
        bManager = LocalBroadcastManager.getInstance(this);
        bManager.registerReceiver(bReceiver, b_intentFilter);

        if (savedInstanceState == null) {
            fragment = new CalendarFragment();
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
                    closeNavigationDrawer();
                    return;

                case 1:
                    drawerList.setItemChecked(0, true);
                    closeNavigationDrawer();

                    Intent intent = new Intent(CalendarActivity.this, DefaultsActivity.class);
                    startActivity(intent);
                    return;

                case 2:
                    drawerList.setItemChecked(0, true);
                    closeNavigationDrawer();

                    intent = new Intent(CalendarActivity.this, SettingsActivity.class);
                    startActivity(intent);
                    return;

                case 3:
                    drawerList.setItemChecked(0, true);
                    closeNavigationDrawer();

                    String url = "https://github.com/jmalenko/alarm-morning/wiki";
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    startActivity(browserIntent);
                    return;
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(drawerList)) closeNavigationDrawer();
        else super.onBackPressed();
    }

    public void closeNavigationDrawer() {
        drawerLayout.closeDrawer(drawerList);
    }

    public void openNavigationDrawer() {
        drawerLayout.openDrawer(drawerList);
    }

    public boolean isNavigationDrawerOpen() {
        return drawerLayout.isDrawerOpen(drawerList);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        unregisterReceiver(timeChangedReceiver);
        bManager.unregisterReceiver(bReceiver);
    }

    private final BroadcastReceiver timeChangedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "onReceive()");
            Log.i(TAG, "Refreshing view on time or timezone change");

            fragment.adapter.onTimeOrTimeZoneChange();
        }
    };

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

        private Handler handler = new Handler();
        private Runnable runnable = new Runnable() {
            @Override
            public void run() {
                adapter.onSystemTimeChange();
                handler.postDelayed(this, 1000);
            }
        };

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

            // for disabling the animation on update
            DefaultItemAnimator animator = new DefaultItemAnimator();
            animator.setSupportsChangeAnimations(false);
            recyclerView.setItemAnimator(animator);

            return rootView;
        }

        @Override
        public void onResume() {
            super.onResume();

            // handler for refreshing the content
            handler.postDelayed(runnable, 1000);

            adapter.onResume();
        }

        @Override
        public void onPause() {
            super.onPause();

            handler.removeCallbacks(runnable);
        }

        @Override
        public void onDestroy() {
            super.onDestroy();

            adapter.onDestroy();
        }

        public void onAlarmTimeOfEarlyDismissedAlarm() {
            Log.d(TAG, "onAlarmTimeOfEarlyDismissedAlarm()");
            adapter.notifyItemChanged(0);
        }

        public void onDismissBeforeRinging() {
            Log.d(TAG, "onDismissBeforeRinging()");
            adapter.updatePositionNextAlarm();
            //adapter.notifyDataSetChanged();
        }

    }
}