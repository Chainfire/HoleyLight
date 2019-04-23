package eu.chainfire.holeylight.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import eu.chainfire.holeylight.service.NotificationListenerService;

public class AlarmReceiver extends BroadcastReceiver {
    public static final String ACTION = "eu.chainfire.holeylight.ALARM";

    @Override
    public void onReceive(Context context, Intent intent) {
        try {
            NotificationListenerService.checkNotifications();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
