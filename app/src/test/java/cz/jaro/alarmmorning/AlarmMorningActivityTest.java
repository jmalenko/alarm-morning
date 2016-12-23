package cz.jaro.alarmmorning;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.preference.PreferenceManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowActivity;

import java.util.Locale;

import cz.jaro.alarmmorning.model.AlarmDataSource;
import cz.jaro.alarmmorning.wizard.Wizard;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test navigation.
 */
@RunWith(RobolectricGradleTestRunner.class)
@Config(constants = BuildConfig.class, manifest = "app/src/main/AndroidManifest.xml", sdk = 21)
public class AlarmMorningActivityTest {

    public static final String TEST_PREFERENCE_NAME = "test_preference_name";

    public static void setLocale(Activity activity, String language, String country) {
        Locale locale = new Locale(language, country);
        // update locale for date formatters
        Locale.setDefault(locale);
        // update locale for app resources
        Resources res = activity.getResources();
        Configuration config = res.getConfiguration();
        config.locale = locale;
        res.updateConfiguration(config, res.getDisplayMetrics());
    }

    @Test
    public void menu_startCalendar_on_first_start() {
        AlarmMorningActivity activity = Robolectric.setupActivity(AlarmMorningActivity.class);
        ShadowActivity shadowActivity = Shadows.shadowOf(activity);

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(activity.getBaseContext());
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean(Wizard.PREF_WIZARD, Wizard.PREF_WIZARD_DEFAULT);
        editor.commit();

        Intent intent = shadowActivity.peekNextStartedActivity();
        assertThat(intent.getComponent().getClassName()).isEqualTo(Wizard.class.getName());
    }

