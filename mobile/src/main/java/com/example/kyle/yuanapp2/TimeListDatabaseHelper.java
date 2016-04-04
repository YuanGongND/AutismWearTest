package com.example.kyle.yuanapp2;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.text.SimpleDateFormat;

/**
 * Created by Kyle on 2016/3/26.
 */
public class TimeListDatabaseHelper {
    private static final int DATABASE_VERSION = 2;
  //  private static String DATABASE_NAME = "timetracker.db";
    private static final String TABLE_NAME = "timerecords";
    private static final String TIMETRACKER_COLUMN_ID = "id";
    private static final String TIMETRACKER_COLUMN_TIME = "time";
    private static final String TIMETRACKER_COLUMN_VAD = "vad";
    private static final String TIMETRACKER_COLUMN_HRT = "hrt";
    private static final String TIMETRACKER_COLUMN_AX= "ax";
    private static final String TIMETRACKER_COLUMN_AY = "ay";
    private static final String TIMETRACKER_COLUMN_AZ = "az";

    private TimeTrackerOpenHelper openHelper;
    private SQLiteDatabase database;

    public TimeListDatabaseHelper(Context context,String DATABASE_NAME)
    {
        openHelper=new TimeTrackerOpenHelper(context,DATABASE_NAME);
        database=openHelper.getWritableDatabase();
    }

    public void saveTimeRecord(String time,long vad,long hrt,float ax,float ay,float az)
    {
        ContentValues contentValues=new ContentValues();
        contentValues.put(TIMETRACKER_COLUMN_TIME,time);
        contentValues.put(TIMETRACKER_COLUMN_VAD, vad);
        contentValues.put(TIMETRACKER_COLUMN_HRT, hrt);
        contentValues.put(TIMETRACKER_COLUMN_AX, ax);
        contentValues.put(TIMETRACKER_COLUMN_AY, ay);
        contentValues.put(TIMETRACKER_COLUMN_AZ, az);
        database.insert(TABLE_NAME, null, contentValues);
    }

    public class TimeTrackerOpenHelper extends SQLiteOpenHelper
    {


        TimeTrackerOpenHelper(Context context,String DATABASE_NAME)
        {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        public void onCreate(SQLiteDatabase database) {
            database.execSQL("CREATE TABLE " + TABLE_NAME + "("
                    + TIMETRACKER_COLUMN_ID + " INTEGER PRIMARY KEY, "
                    + TIMETRACKER_COLUMN_TIME + " VARCHAR, "
                    + TIMETRACKER_COLUMN_VAD + " REAL, "
                    + TIMETRACKER_COLUMN_HRT + " REAL, "
                    + TIMETRACKER_COLUMN_AX + " REAL, "
                    + TIMETRACKER_COLUMN_AY + " REAL, "
                    + TIMETRACKER_COLUMN_AZ + " REAL )");
        }

        public void onUpgrade(SQLiteDatabase database,int oldVersion,int newVersion)
        {
            database.execSQL("DROP TABLE IF EXISTS" + TABLE_NAME);
            onCreate(database);
        }

    }
}


