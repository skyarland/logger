package com.letsdoit.logger.data.activity;

import android.util.Log;

import com.google.common.collect.Lists;

import org.joda.time.DateTime;
import org.joda.time.Period;

import java.util.ArrayList;
import java.util.List;

import static org.joda.time.Period.hours;
import static org.joda.time.Period.minutes;

/**
 * Created by Andrey on 7/12/2014.
 */
public class CompletedActivityFragmentsDAO {
    private static final String TAG = "ADP_CompletedActivityFragmentsDAO";

    public void add(ActivityFragment fragment) {

    }

    public List<ActivityFragment> getAround(DateTime dateTime) {
        Log.d(TAG, "getAround called");
        ArrayList<ActivityFragment> fragments = getActivityFragments(dateTime);
        return ActivityFragment.defragment(fragments);
    }

    private ArrayList<ActivityFragment> getActivityFragments(DateTime dateTime) {
        DateTime start = dateTime.minus(hours(2));
        return Lists.newArrayList(
                new ActivityFragment("Work Out", start, start.plus(minutes(23))),
                new ActivityFragment("Stretch", start.plus(minutes(23)), start.plus(minutes(40))),
                new ActivityFragment("Eat", start.plus(minutes(43)), start.plus(minutes(65))),
                new ActivityFragment("Program", start.plus(minutes(72)), start.plus(minutes(100)))
        );
    }
}
