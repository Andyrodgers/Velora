package com.example.velora;

import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import java.util.List;

public class NotesActivity extends BaseActivity {

    private LinearLayout llNotesContainer;
    private EditText etNoteInput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notes);

        llNotesContainer = findViewById(R.id.llNotesContainer);
        etNoteInput = findViewById(R.id.etNoteInput);
        Button btnSave = findViewById(R.id.btnSaveNote);

        loadNotes();

        btnSave.setOnClickListener(v -> {
            String noteText = etNoteInput.getText().toString().trim();
            if (!noteText.isEmpty()) {
                dataManager.saveNote(noteText);
                etNoteInput.setText("");
                loadNotes(); // Refresh list
                Toast.makeText(this, "Note saved!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadNotes() {
        llNotesContainer.removeAllViews();
        List<String> notes = dataManager.getNotes();
        
        // Show notes in reverse order (newest first)
        for (int i = notes.size() - 1; i >= 0; i--) {
            addNoteToLayout(notes.get(i), i);
        }
    }

    private void addNoteToLayout(String note, int index) {
        LinearLayout noteBox = new LinearLayout(this);
        noteBox.setOrientation(LinearLayout.HORIZONTAL);
        noteBox.setBackgroundResource(android.R.drawable.editbox_dropdown_light_frame);
        noteBox.setPadding(24, 24, 24, 24);
        noteBox.setGravity(Gravity.CENTER_VERTICAL);
        
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 0, 24);
        noteBox.setLayoutParams(params);

        TextView tvNote = new TextView(this);
        tvNote.setText(note);
        tvNote.setTextColor(Color.BLACK);
        tvNote.setTextSize(16);
        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        tvNote.setLayoutParams(textParams);

        ImageButton btnDelete = new ImageButton(this);
        btnDelete.setImageResource(android.R.drawable.ic_menu_delete);
        btnDelete.setBackgroundColor(Color.TRANSPARENT);
        btnDelete.setOnClickListener(v -> {
            dataManager.deleteNote(index);
            loadNotes();
            Toast.makeText(this, "Note deleted", Toast.LENGTH_SHORT).show();
        });
        
        noteBox.addView(tvNote);
        noteBox.addView(btnDelete);
        llNotesContainer.addView(noteBox);
    }
}
