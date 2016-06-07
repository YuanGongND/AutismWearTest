package com.example.kyle.yuanapp2;

import android.app.Service;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import com.microsoft.band.BandClient;
import com.microsoft.band.BandClientManager;
import com.microsoft.band.BandException;
import com.microsoft.band.BandInfo;
import com.microsoft.band.ConnectionState;
import com.microsoft.band.sensors.BandGsrEvent;
import com.microsoft.band.sensors.BandGsrEventListener;
import com.microsoft.band.sensors.BandSkinTemperatureEvent;
import com.microsoft.band.sensors.BandSkinTemperatureEventListener;



public class BodyTempService extends Service {

    private BandClient client = null;
    private BodyTempServiceBinder BodyTempBinder= new BodyTempServiceBinder();
    public float BodyTempValue;

    public BodyTempService() {
    }

    public class BodyTempServiceBinder extends Binder
    {
        public float getBodyTempValue()
        {
            return BodyTempValue;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return BodyTempBinder;
        // TODO: Return the communication channel to the service.
        //throw new UnsupportedOperationException("Not yet implemented");
    }

    private BandSkinTemperatureEventListener mBodyTempEventListener = new BandSkinTemperatureEventListener() {
        @Override
        public void onBandSkinTemperatureChanged(final BandSkinTemperatureEvent event) {
            if (event != null) {
                // appendToUI(String.format("Resistance = %d kOhms\n", event.getResistance()));
                Log.d("yuan-band-temp",""+event.getTemperature());
                BodyTempValue=event.getTemperature();
            }
        }
    };

    public void onCreate(){
        super.onCreate();
        //setContentView(R.layout.content_gsr);

        //txtStatus = (TextView) findViewById(R.id.txtStatus);
        //btnStart = (Button) findViewById(R.id.btnStart);
        //btnStart.setOnClickListener(new View.OnClickListener() {
        // @Override
        //  public void onClick(View v) {
        //      txtStatus.setText("");
        new BodyTempSubscriptionTask().execute();

    }

    @Override
    public void onDestroy() {
        Log.d("yuan-band-temp","destroyed");
        if (client != null) {
            try {
                client.disconnect().await();
            } catch (InterruptedException e) {
                // Do nothing as this is happening during destroy
            } catch (BandException e) {
                // Do nothing as this is happening during destroy
            }
        }
        super.onDestroy();
    }

    private class BodyTempSubscriptionTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... params) {
            try {
                if (getConnectedBandClient()) {
                    int hardwareVersion = Integer.parseInt(client.getHardwareVersion().await());
                    if (hardwareVersion >= 20) {
                        //  appendToUI("Band is connected.\n");
                        Log.d("yuan-band-BodyTemp","Band is connected");
                        client.getSensorManager().registerSkinTemperatureEventListener(mBodyTempEventListener);
                    } else {
                        //    appendToUI("The Gsr sensor is not supported with your Band version. Microsoft Band 2 is required.\n");
                    }
                } else {
                    // appendToUI("Band isn't connected. Please make sure bluetooth is on and the band is in range.\n");
                    Log.d("yuan-band-BodyTemp","Band is not connected");
                }
            } catch (BandException e) {
                String exceptionMessage="";
                switch (e.getErrorType()) {
                    case UNSUPPORTED_SDK_VERSION_ERROR:
                        exceptionMessage = "Microsoft Health BandService doesn't support your SDK Version. Please update to latest SDK.\n";
                        break;
                    case SERVICE_ERROR:
                        exceptionMessage = "Microsoft Health BandService is not available. Please make sure Microsoft Health is installed and that you have the correct permissions.\n";
                        break;
                    default:
                        exceptionMessage = "Unknown error occured: " + e.getMessage() + "\n";
                        break;
                }
                // appendToUI(exceptionMessage);

            } catch (Exception e) {
                // appendToUI(e.getMessage());
            }
            return null;
        }
    }


    private boolean getConnectedBandClient() throws InterruptedException, BandException {
        if (client == null) {
            BandInfo[] devices = BandClientManager.getInstance().getPairedBands();
            if (devices.length == 0) {
                // appendToUI("Band isn't paired with your phone.\n");
                Log.d("yuan-band","Band isn't paired with your phone.\n");
                return false;
            }
            client = BandClientManager.getInstance().create(getBaseContext(), devices[0]);
        } else if (ConnectionState.CONNECTED == client.getConnectionState()) {
            return true;
        }
        Log.d("yuan-band","Band is connecting...");
        // appendToUI("Band is connecting...\n");
        return ConnectionState.CONNECTED == client.connect().await();
    }
}