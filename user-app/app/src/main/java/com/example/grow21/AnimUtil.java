package com.example.grow21;

import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

/**
 * Utility for micro-interaction animations.
 * Apply to any clickable view for scale press feedback.
 */
public class AnimUtil {

    private AnimUtil() {}

    /**
     * Adds press/release scale animation to a view.
     * Call this once in onCreate for each interactive element.
     */
    public static void addPressEffect(View view) {
        view.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    Animation press = AnimationUtils.loadAnimation(v.getContext(), R.anim.btn_press);
                    press.setFillAfter(true);
                    v.startAnimation(press);
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    Animation release = AnimationUtils.loadAnimation(v.getContext(), R.anim.btn_release);
                    release.setFillAfter(true);
                    v.startAnimation(release);
                    break;
            }
            // Don't consume the event — let onClick still fire
            return false;
        });
    }
}
