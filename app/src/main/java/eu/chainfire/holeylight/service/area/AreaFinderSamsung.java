package eu.chainfire.holeylight.service.area;

import android.content.Context;
import android.graphics.Rect;
import android.os.Build;
import android.view.accessibility.AccessibilityNodeInfo;

import eu.chainfire.holeylight.misc.AODControl;
import eu.chainfire.holeylight.misc.Settings;
import eu.chainfire.holeylight.misc.Slog;

public class AreaFinderSamsung extends AreaFinder {
    private String previousNodeClass = null;
    private boolean seenXViewPager = false;
    private boolean haveSeenContainer = false; // if seen, accept no substitute
    private boolean isTapToShow = false;

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
                        (!node.getClassName().equals("com.android.internal.widget.ViewPager"))
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

        if (Settings.DEBUG) {
            String l = "";
            for (int i = 0; i < level; i++) {
                l += "--";
            }
            if (l.length() > 0) l += " ";
            Slog.d(TAG, "Node " + l + node.getClassName().toString() + " " + bounds.toString() + " " + node.getViewIdResourceName());
        }

        if (node.getClassName().equals("com.android.internal.widget.ViewPager") || (
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

        return outerBounds;
    }
}
