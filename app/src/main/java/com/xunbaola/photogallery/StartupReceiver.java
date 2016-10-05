package com.xunbaola.photogallery;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

public class StartupReceiver extends BroadcastReceiver {
private static final String TAG = "StartupReceiver";
    public StartupReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        // TODO: This method is called when the BroadcastReceiver is receiving
        // an Intent broadcast.
        Toast.makeText(context,"Received broadcast intent " +intent.getAction(),Toast.LENGTH_LONG).show();
        Log.d(TAG, "Received broadcast intent " +intent.getAction() );
        boolean isOn= PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(PollService.PREF_IS_ALARM_ON,false);
        PollService.setServiceAlarm(context,isOn);
    }
}
