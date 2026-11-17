package com.example.velora;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;

import androidx.annotation.Nullable;

public class CycleCircleView extends View {

    private final Paint dotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint subTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint dropPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint indicatorPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path dropPath = new Path();

    private int cycleLength = 28;
    private int periodDuration = 5;
    private int currentDay = 1;
    private int ovulationDay = 14;
    private String nextPeriodStr = "--";
    private String ovulationStr = "--";
    private String fertileWindowStr = "--";
    private String phaseName = "Menstruation";

    private final int colorWhite = Color.WHITE;
    
    // Phase Colors
    private final int colorRed = Color.parseColor("#FF4D4D");      // Menstruation
    private final int colorGreen = Color.parseColor("#4CAF50");    // Ovulation
    private final int colorPurple = Color.parseColor("#9C27B0");   // Luteal
    private final int colorLightBlue = Color.parseColor("#03A9F4"); // Follicular
    private final int colorFertile = Color.parseColor("#FFEB3B");  // Fertile (Yellow)

    public CycleCircleView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        dotPaint.setStyle(Paint.Style.FILL);

        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(dpToPx(1.5f));
        borderPaint.setColor(Color.WHITE);

        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setFakeBoldText(true);

        subTextPaint.setTextAlign(Paint.Align.CENTER);

        dropPaint.setStyle(Paint.Style.FILL);

