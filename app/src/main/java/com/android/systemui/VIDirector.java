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

package com.android.systemui;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.Rect;
import android.hardware.display.DisplayManager;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

import dalvik.system.DexClassLoader;
import eu.chainfire.holeylight.misc.Slog;

@SuppressWarnings({ "SameParameterValue", "unused" })
public class VIDirector {
    private static ClassLoader classLoader = null;

    private static ClassLoader loadAPK(final Context context, final String appName) throws PackageManager.NameNotFoundException {
        final String apkPath = context.getPackageManager().getApplicationInfo(appName, 0).sourceDir;
        final File tmpDir = context.getDir("tmp", 0);
        return new DexClassLoader(apkPath, tmpDir.getAbsolutePath(), null, context.getClassLoader());
    }

    private static ClassLoader systemUiClassLoader(Context context) {
        if (classLoader == null) {
            try {
                classLoader = loadAPK(context, "com.android.systemui");
            } catch (Exception e) {
                Slog.e("VIDirector", "Could not load classes :: %s", e);
                e.printStackTrace();
            }
        }
        return classLoader;
    }

    public static VIDirector create(Context context, Boolean flag) {
        try {
            @SuppressLint("PrivateApi") Class<?> clazz = Class.forName("com.android.keyguard.punchhole.VIDirectorFactory", true, systemUiClassLoader(context));

            try {
                // Kotlin-based code found in newer firmwares
                Field fCompanion = clazz.getDeclaredField("Companion");
                Object oCompanion = fCompanion.get(null);

                if (flag == null) {
                    Method mCreateVIDirector = oCompanion.getClass().getDeclaredMethod("createVIDirector", Context.class);
                    return new VIDirector(context, mCreateVIDirector.invoke(oCompanion, context));
                } else {
                    Method mCreateVIDirector = oCompanion.getClass().getDeclaredMethod("createVIDirector", Context.class, boolean.class);
                    return new VIDirector(context, mCreateVIDirector.invoke(oCompanion, context, flag));
                }
            } catch (NoSuchFieldException e) {
                // Java-based code found in older firmwares
                if (flag == null) {
                    Method mCreateVIDirector = clazz.getDeclaredMethod("createVIDirector", Context.class);
                    return new VIDirector(context, mCreateVIDirector.invoke(null, context));
                } else {
                    Method mCreateVIDirector = clazz.getDeclaredMethod("createVIDirector", Context.class, boolean.class);
                    return new VIDirector(context, mCreateVIDirector.invoke(null, context, flag));
                }
            }
        } catch (Exception e) {
            Slog.e("VIDirector", "Could not create instance :: %s", e);
            e.printStackTrace();
        }
        return null;
    }

    private final Context context;
    private final Object reflected;

    public VIDirector(Context context, Object reflected) {
        this.context = context;
        this.reflected = reflected;
    }

    private Object call(String method, Class<?>[] parameterTypes, Object[] args, Object ifNull, Object onError) {
        try {
            Method m = this.reflected.getClass().getDeclaredMethod(method, parameterTypes);
            m.setAccessible(true);
            Object ret = m.invoke(this.reflected, args);
            if (ret == null) ret = ifNull;
            return ret;
        } catch (NoSuchMethodException e) {
            Slog.e("VIDirector", "NoSuchMethod: %s :: %s", method, e);
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            Slog.e("VIDirector", "IllegalAccessException: %s :: %s", method, e);
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            Slog.e("VIDirector", "InvocationTargetException: %s :: %s", method, e);
            e.printStackTrace();
        }
        return onError;
    }

    public int getScreenWidth() {
        // reflected method crashes, due to disallowed access to mContext.getResources().getConfiguration().windowConfiguration field, this should return the same
        Point ret = new Point(0, 0);
        ((DisplayManager)context.getSystemService(Context.DISPLAY_SERVICE)).getDisplay(0).getRealSize(ret);
        return ret.x;
    }

