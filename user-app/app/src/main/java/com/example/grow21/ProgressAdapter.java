package com.example.grow21;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.grow21.models.CategoryProgress;

import java.util.List;
import java.util.Locale;

public class ProgressAdapter extends RecyclerView.Adapter<ProgressAdapter.ProgressViewHolder> {

    private final List<CategoryProgress> progressList;

    public ProgressAdapter(List<CategoryProgress> progressList) {
        this.progressList = progressList;
    }

    @NonNull
    @Override
    public ProgressViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_progress_card, parent, false);
        return new ProgressViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProgressViewHolder holder, int position) {
        CategoryProgress progress = progressList.get(position);

        // Capitalize category name and replace underscores with spaces
        String originalCategory = progress.getCategory() != null ? progress.getCategory() : "";
        String categoryName = originalCategory;
        if (!categoryName.isEmpty()) {
            categoryName = categoryName.substring(0, 1).toUpperCase()
                    + categoryName.substring(1).replace("_", " ");
        }
        holder.tvCategoryName.setText(categoryName);

        // Specific logic for free_draw where grading doesn't apply
        if ("Draw".equalsIgnoreCase(originalCategory) || "free draw".equalsIgnoreCase(originalCategory)) {
            // "free_draw" only relies on completion
            String progressText = String.format(Locale.getDefault(),
                    "%d sessions completed", progress.getAttempted());
            holder.tvProgressText.setText(progressText);

            // Hide the accuracy percentage and progress bar completely for Free Draw
            holder.tvAccuracy.setVisibility(View.GONE);
            holder.progressCategory.setVisibility(View.GONE);

        } else {
            // Make sure these are visible for all other scored categories
            holder.tvAccuracy.setVisibility(View.VISIBLE);
            holder.progressCategory.setVisibility(View.VISIBLE);

            // Regular grading logic
            String progressText = String.format(Locale.getDefault(),
                    "%d / %d correct", progress.getCorrect(), progress.getAttempted());
            holder.tvProgressText.setText(progressText);

            // Set accuracy
            float accuracy = progress.getAccuracy();
            String accuracyText = String.format(Locale.getDefault(), "%.0f%%", accuracy);
            holder.tvAccuracy.setText(accuracyText);

            // Color code accuracy
            if (accuracy >= 70) {
                holder.tvAccuracy.setTextColor(Color.parseColor("#58CC02"));
            } else if (accuracy >= 40) {
                holder.tvAccuracy.setTextColor(Color.parseColor("#FFC800"));
            } else {
                holder.tvAccuracy.setTextColor(Color.parseColor("#FF4B4B"));
            }

            // Set progress bar
            holder.progressCategory.setProgress((int) accuracy);
        }

        // Set category icon color based on category
        View iconView = holder.viewCategoryIcon;
        switch (originalCategory.toLowerCase()) {
            case "vocabulary":
                iconView.setBackgroundResource(R.drawable.bg_skill_card_brain);
                break;
            case "colors":
            case "free_draw":
                iconView.setBackgroundResource(R.drawable.bg_skill_card_wordplay);
                break;
            case "shapes":
                iconView.setBackgroundResource(R.drawable.bg_skill_card_puzzles);
                break;
            default:
                iconView.setBackgroundResource(R.drawable.bg_pastel_blob_pink);
                break;
        }
    }

    @Override
    public int getItemCount() {
        return progressList.size();
    }

    static class ProgressViewHolder extends RecyclerView.ViewHolder {
        View viewCategoryIcon;
        TextView tvCategoryName;
        TextView tvProgressText;
        ProgressBar progressCategory;
        TextView tvAccuracy;

        public ProgressViewHolder(@NonNull View itemView) {
            super(itemView);
            viewCategoryIcon = itemView.findViewById(R.id.view_category_icon);
            tvCategoryName = itemView.findViewById(R.id.tv_category_name);
            tvProgressText = itemView.findViewById(R.id.tv_progress_text);
            progressCategory = itemView.findViewById(R.id.progress_category);
            tvAccuracy = itemView.findViewById(R.id.tv_accuracy);
        }
    }
}