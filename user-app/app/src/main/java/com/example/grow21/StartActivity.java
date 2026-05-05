package com.example.grow21;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

import com.example.grow21.models.Child;

public class StartActivity extends AppCompatActivity {

    private TextView tvGreeting;
    private ImageView ivMascot;
    private Button btnStart;
    private SwitchCompat switchProfile;

    private static final String PREFS_NAME = "grow21_prefs";
    private static final String KEY_LOGGED_IN = "is_logged_in";
    private static final String KEY_USER_EMAIL = "user_email";
    private static final String KEY_PARENT_MODE = "is_parent_mode";

    /** Flag to prevent the switch listener from firing during programmatic changes. */
    private boolean isSwitchListenerActive = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);

        tvGreeting = findViewById(R.id.tv_greeting);
        ivMascot = findViewById(R.id.iv_mascot);
        btnStart = findViewById(R.id.btn_start);
        switchProfile = findViewById(R.id.switch_profile);

        // Load child name from SharedPreferences (set by ProfileChooserActivity from backend)
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String childName = prefs.getString("SERVER_CHILD_NAME", null);
        if (childName != null) {
            String greeting = getString(R.string.greeting_format, childName);
            tvGreeting.setText(greeting);
        }

        btnStart.setOnClickListener(v -> {
            Intent intent = new Intent(StartActivity.this, SkillSelectionActivity.class);
            startActivity(intent);
        });

        setupProfileSwitch();
    }

    /**
     * Sets up the profile switch toggle. When toggled to Parent mode,
     * shows a password verification dialog using the existing login credentials.
     * On successful verification, navigates to ParentDashboardActivity.
     */
    private void setupProfileSwitch() {
        // Ensure switch starts in Child mode (unchecked)
        isSwitchListenerActive = false;
        switchProfile.setChecked(false);
        isSwitchListenerActive = true;

        switchProfile.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (!isSwitchListenerActive) return;

            if (isChecked) {
                // User wants to switch to Parent mode — require password
                showParentAuthDialog();
            }
            // Switching back to Child is handled by returning from ParentDashboardActivity
        });
    }

    /**
     * Displays a password verification dialog. Uses the existing user email
     * from SharedPreferences and validates against the User table.
     */
    private void showParentAuthDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_parent_pin, null);

        EditText etPassword = dialogView.findViewById(R.id.et_pin_password);
        Button btnConfirm = dialogView.findViewById(R.id.btn_pin_confirm);
        Button btnCancel = dialogView.findViewById(R.id.btn_pin_cancel);
        Button btnForgot = dialogView.findViewById(R.id.btn_pin_forgot);
        TextView tvTitle = dialogView.findViewById(R.id.tv_pin_title);
        TextView tvMessage = dialogView.findViewById(R.id.tv_pin_message);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .setCancelable(false)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        final boolean[] isResetMode = {false};

        btnForgot.setOnClickListener(v -> {
            isResetMode[0] = true;
            tvTitle.setText("Reset Password");
            tvMessage.setText("Enter a new password for your account.");
            etPassword.setText("");
            etPassword.setHint("New Password");
            btnConfirm.setText("RESET");
            btnForgot.setVisibility(View.GONE);
        });

        btnConfirm.setOnClickListener(v -> {
            String password = etPassword.getText().toString().trim();
            if (TextUtils.isEmpty(password)) {
                Toast.makeText(this, R.string.error_empty_fields, Toast.LENGTH_SHORT).show();
                return;
            }

            SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
            String userEmail = prefs.getString(KEY_USER_EMAIL, "");

            new Thread(() -> {
                try {
                    String endpoint = isResetMode[0] ? "/api/reset-password" : "/api/login";
                    java.net.URL url = new java.net.URL("http://" + BuildConfig.BACKEND_IP + ":5000" + endpoint);
                    java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("Content-Type", "application/json");
                    conn.setDoOutput(true);

                    String json;
                    if (isResetMode[0]) {
                        json = "{\"email\":\"" + userEmail + "\",\"newPassword\":\"" + password + "\"}";
                    } else {
                        json = "{\"email\":\"" + userEmail + "\",\"password\":\"" + password + "\"}";
                    }
                    
                    java.io.OutputStream os = conn.getOutputStream();
                    os.write(json.getBytes());
                    os.flush();
                    os.close();

                    final boolean valid = (conn.getResponseCode() == 200);

                    runOnUiThread(() -> {
                        if (valid) {
                            if (isResetMode[0]) {
                                Toast.makeText(this, "Password updated successfully!", Toast.LENGTH_SHORT).show();
                            }
                            dialog.dismiss();
                            prefs.edit().putBoolean(KEY_PARENT_MODE, true).apply();
                            Intent intent = new Intent(StartActivity.this, ParentDashboardActivity.class);
                            startActivity(intent);
                        } else {
                            if (isResetMode[0]) {
                                Toast.makeText(this, "Failed to reset password. Network error.", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(this, R.string.error_wrong_password, Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                    runOnUiThread(() -> Toast.makeText(StartActivity.this, "Network Error", Toast.LENGTH_SHORT).show());
                }
            }).start();
        });

        btnCancel.setOnClickListener(v -> {
            dialog.dismiss();
            // Reset switch to Child mode without triggering the listener
            isSwitchListenerActive = false;
            switchProfile.setChecked(false);
            isSwitchListenerActive = true;
        });

        dialog.show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        if (!prefs.getBoolean(KEY_LOGGED_IN, false)) {
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
            finish();
            return;
        }

        // Reset switch to Child mode when returning from Parent Dashboard
        isSwitchListenerActive = false;
        switchProfile.setChecked(false);
        isSwitchListenerActive = true;
        prefs.edit().putBoolean(KEY_PARENT_MODE, false).apply();
    }
}
