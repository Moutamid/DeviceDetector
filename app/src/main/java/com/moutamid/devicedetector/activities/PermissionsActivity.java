package com.moutamid.devicedetector.activities;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.Log;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.app.ActivityCompat;

import com.fxn.stash.Stash;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.PermissionListener;
import com.moutamid.devicedetector.databinding.ActivityPermissionsBinding;
import com.moutamid.devicedetector.utils.Constants;

public class PermissionsActivity extends AppCompatActivity {

    private ActivityPermissionsBinding b;

    private boolean first = false, second = false, third = false,
            fourth = false, fifth = false, sixth = false,
            seventh = false, eighth = false, ninth = false;
    BluetoothAdapter mBluetoothAdapter;
    PowerManager powerManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        b = ActivityPermissionsBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());

        if (Stash.getBoolean(Constants.PERMISSIONS_ALLOWED, false)) {
            finish();
            startActivity(new Intent(PermissionsActivity.this, MainActivity.class));
        }

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        powerManager = (PowerManager) getSystemService(POWER_SERVICE);

        b.bluetoothSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            first = isChecked;
            if (isChecked) {
                askPermission(b.bluetoothSwitch, Manifest.permission.BLUETOOTH);
            }
        });
        b.bluetoothConnectSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            second = isChecked;
            if (isChecked) {
                askPermission(b.bluetoothConnectSwitch, Manifest.permission.BLUETOOTH_CONNECT);
            }
        });
        b.bluetoothScanSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            third = isChecked;
            if (isChecked) {
                askPermission(b.bluetoothScanSwitch, Manifest.permission.BLUETOOTH_SCAN);
            }
        });
        b.bluetoothAdminSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            fourth = isChecked;
            if (isChecked) {
                askPermission(b.bluetoothAdminSwitch, Manifest.permission.BLUETOOTH_ADMIN);
            }
        });
        b.locationSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            fifth = isChecked;
            if (isChecked) {
                askPermission(b.locationSwitch, Manifest.permission.ACCESS_FINE_LOCATION);
            }
        });
        b.wifiSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            sixth = isChecked;
            if (isChecked) {
                askPermission(b.wifiSwitch, Manifest.permission.ACCESS_WIFI_STATE);
                new Handler().postDelayed(() -> {
                    WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                    if (!wifiManager.isWifiEnabled()) {
                        wifiManager.setWifiEnabled(true);
                    }
                }, 1000);
            }
        });
        b.dozeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            seventh = isChecked;
            if (isChecked) {
                try {
                    Intent intent = new Intent();
                    if (!powerManager.isIgnoringBatteryOptimizations(getPackageName())) {
//                        intent.setAction(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
                        intent.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                        intent.setData(Uri.parse("package:" + getPackageName()));
                        startActivityForResult(intent, 2);
                    } else {
                        // DOZE MODE ALREADY ACTIVE
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        b.enableBluetoothSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            eighth = isChecked;
            if (isChecked) {
                if (mBluetoothAdapter == null) {
                    // Device doesn't support Bluetooth, handle accordingly
                    return;
                }
                if (!mBluetoothAdapter.isEnabled()) {
                    Intent intentBtEnabled = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                        return;
                    }
                    startActivityForResult(intentBtEnabled, 1);
                }
            }
        });

        b.notificationsSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            ninth = isChecked;
            if (isChecked) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    askPermission(b.notificationsSwitch, Manifest.permission.POST_NOTIFICATIONS);
                }
            }
        });

        b.continueBtn.setOnClickListener(v -> {
            finish();
            startActivity(new Intent(PermissionsActivity.this, MainActivity.class));
            Stash.put(Constants.PERMISSIONS_ALLOWED, true);
        });

    }

    private void askPermission(SwitchCompat currentSwitch, String permissionString) {
        Dexter.withContext(PermissionsActivity.this)
                .withPermission(permissionString)
                .withListener(new PermissionListener() {
                    @Override
                    public void onPermissionGranted(PermissionGrantedResponse permissionGrantedResponse) {
                        currentSwitch.setChecked(true);
                        checkButtonsState();
                    }

                    @Override
                    public void onPermissionDenied(PermissionDeniedResponse permissionDeniedResponse) {
                        currentSwitch.setChecked(false);

                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(PermissionRequest permissionRequest, PermissionToken permissionToken) {
                        permissionToken.continuePermissionRequest();
                    }
                }).check();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1) {
            if (!mBluetoothAdapter.isEnabled()) {
                b.enableBluetoothSwitch.setChecked(false);
            }
        }
        if (requestCode == 2) {
            if (!powerManager.isIgnoringBatteryOptimizations(getPackageName())) {
                b.dozeSwitch.setChecked(false);
            }
        }

    }

    private void checkButtonsState() {
        if (first && second && third && fourth && fifth && sixth && seventh && eighth && ninth) {
            b.continueBtn.setVisibility(View.VISIBLE);
        }
    }

}
