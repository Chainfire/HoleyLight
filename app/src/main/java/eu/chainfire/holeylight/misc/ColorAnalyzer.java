package eu.chainfire.holeylight.misc;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;

import java.util.HashMap;
import java.util.Map;

import eu.chainfire.holeylight.BuildConfig;

public class ColorAnalyzer {
    private static final Map<String, Integer> colorMap = new HashMap<>();

    public static int analyze(Context context, String packageName, int defaultColor) {
        if ((defaultColor & 0x00FFFFFF) == 0) return defaultColor;
        int color = defaultColor;
        Integer cached = colorMap.get(packageName);
        if (cached == null) {
            boolean adjust = false;
            try {
                PackageManager pm = context.getPackageManager();
                ApplicationInfo info = pm.getApplicationInfo(packageName, 0);
                if (!packageName.equals(BuildConfig.APPLICATION_ID) && (info.flags & (ApplicationInfo.FLAG_SYSTEM | ApplicationInfo.FLAG_UPDATED_SYSTEM_APP)) == 0) {
                    // Average color from icon
                    adjust = true;
                    Drawable icon = info.loadIcon(pm);
                    Bitmap bitmap = Bitmap.createBitmap(icon.getIntrinsicWidth(), icon.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
                    try {
                        Canvas canvas = new Canvas(bitmap);
                        canvas.drawColor(0xFFFFFFFF);
                        icon.setBounds(0, 0, bitmap.getWidth(), bitmap.getHeight());
                        icon.draw(canvas);
                        long[] channels = new long[3];
                        int counter = 0;
                        int[] pixels = new int[bitmap.getWidth() * bitmap.getHeight()];
                        bitmap.getPixels(pixels, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());
                        for (int i = 0; i < pixels.length; i++) {
                            int c = pixels[i] & 0x00FFFFFF;
                            if (c > 0 && c < 0xFFFFFF) {
                                channels[0] += (c & 0xFF0000) >> 16;
                                channels[1] += (c & 0x00FF00) >> 8;
                                channels[2] += (c & 0x0000FF);
                                counter++;
                            }
                        }
                        if (counter > 0) {
                            color = (
                                    (int)(Math.max(Math.min(channels[0] / counter, 255), 0) << 16) |
                                    (int)(Math.max(Math.min(channels[1] / counter, 255), 0) << 8) |
                                    (int)(Math.max(Math.min(channels[2] / counter, 255), 0))
                            );
                        }
                    } finally {
                        bitmap.recycle();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            if (adjust) {
                // Maximize saturation and brightness
                float[] hsv = new float[3];
                Color.colorToHSV(color, hsv);
                hsv[1] = 1.0f;
                hsv[2] = 1.0f;
                color = Color.HSVToColor(255, hsv);
            }
        } else {
            color = cached;
        }
        return color;
    }
}
