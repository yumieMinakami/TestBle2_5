package jp.techacademy.yumie.minakami.testble2_5;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.neovisionaries.bluetooth.ble.advertising.ADPayloadParser;
import com.neovisionaries.bluetooth.ble.advertising.ADStructure;
import com.neovisionaries.bluetooth.ble.advertising.IBeacon;

import java.util.List;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private static final int    BT_REQUEST_ENABLE = 1;
    private static final long   SCAN_PERIOD = 100;
    private static final int    PERMISSIONS_REQUEST_LOCATION_STATE = 100;

    TextView mTextMajor;
    TextView mTextMinor;
    TextView mTextPower;
    TextView mTextuid;
    TextView mTextRssi;

    UUID mUuid;
    int mMajor;
    int mMinor;
    int mPower;
    int mRssi;

    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothLeScanner mBluetoothLeScanner;
//    private LeDeviceListAdapter mLeDeviceListAdapter;
    private BluetoothGatt mBluetoothGatt;

    private Handler mHandler;

    private boolean mUpdate = false;
    private int i = 0;

    private ScanCallback mScanCallback= new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, final ScanResult result) {
            super.onScanResult(callbackType, result);

            Log.d("life", "onScanResult");

            scanData(result.getDevice(), result.getRssi(), result.getScanRecord().getBytes());

            mBluetoothGatt= result.getDevice().connectGatt(getApplication(), false, mBtGattCallback);
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results){
            super.onBatchScanResults(results);
            Log.d("life", "onBatchScanResult");
            for(ScanResult result : results){
                scanData(result.getDevice(), result.getRssi(), result.getScanRecord().getBytes());
            }
        }

        @Override
        public void onScanFailed(int errorCode){
            super.onScanFailed(errorCode);
            Log.d("life", "onScanFailed");
        }

    };      // for API 21 or later

    private final BluetoothGattCallback mBtGattCallback = new BluetoothGattCallback() {

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState)
        {
            Log.d("life", "BluetoothGattCallback --- onConnectionStateChange()");

            // 接続状況が変化したら実行.
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.d("life", "BluetoothGattCallback --- onConnectionStateChange() --- STATE_CONNECTED");
                // 接続に成功したらサービスを検索する.
                gatt.discoverServices();
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.d("life", "BluetoothGattCallback --- onConnectionStateChange() --- STATE_DISCONNECTED");
                // 接続が切れたらGATTを空にする.
                if(mBluetoothGatt != null){
                    mBluetoothGatt.close();
//                    mBluetoothGatt = null;
                }
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status)
        {
            Log.d("life", "BluetoothGattCallback --- onServicesDiscovered()");

            // サービスが見つかったら実行.
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d("life", "BluetoothGattCallback --- onServicesDiscovered() --- service found");

                BluetoothGattService service = gatt.getService(mUuid);
                if(service != null && mUuid != null){
                    mBluetoothGatt = gatt;

                    Log.d("life", "BluetoothGattCallback --- onServicesDiscovered() --- gatt");
                }

            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic)
        {
            Log.d("aaaa", "BluetoothGattCallback --- onCharacteristicChanged()");
            mUpdate = true;
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status){
//            if(mUpdate){
//                mUpdate = false;
            Log.d("life", "onCharacteristicRead");
                scanData(gatt.getDevice(), i, characteristic.getValue());
                if(i == 100000) i = 0; else i++;
//            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mHandler = new Handler();

        Log.d("life", "onCreate");

        // Use this check to determine whether BLE is supported on the device.  Then you can
        // selectively disable BLE-related features.
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            finish();
        }

        // initialize Bluetooth Adapter
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        // check Bluetooth supported
        if(mBluetoothAdapter == null){
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        if(mBluetoothAdapter != null){
            mBluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();
        }

        mTextMajor = (TextView) findViewById(R.id.major);
        mTextMinor = (TextView) findViewById(R.id.minor);
        mTextPower = (TextView) findViewById(R.id.txpower);
        mTextuid   = (TextView) findViewById(R.id.uuid);
        mTextRssi = (TextView) findViewById(R.id.rssi);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        Log.d("life", "onActivityResult");

        // User chose not to enable Bluetooth.
        if (requestCode == BT_REQUEST_ENABLE && resultCode == Activity.RESULT_CANCELED) {
            finish();
            return;
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onResume() {
        super.onResume();

        Log.d("life", "onResume");

        // check Bluetooth supported
        if(mBluetoothAdapter == null){
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        // Ensures Bluetooth is enabled on the device.
        // If Bluetooth is not currently enabled,
        // fire an intent to display a dialog asking the user to grant permission to enable it.
        if (!mBluetoothAdapter.isEnabled()) {
            Log.d("life", "R.string.bt_off @ onResume");

            Toast.makeText(this, R.string.bt_on_request, Toast.LENGTH_SHORT).show();
            Intent setBtIntnt = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(setBtIntnt, BT_REQUEST_ENABLE);
        }
        startBleScan();
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopBleScan();
        Log.d("life", "onPause");
    }

    @Override
    protected void onDestroy(){

        stopBleScan();

        super.onDestroy();
    }

    protected void scanData(BluetoothDevice bluetoothDevice, int rssi, byte[] scanRecord){

        boolean flag = false;

        // Parse the payload of the advertising packet.
        List<ADStructure> str = ADPayloadParser.getInstance().parse(scanRecord);

        for(ADStructure structure : str){
            // if the ADStructure instance can be cast to IBeacon
            if(structure instanceof IBeacon){

                IBeacon iBeacon = (IBeacon) structure;

                mUuid = iBeacon.getUUID();  // Proximity UUID
                mMajor = iBeacon.getMajor(); // Major number
                mMinor = iBeacon.getMinor(); // Minor number
                mPower = iBeacon.getPower(); // tx power
                mRssi = rssi;

                Log.d("life", "IBeacon");
                Log.d("life", "uuid : " + mUuid + ", major : " + mMajor + ", minor : " + mMinor + ", power : " + mPower + ", rssi : " + mRssi);


                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        mTextuid.setText(mUuid.toString());
                        mTextMajor.setText(String.valueOf(mMajor));
                        mTextMinor.setText(String.valueOf(mMinor));
                        mTextPower.setText(String.valueOf(mPower));
                        mTextRssi.setText(String.valueOf(mRssi));
                    }
                });
        }

        if(mBluetoothLeScanner != null)
                mBluetoothLeScanner.stopScan(mScanCallback);
        }
//        mBluetoothGatt = bluetoothDevice.connectGatt(getApplicationContext(), false, mBtGattCallback);
    }

    protected void startBleScan(){
        if(mBluetoothLeScanner != null)
            mBluetoothLeScanner.startScan(mScanCallback);

        Log.d("life", "startBleScan, lollipop <");
    }

    protected void stopBleScan(){
        if(mBluetoothLeScanner != null)
            mBluetoothLeScanner.stopScan(mScanCallback);

        Log.d("life", "stopBleScan, lollipop <");
    }
}
