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

package eu.chainfire.holeylight.ui;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.net.Uri;
import android.os.Bundle;
import android.text.Html;

import androidx.preference.CheckBoxPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;
import eu.chainfire.holeylight.R;
import eu.chainfire.holeylight.animation.SpritePlayer;
import eu.chainfire.holeylight.misc.AODControl;
import eu.chainfire.holeylight.misc.Settings;

@SuppressWarnings({"WeakerAccess"})
public class SettingsFragment extends PreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener {
    private SharedPreferences prefs = null;
    private Settings settings = null;

    private CheckBoxPreference prefScreenOn = null;
    private CheckBoxPreference prefScreenOffCharging = null;
    private CheckBoxPreference prefScreenOffBattery = null;
    private Preference prefAnimationScreenOnCharging = null;
    private Preference prefAnimationScreenOnBattery = null;
    private Preference prefAnimationScreenOffCharging = null;
    private Preference prefAnimationScreenOffBattery = null;
    private CheckBoxPreference prefLockscreenOn = null;
    private CheckBoxPreference prefSeenPickupScreenOnCharging = null;
    private CheckBoxPreference prefSeenPickupScreenOffCharging = null;
    private CheckBoxPreference prefSeenPickupScreenOnBattery = null;
    private CheckBoxPreference prefSeenPickupScreenOffBattery = null;
    private CheckBoxPreference prefSeenOnLockscreen = null;
    private CheckBoxPreference prefSeenOnUserPresent = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);        
        
        settings = Settings.getInstance(getActivity());

        setPreferenceScreen(createPreferenceHierarchy());
    }

    @Override
    public void onDestroy() {
        if (prefs != null) {
            prefs.unregisterOnSharedPreferenceChangeListener(this);
        }
        super.onDestroy();
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
    }

    @SuppressWarnings("ConstantConditions")
    public PreferenceCategory category(PreferenceScreen root, int caption, int summary) {
        PreferenceCategory retval = new PreferenceCategory(getContext());
        retval.setIconSpaceReserved(false);
        if (caption > 0) retval.setTitle(caption);
        if (summary > 0) retval.setSummary(summary);
        root.addPreference(retval);
        return retval;
    }

    @SuppressWarnings("ConstantConditions")
    public Preference pref(PreferenceCategory category, int caption, int summary, String key, boolean enabled, Preference.OnPreferenceClickListener clickListener) {
        Preference retval = new Preference(getContext());
        if (caption > 0) retval.setTitle(caption);
        if (summary > 0) retval.setSummary(summary);
        retval.setEnabled(enabled);
        if (key != null) retval.setKey(key);
        retval.setIconSpaceReserved(false);
        if (clickListener != null) retval.setOnPreferenceClickListener(clickListener);
        if (category != null) category.addPreference(retval);
        return retval;
    }

    @SuppressWarnings("ConstantConditions")
    public CheckBoxPreference check(PreferenceCategory category, int caption, int summary, String key, Object defaultValue, boolean enabled) {
        CheckBoxPreference retval = new CheckBoxPreference(getContext());
        if (caption > 0) retval.setTitle(caption);
        if (summary > 0) retval.setSummary(summary);
        retval.setEnabled(enabled);
        retval.setKey(key);
        retval.setDefaultValue(defaultValue);
        retval.setIconSpaceReserved(false);
        if (category != null) category.addPreference(retval);
        return retval;
    }

    @SuppressWarnings({"ConstantConditions", "deprecation"})
    private PreferenceScreen createPreferenceHierarchy() {
        PreferenceScreen root = getPreferenceManager().createPreferenceScreen(getActivity());
        
        prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());

        String title = getActivity().getString(R.string.app_name);
        try {
            PackageInfo pkg = getActivity().getPackageManager().getPackageInfo(getActivity().getPackageName(), 0);
            title = title + " v" + pkg.versionName;
        } catch (Exception e) {
            // no action
        }
        
        Preference copyright = pref(null, R.string.app_name, R.string.app_details, "copyright", true, preference -> {
            Intent i = new Intent(Intent.ACTION_VIEW);
            i.setData(Uri.parse(getString(R.string.app_website_url)));
            startActivity(i);
            return false;
        });
        copyright.setTitle(title);
        root.addPreference(copyright);

        Preference basicHelp = pref(null, R.string.help_basic_title, R.string.help_basic_descrption, null, true, preference -> {
            (new AlertDialog.Builder(getContext()))
                    .setTitle(R.string.help_basic_title)
                    .setMessage(Html.fromHtml(getString(R.string.help_basic_message)))
                    .setPositiveButton(android.R.string.ok, null)
                    .show();
           return false;
        });
        root.addPreference(basicHelp);

        PreferenceCategory catOperation = category(root, R.string.settings_category_operation_title, 0);
        prefScreenOn = check(catOperation, R.string.settings_screen_on_title, R.string.settings_screen_on_description, Settings.ENABLED_SCREEN_ON, settings.isEnabledWhileScreenOn(), true);
        prefScreenOffCharging = check(catOperation, R.string.settings_screen_off_charging_title, R.string.settings_screen_off_charging_description, Settings.ENABLED_SCREEN_OFF_CHARGING, settings.isEnabledWhileScreenOffCharging(), true);
        prefScreenOffBattery = check(catOperation, R.string.settings_screen_off_battery_title, R.string.settings_screen_off_battery_description, Settings.ENABLED_SCREEN_OFF_BATTERY, settings.isEnabledWhileScreenOffBattery(), false);
        prefLockscreenOn = check(catOperation, R.string.settings_lockscreen_on_title, R.string.settings_lockscreen_on_description, Settings.ENABLED_LOCKSCREEN, settings.isEnabledOnLockscreen(), true);
        if (AODControl.haveHelperPackage(getContext(), true)) {
            check(catOperation, R.string.temp_settings_hide_aod_title, R.string.temp_settings_hide_aod_description, Settings.HIDE_AOD, settings.isHideAOD(), true);
        }

        PreferenceCategory catAnimation = category(root, R.string.settings_category_animation_title, 0);
        pref(catAnimation, R.string.settings_animation_tune_title, R.string.settings_animation_tune_description, null, true, preference -> {
            startActivity(new Intent(getActivity(), TuneActivity.class));
            return false;
        });
        pref(catAnimation, R.string.settings_animation_colors_title, R.string.settings_animation_colors_description_v2, null, true, preference -> {
            startActivity(new Intent(getActivity(), ColorActivity.class));
            return false;
        });

        Preference.OnPreferenceClickListener animationClickListener = preference -> {
            final int mode;
            if (preference == prefAnimationScreenOnCharging) mode = Settings.SCREEN_ON_CHARGING;
            else if (preference == prefAnimationScreenOffCharging) mode = Settings.SCREEN_OFF_CHARGING;
            else if (preference == prefAnimationScreenOnBattery) mode = Settings.SCREEN_ON_BATTERY;
            else if (preference == prefAnimationScreenOffBattery) mode = Settings.SCREEN_OFF_BATTERY;
            else return false;

            boolean screenOff = (mode == Settings.SCREEN_OFF_CHARGING) || (mode == Settings.SCREEN_OFF_BATTERY);

            CharSequence[] options = new CharSequence[screenOff ? 4 : 3];
            options[0] = Html.fromHtml(getString(Settings.ANIMATION_STYLE_TITLES[0]) + "<br><small>" + getString(Settings.ANIMATION_STYLE_DESCRIPTIONS[0]) + "</small>");
            options[1] = Html.fromHtml(getString(Settings.ANIMATION_STYLE_TITLES[1]) + "<br><small>" + getString(Settings.ANIMATION_STYLE_DESCRIPTIONS[1]) + "</small>");
            options[2] = Html.fromHtml(getString(Settings.ANIMATION_STYLE_TITLES[2]) + "<br><small>" + getString(Settings.ANIMATION_STYLE_DESCRIPTIONS[2]) + "</small>");
            if (screenOff) {
                options[3] = Html.fromHtml(getString(Settings.ANIMATION_STYLE_TITLES[3]) + "<br><small>" + getString(Settings.ANIMATION_STYLE_DESCRIPTIONS[3]) + "</small>");
            }

            if (mode >= 0) {
                (new AlertDialog.Builder(getContext()))
                        .setTitle(R.string.settings_animation_style_dialog_title)
                        .setItems(options, (dialog, which) -> settings.setAnimationMode(mode, Settings.ANIMATION_STYLE_VALUES[which]))
                        .setNegativeButton(android.R.string.cancel, null)
                        .show();
            }
            return false;
        };
        prefAnimationScreenOnCharging = pref(catAnimation, R.string.settings_animation_screen_on_charging_title, 0, null, true, animationClickListener);
        prefAnimationScreenOffCharging = pref(catAnimation, R.string.settings_animation_screen_off_charging_title, 0, null, true, animationClickListener);
        prefAnimationScreenOnBattery = pref(catAnimation, R.string.settings_animation_screen_on_battery_title, 0, null, true, animationClickListener);
        prefAnimationScreenOffBattery = pref(catAnimation, R.string.settings_animation_screen_off_battery_title, 0, null, true, animationClickListener);

        PreferenceCategory catMarkAsSeen;

        catMarkAsSeen = category(root, R.string.settings_category_seen_pickup_title, R.string.settings_category_seen_pickup_description);
        prefSeenPickupScreenOnCharging = check(catMarkAsSeen, R.string.settings_seen_pickup_screen_on_charging_title, R.string.settings_seen_pickup_screen_on_charging_description, Settings.SEEN_PICKUP_SCREEN_ON_CHARGING, settings.isSeenPickupScreenOnCharging(false), true);
        prefSeenPickupScreenOffCharging = check(catMarkAsSeen, R.string.settings_seen_pickup_screen_off_charging_title, R.string.settings_seen_pickup_screen_off_charging_description, Settings.SEEN_PICKUP_SCREEN_OFF_CHARGING, settings.isSeenPickupScreenOffCharging(false), true);
        prefSeenPickupScreenOnBattery = check(catMarkAsSeen, R.string.settings_seen_pickup_screen_on_battery_title, R.string.settings_seen_pickup_screen_on_battery_description, Settings.SEEN_PICKUP_SCREEN_ON_BATTERY, settings.isSeenPickupScreenOnBattery(false), true);
        prefSeenPickupScreenOffBattery = check(catMarkAsSeen, R.string.settings_seen_pickup_screen_off_battery_title, R.string.settings_seen_pickup_screen_off_battery_description, Settings.SEEN_PICKUP_SCREEN_OFF_BATTERY, settings.isSeenPickupScreenOffBattery(false), true);

        catMarkAsSeen = category(root, R.string.settings_category_seen_lockscreen_title, 0);
        prefSeenOnLockscreen = check(catMarkAsSeen, R.string.settings_seen_on_lockscreen_title, R.string.settings_seen_on_lockscreen_description, Settings.SEEN_ON_LOCKSCREEN, settings.isSeenOnLockscreen(false), true);
        prefSeenOnUserPresent = check(catMarkAsSeen, R.string.settings_seen_on_user_present_title, R.string.settings_seen_on_user_present_description, Settings.SEEN_ON_USER_PRESENT, settings.isSeenOnUserPresent(false), true);

        PreferenceCategory catChainfire = category(root, R.string.settings_category_chainfire_title, 0);
        pref(catChainfire, R.string.settings_playstore_title, R.string.settings_playstore_description, null, true, preference -> {
            try {
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse("market://search?q=pub:Chainfire"));
                startActivity(i);
            } catch (Exception e) {
                // Play Store not installed
            }
            return false;
        });
        pref(catChainfire, R.string.settings_follow_twitter_title, R.string.settings_follow_twitter_description, null,true, preference -> {
            try {
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse("http://www.twitter.com/ChainfireXDA"));
                startActivity(i);
            } catch (Exception e) {
                //  no action
            }
            return false;
        });

        updatePrefs(null);
        prefs.registerOnSharedPreferenceChangeListener(this);
        return root;
    }

    @Override
    public void onStart() {
        super.onStart();
        updatePrefs(null);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        try {
            updatePrefs(key);
        } catch (Throwable t) {
            // no action
        }
    }

    private String getAnimationStyleTitle(int mode) {
        SpritePlayer.Mode animationMode = settings.getAnimationMode(mode);
        for (int i = 0; i < Settings.ANIMATION_STYLE_VALUES.length; i++) {
            if (Settings.ANIMATION_STYLE_VALUES[i] == animationMode) {
                return getString(Settings.ANIMATION_STYLE_TITLES[i]);
            }
        }
        return "?";
    }

    @SuppressWarnings({ "unused", "ConstantConditions" })
    private void updatePrefs(String key) {
        if (prefScreenOn != null) {
            prefScreenOn.setEnabled(settings.isEnabled());
            prefScreenOffCharging.setEnabled(settings.isEnabled());
            prefScreenOffBattery.setEnabled(settings.isEnabled());
            prefLockscreenOn.setEnabled(settings.isEnabledWhileScreenOn());
            prefSeenPickupScreenOnCharging.setEnabled(settings.isEnabledWhileScreenOn());
            prefSeenPickupScreenOffCharging.setEnabled(settings.isEnabledWhileScreenOffCharging());
            prefSeenPickupScreenOnBattery.setEnabled(settings.isEnabledWhileScreenOn());
            prefSeenPickupScreenOffBattery.setEnabled(settings.isEnabledWhileScreenOffBattery());
            prefSeenOnLockscreen.setEnabled(settings.isEnabledWhileScreenOffAny());
            prefSeenOnUserPresent.setEnabled(settings.isEnabledWhileScreenOffAny());

            prefAnimationScreenOnCharging.setEnabled(settings.isEnabledWhileScreenOn());
            prefAnimationScreenOnCharging.setSummary(getString(R.string.settings_animation_screen_on_charging_description, getAnimationStyleTitle(Settings.SCREEN_ON_CHARGING)));
            prefAnimationScreenOffCharging.setEnabled(settings.isEnabledWhileScreenOffCharging());
            prefAnimationScreenOffCharging.setSummary(getString(R.string.settings_animation_screen_off_charging_description, getAnimationStyleTitle(Settings.SCREEN_OFF_CHARGING)));
            prefAnimationScreenOnBattery.setEnabled(settings.isEnabledWhileScreenOn());
            prefAnimationScreenOnBattery.setSummary(getString(R.string.settings_animation_screen_on_battery_description, getAnimationStyleTitle(Settings.SCREEN_ON_BATTERY)));
            prefAnimationScreenOffBattery.setEnabled(settings.isEnabledWhileScreenOffBattery());
            prefAnimationScreenOffBattery.setSummary(getString(R.string.settings_animation_screen_off_battery_description, getAnimationStyleTitle(Settings.SCREEN_OFF_BATTERY)));
        }
    }
}
