package com.letsdoit.logger.data.dao;

import com.google.common.collect.Lists;

import org.joda.time.DateTime;
import org.joda.time.Duration;

import java.util.List;

/**
 * Created by Andrey on 6/13/2015.
 */
public class Fragmenter {

    /**
     * Chops up the activity into fragments no larger than the maxDuration.
     * [---|---|-]
     *
     * @param activity the activity that might be longer than the max duration
     * @param maxDuration the duration after which activities start getting chopped up
     * @return list of fragments for the activity with fragment larger than the maxDuration
     */
    public static List<ActivityFragment> fragment(Activity activity, Duration maxDuration) {
        List<ActivityFragment> fragments = Lists.newArrayList();
        fragmentAndAppend(activity, maxDuration, fragments);
        return fragments;
    }

    /**
     * Chop up the activity into fragments no longer than the maxDuration and append the fragments to the list.
     *
     * @param activity the activity that might be longer than the max duration
     * @param maxDuration the duration after which activities start getting chopped up
     * @param fragments list of fragments for the activity with fragment larger than the maxDuration
     */
    private static void fragmentAndAppend(Activity activity, Duration maxDuration, List<ActivityFragment> fragments) {
        DateTime fragmentStart = activity.getActivityStart();
        DateTime partitionIfStartBefore = activity.getActivityEnd().minus(maxDuration);

        // Make as many fragments of maxDuration size as you can
        while (fragmentStart.isBefore(partitionIfStartBefore)) {
            fragments.add(new ActivityFragment(
                    activity.getActivityName(),
                    activity.getActivityStart(), activity.getActivityEnd(),
                    fragmentStart, fragmentStart.plus(maxDuration)));
            fragmentStart = fragmentStart.plus(maxDuration);
        }

        // Add the last fragment, smaller than the maxDuration
        fragments.add(new ActivityFragment(
                activity.getActivityName(),
                activity.getActivityStart(), activity.getActivityEnd(),
                fragmentStart, activity.getActivityEnd()));
    }

    /**
     * Chops up a list of Activities, making sure no fragment is larger than the maxDuration.
     * Used for generating test data.
     *
     * @param activities list of activities, potentially longer than the maxDuration
     * @param maxDuration the duration after which activities start getting chopped up
     * @return list of fragments for all of the activities with no fragment larger than the maxDuration
     */
    protected static List<ActivityFragment> fragment(List<Activity> activities, Duration maxDuration) {
        List<ActivityFragment> fragments = Lists.newArrayList();
        for(Activity activity : activities) {
            fragmentAndAppend(activity, maxDuration, fragments);
        }
        return fragments;
    }

    /**
     * Convert a list of ActivityFragments sorted by fragmentStartTime into full Activities.
     * Merge Activities that have been split into multiple fragments.
     * Expand Activities that have just the tail or the head fragments in the list.
     *
     * Assumes there are no overlapping Activities.
     *
     * @param fragments List of ActivityFragments sorted by fragmentStartTime
     * @return List of Activities sorted by Activity startTime
     */
    public static List<Activity> defragment(List<ActivityFragment> fragments) {
        Activity previousActivity = null;
        List<Activity> activities = Lists.newArrayList();
        for (ActivityFragment fragment : fragments) {

            // Skip subsequent fragments from the same Activity
            if (previousActivity != null &&
                    previousActivity.getActivityName().equals(fragment.getActivityName()) &&
                    previousActivity.getActivityStart().equals(fragment.getActivityStart())) {
                continue;
            }

            Activity activity = new Activity(fragment.getActivityName(),
                    fragment.getActivityStart(),fragment.getActivityEnd());

            activities.add(activity);
            previousActivity = activity;
        }

        return activities;
    }

}
