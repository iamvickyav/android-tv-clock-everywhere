package com.vicky.clockeverywhere;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Camera;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

final class FlipDigitView extends View {
    static final long FLIP_DURATION_MS = 400L;

    private static final int CARD_COLOR = Color.rgb(24, 24, 24);
    private static final int TOP_FLAP_COLOR = Color.rgb(31, 31, 31);
    private static final int BOTTOM_FLAP_COLOR = Color.rgb(20, 20, 20);
    private static final int DIGIT_COLOR = Color.rgb(245, 245, 245);
    private static final int HINGE_COLOR = Color.rgb(5, 5, 5);
    private static final int EDGE_COLOR = Color.rgb(52, 52, 52);

    private final Paint cardPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint digitPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
    private final Paint hingePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint edgePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint.FontMetrics fontMetrics = new Paint.FontMetrics();
    private final RectF cardBounds = new RectF();
    private final Camera camera = new Camera();
    private final Matrix transform = new Matrix();
    private final char[] digitText = new char[1];

    private char digit;
    private char previousDigit;
    private float animationProgress = 1f;
    private ValueAnimator animator;

    FlipDigitView(Context context, Typeface digitalTypeface) {
        super(context);
        setLayerType(View.LAYER_TYPE_HARDWARE, null);
        digitPaint.setColor(DIGIT_COLOR);
        digitPaint.setTypeface(digitalTypeface);
        digitPaint.setTextAlign(Paint.Align.CENTER);
        hingePaint.setColor(HINGE_COLOR);
        edgePaint.setColor(EDGE_COLOR);
        edgePaint.setStyle(Paint.Style.STROKE);
        edgePaint.setStrokeWidth(1f);
        camera.setLocation(0f, 0f, -8f);
    }

    void setDigit(char newDigit) {
        if (newDigit < '0' || newDigit > '9' || newDigit == digit) return;

        cancelAnimation();
        if (digit == '\0') {
            digit = newDigit;
            previousDigit = newDigit;
            animationProgress = 1f;
            invalidate();
            return;
        }

        previousDigit = digit;
        digit = newDigit;
        animationProgress = 0f;
        animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(FLIP_DURATION_MS);
        animator.setInterpolator(new DecelerateInterpolator());
        animator.addUpdateListener(animation -> {
            animationProgress = (float) animation.getAnimatedValue();
            invalidate();
        });
        animator.addListener(new AnimatorListenerAdapter() {
            @Override public void onAnimationEnd(Animator animation) {
                animationProgress = 1f;
                previousDigit = digit;
                animator = null;
                invalidate();
            }

            @Override public void onAnimationCancel(Animator animation) {
                animationProgress = 1f;
                previousDigit = digit;
                animator = null;
                invalidate();
            }
        });
        animator.start();
    }

    void stopFlipAnimation() {
        cancelAnimation();
        previousDigit = digit;
        animationProgress = 1f;
        invalidate();
    }

    private void cancelAnimation() {
        if (animator != null) {
            ValueAnimator runningAnimator = animator;
            animator = null;
            runningAnimator.cancel();
        }
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (digit == '\0') return;

        float width = getWidth();
        float height = getHeight();
        float hingeY = height / 2f;
        float radius = height * 0.035f;
        cardBounds.set(0f, 0f, width, height);

        cardPaint.setColor(CARD_COLOR);
        canvas.drawRoundRect(cardBounds, radius, radius, cardPaint);

        if (animator == null || animationProgress >= 1f) {
            drawDigit(canvas, digit);
        } else {
            drawBaseHalves(canvas, hingeY);
            drawFlap(canvas, hingeY, radius);
        }

        cardPaint.setColor(HINGE_COLOR);
        canvas.drawRect(0f, hingeY - height * 0.006f, width, hingeY + height * 0.006f, cardPaint);
        canvas.drawLine(0f, hingeY + height * 0.012f, width, hingeY + height * 0.012f, hingePaint);
        canvas.drawRoundRect(cardBounds, radius, radius, edgePaint);
    }

    private void drawBaseHalves(Canvas canvas, float hingeY) {
        canvas.save();
        canvas.clipRect(0f, 0f, getWidth(), hingeY);
        drawDigit(canvas, digit);
        canvas.restore();

        canvas.save();
        canvas.clipRect(0f, hingeY, getWidth(), getHeight());
        drawDigit(canvas, previousDigit);
        canvas.restore();
    }

    private void drawFlap(Canvas canvas, float hingeY, float radius) {
        boolean upperFlap = animationProgress < 0.5f;
        float phase = upperFlap ? animationProgress * 2f : (animationProgress - 0.5f) * 2f;
        float angle = upperFlap ? -90f * phase : 90f * (1f - phase);
        float pivotY = hingeY;

        canvas.save();
        if (upperFlap) {
            canvas.clipRect(0f, 0f, getWidth(), hingeY);
            cardPaint.setColor(TOP_FLAP_COLOR);
            drawRotatedDigit(canvas, previousDigit, angle, pivotY, radius);
        } else {
            canvas.clipRect(0f, hingeY, getWidth(), getHeight());
            cardPaint.setColor(BOTTOM_FLAP_COLOR);
            drawRotatedDigit(canvas, digit, angle, pivotY, radius);
        }
        canvas.restore();
    }

    private void drawRotatedDigit(Canvas canvas, char value, float angle, float pivotY, float radius) {
        transform.reset();
        camera.save();
        camera.rotateX(angle);
        camera.getMatrix(transform);
        camera.restore();

        transform.preTranslate(-getWidth() / 2f, -pivotY);
        transform.postTranslate(getWidth() / 2f, pivotY);
        canvas.concat(transform);
        canvas.drawRoundRect(cardBounds, radius, radius, cardPaint);
        drawDigit(canvas, value);
    }

    private void drawDigit(Canvas canvas, char value) {
        float height = getHeight();
        digitPaint.setTextSize(height * 0.82f);
        digitPaint.getFontMetrics(fontMetrics);
        float baseline = (height - fontMetrics.ascent - fontMetrics.descent) / 2f;
        digitText[0] = value;
        canvas.drawText(digitText, 0, digitText.length, getWidth() / 2f, baseline, digitPaint);
    }

    @Override protected void onDetachedFromWindow() {
        stopFlipAnimation();
        super.onDetachedFromWindow();
    }
}
