package com.vicky.clockeverywhere;

import org.junit.Before;
import org.junit.Test;

import java.util.Calendar;
import java.util.TimeZone;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class ClockTimeFormatterTest {
    private ClockTimeFormatter formatter;
    private Calendar calendar;
    private final char[] digits = new char[6];

    @Before public void setUp() {
        TimeZone utc = TimeZone.getTimeZone("UTC");
        formatter = new ClockTimeFormatter(utc);
        calendar = Calendar.getInstance(utc);
    }

    @Test public void formats24HourTime() {
        assertFormatted(19, 42, 37, true, "194237");
    }

    @Test public void formats12HourTime() {
        assertFormatted(19, 42, 37, false, "074237");
        assertFormatted(0, 0, 0, false, "120000");
        assertFormatted(1, 0, 0, false, "010000");
    }

    @Test public void secondsIncrementChangesOnlyFinalDigit() {
        assertChangedDigits(19, 42, 37, 19, 42, 38, true, new int[] {5});
    }

    @Test public void minuteRolloverChangesOnlyAffectedDigits() {
        assertChangedDigits(19, 42, 59, 19, 43, 0, true, new int[] {3, 4, 5});
    }

    @Test public void hourRolloverChangesOnlyAffectedDigits() {
        assertChangedDigits(19, 59, 59, 20, 0, 0, true, new int[] {0, 1, 2, 3, 4, 5});
    }

    @Test public void midnightRolloverFormatsCorrectly() {
        assertChangedDigits(23, 59, 59, 0, 0, 0, true, new int[] {0, 1, 2, 3, 4, 5});
    }

    @Test public void twelveHourMidnightRolloverFormatsCorrectly() {
        assertChangedDigits(23, 59, 59, 0, 0, 0, false, new int[] {1, 2, 3, 4, 5});
    }

    @Test public void twelveHourOneAmRolloverFormatsCorrectly() {
        assertChangedDigits(0, 59, 59, 1, 0, 0, false, new int[] {0, 1, 2, 3, 4, 5});
    }

    @Test public void twelveHourMeridiemChangesAtMidnight() {
        formatter.format(timeMillis(23, 59, 59), false, digits);
        assertEquals('P', formatter.getMeridiem());
        formatter.format(timeMillis(0, 0, 0), false, digits);
        assertEquals('A', formatter.getMeridiem());
    }

    private void assertFormatted(int hour, int minute, int second, boolean use24Hour, String expected) {
        formatter.format(timeMillis(hour, minute, second), use24Hour, digits);
        assertEquals(expected, new String(digits));
    }

    private void assertChangedDigits(
            int fromHour,
            int fromMinute,
            int fromSecond,
            int toHour,
            int toMinute,
            int toSecond,
            boolean use24Hour,
            int[] expectedChangedIndices) {
        char[] before = new char[6];
        char[] after = new char[6];
        formatter.format(timeMillis(fromHour, fromMinute, fromSecond), use24Hour, before);
        formatter.format(timeMillis(toHour, toMinute, toSecond), use24Hour, after);
        assertArrayEquals(expectedChangedIndices, changedIndices(before, after));
    }

    private long timeMillis(int hour, int minute, int second) {
        calendar.clear();
        calendar.set(2026, Calendar.JANUARY, 1, hour, minute, second);
        return calendar.getTimeInMillis();
    }

    private int[] changedIndices(char[] before, char[] after) {
        int changedCount = 0;
        for (int index = 0; index < before.length; index++) {
            if (before[index] != after[index]) changedCount++;
        }
        int[] changed = new int[changedCount];
        int next = 0;
        for (int index = 0; index < before.length; index++) {
            if (before[index] != after[index]) changed[next++] = index;
        }
        return changed;
    }
}
