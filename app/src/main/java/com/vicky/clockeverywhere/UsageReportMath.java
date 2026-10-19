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

    enum Period {
        CURRENT_WEEK,
        PREVIOUS_WEEK,
        CURRENT_MONTH,
        PREVIOUS_MONTH
    }

    static final class PeriodRange {
        final long startMillis;
        final long endDayExclusiveMillis;
        final long asOfMillis;

        PeriodRange(long startMillis, long endDayExclusiveMillis, long asOfMillis) {
            this.startMillis = startMillis;
            this.endDayExclusiveMillis = endDayExclusiveMillis;
            this.asOfMillis = asOfMillis;
        }
    }

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

    static long startOfMonth(long timeMillis, TimeZone timeZone) {
        Calendar calendar = Calendar.getInstance(timeZone);
        calendar.setTimeInMillis(startOfDay(timeMillis, timeZone));
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        return calendar.getTimeInMillis();
    }

    static long addMonths(long monthStartMillis, int months, TimeZone timeZone) {
        Calendar calendar = Calendar.getInstance(timeZone);
        calendar.setTimeInMillis(monthStartMillis);
        calendar.add(Calendar.MONTH, months);
        return calendar.getTimeInMillis();
    }

    static long startOfWeek(long timeMillis, TimeZone timeZone) {
        Calendar calendar = Calendar.getInstance(timeZone);
        calendar.setTimeInMillis(startOfDay(timeMillis, timeZone));
        int daysSinceMonday = (calendar.get(Calendar.DAY_OF_WEEK) + 5) % 7;
        calendar.add(Calendar.DAY_OF_MONTH, -daysSinceMonday);
        return calendar.getTimeInMillis();
    }

    static PeriodRange periodRange(Period period, long nowMillis, TimeZone timeZone) {
        long todayStart = startOfDay(nowMillis, timeZone);
        long tomorrowStart = addDays(todayStart, 1, timeZone);
        long currentWeekStart = startOfWeek(nowMillis, timeZone);
        long currentMonthStart = startOfMonth(nowMillis, timeZone);
        long start;
        long endDayExclusive;
        switch (period) {
            case CURRENT_WEEK:
                start = currentWeekStart;
                endDayExclusive = tomorrowStart;
                break;
            case PREVIOUS_WEEK:
                start = addDays(currentWeekStart, -7, timeZone);
                endDayExclusive = currentWeekStart;
                break;
            case CURRENT_MONTH:
                start = currentMonthStart;
                endDayExclusive = tomorrowStart;
                break;
            case PREVIOUS_MONTH:
                start = addMonths(currentMonthStart, -1, timeZone);
                endDayExclusive = currentMonthStart;
                break;
            default:
                throw new IllegalArgumentException("Unknown usage period: " + period);
        }
        long asOfMillis = Math.min(nowMillis, endDayExclusive - 1L);
        return new PeriodRange(start, endDayExclusive, asOfMillis);
    }

    static String formatDuration(long durationMillis) {
        long totalMinutes = Math.max(0L, durationMillis) / 60_000L;
        long hours = totalMinutes / 60L;
        long minutes = totalMinutes % 60L;
        if (hours == 0) return minutes + "m";
        return hours + "h " + minutes + "m";
    }

    static String formatUsageDuration(long durationMillis) {
        if (durationMillis <= 0L) return "0 Minutes";
        long totalMinutes = durationMillis / 60_000L;
        if (totalMinutes == 0L) return "Less than 1 Minute";
        long hours = totalMinutes / 60L;
        long minutes = totalMinutes % 60L;
        if (hours == 0L) return totalMinutes + (totalMinutes == 1L ? " Minute" : " Minutes");
        String result = hours + (hours == 1L ? " Hour" : " Hours");
        if (minutes > 0L) result += " " + minutes + (minutes == 1L ? " Minute" : " Minutes");
        return result;
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

    static PeriodSummary summarizePeriod(List<UsageRepository.DayUsage> days, long asOfMillis, TimeZone timeZone) {
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
        return new PeriodSummary(totalMillis, averageMillis, mostUsedApp, mostUsedDay);
    }

    static List<UsageRepository.AppUsage> aggregateApps(List<UsageRepository.DayUsage> days) {
        Map<String, AppTotal> appTotals = new HashMap<>();
        Map<String, UsageRepository.AppUsage> appDetails = new HashMap<>();
        for (UsageRepository.DayUsage day : days) {
            for (UsageRepository.AppUsage app : day.apps) {
                AppTotal total = appTotals.get(app.packageName);
                if (total == null) {
                    appTotals.put(app.packageName,
                            new AppTotal(app.label, app.packageName, app.foregroundMillis));
                    appDetails.put(app.packageName, app);
                } else {
                    total.millis += app.foregroundMillis;
                }
            }
        }
        List<UsageRepository.AppUsage> apps = new ArrayList<>(appTotals.size());
        for (AppTotal total : appTotals.values()) {
            UsageRepository.AppUsage details = appDetails.get(total.packageName);
            apps.add(new UsageRepository.AppUsage(
                    total.label, total.packageName, total.millis, details.icon));
        }
        return sortApps(apps);
    }

    static String weekday(long dayStartMillis, TimeZone timeZone, Locale locale) {
        Calendar calendar = Calendar.getInstance(timeZone, locale);
        calendar.setTimeInMillis(dayStartMillis);
        return new DateFormatSymbols(locale).getWeekdays()[calendar.get(Calendar.DAY_OF_WEEK)];
    }

    static final class PeriodSummary {
        final long totalMillis;
        final long averageMillis;
        final AppTotal mostUsedApp;
        final UsageRepository.DayUsage mostUsedDay;

        PeriodSummary(long totalMillis, long averageMillis, AppTotal mostUsedApp, UsageRepository.DayUsage mostUsedDay) {
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
