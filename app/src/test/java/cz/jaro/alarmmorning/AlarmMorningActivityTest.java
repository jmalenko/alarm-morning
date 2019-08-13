package cz.jaro.alarmmorning;

import android.app.Application;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.preference.PreferenceManager;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.google.android.material.navigation.NavigationView;

import org.junit.Assert;
import org.junit.Test;
import org.robolectric.Robolectric;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowActivity;

import java.util.Calendar;
import java.util.Locale;

import androidx.recyclerview.widget.RecyclerView;
import cz.jaro.alarmmorning.model.AlarmDataSource;
import cz.jaro.alarmmorning.model.Defaults;
import cz.jaro.alarmmorning.wizard.Wizard;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertTrue;

/**
 * Test navigation.
 */
public class AlarmMorningActivityTest extends FixedTimeTest {

    /**
     * Set the locale as default locale and in the context configuration. Setting these locales for localized tests is necessary, because the Robolectric {@link
     * Config} qualifier will use the language only for resolving resource.
     *
     * @param context  The android context
     * @param language The language of the locale
     * @param country  The country of the locale
     */
    public static void setLocale(Context context, String language, String country) {
        Locale locale = new Locale(language, country);
        Locale.setDefault(locale);
        // update locale for app resources
        Resources res = context.getResources();
        Configuration config = res.getConfiguration();
        config.locale = locale;
        res.updateConfiguration(config, res.getDisplayMetrics());
    }

    public static void saveWizardPreference(boolean wizardPreference) {
        Context context = AlarmMorningApplication.getAppContext();

        // Set to default
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean(Wizard.PREF_WIZARD, wizardPreference);
        editor.commit();
    }

    @Test
    public void start_first_to_Wizard() {
        saveWizardPreference(Wizard.PREF_WIZARD_DEFAULT);

        AlarmMorningActivity activity = Robolectric.setupActivity(AlarmMorningActivity.class);
        ShadowActivity shadowActivity = Shadows.shadowOf(activity);

        Intent intent = shadowActivity.peekNextStartedActivity();
        assertThat(intent.getComponent().getClassName()).isEqualTo(Wizard.class.getName());
    }

    @Test
    public void start_second_to_Calendar() {
        saveWizardPreference(!Wizard.PREF_WIZARD_DEFAULT);

        AlarmMorningActivity activity = Robolectric.setupActivity(AlarmMorningActivity.class);

        Fragment fragment = activity.getFragmentManager().findFragmentById(R.id.content_frame);
        assertTrue(fragment instanceof CalendarFragment);
    }

    @Test
    public void menu_startDefaultsActivity() {
        saveWizardPreference(!Wizard.PREF_WIZARD_DEFAULT);

        AlarmMorningActivity activity = Robolectric.setupActivity(AlarmMorningActivity.class);
        ShadowActivity shadowActivity = Shadows.shadowOf(activity);

        shadowActivity.clickMenuItem(R.id.navigation_defaults);

        Intent intent = shadowActivity.peekNextStartedActivity();
        assertThat(intent.getComponent().getClassName()).isEqualTo(DefaultsActivity.class.getName());
    }

    @Test
    public void menu_startSettingsActivity() {
        saveWizardPreference(!Wizard.PREF_WIZARD_DEFAULT);

        AlarmMorningActivity activity = Robolectric.setupActivity(AlarmMorningActivity.class);
        ShadowActivity shadowActivity = Shadows.shadowOf(activity);

        shadowActivity.clickMenuItem(R.id.navigation_settings);

        Intent intent = shadowActivity.peekNextStartedActivity();
        assertThat(intent.getComponent().getClassName()).isEqualTo(SettingsActivity.class.getName());
    }

    @Test
    public void menu_startWebsiteActivity() {
        saveWizardPreference(!Wizard.PREF_WIZARD_DEFAULT);

        AlarmMorningActivity activity = Robolectric.setupActivity(AlarmMorningActivity.class);
        ShadowActivity shadowActivity = Shadows.shadowOf(activity);

        shadowActivity.clickMenuItem(R.id.navigation_website);

        Intent intent = shadowActivity.peekNextStartedActivity();
        assertThat(intent.getAction()).isEqualTo(Intent.ACTION_VIEW);
    }

    @Test
    public void menu_startUserGuideActivity() {
        saveWizardPreference(!Wizard.PREF_WIZARD_DEFAULT);

        AlarmMorningActivity activity = Robolectric.setupActivity(AlarmMorningActivity.class);
        ShadowActivity shadowActivity = Shadows.shadowOf(activity);

        shadowActivity.clickMenuItem(R.id.navigation_user_guide);

        Intent intent = shadowActivity.peekNextStartedActivity();
        assertThat(intent.getAction()).isEqualTo(Intent.ACTION_VIEW);
    }

