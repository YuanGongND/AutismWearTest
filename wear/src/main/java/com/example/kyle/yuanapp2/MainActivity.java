package com.example.kyle.yuanapp2;

import android.app.Activity;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
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

public class MainActivity extends Activity implements HeartbeatService.OnChangeListener,SensorEventListener {

    Button startRec, stopRec, playBack;
    TextView mText;
    Boolean recording;
    private GoogleApiClient mGoogleApiClient;
    BroadcastReceiver mResultReceiver;
    private SensorManager senSensorManager;
    private Sensor senAccelerometer;
    private long lastUpdate;
    private float last_x, last_y, last_z;
    private static final int SHAKE_THRESHOLD = 1000;
    long sum = 0;
    long hrt=0;
    float speed=0;
    ServiceConnection hrtt;
    Intent hrtintent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.round_activity_main);
        startRec = (Button)findViewById(R.id.start);
        stopRec = (Button)findViewById(R.id.stop);
        playBack = (Button)findViewById(R.id.play);
        mText=(TextView)findViewById(R.id.heartrate);
        Log.v("yuan-wear", "wear started");

        senSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        senAccelerometer = senSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        senSensorManager.registerListener(this, senAccelerometer , SensorManager.SENSOR_DELAY_NORMAL);
        Log.v("yuan-wear", "acelerometer started");

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

    //    Intent hrtintent = new Intent(this, HeartbeatService.class);
    //    startService(hrtintent);
        hrtt=new ServiceConnection()
        {
            @Override
            public void onServiceConnected(ComponentName componentName, IBinder binder) {
                Log.d("yuan-wear", "connected to service.");
                // set our change listener to get change events
                ((HeartbeatService.HeartbeatServiceBinder)binder).setChangeListener(MainActivity.this);
            }
            public void onServiceDisconnected(ComponentName componentName) {}
        };
        hrtintent=new Intent(MainActivity.this, HeartbeatService.class);
        //bindService(hrtintent,hrtt, Service.BIND_AUTO_CREATE);
       // this.unbindService(hrtt);
    }

    View.OnClickListener startRecOnClickListener
            = new View.OnClickListener(){

        @Override
        public void onClick(View arg0) {
            startRec.setClickable(false);
            stopRec.setClickable(true);
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
            startRec.setClickable(true);
            stopRec.setClickable(false);
            recording = false;
            hrt=0;
            sum=0;
            stophrt();
            Log.d("yuan-wear", "plan to stop heart rate service.");
           //MainActivity.this.unbindService(hrtt);

            //startService(hrtintent);
            //stopService(HeartbeatService.class);
        }};

    View.OnClickListener playBackOnClickListener
            = new View.OnClickListener(){

        @Override
        public void onClick(View v) {
            playRecord();
        }

    };

    public void stophrt()
    {
        this.unbindService(hrtt);
        this.stopService(hrtintent);
        Log.d("yuan-wear", "stop heart rate service.");
    }

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
        map.putFloat("speed", speed);
        map.putFloat("acx",last_x);
        map.putFloat("acy",last_y);
        map.putFloat("acz",last_z);
        Wearable.DataApi.putDataItem(mGoogleApiClient,
                putRequest.asPutDataRequest());
        Log.v("yuan-wear", "information sent");
    }

    private void startRecord(){

        bindService(hrtintent,hrtt, Service.BIND_AUTO_CREATE);
        Log.v("yuan-wear", "service rebinded");
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

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {

        Sensor mySensor = sensorEvent.sensor;

        if (mySensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            float x = sensorEvent.values[0];
            float y = sensorEvent.values[1];
            float z = sensorEvent.values[2];

            long curTime = System.currentTimeMillis();
            // only allow one update every 100ms.
            if ((curTime - lastUpdate) > 100) {
                long diffTime = (curTime - lastUpdate);
                lastUpdate = curTime;

                speed = Math.abs(x + y + z - last_x - last_y - last_z) / diffTime * 10000;

                if (speed > SHAKE_THRESHOLD) {
                    Toast.makeText(this, "shake detected w/ speed: " + speed, Toast.LENGTH_SHORT).show();
                }
                last_x = x;
                last_y = y;
                last_z = z;
            }

        }
    }

    @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            // can be safely ignored for this demo
        }
}
