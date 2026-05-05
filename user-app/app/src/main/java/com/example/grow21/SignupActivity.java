package com.example.grow21;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class SignupActivity extends AppCompatActivity {

    private EditText etEmail, etPassword, etConfirmPassword;
    private Button btnSignup;
    private TextView tvLoginLink;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        etEmail = findViewById(R.id.et_signup_email);
        etPassword = findViewById(R.id.et_signup_password);
        etConfirmPassword = findViewById(R.id.et_signup_confirm_password);
        btnSignup = findViewById(R.id.btn_signup);
        tvLoginLink = findViewById(R.id.tv_login_link);

        btnSignup.setOnClickListener(v -> attemptSignup());

        tvLoginLink.setOnClickListener(v -> finish());
    }

    private void attemptSignup() {
        final String email = etEmail.getText().toString().trim();
        final String password = etPassword.getText().toString().trim();
        final String confirmPassword = etConfirmPassword.getText().toString().trim();

        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password) || TextUtils.isEmpty(confirmPassword)) {
            Toast.makeText(this, R.string.error_empty_fields, Toast.LENGTH_SHORT).show();
            return;
        }
        if (password.length() < 4) {
            Toast.makeText(this, R.string.error_password_short, Toast.LENGTH_SHORT).show();
            return;
        }
        if (!password.equals(confirmPassword)) {
            Toast.makeText(this, R.string.error_passwords_mismatch, Toast.LENGTH_SHORT).show();
            return;
        }

        btnSignup.setEnabled(false);
        btnSignup.setText("Creating account...");

        new Thread(() -> {
            int responseCode = -1;
            String responseBody = "";
            try {
                java.net.URL url = new java.net.URL("http://" + BuildConfig.BACKEND_IP + ":5000/api/signup");
                java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);

                org.json.JSONObject json = new org.json.JSONObject();
                json.put("email", email);
                json.put("password", password);
                java.io.OutputStream os = conn.getOutputStream();
                os.write(json.toString().getBytes("UTF-8"));
                os.flush();
                os.close();

                responseCode = conn.getResponseCode();
                java.io.InputStream stream = (responseCode >= 200 && responseCode < 300)
                        ? conn.getInputStream() : conn.getErrorStream();
                if (stream != null) {
                    java.util.Scanner scanner = new java.util.Scanner(stream).useDelimiter("\\A");
                    responseBody = scanner.hasNext() ? scanner.next() : "";
                }
            } catch (Exception e) {
                android.util.Log.e("Signup", "Network error: " + e.getMessage(), e);
            }

            final int finalCode = responseCode;
            final String finalBody = responseBody;
            runOnUiThread(() -> {
                btnSignup.setEnabled(true);
                btnSignup.setText(R.string.btn_signup);

                // Only proceed when the backend confirms the account was created.
                if (finalCode == 201) {
                    Toast.makeText(SignupActivity.this, "Account created.", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(SignupActivity.this, ChildSetupActivity.class);
                    intent.putExtra("PARENT_EMAIL", email);
                    intent.putExtra("PARENT_PASSWORD", password);
                    startActivity(intent);
                    finish();
                } else if (finalCode == 409) {
                    Toast.makeText(SignupActivity.this,
                            "An account with this email already exists. Please log in.",
                            Toast.LENGTH_LONG).show();
                } else if (finalCode == -1) {
                    Toast.makeText(SignupActivity.this,
                            "Cannot reach the server. Check your connection and try again.",
                            Toast.LENGTH_LONG).show();
                } else {
                    String msg = "Signup failed (Code " + finalCode + ")";
                    try {
                        org.json.JSONObject obj = new org.json.JSONObject(finalBody);
                        if (obj.has("error")) msg = obj.getString("error");
                        else if (obj.has("message")) msg = obj.getString("message");
                    } catch (Exception ignored) {}
                    Toast.makeText(SignupActivity.this, msg, Toast.LENGTH_LONG).show();
                }
            });
        }).start();
    }
}