    @Test
    public void menu_startChangeLogActivity() {
        saveWizardPreference(!Wizard.PREF_WIZARD_DEFAULT);

        AlarmMorningActivity activity = Robolectric.setupActivity(AlarmMorningActivity.class);
        ShadowActivity shadowActivity = Shadows.shadowOf(activity);

        shadowActivity.clickMenuItem(R.id.navigation_change_log);

        Intent intent = shadowActivity.peekNextStartedActivity();
        assertThat(intent.getAction()).isEqualTo(Intent.ACTION_VIEW);
    }

    @Test
    public void menu_startReportBugActivity() {
        saveWizardPreference(!Wizard.PREF_WIZARD_DEFAULT);

        AlarmMorningActivity activity = Robolectric.setupActivity(AlarmMorningActivity.class);
        ShadowActivity shadowActivity = Shadows.shadowOf(activity);

        shadowActivity.clickMenuItem(R.id.navigation_report_bug);

        Intent intent = shadowActivity.peekNextStartedActivity();
        assertThat(intent.getAction()).isEqualTo(Intent.ACTION_VIEW);
    }

    @Test
    public void menu_visibleTranslate_translatedLangugeAndCountry() {
        saveWizardPreference(!Wizard.PREF_WIZARD_DEFAULT);

        Context context = RuntimeEnvironment.application.getApplicationContext();
        setLocale(context, "en", "US");

        AlarmMorningActivity activity = Robolectric.setupActivity(AlarmMorningActivity.class);

        NavigationView mNavigationView = activity.findViewById(R.id.left_drawer);
        MenuItem menuItem = mNavigationView.getMenu().findItem(R.id.navigation_translate);

        assertThat(menuItem.isVisible()).isEqualTo(false);
    }

    @Test
    public void menu_visibleTranslate_translatedLangugeOnly() {
        saveWizardPreference(!Wizard.PREF_WIZARD_DEFAULT);

        Context context = RuntimeEnvironment.application.getApplicationContext();
        setLocale(context, "en", "GB");

        AlarmMorningActivity activity = Robolectric.setupActivity(AlarmMorningActivity.class);

        NavigationView mNavigationView = activity.findViewById(R.id.left_drawer);
        MenuItem menuItem = mNavigationView.getMenu().findItem(R.id.navigation_translate);

        assertThat(menuItem.isVisible()).isEqualTo(true);

        CharSequence title = menuItem.getTitle();
        CharSequence language = title.subSequence(title.length() - 14, title.length());
        assertThat(language).isEqualTo("United Kingdom");
    }

    @Test
    public void menu_visibleTranslate_not_translated() {
        saveWizardPreference(!Wizard.PREF_WIZARD_DEFAULT);

        Context context = RuntimeEnvironment.application.getApplicationContext();
        setLocale(context, "sk", "SK");

        AlarmMorningActivity activity = Robolectric.setupActivity(AlarmMorningActivity.class);

        NavigationView mNavigationView = activity.findViewById(R.id.left_drawer);
        MenuItem menuItem = mNavigationView.getMenu().findItem(R.id.navigation_translate);

        assertThat(menuItem.isVisible()).isEqualTo(true);

        CharSequence title = menuItem.getTitle();
        CharSequence language = title.subSequence(title.length() - 10, title.length());
        assertThat(language).isEqualTo("Slovenčina");
    }

    @Test
    public void menu_startTranslateActivity() {
        saveWizardPreference(!Wizard.PREF_WIZARD_DEFAULT);

        AlarmMorningActivity activity = Robolectric.setupActivity(AlarmMorningActivity.class);
        ShadowActivity shadowActivity = Shadows.shadowOf(activity);

        shadowActivity.clickMenuItem(R.id.navigation_translate);

        Intent intent = shadowActivity.peekNextStartedActivity();
        assertThat(intent.getAction()).isEqualTo(Intent.ACTION_VIEW);
    }

    @Test
    public void menu_startRateActivity() {
        saveWizardPreference(!Wizard.PREF_WIZARD_DEFAULT);

        AlarmMorningActivity activity = Robolectric.setupActivity(AlarmMorningActivity.class);
        ShadowActivity shadowActivity = Shadows.shadowOf(activity);

        shadowActivity.clickMenuItem(R.id.navigation_rate);

        Intent intent = shadowActivity.peekNextStartedActivity();
        assertThat(intent.getAction()).isEqualTo(Intent.ACTION_VIEW);
    }

    @Test
    public void menu_startDonateActivity() {
        saveWizardPreference(!Wizard.PREF_WIZARD_DEFAULT);

        AlarmMorningActivity activity = Robolectric.setupActivity(AlarmMorningActivity.class);
        ShadowActivity shadowActivity = Shadows.shadowOf(activity);

        shadowActivity.clickMenuItem(R.id.navigation_donate);

        Intent intent = shadowActivity.peekNextStartedActivity();
        assertThat(intent.getAction()).isEqualTo(Intent.ACTION_VIEW);
    }

