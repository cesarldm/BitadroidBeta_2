package com.polito.cesarldm.bitadroidbeta_2.activities;

import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Parcelable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

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

import static com.polito.cesarldm.bitadroidbeta_2.activities.MainActivity.updateIntentFilter;

public class RecordingMenuActivity extends AppCompatActivity implements View.OnClickListener{
    private final String TAG="¡¡¡RECORDINGACT!!!";
    private Button btnStartRec,btnConnect,btnStop;
    private TextView tvCheck;
    private String address;
    private BluetoothDevice device;
    private BITalinoCommunication bitacom;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recording_menu);
        btnStartRec=(Button)findViewById(R.id.btn_RMA_get_data);
        btnConnect=(Button)findViewById(R.id.btn_RMA_connect);
        btnStop=(Button)findViewById(R.id.btn_RMA_stop);
        Bundle bundle = getIntent().getExtras();
        address = bundle.getString("address");
        tvCheck=(TextView)findViewById(R.id.tv_check_address);

    }
    @Override
    protected  void onResume(){
        super.onResume();
        btnStartRec.setOnClickListener(this);
        btnConnect.setOnClickListener(this);
        btnStop.setOnClickListener(this);
        tvCheck.setText(address);
        initializeBitalinoApi();

        registerReceiver(mBroadcastReceiver, updateIntentFilter());


    }

    private void initializeBitalinoApi() {
        bitacom = new BITalinoCommunicationFactory
                ().getCommunication(Communication.BTH, getBaseContext(), new OnBITalinoDataAvailable(){

            @Override
            public void onBITalinoDataAvailable(BITalinoFrame bitalinoFrame) {
                Log.d(TAG, "BITalinoFrame: " + bitalinoFrame.toString());

            }
        });

    }

    private  BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if (Constants.ACTION_STATE_CHANGED.equals(action)) {
                String identifier = intent.getStringExtra(Constants.IDENTIFIER);
                Constants.States state =
                        Constants.States.getStates(intent.getIntExtra(Constants.EXTRA_STATE_CHANGED,0));
                Log.i(TAG, "Device " + identifier + ": " + state.name());


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

    @Override
    public void onClick(View v) {
        if (v==btnConnect){
            try {
                bitacom.connect(address);
            } catch (BITalinoException e) {
                e.printStackTrace();

            }
        }
        if(v==btnStartRec){

            try {
                bitacom.start(new int[]{0,1,2,3,4,5},1);
            } catch (BITalinoException e) {
                e.printStackTrace();
            }

        }
        if(v==btnStop){
            try {
                bitacom.stop();
            } catch (BITalinoException e) {
                e.printStackTrace();
            }
        }

    }
}
