package com.example.grow21;

import android.graphics.PointF;

import java.util.List;

/**
 * Modular shape and path detection utility.
 * Uses forgiving thresholds designed for children.
 * Easy to extend for new shapes — just add a new detect method.
 */
public class ShapeDetector {

    private static final float PASS_THRESHOLD = 0.6f;

    private ShapeDetector() {
        // Utility class
    }

    /**
     * Evaluate drawing accuracy based on shape type.
     * Returns a score between 0.0 and 1.0.
     */
    public static float evaluate(List<PointF> points, String shapeOrPath) {
        if (points == null || points.size() < 5) return 0f;

        switch (shapeOrPath) {
            case "circle":
                return detectCircle(points);
            case "square":
                return detectSquare(points);
            case "triangle":
                return detectTriangle(points);
            case "straight":
                return detectStraightLine(points);
            case "curve":
                return detectCurve(points);
            case "zigzag":
                return detectZigzag(points);
            default:
                // For unknown types, give full credit for effort
                return 1.0f;
        }
    }

    /**
     * Check if result passes the kid-friendly threshold.
     */
    public static boolean passes(float accuracy) {
        return accuracy >= PASS_THRESHOLD;
    }

    public static float getThreshold() {
        return PASS_THRESHOLD;
    }

    // ==================== SHAPE DETECTION ====================

    /**
     * Circle detection:
     * 1. Find centroid of all points
     * 2. Compute average radius
     * 3. Measure how consistently each point sits at that radius
     * 4. Low variance = good circle
     */
    private static float detectCircle(List<PointF> points) {
        // Find centroid
        float cx = 0, cy = 0;
        for (PointF p : points) {
            cx += p.x;
            cy += p.y;
        }
        cx /= points.size();
        cy /= points.size();

        // Compute average distance from center (radius)
        float avgRadius = 0;
        for (PointF p : points) {
            avgRadius += distance(p.x, p.y, cx, cy);
        }
        avgRadius /= points.size();

        if (avgRadius < 10) return 0f; // Too small

        // Compute standard deviation of distances
        float variance = 0;
        for (PointF p : points) {
            float dist = distance(p.x, p.y, cx, cy);
            float diff = dist - avgRadius;
            variance += diff * diff;
        }
        variance /= points.size();
        float stdDev = (float) Math.sqrt(variance);

        // Coefficient of variation (lower = better circle)
        float cv = stdDev / avgRadius;

        // Convert to 0-1 score. CV of 0 = perfect, CV of 0.5+ = bad
        float score = Math.max(0f, 1f - (cv * 2f));
        return Math.min(1f, score);
    }

    /**
     * Square detection:
     * Check if points form a roughly rectangular bounding shape
     * with most points near the edges (not center).
     */
    private static float detectSquare(List<PointF> points) {
        float minX = Float.MAX_VALUE, maxX = Float.MIN_VALUE;
        float minY = Float.MAX_VALUE, maxY = Float.MIN_VALUE;

        for (PointF p : points) {
            minX = Math.min(minX, p.x);
            maxX = Math.max(maxX, p.x);
            minY = Math.min(minY, p.y);
            maxY = Math.max(maxY, p.y);
        }

        float width = maxX - minX;
        float height = maxY - minY;

        if (width < 20 || height < 20) return 0f;

        // Aspect ratio check: should be close to 1:1
        float aspect = Math.min(width, height) / Math.max(width, height);
        float aspectScore = aspect; // 1.0 = perfect square

        // Edge proximity: points should be near edges
        int nearEdge = 0;
        float edgeThreshold = Math.min(width, height) * 0.25f;
        for (PointF p : points) {
            float dLeft = p.x - minX;
            float dRight = maxX - p.x;
            float dTop = p.y - minY;
            float dBottom = maxY - p.y;
            float minDist = Math.min(Math.min(dLeft, dRight), Math.min(dTop, dBottom));
            if (minDist < edgeThreshold) nearEdge++;
        }
        float edgeScore = (float) nearEdge / points.size();

        return (aspectScore * 0.4f + edgeScore * 0.6f);
    }

    /**
     * Triangle detection:
     * Look for 3 approximate corners in the point set.
     */
    private static float detectTriangle(List<PointF> points) {
        float minX = Float.MAX_VALUE, maxX = Float.MIN_VALUE;
        float minY = Float.MAX_VALUE, maxY = Float.MIN_VALUE;

        for (PointF p : points) {
            minX = Math.min(minX, p.x);
            maxX = Math.max(maxX, p.x);
            minY = Math.min(minY, p.y);
            maxY = Math.max(maxY, p.y);
        }

        float width = maxX - minX;
        float height = maxY - minY;

        if (width < 20 || height < 20) return 0f;

        // For a simple triangle: check if there's a top point and two bottom points
        // Count points in top third vs bottom third
        float thirdY = height / 3f;
        int topPoints = 0, bottomPoints = 0;
        for (PointF p : points) {
            if (p.y - minY < thirdY) topPoints++;
            if (maxY - p.y < thirdY) bottomPoints++;
        }

        // Triangle should have fewer points at top, more at bottom (or vice versa)
        float ratio = (float) Math.min(topPoints, bottomPoints) /
                      Math.max(topPoints, bottomPoints);
        // Good triangle: ratio around 0.3-0.5 (asymmetric distribution)
        float triScore = ratio < 0.8f ? (1f - ratio * 0.5f) : 0.5f;

        // Size check
        float sizeScore = Math.min(1f, (width * height) / 5000f);

        return Math.min(1f, triScore * 0.6f + sizeScore * 0.4f);
    }

