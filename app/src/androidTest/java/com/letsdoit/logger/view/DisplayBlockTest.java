package com.letsdoit.logger.view;

import android.test.AndroidTestCase;

import com.google.common.collect.Lists;
import com.letsdoit.logger.data.dao.ActivityFragment;
import com.letsdoit.logger.data.dao.ActivityInterval;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import static org.joda.time.Period.*;

import java.util.List;

/**
 * Created by Andrey on 7/18/2014.
 */
public class DisplayBlockTest extends AndroidTestCase {
    static {
        DateTimeZone.setDefault(DateTimeZone.UTC);
    }

    private DateTime start = new DateTime(2014, 7, 17, 10, 30, 0, 0);

    /**
     * |*[-----]|[-----]|[-----]|
     */
    public void testEmptyPeriodWithAlignedPartition() {
        List<ActivityFragment> fragments = Lists.newArrayList();
        ActivityInterval interval = new ActivityInterval(start, start.plus(minutes(15)), fragments);
        List<ActivityInterval> blocks = DisplayBlock.wrapFragmentsAndClipFreeTime(interval, minutes(5).toStandardDuration());

        assertEquals(blocks.size(), 3);

        assertEquals(blocks.get(0).getStart(), start);
        assertEquals(blocks.get(0).getEnd(), start.plus(minutes(5)));
        assertTrue(blocks.get(0).isEmpty());

        assertEquals(blocks.get(1).getStart(), start.plus(minutes(5)));
        assertEquals(blocks.get(1).getEnd(), start.plus(minutes(10)));
        assertTrue(blocks.get(1).isEmpty());

        assertEquals(blocks.get(2).getStart(), start.plus(minutes(10)));
        assertEquals(blocks.get(2).getEnd(), start.plus(minutes(15)));
        assertTrue(blocks.get(2).isEmpty());
    }

    /**
     * |*<[-----]|[-----]|[--]>---|
     */
    public void testEmptyPeriodWithUnalignedPartition() {
        List<ActivityFragment> fragments = Lists.newArrayList();
        ActivityInterval interval = new ActivityInterval(start, start.plus(minutes(12)), fragments);
        List<ActivityInterval> blocks = DisplayBlock.wrapFragmentsAndClipFreeTime(interval, minutes(5).toStandardDuration());

        assertEquals(3, blocks.size());

        assertEquals(start, blocks.get(0).getStart());
        assertEquals(start.plus(minutes(5)), blocks.get(0).getEnd());
        assertTrue(blocks.get(0).isEmpty());

        assertEquals(start.plus(minutes(5)), blocks.get(1).getStart());
        assertEquals(start.plus(minutes(10)), blocks.get(1).getEnd());
        assertTrue(blocks.get(1).isEmpty());

        assertEquals(start.plus(minutes(10)), blocks.get(2).getStart());
        assertEquals(start.plus(minutes(12)), blocks.get(2).getEnd());
        assertTrue(blocks.get(2).isEmpty());
    }

    /**
     * |*<[----]>-|
     */
    public void testEmptyPeriodEndBeforePartition() {
        List<ActivityFragment> fragments = Lists.newArrayList();
        ActivityInterval interval = new ActivityInterval(start, start.plus(minutes(4)), fragments);
        List<ActivityInterval> blocks = DisplayBlock.wrapFragmentsAndClipFreeTime(interval, minutes(5).toStandardDuration());

        assertEquals(blocks.size(), 1);

        assertEquals(blocks.get(0).getStart(), start);
        assertEquals(blocks.get(0).getEnd(), start.plus(minutes(4)));
        assertTrue(blocks.get(0).isEmpty());
    }

    /**
     * |*<[-----]>|
     */
    public void testEmptyPeriodExactPartition() {
        List<ActivityFragment> fragments = Lists.newArrayList();
        ActivityInterval interval = new ActivityInterval(start, start.plus(minutes(5)), fragments);
        List<ActivityInterval> blocks = DisplayBlock.wrapFragmentsAndClipFreeTime(interval, minutes(5).toStandardDuration());

        assertEquals(blocks.size(), 1);

        assertEquals(blocks.get(0).getStart(), start);
        assertEquals(blocks.get(0).getEnd(), start.plus(minutes(5)));
        assertTrue(blocks.get(0).isEmpty());
    }

