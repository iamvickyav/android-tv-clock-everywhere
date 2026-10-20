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
import android.os.SystemClock;
import android.provider.Settings;
import android.view.Gravity;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ClockOverlayService extends Service {
    private static final String KEY_ENABLED = "enabled";
    private static final String KEY_24_HOUR = "24_hour";
    private static final String KEY_SHOW_TZ = "show_timezone";
    private static final String KEY_SHOW_APP_USAGE = "show_app_usage";
    private static final String KEY_TIMEZONE = "timezone";
    private static final String CHANNEL_ID = "clock_everywhere_service";
    private static final int NOTIFICATION_ID = 1001;
    private static final long USAGE_REFRESH_INTERVAL_MILLIS = 8_000L;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final ExecutorService usageExecutor = Executors.newSingleThreadExecutor();
    private LinearLayout overlayView;
    private TextView usageView;
    private TextView clockView;
    private WindowManager windowManager;
    private SharedPreferences prefs;
    private UsageRepository usageRepository;
    private SimpleDateFormat clockFormat;
    private String clockPattern;
    private String clockTimeZone;
    private String usageTimeZoneId;
    private String displayedUsagePackage;
    private long displayedUsageMillis;
    private long displayedUsageElapsedRealtime;
    private long displayedTotalTvMillis;
    private long displayedTotalTvSampleElapsedRealtime;
    private boolean viewAdded;
    private boolean usageRefreshPending;
    private volatile boolean destroyed;

    private final Runnable ticker = new Runnable() {
        @Override public void run() {
            if (clockView == null || !viewAdded) return;
            if (!prefs.getBoolean(KEY_ENABLED, false)) {
                stopSelf();
                return;
            }

            String pattern = prefs.getBoolean(KEY_24_HOUR, true) ? "HH:mm:ss" : "hh:mm:ss a";
            if (prefs.getBoolean(KEY_SHOW_TZ, false)) pattern += " z";
            String zoneId = prefs.getString(KEY_TIMEZONE, "Asia/Kolkata");
            if (clockFormat == null || !pattern.equals(clockPattern)) {
                clockFormat = new SimpleDateFormat(pattern, Locale.getDefault());
                clockPattern = pattern;
                clockTimeZone = null;
            }
            if (!zoneId.equals(clockTimeZone)) {
                clockFormat.setTimeZone(TimeZone.getTimeZone(zoneId));
                clockTimeZone = zoneId;
            }
            String time = clockFormat.format(new Date());
            if (!time.equals(clockView.getText().toString())) clockView.setText(time);
            handler.postDelayed(this, 1000);
        }
    };

    private final Runnable usageTicker = new Runnable() {
        @Override public void run() {
            if (!viewAdded || usageView == null) return;
            if (prefs.getBoolean(KEY_SHOW_APP_USAGE, false)) refreshForegroundUsage();
            else hideUsage();
            handler.postDelayed(this, USAGE_REFRESH_INTERVAL_MILLIS);
        }
    };

    private final SharedPreferences.OnSharedPreferenceChangeListener preferenceListener =
            (sharedPreferences, key) -> {
                if (usageView == null) return;
                if (KEY_SHOW_APP_USAGE.equals(key)
                        || UsageLimits.KEY_TV_LIMIT_MINUTES.equals(key)
                        || UsageLimits.KEY_APP_LIMIT_MINUTES.equals(key)) {
                    if (prefs.getBoolean(KEY_SHOW_APP_USAGE, false)) refreshForegroundUsage();
                    else hideUsage();
                }
            };

    @Override public void onCreate() {
        super.onCreate();
        prefs = ClockPreferences.get(this);
        if (!prefs.getBoolean(KEY_ENABLED, false) || !Settings.canDrawOverlays(this)) {
            stopSelf();
            return;
        }
        prefs.registerOnSharedPreferenceChangeListener(preferenceListener);

        createNotificationChannel();
        startForeground(NOTIFICATION_ID, buildNotification());

        try {
            windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
            overlayView = new LinearLayout(this);
            overlayView.setOrientation(LinearLayout.HORIZONTAL);
            overlayView.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
            overlayView.setClickable(false);
            overlayView.setFocusable(false);
            overlayView.setFocusableInTouchMode(false);

            usageView = new TextView(this);
            usageView.setTextColor(Color.WHITE);
            usageView.setTextSize(18);
            usageView.setTypeface(Typeface.create("sans", Typeface.NORMAL));
            usageView.setShadowLayer(4f, 2f, 2f, Color.BLACK);
            usageView.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
            usageView.setSingleLine(true);
            usageView.setMaxWidth(dp(700));
            usageView.setEllipsize(android.text.TextUtils.TruncateAt.END);
            usageView.setVisibility(android.view.View.GONE);
            usageView.setClickable(false);
            usageView.setFocusable(false);
            usageView.setFocusableInTouchMode(false);
            LinearLayout.LayoutParams usageParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            usageParams.setMargins(0, 0, 0, 0);
            overlayView.addView(usageView, usageParams);

            clockView = new TextView(this);
            clockView.setTextColor(Color.WHITE);
            clockView.setTextSize(18);
            clockView.setTypeface(Typeface.create("sans", Typeface.NORMAL));
            clockView.setShadowLayer(4f, 2f, 2f, Color.BLACK);
            clockView.setPadding(8, 4, 8, 4);
            clockView.setClickable(false);
            clockView.setFocusable(false);
            clockView.setFocusableInTouchMode(false);
            overlayView.addView(clockView);

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

            windowManager.addView(overlayView, params);
            viewAdded = true;
            handler.post(ticker);
            handler.post(usageTicker);
        } catch (Exception e) {
            viewAdded = false;
            overlayView = null;
            usageView = null;
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

    private void refreshForegroundUsage() {
        if (usageRefreshPending || !viewAdded || usageView == null
                || !prefs.getBoolean(KEY_SHOW_APP_USAGE, false)) return;
        usageRefreshPending = true;
        usageExecutor.execute(() -> {
            UsageRepository.ForegroundUsage usage;
            UsageRepository.DayUsage today;
            try {
                String currentTimeZoneId = TimeZone.getDefault().getID();
                if (usageRepository == null || !currentTimeZoneId.equals(usageTimeZoneId)) {
                    usageRepository = new UsageRepository(getApplicationContext());
                    usageTimeZoneId = currentTimeZoneId;
                }
                usage = usageRepository.loadForegroundUsageToday();
                today = usageRepository.loadToday();
            } catch (RuntimeException e) {
                android.util.Log.w("ClockOverlayService", "Could not refresh foreground app usage.", e);
                usage = null;
                today = null;
            }
            UsageRepository.ForegroundUsage result = usage;
            UsageRepository.DayUsage todayResult = today;
            if (!destroyed) {
                handler.post(() -> {
                    usageRefreshPending = false;
                    if (!viewAdded || usageView == null
                            || !prefs.getBoolean(KEY_SHOW_APP_USAGE, false)) return;
                    if (result == null || todayResult == null) {
                        hideUsage();
                        return;
                    }

                    long elapsedRealtime = SystemClock.elapsedRealtime();
                    if (result.packageName.equals(displayedUsagePackage)
                            && result.todayMillis >= displayedUsageMillis) {
                        long elapsedSinceSample = Math.max(
                                0L,
                                elapsedRealtime - displayedUsageElapsedRealtime);
                        displayedUsageMillis = Math.max(
                                result.todayMillis,
                                displayedUsageMillis + elapsedSinceSample);
                    } else {
                        displayedUsagePackage = result.packageName;
                        displayedUsageMillis = result.todayMillis;
                    }
                    displayedUsageElapsedRealtime = elapsedRealtime;

                    if (todayResult.totalForegroundMillis >= displayedTotalTvMillis) {
                        long elapsedSinceTotalSample = Math.max(
                                0L,
                                elapsedRealtime - displayedTotalTvSampleElapsedRealtime);
                        displayedTotalTvMillis = Math.max(
                                todayResult.totalForegroundMillis,
                                displayedTotalTvMillis + (displayedTotalTvSampleElapsedRealtime == 0L
                                        ? 0L : elapsedSinceTotalSample));
                    } else {
                        displayedTotalTvMillis = todayResult.totalForegroundMillis;
                    }
                    displayedTotalTvSampleElapsedRealtime = elapsedRealtime;

                    int appLimit = UsageLimits.getAppLimitMinutes(prefs);
                    int tvLimit = UsageLimits.getTvLimitMinutes(prefs);
                    String appStatus = UsageLimits.status(displayedUsageMillis, appLimit);
                    String tvStatus = UsageLimits.status(displayedTotalTvMillis, tvLimit);
                    String display = result.label + " Usage: "
                            + UsageReportMath.formatUsageDuration(displayedUsageMillis)
                            + " " + appStatus + " | "
                            + clockTextNow() + " " + tvStatus;
                    if (!display.equals(usageView.getText().toString())) usageView.setText(display);
                    usageView.setVisibility(android.view.View.VISIBLE);
                });
            }
        });
    }

    private String clockTextNow() {
        String pattern = prefs.getBoolean(KEY_24_HOUR, true) ? "HH:mm:ss" : "hh:mm:ss a";
        if (prefs.getBoolean(KEY_SHOW_TZ, false)) pattern += " z";
        String zoneId = prefs.getString(KEY_TIMEZONE, "Asia/Kolkata");
        SimpleDateFormat format = new SimpleDateFormat(pattern, Locale.getDefault());
        format.setTimeZone(TimeZone.getTimeZone(zoneId));
        return format.format(new Date());
    }

    private void hideUsage() {
        displayedUsagePackage = null;
        displayedUsageMillis = 0L;
        displayedTotalTvMillis = 0L;
        displayedTotalTvSampleElapsedRealtime = 0L;
        if (usageView != null && usageView.getVisibility() != android.view.View.GONE) {
            usageView.setVisibility(android.view.View.GONE);
        }
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
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
        destroyed = true;
        handler.removeCallbacks(ticker);
        handler.removeCallbacks(usageTicker);
        prefs.unregisterOnSharedPreferenceChangeListener(preferenceListener);
        usageExecutor.shutdownNow();
        if (overlayView != null && windowManager != null && viewAdded) {
            try { windowManager.removeView(overlayView); } catch (Exception ignored) { }
        }
        stopForeground(true);
        viewAdded = false;
        overlayView = null;
        usageView = null;
        clockView = null;
        super.onDestroy();
    }

    @Override public IBinder onBind(Intent intent) { return null; }
}
