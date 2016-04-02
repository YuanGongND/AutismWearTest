package com.example.kyle.yuanapp2;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by Kyle on 2016/3/26.
 */
public class TimeListDatabaseHelper {
    private static final int DATABASE_VERSION = 2;
    private static final String DATABASE_NAME = "timetracker.db";
    private static final String TABLE_NAME = "timerecords";
    private static final String TIMETRACKER_COLUMN_ID = "id";
    private static final String TIMETRACKER_COLUMN_TIME = "time";
    private static final String TIMETRACKER_COLUMN_NOTES = "notes";

    private TimeTrackerOpenHelper openHelper;
    private SQLiteDatabase database;

    public TimeListDatabaseHelper(Context context)
    {
        openHelper=new TimeTrackerOpenHelper(context);
        database=openHelper.getWritableDatabase();
    }

    public void saveTimeRecord(long time,long notes)
    {
        ContentValues contentValues=new ContentValues();
        contentValues.put(TIMETRACKER_COLUMN_TIME,time);
        contentValues.put(TIMETRACKER_COLUMN_NOTES,notes);
        database.insert(TABLE_NAME,null,contentValues);
    }

    public class TimeTrackerOpenHelper extends SQLiteOpenHelper
    {


        TimeTrackerOpenHelper(Context context)
        {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        public void onCreate(SQLiteDatabase database) {
            database.execSQL("CREATE TABLE " + TABLE_NAME + "("
                    + TIMETRACKER_COLUMN_ID + " INTEGER PRIMARY KEY, "
                    + TIMETRACKER_COLUMN_TIME + " REAL, "
                    + TIMETRACKER_COLUMN_NOTES + " REAL )");
        }

        public void onUpgrade(SQLiteDatabase database,int oldVersion,int newVersion)
        {
            database.execSQL("DROP TABLE IF EXISTS" + TABLE_NAME);
            onCreate(database);
        }

    }
}


