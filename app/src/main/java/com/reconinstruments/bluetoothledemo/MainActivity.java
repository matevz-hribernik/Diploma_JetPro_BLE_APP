package com.reconinstruments.bluetoothledemo;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

import com.reconinstruments.os.HUDOS;
import com.reconinstruments.os.hardware.extsensor.ExternalSensorConnectionParams;
import com.reconinstruments.os.hardware.extsensor.HUDExternalSensorManager;

import java.util.ArrayList;
import java.util.HashMap;

@SuppressLint("NewApi")
public class MainActivity extends Activity
{
    private static final String TAG = "BluetoothLETest";
    private Context mContext = null;
    private BluetoothAdapter mBluetoothAdapter;
    private Handler mHandler = new Handler();

    private ArrayList<BluetoothDevice> mDeviceList = new ArrayList<BluetoothDevice>();

    // Stops scanning after 10 seconds
    private static final long SCAN_PERIOD = 10000;

    private Button buttonScan;

    private HUDExternalSensorManager mExtSensorManager;
    private NotificationManager mNotificationManager;

    private LinearLayout listLinearLayout;
    private HashMap<Button, BluetoothDevice> buttonDeviceHash = new HashMap<Button, BluetoothDevice>();

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        mContext = this;
        setContentView(R.layout.activity_main);

        mNotificationManager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        mExtSensorManager = (HUDExternalSensorManager) HUDOS.getHUDService(HUDOS.HUD_EXTERNAL_SENSOR_SERVICE);

        if(mExtSensorManager.getHUDNetworkType() == ExternalSensorConnectionParams.ExternalSensorNetworkType.ANT)
        {
            Log.e(TAG, "JET is in ANT+ MODE! Needs to be changed to BLE (in settings menu)");
            createBLENotification();
        }
    }

    @Override
    public void onResume()
    {
        super.onResume();

        buttonScan  = (Button) findViewById(R.id.button_scan);
        buttonScan.setOnClickListener(mOnClickListener);

        listLinearLayout = (LinearLayout) findViewById(R.id.listLinearLayout);

        if(mExtSensorManager.getHUDNetworkType() == ExternalSensorConnectionParams.ExternalSensorNetworkType.ANT)
        {
            Log.e(TAG, "JET is in ANT+ MODE! Needs to be changed to BLE (in settings menu)");
            finish();
            return;
        }

        final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        if(bluetoothManager == null)
        {
            Log.e(TAG, "Unable to retrieve BluetoothManager");
            return;
        }

        mBluetoothAdapter = bluetoothManager.getAdapter();
        if(mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled())
        {
            Log.e(TAG, "BLE is NULL or DISABLED");
            return;
        }

        Log.e(TAG, "BLE is ENABLED");
    }

    @Override
    public void onPause()
    {
        Log.d(TAG, "onPause()");
        if(mBluetoothAdapter != null){ mBluetoothAdapter.stopLeScan(mLeScanCallback); }
        super.onPause();
    }

    View.OnClickListener mOnClickListener = new View.OnClickListener()
    {
        @Override
        public void onClick(View view)
        {
            if(view == buttonScan)
            {
                scanLeDevice(true);
            }
            else
            {
                BluetoothDevice device = buttonDeviceHash.get((Button)view);
                Log.d(TAG, "CLICKED --> " + device.getAddress());

                Intent intent = new Intent(mContext, BLEActivity.class);
                intent.putExtra("name", device.getName());
                intent.putExtra("address", device.getAddress());
                mContext.startActivity(intent);
            }
        }
    };

    private void scanLeDevice(final boolean isStart)
    {
        if(isStart)
        {
            boolean scanStarted = mBluetoothAdapter.startLeScan(mLeScanCallback);
            Log.d(TAG, "startLeScan(), scanStarted: " + scanStarted);

            if(!scanStarted)
            {
                Log.d(TAG, "Scan FAILED to Start.");
                return;
            }

            buttonScan.setText(R.string.button_scan_stop);

            // Stops scanning after a pre-defined scan period.
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
                    buttonScan.setText(R.string.button_scan);
                    Log.d(TAG, "stopLeScan()");
                }
            }, SCAN_PERIOD);
            return;
        }

        mBluetoothAdapter.stopLeScan(mLeScanCallback);
        buttonScan.setText(R.string.button_scan);
    }

    // Device scan callback
    private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback()
    {
        @Override
        public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord)
        {
            if(!mDeviceList.contains(device) && device.getName() != null)
            {
                mDeviceList.add(device);
                Message strMsg = new Message();
                strMsg.obj = device;
                messageHandler.sendMessage(strMsg);
            }
        }
    };

    private Handler messageHandler = new Handler()
    {
        public void handleMessage(Message msg)
        {
            super.handleMessage(msg);

            BluetoothDevice device = (BluetoothDevice) msg.obj;

            Button button = new Button(MainActivity.this);
            button.setText(device.getName() + ":" + device.getAddress());
            button.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
            button.setTextColor(Color.BLACK);
            button.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 24);
            button.setOnClickListener(mOnClickListener);

            listLinearLayout.addView(button);
            buttonDeviceHash.put(button, device);
        }
    };

    public void createBLENotification()
    {
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, new Intent("com.reconinstruments.jetsensorconnect"), PendingIntent.FLAG_UPDATE_CURRENT);
        Notification notification = new Notification.Builder(getApplicationContext())
                .setContentTitle("BLE NOT ENABLED")
                .setSmallIcon(R.drawable.icon_warning)
                .setContentText("You must ENABLE BLE mode.")
                .setContentIntent(pendingIntent)
                .build();
        mNotificationManager.notify(0, notification);
    }
}