package com.letsdoit.logger.view;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import com.google.common.collect.Lists;
import com.letsdoit.logger.R;
import com.letsdoit.logger.data.activity.ActivityFragment;

import org.joda.time.DateTime;
import org.joda.time.Period;

import java.util.Iterator;
import java.util.List;

/**
 * Created by Andrey on 7/12/2014.
 */
public class HourAdapter extends ArrayAdapter<List<ActivityFragment>> {
    private static final String TAG = "ADP_HourAdapter";

    private LayoutInflater inflater;

    public HourAdapter(Context context) {
        super(context, R.layout.hour);
        inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Log.d(TAG, "getView called");

        View view = inflater.inflate(R.layout.hour, parent, false);

        // TODO: add time entry blocks if no activity for the time period
        // TODO: Merge fragments from the same activity in the same hour

        return view;
    }

    public void setData(List<ActivityFragment> fragments, DateTime earliestTime, DateTime latestTime) {
        Log.d(TAG, "setData called");

        clear();
        earliestTime = roundDownToHour(earliestTime);
        latestTime = roundDownToHour(latestTime);
        Log.d(TAG, "earliestTime=" + earliestTime);
        Log.d(TAG, "latestTime=" + latestTime);

        Iterator<ActivityFragment> fragmentIterator = fragments.iterator();
        ActivityFragment fragment = fragmentIterator.next();
        DateTime endOfHour = earliestTime;

        while (endOfHour.isBefore(latestTime)) {
            List<ActivityFragment> hourFragments = Lists.newArrayList();

            while (fragment != null && fragment.getFragmentStart().isBefore(endOfHour)) {
                if (fragment.getFragmentEnd().isAfter(endOfHour)) {
                    // Split fragments that cross the hour
                    Pair<ActivityFragment, ActivityFragment> split = fragment.splitAtTime(endOfHour);
                    hourFragments.add(split.first);
                    // This will will break from this while loop, since the fragment start will be endOfHour
                    fragment = split.second;
                } else {
                    hourFragments.add(fragment);
                    if (fragmentIterator.hasNext()) {
                        fragment = fragmentIterator.next();
                    } else {
                        fragment = null;
                    }
                }
            }

            Log.d(TAG, "Added hour " + endOfHour);
            add(hourFragments);
            endOfHour = endOfHour.plus(Period.hours(1));
        }
    }

    private DateTime roundDownToHour(DateTime time) {
        int minutes = time.getMinuteOfHour();
        int millis = time.getMillisOfSecond();
        int seconds = time.getSecondOfMinute();
        return time.minusMinutes(minutes).minusSeconds(seconds).minusMillis(millis);
    }

}
