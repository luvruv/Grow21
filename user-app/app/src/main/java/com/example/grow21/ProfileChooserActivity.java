package com.example.grow21;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;

public class ProfileChooserActivity extends AppCompatActivity {
    private LinearLayout profilesContainer;
    private ProgressBar progressBar;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Build dynamic UI programmatically to avoid complex XML layouts
        LinearLayout mainLayout = new LinearLayout(this);
        mainLayout.setOrientation(LinearLayout.VERTICAL);
        mainLayout.setGravity(Gravity.CENTER);
        mainLayout.setPadding(64, 64, 64, 64);
        mainLayout.setBackgroundColor(Color.parseColor("#121212")); // Netflix dark theme

        TextView title = new TextView(this);
        title.setText("Who's playing?");
        title.setTextSize(28);
        title.setTextColor(Color.WHITE);
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, 0, 0, 64);
        mainLayout.addView(title);

        progressBar = new ProgressBar(this);
        mainLayout.addView(progressBar);

        profilesContainer = new LinearLayout(this);
        profilesContainer.setOrientation(LinearLayout.VERTICAL);
        profilesContainer.setGravity(Gravity.CENTER);
        mainLayout.addView(profilesContainer);

        setContentView(mainLayout);

        prefs = getSharedPreferences("grow21_prefs", MODE_PRIVATE);
        int parentId = prefs.getInt("SERVER_PARENT_ID", -1);
        
        if (parentId != -1) {
            fetchProfiles(parentId);
        } else {
            Toast.makeText(this, "Error: Parent ID not found. Please log out and log in again.", Toast.LENGTH_LONG).show();
            progressBar.setVisibility(View.GONE);
        }
    }

    private void fetchProfiles(int parentId) {
        new Thread(() -> {
            try {
                URL url = new URL("http://" + BuildConfig.BACKEND_IP + ":5000/api/children/details?role=Parent&refId=" + parentId);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");

                InputStream in = conn.getInputStream();
                Scanner scanner = new Scanner(in).useDelimiter("\\A");
                String responseBody = scanner.hasNext() ? scanner.next() : "";
                
                JSONArray childrenArray = new JSONArray(responseBody);
                
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    
                    if (childrenArray.length() == 0) {
                        TextView noProfiles = new TextView(this);
                        noProfiles.setText("No children found. Please add a child from the Admin CRM.");
                        noProfiles.setTextColor(Color.WHITE);
                        noProfiles.setTextSize(16);
                        noProfiles.setGravity(Gravity.CENTER);
                        profilesContainer.addView(noProfiles);
                        return;
                    }

                    for (int i = 0; i < childrenArray.length(); i++) {
                        try {
                            JSONObject child = childrenArray.getJSONObject(i);
                            int childId = child.getInt("ChildID");
                            String name = child.getString("ChildName");
                            
                            Button profileBtn = new Button(this);
                            profileBtn.setText(name);
                            profileBtn.setTextSize(20);
                            profileBtn.setTextColor(Color.WHITE);
                            profileBtn.setBackgroundColor(Color.parseColor("#E50914")); // Netflix red
                            
                            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                                    LinearLayout.LayoutParams.MATCH_PARENT,
                                    LinearLayout.LayoutParams.WRAP_CONTENT
                            );
                            params.setMargins(0, 0, 0, 48);
                            profileBtn.setLayoutParams(params);
                            profileBtn.setPadding(32, 64, 32, 64);
                            
                            profileBtn.setOnClickListener(v -> {
                                prefs.edit()
                                    .putInt("SERVER_CHILD_ID", childId)
                                    .putString("SERVER_CHILD_NAME", name)
                                    .apply();
                                Intent intent = new Intent(ProfileChooserActivity.this, StartActivity.class);
                                startActivity(intent);
                                finish();
                            });
                            
                            profilesContainer.addView(profileBtn);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(ProfileChooserActivity.this, "Network Error. Check backend server.", Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }
}
