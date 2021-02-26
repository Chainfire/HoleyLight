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

package eu.chainfire.holeylight.service.area;

import android.content.Context;
import android.graphics.Rect;
import android.os.Build;
import android.view.accessibility.AccessibilityNodeInfo;

import eu.chainfire.holeylight.misc.AODControl;
import eu.chainfire.holeylight.misc.Settings;
import eu.chainfire.holeylight.misc.Slog;

import static java.lang.Math.max;
import static java.lang.Math.min;

/* This is a mess of different algos doing more or less the same thing across several
   Samsung Android versions and firmwares. Touching == breaking. Unfortunately there
   isn't really a way to go back and verify after the fact now unless you had a
   bunch of different compatible Samsung devices and then several of each running
   different Android versions :/
 */

public class AreaFinderSamsung extends AreaFinder {
    private String previousNodeClass = null;
    private boolean seenXViewPager = false;
    private boolean haveSeenContainer = false; // if seen, accept no substitute
    private boolean isTapToShow = false;
    private Rect clockArea = null;
    private Integer overlayBottom = null;

    private void logNode(int level, AccessibilityNodeInfo node, Rect bounds) {
        if (Settings.DEBUG) {
            StringBuilder sb = new StringBuilder("");
            for (int i = 0; i < level; i++) {
                sb.append("--");
            }
            if (sb.length() > 0) sb.append(" ");
            Slog.d(TAG, "Node " + sb.toString() + node.getClassName().toString() + " " + bounds.toString() + " " + node.getViewIdResourceName());
        }
    }

