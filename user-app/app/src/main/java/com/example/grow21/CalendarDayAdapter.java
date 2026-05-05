package com.example.grow21;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Calendar;
import java.util.List;
import java.util.Set;

/**
 * Adapter for the monthly streak calendar grid.
 * Displays a 7-column grid of day cells. Empty cells represent padding
 * before the 1st of the month. Active days (with at least one session)
 * are highlighted with a pastel circle background.
 */
public class CalendarDayAdapter extends RecyclerView.Adapter<CalendarDayAdapter.DayViewHolder> {

    /** Each item is either null (empty padding cell) or a day number (1-31). */
    private final List<Integer> dayCells;
    /** Set of day numbers that have recorded activity. */
    private final Set<Integer> activeDays;
    /** The day-of-month for today, or -1 if the displayed month is not the current month. */
    private final int todayDay;

    /**
     * @param dayCells   list where null = empty cell, integer = day number
     * @param activeDays set of day numbers with at least one session
     * @param year       the year being displayed
     * @param month      the month being displayed (1-12)
     */
    public CalendarDayAdapter(List<Integer> dayCells, Set<Integer> activeDays,
                              int year, int month) {
        this.dayCells = dayCells;
        this.activeDays = activeDays;

        // Determine if this month is the current month so we can highlight "today"
        Calendar now = Calendar.getInstance();
        if (now.get(Calendar.YEAR) == year && (now.get(Calendar.MONTH) + 1) == month) {
            this.todayDay = now.get(Calendar.DAY_OF_MONTH);
        } else {
            this.todayDay = -1;
        }
    }

    @NonNull
    @Override
    public DayViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_calendar_day, parent, false);
        return new DayViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DayViewHolder holder, int position) {
        Integer day = dayCells.get(position);

        if (day == null) {
            // Empty padding cell — hide text, clear background
            holder.tvDayNumber.setText("");
            holder.tvDayNumber.setBackgroundResource(0);
            return;
        }

        holder.tvDayNumber.setText(String.valueOf(day));

        if (activeDays.contains(day)) {
            // Active day: show filled pastel circle
            holder.tvDayNumber.setBackgroundResource(R.drawable.bg_calendar_day_active);
            holder.tvDayNumber.setTextColor(
                    holder.itemView.getContext().getResources().getColor(R.color.colorPrimary));
        } else if (day == todayDay) {
            // Today but not active: show outlined circle
            holder.tvDayNumber.setBackgroundResource(R.drawable.bg_calendar_day_today);
            holder.tvDayNumber.setTextColor(
                    holder.itemView.getContext().getResources().getColor(R.color.colorPrimary));
        } else {
            // Regular day: no background
            holder.tvDayNumber.setBackgroundResource(0);
            holder.tvDayNumber.setTextColor(
                    holder.itemView.getContext().getResources().getColor(R.color.colorTextDark));
        }
    }

    @Override
    public int getItemCount() {
        return dayCells.size();
    }

    static class DayViewHolder extends RecyclerView.ViewHolder {
        TextView tvDayNumber;

        DayViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDayNumber = itemView.findViewById(R.id.tv_day_number);
        }
    }
}
