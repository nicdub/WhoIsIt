package ch.epfl.ndubois.whoisit;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.support.annotation.Nullable;

/**
 * Created by NicDub on 28.02.2017.
 */

public class WhoIsItService extends Service {

    private final CallReceiver receiver = new CallReceiver();
    //private String _apiUrl;

//    public WhoIsItService(String apiUrl)
//    {
//        _apiUrl = apiUrl;
//    }

    @Override
    public void onCreate() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(android.telephony.TelephonyManager.ACTION_PHONE_STATE_CHANGED);

        registerReceiver(receiver, filter);

    }

    @Override
    public void onDestroy() {
        unregisterReceiver(receiver);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
