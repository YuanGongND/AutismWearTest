package com.example.kyle.yuanapp2;

import android.app.Service;
import android.content.Intent;
import android.net.Uri;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.android.gms.common.data.FreezableUtils;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.WearableListenerService;

import java.util.List;

public class DataLayerListenerServicePhone extends WearableListenerService {

    public void onDataChanged(DataEventBuffer dataEvents) {
        super.onDataChanged(dataEvents);

        Log.v("yuan-mobile", "mobile service started");

        final List<DataEvent> events = FreezableUtils.freezeIterable(dataEvents);
        for(DataEvent event : events) {
            final Uri uri = event.getDataItem().getUri();
            final String path = uri!=null ? uri.getPath() : null;
            if("/WEAR2PHONE".equals(path)) {
                final DataMap map = DataMapItem
                        .fromDataItem(event.getDataItem()).getDataMap();
                // read your values from map:
                Long X = map.getLong("touchX");
                Long Y = map.getLong("touchY");
                float speed=map.getFloat("speed");
    //            String reply = "Voice Activity=" +X+", Heart Rate=  " + Y+",Speed="+speed;
                String reply="Voice Activity=" +X;
                String hrt="Heart Rate="+Y;
                String spd="Speed="+speed;
                Log.v("yuan-mobile", reply);
                Intent localIntent = new Intent("phone.localIntent");
                localIntent.putExtra("vad", reply);
                localIntent.putExtra("hrt", hrt);
                localIntent.putExtra("spd", spd);
                localIntent.putExtra("vadvalue",X);
                localIntent.putExtra("hrtvalue",Y);
                localIntent.putExtra("speedvalue",speed);

                LocalBroadcastManager.getInstance(this)
                        .sendBroadcast(localIntent);
            }
        }
    }
}
