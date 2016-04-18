package com.example.kyle.yuanapp2;

import android.app.Activity;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.speech.RecognizerIntent;
import android.support.v4.content.LocalBroadcastManager;
import android.support.wearable.view.WatchViewStub;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

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
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.Result;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.CapabilityApi;
import com.google.android.gms.wearable.CapabilityInfo;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Set;
//import java.util.logging.Handler;
import android.os.Handler;

import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Logger;
import com.example.kyle.yuanapp2.VibrateUtil;



public class MainActivity extends Activity implements HeartbeatService.OnChangeListener,SensorEventListener { //must implement listener to service

    Button startRec,MSG,stopRec, playBack,upLoad;
    TextView mText,level;
    Boolean recording;
    private GoogleApiClient mGoogleApiClient;
    BroadcastReceiver mResultReceiver;
    private SensorManager senSensorManager;
    private Sensor senAccelerometer;
    private long lastUpdate,glastUpdate;
    private float last_x, last_y, last_z,last_gx, last_gy, last_gz;
    private static final int SHAKE_THRESHOLD = 1000;
    int vadsum = 0,hrt=0;
    long sum=0,sumhrt,sumvad;
    float speed=0,gspeed=0;
    ServiceConnection hrtt;
    Intent hrtintent;
    int minBufferSize;
    byte RECODERRDER_BPP=8;
    String AUDIO_RECORDER_FOLDER = "YuanAudioRecorderTest";
    String AUDIO_RECORDER_TEMP_FILE = "record_temp_yuan.pcm";
    String AUDIO_RECORDER_FILE_EXT_WAV = ".wav";
    String sfilepath;
    int samplerate=44100;
    int samplebit=16;
    String VOICE_TRANSCRIPTION_CAPABILITY_NAME = "voice_transcription", transcriptionNodeId = null;
    String DESCRIBE_PATH = "/voice_describe", RATE1_PATH = "/voice_rate1",RATE2_PATH = "/voice_rate2";


    AmazonS3 s3;
    CognitoCachingCredentialsProvider credentialsProvider;
    TransferUtility transferUtility;
    TextView uppercentage;
    int percentage;

