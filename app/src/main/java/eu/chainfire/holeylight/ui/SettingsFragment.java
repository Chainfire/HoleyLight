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

package eu.chainfire.holeylight.ui;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.res.AssetManager;
import android.net.Uri;
import android.os.Bundle;
import android.text.Html;
import android.text.format.DateFormat;
import android.util.TypedValue;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Locale;

import androidx.preference.CheckBoxPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;
import eu.chainfire.holeylight.BuildConfig;
import eu.chainfire.holeylight.R;
import eu.chainfire.holeylight.animation.SpritePlayer;
import eu.chainfire.holeylight.misc.AODControl;
import eu.chainfire.holeylight.misc.Manufacturer;
import eu.chainfire.holeylight.misc.Settings;

@SuppressWarnings({"WeakerAccess"})
public class SettingsFragment extends PreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener {
    private SharedPreferences prefs = null;
    private Settings settings = null;

    private CheckBoxPreference prefScreenOnCharging = null;
    private CheckBoxPreference prefScreenOffCharging = null;
    private CheckBoxPreference prefScreenOnBattery = null;
    private CheckBoxPreference prefScreenOffBattery = null;
    private CheckBoxPreference prefLockscreenOn = null;
    private Preference prefAODSchedule = null;
    private CheckBoxPreference prefRespectDND = null;
    private Preference prefTune = null;
    private Preference prefColors = null;
    private CheckBoxPreference prefBlackFill = null;
    private CheckBoxPreference prefUnholeyIcons = null;
    private CheckBoxPreference prefUnholeyClock = null;
    private CheckBoxPreference prefOverlayLinger = null;
    private CheckBoxPreference prefSeenPickup = null;
    private CheckBoxPreference prefSeenOnLockscreen = null;
    private CheckBoxPreference prefSeenOnUserPresent = null;
    private CheckBoxPreference prefSeenIfScreenOn = null;
    private CheckBoxPreference prefSeenOnTimeout = null;
    private CheckBoxPreference prefHideAOD = null;
    private Preference prefAODHelper = null;
    private CheckBoxPreference prefAODHelperControl = null;
    private CheckBoxPreference prefAODHelperBrightness = null;
    private ListPreference prefLocale = null;
    private PreferenceCategory catDebug = null;
    private Preference prefDebug = null;

    private Preference prefDonateTop = null;
    private Preference prefDonateBottom = null;

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

    @SuppressWarnings("ConstantConditions")
    public ListPreference list(PreferenceCategory category, int caption, int summary, int dialogCaption, String key, Object defaultValue, CharSequence[] entries, CharSequence[] entryValues, boolean enabled) {
        ListPreference retval = new ListPreference(getContext());
        if (caption > 0) retval.setTitle(caption);
        if (summary > 0) retval.setSummary(summary);
        retval.setEnabled(enabled);
        if (key != null) retval.setKey(key);
        retval.setDefaultValue(defaultValue);
        retval.setDialogTitle(dialogCaption);
        retval.setEntries(entries);
        retval.setEntryValues(entryValues);
        retval.setIconSpaceReserved(false);
        if (category != null) category.addPreference(retval);
        return retval;
    }

    private int getModeFromPreference(Preference preference) {
        if (preference == prefScreenOnCharging) return Settings.SCREEN_ON_CHARGING;
        else if (preference == prefScreenOffCharging) return Settings.SCREEN_OFF_CHARGING;
        else if (preference == prefScreenOnBattery) return Settings.SCREEN_ON_BATTERY;
        else if (preference == prefScreenOffBattery) return Settings.SCREEN_OFF_BATTERY;
        return -1;
    }

    private class TimeoutHelper {
        public TimeoutHelper() { }

        public void save() { }

        public void close() { }
    }

    private class TimeoutTimeHelper extends TimeoutHelper {
        private final int[] VALUES = new int[] {
            0,
            5 * 1000,
            10 * 1000,
            30 * 1000,
            60 * 1000,
            2 * 60 * 1000,
            5 * 60 * 1000,
            10 * 60 * 1000,
            30 * 60 * 1000,
            60 * 60 * 1000,
            2 * 60 * 60 * 1000
        };

        private final SeekBar seekBar;
        private final TextView textValue;
        private final int mode;
        private AlertDialog base;

