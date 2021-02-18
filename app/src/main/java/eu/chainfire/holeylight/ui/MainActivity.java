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
import android.app.Dialog;
import android.companion.AssociationRequest;
import android.companion.CompanionDeviceManager;
import android.content.Intent;
import android.content.IntentSender;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.text.Html;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import eu.chainfire.holeylight.BuildConfig;
import eu.chainfire.holeylight.R;
import eu.chainfire.holeylight.animation.SpritePlayer;
import eu.chainfire.holeylight.misc.AODControl;
import eu.chainfire.holeylight.misc.Manufacturer;
import eu.chainfire.holeylight.misc.Permissions;
import eu.chainfire.holeylight.misc.Settings;

public class MainActivity extends AppCompatActivity implements Settings.OnSettingsChangedListener {
    private static final int LOGCAT_DUMP_REQUEST_CODE = 12345;

    private Handler handler = null;
    private Settings settings = null;
    private SwitchCompat switchMaster = null;
    private Dialog currentDialog = null;
    private boolean checkPermissionsOnResume = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        handler = new Handler();
        settings = Settings.getInstance(this);
        settings.registerOnSettingsChangedListener(this);

        startActivity(new Intent(this, DetectCutoutActivity.class));
    }

    @Override
    protected void onDestroy() {
        settings.unregisterOnSettingsChangedListener(this);
        super.onDestroy();
    }

    @SuppressWarnings("unused")
    @Override
    public void onSettingsChanged() {
        if (switchMaster != null) {
            boolean enabled = settings.isEnabled();
            if (enabled != switchMaster.isChecked()) {
                switchMaster.setChecked(enabled);
            }
        }
    }

    private AlertDialog.Builder newAlert(boolean finishOnDismiss) {
        if (currentDialog != null) {
            currentDialog.setCancelable(true);
            currentDialog.dismiss();
            currentDialog = null;
        }
        return (new AlertDialog.Builder(this))
                .setOnDismissListener(dialog -> {
                    if (currentDialog == dialog) {
                        currentDialog = null;
                    }
                    if (finishOnDismiss) {
                        if (!inLogcatDump) MainActivity.this.finish();
                    }
        });
    }

    private final Runnable checkPermissionsRunnable = this::checkPermissions;

    private boolean inLogcatDump = false;

    public void logcatDumpRequest() {
        inLogcatDump = true;
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TITLE, "holeylight_logcat.txt");
        //noinspection deprecation
        startActivityForResult(intent, LOGCAT_DUMP_REQUEST_CODE);
    }

    private void logcatDump(Uri uri) {
        OutputStream outputStream;
        try {
            outputStream = getContentResolver().openOutputStream(uri);
            BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(outputStream));

            Process process = Runtime.getRuntime().exec("logcat -d");
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));

            String line;
            while ((line = bufferedReader.readLine()) != null) {
                bufferedWriter.write(line + "\r\n");
            }

            bufferedWriter.flush();
            bufferedWriter.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        inLogcatDump = false;
        finish();
    }

    @SuppressWarnings("deprecation")
    private void checkPermissions() {
        if (inLogcatDump) return;
        if (setupWizard()) return;

        String device = String.format("%s / %s / %s", Build.BRAND, Build.MANUFACTURER, Build.DEVICE);

        switch (Permissions.detect(this)) {
            case DEVICE_SUPPORT:
                (currentDialog = newAlert(!Settings.DEBUG)
                        .setTitle(R.string.error)
                        .setMessage(Html.fromHtml(getString(R.string.error_unsupported_device, device)))
                        .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                            if (Settings.DEBUG) logcatDumpRequest();
                        })
                        .show()).setCanceledOnTouchOutside(false);
                break;
            case DEVICE_OFFICIAL_SUPPORT:
                (currentDialog = newAlert(!Settings.DEBUG)
                        .setTitle(R.string.notice_dialog_title)
                        .setMessage(Html.fromHtml(getString(R.string.notice_not_officially_supported_device, device)))
                        .setPositiveButton(android.R.string.ok, null)
                        .setOnDismissListener(dialog -> {
                            settings.setDeviceOfficialSupportWarningShown(true);
                            checkPermissions();
                        })
                        .show()).setCanceledOnTouchOutside(false);
                break;
            case UNHIDE_NOTCH:
                (currentDialog = newAlert(true)
                        .setTitle(R.string.error)
                        .setMessage(Html.fromHtml(getString(R.string.error_hide_notch)))
                        .setPositiveButton(android.R.string.ok, null)
                        .show()).setCanceledOnTouchOutside(false);
                break;
            case COMPANION_DEVICE:
                (currentDialog = newAlert(false)
                        .setTitle(getString(R.string.permission_required) + " 1/4")
                        .setMessage(Html.fromHtml(getString(R.string.permission_associate) + getString(R.string.permission_associate_2)))
                        .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                            @SuppressLint("WrongConstant") CompanionDeviceManager companionDeviceManager = (CompanionDeviceManager)getSystemService(COMPANION_DEVICE_SERVICE);
                            companionDeviceManager.associate((new AssociationRequest.Builder()).build(), new CompanionDeviceManager.Callback() {
                                @Override
                                public void onDeviceFound(IntentSender chooserLauncher) {
                                    try {
                                        startIntentSenderForResult(chooserLauncher, 0, null, 0, 0, 0);
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                }

                                @Override
                                public void onFailure(CharSequence error) {
                                    // If the users cancels we also get an error
                                    if ((error != null) && (error.toString().contains("Future.cancel"))) return;

                                    (new AlertDialog.Builder(MainActivity.this))
                                            .setTitle(getString(R.string.error))
                                            .setMessage(error)
                                            .setPositiveButton(android.R.string.ok, null)
                                            .show();
                                }
                            }, handler);
                        })
                        .show()).setCanceledOnTouchOutside(false);
                break;
            case NOTIFICATION_SERVICE:
                (currentDialog = newAlert(false)
                        .setTitle(getString(R.string.permission_required) + " 2/4")
                        .setMessage(Html.fromHtml(getString(R.string.permission_notifications)))
                        .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                            Intent intent = new Intent(android.provider.Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS);
                            startActivity(intent);
                        })
                        .show()).setCanceledOnTouchOutside(false);
                break;
            case ACCESSIBILITY_SERVICE:
                (currentDialog = newAlert(false)
                        .setTitle(getString(R.string.permission_required) + " 3/4")
                        .setMessage(Html.fromHtml(getString(R.string.permission_accessibility_v2)))
                        .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                            Intent intent = new Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS);
                            startActivity(intent);
                        })
                        .show()).setCanceledOnTouchOutside(false);
                break;
            case BATTERY_OPTIMIZATION_EXEMPTION:
                (currentDialog = newAlert(false)
                        .setTitle(getString(R.string.permission_required) + " 4/4")
                        .setMessage(Html.fromHtml(getString(R.string.permission_battery)))
                        .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                            checkPermissionsOnResume = true;
                            @SuppressLint("BatteryLife") Intent intent = new Intent(android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                            intent.setData(Uri.parse("package:" + BuildConfig.APPLICATION_ID));
                            startActivity(intent);
                        })
                        .show()).setCanceledOnTouchOutside(false);
                break;
            case NONE:
                newAlert(false); // dismiss leftovers
                validateSettings();
        }
    }

    @SuppressWarnings({ "deprecation", "UnusedReturnValue" })
    public boolean validateSettings() {
        if (!Settings.getInstance(this).isSetupWizardComplete()) return false;
        if (Permissions.detect(this) != Permissions.Needed.NONE) return false;

        boolean aodRequired = settings.isEnabled() && (
                settings.isEnabledWhile(Settings.SCREEN_OFF_CHARGING, true) ||
                settings.isEnabledWhile(Settings.SCREEN_OFF_BATTERY, true)
        );
        boolean aodWithImageRequired = aodRequired && (
                settings.isHideAOD() ||
                (settings.getAnimationMode(Settings.SCREEN_OFF_CHARGING) == SpritePlayer.Mode.TSP) ||
                (settings.getAnimationMode(Settings.SCREEN_OFF_BATTERY) == SpritePlayer.Mode.TSP)
        );
        if (aodRequired) {
            aodRequired = !AODControl.isAODEnabled(this) && !AODControl.usingHelperPackage(this, null);
            if (aodWithImageRequired) {
                aodWithImageRequired = (Manufacturer.isSamsung() && AODControl.getAODThemePackage(this) == null);
            }
            if (aodRequired) {
                (currentDialog = newAlert(false)
                        .setTitle(R.string.notice_dialog_title)
                        .setMessage(Html.fromHtml(getString(
                                Manufacturer.isSamsung() ? R.string.notice_aod_required_message :
                                Manufacturer.isGoogle() ? R.string.notice_aod_required_message_google :
                                0
                        )))
                        .setNegativeButton(android.R.string.cancel, null)
                        .setPositiveButton(R.string.open_android_settings, (dialog, which) -> {
                            try {
                                Intent intent = new Intent(Intent.ACTION_MAIN);
                                intent.addCategory(Intent.CATEGORY_LAUNCHER);
                                intent.setPackage("com.android.settings");
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(intent);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        })
                        .show()).setCanceledOnTouchOutside(false);
                return true;
            } else if (aodWithImageRequired) {
                (currentDialog = newAlert(false)
                        .setTitle(R.string.notice_dialog_title)
                        .setMessage(Html.fromHtml(getString(R.string.notice_aod_with_image_required_message)))
                        .setNegativeButton(android.R.string.cancel, null)
                        .setPositiveButton(R.string.open_theme_store, (dialog, which) -> {
                            try {
                                Intent intent = new Intent();
                                intent.setPackage("com.samsung.android.themestore");
                                intent.setData(Uri.parse("themestore://MainPage?contentsType=aods"));
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(intent);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        })
                        .show()).setCanceledOnTouchOutside(false);
                return true;
            }
        }
        return false;
    }

    @SuppressWarnings("deprecation")
    private AlertDialog.Builder newSetupAlert(int message) {
        return newAlert(false)
                .setTitle(R.string.setup_wizard_title)
                .setMessage(Html.fromHtml(getString(message)))
                .setOnCancelListener((dialog) -> MainActivity.this.finish());
    }

    public boolean setupWizard() {
        Settings settings = Settings.getInstance(this);
        if (settings.isSetupWizardComplete()) return false;

        // dialogs below are in reverse order of being shown

        Runnable done = () ->
            (currentDialog = newSetupAlert(R.string.setup_wizard_complete)
                    .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                        settings.setSetupWizardComplete(true);
                        handler.removeCallbacks(checkPermissionsRunnable);
                        checkPermissions();
                    })
                    .show()).setCanceledOnTouchOutside(false);

        Runnable hideAOD = () ->
                (currentDialog = newSetupAlert(R.string.setup_wizard_hide_aod)
                        .setPositiveButton(R.string.setup_wizard_option_hide_aod, (dialog, which) -> done.run())
                        .setNegativeButton(R.string.setup_wizard_option_show_aod, (dialog, which) -> {
                            settings.setHideAOD(false);
                            done.run();
                        })
                        .show()).setCanceledOnTouchOutside(false);

        Runnable chooseNotificationStyle = () ->
            (currentDialog = newSetupAlert(R.string.setup_wizard_choose_notification_style)
                    .setPositiveButton(R.string.animation_style_swirl_title, (dialog, which) -> {
                        settings.edit();
                        try {
                            settings.setAnimationMode(Settings.SCREEN_OFF_CHARGING, SpritePlayer.Mode.SWIRL);
                            settings.setAnimationMode(Settings.SCREEN_OFF_BATTERY, SpritePlayer.Mode.SWIRL);
                        } finally {
                            settings.save(true);
                        }
                        hideAOD.run();
                    })
                    .setNegativeButton(R.string.animation_style_blink_title, (dialog, which) -> {
                        settings.edit();
                        try {
                            settings.setAnimationMode(Settings.SCREEN_OFF_CHARGING, SpritePlayer.Mode.BLINK);
                            settings.setAnimationMode(Settings.SCREEN_OFF_BATTERY, SpritePlayer.Mode.BLINK);
                        } finally {
                            settings.save(true);
                        }
                        hideAOD.run();
                    })
                    .setNeutralButton(R.string.animation_style_tsp_title, (dialog, which) -> {
                        settings.edit();
                        try {
                            settings.setAnimationMode(Settings.SCREEN_OFF_CHARGING, SpritePlayer.Mode.TSP);
                            settings.setAnimationMode(Settings.SCREEN_OFF_BATTERY, SpritePlayer.Mode.TSP);
                        } finally {
                            settings.save(true);
                        }
                        done.run();
                    })
                    .show()).setCanceledOnTouchOutside(false);

        Runnable runWhileScreenOff = () ->
            (currentDialog = newSetupAlert(R.string.setup_wizard_run_while_screen_off)
                    .setPositiveButton(R.string.yes, (dialog, which) -> {
                        settings.edit();
                        try {
                            settings.setEnabledWhile(Settings.SCREEN_OFF_CHARGING, true);
                            settings.setEnabledWhile(Settings.SCREEN_OFF_BATTERY, true);
                        } finally {
                            settings.save(true);
                        }
                        chooseNotificationStyle.run();
                    })
                    .setNegativeButton(R.string.no, (dialog, which) -> done.run())
                    .show()).setCanceledOnTouchOutside(false);

        settings.edit();
        try {
            settings.setEnabledWhile(Settings.SCREEN_ON_CHARGING, true);
            settings.setEnabledWhile(Settings.SCREEN_OFF_CHARGING, false);
            settings.setEnabledWhile(Settings.SCREEN_ON_BATTERY, true);
            settings.setEnabledWhile(Settings.SCREEN_OFF_BATTERY, false);
            settings.setAnimationMode(Settings.SCREEN_ON_CHARGING, SpritePlayer.Mode.SWIRL);
            settings.setAnimationMode(Settings.SCREEN_OFF_CHARGING, SpritePlayer.Mode.TSP);
            settings.setAnimationMode(Settings.SCREEN_ON_BATTERY, SpritePlayer.Mode.SWIRL);
            settings.setAnimationMode(Settings.SCREEN_OFF_BATTERY, SpritePlayer.Mode.TSP);
            settings.setHideAOD(true);
        } finally {
            settings.save(true);
        }
        Runnable welcome = () ->
            (currentDialog = newSetupAlert(R.string.setup_wizard_welcome)
                    .setPositiveButton(android.R.string.ok, (dialog, which) -> runWhileScreenOff.run())
                    .show()).setCanceledOnTouchOutside(false);
        welcome.run();

        return true;
    }

    @Override
    protected void onStart() {
        super.onStart();
        Permissions.unnotify(this);
        handler.removeCallbacks(checkPermissionsRunnable);
        handler.postDelayed(checkPermissionsRunnable, 1000);
        TestNotification.show(this, TestNotification.NOTIFICATION_ID_MAIN);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (checkPermissionsOnResume) {
            checkPermissionsOnResume = false;
            handler.removeCallbacks(checkPermissionsRunnable);
            handler.postDelayed(checkPermissionsRunnable, 1000);
        }
    }

    @Override
    protected void onStop() {
        handler.removeCallbacks(checkPermissionsRunnable);
        TestNotification.hide(this, TestNotification.NOTIFICATION_ID_MAIN);
        super.onStop();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == LOGCAT_DUMP_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                if (data != null && data.getData() != null) logcatDump(data.getData());
            }
        } else {
            checkPermissions();
        }
    }

    @Override
    public void onBackPressed() {
        finish();
    }

    @Override
    public void finish() {
        TestNotification.hide(this, TestNotification.NOTIFICATION_ID_MAIN);
        super.finish();
    }

    @SuppressLint("AlwaysShowAction")
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        @SuppressLint("InflateParams") View layout = getLayoutInflater().inflate(R.layout.toolbar_switch, null);
        switchMaster = layout.findViewById(R.id.toolbar_switch);
        switchMaster.setChecked(settings.isEnabled());
        switchMaster.setOnCheckedChangeListener((buttonView, isChecked) -> settings.setEnabled(isChecked));

        MenuItem item = menu.add("");
        item.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        item.setActionView(layout);
        return true;
    }
}
