package com.vicky.clockeverywhere;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.TextView;

public class MainActivity extends Activity {
    private static final int REQUEST_OVERLAY = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        TextView status = new TextView(this);
        status.setText("Clock Everywhere\n\nGrant 'Display over other apps' permission to start the clock.");
        status.setTextSize(20);
        status.setPadding(48, 48, 48, 48);
        setContentView(status);

        if (!Settings.canDrawOverlays(this)) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, REQUEST_OVERLAY);
        } else {
            startService(new Intent(this, ClockOverlayService.class));
            finish();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (Settings.canDrawOverlays(this)) {
            startService(new Intent(this, ClockOverlayService.class));
            finish();
        }
    }
}
