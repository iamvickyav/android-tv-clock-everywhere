package com.vicky.clockeverywhere;

import org.junit.Test;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.TimeZone;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class UsageReportMathTest {
    private final TimeZone utc = TimeZone.getTimeZone("UTC");

    @Test public void formatsDurationsForHoursAndMinutes() {
        assertEquals("0m", UsageReportMath.formatDuration(59_999L));
        assertEquals("48m", UsageReportMath.formatDuration(48 * 60_000L));
        assertEquals("1h 12m", UsageReportMath.formatDuration(72 * 60_000L));
        assertEquals("3h 6m", UsageReportMath.formatDuration(186 * 60_000L));
    }

    @Test public void calculatesMondayStartOfWeekAcrossTimezone() {
        Calendar calendar = Calendar.getInstance(utc);
        calendar.clear();
        calendar.set(2026, Calendar.JUNE, 14, 18, 30);
        long sunday = calendar.getTimeInMillis();
        long mondayStart = UsageReportMath.startOfWeek(sunday, utc);

        calendar.setTimeInMillis(mondayStart);
        assertEquals(Calendar.MONDAY, calendar.get(Calendar.DAY_OF_WEEK));
        assertEquals(0, calendar.get(Calendar.HOUR_OF_DAY));
        assertEquals(8, calendar.get(Calendar.DAY_OF_MONTH));
    }

    @Test public void dayBoundariesFollowDaylightSavingChanges() {
        TimeZone newYork = TimeZone.getTimeZone("America/New_York");
        Calendar calendar = Calendar.getInstance(newYork);
        calendar.clear();
        calendar.set(2026, Calendar.MARCH, 8, 12, 0);
        long dayStart = UsageReportMath.startOfDay(calendar.getTimeInMillis(), newYork);
        long nextDay = UsageReportMath.addDays(dayStart, 1, newYork);

        assertEquals(23L * 60L * 60L * 1000L, nextDay - dayStart);
    }

    @Test public void sortsAppsByUsageAndThenLabel() {
        UsageRepository.AppUsage more = app("YouTube", "tv.youtube", 7_200_000L);
        UsageRepository.AppUsage sameUsageA = app("Alpha", "app.alpha", 3_600_000L);
        UsageRepository.AppUsage sameUsageB = app("Beta", "app.beta", 3_600_000L);
        assertEquals(Arrays.asList(more, sameUsageA, sameUsageB),
                UsageReportMath.sortApps(Arrays.asList(sameUsageB, more, sameUsageA)));
    }

    @Test public void systemAppFilteringKeepsLaunchableAppsAndExcludesClock() {
        assertTrue(UsageReportMath.shouldIncludePackage(false, false, false));
        assertTrue(UsageReportMath.shouldIncludePackage(true, true, false));
        assertFalse(UsageReportMath.shouldIncludePackage(true, false, false));
        assertFalse(UsageReportMath.shouldIncludePackage(false, true, true));
    }

    @Test public void emptyDataProducesZeroSummaryWithoutInventingTopAppOrDay() {
        UsageReportMath.WeekSummary summary = UsageReportMath.summarizeWeek(
                Collections.emptyList(),
                0L,
                utc);
        assertEquals(0L, summary.totalMillis);
        assertEquals(0L, summary.averageMillis);
        assertEquals(null, summary.mostUsedApp);
        assertEquals(null, summary.mostUsedDay);
    }

    @Test public void weeklySummaryExcludesFutureDaysFromAverage() {
        long monday = startOfDay(2026, Calendar.JUNE, 8, utc);
        long wednesday = UsageReportMath.addDays(monday, 2, utc);
        UsageRepository.AppUsage app = app("YouTube", "tv.youtube", 60 * 60_000L);
        UsageRepository.DayUsage mondayUsage = day(monday, app, 2 * 60 * 60_000L);
        UsageRepository.DayUsage tuesdayUsage = day(
                UsageReportMath.addDays(monday, 1, utc),
                app("YouTube", "tv.youtube", 60 * 60_000L),
                60 * 60_000L);
        UsageRepository.DayUsage wednesdayUsage = day(wednesday, null, 0L);
        UsageReportMath.WeekSummary summary = UsageReportMath.summarizeWeek(
                Arrays.asList(mondayUsage, tuesdayUsage, wednesdayUsage),
                UsageReportMath.addDays(wednesday, 1, utc),
                utc);

        assertEquals(3 * 60 * 60_000L, summary.totalMillis);
        assertEquals(60 * 60_000L, summary.averageMillis);
        assertSame(mondayUsage, summary.mostUsedDay);
        assertEquals("tv.youtube", summary.mostUsedApp.packageName);
    }

    private UsageRepository.AppUsage app(String label, String packageName, long millis) {
        return new UsageRepository.AppUsage(label, packageName, millis, null);
    }

    private UsageRepository.DayUsage day(long start, UsageRepository.AppUsage app, long totalMillis) {
        return new UsageRepository.DayUsage(
                start,
                UsageReportMath.addDays(start, 1, utc),
                UsageReportMath.weekday(start, utc, java.util.Locale.US),
                app == null ? Collections.emptyList() : Collections.singletonList(app),
                totalMillis);
    }

    private long startOfDay(int year, int month, int day, TimeZone timeZone) {
        Calendar calendar = Calendar.getInstance(timeZone);
        calendar.clear();
        calendar.set(year, month, day);
        return calendar.getTimeInMillis();
    }
}
