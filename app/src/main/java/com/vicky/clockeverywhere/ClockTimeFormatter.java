package com.vicky.clockeverywhere;

import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

final class ClockTimeFormatter {
    private final Calendar calendar;

    ClockTimeFormatter(TimeZone timeZone) {
        calendar = Calendar.getInstance(timeZone, Locale.US);
    }

    void format(long timeMillis, boolean use24Hour, char[] digits) {
        if (digits.length < 6) {
            throw new IllegalArgumentException("The clock digit buffer must have six characters.");
        }
        calendar.setTimeInMillis(timeMillis);
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        if (!use24Hour) {
            hour %= 12;
            if (hour == 0) hour = 12;
        }
        writeTwoDigits(hour, digits, 0);
        writeTwoDigits(calendar.get(Calendar.MINUTE), digits, 2);
        writeTwoDigits(calendar.get(Calendar.SECOND), digits, 4);
    }

    char getMeridiem() {
        return calendar.get(Calendar.AM_PM) == Calendar.AM ? 'A' : 'P';
    }

    private void writeTwoDigits(int value, char[] digits, int offset) {
        digits[offset] = (char) ('0' + value / 10);
        digits[offset + 1] = (char) ('0' + value % 10);
    }
}
