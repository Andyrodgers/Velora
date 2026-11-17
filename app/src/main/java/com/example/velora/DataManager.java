// DataManager.java (SharedPreferences helper)
package com.example.velora;

import android.content.Context;
import android.content.SharedPreferences;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

public class DataManager {

    private static final String PREF_NAME = "VeloraPrefs";
    private final SharedPreferences prefs;
    private final Gson gson;

    public DataManager(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        gson = new Gson();
    }

    public void saveSettings(Calendar lastPeriod, int cycleLength, int periodDuration) {
        SharedPreferences.Editor editor = prefs.edit();
        if (lastPeriod == null) {
            editor.remove("last_period");
        } else {
            editor.putLong("last_period", lastPeriod.getTimeInMillis());
        }
        editor.putInt("cycle_length", cycleLength)
                .putInt("period_duration", periodDuration)
                .apply();
    }

    public Calendar getLastPeriod() {
        long millis = prefs.getLong("last_period", 0);
        if (millis == 0) return null;
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(millis);
        return cal;
    }

    public int getCycleLength() {
        return prefs.getInt("cycle_length", 28);
    }

    public int getPeriodDuration() {
        return prefs.getInt("period_duration", 5);
    }

    public String getNextPeriodDate() {
        Calendar lastPeriod = getLastPeriod();
        if (lastPeriod == null) return null;
        Calendar nextPeriod = (Calendar) lastPeriod.clone();
        nextPeriod.add(Calendar.DAY_OF_YEAR, getCycleLength());
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US);
        return sdf.format(nextPeriod.getTime());
    }

    // --- CYCLE HISTORY (Advanced) ---
    public void saveCycleHistory(List<CycleEntry> history) {
        String json = gson.toJson(history);
        prefs.edit().putString("cycle_history_v2", json).apply();
    }

    public List<CycleEntry> getCycleHistory() {
        String json = prefs.getString("cycle_history_v2", null);
        if (json == null) return new ArrayList<>();
        Type type = new TypeToken<ArrayList<CycleEntry>>() {}.getType();
        return gson.fromJson(json, type);
    }

    public void addToHistory(String dateStr) {
        String history = prefs.getString("cycle_history", "");
        if (history.isEmpty()) {
            prefs.edit().putString("cycle_history", dateStr).apply();
        } else {
            prefs.edit().putString("cycle_history", history + "|" + dateStr).apply();
        }
    }

    public List<String> getHistory() {
        String history = prefs.getString("cycle_history", "");
        List<String> list = new ArrayList<>();
        if (!history.isEmpty()) {
            String[] arr = history.split("\\|");
            Collections.addAll(list, arr);
        }
        return list;
    }

    public String getUserEmail() {
        return prefs.getString("userEmail", "Guest");
    }

    public String getUserName() {
        return prefs.getString("userName", "User Name");
    }

    public int getUserAge() {
        return prefs.getInt("userAge", 0);
    }

    public String getUserPhone() {
        return prefs.getString("userPhone", "");
    }

    public String getUserPassword() {
        return prefs.getString("userPassword", "");
    }

    public String getProfilePhotoUri() {
        return prefs.getString("profilePhotoUri", null);
    }

    public String getUserPhoto() {
        return getProfilePhotoUri();
    }

    public void saveProfile(String name, int age, String email, String phone, String photoUri) {
        prefs.edit()
                .putString("userName", name)
                .putInt("userAge", age)
                .putString("userEmail", email)
                .putString("userPhone", phone)
                .putString("profilePhotoUri", photoUri)
                .apply();
    }

    public void registerUser(String email, String phone, String password, String name, int age) {
        prefs.edit()
                .putString("userEmail", email)
                .putString("userPhone", phone)
                .putString("userPassword", password)
                .putString("userName", name)
                .putInt("userAge", age)
                .putBoolean("isLoggedIn", true)
                .apply();
    }

    public void setLoggedIn(boolean isLoggedIn) {
        prefs.edit().putBoolean("isLoggedIn", isLoggedIn).apply();
    }
    
    public boolean isLoggedIn() {
        return prefs.getBoolean("isLoggedIn", false);
    }

    public void saveLastNotificationDate(String date) {
        prefs.edit().putString("last_notification_date", date).apply();
    }

    public String getLastNotificationDate() {
        return prefs.getString("last_notification_date", "");
    }

    public void setNotificationsEnabled(boolean enabled) {
        prefs.edit().putBoolean("notifications_enabled", enabled).apply();
    }

    public boolean isNotificationsEnabled() {
        return prefs.getBoolean("notifications_enabled", true);
    }

    public void clearCycleData() {
        prefs.edit()
                .remove("last_period")
                .remove("cycle_history")
                .remove("cycle_history_v2")
                .apply();
    }

    // --- COMMUNITY CACHE ---
    public void saveCachedUsers(List<SecretChatActivity.User> users) {
        String json = gson.toJson(users);
        prefs.edit().putString("cached_community_users", json).apply();
    }

    public List<SecretChatActivity.User> getCachedUsers() {
        String json = prefs.getString("cached_community_users", null);
        if (json == null) return new ArrayList<>();
        Type type = new TypeToken<ArrayList<SecretChatActivity.User>>() {}.getType();
        return gson.fromJson(json, type);
    }

    // --- THEME SETTINGS ---
    public void setTheme(int themeMode) {
        prefs.edit().putInt("app_theme", themeMode).apply();
    }

    public int getTheme() {
        return prefs.getInt("app_theme", 0); // 0: Default Pink, 1: Blue, 2: Dark
    }

    // --- LIBIDO TRACKING ---
    public void saveLibido(int level, String note) {
        long today = Calendar.getInstance().getTimeInMillis() / (1000 * 60 * 60 * 24);
        prefs.edit()
                .putInt("libido_" + today, level)
                .putString("libido_note_" + today, note)
                .apply();
    }

    public int getLibidoToday() {
        long today = Calendar.getInstance().getTimeInMillis() / (1000 * 60 * 60 * 24);
        return prefs.getInt("libido_" + today, 2); // default Normal
    }

    public String getLibidoNoteToday() {
        long today = Calendar.getInstance().getTimeInMillis() / (1000 * 60 * 60 * 24);
        return prefs.getString("libido_note_" + today, "");
    }

    // --- NOTES TRACKING ---
    public void saveNote(String noteText) {
        String dateStr = new java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault()).format(new java.util.Date());
        String notes = prefs.getString("user_notes", "");
        String newNote = dateStr + ": " + noteText;
        if (notes.isEmpty()) {
            prefs.edit().putString("user_notes", newNote).apply();
        } else {
            prefs.edit().putString("user_notes", notes + "||" + newNote).apply();
        }
    }

    public List<String> getNotes() {
        String notes = prefs.getString("user_notes", "");
        List<String> list = new ArrayList<>();
        if (!notes.isEmpty()) {
            String[] arr = notes.split("\\|\\|");
            Collections.addAll(list, arr);
        }
        return list;
    }

    public void deleteNote(int index) {
        List<String> notes = getNotes();
        if (index >= 0 && index < notes.size()) {
            notes.remove(index);
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < notes.size(); i++) {
                sb.append(notes.get(i));
                if (i < notes.size() - 1) {
                    sb.append("||");
                }
            }
            prefs.edit().putString("user_notes", sb.toString()).apply();
        }
    }

    // --- PARTNER INFO ---
    public void savePartnerInfo(String id, String name, String age, String phone, String email, String photoUri) {
        prefs.edit()
                .putString("partnerId", id)
                .putString("partnerName", name)
                .putString("partnerAge", age)
                .putString("partnerPhone", phone)
                .putString("partnerEmail", email)
                .putString("partnerPhotoUri", photoUri)
                .apply();
    }

    public String getPartnerId() { return prefs.getString("partnerId", ""); }
    public String getPartnerName() { return prefs.getString("partnerName", ""); }
    public String getPartnerAge() { return prefs.getString("partnerAge", ""); }
    public String getPartnerPhone() { return prefs.getString("partnerPhone", ""); }
    public String getPartnerEmail() { return prefs.getString("partnerEmail", ""); }
    public String getPartnerPhotoUri() { return prefs.getString("partnerPhotoUri", null); }

    // --- PARTNER CHAT & PIN ---
    public void savePartnerChatPin(String pin) {
        prefs.edit().putString("partner_chat_pin", pin).apply();
    }

    public String getPartnerChatPin() {
        return prefs.getString("partner_chat_pin", "");
    }

    public void savePartnerChatMessage(String message, boolean isMe) {
        String history = prefs.getString("partner_chat_history", "");
        String entry = (isMe ? "ME:" : "PARTNER:") + message + "|" + System.currentTimeMillis();
        if (history.isEmpty()) {
            prefs.edit().putString("partner_chat_history", entry).apply();
        } else {
            prefs.edit().putString("partner_chat_history", history + "||" + entry).apply();
        }
    }

    public List<String> getPartnerChatHistory() {
        String history = prefs.getString("partner_chat_history", "");
        List<String> list = new ArrayList<>();
        if (!history.isEmpty()) {
            String[] arr = history.split("\\|\\|");
            Collections.addAll(list, arr);
        }
        return list;
    }
}
