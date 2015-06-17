package com.letsdoit.logger.view;

import android.test.AndroidTestCase;

import com.letsdoit.logger.data.dao.ActivityInterval;

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

    public void testGetNextPartition() {

    }

    /**
     * |*[--]---|
     */
    public void testPeriodSmallerThanSpacing() {
        List<ActivityInterval> blocks = EmptyInterval.makeEmptyBlocks(time, time, time.plus(minutes(2)),
                minutes(5).toStandardDuration());

        assertEquals(1, blocks.size());

        assertEquals(time, blocks.get(0).getStart());
        assertEquals(time.plus(minutes(2)), blocks.get(0).getEnd());
    }

    /**
     * |*--[--]-|
     */
    public void testReferenceDifferentFromStart() {
        List<ActivityInterval> blocks = EmptyInterval.makeEmptyBlocks(time, time.plus(minutes(2)), time.plus(minutes(4)),
                minutes(5).toStandardDuration());

        assertEquals(1, blocks.size());

        assertEquals(time.plus(minutes(2)), blocks.get(0).getStart());
        assertEquals(time.plus(minutes(4)), blocks.get(0).getEnd());
    }

    /**
     * |*[-----]|
     */
    public void testPeriodExactlyEqualToSpacing() {
        List<ActivityInterval> blocks = EmptyInterval.makeEmptyBlocks(time, time, time.plus(minutes(5)),
                minutes(5).toStandardDuration());

        assertEquals(1, blocks.size());

        assertEquals(time, blocks.get(0).getStart());
        assertEquals(time.plus(minutes(5)), blocks.get(0).getEnd());
    }

    /**
     * |*[-----]|[--]---|
     */
    public void testPeriodSlightlyLargerThanSpacing() {
        List<ActivityInterval> blocks = EmptyInterval.makeEmptyBlocks(time, time, time.plus(minutes(7)), minutes(5).toStandardDuration());

        assertEquals(2, blocks.size());

        assertEquals(time, blocks.get(0).getStart());
        assertEquals(time.plus(minutes(5)), blocks.get(0).getEnd());

        assertEquals(time.plus(minutes(5)), blocks.get(1).getStart());
        assertEquals(time.plus(minutes(7)), blocks.get(1).getEnd());
    }

    /**
     * |----|-*[----]|[-]----|
     */
    public void testPeriodSplitRelativeToReference() {

        List<ActivityInterval> blocks = EmptyInterval.makeEmptyBlocks(time.minus(minutes(6)), time, time.plus(minutes(5)),
                minutes(5).toStandardDuration());

        assertEquals(2, blocks.size());

        assertEquals(time, blocks.get(0).getStart());
        assertEquals(time.plus(minutes(4)), blocks.get(0).getEnd());

        assertEquals(time.plus(minutes(4)), blocks.get(1).getStart());
        assertEquals(time.plus(minutes(5)), blocks.get(1).getEnd());
    }

    /**
     * |*----|-[----]|[-]----|
     */
    public void testPeriodSplitRelativeToReference2() {
        List<ActivityInterval> blocks = EmptyInterval.makeEmptyBlocks(time, time.plus(minutes(6)), time.plus(minutes(11)),
                minutes(5).toStandardDuration());

        assertEquals(2, blocks.size());

        assertEquals(time.plus(minutes(6)), blocks.get(0).getStart());
        assertEquals(time.plus(minutes(10)), blocks.get(0).getEnd());

        assertEquals(time.plus(minutes(10)), blocks.get(1).getStart());
        assertEquals(time.plus(minutes(11)), blocks.get(1).getEnd());
    }


    /**
     * |*[-----]|[-----]|
     */
    public void testPeriodIsMultipleOfSpacing() {
        List<ActivityInterval> blocks = EmptyInterval.makeEmptyBlocks(time, time, time.plus(minutes(10)),
                minutes(5).toStandardDuration());

        assertEquals(2, blocks.size());

        assertEquals(time, blocks.get(0).getStart());
        assertEquals(time.plus(minutes(5)), blocks.get(0).getEnd());

        assertEquals(time.plus(minutes(5)), blocks.get(1).getStart());
        assertEquals(time.plus(minutes(10)), blocks.get(1).getEnd());
    }

    /**
     * |*[-----]|[-----]|[--]---|
     */
    public void testPeriodWithSpacingsAndRemainder() {
        List<ActivityInterval> blocks = EmptyInterval.makeEmptyBlocks(time, time, time.plus(minutes(12)),
                minutes(5).toStandardDuration());

        assertEquals(3, blocks.size());

        assertEquals(time, blocks.get(0).getStart());
        assertEquals(time.plus(minutes(5)), blocks.get(0).getEnd());

        assertEquals(time.plus(minutes(5)), blocks.get(1).getStart());
        assertEquals(time.plus(minutes(10)), blocks.get(1).getEnd());

        assertEquals(time.plus(minutes(10)), blocks.get(2).getStart());
        assertEquals(time.plus(minutes(12)), blocks.get(2).getEnd());
    }
}
