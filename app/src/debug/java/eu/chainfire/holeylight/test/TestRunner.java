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

package eu.chainfire.holeylight.test;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.HandlerThread;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Locale;

import eu.chainfire.holeylight.BuildConfig;
import eu.chainfire.holeylight.animation.NotificationAnimation;
import eu.chainfire.holeylight.animation.Overlay;
import eu.chainfire.holeylight.animation.SpritePlayer;
import eu.chainfire.holeylight.misc.AODControl;
import eu.chainfire.holeylight.misc.Display;
import eu.chainfire.holeylight.misc.Permissions;
import eu.chainfire.holeylight.misc.Settings;
import eu.chainfire.holeylight.misc.Slog;
import eu.chainfire.holeylight.service.NotificationListenerService;
import eu.chainfire.holeylight.ui.TestNotification;

/*  To start:

    (Windows, will probably need some escaping on Linux)

    - kill current HoleyLight instance

    - adb shell "settings put secure enabled_accessibility_services eu.chainfire.holeylight/.service.AccessibilityService ; dumpsys deviceidle whitelist +eu.chainfire.holeylight && ( (sleep 5 && am broadcast -a eu.chainfire.holeylight.test eu.chainfire.holeylight) & ) && dalvikvm -cp $(pm path eu.chainfire.holeylight | sed 's/package://g') eu.chainfire.holeylight.test.TestShellRunner"

    - Ctrl+C to exit after "TEST COMPLETE" is logged

    - kill, restart, and reconfigure HoleyLight; preferably restart device

    AOD Helper required. Samsung only. Disable Do-Not-Disturb on device.

    //TODO add tests for AOD schedule... unfortunately Samsung seems to *set* Settings.System("aod_mode_start/end_time") but not read? Makes automating more difficult

 */

public class TestRunner {
    private static String TAG = "Test";

    private static void log(String msg, Object... params) {
        if ((params != null) && (params.length > 0)) {
            msg = String.format(Locale.ENGLISH, msg, params);
        }
        Slog.d(TAG, msg);
    }

    private static volatile TestRunner runner = null;

    public static void start(Context context) {
        synchronized (TestRunner.class) {
            if (runner != null) return;
            log("Start");
            runner = new TestRunner(context, () -> {
                synchronized (TestRunner.class) {
                    runner = null;
                }
            });
        }
    }

    public static boolean isRunning() {
        synchronized (TestRunner.class) {
            return runner != null;
        }
    }

    private interface CompletionCallback {
        void onComplete();
    }

    private final CompletionCallback onComplete;
    private Context context;
    private final Handler handlerMain;
    private final HandlerThread handlerThreadThread;
    private final Handler handlerThread;
    private final Settings settings;

    private boolean notificationState = false;

    private TestRunner(Context context, CompletionCallback onComplete) {
        this.context = context;
        this.onComplete = onComplete;
        handlerMain = new Handler();
        handlerThreadThread = new HandlerThread(BuildConfig.APPLICATION_ID + ":testRunner");
        handlerThreadThread.start();
        handlerThread = new Handler(handlerThreadThread.getLooper());
        settings = Settings.getInstance(context);

        goHome();

        handlerThread.post(runTests);
    }

    private void goHome() {
        Intent startMain = new Intent(Intent.ACTION_MAIN);
        startMain.addCategory(Intent.CATEGORY_HOME);
        startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(startMain);
    }

    private void runCommand(boolean shellRunner, String command) {
        if (shellRunner) {
            log("EXECUTE %s", command);
        } else {
            try {
                Process process = Runtime.getRuntime().exec(command);
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                char[] buf = new char[1024];
                while (bufferedReader.read(buf) == 1024) {
                }
            } catch (Exception e) {
                log("Exception: %s", e);
                e.printStackTrace();
            }
        }
    }

    private enum Mode { DISABLED, UNHOLEY, SWIRL };

