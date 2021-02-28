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
import android.content.ComponentName;
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

import com.android.billingclient.api.AcknowledgePurchaseParams;
import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.SkuDetails;
import com.android.billingclient.api.SkuDetailsParams;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.content.FileProvider;
import eu.chainfire.holeylight.BuildConfig;
import eu.chainfire.holeylight.R;
import eu.chainfire.holeylight.animation.SpritePlayer;
import eu.chainfire.holeylight.misc.AODControl;
import eu.chainfire.holeylight.misc.Manufacturer;
import eu.chainfire.holeylight.misc.Permissions;
import eu.chainfire.holeylight.misc.Settings;
import eu.chainfire.holeylight.misc.Slog;

public class MainActivity extends BaseActivity implements Settings.OnSettingsChangedListener {
    private static final int LOGCAT_DUMP_REQUEST_CODE = 12345;

    private Handler handler = null;
    private Settings settings = null;
    private SwitchCompat switchMaster = null;
    private Dialog currentDialog = null;
    private boolean checkPermissionsOnResume = false;
    private boolean allowHideNotification = true;

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

        switch (Permissions.detect(this, true)) {
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
                (currentDialog = newAlert(false)
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
            case AOD_HELPER_UPDATE:
                (currentDialog = newAlert(false)
                        .setTitle(R.string.aod_helper)
                        .setMessage(Html.fromHtml(getString(R.string.aod_helper_update)))
                        .setPositiveButton(R.string.instructions, (dialog, which) -> {
                            checkPermissionsOnResume = true;
                            aodHelperInstructions();
                        })
                        .setNegativeButton(R.string.ignore, (dialog, which) -> {
                            Permissions.allowAODHelperUpdateNeeded = false;
                            checkPermissions();
                        })
                        .show()).setCanceledOnTouchOutside(false);
                break;
            case AOD_HELPER_PERMISSIONS:
                (currentDialog = newAlert(false)
                        .setTitle(R.string.aod_helper)
                        .setMessage(Html.fromHtml(getString(R.string.aod_helper_permissions)))
                        .setNeutralButton(R.string.root, (dialog, which) -> {
                            AODControl.fixHelperPermissions(MainActivity.this, result -> {
                                settings.incUpdateCounter();
                                checkPermissions();
                            });
                        })
                        .setPositiveButton(R.string.instructions, (dialog, which) -> {
                            checkPermissionsOnResume = true;
                            aodHelperInstructions();
                        })
                        .setNegativeButton(R.string.ignore, (dialog, which) -> {
                            Permissions.allowAODHelperPermissionsNeeded = false;
                            checkPermissions();
                        })
                        .show()).setCanceledOnTouchOutside(false);
                break;
            case NONE:
                newAlert(false); // dismiss leftovers
                validateSettings(true);
        }
    }

    @SuppressWarnings({ "deprecation", "UnusedReturnValue" })
    public boolean validateSettings(boolean aodHelper) {
        if (!Settings.getInstance(this).isSetupWizardComplete()) return false;
        if (Permissions.detect(this, aodHelper) != Permissions.Needed.NONE) return false;

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
            aodRequired = !AODControl.isAODEnabled(this) && !settings.isAODHelperControl();
            if (aodWithImageRequired) {
                aodWithImageRequired = Manufacturer.isSamsung();
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
                if (!settings.isAODImageInstructionsShown()) {
                    showAODImageThemeInstructions(true);
                    settings.setAODImageInstructionsShown();
                }
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
        handler.removeCallbacks(billingConnectorRunnable);
        handler.post(billingConnectorRunnable);
        TestNotification.show(this, TestNotification.NOTIFICATION_ID_MAIN);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (checkPermissionsOnResume) {
            settings.incUpdateCounter();
            checkPermissionsOnResume = false;
            handler.removeCallbacks(checkPermissionsRunnable);
            handler.postDelayed(checkPermissionsRunnable, 1000);
        }
    }

    @Override
    protected void onStop() {
        handler.removeCallbacks(checkPermissionsRunnable);
        handler.removeCallbacks(billingConnectorRunnable);
        if (allowHideNotification) TestNotification.hide(this, TestNotification.NOTIFICATION_ID_MAIN);
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
        if (allowHideNotification) TestNotification.hide(this, TestNotification.NOTIFICATION_ID_MAIN);
        super.finish();
    }

    public void finish(boolean allowHideNotification) {
        this.allowHideNotification = allowHideNotification;
        finish();
    }

    @SuppressLint("AlwaysShowAction")
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        @SuppressLint("InflateParams") View layout = getLayoutInflater().inflate(R.layout.toolbar_switch, null);
        switchMaster = layout.findViewById(R.id.toolbar_switch);
        switchMaster.setChecked(settings.isEnabled());
        switchMaster.setOnCheckedChangeListener((buttonView, isChecked) -> settings.setEnabled(isChecked));
        switchMaster.setOnLongClickListener(v -> {
            String current = " " + getString(R.string.debug_mode_current);
            int selected;
            Boolean isDebug = settings.getDebug(true);
            Boolean isDebugOverlay = settings.getDebugOverlay(true);
            if ((isDebug == null) || (isDebugOverlay == null)) {
                selected = 3;
            } else if (isDebug) {
                if (isDebugOverlay) {
                    selected = 2;
                } else {
                    selected = 1;
                }
            } else {
                selected = 0;
            }
            (currentDialog = newAlert(false)
                    .setTitle(R.string.debug_dialog_title)
                    .setItems(new CharSequence[] {
                            getString(R.string.debug_mode_disabled) + (selected == 0 ? current : ""),
                            getString(R.string.debug_mode_logging) + (selected == 1 ? current : ""),
                            getString(R.string.debug_mode_overlay) + (selected == 2 ? current : ""),
                            getString(R.string.debug_mode_default) + (selected == 3 ? current : "")
                    }, (dialog, which) -> {
                        switch (which) {
                            case 0: settings.setDebug(false, false); break;
                            case 1: settings.setDebug(true, false); break;
                            case 2: settings.setDebug(true, true); break;
                            case 3: settings.setDebug(null, null); break;
                        }
                    })
                    .setNegativeButton(android.R.string.cancel, null)
                    .show()).setCanceledOnTouchOutside(false);
            return true;
        });

        MenuItem item = menu.add("");
        item.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        item.setActionView(layout);
        return true;
    }

