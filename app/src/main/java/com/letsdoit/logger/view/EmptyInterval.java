package com.letsdoit.logger.view;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.Period;

import java.util.List;


/**
 * Created by Andrey on 7/15/2014.
 */
public class EmptyInterval {
    private final DateTime start;
    private final DateTime end;

    public EmptyInterval(DateTime start, DateTime end) {
        this.start = start;
        this.end = end;
    }

    public DateTime getStart() {
        return start;
    }

    public DateTime getEnd() {
        return end;
    }

    public Duration getDuration() {
        return new Duration(start, end);
    }

    public static List<DisplayBlock> makeEmptyBlocks(DateTime reference, DateTime start, DateTime end, Period spacing) {
        Preconditions.checkArgument(false == reference.isAfter(start), "The reference time must be earlier or equal " +
                "to the start time.");
        Preconditions.checkArgument(start.isBefore(end), "The start must be earlier than the end.");
        // TODO: make sure the spacing isn't zero
        // Preconditions.checkArgument(spacing);

        List<DisplayBlock> blocks = Lists.newArrayList();
        DateTime nextPartition = getNextPartition(reference, start, spacing);
        DateTime blockStart = start;
        while (nextPartition.isBefore(end)) {
            blocks.add(new DisplayBlock(new EmptyInterval(blockStart, nextPartition)));
            blockStart = nextPartition;
            nextPartition = nextPartition.plus(spacing);
        }
        blocks.add(new DisplayBlock(new EmptyInterval(blockStart, end)));
        return blocks;
    }

    private static DateTime getNextPartition(DateTime reference, DateTime prev, Period spacing) {
        DateTime time = reference;
        while (false == time.isAfter(prev)) {
            time = time.plus(spacing);
        }
        return time;
    }

    @Override
    public String toString() {
        return "EmptyInterval{" +
                "start=" + start +
                ", end=" + end +
                '}';
    }
}
