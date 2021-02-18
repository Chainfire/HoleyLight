package eu.chainfire.holeylight.misc;

import android.os.Build;

import java.util.Locale;

public class Manufacturer {
    public static boolean isSamsung() {
        return Build.BRAND.toLowerCase(Locale.ENGLISH).equals("samsung") || Build.MANUFACTURER.toLowerCase(Locale.ENGLISH).equals("samsung");
    }
    
    public static boolean isGoogle() {
        return Build.BRAND.toLowerCase(Locale.ENGLISH).equals("google") || Build.MANUFACTURER.toLowerCase(Locale.ENGLISH).equals("google");
    }
}
