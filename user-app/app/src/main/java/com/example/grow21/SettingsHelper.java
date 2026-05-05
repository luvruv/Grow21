package com.example.grow21;

import android.content.Context;
import android.content.SharedPreferences;

public class SettingsHelper {
    private static final String PREFS_NAME = "Grow21Settings";
    private static final String KEY_VOICE_INSTRUCTIONS = "voice_instructions";
    private static final String KEY_SOUND_EFFECTS = "sound_effects";

    public static void setVoiceInstructionsEnabled(Context context, boolean enabled) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putBoolean(KEY_VOICE_INSTRUCTIONS, enabled).apply();
    }

    public static boolean isVoiceInstructionsEnabled(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(KEY_VOICE_INSTRUCTIONS, true); // Default is true (ON)
    }

    public static void setSoundEffectsEnabled(Context context, boolean enabled) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putBoolean(KEY_SOUND_EFFECTS, enabled).apply();
    }

    public static boolean isSoundEffectsEnabled(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(KEY_SOUND_EFFECTS, true); // Default is true (ON)
    }
}