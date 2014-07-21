package com.letsdoit.logger.view;

import android.test.AndroidTestCase;

import com.google.common.collect.Lists;
import com.letsdoit.logger.data.dao.ActivityFragment;

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

    public void testEmptyPeriodWithAlignedPartition() {
        List<ActivityFragment> fragments = Lists.newArrayList();
        List<DisplayBlock> blocks = DisplayBlock.makeDisplayBlocks(start, start.plus(minutes(15)), minutes(5),
                fragments);

        assertEquals(blocks.size(), 3);

        assertEquals(blocks.get(0).getEmptyInterval().getStart(), start);
        assertEquals(blocks.get(0).getEmptyInterval().getEnd(), start.plus(minutes(5)));

        assertEquals(blocks.get(1).getEmptyInterval().getStart(), start.plus(minutes(5)));
        assertEquals(blocks.get(1).getEmptyInterval().getEnd(), start.plus(minutes(10)));

        assertEquals(blocks.get(2).getEmptyInterval().getStart(), start.plus(minutes(10)));
        assertEquals(blocks.get(2).getEmptyInterval().getEnd(), start.plus(minutes(15)));
    }

    public void testEmptyPeriodWithUnalignedPartition() {
        List<ActivityFragment> fragments = Lists.newArrayList();
        List<DisplayBlock> blocks = DisplayBlock.makeDisplayBlocks(start, start.plus(minutes(12)), minutes(5),
                fragments);

        assertEquals(3, blocks.size());

        assertEquals(start, blocks.get(0).getEmptyInterval().getStart());
        assertEquals(start.plus(minutes(5)), blocks.get(0).getEmptyInterval().getEnd());

        assertEquals(start.plus(minutes(5)), blocks.get(1).getEmptyInterval().getStart());
        assertEquals(start.plus(minutes(10)), blocks.get(1).getEmptyInterval().getEnd());

        assertEquals(start.plus(minutes(10)), blocks.get(2).getEmptyInterval().getStart());
        assertEquals(start.plus(minutes(12)), blocks.get(2).getEmptyInterval().getEnd());
    }

    public void testEmptyPeriodEndBeforePartition() {
        List<ActivityFragment> fragments = Lists.newArrayList();
        List<DisplayBlock> blocks = DisplayBlock.makeDisplayBlocks(start, start.plus(minutes(4)), minutes(5),
                fragments);

        assertEquals(blocks.size(), 1);

        assertEquals(blocks.get(0).getEmptyInterval().getStart(), start);
        assertEquals(blocks.get(0).getEmptyInterval().getEnd(), start.plus(minutes(4)));
    }

    public void testEmptyPeriodExactPartition() {
        List<ActivityFragment> fragments = Lists.newArrayList();
        List<DisplayBlock> blocks = DisplayBlock.makeDisplayBlocks(start, start.plus(minutes(5)), minutes(5),
                fragments);

        assertEquals(blocks.size(), 1);

        assertEquals(blocks.get(0).getEmptyInterval().getStart(), start);
        assertEquals(blocks.get(0).getEmptyInterval().getEnd(), start.plus(minutes(5)));
    }

    public void testFragmentStartsAndEndsBeforePeriod() {
        List<ActivityFragment> fragments = Lists.newArrayList(new ActivityFragment("Sleeping",
                start.minus(minutes(11)),
                start.minus(minutes(4))));
        List<DisplayBlock> blocks = DisplayBlock.makeDisplayBlocks(start, start.plus(minutes(10)), minutes(5),
                fragments);

        assertEquals(blocks.size(), 2);

        assertEquals(blocks.get(0).getEmptyInterval().getStart(), start);
        assertEquals(blocks.get(0).getEmptyInterval().getEnd(), start.plus(minutes(5)));

        assertEquals(blocks.get(1).getEmptyInterval().getStart(), start.plus(minutes(5)));
        assertEquals(blocks.get(1).getEmptyInterval().getEnd(), start.plus(minutes(10)));
    }

    public void testFragmentStartsBeforePeriodAndEndsInPeriod() {
        ActivityFragment sleeping = new ActivityFragment("Sleeping", start.minus(minutes(11)), start.plus(minutes(4)));
        List<ActivityFragment> fragments = Lists.newArrayList(sleeping);

        List<DisplayBlock> blocks = DisplayBlock.makeDisplayBlocks(start, start.plus(minutes(10)), minutes(5),
                fragments);

        assertEquals("" + blocks, 3, blocks.size());

        assertEquals(blocks.get(0).getActivityFragment().getStart(), start);
        assertEquals(blocks.get(0).getActivityFragment().getEnd(), start.plus(minutes(4)));
        assertNotSame(blocks.get(0).getActivityFragment(), sleeping);
        assertTrue(blocks.get(0).getActivityFragment().getActivityName().equals(sleeping.getActivityName()));

        assertEquals(blocks.get(1).getEmptyInterval().getStart(), start.plus(minutes(4)));
        assertEquals(blocks.get(1).getEmptyInterval().getEnd(), start.plus(minutes(5)));

        assertEquals(blocks.get(2).getEmptyInterval().getStart(), start.plus(minutes(5)));
        assertEquals(blocks.get(2).getEmptyInterval().getEnd(), start.plus(minutes(10)));
    }

    public void testFragmentStartsBeforePeriodAndEndsOutsideOfPeriod() {
        ActivityFragment sleeping = new ActivityFragment("Sleeping", start.minus(minutes(2)), start.plus(minutes(16)));
        List<ActivityFragment> fragments = Lists.newArrayList(sleeping);

        List<DisplayBlock> blocks = DisplayBlock.makeDisplayBlocks(start, start.plus(minutes(15)), minutes(5),
                fragments);

        assertEquals("" + blocks, 1, blocks.size());

        assertEquals(start, blocks.get(0).getActivityFragment().getStart());
        assertEquals(start.plus(minutes(15)), blocks.get(0).getActivityFragment().getEnd());
        assertNotSame(sleeping, blocks.get(0).getActivityFragment());
        assertTrue(blocks.get(0).getActivityFragment().getActivityName().equals(sleeping.getActivityName()));
    }

    public void testFragmentStartsInPeriodAndEndsOutsideOfPeriod() {
        ActivityFragment sleeping = new ActivityFragment("Sleeping", start.plus(minutes(3)), start.plus(minutes(16)));
        List<ActivityFragment> fragments = Lists.newArrayList(sleeping);

        List<DisplayBlock> blocks = DisplayBlock.makeDisplayBlocks(start, start.plus(minutes(15)), minutes(5),
                fragments);

        assertEquals("" + blocks, 2, blocks.size());

        assertEquals(start, blocks.get(0).getEmptyInterval().getStart());
        assertEquals(start.plus(minutes(3)), blocks.get(0).getEmptyInterval().getEnd());

        assertEquals(start.plus(minutes(3)), blocks.get(1).getActivityFragment().getStart());
        assertEquals(start.plus(minutes(15)), blocks.get(1).getActivityFragment().getEnd());
        assertNotSame(sleeping, blocks.get(1).getActivityFragment());
        assertTrue(blocks.get(1).getActivityFragment().getActivityName().equals(sleeping.getActivityName()));
    }

    public void testFragmentStartsAfterPeriod() {
        ActivityFragment sleeping = new ActivityFragment("Sleeping", start.minus(minutes(20)),
                start.minus(minutes(16)));
        List<ActivityFragment> fragments = Lists.newArrayList(sleeping);

        List<DisplayBlock> blocks = DisplayBlock.makeDisplayBlocks(start, start.plus(minutes(15)), minutes(5),
                fragments);

        assertEquals("" + blocks, 3, blocks.size());

        assertEquals(start, blocks.get(0).getEmptyInterval().getStart());
        assertEquals(start.plus(minutes(5)), blocks.get(0).getEmptyInterval().getEnd());

        assertEquals(start.plus(minutes(5)), blocks.get(1).getEmptyInterval().getStart());
        assertEquals(start.plus(minutes(10)), blocks.get(1).getEmptyInterval().getEnd());

        assertEquals(start.plus(minutes(10)), blocks.get(2).getEmptyInterval().getStart());
        assertEquals(start.plus(minutes(15)), blocks.get(2).getEmptyInterval().getEnd());
    }

    public void testFragmentSpansPeriodExactly() {
        ActivityFragment sleeping = new ActivityFragment("Sleeping", start, start.plus(minutes(15)));
        List<ActivityFragment> fragments = Lists.newArrayList(sleeping);

        List<DisplayBlock> blocks = DisplayBlock.makeDisplayBlocks(start, start.plus(minutes(15)), minutes(5),
                fragments);

        assertEquals("" + blocks, 1, blocks.size());

        assertEquals(start, blocks.get(0).getActivityFragment().getStart());
        assertEquals(start.plus(minutes(15)), blocks.get(0).getActivityFragment().getEnd());
        assertSame(sleeping, blocks.get(0).getActivityFragment());
    }

    public void testSinglePartitionSpaceBetweenFragments() {
        ActivityFragment eating = new ActivityFragment("Eating", start.minus(hours(8)), start.plus(minutes(5)));
        ActivityFragment sleeping = new ActivityFragment("Sleeping", start.plus(minutes(10)), start.plus(minutes(20)));

        List<ActivityFragment> fragments = Lists.newArrayList(eating, sleeping);

        List<DisplayBlock> blocks = DisplayBlock.makeDisplayBlocks(start, start.plus(minutes(15)), minutes(5),
                fragments);

        assertEquals("" + blocks, 3, blocks.size());

        assertEquals(start, blocks.get(0).getActivityFragment().getStart());
        assertEquals(start.plus(minutes(5)), blocks.get(0).getActivityFragment().getEnd());
        assertNotSame(eating, blocks.get(0).getActivityFragment());
        assertTrue(blocks.get(0).getActivityFragment().getActivityName().equals(eating.getActivityName()));

        assertEquals(start.plus(minutes(5)), blocks.get(1).getEmptyInterval().getStart());
        assertEquals(start.plus(minutes(10)), blocks.get(1).getEmptyInterval().getEnd());

        assertEquals(start.plus(minutes(10)), blocks.get(2).getActivityFragment().getStart());
        assertEquals(start.plus(minutes(15)), blocks.get(2).getActivityFragment().getEnd());
        assertNotSame(sleeping, blocks.get(2).getActivityFragment());
        assertTrue(blocks.get(2).getActivityFragment().getActivityName().equals(sleeping.getActivityName()));
    }

    public void testSplitPartitionSpaceBetweenFragments() {
        ActivityFragment chatting = new ActivityFragment("Chatting", start.plus(minutes(1)), start.plus(minutes(4)));
        ActivityFragment reading = new ActivityFragment("Reading", start.plus(minutes(9)), start.plus(minutes(20)));

        List<ActivityFragment> fragments = Lists.newArrayList(chatting, reading);

        List<DisplayBlock> blocks = DisplayBlock.makeDisplayBlocks(start, start.plus(minutes(30)), minutes(5),
                fragments);

        assertEquals("" + blocks, 7, blocks.size());

        assertEquals(start, blocks.get(0).getEmptyInterval().getStart());
        assertEquals(start.plus(minutes(1)), blocks.get(0).getEmptyInterval().getEnd());

        assertEquals(start.plus(minutes(1)), blocks.get(1).getActivityFragment().getStart());
        assertEquals(start.plus(minutes(4)), blocks.get(1).getActivityFragment().getEnd());
        assertSame(chatting, blocks.get(1).getActivityFragment());

        assertEquals(start.plus(minutes(4)), blocks.get(2).getEmptyInterval().getStart());
        assertEquals(start.plus(minutes(5)), blocks.get(2).getEmptyInterval().getEnd());

        assertEquals(start.plus(minutes(5)), blocks.get(3).getEmptyInterval().getStart());
        assertEquals(start.plus(minutes(9)), blocks.get(3).getEmptyInterval().getEnd());

        assertEquals(start.plus(minutes(9)), blocks.get(4).getActivityFragment().getStart());
        assertEquals(start.plus(minutes(20)), blocks.get(4).getActivityFragment().getEnd());
        assertSame(reading, blocks.get(4).getActivityFragment());

        assertEquals(start.plus(minutes(20)), blocks.get(5).getEmptyInterval().getStart());
        assertEquals(start.plus(minutes(25)), blocks.get(5).getEmptyInterval().getEnd());

        assertEquals(start.plus(minutes(25)), blocks.get(6).getEmptyInterval().getStart());
        assertEquals(start.plus(minutes(30)), blocks.get(6).getEmptyInterval().getEnd());
    }

}
