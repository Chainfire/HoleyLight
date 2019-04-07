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

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import eu.chainfire.holeylight.R;
import eu.chainfire.holeylight.misc.Settings;
import top.defaults.colorpicker.ColorPickerPopup;

public class ColorActivity extends AppCompatActivity {
    private Settings settings = null;

    @SuppressWarnings("ConstantConditions")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_color);

        settings = Settings.getInstance(this);

        getSupportActionBar().setTitle(R.string.settings_animation_colors_title);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        RecyclerView list = findViewById(R.id.list);
        list.setLayoutManager(new LinearLayoutManager(this));
        list.addItemDecoration(new DividerItemDecoration(this, LinearLayoutManager.VERTICAL));
        AppAdapter apps = new AppAdapter();
        list.setAdapter(apps);

        //TODO Async this, slow with many items
        apps.loadAppItems(settings.getPackagesAndColors());
    }

    @Override
    protected void onStart() {
        super.onStart();
        TestNotification.show(this, TestNotification.NOTIFICATION_ID_COLOR);
    }

    @Override
    protected void onStop() {
        TestNotification.hide(this, TestNotification.NOTIFICATION_ID_COLOR);
        super.onStop();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings({"WeakerAccess"})
    private class AppAdapter extends RecyclerView.Adapter<AppAdapter.ViewHolder> {
        private final List<AppItem> items;

        public AppAdapter() {
            this.items = new ArrayList<>();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_color, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull final ViewHolder holder, int position) {
            holder.appItem = items.get(position);
            holder.ivIcon.setImageDrawable(holder.appItem.icon);
            if (holder.appItem.title == null) {
                holder.tvTitle.setText(holder.appItem.packageName);
                holder.tvPackageName.setText("");
            } else {
                holder.tvTitle.setText(holder.appItem.title);
                holder.tvPackageName.setText(holder.appItem.packageName);
            }
            holder.ivColor.setBackgroundColor(holder.appItem.color);
            holder.view.setOnClickListener(v -> {
                final AppItem item = holder.appItem;
                (new ColorPickerPopup.Builder(ColorActivity.this))
                        .initialColor(holder.appItem.color)
                        .enableAlpha(false)
                        .enableBrightness(true)
                        .okTitle(getString(android.R.string.ok))
                        .cancelTitle(getString(android.R.string.cancel))
                        .showIndicator(true)
                        .showValue(false)
                        .build()
                        .show(new ColorPickerPopup.ColorPickerObserver() {
                            @Override
                            public void onColorPicked(int color) {
                                settings.setColorForPackage(item.packageName, color, false);
                                item.color = color;

                                // force overlay reload
                                boolean startEnabled = settings.isEnabled();
                                settings.setEnabled(!startEnabled);
                                settings.setEnabled(startEnabled);

                                notifyDataSetChanged();
                            }
                        });
            });
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        public class ViewHolder extends RecyclerView.ViewHolder {
            public final View view;
            public final ImageView ivIcon;
            public final TextView tvTitle;
            public final TextView tvPackageName;
            public final View ivColor;
            public AppItem appItem;

            public ViewHolder(View view) {
                super(view);
                this.view = view;
                ivIcon = view.findViewById(R.id.icon);
                tvTitle = view.findViewById(R.id.title);
                tvPackageName = view.findViewById(R.id.packageName);
                ivColor = view.findViewById(R.id.color);
            }

            @NonNull
            @Override
            public String toString() {
                return super.toString() + " '" + appItem.packageName + "'";
            }
        }

        private String getLabel(PackageManager pm, ApplicationInfo info) {
            try {
                return info.loadLabel(pm).toString();
            } catch (Exception e) {
                return null;
            }
        }

        private Drawable getIcon(PackageManager pm, ApplicationInfo info) {
            try {
                return info.loadIcon(pm);
            } catch (Exception e) {
                return null;
            }
        }

        @SuppressWarnings("ConstantConditions")
        public void loadAppItems(Map<String, Integer> packagesAndColors) {
            List<AppItem> results = new ArrayList<>();
            PackageManager pm = getPackageManager();
            for (String pkg : packagesAndColors.keySet()) {
                try {
                    ApplicationInfo info = pm.getApplicationInfo(pkg, 0);
                    results.add(new AppItem(
                            getLabel(pm, info),
                            pkg,
                            getIcon(pm, info),
                            packagesAndColors.get(pkg)
                    ));
                } catch (PackageManager.NameNotFoundException e) {
                    e.printStackTrace();
                }
            }
            Collections.sort(results, (a, b) -> {
                if ((a.title == null) && (b.title == null)) {
                    return a.packageName.compareToIgnoreCase(b.packageName);
                } else if ((a.title == null) && (b.title != null)) {
                    return 1;
                } else if ((a.title != null) && (b.title == null)) {
                    return -1;
                } else {
                    return a.title.compareToIgnoreCase(b.title);
                }
            });
            items.clear();
            items.addAll(results);
        }

        public class AppItem {
            public final String title;
            public final String packageName;
            public final Drawable icon;
            public int color;

            public AppItem(String title, String packageName, Drawable icon, int color) {
                if ((title != null) && title.equals(packageName)) {
                    title = null;
                }
                this.title = title;
                this.packageName = packageName;
                this.icon = icon;
                this.color = color;
            }
        }
    }
}

