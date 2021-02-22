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

package eu.chainfire.holeylight.aodhelper;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class AODReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        setResultCode(0);
        if (intent == null) return;

        boolean isSamsung = false;
        boolean isGoogle = false;
        if (intent.hasExtra(BuildConfig.APPLICATION_ID + ".MANUFACTURER")) {
            String manufacturer = intent.getStringExtra(BuildConfig.APPLICATION_ID + ".MANUFACTURER");
            isSamsung = "samsung".equals(manufacturer);
            isGoogle = "google".equals(manufacturer);
        }

        if ((BuildConfig.APPLICATION_ID + ".FIX_PERMISSIONS").equals(intent.getAction())) {
            if (!isSamsung && context.checkCallingOrSelfPermission("android.permission.WRITE_SECURE_SETTINGS") != PackageManager.PERMISSION_GRANTED) {
                try {
                    Process process = Runtime.getRuntime().exec("su -c pm grant eu.chainfire.holeylight.aodhelper android.permission.WRITE_SECURE_SETTINGS");
                    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                    char[] buf = new char[1024];
                    while (bufferedReader.read(buf) == 1024) {
                    }
                    setResultCode(context.checkCallingOrSelfPermission("android.permission.WRITE_SECURE_SETTINGS") == PackageManager.PERMISSION_GRANTED ? 1 : 2);
                } catch (Exception e) {
                    setResultCode(3);
                    e.printStackTrace();
                }
            } else {
                setResultCode(1);
            }
        }

        if ((BuildConfig.APPLICATION_ID + ".SET_BRIGHTNESS").equals(intent.getAction())) {
            if (intent.hasExtra(BuildConfig.APPLICATION_ID + ".SET_BRIGHTNESS.enable")) {
                boolean enabled = intent.getBooleanExtra(BuildConfig.APPLICATION_ID + ".SET_BRIGHTNESS.enable", false);
                try {
                    ContentResolver resolver = context.getContentResolver();
                    if (isSamsung) {
                        setResultCode(1);
                    } else {
                        boolean ok = true;
                        if (!isGoogle) {
                            ok = (android.provider.Settings.Global.getString(resolver, "always_on_display_constants") != null);
                        }
                        if (ok) {
                            android.provider.Settings.Global.putString(resolver, "always_on_display_constants", enabled ? "screen_brightness_array=-1:255:255:255:255,dimming_scrim_array=-1:0:0:0:0" : "null");
                            setResultCode(1);
                        } else {
                            setResultCode(2);
                        }
                    }
                } catch (Exception e) {
                    // no permissions
                    setResultCode(3);
                    e.printStackTrace();
                }
            }
        }

        if ((BuildConfig.APPLICATION_ID + ".SET_AOD").equals(intent.getAction())) {
            if (intent.hasExtra(BuildConfig.APPLICATION_ID + ".SET_AOD.enable")) {
                boolean enabled = intent.getBooleanExtra(BuildConfig.APPLICATION_ID + ".SET_AOD.enable", false);
                try {
                    ContentResolver resolver = context.getContentResolver();
                    if (isSamsung) {
                        android.provider.Settings.System.putInt(resolver, "aod_mode", enabled ? 1 : 0);
                        android.provider.Settings.System.putInt(resolver, "aod_tap_to_show_mode", 0);
                        setResultCode(1);
                    } else {
                        //TODO This is currently not supported by the app as it needs more work

                        // Haven't really been able to figure out a good way to make this work.
                        // Unlike with Samsung, Google devices to not actually respond to these
                        // setting changes, doze has to be restarted for changes to take effect,
                        // and there doesn't seem to be a good reliable way to do this.
                        //
                        // Settings:
                        //   secure::doze_always_on
                        //      Controls AOD presence, changes effective after screen on/off cycle
                        //   secure::doze_enabled
                        //      Controls "wake on notification", effective immediately
                        //   global::always_on_display_constants
                        //      Brightness parameters and such, changes effective after on/off cycle
                        //
                        // Broadcast:
                        //   com.android.systemui::com.android.systemui.doze.pulse
                        //      Starts a pulse: lights up the screen into lockscreen mode with
                        //      a dark background. SCREEN_ON/USER_PRESENT not fired until user
                        //      unlocks or presses power button (which goes to "bright"
                        //      lockscreen)
                        //
                        // Possibly "dim" lockscreen can be detected with Display.State.ON +
                        // com.android.systemui:id/lock_icon presence in AccessibilityService,
                        // until SCREEN_ON/USER_PRESENT is seen (user activates) or Display.State
                        // goes to a lower state (screen dims again). Unfortunately there does
                        // not appear to be a good way to get this "ambient state" from the
                        // framework, even if undocumented and hacky.
                        //
                        // When in doze mode and the last notification disappears, we can turn doze
                        // off by setting doze_always_on to 0 and broadcasting a pulse. After a few
                        // seconds the pulse ends and doze restarts, re-reading the new
                        // configuration, and thus turning off. We could possibly hide the entire
                        // thing with a full black overlay (dismissing on SCREEN_ON/USER_PRESENT
                        // to catch user wanting to use device).
                        //
                        // When we want to go from "full off" to doze mode (i.e. the first
                        // notification comes in), we need to wake the device as well. The pulse
                        // broadcast doesn't work because doze isn't running - nobody is listening.
                        // However, if the doze_enabled setting is 1, the device will wake itself
                        // up for a few seconds just like a pulse broadcast. Again we can possibly
                        // cover this with an overlay to hide this from the user. When it powers
                        // down again the doze_always_on 1 setting sticks. We can if so desired
                        // turn off doze_enabled again after the first notification, because
                        // we don't need the wake-up behavior at that point. But from talking to
                        // users most of them seem to want this option, so we have to make it
                        // configurable.
                        //
                        // While messing around with overlays this way is tricky to get right,
                        // that part is doable. The big problem is the proximity sensor. If
                        // the proximity sensor is in the NEAR state, neither the pulse broadcast
                        // nor the notification wake-up do anything, the screen stays off. Due to
                        // this, doze is not restarted, and our new settings do not go into effect.
                        //
                        // While this is not necessarily a show-stopper for turning AOD off - we
                        // could just keep the overlay there and periodically keep trying to turn
                        // AOD off when the CPU randomly wakes until it sticks, possibly based
                        // on our own proximity sensor readings - it does become problematic for
                        // turning AOD on.
                        //
                        // The main problem case where this happens is when the user has the device
                        // in-pocket or in-handbag, and the first notification comes in. Due to
                        // proximity NEAR state, the screen-on triggers described above do nothing.
                        // We could force the screen on with a WakeLock, but then the screen is
                        // "really" on. There's no reliable way to turn it off again except wait
                        // for the normal timeout (which could be long or indefinitely when
                        // charging). DeviceAdmin.lockNow() is deprecated but may still work for
                        // this. Messing with the screen timeout itself to trigger a quick off
                        // is something I want to avoid - those things rarely end well. If we do,
                        // it would at least have to be a magic value to detect for recovery.
                        //
                        // We could monitor the proximity sensor ourselves for this case as well,
                        // and use our own notification to trigger the "wake on notification"
                        // functionality when the sensor goes into FAR state. But this doesn't work
                        // when the CPU is asleep, so we need to keep the CPU alive or periodically
                        // wake-and-poll. And that wake-and-poll should be often, because the one
                        // case we absolutely want this to work is when the device is pulled
                        // out-of-pocket and the users glances at it to see notifications, and
                        // this is also exactly the case which doesn't work well in this setup.
                        // Perhaps the "lift to check phone" setting can be of help here somehow,
                        // but it doesn't work particularly well in testing on my Pixel 2 XL.
                        //
                        // The above will need a lot of testing and tinkering to get any way right,
                        // and will possibly use so much CPU wake time that it defeats the entire
                        // purpose of controlling AOD to conserve power.
                        //
                        // Additionally, the app may need to work on its AOD setting timing,
                        // (if not already implemented when you read this), it currently switches
                        // to the AOD on state around SCREEN_OFF time, which for the Pixel is late
                        // because doze doesn't immediately respond. AOD off state happens as soon
                        // as the last notification disappears. Perhaps the on state should also
                        // trigger whenever a new notification comes in instead of around SCREEN_OFF
                        // time. That would need ample testing though. I recall having a reason
                        // for doing it that way but I don't remember what it was (thanks,
                        // comments!). It works well enough for Samsung devices, though.

                        try {
                            if (!isGoogle) {
                                // throws exception if it doesn't exist
                                android.provider.Settings.Secure.getInt(resolver, "doze_always_on");
                            }
                            android.provider.Settings.Secure.putInt(resolver, "doze_always_on", enabled ? 1 : 0);

                            // these are needed to activate the new state, as Google's default
                            // doze mode doesn't actually update state on changing the settings
                            if (enabled) {
                                // short notification wakeup
                                android.provider.Settings.Secure.putInt(resolver, "doze_enabled", 1);
                            } else {
                                // short pulse wakeup
                                Intent broadcast = new Intent("com.android.systemui.doze.pulse");
                                broadcast.setPackage("com.android.systemui");
                                context.sendBroadcast(broadcast);
                            }

                            setResultCode(1);
                        } catch (android.provider.Settings.SettingNotFoundException e) {
                            setResultCode(2);
                        }
                    }
                } catch (Exception e) {
                    // no permissions
                    setResultCode(3);
                    e.printStackTrace();
                }
            }
        }
    }
}
