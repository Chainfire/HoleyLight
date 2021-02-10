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

package eu.chainfire.holeylight.misc;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.hardware.display.DisplayManager;
import android.view.Display;

import java.util.List;

import androidx.core.view.WindowInsetsCompat;

@SuppressWarnings({"WeakerAccess", "unused"})
public class CameraCutout {
    public static class Cutout {
        private final RectF area;
        private final Point resolution;

        public Cutout(Rect area, Point resolution) {
            this(new RectF(area), resolution);
        }

        public Cutout(RectF area, Point resolution) {
            this.area = area;
            this.resolution = resolution;
        }

        public Cutout(Cutout src) {
            this.area = new RectF(src.getAreaF());
            this.resolution = new Point(src.getResolution());
        }

        public Rect getArea() { return new Rect((int)area.left, (int)area.top, (int)area.right, (int)area.bottom); }
        public RectF getAreaF() { return area; }
        public Point getResolution() { return resolution; }

        public Cutout scaleTo(Point resolution) {
            if (this.resolution.equals(resolution)) return this;
            float sX = (float)resolution.x / (float)this.resolution.x;
            float sY = (float)resolution.y / (float)this.resolution.y;
            return new Cutout(new RectF(
                    area.left * sX,
                    area.top * sY,
                    area.right * sX,
                    area.bottom * sY
            ), resolution);
        }

        public boolean equalsScaled(Cutout cmp) {
            // scaling and rounding introduces errors, allow 2 pixel discrepancy
            if (cmp == null) return false;
            Cutout a = this;
            Cutout b = cmp;
            if (!a.getResolution().equals(b.getResolution())) {
                if (a.getResolution().x * a.getResolution().y > b.getResolution().x * b.getResolution().y) {
                    b = b.scaleTo(a.getResolution());
                } else {
                    a = a.scaleTo(b.getResolution());
                }
            }
            RectF rA = a.getAreaF();
            RectF rB = b.getAreaF();
            return
                    Math.abs(rA.left - rB.left) <= 2f &&
                    Math.abs(rA.top - rB.top) <= 2f &&
                    Math.abs(rA.right - rB.right) <= 2f &&
                    Math.abs(rA.bottom - rB.bottom) <= 2f;
        }

        public boolean isCircular() {
            return Math.abs(area.width() - area.height()) <= 2f;
        }
    }

    // these were determined by running on each devices, algorithm seems perfect for S10/S10E,
    // but has a few extra pixels on the right for S10PLUS.
    public static final Cutout CUTOUT_S10E = new Cutout(new Rect(931, 25, 1021, 116), new Point(1080, 2280));
    public static final Cutout CUTOUT_S10 = new Cutout(new Rect(1237, 33, 1352, 149), new Point(1440, 3040));
    public static final Cutout CUTOUT_S10PLUS = new Cutout(new Rect(1114, 32, 1378, 142), new Point(1440, 3040));

    private final Display display;
    private final int nativeMarginTop;
    private final int nativeMarginRight;

    private Cutout cutout = null;

    public CameraCutout(Context context) {
        this.display = ((DisplayManager)context.getSystemService(Context.DISPLAY_SERVICE)).getDisplay(0);

        int id, id2;
        Resources res = context.getResources();

        // below is Samsung S10(?) specific. Newer firmwares on other Samsung devices also seem to have these values present.

        id = res.getIdentifier("status_bar_camera_top_margin", "dimen", "android");
        nativeMarginTop = id > 0 ? res.getDimensionPixelSize(id) : 0;

        id = res.getIdentifier("status_bar_camera_padding", "dimen", "android");
        id2 = res.getIdentifier("status_bar_camera_side_padding", "dimen", "android");
        nativeMarginRight = id > 0 ? res.getDimensionPixelSize(id) : id2 > 0 ? res.getDimensionPixelSize(id2) : 0;
    }

    public Point getNativeResolution() {
        Point ret = null;

        Display.Mode[] modes = display.getSupportedModes();
        for (Display.Mode mode : modes) {
            if ((ret == null) || (mode.getPhysicalWidth() > ret.x) || (mode.getPhysicalHeight() > ret.y)) {
                ret = new Point(mode.getPhysicalWidth(), mode.getPhysicalHeight());
            }
        }

        return ret;
    }

    public Point getCurrentResolution() {
        Point ret = new Point();
        display.getRealSize(ret);
        return ret;
    }

    public void updateFromBoundingRect(Rect rect) {
        updateFromBoundingRect(new RectF(rect));
    }

    public void updateFromBoundingRect(RectF rect) {
        Point nativeRes = getNativeResolution();
        Point currentRes = getCurrentResolution();

        RectF r = new RectF(rect);

        // convert margins from native to current resolution, and apply to rect; without this we'd get a big notch rather than just the camera area
        r.right -= (float)nativeMarginRight * ((float)currentRes.x / (float)nativeRes.x);
        r.top += (float)nativeMarginTop * ((float)currentRes.y / (float)nativeRes.y);

        cutout = new Cutout(r, currentRes);
    }

    public void updateFromAreaRect(Rect rect) {
        updateFromAreaRect(new RectF(rect));
    }

    public void updateFromAreaRect(RectF rect) {
        cutout = new Cutout(rect, getCurrentResolution());
    }

    public void updateFromInsets(WindowInsetsCompat insets) {
        if ((insets == null) || (insets.getDisplayCutout() == null)) return;
        List<Rect> rects = insets.getDisplayCutout().getBoundingRects();
        if (rects.size() != 1) return;
        updateFromBoundingRect(rects.get(0));
    }

    public Cutout getCutout() {
        return cutout == null ? null : new Cutout(cutout);
    }

    public void applyCutout(Cutout cutout) {
        this.cutout = cutout.scaleTo(getCurrentResolution());
    }

    public boolean isValid() {
        return cutout != null;
    }
}
