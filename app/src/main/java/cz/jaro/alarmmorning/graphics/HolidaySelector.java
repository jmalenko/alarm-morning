package cz.jaro.alarmmorning.graphics;

import android.content.Context;
import android.content.res.Resources;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.List;

import cz.jaro.alarmmorning.Localization;
import cz.jaro.alarmmorning.R;
import cz.jaro.alarmmorning.holiday.HolidayAdapter;
import cz.jaro.alarmmorning.holiday.HolidayHelper;
import de.jollyday.Holiday;

/**
 * The View that allows to select a HolidayCalendar path, eg. a hierarchically organized regions.
 * <p/>
 * Realized as three spinners (only the relevant are visible) and and text with list of nearest holidays.
 */
public class HolidaySelector extends LinearLayout implements AdapterView.OnItemSelectedListener {

    private static final String TAG = HolidaySelector.class.getSimpleName();

    private Spinner spinner1;
    private Spinner spinner2;
    private Spinner spinner3;

    private HolidayAdapter adapter1;
    private HolidayAdapter adapter2;
    private HolidayAdapter adapter3;

    private LinearLayout listOfHolidays;
    private TextView listOfHolidaysDetails;

    private boolean[] spinnerInitialized = new boolean[3]; // which spinners have been initialized

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

        spinner1 = (Spinner) findViewById(R.id.holidaySpinner1);
        spinner2 = (Spinner) findViewById(R.id.holidaySpinner2);
        spinner3 = (Spinner) findViewById(R.id.holidaySpinner3);

        listOfHolidays = (LinearLayout) findViewById(R.id.listOfHolidays);
        listOfHolidaysDetails = (TextView) findViewById(R.id.listOfHolidaysDetails);

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
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        int spinnerId = parent == spinner1 ? 1 : (parent == spinner2 ? 2 : 3);
        Log.v(TAG, "onItemSelected(spinnerId=" + spinnerId + ")");

        // Hack for problem: an undesirable onItemSelected() is triggered whilst the Spinner is initializing. This means that code which is intended to
        // execute ONLY when a user physically makes a selection is prematurely executed.
        // Source: http://stackoverflow.com/questions/5624825/spinner-onitemselected-executes-when-it-is-not-suppose-to/5918177#5918177
        // Addendum: After refactoring to a View, the initialization of all the spinners is not done at once. It is done just before the first usage in the
        // code. Therefore we need to store the initialization state of each spinner.
        if (!spinnerInitialized[spinnerId-1]) {
            spinnerInitialized[spinnerId-1] = true;
            return;
        }

        // TODO This is sometimes called twice. When Germany–Berlin is selected an then changed to Germany–Bavaria, this is called for spinner2 and spinner3

        HolidayAdapter holidayAdapter = (HolidayAdapter) parent.getAdapter();
        String path = holidayAdapter.positionToPreferenceString(position);
        updateView(path);
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        // Nothing
    }

    private void updateView(String path) {
        Log.d(TAG, "updateView(path=" + path + ")");

        HolidayHelper holidayHelper = HolidayHelper.getInstance();
        int pathLength = holidayHelper.pathLength(path);
        boolean hasSubregions = holidayHelper.hasSubregions(path);

        Log.v(TAG, "pathLength=" + pathLength + ", hasSubregions=" + hasSubregions);

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
        Log.v(TAG, "updateListOfHolidays(path=" + path + ")");

        Resources res = getContext().getResources();

        HolidayHelper holidayHelper = HolidayHelper.getInstance();
        if (listOfHolidaysVisibility == View.VISIBLE && holidayHelper.useHoliday(path)) {
            List<Holiday> holidays = holidayHelper.listHolidays(path);

            StringBuffer str = new StringBuffer();

            for (Holiday h : holidays) {
                if (str.length() > 0)
                    str.append('\n');

                str.append(Localization.dateToStringFull(res, h.getDate().toDate())); // TODO Localization - Format date, medium
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
     * @param path
     */
    public void setPath(String path) {
        updateView(path);
    }

    public void setListVisibility(int visibility) {
        listOfHolidaysVisibility = visibility;
    }
}