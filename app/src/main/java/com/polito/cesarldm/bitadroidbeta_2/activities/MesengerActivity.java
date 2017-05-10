package com.polito.cesarldm.bitadroidbeta_2.activities;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.polito.cesarldm.bitadroidbeta_2.R;
import com.polito.cesarldm.bitadroidbeta_2.Services.BitalinoConnection;

public class MesengerActivity extends AppCompatActivity implements View.OnClickListener {
    //testing activity to comunicate with service
    Button btn, btn2;
    Messenger mService=null;
    boolean mBound;
    String address;
    /**
     * Class for interacting with the main interface of the service.
     */
    private ServiceConnection mConnection= new ServiceConnection(){
        public void onServiceConnected(ComponentName className, IBinder service){
            // This is called when the connection with the service has been
            // established, giving us the object we can use to
            // interact with the service.  We are communicating with the
            // service using a Messenger, so here we get a client-side
            // representation of that from the raw IBinder object.
            mService = new Messenger(service);
            mBound = true;


        }
        public void onServiceDisconnected(ComponentName className) {
            // This is called when the connection with the service has been
            // unexpectedly disconnected -- that is, its process crashed.
            mService = null;
            mBound = false;
        }

    };

    public void ConnectToDevice(){

        if (!mBound) return;
        // Create and send a message to the service, using a supported 'what' value
        Message msg = Message.obtain(null, BitalinoConnection.CONNECTION, 0, 0);
        try {
            mService.send(msg);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }


    @Override
    protected void onStart() {
        super.onStart();
        // Bind to the service
        Intent intent=new Intent (this,BitalinoConnection.class);
        intent.putExtra("address",address);
        startService(intent);
        //bindService(intent, mConnection,Context.BIND_AUTO_CREATE);

    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mesenger);
        btn=(Button)findViewById(R.id.button);
        btn2=(Button)findViewById(R.id.button2);

        Intent i=getIntent();
        address=i.getStringExtra("address");
        btn.setOnClickListener(this);
        btn2.setOnClickListener(this);


    }

    @Override
    protected void onStop() {
        super.onStop();
        // Unbind from the service
        if (mBound) {
            unbindService(mConnection);
            mBound = false;
        }
    }

    @Override
    public void onClick(View v) {
       if(v==btn) {
           Intent intent = new Intent(this, BitalinoConnection.class);
           bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
           ConnectToDevice();
       }if(v==btn2){
            Intent intent = new Intent(this, BitalinoConnection.class);
            bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
            startReading();


        }
    }

    private void startReading() {
        if (!mBound) return;
        // Create and send a message to the service, using a supported 'what' value
        Message msg = Message.obtain(null, BitalinoConnection.RECORDING, 0, 0);
        try {
            mService.send(msg);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

}
