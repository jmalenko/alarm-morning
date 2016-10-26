package cz.jaro.alarmmorning.nighttimebell;

import android.content.SharedPreferences;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.LargeTest;

import java.io.File;

import cz.jaro.alarmmorning.R;
import cz.jaro.alarmmorning.SettingsActivity;

/**
 * Tests CustomAlarmTone
 */
@LargeTest
public class CustomAlarmToneTest extends AndroidTestCase {

    private File mFile;
    private SharedPreferences mSharedPreferences;

    @Override
    public void setUp() throws Exception {
        super.setUp();

        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        mSharedPreferences.
                edit().
                putBoolean(CustomAlarmTone.PREF_FILES_INSTALLED, false).
                commit();

        File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_ALARMS);
        String filename = getContext().getResources().getResourceEntryName(R.raw.church_clock_strikes_3) + ".mp3";

        mFile = new File(path, filename);
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Override
    protected void tearDown() throws Exception {
        super.tearDown();

        // Delete the files
        mFile.delete();
    }

    public void test_copiesAlarmTones() {
        installAlarmTones();

        // Make sure the files exist and aren't blank
        assertTrue(mFile.length() > 0);

        // Make sure the shared prefs are set correctly
        assertTrue(mSharedPreferences.getBoolean(CustomAlarmTone.PREF_FILES_INSTALLED, false));
    }

    public void test_overridesDefaultAlarmTone() {
        // Set the ringtone preference to the default
        mSharedPreferences
                .edit()
                .putString(SettingsActivity.PREF_NIGHTTIME_BELL_RINGTONE, SettingsActivity.PREF_NIGHTTIME_BELL_RINGTONE_DEFAULT)
                .commit();

        installAlarmTones();

        // Check if the pref was overridden
        assertNotSame(
                mSharedPreferences.getString(SettingsActivity.PREF_NIGHTTIME_BELL_RINGTONE, ""),
                "");
    }

    public void test_doesNotOverrideNonDefaultAlarmTone() {
        // Set the ringtone preference to something other than the default
        mSharedPreferences.edit()
                .putString(SettingsActivity.PREF_NIGHTTIME_BELL_RINGTONE, "something_other_than_default")
                .commit();

        installAlarmTones();

        // Make sure the ringtone pref wasn't changed
        assertEquals(
                "something_other_than_default",
                mSharedPreferences.getString(SettingsActivity.PREF_NIGHTTIME_BELL_RINGTONE, ""));
    }

    public void test_handlesFilesAlreadyExist() {
        // Run twice
        installAlarmTones();

        mSharedPreferences.
                edit().
                putBoolean(CustomAlarmTone.PREF_FILES_INSTALLED, false).
                commit();

        installAlarmTones();

        assertTrue(mSharedPreferences.getBoolean(CustomAlarmTone.PREF_FILES_INSTALLED, false));
    }

    //
    // Helpers
    //

    private void installAlarmTones() {
        CustomAlarmTone customAlarmTone = new CustomAlarmTone(getContext());
        customAlarmTone.install();
    }
}