    public void aodHelperInstructions() {
        try {
            Intent i = new Intent(Intent.ACTION_VIEW);
            i.setData(Uri.parse("https://github.com/Chainfire/HoleyLight/blob/master/apks/AODHelper.md"));
            startActivity(i);
        } catch (Exception e) {
            //  no action
        }
    }

    public void showAODImageThemeInstructions(boolean required) {
        StringBuilder sb = new StringBuilder();
        if (required) {
            sb.append(getString(R.string.notice_aod_image_required));
            sb.append("<br><br>");
        }
        sb.append(getString(R.string.notice_aod_image_image));
        sb.append("<br><br>");
        sb.append(getString(R.string.notice_aod_image_theme));
        sb.append("<br><br>");
        sb.append(getString(R.string.notice_aod_image_clock));
        sb.append("<br><br>");
        final String message = sb.toString();
        (currentDialog = newAlert(false)
                .setTitle(R.string.notice_dialog_title)
                .setMessage(Html.fromHtml(message))
                .setNeutralButton(required ? R.string.ignore : android.R.string.cancel, null)
                .setNegativeButton(R.string.image, (dialog, which) -> {
                    setBlackAODImage();
                })
                .setPositiveButton(R.string.theme, (dialog, which) -> {
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
    }

    private void setBlackAODImage() {
        boolean ok = false;
        File file = new File(getExternalFilesDir(null), "black_1x1.png");
        if (!file.exists()) {
            Uri uri = Uri.parse("android.resource://" + BuildConfig.APPLICATION_ID + "/drawable/black_1x1");
            try {
                InputStream is = getContentResolver().openInputStream(uri);
                OutputStream os = new FileOutputStream(file);
                try {
                    byte[] buf = new byte[1024];
                    while (is.read(buf) > 0) {
                        os.write(buf);
                    }
                    ok = true;
                } catch (IOException ignored) {
                } finally {
                    try { is.close(); } catch (Exception ignored) { }
                    try { os.close(); } catch (Exception ignored) { }
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        } else {
            ok = true;
        }

        if (ok) {
            Intent intent = new Intent("com.samsung.android.app.aodservice.intent.action.SET_AS_AOD");
            intent.setComponent(new ComponentName("com.samsung.android.app.aodservice", "com.samsung.android.app.aodservice.settings.opreditor.ImageOprEditActivity"));

            Uri uri = FileProvider.getUriForFile(this, BuildConfig.APPLICATION_ID + ".provider", file);
            intent.setDataAndType(uri, "image/jpeg");
            intent.putExtra("filePath", uri.toString());
            intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            startActivity(intent);
        }
    }

    // In-App Purchases

    private final String[] skus = new String[] { "donate.1", "donate.2", "donate.3", "donate.4", "donate.5" };
    private final int[] skusDescriptions = new int[] { R.string.donate_sku_donate_1, R.string.donate_sku_donate_2, R.string.donate_sku_donate_3, R.string.donate_sku_donate_4, R.string.donate_sku_donate_5 };

    public final List<SkuDetails> skusAvailable = new ArrayList<>();
    public boolean iapAvailable = false;

    private boolean billingClientConnected = false;

    private final Runnable billingConnectorRunnable = new Runnable() {
        @Override
        public void run() {
            createBillingClientConnection();
            handler.postDelayed(billingConnectorRunnable, 2500);
        }
    };

    private final PurchasesUpdatedListener purchasesUpdatedListener = (billingResult, purchases) -> {
        if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
            if (purchases != null) {
                for (Purchase purchase : purchases) {
                    Slog.d("IAP", "purchaseUpdate: %s", purchase.getSku());
                    handlePurchase(purchase);
                }
            }
        } // else we could show an error but the app really doesn't care about purchases for functionality
    };

    private BillingClient billingClient = null;

    private void createBillingClientConnection() {
        if (billingClientConnected) return;
        try {
            if (billingClient == null) {
                billingClient = BillingClient.newBuilder(this)
                        .setListener(purchasesUpdatedListener)
                        .enablePendingPurchases()
                        .build();
            }
            billingClient.startConnection(new BillingClientStateListener() {
                @Override
                public void onBillingSetupFinished(@NonNull BillingResult billingResult) {
                    if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                        billingClientConnected = true;
                        iapAvailable = true;

                        List<String> skuList = new ArrayList<>(Arrays.asList(skus));
                        SkuDetailsParams.Builder params = SkuDetailsParams.newBuilder();
                        params.setSkusList(skuList).setType(BillingClient.SkuType.INAPP);
                        billingClient.querySkuDetailsAsync(params.build(), (billingResult1, skuDetailsList) -> {
                            if (billingResult1.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                                skusAvailable.clear();
                                if (skuDetailsList != null) {
                                    for (SkuDetails details : skuDetailsList) {
                                        Slog.d("IAP", "querySkuDetails: %s: %s", details.getSku(), details.getPrice());
                                        skusAvailable.add(details);
                                    }
                                }
                            }
                        });
                        Purchase.PurchasesResult purchasesResult = billingClient.queryPurchases(BillingClient.SkuType.INAPP);
                        if (purchasesResult.getBillingResult() != null && purchasesResult.getBillingResult().getResponseCode() == BillingClient.BillingResponseCode.OK) {
                            List<Purchase> purchases = purchasesResult.getPurchasesList();
                            if (purchases != null) {
                                for (Purchase purchase : purchases) {
                                    Slog.d("IAP", "queryPurchases: %s", purchase.getSku());
                                    handlePurchase(purchase);
                                }
                            }
                        }
                        settings.incUpdateCounter();
                    }
                }
                @Override
                public void onBillingServiceDisconnected() {
                    billingClientConnected = false;
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handlePurchase(Purchase purchase) {
        try {
            if (!billingClientConnected) return;
            if (purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED) {
                if (!purchase.isAcknowledged()) {
                    AcknowledgePurchaseParams acknowledgePurchaseParams =
                        AcknowledgePurchaseParams.newBuilder()
                            .setPurchaseToken(purchase.getPurchaseToken())
                            .build();
                    Slog.d("IAP", "acknowledging: %s", purchase.getSku());
                    final String sku = purchase.getSku();
                    billingClient.acknowledgePurchase(acknowledgePurchaseParams, billingResult -> {
                        Slog.d("IAP", "acknowledged: %s", sku);
                        settings.setPurchased(purchase.getSku());
                    });
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void startPurchase() {
        if (skusAvailable.size() == 0) return;

        List<CharSequence> itemList = new ArrayList<>();
        final List<SkuDetails> skuList = new ArrayList<>();
        for (int i = 0; i < skus.length; i++) {
            SkuDetails details = null;
            for (int j = 0; j < skusAvailable.size(); j++) {
                if (skus[i].equals(skusAvailable.get(j).getSku())) {
                    details = skusAvailable.get(j);
                    break;
                }
            }
            if (details != null) {
                String already_purchased = settings.isPurchased(skus[i]) ? " - " + getString(R.string.donate_sku_already_purchased) : "";
                itemList.add(Html.fromHtml("<b>" + getString(skusDescriptions[i]) + "</b><br><small>" + details.getPrice() + already_purchased + "</small>"));
                skuList.add(details);
            }
        }

        (currentDialog = newAlert(false)
                .setTitle(R.string.donate_title)
                .setItems(itemList.toArray(new CharSequence[0]), (dialog, which) -> {
                    BillingFlowParams billingFlowParams = BillingFlowParams.newBuilder()
                            .setSkuDetails(skuList.get(which))
                            .build();
                    if (billingClient.launchBillingFlow(MainActivity.this, billingFlowParams).getResponseCode() != BillingClient.BillingResponseCode.OK) {
                        Slog.e("IAP", "error launching billing flow");
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show()).setCanceledOnTouchOutside(false);
    }
}