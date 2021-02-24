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
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.SystemClock;
import android.telephony.TelephonyManager;

import java.util.Date;

import eu.chainfire.holeylight.BuildConfig;
import eu.chainfire.holeylight.receiver.AlarmReceiver;

@SuppressWarnings({ "FieldCanBeLocal" })
public class AODControl {
    // Helper package methods
    //
    // We can't control AOD directly because we cannot write the required settings from
    // this app. You need targetApi <= 22 for that. But if we set that, we lose the
    // ability to read the LED colors. Additionally, Google Play now requires targetApi >22.
    // Instead, a separate package was created with targetApi == 22, which contains the
    // code to control AOD which we call through intents.

    public enum AODHelperState { NOT_INSTALLED, OK, NEEDS_UPDATE, NEEDS_PERMISSIONS };

    private static final int HELPER_VERSION_MIN = 54;

    private static Handler handler = null;
    private static HandlerThread handlerThread = null;

    private static Intent getIntent(String action, Boolean enabled) {
        String APPLICATION_ID_AOD_HELPER = BuildConfig.APPLICATION_ID + ".aodhelper";
        Intent intent = new Intent(APPLICATION_ID_AOD_HELPER + "." + action);
        intent.setClassName(APPLICATION_ID_AOD_HELPER, "eu.chainfire.holeylight.aodhelper.AODReceiver");
        intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
        intent.putExtra(APPLICATION_ID_AOD_HELPER + ".MANUFACTURER", Manufacturer.isSamsung() ? "samsung" : Manufacturer.isGoogle() ? "google" : "unknown");
        if (enabled != null) {
            intent.putExtra(APPLICATION_ID_AOD_HELPER + "." + action + ".enable", enabled);
        }
        return intent;
    }

    private static Handler getHandler() {
        synchronized (AODControl.class) {
            if (handler == null) {
                handlerThread = new HandlerThread(BuildConfig.APPLICATION_ID + ":helper");
                handlerThread.start();
                handler = new Handler(handlerThread.getLooper());
            }
            return handler;
        }
    }

    private interface HelperIntentResult {
        Object onIntent(Context context, Intent intent, int resultCode, String resultData, Bundle resultExtras);
    }

    public interface HelperIntentResultReceiver {
        void onResult(Object result);
    }

