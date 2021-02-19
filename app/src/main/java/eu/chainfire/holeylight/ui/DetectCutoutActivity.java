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

import android.os.Bundle;

import androidx.core.view.ViewCompat;
import eu.chainfire.holeylight.BuildConfig;
import eu.chainfire.holeylight.R;
import eu.chainfire.holeylight.animation.NotificationAnimation;
import eu.chainfire.holeylight.service.NotificationTracker;

public class DetectCutoutActivity extends BaseActivity {
    private NotificationAnimation animation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detect_cutout);

        NotificationTracker tracker = NotificationTracker.getInstance();
        byte[] items = getIntent().getByteArrayExtra(BuildConfig.APPLICATION_ID + "/notifications");
        tracker.loadFromBytes(items);

        animation = new NotificationAnimation(this, null, 0, null);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.container), (view, insets) -> {
            animation.updateFromInsets(insets);
            finish();
            return insets;
        });
    }
}
