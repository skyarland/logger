package com.letsdoit.logger.data.dao;

import android.test.AndroidTestCase;

import com.google.common.collect.Lists;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Duration;

import java.util.List;
import java.util.UUID;

import static junit.framework.Assert.assertEquals;
import static org.joda.time.Period.hours;

/**
 * Verify fragmenting and defragmenting.
 *
 * Created by Andrey on 6/13/2015.
 */
public class FragmenterTest extends AndroidTestCase {

    static {
        // Set a default timezone because Android doesn't assume one and crashes
        // when you using joda time.
        DateTimeZone.setDefault(DateTimeZone.UTC);
    }

    private static final DateTime START = new DateTime(2014, 7, 17, 10, 30, 0, 0);

    private String activityName() {
        return "ActivityName-" + UUID.randomUUID().toString();
    }

    public void testFragment_ExactlyMaxDuration() {
        Duration maxDuration = Duration.standardHours(1);
        Activity activity = new Activity(activityName(), START, START.plus(maxDuration));

        List<ActivityFragment> fragments = Fragmenter.fragment(activity, maxDuration);
        assertEquals(1, fragments.size());

        ActivityFragment fragment = fragments.get(0);
        assertEquals(activity.getActivityName(), fragment.getActivityName());
        assertEquals(activity.getActivityStart(), fragment.getActivityStart());
        assertEquals(activity.getActivityStart(), fragment.getFragmentStart());
        assertEquals(activity.getActivityEnd(), fragment.getActivityEnd());
        assertEquals(activity.getActivityEnd(), fragment.getFragmentEnd());
    }

    public void testFragment_SmallerThanMaxDuration() {
        Duration maxDuration = Duration.standardHours(1);
        Duration activityDuration = Duration.standardMinutes(30);

        Activity activity = new Activity(activityName(), START, START.plus(activityDuration));

        List<ActivityFragment> fragments = Fragmenter.fragment(activity, maxDuration);
        assertEquals(1, fragments.size());

        ActivityFragment fragment = fragments.get(0);
        assertEquals(activity.getActivityName(), fragment.getActivityName());
        assertEquals(activity.getActivityStart(), fragment.getActivityStart());
        assertEquals(activity.getActivityStart(), fragment.getFragmentStart());
        assertEquals(activity.getActivityEnd(), fragment.getActivityEnd());
        assertEquals(activity.getActivityEnd(), fragment.getFragmentEnd());
    }

    public void testFragment_EvenMultipleOfMaxDuration() {
        Duration maxDuration = Duration.standardHours(1);

        Activity activity = new Activity(activityName(), START, START.plus(maxDuration).plus(maxDuration));

        List<ActivityFragment> fragments = Fragmenter.fragment(activity, maxDuration);
        assertEquals(2, fragments.size());

        ActivityFragment fragment1 = fragments.get(0);
        assertEquals(activity.getActivityName(), fragment1.getActivityName());
        assertEquals(activity.getActivityStart(), fragment1.getActivityStart());
        assertEquals(activity.getActivityStart(), fragment1.getFragmentStart());
        assertEquals(activity.getActivityStart().plus(maxDuration), fragment1.getFragmentEnd());
        assertEquals(activity.getActivityEnd(), fragment1.getActivityEnd());

        ActivityFragment fragment2 = fragments.get(1);
        assertEquals(activity.getActivityName(), fragment2.getActivityName());
        assertEquals(activity.getActivityStart(), fragment2.getActivityStart());
        assertEquals(activity.getActivityStart().plus(maxDuration), fragment2.getFragmentStart());
        assertEquals(activity.getActivityEnd(), fragment2.getFragmentEnd());
        assertEquals(activity.getActivityEnd(), fragment2.getActivityEnd());
    }

