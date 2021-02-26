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

public class AreaFinderDummy extends AreaFinder {
    @Override
    public void start(Context context) {
    }

    @Override
    public Rect find(AccessibilityNodeInfo root) {
        return new Rect(-1, -1, -1, -1);
    }

    @Override
    public Rect findClock(AccessibilityNodeInfo root) {
        return null;
    }

    @Override
    public Integer findOverlayBottom(AccessibilityNodeInfo root) {
        return null;
    }
}
