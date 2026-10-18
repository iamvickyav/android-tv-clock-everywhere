package com.vicky.clockeverywhere;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.UserManager;

final class ClockPreferences {
    private static final String PREFS = "clock_everywhere";

    private ClockPreferences() { }

    static SharedPreferences get(Context context) {
        Context deviceContext = context.createDeviceProtectedStorageContext();
        SharedPreferences devicePrefs = deviceContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        UserManager userManager = (UserManager) context.getSystemService(Context.USER_SERVICE);
        if (userManager != null && userManager.isUserUnlocked() && devicePrefs.getAll().isEmpty()) {
            SharedPreferences oldPrefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
            if (!oldPrefs.getAll().isEmpty()) {
                copyExistingSettings(oldPrefs, devicePrefs);
            }
        }
        return devicePrefs;
    }

    private static void copyExistingSettings(SharedPreferences source, SharedPreferences destination) {
        SharedPreferences.Editor editor = destination.edit();
        copyBoolean(source, editor, "enabled");
        copyBoolean(source, editor, "show_home");
        copyBoolean(source, editor, "24_hour");
        copyBoolean(source, editor, "show_timezone");
        String timezone = source.getString("timezone", null);
        if (timezone != null) editor.putString("timezone", timezone);
        editor.apply();
    }

    private static void copyBoolean(SharedPreferences source, SharedPreferences.Editor destination, String key) {
        if (source.contains(key)) destination.putBoolean(key, source.getBoolean(key, false));
    }
}
