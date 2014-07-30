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

import java.util.ArrayList;
import java.util.List;

import static org.joda.time.Period.hours;
import static org.joda.time.Period.minutes;

/**
 * Created by Andrey on 7/12/2014.
 */
public class CompletedActivityFragmentsDAO {
    private static final String TAG = "ADP_CompletedActivityFragmentsDAO";

    private SQLiteDatabase database;
    private LoggerDatabaseHelper dbHelper;

    public CompletedActivityFragmentsDAO(Context context) {
        this.dbHelper = new LoggerDatabaseHelper(context);
    }

    public void add(ActivityFragment fragment) {
        ContentValues values = new ContentValues();
        values.put(CompletedActivityTable.COLUMN_ACTIVITY_NAME, fragment.getActivityName());
        values.put(CompletedActivityTable.COLUMN_ACTIVITY_START, fragment.getActivityStart().getMillis());
        values.put(CompletedActivityTable.COLUMN_ACTIVITY_END, fragment.getActivityEnd().getMillis());
        values.put(CompletedActivityTable.COLUMN_START, fragment.getStart().getMillis());
        values.put(CompletedActivityTable.COLUMN_END, fragment.getEnd().getMillis());

        database.insert(CompletedActivityTable.TABLE_NAME, null, values);
    }

    public List<ActivityFragment> getAround(DateTime dateTime) {
        Log.d(TAG, "getAround called");
        ArrayList<ActivityFragment> fragments = Lists.newArrayList();
        fragments.addAll(queryInTimeRange(dateTime.minus(hours(8)), dateTime.minusHours(2)));
        fragments.addAll(getDummyActivityFragments(dateTime.minus(hours(2))));
        return ActivityFragment.defragment(fragments);
    }

    private ArrayList<ActivityFragment> getDummyActivityFragments(DateTime start) {
        return Lists.newArrayList(
                new ActivityFragment("Work Out", start, start.plus(minutes(23))),
                new ActivityFragment("Stretch", start.plus(minutes(23)), start.plus(minutes(40))),
                new ActivityFragment("Eat", start.plus(minutes(43)), start.plus(minutes(65))),
                new ActivityFragment("Program", start.plus(minutes(72)), start.plus(minutes(100)))
        );
    }

    public void open() throws SQLException {
        database = dbHelper.getWritableDatabase();
    }

    public void close() {
        dbHelper.close();
    }

    public List<ActivityFragment> queryInTimeRange(DateTime intervalStart, DateTime intervalEnd) {
        List<ActivityFragment> fragments = Lists.newArrayList();

        String[] selectionArgs = {Long.toString(intervalStart.getMillis()), Long.toString(intervalEnd.getMillis())};
        Cursor cursor = database.query(CompletedActivityTable.TABLE_NAME,
                CompletedActivityTable.ALL_COLUMNS, CompletedActivityTable.QUERY_FRAGMENT_WHERE, selectionArgs, null,
                null, null);

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