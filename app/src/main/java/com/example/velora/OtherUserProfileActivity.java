package com.example.velora;

import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import com.bumptech.glide.Glide;
import kotlin.Unit;

public class OtherUserProfileActivity extends BaseActivity {

    private ImageView ivProfilePhoto;
    private TextView tvName, tvPhone, tvEmail, tvNotes;
    private View llEmailContainer, llNotesContainer;
    private String otherUserId, otherUserPhone;
    private boolean isPartner = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_other_user_profile);

        ivProfilePhoto = findViewById(R.id.ivProfilePhoto);
        tvName = findViewById(R.id.tvProfileName);
        tvPhone = findViewById(R.id.tvProfilePhone);
        tvEmail = findViewById(R.id.tvProfileEmail);
        tvNotes = findViewById(R.id.tvProfileNotes);
        llEmailContainer = findViewById(R.id.llEmailContainer);
        llNotesContainer = findViewById(R.id.llNotesContainer);

        otherUserId = getIntent().getStringExtra("otherUserId");
        otherUserPhone = getIntent().getStringExtra("otherUserPhone");
        String otherUserName = getIntent().getStringExtra("otherUserName");
        String otherUserPhoto = getIntent().getStringExtra("otherUserPhoto");
        isPartner = getIntent().getBooleanExtra("isPartner", false);

        tvName.setText(otherUserName != null ? otherUserName : "User");
        
        if (isPartner) {
            tvPhone.setText(otherUserPhone);
            tvPhone.setVisibility(View.VISIBLE);
            llEmailContainer.setVisibility(View.VISIBLE);
            llNotesContainer.setVisibility(View.VISIBLE);
        } else {
            tvPhone.setVisibility(View.GONE);
            llEmailContainer.setVisibility(View.GONE);
            llNotesContainer.setVisibility(View.GONE);
        }
        
        if (otherUserPhoto != null && !otherUserPhoto.isEmpty()) {
            Glide.with(this)
                    .load(otherUserPhoto)
                    .placeholder(R.drawable.user_account_icon)
                    .error(R.drawable.user_account_icon)
                    .into(ivProfilePhoto);
        } else {
            ivProfilePhoto.setImageResource(R.drawable.user_account_icon);
        }

        fetchOtherUserDetails();

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
    }

    private void fetchOtherUserDetails() {
        if (otherUserId == null && otherUserPhone != null) {
            SupabaseHelper.getUserByPhone(otherUserPhone, user -> {
                if (user != null) {
                    otherUserId = user.getId();
                    fetchOtherUserDetails();
                }
                return Unit.INSTANCE;
            });
            return;
        }

        if (otherUserId == null) return;
        
        SupabaseHelper.getUserById(otherUserId, (user) -> {
            if (user != null) {
                if (isPartner) {
                    tvEmail.setText(user.getEmail() != null ? user.getEmail() : "No email shared.");
                    tvPhone.setText(user.getPhone() != null ? user.getPhone() : "No phone shared.");
                }
                
                String notes = user.getUserNotes();
                if (notes != null && !notes.isEmpty()) {
                    String[] notesArray = notes.split("\\|\\|");
                    StringBuilder sb = new StringBuilder();
                    for (int i = notesArray.length - 1; i >= 0; i--) {
                        sb.append(notesArray[i]).append("\n\n");
                    }
                    tvNotes.setText(sb.toString().trim());
                    llNotesContainer.setVisibility(View.VISIBLE);
                } else {
                    tvNotes.setText("No notes shared.");
                    // Keep hidden if not partner and no notes
                    if (!isPartner) llNotesContainer.setVisibility(View.GONE);
                }

                if (user.getPhotoUri() != null) {
                    Glide.with(this)
                            .load(user.getPhotoUri())
                            .placeholder(R.drawable.user_account_icon)
                            .error(R.drawable.user_account_icon)
                            .into(ivProfilePhoto);
                }
                
                if (user.getName() != null) tvName.setText(user.getName());
            }
            return Unit.INSTANCE;
        });
    }
}
