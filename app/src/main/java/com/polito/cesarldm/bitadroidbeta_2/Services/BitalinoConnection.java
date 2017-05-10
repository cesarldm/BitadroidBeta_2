package com.polito.cesarldm.bitadroidbeta_2.Services;

import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.Parcelable;
import android.os.Process;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;

import info.plux.pluxapi.Communication;
import info.plux.pluxapi.Constants;
import info.plux.pluxapi.bitalino.BITalinoCommunication;
import info.plux.pluxapi.bitalino.BITalinoCommunicationFactory;
import info.plux.pluxapi.bitalino.BITalinoDescription;
import info.plux.pluxapi.bitalino.BITalinoException;
import info.plux.pluxapi.bitalino.BITalinoFrame;
import info.plux.pluxapi.bitalino.BITalinoState;
import info.plux.pluxapi.bitalino.bth.OnBITalinoDataAvailable;

/**
 * Created by CesarLdM on 9/5/17.
 */

public class BitalinoConnection extends Service {
    //Bitalino
   BITalinoCommunication bitaCom;
    private String address;
    private String conState;

    ArrayList<BITalinoFrame> frames=new ArrayList<BITalinoFrame>();


    private static String TAG="BitalinoConnection";
    public static final int CONNECTION=1;
    public static final int RECORDING=3;



    boolean isConnected=false;
    boolean isRecording=false;



    private ServiceHandler mServiceHandler;


    class ServiceHandler extends Handler{

        @Override
        public void handleMessage(Message msg){


            switch(msg.what){
                case CONNECTION:
                    manageConnection();
                    break;
                case RECORDING:
                    manageRecording();
                    break;


                case 0:
                    Log.d(TAG,"CHECK IF IT CONNECTS TO BITALINO");
            }
            //stopSelf(msg.arg1);
        }

    }

    private void manageRecording() {
        try {
            if (isConnected) {
                if (!isRecording) {

                    bitaCom.start(new int[]{0, 1, 2, 3, 4, 5}, 1);
                    isRecording=true;


                }else if (isRecording) {
                    bitaCom.stop();
                    isRecording=false;
                    Log.e(TAG, "BITALINO Readed");
                    for (BITalinoFrame frame : frames)
                        Log.v(TAG, frame.toString());

                }

            }
        } catch (BITalinoException e) {
            e.printStackTrace();
        }
    }

    private void manageConnection() {
        try {
            if(!isConnected) {
                bitaCom.connect(address);
            }if(isConnected){
                bitaCom.disconnect();
            }
        } catch (BITalinoException e) {
            e.printStackTrace();
        }


    }

    @Override
    public void onCreate(){
        HandlerThread thread= new HandlerThread("ServiceStartArguments",
                Process.THREAD_PRIORITY_BACKGROUND);
        thread.start();


        mServiceHandler= new ServiceHandler();

    }
    @Override
    public int onStartCommand(Intent intent,int flags,int startId){
        Toast.makeText(this, "bth service starting",Toast.LENGTH_SHORT).show();
        if(intent.hasExtra("address")) {
            address = intent.getStringExtra("address");
        }
        Message msg=mServiceHandler.obtainMessage();
        msg.arg1=startId;
        mServiceHandler.sendMessage(msg);
        initializeBitalinoApi();
        registerReceiver(updateReceiver, updateIntentFilter());


        return START_STICKY;
    }

    private void initializeBitalinoApi() {
       bitaCom = new BITalinoCommunicationFactory
                ().getCommunication(Communication.BTH, this.getApplicationContext(), new OnBITalinoDataAvailable(){
            @Override
            public void onBITalinoDataAvailable(BITalinoFrame bitalinoFrame) {
                Log.d(TAG, "BITalinoFrame: " + bitalinoFrame.toString());

                frames.add(bitalinoFrame);


            }
        });
    }

    final Messenger mMessenger = new Messenger(new ServiceHandler());

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Toast.makeText(getApplicationContext(),"binding",Toast.LENGTH_SHORT).show();
        return mMessenger.getBinder();
    }

    @Override
    public void onDestroy(){
        try {
            bitaCom.disconnect();
        } catch (BITalinoException e) {
            e.printStackTrace();
        }
        unregisterReceiver(updateReceiver);
        Log.d(TAG,"service ended");
        Toast.makeText(this, "bth service finished",Toast.LENGTH_SHORT).show();
    }

    private final BroadcastReceiver updateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (Constants.ACTION_STATE_CHANGED.equals(action)) {
                String identifier = intent.getStringExtra(Constants.IDENTIFIER);
                Constants.States state =
                        Constants.States.getStates(intent.getIntExtra(Constants.EXTRA_STATE_CHANGED,0));
                Log.i(TAG, "Device " + identifier + ": " + state.name());
                checkConnected(state.name());
                conState=state.name();
                Toast.makeText(getApplicationContext(),"Device "+state.name(),Toast.LENGTH_SHORT).show();
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
                            ((BITalinoDescription)parcelable).isBITalino2() + "; FwVersion:"+
                    String.valueOf(((BITalinoDescription)parcelable).getFwVersion()));
                }
            } else if (Constants.ACTION_MESSAGE_SCAN.equals(action)){
                BluetoothDevice device = intent.getParcelableExtra(Constants.EXTRA_DEVICE_SCAN);
            }
        }
    };

    private void checkConnected(String state) {
        if(state.equals("CONNECTED")){
            isConnected=true;
        }
        if(state.equals("DISCONNECTED")){
            isConnected=false;
        }

    }

    protected static IntentFilter updateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Constants.ACTION_STATE_CHANGED);
        intentFilter.addAction(Constants.ACTION_DATA_AVAILABLE);
        intentFilter.addAction(Constants.ACTION_COMMAND_REPLY);
        intentFilter.addAction(Constants.ACTION_MESSAGE_SCAN);
        return intentFilter;
    }
}

