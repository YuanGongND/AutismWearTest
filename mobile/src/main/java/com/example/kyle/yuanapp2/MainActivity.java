package com.example.kyle.yuanapp2;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.app.NotificationCompat.WearableExtender;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;

public class MainActivity extends AppCompatActivity {

    public static int sign=0;
    public String databasename,timetemp,pathtemp,outpathtemp;
    Boolean databaserecording=false;
    private BroadcastReceiver mResultReceiver;
    private GoogleApiClient mGoogleApiClient;
    private int mColorCount = 0;
    private TimeListDatabaseHelper databaseHelper;
    Button dstart,dstop;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });
       // Intent sint=new Intent(MainActivity.this, DataLayerListenerServicePhone.class);
       // startService(sint);
        dstart = (Button)findViewById(R.id.startdatabase);
        dstop = (Button)findViewById(R.id.stopdatabase);
        dstart.setOnClickListener(dstartOnClickListener);
        dstop.setOnClickListener(dstopOnClickListener);

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                    @Override
                    public void onConnected(Bundle connectionHint) {
                        Log.v("yuan-mobile", "Connection established");
                    }
                    @Override
                    public void onConnectionSuspended(int cause) {
                        Log.v("yuan-mobile", "Connection suspended");
                    }
                })
                .addOnConnectionFailedListener(new GoogleApiClient.OnConnectionFailedListener() {
                    @Override
                    public void onConnectionFailed(ConnectionResult result) {
                        Log.v("yuan-mobile", "Connection failed");
                    }
                })
                .addApi(Wearable.API)
                .build();
        mGoogleApiClient.connect();

        mResultReceiver = createBroadcastReceiver();
        LocalBroadcastManager.getInstance(this).registerReceiver(
                mResultReceiver,
                new IntentFilter("phone.localIntent"));

        sendTextToWear();
        //databaseHelper=new TimeListDatabaseHelper(this);
