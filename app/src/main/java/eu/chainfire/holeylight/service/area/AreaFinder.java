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
    public abstract Rect findClock(AccessibilityNodeInfo root);
    public abstract Integer findOverlayBottom(AccessibilityNodeInfo root);
}
