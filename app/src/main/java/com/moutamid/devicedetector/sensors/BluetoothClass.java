package com.moutamid.devicedetector.sensors;

import android.Manifest;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Handler;

import androidx.core.app.ActivityCompat;

import com.fxn.stash.Stash;
import com.github.douglasjunior.bluetoothclassiclibrary.BluetoothClassicService;
import com.github.douglasjunior.bluetoothclassiclibrary.BluetoothConfiguration;
import com.github.douglasjunior.bluetoothclassiclibrary.BluetoothService;
import com.moutamid.devicedetector.NotificationHelper;
import com.moutamid.devicedetector.activities.MainActivity;
import com.moutamid.devicedetector.models.DeviceModelB;
import com.moutamid.devicedetector.utils.Constants;

import java.util.ArrayList;
import java.util.UUID;

public class BluetoothClass {

    BluetoothService bluetoothManager;
    Context context;

    public BluetoothClass(Context context) {
        this.context = context;

        BluetoothConfiguration config = new BluetoothConfiguration();
        config.context = context.getApplicationContext();
        config.bluetoothServiceClass = BluetoothClassicService.class;
        config.bufferSize = 1024;
        config.characterDelimiter = '\n';
        config.deviceName = "Your App Name";
        config.callListenersInMainThread = true;

        config.uuid = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb"); // Required

        BluetoothService.init(config);

        bluetoothManager = BluetoothService.getDefaultInstance();

        ArrayList<DeviceModelB> bluetoothDevices = new ArrayList<>();

        bluetoothManager.setOnScanCallback(new BluetoothService.OnBluetoothScanCallback() {
            @Override
            public void onDeviceDiscovered(BluetoothDevice device, int rssi) {
                DeviceModelB modelB = new DeviceModelB();
                if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
                modelB.name = "BT: " + device.getName();
                modelB.address = device.getAddress();

                bluetoothDevices.add(modelB);

                int value;
                try {
                    value = Integer.parseInt(modelB.address.replaceAll("[^0-9]", "").trim());
                } catch (Exception w) {
                    String str = modelB.address.trim();
                    String digits = "";
                    for (int i = 0; i < str.length(); i++) {
                        char chrs = str.charAt(i);
                        if (Character.isDigit(chrs))
                            digits = digits + chrs;
                    }
                    value = Integer.parseInt(digits);
                }
                NotificationHelper notificationHelper = new NotificationHelper(context);
                notificationHelper.sendHighPriorityNotification(
                        modelB.name + " Found",
                        modelB.address,
                        value,
                        MainActivity.class
                );
            }

            @Override
            public void onStartScan() {
                bluetoothDevices.clear();
                Stash.clear(Constants.BLUETOOTH_LIST);
            }

            @Override
            public void onStopScan() {
                Stash.put(Constants.BLUETOOTH_LIST, bluetoothDevices);
                new Handler().postDelayed(() -> {
                    bluetoothManager.startScan(); // Start a new Bluetooth scan
                }, 5000);
            }
        });
    }

    public void startScanning() {
        if (bluetoothManager != null)
            bluetoothManager.startScan(); // Start a new Bluetooth scan
    }

}
