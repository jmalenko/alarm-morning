package cz.jaro.alarmmorning.holiday;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.List;

import cz.jaro.alarmmorning.BuildConfig;
import cz.jaro.alarmmorning.SettingsActivity;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

/**
 * Test HolidayHelper class.
 * <p/>
 * Implemented as instrumentation test because the HolidayHelper needs Context to handle preferences and localized strings (when no calendar is selected).
 */
@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class, manifest = "app/src/main/AndroidManifest.xml", sdk = 21)
public class HolidayHelperTest {

    public static final String NONE = SettingsActivity.PREF_HOLIDAY_NONE;

    public static final String CZ = "CZ";
    public static final String DE = "DE";
    public static final String DE_B = "DE.b"; // Berlin (B) doen;t have subregions
    public static final String DE_BY = "DE.by"; // Bavaria (BY) has subregions
    public static final String DE_BY_AG = "DE.by.ag";

    public static final String NON_EXISTENT = "nonexistent";
    public static final String NON_EXISTENT2 = CZ + "." + NON_EXISTENT;
    public static final String NON_EXISTENT3 = DE + "." + NON_EXISTENT;

    private HolidayHelper holidayHelper;

    @Before
    public void before() {
        holidayHelper = HolidayHelper.getInstance();
    }

    @Test
    public void list() {
        List<Region> result;

        result = holidayHelper.list(NONE);
        assertThat(result.size() > 0, is(true));

        Region region = null;
        for (Region r : result) {
            if (r.id.equals(CZ))
                region = r;
        }
        assertThat(region != null, is(true));
        assertThat(region.description, is("Czech Republic"));

        result = holidayHelper.list(CZ);
        assertThat(result.size(), is(0));

        result = holidayHelper.list(DE);
        assertThat(result.size() > 0, is(true));

        result = holidayHelper.list(DE_B);
        assertThat(result.size() == 0, is(true));

        result = holidayHelper.list(DE_BY);
        assertThat(result.size() > 0, is(true));

        result = holidayHelper.list(DE_BY_AG);
        assertThat(result.size() == 0, is(true));
    }

    @Test(expected = IllegalStateException.class)
    public void list_nonexistent() {
        holidayHelper.list(NON_EXISTENT);
    }

    @Test(expected = IllegalStateException.class)
    public void list_nonexistent2() {
        holidayHelper.list(NON_EXISTENT2);
    }

    @Test(expected = IllegalStateException.class)
    public void list_nonexistent3() {
        holidayHelper.list(NON_EXISTENT3);
    }

    @Test
    public void preferenceToDisplayName() {
        String str;

        str = holidayHelper.preferenceToDisplayName(NONE);
        assertThat(str, is("None"));

        str = holidayHelper.preferenceToDisplayName(CZ);
        assertThat(str, is("Czech Republic"));

        str = holidayHelper.preferenceToDisplayName(DE);
        assertThat(str, is("Germany"));

        str = holidayHelper.preferenceToDisplayName(DE_B);
        assertThat(str, is("Germany – Berlin"));

        str = holidayHelper.preferenceToDisplayName(DE_BY);
        assertThat(str, is("Germany – Bavaria"));

        str = holidayHelper.preferenceToDisplayName(DE_BY_AG);
        assertThat(str, is("Germany – Bavaria – Augsburg"));
    }

    @Test(expected = IllegalStateException.class)
    public void preferenceToDisplayName_nonexistent() {
        holidayHelper.preferenceToDisplayName(NON_EXISTENT);
    }

    @Test(expected = IllegalStateException.class)
    public void preferenceToDisplayName_nonexistent2() {
        holidayHelper.preferenceToDisplayName(NON_EXISTENT2);
    }

    @Test(expected = IllegalStateException.class)
    public void preferenceToDisplayName_nonexistent3() {
        holidayHelper.preferenceToDisplayName(NON_EXISTENT3);
    }

    @Test
    public void isPathValid() {
        boolean valid;

        valid = holidayHelper.isPathValid(NONE);
        assertThat(valid, is(true));

        valid = holidayHelper.isPathValid(CZ);
        assertThat(valid, is(true));

        valid = holidayHelper.isPathValid(DE);
        assertThat(valid, is(true));

        valid = holidayHelper.isPathValid(DE_B);
        assertThat(valid, is(true));

        valid = holidayHelper.isPathValid(DE_BY);
        assertThat(valid, is(true));

        valid = holidayHelper.isPathValid(DE_BY_AG);
        assertThat(valid, is(true));
        
        valid = holidayHelper.isPathValid(NON_EXISTENT);
        assertThat(valid, is(false));

        valid = holidayHelper.isPathValid(NON_EXISTENT2);
        assertThat(valid, is(false));

        valid = holidayHelper.isPathValid(NON_EXISTENT3);
        assertThat(valid, is(false));
    }
}