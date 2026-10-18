package com.vicky.clockeverywhere;

import android.app.ActivityManager;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.provider.Settings;
import android.view.Gravity;
import android.view.WindowManager;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class ClockOverlayService extends Service {
    private static final String PREFS = "clock_everywhere";
    private static final String KEY_ENABLED = "enabled";
    private static final String KEY_SHOW_HOME = "show_home";
    private static final String KEY_24_HOUR = "24_hour";
    private static final String KEY_SHOW_TZ = "show_timezone";
    private static final String KEY_TIMEZONE = "timezone";

    private final Handler handler = new Handler(Looper.getMainLooper());
    private TextView clockView;
    private WindowManager windowManager;
    private SharedPreferences prefs;
    private boolean viewAdded;

    private final Runnable ticker = new Runnable() {
        @Override public void run() {
            if (clockView == null || !viewAdded) return;
            if (!prefs.getBoolean(KEY_ENABLED, false)) {
                stopSelf();
                return;
            }

            boolean showOnHome = prefs.getBoolean(KEY_SHOW_HOME, true);
            boolean onHome = isHomeScreen();
            clockView.setVisibility(!showOnHome && onHome ? TextView.GONE : TextView.VISIBLE);

            String pattern = prefs.getBoolean(KEY_24_HOUR, true) ? "HH:mm:ss" : "hh:mm:ss a";
            if (prefs.getBoolean(KEY_SHOW_TZ, false)) pattern += " z";

            SimpleDateFormat format = new SimpleDateFormat(pattern, Locale.getDefault());
            String zoneId = prefs.getString(KEY_TIMEZONE, "Asia/Kolkata");
            format.setTimeZone(TimeZone.getTimeZone(zoneId));
            clockView.setText(format.format(new Date()));
            handler.postDelayed(this, 1000);
        }
    };

    @Override public void onCreate() {
        super.onCreate();
        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        if (!prefs.getBoolean(KEY_ENABLED, false) || !Settings.canDrawOverlays(this)) {
            stopSelf();
            return;
        }

        try {
            windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
            clockView = new TextView(this);
            clockView.setTextColor(Color.WHITE);
            clockView.setTextSize(18);
            clockView.setTypeface(Typeface.create("sans", Typeface.NORMAL));
            clockView.setShadowLayer(4f, 2f, 2f, Color.BLACK);
            clockView.setPadding(8, 4, 8, 4);
            clockView.setClickable(false);
            clockView.setFocusable(false);
            clockView.setFocusableInTouchMode(false);

            WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                            | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                            | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                    android.graphics.PixelFormat.TRANSLUCENT);
            params.gravity = Gravity.TOP | Gravity.RIGHT;
            params.x = 24;
            params.y = 18;

            windowManager.addView(clockView, params);
            viewAdded = true;
            handler.post(ticker);
        } catch (Exception e) {
            // A TV firmware can report overlay permission as granted while its
            // window manager is still rejecting the overlay. Never crash the UI.
            viewAdded = false;
            clockView = null;
            prefs.edit().putBoolean(KEY_ENABLED, false).apply();
            stopSelf();
        }
    }

    private boolean isHomeScreen() {
        try {
            Intent homeIntent = new Intent(Intent.ACTION_MAIN);
            homeIntent.addCategory(Intent.CATEGORY_HOME);
            ComponentName home = homeIntent.resolveActivity(getPackageManager());
            if (home == null) return false;
            ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
            if (am == null) return false;
            java.util.List<ActivityManager.RunningTaskInfo> tasks = am.getRunningTasks(1);
            if (tasks == null || tasks.isEmpty() || tasks.get(0).topActivity == null) return false;
            return home.getPackageName().equals(tasks.get(0).topActivity.getPackageName());
        } catch (Exception ignored) {
            return false;
        }
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        if (!prefs.getBoolean(KEY_ENABLED, false)) {
            stopSelf();
            return START_NOT_STICKY;
        }
        return START_STICKY;
    }

    @Override public void onDestroy() {
        handler.removeCallbacks(ticker);
        if (clockView != null && windowManager != null && viewAdded) {
            try { windowManager.removeView(clockView); } catch (Exception ignored) { }
        }
        viewAdded = false;
        clockView = null;
        super.onDestroy();
    }

    @Override public IBinder onBind(Intent intent) { return null; }
}
