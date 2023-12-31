package com.moutamid.devicedetector.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

public class Restarter extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Log.i("onReceive: ", "        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {");
            context.startForegroundService(new Intent(context, YourService.class));
        } else {
            Log.i("onReceive: ", "} else {");
            context.startService(new Intent(context, YourService.class));
        }
    }
}
