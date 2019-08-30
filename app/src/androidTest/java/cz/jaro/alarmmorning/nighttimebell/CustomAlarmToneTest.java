package cz.jaro.alarmmorning.nighttimebell;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;
import android.preference.PreferenceManager;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;

import androidx.test.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import cz.jaro.alarmmorning.R;
import cz.jaro.alarmmorning.SettingsActivity;
import cz.jaro.alarmmorning.SharedPreferencesHelper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

/**
 * Tests CustomAlarmTone
 */
@RunWith(AndroidJUnit4.class)
public class CustomAlarmToneTest {

    private File mFile;
    private SharedPreferences mSharedPreferences;

    @Before
    public void before() {
        Context appContext = InstrumentationRegistry.getTargetContext();

        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(appContext);
        mSharedPreferences.
                edit().
                putBoolean(CustomAlarmTone.PREF_FILES_INSTALLED, false).
                commit();

        File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_ALARMS);
        String filename = appContext.getResources().getResourceEntryName(R.raw.church_clock_strikes_3) + ".mp3";

        mFile = new File(path, filename);
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @After
    public void after() {
        // Delete the file
        mFile.delete();
    }

    @Test
    public void copiesAlarmTones() {
        installAlarmTones();

        // Make sure the files exist and aren't blank
        assertTrue(mFile.length() > 0);

        // Make sure the shared prefs are set correctly
        assertTrue((boolean) SharedPreferencesHelper.load(CustomAlarmTone.PREF_FILES_INSTALLED, false));
    }

    @Test
    public void overridesDefaultAlarmTone() {
        // Set the ringtone preference to the default
        mSharedPreferences
                .edit()
                .putString(SettingsActivity.PREF_NIGHTTIME_BELL_RINGTONE, SettingsActivity.PREF_NIGHTTIME_BELL_RINGTONE_DEFAULT)
                .commit();

        installAlarmTones();

        // Check if the pref was overridden
        assertNotSame(
                SharedPreferencesHelper.load(SettingsActivity.PREF_NIGHTTIME_BELL_RINGTONE, ""),
                "");
    }

    @Test
    public void doesNotOverrideNonDefaultAlarmTone() {
        // Set the ringtone preference to something other than the default
        mSharedPreferences.edit()
                .putString(SettingsActivity.PREF_NIGHTTIME_BELL_RINGTONE, "something_other_than_default")
                .commit();

        installAlarmTones();

        // Make sure the ringtone pref wasn't changed
        assertEquals(
                "something_other_than_default",
                SharedPreferencesHelper.load(SettingsActivity.PREF_NIGHTTIME_BELL_RINGTONE, ""));
    }

    @Test
    public void handlesFilesAlreadyExist() {
        // Run twice
        installAlarmTones();

        mSharedPreferences.
                edit().
                putBoolean(CustomAlarmTone.PREF_FILES_INSTALLED, false).
                commit();

        installAlarmTones();

        assertTrue((boolean) SharedPreferencesHelper.load(CustomAlarmTone.PREF_FILES_INSTALLED, false));
    }

    //
    // Helpers
    //

    private void installAlarmTones() {
        Context appContext = InstrumentationRegistry.getTargetContext();
        CustomAlarmTone customAlarmTone = new CustomAlarmTone(appContext);
        customAlarmTone.install();
    }
}
