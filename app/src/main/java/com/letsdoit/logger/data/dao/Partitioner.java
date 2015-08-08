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
 * Activities get broken into fragments that fit inside the partitions.
 *
 * Created by Andrey on 6/14/2015.
 */
public class Partitioner {

    /**
     * Split the time between the start and end into intervals of the specified duration. Separate any Activities
     * that cross an interval boundary into separate fragments, putting the earlier one into the earlier interval
     * and the later ones into the later.
     *
     * @param activities - List of complete Activities to chop up into intervals.  Activities will be chopped into
     *                   ActivityFragments representing a portion of the Activity that falls into the intervals.
     * @param start - Activities ending before the start will be excluded.  Activities starting before the start will be
     *              chopped to only include the part that falls into the [start, end) period.
     * @param end - Activities starting after the end will be excluded.  Activities starting before the end and
     *            ending after the end will be chopped to only include the part that falls into the [start, end) period.
     * @param intervalDuration - How long to make the ActivityIntervals.  Will determine where the Activities will get
     *                         split into Fragments inside of the [start, end) period.
     * @return the list of ActivityIntervals containing ActivityFragments for all of the Activities that fall
     * into the [start, end) period.
     */
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

    /**
     * Find the first activity that's not completely before the start, if there is one.
     *
     * @param start - the time after which the Activity should start
     * @param activityIter - an iterator through the list of Activities
     * @return the first activity that's not completely before the start, or null if none exist.
     */
    private static Activity findFirstActivityEndingAfter(DateTime start, ListIterator<Activity> activityIter) {

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

    /**
     * Use the activity and activityIter to create the list of ActivityFragments that should go into the specified
     * interval.  This method is private and intimately tied to the partition() method above.
     *
     * This method has two outputs - the activity that it returns and the list of fragments that it mutates.  The
     * activity acts both as the "current activity" as well as the "we are out of activities" flag when it is null.
     * The fragments are used directly to create an ActivityInterval.
     *
     * @param intervalStart - the start time of the interval
     * @param intervalEnd - the end time of the interval
     * @param activity - the current activity under consideration.  Will be null if there are no more activities.
     * @param activityIter - an iterator through the remaining Activites.
     * @param fragments - will contain the list of ActivityFragments falling between intervalStart and intervalEnd.
     * @return the current activity or null if there are no more activities.  Also, the ActivityFragments that
     * have been added to the fragments list.
     */
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
