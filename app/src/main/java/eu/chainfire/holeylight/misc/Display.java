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
import android.hardware.display.DisplayManager;

import static android.content.Context.DISPLAY_SERVICE;

@SuppressWarnings("WeakerAccess")
public class Display {
    public enum State { ON, OFF, DOZE, OTHER }

    private static android.view.Display display = null;

    @SuppressWarnings("DuplicateBranchesInSwitch")
    public static State get(Context context) {
        if (display == null) {
            display = ((DisplayManager)context.getSystemService(DISPLAY_SERVICE)).getDisplay(0);
        }
        switch (display.getState()) {
            case android.view.Display.STATE_ON: return State.ON;
            case android.view.Display.STATE_OFF: return State.OFF;

            case android.view.Display.STATE_DOZE:
            case android.view.Display.STATE_DOZE_SUSPEND:
            case android.view.Display.STATE_ON_SUSPEND: return State.DOZE; // technically, no

            case android.view.Display.STATE_VR: return State.OTHER;
            case android.view.Display.STATE_UNKNOWN: return State.OTHER;

            default: return State.OTHER;
        }
    }

    public static boolean is(Context context, boolean ifOn, boolean ifOff, boolean ifDoze, boolean ifOther) {
        switch (get(context)) {
            case ON: return ifOn;
            case OFF: return ifOff;
            case DOZE: return ifDoze;
            default: return ifOther;
        }
    }

    public static boolean isOn(Context context, boolean ifDoze) {
        return is(context, true, false, ifDoze, true);
    }

    @SuppressWarnings("unused")
    public static boolean isOff(Context context, boolean ifDoze) {
        return is(context, false, true, ifDoze, false);
    }

    public static boolean isDoze(Context context) {
        return is(context, false, false, true, false);
    }
}
