package tw.com.edlo.myblescanner;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;

import com.inuker.bluetooth.library.search.SearchResult;

/**
 * Created by EdLo on 2018/3/4.
 */

public class MyReceiver extends BroadcastReceiver {
    private MainActivity activity;

    public MyReceiver(){}

    public MyReceiver(MainActivity activity) {
        super();
        this.activity = activity;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        boolean isDeviceSearched = intent.getBooleanExtra("isDeviceSearched",false);
        if(isDeviceSearched){
            activity.selectDevice((SearchResult) intent.getParcelableExtra("device"));
        }

        double weight = intent.getDoubleExtra("weight", 0);
        if(weight > 0){
            activity.setWeight(weight);
        }

    }
}
