package tw.com.edlo.myblescanner;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.TextureView;
import android.view.View;
import android.widget.Toast;

import com.google.gson.Gson;
import com.inuker.bluetooth.library.BluetoothClient;
import com.inuker.bluetooth.library.beacon.Beacon;
import com.inuker.bluetooth.library.connect.listener.BleConnectStatusListener;
import com.inuker.bluetooth.library.connect.listener.BluetoothStateListener;
import com.inuker.bluetooth.library.connect.options.BleConnectOptions;
import com.inuker.bluetooth.library.connect.response.BleConnectResponse;
import com.inuker.bluetooth.library.connect.response.BleNotifyResponse;
import com.inuker.bluetooth.library.connect.response.BleReadResponse;
import com.inuker.bluetooth.library.connect.response.BleWriteResponse;
import com.inuker.bluetooth.library.model.BleGattProfile;
import com.inuker.bluetooth.library.search.SearchRequest;
import com.inuker.bluetooth.library.search.SearchResult;
import com.inuker.bluetooth.library.search.response.SearchResponse;

import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import static com.inuker.bluetooth.library.Constants.REQUEST_FAILED;
import static com.inuker.bluetooth.library.Constants.REQUEST_SUCCESS;
import static com.inuker.bluetooth.library.Constants.STATUS_CONNECTED;
import static com.inuker.bluetooth.library.Constants.STATUS_DISCONNECTED;

/**
 * Created by EdLo on 2018/3/4.
 */

public class MyBLEScannerService extends Service {
    static boolean isBluetoothOpen;
    static boolean isDeviceConnect;
    static boolean isDeviceSearched;
    private boolean isWriteResponsed;
    private String deviceMAC;
    private BluetoothClient mClient;
    private static BleConnectStatusListener mBleConnectStatusListener;
    private static BluetoothStateListener mBluetoothStateListener;
    private final UUID serviceUUID = UUID.fromString("6e400001-b5a3-f393-e0a9-e50e24dcca9e");
    private final UUID notifyUUID = UUID.fromString("6e400003-b5a3-f393-e0a9-e50e24dcca9e");
    private final UUID writeUUID = UUID.fromString("6e400002-b5a3-f393-e0a9-e50e24dcca9e");
    private final UUID readUUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
    private final byte[] test = new byte[]{90,-46,0,0,0,0,0,0,0,0,0,0,-120,-86};
    protected final Integer SEARCH = 0;
    protected final Integer CONNECT = 1;
    protected final Integer DISCONNECT = 2;
    protected final static Integer NOTIFY = 3;
    protected final static Integer WRITE = 4;
    protected final static Integer READ = 5;
    private Timer timer;

    @Override
    public void onCreate() {
        super.onCreate();
        isBluetoothOpen = false;
        isDeviceConnect = false;
        isWriteResponsed = false;
        isDeviceSearched = false;
        init();
    }

