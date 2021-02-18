package eu.chainfire.holeylight.service.area;

import android.content.Context;
import android.graphics.Rect;
import android.view.accessibility.AccessibilityNodeInfo;

public class AreaFinderDummy extends AreaFinder {
    @Override
    public void start(Context context) {
    }

    @Override
    public Rect find(AccessibilityNodeInfo root) {
        return new Rect(-1, -1, -1, -1);
    }
}
