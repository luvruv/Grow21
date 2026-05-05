package com.example.grow21;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

/**
 * Sub-selection screen for Puzzles.
 * Routes to Color Sorting, Memory Flip, or Drag & Match.
 */
public class PuzzleSelectionActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_puzzle_selection);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        // Color Sorting
        CardView cardColorSort = findViewById(R.id.card_color_sort);
        AnimUtil.addPressEffect(cardColorSort);
        cardColorSort.setOnClickListener(v -> {
            startActivity(new Intent(this, ColorSortActivity.class));
        });

        // Memory Flip
        CardView cardMemoryFlip = findViewById(R.id.card_memory_flip);
        AnimUtil.addPressEffect(cardMemoryFlip);
        cardMemoryFlip.setOnClickListener(v -> {
            startActivity(new Intent(this, MemoryFlipActivity.class));
        });

        // Drag & Match (existing game)
        CardView cardDragMatch = findViewById(R.id.card_drag_match);
        AnimUtil.addPressEffect(cardDragMatch);
        cardDragMatch.setOnClickListener(v -> {
            Intent intent = new Intent(this, DragGameActivity.class);
            intent.putExtra("category", "shapes");
            startActivity(intent);
        });
    }
}