    private static Object sendHelperIntent(Context context, Intent intent, HelperIntentResult resultHandler, HelperIntentResultReceiver resultReceiver) {
        final boolean[] complete = new boolean[] { false };
        final Object[] result = new Object[] { null };
        final Handler handler = new Handler();
        context.sendOrderedBroadcast(intent, null, new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                result[0] = resultHandler.onIntent(context, intent, getResultCode(), getResultData(), getResultExtras(false));
                if (resultReceiver != null) {
                    handler.post(() -> resultReceiver.onResult(result[0]));
                } else {
                    synchronized (complete) {
                        complete[0] = true;
                        complete.notify();
                    }
                }
            }
        }, getHandler(), 0, null, null);
        if (resultReceiver == null) {
            while (!complete[0]) {
                synchronized (complete) {
                    try {
                        complete.wait(25);
                    } catch (InterruptedException e) {
                        break;
                    }
                }
            }
            return result[0];
        } else {
            return null;
        }
    }

    public static Boolean setAODBrightness(Context context, boolean high, HelperIntentResultReceiver async) {
        if (!Settings.getInstance(context).isAODHelperBrightness()) return false;
        if (isAODBrightness(context, high) == high) return true;
        return (Boolean)sendHelperIntent(context, getIntent("SET_BRIGHTNESS", high), (context1, intent, resultCode, resultData, resultExtras) -> {
            if (resultCode == 1) {
                Slog.d("AODControl", "SET_BRIGHTNESS called --> %d", high ? 1 : 0);
                return true;
            }
            Slog.d("AODControl", "Error calling SET_BRIGHTNESS: %d", resultCode);
            return false;
        }, async);
    }

    public static Boolean setAODEnabled(Context context, boolean enabled, HelperIntentResultReceiver async) {
        if (!Settings.getInstance(context).isAODHelperControl()) return false;
        if (isAODEnabled(context) == enabled) return true;
        if (((TelephonyManager)context.getSystemService(Context.TELEPHONY_SERVICE)).getCallState() != TelephonyManager.CALL_STATE_IDLE) return false;
        return (Boolean)sendHelperIntent(context, getIntent("SET_AOD", enabled), (context1, intent, resultCode, resultData, resultExtras) -> {
            if (resultCode == 1) {
                Slog.d("AODControl", "SET_AOD called --> %d", enabled ? 1 : 0);
                return true;
            }
            Slog.d("AODControl", "Error calling SET_AOD: %d", resultCode);
            return false;
        }, async);
    }

    public static void fixHelperPermissions(Context context, HelperIntentResultReceiver resultReceiver) {
        if (getAODHelperState(context) != AODHelperState.NEEDS_PERMISSIONS) resultReceiver.onResult(null);
        sendHelperIntent(context, getIntent("FIX_PERMISSIONS", null), (context1, intent, resultCode, resultData, resultExtras) -> {
            if (resultCode == 1) {
                Slog.d("AODControl", "Fix permissions OK");
                return true;
            } else if (resultCode == 2) {
                Slog.d("AODControl", "Fix permissions not OK");
                return false;
            }
            Slog.d("AODControl", "Error calling FIX_PERMISSIONS: %d", resultCode);
            return false;
        }, resultReceiver);
    }

    public static AODHelperState getAODHelperState(Context context) {
        boolean helperHave = false;
        int helperVersion = 0;
        boolean helperOutdated = false;
        boolean helperPermissions = false;
        {
            try {
                PackageInfo packageInfo = context.getPackageManager().getPackageInfo(BuildConfig.APPLICATION_ID + ".aodhelper", PackageManager.GET_PERMISSIONS);
                if (packageInfo != null) {
                    //noinspection deprecation
                    helperVersion = packageInfo.versionCode;
                    helperHave = true;
                    helperOutdated = helperVersion < HELPER_VERSION_MIN;

                    if (Manufacturer.isSamsung()) {
                        helperPermissions = true;
                    } else {
                        for (int i = 0; i < packageInfo.requestedPermissions.length; i++) {
                            // Google Play filters for the original string, some people have allegedly
                            // gotten their apps removed over just having the string inside the APK.
                            // This level of encryption is sure to fool all but the brightest minds!
                            if ((new StringBuilder("SGNITTES_ERUCES_ETIRW.noissimrep.diordna")).reverse().toString().equals(packageInfo.requestedPermissions[i])) {
                                if ((packageInfo.requestedPermissionsFlags[i] & PackageInfo.REQUESTED_PERMISSION_GRANTED) != 0) {
                                    helperPermissions = true;
                                    break;
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
            }
        }

        Settings settings = Settings.getInstance(context);
        if (!(helperHave && !helperOutdated && helperPermissions)) {
            if (settings.isAODHelperControl()) settings.setAODHelperControl(false);
            if (settings.isAODHelperBrightness()) settings.setAODHelperBrightness(false);
        }

        AODHelperState state = AODHelperState.NOT_INSTALLED;
        if (helperHave) {
            if (helperOutdated) {
                state = AODHelperState.NEEDS_UPDATE;
            } else if (!helperPermissions) {
                state = AODHelperState.NEEDS_PERMISSIONS;
            } else {
                state = AODHelperState.OK;
            }
        }

        Slog.d("AODControl", "Helper version: %d, outdated: %d, permissions: %d --> %s", helperVersion, helperOutdated ? 1 : 0, helperPermissions ? 1 : 0, state);
        return state;
    }

    // Generic methods

    private static long lastInScheduleCheck = 0L;
    private static boolean lastInSchedule = true;
    private static Date lastAlarm = null;

    public static boolean isAODEnabled(Context context) {
        ContentResolver resolver = context.getContentResolver();
        if (Manufacturer.isSamsung()) {
            return (android.provider.Settings.System.getInt(resolver, "aod_mode", 0) > 0);
        } else {
            return (android.provider.Settings.Secure.getInt(resolver, "doze_always_on", 0) > 0);
        }
    }

    public static boolean isAODBrightness(Context context, boolean defaultValue) {
        ContentResolver resolver = context.getContentResolver();
        if (Manufacturer.isSamsung()) {
            return defaultValue;
        } else {
            String value = android.provider.Settings.Global.getString(resolver, "always_on_display_constants");
            if (value == null || value.equals("null")) return false;
            return true;
        }
    }

    public static boolean isAODTapToShow(Context context) {
        if (Manufacturer.isSamsung()) {
            ContentResolver resolver = context.getContentResolver();
            return isAODEnabled(context) && (android.provider.Settings.System.getInt(resolver, "aod_tap_to_show_mode", 0) > 0);
        }
        return false;
    }

    public static String getAODThemePackage(Context context) {
        if (Manufacturer.isSamsung()) {
            ContentResolver resolver = context.getContentResolver();
            return android.provider.Settings.System.getString(resolver, "current_sec_aod_theme_package");
        }
        return null;
    }

    public static int[] getAODSchedule(Context context) {
        if (Manufacturer.isSamsung()) {
            ContentResolver resolver = context.getContentResolver();
            int start = android.provider.Settings.System.getInt(resolver, "aod_mode_start_time", 0);
            int end = android.provider.Settings.System.getInt(resolver, "aod_mode_end_time", 0);
            if (start != end) return new int[] { start, end };
        }
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
    
    @SuppressWarnings("deprecation")
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
