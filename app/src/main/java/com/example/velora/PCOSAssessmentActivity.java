package com.example.velora;

import android.os.Bundle;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;

public class PCOSAssessmentActivity extends BaseActivity {

    private RadioGroup rgQ1, rgQ2, rgQ3, rgQ4, rgQ5;
    private Button btnFinish;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pcos_assessment);

        rgQ1 = findViewById(R.id.rgQ1);
        rgQ2 = findViewById(R.id.rgQ2);
        rgQ3 = findViewById(R.id.rgQ3);
        rgQ4 = findViewById(R.id.rgQ4);
        rgQ5 = findViewById(R.id.rgQ5);
        btnFinish = findViewById(R.id.btnFinishAssessment);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        btnFinish.setOnClickListener(v -> calculateResult());
    }

    private void calculateResult() {
        int score = 0;
        if (rgQ1.getCheckedRadioButtonId() == R.id.rbQ1Yes) score++;
        if (rgQ2.getCheckedRadioButtonId() == R.id.rbQ2Yes) score++;
        if (rgQ3.getCheckedRadioButtonId() == R.id.rbQ3Yes) score++;
        if (rgQ4.getCheckedRadioButtonId() == R.id.rbQ4Yes) score++;
        if (rgQ5.getCheckedRadioButtonId() == R.id.rbQ5Yes) score++;

        String resultTitle;
        String resultMessage;

        if (score >= 3) {
            resultTitle = "High Risk Identified";
            resultMessage = "Based on your answers, there is a higher possibility of PCOS symptoms. We strongly recommend consulting a healthcare professional for a clinical diagnosis and blood tests.";
        } else if (score >= 1) {
            resultTitle = "Moderate Risk";
            resultMessage = "You have some symptoms associated with PCOS. It's a good idea to monitor these and discuss them with your doctor during your next visit.";
        } else {
            resultTitle = "Low Risk";
            resultMessage = "Based on your answers, you have few or no typical symptoms of PCOS. Continue monitoring your cycle regularly.";
        }

        new AlertDialog.Builder(this)
                .setTitle(resultTitle)
                .setMessage(resultMessage)
                .setPositiveButton("OK", (dialog, which) -> finish())
                .setCancelable(false)
                .show();
    }
}
