package com.letsdoit.logger.view;

import android.util.Log;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.letsdoit.logger.data.dao.ActivityFragment;
import com.letsdoit.logger.data.dao.ActivityInterval;

import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.Period;

import java.util.Iterator;
import java.util.List;


/**
 * Created by Andrey on 7/15/2014.
 */
public class DisplayBlock {
    private static final String TAG = "ADP_DisplayBlock";

    final boolean hasActivityFragment;
    final ActivityFragment activityFragment;
    final EmptyInterval emptyInterval;

    public DisplayBlock(ActivityFragment activityFragment) {
        this.hasActivityFragment = true;
        this.activityFragment = activityFragment;
        this.emptyInterval = null;
    }

    public DisplayBlock(EmptyInterval emptyInterval) {
        this.hasActivityFragment = false;
        this.activityFragment = null;
        this.emptyInterval = emptyInterval;
    }

    public boolean hasActivityFragment() {
        return hasActivityFragment;
    }

    public ActivityFragment getActivityFragment() {
        Preconditions.checkArgument(hasActivityFragment, "Cannot retrieve activity fragment from a display block with" +
                " an empty interval.");
        return activityFragment;
    }

    public EmptyInterval getEmptyInterval() {
        Preconditions.checkArgument(false == hasActivityFragment, "Cannot retrieve empty interval from a display " +
                "block with an activity fragment.");
        return emptyInterval;
    }

    public DateTime getStart() {
        return hasActivityFragment ? activityFragment.getStart() : emptyInterval.getStart();
    }

    public DateTime getEnd() {
        return hasActivityFragment ? activityFragment.getEnd() : emptyInterval.getEnd();
    }

    @Override
    public String toString() {
        return "DisplayBlock{" +
                "hasActivityFragment=" + hasActivityFragment +
                ", activityFragment=" + activityFragment +
                ", emptyInterval=" + emptyInterval +
                '}';
    }

    public static List<ActivityInterval> wrapFragmentsAndClipFreeTime(ActivityInterval interval, Duration spacing) {
        DateTime start = interval.getStart();
        DateTime end = interval.getEnd();
        List<ActivityFragment> fragments = interval.getFragments();

        Preconditions.checkArgument(start.isBefore(end), "The start time needs to be before the end time.");
        Log.d(TAG, "Start=[" + start + "] End=[" + end + "]");

        List<ActivityInterval> blocks = Lists.newLinkedList();
        DateTime prevFragmentEnd = start;

        for (ActivityFragment fragment : fragments) {
            Log.d(TAG, "Fragment=[" + fragment + "]");
            if (fragment.getEnd().isBefore(start)) {
                // Skip it, it's not in the specified period
                Log.d(TAG, "Fragment ends before the start of the interval.  Continuing.");
                continue;
            } else if (fragment.getStart().isAfter(end)) {
                // We're past the specified period
                Log.d(TAG, "Fragment starts after the end of the interval.  Breaking.");
                break;
            } else if (fragment.getStart().isBefore(start)) {
                // Clip the start
                Log.d(TAG, "Fragment starts before the interval, but ends inside it.  Clipping start and Adding.");
                blocks.add(ActivityInterval.fromFragment(fragment.clip(start, end)));
            } else if (prevFragmentEnd.isBefore(fragment.getStart())) {
                // There is empty space between the previous fragment and the current one
                // Fill it with empty blocks
                Log.d(TAG, "There is space between the last fragment and the current one.  Adding empty space and " +
                        "adding the new Fragment afterwards.");
                blocks.addAll(EmptyInterval.makeEmptyBlocks(start, prevFragmentEnd, fragment.getStart(), spacing));
                blocks.add(ActivityInterval.fromFragment(fragment.clipEnd(end)));
            } else if (prevFragmentEnd.equals(fragment.getStart())) {
                // This fragment starts right after the previous fragment
                // Render it
                Log.d(TAG, "Fragment is inside the interval and right after the previous fragment.  Adding.");
                blocks.add(ActivityInterval.fromFragment(fragment.clipEnd(end)));
            } else {
                // This should never happen
                Log.e(TAG, String.format("Activity fragment %s starts before the previous  fragment ends %s",
                        fragment, prevFragmentEnd));
            }
            prevFragmentEnd = fragment.getEnd();
        }

        if (prevFragmentEnd.isBefore(end)) {
            Log.d(TAG, "Adding trailing empty blocks to fill out the interval.");
            blocks.addAll(EmptyInterval.makeEmptyBlocks(start, prevFragmentEnd, end, spacing));
        }

        return blocks;
    }

