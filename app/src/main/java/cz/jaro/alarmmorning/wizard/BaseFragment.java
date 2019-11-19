package cz.jaro.alarmmorning.wizard;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.LayoutRes;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.github.paolorotolo.appintro.ISlideBackgroundColorHolder;
import com.github.paolorotolo.appintro.ISlideSelectionListener;

import cz.jaro.alarmmorning.MyLog;
import cz.jaro.alarmmorning.R;

/**
 * Parent fragment for all fragments in the {@link Wizard}.
 */
public abstract class BaseFragment extends Fragment implements ISlideSelectionListener, ISlideBackgroundColorHolder {

    private LinearLayout mainLayout;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setRetainInstance(true);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_wizard_base, container, false);

        mainLayout = view.findViewById(R.id.main);
        int backgroundColor = getDefaultBackgroundColor();
        mainLayout.setBackgroundColor(backgroundColor);

        TextView titleView = view.findViewById(R.id.title);
        String title = getTitle();
        titleView.setText(title);
        int titleColor = getTitleColor();
        if (titleColor != 0) {
            titleView.setTextColor(titleColor);
        }

        int descriptionColor = getDescriptionColor();

        TextView description1View = view.findViewById(R.id.description1);
        String descriptionTop = getDescriptionTop();
        if (descriptionTop != null) {
            description1View.setText(descriptionTop);
            if (descriptionColor != 0) {
                description1View.setTextColor(descriptionColor);
            }
        } else {
            description1View.setVisibility(View.GONE);
        }

        TextView description2View = view.findViewById(R.id.description2);
        String descriptionBottom = getDescriptionBottom();
        if (descriptionBottom != null) {
            description2View.setText(descriptionBottom);
            if (descriptionColor != 0) {
                description2View.setTextColor(descriptionColor);
            }
            if (getContentLayoutId() == 0) { // if no content then add bottom margin to descriptionBottom
                final int marginBottomAddDp = 64;
                int px = dpToPx(marginBottomAddDp);

                LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) description2View.getLayoutParams();
                lp.bottomMargin += px;
                description2View.setLayoutParams(lp);
            }
        } else {
            description2View.setVisibility(View.GONE);
        }

        // add content
        if (getContentLayoutId() != 0) {
            ScrollView scrollView = view.findViewById(R.id.content_frame2);

            LayoutInflater layoutInflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View newLayout = layoutInflater.inflate(getContentLayoutId(), null, false);

            scrollView.addView(newLayout);
        } else {
            // Ignore content's layout_weight (useful on small displaye when the descriptions are long)
            ScrollView scrollView = view.findViewById(R.id.content_frame2);
            LinearLayout.LayoutParams params1 = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            scrollView.setLayoutParams(params1);
        }

        return view;
    }

    public static int dpToPx(int dp)
    {
        return (int) (dp * Resources.getSystem().getDisplayMetrics().density);
    }

    public static int pxToDp(int px)
    {
        return (int) (px / Resources.getSystem().getDisplayMetrics().density);
    }

    @Override
    public void onSlideDeselected() {
        MyLog.d(String.format("Slide %s has been deselected.", getTitle()));
    }

    @Override
    public void onSlideSelected() {
        MyLog.d(String.format("Slide %s has been selected.", getTitle()));
    }

    @Override
    public int getDefaultBackgroundColor() {
        // Get theme colors
        TypedArray array = getActivity().getTheme().obtainStyledAttributes(new int[]{
                android.R.attr.colorBackground,
                android.R.attr.textColorPrimary
        });
        int backgroundColor = array.getColor(0, 0xFF00FF);
        //int textColor = array.getColor(1, 0xFF00FF);
        array.recycle();

        //int primaryDarkColor = getResources().getColor(R.color.primary_dark);

        return backgroundColor;
    }

    @Override
    public void setBackgroundColor(@ColorInt int backgroundColor) {
        mainLayout.setBackgroundColor(backgroundColor);
    }

    /**
     * @return content layout Id. 0 means "do not include any layout".
     */
    @LayoutRes
    protected int getContentLayoutId() {
        return 0;
    }

    protected String getTitle() {
        return null;
    }

    protected int getTitleColor() {
        return 0;
    }

    protected String getDescriptionTop() {
        return null;
    }

    protected String getDescriptionBottom() {
        return null;
    }

    protected int getDescriptionColor() {
        return 0;
    }

}
