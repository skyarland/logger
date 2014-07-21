package com.letsdoit.logger;

import android.app.Activity;
import android.app.LoaderManager;
import android.content.Loader;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;

import com.letsdoit.logger.data.dao.ActivityFragment;
import com.letsdoit.logger.data.sqlite.CompletedActivityFragmentsDAO;
import com.letsdoit.logger.loader.CompletedActivityFragmentLoader;
import com.letsdoit.logger.view.HourAdapter;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Period;

import java.util.Collections;
import java.util.List;


public class Main extends Activity implements LoaderManager.LoaderCallbacks<List<ActivityFragment>> {

    private static final String TAG = "ADP_Main";
    private static final int LOADER_ID = 1;

    private CompletedActivityFragmentsDAO dao;
    private HourAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        DateTimeZone.setDefault(DateTimeZone.UTC);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        this.dao = new CompletedActivityFragmentsDAO(this);
        this.adapter = new HourAdapter(this);

        ListView listView = (ListView) findViewById(R.id.listView);
        listView.setAdapter(this.adapter);
        
        getLoaderManager().initLoader(LOADER_ID, null, this);
        
        Log.d(TAG, "onCreate completed");
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void onTimeEntryButtonClick(View view) {
    }

    @Override
    public Loader<List<ActivityFragment>> onCreateLoader(int id, Bundle args) {
        Log.d(TAG, "creating loader");
        return new CompletedActivityFragmentLoader(this, dao);
    }

    @Override
    public void onLoadFinished(Loader<List<ActivityFragment>> loader, List<ActivityFragment> data) {
        DateTime now = new DateTime();
        DateTime earliestTime = now.minus(Period.hours(8));
        DateTime latestTime = now.plus(Period.hours(8));
        this.adapter.setData(data, earliestTime, latestTime);
        Log.d(TAG, "onLoadFinished completed");
    }

    @Override
    public void onLoaderReset(Loader<List<ActivityFragment>> loader) {
        DateTime now = new DateTime();
        List<ActivityFragment> empty = Collections.emptyList();
        adapter.setData(empty, now, now);
        adapter.notifyDataSetChanged();
        Log.d(TAG, "onLoaderReset completed");
    }
}
