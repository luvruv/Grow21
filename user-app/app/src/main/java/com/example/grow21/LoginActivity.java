package com.example.grow21;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

public class LoginActivity extends AppCompatActivity {

    private EditText etEmail, etPassword;
    private Button btnLogin;
    private ProgressBar progressLogin;
    private TextView tvSignupLink;
    private TextView tvForgotPassword;
    private SharedPreferences prefs;

    private static final String PREFS_NAME = "grow21_prefs";
    private static final String KEY_LOGGED_IN = "is_logged_in";
    private static final String KEY_USER_EMAIL = "user_email";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        // If previously logged in, validate session against backend first.
        if (prefs.getBoolean(KEY_LOGGED_IN, false)) {
            setContentView(R.layout.activity_login);
            bindViews();
            showLoading(true);

            new Thread(() -> {
                boolean backendReachable = false;
                try {
                    java.net.URL url = new java.net.URL("http://" + BuildConfig.BACKEND_IP + ":5000/api/validate");
                    java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("Content-Type", "application/json");
                    conn.setDoOutput(true);
                    conn.setConnectTimeout(3000);
                    conn.setReadTimeout(3000);

                    String email = prefs.getString(KEY_USER_EMAIL, "");
                    org.json.JSONObject json = new org.json.JSONObject();
                    json.put("token", android.util.Base64.encodeToString(email.getBytes(), android.util.Base64.NO_WRAP));

                    java.io.OutputStream os = conn.getOutputStream();
                    os.write(json.toString().getBytes("UTF-8"));
                    os.flush();
                    os.close();

                    backendReachable = (conn.getResponseCode() == 200);
                } catch (Exception e) {
                    backendReachable = false;
                }

                final boolean valid = backendReachable;
                runOnUiThread(() -> {
                    showLoading(false);
                    if (valid) {
                        Intent intent = new Intent(LoginActivity.this, ProfileChooserActivity.class);
                        startActivity(intent);
                        finish();
                    } else {
                        prefs.edit().clear().apply();
                        Toast.makeText(LoginActivity.this,
                                "Session expired or server unavailable. Please log in again.",
                                Toast.LENGTH_LONG).show();
                    }
                });
            }).start();
            return;
        }

        setContentView(R.layout.activity_login);
        bindViews();

        btnLogin.setOnClickListener(v -> attemptLogin());

