package eu.chainfire.holeylight.misc;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;

import java.util.List;

public class AODControl {
    private static Boolean helperPackageFound = null;

    private static Intent getIntent(boolean enabled) {
        Intent intent = new Intent("eu.chainfire.holeylight.aodhelper.SET_AOD");
        intent.setClassName("eu.chainfire.holeylight.aodhelper", "eu.chainfire.holeylight.aodhelper.AODReceiver");
        intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
        intent.putExtra("eu.chainfire.holeylight.aodhelper.SET_AOD.enable", enabled);
        return intent;
    }

    public static boolean haveHelperPackage(Context context, boolean refresh) {
        if ((helperPackageFound != null) && !refresh) return helperPackageFound;

        List<ResolveInfo> resolves = context.getPackageManager().queryBroadcastReceivers(getIntent(true), 0);
        for (ResolveInfo info : resolves) {
            if (info.activityInfo != null) {
                if (info.activityInfo.packageName.equals("eu.chainfire.holeylight.aodhelper")) {
                    helperPackageFound = true;
                    return true;
                }
            }
        }
        helperPackageFound = false;
        return false;
    }

    public static void setAOD(Context context, boolean enabled) {
        // We can't en/disable AOD directly because we cannot write the required settings from
        // this app. You need targetApi <= 22 for that. But if we set that, we lose the
        // ability to read the LED colors. Additionally, Google Play now requires targetApi 26.
        // Instead, a separate package was created with targetApi == 22, which we tell this way
        // to en/disable AOD.
        context.sendOrderedBroadcast(getIntent(enabled), null, new BroadcastReceiver() {
            @SuppressWarnings("all")
            @Override
            public void onReceive(Context context, Intent intent) {
                if (getResultCode() != 1) {
                    //TODO raise the alarms, something went awry
                }
            }
        }, null, 0, null, null);
    }
}
