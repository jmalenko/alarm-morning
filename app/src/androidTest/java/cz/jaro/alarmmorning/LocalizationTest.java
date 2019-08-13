package cz.jaro.alarmmorning;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Calendar;
import java.util.Locale;

import androidx.test.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@RunWith(AndroidJUnit4.class)
public class LocalizationTest {

    private void setLocale(String language, String country) {
        Locale locale = new Locale(language, country);
        // update locale for date formatters
        Locale.setDefault(locale);
        // update locale for app resources
        Context appContext = InstrumentationRegistry.getTargetContext();
        Resources res = appContext.getResources();
        Configuration config = res.getConfiguration();
        config.locale = locale;
        res.updateConfiguration(config, res.getDisplayMetrics());
    }

    @Test
    public void dayOfWeekToString_en() {
        setLocale("en", "US");
        Context appContext = InstrumentationRegistry.getTargetContext();
        Resources res = appContext.getResources();

        assertEquals("Sunday", Localization.dayOfWeekToString(res, Calendar.SUNDAY));
        assertEquals("Monday", Localization.dayOfWeekToString(res, Calendar.MONDAY));
        assertEquals("Tuesday", Localization.dayOfWeekToString(res, Calendar.TUESDAY));
        assertEquals("Wednesday", Localization.dayOfWeekToString(res, Calendar.WEDNESDAY));
        assertEquals("Thursday", Localization.dayOfWeekToString(res, Calendar.THURSDAY));
        assertEquals("Friday", Localization.dayOfWeekToString(res, Calendar.FRIDAY));
        assertEquals("Saturday", Localization.dayOfWeekToString(res, Calendar.SATURDAY));
    }

    @Test
    public void dayOfWeekToStringShort_en() {
        setLocale("en", "US");
        Context appContext = InstrumentationRegistry.getTargetContext();
        Resources res = appContext.getResources();

        assertEquals("Sun", Localization.dayOfWeekToStringShort(res, Calendar.SUNDAY));
        assertEquals("Mon", Localization.dayOfWeekToStringShort(res, Calendar.MONDAY));
        assertEquals("Tue", Localization.dayOfWeekToStringShort(res, Calendar.TUESDAY));
        assertEquals("Wed", Localization.dayOfWeekToStringShort(res, Calendar.WEDNESDAY));
        assertEquals("Thu", Localization.dayOfWeekToStringShort(res, Calendar.THURSDAY));
        assertEquals("Fri", Localization.dayOfWeekToStringShort(res, Calendar.FRIDAY));
        assertEquals("Sat", Localization.dayOfWeekToStringShort(res, Calendar.SATURDAY));
    }

    @Test
    public void dayOfWeekToString_cs() {
        setLocale("cs", "CZ");
        Context appContext = InstrumentationRegistry.getTargetContext();
        Resources res = appContext.getResources();

        // Czech: záměrně je uveden 4. pád, protože se používá ve větě "Nastavit budík v neděli?"
        assertEquals("neděli", Localization.dayOfWeekToString(res, Calendar.SUNDAY));
        assertEquals("pondělí", Localization.dayOfWeekToString(res, Calendar.MONDAY));
        assertEquals("úterý", Localization.dayOfWeekToString(res, Calendar.TUESDAY));
        assertEquals("středu", Localization.dayOfWeekToString(res, Calendar.WEDNESDAY));
        assertEquals("čtvrtek", Localization.dayOfWeekToString(res, Calendar.THURSDAY));
        assertEquals("pátek", Localization.dayOfWeekToString(res, Calendar.FRIDAY));
        assertEquals("sobotu", Localization.dayOfWeekToString(res, Calendar.SATURDAY));
    }

    @Test
    public void dayOfWeekToStringShort_cs() {
        setLocale("cs", "CZ");
        Context appContext = InstrumentationRegistry.getTargetContext();
        Resources res = appContext.getResources();

        assertEquals("Ne", Localization.dayOfWeekToStringShort(res, Calendar.SUNDAY));
        assertEquals("Po", Localization.dayOfWeekToStringShort(res, Calendar.MONDAY));
        assertEquals("Út", Localization.dayOfWeekToStringShort(res, Calendar.TUESDAY));
        assertEquals("St", Localization.dayOfWeekToStringShort(res, Calendar.WEDNESDAY));
        assertEquals("Čt", Localization.dayOfWeekToStringShort(res, Calendar.THURSDAY));
        assertEquals("Pá", Localization.dayOfWeekToStringShort(res, Calendar.FRIDAY));
        assertEquals("So", Localization.dayOfWeekToStringShort(res, Calendar.SATURDAY));
    }

