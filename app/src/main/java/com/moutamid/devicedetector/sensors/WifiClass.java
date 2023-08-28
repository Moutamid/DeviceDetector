package com.moutamid.devicedetector.sensors;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Handler;

import androidx.core.app.ActivityCompat;

import com.fxn.stash.Stash;
import com.moutamid.devicedetector.NotificationHelper;
import com.moutamid.devicedetector.activities.MainActivity;
import com.moutamid.devicedetector.models.DeviceModelB;
import com.moutamid.devicedetector.utils.Constants;

import java.util.ArrayList;

public class WifiClass {
    Context context;
    WifiManager wifiManager;

    public WifiClass(Context context) {
        this.context = context;

        wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (!wifiManager.isWifiEnabled()) {
            wifiManager.setWifiEnabled(true);
        }

        context.registerReceiver(broadcastReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));

    }

    BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }

            ArrayList<DeviceModelB> wifiDevices = new ArrayList<>();
            Stash.clear(Constants.WIFI_LIST);

            for (ScanResult device : wifiManager.getScanResults()) {
                DeviceModelB modelB = new DeviceModelB();
                modelB.name = "Wifi: " + device.SSID;
                modelB.address = device.BSSID;

                wifiDevices.add(modelB);

                int value;
                try {
                    value = Integer.parseInt(modelB.address.replaceAll("[^0-9]", "").trim());
                    NotificationHelper notificationHelper = new NotificationHelper(context);
                    notificationHelper.sendHighPriorityNotification(
                            modelB.name + " Found",
                            modelB.address,
                            value,
                            MainActivity.class
                    );
                } catch (Exception w) {
                    try {
                        String str = modelB.address.trim();
                        String digits = "";
                        for (int i = 0; i < str.length(); i++) {
                            char chrs = str.charAt(i);
                            if (Character.isDigit(chrs))
                                digits = digits + chrs;
                        }
                        value = Integer.parseInt(digits);
                        NotificationHelper notificationHelper = new NotificationHelper(context);
                        notificationHelper.sendHighPriorityNotification(
                                modelB.name + " Found",
                                modelB.address,
                                value,
                                MainActivity.class
                        );
                    } catch (Exception w2) {
                        w2.printStackTrace();
                    }
                }

            }
            Stash.put(Constants.WIFI_LIST, wifiDevices);

            new Handler().postDelayed(() -> {
                wifiManager.startScan();
            }, 5000);
        }
    };

    public void startScanning() {
        wifiManager.startScan();
    }
}
