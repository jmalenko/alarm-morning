package cz.jaro.alarmmorning.graphics;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import cz.jaro.alarmmorning.Localization;
import cz.jaro.alarmmorning.MyLog;
import cz.jaro.alarmmorning.R;
import cz.jaro.alarmmorning.holiday.HolidayAdapter;
import cz.jaro.alarmmorning.holiday.HolidayHelper;
import cz.jaro.alarmmorning.holiday.regiondetector.GPSLocationProviderRegionDetector;
import cz.jaro.alarmmorning.holiday.regiondetector.IPRegionDetector;
import cz.jaro.alarmmorning.holiday.regiondetector.LocaleRegionDetector;
import cz.jaro.alarmmorning.holiday.regiondetector.LocationProviderRegionDetector;
import cz.jaro.alarmmorning.holiday.regiondetector.NetworkLocationProviderRegionDetector;
import cz.jaro.alarmmorning.holiday.regiondetector.RegionDetector;
import cz.jaro.alarmmorning.holiday.regiondetector.TelephonyRegionDetector;
import cz.jaro.alarmmorning.wizard.Wizard;
import de.galgtonold.jollydayandroid.Holiday;

/**
 * The View that allows to select a HolidayCalendar path, eg. a hierarchically organized regions.
 * <p/>
 * Realized as three spinners (only the relevant are visible) and and text with list of nearest holidays.
 */
public class HolidaySelector extends LinearLayout implements AdapterView.OnItemSelectedListener, RegionDetector.OnRegionChangeListener {

    private final LinearLayout recommendation;
    private final LinearLayout recommendationContainer;

    private final Spinner spinner1;
    private final Spinner spinner2;
    private final Spinner spinner3;

    private final HolidayAdapter adapter1;
    private final HolidayAdapter adapter2;
    private final HolidayAdapter adapter3;

    private final LinearLayout listOfHolidays;
    private final TextView listOfHolidaysDetails;

    private final boolean[] spinnerInitialized = new boolean[3]; // which spinners have been initialized

    private int listOfHolidaysVisibility = View.VISIBLE;

    public HolidaySelector(Context context) {
        this(context, null);
    }

    public HolidaySelector(Context context, AttributeSet attrs) {
        super(context, attrs);

        setOrientation(LinearLayout.HORIZONTAL);
        setGravity(Gravity.CENTER_VERTICAL);

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.holiday_selector, this, true);

        spinner1 = findViewById(R.id.holidaySpinner1);
        spinner2 = findViewById(R.id.holidaySpinner2);
        spinner3 = findViewById(R.id.holidaySpinner3);

        listOfHolidays = findViewById(R.id.listOfHolidays);
        listOfHolidaysDetails = findViewById(R.id.listOfHolidaysDetails);

        // Setup
        adapter1 = new HolidayAdapter(getContext(), R.layout.simple_spinner_item);
        adapter1.setDropDownViewResource(R.layout.simple_spinner_dropdown_item);
        spinner1.setAdapter(adapter1);
        spinner1.setOnItemSelectedListener(this);

        adapter2 = new HolidayAdapter(getContext(), R.layout.simple_spinner_item);
        adapter2.setDropDownViewResource(R.layout.simple_spinner_dropdown_item);
        spinner2.setAdapter(adapter2);
        spinner2.setOnItemSelectedListener(this);

        adapter3 = new HolidayAdapter(getContext(), R.layout.simple_spinner_item);
        adapter3.setDropDownViewResource(R.layout.simple_spinner_dropdown_item);
        spinner3.setAdapter(adapter3);
        spinner3.setOnItemSelectedListener(this);

        // Recommendations
        recommendation = findViewById(R.id.recommendation);
        recommendationContainer = findViewById(R.id.recommendationContainer);

        startRegionDetectors();
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        int spinnerId = parent == spinner1 ? 1 : (parent == spinner2 ? 2 : 3);
        MyLog.v("onItemSelected(spinnerId=" + spinnerId + ")");

        // Hack for problem: an undesirable onItemSelected() is triggered whilst the Spinner is initializing. This means that code which is intended to
        // execute ONLY when a user physically makes a selection is prematurely executed.
        // Source: http://stackoverflow.com/questions/5624825/spinner-onitemselected-executes-when-it-is-not-suppose-to/5918177#5918177
        // Addendum: After refactoring to a View, the initialization of all the spinners is not done at once. It is done just before the first usage in the
        // code. Therefore we need to store the initialization state of each spinner.
        if (!spinnerInitialized[spinnerId - 1]) {
            spinnerInitialized[spinnerId - 1] = true;
            return;
        }

