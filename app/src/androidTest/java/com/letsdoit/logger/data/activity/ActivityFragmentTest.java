package com.letsdoit.logger.data.activity;

import android.test.AndroidTestCase;
import android.util.Pair;

import com.google.common.collect.Lists;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Period;

import java.util.List;

import static org.joda.time.Period.hours;
import static org.joda.time.Period.minutes;

/**
 * Created by Andrey on 7/18/2014.
 */
public class ActivityFragmentTest extends AndroidTestCase {

    static {
        DateTimeZone.setDefault(DateTimeZone.UTC);
    }

    private DateTime start = new DateTime(2014, 7, 17, 10, 30, 0, 0);

    public void testEvenBorder() {
        List<ActivityFragment> fragments = Lists.newArrayList(new ActivityFragment("Break", start,
                start.plus(hours(1))));
        List<ActivityInterval> intervals = ActivityFragment.partition(start, start.plus(hours(1)), minutes(30),
                fragments);

        assertEquals("" + intervals, 2, intervals.size());

        assertEquals(1, intervals.get(0).getFragments().size());
        assertEquals(start, intervals.get(0).getFragments().get(0).getStart());
        assertEquals(start.plus(minutes(30)), intervals.get(0).getFragments().get(0).getEnd());

        assertEquals(1, intervals.get(1).getFragments().size());
        assertEquals(start.plus(minutes(30)), intervals.get(1).getFragments().get(0).getStart());
        assertEquals(start.plus(hours(1)), intervals.get(1).getFragments().get(0).getEnd());

    }

    public void testPartialIntervalBorder() {
        List<ActivityFragment> fragments = Lists.newArrayList(new ActivityFragment("Break", start.plus(minutes(5)),
                start.plus(minutes(40))));
        List<ActivityInterval> intervals = ActivityFragment.partition(start.minus(hours(1)), start.plus(hours(2)),
                minutes(30),
                fragments);

        assertEquals("" + intervals, 6, intervals.size());

        assertEquals(0, intervals.get(0).getFragments().size());
        assertEquals(0, intervals.get(1).getFragments().size());

        assertEquals(1, intervals.get(2).getFragments().size());
        assertEquals(start.plus(minutes(5)), intervals.get(2).getFragments().get(0).getStart());
        assertEquals(start.plus(minutes(30)), intervals.get(2).getFragments().get(0).getEnd());

        assertEquals(1, intervals.get(3).getFragments().size());
        assertEquals(start.plus(minutes(30)), intervals.get(3).getFragments().get(0).getStart());
        assertEquals(start.plus(minutes(40)), intervals.get(3).getFragments().get(0).getEnd());

        assertEquals(0, intervals.get(4).getFragments().size());
        assertEquals(0, intervals.get(5).getFragments().size());
    }

    public void testDefragmentSingle() {
        List<ActivityFragment> fragments = Lists.newArrayList(new ActivityFragment("Single", start,
                start.plus(minutes(5))));
        List<ActivityFragment> output = ActivityFragment.defragment(fragments);

        assertEquals("" + output, 1, output.size());
        assertSame(fragments.get(0), output.get(0));
    }

    public void testDefragmentDouble() {
        ActivityFragment activity = new ActivityFragment("Activity", start.minus(minutes(10)),
                start.plus(minutes(30)), start, start.plus(minutes(20)));
        Pair<ActivityFragment, ActivityFragment> split = activity.splitAtTime(start.plus(minutes(5)));
        List<ActivityFragment> fragments = Lists.newArrayList(split.first, split.second);
        List<ActivityFragment> outputs = ActivityFragment.defragment(fragments);

        assertEquals("" + outputs, 1, outputs.size());
        ActivityFragment output = outputs.get(0);
        assertTrue(activity.getActivityName().equals(output.getActivityName()));
        assertTrue(activity.getActivityStart().equals(output.getActivityStart()));
        assertTrue(activity.getActivityEnd().equals(output.getActivityEnd()));
        assertTrue(activity.getStart().equals(output.getStart()));
        assertTrue(activity.getEnd().equals(output.getEnd()));
    }

    public void testDefragmentTripple() {
        ActivityFragment activity = new ActivityFragment("Activity", start.minus(minutes(10)),
                start.plus(minutes(30)), start, start.plus(minutes(20)));
        Pair<ActivityFragment, ActivityFragment> split1 = activity.splitAtTime(start.plus(minutes(5)));
        Pair<ActivityFragment, ActivityFragment> split2 = split1.second.splitAtTime(start.plus(minutes(10)));
        List<ActivityFragment> fragments = Lists.newArrayList(split1.first, split2.first, split2.second);

        List<ActivityFragment> outputs = ActivityFragment.defragment(fragments);

        assertEquals("" + outputs, 1, outputs.size());
        ActivityFragment output = outputs.get(0);
        assertTrue(activity.getActivityName().equals(output.getActivityName()));
        assertTrue(activity.getActivityStart().equals(output.getActivityStart()));
        assertTrue(activity.getActivityEnd().equals(output.getActivityEnd()));
        assertTrue(activity.getStart().equals(output.getStart()));
        assertTrue(activity.getEnd().equals(output.getEnd()));
    }
}
