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

package eu.chainfire.holeylight.service;

import android.service.notification.StatusBarNotification;

import java.util.ArrayList;
import java.util.List;

import eu.chainfire.holeylight.BuildConfig;

@SuppressWarnings({"WeakerAccess"})
public class NotificationTracker {
    public static class Item {
        private String key;
        private long posted;
        private long when;
        private boolean seen = false;

        public Item(StatusBarNotification sbn) {
            key = sbn.getKey();
            posted = sbn.getPostTime();
            when = sbn.getNotification().when;
        }

        public boolean match(StatusBarNotification sbn) {
            return key.equals(sbn.getKey()) && (posted == sbn.getPostTime()) && (when == sbn.getNotification().when);
        }
    }

    private List<Item> items = new ArrayList<>();

    public StatusBarNotification[] prune(StatusBarNotification[] active) {
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
                    if (!item.seen || sbn.getPackageName().equals(BuildConfig.APPLICATION_ID)) {
                        sbns.add(sbn);
                    }
                    found = true;
                    break;
                }
            }
            if (!found) {
                items.add(new Item(sbn));
                sbns.add(sbn);
            }
        }
        return sbns.toArray(new StatusBarNotification[0]);
    }

    public void clear() {
        items.clear();
    }

    public void markAllAsSeen() {
        for (Item item : items) {
            item.seen = true;
        }
    }
}
