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
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.ActionBar;
import eu.chainfire.holeylight.R;
import eu.chainfire.holeylight.animation.NotificationAnimation;
import eu.chainfire.holeylight.misc.Settings;

public class TuneActivity extends BaseActivity implements Settings.OnSettingsChangedListener {
    private Settings settings = null;
    private NotificationAnimation animation = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tune);

        settings = Settings.getInstance(this);
        animation = new NotificationAnimation(this, null, 0, null);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(R.string.settings_animation_tune_title);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        TestNotification.show(this, TestNotification.NOTIFICATION_ID_TUNE);
        updateLabels();
        settings.registerOnSettingsChangedListener(this);
        Settings.tuning = true;
    }

    @Override
    protected void onStop() {
        Settings.tuning = false;
        settings.unregisterOnSettingsChangedListener(this);
        TestNotification.hide(this, TestNotification.NOTIFICATION_ID_TUNE);
        super.onStop();
    }

    @Override
    public void onSettingsChanged() {
        updateLabels();
    }

    public void btnClick(View view) {
        if (view == findViewById(R.id.btnAddScaleBaseMinus)) {
            settings.setDpAddScaleBase(animation.getDpAddScaleBase() - 0.25f);
        } else if (view == findViewById(R.id.btnAddScaleBasePlus)) {
            settings.setDpAddScaleBase(animation.getDpAddScaleBase() + 0.25f);
        } else if (view == findViewById(R.id.btnAddScaleHorizontalMinus)) {
            settings.setDpAddScaleHorizontal(animation.getDpAddScaleHorizontal() - 0.25f);
        } else if (view == findViewById(R.id.btnAddScaleHorizontalPlus)) {
            settings.setDpAddScaleHorizontal(animation.getDpAddScaleHorizontal() + 0.25f);
        } else if (view == findViewById(R.id.btnShiftVerticalMinus)) {
            settings.setDpShiftVertical(animation.getDpShiftVertical() - 0.25f);
        } else if (view == findViewById(R.id.btnShiftVerticalPlus)) {
            settings.setDpShiftVertical(animation.getDpShiftVertical() + 0.25f);
        } else if (view == findViewById(R.id.btnShiftHorizontalMinus)) {
            settings.setDpShiftHorizontal(animation.getDpShiftHorizontal() - 0.25f);
        } else if (view == findViewById(R.id.btnShiftHorizontalPlus)) {
            settings.setDpShiftHorizontal(animation.getDpShiftHorizontal() + 0.25f);
        } else if (view == findViewById(R.id.btnAddThicknessMinus)) {
            settings.setDpAddThickness(animation.getDpAddThickness() - 0.25f);
            settings.setDpAddScaleBase(animation.getDpAddScaleBase() - 0.25f);
        } else if (view == findViewById(R.id.btnAddThicknessPlus)) {
            settings.setDpAddThickness(animation.getDpAddThickness() + 0.25f);
            settings.setDpAddScaleBase(animation.getDpAddScaleBase() + 0.25f);
        } else if (view == findViewById(R.id.btnSpeedMinus)) {
            settings.setSpeedFactor(animation.getSpeedFactor() - 0.1f);
        } else if (view == findViewById(R.id.btnSpeedPlus)) {
            settings.setSpeedFactor(animation.getSpeedFactor() + 0.1f);
        }
    }

    private void updateLabels() {
        ((TextView)findViewById(R.id.tvAddScaleBase)).setText(getString(R.string.tune_scale_base, animation.getDpAddScaleBase()));
        ((TextView)findViewById(R.id.tvAddScaleHorizontal)).setText(getString(R.string.tune_scale_horizontal, animation.getDpAddScaleHorizontal()));
        ((TextView)findViewById(R.id.tvShiftVertical)).setText(getString(R.string.tune_shift_vertical, animation.getDpShiftVertical()));
        ((TextView)findViewById(R.id.tvShiftHorizontal)).setText(getString(R.string.tune_shift_horizontal, animation.getDpShiftHorizontal()));
        ((TextView)findViewById(R.id.tvAddThickness)).setText(getString(R.string.tune_thickness, animation.getDpAddThickness()));
        ((TextView)findViewById(R.id.tvSpeed)).setText(getString(R.string.tune_speed, animation.getSpeedFactor()));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @SuppressLint("AlwaysShowAction")
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuItem menuItem = menu.add(R.string.reset);
        menuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        menuItem.setOnMenuItemClickListener(item -> {
            settings.resetTuning();
            updateLabels();
            return true;
        });
        return super.onCreateOptionsMenu(menu);
    }
}
