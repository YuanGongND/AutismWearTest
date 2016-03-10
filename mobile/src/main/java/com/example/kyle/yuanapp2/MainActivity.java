package com.example.kyle.yuanapp2;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.app.NotificationCompat.WearableExtender;
import android.widget.RadioButton;

public class MainActivity extends AppCompatActivity {

    public static int sign=0;
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
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

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
        }
    }
}
