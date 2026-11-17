package com.example.velora;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CycleManager {

    private final DataManager dataManager;

    public CycleManager(DataManager dataManager) {
        this.dataManager = dataManager;
    }

    public static class Prediction {
        private final long earliestDate;
        private final long latestDate;
        private final long predictedOvulation;
        private final int avgDuration;

        public Prediction(long earliestDate, long latestDate, long predictedOvulation, int avgDuration) {
            this.earliestDate = earliestDate;
            this.latestDate = latestDate;
            this.predictedOvulation = predictedOvulation;
            this.avgDuration = avgDuration;
        }

        public String getRangeString() {
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd", Locale.getDefault());
            if (earliestDate == latestDate) {
                // If they are the same, show a 3-day window for more logical prediction
                Calendar cal = Calendar.getInstance();
                cal.setTimeInMillis(earliestDate);
                cal.add(Calendar.DAY_OF_YEAR, -1);
                String start = sdf.format(cal.getTime());
                cal.add(Calendar.DAY_OF_YEAR, 2);
                String end = sdf.format(cal.getTime());
                return start + " - " + end;
            }
            return sdf.format(new Date(earliestDate)) + " - " + sdf.format(new Date(latestDate));
        }

        public String getFertileRange() {
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(predictedOvulation);
            
            cal.add(Calendar.DAY_OF_YEAR, -5);
            long start = cal.getTimeInMillis();
            
            cal.add(Calendar.DAY_OF_YEAR, 6); // -5 + 6 = +1
            long end = cal.getTimeInMillis();
            
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd", Locale.getDefault());
            return sdf.format(new Date(start)) + " - " + sdf.format(new Date(end));
        }

        public int getAvgDuration() {
            return avgDuration;
        }
    }

    public Prediction getAdvancedPrediction() {
        List<CycleEntry> history = dataManager.getCycleHistory();
        Calendar lastPeriod = dataManager.getLastPeriod();
        int defaultDuration = dataManager.getPeriodDuration();

        if (history.isEmpty() || lastPeriod == null) {
            int length = dataManager.getCycleLength();
            Calendar cal = (Calendar) lastPeriod.clone();
            if (cal == null) cal = Calendar.getInstance();
            cal.add(Calendar.DAY_OF_YEAR, length);
            long pred = cal.getTimeInMillis();
            return new Prediction(pred, pred, pred - (14L * 24 * 60 * 60 * 1000), defaultDuration);
        }

        List<Integer> validCycleLengths = new ArrayList<>();
        List<Integer> validDurations = new ArrayList<>();
        
        for (int i = 0; i < history.size(); i++) {
            CycleEntry current = history.get(i);
            
            // Collect durations
            long durationMillis = current.getEndDate() - current.getStartDate();
            int durationDays = (int) (durationMillis / (1000 * 60 * 60 * 24));
            if (durationDays > 0 && durationDays <= 10) {
                validDurations.add(durationDays);
            }

            // Collect cycle lengths
            if (i < history.size() - 1) {
                CycleEntry next = history.get(i + 1);
                long diffMillis = next.getStartDate() - current.getStartDate();
                int days = (int) (diffMillis / (1000 * 60 * 60 * 24));
                if (days >= 21 && days <= 45 && !current.isAnomalous()) {
                    validCycleLengths.add(days);
                }
            }
        }

        int avgDuration = defaultDuration;
        if (!validDurations.isEmpty()) {
            int sum = 0;
            for (int d : validDurations) sum += d;
            avgDuration = Math.round((float) sum / validDurations.size());
        }

        if (validCycleLengths.isEmpty()) {
            int length = dataManager.getCycleLength();
            Calendar cal = (Calendar) lastPeriod.clone();
            cal.add(Calendar.DAY_OF_YEAR, length);
            long pred = cal.getTimeInMillis();
            return new Prediction(pred, pred, pred - (14L * 24 * 60 * 60 * 1000), avgDuration);
        }

        if (validCycleLengths.size() > 6) {
            validCycleLengths = validCycleLengths.subList(validCycleLengths.size() - 6, validCycleLengths.size());
        }

        double weightedSum = 0;
        double weightTotal = 0;
        int minDays = validCycleLengths.get(0);
        int maxDays = validCycleLengths.get(0);

        for (int i = 0; i < validCycleLengths.size(); i++) {
            double weight = 1.0 + (i * 0.2);
            int days = validCycleLengths.get(i);
            weightedSum += days * weight;
            weightTotal += weight;
            
            if (days < minDays) minDays = days;
            if (days > maxDays) maxDays = days;
        }

        int avgCycleLength = (int) Math.round(weightedSum / weightTotal);
        
        Calendar calEarliest = (Calendar) lastPeriod.clone();
        calEarliest.add(Calendar.DAY_OF_YEAR, minDays);
        
        Calendar calLatest = (Calendar) lastPeriod.clone();
        calLatest.add(Calendar.DAY_OF_YEAR, maxDays);
        
        Calendar calPredicted = (Calendar) lastPeriod.clone();
        calPredicted.add(Calendar.DAY_OF_YEAR, avgCycleLength);
        long predictedStart = calPredicted.getTimeInMillis();

        return new Prediction(
                calEarliest.getTimeInMillis(),
                calLatest.getTimeInMillis(),
                predictedStart,
                avgDuration
        );
    }
}
