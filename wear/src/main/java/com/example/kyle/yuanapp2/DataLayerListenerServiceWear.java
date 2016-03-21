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

public class DataLayerListenerServiceWear extends WearableListenerService {
    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        super.onDataChanged(dataEvents);

        Log.v("yuan-wear", "service started");

        final List<DataEvent> events = FreezableUtils
                .freezeIterable(dataEvents);
        for(DataEvent event : events) {
            final Uri uri = event.getDataItem().getUri();
            final String path = uri!=null ? uri.getPath() : null;
            if("/PHONE2WEAR".equals(path)) {
                final DataMap map = DataMapItem
                        .fromDataItem(event.getDataItem()).getDataMap();
                // read your values from map:
                int color = map.getInt("color");
                Log.v("yuan-test", "Color received: " + color);

                Intent localIntent = new Intent("wearable.localIntent");
                localIntent.putExtra("result", color);
                LocalBroadcastManager.getInstance(this)
                        .sendBroadcast(localIntent);

                String colorChanges = map.getString("colorChanges");
                Log.v("yuantest-wear", colorChanges);
            }
        }
    }
}