    // TODO Fix test
/*
    @Test
    public void menu_startCalendar_on_second_start() {
//        Context context = RuntimeEnvironment.application.getApplicationContext();
//        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
//        SharedPreferences.Editor editor = preferences.edit();
//        editor.putBoolean(Wizard.PREF_WIZARD, !Wizard.PREF_WIZARD_DEFAULT);
//        editor.commit();

//        SharedPreferences preferences = RuntimeEnvironment.application.getSharedPreferences(TEST_PREFERENCE_NAME, Context.MODE_PRIVATE);
//        preferences.edit().putBoolean(Wizard.PREF_WIZARD, !Wizard.PREF_WIZARD_DEFAULT).commit();


//        Context context = RuntimeEnvironment.application.getApplicationContext();
//        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
//        SharedPreferences preferences = RuntimeEnvironment.application.getSharedPreferences(TEST_PREFERENCE_NAME, Context.MODE_PRIVATE);
//        preferences.edit().putBoolean(Wizard.PREF_WIZARD, !Wizard.PREF_WIZARD_DEFAULT).commit();


//        Context context = RuntimeEnvironment.application.getApplicationContext();
//        SharedPreferences preferences = ShadowPreferenceManager.getDefaultSharedPreferences(context);
//        preferences.edit().putBoolean(Wizard.PREF_WIZARD, !Wizard.PREF_WIZARD_DEFAULT).commit();


        // Create the activity - this will call onCreate()
//        activity=actController.create().get();

        AlarmMorningActivity activity = Robolectric.setupActivity(AlarmMorningActivity.class);
//        AlarmMorningActivity activity = Robolectric.buildActivity(AlarmMorningActivity.class).create().start().resume().get();
        ShadowActivity shadowActivity = Shadows.shadowOf(activity);

        Intent intent = shadowActivity.peekNextStartedActivity();
        assertThat(intent).isNotNull();
        assertThat(intent.getComponent()).isNotNull();
        assertThat(intent.getComponent().getClassName()).isEqualTo(AlarmMorningActivity.class.getName());

//        mHomeScreenActivity = Robolectric.buildActivity(HomeScreenActivity.class).create().get(); // start HomeScreenActivity, call through to onCreate()
//        mHomeScreenActivity = Robolectric.buildActivity(HomeScreenActivity.class).create().start().resume().get(); // start HomeScreenActivity, call through to onCreate()

        Fragment fragment = activity.getFragmentManager().findFragmentById(R.id.calendar_recycler_view);
        assertThat(fragment).isNotNull();
    }

    @Test
    public void menu_startDefaultsActivity() {
        AlarmMorningActivity activity = Robolectric.setupActivity(AlarmMorningActivity.class);
        ShadowActivity shadowActivity = Shadows.shadowOf(activity);

        shadowActivity.clickMenuItem(R.id.navigation_defaults);

        Intent intent = shadowActivity.peekNextStartedActivity();
        assertThat(intent.getComponent().getClassName()).isEqualTo(DefaultsActivity.class.getName());
    }

    @Test
    public void menu_startSettingsActivity() {
        AlarmMorningActivity activity = Robolectric.setupActivity(AlarmMorningActivity.class);
        ShadowActivity shadowActivity = Shadows.shadowOf(activity);

        shadowActivity.clickMenuItem(R.id.navigation_settings);

        Intent intent = shadowActivity.peekNextStartedActivity();
        assertThat(intent.getComponent().getClassName()).isEqualTo(SettingsActivity.class.getName());
    }

    @Test
    public void menu_startWebsiteActivity() {
        AlarmMorningActivity activity = Robolectric.setupActivity(AlarmMorningActivity.class);
        ShadowActivity shadowActivity = Shadows.shadowOf(activity);

        shadowActivity.clickMenuItem(R.id.navigation_website);

        Intent intent = shadowActivity.peekNextStartedActivity();
        assertThat(intent.getAction()).isEqualTo(Intent.ACTION_VIEW);
    }
*/
    @Test
    public void defaults_ordering_en() {
        DefaultsActivity activity = Robolectric.setupActivity(DefaultsActivity.class);
        ShadowActivity shadowActivity = Shadows.shadowOf(activity);

        setLocale(activity, "en", "US");
        Resources res = activity.getResources();

        RecyclerView recyclerView = (RecyclerView) shadowActivity.findViewById(R.id.defaults_recycler_view);

        // Hack: RecyclerView needs to be measured and layed out manually in Robolectric.
        // Source: http://stackoverflow.com/questions/27052866/android-robolectric-click-recyclerview-item
        recyclerView.measure(0, 0);
        recyclerView.layout(0, 0, 100, 10000);

        assertThat(recyclerView.getChildCount()).isEqualTo(AlarmDataSource.allDaysOfWeek.length);

        for (int position = 0; position < recyclerView.getChildCount(); position++) {
            View item = recyclerView.getChildAt(position);
            TextView textDayOfWeek = (TextView) item.findViewById(R.id.textDayOfWeek);

            // TODO Fix test
//            if (position == 0)
//                assertThat(textDayOfWeek.getText()).isEqualTo("Sun");

            String dayOfWeekText = Localization.dayOfWeekToStringShort(res, AlarmDataSource.allDaysOfWeek[position]); // week starts with Sunday

            // TODO Fix test
//            assertThat(new Integer(position).toString() + textDayOfWeek.getText()).isEqualTo(new Integer(position).toString() + dayOfWeekText);
        }
    }

    @Test
    public void defaults_ordering_cs() {
        DefaultsActivity activity = Robolectric.setupActivity(DefaultsActivity.class);
        ShadowActivity shadowActivity = Shadows.shadowOf(activity);

        setLocale(activity, "cs", "CZ");
        Resources res = activity.getResources();

        RecyclerView recyclerView = (RecyclerView) shadowActivity.findViewById(R.id.defaults_recycler_view);

        // Hack: RecyclerView needs to be measured and layed out manually in Robolectric.
        // Source: http://stackoverflow.com/questions/27052866/android-robolectric-click-recyclerview-item
        recyclerView.measure(0, 0);
        recyclerView.layout(0, 0, 100, 10000);

        assertThat(recyclerView.getChildCount()).isEqualTo(AlarmDataSource.allDaysOfWeek.length);

        for (int position = 0; position < recyclerView.getChildCount(); position++) {
            View item = recyclerView.getChildAt(position);
            TextView textDayOfWeek = (TextView) item.findViewById(R.id.textDayOfWeek);

            if (position == 0)
                assertThat(textDayOfWeek.getText()).isEqualTo("Po");

            String dayOfWeekText = Localization.dayOfWeekToStringShort(res, AlarmDataSource.allDaysOfWeek[(position + 1) % AlarmDataSource.allDaysOfWeek.length]); // week starts with Monday

            assertThat(new Integer(position).toString() + textDayOfWeek.getText()).isEqualTo(new Integer(position).toString() + dayOfWeekText);
        }
    }


}
