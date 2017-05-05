package com.polito.cesarldm.bitadroidbeta_2.activities;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.PopupMenu;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import com.polito.cesarldm.bitadroidbeta_2.R;
import com.polito.cesarldm.bitadroidbeta_2.adapters.ScannedBTHDevicesListAdapter;

import info.plux.pluxapi.BTHDeviceScan;
import info.plux.pluxapi.Constants;

import static android.Manifest.permission.ACCESS_COARSE_LOCATION;

public class BluetoothActivity extends AppCompatActivity implements View.OnClickListener, AdapterView.OnItemClickListener {
    //Views
    Button btnScan, btnStop;
    ListView deviceList;
    ProgressDialog prgScanning;
    //Bluetooth related
    private BluetoothAdapter bthAdapter;
    private Handler mHandler;
    private BTHDeviceScan bthDeviceScan;
    //Adapters
    ScannedBTHDevicesListAdapter mDevicesListAdapter;
    //Boolean
    private boolean mScanning;
    private boolean isRegistered = false;

    private static final long SCAN_PERIOD = 10000;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth);
        btnScan = (Button) findViewById(R.id.btn_AB_Scan);
        btnStop = (Button) findViewById(R.id.btn_AB_Stop_Scan);
        prgScanning = new ProgressDialog(BluetoothActivity.this);
        prgScanning.setMessage("Scanning...");
        final BluetoothManager bthManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        //bthAdapter=BluetoothAdapter.getDefaultAdapter();
        mHandler = new Handler();
        bthAdapter = bthManager.getAdapter();//preguntar por los minsdk¡¡¡
        checkBlueToothState();
        permissionCheck();
        bthDeviceScan = new BTHDeviceScan(this);
        btnScan.setOnClickListener(this);
        btnStop.setOnClickListener(this);


    }
    /** Permission check explicitly required from user at run time*/

    private void permissionCheck() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            //Android Marshmallow and above permission check
            if (this.checkSelfPermission(ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(getString(R.string.permission_check_dialog_title))
                        .setMessage(getString(R.string.permission_check_dialog_message))
                        .setPositiveButton(getString(R.string.permission_check_dialog_positive_button), null)
                        .setOnDismissListener(new DialogInterface.OnDismissListener() {
                            @TargetApi(Build.VERSION_CODES.M)
                            @Override
                            public void onDismiss(DialogInterface dialogInterface) {
                                requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 2);
                            }
                        });
                builder.show();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case 2:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d("MainMenuActivity", "coarse location permission granted");
                } else {
                    final AlertDialog.Builder builder = new AlertDialog.Builder(this)
                            .setTitle(getString(R.string.permission_denied_dialog_title))
                            .setMessage(getString(R.string.permission_denied_dialog_message))
                            .setPositiveButton(getString(R.string.permission_denied_dialog_positive_button), null)
                            .setOnDismissListener(new DialogInterface.OnDismissListener() {
                                @Override
                                public void onDismiss(DialogInterface dialogInterface) {

                                }
                            });
                    builder.show();
                }
                break;
            default:
                return;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mBroadcastReceiver, new IntentFilter(Constants.ACTION_MESSAGE_SCAN));
        isRegistered = true;
        if (!bthAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, 1);
        }
        mDevicesListAdapter = new ScannedBTHDevicesListAdapter(this);
        deviceList = (ListView) findViewById(R.id.lv_AB_FoundDevices);
        deviceList.setAdapter(mDevicesListAdapter);
        deviceList.setOnItemClickListener(this);
        startScanning(true);
    }

    @Override
    protected void onPause() {
        super.onPause();
        startScanning(false);
        mDevicesListAdapter.clear();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (bthDeviceScan != null) {
            bthDeviceScan.closeScanReceiver();
        }

        if (isRegistered) {
            unregisterReceiver(mBroadcastReceiver);
        }
    }


    @Override
    public void onClick(View v) {
        if (v == btnScan) {
            startScanning(true);
        }
        if (v == btnStop) {
            startScanning(false);
            mDevicesListAdapter.clear();
            mDevicesListAdapter.notifyDataSetChanged();
        }

    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        final BluetoothDevice device = mDevicesListAdapter.getItem(position);
        if (device == null) return;
        toastMessageLong(device.getName() + ", MAC= " + device.getAddress());
        Intent returnIntent = new Intent();
        returnIntent.putExtra("result",device);
        setResult(BluetoothActivity.RESULT_OK,returnIntent);
        if (mScanning) {
            bthDeviceScan.stopScan();
            mScanning = false;
        }
        finish();

    }



    private void startScanning(final boolean b) {
        if (b) {
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    prgScanning.dismiss();
                    mScanning = false;
                    bthDeviceScan.stopScan();
                    invalidateOptionsMenu();
                }
            }, SCAN_PERIOD);
            prgScanning.show();
            ;
            mScanning = true;
            bthDeviceScan.doDiscovery();
        } else {
            mScanning = false;
            bthDeviceScan.stopScan();
        }
    }

    private void checkBlueToothState() {
        if (bthAdapter == null) {
            showUnsupported();
            finish();
            return;
        } else {
            if (bthAdapter.isEnabled()) {
                showEnabled();
            } else {
                showDisabled();
            }

        }
    }

    private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            prgScanning.dismiss();
            String action = intent.getAction();
            if (action.equals(Constants.ACTION_MESSAGE_SCAN)) {
                BluetoothDevice bthDevice = intent.getParcelableExtra(Constants.EXTRA_DEVICE_SCAN);
                if (bthDevice != null) {
                    mDevicesListAdapter.addDevice(bthDevice);
                    mDevicesListAdapter.notifyDataSetChanged();

                }
            }


        }
    };


    public void showUnsupported() {
        toastMessageLong("Error-Bluetooth not suported");

    }

    public void showEnabled() {
        toastMessageShort("Bluetooth Enabled");

    }

    public void showDisabled() {
        toastMessageShort("Bluetooth Disabled");

    }

    public void toastMessageShort(String a) {
        Toast.makeText(this, a, Toast.LENGTH_SHORT).show();

    }

    public void toastMessageLong(String a) {
        Toast.makeText(this, a, Toast.LENGTH_LONG).show();

    }


}