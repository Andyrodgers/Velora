package com.example.velora;

import android.os.Bundle;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.Toast;
import androidx.appcompat.widget.SwitchCompat;

public class SettingsActivity extends BaseActivity {

    private RadioGroup rgThemes;
    private SwitchCompat switchNotifications;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        rgThemes = findViewById(R.id.rgThemes);
        switchNotifications = findViewById(R.id.switchNotifications);
        Button btnApply = findViewById(R.id.btnApplyTheme);

        // Load current selection
        int currentTheme = dataManager.getTheme();
        if (currentTheme == 0) rgThemes.check(R.id.rbDefaultTheme);
        else if (currentTheme == 1) rgThemes.check(R.id.rbBlueTheme);
        else if (currentTheme == 2) rgThemes.check(R.id.rbGreenTheme);
        else if (currentTheme == 3) rgThemes.check(R.id.rbYellowTheme);
        else if (currentTheme == 4) rgThemes.check(R.id.rbPurpleTheme);
        else if (currentTheme == 5) rgThemes.check(R.id.rbMaroonTheme);

        // Load notification setting
        switchNotifications.setChecked(dataManager.isNotificationsEnabled());

        btnApply.setOnClickListener(v -> {
            // Save theme
            int selectedId = rgThemes.getCheckedRadioButtonId();
            int themeVal = 0;
            if (selectedId == R.id.rbDefaultTheme) themeVal = 0;
            else if (selectedId == R.id.rbBlueTheme) themeVal = 1;
            else if (selectedId == R.id.rbGreenTheme) themeVal = 2;
            else if (selectedId == R.id.rbYellowTheme) themeVal = 3;
            else if (selectedId == R.id.rbPurpleTheme) themeVal = 4;
            else if (selectedId == R.id.rbMaroonTheme) themeVal = 5;

            dataManager.setTheme(themeVal);
            
            // Save notifications setting
            dataManager.setNotificationsEnabled(switchNotifications.isChecked());
            
            Toast.makeText(this, "Settings applied!", Toast.LENGTH_SHORT).show();
            
            // Restart activity to apply theme change immediately
            recreate();
        });
    }
}
