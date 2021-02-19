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

import android.content.Context;
import android.content.res.Configuration;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import eu.chainfire.holeylight.misc.LocaleHelper;

/* Sheer insanity */
public class BaseActivity extends AppCompatActivity {
    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.getContext(newBase));
    }

    @Override
    public void applyOverrideConfiguration(Configuration overrideConfiguration) {
        super.applyOverrideConfiguration(LocaleHelper.getConfiguration(this));
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        LocaleHelper.updateResources(this, newConfig);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        LocaleHelper.updateResources(this);
        super.onCreate(savedInstanceState);
        getDelegate().applyDayNight();
    }

    @Override
    protected void onStart() {
        super.onStart();
        LocaleHelper.updateResources(this);
        getDelegate().applyDayNight();
    }

    @Override
    protected void onResume() {
        super.onResume();
        LocaleHelper.updateResources(this);
        getDelegate().applyDayNight();
    }
}
