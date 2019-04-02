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

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.PixelFormat;
import android.view.Gravity;
import android.view.WindowManager;

import com.airbnb.lottie.LottieAnimationView;

import eu.chainfire.holeylight.misc.NotificationAnimation;

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

            if (intent.getAction().equals(Intent.ACTION_CONFIGURATION_CHANGED)) {
                updateParams();
            }
        }
    };
    private IntentFilter intentFilter = new IntentFilter(Intent.ACTION_CONFIGURATION_CHANGED);
    
    private final WindowManager windowManager;
    private final LottieAnimationView lottieAnimationView;
    private final NotificationAnimation animation;

    private boolean added = false;

    private Overlay(Context context) {
        windowManager = (WindowManager)context.getSystemService(Activity.WINDOW_SERVICE);
        lottieAnimationView = new LottieAnimationView(context);
        initParams();
        animation = new NotificationAnimation(context, lottieAnimationView, new NotificationAnimation.OnNotificationAnimationListener() {
            @Override
            public void onDimensionsApplied(LottieAnimationView view) {
                if (added) {
                    windowManager.updateViewLayout(lottieAnimationView, lottieAnimationView.getLayoutParams());
                }
            }

            @Override
            public void onAnimationComplete(LottieAnimationView view) {
                removeOverlay();
            }
        });
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
        lottieAnimationView.setLayoutParams(params);
    }

    private void updateParams() {
        animation.applyDimensions();
    }

    private void createOverlay() {
        if (added) return;
        try {
            updateParams();
            added = true; // had a case of a weird exception that caused this to run in a loop if placed after addView
            windowManager.addView(lottieAnimationView, lottieAnimationView.getLayoutParams());
            lottieAnimationView.getContext().registerReceiver(broadcastReceiver, intentFilter);
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
            windowManager.removeView(lottieAnimationView);
            lottieAnimationView.getContext().unregisterReceiver(broadcastReceiver);
            added = false;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void show(int[] colors) {
        createOverlay();
        animation.play(colors, false);
    }

    public void hide(boolean immediately) {
        animation.stop(immediately);
        if (immediately) removeOverlay();
    }
}
