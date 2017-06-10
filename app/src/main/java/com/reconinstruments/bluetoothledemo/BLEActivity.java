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
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.graphics.Rect;
import android.os.Bundle;
import android.view.View;
import com.reconinstruments.ui.carousel.CarouselActivity;
import com.reconinstruments.ui.carousel.CarouselItem;
import com.reconinstruments.ui.breadcrumb.BreadcrumbToast;
import com.reconinstruments.ui.carousel.CarouselPagerViewAdapter;
//import com.reconinstruments.bluetoothledemo.R;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;


@SuppressLint("NewApi")
public class BLEActivity extends CarouselActivity
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
    private String[] fullPacket;
    private int k = 100; //količnik množinski

    public static Map<String, Double> Variables = new HashMap<String, Double>();

    public static UUID UART_UUID = UUID.fromString("6E400001-B5A3-F393-E0A9-E50E24DCCA9E");
    public static UUID TX_UUID   = UUID.fromString("6E400002-B5A3-F393-E0A9-E50E24DCCA9E");
    public static UUID RX_UUID   = UUID.fromString("6E400003-B5A3-F393-E0A9-E50E24DCCA9E");

    static class ListItem extends CarouselItem {
        String value;
        String unit;
        String type;
        TextView typeText;
        public ListItem(String value,String unit,String type){
            this.value = value;
            this.unit = unit;
            this.type = type;
        }
        @Override
        public int getLayoutId() {
                return R.layout.carousel_item_stat;
        }
        @Override
        public void updateView(View view) {

            TextView valueText = (TextView) view.findViewById(R.id.value);
            TextView unitText = (TextView) view.findViewById(R.id.unit);
            Double value_text = Variables.containsKey(value) ? Variables.get(value) : 0;
            valueText.setText(Double.toString(value_text));
            unitText.setText(unit);
            typeText = (TextView) view.findViewById(R.id.type);
            typeText.setText(type);
        }
        @Override
        public void updateViewForPosition(View view,POSITION position) {
            if(position==POSITION.CENTER) {
                typeText.setVisibility(View.VISIBLE);
            }else{
                typeText.setVisibility(View.GONE);
            }
        }
    }
    static class ListItem2 extends CarouselItem {


        public ListItem2(){
            //empty constructor
        }
        @Override
        public int getLayoutId() {
                return R.layout.carousel_item_graphic_stat;
        }
        @Override
        public void updateView(View view) {
            SeekBar os_x = (SeekBar) view.findViewById(R.id.os_x);
            SeekBar os_y = (SeekBar) view.findViewById(R.id.os_y);
            Double value_os_z = Variables.containsKey("A_X") ? Variables.get("A_X")*100 : 0;
            Double value_os_y = Variables.containsKey("A_Y") ? Variables.get("A_Y")*100 : 0;
            os_x.setProgress(value_os_z.intValue()+200);
            os_y.setProgress(value_os_y.intValue()+200);
        }
        @Override
        public void updateViewForPosition(View view,POSITION position) {

        }
    }
    static class ListItem_Posture extends CarouselItem {


        public ListItem_Posture(){
            //empty constructor
        }
        @Override
        public int getLayoutId() {
            return R.layout.carousel_item_images_posture;
        }
        @Override
        public void updateView(View view) {
            // os z naprej nazaj
            //os y side to side
            ImageView straight = (ImageView) view.findViewById(R.id.straight);
            ImageView side = (ImageView) view.findViewById(R.id.side);
            Double value_os_z = Variables.containsKey("A_Z") ? Variables.get("A_Z") : 0;
            Double value_os_y = Variables.containsKey("A_Y") ? Variables.get("A_Y") : 0;

            Log.i(TAG, "A_Z:"+value_os_z);
            Log.i(TAG, "A_Y:"+value_os_y);

            if (value_os_z >= 0.3){
                straight.setImageResource(R.mipmap.drza_side__3);
            } else if (value_os_z >= 0.2 && value_os_z < 0.3){
                straight.setImageResource(R.mipmap.drza_side__3);
            } else if (value_os_z >= 0.1 && value_os_z < 0.2){
                straight.setImageResource(R.mipmap.drza_side__2);
            } else if (value_os_z >= 0.05 && value_os_z < 0.1){
                straight.setImageResource(R.mipmap.drza_side__1);
            } else if (value_os_z >= -0.05 && value_os_z < 0.05){
                straight.setImageResource(R.mipmap.drza_side_0);
            } else if (value_os_z >= -0.1 && value_os_z < -0.05){
                straight.setImageResource(R.mipmap.drza_side_1);
            } else if (value_os_z >= -0.3 && value_os_z < -0.1){
                straight.setImageResource(R.mipmap.drza_side_2);
            } else if (value_os_z >= -0.4 && value_os_z < -0.3){
                straight.setImageResource(R.mipmap.drza_side_3);
            } else if (value_os_z >= -0.9 && value_os_z < -0.4) {
                straight.setImageResource(R.mipmap.drza_side_4);
            }

            if (value_os_y >= 0.4){
                side.setImageResource(R.mipmap.drza_straight_4);
            } else if (value_os_y >= 0.3 && value_os_y < 0.4){
                side.setImageResource(R.mipmap.drza_straight_3);
            } else if (value_os_y >= 0.2 && value_os_y < 0.3){
                side.setImageResource(R.mipmap.drza_straight_2);
            } else if (value_os_y >= 0.1 && value_os_y < 0.2){
                side.setImageResource(R.mipmap.drza_straight_1);
            } else if (value_os_y >= -0.1 && value_os_y < 0.1){
                side.setImageResource(R.mipmap.drza_straight_0);
            } else if (value_os_y >= -0.2 && value_os_y < -0.1){
                side.setImageResource(R.mipmap.drza_straight__1);
            } else if (value_os_y >= -0.3 && value_os_y < -0.2){
                side.setImageResource(R.mipmap.drza_straight__2);
            } else if (value_os_y >= -0.4 && value_os_y < -0.3){
                side.setImageResource(R.mipmap.drza_straight__3);
            } else if (value_os_y >= -0.6 && value_os_y < -0.4){
                side.setImageResource(R.mipmap.drza_straight__4);
            }

        }
        @Override
        public void updateViewForPosition(View view,POSITION position) {

        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        //super.onCreate(savedInstanceState);
        //setContentView(R.layout.device_main);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.carousel_host_stat);

        TextView title = (TextView)findViewById(R.id.title);
        title.setText("BLE Connect App");
        Variables.put("A_X", 0.1);
        Variables.put("A_Y", 0.1);
        Variables.put("A_Z", 0.1);

        getCarousel().setContents(
                new ListItem("A_X", "G", "Os X"),
                new ListItem("A_Y", "G", "Os Y"),
                new ListItem("A_Z", "G", "Os Z"),
                new ListItem_Posture()
                );
    }

    @Override
    public void onResume()
    {
        super.onResume();
        Log.d(TAG, "onResume()");

        Intent intent = getIntent();
        leDeviceName = intent.getStringExtra("name");
        leDeviceAddress = intent.getStringExtra("address");

        //leNameText = (TextView) findViewById(R.id.leNameText);
        //leAddressText = (TextView) findViewById(R.id.leAddressText);

        //leStatusText = (TextView) findViewById(R.id.leStatusText);
        //leStatusText.setText("- Connecting -");

        //leNameText.setText(leDeviceName);
        //leAddressText.setText(leDeviceAddress);

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
                //String[] fullPacket = new String[5];
                Log.i(TAG, "Packet Send:"+packet);
                if (packet.contains("A/") && packet.contains("/Z")){
                    fullPacket = packet.split("/");
                    Log.i(TAG, "Packet Splited");
                    if (fullPacket[0].equals("A")==false){
                        runOnUiThread(new Runnable(){
                            public void run(){
                                TextView title = (TextView)findViewById(R.id.title);
                                title.setText("ERORR reset BLE board");
                            }
                        });
                        Log.i(TAG, "ERROR SET");
                    }
                }
                //Log.i(TAG, "PacketToBeAssembled:"+fullPacket);
                //fullPacket = fullPacket+packet_split2[1];
                //Log.i(TAG, fullPacket);
                Variables.clear();
                Variables.put("A_X", Double.parseDouble(fullPacket[1].replace(" ",""))/k);
                Variables.put("A_Y", Double.parseDouble(fullPacket[2].replace(" ",""))/k);
                Variables.put("A_Z", Double.parseDouble(fullPacket[3].replace(" ",""))/k);
                runOnUiThread(new Runnable(){
                  public void run(){
                     getCarousel().getCurrentCarouselItem().updateView(getCarousel().getCurrentView());
                     }
                });
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
            //leStatusText.setText((String) msg.obj);
        }
    };
}
