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
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;

import com.google.firebase.analytics.FirebaseAnalytics;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import cz.jaro.alarmmorning.model.AppAlarm;
import cz.jaro.alarmmorning.model.Day;
import cz.jaro.alarmmorning.model.OneTimeAlarm;
import cz.jaro.alarmmorning.nighttimebell.CustomAlarmTone;
import cz.jaro.alarmmorning.wizard.Wizard;

public class AlarmMorningActivity extends AppCompatActivity {

    private static final String TAG = AlarmMorningActivity.class.getSimpleName();

    public static final String ACTION_ALARM_SET = "ALARM_SET";
    public static final String ACTION_DISMISS_BEFORE_RINGING = "DISMISS_BEFORE_RINGING";
    public static final String ACTION_ALARM_TIME_OF_EARLY_DISMISSED_ALARM = "ACTION_ALARM_TIME_OF_EARLY_DISMISSED_ALARM";
    public static final String ACTION_RING = "RING";
    public static final String ACTION_DISMISS = "DISMISS";
    public static final String ACTION_SNOOZE = "SNOOZE";
    public static final String ACTION_CANCEL = "CANCEL";

    public static final String EVENT_MODIFY_DAY_ALARM = "EVENT_MODIFY_DAY_ALARM";
    public static final String EVENT_CREATE_ONE_TIME_ALARM = "EVENT_CREATE_ONE_TIME_ALARM";
    public static final String EVENT_DELETE_ONE_TIME_ALARM = "EVENT_DELETE_ONE_TIME_ALARM";
    public static final String EVENT_MODIFY_ONE_TIME_ALARM_DATETIME = "EVENT_MODIFY_ONE_TIME_ALARM_DATETIME";
    public static final String EVENT_MODIFY_ONE_TIME_ALARM_NAME = "EVENT_MODIFY_ONE_TIME_ALARM_NAME";

    public static final String EXTRA_ALARM_TYPE = "EXTRA_ALARM_TYPE";
    public static final String EXTRA_ALARM_ID = "EXTRA_ALARM_ID";

    public static final String URL_WEBSITE = "https://github.com/jmalenko/alarm-morning/wiki";
    public static final String URL_USER_GUIDE = "https://github.com/jmalenko/alarm-morning/wiki/User-Guide";
    public static final String URL_CHANGE_LOG = "https://github.com/jmalenko/alarm-morning/wiki/Change-Log";
    public static final String URL_REPORT_BUG = "https://github.com/jmalenko/alarm-morning/issues";
    public static final String URL_TRANSLATE = "https://crowdin.com/project/alarm-morning";
    public static final String URL_DONATE = "https://www.paypal.me/jaromirmalenko/10usd";

    // TODO Set programmatically (when that becomes possible in Android). For now, keep consistent with res/values directories. https://stackoverflow.com/questions/34797956/android-programmatically-check-if-app-is-localized-for-a-language
    private static final List<String> TRANSLATIONS = Arrays.asList(
            "en_US", // default language
            "cs_CZ", // manually updated by author Jaromír Malenko
            /*
            The following translations are updated from Crowdin.com.

            Only the translations that satisfy certain criteria are accepted:
              1. Translated by a human
              2. No syntax errors

            There a problem with translating "Balinese" as displayed in Crowdin website to "ban" Android locale . Currently, the relevant part of Crowdin URLs
            is used. Event after that, testing Balinese in Android is impossible as there is no item Balinese in Android language settings.
            */
            "ban_ID",
            "es_ES",
            "es_CO",
            "mk_rMK",
            "nl_NL",
            "pl_PL",
            "pt_PT",
            "ro_RO",
            "zh_CN",
            "zh_HK",
            "zh_SG",
            "zh_TW"
    );

    public static final int REQUEST_CODE_WIZARD = 1;

    private DrawerLayout mDrawerLayout;
    private NavigationView mNavigationView;
    private ActionBarDrawerToggle mDrawerToggle;
    private Fragment mFragment;

    private CharSequence fragmentTitle;
    private MenuItem lastMenuItem;

    private static final IntentFilter s_intentFilterTime;

    private LocalBroadcastManager bManager;
    private static IntentFilter s_intentFilterInternal;

    boolean consumed; // Whether the touch event ACTION_DOWN was consumed (so we can consume the following event ACTION_UP)

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
        s_intentFilterInternal.addAction(ACTION_CANCEL);
        s_intentFilterInternal.addAction(EVENT_MODIFY_DAY_ALARM);
        s_intentFilterInternal.addAction(EVENT_CREATE_ONE_TIME_ALARM);
        s_intentFilterInternal.addAction(EVENT_DELETE_ONE_TIME_ALARM);
        s_intentFilterInternal.addAction(EVENT_MODIFY_ONE_TIME_ALARM_DATETIME);
        s_intentFilterInternal.addAction(EVENT_MODIFY_ONE_TIME_ALARM_NAME);
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