    /**
     * [++++] before interval |*<[-----]|[-----]>|
     */
    public void testFragmentStartsAndEndsBeforePeriod() {
        List<ActivityFragment> fragments = Lists.newArrayList(new ActivityFragment("Sleeping",
                start.minus(minutes(11)),
                start.minus(minutes(4))));
        ActivityInterval interval = new ActivityInterval(start, start.plus(minutes(10)), fragments);
        List<ActivityInterval> blocks = DisplayBlock.wrapFragmentsAndClipFreeTime(interval, minutes(5).toStandardDuration());

        assertEquals(blocks.size(), 2);

        assertEquals(blocks.get(0).getStart(), start);
        assertEquals(blocks.get(0).getEnd(), start.plus(minutes(5)));
        assertTrue(blocks.get(0).isEmpty());

        assertEquals(blocks.get(1).getStart(), start.plus(minutes(5)));
        assertEquals(blocks.get(1).getEnd(), start.plus(minutes(10)));
        assertTrue(blocks.get(1).isEmpty());
    }

    /**
     * [++++] before interval |*<[++++][-]|[-----]>|
     * activity gets clipped to only the part inside the interval
     */
    public void testFragmentStartsBeforePeriodAndEndsInPeriod() {
        ActivityFragment sleeping = new ActivityFragment("Sleeping", start.minus(minutes(11)), start.plus(minutes(4)));
        List<ActivityFragment> fragments = Lists.newArrayList(sleeping);

        ActivityInterval interval = new ActivityInterval(start, start.plus(minutes(10)), fragments);
        List<ActivityInterval> blocks = DisplayBlock.wrapFragmentsAndClipFreeTime(interval, minutes(5).toStandardDuration());

        assertEquals("" + blocks, 3, blocks.size());

        assertEquals(blocks.get(0).getStart(), start);
        assertEquals(blocks.get(0).getEnd(), start.plus(minutes(4)));
        assertFalse(blocks.get(0).isEmpty());
        assertEquals(blocks.get(0).getFragments().size(), 1);
        // Verify that the sleeping ActivityFragment was split to be included in this interval
        assertNotSame(blocks.get(0).getActivityFragment(0), sleeping);
        assertTrue(blocks.get(0).getActivityFragment(0).getActivityName().equals(sleeping.getActivityName()));

        assertEquals(blocks.get(1).getStart(), start.plus(minutes(4)));
        assertEquals(blocks.get(1).getEnd(), start.plus(minutes(5)));
        assertTrue(blocks.get(1).isEmpty());

        assertEquals(blocks.get(2).getStart(), start.plus(minutes(5)));
        assertEquals(blocks.get(2).getEnd(), start.plus(minutes(10)));
        assertTrue(blocks.get(2).isEmpty());
    }

    /**
     * [++] before interval |*<[+++++|+++++|+++++]>| after interval [+]
     */
    public void testFragmentStartsBeforePeriodAndEndsOutsideOfPeriod() {
        ActivityFragment sleeping = new ActivityFragment("Sleeping", start.minus(minutes(2)), start.plus(minutes(16)));
        List<ActivityFragment> fragments = Lists.newArrayList(sleeping);

        ActivityInterval interval = new ActivityInterval(start, start.plus(minutes(15)), fragments);
        List<ActivityInterval> blocks = DisplayBlock.wrapFragmentsAndClipFreeTime(interval, minutes(5).toStandardDuration());

        assertEquals("" + blocks, 1, blocks.size());

        assertEquals(start, blocks.get(0).getStart());
        assertEquals(start.plus(minutes(15)), blocks.get(0).getEnd());
        ActivityFragment clippedSleep = blocks.get(0).getActivityFragment(0);
        assertNotSame(sleeping, clippedSleep);
        assertTrue(clippedSleep.getActivityName().equals(sleeping.getActivityName()));
    }

    /**
     * |*<[---][++|+++++|+++++]>| after interval [+]
     */
    public void testFragmentStartsInPeriodAndEndsOutsideOfPeriod() {
        ActivityFragment sleeping = new ActivityFragment("Sleeping", start.plus(minutes(3)), start.plus(minutes(16)));
        List<ActivityFragment> fragments = Lists.newArrayList(sleeping);

        ActivityInterval interval = new ActivityInterval(start, start.plus(minutes(15)), fragments);
        List<ActivityInterval> blocks = DisplayBlock.wrapFragmentsAndClipFreeTime(interval, minutes(5).toStandardDuration());

        assertEquals("" + blocks, 2, blocks.size());

        assertEquals(start, blocks.get(0).getStart());
        assertEquals(start.plus(minutes(3)), blocks.get(0).getEnd());
        assertTrue(blocks.get(0).isEmpty());

        assertEquals(start.plus(minutes(3)), blocks.get(1).getStart());
        assertEquals(start.plus(minutes(15)), blocks.get(1).getEnd());
        assertNotSame(sleeping, blocks.get(1).getActivityFragment(0));
        assertTrue(blocks.get(1).getActivityFragment(0).getActivityName().equals(sleeping.getActivityName()));
    }

