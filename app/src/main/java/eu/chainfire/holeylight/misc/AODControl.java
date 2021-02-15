/*
 * Copyright (C) 2019-2021 Jorrit "Chainfire" Jongma
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

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.os.SystemClock;

import java.util.Date;
import java.util.List;

import eu.chainfire.holeylight.receiver.AlarmReceiver;

public class AODControl {
    private static Boolean helperPackageFound = null;
    private static long lastInScheduleCheck = 0L;
    private static boolean lastInSchedule = true;
    private static Date lastAlarm = null;

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

    @SuppressWarnings("deprecation")
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

    public static boolean isAODTapToShow(Context context) {
        ContentResolver resolver = context.getContentResolver();
        return isAODEnabled(context) && (android.provider.Settings.System.getInt(resolver, "aod_tap_to_show_mode", 0) > 0);
    }

    public static String getAODThemePackage(Context context) {
        ContentResolver resolver = context.getContentResolver();
        return android.provider.Settings.System.getString(resolver, "current_sec_aod_theme_package");
    }

    public static int[] getAODSchedule(Context context) {
        ContentResolver resolver = context.getContentResolver();
        int start = android.provider.Settings.System.getInt(resolver, "aod_mode_start_time", 0);
        int end = android.provider.Settings.System.getInt(resolver, "aod_mode_end_time", 0);
        if (start != end) return new int[] { start, end };
        return null;
    }

    @SuppressWarnings("deprecation")
    public static boolean inAODSchedule(Context context, boolean refresh) {
        long now = SystemClock.elapsedRealtime();
        if ((lastInScheduleCheck != 0) && (Math.abs(now - lastInScheduleCheck) < 60000) && !refresh) {
            return lastInSchedule;
        }

        lastInScheduleCheck = now;
        int[] schedule = getAODSchedule(context);

        if (schedule == null) {
            lastInSchedule = true;
            return true;
        }

        // Deprecated in favor of Calendar, which is... meh
        Date date = new Date(System.currentTimeMillis());
        int cmp = (date.getHours() * 60) + date.getMinutes();
        if (schedule[0] < schedule[1]) {
            lastInSchedule = (cmp >= schedule[0]) && (cmp <= schedule[1]);
        } else {
            lastInSchedule = (cmp >= schedule[0]) || (cmp <= schedule[1]);
        }
        return lastInSchedule;
    }
    
    private static Date nextAlarmTime(int forSchedule) {
        Date now = new Date(System.currentTimeMillis());
        int nowInt = (now.getHours() * 60) + now.getMinutes();
        if (nowInt >= forSchedule) {
            now = new Date(now.getTime() + (24 * 60 * 60 * 1000));
        }
        return new Date(now.getYear(), now.getMonth(), now.getDate(), forSchedule / 60, forSchedule % 60, 0);
    }

    public static void setAODAlarm(Context context) {
        // Schedule an alarm to wake up according to AOD schedule
        int[] schedule = getAODSchedule(context);
        if (schedule != null) {
            AlarmManager alarmManager = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);

            // Add one minute so we're sure to be after the event
            Date next0 = nextAlarmTime(schedule[0] + 1);
            Date next1 = nextAlarmTime(schedule[1] + 1);
            Date when = next0.before(next1) ? next0 : next1;

            if ((lastAlarm != null) && (lastAlarm.equals(when))) return;

            Intent intent = new Intent(AlarmReceiver.ACTION);
            intent.setClass(context, AlarmReceiver.class);

            PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, 0);
            alarmManager.cancel(pendingIntent);
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, when.getTime(), pendingIntent);

            lastAlarm = when;
        }
    }
}
