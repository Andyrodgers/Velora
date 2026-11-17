package com.example.velora;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class CustomCalendarDialog extends Dialog {

    private Calendar selectedDate;
    private final int periodDuration;
    private OnDateSetListener onDateSetListener;
    private CheckBox cbStress, cbIllness, cbTravel;

    public interface OnDateSetListener {
        void onDateSet(Calendar calendar, boolean isStressed, boolean isIll, boolean hasTraveled);
    }

    public CustomCalendarDialog(@NonNull Context context, int periodDuration, Calendar initialDate, OnDateSetListener listener) {
        super(context);
        this.periodDuration = periodDuration;
        this.onDateSetListener = listener;
        this.selectedDate = initialDate != null ? (Calendar) initialDate.clone() : null;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_custom_calendar);

        RecyclerView rvCalendar = findViewById(R.id.rvCalendar);
        rvCalendar.setLayoutManager(new GridLayoutManager(getContext(), 7));

        cbStress = findViewById(R.id.cbStress);
        cbIllness = findViewById(R.id.cbIllness);
        cbTravel = findViewById(R.id.cbTravel);

        ImageButton btnClose = findViewById(R.id.btnCloseCalendar);
        Button btnCancel = findViewById(R.id.btnCancelCalendar);
        Button btnSave = findViewById(R.id.btnSaveCalendar);

        btnClose.setOnClickListener(v -> dismiss());
        btnCancel.setOnClickListener(v -> dismiss());
        btnSave.setOnClickListener(v -> {
            if (selectedDate != null && onDateSetListener != null) {
                onDateSetListener.onDateSet(
                    selectedDate, 
                    cbStress.isChecked(), 
                    cbIllness.isChecked(), 
                    cbTravel.isChecked()
                );
            }
            dismiss();
        });

        List<CalendarItem> calendarItems = new ArrayList<>();
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.DAY_OF_MONTH, 1);
        cal.add(Calendar.MONTH, -1); // Show last month and this month

        for (int i = 0; i < 2; i++) {
            calendarItems.add(new CalendarItem(cal.getDisplayName(Calendar.MONTH, Calendar.LONG, getContext().getResources().getConfiguration().locale)));
            
            int firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
            for (int j = 1; j < firstDayOfWeek; j++) {
                calendarItems.add(new CalendarItem(null, -1, -1, -1)); // empty days
            }
            
            int daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
            for (int j = 1; j <= daysInMonth; j++) {
                calendarItems.add(new CalendarItem(null, j, cal.get(Calendar.MONTH), cal.get(Calendar.YEAR)));
            }
            cal.add(Calendar.MONTH, 1);
        }

        rvCalendar.setAdapter(new CalendarAdapter(calendarItems));
    }

    private static class CalendarItem {
        String monthName;
        int day;
        int month;
        int year;

        CalendarItem(String monthName) { this.monthName = monthName; this.day = -1; }
        CalendarItem(String monthName, int day, int month, int year) { this.monthName = monthName; this.day = day; this.month = month; this.year = year; }
    }

    private class CalendarAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private final List<CalendarItem> items;

        CalendarAdapter(List<CalendarItem> items) {
            this.items = items;
        }

        @Override
        public int getItemViewType(int position) {
            return (items.get(position).monthName != null) ? 0 : 1;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            if (viewType == 0) {
                TextView tv = new TextView(parent.getContext());
                tv.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                tv.setTextSize(18);
                tv.setTypeface(null, Typeface.BOLD);
                tv.setGravity(android.view.Gravity.CENTER);
                tv.setPadding(0, 40, 0, 20);
                tv.setTextColor(Color.BLACK);
                return new MonthViewHolder(tv);
            } else {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_calendar_day, parent, false);
                return new DayViewHolder(view);
            }
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            if (holder instanceof MonthViewHolder) {
                ((MonthViewHolder) holder).bind(items.get(position).monthName);
            } else if (holder instanceof DayViewHolder) {
                ((DayViewHolder) holder).bind(items.get(position));
            }
        }

        @Override
        public int getItemCount() { return items.size(); }

        class MonthViewHolder extends RecyclerView.ViewHolder {
            TextView tvMonth;
            MonthViewHolder(View view) { super(view); tvMonth = (TextView) view; }
            void bind(String month) { tvMonth.setText(month); }
        }

        class DayViewHolder extends RecyclerView.ViewHolder {
            TextView tvDay;
            DayViewHolder(View view) { super(view); tvDay = view.findViewById(R.id.tvDay); }

            void bind(CalendarItem item) {
                if (item.day == -1) {
                    tvDay.setText("");
                    tvDay.setBackground(null);
                    return;
                }
                tvDay.setText(String.valueOf(item.day));
                tvDay.setBackground(null);
                tvDay.setTextColor(Color.BLACK);
                tvDay.setTypeface(null, Typeface.NORMAL);
                tvDay.setTextSize(14);

                Calendar today = Calendar.getInstance();
                if (item.day == today.get(Calendar.DAY_OF_MONTH) && item.month == today.get(Calendar.MONTH) && item.year == today.get(Calendar.YEAR)) {
                    tvDay.setTypeface(null, Typeface.BOLD);
                }

                if (selectedDate != null) {
                    Calendar itemCal = Calendar.getInstance();
                    itemCal.set(item.year, item.month, item.day, 0, 0, 0);
                    itemCal.set(Calendar.MILLISECOND, 0);

                    Calendar selCalStart = (Calendar) selectedDate.clone();
                    selCalStart.set(Calendar.HOUR_OF_DAY, 0);
                    selCalStart.set(Calendar.MINUTE, 0);
                    selCalStart.set(Calendar.SECOND, 0);
                    selCalStart.set(Calendar.MILLISECOND, 0);

                    Calendar selCalEnd = (Calendar) selCalStart.clone();
                    selCalEnd.add(Calendar.DAY_OF_YEAR, periodDuration);

                    if (itemCal.equals(selCalStart)) {
                        tvDay.setBackgroundResource(R.drawable.circle_background);
                        tvDay.setTextColor(Color.WHITE);
                        // Add checkmark symbol
                        tvDay.setText(item.day + " \u2713");
                        tvDay.setTextSize(12);
                    } else if (itemCal.after(selCalStart) && itemCal.before(selCalEnd)) {
                        tvDay.setBackgroundResource(R.drawable.circle_background);
                        tvDay.setTextColor(Color.WHITE);
                        tvDay.setText(String.valueOf(item.day));
                        tvDay.setTextSize(14);
                    }
                }

                itemView.setOnClickListener(v -> {
                    selectedDate = Calendar.getInstance();
                    selectedDate.set(item.year, item.month, item.day);
                    notifyDataSetChanged();
                });
            }
        }
    }
}
