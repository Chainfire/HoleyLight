package eu.chainfire.holeylight.service.area;

import android.content.Context;
import android.graphics.Rect;
import android.view.accessibility.AccessibilityNodeInfo;

import eu.chainfire.holeylight.misc.Manufacturer;

public abstract class AreaFinder {
    public static AreaFinder factory() {
        if (Manufacturer.isSamsung()) {
            return new AreaFinderSamsung();
        } else if (Manufacturer.isGoogle()) {
            return new AreaFinderGoogle();
        } else {
            return new AreaFinderDummy();
        }
    }

    public static final String TAG = "AOD_TSP";

    public abstract void start(Context context);
    public abstract Rect find(AccessibilityNodeInfo root);
    public abstract Integer findOverlayBottom(AccessibilityNodeInfo root);
}
