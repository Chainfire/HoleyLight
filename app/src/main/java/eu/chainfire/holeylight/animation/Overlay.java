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
import android.hardware.display.DisplayManager;
import android.os.Handler;
import android.view.Display;
import android.view.Gravity;
import android.view.WindowManager;

import eu.chainfire.holeylight.misc.Battery;
import eu.chainfire.holeylight.misc.Settings;

import static android.content.Context.DISPLAY_SERVICE;
import static android.content.Context.KEYGUARD_SERVICE;

@SuppressWarnings({"WeakerAccess", "unused"})
public class Overlay {
    private static Overlay instance;
    public static Overlay getInstance(Context context) {
        synchronized (Overlay.class) {
            if (instance == null) {
                instance = new Overlay(context);
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
    private final SpritePlayer spritePlayer;
    private final NotificationAnimation animation;
    private final KeyguardManager keyguardManager;
    private final Display display;
    private final Handler handler;
    private final Settings settings;

    private int[] colors = new int[0];
    private boolean wanted = false;
    private boolean kill = false;
    private boolean visible = false;
    private boolean lastState = false;
    private int[] lastColors = new int[0];
    private boolean added = false;
    private boolean inLockscreen = false;

    private Overlay(Context context) {
        windowManager = (WindowManager)context.getSystemService(Activity.WINDOW_SERVICE);
        spritePlayer = new SpritePlayer(context);
        keyguardManager = (KeyguardManager)context.getSystemService(KEYGUARD_SERVICE);
        display = ((DisplayManager)context.getSystemService(DISPLAY_SERVICE)).getDisplay(0);
        handler = new Handler();
        settings = Settings.getInstance(context);

        initParams();
        animation = new NotificationAnimation(context, spritePlayer, new NotificationAnimation.OnNotificationAnimationListener() {
            @Override
            public void onDimensionsApplied(SpritePlayer view) {
                if (added) {
                    windowManager.updateViewLayout(view, view.getLayoutParams());
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

    @Override
    protected void finalize() throws Throwable {
        spritePlayer.getContext().getApplicationContext().unregisterReceiver(broadcastReceiver);
        super.finalize();
    }

    @SuppressLint("RtlHardcoded")
    private void initParams() {
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                0,
                0,
                0,
                0,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
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
        visible = (display.getState() == Display.STATE_ON) && (!keyguardManager.isKeyguardLocked());
        if (wanted && visible && (colors.length > 0)) {
            if (!lastState || colorsChanged()) {
                createOverlay();
                animation.play(colors, false);
                lastColors = colors;
                lastState = true;
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

        if (visible) {
            spritePlayer.setPowerSaverMode(settings.isAnimationBlinker(settings.getMode(Battery.isCharging(spritePlayer.getContext()), !inLockscreen)));
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

    public void setInLockscreen(boolean inLockscreen) {
        if (this.inLockscreen != inLockscreen) {
            this.inLockscreen = inLockscreen;
            evaluate();
        }
    }

    public boolean isVisible() {
        return visible;
    }
}
