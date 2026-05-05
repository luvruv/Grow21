package com.example.grow21;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class LoginActivity extends AppCompatActivity {

    private EditText etEmail, etPassword;
    private Button btnLogin;
    private ProgressBar progressLogin;
    private TextView tvSignupLink;
    private SharedPreferences prefs;

    private static final String PREFS_NAME = "grow21_prefs";
    private static final String KEY_LOGGED_IN = "is_logged_in";
    private static final String KEY_USER_EMAIL = "user_email";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        // Check if already logged in
        if (prefs.getBoolean(KEY_LOGGED_IN, false)) {
            Intent intent = new Intent(LoginActivity.this, ProfileChooserActivity.class);
            startActivity(intent);
            finish();
            return;
        }

        setContentView(R.layout.activity_login);

        etEmail = findViewById(R.id.et_email);
        etPassword = findViewById(R.id.et_password);
        btnLogin = findViewById(R.id.btn_login);
        progressLogin = findViewById(R.id.progress_login);
        tvSignupLink = findViewById(R.id.tv_signup_link);

        btnLogin.setOnClickListener(v -> attemptLogin());

        tvSignupLink.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, SignupActivity.class);
            startActivity(intent);
        });
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
                // Connect to Node.js backend
                java.net.URL url = new java.net.URL("http://" + BuildConfig.BACKEND_IP + ":5000/api/login");
                android.util.Log.d("Login", "Connecting to: " + url.toString());
                
                java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);

                // Send JSON payload
                org.json.JSONObject jsonParam = new org.json.JSONObject();
                jsonParam.put("email", email);
                jsonParam.put("password", password);
                String payload = jsonParam.toString();
                android.util.Log.d("Login", "Payload: " + payload);

                java.io.OutputStream os = conn.getOutputStream();
                os.write(payload.getBytes("UTF-8"));
                os.flush();
                os.close();

                int responseCode = conn.getResponseCode();
                android.util.Log.d("Login", "Response code: " + responseCode);
                final boolean valid = (responseCode == 200);

                // Read response body (success or error)
                java.io.InputStream stream = valid ? conn.getInputStream() : conn.getErrorStream();
                java.util.Scanner scanner = new java.util.Scanner(stream).useDelimiter("\\A");
                String responseBody = scanner.hasNext() ? scanner.next() : "";
                android.util.Log.d("Login", "Response body: " + responseBody);

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
                        Toast.makeText(LoginActivity.this, "Login failed (Code " + responseCode + "): " + finalResponseBody, Toast.LENGTH_LONG).show();
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
                final String errMsg = e.getMessage();
                android.util.Log.e("Login", "Network error: " + errMsg, e);
                runOnUiThread(() -> {
                    showLoading(false);
                    Toast.makeText(LoginActivity.this, "Network Error: " + errMsg, Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    private void navigateAfterLogin() {
        Intent intent = new Intent(this, ProfileChooserActivity.class);
        startActivity(intent);
        finish();
    }

    private void showLoading(boolean show) {
        progressLogin.setVisibility(show ? View.VISIBLE : View.GONE);
        btnLogin.setEnabled(!show);
    }
}
