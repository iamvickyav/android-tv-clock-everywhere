package com.vicky.clockeverywhere;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;

public class BootReceiver extends BroadcastReceiver {
    private static final String TAG = "ClockEverywhereBoot";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent != null ? intent.getAction() : null;
        if (!Intent.ACTION_BOOT_COMPLETED.equals(action)
                && !Intent.ACTION_LOCKED_BOOT_COMPLETED.equals(action)) {
            return;
        }

        SharedPreferences prefs = context.getSharedPreferences("clock_everywhere", Context.MODE_PRIVATE);
        boolean enabled = prefs.getBoolean("enabled", true);
        boolean overlayAllowed = Settings.canDrawOverlays(context);

        Log.i(TAG, "Boot received: action=" + action
                + ", enabled=" + enabled
                + ", overlayAllowed=" + overlayAllowed);

        if (!enabled || !overlayAllowed) {
            Log.i(TAG, "Clock not started at boot: enabled=" + enabled
                    + ", overlayAllowed=" + overlayAllowed);
            return;
        }

        Intent serviceIntent = new Intent(context, ClockOverlayService.class);
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent);
                Log.i(TAG, "Requested foreground clock service at boot");
            } else {
                context.startService(serviceIntent);
                Log.i(TAG, "Requested clock service at boot");
            }
        } catch (Exception e) {
            Log.e(TAG, "Unable to start clock service at boot", e);
        }
    }
}
