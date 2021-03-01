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

package eu.chainfire.holeylight.service;

import android.app.KeyguardManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationChannelGroup;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.database.ContentObserver;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Process;
import android.os.UserHandle;
import android.service.notification.StatusBarNotification;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import eu.chainfire.holeylight.BuildConfig;
import eu.chainfire.holeylight.animation.Overlay;
import eu.chainfire.holeylight.misc.AODControl;
import eu.chainfire.holeylight.misc.Battery;
import eu.chainfire.holeylight.misc.ColorAnalyzer;
import eu.chainfire.holeylight.misc.Display;
import eu.chainfire.holeylight.misc.LocaleHelper;
import eu.chainfire.holeylight.misc.MotionSensor;
import eu.chainfire.holeylight.misc.ResolutionTracker;
import eu.chainfire.holeylight.misc.Settings;
import eu.chainfire.holeylight.misc.Slog;

@SuppressWarnings({ "WeakerAccess", "unused" })
public class NotificationListenerService extends android.service.notification.NotificationListenerService implements Settings.OnSettingsChangedListener {
    public static volatile Integer test_lastColorCount = null;

    private static NotificationListenerService instance = null;
    public static NotificationListenerService getInstance() {
        return instance;
    }
    public static void checkNotifications() {
        if (instance != null) {
            instance.handleLEDNotifications();
        }
    }

    private ContentObserver refreshLEDObserver = null;
    private ContentObserver refreshLEDObserverSlow = null;

    @SuppressWarnings("unused")
    public static class ActiveNotification {
        private static final Map<String, Drawable> drawableCache = new HashMap<>();
        public static void clearCache() {
            drawableCache.clear();
        }

        private final String key;
        private final String packageName;
        private final String channelName;
        private final CharSequence tickerText;
        private boolean conversation = false;
        private int color = 0;
        private Icon icon = null;

        public ActiveNotification(String key, String packageName, String channelName, CharSequence tickerText) {
            this.key = key;
            this.packageName = packageName;
            this.channelName = channelName;
            this.tickerText = tickerText;
        }

        public String toCompare() {
            return toCompare(false);
        }

        public String toCompare(boolean withColorAndConversation) {
            if (withColorAndConversation) {
                return packageName + "::" + channelName + "::" + color + "::" + (conversation ? "conv" : "noconv");
            } else {
                return packageName + "::" + channelName;
            }
        }

        public String getKey() {
            return key;
        }

        public String getPackageName() {
            return packageName;
        }

        public String getChannelName() {
            return channelName;
        }

        public CharSequence getTickerText() {
            return tickerText;
        }

        public boolean isVisible() {
            return (color & 0xFFFFFF) != 0;
        }

        public int getColor() {
            return color;
        }

        public void setColor(int color) {
            this.color = color;
        }

        public boolean getConversation() {
            return conversation;
        }

        public void setConversation(boolean conversation) {
            this.conversation = conversation;
        }

        public Icon getIcon() {
            return icon;
        }

        public void setIcon(Icon icon) {
            this.icon = icon;
        }

        public String getIconId() {
            int id = 0;
            try {
                id = icon.getResId();
                if (id < 0) id = 0;
            } catch (Exception ignored) {
                // intentionally do nothing
            }
            if (id == 0) return null;
            return getPackageName() + ":" + id;
        }

        public Drawable getIconDrawable(Context context) {
            Drawable d = drawableCache.get(toCompare());
            if (d == null && icon != null) {
                try {
                    d = icon.loadDrawable(context);
                    drawableCache.put(toCompare(), d);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            return d;
        }
    }

    public synchronized List<ActiveNotification> getCurrentlyActiveNotifications() {
        return new ArrayList<>(activeNotifications);
    }

    private Settings settings = null;
    private int accessibilityServiceCounter = 0;
    private NotificationTracker tracker = null;
    private MotionSensor motionSensor = null;
    private KeyguardManager keyguardManager = null;
    private List<ActiveNotification> currentNotifications = new ArrayList<>();
    private boolean enabled = true;
    private long settingsKey = 0L;
    private boolean isUserPresent = false;
    private MotionSensor.MotionState lastMotionState = MotionSensor.MotionState.UNKNOWN;
    private long stationary_for_ms = 0;
    private boolean connected = false;
    private Handler handler;
    private final List<ActiveNotification> activeNotifications = new ArrayList<>();
    private boolean forceRefresh = false;
    private int callState = TelephonyManager.CALL_STATE_IDLE;
    private ResolutionTracker resolutionTracker = null;

    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction() == null) return;

            log("Intent: %s", intent.getAction());