        indicatorPaint.setStyle(Paint.Style.STROKE);
        indicatorPaint.setStrokeWidth(dpToPx(2f));
    }

    private float dpToPx(float dp) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics());
    }

    private float spToPx(float sp) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp, getResources().getDisplayMetrics());
    }

    public void setCycleData(int cycleLength, int periodDuration, int currentDay, int ovulationDay, String phase, String nextPeriod, String ovulation, String fertile) {
        this.cycleLength = (cycleLength > 0) ? cycleLength : 28;
        this.periodDuration = periodDuration;
        this.currentDay = ((currentDay - 1) % this.cycleLength + this.cycleLength) % this.cycleLength + 1;
        this.ovulationDay = ovulationDay;
        this.phaseName = phase;
        this.nextPeriodStr = nextPeriod;
        this.ovulationStr = ovulation;
        this.fertileWindowStr = fertile;
        invalidate();
    }

    private int getPhaseColor(int day) {
        if (day <= periodDuration) return colorRed;
        if (day == ovulationDay) return colorGreen;
        if (isFertile(day)) return colorFertile;
        if (day < ovulationDay) return colorLightBlue;
        return colorPurple;
    }

    private boolean isFertile(int day) {
        // Fertile window is typically 5 days before ovulation plus ovulation day
        return day >= (ovulationDay - 5) && day <= (ovulationDay + 1);
    }

    private boolean isSafe(int day) {
        // Safe days are typically outside the fertile window and not during menstruation (for clarity)
        return !isFertile(day) && day > periodDuration;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float w = getWidth();
        float h = getHeight();
        float cx = w / 2f;
        float cy = h / 2f;
        
        float availableSize = Math.min(w - getPaddingLeft() - getPaddingRight(), h - getPaddingTop() - getPaddingBottom());
        float radius = availableSize * 0.38f;
        
        float maxDotRadius = (float) (radius * Math.PI / cycleLength) * 0.6f;
        float dotRadius = Math.max(dpToPx(4), Math.min(maxDotRadius, dpToPx(10)));

        // Draw all day dots
        for (int i = 1; i <= cycleLength; i++) {
            float angleDeg = (i - 1) * (360f / cycleLength) - 90;
            double angleRad = Math.toRadians(angleDeg);
            float dx = cx + (float) (radius * Math.cos(angleRad));
            float dy = cy + (float) (radius * Math.sin(angleRad));

            int phaseColor = getPhaseColor(i);
            
            // Highlight fertile window with a background glow or stroke
            if (isFertile(i)) {
                indicatorPaint.setColor(colorFertile);
                indicatorPaint.setAlpha(100);
                canvas.drawCircle(dx, dy, dotRadius * 1.4f, indicatorPaint);
            }

            dotPaint.setColor(phaseColor);
            canvas.drawCircle(dx, dy, dotRadius, dotPaint);
            
            if (i == currentDay) {
                canvas.drawCircle(dx, dy, dotRadius, borderPaint);
            }

            textPaint.setColor(i == currentDay || isFertile(i) ? Color.BLACK : colorWhite);
            if (phaseColor == colorPurple || phaseColor == colorRed || phaseColor == colorLightBlue || phaseColor == colorGreen) {
                textPaint.setColor(colorWhite);
            } else {
                textPaint.setColor(Color.BLACK);
            }

            textPaint.setTextSize(dotRadius * 1.1f); 
            Paint.FontMetrics fm = textPaint.getFontMetrics();
            float textOffset = -(fm.ascent + fm.descent) / 2;
            canvas.drawText(String.valueOf(i), dx, dy + textOffset, textPaint);
        }

        // Droplet for current day
        float currentAngleDeg = (currentDay - 1) * (360f / cycleLength) - 90;
        double currentAngleRad = Math.toRadians(currentAngleDeg);
        float curX = cx + (float) (radius * Math.cos(currentAngleRad));
        float curY = cy + (float) (radius * Math.sin(currentAngleRad));
        
        float dropSize = dotRadius * 2.2f;
        int currentPhaseColor = getPhaseColor(currentDay);
        dropPaint.setColor(currentPhaseColor);
        drawDropletAt(canvas, curX, curY, dropSize, currentAngleDeg);
        
        textPaint.setColor(currentPhaseColor == colorFertile ? Color.BLACK : colorWhite);
        textPaint.setTextSize(dropSize * 0.6f);
        Paint.FontMetrics fmDrop = textPaint.getFontMetrics();
        float dropTextOffset = -(fmDrop.ascent + fmDrop.descent) / 2;
        canvas.drawText(String.valueOf(currentDay), curX, curY + dropTextOffset, textPaint);

        // Center Content
        drawCenterUI(canvas, cx, cy, radius);
    }

    private void drawCenterUI(Canvas canvas, float cx, float cy, float radius) {
        // Phase Name
        textPaint.setColor(Color.BLACK);
        textPaint.setTextSize(spToPx(16f));
        textPaint.setFakeBoldText(true);
        canvas.drawText(phaseName, cx, cy - radius * 0.45f, textPaint);
        
        // Pregnancy Chance / Safe Status
        String chanceStr;
        int chanceColor;
        if (isFertile(currentDay)) {
            chanceStr = "High Chance of Pregnancy";
            chanceColor = Color.parseColor("#FF9800"); // Orange
        } else if (currentDay <= periodDuration) {
            chanceStr = "Menstruation - Low Chance";
            chanceColor = colorRed;
        } else {
            chanceStr = "Safe Day - Low Chance";
            chanceColor = colorGreen;
        }
        
        subTextPaint.setTextSize(spToPx(11f));
        subTextPaint.setColor(chanceColor);
        subTextPaint.setFakeBoldText(true);
        canvas.drawText(chanceStr.toUpperCase(), cx, cy - radius * 0.32f, subTextPaint);

        // Day Number
        textPaint.setTextSize(spToPx(64f));
        textPaint.setColor(Color.BLACK);
        canvas.drawText(String.valueOf(currentDay), cx, cy + radius * 0.05f, textPaint);
        
        // "Cycle Day" label
        textPaint.setTextSize(spToPx(12f));
        textPaint.setColor(Color.GRAY);
        textPaint.setFakeBoldText(false);
        canvas.drawText("CYCLE DAY", cx, cy + radius * 0.18f, textPaint);

        // Info Rows
        float infoY = cy + radius * 0.45f;
        float infoSpacing = spToPx(20f);

        subTextPaint.setTextSize(spToPx(13f));
        subTextPaint.setFakeBoldText(false);
        
        // Next Period Row
        subTextPaint.setColor(Color.GRAY);
        subTextPaint.setTextAlign(Paint.Align.RIGHT);
        canvas.drawText("Next Period: ", cx - dpToPx(4), infoY, subTextPaint);
        
        subTextPaint.setColor(colorRed);
        subTextPaint.setFakeBoldText(true);
        subTextPaint.setTextAlign(Paint.Align.LEFT);
        canvas.drawText(nextPeriodStr, cx + dpToPx(4), infoY, subTextPaint);
        
        // Ovulation Row
        subTextPaint.setColor(Color.GRAY);
        subTextPaint.setFakeBoldText(false);
        subTextPaint.setTextAlign(Paint.Align.RIGHT);
        canvas.drawText("Ovulation: ", cx - dpToPx(4), infoY + infoSpacing, subTextPaint);
        
        subTextPaint.setColor(colorGreen);
        subTextPaint.setFakeBoldText(true);
        subTextPaint.setTextAlign(Paint.Align.LEFT);
        canvas.drawText(ovulationStr, cx + dpToPx(4), infoY + infoSpacing, subTextPaint);

        // Fertile Window Row
        subTextPaint.setColor(Color.GRAY);
        subTextPaint.setFakeBoldText(false);
        subTextPaint.setTextAlign(Paint.Align.RIGHT);
        canvas.drawText("Fertile Window: ", cx - dpToPx(4), infoY + infoSpacing * 2, subTextPaint);
        
        subTextPaint.setColor(Color.parseColor("#FBC02D")); // Darker Yellow
        subTextPaint.setFakeBoldText(true);
        subTextPaint.setTextAlign(Paint.Align.LEFT);
        canvas.drawText(fertileWindowStr, cx + dpToPx(4), infoY + infoSpacing * 2, subTextPaint);
        
        // Reset subTextPaint
        subTextPaint.setFakeBoldText(false);
        subTextPaint.setTextAlign(Paint.Align.CENTER);
    }

    private void drawDropletAt(Canvas canvas, float x, float y, float size, float angleDegrees) {
        canvas.save();
        canvas.translate(x, y);
        canvas.rotate(angleDegrees + 90);
        dropPath.reset();
        dropPath.moveTo(0, -size);
        dropPath.cubicTo(size * 0.8f, 0, size * 0.8f, size, 0, size);
        dropPath.cubicTo(-size * 0.8f, size, -size * 0.8f, 0, 0, -size);
        dropPath.close();
        canvas.drawPath(dropPath, dropPaint);
        canvas.restore();
    }
}
