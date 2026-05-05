package com.example.grow21;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

public class SkillSelectionActivity extends AppCompatActivity {

    private CardView cardWordplay, cardBrain, cardPuzzles;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_skill_selection);

        cardWordplay = findViewById(R.id.card_wordplay);
        cardBrain = findViewById(R.id.card_brain);
        cardPuzzles = findViewById(R.id.card_puzzles);

        // Micro-interactions
        AnimUtil.addPressEffect(cardWordplay);
        AnimUtil.addPressEffect(cardBrain);
        AnimUtil.addPressEffect(cardPuzzles);

        // Move & Play → Sub-selection screen with multiple motor activities
        cardWordplay.setOnClickListener(v -> {
            Intent intent = new Intent(SkillSelectionActivity.this, MovePlaySelectionActivity.class);
            startActivity(intent);
        });

        // Vocabulary → Tap game category
        cardBrain.setOnClickListener(v -> {
            Intent intent = new Intent(SkillSelectionActivity.this, TapGameActivity.class);
            intent.putExtra("category", "vocabulary");
            startActivity(intent);
        });

        // Puzzles → Sub-selection screen with multiple puzzle activities
        cardPuzzles.setOnClickListener(v -> {
            Intent intent = new Intent(SkillSelectionActivity.this, PuzzleSelectionActivity.class);
            startActivity(intent);
        });
    }
}
