package com.example.velora;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CycleHistoryActivity extends AppCompatActivity {

    private RecyclerView rvFullHistory;
    private HistoryAdapter adapter;
    private DataManager dataManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cycle_history);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Cycle History");
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        dataManager = new DataManager(this);
        rvFullHistory = findViewById(R.id.rvFullHistory);
        rvFullHistory.setLayoutManager(new LinearLayoutManager(this));

        List<CycleEntry> history = dataManager.getCycleHistory();
        adapter = new HistoryAdapter(history, dataManager);
        rvFullHistory.setAdapter(adapter);
    }

    private static class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.ViewHolder> {
        private final List<CycleEntry> data;
        private final DataManager dataManager;

        HistoryAdapter(List<CycleEntry> data, DataManager dataManager) {
            this.data = data;
            this.dataManager = dataManager;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_cycle_history, parent, false);
            return new ViewHolder(v);
        }

        @SuppressLint("SetTextI18n")
        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            CycleEntry entry = data.get(position);
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
            holder.tvDates.setText(sdf.format(new Date(entry.getStartDate())) + " - " + sdf.format(new Date(entry.getEndDate())));

            long diff = entry.getEndDate() - entry.getStartDate();
            int durationDays = (int) (diff / (1000 * 60 * 60 * 24));
            holder.tvDuration.setText(durationDays + " days");

            holder.ivStress.setVisibility(entry.isStressed() ? View.VISIBLE : View.GONE);
            holder.ivIllness.setVisibility(entry.isIll() ? View.VISIBLE : View.GONE);
            holder.ivTravel.setVisibility(entry.hasTraveled() ? View.VISIBLE : View.GONE);

            // Cycle Dots Logic
            holder.llCycleDots.removeAllViews();
            int cycleLength = dataManager.getCycleLength();
            int ovulationDay = Math.max(1, cycleLength - 14);

            long nowMs = System.currentTimeMillis();
            long diffMs = nowMs - entry.getStartDate();
            int daysSinceStart = (int) (diffMs / (1000L * 60 * 60 * 24));
            int visibleDots = Math.max(1, Math.min(cycleLength, daysSinceStart + 1));

            int dotSize = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 10, holder.itemView.getResources().getDisplayMetrics());
            int margin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2, holder.itemView.getResources().getDisplayMetrics());

            for (int i = 1; i <= visibleDots; i++) {
                View dot = new View(holder.itemView.getContext());
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dotSize, dotSize);
                params.setMargins(margin, 0, margin, 0);
                dot.setLayoutParams(params);

                GradientDrawable shape = new GradientDrawable();
                shape.setShape(GradientDrawable.OVAL);

                int color;
                if (i <= durationDays) {
                    color = Color.parseColor("#FF4D4D"); // Pink/Red
                } else if (i == ovulationDay || (i >= ovulationDay - 2 && i <= ovulationDay + 1)) {
                    color = Color.parseColor("#00897B"); // Teal/Dark Green for fertile
                } else {
                    color = Color.parseColor("#B2EBF2"); // Light Blue
                }

                shape.setColor(color);
                dot.setBackground(shape);
                holder.llCycleDots.addView(dot);
            }
        }

        @Override
        public int getItemCount() {
            return data.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvDates, tvDuration;
            ImageView ivStress, ivIllness, ivTravel;
            LinearLayout llCycleDots;

            ViewHolder(View v) {
                super(v);
                tvDates = v.findViewById(R.id.tvHistoryDates);
                tvDuration = v.findViewById(R.id.tvHistoryDuration);
                ivStress = v.findViewById(R.id.ivStress);
                ivIllness = v.findViewById(R.id.ivIllness);
                ivTravel = v.findViewById(R.id.ivTravel);
                llCycleDots = v.findViewById(R.id.llCycleDots);
            }
        }
    }
}
