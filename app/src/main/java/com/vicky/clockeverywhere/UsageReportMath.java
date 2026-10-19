package com.vicky.clockeverywhere;

import java.text.DateFormatSymbols;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

final class UsageReportMath {
    private UsageReportMath() { }

    static long startOfDay(long timeMillis, TimeZone timeZone) {
        Calendar calendar = Calendar.getInstance(timeZone);
        calendar.setTimeInMillis(timeMillis);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }

    static long addDays(long dayStartMillis, int days, TimeZone timeZone) {
        Calendar calendar = Calendar.getInstance(timeZone);
        calendar.setTimeInMillis(dayStartMillis);
        calendar.add(Calendar.DAY_OF_MONTH, days);
        return calendar.getTimeInMillis();
    }

    static long startOfWeek(long timeMillis, TimeZone timeZone) {
        Calendar calendar = Calendar.getInstance(timeZone);
        calendar.setTimeInMillis(startOfDay(timeMillis, timeZone));
        int daysSinceMonday = (calendar.get(Calendar.DAY_OF_WEEK) + 5) % 7;
        calendar.add(Calendar.DAY_OF_MONTH, -daysSinceMonday);
        return calendar.getTimeInMillis();
    }

    static String formatDuration(long durationMillis) {
        long totalMinutes = Math.max(0L, durationMillis) / 60_000L;
        long hours = totalMinutes / 60L;
        long minutes = totalMinutes % 60L;
        if (hours == 0) return minutes + "m";
        return hours + "h " + minutes + "m";
    }

    static List<UsageRepository.AppUsage> sortApps(List<UsageRepository.AppUsage> apps) {
        List<UsageRepository.AppUsage> sorted = new ArrayList<>(apps);
        Collections.sort(sorted, new Comparator<UsageRepository.AppUsage>() {
            @Override public int compare(UsageRepository.AppUsage left, UsageRepository.AppUsage right) {
                int byDuration = Long.compare(right.foregroundMillis, left.foregroundMillis);
                return byDuration != 0 ? byDuration : left.label.compareToIgnoreCase(right.label);
            }
        });
        return sorted;
    }

    static boolean shouldIncludePackage(boolean isSystemPackage, boolean hasLauncherEntry, boolean isClockApp) {
        return !isClockApp && (!isSystemPackage || hasLauncherEntry);
    }

    static WeekSummary summarizeWeek(List<UsageRepository.DayUsage> days, long asOfMillis, TimeZone timeZone) {
        long totalMillis = 0L;
        int elapsedDays = 0;
        long todayStart = startOfDay(asOfMillis, timeZone);
        UsageRepository.DayUsage mostUsedDay = null;
        Map<String, AppTotal> appTotals = new HashMap<>();
        for (UsageRepository.DayUsage day : days) {
            if (day.startMillis > todayStart) continue;
            elapsedDays++;
            totalMillis += day.totalForegroundMillis;
            if (day.totalForegroundMillis > 0L
                    && (mostUsedDay == null || day.totalForegroundMillis > mostUsedDay.totalForegroundMillis)) {
                mostUsedDay = day;
            }
            for (UsageRepository.AppUsage app : day.apps) {
                AppTotal current = appTotals.get(app.packageName);
                if (current == null) {
                    appTotals.put(app.packageName, new AppTotal(app.label, app.packageName, app.foregroundMillis));
                } else {
                    current.millis += app.foregroundMillis;
                }
            }
        }

        AppTotal mostUsedApp = null;
        for (AppTotal app : appTotals.values()) {
            if (mostUsedApp == null || app.millis > mostUsedApp.millis) mostUsedApp = app;
        }
        long averageMillis = elapsedDays == 0 ? 0L : totalMillis / elapsedDays;
        return new WeekSummary(totalMillis, averageMillis, mostUsedApp, mostUsedDay);
    }

    static String weekday(long dayStartMillis, TimeZone timeZone, Locale locale) {
        Calendar calendar = Calendar.getInstance(timeZone, locale);
        calendar.setTimeInMillis(dayStartMillis);
        return new DateFormatSymbols(locale).getWeekdays()[calendar.get(Calendar.DAY_OF_WEEK)];
    }

    static final class WeekSummary {
        final long totalMillis;
        final long averageMillis;
        final AppTotal mostUsedApp;
        final UsageRepository.DayUsage mostUsedDay;

        WeekSummary(long totalMillis, long averageMillis, AppTotal mostUsedApp, UsageRepository.DayUsage mostUsedDay) {
            this.totalMillis = totalMillis;
            this.averageMillis = averageMillis;
            this.mostUsedApp = mostUsedApp;
            this.mostUsedDay = mostUsedDay;
        }
    }

    static final class AppTotal {
        final String label;
        final String packageName;
        long millis;

        AppTotal(String label, String packageName, long millis) {
            this.label = label;
            this.packageName = packageName;
            this.millis = millis;
        }
    }
}
