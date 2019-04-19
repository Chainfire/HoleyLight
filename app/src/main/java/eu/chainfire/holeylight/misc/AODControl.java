/*
 * Copyright (C) 2019 Jorrit "Chainfire" Jongma
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 */

package eu.chainfire.holeylight.misc;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
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

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
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

    public static boolean isAODEnabled(Context context) {
        ContentResolver resolver = context.getContentResolver();
        return (android.provider.Settings.System.getInt(resolver, "aod_mode", 0) > 0);
    }

    public static String getAODThemePackage(Context context) {
        ContentResolver resolver = context.getContentResolver();
        return android.provider.Settings.System.getString(resolver, "current_sec_aod_theme_package");
    }
}