    private void init() {
        isBluetoothOpen = false;
        isDeviceConnect = false;
        isWriteResponsed = false;
        isDeviceSearched = false;

        mClient = new BluetoothClient(this);
        mClient.openBluetooth();

        // 偵測 BLE 狀態Listener
        mBluetoothStateListener = new BluetoothStateListener() {
            @Override
            public void onBluetoothStateChanged(boolean openOrClosed) {
                isBluetoothOpen = openOrClosed;
                Intent localIntent = new Intent("BLEScannerService");
                localIntent.putExtra("isBluetoothOpen", MyBLEScannerService.isBluetoothOpen);
                MyBLEScannerService.this.sendBroadcast(localIntent);
                Log.d("ed43", "BLE:status:" + openOrClosed);
            }
        };
        mClient.registerBluetoothStateListener(mBluetoothStateListener);


        mBleConnectStatusListener = new BleConnectStatusListener() {
            @Override
            public void onConnectStatusChanged(String mac, int status) {
                if (status == STATUS_CONNECTED) {
                    Log.d("ed43", "Connect Status Changed : CONNECTED");
                } else if (status == STATUS_DISCONNECTED) {
                    Log.d("ed43", "Connect Status Changed : DISCONNECTED");
                }
            }
        };

        isBluetoothOpen = true;
        Intent localIntent = new Intent("BLEScannerService");
        localIntent.putExtra("isBluetoothOpen", isBluetoothOpen);
        sendBroadcast(localIntent);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        //Log.d("ed43", "onStartCommand");
        if (!isBluetoothOpen){
            init();
        }

        if (intent != null) {
            int cmd = intent.getIntExtra("cmd", -1);
            String id = intent.getStringExtra("id");
            int value = intent.getIntExtra("value", 0);

            switch (cmd){
                case 0:
                    Log.d("ed43", "service cmd : search");
                    search();
                    break;
                case 1:
                    Log.d("ed43", "service cmd : connect");
                    connect(id);
                    break;
                case 2:
                    disconnect();
                    break;
                case 3:
                    mNotify();
                    break;
                case 4:
                    write(new byte[]{90,-43,5,30,0,2,-98,0,-64,0,0,0,-56,-86});
                    break;
                case 5:
                    //mNotify();
                    read();
                    break;
                case 6:
                    keepConnect();
                    break;
            }
        }
        return super.onStartCommand(intent, flags, startId);
    }

    private void keepConnect() {
        //Log.d("ed43", "keepConnect()");
        timer = new Timer();
        timer.schedule(new MyTask(),0,2000);
    }

    private void search() {
        isDeviceSearched = false;
        SearchRequest request = new SearchRequest.Builder()
                .searchBluetoothLeDevice(3000, 3)
                .build();

        mClient.search(request, new SearchResponse() {
            @Override
            public void onSearchStarted() {
                Log.d("ed43", "onSearchStarted");
            }

            @Override
            public void onDeviceFounded(SearchResult device) {
                //Log.d("ed43", "Device Founded");
                if(!isDeviceSearched && device.getName().substring(0, 3).equals("MCF")){
                    //Log.d("ed43", "Search Result : " + device.getName());
                    Intent it = new Intent("BLEScannerService");
                    it.putExtra("isDeviceSearched", true);
                    it.putExtra("device", device);
                    sendBroadcast(it);
                    isDeviceSearched = true;
                }

            }

            @Override
            public void onSearchStopped() {
                Log.d("ed43", "Search Stopped");
            }

            @Override
            public void onSearchCanceled() {
                Log.d("ed43", "Search Canceled");
            }
        });
    }

    private void connect(String deviceMAC) {
        connectDevice(deviceMAC);
        this.deviceMAC = deviceMAC;
        mClient.registerConnectStatusListener(deviceMAC, mBleConnectStatusListener);
    }

    private void disconnect() {
    }

    private void mNotify() {
        Log.d("ed43", "mNotify()");
        mClient.notify(deviceMAC, serviceUUID, notifyUUID, new BleNotifyResponse() {
            @Override
            public void onNotify(UUID service, UUID character, byte[] value) {
                //Log.d("ed43", "+++++++++++++++++++++++++++");
                Log.d("ed43", new Gson().toJson(value));
                String mData = toHexString(value);
                //Log.d("ed43", "mData : " + mData);
                if(mData.substring(2,4).equals("D2")){
                    Log.d("ed43", "Weight : " + getWeight(mData));
                    Intent it = new Intent("BLEScannerService");
                    it.putExtra("weight", getWeight(mData));
                    sendBroadcast(it);
                    write(test);
                } else if(mData.substring(2,4).equals("D3")){
                    Log.d("ed43", "data : " + mData);
                }
                //Log.d("ed43", "---------------------------");
                }
            @Override
            public void onResponse(int code) {
                //Log.d("ed43", "notify response code : " + code);
                if (code == REQUEST_SUCCESS) {
                    Log.d("ed43", "Notify : REQUEST_SUCCESS");
                }else if(code == REQUEST_FAILED){
                    Log.d("ed43", "Notify : REQUEST_FAILED");
                }
            }
        });
    }

