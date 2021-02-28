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

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.RectF;
import android.net.Uri;
import android.text.Html;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import androidx.preference.PreferenceManager;
import eu.chainfire.holeylight.Application;
import eu.chainfire.holeylight.BuildConfig;
import eu.chainfire.holeylight.R;
import eu.chainfire.holeylight.animation.SpritePlayer;

@SuppressWarnings({"WeakerAccess", "unused", "UnusedReturnValue"})
public class Settings implements SharedPreferences.OnSharedPreferenceChangeListener {
    public static volatile boolean DEBUG = BuildConfig.DEBUG;
    public static volatile boolean DEBUG_OVERLAY = false;

    public static boolean tuning = false;

    public interface OnSettingsChangedListener {
        void onSettingsChanged();
    }

    private static final int SHIFT_SCREEN_ON = 0;
    private static final int SHIFT_SCREEN_OFF = 1;
    private static final int SHIFT_CHARGING = 0;
    private static final int SHIFT_BATTERY = 2;

    public static final int SCREEN_ON_CHARGING = SHIFT_SCREEN_ON + SHIFT_CHARGING;
    public static final int SCREEN_OFF_CHARGING = SHIFT_SCREEN_OFF + SHIFT_CHARGING;
    public static final int SCREEN_ON_BATTERY = SHIFT_SCREEN_ON + SHIFT_BATTERY;
    public static final int SCREEN_OFF_BATTERY = SHIFT_SCREEN_OFF + SHIFT_BATTERY;

    public static final String[] SCREEN_AND_POWER_STATE = new String[] {
            "screen_on_charging",
            "screen_off_charging",
            "screen_on_battery",
            "screen_off_battery"
    };

    // SCREEN_AND_POWER_STATE indexed
    public static final int[] SCREEN_AND_POWER_STATE_DESCRIPTIONS = new int[] {
            R.string.charging_screen_on,
            R.string.charging_screen_off,
            R.string.battery_screen_on,
            R.string.battery_screen_off
    };

    // SCREEN_AND_POWER_STATE indexed
    public static final boolean[] ENABLED_WHILE_DEFAULTS = new boolean[] {
            true,
            true,
            true,
            true
    };

    public static final String ENABLED_WHILE_FMT = "enabled_%s";

    public static final String ENABLED_MASTER = "enabled_master";
    private static final boolean ENABLED_MASTER_DEFAULT = true;

    public static final String ENABLED_LOCKSCREEN = "enabled_lockscreen";
    private static final boolean ENABLED_LOCKSCREEN_DEFAULT = true;
    
    public static class AnimationStyle {
        public static final String HTML_FORMAT = "%s<br><small>%s</small>";
        private static final String TITLE_FORMAT = "%s (%s)";

        public final String name;
        public final SpritePlayer.Mode mode;
        public final int title;
        public final int location;
        public final float battery;

        public AnimationStyle(String name, SpritePlayer.Mode mode, int title, int location, float battery) {
            this.name = name;
            this.mode = mode;
            this.title = title;
            this.location = location;
            this.battery = battery;
        }

        @SuppressWarnings("deprecation")
        public CharSequence getHtmlDisplay(Context context, boolean charging) {
            if (charging) {
                return Html.fromHtml(String.format(Locale.ENGLISH, HTML_FORMAT, context.getString(title), context.getString(location)));
            } else {
                String combinedTitle = String.format(TITLE_FORMAT, context.getString(title), context.getString(location).toLowerCase());
                String description = context.getString(battery < 0 ? R.string.animation_style_percentage_below : R.string.animation_style_percentage_up_to, Math.abs(battery));
                return Html.fromHtml(String.format(Locale.ENGLISH, HTML_FORMAT, combinedTitle, description));
            }
        }
    }
    
