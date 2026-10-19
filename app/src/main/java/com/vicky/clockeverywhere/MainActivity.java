package com.vicky.clockeverywhere;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.TimeZone;

public class MainActivity extends Activity {
    private static final int REQUEST_OVERLAY = 1001;
    private static final String KEY_ENABLED = "enabled";
    private static final String KEY_24_HOUR = "24_hour";
    private static final String KEY_SHOW_TZ = "show_timezone";
    private static final String KEY_SHOW_APP_USAGE = "show_app_usage";
    private static final String KEY_TIMEZONE = "timezone";

    private SharedPreferences prefs;
    private Switch enabledSwitch;
    private Switch formatSwitch;
    private Switch timezoneSwitch;
    private Switch appUsageSwitch;
    private Spinner timezoneSpinner;
    private boolean waitingForOverlayPermission;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = ClockPreferences.get(this);
        buildScreen();
    }

    @Override protected void onResume() {
        super.onResume();
        if (waitingForOverlayPermission && Settings.canDrawOverlays(this)) {
            waitingForOverlayPermission = false;
            if (enabledSwitch != null && enabledSwitch.isChecked()) startClockSafely();
        }
    }

    private void buildScreen() {
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(Color.rgb(16, 19, 22));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(48), dp(32), dp(48), dp(32));
        scroll.addView(root);

        TextView title = text("Clock Everywhere", 32, Color.WHITE);
        root.addView(title, fullWidth());

        TextView subtitle = text("A clock that stays visible while you use your TV.", 17, Color.LTGRAY);
        subtitle.setPadding(0, dp(6), 0, dp(28));
        root.addView(subtitle, fullWidth());

        enabledSwitch = settingSwitch("Enable App", prefs.getBoolean(KEY_ENABLED, false));
        root.addView(enabledSwitch, fullWidth());
        enabledSwitch.setOnCheckedChangeListener((button, checked) -> {
            prefs.edit().putBoolean(KEY_ENABLED, checked).apply();
            if (!checked) {
                waitingForOverlayPermission = false;
                stopClockSafely();
            } else if (!Settings.canDrawOverlays(this)) {
                waitingForOverlayPermission = true;
                openOverlaySettings();
            } else {
                startClockSafely();
            }
            updateSettingsEnabled();
        });

        Button usageReportButton = new Button(this);
        usageReportButton.setText("App Usage Report");
        usageReportButton.setTextSize(20);
        usageReportButton.setMinHeight(dp(64));
        usageReportButton.setOnClickListener(view ->
                startActivity(new Intent(this, UsageReportActivity.class)));
        LinearLayout.LayoutParams reportButtonParams = fullWidth();
        reportButtonParams.topMargin = dp(18);
        root.addView(usageReportButton, reportButtonParams);

        TextView divider = text("CLOCK SETTINGS", 13, Color.GRAY);
        divider.setPadding(0, dp(28), 0, dp(10));
        root.addView(divider, fullWidth());

        formatSwitch = settingSwitch("24 Hour Format", prefs.getBoolean(KEY_24_HOUR, true));
        root.addView(formatSwitch, fullWidth());
        formatSwitch.setOnCheckedChangeListener((b, checked) -> prefs.edit().putBoolean(KEY_24_HOUR, checked).apply());

        timezoneSwitch = settingSwitch("Show timezone abbreviation", prefs.getBoolean(KEY_SHOW_TZ, false));
        root.addView(timezoneSwitch, fullWidth());
        timezoneSwitch.setOnCheckedChangeListener((b, checked) -> prefs.edit().putBoolean(KEY_SHOW_TZ, checked).apply());

        appUsageSwitch = settingSwitch(
                "Show App Usage",
                prefs.getBoolean(KEY_SHOW_APP_USAGE, false));
        root.addView(appUsageSwitch, fullWidth());
        appUsageSwitch.setOnCheckedChangeListener(
                (button, checked) -> prefs.edit().putBoolean(KEY_SHOW_APP_USAGE, checked).apply());

        TextView zoneLabel = text("Timezone", 17, Color.WHITE);
        zoneLabel.setPadding(0, dp(18), 0, dp(8));
        root.addView(zoneLabel, fullWidth());

        timezoneSpinner = new Spinner(this);
        List<String> zones = new ArrayList<>(Arrays.asList(TimeZone.getAvailableIDs()));
        Collections.sort(zones);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, zones) {
            @Override public View getView(int position, View convertView, android.view.ViewGroup parent) {
                TextView v = (TextView) super.getView(position, convertView, parent);
                v.setTextColor(Color.WHITE);
                v.setTextSize(18);
                v.setPadding(dp(8), dp(8), dp(8), dp(8));
                return v;
            }
        };
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        timezoneSpinner.setAdapter(adapter);
        String savedZone = prefs.getString(KEY_TIMEZONE, "Asia/Kolkata");
        int index = zones.indexOf(savedZone);
        timezoneSpinner.setSelection(index >= 0 ? index : zones.indexOf("Asia/Kolkata"));
        timezoneSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                prefs.edit().putString(KEY_TIMEZONE, zones.get(position)).apply();
            }
            @Override public void onNothingSelected(android.widget.AdapterView<?> parent) { }
        });
        root.addView(timezoneSpinner, fullWidth());

        TextView note = text("The clock uses the selected timezone and updates every second.\nTimezone abbreviation is shown only when enabled. App usage requires Usage Access.", 14, Color.GRAY);
        note.setPadding(0, dp(12), 0, 0);
        root.addView(note, fullWidth());

        setContentView(scroll);
        updateSettingsEnabled();
        enabledSwitch.requestFocus();
    }

    private void openOverlaySettings() {
        Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
        try {
            // Xiaomi/Android TV firmware may not implement the package-specific
            // overlay settings Activity. Try the TV-safe generic screen first.
            startActivityForResult(intent, REQUEST_OVERLAY);
        } catch (Exception genericFailure) {
            try {
                Intent packageIntent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getPackageName()));
                startActivityForResult(packageIntent, REQUEST_OVERLAY);
            } catch (Exception packageFailure) {
                waitingForOverlayPermission = false;
                prefs.edit().putBoolean(KEY_ENABLED, false).apply();
                enabledSwitch.setChecked(false);
                Toast.makeText(this, "Open Display over other apps and allow Clock Everywhere.", Toast.LENGTH_LONG).show();
            }
        }
    }

    private Switch settingSwitch(String label, boolean checked) {
        Switch sw = new Switch(this);
        sw.setText(label);
        sw.setTextColor(Color.WHITE);
        sw.setTextSize(19);
        sw.setChecked(checked);
        sw.setGravity(Gravity.CENTER_VERTICAL);
        sw.setPadding(dp(8), dp(10), dp(8), dp(10));
        return sw;
    }

    private TextView text(String value, int size, int color) {
        TextView tv = new TextView(this);
        tv.setText(value);
        tv.setTextSize(size);
        tv.setTextColor(color);
        return tv;
    }

    private LinearLayout.LayoutParams fullWidth() {
        return new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
    }

    private int dp(int value) { return (int) (value * getResources().getDisplayMetrics().density + 0.5f); }

    private void updateSettingsEnabled() {
        boolean enabled = enabledSwitch != null && enabledSwitch.isChecked();
        if (formatSwitch != null) formatSwitch.setEnabled(enabled);
        if (timezoneSwitch != null) timezoneSwitch.setEnabled(enabled);
        if (appUsageSwitch != null) appUsageSwitch.setEnabled(enabled);
        if (timezoneSpinner != null) timezoneSpinner.setEnabled(enabled);
    }

    private void startClockSafely() {
        if (!Settings.canDrawOverlays(this)) return;
        try {
            startService(new Intent(this, ClockOverlayService.class));
        } catch (Exception e) {
            prefs.edit().putBoolean(KEY_ENABLED, false).apply();
            if (enabledSwitch != null && enabledSwitch.isChecked()) enabledSwitch.setChecked(false);
            Toast.makeText(this, "Clock could not start on this TV.", Toast.LENGTH_LONG).show();
        }
    }

    private void stopClockSafely() {
        try { stopService(new Intent(this, ClockOverlayService.class)); }
        catch (Exception ignored) { }
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_OVERLAY) {
            if (Settings.canDrawOverlays(this) && enabledSwitch != null && enabledSwitch.isChecked()) {
                waitingForOverlayPermission = false;
                startClockSafely();
            } else {
                waitingForOverlayPermission = false;
                prefs.edit().putBoolean(KEY_ENABLED, false).apply();
                if (enabledSwitch != null && enabledSwitch.isChecked()) enabledSwitch.setChecked(false);
                updateSettingsEnabled();
            }
        }
    }
}
