package com.example.grow21;

import android.animation.AnimatorInflater;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.ImageButton;
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

/**
 * Memory Flip Card Game:
 * - Cards start face down (purple back)
 * - Shows all cards for 2 seconds as hint
 * - Tap to flip, match pairs
 * - No timers, no penalties
 * - Match → cards stay open (green border)
 * - No match → flip back slowly
 */
public class MemoryFlipActivity extends AppCompatActivity {

    private ImageButton btnClose;
    private ProgressBar progressBar;
    private TextView tvQuestion, tvHint;
    private GridLayout gridCards;

    private List<QuestionModel> questions;
    private int currentQuestionIndex = 0;

    private List<String> cardData;       // All card labels (duplicated + shuffled)
    private List<CardView> cardViews;    // Card view references
    private List<Boolean> isFlipped;     // Track flip state
    private List<Boolean> isMatched;     // Track matched state

    private int firstFlippedIndex = -1;
    private int matchesFound = 0;
    private int totalPairs = 0;
    private boolean isProcessing = false;

    private DatabaseHelper dbHelper;
    private Handler handler;
    private static final String PREFS_NAME = "grow21_prefs";
    private TTSManager ttsManager;
    private boolean ttsEnabled = false;
    private boolean soundEnabled = true; // Add this

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_memory_flip);

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        ttsEnabled = prefs.getBoolean("voice_instructions", false);
        soundEnabled = prefs.getBoolean("sound_effects", true);

        if (ttsEnabled) {
            ttsManager = new TTSManager(this);
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
        tvHint = findViewById(R.id.tv_hint);
        gridCards = findViewById(R.id.grid_cards);

        btnClose.setOnClickListener(v -> finish());
    }

    private void loadQuestions() {
        questions = QuestionLoader.loadQuestionsByType(this, "memory");
        if (questions.isEmpty()) {
            Toast.makeText(this, "No memory games available", Toast.LENGTH_SHORT).show();
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
        matchesFound = 0;
        firstFlippedIndex = -1;
        isProcessing = false;

        gridCards.removeAllViews();

        // Build card data: duplicate pairs and shuffle
        List<String> pairs = q.getPairs();
        if (pairs == null || pairs.isEmpty()) {
            Toast.makeText(this, "Invalid memory game data", Toast.LENGTH_SHORT).show();
            return;
        }

        totalPairs = pairs.size();
        cardData = new ArrayList<>();
        for (String p : pairs) {
            cardData.add(p);
            cardData.add(p); // Duplicate for matching
        }
        Collections.shuffle(cardData);

        int numCards = cardData.size();
        int columns = numCards <= 4 ? 2 : 3;
        int rows = (int) Math.ceil((double) numCards / columns);

        gridCards.setColumnCount(columns);
        gridCards.setRowCount(rows);

        isFlipped = new ArrayList<>();
        isMatched = new ArrayList<>();
        cardViews = new ArrayList<>();

        int cardSize = calculateCardSize(columns);

        for (int i = 0; i < numCards; i++) {
            CardView card = createCard(i, cardSize);
            cardViews.add(card);
            isFlipped.add(false);
            isMatched.add(false);
            gridCards.addView(card);
        }

        int progress = (int) (((float) currentQuestionIndex / questions.size()) * 100);
        progressBar.setProgress(progress);

        // Show hint: reveal all cards for 2 seconds
        showHint();
    }

    private CardView createCard(int index, int size) {
        CardView card = new CardView(this);
        card.setRadius(dpToPx(16));
        card.setCardElevation(dpToPx(4));
        card.setUseCompatPadding(true);

        GridLayout.LayoutParams params = new GridLayout.LayoutParams();
        params.width = size;
        params.height = size;
        params.setMargins(dpToPx(6), dpToPx(6), dpToPx(6), dpToPx(6));
        params.setGravity(Gravity.CENTER);
        card.setLayoutParams(params);

        // Back view (default — purple gradient)
        TextView tvBack = new TextView(this);
        tvBack.setText("❓");
        tvBack.setTextSize(TypedValue.COMPLEX_UNIT_SP, 32);
        tvBack.setGravity(Gravity.CENTER);
        tvBack.setLayoutParams(new CardView.LayoutParams(
                CardView.LayoutParams.MATCH_PARENT,
                CardView.LayoutParams.MATCH_PARENT));
        card.addView(tvBack);

        card.setCardBackgroundColor(Color.parseColor("#A78BFA")); // Purple back
        card.setTag(index);

        AnimUtil.addPressEffect(card);
        card.setOnClickListener(v -> onCardTap(index));

        return card;
    }

    private void showHint() {
        tvHint.setVisibility(View.VISIBLE);
        tvHint.setText("Memorize the cards...");

        // Reveal all cards
        for (int i = 0; i < cardViews.size(); i++) {
            flipToFront(i, false);
        }

        // After 2 seconds, flip all back
        handler.postDelayed(() -> {
            for (int i = 0; i < cardViews.size(); i++) {
                flipToBack(i);
            }
            tvHint.setText("Tap a card to flip it!");
            handler.postDelayed(() -> tvHint.setVisibility(View.GONE), 1500);
        }, 2500);
    }

    private void onCardTap(int index) {
        if (isProcessing) return;
        if (isMatched.get(index)) return;
        if (isFlipped.get(index)) return;

        flipToFront(index, true);

        if (firstFlippedIndex == -1) {
            firstFlippedIndex = index;
        } else {
            isProcessing = true;
            int secondIndex = index;
            int firstIndex = firstFlippedIndex;
            firstFlippedIndex = -1;

            // Check match after exactly 1 second delay
            handler.postDelayed(() -> {
                if (cardData.get(firstIndex).equals(cardData.get(secondIndex))) {
                    // Match!
                    onMatch(firstIndex, secondIndex);
                } else {
                    // No match
                    onMismatch(firstIndex, secondIndex);
                }
            }, 1000);
        }
    }

    private void onMatch(int i1, int i2) {
        isMatched.set(i1, true);
        isMatched.set(i2, true);
        matchesFound++;

        // Green highlight
        cardViews.get(i1).setCardBackgroundColor(Color.parseColor("#C8E6C9"));
        cardViews.get(i2).setCardBackgroundColor(Color.parseColor("#C8E6C9"));

        // Bounce animation
        ScaleAnimation bounce = new ScaleAnimation(
                1f, 1.15f, 1f, 1.15f,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f);
        bounce.setDuration(200);
        bounce.setRepeatCount(1);
        bounce.setRepeatMode(Animation.REVERSE);
        cardViews.get(i1).startAnimation(bounce);
        cardViews.get(i2).startAnimation(bounce);

        // Sound
        ToneGenerator tone = new ToneGenerator(AudioManager.STREAM_MUSIC, 80);
        tone.startTone(ToneGenerator.TONE_PROP_ACK, 200);
        handler.postDelayed(tone::release, 250);

        isProcessing = false;

        if (matchesFound >= totalPairs) {
            handler.postDelayed(this::onRoundComplete, 600);
        }
    }

    private void onMismatch(int i1, int i2) {
        // Flip back
        flipToBack(i1);
        flipToBack(i2);

        ToneGenerator tone = new ToneGenerator(AudioManager.STREAM_MUSIC, 60);
        tone.startTone(ToneGenerator.TONE_PROP_NACK, 100);
        handler.postDelayed(tone::release, 150);

        isProcessing = false;
    }

    private void flipToFront(int index, boolean animate) {
        isFlipped.set(index, true);
        CardView card = cardViews.get(index);

        if (animate) {
            // True 3D flip animation
            ObjectAnimator flipOut = ObjectAnimator.ofFloat(card, "rotationY", 0f, 90f);
            flipOut.setDuration(150);
            flipOut.setInterpolator(new AccelerateDecelerateInterpolator());

            ObjectAnimator flipIn = ObjectAnimator.ofFloat(card, "rotationY", -90f, 0f);
            flipIn.setDuration(150);
            flipIn.setInterpolator(new AccelerateDecelerateInterpolator());

            flipOut.addListener(new android.animation.AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(android.animation.Animator animation) {
                    showFrontContent(card, index);
                    flipIn.start();
                }
            });
            flipOut.start();
        } else {
            showFrontContent(card, index);
        }
    }

    private void flipToBack(int index) {
        isFlipped.set(index, false);
        CardView card = cardViews.get(index);

        ObjectAnimator flipOut = ObjectAnimator.ofFloat(card, "rotationY", 0f, 90f);
        flipOut.setDuration(150);

        ObjectAnimator flipIn = ObjectAnimator.ofFloat(card, "rotationY", -90f, 0f);
        flipIn.setDuration(150);

        flipOut.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(android.animation.Animator animation) {
                showBackContent(card);
                flipIn.start();
            }
        });
        flipOut.start();
    }

    private void showFrontContent(CardView card, int index) {
        card.removeAllViews();
        card.setCardBackgroundColor(Color.WHITE);

        TextView tv = new TextView(this);
        tv.setText(cardData.get(index));
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
        tv.setTextColor(Color.parseColor("#2E2E2E"));
        tv.setTypeface(null, android.graphics.Typeface.BOLD);
        tv.setGravity(Gravity.CENTER);
        tv.setLayoutParams(new CardView.LayoutParams(
                CardView.LayoutParams.MATCH_PARENT,
                CardView.LayoutParams.MATCH_PARENT));
        card.addView(tv);
    }

    private void showBackContent(CardView card) {
        card.removeAllViews();
        card.setCardBackgroundColor(Color.parseColor("#A78BFA"));

        TextView tv = new TextView(this);
        tv.setText("❓");
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 32);
        tv.setGravity(Gravity.CENTER);
        tv.setLayoutParams(new CardView.LayoutParams(
                CardView.LayoutParams.MATCH_PARENT,
                CardView.LayoutParams.MATCH_PARENT));
        card.addView(tv);
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

        tvEmoji.setText("🧠");
        tvTitle.setText("Great Memory!");
        tvMessage.setText("You found all the pairs!");

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
        dbHelper.insertSession("puzzle_memory", questions.size(), questions.size());
        new AlertDialog.Builder(this)
                .setTitle(R.string.session_complete_title)
                .setMessage(getString(R.string.session_complete_format, questions.size(), questions.size()))
                .setCancelable(false)
                .setPositiveButton(R.string.btn_go_back, (d, w) -> {
                    d.dismiss();
                    finish();
                }).show();
    }

    private int calculateCardSize(int columns) {
        int screenWidth = getResources().getDisplayMetrics().widthPixels;
        int padding = dpToPx(24 * 2); // Grid margins
        int gaps = dpToPx(12 * columns);
        return (screenWidth - padding - gaps) / columns;
    }

    private int dpToPx(int dp) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, dp,
                getResources().getDisplayMetrics());
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }
}
