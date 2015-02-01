package com.letsdoit.logger.data.sqlite;

/**
 * Created by Andrey on 7/20/2014.
 */
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.google.common.base.Preconditions;

public class LoggerDatabaseHelper extends SQLiteOpenHelper {

    public static final String DATABASE_NAME = "activities.db";

    private static final int DATABASE_VERSION = 5;

    public LoggerDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    /**
     * Create the latest version of the database if one does not exist
     * @param database
     */
    @Override
    public void onCreate(SQLiteDatabase database) {
        CompletedActivityTable.createTable(database);
    }

    /**
     * Execute the version migration procedures for every version upgrade from the version that the user currently
     * has (currentVersion) to the version that the software is currently on (newVersion).
     * @param db
     * @param currentVersion
     * @param newVersion
     */
    @Override
    public void onUpgrade(SQLiteDatabase db, int currentVersion, int newVersion) {
        Log.w(LoggerDatabaseHelper.class.getName(),"Upgrading database from version " + currentVersion + " to " +
                newVersion);

        Preconditions.checkArgument(currentVersion < newVersion, "Downgrading the database version is not supported. " +
                " Create a new database upgrade step that rolls back the changes if you want to revert to an older " +
                "database schema.");

        Preconditions.checkArgument(newVersion == DATABASE_VERSION, "Invalid upgrade database version " + newVersion
                + "The only supported database version is " + DATABASE_VERSION + ".");

        // Perform all of the database upgrades in order, starting from the current version
        switch (currentVersion) {
            case 1:
            case 2:
            case 3:
            case 4:
                // All table versions up to 5 were static testing databses.  From 5 onward, we have real user data!
                CompletedActivityTable.moveFromVersion4To5(db);
            case 5:
            case 6:
            case 7:
                // All the cases except the last one should fall through.  The last one breaks to prevent falling
                // into the default case.
                break;
            default:
                Preconditions.checkArgument(false, "Unknown database version " + currentVersion);
        }
    }

}
