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

import android.app.Fragment;
import android.app.FragmentManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;


public class AlarmMorningActivity extends AppCompatActivity implements ActivityInterface {

    private static final String TAG = AlarmMorningActivity.class.getSimpleName();

    public static final String ACTION_DISMISS_BEFORE_RINGING = "DISMISS_BEFORE_RINGING";
    public static final String ACTION_UPDATE_TODAY = "UPDATE_TODAY";

    private DrawerLayout mDrawerLayout;
    private NavigationView mNavigationView;
    private ActionBarDrawerToggle mDrawerToggle;
    private Fragment mFragment;

    CharSequence fragmentTitle;

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

            if (mFragment instanceof CalendarFragment) {
                CalendarFragment calendarFragment = (CalendarFragment) mFragment;
                if (action.equals(ACTION_DISMISS_BEFORE_RINGING)) {
                    calendarFragment.onDismissBeforeRinging();
                } else if (action.equals(ACTION_UPDATE_TODAY)) {
                    calendarFragment.onAlarmTimeOfEarlyDismissedAlarm();
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calendar);

        // Set a Toolbar to replace the ActionBar.
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ActionBar ab = getSupportActionBar();
        ab.setDisplayHomeAsUpEnabled(true);
        ab.setDisplayShowHomeEnabled(false);
        ab.setHomeButtonEnabled(true);

        mNavigationView = (NavigationView) findViewById(R.id.left_drawer);
        setupDrawerContent(mNavigationView);

        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, R.string.drawer_open, R.string.drawer_close) {
            @Override
            public void onDrawerClosed(View view) {
                getSupportActionBar().setTitle(fragmentTitle);
            }

            @Override
            public void onDrawerOpened(View drawerView) {
                getSupportActionBar().setTitle(R.string.app_name);
            }
        };
        mDrawerLayout.setDrawerListener(mDrawerToggle);

        // handler for time nad timezone change
        registerReceiver(timeChangedReceiver, s_intentFilter);

        // handler for local events
        bManager = LocalBroadcastManager.getInstance(this);
        bManager.registerReceiver(bReceiver, b_intentFilter);

        if (savedInstanceState == null) {
            mFragment = new CalendarFragment();
            getFragmentManager().beginTransaction().replace(R.id.content_frame, mFragment).commit();
        }
    }

    private void setupDrawerContent(NavigationView navigationView) {
        navigationView.setNavigationItemSelectedListener(
                new NavigationView.OnNavigationItemSelectedListener() {
                    @Override
                    public boolean onNavigationItemSelected(MenuItem menuItem) {
                        selectDrawerItem(menuItem);
                        return true;
                    }
                });
    }

    public void selectDrawerItem(MenuItem menuItem) {
        android.app.Fragment fragment2 = null;
        Class fragmentClass;

        switch (menuItem.getItemId()) {
            case R.id.navigation_calendar:
                fragmentClass = CalendarFragment.class;
                break;
            case R.id.navigation_defaults:
                fragmentClass = DefaultsFragment.class;
                break;
            case R.id.navigation_settings:
                fragmentClass = SettingsFragment.class;
                break;
            case R.id.navigation_website:
                String url = "https://github.com/jmalenko/alarm-morning/wiki";
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                startActivity(browserIntent);
                return;
            default:
                throw new IllegalArgumentException();
        }

        try {
            fragment2 = (android.app.Fragment) fragmentClass.newInstance();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }

        // Insert the mFragment by replacing any existing mFragment
        getFragmentManager().beginTransaction().replace(R.id.content_frame, fragment2).commit();

        // Highlight the selected item, update the title, and close the drawer
        menuItem.setChecked(true); // TODO Uncheck previous menu item
        setTitleX(menuItem.getTitle());
        closeNavigationDrawer();
    }

    private void setTitleX(CharSequence fragmentTitle) {
        this.fragmentTitle = fragmentTitle;
        getSupportActionBar().setTitle(fragmentTitle);
    }

    @Override
    public void onBackPressed() {
        if (isNavigationDrawerOpen())
            closeNavigationDrawer();
        else
            super.onBackPressed();
    }

    public void closeNavigationDrawer() {
        mDrawerLayout.closeDrawer(mNavigationView);
    }

    public void openNavigationDrawer() {
        mDrawerLayout.openDrawer(mNavigationView);
    }

    public boolean isNavigationDrawerOpen() {
        return mDrawerLayout.isDrawerOpen(mNavigationView);

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

            if (mFragment instanceof CalendarFragment) {
                CalendarFragment calendarFragment = (CalendarFragment) mFragment;
                calendarFragment.adapter.onTimeOrTimeZoneChange();
            }

        }
    };

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                // The action bar home/up action should open or close the drawer.
                if (isNavigationDrawerOpen())
                    closeNavigationDrawer();
                else
                    openNavigationDrawer();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    // TODO Item for Settings remains black when going toSettings and then to Calendar

    // When using the ActionBarDrawerToggle, you must call it during onPostCreate() and onConfigurationChanged()...

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        mDrawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // Pass any configuration change to the drawer toggls
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    public Context getContextI() {
        return this;
    }

    @Override
    public FragmentManager getFragmentManagerI() {
        return getFragmentManager();
    }

    @Override
    public Resources getResourcesI() {
        return getResources();
    }
}

interface ActivityInterface {

    Context getContextI();

    FragmentManager getFragmentManagerI();

    Resources getResourcesI();

}