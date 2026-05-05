package com.example.grow21;

import android.content.SharedPreferences;
import android.graphics.PointF;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.grow21.models.QuestionModel;

import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class MotorGameActivity extends AppCompatActivity {

    private ImageButton btnClose;
    private ProgressBar progressBar;
    private TextView tvQuestion;
    private DrawingView drawingView;
    private Button btnDone, btnUndo, btnClear;

    private List<QuestionModel> questions;
    private int currentQuestionIndex = 0;
    private int score = 0;
    private String category;

    private DatabaseHelper dbHelper;
    private Handler handler;
    private TTSManager ttsManager;
    private boolean ttsEnabled = false;
    private boolean soundEnabled = true; // Add this
    private static final String PREFS_NAME = "grow21_prefs";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_motor_game);

        handler = new Handler(Looper.getMainLooper());
        dbHelper = DatabaseHelper.getInstance(this);

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        ttsEnabled = prefs.getBoolean("voice_instructions", false);
        soundEnabled = prefs.getBoolean("sound_effects", true);

        if (ttsEnabled) {
            ttsManager = new TTSManager(this);
        }

        category = getIntent().getStringExtra("category");
        if (category == null) category = "motor_skills";

        initViews();
        loadQuestions();
    }

    private void initViews() {
        btnClose = findViewById(R.id.btn_close);
        progressBar = findViewById(R.id.progress_bar);
        tvQuestion = findViewById(R.id.tv_question);
        drawingView = findViewById(R.id.drawing_view);
        btnDone = findViewById(R.id.btn_done);
        btnUndo = findViewById(R.id.btn_undo);
        btnClear = findViewById(R.id.btn_clear);

        // Micro-interactions
        AnimUtil.addPressEffect(btnDone);
        AnimUtil.addPressEffect(btnUndo);
        AnimUtil.addPressEffect(btnClear);

        btnClose.setOnClickListener(v -> finish());
        btnUndo.setOnClickListener(v -> drawingView.undo());
        btnClear.setOnClickListener(v -> drawingView.clear());

        btnDone.setOnClickListener(v -> {
            if (!drawingView.hasDrawing()) {
                Toast.makeText(this, "Draw something first!", Toast.LENGTH_SHORT).show();
                return;
            }
            btnDone.setEnabled(false);
            evaluateDrawing();
        });
    }

    private void loadQuestions() {
        questions = QuestionLoader.loadQuestionsByType(this, "motor");
        
        // If filtering by category too
        if (!category.equals("motor_skills")) {
            questions.removeIf(q -> q.getCategory() != null && 
                !q.getCategory().equalsIgnoreCase(category));
        }

        Collections.shuffle(questions);

        if (questions.isEmpty()) {
            Toast.makeText(this, "No motor skill questions available", Toast.LENGTH_SHORT).show();
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

        btnDone.setEnabled(true);
        drawingView.clear();

        QuestionModel question = questions.get(currentQuestionIndex);

        tvQuestion.setText(question.getQuestion());

        // Set guide path based on question data
        String guideType = getGuideType(question);
        if (guideType != null) {
            drawingView.setGuideType(guideType);
        } else {
            drawingView.clearGuide();
        }

        int progress = (int) (((float) currentQuestionIndex / questions.size()) * 100);
        progressBar.setProgress(progress);

        if (ttsEnabled && ttsManager != null) {
            ttsManager.speak("Match the shapes!");
        }
    }

    /**
     * Determine what guide to show based on question's pathType or shape.
     */
    private String getGuideType(QuestionModel question) {
        if (question.getPathType() != null && !question.getPathType().isEmpty()) {
            return question.getPathType();
        }
        if (question.getShape() != null && !question.getShape().isEmpty()) {
            return question.getShape();
        }
        return null;
    }

    /**
     * Determine what detection key to use for ShapeDetector.
     */
    private String getDetectionKey(QuestionModel question) {
        if (question.getShape() != null && !question.getShape().isEmpty()) {
            return question.getShape();
        }
        if (question.getPathType() != null && !question.getPathType().isEmpty()) {
            return question.getPathType();
        }
        return "free"; // Free drawing — always passes
    }

    private void evaluateDrawing() {
        QuestionModel question = questions.get(currentQuestionIndex);
        List<PointF> points = drawingView.getAllPoints();
        String detectionKey = getDetectionKey(question);

        float accuracy = ShapeDetector.evaluate(points, detectionKey);
        boolean passed = ShapeDetector.passes(accuracy);

        dbHelper.insertPerformance(question.getId(), question.getCategory(), passed);

        if (passed) {
            score++;
            ToneGenerator toneGen = new ToneGenerator(AudioManager.STREAM_MUSIC, 100);
            toneGen.startTone(ToneGenerator.TONE_PROP_ACK, 200);
            handler.postDelayed(toneGen::release, 250);
        } else {
            ToneGenerator toneGen = new ToneGenerator(AudioManager.STREAM_MUSIC, 100);
            toneGen.startTone(ToneGenerator.TONE_PROP_NACK, 200);
            handler.postDelayed(toneGen::release, 250);
        }

        handler.postDelayed(() -> showRewardDialog(passed, accuracy), 500);
    }

    private void showRewardDialog(boolean passed, float accuracy) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_reward, null);

        TextView tvEmoji = dialogView.findViewById(R.id.tv_reward_emoji);
        TextView tvTitle = dialogView.findViewById(R.id.tv_reward_title);
        TextView tvMessage = dialogView.findViewById(R.id.tv_reward_message);
        Button btnNext = dialogView.findViewById(R.id.btn_next);

        int percent = (int) (accuracy * 100);

        if (passed) {
            tvEmoji.setText("⭐");
            tvTitle.setText(R.string.reward_correct_title);
            tvMessage.setText("Amazing! " + percent + "% accuracy!");

            ScaleAnimation anim = new ScaleAnimation(
                0.5f, 1.2f, 0.5f, 1.2f,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f);
            anim.setDuration(500);
            anim.setRepeatCount(1);
            anim.setRepeatMode(Animation.REVERSE);
            tvEmoji.startAnimation(anim);
        } else {
            tvEmoji.setText("💪");
            tvTitle.setText(R.string.reward_wrong_title);
            tvMessage.setText("Good try! Follow the guide and try again!");
        }

        boolean isLast = (currentQuestionIndex >= questions.size() - 1);
        if (passed) {
            btnNext.setText(isLast ? R.string.btn_finish : R.string.btn_next);
        } else {
            btnNext.setText("TRY AGAIN");
        }

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .setCancelable(false)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        btnNext.setOnClickListener(v -> {
            dialog.dismiss();
            if (passed) {
                if (isLast) finishGame();
                else loadNextQuestion();
            } else {
                // Retry: clear canvas and let them try again
                btnDone.setEnabled(true);
                drawingView.clear();
            }
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
        String message;
        if ("free_draw".equals(category) || "trace_lines".equals(category)) {
            // No percentage calculation for drawing games
            message = "Great job on your drawing!";
        } else {
            // Show score/percentage for other games
            message = getString(R.string.session_complete_format, score, questions.size());
        }
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


    @Override
    protected void onDestroy() {
        if (ttsManager != null) {
            ttsManager.shutdown();
        }
        handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }
}
