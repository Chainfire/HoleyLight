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
import android.view.accessibility.AccessibilityNodeInfo;

import java.util.List;

import eu.chainfire.holeylight.misc.Settings;
import eu.chainfire.holeylight.misc.Slog;

public class AreaFinderGoogle extends AreaFinder {
    private Rect clockArea = null;

    private void inspectNode(AccessibilityNodeInfo node, Rect outerBounds, int level) {
        if (
                (node == null) ||
                (node.getClassName() == null) ||
                (!Settings.DEBUG && (
                        (!node.getClassName().equals("android.widget.FrameLayout")) &&
                        (!node.getClassName().equals("com.android.internal.widget.ViewPager"))
                ))
        ) {
            return;
        }

        node.refresh();

        Rect bounds = new Rect();
        node.getBoundsInScreen(bounds);

        if (Settings.DEBUG) {
            String l = "";
            for (int i = 0; i < level; i++) {
                l += "--";
            }
            if (l.length() > 0) l += " ";
            Slog.d(TAG, "Node " + l + node.getClassName().toString() + " " + bounds.toString() + " " + node.getViewIdResourceName());
        }

        if (level == 2 && (node.getViewIdResourceName() == null || !node.getViewIdResourceName().startsWith("com.android.systemui:id/keyguard_"))) {
            if (node.getViewIdResourceName() != null && node.getViewIdResourceName().equals("com.android.systemui:id/default_clock_view")) {
                // named text view with clock
                if (bounds.left >= 0 && bounds.top >= 0 && bounds.width() > 0 && bounds.height() > 0) {
                    clockArea = new Rect(bounds);
                }
            } else if (clockArea != null && node.getClassName() != null && node.getClassName().equals("android.widget.TextView")) {
                // unnamed text views below clock with date and weather
                if (bounds.top == clockArea.bottom) {
                    clockArea.bottom = bounds.bottom;
                }
            }
            if ((bounds.left >= 0) && (bounds.right >= 0) && ((outerBounds.left == -1) || (bounds.left < outerBounds.left))) outerBounds.left = bounds.left;
            if ((bounds.top >= 0) && (bounds.bottom >= 0) && ((outerBounds.top == -1) || (bounds.top < outerBounds.top))) outerBounds.top = bounds.top;
            if ((bounds.left >= 0) && (bounds.right >= 0) && ((outerBounds.right == -1) || (bounds.right > outerBounds.right))) outerBounds.right = bounds.right;
            if ((bounds.top >= 0) && (bounds.bottom >= 0) && ((outerBounds.bottom == -1) || (bounds.bottom > outerBounds.bottom))) outerBounds.bottom = bounds.bottom;
            Slog.d(TAG, "^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^");
        } else if (node.getClassName().equals("android.widget.FrameLayout") || Settings.DEBUG)  {
            for (int i = 0; i < node.getChildCount(); i++) {
                inspectNode(node.getChild(i), outerBounds, level + 1);
            }
        }
    }

    @Override
    public void start(Context context) {
        clockArea = null;
    }

    @Override
    public Rect find(AccessibilityNodeInfo root) {
        Rect outerBounds = new Rect(-1, -1, -1, -1);
        inspectNode(root, outerBounds, 0);

        // On Google devices the entire screen is saved rather than a small area, let's enlarge because it's small by default
        if (outerBounds.width() > 0) {
            List<AccessibilityNodeInfo> containers = root.findAccessibilityNodeInfosByViewId("com.android.systemui:id/notification_panel");
            for (AccessibilityNodeInfo container : containers) {
                Rect bounds = new Rect();
                container.getBoundsInScreen(bounds);
                if (bounds.left <= outerBounds.left && bounds.top <= outerBounds.top && bounds.right >= outerBounds.right && bounds.bottom >= outerBounds.bottom) {
                    outerBounds.left = bounds.left;
                    outerBounds.right = bounds.right;
                    int h = outerBounds.height();
                    if (clockArea != null) {
                        outerBounds.bottom += (clockArea.height()*3/2);
                    }
                    outerBounds.top = Math.max(bounds.top, outerBounds.top - (int)h/2);
                    outerBounds.bottom += (int)h/2;
                    while (outerBounds.bottom > bounds.bottom * 0.9) {
                        outerBounds.bottom = (int)(bounds.bottom * 0.9);
                    }
                    break;
                }
            }
        }

        return outerBounds;
    }

    @Override
    public Rect findClock(AccessibilityNodeInfo root) {
        return clockArea;
    }

    @Override
    public Integer findOverlayBottom(AccessibilityNodeInfo root) {
        // already included in the rectangle returned by find()
        return null;
    }
}
