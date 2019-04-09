package eu.chainfire.holeylight.receiver;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;

import eu.chainfire.holeylight.misc.Permissions;
import eu.chainfire.holeylight.service.AccessibilityService;

public class PackageReplaced extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_MY_PACKAGE_REPLACED.equals(intent.getAction())) {
            // Notify user if we need more permissions
            Permissions.notify(context);

            // There's a bug in accessibility services somewhere that makes your package stop working
            // on update sometimes. This often fixes that.
            PackageManager packageManager = context.getPackageManager();
            ComponentName componentName = new ComponentName(context, AccessibilityService.class);
            packageManager.setComponentEnabledSetting(componentName, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
            packageManager.setComponentEnabledSetting(componentName, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, 0);
        }
    }
}
