package cz.jaro.alarmmorning.holiday;

import android.content.Context;
import android.content.res.Resources;
import android.widget.ArrayAdapter;

import cz.jaro.alarmmorning.R;
import cz.jaro.alarmmorning.SettingsActivity;
import de.jollyday.HolidayCalendar;
import de.jollyday.util.ResourceUtil;


/**
 * An adapter to show the "none" item and a list of countries (that matches an enum).
 */
public class HolidayAdapter extends ArrayAdapter<String> {

    public HolidayAdapter(Context context, int resource) {
        super(context, resource);

        addItems();
    }

    private void addItems() {
        // None item
        Resources res = getContext().getResources();
        String muteText = res.getString(R.string.holidays_none);
        add(muteText);

        // Countries
        for (HolidayCalendar c : HolidayCalendar.values()) {
            ResourceUtil resourceUtil = new ResourceUtil();
            String countryDescription = resourceUtil.getCountryDescription(c.getId());
            add(countryDescription);
        }
    }

    public static String positionToPreferenceString(int position) {
        switch (position) {
            case 0:
                return SettingsActivity.PREF_HOLIDAY_NONE;
            default:
                HolidayCalendar holidayCalendar = HolidayCalendar.values()[position - 1];
                return holidayCalendar.getId();
        }
    }

    public int getPositionForId(String id) {
        if (id.equals(SettingsActivity.PREF_HOLIDAY_NONE)) return 0;
        int position = 1;
        for (HolidayCalendar c : HolidayCalendar.values()) {
            if (id.equals(c.getId())) return position;
            position++;
        }
        throw new IllegalStateException("Cannot find HolidayCalendar with id " + id);
    }
}