    private final Runnable runTests = () -> {
        try {
            TestNotification.hide(context, TestNotification.NOTIFICATION_ID_MAIN);
            TestNotification.hide(context, TestNotification.NOTIFICATION_ID_TUNE);
            TestNotification.hide(context, TestNotification.NOTIFICATION_ID_COLOR);
            Permissions.unnotify(context);
            doSleep(1000);

            enforceScreen(true);
            int counter = 1;
            for (boolean startNotification : new boolean[] { true, false }) {
                for (boolean middleNotification : new boolean[] { true, false }) {
                    for (boolean endNotification : new boolean[] { true, false }) {
                        for (Mode onMode : new Mode[] { Mode.SWIRL }) {
                            for (Mode offMode : new Mode[] { Mode.SWIRL, Mode.UNHOLEY, Mode.DISABLED }) {
                                for (boolean aodHide : new boolean[] { true, false }) {
                                    for (boolean aodHelper : new boolean[] { true, false }) {
                                        setNotification(false);
                                        doSleep(1500);
                                        performTest(counter, onMode, offMode, aodHide, aodHelper, startNotification, middleNotification, endNotification);
                                        counter++;
                                    }
                                }
                            }
                        }
                    }
                }
            }
            log("TEST COMPLETE");
        } catch (Exception e) {
            Slog.e(TAG, "Exception: %s", e);
            e.printStackTrace();
        }
    };

    private void doSleep(int millis) {
        try {
            Thread.sleep(millis);
        } catch (Exception ignored) {
        }
    }

    private void pressPowerButton() {
        runCommand(true, "input keyevent 26");
    }

    private void setNotification(boolean on) {
        if (on == notificationState) return;
        log("Setting notification %s", on ? "ON" : "OFF");
        if (on) {
            TestNotification.show(context, TestNotification.NOTIFICATION_ID_MAIN);
        } else {
            TestNotification.hide(context, TestNotification.NOTIFICATION_ID_MAIN);
        }
        notificationState = on;
    }

    private void enforceScreen(boolean on) {
        log("Enforcing screen %s", on ? "ON" : "OFF");
        while (true) {
            boolean current = Display.isOn(context, false);
            if (current == on) {
                break;
            } else {
                pressPowerButton();
                doSleep(2500);
            }
        }
    }

    private boolean currentAODState() {
        return AODControl.isAODEnabled(context);
    }

    private boolean verifyOverlay(boolean on) {
        Boolean overlay = Overlay.test_lastVisible;
        boolean ret = (overlay != null) && (on == overlay);
        log("Verify Overlay == %s --> %s", on ? "ON" : "OFF", ret ? "OK" : "FAIL");
        return ret;
    }

    private boolean verifyDrawMode(SpritePlayer.Mode mode) {
        SpritePlayer.Mode drawMode = SpritePlayer.test_lastDrawMode;
        boolean ret = (drawMode != null) && (drawMode == mode);
        log("Verify DrawMode == %s --> %s [%s]", mode, ret ? "OK" : "FAIL", drawMode);
        return ret;
    }

    private boolean verifyHideAOD(boolean on) {
        Boolean hideAOD = NotificationAnimation.test_lastHideAOD;
        boolean ret = (hideAOD != null) && (on == hideAOD);
        log("Verify HideAOD == %s --> %s", on ? "ON" : "OFF", ret ? "OK" : "FAIL");
        return ret;
    }

    private boolean verifyAOD(boolean on) {
        boolean ret = on == currentAODState();
        log("Verify AOD == %s --> %s", on ? "ON" : "OFF", ret ? "OK" : "FAIL");
        return ret;
    }
    
    private boolean verifyColors(boolean on) {
        Integer colors = NotificationListenerService.test_lastColorCount;
        boolean haveColors = colors == null ? !on : colors > 0;
        boolean ret = on == haveColors;
        log("Verify Colors == %s --> %s", on ? "ON" : "OFF", ret ? "OK" : "FAIL");
        return ret;
    }

