/*
 * Copyright (C) 2019 Jorrit "Chainfire" Jongma
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

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Rect;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import androidx.preference.PreferenceManager;
import eu.chainfire.holeylight.animation.SpritePlayer;

@SuppressWarnings({"WeakerAccess", "unused", "UnusedReturnValue"})
public class Settings implements SharedPreferences.OnSharedPreferenceChangeListener {
    public interface OnSettingsChangedListener {
        void onSettingsChanged();
    }

    private static final int SHIFT_CHARGING = 0;
    private static final int SHIFT_BATTERY = 2;
    private static final int SHIFT_SCREEN_ON = 0;
    private static final int SHIFT_SCREEN_OFF = 1;

    public static final int CHARGING_SCREEN_ON = SHIFT_CHARGING + SHIFT_SCREEN_ON;
    public static final int CHARGING_SCREEN_OFF = SHIFT_CHARGING + SHIFT_SCREEN_OFF;
    public static final int BATTERY_SCREEN_ON = SHIFT_BATTERY + SHIFT_SCREEN_ON;
    public static final int BATTERY_SCREEN_OFF = SHIFT_BATTERY + SHIFT_SCREEN_OFF;

    public static final String ENABLED_MASTER = "enabled_master";
    private static final boolean ENABLED_MASTER_DEFAULT = true;

    public static final String ENABLED_SCREEN_OFF_CHARGING = "enabled_screen_off_charging";
    private static final boolean ENABLED_SCREEN_OFF_CHARGING_DEFAULT = true;

    public static final String ENABLED_SCREEN_OFF_BATTERY = "enabled_screen_off_battery";
    private static final boolean ENABLED_SCREEN_OFF_BATTERY_DEFAULT = true;

    public static final String ENABLED_LOCKSCREEN = "enabled_lockscreen";
    private static final boolean ENABLED_LOCKSCREEN_DEFAULT = true;

    public static final String SEEN_PICKUP_SCREEN_ON_CHARGING = "seen_pickup_screen_on_charging";
    private static final boolean SEEN_PICKUP_SCREEN_ON_CHARGING_DEFAULT = false;

    public static final String SEEN_PICKUP_SCREEN_OFF_CHARGING = "seen_pickup_screen_off_charging";
    private static final boolean SEEN_PICKUP_SCREEN_OFF_CHARGING_DEFAULT = false;

    public static final String SEEN_PICKUP_SCREEN_ON_BATTERY = "seen_pickup_screen_on_battery";
    private static final boolean SEEN_PICKUP_SCREEN_ON_BATTERY_DEFAULT = false;

    public static final String SEEN_PICKUP_SCREEN_OFF_BATTERY = "seen_pickup_screen_off_battery";
    private static final boolean SEEN_PICKUP_SCREEN_OFF_BATTERY_DEFAULT = false;

    public static final String SEEN_ON_LOCKSCREEN = "seen_on_lockscreen";
    private static final boolean SEEN_ON_LOCKSCREEN_DEFAULT = false;

    public static final String SEEN_ON_USER_PRESENT = "seen_on_user_present";
    private static final boolean SEEN_ON_USER_PRESENT_DEFAULT = false;

    public static final String ANIMATION_POWERSAVE = "animation_blinker";
    private static final int ANIMATION_POWERSAVE_DEFAULT = (1 << BATTERY_SCREEN_OFF);
    
    private static final String CUTOUT_AREA_LEFT = "cutout_area_left";
    private static final String CUTOUT_AREA_TOP = "cutout_area_top";
    private static final String CUTOUT_AREA_RIGHT = "cutout_area_right";
    private static final String CUTOUT_AREA_BOTTOM = "cutout_area_bottom";
    private static final String DP_ADD_SCALE_BASE = "dp_add_scale_base";
    private static final String DP_ADD_SCALE_HORIZONTAL = "dp_add_scale_horizontal";
    private static final String DP_SHIFT_VERTICAL = "dp_shift_vertical";
    private static final String DP_SHIFT_HORIZONTAL = "dp_shift_horizontal";
    private static final String SPEED_FACTOR = "speed_factor";

    private static final String PACKAGE_COLOR = "package_color:";
    private static final String PACKAGE_COLOR_FMT = PACKAGE_COLOR + "%s";

    private static Settings instance;
    public static Settings getInstance(Context context) {
        synchronized (Settings.class) {
            if (instance == null) {
                instance = new Settings(context);
            }
            return instance;
        }
    }

    private final List<OnSettingsChangedListener> listeners = new ArrayList<>();
    private final SharedPreferences prefs;
    private volatile SharedPreferences.Editor editor = null;
    private volatile int ref = 0;

    private Settings(Context context) {
        prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    protected void finalize() throws Throwable {
        prefs.unregisterOnSharedPreferenceChangeListener(this);
        super.finalize();
    }

    @Override
    public synchronized void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (ref == 0) notifyListeners();
    }

    public synchronized void registerOnSettingsChangedListener(OnSettingsChangedListener onSettingsChangedListener) {
        if (!listeners.contains(onSettingsChangedListener)) {
            listeners.add(onSettingsChangedListener);
        }
    }

    public synchronized void unregisterOnSettingsChangedListener(OnSettingsChangedListener onSettingsChangedListener) {
        listeners.remove(onSettingsChangedListener);
    }

    private synchronized void notifyListeners() {
        for (OnSettingsChangedListener listener : listeners) {
            listener.onSettingsChanged();
        }
    }

    @SuppressLint("CommitPrefEdits")
    public synchronized Settings edit() {
        if (editor == null) {
            editor = prefs.edit();
            ref = 0;
        }
        ref++;
        return this;
    }

    public synchronized void save(boolean immediately) {
        ref--;
        if (ref < 0) ref = 0;
        if (ref == 0) {
            ref = 1; // prevent double notify
            try {
                if (immediately) {
                    editor.commit();
                } else {
                    editor.apply();
                }
            } finally {
                ref = 0;
            }
            notifyListeners();
            editor = null;
        }
    }

    public synchronized void cancel() {
        ref = 0;
        editor = null;
    }

    public Rect getCutoutAreaRect() {
        return new Rect(
            prefs.getInt(CUTOUT_AREA_LEFT, -1),
            prefs.getInt(CUTOUT_AREA_TOP, -1),
            prefs.getInt(CUTOUT_AREA_RIGHT, -1),
            prefs.getInt(CUTOUT_AREA_BOTTOM, -1)
        );
    }

    public Settings setCutoutAreaRect(Rect rect) {
        edit();
        try {
            editor.putInt(CUTOUT_AREA_LEFT, rect.left);
            editor.putInt(CUTOUT_AREA_TOP, rect.top);
            editor.putInt(CUTOUT_AREA_RIGHT, rect.right);
            editor.putInt(CUTOUT_AREA_BOTTOM, rect.bottom);
        } finally {
            save(true);
        }
        return this;
    }

    public int getDpAddScaleBase(int defaultValue) {
        return prefs.getInt(DP_ADD_SCALE_BASE, defaultValue);
    }

    public void setDpAddScaleBase(int value) {
        edit();
        try {
            editor.putInt(DP_ADD_SCALE_BASE, value);
        } finally {
            save(true);
        }
    }

    public int getDpAddScaleHorizontal(int defaultValue) {
        return prefs.getInt(DP_ADD_SCALE_HORIZONTAL, defaultValue);
    }

    public void setDpAddScaleHorizontal(int value) {
        edit();
        try {
            editor.putInt(DP_ADD_SCALE_HORIZONTAL, value);
        } finally {
            save(true);
        }
    }

    public int getDpShiftVertical(int defaultValue) {
        return prefs.getInt(DP_SHIFT_VERTICAL, defaultValue);
    }

    public void setDpShiftVertical(int value) {
        edit();
        try {
            editor.putInt(DP_SHIFT_VERTICAL, value);
        } finally {
            save(true);
        }
    }

    public int getDpShiftHorizontal(int defaultValue) {
        return prefs.getInt(DP_SHIFT_HORIZONTAL, defaultValue);
    }

    public void setDpShiftHorizontal(int value) {
        edit();
        try {
            editor.putInt(DP_SHIFT_HORIZONTAL, value);
        } finally {
            save(true);
        }
    }

    public float getSpeedFactor() {
        return prefs.getFloat(SPEED_FACTOR, 1.0f);
    }

    public void setSpeedFactor(float value) {
        value = Math.min(Math.max(value, 0.5f), 2.0f);

        edit();
        try {
            editor.putFloat(SPEED_FACTOR, value);
        } finally {
            save(true);
        }
    }

    public boolean isEnabled() {
        return prefs.getBoolean(ENABLED_MASTER, ENABLED_MASTER_DEFAULT);
    }

    public void setEnabled(boolean enabled) {
        edit();
        try {
            editor.putBoolean(ENABLED_MASTER, enabled);
        } finally {
            save(true);
        }
    }

    public boolean isEnabledWhileScreenOffAny() {
        return isEnabledWhileScreenOffCharging() || isEnabledWhileScreenOffBattery();
    }

    public boolean isEnabledWhileScreenOffCharging() {
        return isEnabled() && prefs.getBoolean(ENABLED_SCREEN_OFF_CHARGING, ENABLED_SCREEN_OFF_CHARGING_DEFAULT);
    }

    public boolean isEnabledWhileScreenOffBattery() {
        return isEnabled() && prefs.getBoolean(ENABLED_SCREEN_OFF_BATTERY, ENABLED_SCREEN_OFF_BATTERY_DEFAULT);
    }

    public boolean isEnabledOnLockscreen() {
        return isEnabled() && prefs.getBoolean(ENABLED_LOCKSCREEN, ENABLED_LOCKSCREEN_DEFAULT);
    }

    public int getColorForPackage(String packageName, int defaultValue) {
        return prefs.getInt(String.format(Locale.ENGLISH, PACKAGE_COLOR_FMT, packageName), defaultValue);
    }

    public void setColorForPackage(String packageName, int color, boolean fromListener) {
        String key = String.format(Locale.ENGLISH, PACKAGE_COLOR_FMT, packageName);
        if (!prefs.contains(key) || (prefs.getInt(key, -1) != color)) {
            edit();
            try {
                editor.putInt(key, color);
            } finally {
                save(!fromListener);
            }
        }
    }

    public Map<String, Integer> getPackagesAndColors() {
        Map<String, Integer> ret = new HashMap<>();
        Map<String, ?> all = prefs.getAll();
        for (String key : all.keySet()) {
            if (key.startsWith(PACKAGE_COLOR)) {
                String pkg = key.substring(PACKAGE_COLOR.length());
                Integer color = prefs.getInt(key, 0);
                ret.put(pkg, color);
            }
        }
        return ret;
    }

    public boolean isSeenPickupScreenOnCharging(boolean effective) {
        return (!effective || isEnabled()) && prefs.getBoolean(SEEN_PICKUP_SCREEN_ON_CHARGING, SEEN_PICKUP_SCREEN_ON_CHARGING_DEFAULT);
    }

    public boolean isSeenPickupScreenOffCharging(boolean effective) {
        return (!effective || isEnabledWhileScreenOffCharging()) && prefs.getBoolean(SEEN_PICKUP_SCREEN_OFF_CHARGING, SEEN_PICKUP_SCREEN_OFF_CHARGING_DEFAULT);
    }

    public boolean isSeenPickupScreenOnBattery(boolean effective) {
        return (!effective || isEnabled()) && prefs.getBoolean(SEEN_PICKUP_SCREEN_ON_BATTERY, SEEN_PICKUP_SCREEN_ON_BATTERY_DEFAULT);
    }

    public boolean isSeenPickupScreenOffBattery(boolean effective) {
        return (!effective || isEnabledWhileScreenOffBattery()) && prefs.getBoolean(SEEN_PICKUP_SCREEN_OFF_BATTERY, SEEN_PICKUP_SCREEN_OFF_BATTERY_DEFAULT);
    }

    public boolean isSeenOnLockscreen(boolean effective) {
        return (!effective || isEnabledWhileScreenOffAny()) && prefs.getBoolean(SEEN_ON_LOCKSCREEN, SEEN_ON_LOCKSCREEN_DEFAULT);
    }

    public boolean isSeenOnUserPresent(boolean effective) {
        return (!effective || isEnabledWhileScreenOffAny()) && prefs.getBoolean(SEEN_ON_USER_PRESENT, SEEN_ON_USER_PRESENT_DEFAULT);
    }

    public SpritePlayer.Mode getAnimationMode(Context context, int mode) {
        // testing, correct code below
        boolean powerSave = isAnimationPowerSave(mode);
        if (!powerSave) return SpritePlayer.Mode.SWIRL;
        if ((mode & SHIFT_SCREEN_OFF) == SHIFT_SCREEN_OFF) {
            int aod_mode = 0;
            int aod_tap = 0;
            int fingerprint_unlock = 0;
            int fingerprint_icon = 0;
            try {
                ContentResolver resolver = context.getContentResolver();
                aod_mode = android.provider.Settings.System.getInt(resolver, "aod_mode", 0);
                aod_tap = android.provider.Settings.System.getInt(resolver, "aod_tap_to_show_mode", 0);
                fingerprint_unlock = android.provider.Settings.Secure.getInt(resolver, "fingerprint_screen_lock", 0);
                fingerprint_icon = android.provider.Settings.Secure.getInt(resolver, "fingerprint_adaptive_icon", 0);
            } catch (Exception e) {
                // no action
            }
            if (aod_mode > 0) {
                if (aod_tap > 0) {
                    return SpritePlayer.Mode.SINGLE;
                }
            } else if (fingerprint_unlock > 0) {
                if (fingerprint_icon > 0) {
                    return SpritePlayer.Mode.SINGLE;
                }
            }
        }
        return SpritePlayer.Mode.BLINK;
    }

    public boolean isAnimationPowerSave(int mode) {
        return (prefs.getInt(ANIMATION_POWERSAVE, ANIMATION_POWERSAVE_DEFAULT) & (1 << mode)) == (1 << mode);
    }

    public void setAnimationPowerSave(int mode, boolean enabled) {
        edit();
        try {
            editor.putInt(ANIMATION_POWERSAVE, enabled ?
                    prefs.getInt(ANIMATION_POWERSAVE, ANIMATION_POWERSAVE_DEFAULT) | (1 << mode) :
                    prefs.getInt(ANIMATION_POWERSAVE, ANIMATION_POWERSAVE_DEFAULT) & ~(1 << mode)
            );
        } finally {
            save(true);
        }
    }

    public int getMode(boolean charging, boolean screenOn) {
        return (charging ? SHIFT_CHARGING : SHIFT_BATTERY) + (screenOn ? SHIFT_SCREEN_ON : SHIFT_SCREEN_OFF);
    }
}
