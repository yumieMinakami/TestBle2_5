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
import android.os.Build;
import android.os.Handler;
import android.os.ParcelUuid;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.neovisionaries.bluetooth.ble.advertising.ADPayloadParser;
import com.neovisionaries.bluetooth.ble.advertising.ADStructure;
import com.neovisionaries.bluetooth.ble.advertising.IBeacon;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private static final int    BT_REQUEST_ENABLE = 1;

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

    int mCounter = 0;

    ArrayList<UUID> mIBuuids = new ArrayList<>();

    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothLeScanner mBluetoothLeScanner;

    private Handler mHandler;

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

//                mUuid = iBeacon.getUUID();  // Proximity UUID
                UUID Uuid = iBeacon.getUUID();  // Proximity UUID
                mMajor = iBeacon.getMajor(); // Major number
                mMinor = iBeacon.getMinor(); // Minor number
                mPower = iBeacon.getPower(); // tx power
                mRssi = rssi;

                if(!(mIBuuids.isEmpty())){
                    mUuid = Uuid;
                    mIBuuids.add(mUuid);
                    Log.d("life", "mUUId : " + mUuid);
                } else {
                    boolean b = false;
                    for(UUID u : mIBuuids){
//                        if(u != mUuid){
                        if(u.equals(mUuid)){
//                        if(!(u.equals(mUuid))){
//                            mIBuuids.add(mUuid);
//                            Log.d("life", "mUUId : " + mUuid);
                            b = true;
                        }
                    }
                    if(!b){
                        mIBuuids.add(mUuid);
                        Log.d("life", "mUUId : " + mUuid);
                    }
                }

                Log.d("life", "IBeacon");
                Log.d("life", "uuid : " + mUuid + ", major : " + mMajor + ", minor : " + mMinor + ", power : " + mPower + ", rssi : " + mRssi);

                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if(mUuid != null){
                            mTextuid.setText(mUuid.toString());
                            mTextMajor.setText(String.valueOf(mMajor));
                            mTextMinor.setText(String.valueOf(mMinor));
                            mTextPower.setText(String.valueOf(mPower));
                            mTextRssi.setText(String.valueOf(mRssi));
                        }
                    }
                });
        }

//        if(mBluetoothLeScanner != null)
//                mBluetoothLeScanner.stopScan(mScanCallback);
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
}
