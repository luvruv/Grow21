package com.example.grow21;

import android.app.Application;
import androidx.appcompat.app.AppCompatDelegate;

public class Grow21Application extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        // Follow device system theme for dark/light mode compatibility
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
    }
}
