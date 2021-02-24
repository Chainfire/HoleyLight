package eu.chainfire.holeylight.service.area;

import android.content.Context;
import android.graphics.Rect;
import android.view.accessibility.AccessibilityNodeInfo;

import java.util.List;

import eu.chainfire.holeylight.misc.Settings;
import eu.chainfire.holeylight.misc.Slog;

public class AreaFinderGoogle extends AreaFinder {
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
                    outerBounds.top -= (int)h/2;
                    outerBounds.bottom += (int)h/2;
                    while (outerBounds.top < bounds.top) {
                        outerBounds.top += (int)h/4;
                        outerBounds.bottom += (int)h/4;
                    }
                    while (outerBounds.bottom > bounds.bottom * 0.9) {
                        outerBounds.top -= (int)h/4;
                        outerBounds.bottom -= (int)h/4;
                    }
                    break;
                }
            }
        }

        return outerBounds;
    }

    @Override
    public Rect findClock(AccessibilityNodeInfo root) {
        return null;
    }

    @Override
    public Integer findOverlayBottom(AccessibilityNodeInfo root) {
        // already included in the rectangle returned by find()
        return null;
    }
}
