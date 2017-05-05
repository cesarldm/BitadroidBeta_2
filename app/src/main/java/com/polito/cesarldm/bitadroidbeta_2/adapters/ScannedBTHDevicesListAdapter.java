package com.polito.cesarldm.bitadroidbeta_2.adapters;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.polito.cesarldm.bitadroidbeta_2.R;

import java.util.ArrayList;

/**
 * Created by CesarLdM on 27/4/17.
 */

public class ScannedBTHDevicesListAdapter extends BaseAdapter {
    Context context;
    private ArrayList<BluetoothDevice> devices;


    public ScannedBTHDevicesListAdapter(Context context){
        super();
        this.devices=new ArrayList<>();
        this.context=context;

    }

    public void addDevice(BluetoothDevice device) {
        if(!devices.contains(device)) {
            devices.add(device);
        }
    }

    public void clear(){
        devices.clear();
    }


    @Override
    public int getCount() {return devices.size();}

    @Override
    public BluetoothDevice getItem(int position) {return devices.get(position);}

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView==null){
            LayoutInflater inflater=(LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView=inflater.inflate(R.layout.bth_list_item, null);
        }
        //Otherwise
        holder=new ViewHolder();
        //initialize views
        holder.deviceName= (TextView) convertView.findViewById(R.id.tv_BDLI_name);
        holder.deviceAddress=(TextView) convertView.findViewById(R.id.tv_BDLI_address);
        //assign views data
        holder.deviceName.setText(devices.get(position).getName());
        holder.deviceAddress.setText(devices.get(position).getAddress());
        //add a CONNECTED/DISCONECTED INDICATOR
        return convertView;
    }



    static class ViewHolder{
        TextView deviceName;
        TextView deviceAddress;

    }
}
