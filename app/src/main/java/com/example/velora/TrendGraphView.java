package com.example.velora;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

public class TrendGraphView extends View {

    private Paint linePaint, pointPaint, textPaint, gridPaint, areaPaint, statusPaint, glowPaint, fillPaint;
    private Path path, fillPath;
    private List<Integer> cycleLengths = new ArrayList<>();
    private int averageCycleLength = 28;
    private float pointSpacing = 160f;

    public TrendGraphView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        linePaint.setColor(Color.parseColor("#FF4D67")); // Velora Pink
        linePaint.setStrokeWidth(8f);
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeJoin(Paint.Join.ROUND);
        linePaint.setStrokeCap(Paint.Cap.ROUND);

        fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        fillPaint.setStyle(Paint.Style.FILL);

        pointPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        pointPaint.setStyle(Paint.Style.FILL);

        glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        glowPaint.setStyle(Paint.Style.FILL);

        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.parseColor("#7F8C8D"));
        textPaint.setTextSize(24f);
        textPaint.setTextAlign(Paint.Align.CENTER);

        statusPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        statusPaint.setTextSize(26f);
        statusPaint.setTextAlign(Paint.Align.CENTER);
        statusPaint.setFakeBoldText(true);

        gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        gridPaint.setColor(Color.parseColor("#F5F5F5"));
        gridPaint.setStrokeWidth(3f);

        areaPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        areaPaint.setColor(Color.parseColor("#10000000")); // Very light overlay
        areaPaint.setStyle(Paint.Style.FILL);

        path = new Path();
        fillPath = new Path();
    }

    public void setData(List<Integer> lengths, int avg) {
        this.cycleLengths = lengths != null ? lengths : new ArrayList<>();
        this.averageCycleLength = avg;
        updateViewWidth();
        invalidate();
    }

    private void updateViewWidth() {
        int screenWidth = getResources().getDisplayMetrics().widthPixels - 64;
        int finalWidth = screenWidth;

        if (cycleLengths != null && !cycleLengths.isEmpty()) {
            float paddingX = 100f;
            int calculatedWidth = (int) (paddingX * 2 + (Math.max(1, cycleLengths.size() - 1)) * pointSpacing);
            finalWidth = Math.max(calculatedWidth, screenWidth);
        }

        ViewGroup.LayoutParams params = getLayoutParams();
        if (params != null) {
            params.width = finalWidth;
            setLayoutParams(params);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float w = getWidth();
        float h = getHeight();
        float paddingX = 100f;
        float paddingY = 100f;
        float graphHeight = h - 2 * paddingY;

        // Ensure width is valid
        if (w <= 0 || h <= 0) return;

        // Draw horizontal grid lines
        for (int i = 0; i <= 4; i++) {
            float y = paddingY + i * graphHeight / 4;
            canvas.drawLine(0, y, w, y, gridPaint);
        }

        if (cycleLengths == null || cycleLengths.isEmpty()) {
            statusPaint.setColor(Color.LTGRAY);
            canvas.drawText("Log 2+ periods to see trends", w / 2, h / 2, statusPaint);
            return;
        }

        float minY = 15f;
        float maxY = 45f;
        float rangeY = maxY - minY;

        float stepX = (cycleLengths.size() > 1) ? (w - 2 * paddingX) / (cycleLengths.size() - 1) : 0;
        float startX = (cycleLengths.size() == 1) ? w / 2 : paddingX;

        // Draw Normal Range Area
        float normalValLow = 21f;
        float normalValHigh = 35f;
        float normalYHigh = paddingY + graphHeight - ((normalValHigh - minY) / rangeY) * graphHeight;
        float normalYLow = paddingY + graphHeight - ((normalValLow - minY) / rangeY) * graphHeight;
        canvas.drawRect(0, normalYHigh, w, normalYLow, areaPaint);

        // Prepare points
        List<Float> xPoints = new ArrayList<>();
        List<Float> yPoints = new ArrayList<>();
        for (int i = 0; i < cycleLengths.size(); i++) {
            xPoints.add(startX + i * stepX);
            float val = cycleLengths.get(i);
            yPoints.add(paddingY + graphHeight - ((val - minY) / rangeY) * graphHeight);
        }

        // Draw Smooth Curve and Gradient Fill
        if (xPoints.size() > 1) {
            path.reset();
            fillPath.reset();
            path.moveTo(xPoints.get(0), yPoints.get(0));
            fillPath.moveTo(xPoints.get(0), h);
            fillPath.lineTo(xPoints.get(0), yPoints.get(0));

            for (int i = 0; i < xPoints.size() - 1; i++) {
                float x1 = xPoints.get(i);
                float y1 = yPoints.get(i);
                float x2 = xPoints.get(i + 1);
                float y2 = yPoints.get(i + 1);
                float cx = (x1 + x2) / 2;
                path.cubicTo(cx, y1, cx, y2, x2, y2);
                fillPath.cubicTo(cx, y1, cx, y2, x2, y2);
            }

            fillPath.lineTo(xPoints.get(xPoints.size() - 1), h);
            fillPath.close();

            // Gradient for fill
            fillPaint.setShader(new LinearGradient(0, paddingY, 0, h, 
                Color.parseColor("#40FF4D67"), Color.TRANSPARENT, Shader.TileMode.CLAMP));
            canvas.drawPath(fillPath, fillPaint);
            canvas.drawPath(path, linePaint);
        }

        // Draw Points and Labels
        for (int i = 0; i < cycleLengths.size(); i++) {
            float x = xPoints.get(i);
            float y = yPoints.get(i);
            int val = cycleLengths.get(i);

            boolean isAbnormal = val < 21 || val > 35;

            if (isAbnormal) {
                glowPaint.setColor(Color.parseColor("#FFCC00"));
                glowPaint.setAlpha(60);
                canvas.drawCircle(x, y, 25f, glowPaint);
                pointPaint.setColor(Color.parseColor("#FFA500"));
                canvas.drawCircle(x, y, 14f, pointPaint);
                statusPaint.setColor(Color.parseColor("#D35400"));
                canvas.drawText("!", x, y + 8f, statusPaint);
            } else {
                pointPaint.setColor(Color.parseColor("#FF4D67"));
                canvas.drawCircle(x, y, 12f, pointPaint);
                pointPaint.setColor(Color.WHITE);
                canvas.drawCircle(x, y, 6f, pointPaint);
            }
            
            canvas.drawText(val + " d", x, paddingY + graphHeight + 50f, textPaint);
        }
    }
}