    public void testFragment_UnevenMultipleOfMaxDuration() {
        Duration maxDuration = Duration.standardHours(1);
        Duration activityDuration = Duration.standardMinutes(90);

        Activity activity = new Activity(activityName(), START, START.plus(activityDuration));

        List<ActivityFragment> fragments = Fragmenter.fragment(activity, maxDuration);
        assertEquals(2, fragments.size());

        ActivityFragment fragment1 = fragments.get(0);
        assertEquals(activity.getActivityName(), fragment1.getActivityName());
        assertEquals(activity.getActivityStart(), fragment1.getActivityStart());
        assertEquals(activity.getActivityStart(), fragment1.getFragmentStart());
        assertEquals(activity.getActivityStart().plus(maxDuration), fragment1.getFragmentEnd());
        assertEquals(activity.getActivityEnd(), fragment1.getActivityEnd());

        ActivityFragment fragment2 = fragments.get(1);
        assertEquals(activity.getActivityName(), fragment2.getActivityName());
        assertEquals(activity.getActivityStart(), fragment2.getActivityStart());
        assertEquals(activity.getActivityStart().plus(maxDuration), fragment2.getFragmentStart());
        assertEquals(activity.getActivityEnd(), fragment2.getFragmentEnd());
        assertEquals(activity.getActivityEnd(), fragment2.getActivityEnd());
    }

    public void testDefragment_SingleCompleteFragment() {
        ActivityFragment fragment = new ActivityFragment(activityName(), START, START.plus(hours(1)));
        List<ActivityFragment> fragments = Lists.newArrayList(fragment);

        List<Activity> activities = Fragmenter.defragment(fragments);
        assertEquals(1, activities.size());

        Activity activity = activities.get(0);
        assertEquals(activity.getActivityName(), fragment.getActivityName());
        assertEquals(activity.getActivityStart(), fragment.getActivityStart());
        assertEquals(activity.getActivityEnd(), fragment.getActivityEnd());
    }

    public void testDefragment_TailOnly() {
        ActivityFragment tail = new ActivityFragment(activityName(),
                START, START.plus(hours(3)),
                START.plus(hours(2)), START.plus(hours(3)));
        List<ActivityFragment> fragments = Lists.newArrayList(tail);

        List<Activity> activities = Fragmenter.defragment(fragments);
        assertEquals(1, activities.size());

        Activity activity = activities.get(0);
        assertEquals(activity.getActivityName(), tail.getActivityName());
        assertEquals(activity.getActivityStart(), tail.getActivityStart());
        assertEquals(activity.getActivityEnd(), tail.getActivityEnd());

    }

    public void testDefragment_HeadOnly() {
        ActivityFragment head = new ActivityFragment(activityName(),
                START, START.plus(hours(3)),
                START, START.plus(hours(1)));
        List<ActivityFragment> fragments = Lists.newArrayList(head);

        List<Activity> activities = Fragmenter.defragment(fragments);
        assertEquals(1, activities.size());

        Activity activity = activities.get(0);
        assertEquals(activity.getActivityName(), head.getActivityName());
        assertEquals(activity.getActivityStart(), head.getActivityStart());
        assertEquals(activity.getActivityEnd(), head.getActivityEnd());
    }

    public void testDefragment_SplitActivity() {
        String activityName = activityName();
        ActivityFragment fragment1 = new ActivityFragment(activityName,
                START, START.plus(hours(2)),
                START, START.plus(hours(1)));
        ActivityFragment fragment2 = new ActivityFragment(activityName,
                START, START.plus(hours(2)),
                START.plus(hours(1)), START.plus(hours(2)));

        List<ActivityFragment> fragments = Lists.newArrayList(fragment1, fragment2);

        List<Activity> activities = Fragmenter.defragment(fragments);
        assertEquals(1, activities.size());

        Activity activity = activities.get(0);
        assertEquals(activity.getActivityName(), fragment1.getActivityName());
        assertEquals(activity.getActivityStart(), fragment1.getActivityStart());
        assertEquals(activity.getActivityEnd(), fragment1.getActivityEnd());
    }

    public void testDefragment_SplitActivities() {
        String activityName1 = activityName();
        ActivityFragment activity1_fragment1 = new ActivityFragment(activityName1,
                START, START.plus(hours(2)),
                START, START.plus(hours(1)));
        ActivityFragment activity1_fragment2 = new ActivityFragment(activityName1,
                START, START.plus(hours(2)),
                START.plus(hours(1)), START.plus(hours(2)));

        String activityName2 = activityName();
        ActivityFragment activity2_fragment1 = new ActivityFragment(activityName2,
                START.plus(hours(5)), START.plus(hours(7)),
                START.plus(hours(5)), START.plus(hours(6)));
        ActivityFragment activity2_fragment2 = new ActivityFragment(activityName2,
                START.plus(hours(5)), START.plus(hours(7)),
                START.plus(hours(6)), START.plus(hours(7)));

        List<ActivityFragment> fragments = Lists.newArrayList(
                activity1_fragment1, activity1_fragment2,
                activity2_fragment1, activity2_fragment2);

        List<Activity> activities = Fragmenter.defragment(fragments);
        assertEquals(2, activities.size());

        Activity activity1 = activities.get(0);
        assertEquals(activity1.getActivityName(), activity1_fragment1.getActivityName());
        assertEquals(activity1.getActivityStart(), activity1_fragment1.getActivityStart());
        assertEquals(activity1.getActivityEnd(), activity1_fragment1.getActivityEnd());

        Activity activity2 = activities.get(1);
        assertEquals(activity2.getActivityName(), activity2_fragment1.getActivityName());
        assertEquals(activity2.getActivityStart(), activity2_fragment1.getActivityStart());
        assertEquals(activity2.getActivityEnd(), activity2_fragment1.getActivityEnd());
    }

