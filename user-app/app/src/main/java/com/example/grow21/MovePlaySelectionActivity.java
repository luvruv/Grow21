package com.example.grow21;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

/**
 * Sub-selection screen for Move & Play.
 * Each card launches MotorGameActivity with a different category filter.
 */
public class MovePlaySelectionActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_move_play_selection);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        // Trace Lines → category "trace"
        CardView cardTraceLines = findViewById(R.id.card_trace_lines);
        AnimUtil.addPressEffect(cardTraceLines);
        cardTraceLines.setOnClickListener(v -> {
            Intent intent = new Intent(this, MotorGameActivity.class);
            intent.putExtra("category", "trace");
            startActivity(intent);
        });

        // Draw Shapes → category "draw"
        CardView cardDrawShapes = findViewById(R.id.card_draw_shapes);
        AnimUtil.addPressEffect(cardDrawShapes);
        cardDrawShapes.setOnClickListener(v -> {
            Intent intent = new Intent(this, MotorGameActivity.class);
            intent.putExtra("category", "draw");
            startActivity(intent);
        });

        // Free Draw → category "free"
        CardView cardFreeDraw = findViewById(R.id.card_free_draw);
        AnimUtil.addPressEffect(cardFreeDraw);
        cardFreeDraw.setOnClickListener(v -> {
            Intent intent = new Intent(this, MotorGameActivity.class);
            intent.putExtra("category", "free");
            startActivity(intent);
        });
    }
}
