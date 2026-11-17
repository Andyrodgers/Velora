package com.example.velora;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class CycleHelper {

    public static Calendar calculateNextPeriod(Calendar lastPeriodStart, int cycleLength) {
        if (lastPeriodStart == null) return Calendar.getInstance();
        Calendar next = (Calendar) lastPeriodStart.clone();
        next.add(Calendar.DAY_OF_YEAR, cycleLength);
        return next;
    }

    public static Calendar calculateOvulation(Calendar nextPeriod) {
        Calendar ov = (Calendar) nextPeriod.clone();
        ov.add(Calendar.DAY_OF_YEAR, -14);
        return ov;
    }

    public static Calendar calculateFertileStart(Calendar ovulation) {
        Calendar fs = (Calendar) ovulation.clone();
        fs.add(Calendar.DAY_OF_YEAR, -5);
        return fs;
    }

    public static Calendar calculateFertileEnd(Calendar ovulation) {
        Calendar fe = (Calendar) ovulation.clone();
        fe.add(Calendar.DAY_OF_YEAR, 1);
        return fe;
    }

    public static int daysUntil(Calendar today, Calendar target) {
        if (today == null || target == null) return 0;
        
        Calendar d1 = (Calendar) today.clone();
        d1.set(Calendar.HOUR_OF_DAY, 0); d1.set(Calendar.MINUTE, 0); d1.set(Calendar.SECOND, 0); d1.set(Calendar.MILLISECOND, 0);
        
        Calendar d2 = (Calendar) target.clone();
        d2.set(Calendar.HOUR_OF_DAY, 0); d2.set(Calendar.MINUTE, 0); d2.set(Calendar.SECOND, 0); d2.set(Calendar.MILLISECOND, 0);

        long diff = d2.getTimeInMillis() - d1.getTimeInMillis();
        return (int) (diff / (1000 * 60 * 60 * 24));
    }

    public static boolean isCycleAbnormal(int length, int average) {
        return Math.abs(length - average) > 7 || length < 21 || length > 35;
    }

    public static long calendarToUtc(Calendar cal) {
        if (cal == null) return System.currentTimeMillis();
        Calendar utc = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        utc.set(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH), 0, 0, 0);
        utc.set(Calendar.MILLISECOND, 0);
        return utc.getTimeInMillis();
    }

    public static Calendar utcToCalendar(long millis) {
        Calendar utc = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        utc.setTimeInMillis(millis);
        Calendar local = Calendar.getInstance();
        local.set(utc.get(Calendar.YEAR), utc.get(Calendar.MONTH), utc.get(Calendar.DAY_OF_MONTH), 0, 0, 0);
        local.set(Calendar.MILLISECOND, 0);
        return local;
    }

    public static Date parseHistoryDate(String ds) {
        if (ds == null || ds.isEmpty()) return null;
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
        try {
            String datePart = ds.contains(" (") ? ds.substring(0, ds.indexOf(" (")) : ds;
            return sdf.parse(datePart);
        } catch (ParseException e) {
            return null;
        }
    }

    public static int getLastCycleLength(List<String> history) {
        if (history.size() < 2) return -1;
        List<Calendar> cals = new ArrayList<>();
        for (String ds : history) {
            Date d = parseHistoryDate(ds);
            if (d != null) {
                Calendar c = Calendar.getInstance();
                c.setTime(d);
                cals.add(c);
            }
        }
        Collections.sort(cals);
        if (cals.size() >= 2) {
            return daysUntil(cals.get(cals.size() - 2), cals.get(cals.size() - 1));
        }
        return -1;
    }
}