            switch (intent.getAction()) {
                case Intent.ACTION_SCREEN_OFF:
                    isUserPresent = false;
                    break;
                case Intent.ACTION_SCREEN_ON:
                    if (keyguardManager.isKeyguardLocked()) {
                        onLockscreen();
                    }
                    break;
                case Intent.ACTION_USER_PRESENT:
                    isUserPresent = true;
                    onUserPresent();
                    break;
            }
        }
    };
    private IntentFilter intentFilter = null;

    private final PhoneStateListener phoneStateListener = new PhoneStateListener() {
        @Override
        public void onCallStateChanged(int state, String phoneNumber) {
            log("Call state --> %s", state == TelephonyManager.CALL_STATE_IDLE ? "IDLE" : "CALL");
            callState = state;
            forceRefresh = true;
            handleLEDNotifications();
        }
    };

    private void log(String fmt, Object... args) {
        Slog.d("Listener", fmt, args);
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(LocaleHelper.getContext(base));
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        LocaleHelper.updateResources(this);
        if (resolutionTracker.changed()) ActiveNotification.clearCache();
        forceRefresh = true;
        handleLEDNotifications(250);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        settings = Settings.getInstance(this);
        enabled = settings.isEnabled();
        motionSensor = MotionSensor.getInstance(this);
        keyguardManager = (KeyguardManager)getSystemService(KEYGUARD_SERVICE);

        handler = new Handler(Looper.getMainLooper());

        tracker = NotificationTracker.getInstance();

        callState = ((TelephonyManager)getSystemService(TELEPHONY_SERVICE)).getCallState();

        intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_SCREEN_ON);
        intentFilter.addAction(Intent.ACTION_SCREEN_OFF);
        intentFilter.addAction(Intent.ACTION_USER_PRESENT);
        intentFilter.setPriority(998);

        settings.registerOnSettingsChangedListener(this);

        refreshLEDObserver = new ContentObserver(handler) {
            @Override public boolean deliverSelfNotifications() { return true; }
            @Override public void onChange(boolean selfChange, Uri uri) { onChange(selfChange); }
            @Override
            public void onChange(boolean selfChange) {
                log("Force refresh");
                forceRefresh = true;
                handleLEDNotifications(100);
            }
        };

        refreshLEDObserverSlow = new ContentObserver(handler) {
            @Override public boolean deliverSelfNotifications() { return true; }
            @Override public void onChange(boolean selfChange, Uri uri) { onChange(selfChange); }
            @Override
            public void onChange(boolean selfChange) {
                log("Force refresh (slow)");
                forceRefresh = true;
                handleLEDNotifications(500);
            }
        };

        resolutionTracker = new ResolutionTracker("Listener", this);
    }

    @Override
    public void onDestroy() {
        settings.unregisterOnSettingsChangedListener(this);
        super.onDestroy();
    }

    @Override
    public void onSettingsChanged() {
        enabled = settings.isEnabled();
        long newKey = settings.refreshNotificationsKey();
        if (newKey != settingsKey) {
            settingsKey = newKey;
            apply();
        }
        int counter = settings.getUpdateCounter();
        if (counter != accessibilityServiceCounter) {
            apply();
        }
        accessibilityServiceCounter = counter;
        if (connected) handleLEDNotifications();
    }

    @Override
    public void onListenerConnected() {
        super.onListenerConnected();
        log("onListenerConnected");
        instance = this;
        connected = true;
        tracker.clear();
        isUserPresent = Display.isOn(this, false) && !keyguardManager.isKeyguardLocked();
        ((TelephonyManager)getSystemService(TELEPHONY_SERVICE)).listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
        registerReceiver(broadcastReceiver, intentFilter);
        handleLEDNotifications();
        startMotionSensor();
        getContentResolver().registerContentObserver(android.provider.Settings.Global.getUriFor("zen_mode"), false, refreshLEDObserver);
        getContentResolver().registerContentObserver(android.provider.Settings.Global.getUriFor("aod_show_state"), false, refreshLEDObserver);
        getContentResolver().registerContentObserver(android.provider.Settings.System.getUriFor("aod_show_state"), false, refreshLEDObserver); // moved!
        getContentResolver().registerContentObserver(android.provider.Settings.System.getUriFor("aod_mode"), false, refreshLEDObserverSlow);
        getContentResolver().registerContentObserver(android.provider.Settings.System.getUriFor("aod_mode_start_time"), false, refreshLEDObserverSlow);
        getContentResolver().registerContentObserver(android.provider.Settings.System.getUriFor("aod_mode_end_time"), false, refreshLEDObserverSlow);
        AODControl.setAODBrightness(this, true, result -> {});
    }

    @Override
    public void onListenerDisconnected() {
        log("onListenerDisconnected");
        instance = null;
        connected = false;
        getContentResolver().unregisterContentObserver(refreshLEDObserver);
        getContentResolver().unregisterContentObserver(refreshLEDObserverSlow);
        stopMotionSensor();
        unregisterReceiver(broadcastReceiver);
        ((TelephonyManager)getSystemService(TELEPHONY_SERVICE)).listen(phoneStateListener, PhoneStateListener.LISTEN_NONE);
        Overlay overlay = Overlay.getInstance();
        if (overlay != null) overlay.hide(true);
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

    private final Runnable runHandleLEDNotifications = this::handleLEDNotificationsInternal;

    private void handleLEDNotifications() {
        handleLEDNotifications(100);
    }

    private void handleLEDNotifications(int delayMillis) {
        // Prevent update storm caused by updates in rapid succession, and us updating settings ourselves
        handler.removeCallbacks(runHandleLEDNotifications);
        if (!connected) return;
        handler.postDelayed(runHandleLEDNotifications, delayMillis);
    }

    private String sanitizeChannelId(String channelId) {
        return channelId.replaceAll("[^a-zA-Z0-9_:.-]", "_");
    }

    private synchronized void handleLEDNotificationsInternal() {
        if (!connected) return;

        log("handleLEDNotifications");

        boolean screenOn = !Display.isDoze(this);
        int mode = settings.getMode(Battery.isCharging(this), screenOn);
        boolean dnd = settings.isRespectDoNotDisturb() && (android.provider.Settings.Global.getInt(getContentResolver(), "zen_mode", 0) > 0);
        boolean inAODSchedule = AODControl.inAODSchedule(this, true) || (!Display.isOff(this, false));
        int timeout = settings.getSeenTimeout(mode);

        activeNotifications.clear();

        try {
            StatusBarNotification[] sbns = tracker.prune(
                    getActiveNotifications(),
                    !Display.isOn(this, false) || !settings.isSeenIfScreenOn(true),
                    timeout,
                    !settings.isSeenTimeoutTrackSeparately() ? null : screenOn,
                    Display.isOn(this, false) // slightly different from screenOn
            );
            for (StatusBarNotification sbn : sbns) {
                Notification not = sbn.getNotification();

                int c = 0xFF000000;
                int cChan = c;
                boolean conversation = false;
                boolean bubble = false;
                boolean bubbleUnread = false;
                String groupName = null;
                String channelName = "legacy";

                Boolean shouldShowLights = null;

                if (not.getChannelId() != null) {
                    channelName = sanitizeChannelId(not.getChannelId());

                    List<NotificationChannel> chans = getNotificationChannels(sbn.getPackageName(), Process.myUserHandle());
                    for (NotificationChannel chan : chans) {
                        if (chan.getId().equals(not.getChannelId())) {
                            if (Build.VERSION.SDK_INT >= 29) {
                                bubble = not.getBubbleMetadata() != null;
                                if (bubble) {
                                    bubbleUnread |= !not.getBubbleMetadata().isNotificationSuppressed();
                                }
                            }
                            if (Build.VERSION.SDK_INT >= 30) {
                                for (NotificationChannel child : chans) {
                                    if (!child.getId().equals(chan.getId()) && child.getConversationId() != null && child.getParentChannelId().equals(chan.getId())) {
                                        log("CHILD %s --> #%08X [%s][%s]", child.getId(), child.getLightColor(), child.shouldShowLights() ? "Y" : "N", child.getGroup() == null ? "null" : child.getGroup());
                                        conversation = true;
                                    }
                                }
                            }
                            if (groupName == null) groupName = chan.getGroup();

                            if (chan.shouldShowLights() || conversation) {
                                shouldShowLights = true;
                                c = chan.getLightColor();
                                cChan = c;

                                // Twitter passes black for some reason, make white
                                if ((c & 0xFFFFFF) == 0) c = 0xFFFFFF;

                                // There's a lot of white notifications, try using the notification accent color instead
                                if (
                                        (
                                                ((c & 0xFFFFFF) == 0xFFFFFF) && (
                                                        (((not.color & 0xFFFFFF) > 0) || conversation) &&
                                                        !sbn.getPackageName().equals(BuildConfig.APPLICATION_ID)
                                                )
                                        )
                                ) {
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

                                // Override colors by analyzing icon (unless system, or Holey Light itself)
                                c = ColorAnalyzer.analyze(this, sbn.getPackageName(), c);

                                // Make sure we have alpha
                                c = c | 0xFF000000;
                            } else {
                                if (shouldShowLights == null) {
                                    shouldShowLights = chan.shouldShowLights();
                                } else {
                                    shouldShowLights |= chan.shouldShowLights();
                                }
                            }
                        }
                    }
                }

                ActiveNotification actNot = new ActiveNotification(sbn.getKey(), sbn.getPackageName(), channelName, not.tickerText);
                activeNotifications.add(actNot);

                // Save to prefs, or get overridden value from prefs
                c = settings.getColorForPackageAndChannel(sbn.getPackageName(), channelName, conversation, c, ((cChan & 0x00FFFFFF) != 0x000000) || conversation);
                settings.setColorForPackageAndChannel(sbn.getPackageName(), channelName, conversation, c, true);

                // Respect notification color being black? We normally don't want this as a lot of
                // notifications that we do want to show would disappear, but sometimes the same
                // channel is used with and without color
                if (settings.isRespectNotificationColorStateForPackageAndChannel(sbn.getPackageName(), channelName)) {
                    if (((not.color & 0xFFFFFF) == 0) || (shouldShowLights != null && !shouldShowLights)) {
                        c = 0;
                    }
                }

                // Only light up with unread bubbles. Detected by seeing if the notification is suppressed (== read)
                if (bubble && !bubbleUnread) {
                    c = 0;
                }

                // Ignore auto-groups
                if (not.getGroup() != null && not.getGroup().equals("ranker_group")) {
                    c = 0;
                }

                // Make sure we have alpha (again)
                c = c | 0xFF000000;

                // user has set notification to full black, skip
                log("%s [%s] (%s) --> #%08X / #%08X --> #%08X [%s][%s][%s][%s][%s][%s]", sbn.getKey(), sbn.getPackageName(), channelName, cChan, not.color, c, not.getSmallIcon() != null ? "I" : "x", shouldShowLights == null ? "x" : (shouldShowLights ? "Y" : "N"), conversation ? "C" : "x", bubble ? (bubbleUnread ? "U" : "B") : "x", not.getGroup() == null ? "null" : not.getGroup(), groupName == null ? "null" : groupName);
                if ((c & 0xFFFFFF) == 0) {
                    continue;
                }

                // save
                actNot.setColor(c);
                actNot.setConversation(conversation);
                actNot.setIcon(not.getSmallIcon());
            }
        } catch (SecurityException e) {
            // CompanionDeviceManager.getAssociations().size() == 0
        }

        List<ActiveNotification> visibleNotifications = new ArrayList<>();
        if (!dnd && inAODSchedule) {
            for (ActiveNotification not : activeNotifications) {
                if (not.isVisible()) {
                    boolean found = false;
                    for (ActiveNotification vis : visibleNotifications) {
                        if (not.toCompare().equals(vis.toCompare()) || (
                                not.getIconId() != null &&
                                not.getIconId().equals(vis.getIconId()) &&
                                not.color == vis.color
                        )) {
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        visibleNotifications.add(not);
                    }
                }
            }
        }
        visibleNotifications.sort((o1, o2) -> Integer.compare(o1.color & 0xFFFFFF, o2.color & 0xFFFFFF));

        boolean changes = (visibleNotifications.size() != currentNotifications.size());
        if (!changes) {
            for (int i = 0; i < currentNotifications.size(); i++) {
                if (!currentNotifications.get(i).toCompare(true).equals(visibleNotifications.get(i).toCompare(true))) {
                    changes = true;
                    break;
                }
            }
        }
        if (changes || forceRefresh) {
            currentNotifications = visibleNotifications;
            motionSensor.resetDuration();
            apply();
        }
        if ((currentNotifications.size() > 0) && (timeout > 0)) {
            handler.postDelayed(this::handleLEDNotifications, timeout);
        }
        AODControl.setAODAlarm(this);
    }

    private void apply() {
        if (!connected) return;
        Overlay overlay = Overlay.getInstance();
        if (enabled && callState == TelephonyManager.CALL_STATE_IDLE) {
            int[] currentColors = new int[currentNotifications.size()];
            Drawable[] currentIcons = new Drawable[currentNotifications.size()];
            for (int i = 0; i < currentNotifications.size(); i++) {
                currentColors[i] = currentNotifications.get(i).getColor();
                currentIcons[i] = currentNotifications.get(i).getIconDrawable(this);
            }
            if (overlay != null) {
                overlay.show(currentColors, currentIcons, forceRefresh);
                forceRefresh = false;
                test_lastColorCount = currentColors.length;
            }
        } else {
            if (overlay != null) {
                overlay.hide(true);
                forceRefresh = false;
                test_lastColorCount = 0;
            }
        }
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
        return settings.isSeenPickupWhile(settings.getMode(Battery.isCharging(this), isUserPresent), true);
    }

    private final MotionSensor.OnMotionStateListener onMotionStateListener = (motionState, for_millis) -> {
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
        return (enabled && (currentNotifications.size() > 0) && canMarkAsReadFromPickup());
    }
}
