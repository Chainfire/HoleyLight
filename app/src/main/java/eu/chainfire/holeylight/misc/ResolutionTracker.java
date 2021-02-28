package eu.chainfire.holeylight.misc;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Point;
import android.util.DisplayMetrics;
import android.view.WindowManager;

import java.lang.reflect.Method;

public class ResolutionTracker {
    private final String TAG;
    private final WindowManager windowManager;
    private Point resolution;
    private int density;

    public ResolutionTracker(String tag, Context context) {
        this.TAG = tag;
        windowManager = (WindowManager)context.getSystemService(Context.WINDOW_SERVICE);
        resolution = getResolution();
        density = getDensity();
    }

    public Point getResolution() {
        DisplayMetrics metrics = new DisplayMetrics();
        windowManager.getDefaultDisplay().getRealMetrics(metrics);
        return new Point(metrics.widthPixels, metrics.heightPixels);
    }

    public int getDensity() {
        DisplayMetrics metrics = new DisplayMetrics();
        windowManager.getDefaultDisplay().getRealMetrics(metrics);
        return metrics.densityDpi;
    }

    public float getDensityMultiplier() {
        float ret = 1.0f;
        if (Manufacturer.isGoogle()) {
            // things work differently on Samsung
            try {
                @SuppressLint("PrivateApi") Class<?> c = Class.forName("android.os.SystemProperties");
                Method get = c.getMethod("get", String.class);
                String d = (String)get.invoke(null, "ro.sf.lcd_density");
                if (d != null) {
                    int i = Integer.parseInt(d);
                    ret = (float)i / (float)density;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return ret;
    }

    public boolean changed() {
        Point resolutionNow = getResolution();
        int densityNow = getDensity();
        float densityMult = getDensityMultiplier();
        if ((
                ((resolutionNow.x != resolution.x) || (resolutionNow.y != resolution.y)) &&
                ((resolutionNow.x != resolution.y) || (resolutionNow.y != resolution.x))
        ) || (
                densityNow != density
        )) {
            Slog.d(TAG + "/Resolution", "Resolution: %dx%d --> %dx%d, Density: %d --> %d [%.5f]", resolution.x, resolution.y, resolutionNow.x, resolutionNow.y, density, densityNow, densityMult);
            density = densityNow;
            resolution = resolutionNow;
            return true;
        }
        return false;
    }
}