    private void inspectClockNode(AccessibilityNodeInfo node, int level, Rect outerBounds) {
        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            String childId = child.getViewIdResourceName();
            if ((childId != null) && (
                    // either of those can be not present and draw into parent node itself
                    childId.equals("com.samsung.android.app.aodservice:id/common_hourMin") ||
                    childId.equals("com.samsung.android.app.aodservice:id/common_date")
            )) {
                Rect childBounds = new Rect();
                child.getBoundsInScreen(childBounds);
                if ((childBounds.top >= 0) && (childBounds.left >= 0) && (childBounds.width() > 0) && (childBounds.height() > 0)) {
                    if (clockArea == null) {
                        clockArea = new Rect(
                                min(outerBounds.left, childBounds.left),
                                min(outerBounds.top, childBounds.top),
                                max(outerBounds.right, childBounds.right),
                                childBounds.bottom
                        );
                    } else {
                        clockArea.left = min(clockArea.left, childBounds.left);
                        clockArea.top = min(clockArea.top, childBounds.top);
                        clockArea.right = max(clockArea.right, childBounds.right);
                        clockArea.bottom = max(clockArea.bottom, childBounds.bottom);
                    }
                    logNode(level, child, childBounds);
                    Slog.d(TAG, "+++++++++++++++++++++++++++++++++++++++++++++++++++");
                } else {
                    logNode(level, child, childBounds);
                }
            } else if (childId != null && childId.equals("com.samsung.android.app.aodservice:id/common_clock_widget_container")) {
                Rect childBounds = new Rect();
                logNode(level, child, childBounds);
                inspectClockNode(child, level + 1, outerBounds);
            } else if (Settings.DEBUG) {
                Rect childBounds = new Rect();
                child.getBoundsInScreen(childBounds);
                logNode(level + 1, child, childBounds);
            }
        }
    }

    private void inspectNode(AccessibilityNodeInfo node, Rect outerBounds, int level, boolean a11, boolean isTapToShow) {
        if (level == 0 && a11) {
            previousNodeClass = null;
            seenXViewPager = false;
        }

        if (
                (node == null) ||
                (node.getClassName() == null) ||
                (!Settings.DEBUG && (
                        (!node.getClassName().equals("android.widget.FrameLayout")) &&
                        (!node.getClassName().equals("com.android.internal.widget.ViewPager")) &&
                        (!"com.samsung.android.app.aodservice:id/common_battery_text".equals(node.getViewIdResourceName()))
                ))
        ) {
            if ((node != null) && (node.getClassName() != null)) {
                previousNodeClass = node.getClassName().toString();
                seenXViewPager |= node.getClassName().equals("androidx.viewpager.widget.ViewPager");
            }
            return;
        }

        node.refresh();

        Rect bounds = new Rect();
        node.getBoundsInScreen(bounds);
        logNode(level, node, bounds);

        if ("com.samsung.android.app.aodservice:id/common_battery_text".equals(node.getViewIdResourceName())) {
            overlayBottom = bounds.top - 1;
            Slog.d(TAG, "|||||||||||||||||||||||||||||||||||||||||||||||||||");
        } else if (node.getClassName().equals("com.android.internal.widget.ViewPager") || (
                a11 &&
                node.getClassName().equals("android.widget.FrameLayout") && (
                        (
                                outerBounds.left == -1 && (
                                        (
                                                previousNodeClass != null &&
                                                previousNodeClass.equals("android.widget.ImageView")
                                        ) || (
                                                seenXViewPager &&
                                                level == 2
                                        ) || (
                                                node.getViewIdResourceName() != null &&
                                                node.getViewIdResourceName().equals("com.samsung.android.app.aodservice:id/common_clock_widget_container")
                                        )
                                )
                        ) || (
                                outerBounds.left >= 0
                        )
                )
        )) {
            if (outerBounds.left == -1 && !seenXViewPager && haveSeenContainer && isTapToShow && !(node.getViewIdResourceName() != null && node.getViewIdResourceName().equals("com.samsung.android.app.aodservice:id/common_clock_widget_container"))) {
                // skip
            } else {
                haveSeenContainer |= a11 && node.getViewIdResourceName() != null && node.getViewIdResourceName().equals("com.samsung.android.app.aodservice:id/common_clock_widget_container");
                if ((bounds.left >= 0) && (bounds.right >= 0) && ((outerBounds.left == -1) || (bounds.left < outerBounds.left))) outerBounds.left = bounds.left;
                if ((bounds.top >= 0) && (bounds.bottom >= 0) && ((outerBounds.top == -1) || (bounds.top < outerBounds.top))) outerBounds.top = bounds.top;
                if ((bounds.left >= 0) && (bounds.right >= 0) && ((outerBounds.right == -1) || (bounds.right > outerBounds.right))) outerBounds.right = bounds.right;
                if ((bounds.top >= 0) && (bounds.bottom >= 0) && ((outerBounds.bottom == -1) || (bounds.bottom > outerBounds.bottom))) outerBounds.bottom = bounds.bottom;
                Slog.d(TAG, "^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^");

                inspectClockNode(node, level + 1, outerBounds);
            }
        } else if (node.getClassName().equals("android.widget.FrameLayout") || Settings.DEBUG)  {
            for (int i = 0; i < node.getChildCount(); i++) {
                inspectNode(node.getChild(i), outerBounds, level + 1, a11, isTapToShow);
            }
        }

        if ((node != null) && (node.getClassName() != null)) {
            previousNodeClass = node.getClassName().toString();
            seenXViewPager |= node.getClassName().equals("androidx.viewpager.widget.ViewPager");
        }
    }

    @Override
    public void start(Context context) {
        isTapToShow = AODControl.isAODTapToShow(context);
        overlayBottom = null;
        clockArea = null;
    }

    @Override
    public Rect find(AccessibilityNodeInfo root) {
        Rect outerBounds = new Rect(-1, -1, -1, -1);

        if (Build.VERSION.SDK_INT < 29) { // Android 9
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

                if ((bounds.left >= 0) && ((outerBounds.left == -1) || (bounds.left < outerBounds.left))) outerBounds.left = bounds.left;
                if ((bounds.top >= 0) && ((outerBounds.top == -1) || (bounds.top < outerBounds.top))) outerBounds.top = bounds.top;
                if ((bounds.right >= 0) && ((outerBounds.right == -1) || (bounds.right > outerBounds.right))) outerBounds.right = bounds.right;
                if ((bounds.bottom >= 0) && ((outerBounds.bottom == -1) || (bounds.bottom > outerBounds.bottom))) outerBounds.bottom = bounds.bottom;

                Slog.d(TAG, "Node " + node.getClassName().toString() + " " + bounds.toString());
            }
        } else { // Android 10+
            inspectNode(root, outerBounds, 0, false, isTapToShow);
            if (outerBounds.left == -1 && Build.VERSION.SDK_INT >= 30) {
                // Android 11+
                inspectNode(root, outerBounds, 0, true, isTapToShow);
            }
        }

        if (
                haveSeenContainer &&
                (outerBounds != null) &&
                (outerBounds.left == -1) &&
                (outerBounds.top == -1) &&
                (outerBounds.right == -1) &&
                (outerBounds.bottom == -1) &&
                isTapToShow
        ) {
            return null;
        }

        if ((outerBounds.bottom > -1) && (overlayBottom == null || outerBounds.bottom > overlayBottom)) {
            overlayBottom = outerBounds.bottom;
        }

        return outerBounds;
    }

    @Override
    public Rect findClock(AccessibilityNodeInfo root) {
        if ((clockArea != null) && (clockArea.top >= 0) && (clockArea.left >= 0) && (clockArea.width() > 0) && (clockArea.height() > 0)) return clockArea;
        return null;
    }

    @Override
    public Integer findOverlayBottom(AccessibilityNodeInfo root) {
        return overlayBottom;
    }
}