    public int getScreenHeight() {
        // reflected method crashes, due to disallowed access to mContext.getResources().getConfiguration().windowConfiguration field, this should return the same
        Point ret = new Point(0, 0);
        ((DisplayManager)context.getSystemService(Context.DISPLAY_SERVICE)).getDisplay(0).getRealSize(ret);
        return ret.y;
    }
    
    public int getScreenRotation() {
        // reflected method crashes, due to disallowed access to mContext.getResources().getConfiguration().windowConfiguration field, this should return the same
        return ((DisplayManager)context.getSystemService(Context.DISPLAY_SERVICE)).getDisplay(0).getRotation();
    }
    
    @SuppressWarnings("UnnecessaryLocalVariable")
    public Rect getVIViewLocation(/* int i, boolean flag */) {
        // reflected method crashes, due to disallowed access to mContext.getResources().getConfiguration().windowConfiguration field, adaption from decompile
        Rect rect = new Rect();
        PointF locationRatio = getCameraLocationRatio();
        if (locationRatio == null) return rect;
        PointF sizeRatio = getFaceRecognitionVISizeRatio();
        if (sizeRatio == null) return rect;
        int i = getScreenWidth();
        int j = getScreenHeight();
        int l = getScreenRotation();
        if (l != 1) {
            if (l != 3) {
                float f = i;
                float f3 = locationRatio.x;
                float f6 = sizeRatio.x;
                int i1 = (int) ((f3 - f6 * 0.5F) * f);
                rect.left = i1;
                float f9 = j;
                f3 = locationRatio.y;
                float f12 = sizeRatio.y;
                j = (int) ((f3 - 0.5F * f12) * f9);
                rect.top = j;
                rect.right = (int) ((float) i1 + f * f6);
                rect.bottom = (int) ((float) j + f9 * f12);
            } else {
                float f1 = i;
                float f13 = locationRatio.y;
                float f7 = j;
                float f4 = sizeRatio.x;
                j = (int) ((1.0F - f13) * f1 - f7 * f4 * 0.5F);
                rect.left = j;
                f13 = locationRatio.x;
                float f10 = sizeRatio.y;
                int j1 = (int) (f13 * f7 - f1 * f10 * 0.5F);
                rect.top = j1;
                rect.right = (int) ((float) j + f7 * f4);
                rect.bottom = (int) ((float) j1 + f1 * f10);
            }
        } else {
            float f5 = i;
            float f14 = locationRatio.y;
            float f2 = j;
            float f8 = sizeRatio.x;
            int k1 = (int) (f14 * f5 - f2 * f8 * 0.5F);
            rect.left = k1;
            f14 = locationRatio.x;
            float f11 = sizeRatio.y;
            j = (int) ((1.0F - f14) * f2 - f5 * f11 * 0.5F);
            rect.top = j;
            rect.right = (int) ((float) k1 + f2 * f8);
            rect.bottom = (int) ((float) j + f5 * f11);
        }
        return rect;
    }

    public int getVIViewRotation() {
        // reflected method crashes, due to disallowed access to mContext.getResources().getConfiguration().windowConfiguration field, copy/paste from decompile
         int i = getScreenRotation();
         if (i != 1)
             return i == 3 ? 90 : 0;
         else
             return 270;
    }

    public PointF getCameraLocationRatio() {
        return (PointF)call("getCameraLocationRatio", null, null, null, null);
    }

    public PointF getFaceRecognitionVISizeRatio() {
        return (PointF)call("getFaceRecognitionVISizeRatio", null, null, null, null);
    }

    public String getFaceRecognitionVIFileName() {
        return (String)call("getFaceRecognitionVIFileName", null, null, null, null);
    }

    public String getFaceRecognitionJson() {
        String filename = getFaceRecognitionVIFileName();
        if (filename == null) return null;
        try {
            InputStream is = context.getPackageManager().getResourcesForApplication("com.android.systemui").getAssets().open(filename);
            return (new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))).lines().collect(Collectors.joining("\n"));
        } catch (Exception e) {
            Slog.e("VIDirector", "Failed to load json: %s :: %s", filename, e);
            e.printStackTrace();
        }
        return null;
    }
}
