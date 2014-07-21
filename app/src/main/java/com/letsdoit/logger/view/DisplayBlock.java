package com.letsdoit.logger.view;

import android.util.Log;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.letsdoit.logger.data.dao.ActivityFragment;

import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.Period;

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

    public double getPercentageTimeOfPeriod(long intevalMillis) {
        Duration blockDuration = hasActivityFragment ? activityFragment.getDuration() : emptyInterval.getDuration();
        return (double) blockDuration.getMillis() / (double) intevalMillis;
    }

    @Override
    public String toString() {
        return "DisplayBlock{" +
                "hasActivityFragment=" + hasActivityFragment +
                ", activityFragment=" + activityFragment +
                ", emptyInterval=" + emptyInterval +
                '}';
    }

    public static List<DisplayBlock> makeDisplayBlocks(DateTime start, DateTime end, Period spacing,
                                                       List<ActivityFragment> fragments) {
        Preconditions.checkArgument(start.isBefore(end), "The start time needs to be before the end time.");
        Log.d(TAG, "Start=[" + start + "] End=[" + end +"]");

        List<DisplayBlock> blocks = Lists.newArrayList();
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
                blocks.add(new DisplayBlock(fragment.clip(start, end)));
            } else if (prevFragmentEnd.isBefore(fragment.getStart())) {
                // There is empty space between the previous fragment and the current one
                // Fill it with empty blocks
                Log.d(TAG, "There is space between the last fragment and the current one.  Adding empty space and " +
                        "adding the new Fragment afterwards.");
                blocks.addAll(EmptyInterval.makeEmptyBlocks(start,
                        prevFragmentEnd, fragment.getStart(), spacing));
                blocks.add(new DisplayBlock(fragment.clipEnd(end)));
            } else if (prevFragmentEnd.equals(fragment.getStart())) {
                // This fragment starts right after the previous fragment
                // Render it
                Log.d(TAG, "Fragment is inside the interval and right after the previous fragment.  Adding.");
                blocks.add(new DisplayBlock(fragment.clipEnd(end)));
            } else {
                // This should never happen
                Preconditions.checkArgument(false, "Activity fragment " + fragment + " starts before the previous " +
                        "fragment ends " + prevFragmentEnd);
            }
            prevFragmentEnd = fragment.getEnd();
        }

        if (prevFragmentEnd.isBefore(end)) {
            Log.d(TAG, "Adding trailing empty blocks to fill out the interval.");
            blocks.addAll(EmptyInterval.makeEmptyBlocks(start, prevFragmentEnd, end, spacing));
        }

        return blocks;
    }
}
