package eu.chainfire.holeylight;

import android.content.Context;
import android.content.res.Configuration;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDelegate;
import eu.chainfire.holeylight.misc.LocaleHelper;
import eu.chainfire.holeylight.misc.Settings;

public class Application extends android.app.Application {
    @Override
    public void onCreate() {
        super.onCreate();

        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);

        Settings.getInstance(this); // init DEBUG variables
    }

    @Override
    protected void attachBaseContext(Context context) {
        super.attachBaseContext(LocaleHelper.getContext(context));
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        LocaleHelper.updateResources(this);
    }
}
