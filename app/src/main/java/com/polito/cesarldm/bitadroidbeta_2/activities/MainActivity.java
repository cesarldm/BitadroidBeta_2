package com.polito.cesarldm.bitadroidbeta_2.activities;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.polito.cesarldm.bitadroidbeta_2.R;

import info.plux.pluxapi.Communication;
import info.plux.pluxapi.Constants;
import info.plux.pluxapi.bitalino.BITalinoCommunication;
import info.plux.pluxapi.bitalino.BITalinoCommunicationFactory;
import info.plux.pluxapi.bitalino.BITalinoDescription;
import info.plux.pluxapi.bitalino.BITalinoException;
import info.plux.pluxapi.bitalino.BITalinoFrame;
import info.plux.pluxapi.bitalino.BITalinoState;
import info.plux.pluxapi.bitalino.bth.OnBITalinoDataAvailable;

import static android.Manifest.permission.ACCESS_COARSE_LOCATION;
import static info.plux.pluxapi.Constants.ACTION_STATE_CHANGED;
import static info.plux.pluxapi.Constants.EXTRA_STATE_CHANGED;
import static info.plux.pluxapi.Constants.IDENTIFIER;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{
    //UI
    Button btnConnect,btnStartRec,btnExamRec;
    TextView tvEstate,tvName,tvAddress,tvBat;
    //codes
    private final String TAG="MainActivity";
    final int BLUETOOTH_INTENT=1;
    //bluetooth
    private BITalinoCommunication bitacom;
    private BluetoothDevice device;
    private BITalinoState deviceState;
    //booleans
    private boolean isConnected=false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initializeUI();

    }
    @Override
    protected  void onResume(){
        super.onResume();
        if(device!=null){
            if(bitacom==null) {
                initializeBitalinoApi();
                registerReceiver(mBroadcastReceiver, updateIntentFilter());
                try {
                    bitacom.connect(device.getAddress());
                } catch (BITalinoException e) {
                    e.printStackTrace();
                }
            }
        }
        deviceSelected();
        btnConnect.setOnClickListener(this);
        btnStartRec.setOnClickListener(this);
    }


    private void initializeBitalinoApi() {
        bitacom = new BITalinoCommunicationFactory
                ().getCommunication(Communication.BTH, getBaseContext(), new OnBITalinoDataAvailable(){
            @Override
            public void onBITalinoDataAvailable(BITalinoFrame bitalinoFrame) {
                Log.d(TAG, "BITalinoFrame: " + bitalinoFrame.toString());
                toastMessageLong(bitalinoFrame.toString());
            }
        });

    }

    private void initializeUI() {
        btnConnect=(Button)findViewById(R.id.btn_MA_connect);
        btnStartRec=(Button)findViewById(R.id.btn_MA_start_recording);
        btnExamRec=(Button)findViewById(R.id.btn_MA_examine_recordings);
        tvEstate=(TextView)findViewById(R.id.tv_MA_state);
        tvName=(TextView)findViewById(R.id.tv_MA_name);
        tvAddress=(TextView)findViewById(R.id.tv_MA_address);
        tvBat=(TextView)findViewById(R.id.tv_MA_bat);

    }

    private void deviceSelected(){
        if(device!=null){
            tvName.setText("Name: "+device.getName());
            tvAddress.setText("Address: "+device.getAddress());
          //battery life:

        }else if(device==null){
            tvEstate.setText("No device selected");
            tvName.setText("...");
            tvAddress.setText("...");
        }




    }

    @Override
    public void onClick(View v) {
        switch(v.getId()){
            case R.id.btn_MA_connect:
                try {
                    if (bitacom!=null) {
                        bitacom.disconnect();
                        device=null;
                        bitacom=null;
                    }
                }catch (BITalinoException e) {
                    e.printStackTrace();
                }
                //start Bluetooth  activity
                Intent bthIntent=new Intent(this,BluetoothActivity.class);
                startActivityForResult(bthIntent,BLUETOOTH_INTENT);
                break;
            case R.id.btn_MA_start_recording:
                if(device!=null){
                    if(isConnected==true){
                        try {
                            bitacom.disconnect();
                        } catch (BITalinoException e) {
                            e.printStackTrace();
                        }
                    }
                Intent recordIntent=new Intent(this,RecordingMenuActivity.class);
                    recordIntent.putExtra("address",device.getAddress());
                    startActivity(recordIntent);

                }
                toastMessageLong("Device Not Selected");


        }

    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == 1) {
            if(resultCode == BluetoothActivity.RESULT_OK){
                Bundle b=data.getExtras();
                device=b.getParcelable("result");
                deviceState=new BITalinoState(device.getAddress());
                //connect
                toastMessageLong(device.getName() + ", MAC= " + device.getAddress());
            }
            if (resultCode == BluetoothActivity.RESULT_CANCELED) {
                device=null;
                toastMessageLong("No Bitalino device selected");
            }
        }
    }

    private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if (Constants.ACTION_STATE_CHANGED.equals(action)) {

                String identifier = intent.getStringExtra(Constants.IDENTIFIER);
                Constants.States state =
                        Constants.States.getStates(intent.getIntExtra(Constants.EXTRA_STATE_CHANGED,0));
                Log.i(TAG, "Device " + identifier + ": " + state.name());
                checkConnected(state.name());


            } else if (Constants.ACTION_DATA_AVAILABLE.equals(action)) {

                BITalinoFrame frame = intent.getParcelableExtra(Constants.EXTRA_DATA);
                Log.d(TAG, "BITalinoFrame: " + frame.toString());

            } else if (Constants.ACTION_COMMAND_REPLY.equals(action)) {

                String identifier = intent.getStringExtra(Constants.IDENTIFIER);
                 Parcelable parcelable = intent.getParcelableExtra(Constants.EXTRA_COMMAND_REPLY);

                if(parcelable.getClass().equals(BITalinoState.class)){

                    Log.d(TAG, "BITalinoState: " + parcelable.toString());

                } else if(parcelable.getClass().equals(BITalinoDescription.class)){

                    Log.d(TAG, "BITalinoDescription: isBITalino2: " +
                    ((BITalinoDescription)parcelable).isBITalino2() + "; FwVersion: "+
                            String.valueOf(((BITalinoDescription)parcelable).getFwVersion()));
                }
            } else if (Constants.ACTION_MESSAGE_SCAN.equals(action)){
                BluetoothDevice bthdevice = intent.getParcelableExtra(Constants.EXTRA_DEVICE_SCAN);
            }
        }
    };

 protected static IntentFilter updateIntentFilter() {
final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Constants.ACTION_STATE_CHANGED);
        intentFilter.addAction(Constants.ACTION_DATA_AVAILABLE);
         intentFilter.addAction(Constants.ACTION_COMMAND_REPLY);
        intentFilter.addAction(Constants.ACTION_MESSAGE_SCAN);
        return intentFilter;
        }

public void checkConnected(String check){
    tvEstate.setText(check);
    if(check.equals("CONNECTED")){
        isConnected=true;
                    //Objeto deviceStateVacio????-->Ver como solicitar datos
        deviceState=new BITalinoState(device.getAddress());
        tvBat.setText("Battery: "+deviceState.getBattery()+"%");
    }else if(check.equals("DISCONNECTED")){
        isConnected=false;
    }

}


    private void setData(BluetoothDevice device) {

    }

    public void toastMessageShort(String a) {
        Toast.makeText(this, a, Toast.LENGTH_SHORT).show();

    }

    public void toastMessageLong(String a) {
        Toast.makeText(this, a, Toast.LENGTH_LONG).show();

    }
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


}
