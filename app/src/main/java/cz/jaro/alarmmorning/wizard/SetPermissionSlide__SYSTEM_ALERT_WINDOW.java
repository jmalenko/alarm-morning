package cz.jaro.alarmmorning.wizard;

import android.content.Intent;
import android.net.Uri;
import android.provider.Settings;

import cz.jaro.alarmmorning.CalendarFragment;
import cz.jaro.alarmmorning.R;

public class SetPermissionSlide__SYSTEM_ALERT_WINDOW extends BaseFragment {

    @Override
    public void onSlideDeselected() {
        Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:" + getContext().getPackageName()));
        startActivityForResult(intent, CalendarFragment.REQUEST_CODE_ACTION_MANAGE_OVERLAY_PERMISSION);
        super.onSlideDeselected();
    }

    @Override
    protected String getTitle() {
        return getString(R.string.wizard_set_permission_SYSTEM_ALERT_WINDOW_title);
    }

    @Override
    protected String getDescriptionTop() {
        return getString(R.string.wizard_set_permission_SYSTEM_ALERT_WINDOW_description);
    }

    @Override
    public int getDefaultBackgroundColor() {
        return getResources().getColor(R.color.Blue_Grey_850);
    }
}