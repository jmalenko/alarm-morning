package cz.jaro.alarmmorning.wizard;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import cz.jaro.alarmmorning.R;

public class SetPermissionSlide extends BaseFragment {

    private static final String TAG = SetPermissionSlide.class.getSimpleName();

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);

        TextView explanationTextView = (TextView) view.findViewById(R.id.explanation);
        explanationTextView.setText(Html.fromHtml(getString(R.string.wizard_set_permissions_explanation)));

        return view;
    }

    @Override
    protected int getContentLayoutId() {
        return R.layout.fragment_wizard_permissions;
    }

    @Override
    protected String getTitle() {
        return getString(R.string.wizard_set_permissions_title);
    }

    @Override
    protected String getDescriptionTop() {
        return getString(R.string.wizard_set_permissions_description);
    }

    @Override
    public int getDefaultBackgroundColor() {
        return getResources().getColor(R.color.Blue_Grey_800);
    }
}