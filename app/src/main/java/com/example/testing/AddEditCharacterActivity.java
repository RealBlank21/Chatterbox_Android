package com.example.testing;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

public class AddEditCharacterActivity extends AppCompatActivity {

    private EditText editTextName;
    private EditText editTextPersonality;
    private EditText editTextModel;
    private EditText editTextFirstMessage;
    private EditText editTextTemperature; // Added
    private EditText editTextMaxTokens;   // Added

    private CharacterViewModel characterViewModel;
    private int currentCharacterId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit_character);

        editTextName = findViewById(R.id.edit_text_character_name);
        editTextPersonality = findViewById(R.id.edit_text_character_personality);
        editTextModel = findViewById(R.id.edit_text_character_model);
        editTextFirstMessage = findViewById(R.id.edit_text_character_first_message);
        editTextTemperature = findViewById(R.id.edit_text_temperature); // Added
        editTextMaxTokens = findViewById(R.id.edit_text_max_tokens);   // Added

        characterViewModel = new ViewModelProvider(this).get(CharacterViewModel.class);

        getSupportActionBar().setHomeAsUpIndicator(android.R.drawable.ic_menu_close_clear_cancel);

        Intent intent = getIntent();
        if (intent.hasExtra("CHARACTER_ID")) {
            setTitle("Edit Character");
            currentCharacterId = intent.getIntExtra("CHARACTER_ID", -1);
            // We will fetch the full character from the DB to populate the fields
            characterViewModel.getCharacterById(currentCharacterId).observe(this, character -> {
                if (character != null) {
                    editTextName.setText(character.getName());
                    editTextPersonality.setText(character.getPersonality());
                    editTextModel.setText(character.getModel());
                    editTextFirstMessage.setText(character.getFirstMessage());
                    if (character.getTemperature() != null) {
                        editTextTemperature.setText(String.valueOf(character.getTemperature()));
                    }
                    if (character.getMaxTokens() != null) {
                        editTextMaxTokens.setText(String.valueOf(character.getMaxTokens()));
                    }
                }
            });

        } else {
            setTitle("Add Character");
        }
    }

    private void saveCharacter() {
        String name = editTextName.getText().toString();
        String personality = editTextPersonality.getText().toString();
        String model = editTextModel.getText().toString();
        String firstMessage = editTextFirstMessage.getText().toString();
        String tempStr = editTextTemperature.getText().toString();
        String maxTokensStr = editTextMaxTokens.getText().toString();

        if (name.trim().isEmpty()) {
            Toast.makeText(this, "Please insert a name", Toast.LENGTH_SHORT).show();
            return;
        }

        Float temperature = null;
        if (!TextUtils.isEmpty(tempStr)) {
            try {
                temperature = Float.parseFloat(tempStr);
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Invalid temperature format", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        Integer maxTokens = null;
        if (!TextUtils.isEmpty(maxTokensStr)) {
            try {
                maxTokens = Integer.parseInt(maxTokensStr);
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Invalid max tokens format", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        Character character = new Character(name, personality, firstMessage, model, "", "", "", temperature, maxTokens);

        if (currentCharacterId != -1) {
            character.setId(currentCharacterId);
            characterViewModel.update(character);
        } else {
            characterViewModel.insert(character);
        }

        finish();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.add_character_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.save_character) {
            saveCharacter();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}