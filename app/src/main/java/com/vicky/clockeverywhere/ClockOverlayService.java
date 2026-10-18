package com.vicky.clockeverywhere;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
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
    private static final String KEY_ENABLED = "enabled";
    private static final String KEY_24_HOUR = "24_hour";
    private static final String KEY_SHOW_TZ = "show_timezone";
    private static final String KEY_TIMEZONE = "timezone";
    private static final String CHANNEL_ID = "clock_everywhere_service";
    private static final int NOTIFICATION_ID = 1001;

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
        prefs = ClockPreferences.get(this);
        if (!prefs.getBoolean(KEY_ENABLED, false) || !Settings.canDrawOverlays(this)) {
            stopSelf();
            return;
        }

        createNotificationChannel();
        startForeground(NOTIFICATION_ID, buildNotification());

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
            viewAdded = false;
            clockView = null;
            stopForeground(true);
            prefs.edit().putBoolean(KEY_ENABLED, false).apply();
            stopSelf();
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Clock Everywhere",
                    NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("Keeps the Clock Everywhere overlay running");
            channel.setShowBadge(false);
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(channel);
        }
    }

    private Notification buildNotification() {
        Intent launchIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                0,
                launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT
                        | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0));

        Notification.Builder builder = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? new Notification.Builder(this, CHANNEL_ID)
                : new Notification.Builder(this);

        return builder
                .setContentTitle("Clock Everywhere")
                .setContentText("Clock overlay is running")
                .setSmallIcon(android.R.drawable.ic_menu_recent_history)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setShowWhen(false)
                .setCategory(Notification.CATEGORY_SERVICE)
                .build();
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        if (!prefs.getBoolean(KEY_ENABLED, false)) {
            stopForeground(true);
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
        stopForeground(true);
        viewAdded = false;
        clockView = null;
        super.onDestroy();
    }

    @Override public IBinder onBind(Intent intent) { return null; }
}
