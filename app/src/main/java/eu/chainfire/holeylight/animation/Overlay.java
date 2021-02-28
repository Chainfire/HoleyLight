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

package eu.chainfire.holeylight.animation;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.KeyguardManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.os.SystemClock;
import android.view.Gravity;
import android.view.WindowManager;

import eu.chainfire.holeylight.BuildConfig;
import eu.chainfire.holeylight.misc.AODControl;
import eu.chainfire.holeylight.misc.Battery;
import eu.chainfire.holeylight.misc.Display;
import eu.chainfire.holeylight.misc.ResolutionTracker;
import eu.chainfire.holeylight.misc.Settings;
import eu.chainfire.holeylight.misc.Slog;
import eu.chainfire.holeylight.service.AccessibilityService;
import eu.chainfire.holeylight.service.NotificationTracker;
import eu.chainfire.holeylight.ui.DetectCutoutActivity;

import static android.content.Context.KEYGUARD_SERVICE;
import static android.content.Context.POWER_SERVICE;

@SuppressWarnings({"WeakerAccess", "unused", "FieldCanBeLocal"})
public class Overlay {
    public static volatile Boolean test_lastVisible = null;

    private static Overlay instance;
    public static Overlay getInstance() {
        return getInstance(null, null);
    }
    public static Overlay getInstance(Context context) {
        return getInstance(context, null);
    }
    public static Overlay getInstance(Context context, IBinder windowToken) {
        synchronized (Overlay.class) {
            if (context instanceof AccessibilityService) {
                if (instance == null) instance = new Overlay(context);
                instance.initActualOverlay(context, windowToken);
            }
            return instance;
        }
    }

    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction() == null) return;

            Slog.d("Broadcast", "Intent: %s", intent.getAction());

            switch (intent.getAction()) {
                case BuildConfig.APPLICATION_ID + ".ACTION_CONFIGURATION_CHANGED":
                case Intent.ACTION_CONFIGURATION_CHANGED:
                    boolean force = !intent.getAction().equals(Intent.ACTION_CONFIGURATION_CHANGED);
                    if (resolutionTracker.changed() || force) {
                        if (Build.VERSION.SDK_INT < 29) {
                            // Resolution changed
                            // This is an extremely ugly hack, don't try this at home
                            // There are some internal states that are hard to figure out, including
                            // oddities with Lottie's renderer. We just hard exit and let Android
                            // restart us. This can certainly cause issues, hence using the
                            // shutdown() call on Android 10+. There's no way for me to extensively
                            // test this on Android 9 though, hence the API level split.
                            Intent start = new Intent(context, DetectCutoutActivity.class);
                            start.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            context.startActivity(start);
                            handler.postDelayed(() -> {
                                Context context1 = spritePlayer.getContext();
                                Intent intent1 = new Intent(context1, DetectCutoutActivity.class);
                                intent1.putExtra(BuildConfig.APPLICATION_ID + "/notifications", NotificationTracker.getInstance().saveToBytes());
                                AlarmManager alarmManager = (AlarmManager) context1.getSystemService(Service.ALARM_SERVICE);
                                alarmManager.setExactAndAllowWhileIdle(
                                        AlarmManager.ELAPSED_REALTIME,
                                        SystemClock.elapsedRealtime() + 1000,
                                        PendingIntent.getActivity(
                                                context1,
                                                0,
                                                intent1,
                                                0
                                        )
                                );
                                System.exit(0);
                            }, 1000);
                        } else {
                            // AccessibilityService/NotifificationListenerService should create a
                            // new overlay with updated resources/configuration/etc as needed
                            // within a few seconds
                            shutdown();
                        }
                    } else {
                        updateParams();
                    }
                    break;
                case Intent.ACTION_SCREEN_ON:
                    int linger = settings.getOverlayLinger();
                    if (linger == 0) {
                        removeTSP.run();
                    } else if (handler != null) {
                        // just in case evaluate doesn't run
                        handler.postDelayed(removeTSP, linger + 250);
                    }
                    evaluate(true);
                    break;
                case Intent.ACTION_USER_PRESENT:
                case Intent.ACTION_POWER_CONNECTED:
                case Intent.ACTION_POWER_DISCONNECTED:
                    evaluate(true);
                    break;
                case Intent.ACTION_SCREEN_OFF:
                    if (settings.isHideAOD()) {
                        // without AOD we might immediately go to sleep, give us some time to setup
                        pokeWakeLocks(10000);
                    }
                    evaluate(true);
                    break;
            }
        }
    };

    private final WindowManager windowManager;
    private final KeyguardManager keyguardManager;
    private final PowerManager.WakeLock cpuWakeLock;
    private final PowerManager.WakeLock drawWakeLock;
    private final Handler handler;
    private final Settings settings;

    private SpritePlayer spritePlayer;

    private NotificationAnimation animation;

    private int[] colors = new int[0];
    private Drawable[] icons = new Drawable[0];
    private boolean wanted = false;
    private boolean kill = false;
    private boolean forceRefresh = false;
    private boolean lastState = false;
    private int[] lastColors = new int[0];
    private SpritePlayer.Mode lastMode = SpritePlayer.Mode.SWIRL;
    private boolean lastBlackFill = false;
    private boolean lastDoze = false;
    private boolean lastHideAOD = false;
    private boolean lastWantAOD = false;
    private boolean added = false;
    private final ResolutionTracker resolutionTracker;
    private IBinder windowToken;
    private long lastVisibleTime;
    private volatile boolean terminated = false;

    private Overlay(Context context) {
        windowManager = (WindowManager)context.getSystemService(Activity.WINDOW_SERVICE);
        keyguardManager = (KeyguardManager)context.getSystemService(KEYGUARD_SERVICE);
        cpuWakeLock = ((PowerManager)context.getSystemService(POWER_SERVICE)).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, BuildConfig.APPLICATION_ID + ":cpu");
        drawWakeLock = ((PowerManager)context.getSystemService(POWER_SERVICE)).newWakeLock(0x00000080 | 0x40000000, BuildConfig.APPLICATION_ID + ":draw"); /* DRAW_WAKE_LOCK | UNIMPORTANT_FOR_LOGGING */
        handler = new Handler(Looper.getMainLooper());
        settings = Settings.getInstance(context);
        resolutionTracker = new ResolutionTracker("Overlay", context);
    }

    @SuppressWarnings("all")
    private void log(String fmt, Object... args) {
        Slog.d("Overlay", fmt, args);
    }

    private void pokeWakeLocks(int timeout_ms) {
        Slog.d("WakeLock", "%d", timeout_ms);

        cpuWakeLock.acquire(timeout_ms);

        // This allows us to update the screen while in doze mode. Both
        // according to the docs and what I've read from AOSP code say this
        // isn't possible because we don't have the right permissions,
        // nevertheless, it seems to work on the S10.
        drawWakeLock.acquire(timeout_ms);
    }

    private void initActualOverlay(Context context, IBinder windowToken) {
        synchronized (this) {
            if (this.windowToken == null && windowToken != null) this.windowToken = windowToken;
            if (spritePlayer != null) return;

            spritePlayer = new SpritePlayer(context);

            initParams();
            animation = new NotificationAnimation(context, spritePlayer, resolutionTracker.getDensityMultiplier(), new NotificationAnimation.OnNotificationAnimationListener() {
                private int skips = 0;

                @Override
                public void onDimensionsApplied(SpritePlayer view) {
                    if (added) {
                        try {
                            //TODO remove/add adjusts view layout better more consistently, but flickers?
                            windowManager.updateViewLayout(view, view.getLayoutParams());
                        } catch (IllegalArgumentException e) {
                            //TODO figure out why this happens
                            e.printStackTrace();
                        }
                    }
                }

                @Override
                public boolean onAnimationFrameStart(SpritePlayer view, boolean draw) {
                    if (draw) skips = 0;
                    if (Display.isDoze(spritePlayer.getContext())) {
                        if (!draw) {
                            // If we were to do slow drawing, we would have to poke
                            // WindowManager, by adjusting the x/y/width/height of the root view
                            // and using WindowManager::updateViewLayout.
                            // Otherwise, when we're in *doze* and on *battery* power, our overlay
                            // would disappear (as it's not actually part of the screen maintained
                            // content) unless the screen is being touched (aod-on-tap).
                            // It would be better to poke Android's internal draw wakelock
                            // instead, but there doesn't appear to be a way to reach this code
                            // from userspace. pokeDrawLock() calls that should exist according
                            // to AOSP do not appear to be present on Samsung.
                            // Using updateViewLayout often enough that it would keep our
                            // overlay alive however, triggers about 50% (single-core) CPU usage
                            // in system_server. As such it is cheaper to waste some cycles and
                            // redraw our overlay regularly. From experimentation, 6 works here.
                            skips++;
                            if (skips == 6) {
                                skips = 0;
                                return true;
                            }

                            return false;
                        }
                    }
                    return draw;
                }

                @Override
                public void onAnimationFrameEnd(SpritePlayer view, boolean draw) {
                    if (Display.isDoze(spritePlayer.getContext())) {
                        if (draw) {
                            pokeWakeLocks(250);
                        }
                    }
                }

                @Override
                public boolean onAnimationComplete(SpritePlayer view) {
                    removeOverlay();
                    return false;
                }
            });

            IntentFilter intentFilter = new IntentFilter();
            if (BuildConfig.DEBUG) intentFilter.addAction(BuildConfig.APPLICATION_ID + ".ACTION_CONFIGURATION_CHANGED");
            intentFilter.addAction(Intent.ACTION_CONFIGURATION_CHANGED);
            intentFilter.addAction(Intent.ACTION_SCREEN_ON);
            intentFilter.addAction(Intent.ACTION_USER_PRESENT);
            intentFilter.addAction(Intent.ACTION_SCREEN_OFF);
            intentFilter.addAction(Intent.ACTION_POWER_CONNECTED);
            intentFilter.addAction(Intent.ACTION_POWER_DISCONNECTED);
            intentFilter.setPriority(999);

            spritePlayer.getContext().getApplicationContext().registerReceiver(broadcastReceiver, intentFilter);
        }
        evaluate(true);
    }

    @Override
    protected void finalize() throws Throwable {
        if (spritePlayer != null) {
            try {
                spritePlayer.getContext().getApplicationContext().unregisterReceiver(broadcastReceiver);
            } catch (Exception ignored) {
            }
        }
        super.finalize();
    }

    @SuppressLint("RtlHardcoded")
    private void initParams() {
        if (terminated) return;
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                0,
                0,
                0,
                0,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                      WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                    | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                    | WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION
                    | WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS
                    | WindowManager.LayoutParams.FLAG_FULLSCREEN
                , PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.LEFT | Gravity.TOP;
        params.setTitle("HoleyLight");
        if (windowToken != null) params.token = windowToken;
        try { // disable animation when we move/resize
            int currentFlags = (Integer)params.getClass().getField("privateFlags").get(params);
            params.getClass().getField("privateFlags").setInt(params, currentFlags | 0x00000040); /* PRIVATE_FLAG_NO_MOVE_ANIMATION */
            Slog.d("LayoutParams", "privateFlags set");
        } catch (Exception e) {
            Slog.e("LayoutParams", "Could not set privateFlags");
        }
        try {
            // begone "performDraw() was skipped by AOD_SHOW_STATE"
            params.getClass().getDeclaredMethod("semAddExtensionFlags", int.class).invoke(params, 0x40000);
            Slog.d("LayoutParams", "samsungFlags set");
        } catch (Exception e) {
            Slog.e("LayoutParams", "Could not set samsungFlags");
        }
        spritePlayer.setLayoutParams(params);
    }

    private void updateParams() {
        if (terminated) return;
        animation.applyDimensions();
    }

    private void createOverlay() {
        if (terminated) return;
        if (added) return;
        try {
            updateParams();
            added = true; // had a case of a weird exception that caused this to run in a loop if placed after addView
            windowManager.addView(spritePlayer, spritePlayer.getLayoutParams());
        } catch (Exception e) {
            e.printStackTrace();
            if (e.toString().contains("BadTokenException")) {
                // we will not recover from this without restart
                // unfortunately this may cause the AccessibilityService to get excessive delays
                // in processing until reboot or reinstall :/
                System.exit(0);
            }
        }
        test_lastVisible = added;
    }

    private void updateOverlay() {
        if (terminated) return;
        if (!added) return;
        try {
            updateParams();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void removeOverlay() {
        if (!added) return;
        try {
            windowManager.removeView(spritePlayer);
            added = false;
        } catch (Exception e) {
            e.printStackTrace();
        }
        test_lastVisible = added;
    }

    private boolean colorsChanged() {
        if ((lastColors == null) != (colors == null)) return true;
        if (lastColors == null) return false;
        if (lastColors.length != colors.length) return true;
        if (lastColors.length == 0) return false;
        for (int i = 0; i < lastColors.length; i++) {
            if (lastColors[i] != colors[i]) return true;
        }
        return false;
    }

    private final Runnable evaluateLoop = () -> evaluate(false);

    private boolean evaluateDelayedPosted = false;
    private final Runnable evaluateDelayed = () -> { evaluate(true, true); evaluateDelayedPosted = false; };

    private final Runnable removeTSP = () -> animation.updateTSPRect(new Rect(0, 0, 0, 0), null, 0);

    public void evaluate(boolean refreshAll) {
        evaluate(refreshAll, false);
    }

    public void evaluate(boolean refreshAll, boolean isDelayed) {
        if (terminated) return;

        if (spritePlayer == null) {
            if (wanted) handler.postDelayed(evaluateLoop, 500);
            return;
        }

        if (colors.length == 0) {
            wanted = false;
        }

        Context context = spritePlayer.getContext();

        boolean on = Display.isOn(context, false);
        boolean doze = Display.isDoze(context);
        boolean visible = on || doze;
        boolean inAODSchedule = AODControl.inAODSchedule(context, false);
        boolean haveColors = colors.length > 0;
        boolean allowHideAOD = settings.isEnabledWhileScreenOff();
        if (visible) {
            lastVisibleTime = SystemClock.elapsedRealtime();
        }
        if (!visible && allowHideAOD && settings.isHideAOD() && inAODSchedule) {
            // we will be visible soon
            visible = true;
            doze = true;
        }
        boolean lockscreen = on && keyguardManager.isKeyguardLocked();
        boolean charging = Battery.isCharging(context);

        int mode = settings.getMode(charging, !doze && (!spritePlayer.isTSPMode(lastMode) || on));
        int modeOff = settings.getMode(charging, false);
        SpritePlayer.Mode renderMode = settings.getAnimationMode(mode);
        SpritePlayer.Mode renderModeOff = settings.getAnimationMode(modeOff);

        boolean isHideAOD = allowHideAOD && (settings.isHideAOD() || spritePlayer.isTSPMode(renderMode));
        boolean isHideAODAndDoze = isHideAOD && doze;
        boolean isHideAODAndDozeOrOff = isHideAOD && (doze || !on);
        boolean isHideAODIfOff = allowHideAOD && (settings.isHideAOD() || spritePlayer.isTSPMode(renderModeOff));

        boolean activeHide = (!haveColors || !inAODSchedule) && isHideAODAndDozeOrOff;
        if (activeHide) renderMode = SpritePlayer.Mode.TSP_HIDE;

        boolean wantAOD = (haveColors || !isHideAODIfOff) && inAODSchedule;
        if (wantAOD != lastWantAOD) forceRefresh = true;

        int linger = settings.getOverlayLinger();
        if (linger > 0) {
            handler.removeCallbacks(removeTSP);
            if (!isDelayed && (spritePlayer.isTSPMode(lastMode) || (lastDoze && allowHideAOD && settings.isHideAOD())) && !(spritePlayer.isTSPMode(renderMode) || doze)) {
                log("Linger: %d ms", linger);
                animation.setShowAODClock(false, settings.isShowAODClock());
                animation.setHideAOD(true, true);
                handler.removeCallbacks(evaluateLoop);
                if (!evaluateDelayedPosted) {
                    handler.postDelayed(evaluateDelayed, linger);
                    evaluateDelayedPosted = true;
                }
                return;
            }
        }

        boolean lockscreenOk = !on || !lockscreen || settings.isEnabledOnLockscreen();
        boolean wantedEffective = (wanted || activeHide) && settings.isEnabledWhile(mode) && lockscreenOk;
        boolean hideAODEffective = activeHide || isHideAODAndDoze;

        if (forceRefresh) log("Force refresh");

        if (visible && wantedEffective && (haveColors || activeHide)) {
            boolean blackFill = !doze && settings.isBlackFill();
            if (!lastState || colorsChanged() || renderMode != lastMode || blackFill != lastBlackFill || doze != lastDoze || hideAODEffective != lastHideAOD || forceRefresh) {
                if (!wantAOD && AODControl.isAODEnabled(context) && settings.isAODHelperControl()) {
                    pokeWakeLocks(250); // this is why we check ^^^ explicitly
                    AODControl.setAODEnabled(spritePlayer.getContext(), wantAOD, null);
                }
                animation.setMode(renderMode, blackFill);
                createOverlay();
                animation.setShowAODClock(settings.isShowAODClock(), settings.isShowAODClock());
                animation.setHideAOD(hideAODEffective, settings.isHideAODFully());
                animation.setDoze(doze);
                animation.play(activeHide ? new int[] { Color.BLACK } : colors, settings.isUnholeyLightIcons() ? icons : new Drawable[0], false, (renderMode != lastMode));
                lastColors = colors;
                lastState = true;
                lastMode = renderMode;
                lastBlackFill = blackFill;
                lastDoze = doze;
                lastHideAOD = hideAODEffective;
                lastWantAOD = wantAOD;
                if (wantAOD) AODControl.setAODEnabled(spritePlayer.getContext(), wantAOD, null);
            }
        } else {
            if (lastState || forceRefresh) {
                final boolean fVisible = visible;
                final boolean fDoze = doze;
                final boolean fWantAOD = wantAOD;
                Runnable goAway = () -> {
                    if (lastState) {
                        boolean remove = false;
                        if (animation.isPlaying()) {
                            boolean immediately = !fVisible || kill || isDelayed || spritePlayer.isTSPMode(lastMode) || lastHideAOD || settings.isAODHelperControl();
                            animation.stop(immediately);
                            if (immediately) remove = true;
                        } else {
                            remove = true;
                        }
                        if (remove) {
                            removeOverlay();
                            if (spritePlayer.isTSPMode(lastMode) || lastHideAOD) {
                                animation.setHideAOD(false);
                                lastHideAOD = false;
                            }
                        }
                        lastState = false;
                        lastDoze = fDoze;
                        lastWantAOD = fWantAOD;
                    }
                };
                if (doze && !wantAOD) {
                    animation.setHideAOD(true, settings.isHideAODFully());
                    lastHideAOD = true;
                    AODControl.setAODEnabled(spritePlayer.getContext(), wantAOD, null);
                    pokeWakeLocks(500);
                    handler.postDelayed(goAway, 250);
                } else {
                    goAway.run();
                    AODControl.setAODEnabled(spritePlayer.getContext(), wantAOD, null);
                }
            }
        }

        handler.removeCallbacks(evaluateLoop);
        if (wantedEffective && ((doze && AODControl.isAODEnabled(context)) || on)) {
            handler.postDelayed(evaluateLoop, (spritePlayer.isTSPMode(renderMode) || colors.length == 0) ? 5000 : 500);
        }

        forceRefresh = false;
    }

    public void show(int[] colors, Drawable[] icons, boolean forceRefresh) {
        if (terminated) return;
        handler.removeCallbacks(evaluateLoop);
        this.colors = colors;
        this.icons = icons;
        wanted = true;
        kill = false;
        this.forceRefresh = forceRefresh;
        evaluate(true);
    }

    public void hide(boolean immediately) {
        if (terminated) return;
        handler.removeCallbacks(evaluateLoop);
        wanted = false;
        kill = immediately;
        evaluate(true);
    }

    public void updateTSPRect(Rect rect, Rect clockRect, int overlayBottom) {
        if (terminated) return;
        boolean apply = Display.isOff(spritePlayer.getContext(), true);
        Slog.d("AOD_TSP", "Overlay " + rect.toString() + " clock " + clockRect + " bottom:" + overlayBottom + " apply:" + apply);
        if (apply) {
            pokeWakeLocks(250);
            animation.updateTSPRect(rect, clockRect, overlayBottom);
        }
    }

    public void shutdown() {
        if (Build.VERSION.SDK_INT >= 29) {
            synchronized (Overlay.class) {
                instance = null;
                terminated = true;
            }
            if (animation.isPlaying()) animation.stop(true);
            removeOverlay();
            animation = null;
            spritePlayer.getContext().getApplicationContext().unregisterReceiver(broadcastReceiver);
            spritePlayer = null;
        }
    }
}
