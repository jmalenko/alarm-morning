package cz.jaro.alarmmorning.model;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowActivity;

import java.util.Calendar;
import java.util.GregorianCalendar;

import cz.jaro.alarmmorning.AlarmMorningActivity;
import cz.jaro.alarmmorning.AlarmMorningActivityTest;
import cz.jaro.alarmmorning.BuildConfig;
import cz.jaro.alarmmorning.GlobalManager;
import cz.jaro.alarmmorning.SettingsActivity;
import cz.jaro.alarmmorning.clock.Clock;

import static cz.jaro.alarmmorning.holiday.HolidayHelperTest.DE;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

/**
 * This tests do not depend on {@link Clock}.
 */
@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class, manifest = "app/src/main/AndroidManifest.xml", sdk = 21)
public class DayTest {

    // February 2016 starts with Monday
    public static final int YEAR = 2016;
    public static final int MONTH = Calendar.FEBRUARY;
    public static final int DAY = 1;
    public static final int DAY_OF_WEEK = Calendar.MONDAY;

    public static final int HOUR_DAY = 8;
    public static final int MINUTE_DAY = 1;

    public static final int HOUR_DEFAULT = 7;
    public static final int MINUTE_DEFAULT = 0;

    public static final int HOLIDAY_YEAR = YEAR;
    public static final int HOLIDAY_MONTH = Calendar.JANUARY;
    public static final int HOLIDAY_DAY = 1;
    public static final int HOLIDAY_DAY_OF_WEEK = Calendar.FRIDAY;

    private Defaults defaults;
    private Day day;

    @Before
    public void before() {
        defaults = new Defaults();
        defaults.setDayOfWeek(DAY_OF_WEEK);
        defaults.setHour(HOUR_DEFAULT);
        defaults.setMinute(MINUTE_DEFAULT);

        day = new Day();
        GregorianCalendar date = new GregorianCalendar(YEAR, MONTH, DAY);
        day.setDate(date);
        day.setHour(HOUR_DAY);
        day.setMinute(MINUTE_DAY);
        day.setDefaults(defaults);
    }

    @After
    public void after() {
        GlobalManager1NextAlarm0NoAlarmTest.resetSingleton(GlobalManager.class, "instance");
    }

    @Test
    public void DefaultDisabledDayRule() {
        defaults.setState(Defaults.STATE_DISABLED);
        day.setState(Day.STATE_RULE);

        assertThat(day.isEnabled(), is(false));
        assertThat(day.getHourX(), is(HOUR_DEFAULT));
        assertThat(day.getMinuteX(), is(MINUTE_DEFAULT));
        assertThat(day.sameAsDefault(), is(true));

        day.reverse();
        assertThat(day.getState(), is(Day.STATE_ENABLED));
    }

    @Test
    public void DefaultDisabledDayEnabled() {
        defaults.setState(Defaults.STATE_DISABLED);
        day.setState(Day.STATE_ENABLED);

        assertThat(day.isEnabled(), is(true));
        assertThat(day.getHourX(), is(HOUR_DAY));
        assertThat(day.getMinuteX(), is(1));
        assertThat(day.sameAsDefault(), is(false));

        day.reverse();
        assertThat(day.getState(), is(Day.STATE_DISABLED));
    }

    @Test
    public void DefaultDisabledDayDisabled() {
        defaults.setState(Defaults.STATE_DISABLED);
        day.setState(Day.STATE_DISABLED);

        assertThat(day.isEnabled(), is(false));
        assertThat(day.getHourX(), is(HOUR_DAY));
        assertThat(day.getMinuteX(), is(1));
        assertThat(day.sameAsDefault(), is(true));

        day.reverse();
        assertThat(day.getState(), is(Day.STATE_ENABLED));
    }

    @Test
    public void DefaultEnabledDayDefault() {
        defaults.setState(Defaults.STATE_ENABLED);
        day.setState(Day.STATE_RULE);

        assertThat(day.isEnabled(), is(true));
        assertThat(day.getHourX(), is(HOUR_DEFAULT));
        assertThat(day.getMinuteX(), is(MINUTE_DEFAULT));
        assertThat(day.sameAsDefault(), is(true));

        day.reverse();
        assertThat(day.getState(), is(Day.STATE_DISABLED));
    }

    @Test
    public void DefaultEnabledDayEnabled() {
        defaults.setState(Defaults.STATE_ENABLED);
        day.setState(Day.STATE_ENABLED);

        assertThat(day.isEnabled(), is(true));
        assertThat(day.getHourX(), is(HOUR_DAY));
        assertThat(day.getMinuteX(), is(1));
        assertThat(day.sameAsDefault(), is(false));

        day.reverse();
        assertThat(day.getState(), is(Day.STATE_DISABLED));
    }

    @Test
    public void DefaultEnabledDayRule() {
        defaults.setState(Defaults.STATE_ENABLED);
        day.setState(Day.STATE_DISABLED);

        assertThat(day.isEnabled(), is(false));
        assertThat(day.getHourX(), is(HOUR_DAY));
        assertThat(day.getMinuteX(), is(1));
        assertThat(day.sameAsDefault(), is(false));

        day.reverse();
        assertThat(day.getState(), is(Day.STATE_ENABLED));
    }

    @Test
    public void isHoliday_true() {
        AlarmMorningActivity activity = Robolectric.setupActivity(AlarmMorningActivity.class);
        ShadowActivity shadowActivity = Shadows.shadowOf(activity);

        // Set to use holiday
        useHoliday(activity, DE);

        // Set date to holiday
        GregorianCalendar date = new GregorianCalendar(HOLIDAY_YEAR, HOLIDAY_MONTH, HOLIDAY_DAY);
        day.setDate(date);

        assertThat(day.isHoliday(), is(true));

        // Set English for english name
        AlarmMorningActivityTest.setLocale(activity, "en", "US");

        assertThat(day.getHolidayDescription(), is("New Year"));

        // Continue with standard checks
        defaults.setState(Defaults.STATE_ENABLED);
        day.setState(Day.STATE_RULE);

        assertThat(day.isEnabled(), is(false));
    }

    private void useHoliday(AlarmMorningActivity activity, String path) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(activity);
        SharedPreferences.Editor editor = preferences.edit();

        editor.putString(SettingsActivity.PREF_HOLIDAY, path);

        editor.commit();
    }

    @Test
    public void isHoliday_false() {
        assertThat(day.isHoliday(), is(false));
    }

}