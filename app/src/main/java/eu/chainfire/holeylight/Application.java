package eu.chainfire.holeylight;

import android.content.Context;
import android.content.res.Configuration;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDelegate;
import eu.chainfire.holeylight.misc.LocaleHelper;

public class Application extends android.app.Application {
    @Override
    public void onCreate() {
        super.onCreate();

        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
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
