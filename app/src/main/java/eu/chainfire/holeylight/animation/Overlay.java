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
import android.app.KeyguardManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.PixelFormat;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.view.Gravity;
import android.view.WindowManager;

import eu.chainfire.holeylight.BuildConfig;
import eu.chainfire.holeylight.misc.Battery;
import eu.chainfire.holeylight.misc.Display;
import eu.chainfire.holeylight.misc.Settings;
import eu.chainfire.holeylight.service.AccessibilityService;

import static android.content.Context.KEYGUARD_SERVICE;

@SuppressWarnings({"WeakerAccess", "unused", "FieldCanBeLocal"})
public class Overlay {
    private static Overlay instance;
    public static Overlay getInstance(Context context) {
        return getInstance(context, null);
    }
    public static Overlay getInstance(Context context, IBinder windowToken) {
        synchronized (Overlay.class) {
            if (instance == null) {
                instance = new Overlay(context);
            }
            if (context instanceof AccessibilityService) {
                instance.initActualOverlay(context, windowToken);
            }
            return instance;
        }
    }

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction() == null) return;

            switch (intent.getAction()) {
                case Intent.ACTION_CONFIGURATION_CHANGED:
                    if (added) {
                        updateParams();
                    }
                    break;
                case Intent.ACTION_SCREEN_ON:
                case Intent.ACTION_USER_PRESENT:
                case Intent.ACTION_SCREEN_OFF:
                case Intent.ACTION_POWER_CONNECTED:
                case Intent.ACTION_POWER_DISCONNECTED:
                    evaluate();
                    break;
            }
        }
    };

    private final WindowManager windowManager;
    private final KeyguardManager keyguardManager;
    private final Handler handler;
    private final Settings settings;

    private SpritePlayer spritePlayer;

    private NotificationAnimation animation;

    private int[] colors = new int[0];
    private boolean wanted = false;
    private boolean kill = false;
    private boolean visible = false;
    private boolean lastState = false;
    private int[] lastColors = new int[0];
    private SpritePlayer.Mode lastMode = SpritePlayer.Mode.SWIRL;
    private boolean added = false;

    private Overlay(Context context) {
        windowManager = (WindowManager)context.getSystemService(Activity.WINDOW_SERVICE);
        keyguardManager = (KeyguardManager)context.getSystemService(KEYGUARD_SERVICE);
        handler = new Handler();
        settings = Settings.getInstance(context);
    }

    private void initActualOverlay(Context context, IBinder windowToken) {
        synchronized (this) {
            if (spritePlayer != null) return;

            spritePlayer = new SpritePlayer(context);

            initParams(windowToken);
            animation = new NotificationAnimation(context, spritePlayer, new NotificationAnimation.OnNotificationAnimationListener() {
                private PowerManager.WakeLock wakeLock = null;
                private int skips = 0;

                @Override
                public void onDimensionsApplied(SpritePlayer view) {
                    if (added) {
                        windowManager.updateViewLayout(view, view.getLayoutParams());
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
                            // This allows us to update the screen while in doze mode. Both
                            // according to the docs and what I've read from AOSP code say this
                            // isn't possible because we don't have the right permissions,
                            // nevertheless, it seems to work on the S10.
                            if (wakeLock == null) {
                                wakeLock = ((PowerManager)spritePlayer.getContext().getSystemService(Context.POWER_SERVICE)).newWakeLock(0x00000080 | 0x40000000, BuildConfig.APPLICATION_ID + ":draw"); /* DRAW_WAKE_LOCK | UNIMPORTANT_FOR_LOGGING */
                                wakeLock.setReferenceCounted(false);
                            }
                            try {
                                wakeLock.acquire(250);
                            } catch (Throwable t) {
                                t.printStackTrace();
                            }
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

            spritePlayer.getContext().getApplicationContext().registerReceiver(broadcastReceiver, intentFilter);
        }
        evaluate();
    }

    @Override
    protected void finalize() throws Throwable {
        if (spritePlayer != null) {
            spritePlayer.getContext().getApplicationContext().unregisterReceiver(broadcastReceiver);
        }
        super.finalize();
    }

    @SuppressLint("RtlHardcoded")
    private void initParams(IBinder windowToken) {
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                0,
                0,
                0,
                0,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                      WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                    | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                    | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                    | WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION
                    | WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS
                    | WindowManager.LayoutParams.FLAG_FULLSCREEN
                , PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.LEFT | Gravity.TOP;
        params.setTitle("HoleyLight");
        params.token = windowToken;
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

    private Runnable evaluateLoop = new Runnable() {
        @Override
        public void run() {
            evaluate();
            if (wanted) handler.postDelayed(this, 1000);
        }
    };

    private void evaluate() {
        if (spritePlayer == null) return;

        Context context = spritePlayer.getContext();

        visible = Display.isOn(context, true);
        boolean doze = Display.isDoze(context);
        if (visible && !doze && keyguardManager.isKeyguardLocked()) {
            visible = visible && settings.isEnabledOnLockscreen();
        }
        if (wanted && visible && (colors.length > 0)) {
            SpritePlayer.Mode mode = settings.getAnimationMode(settings.getMode(Battery.isCharging(context), visible && !doze));
            if (!lastState || colorsChanged() || mode != lastMode) {
                spritePlayer.setMode(mode);
                createOverlay();
                animation.play(colors, false, (mode != lastMode));
                lastColors = colors;
                lastState = true;
                lastMode = mode;
            }
        } else {
            if (lastState) {
                if (animation.isPlaying()) {
                    boolean immediately = !visible || kill;
                    animation.stop(immediately);
                    if (immediately) removeOverlay();
                }
                lastState = false;
            }
        }
    }

    public void show(int[] colors) {
        handler.removeCallbacks(evaluateLoop);
        this.colors = colors;
        if ((colors == null) || (colors.length == 0)) {
            wanted = false;
        } else {
            wanted = true;
            handler.postDelayed(evaluateLoop, 1000);
        }
        evaluate();
    }

    public void hide(boolean immediately) {
        handler.removeCallbacks(evaluateLoop);
        wanted = false;
        kill = immediately;
        evaluate();
    }

    public boolean isVisible() {
        return visible;
    }
}
