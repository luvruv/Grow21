package com.example.grow21;

import android.content.SharedPreferences;
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
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.example.grow21.models.QuestionModel;

import java.util.List;
import java.util.Locale;

public class TapGameActivity extends AppCompatActivity {

    private ImageButton btnClose;
    private ProgressBar progressBar;
    private TextView tvQuestion;
    private ImageView ivQuestionImage;

    private CardView cardOption1, cardOption2, cardOption3, cardOption4;
    private FrameLayout frameOption1, frameOption2, frameOption3, frameOption4;
    private TextView tvOption1, tvOption2, tvOption3, tvOption4;
    private ImageView ivOption1, ivOption2, ivOption3, ivOption4;

    private List<QuestionModel> questions;
    private int currentQuestionIndex = 0;
    private int score = 0;
    private String category;
    private boolean isAnswered = false;

    private DatabaseHelper dbHelper;
    private Handler handler;
    private TTSManager ttsManager;
    private boolean ttsEnabled = false;
    private boolean soundEnabled = true; // Add this

    private CardView[] optionCards;
    private FrameLayout[] optionFrames;
    private TextView[] optionTexts;
    private ImageView[] optionImages;

    private static final String PREFS_NAME = "grow21_prefs";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tap_game);

        handler = new Handler(Looper.getMainLooper());
        dbHelper = DatabaseHelper.getInstance(this);

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        ttsEnabled = prefs.getBoolean("voice_instructions", false);
        soundEnabled = prefs.getBoolean("sound_effects", true);

        if (ttsEnabled) {
            ttsManager = new TTSManager(this);
        }

        category = getIntent().getStringExtra("category");
        if (category == null) category = "vocabulary";

        initViews();
        loadQuestions();
    }

    private void initViews() {
        btnClose = findViewById(R.id.btn_close);
        progressBar = findViewById(R.id.progress_bar);
        tvQuestion = findViewById(R.id.tv_question);
        ivQuestionImage = findViewById(R.id.iv_question_image);

        cardOption1 = findViewById(R.id.card_option_1);
        cardOption2 = findViewById(R.id.card_option_2);
        cardOption3 = findViewById(R.id.card_option_3);
        cardOption4 = findViewById(R.id.card_option_4);

        frameOption1 = (FrameLayout) cardOption1.getChildAt(0);
        frameOption2 = (FrameLayout) cardOption2.getChildAt(0);
        frameOption3 = (FrameLayout) cardOption3.getChildAt(0);
        frameOption4 = (FrameLayout) cardOption4.getChildAt(0);

        tvOption1 = findViewById(R.id.tv_option_1);
        tvOption2 = findViewById(R.id.tv_option_2);
        tvOption3 = findViewById(R.id.tv_option_3);
        tvOption4 = findViewById(R.id.tv_option_4);

        ivOption1 = findViewById(R.id.iv_option_1);
        ivOption2 = findViewById(R.id.iv_option_2);
        ivOption3 = findViewById(R.id.iv_option_3);
        ivOption4 = findViewById(R.id.iv_option_4);

        optionCards = new CardView[]{cardOption1, cardOption2, cardOption3, cardOption4};
        optionFrames = new FrameLayout[]{frameOption1, frameOption2, frameOption3, frameOption4};
        optionTexts = new TextView[]{tvOption1, tvOption2, tvOption3, tvOption4};
        optionImages = new ImageView[]{ivOption1, ivOption2, ivOption3, ivOption4};

        btnClose.setOnClickListener(v -> finish());

        for (int i = 0; i < optionCards.length; i++) {
            final int index = i;
            optionCards[i].setOnClickListener(v -> onOptionSelected(index));
        }
    }

    private void loadQuestions() {
        questions = QuestionLoader.loadQuestionsByCategory(this, category);
        if (questions.isEmpty()) {
            questions = QuestionLoader.loadShuffledQuestions(this);
            // Filter to only tap questions
            questions.removeIf(q -> !q.getType().equals("tap"));
        }

        if (questions.isEmpty()) {
            Toast.makeText(this, "No questions available", Toast.LENGTH_SHORT).show();
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

        isAnswered = false;
        QuestionModel question = questions.get(currentQuestionIndex);

        tvQuestion.setText(question.getQuestion());

        if (question.getImage() != null && !question.getImage().isEmpty()) {
            int imageResId = getResources().getIdentifier(question.getImage(), "drawable", getPackageName());
            if (imageResId != 0) {
                ivQuestionImage.setImageResource(imageResId);
                ivQuestionImage.setVisibility(View.VISIBLE);
            } else {
                ivQuestionImage.setImageResource(R.drawable.bg_placeholder_mascot);
                ivQuestionImage.setVisibility(View.VISIBLE);
            }
        } else {
            ivQuestionImage.setVisibility(View.GONE);
        }

        List<String> options = question.getOptions();
        for (int i = 0; i < optionCards.length; i++) {
            if (i < options.size()) {
                optionCards[i].setVisibility(View.VISIBLE);
                String opt = options.get(i);
                
                // Check if option is an image resource
                int optImageResId = getResources().getIdentifier(opt, "drawable", getPackageName());
                if (optImageResId != 0) {
                    optionImages[i].setImageResource(optImageResId);
                    optionImages[i].setVisibility(View.VISIBLE);
                    optionTexts[i].setVisibility(View.GONE);
                    optionCards[i].setTag(opt);
                } else {
                    optionImages[i].setVisibility(View.GONE);
                    optionTexts[i].setVisibility(View.VISIBLE);
                    optionTexts[i].setText(opt);
                    optionCards[i].setTag(opt);
                }
                
                optionFrames[i].setBackgroundResource(R.drawable.bg_option_card_default);
                optionCards[i].setClickable(true);
                optionCards[i].setEnabled(true);
            } else {
                optionCards[i].setVisibility(View.GONE);
            }
        }

        int progress = (int) (((float) currentQuestionIndex / questions.size()) * 100);
        progressBar.setProgress(progress);

        if (ttsEnabled && ttsManager != null) {
            ttsManager.speak("Match the shapes!");
        }
        
        // Handle optional audio
        if (question.getAudio() != null && !question.getAudio().isEmpty()) {
            ToneGenerator toneGen = new ToneGenerator(AudioManager.STREAM_MUSIC, 100);
            toneGen.startTone(ToneGenerator.TONE_DTMF_1, 300);
            handler.postDelayed(toneGen::release, 350);
        }
    }

    private void onOptionSelected(int selectedIndex) {
        if (isAnswered) return;
        isAnswered = true;

        for (CardView card : optionCards) {
            card.setClickable(false);
            card.setEnabled(false);
        }

        QuestionModel question = questions.get(currentQuestionIndex);
        String selectedAnswer = (String) optionCards[selectedIndex].getTag();
        String correctAnswer = question.getAnswer();
        boolean isCorrect = selectedAnswer.equals(correctAnswer);

        if (isCorrect) {
            optionFrames[selectedIndex].setBackgroundResource(R.drawable.bg_option_card_correct);
            score++;
            ToneGenerator toneGen = new ToneGenerator(AudioManager.STREAM_MUSIC, 100);
            toneGen.startTone(ToneGenerator.TONE_PROP_ACK, 200);
            handler.postDelayed(toneGen::release, 250);
        } else {
            optionFrames[selectedIndex].setBackgroundResource(R.drawable.bg_option_card_wrong);
            for (int i = 0; i < optionCards.length; i++) {
                if (optionCards[i].getVisibility() == View.VISIBLE && 
                    optionCards[i].getTag().equals(correctAnswer)) {
                    optionFrames[i].setBackgroundResource(R.drawable.bg_option_card_correct);
                    break;
                }
            }
            ToneGenerator toneGen = new ToneGenerator(AudioManager.STREAM_MUSIC, 100);
            toneGen.startTone(ToneGenerator.TONE_PROP_NACK, 200);
            handler.postDelayed(toneGen::release, 250);
        }

        dbHelper.insertPerformance(question.getId(), question.getCategory(), isCorrect);
        handler.postDelayed(() -> showRewardDialog(isCorrect, correctAnswer), 1200);
    }

    private void showRewardDialog(boolean isCorrect, String correctAnswer) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_reward, null);

        TextView tvEmoji = dialogView.findViewById(R.id.tv_reward_emoji);
        TextView tvTitle = dialogView.findViewById(R.id.tv_reward_title);
        TextView tvMessage = dialogView.findViewById(R.id.tv_reward_message);
        Button btnNext = dialogView.findViewById(R.id.btn_next);

        if (isCorrect) {
            tvEmoji.setText("⭐");
            tvTitle.setText(R.string.reward_correct_title);
            tvMessage.setText(R.string.reward_correct_message);
            
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
            int optImageResId = getResources().getIdentifier(correctAnswer, "drawable", getPackageName());
            if (optImageResId != 0) {
                tvMessage.setText("Good try! Keep learning!");
            } else {
                tvMessage.setText(getString(R.string.reward_wrong_message_format, correctAnswer));
            }
        }

        boolean isLast = (currentQuestionIndex >= questions.size() - 1);
        if (isCorrect) {
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
            if (isCorrect) {
                if (isLast) finishGame();
                else loadNextQuestion();
            } else {
                // Allow retry
                isAnswered = false;
                for (CardView card : optionCards) {
                    if (card.getVisibility() == View.VISIBLE) {
                        card.setClickable(true);
                        card.setEnabled(true);
                        FrameLayout frame = (FrameLayout) card.getChildAt(0);
                        frame.setBackgroundResource(R.drawable.bg_option_card_default);
                    }
                }
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


    @Override
    protected void onDestroy() {
        if (ttsManager != null) {
            ttsManager.shutdown();
        }
        handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }
}
