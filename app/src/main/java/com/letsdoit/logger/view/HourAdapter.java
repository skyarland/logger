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

import java.util.Iterator;
import java.util.List;

/**
 * Created by Andrey on 7/12/2014.
 */
public class HourAdapter extends ArrayAdapter<Pair<ActivityInterval, ActivityInterval>> {
    private static final String TAG = "ADP_HourAdapter";

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
        hourText.setText("" + position);

        LinearLayout firstHalfHourLayout = (LinearLayout) view.findViewById(R.id.firstHalfHourLayout);
        LinearLayout secondHalfHourLayout = (LinearLayout) view.findViewById(R.id.secondHalfHourLayout);
        sizeChildrenInHalfHour(firstHalfHourLayout);
        sizeChildrenInHalfHour(secondHalfHourLayout);

        Pair<ActivityInterval, ActivityInterval> hourData = getItem(position);
        ActivityInterval firstHalfHour = hourData.first;
        ActivityInterval secondHalfHour = hourData.second;

        // TODO: add time entry blocks if no activity for the time period

        return view;
    }

    private Pair<List<ActivityFragment>, List<ActivityFragment>> partitionAtTime(DateTime partition,
                                                                                 List<ActivityFragment> fragments) {
        List<ActivityFragment> first = Lists.newArrayList();
        List<ActivityFragment> second = Lists.newArrayList();

        for (ActivityFragment fragment : fragments) {
            if (fragment.getFragmentStart().isBefore(partition)) {
                if (fragment.getFragmentEnd().isAfter(partition)) {
                    Pair<ActivityFragment, ActivityFragment> beforeAndAfter = fragment.splitAtTime(partition);
                    first.add(beforeAndAfter.first);
                    second.add(beforeAndAfter.second);
                } else {
                    first.add(fragment);
                }
            } else {
                second.add(fragment);
            }
        }

        return new Pair(first, second);
    }

    private List<ActivityFragment> mergeFragments(List<ActivityFragment> fragments) {
        if (fragments.size() < 2) {
            return fragments;
        }

        List<ActivityFragment> mergedFragments = Lists.newArrayList();
        Iterator<ActivityFragment> iterator = fragments.iterator();
        ActivityFragment prevFragment = iterator.next();

        while (iterator.hasNext()) {
            ActivityFragment fragment = iterator.next();
            if (prevFragment.isSameActivityAs(fragment)) {
                prevFragment = ActivityFragment.mergeAndInterpolate(prevFragment, fragment);
            } else {
                mergedFragments.add(prevFragment);
                prevFragment = fragment;
            }
        }

        mergedFragments.add(prevFragment);

        return mergedFragments;
    }

    private void sizeChildrenInHalfHour(LinearLayout halfHourLayout) {

        for(int i = 0; i < halfHourLayout.getChildCount(); i++) {
            Button button = (Button) halfHourLayout.getChildAt(i);
            button.getLayoutParams().width = pixelsInHalfHour / NUM_BLOCKS_IN_HALF_HOUR;
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