    private boolean validateState(boolean screen, Mode mode, boolean notification, boolean aodHide, boolean aodHelper) {
        boolean ret = true;
        if (mode == Mode.DISABLED) {
            if (screen) {
                ret &= verifyOverlay(false);
                ret &= verifyColors(notification);
            } else {
                ret &= verifyOverlay(false);
                ret &= verifyColors(notification);
            }
        } else if (mode == Mode.SWIRL) {
            if (screen) {
                ret &= verifyOverlay(notification);
                if (notification) {
                    ret &= verifyDrawMode(SpritePlayer.Mode.SWIRL);
                    ret &= verifyHideAOD(false);
                    ret &= verifyColors(true);
                }
            } else {
                if (notification) {
                    ret &= verifyOverlay(true);
                    ret &= verifyAOD(true);
                    ret &= verifyDrawMode(SpritePlayer.Mode.SWIRL);
                    ret &= verifyHideAOD(aodHide);
                    ret &= verifyColors(true);
                } else {
                    ret &= verifyOverlay(aodHide);
                    ret &= verifyAOD(!aodHelper || !aodHide);
                    if (aodHide) ret &= verifyDrawMode(SpritePlayer.Mode.TSP_HIDE);
                    if (aodHide) ret &= verifyHideAOD(aodHide);
                    ret &= verifyColors(false);
                }
            }
        } else if (mode == Mode.UNHOLEY) {
            if (screen) {
                return false;
            } else {
                if (notification) {
                    ret &= verifyOverlay(true);
                    ret &= verifyAOD(true);
                    ret &= verifyDrawMode(SpritePlayer.Mode.TSP);
                    ret &= verifyHideAOD(true);
                    ret &= verifyColors(true);
                } else {
                    ret &= verifyOverlay(true);
                    ret &= verifyAOD(!aodHelper);
                    if (aodHide) ret &= verifyDrawMode(SpritePlayer.Mode.TSP_HIDE);
                    ret &= verifyHideAOD(true);
                    ret &= verifyColors(false);
                }
            }
        }
        return ret;
    }

    private boolean performTest(int id, Mode onMode, Mode offMode, boolean aodHide, boolean aodHelper, boolean startNotification, boolean middleNotification, boolean endNotification) {
        settings.setAODHelperControl(aodHelper);
        runCommand(true, "settings put system aod_mode 1");
        doSleep(1000);

        settings.setEnabled(true);
        settings.setEnabledWhile(settings.getMode(true, true), onMode != Mode.DISABLED);
        settings.setAnimationMode(settings.getMode(true, true), onMode == Mode.UNHOLEY ? SpritePlayer.Mode.TSP : SpritePlayer.Mode.SWIRL);
        settings.setEnabledWhile(settings.getMode(true, false), offMode != Mode.DISABLED);
        settings.setAnimationMode(settings.getMode(true, false), offMode == Mode.UNHOLEY ? SpritePlayer.Mode.TSP : SpritePlayer.Mode.SWIRL);
        settings.setHideAOD(aodHide, true);

        log("[%3d] TEST RUN on:%s off:%s aodHide:%d aodHelper:%d notStart:%d notMiddle:%d notEnd:%d", id, onMode, offMode, aodHide ? 1 : 0, aodHelper ? 1 : 0, startNotification ? 1 : 0, middleNotification ? 1 : 0, endNotification ? 1 : 0);

        boolean ret = true;

        log("Section: Start");

        enforceScreen(true);
        doSleep(1500);
        setNotification(startNotification);
        doSleep(1500);
        ret &= validateState(true, onMode, startNotification, aodHide, aodHelper);

        log("Section: Middle #1");

        enforceScreen(false);
        doSleep(1500);
        ret &= validateState(false, offMode, startNotification, aodHide, aodHelper);

        log("Section: Middle #2");

        setNotification(middleNotification);
        doSleep(1500);
        ret &= validateState(false, offMode, middleNotification, aodHide, aodHelper);

        log("Section: End");

        enforceScreen(true);
        setNotification(endNotification);
        doSleep(1500);
        ret &= validateState(true, onMode, endNotification, aodHide, aodHelper);

        log ("[%3d] TEST RESULT --> %s", id, ret ? "OK" : "FAIL");

        return ret;
    }
}