//        TimeTrackerOpenHelper openHelper=new TimeTrackerOpenHelper(this);
    }

    // database botton
    View.OnClickListener dstartOnClickListener
            = new View.OnClickListener(){
        @Override
        public void onClick(View arg0) {
            dstart.setEnabled(false);
            dstop.setEnabled(true);
            databaserecording =true;
            creatdatabase();
            Log.d("yuan-wear", "Created the database and start to record data");
            Toast.makeText(getApplicationContext(), "database created", Toast.LENGTH_SHORT).show();
        }};

    //database button
    View.OnClickListener dstopOnClickListener
            = new View.OnClickListener(){
        @Override
        public void onClick(View arg0) {
            dstart.setEnabled(true);
            dstop.setEnabled(false);
            databaserecording =false;
            writedatabase();
            //databaseHelper=new TimeListDatabaseHelper(MainActivity.this);
            Log.d("yuan-wear", "database saved");
            Toast.makeText(getApplicationContext(), "database saved", Toast.LENGTH_SHORT).show();
        }};


    // create database
    public void creatdatabase()
    {
        SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        java.util.Date date=new java.util.Date();
        timetemp=sdf.format(date);
        databasename=timetemp+".db";
        databaseHelper=new TimeListDatabaseHelper(MainActivity.this,databasename);
    }

    //create database
    public void writedatabase()
    {
        pathtemp="/data/data/com.example.kyle.yuanapp2/databases/"+databasename;
        outpathtemp="/mnt/sdcard/yuan/"+databasename;
        File f=new File(pathtemp);
        File fo=new File(outpathtemp);
        FileInputStream fis=null;
        FileOutputStream fos=null;

        try
        {
            fis=new FileInputStream(f);
            fos=new FileOutputStream(fo);
            while(true)
            {
                int i=fis.read();
                if(i!=-1)
                {fos.write(i);}
                else
                {break;}
            }
            fos.flush();
            Toast.makeText(this, "Database saved to sdcard successfully", Toast.LENGTH_LONG).show();
        }
        catch(Exception e)
        {
            e.printStackTrace();
            Toast.makeText(this, "DB dump ERROR", Toast.LENGTH_LONG).show();
        }
        finally
        {
            try
            {
                fos.close();
                fis.close();
            }
            catch(IOException ioe)
            {}
        }
    }


    //currently useless code
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    //currently useless code
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }


    //send information to wear
    public void sendTextToWear()
    {
        int r = (int) (255 * Math.random());
        int g = (int) (255 * Math.random());
        int b = (int) (255 * Math.random());
        final PutDataMapRequest putRequest = PutDataMapRequest.create("/PHONE2WEAR");
        final DataMap map = putRequest.getDataMap();
        map.putInt("color", Color.rgb(r, g, b));
        map.putString("colorChanges", "Amount of changes: " + mColorCount++);
        PutDataRequest putDataReq=putRequest.asPutDataRequest();
        Wearable.DataApi.putDataItem(mGoogleApiClient,putDataReq);
        PendingResult<DataApi.DataItemResult> pendingResult =
                Wearable.DataApi.putDataItem(mGoogleApiClient, putDataReq);
    }

    // update data to the screen
    private void updateTextFieldvad(String text) {
        Log.v("yuan-mobile", "Arrived text:" + text);
        ((TextView)findViewById(R.id.vad)).setText(text);
    }
    private void updateTextFieldhrt(String text) {
        Log.v("yuan-mobile", "Arrived text:" + text);
        ((TextView)findViewById(R.id.hrt_text)).setText(text);
    }
    private void updateTextFieldspd(String text) {
        Log.v("yuan-mobile", "Arrived text:" + text);
        ((TextView)findViewById(R.id.spd_text)).setText(text);
    }

    private void updateTextFieldax(String text) {
        Log.v("yuan-mobile", "Arrived text:" + text);
        ((TextView)findViewById(R.id.a_x)).setText(text);
    }

    private void updateTextFielday(String text) {
        Log.v("yuan-mobile", "Arrived text:" + text);
        ((TextView)findViewById(R.id.a_y)).setText(text);
    }

    private void updateTextFieldaz(String text) {
        Log.v("yuan-mobile", "Arrived text:" + text);
        ((TextView)findViewById(R.id.a_z)).setText(text);
    }

    private void updateTextFieldgspd(String text) {
        Log.v("yuan-mobile", "Arrived text:" + text);
        ((TextView)findViewById(R.id.gspd)).setText(text);
    }

    private void updateTextFieldgx(String text) {
        Log.v("yuan-mobile", "Arrived text:" + text);
        ((TextView)findViewById(R.id.g_x)).setText(text);
    }

    private void updateTextFieldgy(String text) {
        Log.v("yuan-mobile", "Arrived text:" + text);
        ((TextView)findViewById(R.id.g_y)).setText(text);
    }

    private void updateTextFieldgz(String text) {
        Log.v("yuan-mobile", "Arrived text:" + text);
        ((TextView)findViewById(R.id.g_z)).setText(text);
    }

    // receive data from the datalayer and prepare to show on screen
    private BroadcastReceiver createBroadcastReceiver() {
        return new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                updateTextFieldvad(intent.getStringExtra("vad"));
                updateTextFieldhrt(intent.getStringExtra("hrt"));
                updateTextFieldspd(intent.getStringExtra("spd"));
                updateTextFieldax(intent.getStringExtra("ax"));
                updateTextFielday(intent.getStringExtra("ay"));
                updateTextFieldaz(intent.getStringExtra("az"));
                updateTextFieldgspd(intent.getStringExtra("gspd"));
                updateTextFieldgx(intent.getStringExtra("gx"));
                updateTextFieldgy(intent.getStringExtra("gy"));
                updateTextFieldgz(intent.getStringExtra("gz"));
                SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                java.util.Date date=new java.util.Date();
                timetemp=sdf.format(date);
                if(databaserecording) {
                    databaseHelper.saveTimeRecord(timetemp,intent.getLongExtra("vadvalue", 0), intent.getLongExtra("hrtvalue", 0), intent.getFloatExtra("axvalue", 0), intent.getFloatExtra("ayvalue", 0), intent.getFloatExtra("azvalue", 0));
                }
            }
        };
    }

    // start button and show notification currently invisible
    public void onStartMonitor(View view)
    {
        RadioButton r1=(RadioButton)findViewById(R.id.radioButton);
        if(sign==0)
          {
            r1.setText("Stop");
            r1.setChecked(true);
            sign = 1;
            int notificationId = 001;
            // Build intent for notification content
            Intent viewIntent = new Intent(this, ViewEventActivity.class);
            Intent intent = viewIntent.putExtra("test", 003);
            PendingIntent viewPendingIntent =
                    PendingIntent.getActivity(this, 0, viewIntent, 0);

            Intent mapIntent = new Intent(Intent.ACTION_VIEW);
            Uri geoUri = Uri.parse("geo:0,0?q=" + Uri.encode("sence"));
            mapIntent.setData(geoUri);
            PendingIntent mapPendingIntent = PendingIntent.getActivity(this, 0, mapIntent, 0);

            NotificationCompat.Action action1 =
                    new NotificationCompat.Action.Builder(R.drawable.ic_media_play,
                              getString(R.string.map), viewPendingIntent)
                              .build();


              Notification notificationBuilder =
                    new NotificationCompat.Builder(this)
                            .setSmallIcon(R.drawable.mr_ic_audio_vol)
                            .setContentTitle("Please authorize on your watch")
                            .setContentText("Right swipe click 'agree' on your watch ")
                            .setContentIntent(viewPendingIntent)
                            .addAction(R.drawable.ic_media_play, getString(R.string.map), viewPendingIntent)
                            .extend(new WearableExtender().addAction(action1))
                            .build();
              // Get an instance of the NotificationManager service
            NotificationManagerCompat notificationManager =
                    NotificationManagerCompat.from(this);

              // Build the notification and issues it with notification manager.
            notificationManager.notify(notificationId, notificationBuilder);
        }
        else if(sign==1)
        {
            r1.setText("Start");
            r1.setChecked(false);
            sign=0;
            sendTextToWear();
        }
    }
}



