    /**
     * |*<[-----]|[-----]|[-----]>| after interval -[++++]
     */
    public void testFragmentStartsAfterPeriod() {
        ActivityFragment sleeping = new ActivityFragment("Sleeping", start.plus(minutes(16)), start.plus(minutes(20)));
        List<ActivityFragment> fragments = Lists.newArrayList(sleeping);

        ActivityInterval interval = new ActivityInterval(start, start.plus(minutes(15)), fragments);
        List<ActivityInterval> blocks = DisplayBlock.wrapFragmentsAndClipFreeTime(interval, minutes(5).toStandardDuration());

        assertEquals("" + blocks, 3, blocks.size());

        assertEquals(start, blocks.get(0).getStart());
        assertEquals(start.plus(minutes(5)), blocks.get(0).getEnd());
        assertTrue(blocks.get(0).isEmpty());

        assertEquals(start.plus(minutes(5)), blocks.get(1).getStart());
        assertEquals(start.plus(minutes(10)), blocks.get(1).getEnd());
        assertTrue(blocks.get(1).isEmpty());

        assertEquals(start.plus(minutes(10)), blocks.get(2).getStart());
        assertEquals(start.plus(minutes(15)), blocks.get(2).getEnd());
        assertTrue(blocks.get(2).isEmpty());
    }

    /**
     * |*<[+++++|+++++|+++++]>|
     */
    public void testFragmentSpansPeriodExactly() {
        ActivityFragment sleeping = new ActivityFragment("Sleeping", start, start.plus(minutes(15)));
        List<ActivityFragment> fragments = Lists.newArrayList(sleeping);

        ActivityInterval interval = new ActivityInterval(start, start.plus(minutes(15)), fragments);
        List<ActivityInterval> blocks = DisplayBlock.wrapFragmentsAndClipFreeTime(interval, minutes(5).toStandardDuration());

        assertEquals("" + blocks, 1, blocks.size());

        assertEquals(start, blocks.get(0).getStart());
        assertEquals(start.plus(minutes(15)), blocks.get(0).getEnd());
        assertSame(sleeping, blocks.get(0).getActivityFragment(0));
    }

    /**
     * [+...+] before interval |*<[+++++]|[-----]|[+++++]>| after interval -[++++]
     */
    public void testSinglePartitionSpaceBetweenFragments() {
        ActivityFragment eating = new ActivityFragment("Eating", start.minus(hours(8)), start.plus(minutes(5)));
        ActivityFragment sleeping = new ActivityFragment("Sleeping", start.plus(minutes(10)), start.plus(minutes(20)));

        List<ActivityFragment> fragments = Lists.newArrayList(eating, sleeping);
        ActivityInterval interval = new ActivityInterval(start, start.plus(minutes(15)), fragments);
        List<ActivityInterval> blocks = DisplayBlock.wrapFragmentsAndClipFreeTime(interval, minutes(5).toStandardDuration());

        assertEquals("" + blocks, 3, blocks.size());

        assertEquals(start, blocks.get(0).getStart());
        assertEquals(start.plus(minutes(5)), blocks.get(0).getEnd());
        assertNotSame(eating, blocks.get(0).getActivityFragment(0));
        assertTrue(blocks.get(0).getActivityFragment(0).getActivityName().equals(eating.getActivityName()));

        assertEquals(start.plus(minutes(5)), blocks.get(1).getStart());
        assertEquals(start.plus(minutes(10)), blocks.get(1).getEnd());
        assertTrue(blocks.get(1).isEmpty());

        assertEquals(start.plus(minutes(10)), blocks.get(2).getStart());
        assertEquals(start.plus(minutes(15)), blocks.get(2).getEnd());
        assertNotSame(sleeping, blocks.get(2));
        assertTrue(blocks.get(2).getActivityFragment(0).getActivityName().equals(sleeping.getActivityName()));
    }

