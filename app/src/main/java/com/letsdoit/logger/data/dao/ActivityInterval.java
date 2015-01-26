package com.letsdoit.logger.data.dao;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

import org.joda.time.DateTime;
import org.joda.time.Duration;

import java.util.List;

/**
 * Created by Andrey on 7/14/2014.
 */
public class ActivityInterval {
    private final DateTime start;
    private final DateTime end;
    private final Duration duration;
    private final List<ActivityFragment> fragments;

    public ActivityInterval(DateTime start, DateTime end, List<ActivityFragment> fragments) {
        this.start = start;
        this.end = end;
        this.fragments = fragments;
        this.duration = new Duration(start, end);
    }

    public Duration getDuration() {return duration;}

    public DateTime getStart() {
        return start;
    }

    public DateTime getEnd() {
        return end;
    }

    public List<ActivityFragment> getFragments() {
        return fragments;
    }

    public boolean isEmpty() {
        return fragments.isEmpty();
    }

    public ActivityFragment getActivityFragment(int index) {
        return fragments.get(index);
    }

    @Override
    public String toString() {
        return "ActivityInterval{" +
                "start=" + start +
                ", end=" + end +
                ", fragments=" + fragments +
                '}';
    }

    public static ActivityInterval fromFragment(ActivityFragment fragment) {
        return new ActivityInterval(
                fragment.getStart(),
                fragment.getEnd(),
                Lists.<ActivityFragment>newArrayList(fragment));
    }

    public static ActivityInterval emptyInterval(DateTime start, DateTime end) {
        return new ActivityInterval(start, end, Lists.<ActivityFragment>newArrayList());
    }

    public double getPercentageTimeOfPeriod(Duration interval) {
        return (double) duration.getMillis() / (double) interval.getMillis();
    }

    public ActivityInterval extendWith(ActivityInterval next) {
        Preconditions.checkArgument(end.isEqual(next.getStart()),
                String.format("Extending interval has to start right after current.  " +
                        "Current: %s.  Next: %s", this, next));
        List<ActivityFragment> extendedFragments = Lists.newLinkedList(fragments);
        extendedFragments.addAll(next.getFragments());
        return new ActivityInterval(start, next.end, extendedFragments);
    }
}
