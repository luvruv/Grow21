package com.example.grow21;

import android.content.ClipData;
import android.content.ClipDescription;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
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
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.example.grow21.models.QuestionModel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class DragGameActivity extends AppCompatActivity {

    private ImageButton btnClose;
    private TextView tvScore, tvProgress, tvStatus;
    private LinearLayout llTargets;
    private GridLayout gridDraggables;
    private Button btnReset;

    private List<QuestionModel> questions;
    private int currentQuestionIndex = 0;
    private int score = 0;
    private String category;
    
    private int matchesFound = 0;
    private int totalMatches = 0;

    private DatabaseHelper dbHelper;
    private Handler handler;
    private TTSManager ttsManager;
    private boolean ttsEnabled = false;
    private boolean soundEnabled = true;

    private static final String PREFS_NAME = "grow21_prefs";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_drag_game);

        handler = new Handler(Looper.getMainLooper());
        dbHelper = DatabaseHelper.getInstance(this);

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        ttsEnabled = prefs.getBoolean("voice_instructions", false);
        soundEnabled = prefs.getBoolean("sound_effects", true);

        if (ttsEnabled) {
            ttsManager = new TTSManager(this);
        }

        category = getIntent().getStringExtra("category");
        if (category == null) category = "shapes";

        initViews();
        loadQuestions();
    }

    private void initViews() {
        btnClose = findViewById(R.id.btn_close);
        tvScore = findViewById(R.id.tv_score);
        tvProgress = findViewById(R.id.tv_progress);
        tvStatus = findViewById(R.id.tv_status);
        llTargets = findViewById(R.id.ll_targets);
        gridDraggables = findViewById(R.id.grid_draggables);
        btnReset = findViewById(R.id.btn_reset);

        btnClose.setOnClickListener(v -> finish());
        btnReset.setOnClickListener(v -> displayQuestion());
    }

    private final View.OnTouchListener dragTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                String tag = (String) view.getTag();
                ClipData.Item item = new ClipData.Item(tag);
                ClipData dragData = new ClipData(tag, new String[]{ClipDescription.MIMETYPE_TEXT_PLAIN}, item);
                View.DragShadowBuilder myShadow = new View.DragShadowBuilder(view);
                view.startDragAndDrop(dragData, myShadow, view, 0);
                view.setVisibility(View.INVISIBLE);
                return true;
            }
            return false;
        }
    };

    private void loadQuestions() {
        questions = QuestionLoader.loadQuestionsByCategory(this, category);
        if (questions.isEmpty()) {
            questions = QuestionLoader.loadShuffledQuestions(this);
        }

        questions.removeIf(q -> !q.getType().equals("drag_match"));

        if (questions.isEmpty()) {
            Toast.makeText(this, "No drag & match questions available", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        currentQuestionIndex = 0;
        score = 0;
        displayQuestion();
    }

    private void displayQuestion() {
        if (currentQuestionIndex >= questions.size()) {
            finishGame();
            return;
        }

        QuestionModel question = questions.get(currentQuestionIndex);
        tvStatus.setText("Drag shapes here!");
        tvScore.setText("Score: " + score);
        tvProgress.setText((currentQuestionIndex + 1) + "/" + questions.size());
        
        matchesFound = 0;

        llTargets.removeAllViews();
        gridDraggables.removeAllViews();

        List<String> options = question.getOptions();
        if (options == null || options.isEmpty()) {
            loadNextQuestion();
            return;
        }
        
        totalMatches = options.size();

        // Create target slots
        for (String opt : options) {
            createTargetSlot(opt);
        }

        // Build pool of decoy shapes
        List<String> allDecoys = new ArrayList<>();
        for (QuestionModel q : questions) {
            if (q.getOptions() != null) {
                for (String o : q.getOptions()) {
                    if (!allDecoys.contains(o)) allDecoys.add(o);
                }
            }
        }
        
        // Add robust fallbacks so we always have enough items for the 8-grid
        String[] fallbackDecoys = {
            "shape_circle", "shape_square", "shape_triangle", 
            "shape_star", "shape_pentagon", "shape_diamond", 
            "shape_rectangle", "shape_heart"
        };
        for (String f : fallbackDecoys) {
            if (!allDecoys.contains(f)) allDecoys.add(f);
        }
        
        Collections.shuffle(allDecoys);

        // Create draggable items (options + decoys up to 8)
        List<String> gridItems = new ArrayList<>(options);
        for (String decoy : allDecoys) {
            if (gridItems.size() >= 8) break;
            if (!gridItems.contains(decoy)) {
                gridItems.add(decoy);
            }
        }
        Collections.shuffle(gridItems);
        
        for (String item : gridItems) {
            createDraggableItem(item);
        }

        if (ttsManager != null) {
            ttsManager.speak("What is this?");
        }

        if (soundEnabled) {
            ToneGenerator toneGen = new ToneGenerator(AudioManager.STREAM_MUSIC, 100);
            toneGen.startTone(ToneGenerator.TONE_PROP_ACK, 200); // or TONE_DTMF_1, TONE_PROP_NACK
            handler.postDelayed(toneGen::release, 250);
        }
    }

    private void createTargetSlot(String shapeName) {
        FrameLayout frame = new FrameLayout(this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                dpToPx(95), dpToPx(95)); // Scaled to fit 3 columns easily
        params.weight = 1;
        params.setMargins(dpToPx(4), dpToPx(8), dpToPx(4), dpToPx(8));
        frame.setLayoutParams(params);
        frame.setBackgroundResource(R.drawable.bg_drop_target_idle);
        frame.setTag(shapeName);

        ImageView iv = new ImageView(this);
        FrameLayout.LayoutParams ivParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);
        int padding = dpToPx(8); // Minimal padding so shape fills the block
        iv.setPadding(padding, padding, padding, padding);
        iv.setLayoutParams(ivParams);
        iv.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        iv.setAlpha(0.25f); // Faint outline hint

        int resId = getResources().getIdentifier(shapeName, "drawable", getPackageName());
        if (resId != 0) iv.setImageResource(resId);

        frame.addView(iv);
        llTargets.addView(frame);

        frame.setOnDragListener((v, event) -> {
            switch (event.getAction()) {
                case DragEvent.ACTION_DRAG_STARTED:
                    return event.getClipDescription().hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN);
                case DragEvent.ACTION_DRAG_ENTERED:
                    v.setBackgroundResource(R.drawable.bg_drop_target_active);
                    v.setScaleX(1.05f);
                    v.setScaleY(1.05f);
                    return true;
                case DragEvent.ACTION_DRAG_EXITED:
                    v.setBackgroundResource(R.drawable.bg_drop_target_idle);
                    v.setScaleX(1f);
                    v.setScaleY(1f);
                    return true;
                case DragEvent.ACTION_DROP:
                    v.setBackgroundResource(R.drawable.bg_drop_target_idle);
                    v.setScaleX(1f);
                    v.setScaleY(1f);
                    String draggedData = event.getClipData().getItemAt(0).getText().toString();
                    String targetData = (String) v.getTag();
                    View draggedView = (View) event.getLocalState();
                    
                    if (draggedData.equals(targetData)) {
                        onCorrectMatch(draggedView, (FrameLayout) v, targetData);
                    } else {
                        onWrongMatch(draggedView);
                    }
                    return true;
                case DragEvent.ACTION_DRAG_ENDED:
                    v.setBackgroundResource(R.drawable.bg_drop_target_idle);
                    v.setScaleX(1f);
                    v.setScaleY(1f);
                    if (!event.getResult()) {
                        View view = (View) event.getLocalState();
                        if (view != null) view.setVisibility(View.VISIBLE);
                    }
                    return true;
            }
            return false;
        });
    }

    private void createDraggableItem(String shapeName) {
        ImageView iv = new ImageView(this);
        GridLayout.LayoutParams params = new GridLayout.LayoutParams();
        params.width = dpToPx(72); // Mathematically fits 4 columns on screen without clipping
        params.height = dpToPx(72);
        params.setMargins(dpToPx(4), dpToPx(8), dpToPx(4), dpToPx(8));
        iv.setLayoutParams(params);
        
        int padding = dpToPx(4); // Keep internal padding small so shape is large
        iv.setPadding(padding, padding, padding, padding);
        iv.setScaleType(ImageView.ScaleType.CENTER_INSIDE);

        int resId = getResources().getIdentifier(shapeName, "drawable", getPackageName());
        if (resId != 0) iv.setImageResource(resId);

        iv.setTag(shapeName);
        
        AnimUtil.addPressEffect(iv);
        iv.setOnTouchListener(dragTouchListener);

        gridDraggables.addView(iv);
    }

    private void onCorrectMatch(View draggedView, FrameLayout targetSlot, String shapeName) {
        draggedView.setVisibility(View.GONE);
        targetSlot.setOnDragListener(null); // Disable further drops
        targetSlot.setBackgroundResource(R.drawable.bg_option_card_correct);
        
        ImageView iv = (ImageView) targetSlot.getChildAt(0);
        iv.setAlpha(1.0f); // Make shape fully visible
        
        matchesFound++;
        tvStatus.setText("⭐ Nice match!");

        ScaleAnimation bounce = new ScaleAnimation(
                0.8f, 1.15f, 0.8f, 1.15f,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f);
        bounce.setDuration(300);
        bounce.setRepeatCount(1);
        bounce.setRepeatMode(Animation.REVERSE);
        targetSlot.startAnimation(bounce);

        ToneGenerator toneGen = new ToneGenerator(AudioManager.STREAM_MUSIC, 100);
        toneGen.startTone(ToneGenerator.TONE_PROP_ACK, 200);
        handler.postDelayed(toneGen::release, 250);

        if (matchesFound >= totalMatches) {
            score++;
            dbHelper.insertPerformance(questions.get(currentQuestionIndex).getId(), category, true);
            handler.postDelayed(this::showRewardDialog, 800);
        }
    }

    private void onWrongMatch(View draggedView) {
        if (draggedView != null) {
            draggedView.setVisibility(View.VISIBLE);
            TranslateAnimation shake = new TranslateAnimation(-10, 10, 0, 0);
            shake.setDuration(50);
            shake.setRepeatCount(5);
            shake.setRepeatMode(Animation.REVERSE);
            draggedView.startAnimation(shake);
        }

        tvStatus.setText("Good try! Find the right shape");

        ToneGenerator toneGen = new ToneGenerator(AudioManager.STREAM_MUSIC, 100);
        toneGen.startTone(ToneGenerator.TONE_PROP_NACK, 200);
        handler.postDelayed(toneGen::release, 250);
    }

    private void showRewardDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_reward, null);

        TextView tvEmoji = dialogView.findViewById(R.id.tv_reward_emoji);
        TextView tvTitle = dialogView.findViewById(R.id.tv_reward_title);
        TextView tvMessage = dialogView.findViewById(R.id.tv_reward_message);
        Button btnNext = dialogView.findViewById(R.id.btn_next);

        tvEmoji.setText("✨");
        tvTitle.setText("Puzzle Complete!");
        tvMessage.setText(R.string.reward_correct_message);

        ScaleAnimation anim = new ScaleAnimation(
                0.5f, 1.2f, 0.5f, 1.2f,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f);
        anim.setDuration(500);
        anim.setRepeatCount(1);
        anim.setRepeatMode(Animation.REVERSE);
        tvEmoji.startAnimation(anim);

        boolean isLast = (currentQuestionIndex >= questions.size() - 1);
        btnNext.setText(isLast ? R.string.btn_finish : R.string.btn_next);

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
            else loadNextQuestion();
        });

        dialog.show();
    }

    private void loadNextQuestion() {
        currentQuestionIndex++;
        displayQuestion();
    }

    private void finishGame() {
        GameCelebrationUtil.celebrate(this);
        dbHelper.insertSession(category, score, questions.size());
        String message = getString(R.string.session_complete_format, score, questions.size());
        new AlertDialog.Builder(this)
                .setTitle(R.string.session_complete_title)
                .setMessage(message)
                .setCancelable(false)
                .setPositiveButton(R.string.btn_go_back, (dialog, which) -> {
                    dialog.dismiss();
                    finish();
                })
                .show();
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
