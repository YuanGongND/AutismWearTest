package com.example.kyle.yuanapp2;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import com.microsoft.band.BandClient;
import com.microsoft.band.BandClientManager;
import com.microsoft.band.BandException;
import com.microsoft.band.BandInfo;
import com.microsoft.band.BandIOException;
import com.microsoft.band.BandPendingResult;
import com.microsoft.band.ConnectionState;
import com.microsoft.band.sensors.BandHeartRateEvent;
import com.microsoft.band.sensors.BandHeartRateEventListener;
import com.microsoft.band.sensors.HeartRateConsentListener;
import com.microsoft.band.sensors.BandGsrEvent;
import com.microsoft.band.sensors.BandGsrEventListener;
import com.microsoft.band.sensors.BandSensorManager;


public class BandActivity extends AppCompatActivity {

    BandGsrEventListener gsrlistener;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_band);
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

        bandconnect();
    }

    public void bandconnect()
    {
        BandInfo[] pairedBands = BandClientManager.getInstance().getPairedBands();  // get a list of paired bands
        BandClient bandClient = BandClientManager.getInstance().create(this, pairedBands[0]);
        final BandPendingResult<ConnectionState> pendingResult = bandClient.connect();

        new Thread(new Runnable(){

            @Override
            public void run() {

                try {
                    ConnectionState state = pendingResult.await();
                    if(state == ConnectionState.CONNECTED) {
                        Log.d("yuan-band","connect success");// do work on success
                    } else {
                        Log.d("yuan-band","connect failure");// do work on failure
                    }
                } catch(InterruptedException ex) {
                    Log.d("yuan-band","connect exception");
                } catch(BandException ex) {
                    // handle BandException
                }
            }
        }).start();


    }

    public void getGsr()
    {
        gsrlistener=new BandGsrEventListener() {
            @Override
            public void onBandGsrChanged(BandGsrEvent bandGsrEvent) {

            }
        };
    }


}
