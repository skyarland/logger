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
    private static final String TAG = "ADP_ActivityFragment";

    private final String activityName;

    private final DateTime activityStart;
    private final DateTime activityEnd;

    private final DateTime start;
    private final DateTime end;

    public ActivityFragment(String activityName, DateTime activityStart, DateTime activityEnd, DateTime start,
                            DateTime end) {
        Preconditions.checkArgument(!StringUtils.isEmpty(activityName), "Activity name cannot be empty.");

        Preconditions.checkArgument(activityStart.isBefore(activityEnd),
                "Activity start date must be before the end date.");

        Preconditions.checkArgument(start.isBefore(end),
                "Fragment start date must be before fragment end date.");

        Preconditions.checkArgument(!activityStart.isAfter(start),
                "Fragment start date cannot be earlier than activity start date.");
        Preconditions.checkArgument(!activityEnd.isBefore(end),
                "Fragment end date cannot be later than activity end date.");

//        Preconditions.checkArgument(false == new Duration(start,
//                end).isLongerThan(MAX_FRAGMENT_DURATION), "Fragment cannot be longer than " +
//                MAX_FRAGMENT_DURATION);

        this.activityStart = activityStart;
        this.activityEnd = activityEnd;
        this.start = start;
        this.end = end;
        this.activityName = activityName;
    }

    public ActivityFragment(String activityName, DateTime activityStart, DateTime activityEnd) {
        this(activityName, activityStart, activityEnd, activityStart, activityEnd);
    }

    public Pair<ActivityFragment, ActivityFragment> splitAtTime(DateTime splitTime) {
        Preconditions.checkArgument(splitTime.isAfter(start), "The split time has to be after the fragment " +
                "start time");
        Preconditions.checkArgument(splitTime.isBefore(end), "The split time has to be before the fragment " +
                "end time");

        ActivityFragment first = new ActivityFragment(activityName, activityStart, activityEnd, start,
                splitTime);
        ActivityFragment second = new ActivityFragment(activityName, activityStart, activityEnd, splitTime,
                end);

        return new Pair<ActivityFragment, ActivityFragment>(first, second);
    }

    public static List<ActivityInterval> partition(DateTime startTime, DateTime endTime, Period interval,
                                                         List<ActivityFragment> fragments) {

        Preconditions.checkArgument(!startTime.isAfter(endTime), "The partition start time must not be after the " +
                "partition end time");

        if (!fragments.isEmpty()) {
            ActivityFragment first = fragments.get(0);
            Preconditions.checkArgument(!first.getStart().isBefore(startTime),
                    "The first fragment cannot start before the partitioning period");

            ActivityFragment last = fragments.get(fragments.size() - 1);
            Preconditions.checkArgument(!last.getEnd().isAfter(endTime),
                    "The last fragment cannot end after the partitioning period");
        }

        List<ActivityInterval> intervals = Lists.newArrayList();
        Iterator<ActivityFragment> fragmentIterator = fragments.iterator();
        ActivityFragment fragment = fragmentIterator.hasNext() ? fragmentIterator.next() : null;
        DateTime startOfInterval = startTime;

        while (startOfInterval.isBefore(endTime)) {
            DateTime endOfInterval = startOfInterval.plus(interval);
            List<ActivityFragment> intervalFragments = Lists.newArrayList();

            while (fragment != null && fragment.getStart().isBefore(endOfInterval)) {
                if (fragment.getEnd().isAfter(endOfInterval)) {
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

        Preconditions.checkArgument(!first.end.isAfter(second.start),
                "The first activity cannot end after the second activity starts when merging.  First=" + first +
                        " Second=" + second);

        return new ActivityFragment(first.getActivityName(), first.getActivityStart(), first.getActivityEnd(),
                first.getStart(), second.getEnd());
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
        for (splitTime = activity.getStart().plus(maxFragmentDuration);
             splitTime.isBefore(activity.getEnd());
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

    public DateTime getStart() {
        return start;
    }

    public DateTime getEnd() {
        return end;
    }

    public boolean isSameActivityAs(ActivityFragment other) {
        return other != null && activityName.equals(other.activityName) && activityStart.equals(other.activityStart) &&
                activityEnd.equals(other.activityEnd);
    }

    public Duration getDuration() {
        return new Duration(start, end);
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
                ", start=" + start +
                ", end=" + end +
                '}';
    }

    public ActivityFragment clipEnd(DateTime end) {
        if (this.end.isAfter(end)) {
            return new ActivityFragment(activityName, activityStart, activityEnd, start, end);
        }
        return this;
    }

    public ActivityFragment clipStart(DateTime start) {
        if (this.start.isBefore(start)) {
            return new ActivityFragment(activityName, activityStart, activityEnd, start, end);
        }
        return this;
    }


    public ActivityFragment clip(DateTime start, DateTime end) {
        return clipStart(start).clipEnd(end);
    }
}
