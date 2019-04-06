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

package eu.chainfire.holeylight.service;

import android.app.KeyguardManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationChannelGroup;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.hardware.display.DisplayManager;
import android.os.Process;
import android.os.SystemClock;
import android.os.UserHandle;
import android.service.notification.StatusBarNotification;
import android.util.Log;
import android.view.Display;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import eu.chainfire.holeylight.BuildConfig;
import eu.chainfire.holeylight.misc.Battery;
import eu.chainfire.holeylight.misc.MotionSensor;
import eu.chainfire.holeylight.misc.Settings;
import eu.chainfire.holeylight.ui.LockscreenActivity;

public class NotificationListenerService extends android.service.notification.NotificationListenerService implements Settings.OnSettingsChangedListener {
    private Settings settings = null;
    private Overlay overlay = null;
    private NotificationTracker tracker = null;
    private MotionSensor motionSensor = null;
    private KeyguardManager keyguardManager = null;
    private int[] currentColors = new int[0];
    private boolean enabled = true;
    private Display display = null;
    private boolean isUserPresent = false;
    private MotionSensor.MotionState lastMotionState = MotionSensor.MotionState.UNKNOWN;
    private long stationary_for_ms = 0;

    private boolean isDisplayOn(boolean ifDoze) {
        int state = display.getState();
        if (state == Display.STATE_OFF) return false;
        if (state == Display.STATE_DOZE) return ifDoze;
        if (state == Display.STATE_DOZE_SUSPEND) return ifDoze;
        if (state == Display.STATE_ON_SUSPEND) return ifDoze;
        return true;
    }

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction() == null) return;

            log("Intent: %s", intent.getAction());

            if (
                    intent.getAction().equals(Intent.ACTION_SCREEN_OFF) ||
                    (intent.getAction().equals(Intent.ACTION_POWER_CONNECTED) && !isDisplayOn(false))
            ) {
                if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
                    isUserPresent = false;
                }

                if (settings.isEnabledWhileScreenOffCharging() && Battery.isCharging(NotificationListenerService.this)) {
                    // this is a really bad way to detect we didn't just press the power button while
                    // our LockscreenActivity was in the foreground
                    if (SystemClock.elapsedRealtime() - LockscreenActivity.lastVisible() > 2500) {
                        log("Showing lockscreen");
                        Intent i = new Intent(NotificationListenerService.this, LockscreenActivity.class);
                        i.setFlags(
                                Intent.FLAG_ACTIVITY_NEW_TASK |
                                Intent.FLAG_ACTIVITY_NO_HISTORY |
                                Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
                        );
                        i.putExtra(BuildConfig.APPLICATION_ID + ".colors", currentColors);
                        startActivity(i);
                    }
                }
            } else if (intent.getAction().equals(Intent.ACTION_USER_PRESENT)) {
                isUserPresent = true;
                onUserPresent();
            }
        }
    };
    private IntentFilter intentFilter = null;

    private BroadcastReceiver localBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction() == null) return;

            if (intent.getAction().equals(BuildConfig.APPLICATION_ID + ".onLockscreen")) {
                onLockscreen();
            }
        }
    };
    private IntentFilter localIntentFilter = null;

    private void log(String fmt, Object... args) {
        Log.d("HoleyLight/Listener", String.format(Locale.ENGLISH, fmt, args));
    }

    @Override
    public void onCreate() {
        super.onCreate();
        settings = Settings.getInstance(this);
        enabled = settings.isEnabled();
        overlay = Overlay.getInstance(this);
        motionSensor = MotionSensor.getInstance(this);
        keyguardManager = (KeyguardManager)getSystemService(KEYGUARD_SERVICE);

        display = ((DisplayManager)getSystemService(DISPLAY_SERVICE)).getDisplay(0);

        tracker = new NotificationTracker();

        intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_SCREEN_ON);
        intentFilter.addAction(Intent.ACTION_SCREEN_OFF);
        intentFilter.addAction(Intent.ACTION_USER_PRESENT);
        intentFilter.addAction(Intent.ACTION_POWER_CONNECTED);
        intentFilter.setPriority(998);

        localIntentFilter = new IntentFilter();
        localIntentFilter.addAction(BuildConfig.APPLICATION_ID + ".onLockscreen");

        settings.registerOnSettingsChangedListener(this);
    }

    @Override
    public void onDestroy() {
        settings.unregisterOnSettingsChangedListener(this);
        super.onDestroy();
    }

    @Override
    public void onSettingsChanged() {
        boolean newEnabled = settings.isEnabled();
        if (newEnabled != enabled) {
            enabled = newEnabled;
            apply();
        }
    }

    @Override
    public void onListenerConnected() {
        super.onListenerConnected();
        log("onListenerConnected");
        tracker.clear();
        isUserPresent = isDisplayOn(false) && !keyguardManager.isKeyguardLocked();
        registerReceiver(broadcastReceiver, intentFilter);
        LocalBroadcastManager.getInstance(this).registerReceiver(localBroadcastReceiver, localIntentFilter);
        handleLEDNotifications();
        startMotionSensor();
    }

    @Override
    public void onListenerDisconnected() {
        log("onListenerDisconnected");
        stopMotionSensor();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(localBroadcastReceiver);
        unregisterReceiver(broadcastReceiver);
        overlay.hide(true);
        tracker.clear();
        super.onListenerDisconnected();
    }

    @Override
    public void onInterruptionFilterChanged(int interruptionFilter) {
        super.onInterruptionFilterChanged(interruptionFilter);
        log("onInterruptionFilterChanged");
        handleLEDNotifications();
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        super.onNotificationPosted(sbn);
        log("onNotificationPosted");
        handleLEDNotifications();
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        super.onNotificationRemoved(sbn);
        log("onNotificationRemoved");
        handleLEDNotifications();
    }

    @Override
    public void onNotificationChannelGroupModified(String pkg, UserHandle user, NotificationChannelGroup group, int modificationType) {
        super.onNotificationChannelGroupModified(pkg, user, group, modificationType);
        log("onNotificationChannelGroupModified");
        handleLEDNotifications();
    }

    @Override
    public void onNotificationChannelModified(String pkg, UserHandle user, NotificationChannel channel, int modificationType) {
        super.onNotificationChannelModified(pkg, user, channel, modificationType);
        log("onNotificationChannelModified");
        handleLEDNotifications();
    }

    @Override
    public void onNotificationRankingUpdate(RankingMap rankingMap) {
        super.onNotificationRankingUpdate(rankingMap);
        log("onNotificationRankingUpdate");
        handleLEDNotifications();
    }

    private void handleLEDNotifications() {
        List<Integer> colors = new ArrayList<>();
        List<String> pkgs = new ArrayList<>();

        StatusBarNotification[] sbns = tracker.prune(getActiveNotifications());
        for (StatusBarNotification sbn : sbns) {
            try {
                Notification not = sbn.getNotification();
                if (not.getChannelId() != null) {
                    List<NotificationChannel> chans = getNotificationChannels(sbn.getPackageName(), Process.myUserHandle());
                    for (NotificationChannel chan : chans) {
                        if (chan.getId().equals(not.getChannelId())) {
                            if (chan.shouldShowLights()) {
                                int c = chan.getLightColor();

                                // Twitter passes black for some reason, make white
                                if ((c & 0xFFFFFF) == 0) c = 0xFFFFFF;

                                // There's a lot of white notifications, try using the notification accent color instead
                                if (((c & 0xFFFFFF) == 0xFFFFFF) && ((not.color & 0xFFFFFF) > 0) && !sbn.getPackageName().equals(BuildConfig.APPLICATION_ID)) {

                                    // Set dominant channel to max brightness
                                    int r = Color.red(not.color);
                                    int g = Color.green(not.color);
                                    int b = Color.blue(not.color);

                                    if ((r >= g) && (r >= b)) {
                                        r = 255;
                                    } else if ((g >= r) && (g >= b)) {
                                        g = 255;
                                    } else {
                                        b = 255;
                                    }

                                    c = Color.rgb(r, g, b);
                                }

                                // Make sure we have alpha
                                c = c | 0xFF000000;

                                // Save to prefs, or get overriden value from prefs
                                c = settings.getColorForPackage(sbn.getPackageName(), c);
                                settings.setColorForPackage(sbn.getPackageName(), c, true);

                                // user has set notification to full black, skip
                                if ((c & 0x00FFFFFF) == 0) {
                                    continue;
                                }

                                // Make sure we have alpha (again)
                                c = c | 0xFF000000;

                                // Log and save
                                Integer color = c;
                                log("%s --> #%08X / #%08X --> #%08X", sbn.getPackageName(), chan.getLightColor(), not.color, c);
                                if (!colors.contains(color)) {
                                    colors.add(color);
                                }
                                if (!pkgs.contains(sbn.getPackageName())) {
                                    pkgs.add(sbn.getPackageName());
                                }
                            }
                        }
                    }
                }
            } catch (SecurityException e) {
                // CompanionDeviceManager.getAssociations().size() == 0
            }
        }

        int[] sorted = new int[colors.size()];
        for (int i = 0; i < sorted.length; i++) {
            sorted[i] = colors.get(i);
        }
        Arrays.sort(sorted);

        boolean changes = (sorted.length != currentColors.length);
        if (!changes) {
            for (int i = 0; i < currentColors.length; i++) {
                if (currentColors[i] != sorted[i]) {
                    changes = true;
                    break;
                }
            }
        }
        if (changes) {
            currentColors = sorted;
            motionSensor.resetDuration();
            apply();
        }
    }

    private void apply() {
        if ((currentColors.length > 0) && (enabled)) {
            overlay.show(currentColors);
        } else {
            overlay.hide(!enabled);
        }
        //TODO do we need a (short) wakelock here?
        Intent intent = new Intent(BuildConfig.APPLICATION_ID + ".colors");
        intent.putExtra(BuildConfig.APPLICATION_ID + ".colors", currentColors);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
        startMotionSensor();
    }

    private void onLockscreen() {
        log("onLockscreen");
        if (settings.isSeenOnLockscreen(true)) {
            tracker.markAllAsSeen();
            handleLEDNotifications();
        }
    }

    private void onUserPresent() {
        log("onUserPresent");
        if (settings.isSeenOnUserPresent(true)) {
            tracker.markAllAsSeen();
            handleLEDNotifications();
        }
        startMotionSensor();
    }

    private boolean canMarkAsReadFromPickup() {
        boolean charging = Battery.isCharging(this);
        return
            ((isUserPresent) && (
                    (charging && settings.isSeenPickupScreenOnCharging(true)) ||
                    (!charging && settings.isSeenPickupScreenOnBattery(true))
            )) || ((!isUserPresent) && (
                    (charging && settings.isSeenPickupScreenOffCharging(true)) ||
                    (!charging && settings.isSeenPickupScreenOffBattery(true))
            ));
    }

    private MotionSensor.OnMotionStateListener onMotionStateListener = (motionState, for_millis) -> {
        if (motionState != lastMotionState) {
            log("onMovement --> " + motionState);
            lastMotionState = motionState;
        }

        if (motionState == MotionSensor.MotionState.STATIONARY) {
            stationary_for_ms = for_millis;
        } else {
            if ((stationary_for_ms >= 10000) && (motionState == MotionSensor.MotionState.MOVING)) {
                if (canMarkAsReadFromPickup()) {
                    log("onMovement: pickup");
                    //TODO screen must actually be visible AND not temp-off due to proximity here
                    tracker.markAllAsSeen();
                    handleLEDNotifications();
                }
            }
            stationary_for_ms = 0;
        }
        return wantMotionSensor();
    };

    private void startMotionSensor() {
        if (wantMotionSensor()) {
            motionSensor.start(onMotionStateListener);
        }
    }

    private void stopMotionSensor() {
        motionSensor.stop(onMotionStateListener);
    }

    private boolean wantMotionSensor() {
        return (enabled && (currentColors.length > 0) && canMarkAsReadFromPickup());
    }
}
