package com.letsdoit.logger.view;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Point;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.letsdoit.logger.R;
import com.letsdoit.logger.data.dao.ActivityFragment;
import com.letsdoit.logger.data.dao.ActivityInterval;

import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.Period;

import java.util.List;

/**
 * Created by Andrey on 7/12/2014.
 */
public class HourAdapter extends ArrayAdapter<Hour> {
    private static final String TAG = "ADP_HourAdapter";
    private static Duration HALF_HOUR = new Duration(30 * 60 * 1000);
    private static Duration FIVE_MINUTES = new Duration(5 * 60 * 1000);
    private static Duration FOUR_MINUTES = new Duration(4 * 60 * 1000);

    // The width of the hour label on the left side of the screen
    private static int HOUR_FIELD_WIDTH_IN_DIP = 24;
    // The number of blocks to subdivide the free areas in each half-hour row
    private static int NUM_BLOCKS_IN_HALF_HOUR = 6;

    private int pixelsInHalfHour;

    private LayoutInflater inflater;

    public HourAdapter(Context context) {
        super(context, R.layout.hour);
        inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        int hourFieldWidth = getHourFieldWidth(context);
        int screenWidth = getScreenWidthInPixels(context);
        pixelsInHalfHour = screenWidth - hourFieldWidth;

        Log.d(TAG, "pixelsInHalfHour=" + pixelsInHalfHour);
    }

    private int getScreenWidthInPixels(Context context) {
        Point outSize = new Point();
        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        windowManager.getDefaultDisplay().getSize(outSize);
        return outSize.x;
    }

    private int getHourFieldWidth(Context context) {
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, HOUR_FIELD_WIDTH_IN_DIP,
                displayMetrics);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Log.d(TAG, "getView called");

        View view;
//        if (convertView == null) {
//            Log.d(TAG, "Inflating new hour.");
//            view = inflater.inflate(R.layout.hour, parent, false);
//        } else {
//            Log.d(TAG, "Reusing hour layout.");
//            view = convertView;
//        }

        view = inflater.inflate(R.layout.hour, parent, false);

        TextView hourText = (TextView) view.findViewById(R.id.hour);

        Hour hourData = getItem(position);
        ActivityInterval firstHalfHour = hourData.getFirstHalfHour();
        ActivityInterval secondHalfHour = hourData.getSecondHalfHour();
        hourText.setText("" + firstHalfHour.getStart().getHourOfDay());

        DateTime now = DateTime.now();
        if (!firstHalfHour.getStart().isAfter(now) && !secondHalfHour.getEnd().isBefore(now)) {
            hourText.setTextColor(Color.BLUE);
        }

        ActivityInterval firstHalfHourInterval = new ActivityInterval(firstHalfHour.getStart(), firstHalfHour.getEnd(),
                firstHalfHour.getFragments());
        ActivityInterval secondHalfHourInterval = new ActivityInterval(secondHalfHour.getStart(),
                secondHalfHour.getEnd(), secondHalfHour.getFragments());

        List<ActivityInterval> firstHalfHourDisplayBlocks =
                DisplayBlock.mergeTooSmall(
                        DisplayBlock.wrapFragmentsAndClipFreeTime(firstHalfHourInterval, FIVE_MINUTES),
                        FOUR_MINUTES);
        List<ActivityInterval> secondHalfHourDisplayBlocks =
                DisplayBlock.mergeTooSmall(
                        DisplayBlock.wrapFragmentsAndClipFreeTime(secondHalfHourInterval, FIVE_MINUTES),
                        FOUR_MINUTES);

        LinearLayout firstHalfHourLayout = (LinearLayout) view.findViewById(R.id.firstHalfHourLayout);
        LinearLayout secondHalfHourLayout = (LinearLayout) view.findViewById(R.id.secondHalfHourLayout);

        sizeChildrenInHalfHour(firstHalfHourDisplayBlocks, firstHalfHourLayout);
        sizeChildrenInHalfHour(secondHalfHourDisplayBlocks, secondHalfHourLayout);

        return view;
    }

    private void sizeChildrenInHalfHour(List<ActivityInterval> halfHour, LinearLayout halfHourLayout) {
        // Make sure there are enough GUI blocks to display all of the DisplayBlocks
        int numAdditionalButtonsNeeded = halfHour.size() - halfHourLayout.getChildCount();

        for (int i = 0; i < numAdditionalButtonsNeeded; i++) {
            Log.d(TAG, "Inflating additional buttons needed for display");
            inflater.inflate(R.layout.disply_block, halfHourLayout, true);
        }
//
//        for (int i = halfHour.size(); i < halfHourLayout.getChildCount(); i++) {
//            Log.d(TAG, "Removing un-needed display block");
//            halfHourLayout.removeViewAt(i);
//        }

        // Resize and update the text on the GUI blocks
        for(int i = 0; i < halfHour.size() && i < halfHourLayout.getChildCount(); i++) {
            ActivityInterval block = halfHour.get(i);
            double relativeSize = block.getPercentageTimeOfPeriod(HALF_HOUR);

            Button button = (Button) halfHourLayout.getChildAt(i);
            int buttonWidth = (int) (relativeSize * pixelsInHalfHour * 0.7);
            Log.d(TAG, String.format("rel: %.2f, abs; %d", relativeSize, buttonWidth));
            button.setWidth(buttonWidth);
            
            button.setTag(R.id.display_block_key, block);

            if (block.isEmpty()) {
                button.setText("");
            } else {
                if (relativeSize < 0.1) {
                    button.setText("");
                } else {
                    String activityName = block.getActivityFragment(0).getActivityName();
                    Log.d(TAG, "Updating button text with activity name " + activityName);
                    button.setText(activityName);
                }
            }
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
                add(new Hour(prev, halfHour));
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
