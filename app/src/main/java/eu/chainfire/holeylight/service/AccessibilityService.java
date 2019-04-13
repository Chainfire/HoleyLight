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
import eu.chainfire.holeylight.misc.Slog;

public class AccessibilityService extends android.accessibilityservice.AccessibilityService {
    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // AOD runs inside the SystemUI package. The main image (or clock) is displayed inside
        // a ViewPager, and notification icons below that in ImageViews.
        //
        // The area of the combined ViewPager and ImageViews is saved to the screen when it goes
        // into DOZE_SUSPEND mode, and requires no power nor CPU to keep displaying it.
        //
        // Of course it is possible we're getting the wrong views inside some random Android
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

            Rect outerBounds = new Rect(-1, -1, -1, -1);

            for (int i = 0; i < root.getChildCount(); i++) {
                AccessibilityNodeInfo node = root.getChild(i);
                if (
                        (node == null) ||
                        (node.getClassName() == null) ||
                        (
                            (!node.getClassName().equals("android.support.v4.view.ViewPager")) &&
                            (!node.getClassName().equals("android.widget.ImageView"))
                        )
                ) continue;

                node.refresh();

                Rect bounds = new Rect();
                node.getBoundsInScreen(bounds);
                
                if ((outerBounds.left == -1) || (bounds.left < outerBounds.left)) outerBounds.left = bounds.left;
                if ((outerBounds.top == -1) || (bounds.top < outerBounds.top)) outerBounds.top = bounds.top;
                if ((outerBounds.right == -1) || (bounds.right > outerBounds.right)) outerBounds.right = bounds.right;
                if ((outerBounds.bottom == -1) || (bounds.bottom > outerBounds.bottom)) outerBounds.bottom = bounds.bottom;

                Slog.d("AOD_TSP", "Node " + node.getClassName().toString() + " " + bounds.toString());
            }

            if (
                    (outerBounds.left > -1) &&
                    (outerBounds.top > -1) &&
                    (outerBounds.right > -1) &&
                    (outerBounds.bottom > -1)
            ) {
                Slog.d("AOD_TSP", "Access " + outerBounds.toString());
                Overlay.getInstance(this).updateTSPRect(outerBounds);
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