        HolidayAdapter holidayAdapter = (HolidayAdapter) parent.getAdapter();
        String path = holidayAdapter.positionToPreferenceString(position);
        updateView(path);
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        // Nothing
    }

    private void updateView(String path) {
        MyLog.d("updateView(path=" + path + ")");

        HolidayHelper holidayHelper = HolidayHelper.getInstance();
        int pathLength = holidayHelper.pathLength(path);
        boolean hasSubregions = holidayHelper.hasSubregions(path);

        MyLog.v("pathLength=" + pathLength + ", hasSubregions=" + hasSubregions);

        for (int level = 1; level <= 3; level++) {
            HolidayAdapter adapter;
            Spinner spinner;
            switch (level) {
                case 1:
                    adapter = adapter1;
                    spinner = spinner1;
                    break;
                case 2:
                    adapter = adapter2;
                    spinner = spinner2;
                    break;
                case 3:
                    adapter = adapter3;
                    spinner = spinner3;
                    break;
                default:
                    throw new IllegalStateException("Unsupported level " + level);
            }

            if (pathLength >= level || (pathLength == level - 1 && hasSubregions)) {
                // Set parent path
                String parentPath;

                if (pathLength >= level) {
                    parentPath = holidayHelper.pathPrefix(path, level - 1);
                } else {
                    parentPath = path;
                }

                boolean changed = false;
                if (!parentPath.equals(adapter.getParentPath())) changed = true;

                if (changed) {
                    adapter.setParentPath(parentPath);
                }

                // Set position

                int position;
                if (pathLength >= level) {
                    String id = holidayHelper.pathPart(path, level);
                    position = adapter.getPositionForId(id);
                } else {
                    position = 0;
                }

                if (position != spinner.getSelectedItemPosition()) changed = true;
                if (View.VISIBLE != spinner.getVisibility()) changed = true;

                if (changed) {
                    spinner.setSelection(position);
                    spinner.setVisibility(View.VISIBLE);
                }
            } else {
                boolean changed = false;
                if (View.GONE != spinner.getVisibility()) changed = true;

                if (changed) {
                    spinner.setVisibility(View.GONE);
                }
            }
        }

        updateListOfHolidays(path);
    }

    private void updateListOfHolidays(String path) {
        MyLog.v("updateListOfHolidays(path=" + path + ")");

        Resources res = getContext().getResources();

        HolidayHelper holidayHelper = HolidayHelper.getInstance();
        if (listOfHolidaysVisibility == View.VISIBLE && holidayHelper.useHoliday(path)) {
            List<Holiday> holidays = holidayHelper.listHolidays(path);

            StringBuffer str = new StringBuffer();

            for (Holiday h : holidays) {
                if (str.length() > 0)
                    str.append('\n');

                str.append(Localization.dateToStringFull(res, h.getDate().toDate()));
                str.append(" – ");
                str.append(h.getDescription());
            }

            listOfHolidaysDetails.setText(str);
            listOfHolidays.setVisibility(View.VISIBLE);
        } else {
            listOfHolidays.setVisibility(View.GONE);
        }
    }

    /**
     * Get the path.
     *
     * @return path
     */
    public String getPath() {
        if (spinner3.getVisibility() == View.VISIBLE) {
            int position = spinner3.getSelectedItemPosition();
            return adapter3.positionToPreferenceString(position);
        } else if (spinner2.getVisibility() == View.VISIBLE) {
            int position = spinner2.getSelectedItemPosition();
            return adapter2.positionToPreferenceString(position);
        } else {
            int position = spinner1.getSelectedItemPosition();
            return adapter1.positionToPreferenceString(position);
        }
    }

    /**
     * Set the path.
     *
     * @param path Path
     */
    public void setPath(String path) {
        updateView(path);
    }

    public void setListVisibility(int visibility) {
        listOfHolidaysVisibility = visibility;
    }

    private void startRegionDetectors() {
        List<RegionDetector> regionDetectors = new ArrayList<>();

        regionDetectors.add(new LocaleRegionDetector(getContext()));
        regionDetectors.add(new TelephonyRegionDetector(getContext()));
        regionDetectors.add(new IPRegionDetector(getContext()));
        regionDetectors.add(new GPSLocationProviderRegionDetector(getContext())); // TODO Starting the LocationProviderRegionDetector in a thread like currently implemented causes memory leak (discovered by LeakCanary)
        regionDetectors.add(new NetworkLocationProviderRegionDetector(getContext()));

        for (RegionDetector regionDetector : regionDetectors) {
            regionDetector.setOnRegionChangeListener(this);

            // Run in a thread. Thread needed for parallel use of several RegionDetectors and to prevent NetworkOnMainThreadException.
            Thread thread = new Thread() {

                @Override
                public void run() {
                    // Looper needed because of LocationProviderRegionDetector
                    boolean useLooper = regionDetector instanceof LocationProviderRegionDetector;

                    if (useLooper)
                        Looper.prepare();

                    regionDetector.detect();

                    if (useLooper)
                        Looper.loop();
                }
            };
            thread.setName("Thread for " + regionDetector.getClass().getSimpleName());
            thread.start();
        }
    }

    @Override
    public boolean onRegionChange(RegionDetector regionDetector, String countryCode, Object region) {
        MyLog.d("onRegionChange(regionDetector=" + regionDetector.getClass().getSimpleName() + ", countryCode=" + countryCode + ")");

        // Smart fix of country codes
        if (countryCode.equals("GB"))
            countryCode = "UK";

        // Is this region's holiday calendar available?
        HolidayHelper holidayHelper = HolidayHelper.getInstance();
        if (!holidayHelper.isPathValid(countryCode)) {
            return true;
        }

        // The UI must be updated from the UI thread
        Activity activity = (Activity) getContext();
        final String finalCountryCode = countryCode;
        activity.runOnUiThread(() -> {
            int quality = regionDetectorQuality(regionDetector);

            Button button = getButton(finalCountryCode);
            if (button == null) { // Button for this region doesn't exist
                String countryName = holidayHelper.preferenceToDisplayName(finalCountryCode);


                // Create the button
                button = new Button(getContext());
                button.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
                button.setText(countryName);
                button.setTag(R.id.button_tag_country_code, finalCountryCode);
                button.setTag(R.id.button_tag_quality, quality);
                button.setOnClickListener(v -> {
                    String path = (String) v.getTag(R.id.button_tag_country_code);
                    updateView(path);
                });

                int index = findTargetIndex(quality);
                MyLog.v("Adding " + finalCountryCode + " at index " + index);
                recommendationContainer.addView(button, index);

                // Show...
                //    in Wizard shown for the 1st time: if there are 2 (or more) regions. Because the 1st region is preselected.
                //    otherwise: if a detected region is different from the selected one
                if (!Wizard.loadWizardFinished()) {
                    if (recommendationContainer.getChildCount() == 2)
                        recommendation.setVisibility(VISIBLE);
                } else {
                    if (!finalCountryCode.equals(getPath()))
                        recommendation.setVisibility(VISIBLE);
                }
            } else {
                int qualityButton = (int) button.getTag(R.id.button_tag_quality);
                quality += qualityButton;
                button.setTag(R.id.button_tag_quality, quality);

                // TODO For RegionDetectors that detect region continuously, subtract quality from the previously detected region.

                // Reorder
                recommendationContainer.removeView(button);

                int index = findTargetIndex(quality);
                MyLog.v("Moving " + finalCountryCode + " to index " + index);
                recommendationContainer.addView(button, index);
            }
        });

        return true;
    }

    private int findTargetIndex(int qualityButton) {
        for (int index = 0; index < recommendationContainer.getChildCount(); index++) {
            View v = recommendationContainer.getChildAt(index);
            int quality = (int) v.getTag(R.id.button_tag_quality);

            if (qualityButton > quality)
                return index;
        }
        return recommendationContainer.getChildCount();
    }

    private Button getButton(String countryCode) {
        for (int index = 0; index < recommendationContainer.getChildCount(); index++) {
            View v = recommendationContainer.getChildAt(index);
            String countryCode2 = (String) v.getTag(R.id.button_tag_country_code);
            if (countryCode.equals(countryCode2))
                return (Button) v;
        }
        return null;
    }

    private int regionDetectorQuality(RegionDetector regionDetector) {
        if (regionDetector instanceof LocaleRegionDetector)
            return 1;
        else if (regionDetector instanceof TelephonyRegionDetector)
            return 1;
        else if (regionDetector instanceof IPRegionDetector)
            return 5;
        else if (regionDetector instanceof GPSLocationProviderRegionDetector)
            return 10;
        else if (regionDetector instanceof NetworkLocationProviderRegionDetector)
            return 8;
        else
            throw new IllegalStateException("Unsupported region detector " + regionDetector.getClass().getSimpleName());
    }

}