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

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.net.Uri;
import android.os.Bundle;

import androidx.preference.CheckBoxPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;
import eu.chainfire.holeylight.R;
import eu.chainfire.holeylight.misc.Settings;

@SuppressWarnings({"WeakerAccess"})
public class SettingsFragment extends PreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener {
    private SharedPreferences prefs = null;
    private Settings settings = null;

    private CheckBoxPreference prefScreenOn = null;
    private CheckBoxPreference prefScreenOffCharging = null;
    private CheckBoxPreference prefScreenOffBattery = null;
    private CheckBoxPreference prefSeenOnLockscreen = null;
    private CheckBoxPreference prefSeenOnUserPresent = null;
    private Preference prefAdviceAOD = null;
    private Preference prefAdviceLock = null;

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
    public PreferenceCategory category(PreferenceScreen root, int caption) {
        PreferenceCategory retval = new PreferenceCategory(getContext());
        retval.setIconSpaceReserved(false);
        if (caption > 0) retval.setTitle(caption);
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

    @SuppressWarnings("ConstantConditions")
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

        PreferenceCategory catOperation = category(root, R.string.settings_category_operation);
        prefScreenOn = check(catOperation, R.string.settings_screen_on_title, R.string.settings_screen_on_description, Settings.ENABLED_MASTER, settings.isEnabled(), true);
        prefScreenOffCharging = check(catOperation, R.string.settings_screen_off_charging_title, R.string.settings_screen_off_charging_description, Settings.ENABLED_SCREEN_OFF_CHARGING, settings.isEnabledWhileScreenOffCharging(), true);
        prefScreenOffBattery = check(catOperation, R.string.settings_screen_off_battery_title, R.string.settings_screen_off_battery_description, Settings.ENABLED_SCREEN_OFF_BATTERY, settings.isEnabledWhileScreenOffBattery(), false);

        PreferenceCategory catMarkAsSeen = category(root, R.string.settings_category_seen_screen_off_to_on);
        prefSeenOnLockscreen = check(catMarkAsSeen, R.string.settings_seen_on_lockscreen_title, R.string.settings_seen_on_lockscreen_description, Settings.SEEN_ON_LOCKSCREEN, settings.isSeenOnLockscreen(false), true);
        prefSeenOnUserPresent = check(catMarkAsSeen, R.string.settings_seen_on_user_present_title, R.string.settings_seen_on_user_present_description, Settings.SEEN_ON_USER_PRESENT, settings.isSeenOnUserPresent(false), true);

        PreferenceCategory catAnimation = category(root, R.string.settings_category_animation);
        pref(catAnimation, R.string.settings_animation_tune_title, R.string.settings_animation_tune_description, null, true, preference -> {
            startActivity(new Intent(getActivity(), TuneActivity.class));
            return false;
        });
        pref(catAnimation, R.string.settings_animation_colors_title, R.string.settings_animation_colors_description, null, true, preference -> {
            startActivity(new Intent(getActivity(), ColorActivity.class));
            return false;
        });

        PreferenceCategory catChainfire = category(root, R.string.settings_category_chainfire);
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

        PreferenceCategory catAdvice = category(root, R.string.settings_category_advice);
        prefAdviceAOD = pref(catAdvice, R.string.settings_advice_aod_title, 0, null, true, null);
        prefAdviceLock = pref(catAdvice, R.string.settings_advice_lock_title, 0, null, true, null);

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

    private void updatePrefs(String key) {
        if (prefAdviceAOD != null) {
            try {
                if (settings.isEnabledWhileScreenOffCharging() || settings.isEnabledWhileScreenOffBattery()) {
                    int aod_mode = android.provider.Settings.System.getInt(getContext().getContentResolver(), "aod_mode", 0);
                    prefAdviceAOD.setSummary(getString(R.string.settings_advice_aod_description, aod_mode == 1 ? getString(R.string.enabled) : getString(R.string.disabled)));
                } else {
                    prefAdviceAOD.setSummary(R.string.settings_advice_irrelevant);
                }
            } catch (Exception e) {
                // no action
            }
        }
        if (prefAdviceLock != null) {
            try {
                if (settings.isEnabledWhileScreenOffCharging() || settings.isEnabledWhileScreenOffBattery()) {
                    int timeout = android.provider.Settings.Secure.getInt(getContext().getContentResolver(), "lock_screen_lock_after_timeout", 0);
                    prefAdviceLock.setSummary(getString(R.string.settings_advice_lock_description, (int)(timeout / 1000)));
                } else {
                    prefAdviceLock.setSummary(R.string.settings_advice_irrelevant);
                }
            } catch (Exception e) {
                // no action
            }
        }
        if (prefScreenOn != null) {
            prefScreenOn.setChecked(settings.isEnabled()); // for sync with master switch
            prefScreenOffCharging.setEnabled(settings.isEnabled());
            prefScreenOffBattery.setEnabled(settings.isEnabledWhileScreenOffCharging() && false);
            prefSeenOnLockscreen.setEnabled(settings.isEnabledWhileScreenOffAny());
            prefSeenOnUserPresent.setEnabled(settings.isEnabledWhileScreenOffAny());
        }
    }
}
