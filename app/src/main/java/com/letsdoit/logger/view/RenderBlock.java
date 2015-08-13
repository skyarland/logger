package com.letsdoit.logger.view;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.letsdoit.logger.data.dao.Activity;
import com.letsdoit.logger.data.dao.ActivityFragment;

import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.Period;

import java.util.List;

/**
 * A display-layer agnostic representation of a block of time.
 * May be empty (no activities), have one or more ActivityFragments.
 *
 * Created by Andrey on 6/20/2015.
 */
public class RenderBlock {
    /**
     * There will frequently be blocks with no activities in them.  Instead of allocating lots of empty arrays,
     * just use this one.
     */
    private static final ImmutableList<ActivityFragment> EMPTY_FRAGMENTS =
            ImmutableList.<ActivityFragment>builder().build();

    private final DateTime blockStart;
    private final DateTime blockEnd;
    private final ImmutableList<ActivityFragment> fragments;

    public RenderBlock(DateTime blockStart, DateTime blockEnd, List<ActivityFragment> fragments) {
        this.blockStart = blockStart;
        this.blockEnd = blockEnd;
        this.fragments = fragments.isEmpty() ? null : ImmutableList.<ActivityFragment>copyOf(fragments);
    }

    public DateTime getBlockStart() {
        return blockStart;
    }

    public DateTime getBlockEnd() {
        return blockEnd;
    }

    public List<ActivityFragment> getFragments() {
        return fragments == null ? EMPTY_FRAGMENTS : fragments;
    }

    public Duration getDuration() {
        return new Duration(blockStart, blockEnd);
    }

    /**
     * A RenderBlock's list of fragments, start, and end time are immutable.  But it's useful to update these values
     * when you're constructing the block.  This class lets you do that.
     */
    public static class Builder {
        private DateTime blockStart;
        private DateTime blockEnd;
        private List<ActivityFragment> fragments = Lists.newArrayList();

        public Builder(DateTime blockStart) {
            this.blockStart = blockStart;
            this.blockEnd = blockStart;
        }

        public Builder withBlockStart(DateTime blockStart) {
            this.blockStart = blockStart;
            return this;
        }

        public Builder withBlockEnd(DateTime blockEnd) {
            this.blockEnd = blockEnd;
            return this;
        }

        public Builder add(ActivityFragment fragment) {
            if (!fragments.isEmpty()) {
                ActivityFragment lastFragment = fragments.get(fragments.size() - 1);
                Preconditions.checkArgument(
                        !fragment.getFragmentStart().isBefore(lastFragment.getFragmentEnd()),
                        String.format("Fragments need to be added in sequential order.  Got a fragment that" +
                                "starts before the previous one ends: previous [%s] current [%s].",
                                lastFragment, fragment));
            }

            fragments.add(fragment);
            this.blockEnd = fragment.getFragmentEnd();
            return this;
        }

        public DateTime getBlockStart() {
            return blockStart;
        }

        public DateTime getBlockEnd() {
            return blockEnd;
        }

        public Duration getDuration() {
            return new Duration(blockStart, blockEnd);
        }

        public RenderBlock build() {
            return new RenderBlock(blockStart, blockEnd, fragments);
        }

        public Builder nextBuilder() {
            return new Builder(blockEnd);
        }

    }
}