    public final AnimationStyle[] ANIMATION_STYLES = new AnimationStyle[] {
            new AnimationStyle("swirl", SpritePlayer.Mode.SWIRL, R.string.animation_style_swirl_title, R.string.animation_style_location_camera, 5.5f),
            new AnimationStyle("blink", SpritePlayer.Mode.BLINK, R.string.animation_style_blink_title, R.string.animation_style_location_camera, 3.5f),
            new AnimationStyle("pie", SpritePlayer.Mode.SINGLE, R.string.animation_style_single_title, R.string.animation_style_location_camera, 3.5f),
            new AnimationStyle("tsp", SpritePlayer.Mode.TSP, R.string.animation_style_tsp_title, R.string.animation_style_location_screen_center, -1f),
    };

    public AnimationStyle getAnimationStyle(String name) {
        for (AnimationStyle as : ANIMATION_STYLES) {
            if (as.name.equals(name)) return as;
        }
        return null;
    }

    public AnimationStyle getAnimationStyle(SpritePlayer.Mode mode) {
        for (AnimationStyle as : ANIMATION_STYLES) {
            if (as.mode == mode) return as;
        }
        return null;
    }

    // SCREEN_AND_POWER_STATE indexed
    public static final String[] ANIMATION_STYLE_DEFAULTS = new String[] {
            "swirl",
            "tsp",
            "blink",
            "tsp"
    };

    private static final String ANIMATION_STYLE_FMT = "animation_%s";

    private static final String SEEN_PICKUP_WHILE_FMT = "seen_pickup_%s";
    public static final boolean SEEN_PICKUP_DEFAULT = false;

    public static final String SEEN_IF_SCREEN_ON = "seen_if_screen_on";
    private static final boolean SEEN_IF_SCREEN_ON_DEFAULT = false;

    public static final String SEEN_ON_LOCKSCREEN = "seen_on_lockscreen";
    private static final boolean SEEN_ON_LOCKSCREEN_DEFAULT = false;

    public static final String SEEN_ON_USER_PRESENT = "seen_on_user_present";
    private static final boolean SEEN_ON_USER_PRESENT_DEFAULT = false;

    private static final String CUTOUT_AREA_LEFT = "cutout_area_left_f";
    private static final String CUTOUT_AREA_TOP = "cutout_area_top_f";
    private static final String CUTOUT_AREA_RIGHT = "cutout_area_right_f";
    private static final String CUTOUT_AREA_BOTTOM = "cutout_area_bottom_f";

    private static final String DP_ADD_SCALE_BASE = "dp_add_scale_base_float";
    private static final String DP_ADD_SCALE_HORIZONTAL = "dp_add_scale_horizontal_float";
    private static final String DP_SHIFT_VERTICAL = "dp_shift_vertical_float";
    private static final String DP_SHIFT_HORIZONTAL = "dp_shift_horizontal_float";
    private static final String DP_ADD_THICKNESS = "dp_add_thickness";

    private static final String SPEED_FACTOR = "speed_factor";

    private static final String CHANNEL_COLOR = "CHANNEL_COLOR:";
    private static final String CHANNEL_COLOR_CONVERSATION = "CHANNEL_COLOR_CONVERSATION:";
    private static final String CHANNEL_COLOR_FMT = CHANNEL_COLOR + "%s:%s";
    private static final String CHANNEL_COLOR_CONVERSATION_FMT = CHANNEL_COLOR_CONVERSATION + "%s:%s";
    public static final String CHANNEL_NAME_DEFAULT = "default";

    private static final String CHANNEL_RESPECT_NOTIFICATION_COLOR_STATE = "RESPECT_NOTIFICATION_COLOR_STATE:";
    private static final String CHANNEL_RESPECT_NOTIFICATION_COLOR_STATE_FMT = CHANNEL_RESPECT_NOTIFICATION_COLOR_STATE + "%s:%s";

    public static final String HIDE_AOD = "hide_aod";
    private static final boolean HIDE_AOD_DEFAULT = false;

    public static final String HIDE_AOD_FULLY = "hide_aod_fully";
    private static final boolean HIDE_AOD_FULLY_DEFAULT = false;

    public static final String RESPECT_DND = "respect_dnd";
    private static final boolean RESPECT_DND_DEFAULT = true;

