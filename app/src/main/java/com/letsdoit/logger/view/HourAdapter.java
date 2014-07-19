package com.letsdoit.logger.view;

import android.content.Context;
import android.graphics.Point;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Pair;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.common.collect.Lists;
import com.letsdoit.logger.R;
import com.letsdoit.logger.data.activity.ActivityFragment;
import com.letsdoit.logger.data.activity.ActivityInterval;

import org.joda.time.DateTime;
import org.joda.time.Period;

import java.util.List;

/**
 * Created by Andrey on 7/12/2014.
 */
public class HourAdapter extends ArrayAdapter<Pair<ActivityInterval, ActivityInterval>> {
    private static final String TAG = "ADP_HourAdapter";
    private static final long HALF_HOUR_MILLIS = 30 * 60 * 1000;
    private static Period FIVE_MINUTES = Period.minutes(5);

    private static int HOUR_FIELD_WIDTH_IN_DIP = 24;
    private static int NUM_BLOCKS_IN_HALF_HOUR = 6;

    private int hourFieldWidthInPixels;
    private int pixelsInHalfHour;

    private LayoutInflater inflater;

    public HourAdapter(Context context) {
        super(context, R.layout.hour);
        inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        hourFieldWidthInPixels = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, HOUR_FIELD_WIDTH_IN_DIP,
                displayMetrics);

        Point outSize = new Point();
        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        windowManager.getDefaultDisplay().getSize(outSize);
        pixelsInHalfHour = outSize.x - hourFieldWidthInPixels;

        Log.d(TAG, "pixelsInHalfHour=" + pixelsInHalfHour);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Log.d(TAG, "getView called");

        View view;
        if (convertView == null) {
            view = inflater.inflate(R.layout.hour, parent, false);
        } else {
            view = convertView;
        }

        TextView hourText = (TextView) view.findViewById(R.id.hour);

        Pair<ActivityInterval, ActivityInterval> hourData = getItem(position);
        ActivityInterval firstHalfHour = hourData.first;
        ActivityInterval secondHalfHour = hourData.second;
        hourText.setText("" + firstHalfHour.getStartTime().getHourOfDay());

        List<DisplayBlock> firstHalfHourDisplayBlocks = DisplayBlock.makeDisplayBlocks(firstHalfHour.getStartTime(),
                firstHalfHour.getEndTime(), FIVE_MINUTES, firstHalfHour.getFragments());
        List<DisplayBlock> secondHalfHourDisplayBlocks = DisplayBlock.makeDisplayBlocks(secondHalfHour.getStartTime(),
                secondHalfHour.getEndTime(), FIVE_MINUTES, secondHalfHour.getFragments());
        Log.d(TAG, "Display blocks in first half hour: " + firstHalfHourDisplayBlocks.size() + " " +
                firstHalfHourDisplayBlocks);
        Log.d(TAG, "Display blocks in second half hour: " + secondHalfHourDisplayBlocks.size() + " " +
                secondHalfHourDisplayBlocks);

        LinearLayout firstHalfHourLayout = (LinearLayout) view.findViewById(R.id.firstHalfHourLayout);
        LinearLayout secondHalfHourLayout = (LinearLayout) view.findViewById(R.id.secondHalfHourLayout);

        sizeChildrenInHalfHour(firstHalfHourDisplayBlocks, firstHalfHourLayout);
        sizeChildrenInHalfHour(secondHalfHourDisplayBlocks, secondHalfHourLayout);

        return view;
    }

    private void sizeChildrenInHalfHour(List<DisplayBlock> halfHour, LinearLayout halfHourLayout) {
        // Make sure there are enough GUI blocks to display all of the DisplayBlocks
        int numAdditionalButtonsNeeded = halfHour.size() - halfHourLayout.getChildCount();
        for (int i = 0; i < numAdditionalButtonsNeeded; i++) {
            Log.d(TAG, "Inflating additional buttons needed for display");
            inflater.inflate(R.layout.disply_block, halfHourLayout, false);
        }

        // Resize and update the text on the GUI blocks
        for(int i = 0; i < halfHour.size() && i < halfHourLayout.getChildCount(); i++) {
            DisplayBlock block = halfHour.get(i);
            double relativeSize = block.getPercentageTimeOfPeriod(HALF_HOUR_MILLIS);
            Log.d(TAG, "Relative button size=" + relativeSize);

            Button button = (Button) halfHourLayout.getChildAt(i);
            int buttonWidth = (int) (relativeSize * pixelsInHalfHour);
            button.getLayoutParams().width = buttonWidth;
            if (block.hasActivityFragment()) {
                Log.d(TAG, "Updating button text with activity name " + block.getActivityFragment().getActivityName());
                button.setText(block.getActivityFragment().getActivityName());
            }
        }

        for (int i = halfHour.size(); i < halfHourLayout.getChildCount(); i++) {
            Log.d(TAG, "Setting button size to 0 for extra buttons");
            Button button = (Button) halfHourLayout.getChildAt(i);
            button.getLayoutParams().width = 0;
        }
    }

    public void setData(List<ActivityFragment> fragments, DateTime earliestTime, DateTime latestTime) {
        Log.d(TAG, "setData called");

        clear();
        earliestTime = roundDownToHour(earliestTime);
        latestTime = roundDownToHour(latestTime);
        Log.d(TAG, "earliestTime=" + earliestTime);
        Log.d(TAG, "latestTime=" + latestTime);

        List<ActivityInterval> halfHours = ActivityFragment.partition(earliestTime, latestTime, Period.minutes(30), fragments);

        ActivityInterval prev = null;
        for (ActivityInterval halfHour : halfHours) {
            if (prev == null) {
                prev = halfHour;
            } else {
                add(new Pair(prev, halfHour));
                prev = null;
            }
        }

    }

    private DateTime roundDownToHour(DateTime time) {
        int minutes = time.getMinuteOfHour();
        int millis = time.getMillisOfSecond();
        int seconds = time.getSecondOfMinute();
        return time.minusMinutes(minutes).minusSeconds(seconds).minusMillis(millis);
    }

}
