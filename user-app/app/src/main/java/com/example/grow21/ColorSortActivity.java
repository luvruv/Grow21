package com.example.grow21;

import android.content.ClipData;
import android.content.ClipDescription;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import android.view.DragEvent;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
import android.view.animation.TranslateAnimation;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.example.grow21.models.QuestionModel;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Color Sorting Game: Drag colored items to matching colored bins.
 * - 3 color bins (Red, Blue, Yellow)
 * - Items show emoji + label, colored by target color
 * - Correct → snap into bin, star sparkle
 * - Wrong → shake, return, "Good Try!"
 */
public class ColorSortActivity extends AppCompatActivity {

    private ImageButton btnClose;
    private ProgressBar progressBar;
    private TextView tvQuestion, tvStatus;
    private LinearLayout llItems, llBins;

    private List<QuestionModel> questions;
    private int currentQuestionIndex = 0;
    private int sortedCount = 0;
    private int totalItems = 0;

    private DatabaseHelper dbHelper;
    private Handler handler;
    private static final String PREFS_NAME = "grow21_prefs";

    private TTSManager ttsManager;
    private boolean ttsEnabled = false;
    private boolean soundEnabled = true; // Add this

    private static final Map<String, Integer> BIN_DRAWABLES = new HashMap<>();
    private static final Map<String, Integer> BIN_COLORS = new HashMap<>();
    private static final Map<String, String> BIN_LABELS = new HashMap<>();

    static {
        BIN_DRAWABLES.put("red", R.drawable.bg_bin_red);
        BIN_DRAWABLES.put("blue", R.drawable.bg_bin_blue);
        BIN_DRAWABLES.put("yellow", R.drawable.bg_bin_yellow);
        BIN_COLORS.put("red", Color.parseColor("#EF5350"));
        BIN_COLORS.put("blue", Color.parseColor("#42A5F5"));
        BIN_COLORS.put("yellow", Color.parseColor("#FFCA28"));
        BIN_LABELS.put("red", "🔴 RED");
        BIN_LABELS.put("blue", "🔵 BLUE");
        BIN_LABELS.put("yellow", "🟡 YELLOW");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_color_sort);

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        ttsEnabled = prefs.getBoolean("voice_instructions", false);
        soundEnabled = prefs.getBoolean("sound_effects", true);

        if (ttsEnabled && ttsManager != null) {
            ttsManager.speak("Sort by color");
        }

        handler = new Handler(Looper.getMainLooper());
        dbHelper = DatabaseHelper.getInstance(this);

