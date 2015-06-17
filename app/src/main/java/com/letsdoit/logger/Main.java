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
import android.widget.AbsListView;
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
import org.joda.time.Duration;

import java.util.Collections;
import java.util.List;
import java.util.TimeZone;

import static org.joda.time.Period.days;
import static org.joda.time.Period.hours;


public class Main extends Activity implements LoaderManager.LoaderCallbacks<List<ActivityFragment>>, AbsListView.OnScrollListener {
    public static final String START_BLOCK = "StartBlock";
    public static final String END_BLOCK = "EndBlock";

    private static final String TAG = "ADP_Main";
    private static final int LOADER_ID = 1;
    private static int DEFAULT_HOURS_TO_LOAD = 8;
    private static final Duration MAX_SCROLL_WINDOW_SIZE = days(2).toStandardDuration();

    private static final Gson GSON = Converters.registerDateTime(new GsonBuilder()).create();

    private CompletedActivityFragmentsDAO dao;

    private DateTime start;
    private DateTime end;

    private boolean loadedAtStart;
    private boolean loadedAtEnd;

    private ListView listView;
    private HourAdapter adapter;

    private View cachedStartBlock = null;
    private ActivityInterval cachedStartInterval = null;
    private boolean isMaxWindowSize = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        DateTimeZone.setDefault(DateTimeZone.forTimeZone(TimeZone.getDefault()));

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        this.dao = new CompletedActivityFragmentsDAO(this);
        this.adapter = new HourAdapter(this);

        listView = (ListView) findViewById(R.id.listView);
        listView.setAdapter(this.adapter);
        listView.setOnScrollListener(this);

        DateTime now = DateTime.now();
        start = now.minus(hours(DEFAULT_HOURS_TO_LOAD));
        end = now.plus(hours(DEFAULT_HOURS_TO_LOAD));

        getLoaderManager().initLoader(LOADER_ID, null, this);

        Log.d(TAG, "onCreate completed");
    }

    @Override
    public Loader<List<ActivityFragment>> onCreateLoader(int id, Bundle args) {
        Log.d(TAG, "creating loader");

        CompletedActivityFragmentLoader loader = new CompletedActivityFragmentLoader(this, dao);

        loader.setStart(start);
        loader.setEnd(end);

        return loader;
    }

    // Update the adapter with the loaded data
    @Override
    public void onLoadFinished(Loader<List<ActivityFragment>> loader, List<ActivityFragment> data) {
        CompletedActivityFragmentLoader fragmentLoader = (CompletedActivityFragmentLoader) loader;

        adapter.setData(data, start, end);
        adapter.notifyDataSetChanged();

        int listViewPosition = DEFAULT_HOURS_TO_LOAD - 3;
        int pixelsFromTop = 0;

        if (listView.getSelectedView() != null) {
            listViewPosition = listView.getSelectedItemPosition();
            pixelsFromTop = listView.getSelectedView().getTop();
        }

        if (loadedAtStart) {
            listViewPosition += DEFAULT_HOURS_TO_LOAD - 1;
        } else if (loadedAtEnd) {
            listViewPosition -= DEFAULT_HOURS_TO_LOAD;
            if (isMaxWindowSize) {
                listViewPosition -= DEFAULT_HOURS_TO_LOAD;
            }
        }

        listView.setSelectionFromTop(listViewPosition, pixelsFromTop);

        loadedAtStart = false;
        loadedAtEnd = false;

        Log.d(TAG, "onLoadFinished completed");
    }

    @Override
    protected void onResume() {
        super.onResume();
        getLoaderManager().getLoader(LOADER_ID).forceLoad();
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

    // Clear out the loader
    @Override
    public void onLoaderReset(Loader<List<ActivityFragment>> loader) {
        DateTime now = new DateTime();
        List<ActivityFragment> empty = Collections.emptyList();
        adapter.setData(empty, now, now);
        adapter.notifyDataSetChanged();
        Log.d(TAG, "onLoaderReset completed");
    }

    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {

    }

    @Override
    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {

        if (totalItemCount < DEFAULT_HOURS_TO_LOAD || loadedAtEnd || loadedAtStart) {
            return;
        }

        boolean closeToStart = isCloseToStart(firstVisibleItem);
        boolean closeToEnd = isCloseToEnd(firstVisibleItem, visibleItemCount, totalItemCount);

        if (closeToStart || closeToEnd) {

            CompletedActivityFragmentLoader loader = (CompletedActivityFragmentLoader)
                    getLoaderManager().<List<ActivityFragment>>getLoader(LOADER_ID);

            if (loader == null) {
                return;
            }

            if (closeToStart) {
                start = start.minus(hours(DEFAULT_HOURS_TO_LOAD));
                DateTime maxEnd = start.plus(MAX_SCROLL_WINDOW_SIZE);
                if (maxEnd.isBefore(end)) {
                    end = maxEnd;
                    isMaxWindowSize = true;
                }
                loadedAtStart = true;
            }

            if (closeToEnd) {
                end = end.plus(hours(DEFAULT_HOURS_TO_LOAD));
                DateTime minStart = end.minus(MAX_SCROLL_WINDOW_SIZE);
                if (minStart.isAfter(start)) {
                    start = minStart;
                    isMaxWindowSize = true;
                }
                loadedAtEnd = true;
            }

            Log.d(TAG, "Setting loader start and end ");
            Log.d(TAG, "start: " + start);
            Log.d(TAG, "end:   " + end);

            loader.setStart(start);
            loader.setEnd(end);

            getLoaderManager().getLoader(LOADER_ID).forceLoad();
        }
    }

    private boolean isCloseToEnd(int firstVisibleItem, int visibleItemCount, int totalItemCount) {
        return firstVisibleItem + visibleItemCount > totalItemCount - 4;
    }

    private boolean isCloseToStart(int firstVisibleItem) {
        return firstVisibleItem < 4;
    }
}
