package com.example.velora;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.List;

public class SymptomsActivity extends BaseActivity {

    private Spinner moodSpinner;
    private EditText etNotes;
    private Button btnSaveLog;
    private CheckBox cbCramps, cbHeadache, cbBloating, cbAcne, cbBackache, cbNausea, cbTiredness, cbTenderBreasts;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_symptoms);

        moodSpinner = findViewById(R.id.moodSpinner);
        etNotes = findViewById(R.id.etNotes);
        btnSaveLog = findViewById(R.id.btnSaveLog);
        
        cbCramps = findViewById(R.id.cbCramps);
        cbHeadache = findViewById(R.id.cbHeadache);
        cbBloating = findViewById(R.id.cbBloating);
        cbAcne = findViewById(R.id.cbAcne);
        cbBackache = findViewById(R.id.cbBackache);
        cbNausea = findViewById(R.id.cbNausea);
        cbTiredness = findViewById(R.id.cbTiredness);
        cbTenderBreasts = findViewById(R.id.cbTenderBreasts);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        String[] moods = {"Happy", "Irritable", "Tired", "Energetic", "Sad", "Anxious", "Calm"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.spinner_item, moods);
        adapter.setDropDownViewResource(R.layout.spinner_item);
        moodSpinner.setAdapter(adapter);

        btnSaveLog.setOnClickListener(v -> saveSymptoms());
    }

    private void saveSymptoms() {
        String mood = moodSpinner.getSelectedItem().toString();
        
        List<String> selectedSymptoms = new ArrayList<>();
        if (cbCramps.isChecked()) selectedSymptoms.add("Cramps");
        if (cbHeadache.isChecked()) selectedSymptoms.add("Headache");
        if (cbBloating.isChecked()) selectedSymptoms.add("Bloating");
        if (cbAcne.isChecked()) selectedSymptoms.add("Acne");
        if (cbBackache.isChecked()) selectedSymptoms.add("Backache");
        if (cbNausea.isChecked()) selectedSymptoms.add("Nausea");
        if (cbTiredness.isChecked()) selectedSymptoms.add("Tiredness");
        if (cbTenderBreasts.isChecked()) selectedSymptoms.add("Tender Breasts");

        String notes = etNotes.getText().toString().trim();

        StringBuilder logBuilder = new StringBuilder();
        logBuilder.append("Mood: ").append(mood);
        
        if (!selectedSymptoms.isEmpty()) {
            logBuilder.append(" | Symptoms: ").append(String.join(", ", selectedSymptoms));
        }
        
        if (!notes.isEmpty()) {
            logBuilder.append(" | Notes: ").append(notes);
        }

        dataManager.saveNote(logBuilder.toString());

        Toast.makeText(this, "Symptoms logged successfully!", Toast.LENGTH_SHORT).show();
        finish();
    }
}
