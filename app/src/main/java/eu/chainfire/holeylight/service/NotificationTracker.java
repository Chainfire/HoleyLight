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

import android.os.Parcel;
import android.os.Parcelable;
import android.os.SystemClock;
import android.service.notification.StatusBarNotification;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import eu.chainfire.holeylight.BuildConfig;
import eu.chainfire.holeylight.test.TestRunner;

@SuppressWarnings({"WeakerAccess"})
public class NotificationTracker {
    @SuppressWarnings("rawtypes")
    public static class Item implements Parcelable {
        private final String key;
        private final long posted;
        private final long when;
        private final long firstSeen;
        private final boolean[] seen = new boolean[] { false, false };
        private int shown = 0;

        public static final Parcelable.Creator CREATOR = new Parcelable.Creator() {
            public Item createFromParcel(Parcel in) {
                return new Item(in);
            }

            public Item[] newArray(int size) {
                return new Item[size];
            }
        };

        public Item(StatusBarNotification sbn) {
            key = sbn.getKey();
            posted = sbn.getPostTime();
            when = sbn.getNotification().when;
            firstSeen = SystemClock.elapsedRealtime();
        }

        public Item(Parcel in) {
            key = in.readString();
            posted = in.readLong();
            when = in.readLong();
            firstSeen = in.readLong();
            seen[0] = in.readInt() == 1;
            seen[1] = in.readInt() == 1;
            shown = in.readInt();
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

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeString(key);
            dest.writeLong(posted);
            dest.writeLong(when);
            dest.writeLong(firstSeen);
            dest.writeInt(seen[0] ? 1 : 0);
            dest.writeInt(seen[1] ? 1 : 0);
            dest.writeInt(shown);
        }
    }

    private static NotificationTracker instance = null;
    public static NotificationTracker getInstance() {
        if (instance == null) instance = new NotificationTracker();
        return instance;
    }

    private NotificationTracker() {
    }

    private final List<Item> items = new ArrayList<>();

    private void load(Item[] array) {
        items.clear();
        if (array != null) {
            items.addAll(Arrays.asList(array));
        }
    }

    private Item[] save() {
        return items.toArray(new Item[0]);
    }

    public void loadFromBytes(byte[] bytes) {
        // Using Parcelables with AlarmManager doesn't work these days

        if (bytes == null) {
            load(null);
        } else {
            Parcel parcel = Parcel.obtain();
            parcel.unmarshall(bytes, 0, bytes.length);
            parcel.setDataPosition(0);
            Item[] items = new Item[parcel.readInt()];
            for (int i = 0; i < items.length; i++) {
                items[i] = (Item)Item.CREATOR.createFromParcel(parcel);
            }
            load(items);
            parcel.recycle();
        }
    }

    public byte[] saveToBytes() {
        // Using Parcelables with AlarmManager doesn't work these days

        Item[] items = save();
        Parcel parcel = Parcel.obtain();
        parcel.writeInt(items.length);
        for (Item item : items) {
            item.writeToParcel(parcel, 0);
        }
        byte[] result = parcel.marshall();
        parcel.recycle();
        return result;
    }

    public StatusBarNotification[] prune(StatusBarNotification[] active, boolean addNewNotifications, int timeout, Boolean screenOnForTracking, boolean screenOn) {
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
            boolean isSelf = BuildConfig.APPLICATION_ID.equals(sbn.getPackageName());
            boolean blockSelf = isSelf && !screenOn && !TestRunner.isRunning();
            boolean blockOther = !isSelf && TestRunner.isRunning();

            boolean found = false;
            for (Item item : items) {
                if (item.match(sbn)) {
                    if (!item.getSeen(screenOnForTracking) || sbn.getPackageName().equals(BuildConfig.APPLICATION_ID)) {
                        if ((timeout > 0) && (now - item.firstSeen > timeout) && (item.shown > 0) && !sbn.getPackageName().equals(BuildConfig.APPLICATION_ID)) {
                            item.setSeen(screenOnForTracking);
                        } else {
                            item.shown++;
                            if (!blockSelf && !blockOther) {
                                sbns.add(sbn);
                            }
                        }
                    }
                    found = true;
                    break;
                }
            }
            if (!found) {
                Item item = new Item(sbn);
                if (addNewNotifications || isSelf) {
                    if (!blockSelf && !blockOther) {
                        sbns.add(sbn);
                    }
                } else {
                    item.setSeen(screenOnForTracking);
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

