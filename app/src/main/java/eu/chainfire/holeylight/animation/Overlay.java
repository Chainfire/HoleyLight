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
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.os.SystemClock;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.WindowManager;

import eu.chainfire.holeylight.BuildConfig;
import eu.chainfire.holeylight.misc.AODControl;
import eu.chainfire.holeylight.misc.Battery;
import eu.chainfire.holeylight.misc.Display;
import eu.chainfire.holeylight.misc.Settings;
import eu.chainfire.holeylight.misc.Slog;
import eu.chainfire.holeylight.service.AccessibilityService;
import eu.chainfire.holeylight.ui.DetectCutoutActivity;

import static android.content.Context.KEYGUARD_SERVICE;
import static android.content.Context.POWER_SERVICE;

@SuppressWarnings({"WeakerAccess", "unused", "FieldCanBeLocal"})
public class Overlay {
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

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction() == null) return;

            log("Intent: %s", intent.getAction());

            switch (intent.getAction()) {
                case Intent.ACTION_CONFIGURATION_CHANGED:
                    Point resolutionNow = getResolution();
                    if (
                            ((resolutionNow.x != resolution.x) || (resolutionNow.y != resolution.y)) &&
                            ((resolutionNow.x != resolution.y) || (resolutionNow.y != resolution.x))
                    ) {
                        // Resolution changed
                        // This is an extremely ugly hack, don't try this at home
                        // There are some internal states that are hard to figure out, including
                        // oddities with Lottie's renderer. We just hard exit and let Android
                        // restart us.
                        resolution = resolutionNow;
                        Intent start = new Intent(context, DetectCutoutActivity.class);
                        start.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        context.startActivity(start);
                        handler.postDelayed(() -> {
                            Context context1 = spritePlayer.getContext();
                            AlarmManager alarmManager = (AlarmManager) context1.getSystemService(Service.ALARM_SERVICE);
                            alarmManager.setExactAndAllowWhileIdle(
                                    AlarmManager.ELAPSED_REALTIME,
                                    SystemClock.elapsedRealtime() + 1000,
                                    PendingIntent.getActivity(
                                            context1,
                                            0,
                                            new Intent(context1, DetectCutoutActivity.class),
                                            0
                                    )
                            );
                            System.exit(0);
                        }, 1000);
                    } else {
                        updateParams();
                    }
                    break;
                case Intent.ACTION_SCREEN_ON:
                    animation.updateTSPRect(new Rect(0, 0, 0, 0));
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
    private boolean lastState = false;
    private int[] lastColors = new int[0];
    private SpritePlayer.Mode lastMode = SpritePlayer.Mode.SWIRL;
    private int lastDpAdd = 0;
    private boolean added = false;
    private Point resolution;
    private IBinder windowToken;
    private long lastVisibleTime;

    private Overlay(Context context) {
        windowManager = (WindowManager)context.getSystemService(Activity.WINDOW_SERVICE);
        keyguardManager = (KeyguardManager)context.getSystemService(KEYGUARD_SERVICE);
        cpuWakeLock = ((PowerManager)context.getSystemService(POWER_SERVICE)).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, BuildConfig.APPLICATION_ID + ":cpu");
        drawWakeLock = ((PowerManager)context.getSystemService(POWER_SERVICE)).newWakeLock(0x00000080 | 0x40000000, BuildConfig.APPLICATION_ID + ":draw"); /* DRAW_WAKE_LOCK | UNIMPORTANT_FOR_LOGGING */
        handler = new Handler(Looper.getMainLooper());
        settings = Settings.getInstance(context);
        resolution = getResolution();
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

    private Point getResolution() {
        DisplayMetrics metrics = new DisplayMetrics();
        windowManager.getDefaultDisplay().getRealMetrics(metrics);
        return new Point(metrics.widthPixels, metrics.heightPixels);
    }

    private void initActualOverlay(Context context, IBinder windowToken) {
        synchronized (this) {
            if (this.windowToken == null && windowToken != null) this.windowToken = windowToken;
            if (spritePlayer != null) return;

            spritePlayer = new SpritePlayer(context);

            initParams();
            animation = new NotificationAnimation(context, spritePlayer, new NotificationAnimation.OnNotificationAnimationListener() {
                private int skips = 0;

                @Override
                public void onDimensionsApplied(SpritePlayer view) {
                    if (added) {
                        try {
                            //TODO remove/add adjusts view layout better more consistently, but flickers?
                            //TODO maybe loop this to fix Unholey Light location sometimes not updating?
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
                    return true;
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
            spritePlayer.getContext().getApplicationContext().unregisterReceiver(broadcastReceiver);
        }
        super.finalize();
    }

    @SuppressLint("RtlHardcoded")
    private void initParams() {
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
            params.getClass().getField("privateFlags").set(params, currentFlags | 0x00000040); /* PRIVATE_FLAG_NO_MOVE_ANIMATION */
        } catch (Exception e) {
            //do nothing. Probably using other version of android
        }
        spritePlayer.setLayoutParams(params);
    }

    private void updateParams() {
        animation.applyDimensions();
    }

    private void createOverlay() {
        if (added) return;
        try {
            updateParams();
            added = true; // had a case of a weird exception that caused this to run in a loop if placed after addView
            windowManager.addView(spritePlayer, spritePlayer.getLayoutParams());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateOverlay() {
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

    private Runnable evaluateLoop = () -> evaluate(false);

    public void evaluate(boolean refreshAll) {
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
        if (visible) {
            lastVisibleTime = SystemClock.elapsedRealtime();
        }
        boolean allowHideAOD = settings.isEnabledWhileScreenOff();
        if (!visible && settings.isHideAOD() && allowHideAOD && AODControl.inAODSchedule(context, false)) {
            // we will be visible soon
            visible = true;
            doze = true;
        }
        boolean lockscreen = on && keyguardManager.isKeyguardLocked();
        boolean charging = Battery.isCharging(context);

        int mode = settings.getMode(charging, !doze);
        SpritePlayer.Mode renderMode = settings.getAnimationMode(mode);

        // We don't have the helper package that properly turns off AOD (passive hide) when we want
        // to hide it, but we still want AOD to be invisible: active hide
        boolean activeHide = (colors.length == 0) && doze && allowHideAOD && (settings.isHideAOD() || spritePlayer.isTSPMode(renderMode)) && !AODControl.haveHelperPackage(context, refreshAll);
        if (activeHide) {
            renderMode = SpritePlayer.Mode.TSP_HIDE;
        }

        boolean lockscreenOk = !on || !lockscreen || settings.isEnabledOnLockscreen();
        boolean wantedEffective = (wanted || activeHide) && settings.isEnabledWhile(mode) && lockscreenOk;

        if (visible && wantedEffective && ((colors.length > 0) || activeHide)) {
            int dpAdd = (doze ? 1 : 0);
            if (!lastState || colorsChanged() || renderMode != lastMode || (dpAdd != lastDpAdd)) {
                animation.setMode(renderMode);
                createOverlay();
                if (settings.isHideAOD() && doze && allowHideAOD) {
                    animation.setHideAOD(true, settings.isHideAODFully());
                    AODControl.setAOD(spritePlayer.getContext(), true);
                } else {
                    animation.setHideAOD(spritePlayer.isTSPMode(renderMode), settings.isHideAODFully());
                }
                animation.setDpAdd(dpAdd);
                animation.play(activeHide ? new int[] { Color.BLACK } : colors, settings.isUnholeyLightIcons() ? icons : new Drawable[0], false, (renderMode != lastMode));
                lastColors = colors;
                lastState = true;
                lastMode = renderMode;
                lastDpAdd = dpAdd;
            }
        } else {
            if (lastState) {
                if (spritePlayer.isTSPMode(lastMode)) {
                    animation.setHideAOD(false);
                }
                if (settings.isHideAOD()) {
                    animation.setHideAOD(false);
                    AODControl.setAOD(spritePlayer.getContext(), false);
                }
                if (animation.isPlaying()) {
                    boolean immediately = !visible || kill;
                    animation.stop(immediately);
                    if (immediately) removeOverlay();
                }
                lastState = false;
            }
        }

        if (wantedEffective) handler.postDelayed(evaluateLoop, 500);
    }

    public void show(int[] colors, Drawable[] icons) {
        handler.removeCallbacks(evaluateLoop);
        this.colors = colors;
        this.icons = icons;
        wanted = true;
        kill = false;
        evaluate(true);
    }

    public void hide(boolean immediately) {
        handler.removeCallbacks(evaluateLoop);
        wanted = false;
        kill = immediately;
        evaluate(true);
    }

    public void updateTSPRect(Rect rect) {
        boolean apply = Display.isOff(spritePlayer.getContext(), true);
        Slog.d("AOD_TSP", "Overlay " + rect.toString() + " apply:" + String.valueOf(apply));
        if (apply) {
            pokeWakeLocks(250);
            animation.updateTSPRect(rect);
        }
    }
}
