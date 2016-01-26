package cz.jaro.alarmmorning;

/**
 * Created by ext93831 on 26.1.2016.
 */

import android.app.Fragment;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import cz.jaro.alarmmorning.graphics.SimpleDividerItemDecoration;

/**
 * Fragment that appears in the "content_frame"
 */
public class CalendarFragment extends Fragment {

    private static final String TAG = CalendarFragment.class.getSimpleName();

    private RecyclerView recyclerView;
    protected CalendarAdapter adapter;
    private RecyclerView.LayoutManager layoutManager;

    private Handler handler = new Handler();
    private Runnable runnable = new Runnable() {
        @Override
        public void run() {
            adapter.onSystemTimeChange();
            handler.postDelayed(this, 1000);
        }
    };

    public CalendarFragment() {
        // Empty constructor required for fragment subclasses
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_calendar, container, false);

        recyclerView = (RecyclerView) rootView.findViewById(R.id.calendar_recycler_view);

        // use a linear layout manager
        layoutManager = new LinearLayoutManager(getActivity());
        recyclerView.setLayoutManager(layoutManager);

        // specify an adapter
        CalendarActivity calendarActivity = (CalendarActivity) getActivity();
        adapter = new CalendarAdapter(calendarActivity);
        recyclerView.setAdapter(adapter);

        // item separator
        recyclerView.addItemDecoration(new SimpleDividerItemDecoration(getActivity()));

        // for disabling the animation on update
        DefaultItemAnimator animator = new DefaultItemAnimator();
        animator.setSupportsChangeAnimations(false);
        recyclerView.setItemAnimator(animator);

        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();

        // handler for refreshing the content
        handler.postDelayed(runnable, 1000);

        adapter.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();

        handler.removeCallbacks(runnable);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        adapter.onDestroy();
    }

    public void onAlarmTimeOfEarlyDismissedAlarm() {
        Log.d(TAG, "onAlarmTimeOfEarlyDismissedAlarm()");
        adapter.notifyItemChanged(0);
    }

    public void onDismissBeforeRinging() {
        Log.d(TAG, "onDismissBeforeRinging()");
        adapter.updatePositionNextAlarm();
        //adapter.notifyDataSetChanged();
    }
}