        initViews();
        loadQuestions();
    }

    private void initViews() {
        btnClose = findViewById(R.id.btn_close);
        progressBar = findViewById(R.id.progress_bar);
        tvQuestion = findViewById(R.id.tv_question);
        tvStatus = findViewById(R.id.tv_status);
        llItems = findViewById(R.id.ll_items);
        llBins = findViewById(R.id.ll_bins);

        btnClose.setOnClickListener(v -> finish());
    }

    private void loadQuestions() {
        questions = QuestionLoader.loadQuestionsByType(this, "color_sort");
        if (questions.isEmpty()) {
            Toast.makeText(this, "No color sorting puzzles available", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        currentQuestionIndex = 0;
        displayQuestion();
    }

    private void displayQuestion() {
        if (currentQuestionIndex >= questions.size()) {
            finishGame();
            return;
        }

        QuestionModel q = questions.get(currentQuestionIndex);
        tvQuestion.setText(q.getQuestion());
        tvStatus.setText("Drag each item to the correct box");
        sortedCount = 0;

        llItems.removeAllViews();
        llBins.removeAllViews();

        // Create bins
        List<String> colors = q.getColors();
        if (colors != null) {
            for (String color : colors) {
                createBin(color);
            }
        }

        // Create draggable items
        List<String> items = q.getItems();
        if (items != null) {
            totalItems = items.size();
            for (String itemData : items) {
                // Format: "emoji Label:color"
                String[] parts = itemData.split(":");
                if (parts.length == 2) {
                    String label = parts[0].trim();
                    String color = parts[1].trim();
                    createDraggableItem(label, color);
                }
            }
        }

        int progress = (int) (((float) currentQuestionIndex / questions.size()) * 100);
        progressBar.setProgress(progress);
    }

    private void createBin(String color) {
        LinearLayout bin = new LinearLayout(this);
        bin.setOrientation(LinearLayout.VERTICAL);
        bin.setGravity(Gravity.CENTER);

        int drawableRes = BIN_DRAWABLES.getOrDefault(color, R.drawable.bg_bin_red);
        bin.setBackgroundResource(drawableRes);
        bin.setTag(color);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                0, dpToPx(140), 1f);
        params.setMargins(dpToPx(12), dpToPx(4), dpToPx(12), dpToPx(4));
        bin.setLayoutParams(params);
        bin.setPadding(dpToPx(8), dpToPx(8), dpToPx(8), dpToPx(8));
        bin.setElevation(dpToPx(6));

        // Label
        String label = BIN_LABELS.getOrDefault(color, color.toUpperCase());
        TextView tvLabel = new TextView(this);
        tvLabel.setText(label);
        tvLabel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        tvLabel.setTextColor(Color.parseColor("#2E2E2E"));
        tvLabel.setTypeface(null, android.graphics.Typeface.BOLD);
        tvLabel.setGravity(Gravity.CENTER);
        bin.addView(tvLabel);

        // Drop listener
        bin.setOnDragListener((v, event) -> {
            switch (event.getAction()) {
                case DragEvent.ACTION_DRAG_STARTED:
                    return event.getClipDescription().hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN);

                case DragEvent.ACTION_DRAG_ENTERED:
                    v.setAlpha(0.6f);
                    v.setScaleX(1.05f);
                    v.setScaleY(1.05f);
                    return true;

                case DragEvent.ACTION_DRAG_EXITED:
                    v.setAlpha(1.0f);
                    v.setScaleX(1.0f);
                    v.setScaleY(1.0f);
                    return true;

                case DragEvent.ACTION_DROP:
                    v.setAlpha(1.0f);
                    v.setScaleX(1.0f);
                    v.setScaleY(1.0f);
                    String draggedColor = event.getClipData().getItemAt(0).getText().toString();
                    String binColor = (String) v.getTag();
                    View draggedView = (View) event.getLocalState();

                    if (draggedColor.equals(binColor)) {
                        onCorrectSort(draggedView, (LinearLayout) v);
                    } else {
                        onWrongSort(draggedView);
                    }
                    return true;

                case DragEvent.ACTION_DRAG_ENDED:
                    v.setAlpha(1.0f);
                    v.setScaleX(1.0f);
                    v.setScaleY(1.0f);
                    if (!event.getResult()) {
                        View view = (View) event.getLocalState();
                        if (view != null) view.setVisibility(View.VISIBLE);
                    }
                    return true;
            }
            return false;
        });

        llBins.addView(bin);
    }

    private void createDraggableItem(String label, String color) {
        CardView card = new CardView(this);
        card.setRadius(dpToPx(16));
        card.setCardElevation(dpToPx(6));
        card.setUseCompatPadding(true);

        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                dpToPx(90), dpToPx(90));
        cardParams.setMargins(dpToPx(8), dpToPx(4), dpToPx(8), dpToPx(4));
        card.setLayoutParams(cardParams);

        // Inner view
        TextView tv = new TextView(this);
        tv.setText(label);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        tv.setTextColor(Color.parseColor("#2E2E2E"));
        tv.setTypeface(null, android.graphics.Typeface.BOLD);
        tv.setGravity(Gravity.CENTER);
        tv.setPadding(dpToPx(4), dpToPx(4), dpToPx(4), dpToPx(4));

        // Tint the card background with a lighter version of the color
        int bgColor = BIN_COLORS.getOrDefault(color, Color.GRAY);
        int lightBg = Color.argb(40, Color.red(bgColor), Color.green(bgColor), Color.blue(bgColor));
        card.setCardBackgroundColor(lightBg);

        card.addView(tv);
        card.setTag(color);

        AnimUtil.addPressEffect(card);

        // Touch listener for drag
        card.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                ClipData.Item item = new ClipData.Item(color);
                ClipData dragData = new ClipData(color,
                        new String[]{ClipDescription.MIMETYPE_TEXT_PLAIN}, item);
                View.DragShadowBuilder shadow = new View.DragShadowBuilder(v);
                v.startDragAndDrop(dragData, shadow, v, 0);
                v.setVisibility(View.INVISIBLE);
                return true;
            }
            return false;
        });

        llItems.addView(card);
    }

    private void onCorrectSort(View itemView, LinearLayout bin) {
        itemView.setVisibility(View.GONE);
        sortedCount++;

        // Bounce bin
        ScaleAnimation bounce = new ScaleAnimation(
                1f, 1.15f, 1f, 1.15f,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f);
        bounce.setDuration(250);
        bounce.setRepeatCount(1);
        bounce.setRepeatMode(Animation.REVERSE);
        bin.startAnimation(bounce);

        // Star animation on status
        tvStatus.setText("⭐ Correct!");

        // Sound
        ToneGenerator tone = new ToneGenerator(AudioManager.STREAM_MUSIC, 80);
        tone.startTone(ToneGenerator.TONE_PROP_ACK, 200);
        handler.postDelayed(tone::release, 250);

        if (sortedCount >= totalItems) {
            handler.postDelayed(this::onRoundComplete, 800);
        } else {
            handler.postDelayed(() -> tvStatus.setText("Keep going!"), 1000);
        }
    }

    private void onWrongSort(View itemView) {
        itemView.setVisibility(View.VISIBLE);

        // Shake animation
        TranslateAnimation shake = new TranslateAnimation(-10, 10, 0, 0);
        shake.setDuration(50);
        shake.setRepeatCount(5);
        shake.setRepeatMode(Animation.REVERSE);
        itemView.startAnimation(shake);

        tvStatus.setText("Good try! Try another box");

        ToneGenerator tone = new ToneGenerator(AudioManager.STREAM_MUSIC, 80);
        tone.startTone(ToneGenerator.TONE_PROP_NACK, 150);
        handler.postDelayed(tone::release, 200);
    }

    private void onRoundComplete() {
        dbHelper.insertPerformance(
                questions.get(currentQuestionIndex).getId(),
                "puzzle", true);

        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_reward, null);
        TextView tvEmoji = dialogView.findViewById(R.id.tv_reward_emoji);
        TextView tvTitle = dialogView.findViewById(R.id.tv_reward_title);
        TextView tvMessage = dialogView.findViewById(R.id.tv_reward_message);
        Button btnNext = dialogView.findViewById(R.id.btn_next);

        tvEmoji.setText("🌟");
        tvTitle.setText("Amazing!");
        tvMessage.setText("You sorted all the items!");

        boolean isLast = currentQuestionIndex >= questions.size() - 1;
        btnNext.setText(isLast ? "FINISH" : "NEXT");

        ScaleAnimation anim = new ScaleAnimation(
                0.5f, 1.2f, 0.5f, 1.2f,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f);
        anim.setDuration(500);
        anim.setRepeatCount(1);
        anim.setRepeatMode(Animation.REVERSE);
        tvEmoji.startAnimation(anim);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .setCancelable(false)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        btnNext.setOnClickListener(v -> {
            dialog.dismiss();
            if (isLast) finishGame();
            else {
                currentQuestionIndex++;
                displayQuestion();
            }
        });

        dialog.show();
    }

    private void finishGame() {
        GameCelebrationUtil.celebrate(this);
        dbHelper.insertSession("puzzle_color_sort", sortedCount, totalItems);
        new AlertDialog.Builder(this)
                .setTitle(R.string.session_complete_title)
                .setMessage(getString(R.string.session_complete_format, questions.size(), questions.size()))
                .setCancelable(false)
                .setPositiveButton(R.string.btn_go_back, (d, w) -> {
                    d.dismiss();
                    finish();
                }).show();
    }

    private int dpToPx(int dp) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, dp,
                getResources().getDisplayMetrics());
    }
    @Override
    protected void onDestroy() {
        if (ttsManager != null) {
            ttsManager.shutdown();
        }
        handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }
}
