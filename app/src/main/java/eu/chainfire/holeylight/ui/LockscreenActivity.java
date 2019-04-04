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

package eu.chainfire.holeylight.ui;

import android.app.KeyguardManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.os.SystemClock;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;

import com.airbnb.lottie.LottieAnimationView;

import java.util.Locale;

import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import eu.chainfire.holeylight.BuildConfig;
import eu.chainfire.holeylight.R;
import eu.chainfire.holeylight.misc.NotificationAnimation;
import eu.chainfire.holeylight.misc.Settings;

public class LockscreenActivity extends AppCompatActivity implements GestureDetector.OnGestureListener, GestureDetector.OnDoubleTapListener {
    private static long visible = 0L;
    public static long lastVisible() {
        return visible;
    }

    private Settings settings = null;
    private NotificationAnimation notificationAnimation;
    private LottieAnimationView lottieAnimationView;
    private KeyguardManager keyguardManager;
    private PowerManager powerManager;
    private Handler handler;
    private GestureDetector gestureDetector;
    private boolean haveNotifications = false;

    private PowerManager.WakeLock partialWakeLock = null;
    private PowerManager.WakeLock screenWakeLock = null;
    private PowerManager.WakeLock proximityWakeLock = null;

    private void log(String fmt, Object... args) {
        Log.d("HoleyLight/Lockscreen", String.format(Locale.ENGLISH, fmt, args));
    }

