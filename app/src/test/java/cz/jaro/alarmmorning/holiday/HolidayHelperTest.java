package cz.jaro.alarmmorning.holiday;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.List;

import cz.jaro.alarmmorning.FixedTimeTest;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

/**
 * Test HolidayHelper class.
 * <p/>
 * Implemented as instrumentation test because the HolidayHelper needs Context to handle preferences and localized strings (when no calendar is selected).
 */
@RunWith(RobolectricTestRunner.class)
public class HolidayHelperTest {

    public static final String NONE = HolidayHelper.PATH_TOP;

    public static final String CZ = "CZ";
    public static final String DE = "DE";
    public static final String DE_BB = "DE.bb"; // Brandenburg (BB) has no subregions
    public static final String DE_BY = "DE.by"; // Bavaria (BY) has subregions
    public static final String DE_BY_AG = "DE.by.ag";

    public static final String NON_EXISTENT = "nonexistent";
    public static final String CZ_NON_EXISTENT = CZ + "." + NON_EXISTENT;
    public static final String DE_NON_EXISTENT = DE + "." + NON_EXISTENT;
    public static final String DE_BB_NON_EXISTENT = DE_BB + "." + NON_EXISTENT;

    private HolidayHelper holidayHelper;

    @Before
    public void before() {
        holidayHelper = HolidayHelper.getInstance();
    }

    @After
    public void after() {
        FixedTimeTest.resetSingletons();
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

        result = holidayHelper.list(DE_BB);
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
        holidayHelper.list(CZ_NON_EXISTENT);
    }

    @Test(expected = IllegalStateException.class)
    public void list_nonexistent3() {
        holidayHelper.list(DE_NON_EXISTENT);
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

        str = holidayHelper.preferenceToDisplayName(DE_BB);
        assertThat(str, is("Germany – Brandenburg"));

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
    public void preferenceToDisplayName_nonexistent_CZ() {
        holidayHelper.preferenceToDisplayName(CZ_NON_EXISTENT);
    }

    @Test(expected = IllegalStateException.class)
    public void preferenceToDisplayName_nonexistent_DE() {
        holidayHelper.preferenceToDisplayName(DE_NON_EXISTENT);
    }

    @Test(expected = IllegalStateException.class)
    public void preferenceToDisplayName_nonexistent_DE_BB() {
        holidayHelper.preferenceToDisplayName(DE_BB_NON_EXISTENT);
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

        valid = holidayHelper.isPathValid(DE_BB);
        assertThat(valid, is(true));

        valid = holidayHelper.isPathValid(DE_BY);
        assertThat(valid, is(true));

        valid = holidayHelper.isPathValid(DE_BY_AG);
        assertThat(valid, is(true));

        valid = holidayHelper.isPathValid(NON_EXISTENT);
        assertThat(valid, is(false));

        valid = holidayHelper.isPathValid(CZ_NON_EXISTENT);
        assertThat(valid, is(false));

        valid = holidayHelper.isPathValid(DE_NON_EXISTENT);
        assertThat(valid, is(false));

        valid = holidayHelper.isPathValid(DE_BB_NON_EXISTENT);
        assertThat(valid, is(false));
    }

    @Test
    public void findExistingParent() {
        assertThat(holidayHelper.findExistingParent(HolidayHelper.PATH_TOP), is(HolidayHelper.PATH_TOP));
        assertThat(holidayHelper.findExistingParent(NON_EXISTENT), is(HolidayHelper.PATH_TOP));

        assertThat(holidayHelper.findExistingParent(DE), is(DE));
        assertThat(holidayHelper.findExistingParent(DE_NON_EXISTENT), is(DE));

        assertThat(holidayHelper.findExistingParent(DE_BB), is(DE_BB));
        assertThat(holidayHelper.findExistingParent(DE_BB_NON_EXISTENT), is(DE_BB));
    }
}