    private static final String SEEN_TIMEOUT_FMT = "seen_timeout_%s";
    public static final int SEEN_TIMEOUT_DEFAULT = 0;

    private static final String SETUP_WIZARD_COMPLETE = "setup_wizard_complete";

    public static final String UNHOLEY_LIGHT_ICONS = "unholey_icons";
    public static final Boolean UNHOLEY_LIGHT_ICONS_DEFAULT = true;

    public static final String UPDATE_COUNTER = "update_counter";

    public static final String USING_VI_DIRECTOR = "using_vidirector";
    public static final boolean USING_VI_DIRECTOR_DEFAULT = false;

    private static final String SEEN_TIMEOUT_TRACK_SEPARATELY = "seen_timeout_track_separately";
    private static final boolean SEEN_TIMEOUT_TRACK_SEPARATELY_DEFAULT = false;

    private static final String OVERLAY_LINGER = "overlay_linger";
    private static final int OVERLAY_LINGER_DEFAULT = Manufacturer.isSamsung() ? 125 : 0;
    
    public static final String BLACK_FILL = "black_fill";
    public static final boolean BLACK_FILL_DEFAULT = true;

    private static final String DEVICE_OFFICIAL_SUPPORT_WARNING_SHOWN = "device_official_support_warning_shown";

    public static final String AOD_HELPER_CONTROL = "aod_helper_control";
    public static final boolean AOD_HELPER_CONTROL_DEFAULT = false;

    public static final String AOD_HELPER_BRIGHTNESS = "aod_helper_brightness";
    public static final boolean AOD_HELPER_BRIGHTNESS_DEFAULT = false;

    public static final String LOCALE = "locale";

    private static final String PURCHASES = "purchases";

    public static final String AOD_SHOW_CLOCK = "aod_show_clock";
    public static final boolean AOD_SHOW_CLOCK_DEFAULT = false;

    public static final String AOD_IMAGE_INSTRUCTIONS_SHOWN = "aod_image_instructions_shown";

