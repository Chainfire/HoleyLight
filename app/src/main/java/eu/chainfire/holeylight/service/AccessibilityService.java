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

import android.graphics.Rect;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityWindowInfo;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Locale;

import eu.chainfire.holeylight.animation.Overlay;
import eu.chainfire.holeylight.misc.Display;
import eu.chainfire.holeylight.misc.Settings;
import eu.chainfire.holeylight.misc.Slog;
import eu.chainfire.holeylight.service.area.AreaFinder;

@SuppressWarnings("FieldCanBeLocal")
public class AccessibilityService extends android.accessibilityservice.AccessibilityService {
    private HandlerThread handlerThread = null;
    private Handler handler = null;
    private Handler handlerMain = null;
    private Display.State lastState = null;
    private AreaFinder areaFinder = null;

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // AOD runs inside the SystemUI package. The main image (or clock) is displayed inside
        // a ViewPager, and notification icons below that in ImageViews.
        //
        // The area of the combined ViewPager and ImageViews is saved to the screen when it goes
        // into DOZE_SUSPEND mode, and requires no power nor CPU to keep displaying it.
        //
        // Update: on Android 11 it seems the ViewPager is no longer always used as container, the
        // elements are now often part of a FrameLayout directly.
        //
        // Of course it is possible we're getting the wrong views inside some random Android
        // activity, so we make sure elsewhere the system is actually in doze mode before using it.
        //
        // The refresh calls are quite costly, abandon processing as soon as possible if no match.
        //
        // We also track screen state here, we regularly get that information here before we
        // receive SCREEN_ON/OFF events.

        Display.State state = Display.get(this);
        if (state != lastState) {
            Slog.d("Access", String.format(Locale.ENGLISH, "display %s --> %s [%d/%s]", lastState != null ? lastState.toString() : "null", state.toString(), event.getEventType(), event.getPackageName() != null ? event.getPackageName() : "null"));
            lastState = state;
            Overlay.getInstance(this).evaluate(true);
            NotificationListenerService.checkNotifications();
        }

        if ((lastState == Display.State.ON) || (lastState == Display.State.OTHER)) {
            return;
        }

        if (event.getPackageName() == null) return;
        if (event.getEventType() != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) return;
        if (!event.getPackageName().toString().equals("com.android.systemui")) return;

        handler.post(() -> {
            try {
                areaFinder.start(this);

                List<AccessibilityWindowInfo> windows = getWindows();
                for (AccessibilityWindowInfo window : windows) {
                    AccessibilityNodeInfo root = window.getRoot();

                    if (
                            (root == null) ||
                            (root.getChildCount() == 0) ||
                            (root.getPackageName() == null) ||
                            (!root.getPackageName().toString().equals("com.android.systemui"))
                    ) continue;

                    root.refresh();

                    final Rect outerBounds = areaFinder.find(root);

                    if (
                            (outerBounds != null) &&
                            (outerBounds.left > -1) &&
                            (outerBounds.top > -1) &&
                            (outerBounds.right > -1) &&
                            (outerBounds.bottom > -1) &&
                            (outerBounds.width() > 0) &&
                            (outerBounds.height() > 0)
                    ) {
                        Slog.d("AOD_TSP", "Access " + outerBounds.toString());
                        handlerMain.post(() -> Overlay.getInstance(AccessibilityService.this).updateTSPRect(outerBounds));
                    } else if (outerBounds == null) {
                        Slog.d("AOD_TSP", "Access null");
                        handlerMain.post(() -> Overlay.getInstance(AccessibilityService.this).updateTSPRect(new Rect(0, 0, 0, 0)));
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    @Override
    public void onInterrupt() {
    }

    @Override
    public void onServiceConnected() {
        // When the service is created, it *should* setup the WindowManager with the correct token.
        // The source and the docs say so. But for reasons unknown that doesn't happen or work.
        // There doesn't appear to be a clean way to retrieve the token normally either. So we use
        // some reflection. This is bad, but it *did* work pre-Android 11.

        // Since reflection has been crippled in Android 11, we're now ensuring the WindowManager
        // is created by the correct context (this one), which also solves the problem. Likely
        // this also fixes the problem on Android 9/10, but as there's no way to test that right
        // now I'm leaving in the old code.

        boolean done = false;

        if (Build.VERSION.SDK_INT < 30) {
            // Android 10-
            try {
                Class<?> clazz = AccessibilityService.class.getSuperclass();
                if (clazz != null) {
                    Field token = clazz.getDeclaredField("mWindowToken");
                    token.setAccessible(true);
                    IBinder windowToken = (IBinder)token.get(AccessibilityService.this);
                    if (windowToken != null) {
                        Overlay.getInstance(AccessibilityService.this, windowToken);
                        done = true;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // Android 11+
        if (!done) {
            Overlay.getInstance(AccessibilityService.this);
        }

        Settings.getInstance(this).incAccessibilityServiceCounter();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        handlerThread = new HandlerThread("AccessibilityService");
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper());
        handlerMain = new Handler(Looper.getMainLooper());
        areaFinder = AreaFinder.factory();
    }
}