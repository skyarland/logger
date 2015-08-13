package com.letsdoit.logger.view;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.letsdoit.logger.data.dao.ActivityFragment;
import com.letsdoit.logger.data.dao.ActivityInterval;

import org.joda.time.DateTime;
import org.joda.time.Duration;

import java.util.Iterator;
import java.util.List;

/**
 * Converts an ActivityInterval list of RenderBlocks which will get rendered.
 *
 * Fills empty times (ones with no Activity) with fixed size empty blocks.
 *
 * Makes sure that no block is smaller than the configured size.
 *
 * TODO: Render is not the right verb.  Rendering is when you actually draw something
 * onto the screen.  This is the "pre-rendering" stage - the DisplayBlocks are the
 * render-layer-agnostic representation that can get rendered.
 *
 * Created by Andrey on 6/20/2015.
 */
public class IntervalRenderer {

    /**
     * Break an ActivityInterval into a set of RenderBlocks, making sure that no RenderBlock is smaller than the
     * minBlockDuration and blocks of free time are partitioned on the freeTimePartitionDuration relative to the
     * start of the ActivityInterval.
     *
     * @param interval a time interval with activities and free time between activities.
     * @param minBlockDuration the smallest allowed size for a render block.  None of the returned RenderBlocks will
     *                         be smaller than the minBlockDuration.
     * @param freeTimePartitionDuration the time interval relative to the start of the ActivityInterval that free time
     *                                  should be split on as long as the resulting RenderBlocks are not smaller than
     *                                  the minBlockDuration.
     * @return a list of RenderBlocks representing the ActivityInterval's activities and free time.
     */
    public static List<RenderBlock> render(ActivityInterval interval,
                                           Duration minBlockDuration,
                                           Duration freeTimePartitionDuration) {

        Preconditions.checkArgument(minBlockDuration.isShorterThan(interval.getDuration()),
                String.format("The minBlockDuration cannot be larger than the interval.  " +
                        "Interval [%s], minBlockDuration [%s].", interval, minBlockDuration));

        long remainder = interval.getDuration().getMillis() % freeTimePartitionDuration.getMillis();
        Preconditions.checkArgument(remainder == 0,
                String.format("The freeTimePartitionDuration needs to evenly divide the interval." +
                        "  Interval [%s], freeTimePartitionDuration [%s], remainder [%s]", interval,
                        freeTimePartitionDuration, remainder));

        DateTime lastBlockCutoff = interval.getEnd().minus(minBlockDuration);

        Iterator<ActivityFragment> fragmentIter = interval.getFragments().iterator();
        ActivityFragment fragment = nextOrNull(fragmentIter);
        DateTime freeTimePartition = interval.getStart();
        RenderBlock.Builder blockBuilder = new RenderBlock.Builder(interval.getStart());
        List<RenderBlock> blocks = Lists.newArrayList();

        // This loop only handles blocks that aren't too close to the end of the interval
        while (blockBuilder.getBlockEnd().isBefore(lastBlockCutoff)) {

            // Make sure that the freeTimePartition is pointing to the next partition
            while (!freeTimePartition.isAfter(blockBuilder.getBlockEnd())) {
                freeTimePartition = freeTimePartition.plus(freeTimePartitionDuration);
            }

            if (!blockBuilder.getDuration().isShorterThan(minBlockDuration)) {
                blocks.add(blockBuilder.build());
                blockBuilder = blockBuilder.nextBuilder();
            } else {

                if (fragment == null) {
                    // There are no more activities in this interval, only free time left
                    blockBuilder.withBlockEnd(freeTimePartition);
                } else if (blockBuilder.getBlockEnd().isEqual(fragment.getFragmentStart())) {
                    // Fragments will frequently start back to back.  Don't compute the duration to the next fragment
                    // if that's the case since we know it's too close and should be added to this block.
                    blockBuilder.add(fragment);
                    fragment = nextOrNull(fragmentIter);
                } else {
                    // There are more fragments in the activity interval, but the next fragment doesn't start
                    // immediately after the current one.
                    Duration durationToNextFragment = new Duration(
                            blockBuilder.getBlockStart(),
                            fragment.getFragmentStart());

                    if (durationToNextFragment.isShorterThan(minBlockDuration)) {
                        // Include the next fragment if it's too close to create an empty block
                        blockBuilder.add(fragment);
                        fragment = nextOrNull(fragmentIter);
                    } else if (fragment.getFragmentStart().isBefore(freeTimePartition)) {
                        // If the fragment is closer than the freeTimePartition, extend the current block until
                        // it touches the next fragment
                        blockBuilder.withBlockEnd(fragment.getFragmentStart());
                    } else {
                        // If the freeTimePartition is before the fragment start, extend the block to the partition
                        // and let the next iteration of the loop figure out what to do with remaining free time
                        // and the next fragment.
                        blockBuilder.withBlockEnd(freeTimePartition);
                    }
                }
            }
        }

        // The above loop exists if we get too close to the end of the ActivityInterval.
        // All of the free time and fragments go into the last block if we get too close to the end.
        if (blockBuilder.getBlockEnd().isBefore(interval.getEnd())) {
            while (fragmentIter.hasNext()) {
                blockBuilder.add(fragmentIter.next());
            }
            blockBuilder.withBlockEnd(interval.getEnd());
        }

        // The while loop above only adds blocks at the beginning of the loop.
        // The last block will never get added, since it will end "too close to the end."
        // Whether we had to fill it out, or it was complete upon exiting the loop,
        // add it to the list of blocks.
        blocks.add(blockBuilder.build());

        return blocks;
    }

    private static ActivityFragment nextOrNull(Iterator<ActivityFragment> fragmentIter) {
        return fragmentIter.hasNext() ? fragmentIter.next() : null;
    }

}
