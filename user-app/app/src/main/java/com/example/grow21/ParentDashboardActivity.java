package com.example.grow21;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.grow21.models.CategoryProgress;
import com.example.grow21.models.Child;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Parent Dashboard screen showing the child's streak calendar and
 * per-category progress report. Accessed by toggling the profile switch
 * to Parent mode (requires password verification).
 */
public class ParentDashboardActivity extends AppCompatActivity {

    private SwitchCompat switchProfile;
    private TextView tvChildNameSubtitle;
    private TextView tvStreakCount;
    private TextView tvMonthYear;
    private ImageButton btnPrevMonth, btnNextMonth;
    private RecyclerView rvCalendar;
    private RecyclerView rvProgressReport;
    private TextView tvTotalAttempted, tvOverallAccuracy;
    private TextView tvEmptyReport;
    private View cardSummary;

    private ImageView btnSettings;

    private DatabaseHelper dbHelper;

    /** Currently displayed month/year for the calendar. */
    private int displayYear;
    private int displayMonth; // 1-12

    private static final String PREFS_NAME = "grow21_prefs";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_parent_dashboard);

        dbHelper = DatabaseHelper.getInstance(this);

        initViews();
        setupProfileSwitch();
        setupSettingsButton();
        loadChildInfo();
        initCalendar();
        loadStreakData();
        loadProgressReport();
    }

    private void initViews() {
        switchProfile = findViewById(R.id.switch_profile);
        tvChildNameSubtitle = findViewById(R.id.tv_child_name_subtitle);
        tvStreakCount = findViewById(R.id.tv_streak_count);
        tvMonthYear = findViewById(R.id.tv_month_year);
        btnPrevMonth = findViewById(R.id.btn_prev_month);
        btnNextMonth = findViewById(R.id.btn_next_month);
        rvCalendar = findViewById(R.id.rv_calendar);
        rvProgressReport = findViewById(R.id.rv_progress_report);
        tvTotalAttempted = findViewById(R.id.tv_total_attempted);
        tvOverallAccuracy = findViewById(R.id.tv_overall_accuracy);
        tvEmptyReport = findViewById(R.id.tv_empty_report);
        cardSummary = findViewById(R.id.card_summary);
        btnSettings = findViewById(R.id.btn_settings);
    }

    private void setupSettingsButton() {
        btnSettings.setOnClickListener(v -> {
            Intent intent = new Intent(ParentDashboardActivity.this, SettingsActivity.class);
            startActivity(intent);
        });
    }

    /**
     * Profile switch toggle: when toggled OFF (Child mode), navigate back
     * to StartActivity and finish this activity.
     */
    private void setupProfileSwitch() {
        // Switch is checked = Parent mode (we start in parent mode)
        switchProfile.setChecked(true);

        switchProfile.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (!isChecked) {
                // Switching back to Child mode
                SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
                prefs.edit().putBoolean("is_parent_mode", false).apply();

                Intent intent = new Intent(ParentDashboardActivity.this, StartActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish();
            }
        });
    }

    /** Load and display child name in the subtitle. */
    private void loadChildInfo() {
        Child child = dbHelper.getChild();
        if (child != null) {
            tvChildNameSubtitle.setText(getString(R.string.child_name_format, child.getName()));
        }
    }

    /** Initialize calendar to the current month and set up month navigation. */
    private void initCalendar() {
        Calendar now = Calendar.getInstance();
        displayYear = now.get(Calendar.YEAR);
        displayMonth = now.get(Calendar.MONTH) + 1; // Calendar.MONTH is 0-based

        // 7-column grid for the calendar
        rvCalendar.setLayoutManager(new GridLayoutManager(this, 7));

        btnPrevMonth.setOnClickListener(v -> {
            displayMonth--;
            if (displayMonth < 1) {
                displayMonth = 12;
                displayYear--;
            }
            loadCalendarMonth();
        });

        btnNextMonth.setOnClickListener(v -> {
            displayMonth++;
            if (displayMonth > 12) {
                displayMonth = 1;
                displayYear++;
            }
            loadCalendarMonth();
        });

        loadCalendarMonth();
    }

    /** Load streak count and display it. */
    private void loadStreakData() {
        int streak = dbHelper.getCurrentStreak();
        if (streak > 0) {
            tvStreakCount.setText(getString(R.string.streak_format, streak));
        } else {
            tvStreakCount.setText(R.string.streak_zero);
        }
    }

    /**
     * Build the day cell list for the currently selected month and
     * populate the calendar RecyclerView.
     */
    private void loadCalendarMonth() {
        // Update month/year label
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.YEAR, displayYear);
        cal.set(Calendar.MONTH, displayMonth - 1); // Calendar.MONTH is 0-based
        cal.set(Calendar.DAY_OF_MONTH, 1);

        SimpleDateFormat monthFormat = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
        tvMonthYear.setText(monthFormat.format(cal.getTime()));

        // Determine the day-of-week for the 1st (Sunday=1 in Calendar)
        int firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK); // 1=Sun, 2=Mon...
        int daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH);

        // Build cell list: null entries for padding before day 1
        List<Integer> dayCells = new ArrayList<>();
        for (int i = 1; i < firstDayOfWeek; i++) {
            dayCells.add(null); // empty padding
        }
        for (int day = 1; day <= daysInMonth; day++) {
            dayCells.add(day);
        }

        // Fetch active days from the database
        Set<Integer> activeDays = dbHelper.getActiveDaysForMonth(displayYear, displayMonth);

        CalendarDayAdapter adapter = new CalendarDayAdapter(
                dayCells, activeDays, displayYear, displayMonth);
        rvCalendar.setAdapter(adapter);
    }

    /**
     * Load per-category progress data and populate the progress report
     * RecyclerView using the existing ProgressAdapter.
     */
    private void loadProgressReport() {
        List<CategoryProgress> progressList = dbHelper.getProgressByCategory();

        if (progressList.isEmpty()) {
            tvEmptyReport.setVisibility(View.VISIBLE);
            rvProgressReport.setVisibility(View.GONE);
            cardSummary.setVisibility(View.GONE);
        } else {
            tvEmptyReport.setVisibility(View.GONE);
            rvProgressReport.setVisibility(View.VISIBLE);
            cardSummary.setVisibility(View.VISIBLE);

            // Calculate totals for summary card
            int totalAttempted = 0;
            int totalCorrect = 0;
            for (CategoryProgress cp : progressList) {
                totalAttempted += cp.getAttempted();
                totalCorrect += cp.getCorrect();
            }
            float overallAccuracy = totalAttempted > 0
                    ? ((float) totalCorrect / totalAttempted) * 100 : 0;

            tvTotalAttempted.setText(getString(R.string.total_attempted_format, totalAttempted));
            tvOverallAccuracy.setText(String.format(Locale.getDefault(),
                    getString(R.string.overall_accuracy_format), overallAccuracy));

            // Reuse the existing ProgressAdapter for per-category cards
            rvProgressReport.setLayoutManager(new LinearLayoutManager(this));
            rvProgressReport.setNestedScrollingEnabled(false);
            ProgressAdapter adapter = new ProgressAdapter(progressList);
            rvProgressReport.setAdapter(adapter);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh data when returning to this screen
        loadStreakData();
        loadCalendarMonth();
        loadProgressReport();
    }
}
