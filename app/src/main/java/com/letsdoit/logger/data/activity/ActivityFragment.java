package com.letsdoit.logger.data.activity;

import android.util.Pair;

import com.google.common.base.Preconditions;

import org.apache.commons.lang3.StringUtils;

import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.Period;

/**
 * An activity can be of any duration.  This causes challenges when it comes to answering "what activites were
 * happening during this hour."  Some examples:
 *
 * (a) Activity started at 7am and ended at 9am.  You want to display the activities that happened at 8am.
 * (b) Activity started at 7am two days ago and ended at 9am a week from now.  You want to display the activities that
 * happened at 8am today.
 *
 * Do you query for 2 hours around the target hour (would work for (a)?  2 weeks (would work for (b),
 * but seems like overkill)?
 *
 * By splitting the activities into MAX_FRAGMENT_DURATION fragments, we know that we only need to look at
 *
 * (INSTANT - MAX_FRAGMENT_DURATION) to (INSTANT + MAX_FRAGMENT_DURATION)
 *
 * The fragments know everything about the activity to which they refer.
 *
 * Created by Andrey on 7/12/2014.
 */
public class ActivityFragment {
    /**
     * An activity that was happening at a specific instant will have a fragment that started no longer than this
     * duration relative to the instant and ended no later than this duration relative to this instant.  This makes
     * figuring out "what activities were happening in this hour" much simpler than having to try to figure out when
     * the previous activity that ran into this hour started or whether there was an activity that started in this
     * hour and ran over.
     */
    private static final Duration MAX_FRAGMENT_DURATION = new Duration(60 * 60 * 1000);

    private final String activityName;

    private final DateTime activityStart;
    private final DateTime activityEnd;

    private final DateTime fragmentStart;
    private final DateTime fragmentEnd;


    public ActivityFragment(String activityName, DateTime activityStart, DateTime activityEnd, DateTime fragmentStart,
                            DateTime fragmentEnd) {
        Preconditions.checkArgument(StringUtils.isEmpty(activityName), "Activity name cannot be empty.");

        Preconditions.checkArgument(activityStart.isBefore(activityEnd),
                "Activity start date must be before the end date.");

        Preconditions.checkArgument(fragmentStart.isBefore(fragmentEnd),
                "Fragment start date must be before fragment end date.");

        Preconditions.checkArgument(false == activityStart.isAfter(fragmentStart),
                "Fragment start date cannot be earlier than activity start date.");
        Preconditions.checkArgument(false == activityEnd.isBefore(fragmentEnd),
                "Fragment end date cannot be later than activity end date.");

        Preconditions.checkArgument(false == new Duration(fragmentStart,
                fragmentEnd).isLongerThan(MAX_FRAGMENT_DURATION), "Fragment cannot be longer than " +
                MAX_FRAGMENT_DURATION);

        this.activityStart = activityStart;
        this.activityEnd = activityEnd;
        this.fragmentStart = fragmentStart;
        this.fragmentEnd = fragmentEnd;
        this.activityName = activityName;
    }

    public ActivityFragment(String activityName, DateTime activityStart, DateTime activityEnd) {
        this(activityName, activityStart, activityEnd, activityStart, activityEnd);
    }

    public Pair<ActivityFragment, ActivityFragment> splitAtTime(DateTime splitTime) {
        Preconditions.checkArgument(splitTime.isAfter(fragmentStart), "The split time has to be after the fragment " +
                "start time");
        Preconditions.checkArgument(splitTime.isBefore(fragmentEnd), "The split time has to be before the fragment " +
                "end time");

        ActivityFragment first = new ActivityFragment(activityName, activityStart, activityEnd, fragmentStart,
                splitTime);
        ActivityFragment second = new ActivityFragment(activityName, activityStart, activityEnd, splitTime,
                fragmentEnd);

        return new Pair<ActivityFragment, ActivityFragment>(first, second);
    }

//    public static ActivityFragment merge(ActivityFragment first, ActivityFragment second) {
//        Preconditions.checkArgument(first.activityName.equals(second.activityName),
//                "Merged activities need to have the same activity name.");
//
//        Preconditions.checkArgument(first.activityStart.equals(second.activityStart),
//                "Merged activities need to have the same activity start time.");
//        Preconditions.checkArgument(first.activityEnd.equals(second.activityEnd),
//                "Merged activities need to have the same activity end time.");
//
//        Preconditions.checkArgument(first.fragmentEnd.equals(second.fragmentStart),
//                "The first activity needs to end at the same time as the second activity starts when merging.");
//
//        return new ActivityFragment(first.getActivityName(), first.getActivityStart(), first.getActivityEnd(),
//                first.getFragmentStart(), second.getFragmentEnd());
//    }

    public String getActivityName() {
        return activityName;
    }

    public DateTime getActivityStart() {
        return activityStart;
    }

    public DateTime getActivityEnd() {
        return activityEnd;
    }

    public DateTime getFragmentStart() {
        return fragmentStart;
    }

    public DateTime getFragmentEnd() {
        return fragmentEnd;
    }

}
