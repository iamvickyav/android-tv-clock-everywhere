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
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class MainActivity extends Activity {
    private static final int REQUEST_OVERLAY = 1001;
    private static final String PREFS = "clock_everywhere";
    private static final String KEY_ENABLED = "enabled";
    private static final String KEY_SHOW_HOME = "show_home";
    private static final String KEY_24_HOUR = "24_hour";
    private static final String KEY_SHOW_TZ = "show_timezone";
    private static final String KEY_TIMEZONE = "timezone";

    private SharedPreferences prefs;
    private Switch enabledSwitch;
    private Switch homeSwitch;
    private Switch formatSwitch;
    private Switch timezoneSwitch;
    private Spinner timezoneSpinner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        buildScreen();
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

        enabledSwitch = settingSwitch("Enable App", prefs.getBoolean(KEY_ENABLED, true));
        root.addView(enabledSwitch, fullWidth());
        enabledSwitch.setOnCheckedChangeListener((button, checked) -> {
            prefs.edit().putBoolean(KEY_ENABLED, checked).apply();
            if (checked) {
                if (!Settings.canDrawOverlays(this)) {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:" + getPackageName()));
                    startActivityForResult(intent, REQUEST_OVERLAY);
                } else {
                    startClock();
                }
            } else {
                stopService(new Intent(this, ClockOverlayService.class));
            }
            updateSettingsEnabled();
        });

        TextView divider = text("CLOCK SETTINGS", 13, Color.GRAY);
        divider.setPadding(0, dp(28), 0, dp(10));
        root.addView(divider, fullWidth());

        homeSwitch = settingSwitch("Show on Home screen", prefs.getBoolean(KEY_SHOW_HOME, true));
        root.addView(homeSwitch, fullWidth());
        homeSwitch.setOnCheckedChangeListener((b, checked) -> prefs.edit().putBoolean(KEY_SHOW_HOME, checked).apply());

        formatSwitch = settingSwitch("24 Hour Format", prefs.getBoolean(KEY_24_HOUR, true));
        root.addView(formatSwitch, fullWidth());
        formatSwitch.setOnCheckedChangeListener((b, checked) -> prefs.edit().putBoolean(KEY_24_HOUR, checked).apply());

        timezoneSwitch = settingSwitch("Show timezone abbreviation", prefs.getBoolean(KEY_SHOW_TZ, false));
        root.addView(timezoneSwitch, fullWidth());
        timezoneSwitch.setOnCheckedChangeListener((b, checked) -> prefs.edit().putBoolean(KEY_SHOW_TZ, checked).apply());

        TextView zoneLabel = text("Timezone", 17, Color.WHITE);
        zoneLabel.setPadding(0, dp(18), 0, dp(8));
        root.addView(zoneLabel, fullWidth());

        timezoneSpinner = new Spinner(this);
        List<String> zones = new ArrayList<>(Arrays.asList(TimeZone.getAvailableIDs()));
        Collections.sort(zones);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, zones) {
            @Override public View getView(int position, android.view.View convertView, android.view.ViewGroup parent) {
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

        TextView note = text("The clock uses the selected timezone and updates every second.\nTimezone abbreviation is shown only when enabled.", 14, Color.GRAY);
        note.setPadding(0, dp(12), 0, 0);
        root.addView(note, fullWidth());

        setContentView(scroll);
        updateSettingsEnabled();
        enabledSwitch.requestFocus();
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
        if (homeSwitch != null) homeSwitch.setEnabled(enabled);
        if (formatSwitch != null) formatSwitch.setEnabled(enabled);
        if (timezoneSwitch != null) timezoneSwitch.setEnabled(enabled);
        if (timezoneSpinner != null) timezoneSpinner.setEnabled(enabled);
    }

    private void startClock() {
        if (Settings.canDrawOverlays(this)) {
            startService(new Intent(this, ClockOverlayService.class));
        }
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_OVERLAY) {
            if (Settings.canDrawOverlays(this) && enabledSwitch.isChecked()) startClock();
            else if (!Settings.canDrawOverlays(this)) enabledSwitch.setChecked(false);
        }
    }
}
