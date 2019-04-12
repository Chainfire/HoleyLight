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

import android.graphics.Rect;
import android.os.IBinder;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityWindowInfo;

import java.lang.reflect.Field;
import java.util.List;

import eu.chainfire.holeylight.animation.Overlay;

public class AccessibilityService extends android.accessibilityservice.AccessibilityService {
    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // The image displayed in AOD is inside a ViewPager, and runs inside the SystemUI package.
        //
        // The dimensions returns are not an exact match if you monitor TSP in logcat, but its
        // only a few pixels off (probably because AOD moves the images around a few pixels
        // to prevent burn-in), that's why we add a margin too.
        //
        // The area of the ViewPager is saved to the screen when it goes into DOZE_SUSPEND mode,
        // and requires no power nor CPU to keep displaying it.
        //
        // Of course it is possible we're getting the wrong ViewPager inside some random Android
        // activity, so we make sure elsewhere the system is actually in doze mode before using it.

        if (event.getPackageName() == null) return;
        if (event.getEventType() != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) return;
        if (event.getPackageName() == null) return;
        if (!event.getPackageName().toString().equals("com.android.systemui")) return;

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

            for (int i = 0; i < root.getChildCount(); i++) {
                AccessibilityNodeInfo node = root.getChild(i);
                if (
                        (node.getClassName() == null) ||
                        (!node.getClassName().equals("android.support.v4.view.ViewPager"))
                ) continue;

                node.refresh();

                Rect bounds = new Rect();
                node.getBoundsInScreen(bounds);

                Overlay.getInstance(this).updateTSPRect(bounds);
            }
        }
    }

    @Override
    public void onInterrupt() {
    }

    @Override
    public void onServiceConnected() {
        // When the service is created, it *should* setup the WindowManager with the correct token.
        // The source and the docs say so. But for reasons unknown that doesn't happen or work.
        // There doesn't appear to be a clean way to retrieve the token normally either. So we use
        // some reflection. This is bad, but it does work.

        try {
            Class<?> clazz = AccessibilityService.class.getSuperclass();
            if (clazz != null) {
                Field token = clazz.getDeclaredField("mWindowToken");
                token.setAccessible(true);
                IBinder windowToken = (IBinder)token.get(AccessibilityService.this);
                if (windowToken != null) {
                    Overlay.getInstance(AccessibilityService.this, windowToken);
                }
            }
        } catch (Exception e) {
            // we're pretty much screwed if we end up here
            e.printStackTrace();
        }
    }
}