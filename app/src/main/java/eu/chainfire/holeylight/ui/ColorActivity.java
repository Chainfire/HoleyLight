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
import android.text.Html;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import eu.chainfire.holeylight.R;
import eu.chainfire.holeylight.misc.Settings;
import eu.chainfire.holeylight.service.NotificationListenerService;
import top.defaults.colorpicker.ColorPickerPopup;

public class ColorActivity extends AppCompatActivity {
    private Settings settings = null;
    private AppAdapter apps = null;

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
        apps = new AppAdapter();
        list.setAdapter(apps);

        //TODO Async this, slow with many items
        apps.loadAppItems(settings.getPackagesChannelsAndColors());
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
    public void onBackPressed() {
        finish();
    }

    @Override
    protected void onUserLeaveHint() {
        finish();
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
        //TODO Refactor help/header/item into different ViewHolder classes, etc

        private static final int VIEW_TYPE_HELP = 0;
        private static final int VIEW_TYPE_HEADER = 1;
        private static final int VIEW_TYPE_ITEM = 2;

        private final List<AppItem> items;

        public AppAdapter() {
            this.items = new ArrayList<>();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view;
            switch (viewType) {
                case VIEW_TYPE_HELP:
                    view = LayoutInflater.from(parent.getContext())
                            .inflate(R.layout.item_color_help, parent, false);
                    return new ViewHolder(view);
                case VIEW_TYPE_HEADER:
                    view = LayoutInflater.from(parent.getContext())
                            .inflate(R.layout.item_color_header, parent, false);
                    return new ViewHolder(view);
                case VIEW_TYPE_ITEM:
                    view = LayoutInflater.from(parent.getContext())
                            .inflate(R.layout.item_color, parent, false);
                    return new ViewHolder(view);
            }
            return null;
        }

        @SuppressWarnings("deprecation")
        @Override
        public void onBindViewHolder(@NonNull final ViewHolder holder, int position) {
            holder.appItem = items.get(position);
            if (holder.appItem.viewType == VIEW_TYPE_HELP) {
                holder.tvTitleOrPackage.setText(Html.fromHtml(getString(R.string.colors_help)));
            } else if (holder.appItem.viewType == VIEW_TYPE_HEADER) {
                holder.tvTitleOrPackage.setText(holder.appItem.title);
            } else if (holder.appItem.viewType == VIEW_TYPE_ITEM) {
                holder.ivIcon.setImageDrawable(holder.appItem.icon);
                holder.tvTitleOrPackage.setText(holder.appItem.title != null ? holder.appItem.title : holder.appItem.packageName);
                if (holder.appItem.tickerText != null) {
                    holder.tvChannel.setText(Html.fromHtml("<small>" + holder.appItem.channelName + "<br>" + holder.appItem.tickerText.toString() + "</small>"));
                } else {
                    holder.tvChannel.setText(Html.fromHtml("<small>" + holder.appItem.channelName + "</small>"));
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
                                    settings.setColorForPackageAndChannel(item.packageName, item.channelName, color, false);
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
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        @Override
        public int getItemViewType(int position) {
            return items.get(position).viewType;
        }

        public class ViewHolder extends RecyclerView.ViewHolder {
            public final View view;
            public final ImageView ivIcon;
            public final TextView tvTitleOrPackage;
            public final TextView tvChannel;
            public final View ivColor;
            public AppItem appItem;

            public ViewHolder(View view) {
                super(view);

                this.view = view;
                ivIcon = view.findViewById(R.id.icon);
                tvChannel = view.findViewById(R.id.channel);
                ivColor = view.findViewById(R.id.color);

                TextView tv;
                tv = view.findViewById(R.id.titleOrPackage);
                if (tv == null) tv = view.findViewById(R.id.title);
                tvTitleOrPackage = tv;
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
        public void loadAppItems(Map<String, Integer> packagesChannelsAndColors) {
            List<NotificationListenerService.ActiveNotification> active = null;
            NotificationListenerService service = NotificationListenerService.getInstance();
            if (service != null) {
                active = service.getCurrentlyActiveNotifications();
            }

            List<AppItem> activeItems = new ArrayList<>();
            List<AppItem> inactiveItems = new ArrayList<>();
            PackageManager pm = getPackageManager();
            for (String pkgChan : packagesChannelsAndColors.keySet()) {
                int sep = pkgChan.indexOf(':');
                if (sep >= 0) {
                    String pkg = pkgChan.substring(0, sep);
                    String chan = pkgChan.substring(sep + 1);
                    try {
                        NotificationListenerService.ActiveNotification found = null;
                        if (active != null) {
                            for (NotificationListenerService.ActiveNotification not : active) {
                                if (not.getPackageName().equals(pkg) && not.getChannelName().equals(chan)) {
                                    found = not;
                                    break;
                                }
                            }
                        }

                        List<AppItem> target = found != null ? activeItems : inactiveItems;
                        ApplicationInfo info = pm.getApplicationInfo(pkg, 0);
                        target.add(new AppItem(
                                getLabel(pm, info),
                                pkg,
                                chan,
                                found != null ? found.getTickerText() : null,
                                getIcon(pm, info),
                                packagesChannelsAndColors.get(pkgChan),
                                VIEW_TYPE_ITEM
                        ));
                    } catch (PackageManager.NameNotFoundException e) {
                        e.printStackTrace();
                    }
                }
            }
            Comparator<AppItem> sorter = (a, b) -> {
                int sort;
                if ((a.title == null) && (b.title == null)) {
                    sort = a.packageName.compareToIgnoreCase(b.packageName);
                } else if ((a.title == null) && (b.title != null)) {
                    sort = 1;
                } else if ((a.title != null) && (b.title == null)) {
                    sort = -1;
                } else {
                    sort = a.title.compareToIgnoreCase(b.title);
                }
                if (sort == 0) {
                    sort = a.channelName.compareToIgnoreCase(b.channelName);
                }
                return sort;
            };

            Collections.sort(activeItems, sorter);
            Collections.sort(inactiveItems, sorter);

            items.clear();
            items.add(new AppItem("", null, null, null, null, 0, VIEW_TYPE_HELP));
            items.add(new AppItem(getString(R.string.colors_header_active), null, null, null, null, 0, VIEW_TYPE_HEADER));
            items.addAll(activeItems);
            items.add(new AppItem(getString(R.string.colors_header_inactive), null, null, null, null, 0, VIEW_TYPE_HEADER));
            items.addAll(inactiveItems);
            notifyDataSetChanged();
        }

        public class AppItem {
            public final String title;
            public final String packageName;
            public final String channelName;
            public final CharSequence tickerText;
            public final Drawable icon;
            public int color;
            public final int viewType;

            public AppItem(String title, String packageName, String channelName, CharSequence tickerText, Drawable icon, int color, int viewType) {
                if ((title != null) && title.equals(packageName)) {
                    title = null;
                }
                this.title = title;
                this.packageName = packageName;
                this.channelName = channelName;
                this.tickerText = tickerText;
                this.icon = icon;
                this.color = color;
                this.viewType = viewType;
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuItem menuItem = menu.add(R.string.refresh);
        menuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        menuItem.setOnMenuItemClickListener(item -> {
            //TODO Async this, slow with many items
            apps.loadAppItems(settings.getPackagesChannelsAndColors());
            return true;
        });
        return super.onCreateOptionsMenu(menu);
    }
}

