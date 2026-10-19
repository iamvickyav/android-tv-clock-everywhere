package com.vicky.clockeverywhere;

import android.app.AppOpsManager;
import android.app.usage.UsageEvents;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Process;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

final class UsageRepository {
    private static final String TAG = "UsageRepository";
    private static final int EVENT_ACTIVE_ACTIVITIES = 0;
    private static final int EVENT_SESSION_START = 1;
    private static final int EVENT_TOTAL_MILLIS = 2;

    private final Context context;
    private final PackageManager packageManager;
    private final UsageStatsManager usageStatsManager;
    private final TimeZone timeZone;
    private final Set<String> launchablePackages;

    UsageRepository(Context context) {
        this.context = context.getApplicationContext();
        packageManager = this.context.getPackageManager();
        usageStatsManager = (UsageStatsManager) this.context.getSystemService(Context.USAGE_STATS_SERVICE);
        timeZone = TimeZone.getDefault();
        launchablePackages = findLaunchablePackages();
    }

    static boolean hasUsageAccess(Context context) {
        AppOpsManager appOps = (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);
        if (appOps == null) return false;
        try {
            return appOps.checkOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    Process.myUid(),
                    context.getPackageName()) == AppOpsManager.MODE_ALLOWED;
        } catch (SecurityException e) {
            Log.w(TAG, "Could not check Usage Access state.", e);
            return false;
        }
    }

    PeriodUsage loadPeriod(UsageReportMath.Period period) {
        if (usageStatsManager == null) {
            throw new IllegalStateException("Android UsageStats service is unavailable on this TV.");
        }
        long now = System.currentTimeMillis();
        UsageReportMath.PeriodRange range = UsageReportMath.periodRange(period, now, timeZone);
        List<DayUsage> days = new ArrayList<>();
        for (long dayStart = range.startMillis;
                dayStart < range.endDayExclusiveMillis;
                dayStart = UsageReportMath.addDays(dayStart, 1, timeZone)) {
            days.add(loadDay(dayStart, now));
        }
        UsageReportMath.PeriodSummary summary =
                UsageReportMath.summarizePeriod(days, range.asOfMillis, timeZone);
        return new PeriodUsage(
                period,
                range.startMillis,
                range.endDayExclusiveMillis,
                days,
                summary,
                UsageReportMath.aggregateApps(days));
    }

    DayUsage loadToday() {
        long now = System.currentTimeMillis();
        return loadDay(UsageReportMath.startOfDay(now, timeZone), now);
    }

    ForegroundUsage loadForegroundUsageToday() {
        if (usageStatsManager == null || !hasUsageAccess(context)) return null;

        long now = System.currentTimeMillis();
        long todayStart = UsageReportMath.startOfDay(now, timeZone);
        long eventStart = UsageReportMath.addDays(todayStart, -1, timeZone);
        UsageEvents events;
        try {
            events = usageStatsManager.queryEvents(eventStart, now);
        } catch (RuntimeException e) {
            Log.w(TAG, "Could not read foreground app events.", e);
            return null;
        }
        if (events == null) return null;

        Map<String, long[]> eventUsages = new HashMap<>();
        String foregroundPackage = null;
        UsageEvents.Event event = new UsageEvents.Event();
        while (events.hasNextEvent()) {
            events.getNextEvent(event);
            String packageName = event.getPackageName();
            if (packageName == null) continue;

            int type = event.getEventType();
            if (isForegroundEvent(type)) {
                long[] usage = eventUsages.get(packageName);
                if (usage == null) {
                    usage = new long[3];
                    eventUsages.put(packageName, usage);
                }
                if (usage[EVENT_ACTIVE_ACTIVITIES] == 0L) {
                    usage[EVENT_SESSION_START] = event.getTimeStamp();
                }
                usage[EVENT_ACTIVE_ACTIVITIES]++;
                foregroundPackage = packageName;
            } else if (isBackgroundEvent(type)) {
                long[] usage = eventUsages.get(packageName);
                if (usage != null && usage[EVENT_ACTIVE_ACTIVITIES] > 0L) {
                    usage[EVENT_ACTIVE_ACTIVITIES]--;
                    if (usage[EVENT_ACTIVE_ACTIVITIES] == 0L) {
                        long sessionStart = Math.max(usage[EVENT_SESSION_START], todayStart);
                        usage[EVENT_TOTAL_MILLIS] += Math.max(0L, event.getTimeStamp() - sessionStart);
                        usage[EVENT_SESSION_START] = 0L;
                    }
                }
                if (packageName.equals(foregroundPackage)
                        && (usage == null || usage[EVENT_ACTIVE_ACTIVITIES] == 0L)) {
                    foregroundPackage = null;
                }
            }
        }

        if (foregroundPackage == null || foregroundPackage.equals(context.getPackageName())) return null;
        long[] eventUsage = eventUsages.get(foregroundPackage);
        if (eventUsage == null) return null;

        long eventDuration = eventUsage[EVENT_TOTAL_MILLIS];
        if (eventUsage[EVENT_ACTIVE_ACTIVITIES] > 0L) {
            eventDuration += Math.max(
                    0L,
                    now - Math.max(eventUsage[EVENT_SESSION_START], todayStart));
        }
        long statsDuration = loadTodayDuration(foregroundPackage, todayStart, now);
        long duration = Math.max(eventDuration, statsDuration);
        ApplicationInfo info = resolveApplication(foregroundPackage);
        String label = info == null ? foregroundPackage : loadLabel(info, foregroundPackage);
        return new ForegroundUsage(label, foregroundPackage, duration);
    }

    private long loadTodayDuration(String packageName, long todayStart, long now) {
        if (now <= todayStart) return 0L;
        try {
            List<UsageStats> stats = usageStatsManager.queryUsageStats(
                    UsageStatsManager.INTERVAL_DAILY,
                    todayStart,
                    now);
            long totalMillis = 0L;
            if (stats != null) {
                for (UsageStats usage : stats) {
                    if (packageName.equals(usage.getPackageName())) {
                        totalMillis += usage.getTotalTimeInForeground();
                    }
                }
            }
            return totalMillis;
        } catch (RuntimeException e) {
            Log.w(TAG, "Could not read today's usage for " + packageName + ".", e);
            return 0L;
        }
    }

    AppDetails loadAppDetails(String packageName) {
        long now = System.currentTimeMillis();
        long todayStart = UsageReportMath.startOfDay(now, timeZone);
        long firstDay = UsageReportMath.addDays(todayStart, -6, timeZone);
        List<DayUsage> dailyUsage = new ArrayList<>(7);
        long totalMillis = 0L;
        for (int dayIndex = 0; dayIndex < 7; dayIndex++) {
            DayUsage day = loadDay(UsageReportMath.addDays(firstDay, dayIndex, timeZone), now);
            dailyUsage.add(day);
            AppUsage app = findApp(day.apps, packageName);
            if (app != null) totalMillis += app.foregroundMillis;
        }

        SessionMetrics sessions = loadCompletedSessions(packageName, firstDay, now);
        DayUsage today = dailyUsage.get(dailyUsage.size() - 1);
        AppUsage todayUsage = findApp(today.apps, packageName);
        AppUsage appInfo = todayUsage;
        if (appInfo == null) {
            for (DayUsage day : dailyUsage) {
                appInfo = findApp(day.apps, packageName);
                if (appInfo != null) break;
            }
        }
        String label = appInfo != null ? appInfo.label : resolveLabel(packageName);
        Drawable icon = appInfo != null ? appInfo.icon : resolveIcon(packageName);
        return new AppDetails(
                label,
                packageName,
                todayUsage == null ? 0L : todayUsage.foregroundMillis,
                dailyUsage,
                totalMillis,
                sessions.completedCount,
                sessions.longestMillis,
                sessions.eventsAvailable);
    }

    private DayUsage loadDay(long dayStart, long now) {
        long dayEnd = UsageReportMath.addDays(dayStart, 1, timeZone);
        long queryEnd = Math.min(dayEnd, now);
        List<AppUsage> apps = Collections.emptyList();
        if (usageStatsManager != null && queryEnd > dayStart) {
            List<UsageStats> stats = usageStatsManager.queryUsageStats(
                    UsageStatsManager.INTERVAL_DAILY,
                    dayStart,
                    queryEnd);
            if (stats != null && !stats.isEmpty()) apps = buildAppUsages(stats);
        }
        long total = 0L;
        for (AppUsage app : apps) total += app.foregroundMillis;
        return new DayUsage(
                dayStart,
                dayEnd,
                UsageReportMath.weekday(dayStart, timeZone, java.util.Locale.getDefault()),
                apps,
                total);
    }

    private List<AppUsage> buildAppUsages(List<UsageStats> stats) {
        Map<String, Long> durations = new HashMap<>();
        for (UsageStats usage : stats) {
            long foregroundMillis = usage.getTotalTimeInForeground();
            String packageName = usage.getPackageName();
            if (foregroundMillis <= 0L || packageName == null || packageName.equals(context.getPackageName())) {
                continue;
            }
            Long previous = durations.get(packageName);
            durations.put(packageName, (previous == null ? 0L : previous) + foregroundMillis);
        }

        List<AppUsage> apps = new ArrayList<>(durations.size());
        for (Map.Entry<String, Long> entry : durations.entrySet()) {
            ApplicationInfo info = resolveApplication(entry.getKey());
            boolean isSystem = info != null && (info.flags & ApplicationInfo.FLAG_SYSTEM) != 0;
            if (!UsageReportMath.shouldIncludePackage(
                    isSystem,
                    launchablePackages.contains(entry.getKey()),
                    entry.getKey().equals(context.getPackageName()))) {
                continue;
            }
            String label = info == null ? entry.getKey() : loadLabel(info, entry.getKey());
            Drawable icon = info == null ? null : loadIcon(info, entry.getKey());
            apps.add(new AppUsage(label, entry.getKey(), entry.getValue(), icon));
        }
        return UsageReportMath.sortApps(apps);
    }

    private SessionMetrics loadCompletedSessions(String packageName, long start, long end) {
        if (usageStatsManager == null || end <= start) return new SessionMetrics(0, 0L, false);
        UsageEvents events = usageStatsManager.queryEvents(start, end);
        if (events == null) return new SessionMetrics(0, 0L, false);

        UsageEvents.Event event = new UsageEvents.Event();
        int activeActivities = 0;
        long sessionStart = 0L;
        int completedCount = 0;
        long longestMillis = 0L;
        while (events.hasNextEvent()) {
            events.getNextEvent(event);
            if (!packageName.equals(event.getPackageName())) continue;
            int type = event.getEventType();
            boolean foreground = isForegroundEvent(type);
            boolean background = isBackgroundEvent(type);
            if (foreground) {
                if (activeActivities == 0) sessionStart = event.getTimeStamp();
                activeActivities++;
            } else if (background && activeActivities > 0) {
                activeActivities--;
                if (activeActivities == 0) {
                    long duration = event.getTimeStamp() - sessionStart;
                    if (sessionStart >= start && duration > 0L) {
                        completedCount++;
                        longestMillis = Math.max(longestMillis, duration);
                    }
                }
            }
        }
        return new SessionMetrics(completedCount, longestMillis, true);
    }

    private boolean isForegroundEvent(int type) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return type == UsageEvents.Event.ACTIVITY_RESUMED;
        }
        return type == UsageEvents.Event.MOVE_TO_FOREGROUND;
    }

    private boolean isBackgroundEvent(int type) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return type == UsageEvents.Event.ACTIVITY_PAUSED;
        }
        return type == UsageEvents.Event.MOVE_TO_BACKGROUND;
    }

    private Set<String> findLaunchablePackages() {
        Set<String> packages = new HashSet<>();
        addPackagesForCategory(packages, android.content.Intent.CATEGORY_LAUNCHER);
        addPackagesForCategory(packages, android.content.Intent.CATEGORY_LEANBACK_LAUNCHER);
        return packages;
    }

    private void addPackagesForCategory(Set<String> packages, String category) {
        android.content.Intent intent = new android.content.Intent(android.content.Intent.ACTION_MAIN);
        intent.addCategory(category);
        try {
            for (android.content.pm.ResolveInfo result : packageManager.queryIntentActivities(intent, 0)) {
                if (result.activityInfo != null) packages.add(result.activityInfo.packageName);
            }
        } catch (RuntimeException e) {
            Log.w(TAG, "Could not enumerate launchable apps for category " + category, e);
        }
    }

    private ApplicationInfo resolveApplication(String packageName) {
        try {
            return packageManager.getApplicationInfo(packageName, 0);
        } catch (PackageManager.NameNotFoundException | SecurityException e) {
            Log.w(TAG, "Could not resolve application " + packageName, e);
            return null;
        }
    }

    private String resolveLabel(String packageName) {
        ApplicationInfo info = resolveApplication(packageName);
        return info == null ? packageName : loadLabel(info, packageName);
    }

    private Drawable resolveIcon(String packageName) {
        ApplicationInfo info = resolveApplication(packageName);
        return info == null ? null : loadIcon(info, packageName);
    }

    private String loadLabel(ApplicationInfo info, String fallback) {
        try {
            CharSequence label = packageManager.getApplicationLabel(info);
            return label == null || label.length() == 0 ? fallback : label.toString();
        } catch (RuntimeException e) {
            Log.w(TAG, "Could not load app label for " + fallback, e);
            return fallback;
        }
    }

    private Drawable loadIcon(ApplicationInfo info, String packageName) {
        try {
            return packageManager.getApplicationIcon(info);
        } catch (RuntimeException e) {
            Log.w(TAG, "Could not load app icon for " + packageName, e);
            return null;
        }
    }

    private static AppUsage findApp(List<AppUsage> apps, String packageName) {
        for (AppUsage app : apps) {
            if (app.packageName.equals(packageName)) return app;
        }
        return null;
    }

    static final class AppUsage {
        final String label;
        final String packageName;
        final long foregroundMillis;
        final Drawable icon;

        AppUsage(String label, String packageName, long foregroundMillis, Drawable icon) {
            this.label = label;
            this.packageName = packageName;
            this.foregroundMillis = foregroundMillis;
            this.icon = icon;
        }
    }

    static final class DayUsage {
        final long startMillis;
        final long endMillis;
        final String weekday;
        final List<AppUsage> apps;
        final long totalForegroundMillis;

        DayUsage(long startMillis, long endMillis, String weekday, List<AppUsage> apps, long totalForegroundMillis) {
            this.startMillis = startMillis;
            this.endMillis = endMillis;
            this.weekday = weekday;
            this.apps = Collections.unmodifiableList(new ArrayList<>(apps));
            this.totalForegroundMillis = totalForegroundMillis;
        }
    }

    static final class PeriodUsage {
        final UsageReportMath.Period period;
        final long startMillis;
        final long endDayExclusiveMillis;
        final List<DayUsage> days;
        final UsageReportMath.PeriodSummary summary;
        final List<AppUsage> apps;

        PeriodUsage(
                UsageReportMath.Period period,
                long startMillis,
                long endDayExclusiveMillis,
                List<DayUsage> days,
                UsageReportMath.PeriodSummary summary,
                List<AppUsage> apps) {
            this.period = period;
            this.startMillis = startMillis;
            this.endDayExclusiveMillis = endDayExclusiveMillis;
            this.days = Collections.unmodifiableList(new ArrayList<>(days));
            this.summary = summary;
            this.apps = Collections.unmodifiableList(new ArrayList<>(apps));
        }
    }

    static final class ForegroundUsage {
        final String label;
        final String packageName;
        final long todayMillis;

        ForegroundUsage(String label, String packageName, long todayMillis) {
            this.label = label;
            this.packageName = packageName;
            this.todayMillis = todayMillis;
        }
    }

    static final class AppDetails {
        final String label;
        final String packageName;
        final long todayMillis;
        final List<DayUsage> days;
        final long previousSevenDayMillis;
        final int completedSessions;
        final long longestCompletedSessionMillis;
        final boolean sessionEventsAvailable;

        AppDetails(
                String label,
                String packageName,
                long todayMillis,
                List<DayUsage> days,
                long previousSevenDayMillis,
                int completedSessions,
                long longestCompletedSessionMillis,
                boolean sessionEventsAvailable) {
            this.label = label;
            this.packageName = packageName;
            this.todayMillis = todayMillis;
            this.days = Collections.unmodifiableList(new ArrayList<>(days));
            this.previousSevenDayMillis = previousSevenDayMillis;
            this.completedSessions = completedSessions;
            this.longestCompletedSessionMillis = longestCompletedSessionMillis;
            this.sessionEventsAvailable = sessionEventsAvailable;
        }
    }

    private static final class SessionMetrics {
        final int completedCount;
        final long longestMillis;
        final boolean eventsAvailable;

        SessionMetrics(int completedCount, long longestMillis, boolean eventsAvailable) {
            this.completedCount = completedCount;
            this.longestMillis = longestMillis;
            this.eventsAvailable = eventsAvailable;
        }
    }

}
