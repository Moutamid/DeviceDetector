package com.moutamid.devicedetector.activities;

import android.app.ActivityManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.fxn.stash.Stash;
import com.moutamid.devicedetector.R;
import com.moutamid.devicedetector.databinding.ActivityMainBinding;
import com.moutamid.devicedetector.models.DeviceModelB;
import com.moutamid.devicedetector.service.YourService;
import com.moutamid.devicedetector.utils.Constants;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    private ActivityMainBinding b;

    Intent mServiceIntent;
    private YourService mYourService;

    private ProgressDialog progressDialog;

    private boolean isAppStopped = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        b = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());
        Log.d(TAG, "onCreate: ");


        progressDialog = new ProgressDialog(this);
        progressDialog.setCancelable(false);
        progressDialog.setMessage("Loading devices...");
        progressDialog.show();

        initRecyclerView();

        startInitService();

        requestNewList();
    }

    private void requestNewList() {
        ArrayList<DeviceModelB> bluetoothDevices = Stash.getArrayList(Constants.BLUETOOTH_LIST, DeviceModelB.class);
        ArrayList<DeviceModelB> wifiDevices = Stash.getArrayList(Constants.WIFI_LIST, DeviceModelB.class);

        tasksArrayList.clear();
        tasksArrayList.addAll(bluetoothDevices);
        tasksArrayList.addAll(wifiDevices);

        adapter.notifyDataSetChanged();

        if (adapter.getItemCount() > 0 && progressDialog.isShowing())
            progressDialog.dismiss();

        if (!isAppStopped)
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    requestNewList();
                }
            }, 5000);

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        isAppStopped = true;
    }

    @Override
    protected void onPause() {
        super.onPause();
        isAppStopped = true;
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        isAppStopped = false;
        requestNewList();
    }

    @Override
    protected void onResume() {
        super.onResume();
        isAppStopped = false;
        requestNewList();
    }

    private void startInitService() {
        mYourService = new YourService();
        mServiceIntent = new Intent(this, mYourService.getClass());
        if (!isMyServiceRunning(mYourService.getClass())) {
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.O)
                startForegroundService(mServiceIntent);
            else startService(mServiceIntent);
        }
    }

    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                Log.i("Service status", "Running");
                return true;
            }
        }
        Log.i("Service status", "Not running");
        return false;
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