        tvSignupLink.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, SignupActivity.class);
            startActivity(intent);
        });

        tvForgotPassword.setOnClickListener(v -> showForgotPasswordDialog());
    }

    private void bindViews() {
        etEmail = findViewById(R.id.et_email);
        etPassword = findViewById(R.id.et_password);
        btnLogin = findViewById(R.id.btn_login);
        progressLogin = findViewById(R.id.progress_login);
        tvSignupLink = findViewById(R.id.tv_signup_link);
        tvForgotPassword = findViewById(R.id.tv_forgot_password);
    }

    private void attemptLogin() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            Toast.makeText(this, R.string.error_empty_fields, Toast.LENGTH_SHORT).show();
            return;
        }

        showLoading(true);

        new Thread(() -> {
            try {
                java.net.URL url = new java.net.URL("http://" + BuildConfig.BACKEND_IP + ":5000/api/login");
                android.util.Log.d("Login", "Connecting to: " + url.toString());

                java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);

                org.json.JSONObject jsonParam = new org.json.JSONObject();
                jsonParam.put("email", email);
                jsonParam.put("password", password);
                String payload = jsonParam.toString();

                java.io.OutputStream os = conn.getOutputStream();
                os.write(payload.getBytes("UTF-8"));
                os.flush();
                os.close();

                int responseCode = conn.getResponseCode();
                final boolean valid = (responseCode == 200);

                java.io.InputStream stream = valid ? conn.getInputStream() : conn.getErrorStream();
                java.util.Scanner scanner = new java.util.Scanner(stream).useDelimiter("\\A");
                String responseBody = scanner.hasNext() ? scanner.next() : "";

                int parsedParentId = -1;
                if (valid) {
                    try {
                        org.json.JSONObject jsonResponse = new org.json.JSONObject(responseBody);
                        parsedParentId = jsonResponse.getJSONObject("user").getInt("refId");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                final int finalParentId = parsedParentId;
                final String finalResponseBody = responseBody;
                final int finalResponseCode = responseCode;

                runOnUiThread(() -> {
                    showLoading(false);
                    if (valid) {
                        prefs.edit()
                                .putBoolean(KEY_LOGGED_IN, true)
                                .putString(KEY_USER_EMAIL, email)
                                .putInt("SERVER_PARENT_ID", finalParentId)
                                .apply();
                        Intent intent = new Intent(LoginActivity.this, ProfileChooserActivity.class);
                        startActivity(intent);
                        finish();
                    } else {
                        Toast.makeText(LoginActivity.this,
                                "Login failed (Code " + finalResponseCode + "): " + finalResponseBody,
                                Toast.LENGTH_LONG).show();
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
                final String errMsg = e.getMessage();
                runOnUiThread(() -> {
                    showLoading(false);
                    Toast.makeText(LoginActivity.this, "Network Error: " + errMsg, Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    /**
     * Two-step reset using the existing dialog_parent_pin layout:
     * (1) ask for the email; (2) ask for the new password; call /api/reset-password.
     */
    private void showForgotPasswordDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_parent_pin, null);

        TextView tvTitle    = dialogView.findViewById(R.id.tv_pin_title);
        TextView tvMessage  = dialogView.findViewById(R.id.tv_pin_message);
        EditText etInput    = dialogView.findViewById(R.id.et_pin_password);
        Button   btnConfirm = dialogView.findViewById(R.id.btn_pin_confirm);
        Button   btnCancel  = dialogView.findViewById(R.id.btn_pin_cancel);
        Button   btnForgot  = dialogView.findViewById(R.id.btn_pin_forgot);

        tvTitle.setText("Forgot Password");
        tvMessage.setText("Enter the email address linked to your account.");
        etInput.setInputType(android.text.InputType.TYPE_CLASS_TEXT
                | android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        etInput.setHint("Email");
        etInput.setText(etEmail.getText().toString());
        btnConfirm.setText("NEXT");
        btnForgot.setVisibility(View.GONE);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .setCancelable(false)
                .create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        // Stage tracking: 0 = collect email, 1 = collect new password.
        final int[] stage = {0};
        final String[] capturedEmail = {""};

        btnConfirm.setOnClickListener(v -> {
            String value = etInput.getText().toString().trim();
            if (TextUtils.isEmpty(value)) {
                Toast.makeText(this, R.string.error_empty_fields, Toast.LENGTH_SHORT).show();
                return;
            }

            if (stage[0] == 0) {
                capturedEmail[0] = value;
                // Verify the email exists via /api/forgot-password before showing the new-password step.
                btnConfirm.setEnabled(false);
                new Thread(() -> {
                    boolean exists = postJson("/api/forgot-password",
                            "{\"email\":\"" + value + "\"}") == 200;
                    runOnUiThread(() -> {
                        btnConfirm.setEnabled(true);
                        if (exists) {
                            stage[0] = 1;
                            tvTitle.setText("New Password");
                            tvMessage.setText("Choose a new password for " + value + ".");
                            etInput.setText("");
                            etInput.setHint("New Password");
                            etInput.setInputType(android.text.InputType.TYPE_CLASS_TEXT
                                    | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
                            btnConfirm.setText("RESET");
                        } else {
                            Toast.makeText(this, "No account found for that email.",
                                    Toast.LENGTH_SHORT).show();
                        }
                    });
                }).start();
            } else {
                // stage 1 — submit the new password.
                btnConfirm.setEnabled(false);
                String emailFinal = capturedEmail[0];
                String newPassword = value;
                new Thread(() -> {
                    int code = postJson("/api/reset-password",
                            "{\"email\":\"" + emailFinal + "\",\"newPassword\":\"" + newPassword + "\"}");
                    runOnUiThread(() -> {
                        btnConfirm.setEnabled(true);
                        if (code == 200) {
                            Toast.makeText(this, "Password updated. You can now log in.",
                                    Toast.LENGTH_LONG).show();
                            etEmail.setText(emailFinal);
                            etPassword.setText(newPassword);
                            dialog.dismiss();
                        } else {
                            Toast.makeText(this, "Reset failed (Code " + code + "). Try again.",
                                    Toast.LENGTH_SHORT).show();
                        }
                    });
                }).start();
            }
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    /**
     * POSTs a JSON body to the backend and returns the HTTP status code (-1 on network error).
     */
    private int postJson(String path, String body) {
        try {
            java.net.URL url = new java.net.URL("http://" + BuildConfig.BACKEND_IP + ":5000" + path);
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            java.io.OutputStream os = conn.getOutputStream();
            os.write(body.getBytes("UTF-8"));
            os.flush();
            os.close();
            return conn.getResponseCode();
        } catch (Exception e) {
            android.util.Log.e("Login", "postJson " + path + " failed: " + e.getMessage(), e);
            return -1;
        }
    }

    private void showLoading(boolean show) {
        progressLogin.setVisibility(show ? View.VISIBLE : View.GONE);
        btnLogin.setEnabled(!show);
    }
}