    @Test
    @Config(qualifiers = "en")
    public void resourceLanguage_en() {
        Application application = RuntimeEnvironment.application;
        assertThat(application.getString(R.string.app_name)).isEqualTo("Alarm Morning");
    }

    @Test
    @Config(qualifiers = "cs")
    public void resourceLanguage_cs() {
        Application application = RuntimeEnvironment.application;
        assertThat(application.getString(R.string.app_name)).isEqualTo("Budík");
    }

    @Test
    public void set_locale_en() {
        DefaultsActivity activity = Robolectric.setupActivity(DefaultsActivity.class);

        setLocale(activity, "en", "US");

        Resources resources = activity.getResources();
        Configuration configuration = resources.getConfiguration();
        Assert.assertThat(configuration.locale.toString(), is("en_US"));
    }

    @Test
    public void set_locale_cs() {
        DefaultsActivity activity = Robolectric.setupActivity(DefaultsActivity.class);

        setLocale(activity, "cs", "CZ");

        Resources resources = activity.getResources();
        Configuration configuration = resources.getConfiguration();
        Assert.assertThat(configuration.locale.toString(), is("cs_CZ"));
    }

    @Test
    public void first_day_of_week_en() {
        Context context = RuntimeEnvironment.application.getApplicationContext();
        setLocale(context, "en", "US");

        DefaultsActivity activity = Robolectric.setupActivity(DefaultsActivity.class);

        Defaults defaults = activity.loadPosition(0);

        Assert.assertThat(defaults.getDayOfWeek(), is(Calendar.SUNDAY));
    }

    @Test
    public void first_day_of_week_cs() {
        Context context = RuntimeEnvironment.application.getApplicationContext();
        setLocale(context, "cs", "CZ");

        DefaultsActivity activity = Robolectric.setupActivity(DefaultsActivity.class);

        Defaults defaults = activity.loadPosition(0);

        Assert.assertThat(defaults.getDayOfWeek(), is(Calendar.MONDAY));
    }

    @Test
    public void defaults_ordering_en() {
        Context context = RuntimeEnvironment.application.getApplicationContext();
        setLocale(context, "en", "US");

        DefaultsActivity activity = Robolectric.setupActivity(DefaultsActivity.class);

        Resources res = activity.getResources();

        RecyclerView recyclerView = activity.findViewById(R.id.defaults_recycler_view);

        // XXX Workaround Robolectric - RecyclerView needs to be measured and laid out manually in Robolectric.
        // Source: http://stackoverflow.com/questions/27052866/android-robolectric-click-recyclerview-item
        recyclerView.measure(0, 0);
        recyclerView.layout(0, 0, 100, 10000);

        assertThat(recyclerView.getChildCount()).isEqualTo(AlarmDataSource.allDaysOfWeek.length);

        for (int position = 0; position < recyclerView.getChildCount(); position++) {
            View item = recyclerView.getChildAt(position);
            TextView textDayOfWeek = item.findViewById(R.id.textDayOfWeek);

            if (position == 0)
                assertThat(textDayOfWeek.getText()).isEqualTo("Sun");

            String dayOfWeekText = Localization.dayOfWeekToStringShort(res, AlarmDataSource.allDaysOfWeek[position]); // week starts with Sunday

            assertThat(Integer.toString(position) + textDayOfWeek.getText()).isEqualTo(Integer.toString(position) + dayOfWeekText);
        }
    }

    @Test
    public void defaults_ordering_cs() {
        Context context = RuntimeEnvironment.application.getApplicationContext();
        setLocale(context, "cs", "CZ");

        DefaultsActivity activity = Robolectric.setupActivity(DefaultsActivity.class);

        Resources res = activity.getResources();

        RecyclerView recyclerView = activity.findViewById(R.id.defaults_recycler_view);

        // XXX Workaround Robolectric - RecyclerView needs to be measured and laid out manually in Robolectric.
        // Source: http://stackoverflow.com/questions/27052866/android-robolectric-click-recyclerview-item
        recyclerView.measure(0, 0);
        recyclerView.layout(0, 0, 100, 10000);

        assertThat(recyclerView.getChildCount()).isEqualTo(AlarmDataSource.allDaysOfWeek.length);

        for (int position = 0; position < recyclerView.getChildCount(); position++) {
            View item = recyclerView.getChildAt(position);
            TextView textDayOfWeek = item.findViewById(R.id.textDayOfWeek);

            if (position == 0)
                assertThat(textDayOfWeek.getText()).isEqualTo("Po");

            String dayOfWeekText = Localization.dayOfWeekToStringShort(res, AlarmDataSource.allDaysOfWeek[(position + 1) % AlarmDataSource.allDaysOfWeek.length]); // week starts with Monday

            assertThat(Integer.toString(position) + textDayOfWeek.getText()).isEqualTo(position + dayOfWeekText);
        }
    }

}