    private static final String ENABLE_DEBUG = "enable_debug";
    private static final String ENABLE_DEBUG_OVERLAY = "enable_debug_overlay";

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
        DEBUG = getDebug(false);
        DEBUG_OVERLAY = getDebugOverlay(false);
    }

    public void setDebug(Boolean debug, Boolean overlay) {
        edit();
        try {
            if (debug == null || overlay == null) {
                editor.remove(ENABLE_DEBUG);
                editor.remove(ENABLE_DEBUG_OVERLAY);
                DEBUG = BuildConfig.DEBUG;
                DEBUG_OVERLAY = false;
            } else {
                editor.putBoolean(ENABLE_DEBUG, debug);
                editor.putBoolean(ENABLE_DEBUG_OVERLAY, debug && overlay);
                DEBUG = debug;
                DEBUG_OVERLAY = debug && overlay;
            }
        } finally {
            save(true);
        }
    }

    public Boolean getDebug(boolean allowNull) {
        if (!prefs.contains(ENABLE_DEBUG) && allowNull) return null;
        return prefs.getBoolean(ENABLE_DEBUG, BuildConfig.DEBUG);
    }

    public Boolean getDebugOverlay(boolean allowNull) {
        if (!prefs.contains(ENABLE_DEBUG_OVERLAY) && allowNull) return null;
        return getDebug(false) && prefs.getBoolean(ENABLE_DEBUG_OVERLAY, false);
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

    private void put(String key, float value, boolean saveImmediately) {
        edit();
        try {
            editor.putFloat(key, value);
        } finally {
            save(saveImmediately);
        }
    }

    private void put(String key, int value, boolean saveImmediately) {
        edit();
        try {
            editor.putInt(key, value);
        } finally {
            save(saveImmediately);
        }
    }

    private void put(String key, boolean value, boolean saveImmediately) {
        edit();
        try {
            editor.putBoolean(key, value);
        } finally {
            save(saveImmediately);
        }
    }

    private void put(String key, String value, boolean saveImmediately) {
        edit();
        try {
            editor.putString(key, value);
        } finally {
            save(saveImmediately);
        }
    }

    public RectF getCutoutAreaRect() {
        return new RectF(
            prefs.getFloat(CUTOUT_AREA_LEFT, -1f),
            prefs.getFloat(CUTOUT_AREA_TOP, -1f),
            prefs.getFloat(CUTOUT_AREA_RIGHT, -1f),
            prefs.getFloat(CUTOUT_AREA_BOTTOM, -1f)
        );
    }

    public Settings setCutoutAreaRect(RectF rect) {
        edit();
        try {
            editor.putFloat(CUTOUT_AREA_LEFT, rect.left);
            editor.putFloat(CUTOUT_AREA_TOP, rect.top);
            editor.putFloat(CUTOUT_AREA_RIGHT, rect.right);
            editor.putFloat(CUTOUT_AREA_BOTTOM, rect.bottom);
        } finally {
            save(true);
        }
        return this;
    }

    public float getDpAddScaleBase(float defaultValue) {
        return prefs.getFloat(DP_ADD_SCALE_BASE, defaultValue);
    }

    public void setDpAddScaleBase(float value) {
        put(DP_ADD_SCALE_BASE, value, true);
    }

    public float getDpAddScaleHorizontal(float defaultValue) {
        return prefs.getFloat(DP_ADD_SCALE_HORIZONTAL, defaultValue);
    }

    public void setDpAddScaleHorizontal(float value) {
        put(DP_ADD_SCALE_HORIZONTAL, value, true);
    }

    public float getDpShiftVertical(float defaultValue) {
        return prefs.getFloat(DP_SHIFT_VERTICAL, defaultValue);
    }

    public void setDpShiftVertical(float value) {
        put(DP_SHIFT_VERTICAL, value, true);
    }

    public float getDpShiftHorizontal(float defaultValue) {
        return prefs.getFloat(DP_SHIFT_HORIZONTAL, defaultValue);
    }

    public void setDpShiftHorizontal(float value) {
        put(DP_SHIFT_HORIZONTAL, value, true);
    }

    public float getDpAddThickness(float defaultValue) {
        return prefs.getFloat(DP_ADD_THICKNESS, defaultValue);
    }

    public void setDpAddThickness(float value) {
        value = Math.min(Math.max(value, 0.0f), 7.5f);

        put(DP_ADD_THICKNESS, value, true);
    }

    public float getSpeedFactor() {
        return prefs.getFloat(SPEED_FACTOR, 1.0f);
    }

    public void setSpeedFactor(float value) {
        value = Math.min(Math.max(value, 0.5f), 2.0f);

        put(SPEED_FACTOR, value, true);
    }

    public boolean isEnabled() {
        return prefs.getBoolean(ENABLED_MASTER, ENABLED_MASTER_DEFAULT);
    }

    public void setEnabled(boolean enabled) {
        put(ENABLED_MASTER, enabled, true);
    }

    public String getEnabledWhileKey(int mode) {
        return String.format(Locale.ENGLISH, ENABLED_WHILE_FMT, SCREEN_AND_POWER_STATE[mode]);
    }

    public void setEnabledWhile(int mode, boolean enabled) {
        put(getEnabledWhileKey(mode), enabled, true);
    }

    public boolean isEnabledWhile(int mode) {
        return isEnabledWhile(mode, true);
    }

    public boolean isEnabledWhile(int mode, boolean effective) {
        return (!effective || isEnabled()) && prefs.getBoolean(getEnabledWhileKey(mode), ENABLED_WHILE_DEFAULTS[mode]);
    }

    public boolean isEnabledWhileScreenOn() {
        return isEnabledWhile(Settings.SCREEN_ON_CHARGING) || isEnabledWhile(Settings.SCREEN_ON_BATTERY);
    }

    public boolean isEnabledWhileScreenOff() {
        return isEnabledWhile(Settings.SCREEN_OFF_CHARGING) || isEnabledWhile(Settings.SCREEN_OFF_BATTERY);
    }

    public boolean isEnabledOnLockscreen() {
        return isEnabledWhileScreenOn() && prefs.getBoolean(ENABLED_LOCKSCREEN, ENABLED_LOCKSCREEN_DEFAULT);
    }

    public long refreshNotificationsKey() {
        long ret = isEnabled() ? 1 : 0;
        int shift = 1;
        for (int i = 0; i < SCREEN_AND_POWER_STATE.length; i++) {
            ret += (isEnabledWhile(i) ? 1 : 0) << shift;
            shift++;
        }
        ret += (isEnabledOnLockscreen() ? 1 : 0) << shift;
        return ret;
    }

    public int getColorForPackageAndChannel(String packageName, String channelName, boolean conversation, int defaultValue, boolean returnAppDefault) {
        if (channelName == null) {
            channelName = CHANNEL_NAME_DEFAULT;
            conversation = false;
        }
        String keyChannel = String.format(Locale.ENGLISH, conversation ? CHANNEL_COLOR_CONVERSATION_FMT : CHANNEL_COLOR_FMT, packageName, channelName);
        String keyDefault = String.format(Locale.ENGLISH, CHANNEL_COLOR_FMT, packageName, CHANNEL_NAME_DEFAULT);
        if (prefs.contains(keyChannel)) {
            return prefs.getInt(keyChannel, defaultValue);
        } else if (prefs.contains(keyDefault) && returnAppDefault) {
            return prefs.getInt(keyDefault, defaultValue);
        }
        return defaultValue;
    }

    public void setColorForPackageAndChannel(String packageName, String channelName, boolean conversation, int color, boolean fromListener) {
        if (channelName == null) {
            channelName = CHANNEL_NAME_DEFAULT;
            conversation = false;
        }
        String key = String.format(Locale.ENGLISH, conversation ? CHANNEL_COLOR_CONVERSATION_FMT : CHANNEL_COLOR_FMT, packageName, channelName);
        if (!prefs.contains(key) || (prefs.getInt(key, -1) != color)) {
            put(key, color, fromListener);
        }
    }

    public void deleteColorForPackageAndChannel(String packageName, String channelName) {
        if (channelName == null) channelName = CHANNEL_NAME_DEFAULT;
        String key = String.format(Locale.ENGLISH, CHANNEL_COLOR_FMT, packageName, channelName);
        if (prefs.contains(key)) {
            edit();
            try {
                editor.remove(key);
            } finally {
                save(true);
            }
        }
    }

    public static class PackageColor {
        public final String packageName;
        public final String channelName;
        public final boolean conversation;
        public final int color;

        public PackageColor(String packageName, String channelName, boolean conversation, int color) {
            this.packageName = packageName;
            this.channelName = channelName;
            this.conversation = conversation;
            this.color = color;
        }
    }

    public Map<String, PackageColor> getPackagesChannelsAndColors() {
        Map<String, PackageColor> ret = new HashMap<>();
        Map<String, ?> all = prefs.getAll();
        for (String key : all.keySet()) {
            String content;
            boolean conversation;
            if (key.startsWith(CHANNEL_COLOR)) {
                content = key.substring(CHANNEL_COLOR.length());
                conversation = false;
            } else if (key.startsWith(CHANNEL_COLOR_CONVERSATION)) {
                content = key.substring(CHANNEL_COLOR_CONVERSATION.length());
                conversation = true;
            } else continue;

            int sep = content.indexOf(':');
            if (sep >= 0) {
                String pkg = content.substring(0, sep);
                String chan = content.substring(sep + 1);
                int color = prefs.getInt(key, 0);
                ret.put(key, new PackageColor(pkg, chan, conversation, color));
            }
        }
        return ret;
    }

    public boolean isRespectNotificationColorStateForPackageAndChannel(String packageName, String channelName) {
        return prefs.getBoolean(String.format(CHANNEL_RESPECT_NOTIFICATION_COLOR_STATE_FMT, packageName, channelName), false);
    }
    
    public void setRespectNotificationColorStateForPackageAndChannel(String packageName, String channelName, boolean value) {
        put(String.format(CHANNEL_RESPECT_NOTIFICATION_COLOR_STATE_FMT, packageName, channelName), value, true);
    }

    public String getSeenPickupWhileKey(int mode) {
        return String.format(Locale.ENGLISH, SEEN_PICKUP_WHILE_FMT, SCREEN_AND_POWER_STATE[mode]);
    }
    
    public void setSeenPickupWhile(int mode, boolean seenPickup) {
        put(getSeenPickupWhileKey(mode), seenPickup, true);
    }

    public boolean isSeenPickupWhile(int mode, boolean effective) {
        return (!effective || isEnabledWhile(mode)) && prefs.getBoolean(getSeenPickupWhileKey(mode), SEEN_PICKUP_DEFAULT);
    }
    
    public boolean isSeenIfScreenOn(boolean effective) {
        return (!effective || isEnabled()) && prefs.getBoolean(SEEN_IF_SCREEN_ON, SEEN_IF_SCREEN_ON_DEFAULT);
    }

    public boolean isSeenOnLockscreen(boolean effective) {
        return (!effective || isEnabledWhileScreenOff()) && prefs.getBoolean(SEEN_ON_LOCKSCREEN, SEEN_ON_LOCKSCREEN_DEFAULT);
    }

    public boolean isSeenOnUserPresent(boolean effective) {
        return (!effective || isEnabledWhileScreenOff()) && prefs.getBoolean(SEEN_ON_USER_PRESENT, SEEN_ON_USER_PRESENT_DEFAULT);
    }

    public String getAnimationModeKey(int mode) {
        return String.format(Locale.ENGLISH, ANIMATION_STYLE_FMT, SCREEN_AND_POWER_STATE[mode]);
    }

    public SpritePlayer.Mode getAnimationMode(int mode) {
        AnimationStyle as = getAnimationStyle(prefs.getString(getAnimationModeKey(mode), ANIMATION_STYLE_DEFAULTS[mode]));
        if (as != null) return as.mode;
        return null;
    }

    public void setAnimationMode(int mode, SpritePlayer.Mode animationMode) {
        AnimationStyle as = getAnimationStyle(animationMode);
        if (as != null) {
            put(getAnimationModeKey(mode), as.name, true);
        }
    }

    public int getMode(boolean charging, boolean screenOn) {
        return (charging ? SHIFT_CHARGING : SHIFT_BATTERY) + (screenOn ? SHIFT_SCREEN_ON : SHIFT_SCREEN_OFF);
    }

    public void resetTuning() {
        edit();
        try {
            for (String key : new String[] {
                    DP_ADD_SCALE_BASE,
                    DP_ADD_SCALE_HORIZONTAL,
                    DP_SHIFT_VERTICAL,
                    DP_SHIFT_HORIZONTAL,
                    DP_ADD_THICKNESS,
                    SPEED_FACTOR
            }) {
                if (prefs.contains(key)) {
                    editor.remove(key);
                }
            }
        } finally {
            save(true);
        }
    }

    public boolean isHideAOD() {
        return prefs.getBoolean(HIDE_AOD, HIDE_AOD_DEFAULT);
    }

    public void setHideAOD(boolean hide) {
        put(HIDE_AOD, hide, true);
    }
    
    public void setHideAOD(boolean hide, boolean fully) {
        edit();
        try {
            setHideAOD(hide);
            setHideAODFully(fully);
        } finally {
            save(true);
        }
    }

    public boolean isHideAODFully() {
        return prefs.getBoolean(HIDE_AOD_FULLY, HIDE_AOD_FULLY_DEFAULT);
    }

    public void setHideAODFully(boolean fully) {
        put(HIDE_AOD_FULLY, fully, true);
    }

    public boolean isSetupWizardComplete() {
        return prefs.getBoolean(SETUP_WIZARD_COMPLETE, false);
    }

    public void setSetupWizardComplete(boolean complete) {
        put(SETUP_WIZARD_COMPLETE, complete, true);
    }

    public boolean isRespectDoNotDisturb() {
        return prefs.getBoolean(RESPECT_DND, RESPECT_DND_DEFAULT);
    }

    public boolean haveSeenTimeoutAny() {
        return
                (getSeenTimeout(SCREEN_ON_CHARGING) > 0) ||
                (getSeenTimeout(SCREEN_OFF_CHARGING) > 0) ||
                (getSeenTimeout(SCREEN_ON_BATTERY) > 0) ||
                (getSeenTimeout(SCREEN_OFF_BATTERY) > 0);
    }

    public String getSeenTimeoutKey(int mode) {
        return String.format(Locale.ENGLISH, SEEN_TIMEOUT_FMT, SCREEN_AND_POWER_STATE[mode]);
    }

    public int getSeenTimeout(int mode) {
        return prefs.getInt(getSeenTimeoutKey(mode), SEEN_TIMEOUT_DEFAULT);
    }

    public void setSeenTimeout(int mode, int value) {
        put(getSeenTimeoutKey(mode), value, true);
    }

    public boolean isUnholeyLightIcons() {
        return prefs.getBoolean(UNHOLEY_LIGHT_ICONS, UNHOLEY_LIGHT_ICONS_DEFAULT);
    }

    public int getUpdateCounter() {
        return prefs.getInt(UPDATE_COUNTER, 0);
    }

    public void incUpdateCounter() {
        put(UPDATE_COUNTER, (getUpdateCounter() + 1) % 1000, true);
    }

    public boolean isUsingVIDirector() {
        return prefs.getBoolean(USING_VI_DIRECTOR, USING_VI_DIRECTOR_DEFAULT);
    }

    public void setUsingVIDirector(boolean value) {
        if (value == isUsingVIDirector()) return;

        put(USING_VI_DIRECTOR, value, true);
        resetTuning();
    }

    public boolean isSeenTimeoutTrackSeparately() {
        return prefs.getBoolean(SEEN_TIMEOUT_TRACK_SEPARATELY, SEEN_TIMEOUT_TRACK_SEPARATELY_DEFAULT);
    }

    public void setSeenTimeoutTrackSeparately(boolean value) {
        put(SEEN_TIMEOUT_TRACK_SEPARATELY, value, true);
    }

    public int getOverlayLinger() {
        return prefs.getInt(OVERLAY_LINGER, OVERLAY_LINGER_DEFAULT);
    }

    public void setOverlayLinger(int value) {
        put(OVERLAY_LINGER, value, true);
    }

    public boolean isBlackFill() {
        return prefs.getBoolean(BLACK_FILL, BLACK_FILL_DEFAULT);
    }
    
    public void setBlackFill(boolean value) {
        put(BLACK_FILL, value, true);
    }

    public boolean isDeviceOfficialSupportWarningShown() {
        return prefs.getBoolean(DEVICE_OFFICIAL_SUPPORT_WARNING_SHOWN, false);
    }

    public void setDeviceOfficialSupportWarningShown(boolean value) {
        put(DEVICE_OFFICIAL_SUPPORT_WARNING_SHOWN, value, true);
    }
    
    public boolean isAODHelperControl() {
        return prefs.getBoolean(AOD_HELPER_CONTROL, AOD_HELPER_CONTROL_DEFAULT);
    }
    
    public void setAODHelperControl(boolean value) {
        put(AOD_HELPER_CONTROL, value, true);
    }

    public boolean isAODHelperBrightness() {
        return prefs.getBoolean(AOD_HELPER_BRIGHTNESS, AOD_HELPER_BRIGHTNESS_DEFAULT);
    }
    
    public void setAODHelperBrightness(boolean value) {
        put(AOD_HELPER_BRIGHTNESS, value, true);
    }

    public String getLocale(boolean resolveDefault) {
        String ret = prefs.getString(LOCALE, "");
        if (ret == null) ret = "";
        if (ret.equals("") && resolveDefault) ret = Application.defaultLocale;
        return ret;
    }

    public void setLocale(String locale) {
        put(LOCALE, locale != null ? locale : "", true);
    }

    public boolean isAODImageInstructionsShown() {
        return prefs.getBoolean(AOD_IMAGE_INSTRUCTIONS_SHOWN, false);
    }

    public void setAODImageInstructionsShown() {
        put(AOD_IMAGE_INSTRUCTIONS_SHOWN, true, true);
    }

    public String[] getPurchased() {
        String[] ret = prefs.getString(PURCHASES, "").split(",");
        if (ret.length == 1 && (ret[0] == null || ret[0].equals(""))) return new String[0];
        return ret;
    }

    public boolean isPurchased(String sku) {
        String[] purchased = getPurchased();
        boolean found = false;
        for (String purchase : purchased) {
            if (sku.equals(purchase)) {
                return true;
            }
        }
        return false;
    }

    public void setPurchased(String sku) {
        if (!isPurchased(sku)) {
            String[] purchased = getPurchased();
            StringBuilder sb = new StringBuilder();
            for (String s : purchased) {
                sb.append(s);
                sb.append(",");
            }
            sb.append(sku);
            put(PURCHASES, sb.toString(), true);
        }
    }

    public boolean isShowAODClock() {
        return prefs.getBoolean(AOD_SHOW_CLOCK, AOD_SHOW_CLOCK_DEFAULT);
    }

    public void setShowAODClock(boolean value) {
        put(AOD_SHOW_CLOCK, value, true);
    }

    public boolean saveToUri(ContentResolver resolver, Uri uri) {
        OutputStream outputStream;
        try {
            outputStream = resolver.openOutputStream(uri);
            BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(outputStream));

            bufferedWriter.write(BuildConfig.APPLICATION_ID + " 1\r\n");

            Map<String, ?> prefsMap = prefs.getAll();
            for (String key : prefsMap.keySet()) {
                Object value = prefsMap.get(key);
                if (value != null) {
                    if (key.startsWith(CHANNEL_COLOR)) {
                        bufferedWriter.write(String.format(Locale.ENGLISH, "i 0x%08X %s\r\n", (Integer)value, key));
                    } else if (key.startsWith(CHANNEL_RESPECT_NOTIFICATION_COLOR_STATE)) {
                        bufferedWriter.write(String.format(Locale.ENGLISH, "b %d %s\r\n", (Boolean)value ? 1 : 0, key));
                    }
                }
            }

            bufferedWriter.flush();
            bufferedWriter.close();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    
    public boolean loadFromUri(ContentResolver resolver, Uri uri, boolean clear, boolean overwrite) {
        InputStream inputStream;
        try {
            inputStream = resolver.openInputStream(uri);
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));

            String line;
            boolean first = true;
            while ((line = bufferedReader.readLine()) != null) {
                if (first) {
                    if (!line.trim().equals(BuildConfig.APPLICATION_ID + " 1")) {
                        return false;
                    }
                    first = false;

                    if (clear) {
                        Map<String, ?> prefsMap = prefs.getAll();
                        List<String> toRemove = new ArrayList<>();
                        for (String key : prefsMap.keySet()) {
                            Object value = prefsMap.get(key);
                            if (value != null) {
                                if (key.startsWith(CHANNEL_COLOR) || key.startsWith(CHANNEL_RESPECT_NOTIFICATION_COLOR_STATE)) {
                                    toRemove.add(key);
                                }
                            }
                        }
                        edit();
                        try {
                            for (String key : toRemove) {
                                editor.remove(key);
                            }
                        } finally {
                            save(true);
                        }
                    }
                } else {
                    edit();
                    try {
                        String[] parts = line.trim().split(" ", 3);
                        if (parts.length == 3) {
                            String key = parts[2];
                            if (!overwrite && prefs.contains(key)) continue;
                            if (parts[0].equals("i")) {
                                editor.putInt(key, Long.decode(parts[1]).intValue());
                            } else if (parts[0].equals("b")) {
                                editor.putBoolean(key, parts[1].equals("1"));
                            }
                        }
                    } finally {
                        save(true);
                    }
                }
            }

            bufferedReader.close();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
