package com.reconinstruments.bluetoothledemo;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.TextView;
import java.util.List;
import java.util.UUID;


@SuppressLint("NewApi")
public class BLEActivity extends Activity
{
    private final static String TAG = "BluetoothLETest";

    private BluetoothGatt mBluetoothGatt;
    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;

    private TextView leNameText;
    private TextView leAddressText;
    private TextView leStatusText;

    private String leDeviceName;
    private String leDeviceAddress;

    public static UUID UART_UUID = UUID.fromString("6E400001-B5A3-F393-E0A9-E50E24DCCA9E");
    public static UUID TX_UUID   = UUID.fromString("6E400002-B5A3-F393-E0A9-E50E24DCCA9E");
    public static UUID RX_UUID   = UUID.fromString("6E400003-B5A3-F393-E0A9-E50E24DCCA9E");

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.device_main);
    }

    @Override
    public void onResume()
    {
        super.onResume();
        Log.d(TAG, "onResume()");

        Intent intent = getIntent();
        leDeviceName = intent.getStringExtra("name");
        leDeviceAddress = intent.getStringExtra("address");

        leNameText = (TextView) findViewById(R.id.leNameText);
        leAddressText = (TextView) findViewById(R.id.leAddressText);

        leStatusText = (TextView) findViewById(R.id.leStatusText);
        leStatusText.setText("- Connecting -");

        leNameText.setText(leDeviceName);
        leAddressText.setText(leDeviceAddress);

        // Attempt BLE Connection
        mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        if (mBluetoothManager != null)
        {
            mBluetoothAdapter = mBluetoothManager.getAdapter();
            if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled())
            {
                Log.e(TAG, "BT Adapter is null or not enabled!");
            }
        }
        else { Log.e(TAG, "Unable to retrieve BluetoothManager"); }
        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(leDeviceAddress);
        mBluetoothGatt = device.connectGatt(this, false, mGattCallback);
    }

    @Override
    public void onPause()
    {
        Log.d(TAG, "onPause()");
        if (mBluetoothGatt != null)
        {
            mBluetoothGatt.close();
            mBluetoothGatt = null;
        }
        super.onPause();
    }

    // Various callback methods defined by the BLE API.
    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback()
    {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState)
        {
            if (newState == BluetoothProfile.STATE_CONNECTED)
            {
                Log.i(TAG, "Connected to GATT server.");
                Log.i(TAG, "Attempting to start service discovery:" + mBluetoothGatt.discoverServices());
                broadcastMessage("- Connected -");

            }
            else if (newState == BluetoothProfile.STATE_DISCONNECTED)
            {
                Log.i(TAG, "Disconnected from GATT server.");
                broadcastMessage("- Disconnected -");
            }
        }

        @Override
        // New services discovered
        public void onServicesDiscovered(BluetoothGatt gatt, int status)
        {
            if (status == BluetoothGatt.GATT_SUCCESS)
            {
                Log.i(TAG, "Services discovered.");
                List<BluetoothGattService> services = gatt.getServices();
                //String gatt_services = gatt.getServices().toString();
                Log.i(TAG, services.get(4).getCharacteristics().get(0).getUuid().toString());
                Log.i(TAG, gatt.getService(UART_UUID).getCharacteristic(RX_UUID).toString());
                gatt.setCharacteristicNotification(gatt.getService(UART_UUID).getCharacteristic(RX_UUID), true);
                broadcastMessage("- Services Discovered -");
            }
            else { Log.w(TAG, "onServicesDiscovered received: " + status); }
        }
        @Override
        // Result of a characteristic read operation
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status)
        {
            if (status == BluetoothGatt.GATT_SUCCESS)
            {
                Log.i(TAG, "onCharacteristicRead is available.");
                broadcastMessage("- onCharacteristicRead Available -");
            }
            else {
                Log.i(TAG, "onCharacteristicRead is not available..");
            }
        }
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic)
        {
                Log.i(TAG, "onCharacteristicChanged is available.");
                byte[] bytes = characteristic.getValue();
                String packet = new String(bytes);
                String[] packet_split = packet.split("//");
                broadcastMessage(packet_split[1]+"  "+packet_split[2]);
        }
    };

    private void broadcastMessage(final String msg)
    {
        Message strMsg = new Message();
        strMsg.obj = msg;
        messageHandler.sendMessage(strMsg);
    }

    private Handler messageHandler = new Handler()
    {
        public void handleMessage(Message msg)
        {
            super.handleMessage(msg);
            leStatusText.setText((String) msg.obj);
        }
    };
}
