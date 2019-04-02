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

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import eu.chainfire.holeylight.R;
import eu.chainfire.holeylight.misc.NotificationAnimation;
import eu.chainfire.holeylight.misc.Settings;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.TextView;

import java.util.Locale;

public class DebugActivity extends AppCompatActivity implements Settings.OnSettingsChangedListener {
    private Handler handler;
    private Settings settings = null;
    private NotificationAnimation animation = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_debug);

        handler = new Handler();
        settings = Settings.getInstance(this);
        animation = new NotificationAnimation(this,null, null);
    }

    @Override
    protected void onStart() {
        super.onStart();
        updateLabels();
        settings.registerOnSettingsChangedListener(this);
    }

    @Override
    protected void onStop() {
        settings.unregisterOnSettingsChangedListener(this);
        super.onStop();
    }

    @Override
    public void onSettingsChanged() {
        updateLabels();
    }

    public void btnClick(View view) {
        if (view == findViewById(R.id.btnAddScaleBaseMinus)) {
            settings.setDpAddScaleBase(animation.getDpAddScaleBase() - 1);
        } else if (view == findViewById(R.id.btnAddScaleBasePlus)) {
            settings.setDpAddScaleBase(animation.getDpAddScaleBase() + 1);
        } else if (view == findViewById(R.id.btnAddScaleHorizontalMinus)) {
            settings.setDpAddScaleBase(animation.getDpAddScaleHorizontal() - 1);
        } else if (view == findViewById(R.id.btnAddScaleHorizontalPlus)) {
            settings.setDpAddScaleBase(animation.getDpAddScaleHorizontal() + 1);
        } else if (view == findViewById(R.id.btnShiftVerticalMinus)) {
            settings.setDpAddScaleBase(animation.getDpShiftVertical() - 1);
        } else if (view == findViewById(R.id.btnShiftVerticalPlus)) {
            settings.setDpAddScaleBase(animation.getDpShiftVertical() + 1);
        } else if (view == findViewById(R.id.btnShiftHorizontalMinus)) {
            settings.setDpAddScaleBase(animation.getDpShiftHorizontal() - 1);
        } else if (view == findViewById(R.id.btnShiftHorizontalPlus)) {
            settings.setDpAddScaleBase(animation.getDpShiftHorizontal() + 1);
        }
    }

    private void updateLabels() {
        ((TextView)findViewById(R.id.tvAddScaleBase)).setText(String.format(Locale.ENGLISH, "Scale base: %ddp", animation.getDpAddScaleBase()));
        ((TextView)findViewById(R.id.tvAddScaleHorizontal)).setText(String.format(Locale.ENGLISH, "Scale horizontal: %ddp", animation.getDpAddScaleHorizontal()));
        ((TextView)findViewById(R.id.tvShiftVertical)).setText(String.format(Locale.ENGLISH, "Shift vertical: %ddp", animation.getDpShiftVertical()));
        ((TextView)findViewById(R.id.tvShiftHorizontal)).setText(String.format(Locale.ENGLISH, "Shift horizontal: %ddp", animation.getDpAddScaleHorizontal()));
    }

    public void btnNotificationTestClick(View view) {
        final NotificationManager notMan = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);

        NotificationManagerCompat.from(this).deleteNotificationChannel("eu.chainfire.test.1");
        final NotificationChannel chan = new NotificationChannel("eu.chainfire.test.1", getString(R.string.app_name), NotificationManager.IMPORTANCE_LOW);
        chan.setDescription(getString(R.string.app_name));
        chan.enableLights(true);
        chan.setLightColor(Color.RED);
        NotificationManagerCompat.from(this).createNotificationChannel(chan);

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                final Notification not = (new NotificationCompat.Builder(DebugActivity.this, chan.getId()))
                        .setContentTitle("title")
                        .setContentText("text")
                        .setBadgeIconType(NotificationCompat.BADGE_ICON_NONE)
                        .setOngoing(true)
                        .setOnlyAlertOnce(true)
                        .setNumber(0)
                        .setSmallIcon(R.drawable.ic_launcher_vector)
                        .build();

                NotificationManagerCompat.from(DebugActivity.this).notify(3, not);
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        NotificationManagerCompat.from(DebugActivity.this).cancel(3);
                    }
                }, 30000);
            }
        }, 10000);
    }
}
