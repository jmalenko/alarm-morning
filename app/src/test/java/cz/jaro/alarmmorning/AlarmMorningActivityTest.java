package cz.jaro.alarmmorning;

import android.content.Intent;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowActivity;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test navigation.
 */
@RunWith(RobolectricGradleTestRunner.class)
@Config(constants = BuildConfig.class, manifest = "app/src/main/AndroidManifest.xml", sdk = 21)
public class AlarmMorningActivityTest {

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

}
