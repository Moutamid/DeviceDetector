package com.moutamid.devicedetector;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.github.douglasjunior.bluetoothclassiclibrary.BluetoothClassicService;
import com.github.douglasjunior.bluetoothclassiclibrary.BluetoothConfiguration;
import com.github.douglasjunior.bluetoothclassiclibrary.BluetoothService;
import com.karumi.dexter.BuildConfig;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;
import com.karumi.dexter.listener.single.PermissionListener;
import com.moutamid.devicedetector.databinding.ActivityMainBinding;
import com.moutamid.devicedetector.models.DeviceModelB;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    private ActivityMainBinding b;
    BluetoothAdapter mBluetoothAdapter;
    BluetoothService bluetoothManager;

    private WifiManager wifiManager;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        b = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());
        Log.d(TAG, "onCreate: ");
        adapter = new RecyclerViewAdapterMessages();
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setReverseLayout(true);
        b.bluetoothRv.setLayoutManager(linearLayoutManager);
        b.bluetoothRv.setHasFixedSize(true);
        b.bluetoothRv.setNestedScrollingEnabled(false);
        b.bluetoothRv.setAdapter(adapter);

        BluetoothConfiguration config = new BluetoothConfiguration();
        config.context = getApplicationContext();
        config.bluetoothServiceClass = BluetoothClassicService.class;
        config.bufferSize = 1024;
        config.characterDelimiter = '\n';
        config.deviceName = "Your App Name";
        config.callListenersInMainThread = true;

        config.uuid = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb"); // Required

        BluetoothService.init(config);

        bluetoothManager = BluetoothService.getDefaultInstance();

        bluetoothManager.setOnScanCallback(new BluetoothService.OnBluetoothScanCallback() {
            @Override
            public void onDeviceDiscovered(BluetoothDevice device, int rssi) {
                DeviceModelB modelB = new DeviceModelB();
                if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
                modelB.name = "BT: " + device.getName();
                modelB.address = device.getAddress();

                tasksArrayList.add(modelB);
                initRecyclerView();
            }

            @Override
            public void onStartScan() {
                Log.d(TAG, "onStartScan: ");
            }

            @Override
            public void onStopScan() {
                Log.d(TAG, "onStopScan: ");
                new Handler().postDelayed(() -> {
                    clearAndRefreshRecyclerView(); // Clear the existing list of devices
                    bluetoothManager.startScan(); // Start a new Bluetooth scan
                }, 5000);
            }
        });

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Dexter.withContext(this).withPermission(Manifest.permission.BLUETOOTH_CONNECT)
                    .withListener(new PermissionListener() {
                        @Override
                        public void onPermissionGranted(PermissionGrantedResponse response) {
                            Log.d(TAG, "onPermissionGranted: BLUETOOTH_CONNECT");
                            openBluetooth();
                        }

                        @Override
                        public void onPermissionDenied(PermissionDeniedResponse response) {
                            Toast.makeText(MainActivity.this, "You need to provide permission!", Toast.LENGTH_SHORT).show();

                            b.text.setText(b.text.getText().toString() + "\nBLUETOOTH_CONNECT");

                            Log.d(TAG, "onPermissionDenied: BLUETOOTH_CONNECT");
                            Intent intent = new Intent();
                            intent.setAction(
                                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                            Uri uri = Uri.fromParts("package",
                                    BuildConfig.APPLICATION_ID, null);
                            intent.setData(uri);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(intent);
                        }

                        @Override
                        public void onPermissionRationaleShouldBeShown(PermissionRequest permission, PermissionToken token) {
                            Log.d(TAG, "onPermissionRationaleShouldBeShown: BLUETOOTH_CONNECT");
                            token.continuePermissionRequest();

                        }
                    }).check();
        } else {
            Log.d(TAG, "onCreate: } else {");
            openBluetooth();
        }


    }

    private void configureWifiManager() {
        Log.d(TAG, "configureWifiManager: ");
        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (!wifiManager.isWifiEnabled()) {
            Log.d(TAG, "configureWifiManager: if (!wifiManager.isWifiEnabled()) {");
//            Toast.makeText(this, "wifi is disabled...Please enable it.", Toast.LENGTH_LONG).show();
            wifiManager.setWifiEnabled(true);
        }
    }

    private void openBluetooth() {
        Log.d(TAG, "openBluetooth: ");
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter(); // Initialize BluetoothAdapter
        if (mBluetoothAdapter == null) {
            // Device doesn't support Bluetooth, handle accordingly
            return;
        }
        if (!mBluetoothAdapter.isEnabled()) {
            Intent intentBtEnabled = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            // The REQUEST_ENABLE_BT constant passed to startActivityForResult() is a locally defined integer (which must be greater than 0), that the system passes back to you in your onActivityResult()
            // implementation as the requestCode parameter.
            int REQUEST_ENABLE_BT = 1;
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            startActivityForResult(intentBtEnabled, REQUEST_ENABLE_BT);
            Log.d(TAG, "openBluetooth: if (!mBluetoothAdapter.isEnabled()) {");
        } else {
            Log.d(TAG, "openBluetooth: } else {");
            enablePermissionsAndStart();
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(TAG, "onActivityResult: ");
        if (requestCode == 1) {
            Log.d(TAG, "onActivityResult: if (requestCode == 1) {");
            enablePermissionsAndStart();
        }

    }

    private boolean permissionsAllowed = false;

    private void enablePermissionsAndStart() {
        Log.d(TAG, "enablePermissionsAndStart: ");

        String[] permissions;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions = new String[]{Manifest.permission.BLUETOOTH,
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.BLUETOOTH_ADMIN,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_WIFI_STATE,
                    Manifest.permission.CHANGE_WIFI_STATE};
        } else {
            permissions = new String[]{Manifest.permission.BLUETOOTH,
                    Manifest.permission.BLUETOOTH_ADMIN,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_WIFI_STATE,
                    Manifest.permission.CHANGE_WIFI_STATE};
        }

        Dexter.withContext(MainActivity.this)
                .withPermissions(
                        permissions
                )
                .withListener(new MultiplePermissionsListener() {
                    @Override
                    public void onPermissionsChecked(MultiplePermissionsReport report) {
                        Log.d(TAG, "onPermissionsChecked: all Permissions");
                        if (report.areAllPermissionsGranted()) {
                            permissionsAllowed = true;
                            Log.d(TAG, "onPermissionsChecked: if (report.areAllPermissionsGranted()) {");

                            bluetoothManager.startScan();

                            configureWifiManager();

                            new Handler().postDelayed(() -> {
                                Log.d(TAG, "onPermissionsChecked: new Handler().postDelayed(() -> {");

                                registerReceiver(broadcastReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
                                wifiManager.startScan();

                            }, 5000);

                            // Todo: tag

                        } else if (report.isAnyPermissionPermanentlyDenied()) {
                            Log.d(TAG, "onPermissionsChecked: } else if (report.isAnyPermissionPermanentlyDenied()) {");

                            List<PermissionDeniedResponse> jh = report.getDeniedPermissionResponses();
                            for (PermissionDeniedResponse permission : jh) {
                                b.text.setText(b.text.getText().toString() + "\n" + permission.getPermissionName());
                            }

                            // open device settings when the permission is
                            // denied permanently
                            Toast.makeText(MainActivity.this, "You need to provide permission!", Toast.LENGTH_SHORT).show();

                            Intent intent = new Intent();
                            intent.setAction(
                                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                            Uri uri = Uri.fromParts("package",
                                    BuildConfig.APPLICATION_ID, null);
                            intent.setData(uri);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(intent);

                        }
                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(List<PermissionRequest> permissions, PermissionToken token) {
                        Log.d(TAG, "onPermissionRationaleShouldBeShown: ");
                        token.continuePermissionRequest();
                    }
                }).check();


        /*new Handler().postDelayed(() -> {
            if (!permissionsAllowed){
                enablePermissionsAndStart();
            }

        }, 5000);*/
    }

    BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            clearAndRefreshRecyclerView(); // Clear the existing list of devices
            for (ScanResult device : wifiManager.getScanResults()) {
                DeviceModelB modelB = new DeviceModelB();
                modelB.name = "Wifi: " + device.SSID;
                modelB.address = device.BSSID;

                tasksArrayList.add(modelB);
            }

            adapter.notifyDataSetChanged(); // Notify the adapter of the new data

            new Handler().postDelayed(() -> {
                wifiManager.startScan();
            }, 5000);
        }
    };

    private void clearAndRefreshRecyclerView() {
        tasksArrayList.clear(); // Clear the list of devices
        adapter.notifyDataSetChanged(); // Notify the adapter that the data has changed
    }
    private ArrayList<DeviceModelB> tasksArrayList = new ArrayList<>();

    private RecyclerView conversationRecyclerView;
    private RecyclerViewAdapterMessages adapter;

    private void initRecyclerView() {

        conversationRecyclerView = b.bluetoothRv;
        conversationRecyclerView.addItemDecoration(new DividerItemDecoration(conversationRecyclerView.getContext(), DividerItemDecoration.VERTICAL));
        adapter = new RecyclerViewAdapterMessages();
        //        LinearLayoutManager layoutManagerUserFriends = new LinearLayoutManager(mContext, LinearLayoutManager.HORIZONTAL, false);
        //    int numberOfColumns = 3;
        //int mNoOfColumns = calculateNoOfColumns(getApplicationContext(), 50);
        //  recyclerView.setLayoutManager(new GridLayoutManager(this, mNoOfColumns));
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setReverseLayout(true);
        conversationRecyclerView.setLayoutManager(linearLayoutManager);
        conversationRecyclerView.setHasFixedSize(true);
        conversationRecyclerView.setNestedScrollingEnabled(false);

        conversationRecyclerView.setAdapter(adapter);

        b.bluetoothRv.scrollToPosition(adapter.getItemCount() - 1);

    }

    private class RecyclerViewAdapterMessages extends RecyclerView.Adapter
            <RecyclerViewAdapterMessages.ViewHolderRightMessage> {

        @NonNull
        @Override
        public ViewHolderRightMessage onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_items, parent, false);
            return new ViewHolderRightMessage(view);
        }

        @Override
        public void onBindViewHolder(@NonNull final ViewHolderRightMessage holder, int position) {
            DeviceModelB modelB = tasksArrayList.get(position);

            if (modelB.name.equals("BT: null") || modelB.name.equals("Wifi: null") || modelB.name.isEmpty())
                modelB.name = "Unknown Device";

            String data = "Name: " + modelB.name + "\nAddress: " + modelB.address;
//            + "\nAlias: " + modelB.alias + "\nType: " + modelB.type + "\nUUID: " + modelB.uuid


            holder.title.setText(data);

        }

        @Override
        public int getItemCount() {
            if (tasksArrayList == null)
                return 0;
            return tasksArrayList.size();
        }

        public class ViewHolderRightMessage extends RecyclerView.ViewHolder {

            TextView title;

            public ViewHolderRightMessage(@NonNull View v) {
                super(v);
                title = v.findViewById(R.id.titleTextview);

            }
        }

    }

}
