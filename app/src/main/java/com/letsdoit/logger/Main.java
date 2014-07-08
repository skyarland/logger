package com.letsdoit.logger;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;

import java.util.HashMap;
import java.util.Map;


public class Main extends Activity {

    private boolean isStarted = false;
    private int startHour;
    private int startMinute;

    private static Map<Integer, Integer> START_MINUTE;
    private static Map<Integer, Integer> END_MINUTE;

    static {
        START_MINUTE = new HashMap<Integer, Integer>();
        START_MINUTE.put(R.id.m00_05, 0);
        START_MINUTE.put(R.id.m05_10, 5);
        START_MINUTE.put(R.id.m10_15, 10);
        START_MINUTE.put(R.id.m15_20, 15);
        START_MINUTE.put(R.id.m20_25, 20);
        START_MINUTE.put(R.id.m25_30, 25);
        START_MINUTE.put(R.id.m30_35, 30);
        START_MINUTE.put(R.id.m35_40, 35);
        START_MINUTE.put(R.id.m40_45, 40);
        START_MINUTE.put(R.id.m45_50, 45);
        START_MINUTE.put(R.id.m50_55, 50);
        START_MINUTE.put(R.id.m55_60, 55);

        END_MINUTE = new HashMap<Integer, Integer>();
        END_MINUTE.put(R.id.m00_05, 5);
        END_MINUTE.put(R.id.m05_10, 10);
        END_MINUTE.put(R.id.m10_15, 15);
        END_MINUTE.put(R.id.m15_20, 20);
        END_MINUTE.put(R.id.m20_25, 25);
        END_MINUTE.put(R.id.m25_30, 30);
        END_MINUTE.put(R.id.m30_35, 35);
        END_MINUTE.put(R.id.m35_40, 40);
        END_MINUTE.put(R.id.m40_45, 45);
        END_MINUTE.put(R.id.m45_50, 50);
        END_MINUTE.put(R.id.m50_55, 55);
        END_MINUTE.put(R.id.m55_60, 60);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ListView listView = (ListView) findViewById(R.id.listView);
        String[] hours = {"4", "5", "6", "7", "8", "9", "10", "11", "12", "1", "2", "3", "4", "5",
                "6", "7", "8", "9", "10", "11", "12", "1", "2", "3"};
        ListAdapter adapter = new ArrayAdapter<String>(this, R.layout.hour, R.id.hour, hours);
        listView.setAdapter(adapter);
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
        int id = view.getId();

        if (isStarted) {
            int endMinute = END_MINUTE.get(id);
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage(String.format("Selection from (%sm) to (%sm)", startMinute, endMinute));
            builder.create().show();
            isStarted = false;
        } else {
            startMinute = START_MINUTE.get(id);
            isStarted = true;
        }
    }
}
