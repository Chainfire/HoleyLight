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

package eu.chainfire.holeylight.service;

import android.os.SystemClock;
import android.service.notification.StatusBarNotification;

import java.util.ArrayList;
import java.util.List;

import eu.chainfire.holeylight.BuildConfig;

@SuppressWarnings({"WeakerAccess"})
public class NotificationTracker {
    public static class Item {
        private final String key;
        private final long posted;
        private final long when;
        private final long firstSeen;
        private final boolean[] seen = new boolean[] { false, false };
        private int shown = 0;

        public Item(StatusBarNotification sbn) {
            key = sbn.getKey();
            posted = sbn.getPostTime();
            when = sbn.getNotification().when;
            firstSeen = SystemClock.elapsedRealtime();
        }

        public boolean match(StatusBarNotification sbn) {
            return key.equals(sbn.getKey()) && (posted == sbn.getPostTime()) && (when == sbn.getNotification().when);
        }

        public boolean getSeen(Boolean screenOn) {
            if (screenOn == null) {
                return seen[0] || seen[1];
            } else if (!screenOn) {
                return seen[0];
            } else {
                return seen[1];
            }
        }

        public void setSeen(Boolean screenOn) {
            if (screenOn == null) {
                seen[0] = true;
                seen[1] = true;
            } else if (!screenOn) {
                seen[0] = true;
            } else {
                seen[1] = true;
            }
        }
    }

    private final List<Item> items = new ArrayList<>();

    public StatusBarNotification[] prune(StatusBarNotification[] active, boolean addNewNotifications, int timeout, Boolean screenOn) {
        long now = SystemClock.elapsedRealtime();

        // remove notifications from our own list that are no longer active
        for (int i = items.size() - 1; i >= 0; i--) {
            Item item = items.get(i);

            boolean found = false;
            for (StatusBarNotification sbn : active) {
                if (item.match(sbn)) {
                    found = true;
                    break;
                }
            }

            if (!found) {
                items.remove(i);
            }
        }

        // find all active notifications that are not marked as seen in our own list
        List<StatusBarNotification> sbns = new ArrayList<>();
        for (StatusBarNotification sbn : active) {
            boolean found = false;
            for (Item item : items) {
                if (item.match(sbn)) {
                    if (!item.getSeen(screenOn) || sbn.getPackageName().equals(BuildConfig.APPLICATION_ID)) {
                        if ((timeout > 0) && (now - item.firstSeen > timeout) && (item.shown > 0) && !sbn.getPackageName().equals(BuildConfig.APPLICATION_ID)) {
                            item.setSeen(screenOn);
                        } else {
                            item.shown++;
                            sbns.add(sbn);
                        }
                    }
                    found = true;
                    break;
                }
            }
            if (!found) {
                Item item = new Item(sbn);
                if (addNewNotifications || BuildConfig.APPLICATION_ID.equals(sbn.getPackageName())) {
                    sbns.add(sbn);
                } else {
                    item.setSeen(screenOn);
                }
                items.add(item);
            }
        }
        return sbns.toArray(new StatusBarNotification[0]);
    }

    public void clear() {
        items.clear();
    }

    public void markAllAsSeen() {
        for (Item item : items) {
            item.setSeen(null);
        }
    }
}