    /**
     * |*<[-][+++][-]|[----][+|+++++|+++++]|[-----]|[-----]>|
     */
    public void testSplitPartitionSpaceBetweenFragments() {
        ActivityFragment chatting = new ActivityFragment("Chatting", start.plus(minutes(1)), start.plus(minutes(4)));
        ActivityFragment reading = new ActivityFragment("Reading", start.plus(minutes(9)), start.plus(minutes(20)));

        List<ActivityFragment> fragments = Lists.newArrayList(chatting, reading);

        ActivityInterval interval = new ActivityInterval(start, start.plus(minutes(30)), fragments);
        List<ActivityInterval> blocks = DisplayBlock.wrapFragmentsAndClipFreeTime(interval, minutes(5).toStandardDuration());

        assertEquals("" + blocks, 7, blocks.size());

        assertEquals(start, blocks.get(0).getStart());
        assertEquals(start.plus(minutes(1)), blocks.get(0).getEnd());
        assertTrue(blocks.get(0).isEmpty());

        assertEquals(start.plus(minutes(1)), blocks.get(1).getStart());
        assertEquals(start.plus(minutes(4)), blocks.get(1).getEnd());
        assertSame(chatting, blocks.get(1).getActivityFragment(0));

        assertEquals(start.plus(minutes(4)), blocks.get(2).getStart());
        assertEquals(start.plus(minutes(5)), blocks.get(2).getEnd());
        assertTrue(blocks.get(2).isEmpty());

        assertEquals(start.plus(minutes(5)), blocks.get(3).getStart());
        assertEquals(start.plus(minutes(9)), blocks.get(3).getEnd());
        assertTrue(blocks.get(3).isEmpty());

        assertEquals(start.plus(minutes(9)), blocks.get(4).getStart());
        assertEquals(start.plus(minutes(20)), blocks.get(4).getEnd());
        assertSame(reading, blocks.get(4).getActivityFragment(0));

        assertEquals(start.plus(minutes(20)), blocks.get(5).getStart());
        assertEquals(start.plus(minutes(25)), blocks.get(5).getEnd());
        assertTrue(blocks.get(5).isEmpty());

        assertEquals(start.plus(minutes(25)), blocks.get(6).getStart());
        assertEquals(start.plus(minutes(30)), blocks.get(6).getEnd());
        assertTrue(blocks.get(6).isEmpty());
    }

    /**
     * |*<[++++][-]|[----][+|+++++|+++++]|[-----]|[-----]>|
     */
    public void testPartialStart() {
        ActivityFragment chatting = new ActivityFragment("Chatting", start, start.plus(minutes(4)));
        ActivityFragment reading = new ActivityFragment("Reading", start.plus(minutes(9)), start.plus(minutes(20)));

        List<ActivityFragment> fragments = Lists.newArrayList(chatting, reading);

        ActivityInterval interval = new ActivityInterval(start, start.plus(minutes(30)), fragments);
        List<ActivityInterval> blocks = DisplayBlock.wrapFragmentsAndClipFreeTime(interval, minutes(5).toStandardDuration());

        assertEquals("" + blocks, 6, blocks.size());

        assertEquals(start, blocks.get(0).getStart());
        assertEquals(start.plus(minutes(4)), blocks.get(0).getEnd());
        assertSame(chatting, blocks.get(0).getActivityFragment(0));

        assertEquals(start.plus(minutes(4)), blocks.get(1).getStart());
        assertEquals(start.plus(minutes(5)), blocks.get(1).getEnd());
        assertTrue(blocks.get(1).isEmpty());

        assertEquals(start.plus(minutes(5)), blocks.get(2).getStart());
        assertEquals(start.plus(minutes(9)), blocks.get(2).getEnd());
        assertTrue(blocks.get(2).isEmpty());

        assertEquals(start.plus(minutes(9)), blocks.get(3).getStart());
        assertEquals(start.plus(minutes(20)), blocks.get(3).getEnd());
        assertSame(reading, blocks.get(3).getActivityFragment(0));

        assertEquals(start.plus(minutes(20)), blocks.get(4).getStart());
        assertEquals(start.plus(minutes(25)), blocks.get(4).getEnd());
        assertTrue(blocks.get(4).isEmpty());

        assertEquals(start.plus(minutes(25)), blocks.get(5).getStart());
        assertEquals(start.plus(minutes(30)), blocks.get(5).getEnd());
        assertTrue(blocks.get(5).isEmpty());
    }

    // TODO: Add more merging test cases
}
