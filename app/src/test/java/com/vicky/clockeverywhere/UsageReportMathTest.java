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

    @Test public void formatsForegroundUsageForTheOverlay() {
        assertEquals("2 Hours 3 Minutes",
                UsageReportMath.formatUsageDuration(2 * 60L * 60_000L + 3 * 60_000L));
        assertEquals("45 Minutes", UsageReportMath.formatUsageDuration(45 * 60_000L));
        assertEquals("1 Hour", UsageReportMath.formatUsageDuration(60 * 60_000L));
        assertEquals("1 Hour 5 Minutes",
                UsageReportMath.formatUsageDuration(65 * 60_000L));
        assertEquals("Less than 1 Minute", UsageReportMath.formatUsageDuration(59_999L));
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

    @Test public void currentAndPreviousWeekUseMondayCalendarBoundaries() {
        long now = time(2026, Calendar.JANUARY, 1, 12, 30, utc);
        UsageReportMath.PeriodRange current = UsageReportMath.periodRange(
                UsageReportMath.Period.CURRENT_WEEK, now, utc);
        UsageReportMath.PeriodRange previous = UsageReportMath.periodRange(
                UsageReportMath.Period.PREVIOUS_WEEK, now, utc);

        assertDate(current.startMillis, 2025, Calendar.DECEMBER, 29, utc);
        assertDate(current.endDayExclusiveMillis, 2026, Calendar.JANUARY, 2, utc);
        assertDate(previous.startMillis, 2025, Calendar.DECEMBER, 22, utc);
        assertEquals(current.startMillis, previous.endDayExclusiveMillis);
        assertEquals(now, current.asOfMillis);
        assertEquals(current.startMillis - 1L, previous.asOfMillis);
    }

    @Test public void currentAndPreviousMonthRespectPartialCurrentDayAndYearChange() {
        long now = time(2026, Calendar.JANUARY, 19, 16, 45, utc);
        UsageReportMath.PeriodRange current = UsageReportMath.periodRange(
                UsageReportMath.Period.CURRENT_MONTH, now, utc);
        UsageReportMath.PeriodRange previous = UsageReportMath.periodRange(
                UsageReportMath.Period.PREVIOUS_MONTH, now, utc);

        assertDate(current.startMillis, 2026, Calendar.JANUARY, 1, utc);
        assertDate(current.endDayExclusiveMillis, 2026, Calendar.JANUARY, 20, utc);
        assertEquals(now, current.asOfMillis);
        assertDate(previous.startMillis, 2025, Calendar.DECEMBER, 1, utc);
        assertDate(previous.endDayExclusiveMillis, 2026, Calendar.JANUARY, 1, utc);
        assertEquals(previous.endDayExclusiveMillis - 1L, previous.asOfMillis);
        assertEquals(31L, (previous.endDayExclusiveMillis - previous.startMillis) / 86_400_000L);
    }

    @Test public void monthRangesHandleFebruaryLeapYearsAndThirtyDayMonths() {
        long marchLeapYear = time(2024, Calendar.MARCH, 15, 12, 0, utc);
        UsageReportMath.PeriodRange february = UsageReportMath.periodRange(
                UsageReportMath.Period.PREVIOUS_MONTH, marchLeapYear, utc);
        assertDate(february.startMillis, 2024, Calendar.FEBRUARY, 1, utc);
        assertEquals(29L, (february.endDayExclusiveMillis - february.startMillis) / 86_400_000L);

        long april = time(2025, Calendar.APRIL, 15, 12, 0, utc);
        UsageReportMath.PeriodRange march = UsageReportMath.periodRange(
                UsageReportMath.Period.PREVIOUS_MONTH, april, utc);
        assertEquals(31L, (march.endDayExclusiveMillis - march.startMillis) / 86_400_000L);

        long may = time(2025, Calendar.MAY, 15, 12, 0, utc);
        UsageReportMath.PeriodRange aprilRange = UsageReportMath.periodRange(
                UsageReportMath.Period.PREVIOUS_MONTH, may, utc);
        assertEquals(30L, (aprilRange.endDayExclusiveMillis - aprilRange.startMillis) / 86_400_000L);
    }

    @Test public void weekStartsOnMondayForBothSundayAndMondayDates() {
        long monday = time(2026, Calendar.JUNE, 8, 12, 0, utc);
        long sunday = time(2026, Calendar.JUNE, 14, 12, 0, utc);
        assertEquals(monday, UsageReportMath.startOfWeek(monday, utc));
        assertEquals(monday, UsageReportMath.startOfWeek(sunday, utc));
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
        UsageReportMath.PeriodSummary summary = UsageReportMath.summarizePeriod(
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
        UsageReportMath.PeriodSummary summary = UsageReportMath.summarizePeriod(
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

    private long time(int year, int month, int day, int hour, int minute, TimeZone timeZone) {
        Calendar calendar = Calendar.getInstance(timeZone);
        calendar.clear();
        calendar.set(year, month, day, hour, minute, 0);
        return calendar.getTimeInMillis();
    }

    private void assertDate(long millis, int year, int month, int day, TimeZone timeZone) {
        Calendar calendar = Calendar.getInstance(timeZone);
        calendar.setTimeInMillis(millis);
        assertEquals(year, calendar.get(Calendar.YEAR));
        assertEquals(month, calendar.get(Calendar.MONTH));
        assertEquals(day, calendar.get(Calendar.DAY_OF_MONTH));
    }
}
