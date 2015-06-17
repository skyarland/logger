package com.letsdoit.logger.data.dao;

import com.google.common.collect.Lists;

import org.joda.time.DateTime;
import org.joda.time.Duration;

import java.util.List;
import java.util.ListIterator;

/**
 * Converts a list of Activities into a list of ActivityIntervals.
 *
 * Each activity interval represents the same partition of time (e.g. every half hour).
 *
 * Created by Andrey on 6/14/2015.
 */
public class Partitioner {

    public static List<ActivityInterval> partition(
            List<Activity> activities,
            DateTime start, DateTime end,
            Duration intervalDuration) {

        ListIterator<Activity> activityIter = activities.listIterator();
        Activity activity = findFirstActivityEndingAfter(start, activityIter);

        List<ActivityInterval> intervals = Lists.newArrayList();
        DateTime intervalStart = start;

        while (intervalStart.isBefore(end)) {
            DateTime intervalEnd = intervalStart.plus(intervalDuration);
            List<ActivityFragment> fragments = Lists.newArrayList();

            activity = populateFragmentsForInterval(intervalStart, intervalEnd, activity, activityIter, fragments);

            ActivityInterval interval = new ActivityInterval(intervalStart, intervalEnd, fragments);
            intervals.add(interval);

            intervalStart = intervalEnd;
        }
        return intervals;
    }

    private static Activity findFirstActivityEndingAfter(DateTime start, ListIterator<Activity> activityIter) {
        // Find the first activity that's not completely before the start, if there is one.
        Activity activity = null;
        if (activityIter.hasNext()) {
            activity = activityIter.next();

            // Skip all of the activities completely before the start
            while(!activity.getActivityEnd().isAfter(start) && activityIter.hasNext()) {
                activity = activityIter.next();
            }

            // If the last element is still completely before the start,
            // flag that there are no activities in the interval
            if (!activity.getActivityEnd().isAfter(start)) {
                activity = null;
            }
        }
        return activity;
    }

    private static Activity populateFragmentsForInterval(
            DateTime intervalStart, DateTime intervalEnd,
            Activity activity, ListIterator<Activity> activityIter,
            List<ActivityFragment> fragments) {

        DateTime fragmentEnd = intervalStart;
        while (
                // We haven't run out of activities
                activity != null &&

                // The activity isn't past this interval
                activity.getActivityStart().isBefore(intervalEnd) &&

                // We haven't added the fragment for the case that
                // the activity spans before and after the interval
                fragmentEnd.isBefore(intervalEnd)) {

            DateTime fragmentStart;
            // Clip the fragmentStart to the intervalStart
            if (activity.getActivityStart().isBefore(intervalStart)) {
                fragmentStart = intervalStart;
            } else {
                fragmentStart = activity.getActivityStart();
            }

            // Clip the fragmentEnd to the intervalEnd
            if (activity.getActivityEnd().isAfter(intervalEnd)) {
                fragmentEnd = intervalEnd;
            } else {
                fragmentEnd = activity.getActivityEnd();
            }

            fragments.add(new ActivityFragment(
                    activity.getActivityName(),
                    activity.getActivityStart(), activity.getActivityEnd(),
                    fragmentStart, fragmentEnd));

            // If we're done with this activity, grab the next one, if it exists,
            // or flag that we are out of activities.
            if (activity.getActivityEnd().isEqual(fragmentEnd)) {
                if (activityIter.hasNext()) {
                    activity = activityIter.next();
                } else {
                    activity = null;
                }
            }
        }
        return activity;
    }
}
