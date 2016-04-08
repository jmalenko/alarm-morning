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

        assertEquals(Localization.dayOfWeekToString(Calendar.SUNDAY, res), "Sun");
        assertEquals(Localization.dayOfWeekToString(Calendar.MONDAY, res), "Mon");
        assertEquals(Localization.dayOfWeekToString(Calendar.TUESDAY, res), "Tue");
        assertEquals(Localization.dayOfWeekToString(Calendar.WEDNESDAY, res), "Wed");
        assertEquals(Localization.dayOfWeekToString(Calendar.THURSDAY, res), "Thu");
        assertEquals(Localization.dayOfWeekToString(Calendar.FRIDAY, res), "Fri");
        assertEquals(Localization.dayOfWeekToString(Calendar.SATURDAY, res), "Sat");
    }

    public void test_dayOfWeekToString_cs() {
        setLocale("cs", "CZ");
        Resources res = getContext().getResources();

        assertEquals(Localization.dayOfWeekToString(Calendar.SUNDAY, res), "Ne");
        assertEquals(Localization.dayOfWeekToString(Calendar.MONDAY, res), "Po");
        assertEquals(Localization.dayOfWeekToString(Calendar.TUESDAY, res), "Út");
        assertEquals(Localization.dayOfWeekToString(Calendar.WEDNESDAY, res), "St");
        assertEquals(Localization.dayOfWeekToString(Calendar.THURSDAY, res), "Čt");
        assertEquals(Localization.dayOfWeekToString(Calendar.FRIDAY, res), "Pá");
        assertEquals(Localization.dayOfWeekToString(Calendar.SATURDAY, res), "So");
    }

    public void test_dayOfWeekToString_invalid() {
        setLocale("en", "EN");
        Resources res = getContext().getResources();

        try {
            Localization.dayOfWeekToString(100, res);
            fail("Expected IllegalArgumentException to be thrown");
        } catch (IllegalArgumentException e) {
            assertTrue(true);
        }
    }


}