    int monitorsample=720,imonitor=0;
    int[] monitorarrayvad =new int[monitorsample];
    int[] monitorarrayhrt=new int[monitorsample];
    int SPEECH_REQUEST_CODE = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.round_activity_main);
        startRec = (Button)findViewById(R.id.start);
        MSG=(Button)findViewById(R.id.MSG);
        stopRec = (Button)findViewById(R.id.stop);
        playBack = (Button)findViewById(R.id.play);
        upLoad=(Button)findViewById(R.id.upload);
        mText=(TextView)findViewById(R.id.heartrate);
        uppercentage=(TextView)findViewById(R.id.percentage);
        level=(TextView)findViewById(R.id.level);
        Log.v("yuan-wear", "wear started");

        senSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        senAccelerometer = senSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        senSensorManager.registerListener(this, senAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        Log.v("yuan-wear", "acelerometer started");

        startRec.setOnClickListener(startRecOnClickListener);
        MSG.setOnClickListener(MSGClickListener);
        stopRec.setOnClickListener(stopRecOnClickListener);
        playBack.setOnClickListener(playBackOnClickListener);
        upLoad.setOnClickListener(uploadRecOnClickListener);



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
        hrtt=new ServiceConnection()   //prepare heart rate service, not started yet
        {
            @Override
            public void onServiceConnected(ComponentName componentName, IBinder binder) {
                Log.d("yuan-wear", "connected to service.");
                // set our change listener to get change events
                ((HeartbeatService.HeartbeatServiceBinder)binder).setChangeListener(MainActivity.this);
            }
            public void onServiceDisconnected(ComponentName componentName) {}
        };
        hrtintent=new Intent(MainActivity.this, HeartbeatService.class); //build intent, still not start, start in the onrecord
        //bindService(hrtintent,hrtt, Service.BIND_AUTO_CREATE);
       // this.unbindService(hrtt);

        credentialsProvider = new CognitoCachingCredentialsProvider(
                getApplicationContext(),
                "us-east-1:ae22f8ae-d55e-4c00-877c-04629a3bdb25", // Identity Pool ID
                Regions.US_EAST_1 // Region
        );

        s3= new AmazonS3Client(credentialsProvider); // Set the region of your S3 bucket
        s3.setRegion(Region.getRegion(Regions.US_WEST_2));
    }


    // inner listener class,start recording button
    View.OnClickListener startRecOnClickListener
            = new View.OnClickListener(){
        @Override
        public void onClick(View arg0) {
            startRec.setEnabled(false);
            stopRec.setEnabled(true);
            Thread recordThread = new Thread(new Runnable(){
                @Override
                public void run() {
                    recording = true;
                    startRecord();  // inside,also start heart rate service by binding service
                }
            });
            testwear2mobile(vadsum);
            Toast.makeText(getApplicationContext(), "information sent ", Toast.LENGTH_SHORT).show();
            recordThread.start();
        }};

    View.OnClickListener MSGClickListener
            = new View.OnClickListener(){
        @Override
        public void onClick(View arg0) {

            SendMessageToHand();

        }};

    //inner class button, start
    View.OnClickListener stopRecOnClickListener
            = new View.OnClickListener(){
        @Override
        public void onClick(View arg0) {
            startRec.setEnabled(true);
            stopRec.setEnabled(false);
            recording = false;
            hrt=0;
            sum=0;
            stophrt();  // stop heart rate service

            //String displayfilepath = Environment.getExternalStorageDirectory().getPath();
            //File displayfile = new File(displayfilepath,AUDIO_RECORDER_FOLDER);
            //uppercentage.setText(displayfile.length() + " wait upload");

            Log.d("yuan-wear", "plan to stop heart rate service.");
        }};

    View.OnClickListener uploadRecOnClickListener
            = new View.OnClickListener() {
        public void onClick(View arg0) {
            String filepath = Environment.getExternalStorageDirectory().getPath();
            File file = new File(filepath,AUDIO_RECORDER_FOLDER);
            uploadFolderFile(file);
        }
    };

    //inner class button, play ,currently useless code
    View.OnClickListener playBackOnClickListener
            = new View.OnClickListener(){
        @Override
        public void onClick(View v) {
            playRecord();
        }
    };


    // stop heart rate service
    public void stophrt()
    {
        this.unbindService(hrtt);
        this.stopService(hrtintent);
        Log.d("yuan-wear", "stop heart rate service.");
    }

    // send related information to handheld device
    public void testwear2mobile(int vad)
    {
        int ic;
        final PutDataMapRequest putRequest =
                PutDataMapRequest.create("/WEAR2PHONE");
        final DataMap map = putRequest.getDataMap();
        map.putInt("touchX", vad);
        map.putInt("touchY", hrt);
        map.putFloat("speed", speed);
        map.putFloat("acx", last_x);
        map.putFloat("acy", last_y);
        map.putFloat("acz", last_z);
        map.putFloat("gspeed",gspeed);
        map.putFloat("gyx",last_gx);
        map.putFloat("gyy", last_gy);
        map.putFloat("gyz", last_gz);
        Wearable.DataApi.putDataItem(mGoogleApiClient,
                putRequest.asPutDataRequest());
        Log.v("yuan-wear", "information sent");

        if(imonitor<monitorsample) {
            monitorarrayvad[imonitor] = vad;
            monitorarrayhrt[imonitor] = hrt;
            imonitor = imonitor + 1;
        }
        if(imonitor==monitorsample)
        {
            imonitor=0;
            for (ic=0;ic<monitorsample;ic++)
            {
                sumhrt=sumhrt+monitorarrayhrt[ic];
                sumvad=sumvad+monitorarrayvad[ic];
            }
            if(sumhrt>36000&&sumvad>1440000)
            {
                //recording=false;
                SendMessageToHand();
            }
            Log.v("yuan-wear","vad sum="+sumvad+"hrt sum="+sumhrt);
            sumhrt=0;sumvad=0;
        }
    }

    private void SendMessageToHand()
    {
        Log.v("yuan-wear", "msg prepared");

        new Timer().schedule(
                new TimerTask() {
                    @Override
                    public void run() {
                        byte[] tempmsg = new byte[2];
                        tempmsg[0] = 2;
                        tempmsg[1] = 9;
                        setupVoiceTranscription();
                        requestTranscription(tempmsg, DESCRIBE_PATH);
                        VibrateUtil.Vibrate(MainActivity.this, 1000);
                        new Timer().schedule(
                                new TimerTask() {
                                    @Override
                                    public void run() {
                                        recording = false;
                                        byte[] tempmsg = new byte[2];
                                        tempmsg[0] = 2;
                                        tempmsg[1] = 9;
                                        setupVoiceTranscription();
                                        requestTranscription(tempmsg, RATE1_PATH);
                                        VibrateUtil.Vibrate(MainActivity.this, 1000);
                                        new Timer().schedule(
                                                new TimerTask() {
                                                    @Override
                                                    public void run() {
                                                        byte[] tempmsg = new byte[2];
                                                        tempmsg[0] = 2;
                                                        tempmsg[1] = 9;
                                                        setupVoiceTranscription();
                                                        requestTranscription(tempmsg, RATE2_PATH);
                                                        VibrateUtil.Vibrate(MainActivity.this, 1000);
                                                        new Timer().schedule(
                                                                new TimerTask() {
                                                                    @Override
                                                                    public void run() {
                                                                        displaySpeechRecognizer();
                                                                    }
                                                                },
                                                                3000
                                                        );
                                                    }
                                                },
                                                15000
                                        );
                                    }
                                },
                                15000
                        );
                    }
                },
                1000
        );
        /*
        new Handler().postDelayed(new Runnable() {
            public void run() {
                recording = true;
                startRecord();
                byte[] tempmsg = new byte[2];
                tempmsg[0]=2;
                tempmsg[1]=9;
                setupVoiceTranscription();
                requestTranscription(tempmsg, DESCRIBE_PATH);
                VibrateUtil.Vibrate(MainActivity.this,1000);
            }
        }, 10000);

        Runnable runnable1 = new Runnable() {
            @Override
            public void run() {
                recording = true;
                startRecord();
                byte[] tempmsg = new byte[2];
                tempmsg[0]=2;
                tempmsg[1]=9;
                setupVoiceTranscription();
                requestTranscription(tempmsg, DESCRIBE_PATH);
                VibrateUtil.Vibrate(MainActivity.this,1000);
            }
        };
        Handler handler1 = new Handler();
        handler1.postDelayed(runnable1, 10000);

        Thread th1=new Thread(){
            public void run() {
                recording = true;
                startRecord();
                byte[] tempmsg = new byte[2];
                tempmsg[0]=2;
                tempmsg[1]=9;
                setupVoiceTranscription();
                requestTranscription(tempmsg, DESCRIBE_PATH);
                VibrateUtil.Vibrate(MainActivity.this,1000);
            }
        };
       // th1.start();
        Log.v("yuan-wear", "time1");

        new Timer().schedule(
                new TimerTask() {
                    @Override
                    public void run() {
                        recording = true;
                        startRecord();
                        byte[] tempmsg = new byte[2];
                        tempmsg[0] = 2;
                        tempmsg[1] = 9;
                        setupVoiceTranscription();
                        requestTranscription(tempmsg, DESCRIBE_PATH);
                        VibrateUtil.Vibrate(MainActivity.this, 1000);
                    }
                },
                10000
        );

        Log.v("yuan-wear", "time2");
        new Timer().schedule(
                new TimerTask() {
                    @Override
                    public void run() {
                        byte[] tempmsg = new byte[2];
                        tempmsg[0] = 2;
                        tempmsg[1] = 9;
                        setupVoiceTranscription();
                        requestTranscription(tempmsg, RATE1_PATH);
                        recording = Boolean.FALSE;
                    }
                },
                30000
        );
        Log.v("yuan-wear", "time3");
       */
    }

    private void setupVoiceTranscription() {
        CapabilityApi.GetCapabilityResult result =
                Wearable.CapabilityApi.getCapability(
                        mGoogleApiClient, VOICE_TRANSCRIPTION_CAPABILITY_NAME,
                        CapabilityApi.FILTER_REACHABLE).await();
                Log.v("yuan-wear", "msg" + result);
                        //.setResultCallback(new ResultCallback<CapabilityApi.GetCapabilityResult>()
               // {
               //     public String onResult(CapabilityApi.GetCapabilityResult sendMessageResult) {
               //         Log.v("yuan-mobile", "Sent message");
               //     }
               // });
        updateTranscriptionCapability(result.getCapability());

        CapabilityApi.CapabilityListener capabilityListener =
                new CapabilityApi.CapabilityListener() {
                    @Override
                    public void onCapabilityChanged(CapabilityInfo capabilityInfo) {
                        updateTranscriptionCapability(capabilityInfo);
                    }
                };

        Wearable.CapabilityApi.addCapabilityListener(
                mGoogleApiClient,
                capabilityListener,
                VOICE_TRANSCRIPTION_CAPABILITY_NAME);
    }

    private void updateTranscriptionCapability(CapabilityInfo capabilityInfo) {
        Set<Node> connectedNodes = capabilityInfo.getNodes();

        transcriptionNodeId = pickBestNodeId(connectedNodes);
        Log.v("yuan-wear", "msg" + transcriptionNodeId);
    }

    private String pickBestNodeId(Set<Node> nodes) {
        String bestNodeId = null;
        // Find a nearby node or pick one arbitrarily
        for (Node node : nodes) {
            if (node.isNearby()) {
                return node.getId();
            }
            bestNodeId = node.getId();
        }
        Log.v("yuan-wear", "msg" + bestNodeId);
        return bestNodeId;
    }

    private void requestTranscription(byte[] voiceData,String Path) {
        if (transcriptionNodeId != null) {
            Log.v("yuan-wear", "msg is sending");
            Wearable.MessageApi.sendMessage(mGoogleApiClient, transcriptionNodeId,
                    Path, voiceData).setResultCallback(
                    new ResultCallback<MessageApi.SendMessageResult>() {
                        public void onResult(MessageApi.SendMessageResult sendMessageResult) {
                            if (!sendMessageResult.getStatus().isSuccess()) {
                                // Failed to send message
                                Log.v("yuan-wear", "msg failed to sent");
                            }
                        }
                    }
            );
        } else {
            // Unable to retrieve node with transcription capability
        }
    }



    private byte[] shortToByteArray(short s) {
        byte[] shortBuf = new byte[2];
        for(int i=0;i<2;i++) {
            int offset = (shortBuf.length - 1 -i)*8;
            shortBuf[i] = (byte)((s>>>offset)&0xff);
        }
        return shortBuf;
    }
    //start recording,inside realize the heart rate

    private void startRecord() {
        bindService(hrtintent, hrtt, Service.BIND_AUTO_CREATE);
        Log.v("yuan-wear", "service rebinded");
        String tfilename = getTempFilename();
        File file = new File(tfilename);

        try {
            file.createNewFile();
            OutputStream outputStream = new FileOutputStream(file);
            BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(outputStream);
            DataOutputStream dataOutputStream = new DataOutputStream(bufferedOutputStream);

            minBufferSize = AudioRecord.getMinBufferSize(samplerate,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT);
           //         AudioFormat.ENCODING_PCM_16BIT);

            short [] audioData = new short[minBufferSize];

            AudioRecord audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC,
                    samplerate,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    minBufferSize);

            audioRecord.startRecording();

            runOnUiThread(new Runnable() {
                public void run() {
                    Toast.makeText(getApplicationContext(), "record start ", Toast.LENGTH_SHORT).show();
                }
            });

            while (recording){
                int numberOfShort = audioRecord.read(audioData, 0, minBufferSize);
                for(int i = 0; i < numberOfShort; i++){
                    byte [] tempbyte=shortToByteArray(audioData[i]);
                    dataOutputStream.writeByte(tempbyte[1]);
                    dataOutputStream.writeByte(tempbyte[0]);
                    //sum += Math.abs(audioData[i]);
                    sum += audioData[i]*audioData[i];
                }
                sum=(long)sum/numberOfShort;
                vadsum=(int)sum;
                testwear2mobile(vadsum);
                sum=0;vadsum=0;
            }

            audioRecord.stop();
            audioRecord.release();
            long lengthbefore=file.length();
            Log.i("yuan-wear", "the length before converting to wav=" + lengthbefore);
            copyWaveFile(getTempFilename(), getFilename());
            deleteTempFile();
            SendRecording();
            transamazon(sfilepath);



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

    //play record function, currently useless
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
                    samplerate,
                    AudioFormat.CHANNEL_IN_MONO,
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
        hrt=newValue;
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

        if (mySensor.getType() == Sensor.TYPE_GYROSCOPE) {
            float gx = sensorEvent.values[0];
            float gy = sensorEvent.values[1];
            float gz = sensorEvent.values[2];

            long gcurTime = System.currentTimeMillis();
            // only allow one update every 100ms.
            if ((gcurTime - glastUpdate) > 100) {
                long gdiffTime = (gcurTime - glastUpdate);
               glastUpdate = gcurTime;

                gspeed = Math.abs(gx + gy + gz - last_gx - last_gy - last_gz) / gdiffTime * 10000;

                if (speed > SHAKE_THRESHOLD) {
                    Toast.makeText(this, "rotate detected w/ speed: " + gspeed, Toast.LENGTH_SHORT).show();
                }
                last_gx = gx;
                last_gy = gy;
                last_gz = gz;
            }

        }
    }
    @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            // can be safely ignored for this demo
        }

    private String getTempFilename(){
        String filepath = Environment.getExternalStorageDirectory().getPath();
        File file = new File(filepath,AUDIO_RECORDER_FOLDER);

        if(!file.exists()){
            file.mkdirs();
        }

        File tempFile = new File(filepath,AUDIO_RECORDER_TEMP_FILE);

        if(tempFile.exists())
            tempFile.delete();

        return (file.getAbsolutePath() + "/" + AUDIO_RECORDER_TEMP_FILE);
    }

    private String getFilename(){
        String filepath = Environment.getExternalStorageDirectory().getPath();
        File file = new File(filepath,AUDIO_RECORDER_FOLDER);

        if(!file.exists()){
            file.mkdirs();
        }
     //   Toast.makeText(getApplicationContext(), file.getAbsolutePath() + "/" + System.currentTimeMillis() + AUDIO_RECORDER_FILE_EXT_WAV, Toast.LENGTH_SHORT).show();
        Log.v("yuan-wear", file.getAbsolutePath() + "/" + System.currentTimeMillis() + AUDIO_RECORDER_FILE_EXT_WAV);
        sfilepath=file.getAbsolutePath() + "/" + System.currentTimeMillis() + AUDIO_RECORDER_FILE_EXT_WAV;
        return (file.getAbsolutePath() + "/" +System.currentTimeMillis() + AUDIO_RECORDER_FILE_EXT_WAV);
    }

    private void deleteTempFile() {
        File file = new File(getTempFilename());

        file.delete();
    }

    private void copyWaveFile(String inFilename,String outFilename) {
        FileInputStream in = null;
        FileOutputStream out = null;
        long totalAudioLen = 0;
        long totalDataLen = totalAudioLen + 36;
        long longSampleRate = samplerate;
        int channels = 1;
        long byteRate = samplebit * samplerate * channels / 8;

        byte[] data = new byte[minBufferSize];

        try {
            File infile = new File(inFilename);
            in = new FileInputStream(infile);
            long length2 = infile.length();
            Log.i("yuan-wear", "the length2 before converting to wav=" + length2);
            File outfile = new File(outFilename);
            out = new FileOutputStream(outfile);
            long length3 = infile.length();
            Log.i("yuan-wear", "the length3 before converting to wav=" + length3);
            totalAudioLen = in.getChannel().size();
            totalDataLen = totalAudioLen + 36;

            //   AppLog.logString("File size: " + totalDataLen);

            WriteWaveFileHeader(out, totalAudioLen, totalDataLen,
                    longSampleRate, channels, byteRate);

            while (in.read(data) != -1) {
                out.write(data);
            }

            in.close();
            out.close();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    void SendRecording()throws FileNotFoundException, IOException
    {
        File tfile=new File(sfilepath);
        FileInputStream tfis = new FileInputStream(tfile);
        long length4=tfile.length();
        Log.i("yuan-wear", "the length4 before converting to wav="+length4);
     //   Asset asset=Asset.createFromUri(Uri.fromFile(tfile));
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buf = new byte[1024];
        try {
            for (int readNum; (readNum = tfis.read(buf)) != -1;) {
                bos.write(buf, 0, readNum); //no doubt here is 0
                //Writes len bytes from the specified byte array starting at offset off to this byte array output stream.
                System.out.println("read " + readNum + " bytes,");
                Log.i("yuan-wear","read " + readNum + " bytes,");
            }
        } catch (IOException ex) {
        }
        Asset asset=Asset.createFromBytes(bos.toByteArray());


        PutDataMapRequest dataMap = PutDataMapRequest.create("/rcd");
        dataMap.getDataMap().putAsset("profilercd", asset);
        PutDataRequest request = dataMap.asPutDataRequest();
        PendingResult<DataApi.DataItemResult> pendingResult = Wearable.DataApi
                .putDataItem(mGoogleApiClient, request);
        //PutDataRequest request = PutDataRequest.create("/rcd");
        //Wearable.DataApi.putDataItem(mGoogleApiClient, request);
        //request.putAsset("profilercd", asset);
        Log.i("yuan-wear", "After send recording");
    }

    private void WriteWaveFileHeader(
            FileOutputStream out, long totalAudioLen,
            long totalDataLen, long longSampleRate, int channels,
            long byteRate) throws IOException {

        byte[] header = new byte[44];
        Log.v("yuan-wear", "write head");
        header[0] = 'R'; // RIFF/WAVE header
        header[1] = 'I';
        header[2] = 'F';
        header[3] = 'F';
        header[4] = (byte) (totalDataLen & 0xff);
        header[5] = (byte) ((totalDataLen >> 8) & 0xff);
        header[6] = (byte) ((totalDataLen >> 16) & 0xff);
        header[7] = (byte) ((totalDataLen >> 24) & 0xff);
        header[8] = 'W';
        header[9] = 'A';
        header[10] = 'V';
        header[11] = 'E';
        header[12] = 'f'; // 'fmt ' chunk
        header[13] = 'm';
        header[14] = 't';
        header[15] = ' ';
        header[16] = 16; // 4 bytes: size of 'fmt ' chunk
        header[17] = 0;
        header[18] = 0;
        header[19] = 0;
        header[20] = 1; // format = 1
        header[21] = 0;
        header[22] = (byte) channels;
        header[23] = 0;
        header[24] = (byte) (longSampleRate & 0xff);
        header[25] = (byte) ((longSampleRate >> 8) & 0xff);
        header[26] = (byte) ((longSampleRate >> 16) & 0xff);
        header[27] = (byte) ((longSampleRate >> 24) & 0xff);
        header[28] = (byte) (byteRate & 0xff);
        header[29] = (byte) ((byteRate >> 8) & 0xff);
        header[30] = (byte) ((byteRate >> 16) & 0xff);
        header[31] = (byte) ((byteRate >> 24) & 0xff);
        header[32] = (byte) (channels * 16 / 8); // block align
        header[33] = 0;
        header[34] = 16; // bits per sample
        header[35] = 0;
        header[36] = 'd';
        header[37] = 'a';
        header[38] = 't';
        header[39] = 'a';
        header[40] = (byte) (totalAudioLen & 0xff);
        header[41] = (byte) ((totalAudioLen >> 8) & 0xff);
        header[42] = (byte) ((totalAudioLen >> 16) & 0xff);
        header[43] = (byte) ((totalAudioLen >> 24) & 0xff);

        out.write(header, 0, 44);
    }

    public void transamazon(String f)
    {
        File upfile=new File(f);

        SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        java.util.Date date=new java.util.Date();
        String timetemp=sdf.format(date);
        String rcdname=timetemp+".wav";

        transferUtility = new TransferUtility(s3, getApplicationContext());
        TransferObserver observer = transferUtility.upload(
                "yuanautism3",     /* The bucket to upload to */
                rcdname,    /* The key for the uploaded object */
                upfile        /* The file where the data to upload exists */
        );
        observer.setTransferListener(new TransferListener() {

            @Override
            public void onStateChanged(int id, TransferState state) {
                // do something
            }

            @Override
            public void onProgressChanged(int id, long bytesCurrent, long bytesTotal) {
                percentage = (int) (bytesCurrent / (bytesTotal) * 100);
                uppercentage.setText(percentage + "% uploaded");
            }

            @Override
            public void onError(int id, Exception ex) {
                // do something
            }

        });
    }

    public void uploadFolderFile(File file) {
        if (true) {
            try {
                if (file.isDirectory()) {// 处理目录
                    File files[] = file.listFiles();
                    for (int i = 0; i < files.length; i++) {
                        transamazon(files[i].getAbsolutePath());
                        File tempde=new File(files[i].getAbsolutePath());
                 //       tempde.delete();
                    }
                }
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    private void displaySpeechRecognizer() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
// Start the activity, the intent will be populated with the speech text
        startActivityForResult(intent, SPEECH_REQUEST_CODE);
    }

    // This callback is invoked when the Speech Recognizer returns.
// This is where you process the intent and extract the speech text from the intent.
    @Override
    protected void onActivityResult(int requestCode, int resultCode,
                                    Intent data) {
        if (requestCode == SPEECH_REQUEST_CODE && resultCode == RESULT_OK) {
            List<String> results = data.getStringArrayListExtra(
                    RecognizerIntent.EXTRA_RESULTS);
            final String spokenText = results.get(0);
            runOnUiThread(new Runnable() {
                public void run() {
                    level.setText("Current Anxious Level="+spokenText);
                }
            });

        }
        super.onActivityResult(requestCode, resultCode, data);
    }
}

