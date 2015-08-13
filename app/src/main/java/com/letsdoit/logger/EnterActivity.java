package com.letsdoit.logger;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;

import com.fatboyindustrial.gsonjodatime.Converters;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.letsdoit.logger.data.dao.Activity;
import com.letsdoit.logger.data.dao.ActivityInterval;
import com.letsdoit.logger.data.sqlite.CompletedActivityFragmentsDAO;
import com.letsdoit.logger.view.RenderBlock;

import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

public class EnterActivity extends android.app.Activity {
    private static final Gson GSON = Converters.registerDateTime(new GsonBuilder()).create();
    private static DateTimeFormatter format = DateTimeFormat.forPattern("HH:mm:ss");

    private RenderBlock start;
    private RenderBlock end;

    private CompletedActivityFragmentsDAO dao;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_enter);

        dao = new CompletedActivityFragmentsDAO(this);

        Intent intent = getIntent();
        start = GSON.fromJson(intent.getStringExtra(Main.START_BLOCK), RenderBlock.class);
        end = GSON.fromJson(intent.getStringExtra(Main.END_BLOCK), RenderBlock.class);

        TextView startTimeView = (TextView) findViewById(R.id.startTime);
        TextView endTimeView = (TextView) findViewById(R.id.endTime);

        startTimeView.setText(format.print(start.getBlockStart()));
        endTimeView.setText(format.print(end.getBlockEnd()));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.enter, menu);
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

    public void onLogActivity(View view) {
        AutoCompleteTextView activityNameView = (AutoCompleteTextView) findViewById(R.id.activityNameEntry);
        String activityName = activityNameView.getText().toString();
        dao.open();
        Activity activity = new Activity(activityName, start.getBlockStart(), end.getBlockEnd());
        dao.addActivity(activity);
        dao.close();
        finish();
    }
}
