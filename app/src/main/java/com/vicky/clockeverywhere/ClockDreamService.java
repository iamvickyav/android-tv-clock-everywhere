package com.vicky.clockeverywhere;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Looper;
import android.service.dreams.DreamService;
import android.view.View;
import android.view.WindowManager;

import java.util.TimeZone;

public class ClockDreamService extends DreamService implements SharedPreferences.OnSharedPreferenceChangeListener {
    private static final String KEY_24_HOUR = "24_hour";
    private static final String KEY_TIMEZONE = "timezone";

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final char[] timeDigits = new char[6];
    private final Runnable tick = new Runnable() {
        @Override public void run() {
            if (clockView == null || !dreamRunning) return;
            updateClock();
            scheduleNextTick();
        }
    };

    private SharedPreferences prefs;
    private FlipClockView clockView;
    private ClockTimeFormatter formatter;
    private Typeface digitalTypeface;
    private boolean use24Hour;
    private boolean dreamRunning;

    @Override public void onAttachedToWindow() {
        super.onAttachedToWindow();
        setInteractive(false);
        setFullscreen(true);

        prefs = ClockPreferences.get(this);
        prefs.registerOnSharedPreferenceChangeListener(this);
        loadTimePreferences();

        digitalTypeface = Typeface.createFromAsset(getAssets(), "fonts/DSEG7Classic-Bold.ttf");
        clockView = new FlipClockView(this, digitalTypeface);
        clockView.setBackgroundColor(Color.BLACK);
        clockView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        setContentView(clockView, new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT));
    }

    @Override public void onDreamingStarted() {
        super.onDreamingStarted();
        dreamRunning = true;
        updateClock();
        scheduleNextTick();
    }

    @Override public void onDreamingStopped() {
        dreamRunning = false;
        handler.removeCallbacks(tick);
        if (clockView != null) clockView.stopAnimations();
        super.onDreamingStopped();
    }

    @Override public void onDetachedFromWindow() {
        dreamRunning = false;
        handler.removeCallbacks(tick);
        if (prefs != null) prefs.unregisterOnSharedPreferenceChangeListener(this);
        if (clockView != null) clockView.stopAnimations();
        clockView = null;
        digitalTypeface = null;
        formatter = null;
        prefs = null;
        super.onDetachedFromWindow();
    }

    @Override public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (KEY_24_HOUR.equals(key) || KEY_TIMEZONE.equals(key)) {
            loadTimePreferences();
            updateClock();
        }
    }

    private void loadTimePreferences() {
        use24Hour = prefs.getBoolean(KEY_24_HOUR, true);
        String timezoneId = prefs.getString(KEY_TIMEZONE, "Asia/Kolkata");
        formatter = new ClockTimeFormatter(TimeZone.getTimeZone(timezoneId));
    }

    private void updateClock() {
        if (clockView == null || formatter == null) return;
        formatter.format(System.currentTimeMillis(), use24Hour, timeDigits);
        clockView.setTime(timeDigits, use24Hour ? '\0' : formatter.getMeridiem());
    }

    private void scheduleNextTick() {
        long now = System.currentTimeMillis();
        long delay = 1000L - now % 1000L;
        handler.postDelayed(tick, delay);
    }
}
