package com.example.velora;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.bumptech.glide.Glide;
import com.example.velora.databinding.ActivityMainBinding;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class MainActivity extends BaseActivity {

    private ActivityMainBinding binding;
    private CycleManager cycleManager;
    private Calendar lastPeriodSelected;

    private TextView tvStatus, tvNextPeriodPrediction, tvLastPeriodDate;
    private TextView tvDischargeTips, tvSexTips, tvBloatingTips, tvBleedingTips, tvMealTips;
    private TextView tvTrendStatus, tvTrendDescription;
    private EditText etCycleLength, etPeriodDuration;
    private CycleCircleView cycleCircleView;
    private TrendGraphView trendGraphView;
    private RecyclerView rvHistory;
    private HistoryAdapter historyAdapter;

    private final Handler animationHandler = new Handler(Looper.getMainLooper());
    private Runnable prePeriodRunnable;
    private Runnable tipsScrollRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        cycleManager = new CycleManager(dataManager);

        initViews();
        setupListeners();
        loadSettings();
        updateAllCalculations();
        updateGreeting();
        renderHistoryAndGraph();
        
        startAnimations();

        // Presence
        if (SupabaseHelper.isLoggedIn()) {
            SupabaseHelper.setPresence(SupabaseHelper.getCurrentUserId(), "online");
        }

        // Schedule Period Reminders
        schedulePeriodReminders();
        
        // Request Notification Permission for Android 13+
        requestNotificationPermission();
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1001);
            }
        }
    }

    @Override
    protected void schedulePeriodReminders() {
        Calendar calendar = Calendar.getInstance();
        long now = calendar.getTimeInMillis();

        // Target: 9:00 AM
        calendar.set(Calendar.HOUR_OF_DAY, 9);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        // If 9 AM has passed, schedule for tomorrow
        if (calendar.getTimeInMillis() <= now) {
            calendar.add(Calendar.DAY_OF_YEAR, 1);
        }

        long initialDelay = calendar.getTimeInMillis() - now;

        PeriodicWorkRequest reminderRequest =
                new PeriodicWorkRequest.Builder(PeriodReminderWorker.class, 24, TimeUnit.HOURS)
                        .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
                        .build();

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "PeriodReminderWork",
                ExistingPeriodicWorkPolicy.KEEP,
                reminderRequest
        );
    }

    private void updateGreeting() {
        Calendar cal = Calendar.getInstance();
        int hour = cal.get(Calendar.HOUR_OF_DAY);
        String greeting;
        if (hour >= 0 && hour < 12) {
            greeting = "Good morning";
        } else if (hour >= 12 && hour < 17) {
            greeting = "Good afternoon";
        } else {
            greeting = "Good evening";
        }

        String name = dataManager.getUserName();
        if (name == null || name.isEmpty() || name.equals("User Name")) {
            name = "User";
        } else {
            name = name.split(" ")[0];
        }

        binding.tvHeader.setText(greeting + ", " + name);

        SimpleDateFormat sdf = new SimpleDateFormat("EEEE, d MMM", Locale.getDefault());
        binding.tvCurrentDate.setText(sdf.format(cal.getTime()));
    }

    private void initViews() {
        tvStatus = binding.tvStatus;
        tvNextPeriodPrediction = binding.tvNextPeriodPrediction;
        tvLastPeriodDate = binding.tvLastPeriodDate;
        etCycleLength = binding.etCycleLength;
        etPeriodDuration = binding.etPeriodDuration;
        cycleCircleView = binding.cycleCircleView;
        
        tvDischargeTips = binding.tvDischargeTips;
        tvSexTips = binding.tvSexTips;
        tvBloatingTips = binding.tvBloatingTips;
        tvBleedingTips = binding.tvBleedingTips;
        tvMealTips = binding.tvMealTips;
        
        tvTrendStatus = binding.tvTrendStatus;
        tvTrendDescription = binding.tvTrendDescription;
        trendGraphView = binding.trendGraphView;

        rvHistory = binding.rvHistory;
        rvHistory.setLayoutManager(new LinearLayoutManager(this));
        historyAdapter = new HistoryAdapter(new ArrayList<>(), dataManager);
        rvHistory.setAdapter(historyAdapter);

        String photo = dataManager.getUserPhoto();
        if (photo != null && !photo.isEmpty()) {
            Glide.with(this).load(photo).placeholder(R.drawable.user_account_icon).into(binding.ivUserHeader);
        }
    }

    private void setupListeners() {
        binding.btnSetLastPeriod.setOnClickListener(v -> showDatePicker());
        binding.btnSaveSettings.setOnClickListener(v -> saveSettings());
        binding.btnResetCycle.setOnClickListener(v -> resetCycleData());

        binding.btnLogToday.setOnClickListener(v -> startActivity(new Intent(this, SymptomsActivity.class)));
        binding.btnStopGuessing.setOnClickListener(v -> startActivity(new Intent(this, ChatActivity.class)));
        binding.btnSexDrive.setOnClickListener(v -> startActivity(new Intent(this, LibidoActivity.class)));
        binding.btnPCOS.setOnClickListener(v -> startActivity(new Intent(this, PCOSAssessmentActivity.class)));

        binding.cvDischargeTips.setOnClickListener(v -> openTip("discharge types to watch out for"));
        binding.cvSexTips.setOnClickListener(v -> openTip("non-preventive sex ideas"));
        binding.cvBloatingTips.setOnClickListener(v -> openTip("how to prevent bloating"));
        binding.cvBleedingTips.setOnClickListener(v -> openTip("how to manage heavy bleeding"));
        binding.cvMealTips.setOnClickListener(v -> openTip("best meals during periods"));

        binding.ivUserHeader.setOnClickListener(v -> startActivity(new Intent(this, ProfileActivity.class)));

        binding.btnFooterToday.setOnClickListener(v -> binding.mainScrollView.smoothScrollTo(0, 0));
        binding.btnFooterInsights.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://gemini.google.com/"));
            startActivity(intent);
        });
        binding.btnFooterNotes.setOnClickListener(v -> startActivity(new Intent(this, NotesActivity.class)));
        binding.btnHealthAssistant.setOnClickListener(v -> startActivity(new Intent(this, ChatActivity.class)));
        binding.btnCommunityChat.setOnClickListener(v -> startActivity(new Intent(this, SecretChatActivity.class)));
        binding.btnPartnerChat.setOnClickListener(v -> {
            if (dataManager.getPartnerPhone().isEmpty()) {
                startActivity(new Intent(this, PartnerActivity.class));
            } else {
                startActivity(new Intent(this, PartnerChatActivity.class));
            }
        });

        binding.tvSeeAllHistory.setOnClickListener(v -> startActivity(new Intent(this, CycleHistoryActivity.class)));
    }

    private void startAnimations() {
        prePeriodRunnable = new Runnable() {
            @Override
            public void run() {
                int currentChild = binding.vfPrePeriodInfo.getDisplayedChild();
                int nextChild = (currentChild + 1) % binding.vfPrePeriodInfo.getChildCount();
                binding.vfPrePeriodInfo.setDisplayedChild(nextChild);

                long delay = 10000;
                if (nextChild == 2) {
                    delay = 40000;
                }
                animationHandler.postDelayed(this, delay);
            }
        };
        animationHandler.postDelayed(prePeriodRunnable, 10000);

        tipsScrollRunnable = new Runnable() {
            long startTime = System.currentTimeMillis();
            boolean isPaused = false;

            @Override
            public void run() {
                long elapsed = System.currentTimeMillis() - startTime;
                
                if (!isPaused) {
                    if (elapsed < 60000) {
                        View content = binding.hsvCycleTips.getChildAt(0);
                        if (content != null) {
                            int maxScroll = content.getWidth() - binding.hsvCycleTips.getWidth();
                            if (maxScroll > 0) {
                                float progress = (float) elapsed / 60000f;
                                binding.hsvCycleTips.setScrollX((int) (progress * maxScroll));
                            }
                        }
                        animationHandler.postDelayed(this, 30);
                    } else {
                        isPaused = true;
                        startTime = System.currentTimeMillis();
                        animationHandler.postDelayed(this, 30000);
                    }
                } else {
                    isPaused = false;
                    startTime = System.currentTimeMillis();
                    binding.hsvCycleTips.smoothScrollTo(0, 0);
                    animationHandler.postDelayed(this, 1000);
                }
            }
        };
        animationHandler.postDelayed(tipsScrollRunnable, 2000);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        animationHandler.removeCallbacks(prePeriodRunnable);
        animationHandler.removeCallbacks(tipsScrollRunnable);
    }

    private void openTip(String query) {
        Intent intent = new Intent(this, ChatActivity.class);
        intent.putExtra("initial_query", "Tell me about " + query);
        startActivity(intent);
    }

    private void resetCycleData() {
        new AlertDialog.Builder(this)
                .setTitle("Reset Cycle Data")
                .setMessage("This will clear all your cycle history and settings. Are you sure?")
                .setPositiveButton("Reset", (dialog, which) -> {
                    dataManager.clearCycleData();
                    lastPeriodSelected = null;
                    loadSettings();
                    updateAllCalculations();
                    renderHistoryAndGraph();
                    tvLastPeriodDate.setText("Not set");
                    Toast.makeText(this, "Cycle data reset", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void loadSettings() {
        lastPeriodSelected = dataManager.getLastPeriod();
        etCycleLength.setText(String.valueOf(dataManager.getCycleLength()));
        etPeriodDuration.setText(String.valueOf(dataManager.getPeriodDuration()));
        if (lastPeriodSelected != null) {
            tvLastPeriodDate.setText(new SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(lastPeriodSelected.getTime()));
        } else {
            tvLastPeriodDate.setText("Not set");
        }
    }

    private void showDatePicker() {
        int duration = 5;
        try {
            duration = Integer.parseInt(etPeriodDuration.getText().toString());
        } catch (Exception ignored) {}
        
        final int finalDuration = duration;
        CustomCalendarDialog dialog = new CustomCalendarDialog(this, finalDuration, lastPeriodSelected, (calendar, isStressed, isIll, hasTraveled) -> {
            lastPeriodSelected = calendar;
            
            List<CycleEntry> history = dataManager.getCycleHistory();
            long endDate = calendar.getTimeInMillis() + (long)finalDuration * 24 * 60 * 60 * 1000;
            history.add(0, new CycleEntry(calendar.getTimeInMillis(), endDate, isStressed, isIll, hasTraveled));
            dataManager.saveCycleHistory(history);

            SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
            String dateStr = sdf.format(lastPeriodSelected.getTime());
            List<String> legacyHistory = dataManager.getHistory();
            String entryToAdd = dateStr;
            int cycleLen = -1;
            if (!legacyHistory.isEmpty()) {
                Date lastDate = CycleHelper.parseHistoryDate(legacyHistory.get(legacyHistory.size() - 1));
                if (lastDate != null) {
                    Calendar cPrev = Calendar.getInstance(); cPrev.setTime(lastDate);
                    cycleLen = CycleHelper.daysUntil(cPrev, lastPeriodSelected);
                    entryToAdd += " (" + cycleLen + " days)";
                }
            }
            tvLastPeriodDate.setText(cycleLen != -1 ? dateStr + " (" + cycleLen + " days)" : dateStr);
            dataManager.addToHistory(entryToAdd);
            
            saveSettings();
        });
        dialog.show();
    }

    private void saveSettings() {
        if (lastPeriodSelected == null) {
            Toast.makeText(this, "Please select last period date first", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            int cycle = Integer.parseInt(etCycleLength.getText().toString());
            int duration = Integer.parseInt(etPeriodDuration.getText().toString());
            dataManager.saveSettings(lastPeriodSelected, cycle, duration);
            updateAllCalculations();
            renderHistoryAndGraph();
            Toast.makeText(this, "Settings saved", Toast.LENGTH_SHORT).show();
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Please enter valid numbers", Toast.LENGTH_SHORT).show();
        }
    }

    @SuppressLint("SetTextI18n")
    private void updateAllCalculations() {
        int baseCycle = dataManager.getCycleLength();
        int baseDuration = dataManager.getPeriodDuration();
        Calendar today = Calendar.getInstance();

        if (lastPeriodSelected == null) {
            tvStatus.setText("Set last period");
            tvNextPeriodPrediction.setText("");
            if (cycleCircleView != null) cycleCircleView.setCycleData(baseCycle, baseDuration, 1, 14, "Set last period", "--", "--", "--");
            return;
        }

        CycleManager.Prediction p = cycleManager.getAdvancedPrediction();
        tvNextPeriodPrediction.setText("Expected: " + p.getRangeString());
        
        int dynamicDuration = p.getAvgDuration();

        long diffMillis = today.getTimeInMillis() - lastPeriodSelected.getTimeInMillis();
        long diffDays = diffMillis / (1000 * 60 * 60 * 24);
        int currentDay = (int) ((diffDays % baseCycle + baseCycle) % baseCycle) + 1;
        int ovulationDay = baseCycle - 14; 

        int daysToPeriod = baseCycle - currentDay + 1;
        String nextStr = (daysToPeriod == 1) ? "Tomorrow" : "in " + daysToPeriod + " days";

        int daysToOvulation;
        if (currentDay <= ovulationDay) {
            daysToOvulation = ovulationDay - currentDay;
        } else {
            daysToOvulation = (int) ((baseCycle - currentDay) + ovulationDay);
        }
        
        String ovStr;
        if (daysToOvulation == 0) ovStr = "Today";
        else if (daysToOvulation == 1) ovStr = "Tomorrow";
        else ovStr = "in " + daysToOvulation + " days";

        String phase;
        if (currentDay <= dynamicDuration) phase = "Menstruation";
        else if (currentDay == ovulationDay) phase = "Ovulation";
        else if (currentDay >= (ovulationDay - 5) && currentDay <= (ovulationDay + 1)) phase = "Fertile Window";
        else if (currentDay < ovulationDay) phase = "Follicular Phase";
        else if (daysToPeriod <= 3) phase = "PMS / Luteal";
        else phase = "Luteal Phase";

        tvStatus.setText(phase);
        
        if (cycleCircleView != null) {
            cycleCircleView.setCycleData(baseCycle, dynamicDuration, currentDay, ovulationDay, phase, nextStr, ovStr, p.getFertileRange());
        }

        if (phase.equals("Ovulation") || phase.equals("Fertile Window")) {
            tvDischargeTips.setText("High fertility! Watch for Egg-white discharge.");
            tvSexTips.setText("High risk days. Use protection if not planning.");
            tvBloatingTips.setText("Light exercise and water can help bloating.");
            tvBleedingTips.setText("Monitor your energy levels.");
            tvMealTips.setText("Zinc-rich foods are beneficial now.");
        } else if (phase.equals("Menstruation")) {
            tvDischargeTips.setText("Period flow is active.");
            tvSexTips.setText("Low risk, but comfort is priority.");
            tvBloatingTips.setText("Warm compress helps with cramps.");
            tvBleedingTips.setText("Change pads/tampons every 4-6 hours.");
            tvMealTips.setText("Iron-rich foods (spinach, lentils) are best.");
        } else {
            tvDischargeTips.setText("Dry or sticky discharge is normal.");
            tvSexTips.setText("Lower risk days.");
            tvBloatingTips.setText("Stay hydrated and limit salt.");
            tvBleedingTips.setText("No bleeding expected.");
            tvMealTips.setText("Balanced diet with fiber.");
        }
    }

    private void renderHistoryAndGraph() {
        List<CycleEntry> history = dataManager.getCycleHistory();
        historyAdapter.updateData(history);

        if (history.isEmpty()) {
            if (tvTrendStatus != null) tvTrendStatus.setText("No data yet");
            if (tvTrendDescription != null) tvTrendDescription.setText("Log your periods to see trends.");
            if (trendGraphView != null) trendGraphView.setData(new ArrayList<>(), dataManager.getCycleLength());
            return;
        }

        List<Integer> lengths = new ArrayList<>();
        for (int i = 0; i < history.size() - 1; i++) {
            long currentStart = history.get(i).getStartDate();
            long nextStart = history.get(i + 1).getStartDate();
            int len = (int) ((currentStart - nextStart) / (1000 * 60 * 60 * 24));
            if (len > 0 && len < 100) lengths.add(len);
        }

        List<Integer> graphData = new ArrayList<>(lengths);
        Collections.reverse(graphData);

        int avg = dataManager.getCycleLength();
        if (trendGraphView != null) {
            trendGraphView.setData(graphData, avg);
        }

        analyzeTrends(lengths);
    }

    private void analyzeTrends(List<Integer> lengths) {
        if (lengths.isEmpty()) {
            if (tvTrendStatus != null) tvTrendStatus.setText("Data needed");
            if (tvTrendDescription != null) tvTrendDescription.setText("Log at least two periods to see cycle regularity and trends.");
            return;
        }

        int latest = lengths.get(0);
        boolean isNormal = latest >= 21 && latest <= 35;

        boolean isRegular = true;
        if (lengths.size() >= 2) {
            int max = Collections.max(lengths);
            int min = Collections.min(lengths);
            if (max - min > 7) isRegular = false;
        }

        StringBuilder status = new StringBuilder();
        status.append(isNormal ? "Normal" : "Abnormal");
        status.append(" & ");
        status.append(isRegular ? "Regular" : "Irregular");
        status.append(" Cycle");

        if (tvTrendStatus != null) tvTrendStatus.setText(status.toString());

        if (tvTrendDescription != null) {
            if (!isNormal || !isRegular) {
                tvTrendDescription.setText("Suggestions: Stress, changes in diet, or intense exercise can affect your cycle. If this persists for 3+ cycles, consider consulting a healthcare professional.");
            } else {
                tvTrendDescription.setText("Your cycle patterns look healthy! Keep tracking to stay informed about your body's rhythm.");
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateAllCalculations();
        updateGreeting();
        renderHistoryAndGraph();
    }

    private static class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.ViewHolder> {
        private final List<CycleEntry> data;
        private final DataManager dataManager;

        HistoryAdapter(List<CycleEntry> data, DataManager dataManager) { 
            this.data = data; 
            this.dataManager = dataManager;
        }

        @SuppressLint("NotifyDataSetChanged")
        void updateData(List<CycleEntry> newData) {
            data.clear();
            data.addAll(newData);
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_cycle_history, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            CycleEntry entry = data.get(position);
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd", Locale.getDefault());
            holder.tvDates.setText(sdf.format(new Date(entry.getStartDate())) + " - " + sdf.format(new Date(entry.getEndDate())));
            
            long durationMillis = entry.getEndDate() - entry.getStartDate();
            int durationDays = (int) (durationMillis / (1000 * 60 * 60 * 24));
            holder.tvDuration.setText(durationDays + " days");

            holder.ivStress.setVisibility(entry.isStressed() ? View.VISIBLE : View.GONE);
            holder.ivIllness.setVisibility(entry.isIll() ? View.VISIBLE : View.GONE);
            holder.ivTravel.setVisibility(entry.hasTraveled() ? View.VISIBLE : View.GONE);

            holder.llCycleDots.removeAllViews();
            int cycleLength = dataManager.getCycleLength();
            int ovulationDay = Math.max(1, cycleLength - 14);

            long startMs = entry.getStartDate();
            long diffMs = System.currentTimeMillis() - startMs;
            int visibleDots = Math.max(1, Math.min(cycleLength, (int) (diffMs / (1000L * 60 * 60 * 24)) + 1));

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
                    color = Color.parseColor("#FF4D4D");
                } else if (i == ovulationDay || (i >= ovulationDay - 2 && i <= ovulationDay + 1)) {
                    color = Color.parseColor("#00897B");
                } else {
                    color = Color.parseColor("#B2EBF2");
                }

                shape.setColor(color);
                dot.setBackground(shape);
                holder.llCycleDots.addView(dot);
            }
        }

        @Override public int getItemCount() { return Math.min(data.size(), 3); }

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
