package jp.techacademy.yumie.minakami.testble2_5;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.neovisionaries.bluetooth.ble.advertising.ADPayloadParser;
import com.neovisionaries.bluetooth.ble.advertising.ADStructure;
import com.neovisionaries.bluetooth.ble.advertising.IBeacon;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private static final int    BT_REQUEST_ENABLE = 1;
    private static final int    BT_UNEBLE_PERIOD = 10000;   // 10[sec]

    TextView mTextMajor;
    TextView mTextMinor;
    TextView mTextPower;
    TextView mTextuid;
    TextView mTextRssi;

//    ArrayList<BleIbeacon> mBleIbeacon = new ArrayList<>();
    BleIbeacon mBleIbeacon = new BleIbeacon();

//    UUID mUuid;
//    int mMajor;
//    int mMinor;
//    int mPower;
//    int mRssi;

    int mCounter = 0;

    ArrayList<UUID> mIBuuids = new ArrayList<>();

    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothLeScanner mBluetoothLeScanner;

    private Handler mHandler;

    private CountDownTimer mCountDowmTimer = new CountDownTimer(BT_UNEBLE_PERIOD, 500) {
        @Override
        public void onTick(long millisUntilFinished) {
            Log.d("life", "CountDown remain time [sec] = " + millisUntilFinished / 1000);
        }

        @Override
        public void onFinish() {

            final HandlerThread ht = new HandlerThread("Timer onFinish");
            ht.start();

            Handler h = new Handler(ht.getLooper());
            h.post(new Runnable() {
                @Override
                public void run() {
                    displayToast(false, mBleIbeacon.uuid);
                }
            });

            Log.d("life", "CountDown ramain time [sec] = 0!, Ble is out of range.");
            mBleIbeacon.uuid = null;
        }
    };

    private ScanCallback mScanCallback= new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, final ScanResult result) {
            super.onScanResult(callbackType, result);

            Log.d("life", "onScanResult, callbackType : " + callbackType);

//            ParcelUuid[] iBuuids = (result.getDevice()).getUuids();
//            String u = "";
//            if(iBuuids != null){
//                for(ParcelUuid pu : iBuuids){
//                    u += pu.toString() + "  ";
//                }
//            }
//            Log.d("life", "name=" + result.getDevice().getName()
//                    + ", bondStatus=" + result.getDevice().getBondState()
//                    + ", address=" + result.getDevice().getAddress()
//                    + ", type" + result.getDevice().getType()
//                    + ", uuids=" + u);

            scanData(result.getDevice(), result.getRssi(), result.getScanRecord().getBytes());
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

//        if(mBluetoothAdapter != null){
            mBluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();
//        }

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

        boolean b = false;
        final BleIbeacon bleIbeacon = new BleIbeacon();
//        ArrayList<BleIbeacon> arBleIbeacon = new ArrayList<>();
        bleIbeacon.counter = 0;
        mCounter++;
        Log.d("life", "Counter : " + mCounter);

        boolean flag = false;

        // Parse the payload of the advertising packet.
        List<ADStructure> str = ADPayloadParser.getInstance().parse(scanRecord);

        Log.d("life", "ADStructure" + str);

        for(ADStructure structure : str){

            // if the ADStructure instance can be cast to IBeacon
            if(structure instanceof IBeacon){

                IBeacon iBeacon = (IBeacon) structure;

                bleIbeacon.uuid = iBeacon.getUUID();
                bleIbeacon.major = iBeacon.getMajor();
                bleIbeacon.minor = iBeacon.getMinor();
                bleIbeacon.power = iBeacon.getPower();
                bleIbeacon.rssi = rssi;
                bleIbeacon.counter++;
                bleIbeacon.time = System.currentTimeMillis();

  //              arBleIbeacon.add(bleIbeacon);

////                mUuid = iBeacon.getUUID();  // Proximity UUID
//                UUID Uuid = iBeacon.getUUID();  // Proximity UUID
//                mMajor = iBeacon.getMajor(); // Major number
//                mMinor = iBeacon.getMinor(); // Minor number
//                mPower = iBeacon.getPower(); // tx power
//                mRssi = rssi;
//
//                if(!(mIBuuids.isEmpty())){
//                    mUuid = Uuid;
//                    mIBuuids.add(mUuid);
//                    Log.d("life", "mUUId : " + mUuid);
//                } else {
//                    boolean b = false;
//                    for(UUID u : mIBuuids){
////                        if(u != mUuid){
//                        if(u.equals(mUuid)){
////                        if(!(u.equals(mUuid))){
////                            mIBuuids.add(mUuid);
////                            Log.d("life", "mUUId : " + mUuid);
//                            b = true;
//                        }
//                    }
//                    if(!b){
//                        mIBuuids.add(mUuid);
//                        Log.d("life", "mUUId : " + mUuid);
//                    }
//                }
//
//
//                mHandler.post(new Runnable() {
//                    @Override
//                    public void run() {
//                        if(mUuid != null){
//                            mTextuid.setText(mUuid.toString());
//                            mTextMajor.setText(String.valueOf(mMajor));
//                            mTextMinor.setText(String.valueOf(mMinor));
//                            mTextPower.setText(String.valueOf(mPower));
//                            mTextRssi.setText(String.valueOf(mRssi));
//                        }
//                    }
//                });

                Log.d("life", "IBeacon  no[" + bleIbeacon.counter + "] uuid : " + bleIbeacon.uuid + ", major : " + bleIbeacon.major + ", minor : " + bleIbeacon.minor + ", power : " + bleIbeacon.power + ", rssi : " + bleIbeacon.rssi);
            }

//        if(mBluetoothLeScanner != null)
//                mBluetoothLeScanner.stopScan(mScanCallback);
        }

        if(mBleIbeacon.uuid == null){    //  When there is no items on the mBleIbeacon
            b = true;
            mBleIbeacon = bleIbeacon;
//            Collections.addAll(mBleIbeacon, bleIbeacon);    // add all items on the bleIbeacon to mBleIbeacon
//            Log.d("life", "1 mBleIbeacon uuid = " + mBleIbeacon.uuid + "bleIbeacon.uuid = " + bleIbeacon.uuid);

            mCountDowmTimer.start();
            Toast.makeText(MainActivity.this, mBleIbeacon.uuid + " is coming.", Toast.LENGTH_SHORT).show();
            Log.d("life", "1 countdown timer is started.");

        } else {    // When there are some items on the mBleIbeacon
            if(mBleIbeacon.uuid.equals(bleIbeacon.uuid)){    // iBeacon UUID is equal
                b = true;

            } else {    // iBeacon UUID is not equal

            }
//            Log.d("timer", "2 mBleIbeacon uuid = " + mBleIbeacon.uuid + "   bleIbeacon.uuid = " + bleIbeacon.uuid);
        }

//        Log.d("timer", "b = " + b);

        if(b){
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    if(bleIbeacon != null){
                        mTextuid.setText(mBleIbeacon.uuid.toString());
                        mTextMajor.setText(String.valueOf(bleIbeacon.major));
                        mTextMinor.setText(String.valueOf(bleIbeacon.minor));
                        mTextPower.setText(String.valueOf(bleIbeacon.power));
                        mTextRssi.setText(String.valueOf(bleIbeacon.rssi));
                    }
                }
            });

//            Log.d("life", "b = true, mBleIbeacon uuid = " + mBleIbeacon.uuid);

            mCountDowmTimer.cancel();
            Log.d("life", "2 count down timer is canceled.");

            mCountDowmTimer.start();
            Log.d("life", "2 countdown timer is started.");
        }
    }

    protected void startBleScan(){
        if(mBluetoothLeScanner != null)
            mBluetoothLeScanner.startScan(mScanCallback);

        Log.d("life", "startBleScan");
    }

    protected void stopBleScan(){
        if(mBluetoothLeScanner != null)
            mBluetoothLeScanner.stopScan(mScanCallback);

        Log.d("life", "stopBleScan");
    }

    static class BleIbeacon{
        UUID    uuid;
        int     major;
        int     minor;
        int     power;
        int     rssi;
        int     counter;
        long    time;
    }

    private void displayToast(boolean f, UUID uuid){
        if(f){
            Toast.makeText(MainActivity.this, uuid + "is coming.", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(MainActivity.this, uuid + "is out of range.", Toast.LENGTH_LONG).show();
        }
    }
}
