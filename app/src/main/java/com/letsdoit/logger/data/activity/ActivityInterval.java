package com.letsdoit.logger.data.activity;

import org.joda.time.DateTime;

import java.util.List;

/**
 * Created by Andrey on 7/14/2014.
 */
public class ActivityInterval {
    private final DateTime startTime;
    private final DateTime endTime;
    private final List<ActivityFragment> fragments;

    public ActivityInterval(DateTime startTime, DateTime endTime, List<ActivityFragment> fragments) {
        this.startTime = startTime;
        this.endTime = endTime;
        this.fragments = fragments;
    }

    public DateTime getStartTime() {
        return startTime;
    }

    public DateTime getEndTime() {
        return endTime;
    }

    public List<ActivityFragment> getFragments() {
        return fragments;
    }
}