    public static List<ActivityInterval> mergeTooSmall(List<ActivityInterval> blocks, Duration minDuration) {
        Preconditions.checkArgument(!blocks.isEmpty(), "The list of blocks cannot be empty");

        List<ActivityInterval> merged = Lists.newLinkedList();
        DateTime intervalEnd = blocks.get(blocks.size() - 1).getEnd();

        Duration durationToIntervalEnd = new Duration(blocks.get(0).getStart(), intervalEnd);

        Iterator<ActivityInterval> blocksIter = blocks.iterator();

        ActivityInterval prev = blocksIter.next();
        ActivityInterval curr = null;
        ActivityInterval next = null;

        while (blocksIter.hasNext() && !minDuration.isLongerThan(durationToIntervalEnd)) {
            ActivityInterval block = blocksIter.next();

            if (prev == null) {
                prev = block;
            } else if (curr == null) {
                curr = block;
            } else if (next == null) {
                next = block;
            }

            // Make sure there is always enough space at the end, in case the last blocks are too small
            durationToIntervalEnd = new Duration(prev.getEnd(), intervalEnd);

            // fill up all of the buffers first
            if (prev == null || curr == null || next == null) {
                continue;
            }

            if (prev.getDuration().isShorterThan(minDuration)) {
                prev = prev.extendWith(curr);
                curr = next;
                next = null;
            } else if (curr.getDuration().isShorterThan(minDuration)) {
                // merge free time with free time, if possible
                if (curr.isEmpty()) {
                    if (prev.isEmpty()) {
                        prev = prev.extendWith(curr);
                        curr = next;
                        next = null;
                    } else if (next.isEmpty()) {
                        curr = curr.extendWith(next);
                        next = null;
                    }
                } else { // curr is not empty
                    // merge with the smaller block
                    if (prev.getDuration().isShorterThan(next.getDuration())) {
                        prev = prev.extendWith(curr);
                        curr = next;
                        next = null;
                    } else {
                        curr = curr.extendWith(next);
                        next = null;
                    }
                }
            }

            // If all of the buffers are full and prev is long enough, make space for the next block by putting prev
            // into the output
            if (next != null) {
                Preconditions.checkState(!prev.getDuration().isShorterThan(minDuration),
                        String.format("Trying to add prev  shorter than the minDuration to the merged output.  Merged" +
                                " so far: %s, prev: %s, curr: %s, next: %s.", merged, prev, curr, next));
                merged.add(prev);
                prev = curr;
                curr = next;
                next = null;
            }
        }

        Preconditions.checkState(next == null, "Next should be null");

        // There's not enough room after prev, so append everything to prev
        if (minDuration.isLongerThan(durationToIntervalEnd)) {
            if (curr != null) {
                prev = prev.extendWith(curr);
            }
            while (blocksIter.hasNext()) {
                prev = prev.extendWith(blocksIter.next());
            }
            merged.add(prev);
        } else {
            // There's room after prev, we just ran out blocks
            // Make sure prev is long enough
            if (prev.getDuration().isShorterThan(minDuration)) {
                // if prev is too short, but we didn't run out of space, then curr must exist
                prev = prev.extendWith(curr);
                merged.add(prev);
            } else {
                if (curr != null && curr.getDuration().isShorterThan(minDuration)) {
                    prev = prev.extendWith(curr);
                    merged.add(prev);
                } else {
                    merged.add(prev);
                    if (curr != null) {
                        merged.add(curr);
                    }
                }
            }
        }

        return merged;
    }
}
