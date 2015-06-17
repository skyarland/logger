package com.letsdoit.logger.data.dao;

import org.joda.time.DateTime;
import org.joda.time.Duration;

/**
 * Activities represent something a user has done at a specific period of time.
 * Activities can be of any duration.
 *
 * Activities can be subdivided into ActivityFragments for storage and display.
 *
 * Created by Andrey on 6/13/2015.
 */
public class Activity {
    private static final String TAG = "ADP_Activity";

    private final String activityName;

    private final DateTime activityStart;
    private final DateTime activityEnd;

    private final Duration cachedDuration;

    public Activity(String activityName, DateTime activityStart, DateTime activityEnd) {
        this.activityName = activityName;
        this.activityStart = activityStart;
        this.activityEnd = activityEnd;
        this.cachedDuration = new Duration(activityStart, activityEnd);
    }

    public String getActivityName() {
        return activityName;
    }

    public DateTime getActivityStart() {
        return activityStart;
    }

    public DateTime getActivityEnd() {
        return activityEnd;
    }

    public Duration getActivityDuration() {
        return cachedDuration;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Activity activity = (Activity) o;

        if (!activityEnd.equals(activity.activityEnd)) return false;
        if (!activityName.equals(activity.activityName)) return false;
        if (!activityStart.equals(activity.activityStart)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = activityName.hashCode();
        result = 31 * result + activityStart.hashCode();
        result = 31 * result + activityEnd.hashCode();
        return result;
    }
}
