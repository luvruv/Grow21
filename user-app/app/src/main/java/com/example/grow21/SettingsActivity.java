package com.example.grow21;

import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.SwitchCompat;
import androidx.cardview.widget.CardView;

import android.content.Intent;
import android.widget.RadioGroup;

public class SettingsActivity extends AppCompatActivity {

    private SwitchCompat switchVoice, switchSound, switchLargeText;
    private CardView cardLogout;
    private RadioGroup themeGroup;

    private SharedPreferences prefs;

    private static final String PREFS_NAME = "grow21_prefs";

    private static final String KEY_VOICE = "voice_instructions";
    private static final String KEY_SOUND = "sound_effects";
    private static final String KEY_LARGE_TEXT = "large_text_mode";
    private static final String KEY_LOGGED_IN = "is_logged_in";
    private static final String KEY_PARENT_MODE = "is_parent_mode";
    private static final String KEY_THEME = "theme_mode";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        // 🔒 PARENT ONLY ACCESS
        boolean isParentMode = prefs.getBoolean(KEY_PARENT_MODE, false);
        if (!isParentMode) {
            finish();
            return;
        }

        setContentView(R.layout.activity_settings);

        switchVoice = findViewById(R.id.switch_voice);
        switchSound = findViewById(R.id.switch_sound);
        switchLargeText = findViewById(R.id.switch_large_text);
        cardLogout = findViewById(R.id.card_logout);
        themeGroup = findViewById(R.id.theme_group);

        // 🔊 Load values
        switchVoice.setChecked(prefs.getBoolean(KEY_VOICE, false));
        switchSound.setChecked(prefs.getBoolean(KEY_SOUND, true));
        switchLargeText.setChecked(prefs.getBoolean(KEY_LARGE_TEXT, false));

        // 🔁 Save toggles
        switchVoice.setOnCheckedChangeListener((b, val) ->
                prefs.edit().putBoolean(KEY_VOICE, val).apply());

        switchSound.setOnCheckedChangeListener((b, val) ->
                prefs.edit().putBoolean(KEY_SOUND, val).apply());

        switchLargeText.setOnCheckedChangeListener((b, val) -> {
            prefs.edit().putBoolean(KEY_LARGE_TEXT, val).apply();

            Configuration config = getResources().getConfiguration();
            config.fontScale = val ? 1.3f : 1.0f;
            getResources().updateConfiguration(config, getResources().getDisplayMetrics());
            recreate();
        });

        // 🎨 THEME SETUP
        int savedTheme = prefs.getInt(KEY_THEME, 0); // 0=system,1=light,2=dark
        if (savedTheme == 0) themeGroup.check(R.id.theme_system);
        else if (savedTheme == 1) themeGroup.check(R.id.theme_light);
        else themeGroup.check(R.id.theme_dark);

        themeGroup.setOnCheckedChangeListener((group, checkedId) -> {
            int mode = 0;

            if (checkedId == R.id.theme_light) mode = 1;
            else if (checkedId == R.id.theme_dark) mode = 2;

            prefs.edit().putInt(KEY_THEME, mode).apply();
            applyTheme(mode);
        });

        // 🔓 Logout
        cardLogout.setOnClickListener(v -> showLogoutDialog());
    }

    // 🎨 APPLY THEME
    private void applyTheme(int mode) {
        if (mode == 2) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else if (mode == 1) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        }
    }

    // 🚪 LOGOUT (UPDATED)
    private void showLogoutDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.dialog_logout_title)
                .setMessage(R.string.dialog_logout_message)
                .setPositiveButton(R.string.btn_confirm, (dialog, which) -> {

                    // 🔥 Clear all stored data
                    prefs.edit().clear().apply();

                    // 🚀 Redirect to login & clear backstack
                    Intent intent = new Intent(SettingsActivity.this, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton(R.string.btn_cancel, null)
                .show();
    }
}