    @Test
    public void dayOfWeekToString_invalid() {
        setLocale("en", "US");
        Context appContext = InstrumentationRegistry.getTargetContext();
        Resources res = appContext.getResources();

        try {
            Localization.dayOfWeekToString(res, 100);
            fail("Expected IllegalArgumentException to be thrown");
        } catch (IllegalArgumentException e) {
            assertTrue(true);
        }
    }

    @Test
    public void weekStart_en() {
        setLocale("en", "US");
        com.ibm.icu.util.Calendar c = com.ibm.icu.util.Calendar.getInstance();

        assertEquals(Calendar.SUNDAY, c.getFirstDayOfWeek());
    }

    @Test
    public void weekStart_cs() {
        setLocale("cs", "CZ");
        com.ibm.icu.util.Calendar c = com.ibm.icu.util.Calendar.getInstance();

        assertEquals(Calendar.MONDAY, c.getFirstDayOfWeek());
    }

    @Test
    public void weekend_en() {
        setLocale("en", "US");
        com.ibm.icu.util.Calendar c = com.ibm.icu.util.Calendar.getInstance();

        assertEquals(com.ibm.icu.util.Calendar.WEEKEND, c.getDayOfWeekType(Calendar.SUNDAY));
        assertEquals(com.ibm.icu.util.Calendar.WEEKDAY, c.getDayOfWeekType(Calendar.MONDAY));
        assertEquals(com.ibm.icu.util.Calendar.WEEKDAY, c.getDayOfWeekType(Calendar.TUESDAY));
        assertEquals(com.ibm.icu.util.Calendar.WEEKDAY, c.getDayOfWeekType(Calendar.WEDNESDAY));
        assertEquals(com.ibm.icu.util.Calendar.WEEKDAY, c.getDayOfWeekType(Calendar.THURSDAY));
        assertEquals(com.ibm.icu.util.Calendar.WEEKDAY, c.getDayOfWeekType(Calendar.FRIDAY));
        assertEquals(com.ibm.icu.util.Calendar.WEEKEND, c.getDayOfWeekType(Calendar.SATURDAY));
    }

    @Test
    public void weekend_cs() {
        setLocale("cs", "CZ");
        com.ibm.icu.util.Calendar c = com.ibm.icu.util.Calendar.getInstance();

        assertEquals(com.ibm.icu.util.Calendar.WEEKEND, c.getDayOfWeekType(Calendar.SUNDAY));
        assertEquals(com.ibm.icu.util.Calendar.WEEKDAY, c.getDayOfWeekType(Calendar.MONDAY));
        assertEquals(com.ibm.icu.util.Calendar.WEEKDAY, c.getDayOfWeekType(Calendar.TUESDAY));
        assertEquals(com.ibm.icu.util.Calendar.WEEKDAY, c.getDayOfWeekType(Calendar.WEDNESDAY));
        assertEquals(com.ibm.icu.util.Calendar.WEEKDAY, c.getDayOfWeekType(Calendar.THURSDAY));
        assertEquals(com.ibm.icu.util.Calendar.WEEKDAY, c.getDayOfWeekType(Calendar.FRIDAY));
        assertEquals(com.ibm.icu.util.Calendar.WEEKEND, c.getDayOfWeekType(Calendar.SATURDAY));
    }

    @Test
    public void weekend_ir() {
        setLocale("fa", "IR");
        com.ibm.icu.util.Calendar c = com.ibm.icu.util.Calendar.getInstance();

        assertEquals(com.ibm.icu.util.Calendar.WEEKDAY, c.getDayOfWeekType(Calendar.SUNDAY));
        assertEquals(com.ibm.icu.util.Calendar.WEEKDAY, c.getDayOfWeekType(Calendar.MONDAY));
        assertEquals(com.ibm.icu.util.Calendar.WEEKDAY, c.getDayOfWeekType(Calendar.TUESDAY));
        assertEquals(com.ibm.icu.util.Calendar.WEEKDAY, c.getDayOfWeekType(Calendar.WEDNESDAY));
        assertEquals(com.ibm.icu.util.Calendar.WEEKEND, c.getDayOfWeekType(Calendar.THURSDAY));
        assertEquals(com.ibm.icu.util.Calendar.WEEKEND, c.getDayOfWeekType(Calendar.FRIDAY));
        assertEquals(com.ibm.icu.util.Calendar.WEEKDAY, c.getDayOfWeekType(Calendar.SATURDAY));
    }

}
