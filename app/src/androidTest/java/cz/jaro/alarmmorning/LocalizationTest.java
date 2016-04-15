package cz.jaro.alarmmorning;

import android.content.res.Configuration;
import android.content.res.Resources;
import android.test.AndroidTestCase;

import java.util.Calendar;
import java.util.Locale;

public class LocalizationTest extends AndroidTestCase {

    private void setLocale(String language, String country) {
        Locale locale = new Locale(language, country);
        // update locale for date formatters
        Locale.setDefault(locale);
        // update locale for app resources
        Resources res = getContext().getResources();
        Configuration config = res.getConfiguration();
        config.locale = locale;
        res.updateConfiguration(config, res.getDisplayMetrics());
    }

    public void test_dayOfWeekToString_en() {
        setLocale("en", "EN");
        Resources res = getContext().getResources();

        assertEquals(Localization.dayOfWeekToString(res, Calendar.SUNDAY), "Sun");
        assertEquals(Localization.dayOfWeekToString(res, Calendar.MONDAY), "Mon");
        assertEquals(Localization.dayOfWeekToString(res, Calendar.TUESDAY), "Tue");
        assertEquals(Localization.dayOfWeekToString(res, Calendar.WEDNESDAY), "Wed");
        assertEquals(Localization.dayOfWeekToString(res, Calendar.THURSDAY), "Thu");
        assertEquals(Localization.dayOfWeekToString(res, Calendar.FRIDAY), "Fri");
        assertEquals(Localization.dayOfWeekToString(res, Calendar.SATURDAY), "Sat");
    }

    public void test_dayOfWeekToString_cs() {
        setLocale("cs", "CZ");
        Resources res = getContext().getResources();

        assertEquals(Localization.dayOfWeekToString(res, Calendar.SUNDAY), "Ne");
        assertEquals(Localization.dayOfWeekToString(res, Calendar.MONDAY), "Po");
        assertEquals(Localization.dayOfWeekToString(res, Calendar.TUESDAY), "Út");
        assertEquals(Localization.dayOfWeekToString(res, Calendar.WEDNESDAY), "St");
        assertEquals(Localization.dayOfWeekToString(res, Calendar.THURSDAY), "Čt");
        assertEquals(Localization.dayOfWeekToString(res, Calendar.FRIDAY), "Pá");
        assertEquals(Localization.dayOfWeekToString(res, Calendar.SATURDAY), "So");
    }

    public void test_dayOfWeekToString_invalid() {
        setLocale("en", "EN");
        Resources res = getContext().getResources();

        try {
            Localization.dayOfWeekToString(res, 100);
            fail("Expected IllegalArgumentException to be thrown");
        } catch (IllegalArgumentException e) {
            assertTrue(true);
        }
    }


}
