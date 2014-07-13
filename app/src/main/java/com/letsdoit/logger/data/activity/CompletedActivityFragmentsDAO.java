package com.letsdoit.logger.data.activity;

import android.util.Log;

import com.google.common.collect.Lists;

import org.joda.time.DateTime;
import org.joda.time.Period;

import java.util.List;

/**
 * Created by Andrey on 7/12/2014.
 */
public class CompletedActivityFragmentsDAO {
    private static final String TAG = "ADP_CompletedActivityFragmentsDAO";

    public void add(ActivityFragment fragment) {

    }

    public List<ActivityFragment> getAround(DateTime dateTime) {
        Log.d(TAG, "getAround called");
        DateTime start = dateTime.minus(Period.minutes(30));
        DateTime end = dateTime.minus(Period.minutes(5));
        ActivityFragment fragment = new ActivityFragment("Work Out", start, end);
        return Lists.newArrayList(fragment);
    }
}
