package com.example.kyle.yuanapp2;

import android.app.Service;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.TextView;

import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferObserver;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.data.FreezableUtils;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.concurrent.TimeUnit;



public class DataLayerListenerServicePhone extends WearableListenerService {
    private GoogleApiClient mGoogleApiClient;
    int TIMEOUT_MS=1000;
    TextView uppercentage;
    AmazonS3 s3;
    CognitoCachingCredentialsProvider credentialsProvider;
    TransferUtility transferUtility;
    public void onCreate() {

        super.onCreate();
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .build();
        mGoogleApiClient.connect();
        credentialsProvider = new CognitoCachingCredentialsProvider(
                getApplicationContext(),
                "us-east-1:ae22f8ae-d55e-4c00-877c-04629a3bdb25", // Identity Pool ID
                Regions.US_EAST_1 // Region
        );

        s3= new AmazonS3Client(credentialsProvider); // Set the region of your S3 bucket
        s3.setRegion(Region.getRegion(Regions.US_WEST_2));

    }

    public void onDataChanged(DataEventBuffer dataEvents)  {
        super.onDataChanged(dataEvents);

        Log.v("yuan-mobile", "mobile service started");

        final List<DataEvent> events = FreezableUtils.freezeIterable(dataEvents);
        for(DataEvent event : events) {
            final Uri uri = event.getDataItem().getUri();
            final String path = uri!=null ? uri.getPath() : null;


            if (event.getType() == DataEvent.TYPE_CHANGED &&
                    event.getDataItem().getUri().getPath().equals("/rcd")) {
                Log.v("yuan-mobile", "Received rcd.");
                DataMapItem dataMapItem = DataMapItem.fromDataItem(event.getDataItem());
                Asset profileAsset = dataMapItem.getDataMap().getAsset("profilercd");
                loadBitmapFromAsset(profileAsset);

                // Do something with the bitmap
            }

            if("/WEAR2PHONE".equals(path)) {
                final DataMap map = DataMapItem
                        .fromDataItem(event.getDataItem()).getDataMap();
                // read values from datalayer:
                int X = map.getInt("touchX");
                int Y = map.getInt("touchY");
                float speed=map.getFloat("speed");
                float a_x=map.getFloat("acx");
                float a_y=map.getFloat("acy");
                float a_z=map.getFloat("acz");
                float gspeed=map.getFloat("gspeed");
                float g_x=map.getFloat("gyx");
                float g_y=map.getFloat("gyy");
                float g_z=map.getFloat("gyz");
                // convert to String (easier to use for demonstration)
                String reply="Voice Activity=" +X;
                String hrt="Heart Rate="+Y;
                String spd="A-Speed="+speed;
                String ax="a_x="+a_x;
                String ay="a_y="+a_y;
                String az="a_z="+a_z;
                String gspd="G-Speed="+gspeed;
                String gx="g_x="+g_x;
                String gy="g_y="+g_y;
                String gz="g_z="+g_z;
                //send to mainactivity
                Intent localIntent = new Intent("phone.localIntent");
                localIntent.putExtra("vad", reply);//string
                localIntent.putExtra("hrt", hrt);
                localIntent.putExtra("spd", spd);
                localIntent.putExtra("ax",ax);
                localIntent.putExtra("ay",ay);
                localIntent.putExtra("az",az);
                localIntent.putExtra("gspd",gspd);
                localIntent.putExtra("gx",gx);
                localIntent.putExtra("gy",gy);
                localIntent.putExtra("gz",gz);
                localIntent.putExtra("vadvalue",X);//value
                localIntent.putExtra("hrtvalue",Y);
                localIntent.putExtra("speedvalue",speed);
                localIntent.putExtra("axvalue",a_x);
                localIntent.putExtra("ayvalue",a_y);
                localIntent.putExtra("azvalue",a_z);
                localIntent.putExtra("gxvalue",g_x);
                localIntent.putExtra("gyvalue",g_y);
                localIntent.putExtra("gzvalue",g_z);
                LocalBroadcastManager.getInstance(this)
                        .sendBroadcast(localIntent);
            }

        }
    }


    private void transamazons(String f,String name)
    {
        File upfile=new File(f);
        transferUtility = new TransferUtility(s3, getApplicationContext());
        TransferObserver observer = transferUtility.upload(
                "yuanautism3",     /* The bucket to upload to */
                name,    /* The key for the uploaded object */
                upfile        /* The file where the data to upload exists */
        );
        observer.setTransferListener(new TransferListener(){

            @Override
            public void onStateChanged(int id, TransferState state) {
                // do something
            }

            @Override
            public void onProgressChanged(int id, long bytesCurrent, long bytesTotal) {
                //int percentage = (int) (bytesCurrent/bytesTotal * 100);
                //uppercentage.setText(percentage+"%");
            }

            @Override
            public void onError(int id, Exception ex) {
                // do something
            }

        });
    }

    public void loadBitmapFromAsset(Asset asset) {
        if (asset == null) {
            throw new IllegalArgumentException("Asset must be non-null");
        }
        ConnectionResult result =
                mGoogleApiClient.blockingConnect(TIMEOUT_MS, TimeUnit.MILLISECONDS);
        if (!result.isSuccess()) {
           // return null;
        }
        // convert asset into a file descriptor and block until it's ready
        InputStream assetInputStream = Wearable.DataApi.getFdForAsset(
                mGoogleApiClient, asset).await().getInputStream();
        mGoogleApiClient.disconnect();

        if (assetInputStream == null) {
            Log.v("yuan-wear", "Requested an unknown Asset.");
            //return null;
        }
        // decode the stream into a bitmap
        try {
            byte[] buffer = new byte[assetInputStream.available()];
            assetInputStream.read(buffer);

            String outpathtemp = "/mnt/sdcard/yuan/testrcd.wav";
            File testrcd = new File(outpathtemp);
            OutputStream outStream = new FileOutputStream(testrcd);
            outStream.write(buffer);
            Log.v("yuan-mobile", "written.");
            SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            java.util.Date date=new java.util.Date();
            String timetemp=sdf.format(date);
            String rcdname=timetemp+"-fromtablet.wav";
            transamazons("/mnt/sdcard/yuan/testrcd.wav",rcdname);

//        return BitmapFactory.decodeStream(assetInputStream);
        }catch (IOException e){}
    }
}
