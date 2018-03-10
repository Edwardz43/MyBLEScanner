package tw.com.edlo.myblescanner;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.TextureView;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.inuker.bluetooth.library.search.SearchResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {
    private MyReceiver receiver;
    protected List<SearchResult> device_List;
    private AlertDialog.Builder dialog_list;
    private MyOnclickListener mOnclickListener;
    protected boolean isDialogShowed;
    private String deviceMAC;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Use this check to determine whether BLE is supported on the device. Then
        // you can selectively disable BLE-related features.
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            finish();
        }

        // request Permission
        if(ContextCompat.checkSelfPermission(this,
                Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED){
            Log.d("ed43","no permission");
            ActivityCompat.requestPermissions(this,
                    new String[] {
                            Manifest.permission.INTERNET,
                            Manifest.permission.BLUETOOTH_ADMIN,
                            Manifest.permission.BLUETOOTH,
                            Manifest.permission.ACCESS_COARSE_LOCATION,
                            Manifest.permission.ACCESS_FINE_LOCATION
                    },1 );
        }else {
            Log.d("ed43","got permission");
            init();
        }
    }

    private void init() {
        mOnclickListener = new MyOnclickListener();
        mOnclickListener.setParent(this);
        dialog_list = new AlertDialog.Builder(this);
//        Intent serviceIntent = new Intent(this, MyBLEScannerService.class);
//        serviceIntent.putExtra("cmd",0);
//        startService(serviceIntent);
        IntentFilter filter = new IntentFilter("BLEScannerService");
        receiver = new MyReceiver(this);
        registerReceiver(receiver, filter);
    }

    public void selectDevice(SearchResult device){
        //Log.d("ed43","deviceSearched : "+device.getName());
        isDialogShowed = false;
        device_List = new ArrayList<>();
        if(device.getName().substring(0, 3).equals("MCF")){
            if(!device_List.contains(device)) device_List.add(device);
            dialog_list.setTitle("Select Device");
            String[] device_Name = new String[device_List.size()];
            String[] device_Address = new String[device_List.size()];
            for (int i = 0; i < device_List.size(); i++){
                device_Name[i] = device_List.get(i).getName();
                device_Address[i] = device_List.get(i).getAddress();
            }
            dialog_list.setSingleChoiceItems(device_Name, -1, mOnclickListener);
            dialog_list.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    //Log.d("ed43", "select device : " + deviceMAC);
                    connect();
                    isDialogShowed = false;
                }
            });
            if(!isDialogShowed) {
                dialog_list.show();
                isDialogShowed = true;
            }
        }
    }

    private void connect(){
        Intent serviceIntent = new Intent(this, MyBLEScannerService.class);
        serviceIntent.putExtra("cmd",1);
        serviceIntent.putExtra("id", deviceMAC);
        startService(serviceIntent);
    }

    public void search(View view){
        Intent intent = new Intent(this, MyBLEScannerService.class);
        intent.putExtra("cmd", 0);
        startService(intent);
    }

    public void write(View view){
        Intent intent = new Intent(this, MyBLEScannerService.class);
        intent.putExtra("cmd", 4);
        startService(intent);
    }

    public void read(View view){
        Intent intent = new Intent(this, MyBLEScannerService.class);
        intent.putExtra("cmd", 5);
        startService(intent);
    }

    public void notify(View view){
        Intent intent = new Intent(this, MyBLEScannerService.class);
        intent.putExtra("cmd", 3);
        startService(intent);
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(receiver);
        super.onDestroy();
    }

    @Override
    protected void onStop() {
        Intent intent = new Intent(this, MyBLEScannerService.class);
        stopService(intent);
        super.onStop();
    }

    private class MyOnclickListener implements DialogInterface.OnClickListener{
        private MainActivity activity;

        public void setParent(MainActivity activity){this.activity = activity;}
        @Override
        public void onClick(DialogInterface dialogInterface, int i) {
            //只要你在onClick處理事件內，使用which參數，就可以知道按下陣列裡的哪一個了
            // TODO Auto-generated method stub
            Toast.makeText(activity, "你選的是" + activity.device_List.get(i).getName(), Toast.LENGTH_SHORT).show();
            activity.deviceMAC = activity.device_List.get(i).getAddress();
        }
    }


    public void setWeight(double i){
        TextView weightTV = findViewById(R.id.weightTV);
        double d = (i*22000/1000+5) / 10;
        weightTV.setText("" + d);
    }

}