    private void write(byte[] bytes) {
        //String data = "5a d4 00 00 00 00 00 00 00 00 00 00 8e aa";
        //Log.d("ed43", "Write : REQUEST");
        String mData = toHexString(bytes);
        Log.d("ed43", mData);
        mClient.write(deviceMAC, serviceUUID, writeUUID, bytes, new BleWriteResponse() {
            @Override
            public void onResponse(int code) {
                //Log.d("ed43", "write response code : " + code);
                if (code == REQUEST_SUCCESS) {
                    Log.d("ed43", "OK");
                }else if(code == REQUEST_FAILED){
                    //Log.d("ed43", "FAILED");
                }
            }
        });
    }

    private void read() {
        mClient.readDescriptor(deviceMAC, serviceUUID, notifyUUID, readUUID, new BleReadResponse() {
            @Override
            public void onResponse(int code, byte[] data) {
                Log.d("ed43", "read description");
                Log.d("ed43", new Gson().toJson(data));
            }
        });
    }

    public void disConnect(View view){
        if (timer != null){
            timer.cancel();
            timer.purge();
            timer = null;
        }
        mClient.disconnect(deviceMAC);
    }

    private boolean connectDevice(String deviceMAC){
        if (deviceMAC == null) {
            return false;
        }
        BleConnectOptions options = new BleConnectOptions.Builder()
                .setConnectRetry(3)   // 连接如果失败重试3次
                .setConnectTimeout(30000)   // 连接超时30s
                .setServiceDiscoverRetry(3)  // 发现服务如果失败重试3次
                .setServiceDiscoverTimeout(20000)  // 发现服务超时20s
                .build();
        Log.d("ed43","connectDevice deviceMAC : " + deviceMAC);
        mClient.connect(deviceMAC, options, new BleConnectResponse() {
            @Override
            public void onResponse(int code, BleGattProfile data) {
                Log.d("ed43","connectDevice Response");
                if(code == REQUEST_SUCCESS){
                    //isConnected = true;
                    //testRead();
                    Log.d("ed43", "Connect : REQUEST_SUCCESS");
                    Log.d("ed43", new Gson().toJson(data));
                    mNotify();
                }else if (code == REQUEST_FAILED){
                    Log.d("ed43","Connect : REQUEST_FAILED");
                }
            }
        });
        return true;
    }

    public String toHexString (byte[] bytes) {
        final char[] hexArray = {'0','1','2','3','4','5','6','7','8','9','A','B','C','D','E','F'};
        char[] hexChars = new char[bytes.length * 2];
        int v;
        for ( int j = 0; j < bytes.length; j++ ) {
            v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    public double getWeight(String s) {
        int highDigit = Integer.parseInt(s.substring(5, 6), 16);
        int midDigit = Integer.parseInt(s.substring(6, 7), 16);
        int lowDigit = Integer.parseInt(s.substring(7, 8), 16);
        return (highDigit * 16 * 16 + midDigit * 16 + lowDigit)/10.0;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mClient.unregisterConnectStatusListener(deviceMAC, mBleConnectStatusListener);
        mClient.unregisterBluetoothStateListener(mBluetoothStateListener);
        if (timer != null){
            timer.cancel();
            timer.purge();
            timer = null;
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    class MyTask extends TimerTask{
        @Override
        public void run() {
            //String data = "5a d4 00 00 00 00 00 00 00 00 00 00 8e aa";
            //Log.d("ed43", "Write : REQUEST");
            byte[] bytes = new byte[]{90, -44, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -114, -86};
            mClient.write(deviceMAC, serviceUUID, writeUUID, bytes, new BleWriteResponse() {
                @Override
                public void onResponse(int code) {
                    //Log.d("ed43", "write response code : " + code);
                    if (code == REQUEST_SUCCESS) {
                        Log.d("ed43", "OK");
                    }else if(code == REQUEST_FAILED){
                        Log.d("ed43", "FAILED");
                    }
                }
            });
        }
    }
}
