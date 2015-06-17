package com.letsdoit.logger.data.dao;

import android.test.AndroidTestCase;

import com.google.common.collect.Lists;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.util.List;
import java.util.UUID;

import static org.joda.time.Period.minutes;

/**
 * Created by Andrey on 6/14/2015.
 */
public class PartitionerTest extends AndroidTestCase {

    static {
        // Set a default timezone because Android doesn't assume one and crashes
        // when you using joda time.
        DateTimeZone.setDefault(DateTimeZone.UTC);
    }

    private static final DateTime START = new DateTime(2014, 7, 17, 10, 30, 0, 0);

    private String activityName() {
        return "ActivityName-" + UUID.randomUUID().toString();
    }

    private void assertFragment(Activity activity,
                                int expectedFragmentStartOffsetMinutes,
                                int expectedFragmentEndOffsetMinutes,
                                ActivityFragment fragment) {
        assertEquals(activity.getActivityName(), fragment.getActivityName());
        assertEquals(activity.getActivityStart(), fragment.getActivityStart());
        assertEquals(START.plus(minutes(expectedFragmentStartOffsetMinutes)), fragment.getFragmentStart());
        assertEquals(START.plus(minutes(expectedFragmentEndOffsetMinutes)), fragment.getFragmentEnd());
        assertEquals(activity.getActivityEnd(), fragment.getActivityEnd());
    }

    public void testPartition_NoActivities() {
        List<Activity> activities = Lists.newArrayList();

        List<ActivityInterval> intervals = Partitioner.partition(activities,
                START, START.plus(minutes(30)),
                minutes(30).toStandardDuration());

        assertEquals(1, intervals.size());
        assertTrue(intervals.get(0).getFragments().isEmpty());
    }

    public void testPartition_ActivitiesBeforeInterval() {
        List<Activity> activities = Lists.newArrayList(
                new Activity(activityName(), START.minus(minutes(60)), START.minus(minutes(30))));

        List<ActivityInterval> intervals = Partitioner.partition(activities,
                START, START.plus(minutes(30)),
                minutes(30).toStandardDuration());

        assertEquals(1, intervals.size());
        assertTrue(intervals.get(0).getFragments().isEmpty());
    }

    public void testPartition_ActivitySpansInterval() {
        Activity activity = new Activity(activityName(), START, START.plus(minutes(30)));
        List<Activity> activities = Lists.newArrayList(activity);

        List<ActivityInterval> intervals = Partitioner.partition(activities,
                START, START.plus(minutes(30)),
                minutes(30).toStandardDuration());

        assertEquals(1, intervals.size());
        ActivityInterval interval = intervals.get(0);
        assertEquals(1, interval.getFragments().size());

        ActivityFragment fragment = interval.getFragments().get(0);
        assertFragment(activity, 0, 30, fragment);
    }

    public void testPartition_ActivitiesAfterInterval() {
        List<Activity> activities = Lists.newArrayList(
                new Activity(activityName(), START.plus(minutes(60)), START.plus(minutes(90))));

        List<ActivityInterval> intervals = Partitioner.partition(activities,
                START, START.plus(minutes(30)),
                minutes(30).toStandardDuration());

        assertEquals(1, intervals.size());
        assertTrue(intervals.get(0).getFragments().isEmpty());
    }

    public void testPartition_LongActivitySpanningPastInterval() {
        Activity activity = new Activity(activityName(), START.minus(minutes(30)), START.plus(minutes(60)));
        List<Activity> activities = Lists.newArrayList(activity);

        List<ActivityInterval> intervals = Partitioner.partition(activities,
                START, START.plus(minutes(30)),
                minutes(30).toStandardDuration());

        assertEquals(1, intervals.size());
        ActivityInterval interval = intervals.get(0);
        assertEquals(1, interval.getFragments().size());

        ActivityFragment fragment = interval.getFragments().get(0);
        assertFragment(activity, 0, 30, fragment);
    }

    public void testPartition_MultipleActivitiesInSameInterval() {
        Activity activity1 = new Activity(activityName(), START.minus(minutes(10)), START.plus(minutes(10)));
        Activity activity2 = new Activity(activityName(), START.plus(minutes(10)), START.plus(minutes(15)));
        Activity activity3 = new Activity(activityName(), START.plus(minutes(15)), START.plus(minutes(30)));
        Activity activity4 = new Activity(activityName(), START.plus(minutes(30)), START.plus(minutes(40)));
        Activity activity5 = new Activity(activityName(), START.plus(minutes(50)), START.plus(minutes(75)));
        Activity activity6 = new Activity(activityName(), START.plus(minutes(120)), START.plus(minutes(130)));
        Activity activity7 = new Activity(activityName(), START.plus(minutes(150)), START.plus(minutes(160)));

        List<Activity> activities = Lists.newArrayList(activity1, activity2, activity3, activity4, activity5,
                activity6, activity7);

        List<ActivityInterval> intervals = Partitioner.partition(activities,
                START, START.plus(minutes(150)),
                minutes(30).toStandardDuration());

        assertEquals(5, intervals.size());

        List<ActivityFragment> interval00Fragments = intervals.get(0).getFragments();
        assertEquals(3, interval00Fragments.size());
        assertFragment(activity1, 0, 10, interval00Fragments.get(0));
        assertFragment(activity2, 10, 15, interval00Fragments.get(1));
        assertFragment(activity3, 15, 30, interval00Fragments.get(2));

        List<ActivityFragment> interval30Fragments = intervals.get(1).getFragments();
        assertEquals(2, interval30Fragments.size());
        assertFragment(activity4, 30, 40, interval30Fragments.get(0));
        assertFragment(activity5, 50, 60, interval30Fragments.get(1));

        List<ActivityFragment> interval60Fragments = intervals.get(2).getFragments();
        assertEquals(1, interval60Fragments.size());
        assertFragment(activity5, 60, 75, interval60Fragments.get(0));

        List<ActivityFragment> interval90Fragments = intervals.get(3).getFragments();
        assertTrue(interval90Fragments.isEmpty());

        List<ActivityFragment> interval120Fragments = intervals.get(4).getFragments();
        assertEquals(1, interval120Fragments.size());
        assertFragment(activity6, 120, 130, interval120Fragments.get(0));
    }

}
