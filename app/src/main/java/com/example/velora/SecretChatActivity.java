package com.example.velora;

import android.Manifest;
import android.app.Dialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.google.android.material.imageview.ShapeableImageView;
import java.text.SimpleDateFormat;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import kotlin.Unit;

public class SecretChatActivity extends BaseActivity {

    private RecyclerView rvUsers;
    private UserAdapter adapter;
    private List<User> userList = new ArrayList<>();
    private List<User> fullUserList = new ArrayList<>();
    private EditText etSearchUsers;
    private MessageDao messageDao;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_secret_chat);

        messageDao = VeloraDatabase.getInstance(this).messageDao();

        rvUsers = findViewById(R.id.rvUsers);
        etSearchUsers = findViewById(R.id.etSearchUsers);
        rvUsers.setLayoutManager(new LinearLayoutManager(this));

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        
        // --- OFFLINE SUPPORT: Load cached users from SharedPreferences ---
        List<User> cachedUsers = dataManager.getCachedUsers();
        if (cachedUsers != null && !cachedUsers.isEmpty()) {
            fullUserList.addAll(cachedUsers);
            userList.addAll(cachedUsers);
            sortUsers();
        }

        adapter = new UserAdapter(userList);
        rvUsers.setAdapter(adapter);

        String currentUserId = SupabaseHelper.getCurrentUserId();
        boolean hasLocalLogin = dataManager.isLoggedIn();

        if (!hasLocalLogin && (currentUserId == null || currentUserId.isEmpty())) {
            Toast.makeText(this, "Please log in again to access Community", Toast.LENGTH_LONG).show();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        setupSearch();
        if (currentUserId != null && !currentUserId.isEmpty()) {
            fetchRealUsers();
            observeUnreadCounts();
        } else {
            Toast.makeText(this, "Community is in offline mode for now.", Toast.LENGTH_SHORT).show();
        }

        // Initialize reminders and request notification permission
        schedulePeriodReminders();
        requestNotificationPermission();
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1001);
            }
        }
    }

    private void setupSearch() {
        etSearchUsers.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterUsers(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private boolean canUpdateUi() {
        return !isFinishing() && !isDestroyed();
    }

    private void filterUsers(String query) {
        userList.clear();
        if (fullUserList == null) {
            if (adapter != null) adapter.notifyDataSetChanged();
            return;
        }
        if (query.isEmpty()) {
            userList.addAll(fullUserList);
        } else {
            String lowerCaseQuery = query.toLowerCase();
            for (User user : fullUserList) {
                if (user == null) continue;
                if ((user.name != null && user.name.toLowerCase().contains(lowerCaseQuery)) ||
                    (user.phoneNumber != null && user.phoneNumber.contains(query))) {
                    userList.add(user);
                }
            }
        }
        sortUsers();
        if (adapter != null) adapter.notifyDataSetChanged();
    }

    private void sortUsers() {
        Collections.sort(userList, (u1, u2) -> {
            // Unread messages first
            if (u1.unreadCount > 0 && u2.unreadCount == 0) return -1;
            if (u1.unreadCount == 0 && u2.unreadCount > 0) return 1;
            
            // Then by last activity
            long t1 = Math.max(u1.lastActivityTimestamp, u1.lastSeen);
            long t2 = Math.max(u2.lastActivityTimestamp, u2.lastSeen);
            return Long.compare(t2, t1);
        });
    }

    private String normalizePhotoUri(String photoUri) {
        if (photoUri == null) return "";
        String value = photoUri.trim();
        return value.isEmpty() ? "" : value;
    }

    private void fetchRealUsers() {
        String myId = SupabaseHelper.getCurrentUserId();
        if (myId == null || myId.isEmpty()) {
            if (fullUserList.isEmpty()) {
                Toast.makeText(this, "Offline mode: no active Supabase session.", Toast.LENGTH_SHORT).show();
            }
            return;
        }

        SupabaseHelper.getAllUsers((users) -> {
            if (!canUpdateUi()) return Unit.INSTANCE;
            if (users != null) {
                fullUserList.clear();

                for (UserProfile profile : users) {
                    if (profile == null) continue;
                    if (myId.equals(profile.getId())) continue;

                    User user = new User(
                            profile.getId(),
                            profile.getName() != null ? profile.getName() : "Velora User",
                            profile.getStatus() != null ? profile.getStatus() : "offline",
                            profile.getPhone(),
                            profile.getEmail(),
                            normalizePhotoUri(profile.getPhotoUri())
                    );
                    user.lastSeen = parseTimestamp(profile.getLastSeen());
                    fullUserList.add(user);

                    observeStatus(user);
                    fetchLastMessageTime(user);
                }

                dataManager.saveCachedUsers(fullUserList);
                filterUsers(etSearchUsers != null ? etSearchUsers.getText().toString() : "");

                if (fullUserList.isEmpty()) {
                    Toast.makeText(this, "No other users registered in Velora yet.", Toast.LENGTH_SHORT).show();
                }
                observeUnreadCounts();
            } else {
                if (fullUserList.isEmpty()) {
                    Toast.makeText(this, "Could not load users. Check your internet connection.", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(this, "Offline Mode: Showing cached users.", Toast.LENGTH_SHORT).show();
                }
            }
            return Unit.INSTANCE;
        });
    }

    private void fetchLastMessageTime(User user) {
        String myId = SupabaseHelper.getCurrentUserId();
        if (myId == null || myId.isEmpty() || user == null || user.id == null || user.id.isEmpty()) {
            return;
        }

        // First check local database
        new Thread(() -> {
            ChatMessage lastLocal = messageDao.getLastMessage(myId, user.id);
            if (lastLocal != null) {
                runOnUiThread(() -> {
                    user.lastActivityTimestamp = lastLocal.getTimestamp();
                    sortUsers();
                    if (adapter != null) adapter.notifyDataSetChanged();
                });
            }
        }).start();

        // Then check Supabase
        SupabaseHelper.getLastMessage(myId, user.id, (msg) -> {
            if (msg != null) {
                long ts = msg.getTimestamp() != null ? msg.getTimestamp() : 0;
                if (ts > user.lastActivityTimestamp) {
                    user.lastActivityTimestamp = ts;
                    sortUsers();
                    if (adapter != null) adapter.notifyDataSetChanged();
                    dataManager.saveCachedUsers(fullUserList);
                }
            }
            return Unit.INSTANCE;
        });
    }

    private void observeStatus(User user) {
        if (user == null || user.id == null || user.id.isEmpty()) return;
        SupabaseHelper.observeUserDetails(user.id, (profile) -> {
            if (!canUpdateUi()) return Unit.INSTANCE;
            if (profile != null) {
                user.status = profile.getStatus();
                user.lastSeen = parseTimestamp(profile.getLastSeen());
                user.photoUri = normalizePhotoUri(profile.getPhotoUri());
                if (user.name == null || user.name.isEmpty()) {
                    user.name = profile.getName() != null ? profile.getName() : user.name;
                }
                sortUsers();
                if (adapter != null) adapter.notifyDataSetChanged();
                dataManager.saveCachedUsers(fullUserList);
            }
            return Unit.INSTANCE;
        });
    }

    private void observeUnreadCounts() {
        String myId = SupabaseHelper.getCurrentUserId();
        if (myId == null || myId.isEmpty()) return;

        SupabaseHelper.observeUnreadCounts(myId, (counts) -> {
            if (!canUpdateUi()) return Unit.INSTANCE;
            if (counts != null) {
                for (User user : fullUserList) {
                    if (user != null) user.unreadCount = 0;
                }
                for (UnreadCount count : counts) {
                    for (User user : fullUserList) {
                        if (user != null && user.id != null && user.id.equals(count.getSenderId())) {
                            user.unreadCount = count.getCount();
                            break;
                        }
                    }
                }
                sortUsers();
                if (adapter != null) adapter.notifyDataSetChanged();
                dataManager.saveCachedUsers(fullUserList);
            }
            return Unit.INSTANCE;
        });
    }

    private void onUserClicked(User user) {
        if (user == null || user.id == null || user.id.isEmpty()) return;

        String myId = SupabaseHelper.getCurrentUserId();
        if (myId == null || myId.isEmpty()) {
            Toast.makeText(this, "Please log in again to chat with the community", Toast.LENGTH_LONG).show();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        String partnerId = dataManager.getPartnerId();

        user.unreadCount = 0;
        if (adapter != null) adapter.notifyDataSetChanged();

        if (partnerId != null && !partnerId.isEmpty() && user.id.equals(partnerId)) {
            startActivity(new Intent(this, PartnerChatActivity.class));
        } else {
            Intent intent = new Intent(this, UserChatActivity.class);
            intent.putExtra("otherUserId", user.id);
            intent.putExtra("otherUserPhone", user.phoneNumber);
            intent.putExtra("otherUserName", user.name);
            intent.putExtra("otherUserPhoto", user.photoUri);
            startActivity(intent);
        }

        SupabaseHelper.resetUnreadCount(myId, user.id);
    }

    private void showUserProfile(User user) {
        if (user == null) return;
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_user_profile);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        ShapeableImageView ivPhoto = dialog.findViewById(R.id.ivProfilePhoto);
        TextView tvName = dialog.findViewById(R.id.tvProfileName);
        TextView tvPhone = dialog.findViewById(R.id.tvProfilePhone);
        TextView tvNotes = dialog.findViewById(R.id.tvProfileNotes);
        Button btnStartChat = dialog.findViewById(R.id.btnStartChat);
        Button btnClose = dialog.findViewById(R.id.btnCloseProfile);

        tvName.setText(user.name != null ? user.name : "User");
        tvPhone.setText(user.phoneNumber != null ? user.phoneNumber : "No phone shared");
        tvNotes.setText(user.email != null && !user.email.isEmpty() ? user.email : "No email shared.");

        Glide.with(this)
                .load((user.photoUri != null && !user.photoUri.isEmpty()) ? user.photoUri : R.drawable.user_account_icon)
                .placeholder(R.drawable.user_account_icon)
                .error(R.drawable.user_account_icon)
                .into(ivPhoto);

        btnStartChat.setOnClickListener(v -> {
            onUserClicked(user);
            dialog.dismiss();
        });

        tvPhone.setOnClickListener(v -> {
            openWhatsApp(user.phoneNumber);
            dialog.dismiss();
        });

        btnClose.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void openWhatsApp(String phoneNumber) {
        try {
            String cleanNumber = SupabaseHelper.cleanPhone(phoneNumber);
            String url = "https://api.whatsapp.com/send?phone=" + cleanNumber;
            Intent i = new Intent(Intent.ACTION_VIEW);
            i.setData(Uri.parse(url));
            startActivity(i);
        } catch (Exception e) {
            Toast.makeText(this, "WhatsApp not installed or invalid number", Toast.LENGTH_SHORT).show();
        }
    }

    private long parseTimestamp(String timestamp) {
        if (timestamp == null || timestamp.isEmpty()) return 0L;
        try {
            return OffsetDateTime.parse(timestamp).toInstant().toEpochMilli();
        } catch (Exception e) {
            return 0L;
        }
    }

    public static class User {
        public String id;
        public String name;
        public String status;
        public String phoneNumber;
        public String email;
        public String photoUri;
        public long unreadCount = 0;
        public long lastSeen = 0;
        public long lastActivityTimestamp = 0;

        public User(String id, String name, String status, String phoneNumber, String email, String photoUri) {
            this.id = id;
            this.name = name;
            this.status = status;
            this.phoneNumber = phoneNumber;
            this.email = email;
            this.photoUri = photoUri;
        }
    }

    private class UserAdapter extends RecyclerView.Adapter<UserAdapter.UserViewHolder> {
        private List<User> users;
        public UserAdapter(List<User> users) { this.users = users; }

        @NonNull
        @Override
        public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_user, parent, false);
            return new UserViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
            if (users == null || position < 0 || position >= users.size()) return;
            User user = users.get(position);
            if (user == null) return;
            holder.tvUserName.setText(user.name != null ? user.name : "Velora User");

            if ("online".equalsIgnoreCase(user.status)) {
                holder.tvUserStatus.setText("Online");
                holder.tvUserStatus.setTextColor(0xFF4CAF50);
            } else {
                holder.tvUserStatus.setText("Offline");
                holder.tvUserStatus.setTextColor(0xFF888888);
            }

            String photoUri = (user.photoUri != null && !user.photoUri.isEmpty()) ? user.photoUri : null;
            Glide.with(SecretChatActivity.this)
                    .load(photoUri != null ? photoUri : R.drawable.user_account_icon)
                    .placeholder(R.drawable.user_account_icon)
                    .error(R.drawable.user_account_icon)
                    .into(holder.ivUserPhoto);

            long displayTime = Math.max(user.lastActivityTimestamp, user.lastSeen);
            if (displayTime > 0) {
                holder.tvLastTime.setText(getFormattedTime(displayTime));
            } else {
                holder.tvLastTime.setText("");
            }

            if (user.unreadCount > 0) {
                holder.tvUnreadCount.setVisibility(View.VISIBLE);
                holder.tvUnreadCount.setText(String.valueOf(user.unreadCount));
                holder.tvLastTime.setTextColor(0xFF25D366);
            } else {
                holder.tvUnreadCount.setVisibility(View.GONE);
                holder.tvLastTime.setTextColor(0xFF888888);
            }

            holder.itemView.setOnClickListener(v -> onUserClicked(user));
            holder.ivUserPhoto.setOnClickListener(v -> showUserProfile(user));
        }

        private String getFormattedTime(long timestamp) {
            Calendar now = Calendar.getInstance();
            Calendar time = Calendar.getInstance();
            time.setTimeInMillis(timestamp);

            if (now.get(Calendar.DATE) == time.get(Calendar.DATE)) {
                return new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date(timestamp));
            } else if (now.get(Calendar.DATE) - time.get(Calendar.DATE) == 1) {
                return "Yesterday";
            } else if (now.get(Calendar.WEEK_OF_YEAR) == time.get(Calendar.WEEK_OF_YEAR)) {
                return new SimpleDateFormat("EEEE", Locale.getDefault()).format(new Date(timestamp));
            } else {
                return new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date(timestamp));
            }
        }

        @Override
        public int getItemCount() { return users.size(); }

        class UserViewHolder extends RecyclerView.ViewHolder {
            ShapeableImageView ivUserPhoto;
            TextView tvUserName, tvUserStatus, tvUnreadCount, tvLastTime;
            public UserViewHolder(@NonNull View itemView) {
                super(itemView);
                ivUserPhoto = itemView.findViewById(R.id.ivUserPhoto);
                tvUserName = itemView.findViewById(R.id.tvUserName);
                tvUserStatus = itemView.findViewById(R.id.tvUserStatus);
                tvUnreadCount = itemView.findViewById(R.id.tvUnreadCount);
                tvLastTime = itemView.findViewById(R.id.tvLastTime);
            }
        }
    }
}
