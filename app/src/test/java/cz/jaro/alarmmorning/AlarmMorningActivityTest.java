package cz.jaro.alarmmorning;

import android.app.Application;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.preference.PreferenceManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

import org.junit.Assert;
import org.junit.Test;
import org.robolectric.Robolectric;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowActivity;

import java.util.Calendar;
import java.util.Locale;

import cz.jaro.alarmmorning.model.AlarmDataSource;
import cz.jaro.alarmmorning.model.Defaults;
import cz.jaro.alarmmorning.wizard.Wizard;

import static junit.framework.Assert.assertTrue;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.core.Is.is;

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

    static void saveWizardPreference(boolean wizardPreference) {
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
        DefaultsActivity activity = Robolectric.setupActivity(DefaultsActivity.class);

        setLocale(activity, "en", "US");

        Defaults defaults = activity.loadPosition(0);

        // TODO Fix test
        Assert.assertThat(defaults.getDayOfWeek(), is(Calendar.SUNDAY));
    }

    @Test
    public void first_day_of_week_cs() {
        DefaultsActivity activity = Robolectric.setupActivity(DefaultsActivity.class);

        setLocale(activity, "cs", "CZ");

        Defaults defaults = activity.loadPosition(0);

        // TODO Fix test
        Assert.assertThat(defaults.getDayOfWeek(), is(Calendar.MONDAY));
    }

    @Test
    @Config(qualifiers = "en")
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
            if (position == 0)
                assertThat(textDayOfWeek.getText()).isEqualTo("Sun");

            String dayOfWeekText = Localization.dayOfWeekToStringShort(res, AlarmDataSource.allDaysOfWeek[position]); // week starts with Sunday

            assertThat(new Integer(position).toString() + textDayOfWeek.getText()).isEqualTo(new Integer(position).toString() + dayOfWeekText);
        }
    }

    @Test
    @Config(qualifiers = "cs")
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

            // TODO Fix test
            if (position == 0)
                assertThat(textDayOfWeek.getText()).isEqualTo("Po");

            String dayOfWeekText = Localization.dayOfWeekToStringShort(res, AlarmDataSource.allDaysOfWeek[(position + 1) % AlarmDataSource.allDaysOfWeek.length]); // week starts with Monday

            assertThat(Integer.toString(position) + textDayOfWeek.getText()).isEqualTo(Integer.toString(position) + dayOfWeekText);
        }
    }

}