    private boolean broadcastReceiverRegistered = false;
    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @SuppressWarnings("deprecation")
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction() == null) return;

            log("Intent: %s", intent.getAction());

            switch (intent.getAction()) {
                case Intent.ACTION_SCREEN_ON:
                    if (!haveNotifications && !notificationAnimation.isPlaying()) {
                        // We timed out the screen earlier, and user turned it back on again.
                        finish();
                    }
                    break;
                case Intent.ACTION_SCREEN_OFF:
                    //TODO battery// if ((proximityWakeLock != null) && proximityWakeLock.isHeld()) {
                    if (!screenWakeLock.isHeld()) {
                        // Wakelock expired
                        finish();
                    } else {
                        // The power button was pressed while this lockscreen was displaying, instead of
                        // turning off, turn on and show the real lockscreen.
                        finish();
                        PowerManager.WakeLock wakelock = powerManager.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP, BuildConfig.APPLICATION_ID + ":Lockscreen/Exit");
                        wakelock.acquire(1000);
                    }
                    //}
                    break;
                case Intent.ACTION_POWER_DISCONNECTED:
                    if (!settings.isEnabledWhileScreenOffBattery()) {
                        // Turn off
                        finish();
                    }
                    break;
            }
        }
    };

    private boolean localBroadcastReceiverRegistered = false;
    private BroadcastReceiver localBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction() == null) return;

            if (intent.getAction().equals(BuildConfig.APPLICATION_ID + ".colors")) {
                int[] colors = intent.getIntArrayExtra(BuildConfig.APPLICATION_ID + ".colors");
                haveNotifications = (colors != null) && (colors.length > 0);
                if (haveNotifications) {
                    wakeup();
                    notificationAnimation.play(colors, false);
                } else {
                    notificationAnimation.stop(false);
                }
            }
        }
    };

    private void wakeup() {
        if (!partialWakeLock.isHeld()) partialWakeLock.acquire(2500);
        if (!screenWakeLock.isHeld()) screenWakeLock.acquire(2500);
        //TODO battery only// if (!proximityWakeLock.isHeld()) proximityWakeLock.acquire(2500);
        //TODO battery - check order of these.. proximity before others? also check other code
    }

    @SuppressWarnings("deprecation")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        hideSystemUI();
        getWindow().getDecorView().setOnSystemUiVisibilityChangeListener(visibility -> {
            if ((visibility & View.SYSTEM_UI_FLAG_FULLSCREEN) == 0) {
                hideSystemUI();
            }
        });

        setShowWhenLocked(true);
        getWindow().addFlags(
                // In case we are shown due to screen off, but not automatically locked yet,
                // allow the device to time-out and lock, rather than staying unlocked forever
                // while this screen is active
                WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON
        );

        setContentView(R.layout.activity_lockscreen);

        settings = Settings.getInstance(this);
        keyguardManager = (KeyguardManager)getSystemService(KEYGUARD_SERVICE);
        powerManager = (PowerManager)getSystemService(POWER_SERVICE);

        lottieAnimationView = findViewById(R.id.lottie);
        notificationAnimation = new NotificationAnimation(this, lottieAnimationView, new NotificationAnimation.OnNotificationAnimationListener() {
            @Override public void onAnimationComplete(LottieAnimationView view) { }
            @Override public void onDimensionsApplied(LottieAnimationView view) { }
        });
        int[] colors = getIntent().getIntArrayExtra(BuildConfig.APPLICATION_ID + ".colors");
        haveNotifications = (colors != null) && (colors.length > 0);
        notificationAnimation.play(colors, false);

        partialWakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, BuildConfig.APPLICATION_ID + ":Lockscreen/CPU");
        partialWakeLock.setReferenceCounted(false);
        partialWakeLock.acquire(2500);

        screenWakeLock = powerManager.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP, BuildConfig.APPLICATION_ID + ":Lockscreen/Screen");
        screenWakeLock.setReferenceCounted(false);

        proximityWakeLock = powerManager.newWakeLock(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP, BuildConfig.APPLICATION_ID + ":Lockscreen/Proximity");
        proximityWakeLock.setReferenceCounted(false);

        handler = new Handler();
        handler.postDelayed(() -> {
            if (haveNotifications) {
                // doesn't work right if we do it directly from onCreate
                wakeup();
            }
        }, 250);

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_SCREEN_ON);
        intentFilter.addAction(Intent.ACTION_SCREEN_OFF);
        intentFilter.addAction(Intent.ACTION_POWER_DISCONNECTED);
        intentFilter.setPriority(999);
        registerReceiver(broadcastReceiver, intentFilter);
        broadcastReceiverRegistered = true;

        gestureDetector = new GestureDetector(this, this);

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                // while we are not in an actually locked state, both the overlay and this screen
                // would be visible simultaneously, with overlapping animations
                if (!keyguardManager.isKeyguardLocked()) {
                    if (lottieAnimationView.getVisibility() != View.INVISIBLE) {
                        lottieAnimationView.setVisibility(View.INVISIBLE);
                    }
                    handler.postDelayed(this, 500);
                } else {
                    if (lottieAnimationView.getVisibility() == View.INVISIBLE) {
                        lottieAnimationView.setVisibility(View.VISIBLE);
                    }
                }
            }
        }, 0);

        LocalBroadcastManager.getInstance(this).registerReceiver(localBroadcastReceiver, new IntentFilter(BuildConfig.APPLICATION_ID + ".colors"));
        localBroadcastReceiverRegistered = true;
    }

    private void hideSystemUI() {
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN);
    }

    private Runnable repeatedWhileVisible = new Runnable() {
        @Override
        public void run() {
            visible = SystemClock.elapsedRealtime();
            partialWakeLock.acquire(15000);
            if (haveNotifications || notificationAnimation.isPlaying()) {
                screenWakeLock.acquire(15000);
                //TODO battery only// proximityWakeLock.acquire(15000);
            } else {
                if (screenWakeLock.isHeld()) screenWakeLock.release();
                //TODO battery only// if (proximityWakeLock.isHeld()) proximityWakeLock.release();
            }
            //TODO check order of proximity
            handler.removeCallbacks(repeatedWhileVisible);
            handler.postDelayed(this, 10000);
        }
    };

    @Override
    protected void onStart() {
        log("onStart");
        super.onStart();
        visible = SystemClock.elapsedRealtime();
        handler.postDelayed(repeatedWhileVisible, 0);
    }

    @Override
    protected void onStop() {
        log("onStop");
        visible = SystemClock.elapsedRealtime();
        handler.removeCallbacks(repeatedWhileVisible);
        super.onStop();
    }

    private void unregister() {
        try {
            if (broadcastReceiverRegistered) {
                unregisterReceiver(broadcastReceiver);
                broadcastReceiverRegistered = false;
            }
            if (localBroadcastReceiverRegistered) {
                LocalBroadcastManager.getInstance(this).unregisterReceiver(localBroadcastReceiver);
                localBroadcastReceiverRegistered = false;
            }
        } catch (Throwable t) {
            // the amount of grief Android will give you for trying to *not* do something...
        }
    }

    @Override
    protected void onDestroy() {
        unregister();
        super.onDestroy();
    }

    @Override
    protected void onUserLeaveHint() {
        // Usually (but not always) Home button
        super.onUserLeaveHint();
        finish();
    }

    @Override
    public void finish() {
        LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent(BuildConfig.APPLICATION_ID + ".onLockscreen"));
        handler.removeCallbacks(repeatedWhileVisible);
        unregister();
        super.finish();
        overridePendingTransition(0, 0);
        if (proximityWakeLock.isHeld()) proximityWakeLock.release();
        if (screenWakeLock.isHeld()) screenWakeLock.release();
        if (partialWakeLock.isHeld()) partialWakeLock.release();
    }

    @Override
    public void onBackPressed() {
        finish();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event){
        gestureDetector.onTouchEvent(event);
        return super.onTouchEvent(event);
    }

    private void unlock() {
        if (keyguardManager.isKeyguardLocked() && (!keyguardManager.isDeviceLocked() || (!keyguardManager.isKeyguardSecure() && !keyguardManager.isDeviceSecure()))) {
            // Unlock device, if we don't do this (or it fails) we end up on device lockscreen
            // The idea is to do this when the user has performed some action, and something like
            // SmartLock has kept the device from a secure locking, might as well treat that
            // user actions as an immediate unlock
            keyguardManager.requestDismissKeyguard(this, null);
        }
        finish();
    }

    @Override
    public boolean onDoubleTap(MotionEvent e) {
        unlock();
        return true;
    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        float distance = (float)Math.sqrt(Math.pow(e2.getX() - e1.getX(), 2) + Math.pow(e2.getY() - e1.getY(), 2));
        if (distance > (float)getWindow().getDecorView().getHeight() / 4.0f) {
            unlock();
            return true;
        }
        return false;
    }

    @Override
    public boolean onDown(MotionEvent e) {
        if (e.getY() > ((float)getWindow().getDecorView().getHeight() * (5.0f/6.0f)) && keyguardManager.isDeviceLocked() && keyguardManager.isDeviceSecure()) {
            // User pressed in fingerprint area, and device is locked, and device is secure.
            // Disappearing now (and thus showing the lockscreen) will allow the user
            // to fingerprint unlock in a single action.
            finish();
            return true;
        }
        return false;
    }

    @Override
    public void onLongPress(MotionEvent e) {
        if (e.getY() > ((float)getWindow().getDecorView().getHeight() * (5.0f/6.0f))) {
            // User pressed and held in fingerprint area, and fingerprint detection isn't thought
            // to be relevant (see onDown()), unlock for consistency
            unlock();
        }
    }

    @Override public boolean onSingleTapConfirmed(MotionEvent e) { return false; }
    @Override public boolean onDoubleTapEvent(MotionEvent e) { return false; }
    @Override public void onShowPress(MotionEvent e) {  }
    @Override public boolean onSingleTapUp(MotionEvent e) { return false; }
    @Override public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) { return false; }
}
