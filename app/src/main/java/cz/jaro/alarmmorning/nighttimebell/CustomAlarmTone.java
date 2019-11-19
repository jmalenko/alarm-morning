package cz.jaro.alarmmorning.nighttimebell;

import android.Manifest;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.MediaScannerConnection;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;

import androidx.core.content.ContextCompat;

import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.NoSuchElementException;

import cz.jaro.alarmmorning.JSONSharedPreferences;
import cz.jaro.alarmmorning.MyLog;
import cz.jaro.alarmmorning.R;
import cz.jaro.alarmmorning.SettingsActivity;
import cz.jaro.alarmmorning.SharedPreferencesHelper;

/**
 * This class copies the alarm tone distributed with this app (in the APK) into the alarms directory on the device's shared storage
 */
public class CustomAlarmTone {

    /**
     * Value true = the files were copied into the system.
     */
    public static final String PREF_FILES_INSTALLED = "pref_files_copied";
    public static final boolean PREF_FILES_INSTALLED_DEFAULT = false;

    /**
     * The URI of the local file (that was installed)
     */
    public static final String INSTALLED_FILES_PATH = "installed_files_path";

    private final Context mContext;

    public CustomAlarmTone(Context context) {
        mContext = context;
    }

    /**
     * Copies the files to shared storage if not done yet.
     */
    public void install() {
        MyLog.d("install()");

        int permissionCheck = ContextCompat.checkSelfPermission(mContext, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
            boolean filesInstalledPreference = (boolean) SharedPreferencesHelper.load(CustomAlarmTone.PREF_FILES_INSTALLED, CustomAlarmTone.PREF_FILES_INSTALLED_DEFAULT);
            if (!filesInstalledPreference) {
                MyLog.i("Copying ringtone files");

                // Copy the file
                boolean status = copyRawFile(R.raw.church_clock_strikes_3, mContext.getString(R.string.alarmtone_title_church_bell), true);

                if (status) {
                    // Remember that files were installed
                    SharedPreferencesHelper.save(PREF_FILES_INSTALLED, true);
                }
            } else {
                MyLog.d("Already installed");
            }
        } else {
            MyLog.d("Permission to write to storage not granted");
        }
    }

    /**
     * Copies a raw resource into the alarms directory on the device's shared storage
     *
     * @param resID        The resource ID of the raw resource to copy, in the form of R.raw.*
     * @param title        The title to use for the alarm tone
     * @param setAsDefault Set the file as the default alarm tone for the Nighttime bell
     * @return Whether the file was copied successfully
     */
    private boolean copyRawFile(int resID, String title, boolean setAsDefault) {
        // Make sure the shared storage is currently writable
        if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED))
            return false;

        File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_ALARMS);
        path.mkdirs(); // Make sure the directory exists
        String filename = mContext.getResources().getResourceEntryName(resID) + ".mp3";
        File outFile = new File(path, filename);

        String mimeType = "audio/mpeg";

        boolean isError = false;

        // Write the file
        InputStream inputStream = null;
        FileOutputStream outputStream = null;
        try {
            inputStream = mContext.getResources().openRawResource(resID);
            outputStream = new FileOutputStream(outFile);

            // Write in 1024-byte chunks
            byte[] buffer = new byte[1024];
            int bytesRead;
            // Keep writing until `inputStream.read()` returns -1, which means we reached the end of the stream
            while ((bytesRead = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, bytesRead);
            }

            // Set the file metadata
            String outAbsPath = outFile.getAbsolutePath();
            ContentValues contentValues = new ContentValues();
            contentValues.put(MediaStore.MediaColumns.DATA, outAbsPath);
            contentValues.put(MediaStore.MediaColumns.TITLE, title); // Android does not support title localization. Therefore we use title in current locale (that is not changed when the user changes locale later)
            contentValues.put(MediaStore.MediaColumns.MIME_TYPE, mimeType);
            contentValues.put(MediaStore.Audio.Media.IS_ALARM, true);
            contentValues.put(MediaStore.Audio.Media.IS_NOTIFICATION, true);
            contentValues.put(MediaStore.Audio.Media.IS_RINGTONE, true);
            contentValues.put(MediaStore.Audio.Media.IS_MUSIC, false);

            Uri contentUri = MediaStore.Audio.Media.getContentUriForPath(outAbsPath);

            // If the ringtone already exists in the database, delete it first
            mContext.getContentResolver().delete(contentUri, MediaStore.MediaColumns.DATA + "=\"" + outAbsPath + "\"", null);

            // Add the metadata to the file in the database
            Uri newUri = mContext.getContentResolver().insert(contentUri, contentValues);

            // Tell the media scanner about the new ringtone
            MediaScannerConnection.scanFile(
                    mContext,
                    new String[]{newUri.toString()},
                    new String[]{mimeType},
                    null
            );

            // Save local path
            JSONObject map = JSONSharedPreferences.loadJSONObject(INSTALLED_FILES_PATH);
            map.put(filename, newUri.toString());
            JSONSharedPreferences.saveJSONObject(INSTALLED_FILES_PATH, map);

            if (setAsDefault) {
                // Update the preference if set to default
                String ringtonePreference;
                try {
                    ringtonePreference = (String) SharedPreferencesHelper.load(SettingsActivity.PREF_NIGHTTIME_BELL_RINGTONE);
                } catch (NoSuchElementException e) {
                    ringtonePreference = SettingsActivity.PREF_NIGHTTIME_BELL_RINGTONE_DEFAULT;
                }
                if (ringtonePreference.equals(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM).toString())
                        || ringtonePreference.equals(SettingsActivity.PREF_NIGHTTIME_BELL_RINGTONE_DEFAULT)) {
                    SharedPreferencesHelper.save(SettingsActivity.PREF_NIGHTTIME_BELL_RINGTONE, newUri.toString());
                }
            }

            MyLog.d("Copied alarm tone '" + title + "' to " + outAbsPath);
            MyLog.d("URI is " + newUri.toString());
        } catch (Exception e) {
            MyLog.w("Error writing " + filename, e);
            isError = true;
        } finally {
            // Close the streams
            try {
                if (inputStream != null) {
                    inputStream.close();
                }
                if (outputStream != null) {
                    outputStream.close();
                }
            } catch (IOException e) {
                // Means there was an error trying to close the streams, so do nothing
                MyLog.w("Error closing stream", e);
            }
        }

        return !isError;
    }
}
