package com.example.velora;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import com.bumptech.glide.Glide;
import com.google.android.material.card.MaterialCardView;
import kotlin.Unit;

public class PartnerActivity extends BaseActivity {

    private EditText etPartnerSearchEmail;
    private Button btnSearchPartner;
    private MaterialCardView cardPartnerResult;
    private ImageView ivPartnerPhoto;
    private TextView tvPartnerName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_partner);

        etPartnerSearchEmail = findViewById(R.id.etPartnerSearchEmail);
        btnSearchPartner = findViewById(R.id.btnSearchPartner);
        cardPartnerResult = findViewById(R.id.cardPartnerResult);
        ivPartnerPhoto = findViewById(R.id.ivPartnerPhoto);
        tvPartnerName = findViewById(R.id.tvPartnerName);

        btnSearchPartner.setOnClickListener(v -> {
            String email = etPartnerSearchEmail.getText().toString().trim();
            if (email.isEmpty()) {
                Toast.makeText(this, "Please enter an email", Toast.LENGTH_SHORT).show();
                return;
            }
            searchAndLinkPartner(email);
        });

        // Auto-load existing partner
        loadCurrentPartner();
    }

    private void loadCurrentPartner() {
        SupabaseHelper.getCurrentUserProfile(profile -> {
            if (profile != null && profile.getPartnerId() != null && !profile.getPartnerId().isEmpty()) {
                fetchPartnerDetails(profile.getPartnerId());
                
                // Realtime updates - observe the current user profile for partner changes
                observeMyProfileForPartnerChanges(profile.getId());
            }
            return Unit.INSTANCE;
        });
    }

    private void observeMyProfileForPartnerChanges(String myId) {
        SupabaseHelper.observeProfileById(myId, profile -> {
            if (profile.getPartnerId() != null && !profile.getPartnerId().isEmpty()) {
                fetchPartnerDetails(profile.getPartnerId());
            } else {
                cardPartnerResult.setVisibility(View.GONE);
            }
            return Unit.INSTANCE;
        });
    }

    private void fetchPartnerDetails(String partnerId) {
        SupabaseHelper.getUserById(partnerId, partner -> {
            if (partner != null) {
                updatePartnerUI(partner);
                // Also observe partner for realtime info changes (like name/photo)
                SupabaseHelper.observeProfileById(partnerId, updatedPartner -> {
                    updatePartnerUI(updatedPartner);
                    return Unit.INSTANCE;
                });
            }
            return Unit.INSTANCE;
        });
    }

    private void updatePartnerUI(UserProfile partner) {
        cardPartnerResult.setVisibility(View.VISIBLE);
        
        String displayName = partner.getName();
        if (displayName == null || displayName.isEmpty()) {
            displayName = "Partner";
        }
        tvPartnerName.setText(displayName);

        String avatarUrl = partner.getPhotoUri();

        Glide.with(this)
                .load(avatarUrl)
                .centerCrop()
                .placeholder(R.drawable.user_account_icon)
                .error(R.drawable.user_account_icon)
                .into(ivPartnerPhoto);

        // Save to local storage for quick access in PartnerChatActivity
        dataManager.savePartnerInfo(
                partner.getId(),
                displayName,
                partner.getAge() != null ? String.valueOf(partner.getAge()) : "0",
                partner.getPhone() != null ? partner.getPhone() : "",
                partner.getEmail() != null ? partner.getEmail() : "",
                avatarUrl
        );
    }

    private void searchAndLinkPartner(String partnerEmail) {
        btnSearchPartner.setEnabled(false);
        
        SupabaseHelper.searchAndLinkPartner(partnerEmail, (partner, error) -> {
            btnSearchPartner.setEnabled(true);
            
            if (error != null) {
                Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
            } else if (partner != null) {
                updatePartnerUI(partner);
                Toast.makeText(this, "Accounts linked successfully!", Toast.LENGTH_SHORT).show();
                
                // Start observing the newly linked partner
                observeMyProfileForPartnerChanges(SupabaseHelper.getCurrentUserId());
            } else {
                Toast.makeText(this, "User not found", Toast.LENGTH_SHORT).show();
                cardPartnerResult.setVisibility(View.GONE);
            }
            return Unit.INSTANCE;
        });
    }
}