                // Get the AppAlarm related to the event
                AppAlarm appAlarm = null;

                GlobalManager globalManager = GlobalManager.getInstance();
                String alarmType = intent.getStringExtra(EXTRA_ALARM_TYPE);
                String alarmId = intent.getStringExtra(EXTRA_ALARM_ID);
                if (alarmType != null && alarmId != null)
                    appAlarm = globalManager.load(alarmType, alarmId);

                switch (action) {
                    // Events relevant to current alarm

                    case ACTION_ALARM_SET:
                        calendarFragment.onAlarmSet(appAlarm);
                        break;
                    case ACTION_DISMISS_BEFORE_RINGING:
                        calendarFragment.onDismissBeforeRinging(appAlarm);
                        break;
                    case ACTION_ALARM_TIME_OF_EARLY_DISMISSED_ALARM:
                        calendarFragment.onAlarmTimeOfEarlyDismissedAlarm(appAlarm);
                        break;
                    case ACTION_RING:
                        calendarFragment.onRing(appAlarm);
                        break;
                    case ACTION_DISMISS:
                        calendarFragment.onDismiss(appAlarm);
                        break;
                    case ACTION_SNOOZE:
                        calendarFragment.onSnooze(appAlarm);
                        break;
                    case ACTION_CANCEL:
                        calendarFragment.onCancel(appAlarm);
                        break;

                    // Events relevant to alarm management

                    case EVENT_MODIFY_DAY_ALARM:
                        calendarFragment.onModifyDayAlarm((Day) appAlarm);
                        break;
                    case EVENT_CREATE_ONE_TIME_ALARM:
                        calendarFragment.onCreateOneTimeAlarm((OneTimeAlarm) appAlarm);
                        break;
                    case EVENT_DELETE_ONE_TIME_ALARM:
                        // Note: The alarm is already deleted from the database. Therefore we can only pass the id.
                        calendarFragment.onDeleteOneTimeAlarm(Integer.valueOf(alarmId));
                        break;
                    case EVENT_MODIFY_ONE_TIME_ALARM_DATETIME:
                        calendarFragment.onModifyOneTimeAlarmDateTime((OneTimeAlarm) appAlarm);
                        break;
                    case EVENT_MODIFY_ONE_TIME_ALARM_NAME:
                        calendarFragment.onModifyOneTimeAlarmName((OneTimeAlarm) appAlarm);
                        break;
                    default:
                        throw new IllegalArgumentException("Unexpected argument " + action);
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

        // Possibly install the raws (if not installed yet)
        CustomAlarmTone customAlarmTone = new CustomAlarmTone(this);
        customAlarmTone.install();

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

        // Show Translate menu item only if the user language is not translatedLanguageAndCountry
        Resources resources = getResources();
        MenuItem translateMenuItem = mNavigationView.getMenu().findItem(R.id.navigation_translate);

        Locale locale = Locale.getDefault();
        boolean translatedLanguageAndCountry = TRANSLATIONS.contains(locale.getLanguage() + "_" + locale.getCountry());

        if (translatedLanguageAndCountry) {
            translateMenuItem.setVisible(false);
        } else {
            boolean translatedLanguage = false;
            for (String translation : TRANSLATIONS) {
                String[] localeTranslation = translation.split("_");
                if (locale.getLanguage().equals(localeTranslation[0])) {
                    translatedLanguage = true;
                    break;
                }
            }

            String displayLanguage = Locale.getDefault().getDisplayLanguage();
            String displayCountry = Locale.getDefault().getDisplayCountry();
            String title = translatedLanguage ? resources.getString(R.string.menu_translate2_title, displayLanguage, displayCountry) : resources.getString(R.string.menu_translate1_title, displayLanguage);

            translateMenuItem.setTitle(title);
            translateMenuItem.setVisible(true);
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
            return;
        }

        if (mFragment instanceof CalendarFragment) {
            CalendarFragment calendarFragment = (CalendarFragment) mFragment;
            calendarFragment.refresh();
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
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.day_toolbar_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Analytics analytics = new Analytics(this, Analytics.Event.Click, Analytics.Channel.Activity, Analytics.ChannelName.Calendar);
        String analyticsTarget;

        switch (item.getItemId()) {
            case android.R.id.home:
                // The action bar home/up action should open or close the drawer.
                if (isNavigationDrawerOpen()) {
                    closeNavigationDrawer();

                    analyticsTarget = Analytics.TARGET_MENU_ACTION_CLOSE;
                } else {
                    openNavigationDrawer();

                    analyticsTarget = Analytics.TARGET_MENU_ACTION_OPEN;
                }

                analytics.set(Analytics.Param.Target, analyticsTarget);
                analytics.save();

                return true;

            case R.id.navigation_calendar:
                mFragment = new CalendarFragment();

                // Insert the mFragment by replacing any existing mFragment
                getFragmentManager().beginTransaction().replace(R.id.content_frame, mFragment).commit();

                // Highlight the selected item, update the title, and close the drawer
                highlightMenuItem(item);
                setFragmentTitle(item.getTitle());

                analyticsTarget = Analytics.TARGET_MENU_CALENDAR;

                break;

            case R.id.navigation_defaults:
                Intent defaultsActivityIntent = new Intent(this, DefaultsActivity.class);
                startActivity(defaultsActivityIntent);

                analyticsTarget = Analytics.TARGET_MENU_DEFAULTS;

                break;

            case R.id.navigation_settings:
                Intent settingsActivityIntent = new Intent(this, SettingsActivity.class);
                startActivity(settingsActivityIntent);

                analyticsTarget = Analytics.TARGET_MENU_SETTINGS;

                break;

            case R.id.navigation_website:
                Intent websiteIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(URL_WEBSITE));
                startActivity(websiteIntent);

                analyticsTarget = Analytics.TARGET_MENU_WEBSITE;

                break;

            case R.id.navigation_user_guide:
                Intent userGuideIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(URL_USER_GUIDE));
                startActivity(userGuideIntent);

                analyticsTarget = Analytics.TARGET_MENU_USER_GUIDE;

                break;

            case R.id.navigation_change_log:
                Intent changeLogIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(URL_CHANGE_LOG));
                startActivity(changeLogIntent);

