package cz.jaro.alarmmorning.wizard;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.provider.Settings;

import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.github.paolorotolo.appintro.AppIntro;

import java.util.ArrayList;

import cz.jaro.alarmmorning.MyLog;
import cz.jaro.alarmmorning.SharedPreferencesHelper;

/**
 * Wizard contains several fragments that allow the user to quickly configure the app.
 */
public class Wizard extends AppIntro {

    /**
     * Value true = the wizard finished. Otherwise it should be presented to the user.
     */
    public static final String PREF_WIZARD = "wizard";

    public static final boolean PREF_WIZARD_DEFAULT = false;

    // Must be a subset of permissions in AndroidManifest.xml (the subset that requires the user to explicitly grant permission)
    public static final String[] allPermissions = new String[]{
            Manifest.permission.READ_CALENDAR,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.CAMERA,
            Manifest.permission.INTERNET,
            Manifest.permission.RECORD_AUDIO
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        showSkipButton(false);

        // Add slides
        addSlide(new SetWelcomeSlide());

        addSlide(new SetTimeSlide());

        addSlide(new SetHolidaySlide());

        addSlide(new SetActionsSlide());

        addSlide(new SetFeaturesSlide());

        // Show the "Set permissions" slide only when not all permissions are granted
        String[] missingPermissions = calcMissingPermissions();
        boolean allPermissionsGranted = missingPermissions.length == 0;
        if (!allPermissionsGranted) {
            StringBuilder missingPermissionsStr = new StringBuilder(missingPermissions[0]);
            for (int i = 1; i < missingPermissions.length; i++) {
                missingPermissionsStr.append(", ").append(missingPermissions[i]);
            }
            MyLog.v("Following permissions are not granted: " + missingPermissionsStr);

            addSlide(new SetPermissionSlide());
            askForPermissions(missingPermissions, fragments.size());
            // Note: The AppIntro library disables swiping when using askForPermissions. Source: https://github.com/PaoloRotolo/AppIntro/issues/123
        } else {
            MyLog.v("All permissions are granted. Skipping permission slide.");
        }

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            // Show the "Allow display over other apps" slide only when the related permissions isn't granted
            boolean canDrawOverlays = Settings.canDrawOverlays(this);
            if (!canDrawOverlays) {
                MyLog.v("Following permission is not granted: SYSTEM_ALERT_WINDOW (Allow display over other apps)");

                addSlide(new SetPermissionSlide__SYSTEM_ALERT_WINDOW());
            } else {
                MyLog.v("The SYSTEM_ALERT_WINDOW (Allow display over other apps) permission is granted. Skipping it's permission slide.");
            }
        }
        // Note: For older Android versions the app gets the permission automatically.

        addSlide(new SetDoneSlide());
    }

    private String[] calcMissingPermissions() {
        MyLog.d("calcMissingPermissions()");
        ArrayList<String> missingPermissions = new ArrayList<>();

        for (String permission : allPermissions) {
            int permissionCheck = ContextCompat.checkSelfPermission(this, permission);
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
        SharedPreferencesHelper.save(PREF_WIZARD, true);

        // Set return intent
        Intent returnIntent = new Intent();
        setResult(Activity.RESULT_OK, returnIntent);

        finish();
    }

    static public boolean loadWizardFinished() {
        return (boolean) SharedPreferencesHelper.load(Wizard.PREF_WIZARD, Wizard.PREF_WIZARD_DEFAULT);
    }

}