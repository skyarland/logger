package com.letsdoit.logger.loader;

import android.content.AsyncTaskLoader;
import android.content.Context;
import android.util.Log;

import com.google.common.base.Preconditions;
import com.letsdoit.logger.data.dao.ActivityFragment;
import com.letsdoit.logger.data.sqlite.CompletedActivityFragmentsDAO;

import org.joda.time.DateTime;

import java.util.List;

import static org.joda.time.Period.hours;

/**
 * Created by Andrey on 7/12/2014.
 */
public class CompletedActivityFragmentLoader extends AsyncTaskLoader<List<ActivityFragment>> {

    private static final String TAG = "ADP_CompletedActivityFragmentLoader";
    private static final boolean DEBUG = true;

    private final CompletedActivityFragmentsDAO dao;

    // The time range we are querying for
    private DateTime start;
    private DateTime end;

    // We hold a reference to the Loader's data here.
    private List<ActivityFragment> fragments;

    public CompletedActivityFragmentLoader(Context context, CompletedActivityFragmentsDAO dao) {
        // Loaders may be used across multiple Activitys (assuming they aren't
        // bound to the LoaderManager), so NEVER hold a reference to the context
        // directly. Doing so will cause you to leak an entire Activity's context.
        // The superclass constructor will store a reference to the Application
        // Context instead, and can be retrieved with a call to getContext().
        super(context);
        this.dao = dao;

        // Load activities for the past 8 hours by default
        this.end = DateTime.now();
        this.start = end.minus(hours(8));
    }

    /****************************************************/
    /** (1) A task that performs the asynchronous load **/
    /****************************************************/

    /**
     * This method is called on a background thread and generates a List of
     * {@link ActivityFragment} objects. Each entry corresponds to a single installed
     * application on the device.
     */
    @Override
    public List<ActivityFragment> loadInBackground() {
        Preconditions.checkArgument(start != null, "The start time cannot be null.");
        Preconditions.checkArgument(end != null, "The end time cannot be null.");

        Log.i(TAG, "+++ loadInBackground() called! +++");

        dao.open();
        // Retrieve activity fragments in the specified range
        List<ActivityFragment> fragments = dao.getInRange(start, end);
        dao.close();

        return fragments;
    }

    /*******************************************/
    /** (2) Deliver the results to the client **/
    /*******************************************/

    /**
     * Called when there is new data to deliver to the client. The superclass will
     * deliver it to the registered listener (i.e. the LoaderManager), which will
     * forward the results to the client through a call to onLoadFinished.
     */
    @Override
    public void deliverResult(List<ActivityFragment> fragments) {
        if (isReset()) {
            if (DEBUG) Log.w(TAG, "+++ Warning! An async query came in while the Loader was reset! +++");
            // The Loader has been reset; ignore the result and invalidate the data.
            // This can happen when the Loader is reset while an asynchronous query
            // is working in the background. That is, when the background thread
            // finishes its work and attempts to deliver the results to the client,
            // it will see here that the Loader has been reset and discard any
            // resources associated with the new data as necessary.
            if (fragments != null) {
                releaseResources(fragments);
                return;
            }
        }

        // Hold a reference to the old data so it doesn't get garbage collected.
        // We must protect it until the new data has been delivered.
        List<ActivityFragment> oldFragments = this.fragments;
        this.fragments = fragments;

        if (isStarted()) {
            if (DEBUG) Log.i(TAG, "+++ Delivering results to the LoaderManager for" +
                    " the ListFragment to display! +++");
            // If the Loader is in a started state, have the superclass deliver the
            // results to the client.
            super.deliverResult(fragments);
        }

        // Invalidate the old data as we don't need it any more.
        if (oldFragments != null && oldFragments != fragments) {
            if (DEBUG) Log.i(TAG, "+++ Releasing any old data associated with this Loader. +++");
            releaseResources(oldFragments);
        }
    }

    /*********************************************************/
    /** (3) Implement the Loader's state-dependent behavior **/
    /*********************************************************/

    @Override
    protected void onStartLoading() {
        if (DEBUG) Log.i(TAG, "+++ onStartLoading() called! +++");

        if (fragments != null) {
            // Deliver any previously loaded data immediately.
            if (DEBUG) Log.i(TAG, "+++ Delivering previously loaded data to the client...");
            deliverResult(fragments);
        }

        // TODO: Register the observers that will notify the Loader when changes are made.

        if (takeContentChanged()) {
            // When the observer detects a new installed application, it will call
            // onContentChanged() on the Loader, which will cause the next call to
            // takeContentChanged() to return true. If this is ever the case (or if
            // the current data is null), we force a new load.
            if (DEBUG) Log.i(TAG, "+++ A content change has been detected... so force load! +++");
            forceLoad();
        } else if (fragments == null) {
            // If the current data is null... then we should make it non-null! :)
            if (DEBUG) Log.i(TAG, "+++ The current data is data is null... so force load! +++");
            forceLoad();
        }
    }

    @Override
    protected void onStopLoading() {
        if (DEBUG) Log.i(TAG, "+++ onStopLoading() called! +++");

        // The Loader has been put in a stopped state, so we should attempt to
        // cancel the current load (if there is one).
        cancelLoad();

        // Note that we leave the observer as is; Loaders in a stopped state
        // should still monitor the data source for changes so that the Loader
        // will know to force a new load if it is ever started again.
    }

    @Override
    protected void onReset() {
        if (DEBUG) Log.i(TAG, "+++ onReset() called! +++");

        // Ensure the loader is stopped.
        onStopLoading();

        // At this point we can release the resources associated with 'apps'.
        if (fragments != null) {
            releaseResources(fragments);
            fragments = null;
        }

        // The Loader is being reset, so we should stop monitoring for changes.

    }

    @Override
    public void onCanceled(List<ActivityFragment> fragments) {
        if (DEBUG) Log.i(TAG, "+++ onCanceled() called! +++");

        // Attempt to cancel the current asynchronous load.
        super.onCanceled(fragments);

        // The load has been canceled, so we should release the resources
        // held in the local fields.
        releaseResources(fragments);
    }

    @Override
    public void forceLoad() {
        if (DEBUG) Log.i(TAG, "+++ forceLoad() called! +++");
        super.forceLoad();
    }

    /**
     * Helper method to take care of releasing resources associated with an
     * actively loaded data set.
     */
    private void releaseResources(List<ActivityFragment> apps) {
        // For a simple List, there is nothing to do. For something like a Cursor,
        // we would close it in this method. All resources associated with the
        // Loader should be released here.
    }

    /*********************************************************************/
    /** TODO: (4) Observer which receives notifications when the data changes **/
    /*********************************************************************/

    public void setStart(DateTime start) {
        this.start = start;
    }

    public void setEnd(DateTime end) {
        this.end = end;
    }

    public DateTime getStart() {
        return start;
    }

    public DateTime getEnd() {
        return end;
    }
}
