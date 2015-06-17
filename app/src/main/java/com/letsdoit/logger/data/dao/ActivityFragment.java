package com.letsdoit.logger.data.dao;

import android.util.Log;
import android.util.Pair;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

import org.apache.commons.lang3.StringUtils;

import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.Period;

import java.util.Iterator;
import java.util.List;

/**
 * Represents a sub-chunk of time of an Activity.
 *
 * Contains all of the information of an Activity along with the specific time range this
 * ActivityFragment is covering.
 *
 * Since Activities can be an arbitrary duration, they can easily fall outside of any
 * specific time window.  By chopping them up into smaller pieces, we can make sure that
 * each of the pieces is bounded in size.  By duplicating the Activity data, we can
 * reconstruct the entire activity from just one fragment, even it if is larger.
 *
 * Created by Andrey on 7/12/2014.
 */
public class ActivityFragment {
    private static final String TAG = "ADP_ActivityFragment";

    private final String activityName;

    private final DateTime activityStart;
    private final DateTime activityEnd;

    private final DateTime fragmentStart;
    private final DateTime fragmentEnd;

    public ActivityFragment(
            String activityName,
            DateTime activityStart, DateTime activityEnd,
            DateTime fragmentStart, DateTime fragmentEnd) {
        Preconditions.checkArgument(!StringUtils.isEmpty(activityName), "Activity name cannot be empty.");

        Preconditions.checkArgument(activityStart.isBefore(activityEnd),
                "Activity start date must be before the end date.");

        Preconditions.checkArgument(fragmentStart.isBefore(fragmentEnd),
                "Fragment start date must be before fragment end date.");

        Preconditions.checkArgument(!activityStart.isAfter(fragmentStart),
                "Fragment start date cannot be earlier than activity start date.");
        Preconditions.checkArgument(!activityEnd.isBefore(fragmentEnd),
                "Fragment end date cannot be later than activity end date.");

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

    public static List<ActivityInterval> partition(
            DateTime startTime, DateTime endTime,
            Period interval,
            List<ActivityFragment> fragments) {

        Preconditions.checkArgument(!startTime.isAfter(endTime), "The partition start time must not be after the " +
                "partition end time");

        if (!fragments.isEmpty()) {
            ActivityFragment first = fragments.get(0);
            Preconditions.checkArgument(!first.getFragmentStart().isBefore(startTime),
                    String.format("The first fragment cannot start before the partitioning period\n" +
                            "Start time: %s\n" +
                            "First fragment: %s",
                            startTime, first));

            ActivityFragment last = fragments.get(fragments.size() - 1);
            Preconditions.checkArgument(!last.getFragmentEnd().isAfter(endTime),
                    String.format("The last fragment cannot end after the partitioning period.\n" +
                            "End time: %s\n" +
                            "Last fragment: %s",
                            endTime, last));
        }

        List<ActivityInterval> intervals = Lists.newArrayList();
        Iterator<ActivityFragment> fragmentIterator = fragments.iterator();
        ActivityFragment fragment = fragmentIterator.hasNext() ? fragmentIterator.next() : null;
        DateTime startOfInterval = startTime;

        while (startOfInterval.isBefore(endTime)) {
            DateTime endOfInterval = startOfInterval.plus(interval);
            List<ActivityFragment> intervalFragments = Lists.newArrayList();

            while (fragment != null && fragment.getFragmentStart().isBefore(endOfInterval)) {
                if (fragment.getFragmentEnd().isAfter(endOfInterval)) {
                    // Split fragments that cross the hour
                    Pair<ActivityFragment, ActivityFragment> split = fragment.splitAtTime(endOfInterval);
                    intervalFragments.add(split.first);
                    // This will will break from this while loop, since the fragment start will be endOfInterval
                    fragment = split.second;
                } else {
                    intervalFragments.add(fragment);
                    if (fragmentIterator.hasNext()) {
                        fragment = fragmentIterator.next();
                    } else {
                        fragment = null;
                    }
                }
            }

            ActivityInterval activityInterval = new ActivityInterval(startOfInterval, endOfInterval, intervalFragments);
            Log.d(TAG, activityInterval.toString());
            intervals.add(activityInterval);
            startOfInterval = endOfInterval;
        }

        return intervals;
    }

    public static ActivityFragment mergeAndInterpolate(ActivityFragment first, ActivityFragment second) {
        Preconditions.checkArgument(first.activityName.equals(second.activityName),
                "Merged activities need to have the same activity name.  First=" + first + " Second=" + second);

        Preconditions.checkArgument(first.activityStart.equals(second.activityStart),
                "Merged activities need to have the same activity start time.  First=" + first + " Second=" + second);
        Preconditions.checkArgument(first.activityEnd.equals(second.activityEnd),
                "Merged activities need to have the same activity end time.  First=" + first + " Second=" + second);

        Preconditions.checkArgument(!first.fragmentEnd.isAfter(second.fragmentStart),
                "The first activity cannot end after the second activity starts when merging.  First=" + first +
                        " Second=" + second);

        return new ActivityFragment(first.getActivityName(), first.getActivityStart(), first.getActivityEnd(),
                first.getFragmentStart(), second.getFragmentEnd());
    }

    /**
     * Split the activity into fragments of at most the specified duration.
     *
     * @param activity
     * @param maxFragmentDuration
     * @return
     */
    public static List<ActivityFragment> fragment(ActivityFragment activity, Duration maxFragmentDuration) {
        List<ActivityFragment> fragments = Lists.newArrayList();

        DateTime splitTime;
        ActivityFragment rest = activity;
        for (splitTime = activity.getFragmentStart().plus(maxFragmentDuration);
             splitTime.isBefore(activity.getFragmentEnd());
             splitTime = splitTime.plus(maxFragmentDuration)) {
            Pair<ActivityFragment, ActivityFragment> firstRest = rest.splitAtTime(splitTime);
            fragments.add(firstRest.first);
            rest = firstRest.second;
        }
        fragments.add(rest);

        return fragments;
    }

    public static List<ActivityFragment> defragment(List<ActivityFragment> splitFragments) {
        List<ActivityFragment> fragments = Lists.newArrayList();

        ActivityFragment mergeBlockStart = null;
        ActivityFragment mergeBlockEnd = null;

        Iterator<ActivityFragment> iterator = splitFragments.iterator();
        ActivityFragment next = null;

        while (iterator.hasNext()) {
            next = iterator.next();
            Log.d(TAG, next.toString());
            if (mergeBlockStart == null) {
                Log.d(TAG, "Start block was null.  Saving fragment in start block.");
                mergeBlockStart = next;
            } else if (next.isSameActivityAs(mergeBlockStart)) {
                Log.d(TAG, "Current activity is the same as the start block's.  Writing fragment to the end block.");
                mergeBlockEnd = next;
            } else if (mergeBlockEnd != null) {
                Log.d(TAG, "There was an end block, but the current fragment didn't match the start block's activity." +
                        "  Merging the start and end blocks and resetting the start block.");
                fragments.add(mergeAndInterpolate(mergeBlockStart, mergeBlockEnd));
                mergeBlockStart = next;
                mergeBlockEnd = null;
            } else if(mergeBlockStart != null) {
                Log.d(TAG, "Have a start block but no end block.  Adding start block and resetting the start block.");
                fragments.add(mergeBlockStart);
                mergeBlockStart = next;
            } else {
                Preconditions.checkArgument(false, "Unexpected end state for main ActivityFragment merge");
            }
        }

        if (next != null) {
            if (mergeBlockEnd != null) {
                // previous is merge, last one is not
                fragments.add(mergeAndInterpolate(mergeBlockStart, mergeBlockEnd));
            } else if (mergeBlockStart != null) {
                fragments.add(mergeBlockStart);
                // last is non-merge
                if (next != mergeBlockStart) {
                    fragments.add(next);
                }
            } else {
                // Throw an exception - this should never happen
                Preconditions.checkArgument(false, "Unexpected end state for end of ActivityFragment merge");
            }
        }

        return fragments;
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

    public DateTime getFragmentStart() {
        return fragmentStart;
    }

    public DateTime getFragmentEnd() {
        return fragmentEnd;
    }

    public boolean isSameActivityAs(ActivityFragment other) {
        return other != null && activityName.equals(other.activityName) && activityStart.equals(other.activityStart) &&
                activityEnd.equals(other.activityEnd);
    }

    public Duration getDuration() {
        return new Duration(fragmentStart, fragmentEnd);
    }

    public Duration getActivityDuration() {
        return new Duration(activityStart, activityEnd);
    }

    @Override
    public String toString() {
        return "ActivityFragment{" +
                "activityName='" + activityName + '\'' +
                ", activityStart=" + activityStart +
                ", activityEnd=" + activityEnd +
                ", start=" + fragmentStart +
                ", end=" + fragmentEnd +
                '}';
    }

    public ActivityFragment clipEnd(DateTime end) {
        if (this.fragmentEnd.isAfter(end)) {
            return new ActivityFragment(activityName, activityStart, activityEnd, fragmentStart, end);
        }
        return this;
    }

    public ActivityFragment clipStart(DateTime start) {
        if (this.fragmentStart.isBefore(start)) {
            return new ActivityFragment(activityName, activityStart, activityEnd, start, fragmentEnd);
        }
        return this;
    }


    public ActivityFragment clip(DateTime start, DateTime end) {
        return clipStart(start).clipEnd(end);
    }
}
