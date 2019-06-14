package cz.jaro.alarmmorning.holiday;

import android.content.Context;
import android.content.res.Resources;
import android.util.Log;
import android.widget.ArrayAdapter;

import java.util.List;

import cz.jaro.alarmmorning.R;


/**
 * An adapter to show the "none" item and a list of countries (that matches an enum).
 */
public class HolidayAdapter extends ArrayAdapter<String> {

    private static final String TAG = HolidayAdapter.class.getSimpleName();

    private String parentPath;

    public HolidayAdapter(Context context, int resource) {
        super(context, resource);
    }

    private void addItems() {
        clear();

        // None item
        Resources res = getContext().getResources();
        String none;
        if (parentPath.equals(""))
            none = res.getString(R.string.holidays_none);
        else
            none = res.getString(R.string.holidays_all);
        add(none);

        // Real items
        List<Region> regions = HolidayHelper.getInstance().list(parentPath);
        for (Region region : regions) {
            add(region.description);
        }

        notifyDataSetChanged();
    }

    public String positionToPreferenceString(int position) {
        Log.v(TAG, "positionToPreferenceString(position=" + position + ")");
        String preferenceString;
        if (position == 0) {
            preferenceString = parentPath;
        } else {
            List<Region> regions = HolidayHelper.getInstance().list(parentPath);
            Region region = regions.get(position - 1);
            preferenceString = region.getFullPath();
        }
        Log.v(TAG, "positionToPreferenceString=" + preferenceString);
        return preferenceString;
    }

    public int getPositionForId(String id) {
        if (id.equals("")) {
            return 0;
        }

        int position = 1;
        List<Region> regions = HolidayHelper.getInstance().list(parentPath);
        for (Region region : regions) {
            if (id.equals(region.id))
                return position;

            position++;
        }
        throw new IllegalStateException("Cannot find Region with id " + id);
    }

    public String getParentPath() {
        return parentPath;
    }

    public void setParentPath(String parentPath) {
        this.parentPath = parentPath;

        addItems();
    }

}