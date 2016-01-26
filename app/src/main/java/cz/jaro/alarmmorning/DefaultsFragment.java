package cz.jaro.alarmmorning;

import android.app.Fragment;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import cz.jaro.alarmmorning.graphics.SimpleDividerItemDecoration;

public class DefaultsFragment extends Fragment {

    public DefaultsFragment() {
        // Empty constructor required for fragment subclasses
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_defaults, container, false);

        RecyclerView recyclerView = (RecyclerView) rootView.findViewById(R.id.defaults_recycler_view);

        // use a linear layout manager
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(rootView.getContext());
        recyclerView.setLayoutManager(layoutManager);

        // specify an adapter
        ActivityInterface activityInterface = (ActivityInterface) getActivity();
        DefaultsAdapter adapter = new DefaultsAdapter(activityInterface);
        recyclerView.setAdapter(adapter);

        // item separator
        recyclerView.addItemDecoration(new SimpleDividerItemDecoration(rootView.getContext()));

        return rootView;
    }
}
