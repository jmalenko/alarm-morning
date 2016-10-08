package cz.jaro.alarmmorning.wizard;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import com.github.paolorotolo.appintro.AppIntro;

import java.util.ArrayList;

/**
 * Created by jmalenko on 11.8.2016.
 */
public class Wizard extends AppIntro {

    private static final String TAG = Wizard.class.getSimpleName();

    /**
     * Value true = the wizard finished. Otherwise it should be presented to the user.
     */
    public static final String PREF_WIZARD = "wizard";

    public static final boolean PREF_WIZARD_DEFAULT = false;

    // Must be a subset of permissions in AndroidManifest.xml (the subset that requires the user to explicitly grant permission)
    private static final String[] allPermissions = new String[]{
            Manifest.permission.READ_CALENDAR,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        showSkipButton(false);

        // Add slides
        addSlide(new SetWelcomeSlide());

        addSlide(new SetTimeSlide());

        // TODO Add slide - Set holiday calendar, description "The alarm will not ring on holiday."

        addSlide(new SetActionsSlide());

        // Show the "Set permissions" slide only when not all permissions are granted
        String[] missingPermissions = calcMissingPermissions();
        boolean allPermissionsGranted = missingPermissions.length == 0;
        if (!allPermissionsGranted) {
            StringBuffer missingPermissionsStr = new StringBuffer(missingPermissions[0]);
            for (int i = 1; i < missingPermissions.length; i++) {
                missingPermissionsStr.append(", " + missingPermissions[i]);
            }
            Log.v(TAG, "Following permissions are not granted: " + missingPermissionsStr);

            addSlide(new SetPermissionSlide());
            askForPermissions(missingPermissions, 4);
            // TODO The AppIntro library disables swiping when using askForPermissions. Source: https://github.com/PaoloRotolo/AppIntro/issues/123
        } else {
            Log.v(TAG, "All permissions are granted. Skipping permission slide.");
        }

        // TODO Add slide - Add widget to home screen

        addSlide(new SetDoneSlide());
    }

    private String[] calcMissingPermissions() {
        Log.d(TAG, "calcMissingPermissions()");
        ArrayList<String> missingPermissions = new ArrayList<>();

        for (String permission : allPermissions) {
            int permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CALENDAR);
            if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
                missingPermissions.add(permission);
            }
        }

        return missingPermissions.toArray(new String[0]);
    }

    @Override
    public void onDonePressed(Fragment currentFragment) {
        super.onDonePressed(currentFragment);

        // Remember that wizard finished
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = preferences.edit();

        editor.putBoolean(PREF_WIZARD, true);

        editor.commit();

        // Set return intent
        Intent returnIntent = new Intent();
        setResult(Activity.RESULT_OK, returnIntent);

        finish();
    }

}