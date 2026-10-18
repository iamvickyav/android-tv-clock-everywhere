package com.vicky.clockeverywhere;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.view.View;
import android.view.ViewGroup;

final class FlipClockView extends ViewGroup {
    private static final int DIGIT_COUNT = 6;
    private static final int DIGIT_GAP_FACTOR = 10;
    private static final int SEPARATOR_GAP_FACTOR = 28;
    private static final int COLON_WIDTH_FACTOR = 20;
    private static final int MERIDIEM_GAP_FACTOR = 18;
    private static final int MERIDIEM_WIDTH_FACTOR = 56;

    private final FlipDigitView[] digits = new FlipDigitView[DIGIT_COUNT];
    private final ClockColonView[] colons = new ClockColonView[2];
    private final ClockMarkerView meridiem;
    private final View[] row = new View[9];
    private int cardWidth;
    private int cardHeight;
    private int digitGap;
    private int separatorGap;
    private int colonWidth;

    FlipClockView(Context context, Typeface digitalTypeface) {
        super(context);
        setBackgroundColor(Color.BLACK);
        setWillNotDraw(true);

        int digitIndex = 0;
        int colonIndex = 0;
        meridiem = new ClockMarkerView(context, digitalTypeface);
        for (int index = 0; index < row.length; index++) {
            if (index == 2 || index == 5) {
                colons[colonIndex] = new ClockColonView(context);
                row[index] = colons[colonIndex++];
            } else if (index == row.length - 1) {
                row[index] = meridiem;
            } else {
                digits[digitIndex] = new FlipDigitView(context, digitalTypeface);
                row[index] = digits[digitIndex++];
            }
            addView(row[index], new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
        }
    }

    void setTime(char[] timeDigits, char meridiemMarker) {
        if (timeDigits.length < DIGIT_COUNT) {
            throw new IllegalArgumentException("The clock requires six digits.");
        }
        for (int index = 0; index < DIGIT_COUNT; index++) {
            digits[index].setDigit(timeDigits[index]);
        }
        meridiem.setMeridiem(meridiemMarker);
    }

    void stopAnimations() {
        for (FlipDigitView digit : digits) {
            digit.stopFlipAnimation();
        }
    }

    @Override protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int availableWidth = MeasureSpec.getSize(widthMeasureSpec) - getPaddingLeft() - getPaddingRight();
        int availableHeight = MeasureSpec.getSize(heightMeasureSpec) - getPaddingTop() - getPaddingBottom();
        boolean showMeridiem = meridiem.getVisibility() != GONE;
        float widthFactor = showMeridiem ? 6.46f : 5.72f;
        float heightByWidth = availableWidth / widthFactor;
        cardHeight = Math.max(1, Math.min((int) (availableHeight * 0.72f), (int) heightByWidth));
        cardWidth = Math.max(1, (int) (cardHeight * 0.65f));
        digitGap = Math.max(1, (int) (cardHeight * DIGIT_GAP_FACTOR / 100f));
        separatorGap = Math.max(1, (int) (cardHeight * SEPARATOR_GAP_FACTOR / 100f));
        colonWidth = Math.max(1, (int) (cardHeight * COLON_WIDTH_FACTOR / 100f));

        int rowWidth = 6 * cardWidth
                + 3 * digitGap
                + 4 * separatorGap
                + 2 * colonWidth
                + (showMeridiem ? (int) (cardHeight * (MERIDIEM_GAP_FACTOR + MERIDIEM_WIDTH_FACTOR) / 100f) : 0);
        int rowHeight = cardHeight;
        setMeasuredDimension(resolveSize(rowWidth + getPaddingLeft() + getPaddingRight(), widthMeasureSpec),
                resolveSize(rowHeight + getPaddingTop() + getPaddingBottom(), heightMeasureSpec));

        for (int index = 0; index < row.length; index++) {
            int childWidth;
            if (index == 2 || index == 5) {
                childWidth = colonWidth;
            } else if (index == row.length - 1) {
                childWidth = showMeridiem ? (int) (cardHeight * MERIDIEM_WIDTH_FACTOR / 100f) : 0;
            } else {
                childWidth = cardWidth;
            }
            row[index].measure(MeasureSpec.makeMeasureSpec(childWidth, MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(cardHeight, MeasureSpec.EXACTLY));
        }
    }

    @Override protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        int width = right - left;
        int height = bottom - top;
        int rowWidth = 6 * cardWidth
                + 3 * digitGap
                + 4 * separatorGap
                + 2 * colonWidth
                + (meridiem.getVisibility() != GONE
                        ? (int) (cardHeight * (MERIDIEM_GAP_FACTOR + MERIDIEM_WIDTH_FACTOR) / 100f)
                        : 0);
        int x = (width - rowWidth) / 2;
        int y = (height - cardHeight) / 2;

        for (int index = 0; index < row.length; index++) {
            View child = row[index];
            int childWidth = child.getMeasuredWidth();
            child.layout(x, y, x + childWidth, y + cardHeight);
            x += childWidth;
            if (index == 1 || index == 4) {
                x += separatorGap;
            } else if (index == 2 || index == 5) {
                x += separatorGap;
            } else if (index == 7 && meridiem.getVisibility() != GONE) {
                x += (int) (cardHeight * MERIDIEM_GAP_FACTOR / 100f);
            } else if (index != 7) {
                x += digitGap;
            }
        }
    }

    private static final class ClockColonView extends View {
        private final Paint dotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        ClockColonView(Context context) {
            super(context);
            dotPaint.setColor(Color.rgb(235, 235, 235));
        }

        @Override protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float radius = Math.min(getWidth() * 0.22f, getHeight() * 0.035f);
            float centerX = getWidth() / 2f;
            canvas.drawCircle(centerX, getHeight() * 0.37f, radius, dotPaint);
            canvas.drawCircle(centerX, getHeight() * 0.63f, radius, dotPaint);
        }
    }

    private static final class ClockMarkerView extends View {
        private final Paint markerPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
        private final Paint.FontMetrics fontMetrics = new Paint.FontMetrics();
        private final char[] marker = new char[] {'A', 'M'};

        ClockMarkerView(Context context, Typeface digitalTypeface) {
            super(context);
            markerPaint.setColor(Color.rgb(220, 220, 220));
            markerPaint.setTypeface(digitalTypeface);
            markerPaint.setTextAlign(Paint.Align.CENTER);
        }

        void setMeridiem(char value) {
            if (value != 'A' && value != 'P') {
                if (getVisibility() != GONE) {
                    setVisibility(GONE);
                    requestLayout();
                }
                return;
            }
            if (getVisibility() != VISIBLE) {
                setVisibility(VISIBLE);
                requestLayout();
            }
            if (marker[0] != value) {
                marker[0] = value;
                invalidate();
            }
        }

        @Override protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            if (getVisibility() == GONE) return;
            markerPaint.setTextSize(getHeight() * 0.24f);
            markerPaint.getFontMetrics(fontMetrics);
            float baseline = (getHeight() - fontMetrics.ascent - fontMetrics.descent) / 2f;
            canvas.drawText(marker, 0, marker.length, getWidth() / 2f, baseline, markerPaint);
        }
    }
}
