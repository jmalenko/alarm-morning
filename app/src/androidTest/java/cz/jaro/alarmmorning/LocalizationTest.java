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

        assertEquals("Sunday", Localization.dayOfWeekToString(res, Calendar.SUNDAY));
        assertEquals("Monday", Localization.dayOfWeekToString(res, Calendar.MONDAY));
        assertEquals("Tuesday", Localization.dayOfWeekToString(res, Calendar.TUESDAY));
        assertEquals("Wednesday", Localization.dayOfWeekToString(res, Calendar.WEDNESDAY));
        assertEquals("Thursday", Localization.dayOfWeekToString(res, Calendar.THURSDAY));
        assertEquals("Friday", Localization.dayOfWeekToString(res, Calendar.FRIDAY));
        assertEquals("Saturday", Localization.dayOfWeekToString(res, Calendar.SATURDAY));
    }

    public void test_dayOfWeekToStringShort_en() {
        setLocale("en", "EN");
        Resources res = getContext().getResources();

        assertEquals("Sun", Localization.dayOfWeekToStringShort(res, Calendar.SUNDAY));
        assertEquals("Mon", Localization.dayOfWeekToStringShort(res, Calendar.MONDAY));
        assertEquals("Tue", Localization.dayOfWeekToStringShort(res, Calendar.TUESDAY));
        assertEquals("Wed", Localization.dayOfWeekToStringShort(res, Calendar.WEDNESDAY));
        assertEquals("Thu", Localization.dayOfWeekToStringShort(res, Calendar.THURSDAY));
        assertEquals("Fri", Localization.dayOfWeekToStringShort(res, Calendar.FRIDAY));
        assertEquals("Sat", Localization.dayOfWeekToStringShort(res, Calendar.SATURDAY));
    }

    public void test_dayOfWeekToString_cs() {
        setLocale("cs", "CZ");
        Resources res = getContext().getResources();

        // Czech: záměrně je uveden 4. pád, protože se používá ve větě "Nastavit budík v neděli?"
        assertEquals("neděli", Localization.dayOfWeekToString(res, Calendar.SUNDAY));
        assertEquals("pondělí", Localization.dayOfWeekToString(res, Calendar.MONDAY));
        assertEquals("úterý", Localization.dayOfWeekToString(res, Calendar.TUESDAY));
        assertEquals("středu", Localization.dayOfWeekToString(res, Calendar.WEDNESDAY));
        assertEquals("čtvrtek", Localization.dayOfWeekToString(res, Calendar.THURSDAY));
        assertEquals("pátek", Localization.dayOfWeekToString(res, Calendar.FRIDAY));
        assertEquals("sobotu", Localization.dayOfWeekToString(res, Calendar.SATURDAY));
    }

    public void test_dayOfWeekToStringShort_cs() {
        setLocale("cs", "CZ");
        Resources res = getContext().getResources();

        assertEquals("Ne", Localization.dayOfWeekToStringShort(res, Calendar.SUNDAY));
        assertEquals("Po", Localization.dayOfWeekToStringShort(res, Calendar.MONDAY));
        assertEquals("Út", Localization.dayOfWeekToStringShort(res, Calendar.TUESDAY));
        assertEquals("St", Localization.dayOfWeekToStringShort(res, Calendar.WEDNESDAY));
        assertEquals("Čt", Localization.dayOfWeekToStringShort(res, Calendar.THURSDAY));
        assertEquals("Pá", Localization.dayOfWeekToStringShort(res, Calendar.FRIDAY));
        assertEquals("So", Localization.dayOfWeekToStringShort(res, Calendar.SATURDAY));
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
