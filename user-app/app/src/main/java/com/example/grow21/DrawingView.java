package com.example.grow21;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

/**
 * Enhanced drawing canvas with:
 * - Path stack for undo support
 * - Point tracking for shape detection
 * - Guide path rendering (dotted/faint)
 */
public class DrawingView extends View {

    // User drawing
    private final Paint userPaint;
    private final List<Path> pathStack = new ArrayList<>();
    private final List<List<PointF>> pointsStack = new ArrayList<>();
    private Path currentPath;
    private List<PointF> currentPoints;

    // Guide path
    private final Paint guidePaint;
    private Path guidePath;
    private String guideType; // "straight", "curve", "zigzag", "circle", "square", "triangle"

    public DrawingView(Context context, AttributeSet attrs) {
        super(context, attrs);

        // User drawing paint — soft purple, smooth
        userPaint = new Paint();
        userPaint.setColor(Color.parseColor("#7B61FF")); // primary purple
        userPaint.setStyle(Paint.Style.STROKE);
        userPaint.setStrokeWidth(18f);
        userPaint.setStrokeJoin(Paint.Join.ROUND);
        userPaint.setStrokeCap(Paint.Cap.ROUND);
        userPaint.setAntiAlias(true);

        // Guide paint — light purple, dashed, visible
        guidePaint = new Paint();
        guidePaint.setColor(Color.parseColor("#C5B8FF")); // soft purple guide
        guidePaint.setStyle(Paint.Style.STROKE);
        guidePaint.setStrokeWidth(16f);
        guidePaint.setStrokeJoin(Paint.Join.ROUND);
        guidePaint.setStrokeCap(Paint.Cap.ROUND);
        guidePaint.setAntiAlias(true);
        guidePaint.setPathEffect(new DashPathEffect(new float[]{30f, 18f}, 0f));
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        // Regenerate guide path when size is known
        if (guideType != null) {
            guidePath = generateGuidePath(guideType, w, h);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        // Draw guide path first (behind user drawing)
        if (guidePath != null) {
            canvas.drawPath(guidePath, guidePaint);
        }

        // Draw all completed strokes
        for (Path path : pathStack) {
            canvas.drawPath(path, userPaint);
        }

        // Draw current in-progress stroke
        if (currentPath != null) {
            canvas.drawPath(currentPath, userPaint);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                currentPath = new Path();
                currentPoints = new ArrayList<>();
                currentPath.moveTo(x, y);
                currentPoints.add(new PointF(x, y));
                return true;

            case MotionEvent.ACTION_MOVE:
                if (currentPath != null) {
                    currentPath.lineTo(x, y);
                    currentPoints.add(new PointF(x, y));
                }
                break;

            case MotionEvent.ACTION_UP:
                if (currentPath != null && currentPoints != null) {
                    pathStack.add(currentPath);
                    pointsStack.add(currentPoints);
                    currentPath = null;
                    currentPoints = null;
                }
                break;

            default:
                return false;
        }

        invalidate();
        return true;
    }

    // ==================== PUBLIC API ====================

    /**
     * Undo the last stroke.
     */
    public void undo() {
        if (!pathStack.isEmpty()) {
            pathStack.remove(pathStack.size() - 1);
            pointsStack.remove(pointsStack.size() - 1);
            invalidate();
        }
    }

    /**
     * Clear all strokes.
     */
    public void clear() {
        pathStack.clear();
        pointsStack.clear();
        currentPath = null;
        currentPoints = null;
        invalidate();
    }

    /**
     * Get all tracked points across all strokes (flattened).
     */
    public List<PointF> getAllPoints() {
        List<PointF> all = new ArrayList<>();
        for (List<PointF> stroke : pointsStack) {
            all.addAll(stroke);
        }
        return all;
    }

    /**
     * Check if user has drawn anything.
     */
    public boolean hasDrawing() {
        return !pathStack.isEmpty();
    }

    /**
     * Set the guide path type. Call before view is measured or call invalidate.
     */
    public void setGuideType(String type) {
        this.guideType = type;
        if (getWidth() > 0 && getHeight() > 0) {
            guidePath = generateGuidePath(type, getWidth(), getHeight());
        }
        invalidate();
    }

    /**
     * Clear guide path (for free drawing mode).
     */
    public void clearGuide() {
        guidePath = null;
        guideType = null;
        invalidate();
    }

    // ==================== GUIDE PATH GENERATION ====================

    private Path generateGuidePath(String type, int w, int h) {
        Path path = new Path();
        float padding = w * 0.15f;
        float cx = w / 2f;
        float cy = h / 2f;

        switch (type) {
            case "straight":
                path.moveTo(padding, cy);
                path.lineTo(w - padding, cy);
                break;

            case "curve":
                path.moveTo(padding, cy + h * 0.15f);
                path.cubicTo(
                    cx * 0.6f, cy - h * 0.3f,
                    cx * 1.4f, cy - h * 0.3f,
                    w - padding, cy + h * 0.15f
                );
                break;

            case "zigzag":
                float segW = (w - 2 * padding) / 4f;
                path.moveTo(padding, cy);
                path.lineTo(padding + segW, cy - h * 0.2f);
                path.lineTo(padding + segW * 2, cy + h * 0.2f);
                path.lineTo(padding + segW * 3, cy - h * 0.2f);
                path.lineTo(padding + segW * 4, cy);
                break;

            case "circle":
                float radius = Math.min(w, h) * 0.3f;
                path.addCircle(cx, cy, radius, Path.Direction.CW);
                break;

            case "square":
                float half = Math.min(w, h) * 0.3f;
                path.moveTo(cx - half, cy - half);
                path.lineTo(cx + half, cy - half);
                path.lineTo(cx + half, cy + half);
                path.lineTo(cx - half, cy + half);
                path.close();
                break;

            case "triangle":
                float triH = Math.min(w, h) * 0.3f;
                path.moveTo(cx, cy - triH);
                path.lineTo(cx + triH, cy + triH);
                path.lineTo(cx - triH, cy + triH);
                path.close();
                break;

            default:
                // No guide for unknown types
                return null;
        }

        return path;
    }
}
