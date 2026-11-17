package com.example.velora;

import android.graphics.Color;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HistoryDetailActivity extends BaseActivity {

    private LinearLayout container;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history_detail);

        container = findViewById(R.id.llFullHistoryContainer);

        renderFullHistory();
    }

    private void renderFullHistory() {
        List<String> historyStr = dataManager.getHistory();
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
        List<Calendar> historyCals = new ArrayList<>();

        for (String ds : historyStr) {
            try {
                Date d = sdf.parse(ds);
                if (d != null) {
                    Calendar c = Calendar.getInstance();
                    c.setTime(d);
                    historyCals.add(c);
                }
            } catch (ParseException e) { e.printStackTrace(); }
        }

        Collections.sort(historyCals);
        
        // Calculate details newest first
        for (int i = historyCals.size() - 1; i >= 0; i--) {
            Calendar current = historyCals.get(i);
            int length = 0;
            String range = sdf.format(current.getTime());

            if (i > 0) {
                Calendar prev = historyCals.get(i - 1);
                length = CycleHelper.daysUntil(prev, current);
                range = sdf.format(prev.getTime()) + " - " + sdf.format(current.getTime());
            }

            addFullRow(length > 0 ? length + " days" : "First logged cycle", range, length);
        }
    }

    private void addFullRow(String title, String subtitle, int days) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(32, 32, 32, 32);
        row.setBackgroundResource(android.R.drawable.editbox_dropdown_light_frame);
        
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 0, 24);
        row.setLayoutParams(params);

        TextView tvTitle = new TextView(this);
        tvTitle.setText(title);
        tvTitle.setTextSize(18);
        tvTitle.setTextColor(Color.BLACK);
        tvTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        row.addView(tvTitle);

        TextView tvSub = new TextView(this);
        tvSub.setText(subtitle);
        tvSub.setTextColor(Color.GRAY);
        row.addView(tvSub);

        if (days > 0) {
            TextView tvStatus = new TextView(this);
            boolean isAbnormal = CycleHelper.isCycleAbnormal(days, dataManager.getCycleLength());
            tvStatus.setText(isAbnormal ? "⚠️ Irregular/Abnormal" : "✅ Regular/Normal");
            tvStatus.setTextColor(isAbnormal ? Color.parseColor("#FFA500") : Color.parseColor("#008000"));
            tvStatus.setPadding(0, 8, 0, 0);
            row.addView(tvStatus);
        }

        container.addView(row);
    }
}