package com.letsdoit.logger;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.LoaderManager;
import android.content.Loader;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewParent;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.google.common.collect.Lists;
import com.letsdoit.logger.data.activity.ActivityFragment;
import com.letsdoit.logger.data.activity.CompletedActivityFragmentsDAO;
import com.letsdoit.logger.loader.CompletedActivityFragmentLoader;
import com.letsdoit.logger.view.HourAdapter;

import org.joda.time.DateTime;
import org.joda.time.Period;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class Main extends Activity implements LoaderManager.LoaderCallbacks<List<ActivityFragment>> {

    private CompletedActivityFragmentsDAO dao;
    private HourAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        this.dao = new CompletedActivityFragmentsDAO();
        this.adapter = new HourAdapter(this);

        ListView listView = (ListView) findViewById(R.id.listView);
        listView.setAdapter(this.adapter);
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
        return new CompletedActivityFragmentLoader(this, dao);
    }

    @Override
    public void onLoadFinished(Loader<List<ActivityFragment>> loader, List<ActivityFragment> data) {
        DateTime now = new DateTime();
        DateTime earliestTime = now.minus(Period.hours(8));
        DateTime latesetTime = now.plus(Period.hours(8));
        this.adapter.setData(data, earliestTime, latesetTime);
    }

    @Override
    public void onLoaderReset(Loader<List<ActivityFragment>> loader) {
        DateTime now = new DateTime();
        List<ActivityFragment> empty = Collections.emptyList();
        adapter.setData(empty, now, now);
    }
}
