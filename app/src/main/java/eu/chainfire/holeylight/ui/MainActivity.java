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

import android.annotation.SuppressLint;
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

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import eu.chainfire.holeylight.BuildConfig;
import eu.chainfire.holeylight.R;
import eu.chainfire.holeylight.misc.Permissions;
import eu.chainfire.holeylight.misc.Settings;

public class MainActivity extends AppCompatActivity implements Settings.OnSettingsChangedListener {
    private Handler handler = null;
    private Settings settings = null;
    private SwitchCompat switchMaster = null;
    private Dialog currentDialog = null;

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
            currentDialog.dismiss();
            currentDialog = null;
        }
        return (new AlertDialog.Builder(this))
                .setOnDismissListener(dialog -> {
                    currentDialog = null;
                    if (finishOnDismiss) {
                        MainActivity.this.finish();
                    }
        });
    }

    @SuppressWarnings("deprecation")
    private void checkPermissions() {
        switch (Permissions.detect(this)) {
            case DEVICE_SUPPORT:
                currentDialog = newAlert(true)
                        .setTitle(R.string.error)
                        .setMessage(Html.fromHtml(getString(R.string.error_unsupported_device, Build.DEVICE)))
                        .setPositiveButton(android.R.string.ok, null)
                        .show();
                break;
            case UNHIDE_NOTCH:
                currentDialog = newAlert(true)
                        .setTitle(R.string.error)
                        .setMessage(Html.fromHtml(getString(R.string.error_hide_notch)))
                        .setPositiveButton(android.R.string.ok, null)
                        .show();
                break;
            case COMPANION_DEVICE:
                currentDialog = newAlert(false)
                        .setTitle(getString(R.string.permission_required) + " 1/4")
                        .setMessage(Html.fromHtml(getString(R.string.permission_associate)))
                        .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                            CompanionDeviceManager companionDeviceManager = (CompanionDeviceManager)getSystemService(COMPANION_DEVICE_SERVICE);
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
                                    (new AlertDialog.Builder(MainActivity.this))
                                            .setTitle(getString(R.string.error))
                                            .setMessage(error)
                                            .setPositiveButton(android.R.string.ok, null)
                                            .show();
                                }
                            }, handler);
                        })
                        .show();
                break;
            case NOTIFICATION_SERVICE:
                currentDialog = newAlert(false)
                        .setTitle(getString(R.string.permission_required) + " 2/4")
                        .setMessage(Html.fromHtml(getString(R.string.permission_notifications)))
                        .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                            Intent intent = new Intent(android.provider.Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS);
                            startActivity(intent);
                        })
                        .show();
                break;
            case ACCESSIBILITY_SERVICE:
                currentDialog = newAlert(false)
                        .setTitle(getString(R.string.permission_required) + " 3/4")
                        .setMessage(Html.fromHtml(getString(R.string.permission_accessibility_v2)))
                        .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                            Intent intent = new Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS);
                            startActivity(intent);
                        })
                        .show();
                break;
            case BATTERY_OPTIMIZATION_EXEMPTION:
                currentDialog = newAlert(false)
                        .setTitle(getString(R.string.permission_required) + " 4/4")
                        .setMessage(Html.fromHtml(getString(R.string.permission_battery)))
                        .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                            @SuppressLint("BatteryLife") Intent intent = new Intent(android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                            intent.setData(Uri.parse("package:" + BuildConfig.APPLICATION_ID));
                            startActivity(intent);
                        })
                        .show();
                break;
            case NONE:
                newAlert(false); // dismiss leftovers
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        Permissions.unnotify(this);
        checkPermissions();
        TestNotification.show(this, TestNotification.NOTIFICATION_ID_MAIN);
    }

    @Override
    protected void onStop() {
        TestNotification.hide(this, TestNotification.NOTIFICATION_ID_MAIN);
        super.onStop();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        checkPermissions();
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
