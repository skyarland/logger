package com.letsdoit.logger.data.sqlite;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.google.common.collect.Lists;
import com.letsdoit.logger.data.dao.ActivityFragment;

import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.Period;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Andrey on 7/12/2014.
 */
public class CompletedActivityFragmentsDAO {
    private static final String TAG = "ADP_CompletedActivityFragmentsDAO";

    /**
     * An activity that was happening at a specific instant will have a fragment that started no longer than this
     * duration relative to the instant and ended no later than this duration relative to this instant.  This makes
     * figuring out "what activities were happening in this hour" much simpler than having to try to figure out when
     * the previous activity that ran into this hour started or whether there was an activity that started in this
     * hour and ran over.
     */
    private static final Duration MAX_FRAGMENT_DURATION = Period.hours(1).toStandardDuration();

    private SQLiteDatabase database;
    private LoggerDatabaseHelper dbHelper;

    public CompletedActivityFragmentsDAO(Context context) {
        this.dbHelper = new LoggerDatabaseHelper(context);
    }

    public void addCompletedActivity(ActivityFragment activity) {
        List<ActivityFragment> fragments = ActivityFragment.fragment(activity, MAX_FRAGMENT_DURATION);
        for (ActivityFragment fragment : fragments) {
            addFragment(fragment);
        }
    }

    private void addFragment(ActivityFragment fragment) {
        ContentValues values = new ContentValues();
        values.put(CompletedActivityTable.COLUMN_ACTIVITY_NAME, fragment.getActivityName());
        values.put(CompletedActivityTable.COLUMN_ACTIVITY_START, fragment.getActivityStart().getMillis());
        values.put(CompletedActivityTable.COLUMN_ACTIVITY_END, fragment.getActivityEnd().getMillis());
        values.put(CompletedActivityTable.COLUMN_START, fragment.getStart().getMillis());
        values.put(CompletedActivityTable.COLUMN_END, fragment.getEnd().getMillis());

        database.insert(CompletedActivityTable.TABLE_NAME, null, values);
    }

    /**
     * Make sure that the fragment doesn't overlap with any other fragments
     * @param fragment
     */
    public void addWithoutOverlap(ActivityFragment fragment) {

    }

    public List<ActivityFragment> getInRange(DateTime start, DateTime end) {
        Log.d(TAG, "getInRange called");

        ArrayList<ActivityFragment> fragments = Lists.newArrayList();
        fragments.addAll(queryInTimeRange(start, end));

        return ActivityFragment.defragment(fragments);
    }

    public void open() throws SQLException {
        database = dbHelper.getWritableDatabase();
    }

    public void close() {
        dbHelper.close();
    }

    public List<ActivityFragment> queryInTimeRange(DateTime intervalStart, DateTime intervalEnd) {
        List<ActivityFragment> fragments = Lists.newArrayList();

        String beginningStartTime = Long.toString(intervalStart.getMillis());
        String endingStartTime = Long.toString(intervalEnd.getMillis());

        String[] selectionArgs = {beginningStartTime, endingStartTime};
        Cursor cursor = database.query(CompletedActivityTable.TABLE_NAME,
                CompletedActivityTable.ALL_COLUMNS, CompletedActivityTable.QUERY_FRAGMENT_ON_START_TIME,
                selectionArgs, null, null, null);

        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            ActivityFragment fragment = cursorToComment(cursor);
            fragments.add(fragment);
            cursor.moveToNext();
        }

        cursor.close();
        return fragments;
    }

    private ActivityFragment cursorToComment(Cursor cursor) {
        String activityName = cursor.getString(CompletedActivityTable.COLUMN_INDEX_ACTIVITY_NAME);
        long activityStartMs = cursor.getLong(CompletedActivityTable.COLUMN_INDEX_ACTIVITY_START);
        long activityEndMs = cursor.getLong(CompletedActivityTable.COLUMN_INDEX_ACTIVITY_END);
        long startMs = cursor.getLong(CompletedActivityTable.COLUMN_INDEX_START);
        long endMs = cursor.getLong(CompletedActivityTable.COLUMN_INDEX_END);

        return new ActivityFragment(activityName, new DateTime(activityStartMs), new DateTime(activityEndMs),
                new DateTime(startMs), new DateTime(endMs));
    }

}
