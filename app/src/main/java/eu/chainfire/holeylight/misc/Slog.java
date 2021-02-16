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

import java.util.Locale;

// Existed originally for compatibility with AOSP-copied code, but is now also used by my own code
@SuppressWarnings({"WeakerAccess", "UnusedReturnValue", "unused", "SameReturnValue"})
public class Slog {
    public static int d(String tag, String msg, Object... params) {
        if (Settings.DEBUG) {
            if ((params == null) || (params.length == 0)) {
                return android.util.Log.d("HoleyLight/" + tag, msg);
            } else {
                return android.util.Log.d("HoleyLight/" + tag, String.format(Locale.ENGLISH, msg, params));
            }
        }
        return 0;
    }

    public static int w(String tag, String msg, Object... params) {
        d(tag, msg, params);
        return 0;
    }

    public static int i(String tag, String msg, Object... params) {
        d(tag, msg, params);
        return 0;
    }

    public static int e(String tag, String msg, Object... params) {
        if ((params == null) || (params.length == 0)) {
            return android.util.Log.e("HoleyLight/" + tag, msg);
        } else {
            return android.util.Log.e("HoleyLight/" + tag, String.format(Locale.ENGLISH, msg, params));
        }
    }
}

