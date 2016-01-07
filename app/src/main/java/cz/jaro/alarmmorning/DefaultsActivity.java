package cz.jaro.alarmmorning;

import android.app.ActionBar;
import android.app.Activity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

import cz.jaro.alarmmorning.graphics.SimpleDividerItemDecoration;


public class DefaultsActivity extends Activity {

    private RecyclerView recyclerView;
    private DefaultsAdapter adapter;
    private RecyclerView.LayoutManager layoutManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_defaults);

        ActionBar ab = getActionBar();
        ab.setDisplayHomeAsUpEnabled(true);
        ab.setDisplayShowHomeEnabled(false);

        recyclerView = (RecyclerView) findViewById(R.id.defaults_recycler_view);

        // use a linear layout manager
        layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);

        // specify an adapter
        adapter = new DefaultsAdapter(this);
        recyclerView.setAdapter(adapter);

        // item separator
        recyclerView.addItemDecoration(new SimpleDividerItemDecoration(this));
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        adapter.onDestroy();
    }

}