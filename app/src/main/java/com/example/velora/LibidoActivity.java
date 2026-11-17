package com.example.velora;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.material.card.MaterialCardView;
import java.util.Calendar;

public class LibidoActivity extends BaseActivity {

    private RadioGroup rgLibido;
    private TextView tvLibidoStatus, tvHormonalFact, tvLibidoSuggestion, tvCycleLibidoInsight;
    private EditText etLibidoNotes;
    private MaterialCardView cardHormonal;
    private ImageView ivPrivacyToggle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_libido);

        rgLibido = findViewById(R.id.rgLibido);
        tvLibidoStatus = findViewById(R.id.tvLibidoStatus);
        tvHormonalFact = findViewById(R.id.tvHormonalFact);
        tvLibidoSuggestion = findViewById(R.id.tvLibidoSuggestion);
        tvCycleLibidoInsight = findViewById(R.id.tvCycleLibidoInsight);
        etLibidoNotes = findViewById(R.id.etLibidoNotes);
        cardHormonal = findViewById(R.id.cardHormonal);
        ivPrivacyToggle = findViewById(R.id.ivPrivacyToggle);
        Button btnSave = findViewById(R.id.btnSaveLibido);

        // Icon click -> Opens Partner Chat
        ivPrivacyToggle.setOnClickListener(v -> {
            startActivity(new Intent(this, PartnerChatActivity.class));
        });

        // Load existing data for today
        int savedLevel = dataManager.getLibidoToday();
        setLibidoUI(savedLevel);
        etLibidoNotes.setText(dataManager.getLibidoNoteToday());

        rgLibido.setOnCheckedChangeListener((group, checkedId) -> {
            int level = 2; // Normal
            if (checkedId == R.id.rbVeryLow) level = 0;
            else if (checkedId == R.id.rbLow) level = 1;
            else if (checkedId == R.id.rbNormal) level = 2;
            else if (checkedId == R.id.rbHigh) level = 3;
            else if (checkedId == R.id.rbVeryHigh) level = 4;
            setLibidoUI(level);
        });

        cardHormonal.setOnClickListener(v -> showHormonalExplanation());

        btnSave.setOnClickListener(v -> {
            int selectedId = rgLibido.getCheckedRadioButtonId();
            int level = 2;
            if (selectedId == R.id.rbVeryLow) level = 0;
            else if (selectedId == R.id.rbLow) level = 1;
            else if (selectedId == R.id.rbNormal) level = 2;
            else if (selectedId == R.id.rbHigh) level = 3;
            else if (selectedId == R.id.rbVeryHigh) level = 4;

            dataManager.saveLibido(level, etLibidoNotes.getText().toString());
            Toast.makeText(this, "Daily wellness saved!", Toast.LENGTH_SHORT).show();
            finish();
        });

        calculateCycleInsight();
    }

    private void setLibidoUI(int level) {
        String status = "";
        String suggestion = "";

        switch (level) {
            case 0: // Very Low
                status = "Very Low";
                suggestion = "💞 Suggestion: Try a warm bath or a cozy cuddle. Focus on emotional bonding today.";
                rgLibido.check(R.id.rbVeryLow);
                break;
            case 1: // Low
                status = "Low";
                suggestion = "💞 Suggestion: How about a relaxing massage? No pressure, just connection.";
                rgLibido.check(R.id.rbLow);
                break;
            case 2: // Normal
                status = "Normal";
                suggestion = "💞 Suggestion: A romantic walk or a nice dinner together could be perfect.";
                rgLibido.check(R.id.rbNormal);
                break;
            case 3: // High
                status = "High";
                suggestion = "💞 Suggestion: High energy! Send a flirty text or plan a spontaneous date night.";
                rgLibido.check(R.id.rbHigh);
                break;
            case 4: // Very High
                status = "Very High";
                suggestion = "💞 Suggestion: Peak desire! Perfect time for deep intimacy and adventurous ideas.";
                rgLibido.check(R.id.rbVeryHigh);
                break;
        }
        tvLibidoStatus.setText(status);
        tvLibidoSuggestion.setText(suggestion);
    }

    private void showHormonalExplanation() {
        // Simple logic based on cycle day if available
        Calendar lastPeriod = dataManager.getLastPeriod();
        if (lastPeriod == null) {
            tvHormonalFact.setText("🧠 Estrogen rising usually increases desire, while Progesterone might lower it.");
            return;
        }

        int day = CycleHelper.daysUntil(lastPeriod, Calendar.getInstance()) + 1;
        if (day >= 12 && day <= 16) {
            tvHormonalFact.setText("🧠 Estrogen is peaking! This hormone rising usually triggers a significant spike in desire and energy.");
        } else if (day > 20) {
            tvHormonalFact.setText("🧠 Progesterone is rising. This hormone prepares the body for a possible pregnancy and often leads to a more 'relaxed' or lower libido.");
        } else {
            tvHormonalFact.setText("🧠 Your hormones are in a transitional phase. Libido can be mood-dependent during this time.");
        }
    }

    private void calculateCycleInsight() {
        Calendar lastPeriod = dataManager.getLastPeriod();
        if (lastPeriod != null) {
            int day = CycleHelper.daysUntil(lastPeriod, Calendar.getInstance()) + 1;
            if (day >= 12 && day <= 16) {
                tvCycleLibidoInsight.setText("Your desire is naturally higher right now due to the Ovulation phase.");
            } else {
                tvCycleLibidoInsight.setText("You are currently in a phase where libido is usually steady. Patterns often emerge around Day 14.");
            }
        }
    }
}
