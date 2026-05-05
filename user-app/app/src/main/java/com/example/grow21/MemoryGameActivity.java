package com.example.grow21;

import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MemoryGameActivity extends AppCompatActivity {

    private GridView gridView;
    private List<Integer> cards;
    private Set<Integer> matchedPositions = new HashSet<>();
    private Integer firstCardPos = null;
    private Integer secondCardPos = null;
    private boolean isProcessing = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_memory_game);

        gridView = findViewById(R.id.gridView);
        setupGame();
    }

    private void setupGame() {
        cards = new ArrayList<>();
        int[] drawables = {
                R.drawable.apple, R.drawable.ball, R.drawable.cat,
                R.drawable.dog, R.drawable.book
        };

        // Create pairs
        for (int drawable : drawables) {
            cards.add(drawable);
            cards.add(drawable);
        }
        // Add one more pair to make it 12 (4x3 grid)
        cards.add(R.drawable.apple);
        cards.add(R.drawable.apple);

        Collections.shuffle(cards);

        MemoryAdapter adapter = new MemoryAdapter();
        gridView.setAdapter(adapter);
        gridView.setOnItemClickListener((parent, view, position, id) -> {
            if (isProcessing || matchedPositions.contains(position)) return;
            handleCardClick(position);
        });
    }

    private void handleCardClick(int position) {
        if (firstCardPos != null && firstCardPos == position) return;

        if (firstCardPos == null) {
            firstCardPos = position;
            ((MemoryAdapter) gridView.getAdapter()).notifyDataSetChanged();
        } else {
            secondCardPos = position;
            ((MemoryAdapter) gridView.getAdapter()).notifyDataSetChanged();

            if (cards.get(firstCardPos).equals(cards.get(secondCardPos))) {
                matchedPositions.add(firstCardPos);
                matchedPositions.add(secondCardPos);
                firstCardPos = null;
                secondCardPos = null;
                if (matchedPositions.size() == cards.size()) {
                    Toast.makeText(this, "🎉 You found all matches!", Toast.LENGTH_SHORT).show();
                    new Handler().postDelayed(this::finish, 1500);
                }
            } else {
                isProcessing = true;
                new Handler().postDelayed(() -> {
                    firstCardPos = null;
                    secondCardPos = null;
                    isProcessing = false;
                    ((MemoryAdapter) gridView.getAdapter()).notifyDataSetChanged();
                }, 1000);
            }
        }
    }

    private class MemoryAdapter extends BaseAdapter {
        @Override
        public int getCount() { return cards.size(); }
        @Override
        public Object getItem(int position) { return cards.get(position); }
        @Override
        public long getItemId(int position) { return position; }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ImageView imageView;
            if (convertView == null) {
                imageView = new ImageView(MemoryGameActivity.this);
                imageView.setLayoutParams(new ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, 250));
                imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                imageView.setPadding(16, 16, 16, 16);
            } else {
                imageView = (ImageView) convertView;
            }

            boolean isFlipped = matchedPositions.contains(position) || 
                               (firstCardPos != null && firstCardPos == position) ||
                               (secondCardPos != null && secondCardPos == position);

            if (isFlipped) {
                imageView.setImageResource(cards.get(position));
                imageView.setBackgroundResource(R.drawable.bg_option_card_default);
            } else {
                imageView.setImageResource(0);
                imageView.setBackgroundResource(R.drawable.bg_play_icon_circle);
            }

            return imageView;
        }
    }
}