                analyticsTarget = Analytics.TARGET_MENU_CHANGE_LOG;

                break;

            case R.id.navigation_report_bug:
                Intent reportBugIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(URL_REPORT_BUG));
                startActivity(reportBugIntent);

                analyticsTarget = Analytics.TARGET_MENU_REPORT_BUG;

                break;

            case R.id.navigation_translate:
                Intent translateIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(URL_TRANSLATE));
                startActivity(translateIntent);

                analyticsTarget = Analytics.TARGET_MENU_TRANSLATE;

                break;

            case R.id.navigation_rate:
                rateApp();

                analyticsTarget = Analytics.TARGET_MENU_RATE;
                break;

            case R.id.navigation_donate:
                Intent donateIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(URL_DONATE));
                startActivity(donateIntent);

                analyticsTarget = Analytics.TARGET_MENU_DONATE;

                break;

            case R.id.day_add_alarm:
                if (mFragment instanceof CalendarFragment) {
                    CalendarFragment calendarFragment = (CalendarFragment) mFragment;
                    calendarFragment.onAddOneTimeAlarm();
                }

                analyticsTarget = Analytics.TARGET_MENU_ADD_ONE_TIME_ALARM;

                break;

            default:
                return super.onOptionsItemSelected(item);
        }

        closeNavigationDrawer();

        analytics.set(Analytics.Param.Target, analyticsTarget);
        analytics.set(Analytics.Param.Target, analyticsTarget);
        analytics.save();

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

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        // Handles the situation when user is editing the name of a one-time alarm and touches somewhere in the activity

        if (mFragment instanceof CalendarFragment) {
            CalendarFragment calendarFragment = (CalendarFragment) mFragment;

            switch (ev.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    consumed = calendarFragment.onEditNameEnd();
                    if (consumed)
                        return true;
                    break;

                case MotionEvent.ACTION_UP:
                    if (consumed)
                        return true;
                    break;
            }
        }

        return super.dispatchTouchEvent(ev);
    }

    /**
     * Start with rating the app.
     * Determine if the Play Store app is installed on the device.
     */
    public void rateApp() {
        try {
            Intent rateIntent = rateIntentForUrl("market://details?id=");
            startActivity(rateIntent);
            Log.d(TAG, "Started rating in Google Play app");
        } catch (ActivityNotFoundException e) {
            try {
                Intent rateIntent = rateIntentForUrl("amzn://apps/android?p=");
                startActivity(rateIntent);
                Log.d(TAG, "Started rating in Amazon app");
            } catch (ActivityNotFoundException e2) {
                Intent rateIntent = rateIntentForUrl("https://play.google.com/store/apps/details?id=");
                startActivity(rateIntent);
                Log.d(TAG, "Started rating in Google Play website");
            }
        }
    }

    private Intent rateIntentForUrl(String uri) {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri + getPackageName()));
        int flags = Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_ACTIVITY_MULTIPLE_TASK;
        if (Build.VERSION.SDK_INT >= 21) {
            flags |= Intent.FLAG_ACTIVITY_NEW_DOCUMENT;
        } else {
            // noinspection deprecation
            flags |= Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET;
        }
        intent.addFlags(flags);
        return intent;
    }
}