        public TimeoutTimeHelper(AlertDialog base, int seekBarId, int textValueId, int mode) {
            super();
            seekBar = base.findViewById(seekBarId);
            textValue = base.findViewById(textValueId);
            this.mode = mode;
            this.base = base;

            seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    textValue.setText(getDescriptionFromIndex(progress));
                    base.setTitle(getDescriptionFromIndex(progress));
                }
                @Override public void onStartTrackingTouch(SeekBar seekBar) {
                }
                @Override public void onStopTrackingTouch(SeekBar seekBar) {
                    base.setTitle(R.string.settings_seen_on_timeout_title);
                }
            });
            seekBar.setMax(VALUES.length - 1);
            seekBar.setProgress(getIndexFromValue(settings.getSeenTimeout(mode)), false);
            textValue.setText(getDescriptionFromIndex(seekBar.getProgress()));
            base.setTitle(R.string.settings_seen_on_timeout_title);
        }

        public int getIndexFromValue(int value) {
            for (int i = 0; i < VALUES.length; i++) {
                if (value <= VALUES[i]) return i;
            }
            return 0;
        }

        public int getValueFromIndex(int index) {
            return VALUES[index];
        }

        public int getValue() {
            return getValueFromIndex(seekBar.getProgress());
        }

        public String getDescriptionFromIndex(int index) {
            int ms = getValueFromIndex(index);
            if (ms == 0) return getString(R.string.settings_seen_on_timeout_none);
            if (ms < 2 * 60 * 1000) return getString(R.string.x_seconds, ms / 1000);
            if (ms < 2 * 60 * 60 * 1000) return getString(R.string.x_minutes, ms / (60 * 1000));
            return getString(R.string.x_hours, ms / (60 * 60 * 1000));
        }

        @Override
        public void save() {
            settings.setSeenTimeout(mode, getValue());
        }

        @Override
        public void close() {
            base.setTitle(R.string.settings_seen_on_timeout_title);
            base = null;
        }
    }

    private class TimeoutTrackHelper extends TimeoutHelper {
        private final CheckBox checkBox;

        public TimeoutTrackHelper(AlertDialog base, int checkBoxId) {
            super();
            checkBox = base.findViewById(checkBoxId);
            checkBox.setChecked(settings.isSeenTimeoutTrackSeparately());
        }

        @Override
        public void save() {
            settings.setSeenTimeoutTrackSeparately(checkBox.isChecked());
        }

        @Override
        public void close() {
        }
    }

    private class TimeoutLingerHelper extends TimeoutHelper {
        private final SeekBar seekBar;
        private final TextView textValue;
        private AlertDialog base;

        public TimeoutLingerHelper(AlertDialog base, int seekBarId, int textValueId) {
            super();
            seekBar = base.findViewById(seekBarId);
            textValue = base.findViewById(textValueId);
            this.base = base;

            seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    textValue.setText(getDescriptionFromIndex(progress));
                    base.setTitle(getDescriptionFromIndex(progress));
                }
                @Override public void onStartTrackingTouch(SeekBar seekBar) {
                }
                @Override public void onStopTrackingTouch(SeekBar seekBar) {
                    base.setTitle(R.string.settings_animation_overlay_linger_title);
                }
            });
            seekBar.setMax(20);
            seekBar.setProgress(getIndexFromValue(settings.getOverlayLinger()), false);
            textValue.setText(getDescriptionFromIndex(seekBar.getProgress()));
            base.setTitle(R.string.settings_animation_overlay_linger_title);
        }

        public int getIndexFromValue(int value) {
            return value / 25;
        }

        public int getValueFromIndex(int index) {
            return index * 25;
        }

        public int getValue() {
            return getValueFromIndex(seekBar.getProgress());
        }

        public String getDescriptionFromIndex(int index) {
            int ms = getValueFromIndex(index);
            if (ms == 0) return getString(R.string.disabled);
            return getString(R.string.x_ms, ms);
        }

        @Override
        public void save() {
            settings.setOverlayLinger(getValue());
        }

        @Override
        public void close() {
            base.setTitle(R.string.settings_animation_overlay_linger_title);
            base = null;
        }
    }

    private boolean supportsAODSchedule() {
        return Manufacturer.isSamsung();
    }

    @SuppressLint("SoonBlockedPrivateApi")
    private ListPreference getLocalesPreference(PreferenceCategory category) {
        ArrayList<String> locales = new ArrayList<>();

        AssetManager assetManager = getActivity().getAssets();
        String[] assetLocales = assetManager.getLocales();
        for (String s : assetLocales) {
            String lang = s.replace("-", "_");

            boolean found = BuildConfig.TRANSLATION_ARRAY.length == 0;
            for (String match : BuildConfig.TRANSLATION_ARRAY) {
                found = match.replace("-", "_").equals(lang);
                if (found) break;
            }

            if (found) locales.add(lang);
        }
        locales.sort((lhs, rhs) -> {
            if ((lhs != null) && (rhs != null)) {
                String left_lang = lhs;
                String left_country = null;

                String right_lang = rhs;
                String right_country = null;

                Locale left_locale;
                Locale right_locale;

                if (left_lang.contains("_") && !left_lang.endsWith("_")) {
                    left_country = left_lang.substring(left_lang.indexOf("_") + 1);
                    left_lang = left_lang.substring(0, left_lang.indexOf("_"));
                }

                if (right_lang.contains("_") && !right_lang.endsWith("_")) {
                    right_country = right_lang.substring(right_lang.indexOf("_") + 1);
                    right_lang = right_lang.substring(0, right_lang.indexOf("_"));
                }

                if (left_country != null) {
                    left_locale = new Locale(left_lang, left_country);
                } else {
                    left_locale = new Locale(left_lang);
                }

                if (right_country != null) {
                    right_locale = new Locale(right_lang, right_country);
                } else {
                    right_locale = new Locale(right_lang);
                }

                if ((left_locale != null) && (right_locale != null)) {
                    return left_locale.getDisplayName().compareTo(right_locale.getDisplayName());
                } else {
                    return 0;
                }
            } else {
                return 0;
            }
        });

        CharSequence[] lang_entries = new CharSequence[locales.size() + 1];
        CharSequence[] lang_values = new CharSequence[locales.size() + 1];

        lang_entries[0] = getString(R.string.settings_customization_locale_default);
        lang_values[0] = "";

        for (int i = 0; i < locales.size(); i++) {
            String lang = locales.get(i);

            Locale loc;
            if (lang.contains("_") && !lang.endsWith("_")) {
                loc = new Locale(lang.substring(0, lang.indexOf("_")), lang.substring(lang.indexOf("_") + 1));
            } else {
                loc = new Locale(lang);
            }

            lang_entries[i + 1] = loc.getDisplayName();
            lang_values[i + 1] = lang;
        }

        return list(category, R.string.settings_customization_locale_title, 0, R.string.settings_customization_locale_title, Settings.LOCALE, "", lang_entries, lang_values, true);
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

        String details = getString(R.string.app_details);
        if (getString(R.string.translation_by).length() > 0) {
            details += "\n" + getString(R.string.translation) + " " + getString(R.string.translation_by);
        }

        Preference copyright = pref(null, R.string.app_name, 0, "copyright", true, preference -> {
            Intent i = new Intent(Intent.ACTION_VIEW);
            i.setData(Uri.parse(getString(R.string.app_website_url)));
            startActivity(i);
            return false;
        });
        copyright.setTitle(title);
        copyright.setSummary(details);
        root.addPreference(copyright);

        prefDonateTop = pref(null, R.string.donate_title, 0, null, true, donateClickListener);
        prefDonateTop.setVisible(false);
        root.addPreference(prefDonateTop);

        Preference setupWizard = pref(null, R.string.setup_wizard_title, R.string.setup_wizard_description, null, true, preference -> {
            settings.setSetupWizardComplete(false);
            settings.setDeviceOfficialSupportWarningShown(false);
            Activity activity = getActivity();
            if (activity instanceof MainActivity) {
                ((MainActivity)activity).setupWizard();
            }
            return false;
        });
        root.addPreference(setupWizard);

        PreferenceCategory catOperation = category(root, R.string.settings_category_operation_title_v2, 0);
        catOperation.setSummary(Html.fromHtml(getString(R.string.settings_category_operation_description_v2)));

        Preference.OnPreferenceClickListener operationClickListener = preference -> {
            final int mode = getModeFromPreference(preference);
            if (mode < 0) return false;

            boolean charging = (mode == Settings.SCREEN_ON_CHARGING) || (mode == Settings.SCREEN_OFF_CHARGING);
            boolean screenOff = (mode == Settings.SCREEN_OFF_CHARGING) || (mode == Settings.SCREEN_OFF_BATTERY);

            CharSequence[] options = new CharSequence[screenOff ? 5 : 4];
            options[0] = Html.fromHtml(String.format(Locale.ENGLISH, Settings.AnimationStyle.HTML_FORMAT, getString(R.string.operation_mode_disabled_title), getString(R.string.operation_mode_disabled_description)));
            options[1] = settings.getAnimationStyle(SpritePlayer.Mode.SWIRL).getHtmlDisplay(getContext(), charging);
            options[2] = settings.getAnimationStyle(SpritePlayer.Mode.BLINK).getHtmlDisplay(getContext(), charging);
            options[3] = settings.getAnimationStyle(SpritePlayer.Mode.SINGLE).getHtmlDisplay(getContext(), charging);
            if (screenOff) {
                options[4] = settings.getAnimationStyle(SpritePlayer.Mode.TSP).getHtmlDisplay(getContext(), charging);
            }

            if (mode >= 0) {
                (new AlertDialog.Builder(getContext()))
                        .setTitle(preference.getTitle())
                        .setItems(options, (dialog, which) -> {
                            settings.setEnabledWhile(mode, which > 0);
                            switch (which) {
                                case 1:
                                    settings.setAnimationMode(mode, SpritePlayer.Mode.SWIRL);
                                    break;
                                case 2:
                                    settings.setAnimationMode(mode, SpritePlayer.Mode.BLINK);
                                    break;
                                case 3:
                                    settings.setAnimationMode(mode, SpritePlayer.Mode.SINGLE);
                                    break;
                                case 4:
                                    settings.setAnimationMode(mode, SpritePlayer.Mode.TSP);
                                    break;
                            }
                        })
                        .setNegativeButton(android.R.string.cancel, null)
                        .show();
            }
            return false;
        };

        prefScreenOnCharging = check(catOperation, R.string.settings_screen_on_charging_title, R.string.settings_screen_on_charging_description, settings.getEnabledWhileKey(Settings.SCREEN_ON_CHARGING), settings.isEnabledWhile(Settings.SCREEN_ON_CHARGING), true);
        prefScreenOffCharging = check(catOperation, R.string.settings_screen_off_charging_title, R.string.settings_screen_off_charging_description, settings.getEnabledWhileKey(Settings.SCREEN_OFF_CHARGING), settings.isEnabledWhile(Settings.SCREEN_OFF_CHARGING), true);
        prefScreenOnBattery = check(catOperation, R.string.settings_screen_on_battery_title, R.string.settings_screen_on_battery_description, settings.getEnabledWhileKey(Settings.SCREEN_ON_BATTERY), settings.isEnabledWhile(Settings.SCREEN_ON_BATTERY), true);
        prefScreenOffBattery = check(catOperation, R.string.settings_screen_off_battery_title, R.string.settings_screen_off_battery_description, settings.getEnabledWhileKey(Settings.SCREEN_OFF_BATTERY), settings.isEnabledWhile(Settings.SCREEN_OFF_BATTERY), false);
        for (CheckBoxPreference pref : new CheckBoxPreference[] { prefScreenOnCharging, prefScreenOffCharging, prefScreenOnBattery, prefScreenOffBattery }) {
            pref.setOnPreferenceChangeListener((preference, newValue) -> false);
            pref.setOnPreferenceClickListener(operationClickListener);
        }

        prefLockscreenOn = check(catOperation, R.string.settings_lockscreen_on_title, R.string.settings_lockscreen_on_description, Settings.ENABLED_LOCKSCREEN, settings.isEnabledOnLockscreen(), true);

        prefHideAOD = check(catOperation, R.string.settings_hide_aod_title, R.string.settings_hide_aod_description, Settings.HIDE_AOD, settings.isHideAOD(), true);
        prefHideAOD.setOnPreferenceChangeListener((preference, newValue) -> false);
        prefHideAOD.setOnPreferenceClickListener(preference -> {
            String FORMAT = "%s<br><small>%s</small>";
            (new AlertDialog.Builder(getContext()))
                    .setTitle(preference.getTitle())
                    .setItems(new CharSequence[] {
                            getString(R.string.disabled),
                            Html.fromHtml(String.format(Locale.ENGLISH, FORMAT, getString(R.string.hide_aod_full_title), getString(R.string.hide_aod_full_description))),
                            Html.fromHtml(String.format(Locale.ENGLISH, FORMAT, getString(R.string.hide_aod_partial_title), getString(R.string.hide_aod_partial_description))),
                    }, (dialog, which) -> {
                        switch (which) {
                            case 0:
                                settings.setHideAOD(false);
                                break;
                            case 1:
                                settings.setHideAOD(true, true);
                                break;
                            case 2:
                                settings.setHideAOD(true, false);
                                break;
                        }
                    })
                    .setNegativeButton(android.R.string.cancel, null)
                    .show();
            return false;
        });

        PreferenceCategory catSchedule = category(root, R.string.settings_category_schedule, 0);
        prefAODSchedule = pref(catSchedule, R.string.settings_schedule_aod_title, 0, null, true, null);
        prefRespectDND = check(catSchedule, R.string.settings_schedule_dnd_title, 0, Settings.RESPECT_DND, settings.isRespectDoNotDisturb(), true);
        prefRespectDND.setSummary(Html.fromHtml(getString(R.string.settings_schedule_dnd_description)));

        PreferenceCategory catAnimation = category(root, R.string.settings_category_animation_title, 0);
        Preference prefAODBrightness = pref(catAnimation, R.string.settings_animation_brightness_title, 0, null, true, null);
        prefAODBrightness.setSummary(Html.fromHtml(getString(Manufacturer.isSamsung() ? R.string.settings_animation_brightness_description : Manufacturer.isGoogle() ? R.string.settings_animation_brightness_description_google : R.string.manufacturer_detection_error)));
        prefTune = pref(catAnimation, R.string.settings_animation_tune_title, R.string.settings_animation_tune_description, null, true, preference -> {
            startActivity(new Intent(getActivity(), TuneActivity.class));
            return false;
        });
        prefColors = pref(catAnimation, R.string.settings_animation_colors_title, R.string.settings_animation_colors_description_v3, null, true, preference -> {
            startActivity(new Intent(getActivity(), ColorActivity.class));
            return false;
        });
        prefBlackFill = check(catAnimation, R.string.settings_animation_black_fill_title, R.string.settings_animation_black_fill_description, Settings.BLACK_FILL, Settings.BLACK_FILL_DEFAULT, true);
        prefUnholeyIcons = check(catAnimation, R.string.settings_animation_unholey_light_icons_title, R.string.settings_animation_unholey_light_icons_description, Settings.UNHOLEY_LIGHT_ICONS, Settings.UNHOLEY_LIGHT_ICONS_DEFAULT, true);
        prefUnholeyClock = check(catAnimation, R.string.settings_animation_unholey_light_show_clock_title, 0, Settings.AOD_SHOW_CLOCK, Settings.AOD_SHOW_CLOCK_DEFAULT, true);

        prefOverlayLinger = check(catAnimation, R.string.settings_animation_overlay_linger_title, 0, null, false, true);
        prefOverlayLinger.setOnPreferenceChangeListener((preference, newValue) -> false);
        prefOverlayLinger.setOnPreferenceClickListener(preference -> {
            ArrayList<TimeoutHelper> helpers = new ArrayList<>();

            AlertDialog dialog = (new AlertDialog.Builder(getContext()))
                    .setTitle(preference.getTitle())
                    .setView(R.layout.dialog_linger)
                    .setNegativeButton(android.R.string.cancel, null)
                    .setPositiveButton(android.R.string.ok, (dialog1, which) -> {
                        settings.edit();
                        try {
                            for (TimeoutHelper helper : helpers) {
                                helper.save();
                            }
                        } finally {
                            settings.save(true);
                        }
                    })
                    .setOnDismissListener(dialog1 -> {
                        for (TimeoutHelper helper : helpers) {
                            helper.close();
                        }
                    })
                    .show();

            helpers.add(new TimeoutLingerHelper(dialog, R.id.timeout_linger_seek, R.id.timeout_linger_value));
            return false;
        });

        PreferenceCategory catMarkAsSeen;

        catMarkAsSeen = category(root, R.string.settings_category_seen_title, R.string.settings_category_seen_description);
        prefSeenPickup = check(catMarkAsSeen, R.string.settings_seen_pickup_title, R.string.settings_seen_pickup_description, null, false, true);
        prefSeenPickup.setOnPreferenceChangeListener((preference, newValue) -> false);
        prefSeenPickup.setOnPreferenceClickListener(preference -> {
            String FORMAT = "%s<br><small>%s</small>";
            AlertDialog alertDialog = (new AlertDialog.Builder(getContext()))
                    .setTitle(preference.getTitle())
                    .setMultiChoiceItems(new CharSequence[] {
                            Html.fromHtml(String.format(Locale.ENGLISH, FORMAT, getString(R.string.charging_screen_on), getString(R.string.settings_seen_pickup_screen_on_charging_description))),
                            Html.fromHtml(String.format(Locale.ENGLISH, FORMAT, getString(R.string.charging_screen_off), getString(R.string.settings_seen_pickup_screen_off_charging_description))),
                            Html.fromHtml(String.format(Locale.ENGLISH, FORMAT, getString(R.string.battery_screen_on), getString(R.string.settings_seen_pickup_screen_on_battery_description))),
                            Html.fromHtml(String.format(Locale.ENGLISH, FORMAT, getString(R.string.battery_screen_off), getString(R.string.settings_seen_pickup_screen_off_battery_description)))
                    }, new boolean[] {
                            settings.isSeenPickupWhile(Settings.SCREEN_ON_CHARGING, false),
                            settings.isSeenPickupWhile(Settings.SCREEN_OFF_CHARGING, false),
                            settings.isSeenPickupWhile(Settings.SCREEN_ON_BATTERY, false),
                            settings.isSeenPickupWhile(Settings.SCREEN_OFF_BATTERY, false),
                    }, (dialog, which, isChecked) -> {
                        switch (which) {
                            case 0:
                                settings.setSeenPickupWhile(Settings.SCREEN_ON_CHARGING, isChecked);
                                break;
                            case 1:
                                settings.setSeenPickupWhile(Settings.SCREEN_OFF_CHARGING, isChecked);
                                break;
                            case 2:
                                settings.setSeenPickupWhile(Settings.SCREEN_ON_BATTERY, isChecked);
                                break;
                            case 3:
                                settings.setSeenPickupWhile(Settings.SCREEN_OFF_BATTERY, isChecked);
                                break;
                        }
                    })
                    .setPositiveButton(android.R.string.ok, null)
                    .show();
            
            ListView listView = alertDialog.getListView();
            listView.setDividerHeight((int)(16 * TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1, getContext().getResources().getDisplayMetrics())));

            // Bloody Android and its AlertDialog auto-sizing
            ViewGroup.LayoutParams params = listView.getLayoutParams();
            params.width = ViewGroup.LayoutParams.WRAP_CONTENT;
            params.height = (int)(getActivity().getWindow().getDecorView().getHeight() * 0.75f);
            listView.setLayoutParams(params);

            return false;
        });

        prefSeenOnLockscreen = check(catMarkAsSeen, R.string.settings_seen_on_lockscreen_title, R.string.settings_seen_on_lockscreen_description, Settings.SEEN_ON_LOCKSCREEN, settings.isSeenOnLockscreen(false), true);
        prefSeenOnUserPresent = check(catMarkAsSeen, R.string.settings_seen_on_user_present_title, R.string.settings_seen_on_user_present_description, Settings.SEEN_ON_USER_PRESENT, settings.isSeenOnUserPresent(false), true);
        prefSeenIfScreenOn = check(catMarkAsSeen, R.string.settings_seen_if_screen_on_title, R.string.settings_seen_if_screen_on_description, Settings.SEEN_IF_SCREEN_ON, settings.isSeenIfScreenOn(false), true);

        prefSeenOnTimeout = check(catMarkAsSeen, R.string.settings_seen_on_timeout_title, R.string.settings_seen_on_timeout_description, null, false, true);
        prefSeenOnTimeout.setOnPreferenceChangeListener((preference, newValue) -> false);
        prefSeenOnTimeout.setOnPreferenceClickListener(preference -> {
            ArrayList<TimeoutHelper> timeoutHelpers = new ArrayList<>();

            AlertDialog dialog = (new AlertDialog.Builder(getContext()))
                    .setTitle(preference.getTitle())
                    .setView(R.layout.dialog_timeout)
                    .setNegativeButton(android.R.string.cancel, null)
                    .setPositiveButton(android.R.string.ok, (dialog1, which) -> {
                        settings.edit();
                        try {
                            for (TimeoutHelper helper : timeoutHelpers) {
                                helper.save();
                            }
                        } finally {
                            settings.save(true);
                        }
                    })
                    .setOnDismissListener(dialog1 -> {
                        for (TimeoutHelper helper : timeoutHelpers) {
                            helper.close();
                        }
                    })
                    .show();

            timeoutHelpers.add(new TimeoutTimeHelper(dialog, R.id.timeout_charging_screen_on_seek, R.id.timeout_charging_screen_on_value, Settings.SCREEN_ON_CHARGING));
            timeoutHelpers.add(new TimeoutTimeHelper(dialog, R.id.timeout_charging_screen_off_seek, R.id.timeout_charging_screen_off_value, Settings.SCREEN_OFF_CHARGING));
            timeoutHelpers.add(new TimeoutTimeHelper(dialog, R.id.timeout_battery_screen_on_seek, R.id.timeout_battery_screen_on_value, Settings.SCREEN_ON_BATTERY));
            timeoutHelpers.add(new TimeoutTimeHelper(dialog, R.id.timeout_battery_screen_off_seek, R.id.timeout_battery_screen_off_value, Settings.SCREEN_OFF_BATTERY));
            timeoutHelpers.add(new TimeoutTrackHelper(dialog, R.id.timeout_track_separately));
            return false;
        });

        PreferenceCategory catAODHelper = category(root, R.string.settings_category_aod_helper_title, R.string.settings_category_aod_helper_description);
        prefAODHelper = pref(catAODHelper, 0, 0, null, true, null);

        if (Manufacturer.isSamsung()) {
            prefAODHelperControl = check(catAODHelper, R.string.settings_aod_helper_control_title, 0, Settings.AOD_HELPER_CONTROL, Settings.AOD_HELPER_CONTROL_DEFAULT, true);
            prefAODHelperControl.setSummary(Html.fromHtml(getString(Manufacturer.isSamsung() ? R.string.settings_aod_helper_control_description_samsung_2 : Manufacturer.isGoogle() ? R.string.settings_aod_helper_control_description_google : R.string.manufacturer_detection_error)));
            prefAODHelperControl.setOnPreferenceChangeListener((preference, newValue) -> {
                if ((Boolean) newValue) {
                    (new AlertDialog.Builder(getContext()))
                            .setTitle(getString(R.string.aod_helper_known_issues_samsung_title))
                            .setMessage(Html.fromHtml(
                                    getString(R.string.aod_helper_known_issues_samsung_description) + "<br><br>" +
                                    getString(R.string.aod_helper_known_issues_samsung_settings) + "<br><br>" +
                                    getString(R.string.aod_helper_known_issues_samsung_tap_to_show) + "<br><br>" +
                                    getString(R.string.aod_helper_known_issues_samsung_hide_aod) + "<br><br>" +
                                    getString(R.string.aod_helper_known_issues_samsung_unholey_clock)
                            ))
                            .setPositiveButton(android.R.string.ok, null)
                            .show();
                }
                return true;
            });
        }

        if (Manufacturer.isGoogle()) {
            prefAODHelperBrightness = check(catAODHelper, R.string.settings_aod_helper_increase_brightness_title_google, 0, Settings.AOD_HELPER_BRIGHTNESS, Settings.AOD_HELPER_BRIGHTNESS_DEFAULT, true);
            prefAODHelperBrightness.setSummary(Html.fromHtml(getString(R.string.settings_aod_helper_increase_brightness_description_google)));
            prefAODHelperBrightness.setOnPreferenceClickListener(preference -> {
                if (settings.isAODHelperBrightness()) {
                    AODControl.setAODBrightness(getContext(), true, result -> {});
                }
                return true;
            });
            prefAODHelperBrightness.setOnPreferenceChangeListener((preference, newValue) -> {
                if (!((Boolean)newValue)) {
                    AODControl.setAODBrightness(getContext(), false, result -> {});
                }
                return true;
            });
        }

        PreferenceCategory catChainfire = category(root, R.string.settings_category_chainfire_title, 0);

        prefDonateBottom = pref(catChainfire, R.string.donate_title, 0, null, true, donateClickListener);
        prefDonateBottom.setVisible(false);

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

        PreferenceCategory catCustomizations = category(root, R.string.settings_category_customization, 0);
        prefLocale = getLocalesPreference(catCustomizations);

        catDebug = category(root, R.string.settings_category_debug, 0);
        catDebug.setVisible(false);
        prefDebug = pref(catDebug, R.string.logcat_dump_title, R.string.logcat_dump_description, null, true, preference -> {
            Activity activity = getActivity();
            if (activity instanceof MainActivity) {
                ((MainActivity)activity).logcatDumpRequest();
            }
            return false;
        });
        prefDebug.setVisible(false);

        if (Manufacturer.isSamsung()) {
            PreferenceCategory catTips = category(root, R.string.settings_category_tips, 0);
            pref(catTips, R.string.tips_samsung_aod_image_title, 0, null, true, (preference) -> {
                Activity activity = getActivity();
                if (activity instanceof MainActivity) {
                    ((MainActivity)activity).showAODImageThemeInstructions(false);
                }
                return true;
            }).setSummary(Html.fromHtml(getString(R.string.tips_samsung_aod_image_description)));
            pref(catTips, R.string.tips_samsung_fingerprint_title, 0, null, true, null).setSummary(Html.fromHtml(getString(R.string.tips_samsung_fingerprint_description)));
            pref(catTips, R.string.tips_samsung_battery_title, 0, null, true, null).setSummary(Html.fromHtml(getString(R.string.tips_samsung_battery_description)));
        }

        updatePrefs(null, true);
        prefs.registerOnSharedPreferenceChangeListener(this);
        return root;
    }

    @Override
    public void onStart() {
        super.onStart();
        updatePrefs(null, true);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        try {
            updatePrefs(key, false);
        } catch (Throwable t) {
            // no action
        }
    }

    private String getAnimationStyleSuffix(int mode) {
        SpritePlayer.Mode animationMode = settings.getAnimationMode(mode);
        Settings.AnimationStyle as = settings.getAnimationStyle(animationMode);
        if (as != null) {
            return "\n[ " + getString(as.title) + " ]";
        }
        return "\n[ ? ]";
    }

    private final Preference.OnPreferenceClickListener aodHelperInstructions = preference -> {
        Activity activity = getActivity();
        if (activity instanceof MainActivity) {
            ((MainActivity)activity).aodHelperInstructions();
        }
        return false;
    };

    private final Preference.OnPreferenceClickListener aodHelperNone = preference -> false;

    @SuppressWarnings({ "unused", "ConstantConditions", "deprecation" })
    private void updatePrefs(String key, boolean evaluateHelper) {
        if (prefScreenOnCharging != null) {
            MainActivity activity = null;
            {
                Activity act = getActivity();
                if (act instanceof MainActivity) {
                    activity = (MainActivity)act;
                }
            }

            for (CheckBoxPreference pref : new CheckBoxPreference[] { prefScreenOnCharging, prefScreenOffCharging, prefScreenOnBattery, prefScreenOffBattery }) {
                int mode = getModeFromPreference(pref);
                pref.setEnabled(settings.isEnabled());
                pref.setChecked(settings.isEnabledWhile(mode, false));
                String summary = pref.getSummary().toString();
                int index = summary.indexOf("\n[");
                if (index > -1) summary = summary.substring(0, index);
                if (settings.isEnabledWhile(mode, false)) {
                    summary += getAnimationStyleSuffix(mode);
                }
                pref.setSummary(summary);
            }
            prefLockscreenOn.setEnabled(settings.isEnabledWhileScreenOn());

            if (!supportsAODSchedule()) {
                prefAODSchedule.setSummary(R.string.settings_schedule_aod_description_unsupported);
            } else {
                int[] aodSchedule = AODControl.getAODSchedule(getContext());
                if (aodSchedule == null) {
                    prefAODSchedule.setSummary(Html.fromHtml(getString(R.string.settings_schedule_aod_description) + "<br>[ " + getString(R.string.settings_schedule_aod_none) + " ]"));
                } else {
                    String start;
                    String end;
                    if (DateFormat.is24HourFormat(getContext())) {
                        start = String.format(Locale.ENGLISH, "%d:%02d", aodSchedule[0] / 60, aodSchedule[0] % 60);
                        end = String.format(Locale.ENGLISH, "%d:%02d", aodSchedule[1] / 60, aodSchedule[1] % 60);
                    } else {
                        int startHour = (aodSchedule[0] % (12 * 60)) / 60;
                        if (startHour == 0) startHour = 12;
                        int endHour = (aodSchedule[1] % (12 * 60)) / 60;
                        if (endHour == 0) endHour = 12;
                        start = String.format(Locale.ENGLISH, "%d:%02d %s", startHour, aodSchedule[0] % 60, aodSchedule[0] < 12 * 60 ? "AM" : "PM");
                        end = String.format(Locale.ENGLISH, "%d:%02d %s", endHour, aodSchedule[1] % 60, aodSchedule[1] < 12 * 60 ? "AM" : "PM");
                    }
                    prefAODSchedule.setSummary(Html.fromHtml(getString(R.string.settings_schedule_aod_description) + "<br>[ " + start + " - " + end + " ]"));
                }
            }

            prefRespectDND.setEnabled(settings.isEnabled());

            AODControl.AODHelperState aodHelperState = AODControl.getAODHelperState(getContext());

            prefTune.setEnabled(settings.isEnabled());
            prefColors.setEnabled(settings.isEnabled());
            prefBlackFill.setEnabled(settings.isEnabled());
            prefUnholeyIcons.setEnabled(settings.isEnabled());

            prefUnholeyClock.setEnabled(settings.isEnabled());
            String sSummary = getString(R.string.settings_animation_unholey_light_show_clock_description);
            if (Manufacturer.isSamsung() && aodHelperState != AODControl.AODHelperState.NOT_INSTALLED) {
                sSummary += ". " + getString(R.string.settings_animation_unholey_light_show_clock_description_incompat);
            }
            prefUnholeyClock.setSummary(Html.fromHtml(sSummary));

            prefOverlayLinger.setEnabled(settings.isEnabled());
            prefOverlayLinger.setChecked(settings.getOverlayLinger() > 0);
            prefOverlayLinger.setSummary(getString(R.string.settings_animation_overlay_linger_description) + (settings.getOverlayLinger() > 0 ? "\n[ " + getString(R.string.x_ms, settings.getOverlayLinger()) + " ]" : ""));

            ArrayList<String> seenPickup = new ArrayList<>();
            for (int i = 0; i < Settings.SCREEN_AND_POWER_STATE_DESCRIPTIONS.length; i++) {
                if (settings.isSeenPickupWhile(i, false)) {
                    seenPickup.add(getString(Settings.SCREEN_AND_POWER_STATE_DESCRIPTIONS[i]));
                }
            }
            prefSeenPickup.setEnabled(settings.isEnabled());
            prefSeenPickup.setChecked(seenPickup.size() > 0);
            prefSeenPickup.setSummary(getString(R.string.settings_seen_pickup_description) + (seenPickup.size() > 0 ? "\n[ " + String.join(", ", seenPickup) + " ]" : ""));

            prefSeenOnLockscreen.setEnabled(settings.isEnabledWhileScreenOff());
            prefSeenOnUserPresent.setEnabled(settings.isEnabledWhileScreenOff());
            prefSeenIfScreenOn.setEnabled(settings.isEnabled());
            prefSeenOnTimeout.setChecked(settings.haveSeenTimeoutAny());
            prefSeenOnTimeout.setEnabled(settings.isEnabled());

            prefHideAOD.setEnabled(settings.isEnabledWhileScreenOff());
            prefHideAOD.setChecked(settings.isHideAOD());
            prefHideAOD.setSummary(getString(R.string.settings_hide_aod_description) + (settings.isHideAOD() ? "\n[ " + getString(settings.isHideAODFully() ? R.string.hide_aod_full_title : R.string.hide_aod_partial_title) + " ]" : ""));

            prefAODHelper.setEnabled(settings.isEnabled());
            if (aodHelperState == AODControl.AODHelperState.NEEDS_UPDATE) {
                prefAODHelper.setTitle(R.string.settings_aod_helper_update_title);
                prefAODHelper.setSummary(Html.fromHtml(getString(R.string.settings_aod_helper_update_description_normal)));
                prefAODHelper.setOnPreferenceClickListener(aodHelperInstructions);
                prefAODHelper.setVisible(true);
            } else if (aodHelperState == AODControl.AODHelperState.NEEDS_PERMISSIONS) {
                prefAODHelper.setTitle(R.string.settings_aod_helper_update_title);
                prefAODHelper.setSummary(Html.fromHtml(getString(R.string.settings_aod_helper_update_description_permissions)));
                prefAODHelper.setOnPreferenceClickListener(aodHelperInstructions);
                prefAODHelper.setVisible(true);
            } else if (aodHelperState == AODControl.AODHelperState.NOT_INSTALLED) {
                prefAODHelper.setTitle(R.string.settings_aod_helper_install_title);
                prefAODHelper.setSummary(Html.fromHtml(getString(Manufacturer.isSamsung() ? R.string.settings_aod_helper_install_description_samsung : Manufacturer.isGoogle() ? R.string.settings_aod_helper_install_description_google : R.string.manufacturer_detection_error)));
                prefAODHelper.setOnPreferenceClickListener(aodHelperInstructions);
                prefAODHelper.setVisible(true);
            } else {
                prefAODHelper.setOnPreferenceClickListener(aodHelperNone);
                prefAODHelper.setVisible(false);
            }

            if (prefAODHelperControl != null) {
                prefAODHelperControl.setEnabled(settings.isEnabled() && aodHelperState == AODControl.AODHelperState.OK);
            }
            if (prefAODHelperBrightness != null) {
                prefAODHelperBrightness.setEnabled(settings.isEnabled() && aodHelperState == AODControl.AODHelperState.OK);
            }

            catDebug.setVisible(Settings.DEBUG);
            prefDebug.setVisible(Settings.DEBUG);

            if ((key == null) || (key.equals(Settings.LOCALE))) {
                String lang = settings.getLocale(false);
                if ("".equals(lang)) {
                    prefLocale.setSummary(String.format(Locale.ENGLISH, "[ %s ]", getString(R.string.settings_customization_locale_default)));
                } else {
                    Locale loc;
                    if (lang.contains("_")) {
                        loc = new Locale(lang.substring(0, lang.indexOf("_")), lang.substring(lang.indexOf("_") + 1));
                    } else {
                        loc = new Locale(lang);
                    }
                    prefLocale.setSummary(String.format(Locale.ENGLISH, "[ %s ]", loc.getDisplayName()));
                }

                if (key != null) {
                    if (activity != null) activity.finish(false);
                    Intent i = new Intent(getActivity(), getActivity().getClass());
                    i.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                    getActivity().startActivity(i);
                    getActivity().overridePendingTransition(0, 0);
                    settings.setLocale(settings.getLocale(false)); // force save
                }
            }

            if (activity != null) {
                boolean donateAtTop = activity.iapAvailable && settings.getPurchased().length == 0;
                int iSummary = activity.iapAvailable ? R.string.donate_description_iap : R.string.donate_description_paypal;
                boolean enabled = activity.iapAvailable;
                
                prefDonateTop.setSummary(iSummary);
                prefDonateTop.setEnabled(enabled);
                prefDonateTop.setVisible(donateAtTop);

                prefDonateBottom.setSummary(iSummary);
                prefDonateBottom.setEnabled(enabled);
                prefDonateBottom.setVisible(!donateAtTop);
            }

            if (key != null && activity != null) {
                activity.validateSettings(evaluateHelper);
            }
        }
    }

    private final Preference.OnPreferenceClickListener donateClickListener = preference -> {
        Activity activity = getActivity();
        if (activity instanceof MainActivity) {
            ((MainActivity)activity).startPurchase();
        }
        return true;
    };
}
