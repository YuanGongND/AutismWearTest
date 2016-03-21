package com.example.kyle.yuanapp2;

import android.app.Activity;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.support.wearable.view.WatchViewStub;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.Wearable;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class MainActivity extends Activity implements HeartbeatService.OnChangeListener{

    Button startRec, stopRec, playBack;
    TextView mText;
    Boolean recording;
    private GoogleApiClient mGoogleApiClient;
    BroadcastReceiver mResultReceiver;
    long sum = 0;
    long hrt=0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.round_activity_main);
        startRec = (Button)findViewById(R.id.start);
        stopRec = (Button)findViewById(R.id.stop);
        playBack = (Button)findViewById(R.id.play);
        mText=(TextView)findViewById(R.id.heartrate);
        Log.v("yuan-test", "wear startes ");

        startRec.setOnClickListener(startRecOnClickListener);
        stopRec.setOnClickListener(stopRecOnClickListener);
        playBack.setOnClickListener(playBackOnClickListener);


        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(new GoogleApiClient
                        .ConnectionCallbacks() {
                    @Override
                    public void onConnected(Bundle connectionHint) {
                        Log.v("yuan-wear", "Connection established");
                    }
                    @Override
                    public void onConnectionSuspended(int cause) {
                        Log.v("yuan-wear", "Connection suspended");
                    }
                })
                .addOnConnectionFailedListener(new GoogleApiClient
                        .OnConnectionFailedListener() {
                    @Override
                    public void onConnectionFailed(ConnectionResult result) {
                        Log.v("yuan-wear", "Connection failed");
                    }
                })
                .addApi(Wearable.API)
                .build();
        mGoogleApiClient.connect();

        mResultReceiver = createBroadcastReceiver();
        LocalBroadcastManager.getInstance(this).registerReceiver(
                mResultReceiver,
                new IntentFilter("wearable.localIntent"));

        testwear2mobile(hrt);


        bindService(new Intent(MainActivity.this, HeartbeatService.class), new ServiceConnection() {
                    @Override
                    public void onServiceConnected(ComponentName componentName, IBinder binder) {
                        Log.d("yuan-wear", "connected to service.");
                        // set our change listener to get change events
                        ((HeartbeatService.HeartbeatServiceBinder)binder).setChangeListener(MainActivity.this);
                    }
                    public void onServiceDisconnected(ComponentName componentName) {}
        }, Service.BIND_AUTO_CREATE);
    }

    View.OnClickListener startRecOnClickListener
            = new View.OnClickListener(){

        @Override
        public void onClick(View arg0) {

            Thread recordThread = new Thread(new Runnable(){

                @Override
                public void run() {
                    recording = true;
                    startRecord();
                }

            });
            testwear2mobile(sum);
            Toast.makeText(getApplicationContext(), "information sent ", Toast.LENGTH_SHORT).show();
            recordThread.start();


        }};

    View.OnClickListener stopRecOnClickListener
            = new View.OnClickListener(){

        @Override
        public void onClick(View arg0) {
            recording = false;
            hrt=0;
            sum=0;
        }};

    View.OnClickListener playBackOnClickListener
            = new View.OnClickListener(){

        @Override
        public void onClick(View v) {
            playRecord();
        }

    };

    public void testwear2mobile(long xm)
    {
    //    float xm = (float) (255 * Math.random());
       // if (ym<xm)
       // {ym=xm;}
        final PutDataMapRequest putRequest =
                PutDataMapRequest.create("/WEAR2PHONE");
        final DataMap map = putRequest.getDataMap();
        map.putLong("touchX", xm);
        map.putLong("touchY", hrt);
        Wearable.DataApi.putDataItem(mGoogleApiClient,
                putRequest.asPutDataRequest());
        Log.v("yuan-wear", "information sent");
    }

    private void startRecord(){

        File file = new File(Environment.getExternalStorageDirectory(), "testyuan.pcm");

        try {
            file.createNewFile();

            OutputStream outputStream = new FileOutputStream(file);
            BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(outputStream);
            DataOutputStream dataOutputStream = new DataOutputStream(bufferedOutputStream);

            int minBufferSize = AudioRecord.getMinBufferSize(8000,
                    AudioFormat.CHANNEL_IN_DEFAULT,
                    AudioFormat.ENCODING_PCM_16BIT);

            short[] audioData = new short[minBufferSize];

            AudioRecord audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC,
                    8000,
                    AudioFormat.CHANNEL_IN_DEFAULT,
                    AudioFormat.ENCODING_PCM_16BIT,
                    minBufferSize);

            audioRecord.startRecording();

            runOnUiThread(new Runnable() {
                public void run() {
                    Toast.makeText(getApplicationContext(), "record start ", Toast.LENGTH_SHORT).show();
                }
            });

            while(recording){
                int numberOfShort = audioRecord.read(audioData, 0, minBufferSize);
                for(int i = 0; i < numberOfShort; i++){
                    dataOutputStream.writeShort(audioData[i]);
                    //sum += Math.abs(audioData[i]);
                    sum += audioData[i]*audioData[i];
                }
                sum=(long)sum/numberOfShort;
                testwear2mobile(sum);
                sum=0;
            }

            audioRecord.stop();
            audioRecord.release();
            runOnUiThread(new Runnable() {
                public void run() {
                    Toast.makeText(getApplicationContext(), "stored in /storage/emulated/0/testyuan.pcm ", Toast.LENGTH_SHORT).show();
                }
            });

            dataOutputStream.close();

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    void playRecord(){

        File file = new File(Environment.getExternalStorageDirectory(), "testyuan.pcm");

        int shortSizeInBytes = Short.SIZE/Byte.SIZE;

        int bufferSizeInBytes = (int)(file.length()/shortSizeInBytes);
        short[] audioData = new short[bufferSizeInBytes];

        try {
            InputStream inputStream = new FileInputStream(file);
            BufferedInputStream bufferedInputStream = new BufferedInputStream(inputStream);
            DataInputStream dataInputStream = new DataInputStream(bufferedInputStream);

            int i = 0;
            while(dataInputStream.available() > 0){
                audioData[i] = dataInputStream.readShort();
                i++;
            }

            dataInputStream.close();

            AudioTrack audioTrack = new AudioTrack(
                    AudioManager.STREAM_MUSIC,
                    8000,
                    AudioFormat.CHANNEL_IN_DEFAULT,
                    AudioFormat.ENCODING_PCM_16BIT,
                    bufferSizeInBytes,
                    AudioTrack.MODE_STREAM);

            audioTrack.play();
            audioTrack.write(audioData, 0, bufferSizeInBytes);


        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private BroadcastReceiver createBroadcastReceiver() {
        return new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                //setBackgroundColor(Integer.parseInt
                  //      (intent.getStringExtra("result")));
            }
        };
    }

    public void onValueChanged(int newValue) {
        // will be called by the service whenever the heartbeat value changes.
        mText.setText(Integer.toString(newValue));
        hrt=(long)newValue;
    }

}
