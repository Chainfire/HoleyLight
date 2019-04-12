package eu.chainfire.holeylight.aodhelper;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;

public class AODReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        setResultCode(0);
        if ("eu.chainfire.holeylight.aodhelper.SET_AOD".equals(intent.getAction())) {
            if (intent.hasExtra("eu.chainfire.holeylight.aodhelper.SET_AOD.enable")) {
                boolean enabled = intent.getBooleanExtra("eu.chainfire.holeylight.aodhelper.SET_AOD.enable", false);
                try {
                    ContentResolver resolver = context.getContentResolver();
                    android.provider.Settings.System.putInt(resolver, "aod_mode", enabled ? 1 : 0);
                    android.provider.Settings.System.putInt(resolver, "aod_tap_to_show_mode", 0);
                    setResultCode(1);
                } catch (Exception e) {
                    // no permissions
                    setResultCode(2);
                    e.printStackTrace();
                }
            }
        }
    }
}
