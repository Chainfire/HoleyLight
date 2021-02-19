package eu.chainfire.holeylight.misc;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.companion.CompanionDeviceManager;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;
import android.view.accessibility.AccessibilityManager;

import java.util.List;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import eu.chainfire.holeylight.BuildConfig;
import eu.chainfire.holeylight.R;
import eu.chainfire.holeylight.animation.NotificationAnimation;
import eu.chainfire.holeylight.service.AccessibilityService;
import eu.chainfire.holeylight.ui.MainActivity;

import static android.content.Context.ACCESSIBILITY_SERVICE;
import static android.content.Context.COMPANION_DEVICE_SERVICE;
import static android.content.Context.POWER_SERVICE;

@SuppressWarnings("WeakerAccess")
public class Permissions {
    private static final int NOTIFICATION_ID_PERMISSIONS = 2001;

    public enum Needed { DEVICE_SUPPORT, DEVICE_OFFICIAL_SUPPORT, UNHIDE_NOTCH, COMPANION_DEVICE, NOTIFICATION_SERVICE, ACCESSIBILITY_SERVICE, BATTERY_OPTIMIZATION_EXEMPTION, AOD_HELPER_UPDATE, AOD_HELPER_PERMISSIONS, NONE }

    public static boolean allowAODHelperUpdateNeeded = true;
    public static boolean allowAODHelperPermissionsNeeded = true;

    private static boolean haveAccessibilityService(Context context) {
        AccessibilityManager accessibilityManager = ((AccessibilityManager)context.getSystemService(ACCESSIBILITY_SERVICE));

        {
            // Official way
            List<AccessibilityServiceInfo> services = accessibilityManager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK);
            for (AccessibilityServiceInfo serviceInfo : services) {
                if (serviceInfo.getResolveInfo().serviceInfo.packageName.equals(BuildConfig.APPLICATION_ID)) {
                    return accessibilityManager.isEnabled();
                }
            }
        }

        {
            // Sometimes the official way doesn't work and returns an empty list, even if the
            // service is enabled. Try it this way.
            try {
                String services = android.provider.Settings.Secure.getString(context.getContentResolver(), android.provider.Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
                String name = AccessibilityService.class.getCanonicalName();
                if ((services != null) && (name != null) && (services.contains(name))) {
                    return accessibilityManager.isEnabled();
                }
            } catch (Exception e) {
                // no action
            }
        }

        return false;
    }

    @SuppressLint("WrongConstant")
    public static Needed detect(Context context, boolean aodHelper) {
        NotificationAnimation animation = new NotificationAnimation(context, null, 0, null);
        if (!animation.isDeviceSupported()) {
            return Needed.DEVICE_SUPPORT;
        } else if (!animation.isDeviceOfficiallySupported() && !Settings.getInstance(context).isDeviceOfficialSupportWarningShown()) {
            return Needed.DEVICE_OFFICIAL_SUPPORT;
        } else if (android.provider.Settings.Secure.getInt(context.getContentResolver(), "display_cutout_hide_notch", 0) == 1) {
            return Needed.UNHIDE_NOTCH;
        } else if (((CompanionDeviceManager)context.getSystemService(COMPANION_DEVICE_SERVICE)).getAssociations().size() == 0) {
            return Needed.COMPANION_DEVICE;
        } else if (!NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.getPackageName())) {
            return Needed.NOTIFICATION_SERVICE;
        } else if (!haveAccessibilityService(context)) {
            return Needed.ACCESSIBILITY_SERVICE;
        } else if (!((PowerManager)context.getSystemService(POWER_SERVICE)).isIgnoringBatteryOptimizations(BuildConfig.APPLICATION_ID)) {
            return Needed.BATTERY_OPTIMIZATION_EXEMPTION;
        } else {
            if (aodHelper) {
                AODControl.AODHelperState state;
                if (allowAODHelperUpdateNeeded || allowAODHelperPermissionsNeeded) {
                    state = AODControl.getAODHelperState(context);
                } else {
                    state = AODControl.AODHelperState.NOT_INSTALLED;
                }
                if (allowAODHelperUpdateNeeded && state == AODControl.AODHelperState.NEEDS_UPDATE) {
                    return Needed.AOD_HELPER_UPDATE;
                } else if (allowAODHelperPermissionsNeeded && state == AODControl.AODHelperState.NEEDS_PERMISSIONS) {
                    return Needed.AOD_HELPER_PERMISSIONS;
                }
            }
            return Needed.NONE;
        }
    }

    public static boolean isNotificationWorthy(Needed needed) {
        return (needed != Needed.NONE) && (needed != Needed.DEVICE_SUPPORT) && (needed != Needed.DEVICE_OFFICIAL_SUPPORT);
    }

    public static void notify(Context context) {
        if (isNotificationWorthy(detect(context, true))) {
            NotificationManagerCompat.from(context).deleteNotificationChannel(BuildConfig.APPLICATION_ID + ":permission");
            @SuppressLint("WrongConstant") final NotificationChannel chan = new NotificationChannel(BuildConfig.APPLICATION_ID + ":permission", context.getString(R.string.app_name), NotificationManager.IMPORTANCE_HIGH);
            chan.setDescription(context.getString(R.string.app_name));
            NotificationManagerCompat.from(context).createNotificationChannel(chan);

            Notification notificationTest = (new NotificationCompat.Builder(context, chan.getId()))
                    .setContentTitle(context.getString(R.string.notification_permissions_title))
                    .setContentText(context.getString(R.string.notification_permissions_description))
                    .setBadgeIconType(NotificationCompat.BADGE_ICON_NONE)
                    .setOngoing(false)
                    .setOnlyAlertOnce(true)
                    .setNumber(0)
                    .setSmallIcon(R.drawable.ic_notify_jh)
                    .setContentIntent(PendingIntent.getActivity(context, 0, new Intent(context, MainActivity.class), 0))
                    .build();

            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_PERMISSIONS, notificationTest);
        } else {
            unnotify(context);
        }
    }

    public static void unnotify(Context context) {
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID_PERMISSIONS);
    }
}
