package com.letsdoit.logger;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.LoaderManager;
import android.content.Intent;
import android.content.Loader;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;

import com.fatboyindustrial.gsonjodatime.Converters;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.letsdoit.logger.data.dao.ActivityFragment;
import com.letsdoit.logger.data.dao.ActivityInterval;
import com.letsdoit.logger.data.sqlite.CompletedActivityFragmentsDAO;
import com.letsdoit.logger.loader.CompletedActivityFragmentLoader;
import com.letsdoit.logger.view.HourAdapter;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Period;

import java.util.Collections;
import java.util.List;
import java.util.TimeZone;


public class Main extends Activity implements LoaderManager.LoaderCallbacks<List<ActivityFragment>> {
    public static final String START_BLOCK = "StartBlock";
    public static final String END_BLOCK = "EndBlock";

    private static final String TAG = "ADP_Main";
    private static final int LOADER_ID = 1;

    private static final Gson GSON = Converters.registerDateTime(new GsonBuilder()).create();

    private CompletedActivityFragmentsDAO dao;
    private HourAdapter adapter;

    private View cachedStartBlock = null;
    private ActivityInterval cachedStartInterval = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        DateTimeZone.setDefault(DateTimeZone.forTimeZone(TimeZone.getDefault()));

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
    protected void onResume() {
        super.onResume();
        getLoaderManager().restartLoader(LOADER_ID, null, this);
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
        ActivityInterval block = (ActivityInterval) view.getTag(R.id.display_block_key);

        // Don't allow selections in the future
        if (block.getEnd().isAfter(DateTime.now())) {
            return;
        }

        if (cachedStartInterval == null) {
            // Save the clicked empty block as the start of the selection
            if (block.isEmpty()) {
                cachedStartBlock = view;
                cachedStartInterval = block;
                view.setAlpha((float) 0.25);
            } else {
                new AlertDialog.Builder(this).setMessage(block.stringify()).create().show();
            }
        } else {
            dao.open();
            List<ActivityFragment> activitiesInInterval = dao.getInRange(cachedStartInterval.getStart(), block.getEnd());
            dao.close();
            Log.d(TAG, String.format("Num activities in selection: %s", activitiesInInterval.size()));

            // Don't create activity if it overlaps with another activity
            if (activitiesInInterval.isEmpty()) {
                // Clear the start selection
                ActivityInterval startBlock = cachedStartInterval;
                cachedStartBlock.setAlpha(1);
                cachedStartInterval = null;
                cachedStartBlock = null;

                if (block.isEmpty()) {
                    // Make sure that the selected block is after
                    if (block.getEnd().isAfter(startBlock.getStart())) {
                        Intent intent = new Intent(this, EnterActivity.class);

                        intent.putExtra(START_BLOCK, GSON.toJson(startBlock));
                        intent.putExtra(END_BLOCK, GSON.toJson(block));
                        startActivity(intent);
                    }
                } else {
                    new AlertDialog.Builder(this).setMessage(block.stringify()).create().show();
                }
            }
        }
    }

    @Override
    public Loader<List<ActivityFragment>> onCreateLoader(int id, Bundle args) {
        Log.d(TAG, "creating loader");
        return new CompletedActivityFragmentLoader(this, dao);
    }

    // Update the adapter with the loaded data
    @Override
    public void onLoadFinished(Loader<List<ActivityFragment>> loader, List<ActivityFragment> data) {
        CompletedActivityFragmentLoader fragmentLoader = (CompletedActivityFragmentLoader) loader;

        DateTime earliestTime = fragmentLoader.getStart();
        DateTime latestTime = fragmentLoader.getEnd().plus(Period.hours(8));

        this.adapter.setData(data, earliestTime, latestTime);

        Log.d(TAG, "onLoadFinished completed");
    }

    // Clear out the loader
    @Override
    public void onLoaderReset(Loader<List<ActivityFragment>> loader) {
        DateTime now = new DateTime();
        List<ActivityFragment> empty = Collections.emptyList();
        adapter.setData(empty, now, now);
        adapter.notifyDataSetChanged();
        Log.d(TAG, "onLoaderReset completed");
    }
}
