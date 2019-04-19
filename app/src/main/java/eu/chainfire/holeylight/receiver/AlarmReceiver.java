package eu.chainfire.holeylight.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import eu.chainfire.holeylight.animation.Overlay;

public class AlarmReceiver extends BroadcastReceiver {
    public static final String ACTION = "eu.chainfire.holeylight.ALARM";

    @Override
    public void onReceive(Context context, Intent intent) {
        try {
            Overlay.getInstance(context).evaluate(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