    public void testDefragment_SplitWithHeadAndTail() {
        String activityName1 = activityName();
        ActivityFragment activity1_fragment2 = new ActivityFragment(activityName1,
                START, START.plus(hours(2)),
                START.plus(hours(1)), START.plus(hours(2)));

        String activityName2 = activityName();
        ActivityFragment activity2_fragment1 = new ActivityFragment(activityName2,
                START.plus(hours(5)), START.plus(hours(7)),
                START.plus(hours(5)), START.plus(hours(6)));
        ActivityFragment activity2_fragment2 = new ActivityFragment(activityName2,
                START.plus(hours(5)), START.plus(hours(7)),
                START.plus(hours(6)), START.plus(hours(7)));

        String activityName3 = activityName();
        ActivityFragment activity3_fragment1 = new ActivityFragment(activityName3,
                START.plus(hours(7)), START.plus(hours(10)),
                START.plus(hours(7)), START.plus(hours(8)));

        List<ActivityFragment> fragments = Lists.newArrayList(
                                     activity1_fragment2,
                activity2_fragment1, activity2_fragment2,
                activity3_fragment1
        );

        List<Activity> activities = Fragmenter.defragment(fragments);
        assertEquals(3, activities.size());

        Activity activity1 = activities.get(0);
        assertEquals(activity1.getActivityName(), activity1_fragment2.getActivityName());
        assertEquals(activity1.getActivityStart(), activity1_fragment2.getActivityStart());
        assertEquals(activity1.getActivityEnd(), activity1_fragment2.getActivityEnd());

        Activity activity2 = activities.get(1);
        assertEquals(activity2.getActivityName(), activity2_fragment1.getActivityName());
        assertEquals(activity2.getActivityStart(), activity2_fragment1.getActivityStart());
        assertEquals(activity2.getActivityEnd(), activity2_fragment1.getActivityEnd());

        Activity activity3 = activities.get(2);
        assertEquals(activity3.getActivityName(), activity3_fragment1.getActivityName());
        assertEquals(activity3.getActivityStart(), activity3_fragment1.getActivityStart());
        assertEquals(activity3.getActivityEnd(), activity3_fragment1.getActivityEnd());
    }

    public void testDefragment_FragmentedAndClipped() {
        // [---][--][-][---]
        List<Activity> activities = Lists.newArrayList(
                new Activity(activityName(), START, START.plus(hours(3))),
                new Activity(activityName(), START.plus(hours(3)), START.plus(hours(5))),
                new Activity(activityName(), START.plus(hours(5)), START.plus(hours(6))),
                new Activity(activityName(), START.plus(hours(6)), START.plus(hours(9)))
        );

        List<ActivityFragment> fragments = Fragmenter.fragment(activities, hours(1).toStandardDuration());

        // Make the first one a tail
        fragments.remove(0);
        fragments.remove(0);

        // Make the last one a head, but with two fragments
        fragments.remove(fragments.size() - 1);

        // --[-][--][-][--]-
        assertEquals(6, fragments.size());

        List<Activity> reconstructedActivities = Fragmenter.defragment(fragments);
        assertEquals(activities.size(), reconstructedActivities.size());

        for (int i = 0; i < activities.size(); i++) {
            Activity activity = activities.get(i);
            Activity reconstructedActivity = reconstructedActivities.get(i);

            assertEquals(activity.getActivityName(), reconstructedActivity.getActivityName());
            assertEquals(activity.getActivityStart(), reconstructedActivity.getActivityStart());
            assertEquals(activity.getActivityEnd(), reconstructedActivity.getActivityEnd());
            assertEquals(activity, reconstructedActivity);
        }
    }
}
