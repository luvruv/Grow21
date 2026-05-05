package com.example.grow21;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class ChildSetupActivity extends AppCompatActivity {

    private EditText etChildName;
    private TextView tvAgeValue;
    private Button btnAgeMinus, btnAgePlus, btnSave;
    private ImageView avatar1, avatar2, avatar3, avatar4;

    private int selectedAge = 5;
    private int selectedAvatar = 0;
    private ImageView[] avatarViews;

    private static final String PREFS_NAME = "grow21_prefs";
    private static final String KEY_SETUP_COMPLETE = "setup_complete";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_child_setup);

        etChildName = findViewById(R.id.et_child_name);
        tvAgeValue = findViewById(R.id.tv_age_value);
        btnAgeMinus = findViewById(R.id.btn_age_minus);
        btnAgePlus = findViewById(R.id.btn_age_plus);
        btnSave = findViewById(R.id.btn_save_child);

        avatar1 = findViewById(R.id.avatar_1);
        avatar2 = findViewById(R.id.avatar_2);
        avatar3 = findViewById(R.id.avatar_3);
        avatar4 = findViewById(R.id.avatar_4);

        avatarViews = new ImageView[]{avatar1, avatar2, avatar3, avatar4};

        tvAgeValue.setText(String.valueOf(selectedAge));

        btnAgeMinus.setOnClickListener(v -> {
            if (selectedAge > 3) {
                selectedAge--;
                tvAgeValue.setText(String.valueOf(selectedAge));
            }
        });

        btnAgePlus.setOnClickListener(v -> {
            if (selectedAge < 18) {
                selectedAge++;
                tvAgeValue.setText(String.valueOf(selectedAge));
            }
        });

        // Avatar selection
        for (int i = 0; i < avatarViews.length; i++) {
            final int index = i;
            avatarViews[i].setOnClickListener(v -> selectAvatar(index));
        }

        btnSave.setOnClickListener(v -> saveChild());
    }

    private void selectAvatar(int index) {
        selectedAvatar = index;
        for (int i = 0; i < avatarViews.length; i++) {
            if (i == index) {
                avatarViews[i].setAlpha(1.0f);
                avatarViews[i].setScaleX(1.15f);
                avatarViews[i].setScaleY(1.15f);
            } else {
                avatarViews[i].setAlpha(0.5f);
                avatarViews[i].setScaleX(1.0f);
                avatarViews[i].setScaleY(1.0f);
            }
        }
    }

    private void saveChild() {
        String name = etChildName.getText().toString().trim();

        if (TextUtils.isEmpty(name)) {
            Toast.makeText(this, R.string.error_name_empty, Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedAge < 3 || selectedAge > 18) {
            Toast.makeText(this, R.string.error_age_invalid, Toast.LENGTH_SHORT).show();
            return;
        }

        String parentEmail = getIntent().getStringExtra("PARENT_EMAIL");
        String parentPassword = getIntent().getStringExtra("PARENT_PASSWORD");

        if (parentEmail == null || parentPassword == null) {
            Toast.makeText(this, "Missing parent credentials", Toast.LENGTH_SHORT).show();
            return;
        }

        new Thread(() -> {
            try {
                java.net.URL url = new java.net.URL("http://" + BuildConfig.BACKEND_IP + ":5000/api/children");
                java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);

                // Construct JSON payload
                org.json.JSONObject jsonParam = new org.json.JSONObject();
                jsonParam.put("ParentEmail", parentEmail);
                jsonParam.put("ParentPassword", parentPassword);
                jsonParam.put("Name", name);
                jsonParam.put("Age", selectedAge);
                jsonParam.put("BaselineSkillLevel", "Beginner");

                String payload = jsonParam.toString();
                android.util.Log.d("ChildSetup", "Sending to: " + url.toString());
                android.util.Log.d("ChildSetup", "Payload: " + payload);

                java.io.OutputStream os = conn.getOutputStream();
                os.write(payload.getBytes("UTF-8"));
                os.flush();
                os.close();

                int responseCode = conn.getResponseCode();
                android.util.Log.d("ChildSetup", "Response code: " + responseCode);

                // Read response body
                java.io.InputStream stream = (responseCode >= 200 && responseCode < 300) 
                    ? conn.getInputStream() : conn.getErrorStream();
                java.util.Scanner scanner = new java.util.Scanner(stream).useDelimiter("\\A");
                final String responseBody = scanner.hasNext() ? scanner.next() : "";
                android.util.Log.d("ChildSetup", "Response body: " + responseBody);

                runOnUiThread(() -> {
                    if (responseCode == 201) {
                        Toast.makeText(ChildSetupActivity.this, "Account & Child created!", Toast.LENGTH_LONG).show();
                        Intent intent = new Intent(ChildSetupActivity.this, LoginActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    } else if (responseCode == 401) {
                        Toast.makeText(ChildSetupActivity.this, "Incorrect password for existing email.", Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(ChildSetupActivity.this, "Error " + responseCode + ": " + responseBody, Toast.LENGTH_LONG).show();
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
                final String errMsg = e.getMessage();
                android.util.Log.e("ChildSetup", "Network error: " + errMsg, e);
                runOnUiThread(() -> Toast.makeText(ChildSetupActivity.this, "Network Error: " + errMsg, Toast.LENGTH_LONG).show());
            }
        }).start();
    }
}
