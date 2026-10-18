package com.vicky.clockeverywhere;

import android.app.Service;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.IBinder;
import android.provider.Settings;
import android.view.Gravity;
import android.view.WindowManager;
import android.widget.TextView;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ClockOverlayService extends Service {
    private final Handler handler = new Handler();
    private TextView clockView;
    private WindowManager windowManager;
    private final Runnable ticker = new Runnable() {
        @Override public void run() {
            if (clockView != null) {
                clockView.setText(new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date()));
            }
            handler.postDelayed(this, 1000);
        }
    };

    @Override public void onCreate() {
        super.onCreate();
        if (!Settings.canDrawOverlays(this)) { stopSelf(); return; }

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

        int type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                type,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                        | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                android.graphics.PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.TOP | Gravity.RIGHT;
        params.x = 24;
        params.y = 18;

        windowManager.addView(clockView, params);
        handler.post(ticker);
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override public void onDestroy() {
        handler.removeCallbacks(ticker);
        if (clockView != null && windowManager != null) {
            try { windowManager.removeView(clockView); } catch (Exception ignored) { }
        }
        clockView = null;
        super.onDestroy();
    }

    @Override public IBinder onBind(Intent intent) { return null; }
}
