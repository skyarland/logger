package com.letsdoit.logger.view;

import android.test.AndroidTestCase;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.util.List;

import static org.joda.time.Period.minutes;

/**
 * Created by Andrey on 7/18/2014.
 */
public class EmptyIntervalTest extends AndroidTestCase {
    static {
        DateTimeZone.setDefault(DateTimeZone.UTC);
    }

    private DateTime time = new DateTime(2014, 7, 17, 10, 30, 0, 0);

    public void testPeriodSmallerThanSpacing() {
        List<DisplayBlock> blocks = EmptyInterval.makeEmptyBlocks(time, time, time.plus(minutes(2)), minutes(5));

        assertEquals(1, blocks.size());

        assertEquals(time, blocks.get(0).getEmptyInterval().getStart());
        assertEquals(time.plus(minutes(2)), blocks.get(0).getEmptyInterval().getEnd());
    }

    public void testReferenceDifferentFromStart() {
        List<DisplayBlock> blocks = EmptyInterval.makeEmptyBlocks(time, time.plus(minutes(2)), time.plus(minutes(4)),
                minutes(5));

        assertEquals(1, blocks.size());

        assertEquals(time.plus(minutes(2)), blocks.get(0).getEmptyInterval().getStart());
        assertEquals(time.plus(minutes(4)), blocks.get(0).getEmptyInterval().getEnd());
    }

    public void testPeriodExactlyEqualToSpacing() {
        List<DisplayBlock> blocks = EmptyInterval.makeEmptyBlocks(time, time, time.plus(minutes(5)), minutes(5));

        assertEquals(1, blocks.size());

        assertEquals(time, blocks.get(0).getEmptyInterval().getStart());
        assertEquals(time.plus(minutes(5)), blocks.get(0).getEmptyInterval().getEnd());
    }

    public void testPeriodSlightlyLargerThanSpacing() {
        List<DisplayBlock> blocks = EmptyInterval.makeEmptyBlocks(time, time, time.plus(minutes(7)), minutes(5));

        assertEquals(2, blocks.size());

        assertEquals(time, blocks.get(0).getEmptyInterval().getStart());
        assertEquals(time.plus(minutes(5)), blocks.get(0).getEmptyInterval().getEnd());

        assertEquals(time.plus(minutes(5)), blocks.get(1).getEmptyInterval().getStart());
        assertEquals(time.plus(minutes(7)), blocks.get(1).getEmptyInterval().getEnd());
    }

    public void tesetPeriodSplitRelativeToReference() {
        List<DisplayBlock> blocks = EmptyInterval.makeEmptyBlocks(time.minus(6), time, time.plus(minutes(5)),
                minutes(5));

        assertEquals(2, blocks.size());

        assertEquals(time, blocks.get(0).getEmptyInterval().getStart());
        assertEquals(time.plus(minutes(4)), blocks.get(0).getEmptyInterval().getEnd());

        assertEquals(time.plus(minutes(4)), blocks.get(1).getEmptyInterval().getStart());
        assertEquals(time.plus(minutes(5)), blocks.get(1).getEmptyInterval().getEnd());
    }

    public void testPeriodIsMultipleOfSpacing() {
        List<DisplayBlock> blocks = EmptyInterval.makeEmptyBlocks(time, time, time.plus(minutes(10)), minutes(5));

        assertEquals(2, blocks.size());

        assertEquals(time, blocks.get(0).getEmptyInterval().getStart());
        assertEquals(time.plus(minutes(5)), blocks.get(0).getEmptyInterval().getEnd());

        assertEquals(time.plus(minutes(5)), blocks.get(1).getEmptyInterval().getStart());
        assertEquals(time.plus(minutes(10)), blocks.get(1).getEmptyInterval().getEnd());
    }

    public void testPeriodWithSpacingsAndRemainder() {
        List<DisplayBlock> blocks = EmptyInterval.makeEmptyBlocks(time, time, time.plus(minutes(12)), minutes(5));

        assertEquals(3, blocks.size());

        assertEquals(time, blocks.get(0).getEmptyInterval().getStart());
        assertEquals(time.plus(minutes(5)), blocks.get(0).getEmptyInterval().getEnd());

        assertEquals(time.plus(minutes(5)), blocks.get(1).getEmptyInterval().getStart());
        assertEquals(time.plus(minutes(10)), blocks.get(1).getEmptyInterval().getEnd());

        assertEquals(time.plus(minutes(10)), blocks.get(2).getEmptyInterval().getStart());
        assertEquals(time.plus(minutes(12)), blocks.get(2).getEmptyInterval().getEnd());
    }
}
