package com.letsdoit.logger.data.sqlite;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.letsdoit.logger.data.dao.ActivityFragment;

import org.joda.time.DateTime;

import static org.joda.time.Period.hours;
import static org.joda.time.Period.minutes;

/**
 * Created by Andrey on 7/20/2014.
 */
public class CompletedActivityTable {
    private static final String TAG = "CompletedActivityTable";

    public static final String TABLE_NAME = "CompletedActivityFragment";
    public static final String TABLE_INDEX_NAME = "FRAGMENT_START_TIME";

    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_ACTIVITY_NAME = "activityName";
    public static final String COLUMN_ACTIVITY_START = "activityStart";
    public static final String COLUMN_ACTIVITY_END = "activityEnd";
    public static final String COLUMN_START = "start";
    public static final String COLUMN_END = "end";

    public static final String[] ALL_COLUMNS = {COLUMN_ID, COLUMN_ACTIVITY_NAME, COLUMN_ACTIVITY_START,
            COLUMN_ACTIVITY_END, COLUMN_START, COLUMN_END};

    // Index of the column in ALL_COLUMNS, used to query from cursors
    public static final int COLUMN_INDEX_ID = 0;
    public static final int COLUMN_INDEX_ACTIVITY_NAME = 1;
    public static final int COLUMN_INDEX_ACTIVITY_START = 2;
    public static final int COLUMN_INDEX_ACTIVITY_END = 3;
    public static final int COLUMN_INDEX_START = 4;
    public static final int COLUMN_INDEX_END = 5;


    private static final String SQL_CREATE_TABLE = "create table " + TABLE_NAME + "("
            + COLUMN_ID + " integer primary key autoincrement, "
            + COLUMN_ACTIVITY_NAME + " text not null, "
            + COLUMN_ACTIVITY_START + " integer not null, "
            + COLUMN_ACTIVITY_END + " integer not null, "
            + COLUMN_START + " integer not null, "
            + COLUMN_END + " integer not null"
            + ");";

    private static final String SQL_CREATE_INDEX = "create index " + TABLE_INDEX_NAME
            + " on " + TABLE_NAME + "(" + COLUMN_START + ")";

    public static void createTable(SQLiteDatabase database) {
        database.execSQL(SQL_CREATE_TABLE);
        database.execSQL(SQL_CREATE_INDEX);

        DateTime now = new DateTime();

        // The dummy data starts 2 hours from whenever "now" is when the user opens the app.
        // So our dummy data should not overlap with this.
        add(database, new ActivityFragment("Code", now.minus(hours(4)), now.minus(hours(3)).minus(minutes(20))));
        add(database, new ActivityFragment("Test", now.minus(hours(3)).minus(minutes(20)), now.minus(hours(3))));
        add(database, new ActivityFragment("Play", now.minus(hours(3)), now.minus(hours(2)).minus(minutes(40))));
        add(database, new ActivityFragment("Eat", now.minus(hours(2)).minus(minutes(30)),
                now.minus(hours(2)).minus(minutes(10))));
    }

    public static final String QUERY_FRAGMENT_WHERE = COLUMN_START + " between ? and ?";

    // TODO: Delete this once the database starts really being used
    private static void add(SQLiteDatabase database, ActivityFragment fragment) {
        ContentValues values = new ContentValues();
        values.put(CompletedActivityTable.COLUMN_ACTIVITY_NAME, fragment.getActivityName());
        values.put(CompletedActivityTable.COLUMN_ACTIVITY_START, fragment.getActivityStart().getMillis());
        values.put(CompletedActivityTable.COLUMN_ACTIVITY_END, fragment.getActivityEnd().getMillis());
        values.put(CompletedActivityTable.COLUMN_START, fragment.getStart().getMillis());
        values.put(CompletedActivityTable.COLUMN_END, fragment.getEnd().getMillis());

        database.insert(CompletedActivityTable.TABLE_NAME, null, values);
    }

    // This is a utility class with only static members.  Don't allow instantiation.
    private CompletedActivityTable() {}

    public static void moveFromVersion3To4(SQLiteDatabase database) {
        Log.d(TAG, "Dropping index and table");

        database.execSQL("DROP INDEX IF EXISTS " + TABLE_INDEX_NAME);
        database.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);

        createTable(database);
    }
}
