package com.example.grow21;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class GameCelebrationUtil {

    public static void celebrate(Activity activity) {
        // Read the sound preference
        android.content.SharedPreferences prefs = activity.getSharedPreferences("grow21_prefs", Context.MODE_PRIVATE);
        boolean soundEnabled = prefs.getBoolean("sound_effects", true);

        triggerHaptics(activity);

        // Only play the tune if sound effects are ON
        if (soundEnabled) {
            playCheerfulTune();
        }

        showConfetti(activity);
    }

    private static void triggerHaptics(Context context) {
        Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator != null && vibrator.hasVibrator()) {
            // A cheerful "da-da-da-DAAA" vibration pattern
            long[] pattern = {0, 100, 50, 100, 50, 100, 50, 300};
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1));
            } else {
                vibrator.vibrate(pattern, -1);
            }
        }
    }

    private static void playCheerfulTune() {
        new Thread(() -> {
            ToneGenerator tone = new ToneGenerator(AudioManager.STREAM_MUSIC, 100);
            try {
                // Cheerful 4-note arpeggio
                tone.startTone(ToneGenerator.TONE_SUP_INTERCEPT, 100);
                Thread.sleep(120);
                tone.startTone(ToneGenerator.TONE_SUP_CONFIRM, 100);
                Thread.sleep(120);
                tone.startTone(ToneGenerator.TONE_PROP_BEEP, 100);
                Thread.sleep(120);
                tone.startTone(ToneGenerator.TONE_PROP_ACK, 400);
                Thread.sleep(400);
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                tone.release();
            }
        }).start();
    }

    private static void showConfetti(Activity activity) {
        ViewGroup rootView = activity.findViewById(android.R.id.content);
        ConfettiView confettiView = new ConfettiView(activity);

        // Add the confetti on top of everything
        rootView.addView(confettiView, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));

        // Let the confetti fall for 3.5 seconds, then cleanly remove it
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            rootView.removeView(confettiView);
        }, 3500);
    }

    /**
     * Lightweight custom view that renders falling colored particles
     */
    private static class ConfettiView extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final List<Particle> particles = new ArrayList<>();
        private final Random random = new Random();
        private final int[] colors = {
                Color.parseColor("#FF3B30"), // Red
                Color.parseColor("#34C759"), // Green
                Color.parseColor("#007AFF"), // Blue
                Color.parseColor("#FFCC00"), // Yellow
                Color.parseColor("#AF52DE"), // Purple
                Color.parseColor("#FF9500")  // Orange
        };

        public ConfettiView(Context context) {
            super(context);
            for (int i = 0; i < 150; i++) { // 150 pieces of confetti
                particles.add(new Particle());
            }
        }

        @Override
        protected void onSizeChanged(int w, int h, int oldw, int oldh) {
            super.onSizeChanged(w, h, oldw, oldh);
            // Spawn particles above the screen randomly
            for (Particle p : particles) {
                p.x = random.nextInt(w);
                p.y = -random.nextInt(Math.max(1, h)); // Start above top edge
                p.speed = 10 + random.nextInt(25);
                p.size = 15 + random.nextInt(20);
                p.color = colors[random.nextInt(colors.length)];
                p.isCircle = random.nextBoolean();
                p.rotationSpeed = -15 + random.nextInt(30);
            }
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            boolean hasActive = false;

            for (Particle p : particles) {
                p.update();
                if (p.y < getHeight()) {
                    hasActive = true;
                }

                paint.setColor(p.color);
                if (p.isCircle) {
                    canvas.drawCircle(p.x, p.y, p.size, paint);
                } else {
                    canvas.save();
                    canvas.translate(p.x, p.y);
                    canvas.rotate(p.rotation);
                    canvas.drawRect(-p.size, -p.size, p.size, p.size, paint);
                    canvas.restore();
                }
            }
            if (hasActive) {
                invalidate(); // Keep refreshing until all pieces fall off screen
            }
        }

        private class Particle {
            float x, y, speed, size, rotation, rotationSpeed;
            int color;
            boolean isCircle;

            void update() {
                y += speed;
                rotation += rotationSpeed;
                x += (float) Math.sin(y / 100f) * 5; // Adds a fluttering wind effect
            }
        }
    }
}