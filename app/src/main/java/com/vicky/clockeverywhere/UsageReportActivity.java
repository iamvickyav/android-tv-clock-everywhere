package com.vicky.clockeverywhere;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.DateFormat;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class UsageReportActivity extends Activity {
    private static final String TAG = "UsageReportActivity";
    private static final int TAB_TODAY = 0;
    private static final int TAB_WEEK = 1;
    private static final int TAB_APPS = 2;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private LinearLayout content;
    private UsageRepository.PeriodUsage periodUsage;
    private UsageRepository.DayUsage todayUsage;
    private UsageRepository.AppDetails selectedAppDetails;
    private UsageRepository repository;
    private String selectedPackage;
    private int selectedTab = TAB_TODAY;
    private UsageReportMath.Period selectedPeriod = UsageReportMath.Period.CURRENT_WEEK;
    private int generation;
    private boolean destroyed;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        renderLoading("Checking Usage Access...");
    }

    @Override protected void onResume() {
        super.onResume();
        refreshReport();
    }

    @Override protected void onDestroy() {
        destroyed = true;
        generation++;
        worker.shutdownNow();
        mainHandler.removeCallbacksAndMessages(null);
        repository = null;
        super.onDestroy();
    }

    @Override public void onBackPressed() {
        if (selectedPackage != null) {
            selectedPackage = null;
            selectedAppDetails = null;
            renderReport();
            return;
        }
        super.onBackPressed();
    }

    private void refreshReport() {
        if (!UsageRepository.hasUsageAccess(this)) {
            periodUsage = null;
            todayUsage = null;
            selectedPackage = null;
            renderAccessRequired();
            return;
        }

        int request = ++generation;
        UsageReportMath.Period period = selectedPeriod;
        renderLoading("Loading usage report...");
        worker.execute(() -> {
            try {
                UsageRepository repositoryForResult = new UsageRepository(getApplicationContext());
                UsageRepository.PeriodUsage report = repositoryForResult.loadPeriod(period);
                UsageRepository.DayUsage today = repositoryForResult.loadToday();
                mainHandler.post(() -> {
                    if (destroyed || request != generation) return;
                    repository = repositoryForResult;
                    periodUsage = report;
                    todayUsage = today;
                    renderReport();
                });
            } catch (RuntimeException e) {
                Log.e(TAG, "Could not load Android usage statistics.", e);
                mainHandler.post(() -> {
                    if (destroyed || request != generation) return;
                    renderReportError();
                });
            }
        });
    }

    private void openUsageAccessSettings() {
        try {
            startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS));
        } catch (ActivityNotFoundException | SecurityException e) {
            Log.w(TAG, "Usage Access settings are unavailable on this TV.", e);
            Toast.makeText(this,
                    "Open Settings > Apps > Special app access > Usage access.",
                    Toast.LENGTH_LONG).show();
        }
    }

    private void renderAccessRequired() {
        beginPage("App Usage Report", "Usage Access is required to read Android's app usage history.");
        addText("The report reads usage data only while open. The optional Show App Usage overlay reads the foreground app only when enabled.",
                22, Color.LTGRAY, dp(24));
        addButton("Open Usage Access Settings", this::openUsageAccessSettings);
        addText("On the settings page, allow Usage Access for Clock Everywhere, then return here.",
                18, Color.GRAY, dp(18));
        addButton("Back", this::finish);
    }

    private void renderLoading(String message) {
        beginPage("App Usage Report", message);
        addText(message, 24, Color.WHITE, dp(20));
        if (UsageRepository.hasUsageAccess(this)) addButton("Refresh", this::refreshReport);
        else addButton("Open Usage Access Settings", this::openUsageAccessSettings);
        addButton("Back", this::finish);
    }

    private void renderReportError() {
        beginPage("App Usage Report", "Android could not provide usage data.");
        addText("Check Usage Access in system settings, then retry. Some TV firmware may restrict usage reporting.",
                20, Color.LTGRAY, dp(20));
        addText("Check Usage Access in system settings and try again.", 18, Color.GRAY, dp(16));
        addButton("Open Usage Access Settings", this::openUsageAccessSettings);
        addButton("Retry", this::refreshReport);
        if (selectedPackage != null) {
            addButton("Back to App Breakdown", () -> {
                selectedPackage = null;
                selectedAppDetails = null;
                renderReport();
            });
        }
        addButton("Back", this::finish);
    }

    private void renderReport() {
        if (periodUsage == null || todayUsage == null) {
            renderReportError();
            return;
        }
        String subtitle = selectedPackage == null
                ? "Usage reported by Android for " + DateFormat.getDateInstance(DateFormat.LONG).format(new Date())
                : "App details";
        beginPage("App Usage Report", subtitle);
        addButton("Back", this::finish);
        if (selectedPackage != null) {
            renderAppDetails();
            return;
        }

        addButton("Refresh Usage Report", this::refreshReport);
        addTabs();
        if (selectedTab == TAB_TODAY) renderToday();
        else {
            addPeriodSelector();
            if (selectedTab == TAB_WEEK) renderPeriod();
            else renderApps();
        }
    }

    private void addTabs() {
        LinearLayout tabs = new LinearLayout(this);
        tabs.setOrientation(LinearLayout.HORIZONTAL);
        addTab(tabs, "Today", TAB_TODAY);
        addTab(tabs, "Period Summary", TAB_WEEK);
        addTab(tabs, "App Breakdown", TAB_APPS);
        LinearLayout.LayoutParams params = fullWidth();
        params.topMargin = dp(18);
        content.addView(tabs, params);
    }

    private void addTab(LinearLayout tabs, String label, int tab) {
        Button button = new Button(this);
        button.setText(label);
        button.setTextSize(18);
        button.setMinHeight(dp(58));
        button.setSelected(selectedTab == tab);
        if (selectedTab == tab) button.setTextColor(Color.WHITE);
        button.setOnClickListener(view -> {
            selectedTab = tab;
            selectedPackage = null;
            renderReport();
        });
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        params.setMargins(dp(4), 0, dp(4), 0);
        tabs.addView(button, params);
    }

    private void addPeriodSelector() {
        LinearLayout periods = new LinearLayout(this);
        periods.setOrientation(LinearLayout.HORIZONTAL);
        addPeriodButton(periods, "Current Week", UsageReportMath.Period.CURRENT_WEEK);
        addPeriodButton(periods, "Previous Week", UsageReportMath.Period.PREVIOUS_WEEK);
        addPeriodButton(periods, "Current Month", UsageReportMath.Period.CURRENT_MONTH);
        addPeriodButton(periods, "Previous Month", UsageReportMath.Period.PREVIOUS_MONTH);
        LinearLayout.LayoutParams params = fullWidth();
        params.topMargin = dp(12);
        content.addView(periods, params);
    }

    private void addPeriodButton(
            LinearLayout periods,
            String label,
            UsageReportMath.Period period) {
        Button button = new Button(this);
        button.setText(label);
        button.setTextSize(15);
        button.setMinHeight(dp(54));
        button.setSelected(selectedPeriod == period);
        if (selectedPeriod == period) button.setTextColor(Color.WHITE);
        button.setOnClickListener(view -> {
            if (selectedPeriod == period) return;
            selectedPeriod = period;
            selectedPackage = null;
            selectedAppDetails = null;
            refreshReport();
        });
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f);
        params.setMargins(dp(2), 0, dp(2), 0);
        periods.addView(button, params);
    }

    private void renderToday() {
        UsageRepository.DayUsage today = todayUsage;
        addText("TOTAL TV TIME · APP FOREGROUND", 17, Color.LTGRAY, dp(14));
        addText(UsageReportMath.formatDuration(today.totalForegroundMillis), 38, Color.WHITE, dp(20));
        if (today.apps.isEmpty()) {
            addText("No app usage was reported for today yet.", 22, Color.LTGRAY, dp(20));
            return;
        }
        addText("Most used apps", 22, Color.WHITE, dp(22));
        int count = Math.min(3, today.apps.size());
        for (int index = 0; index < count; index++) addAppRow(today.apps.get(index));
    }

    private void renderPeriod() {
        UsageReportMath.PeriodSummary summary = periodUsage.summary;
        addText(periodLabel(selectedPeriod).toUpperCase(java.util.Locale.getDefault()), 22, Color.WHITE, dp(18));
        for (UsageRepository.DayUsage day : periodUsage.days) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            String date = DateFormat.getDateInstance(DateFormat.SHORT).format(new Date(day.startMillis));
            TextView weekday = text(day.weekday + " " + date, 21, Color.WHITE);
            row.addView(weekday, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
            TextView duration = text(UsageReportMath.formatDuration(day.totalForegroundMillis), 21, Color.WHITE);
            row.addView(duration);
            addRow(row);
        }
        addText("Total foreground usage: " + UsageReportMath.formatDuration(summary.totalMillis), 22, Color.WHITE, dp(20));
        addText("Daily average: " + UsageReportMath.formatDuration(summary.averageMillis), 20, Color.LTGRAY, dp(10));
        addText("Most-used app: " + (summary.mostUsedApp == null
                ? "—"
                : summary.mostUsedApp.label + " (" + UsageReportMath.formatDuration(summary.mostUsedApp.millis) + ")"),
                20, Color.LTGRAY, dp(10));
        addText("Most-used day: " + (summary.mostUsedDay == null
                ? "—"
                : summary.mostUsedDay.weekday + " (" + UsageReportMath.formatDuration(summary.mostUsedDay.totalForegroundMillis) + ")"),
                20, Color.LTGRAY, dp(10));
        if (!periodUsage.apps.isEmpty()) {
            addText("Top apps", 21, Color.WHITE, dp(22));
            for (int index = 0; index < Math.min(3, periodUsage.apps.size()); index++) {
                addAppRow(periodUsage.apps.get(index));
            }
        }
    }

    private void renderApps() {
        if (periodUsage.apps.isEmpty()) {
            addText("No app usage was reported for this period.", 22, Color.LTGRAY, dp(20));
            return;
        }
        addText(periodLabel(selectedPeriod).toUpperCase(java.util.Locale.getDefault()) + " APPS",
                22, Color.WHITE, dp(18));
        for (UsageRepository.AppUsage app : periodUsage.apps) addAppRow(app);
    }

    private void renderAppDetails() {
        if (selectedAppDetails == null) {
            addText("Loading app details...", 22, Color.LTGRAY, dp(20));
            return;
        }
        UsageRepository.AppDetails details = selectedAppDetails;
        addText(details.label, 30, Color.WHITE, dp(8));
        addText(details.packageName, 15, Color.GRAY, dp(12));
        addText("Today: " + UsageReportMath.formatDuration(details.todayMillis), 24, Color.WHITE, dp(14));
        addText("Past 7 days (including today): " + UsageReportMath.formatDuration(details.previousSevenDayMillis), 22, Color.LTGRAY, dp(12));
        addText("DAILY USAGE", 19, Color.WHITE, dp(24));
        for (UsageRepository.DayUsage day : details.days) {
            UsageRepository.AppUsage app = findApp(day, details.packageName);
            long duration = app == null ? 0L : app.foregroundMillis;
            addText(day.weekday + " " + DateFormat.getDateInstance(DateFormat.SHORT).format(new Date(day.startMillis))
                    + "   " + UsageReportMath.formatDuration(duration), 20, Color.LTGRAY, dp(8));
        }
        addText("Completed recorded sessions: " + (details.sessionEventsAvailable
                ? String.valueOf(details.completedSessions)
                : "unavailable"), 19, Color.LTGRAY, dp(22));
        addText("Longest completed recorded session: " + (details.sessionEventsAvailable
                ? UsageReportMath.formatDuration(details.longestCompletedSessionMillis)
                : "unavailable"), 19, Color.LTGRAY, dp(8));
        addText("Session counts use paired Android foreground/background events only. Incomplete or ambiguous sessions are excluded.",
                16, Color.GRAY, dp(16));
        addButton("Back to App Breakdown", () -> {
            selectedPackage = null;
            selectedAppDetails = null;
            renderReport();
        });
    }

    private void addAppRow(UsageRepository.AppUsage app) {
        Button row = new Button(this);
        row.setText(app.label + "\n" + UsageReportMath.formatDuration(app.foregroundMillis));
        row.setTextSize(20);
        row.setGravity(Gravity.CENTER_VERTICAL | Gravity.START);
        row.setMinHeight(dp(82));
        Drawable icon = app.icon;
        if (icon != null) {
            icon.setBounds(0, 0, dp(48), dp(48));
            row.setCompoundDrawables(icon, null, null, null);
            row.setCompoundDrawablePadding(dp(18));
        }
        row.setOnClickListener(view -> loadAppDetails(app.packageName));
        addRow(row);
    }

    private void loadAppDetails(String packageName) {
        selectedPackage = packageName;
        selectedAppDetails = null;
        renderReport();
        int request = ++generation;
        worker.execute(() -> {
            try {
                UsageRepository.AppDetails details = repository.loadAppDetails(packageName);
                mainHandler.post(() -> {
                    if (destroyed || request != generation || !packageName.equals(selectedPackage)) return;
                    selectedAppDetails = details;
                    renderReport();
                });
            } catch (RuntimeException e) {
                Log.e(TAG, "Could not load usage details for " + packageName, e);
                mainHandler.post(() -> {
                    if (destroyed || request != generation) return;
                    renderReportError();
                });
            }
        });
    }

    private String periodLabel(UsageReportMath.Period period) {
        switch (period) {
            case CURRENT_WEEK: return "Current Week";
            case PREVIOUS_WEEK: return "Previous Week";
            case CURRENT_MONTH: return "Current Month";
            case PREVIOUS_MONTH: return "Previous Month";
            default: throw new IllegalArgumentException("Unknown usage period: " + period);
        }
    }

    private UsageRepository.AppUsage findApp(UsageRepository.DayUsage day, String packageName) {
        for (UsageRepository.AppUsage app : day.apps) {
            if (app.packageName.equals(packageName)) return app;
        }
        return null;
    }

    private void beginPage(String title, String subtitle) {
        ScrollView scrollView = new ScrollView(this);
        scrollView.setBackgroundColor(Color.rgb(16, 19, 22));
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(48), dp(28), dp(48), dp(32));
        scrollView.addView(root);

        TextView heading = text(title, 32, Color.WHITE);
        root.addView(heading, fullWidth());
        TextView subheading = text(subtitle, 18, Color.LTGRAY);
        subheading.setPadding(0, dp(6), 0, dp(14));
        root.addView(subheading, fullWidth());

        content = root;
        setContentView(scrollView);
    }

    private void addText(String value, int size, int color, int topMargin) {
        TextView textView = text(value, size, color);
        LinearLayout.LayoutParams params = fullWidth();
        params.topMargin = topMargin;
        content.addView(textView, params);
    }

    private void addButton(String label, Runnable action) {
        Button button = new Button(this);
        button.setText(label);
        button.setTextSize(20);
        button.setMinHeight(dp(64));
        button.setOnClickListener(view -> action.run());
        LinearLayout.LayoutParams params = fullWidth();
        params.topMargin = dp(12);
        content.addView(button, params);
    }

    private void addRow(View row) {
        LinearLayout.LayoutParams params = fullWidth();
        params.topMargin = dp(8);
        content.addView(row, params);
    }

    private TextView text(String value, int size, int color) {
        TextView text = new TextView(this);
        text.setText(value);
        text.setTextSize(size);
        text.setTextColor(color);
        return text;
    }

    private LinearLayout.LayoutParams fullWidth() {
        return new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }
}