    // ==================== PATH DETECTION ====================

    /**
     * Straight line: points should have low Y variance (horizontal)
     * or low X variance (vertical), or follow a consistent slope.
     */
    private static float detectStraightLine(List<PointF> points) {
        // Use linear regression to measure straightness
        float sumX = 0, sumY = 0, sumXY = 0, sumX2 = 0;
        int n = points.size();

        for (PointF p : points) {
            sumX += p.x;
            sumY += p.y;
            sumXY += p.x * p.y;
            sumX2 += p.x * p.x;
        }

        float meanX = sumX / n;
        float meanY = sumY / n;

        // Calculate R-squared (coefficient of determination)
        float ssTot = 0, ssRes = 0;
        float denominator = (n * sumX2 - sumX * sumX);

        if (Math.abs(denominator) < 0.001f) {
            // Vertical line — check X variance
            float xVar = 0;
            for (PointF p : points) {
                float diff = p.x - meanX;
                xVar += diff * diff;
            }
            xVar /= n;
            float xStd = (float) Math.sqrt(xVar);
            return Math.max(0f, 1f - (xStd / 50f));
        }

        float slope = (n * sumXY - sumX * sumY) / denominator;
        float intercept = (sumY - slope * sumX) / n;

        for (PointF p : points) {
            float predicted = slope * p.x + intercept;
            float residual = p.y - predicted;
            ssRes += residual * residual;
            float diff = p.y - meanY;
            ssTot += diff * diff;
        }

        if (ssTot < 0.001f) return 1f; // All points same Y = perfect line
        float rSquared = 1f - (ssRes / ssTot);
        return Math.max(0f, rSquared);
    }

    /**
     * Curve: should have some curvature but be smooth.
     * Check that direction changes gradually, not abruptly.
     */
    private static float detectCurve(List<PointF> points) {
        if (points.size() < 10) return 0.5f;

        // Check for smooth direction changes
        int smoothChanges = 0;
        int totalSegments = 0;

        for (int i = 2; i < points.size(); i++) {
            float dx1 = points.get(i - 1).x - points.get(i - 2).x;
            float dy1 = points.get(i - 1).y - points.get(i - 2).y;
            float dx2 = points.get(i).x - points.get(i - 1).x;
            float dy2 = points.get(i).y - points.get(i - 1).y;

            float angle1 = (float) Math.atan2(dy1, dx1);
            float angle2 = (float) Math.atan2(dy2, dx2);
            float angleDiff = Math.abs(angle2 - angle1);
            if (angleDiff > Math.PI) angleDiff = (float) (2 * Math.PI - angleDiff);

            // Smooth = small angle change per segment
            if (angleDiff < 0.3f) smoothChanges++;
            totalSegments++;
        }

        if (totalSegments == 0) return 0.5f;
        float smoothness = (float) smoothChanges / totalSegments;

        // Must not be a straight line (need some total direction change)
        float totalAngleChange = 0;
        for (int i = 2; i < points.size(); i++) {
            float dx1 = points.get(i - 1).x - points.get(i - 2).x;
            float dy1 = points.get(i - 1).y - points.get(i - 2).y;
            float dx2 = points.get(i).x - points.get(i - 1).x;
            float dy2 = points.get(i).y - points.get(i - 1).y;
            float angle1 = (float) Math.atan2(dy1, dx1);
            float angle2 = (float) Math.atan2(dy2, dx2);
            float diff = angle2 - angle1;
            if (diff > Math.PI) diff -= 2 * Math.PI;
            if (diff < -Math.PI) diff += 2 * Math.PI;
            totalAngleChange += Math.abs(diff);
        }

        float hasCurvature = Math.min(1f, totalAngleChange / 1.0f);

        return smoothness * 0.6f + hasCurvature * 0.4f;
    }

    /**
     * Zigzag: should have sharp direction reversals.
     */
    private static float detectZigzag(List<PointF> points) {
        if (points.size() < 10) return 0.3f;

        int sharpChanges = 0;
        int totalSegments = 0;

        for (int i = 2; i < points.size(); i += 3) {
            float dx1 = points.get(i - 1).x - points.get(i - 2).x;
            float dy1 = points.get(i - 1).y - points.get(i - 2).y;
            float dx2 = points.get(i).x - points.get(i - 1).x;
            float dy2 = points.get(i).y - points.get(i - 1).y;

            // Check for Y-direction reversal (zigzag goes up/down)
            if ((dy1 > 0 && dy2 < 0) || (dy1 < 0 && dy2 > 0)) {
                sharpChanges++;
            }
            totalSegments++;
        }

        if (totalSegments == 0) return 0.3f;

        // At least a few direction reversals needed
        float zigzagScore = Math.min(1f, (float) sharpChanges / 2f);

        // Also check that the overall path moves forward (left to right)
        float firstX = points.get(0).x;
        float lastX = points.get(points.size() - 1).x;
        float progressScore = Math.abs(lastX - firstX) > 50 ? 1f : 0.5f;

        return zigzagScore * 0.7f + progressScore * 0.3f;
    }

    // ==================== UTILITY ====================

    private static float distance(float x1, float y1, float x2, float y2) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        return (float) Math.sqrt(dx * dx + dy * dy);
    }
}
