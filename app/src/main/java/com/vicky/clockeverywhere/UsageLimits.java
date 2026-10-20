package com.vicky.clockeverywhere;

final class UsageLimits {
    static final String KEY_TV_LIMIT_MINUTES = "usage_limit_tv_minutes";
    static final String KEY_APP_LIMIT_MINUTES = "usage_limit_app_minutes";
    static final int DEFAULT_TV_LIMIT_MINUTES = 180;
    static final int DEFAULT_APP_LIMIT_MINUTES = 120;
    private static final float APPROACHING_RATIO = 0.80f;

    private UsageLimits() { }

    static int getTvLimitMinutes(android.content.SharedPreferences prefs) {
        return Math.max(1, prefs.getInt(KEY_TV_LIMIT_MINUTES, DEFAULT_TV_LIMIT_MINUTES));
    }

    static int getAppLimitMinutes(android.content.SharedPreferences prefs) {
        return Math.max(1, prefs.getInt(KEY_APP_LIMIT_MINUTES, DEFAULT_APP_LIMIT_MINUTES));
    }

    static String status(long usageMillis, int limitMinutes) {
        long limitMillis = Math.max(1L, limitMinutes) * 60_000L;
        if (usageMillis >= limitMillis) return "⚠️";
        if (usageMillis >= (long) (limitMillis * APPROACHING_RATIO)) return "⏳";
        return "✔";
    }

    static String status(long usageMillis, int limitMinutes, boolean unused) {
        return status(usageMillis, limitMinutes);
    }
}
