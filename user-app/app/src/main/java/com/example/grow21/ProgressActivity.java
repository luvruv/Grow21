package com.example.grow21;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.grow21.models.CategoryProgress;

import java.util.List;

public class ProgressActivity extends AppCompatActivity {

    private RecyclerView rvProgress;
    private TextView tvEmptyProgress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_progress);

        rvProgress = findViewById(R.id.rv_progress);
        tvEmptyProgress = findViewById(R.id.tv_empty_progress);

        rvProgress.setLayoutManager(new LinearLayoutManager(this));

        loadProgress();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadProgress();
    }

    private void loadProgress() {
        DatabaseHelper db = DatabaseHelper.getInstance(this);
        List<CategoryProgress> progressList = db.getProgressByCategory();

        if (progressList.isEmpty()) {
            rvProgress.setVisibility(View.GONE);
            tvEmptyProgress.setVisibility(View.VISIBLE);
        } else {
            rvProgress.setVisibility(View.VISIBLE);
            tvEmptyProgress.setVisibility(View.GONE);
            ProgressAdapter adapter = new ProgressAdapter(progressList);
            rvProgress.setAdapter(adapter);
        }
    }
}
