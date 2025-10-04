package com.example.testing;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

public class SettingsActivity extends AppCompatActivity {

    private EditText editTextUsername;
    private EditText editTextApiKey;
    private EditText editTextPreferredModel;
    private EditText editTextGlobalPrompt;
    private Button buttonSave;
    private SettingsViewModel settingsViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        setTitle("Settings");

        editTextUsername = findViewById(R.id.edit_text_username);
        editTextApiKey = findViewById(R.id.edit_text_api_key);
        editTextPreferredModel = findViewById(R.id.edit_text_preferred_model);
        editTextGlobalPrompt = findViewById(R.id.edit_text_global_system_prompt);
        buttonSave = findViewById(R.id.button_save_settings);

        settingsViewModel = new ViewModelProvider(this).get(SettingsViewModel.class);

        // Observe the User data and populate all fields
        settingsViewModel.getUser().observe(this, user -> {
            if (user != null) {
                if (user.getUsername() != null) editTextUsername.setText(user.getUsername());
                if (user.getApiKey() != null) editTextApiKey.setText(user.getApiKey());
                if (user.getPreferredModel() != null) editTextPreferredModel.setText(user.getPreferredModel());
                if (user.getGlobalSystemPrompt() != null) editTextGlobalPrompt.setText(user.getGlobalSystemPrompt());
            }
        });

        buttonSave.setOnClickListener(v -> saveSettings());
    }

    private void saveSettings() {
        String username = editTextUsername.getText().toString().trim();
        String apiKey = editTextApiKey.getText().toString().trim();
        String preferredModel = editTextPreferredModel.getText().toString().trim();
        String globalPrompt = editTextGlobalPrompt.getText().toString().trim();

        if (apiKey.isEmpty()) {
            Toast.makeText(this, "API Key cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }

        settingsViewModel.saveSettings(username, apiKey, preferredModel, globalPrompt);
        Toast.makeText(this, "Settings saved!", Toast.LENGTH_SHORT).show();
        finish();
    }
}