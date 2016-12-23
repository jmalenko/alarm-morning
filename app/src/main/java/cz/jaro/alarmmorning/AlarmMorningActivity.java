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

import android.app.Activity;
import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
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

import com.google.firebase.analytics.FirebaseAnalytics;

import cz.jaro.alarmmorning.wizard.Wizard;

public class AlarmMorningActivity extends AppCompatActivity {

    private static final String TAG = AlarmMorningActivity.class.getSimpleName();

    public static final String ACTION_ALARM_SET = "ALARM_SET";
    public static final String ACTION_DISMISS_BEFORE_RINGING = "DISMISS_BEFORE_RINGING";
    public static final String ACTION_ALARM_TIME_OF_EARLY_DISMISSED_ALARM = "ACTION_ALARM_TIME_OF_EARLY_DISMISSED_ALARM";
    public static final String ACTION_RING = "RING";
    public static final String ACTION_DISMISS = "DISMISS";
    public static final String ACTION_SNOOZE = "SNOOZE";

    private final static String url = "https://github.com/jmalenko/alarm-morning/wiki";

    public static final int REQUEST_CODE_WIZARD = 1;

    private DrawerLayout mDrawerLayout;
    private NavigationView mNavigationView;
    private ActionBarDrawerToggle mDrawerToggle;
    private Fragment mFragment;

    CharSequence fragmentTitle;
    private MenuItem lastMenuItem;

    private static final IntentFilter s_intentFilterTime;

    LocalBroadcastManager bManager;
    private static IntentFilter s_intentFilterInternal;

    static {
        s_intentFilterTime = new IntentFilter();
        s_intentFilterTime.addAction(Intent.ACTION_TIMEZONE_CHANGED);
        s_intentFilterTime.addAction(Intent.ACTION_TIME_CHANGED);

        s_intentFilterInternal = new IntentFilter();
        s_intentFilterInternal.addAction(ACTION_ALARM_SET);
        s_intentFilterInternal.addAction(ACTION_DISMISS_BEFORE_RINGING);
        s_intentFilterInternal.addAction(ACTION_ALARM_TIME_OF_EARLY_DISMISSED_ALARM);
        s_intentFilterInternal.addAction(ACTION_RING);
        s_intentFilterInternal.addAction(ACTION_DISMISS);
        s_intentFilterInternal.addAction(ACTION_SNOOZE);
    }

    private final BroadcastReceiver timeChangedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "onReceive()");
            Log.i(TAG, "Refreshing view on time or timezone change");

            if (mFragment instanceof CalendarFragment) {
                CalendarFragment calendarFragment = (CalendarFragment) mFragment;
                calendarFragment.onTimeOrTimeZoneChange();
            }
        }
    };

    private BroadcastReceiver bReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d(TAG, "onReceive(action=" + action + ")");

            if (mFragment instanceof CalendarFragment) {
                CalendarFragment calendarFragment = (CalendarFragment) mFragment;
                switch (action) {
                    case ACTION_ALARM_SET:
                        calendarFragment.onAlarmSet();
                        break;
                    case ACTION_DISMISS_BEFORE_RINGING:
                        calendarFragment.onDismissBeforeRinging();
                        break;
                    case ACTION_ALARM_TIME_OF_EARLY_DISMISSED_ALARM:
                        calendarFragment.onAlarmTimeOfEarlyDismissedAlarm();
                        break;
                    case ACTION_RING:
                        calendarFragment.onRing();
                        break;
                    case ACTION_DISMISS:
                        calendarFragment.onDismiss();
                        break;
                    case ACTION_SNOOZE:
                        calendarFragment.onSnooze();
                        break;
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.v(TAG, "onCreate()");
        super.onCreate(savedInstanceState);

        // Possibly run the wizard
        boolean wizardPreference = Wizard.loadWizardFinished(getBaseContext());
        if (!wizardPreference) {
            Intent wizardIntent = new Intent(this, Wizard.class);
            startActivityForResult(wizardIntent, REQUEST_CODE_WIZARD);
        }

        FirebaseAnalytics mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);

        Bundle payload = new Bundle();
        payload.putString(FirebaseAnalytics.Param.VALUE, TAG);
        mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.APP_OPEN, payload);

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
        registerReceiver(timeChangedReceiver, s_intentFilterTime);

        // handler for local events
        bManager = LocalBroadcastManager.getInstance(this);
        bManager.registerReceiver(bReceiver, s_intentFilterInternal);

        if (savedInstanceState == null) {
            mFragment = new CalendarFragment();
            getFragmentManager().beginTransaction().replace(R.id.content_frame, mFragment).commit();

            // Highlight the menu item
            MenuItem calendarMenu = mNavigationView.getMenu().findItem(R.id.navigation_calendar);
            highlightMenuItem(calendarMenu);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.v(TAG, "onActivityResult(requestCode=" + requestCode + ", resultCode=" + resultCode + ")");
        if (resultCode == Activity.RESULT_OK && requestCode == REQUEST_CODE_WIZARD) {
            if (mFragment instanceof CalendarFragment) {
                CalendarFragment calendarFragment = (CalendarFragment) mFragment;
                calendarFragment.refresh();
            }
        }
    }

    @Override
    protected void onResume() {
        Log.v(TAG, "onResume()");
        super.onResume();

        // Redirect to ring activity if ringing
        GlobalManager globalManager = GlobalManager.getInstance();
        if (globalManager.isRinging()) {
            globalManager.startRingingActivity(this);
        }
    }

    private void setupDrawerContent(NavigationView navigationView) {
        navigationView.setNavigationItemSelectedListener(menuItem -> {
            onOptionsItemSelected(menuItem);
            return true;
        });

        MenuItem calendarMenuItem = mNavigationView.getMenu().getItem(0);
        setFragmentTitle(calendarMenuItem.getTitle());
    }

    private void highlightMenuItem(MenuItem menuItem) {
        if (lastMenuItem != null)
            lastMenuItem.setChecked(false);

        lastMenuItem = menuItem;

        lastMenuItem.setChecked(true);
    }

    private void setFragmentTitle(CharSequence fragmentTitle) {
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

            case R.id.navigation_calendar:
                mFragment = new CalendarFragment();

                // Insert the mFragment by replacing any existing mFragment
                getFragmentManager().beginTransaction().replace(R.id.content_frame, mFragment).commit();

                // Highlight the selected item, update the title, and close the drawer
                highlightMenuItem(item);
                setFragmentTitle(item.getTitle());
                break;

            case R.id.navigation_defaults:
                Intent defaultsActivityIntent = new Intent(this, DefaultsActivity.class);
                startActivity(defaultsActivityIntent);
                break;

            case R.id.navigation_settings:
                Intent settingsActivityIntent = new Intent(this, SettingsActivity.class);
                startActivity(settingsActivityIntent);
                break;

            case R.id.navigation_website:
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                startActivity(browserIntent);
                break;

            default:
                return super.onOptionsItemSelected(item);
        }

        closeNavigationDrawer();
        return true;
    }

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
}
