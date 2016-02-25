package cz.jaro.alarmmorning;

import android.app.FragmentManager;
import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;

import cz.jaro.alarmmorning.graphics.SimpleDividerItemDecoration;

public class DefaultsActivity extends AppCompatActivity implements ActivityInterface {

    DefaultsAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_defaults);

        // Set a Toolbar to replace the ActionBar.
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.defaults_recycler_view);

        // use a linear layout manager
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this);
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

    @Override
    public Context getContextI() {
        return this;
    }

    @Override
    public FragmentManager getFragmentManagerI() {
        return getFragmentManager();
    }

    @Override
    public Resources getResourcesI() {
        return getResources();
    }

}
