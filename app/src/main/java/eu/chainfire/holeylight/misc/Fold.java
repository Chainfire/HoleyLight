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

// Hardcoded Galaxy Fold 2 support
//
// The current version of Android Jetpack WindowManager - which was
// seemingly written for this device specifically? - doesn't actually
// work to detect the device, hinge angle, or state. Also the API
// is... inconvenient.
//
// Android's new HINGE_ANGLE sensor which should be nice for this is
// reported as not present on the tester's device.
//
// Trying to access Samsung's own sensors directly fails. Not sure why.

package eu.chainfire.holeylight.misc;

import android.os.Build;

public class Fold {
    public static boolean isFold() {
        return Manufacturer.isSamsung() && Build.DEVICE.toUpperCase().startsWith("F2");
    }

    public static boolean isFolded(int w, int h) {
        return w < h/2;
    }
}
