package com.example.testing;

import android.content.Intent;
import android.os.Bundle;
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

    private CharacterViewModel characterViewModel;
    private int currentCharacterId = -1; // To check if we are editing or adding

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit_character);

        editTextName = findViewById(R.id.edit_text_character_name);
        editTextPersonality = findViewById(R.id.edit_text_character_personality);
        editTextModel = findViewById(R.id.edit_text_character_model);
        editTextFirstMessage = findViewById(R.id.edit_text_character_first_message);

        characterViewModel = new ViewModelProvider(this).get(CharacterViewModel.class);

        getSupportActionBar().setHomeAsUpIndicator(android.R.drawable.ic_menu_close_clear_cancel);

        // --- Check if we are editing or adding ---
        Intent intent = getIntent();
        if (intent.hasExtra("CHARACTER_ID")) {
            setTitle("Edit Character");
            currentCharacterId = intent.getIntExtra("CHARACTER_ID", -1);
            editTextName.setText(intent.getStringExtra("CHARACTER_NAME"));
            editTextPersonality.setText(intent.getStringExtra("CHARACTER_PERSONALITY"));
            editTextModel.setText(intent.getStringExtra("CHARACTER_MODEL"));
            editTextFirstMessage.setText(intent.getStringExtra("CHARACTER_FIRST_MESSAGE"));
        } else {
            setTitle("Add Character");
        }
    }

    private void saveCharacter() {
        String name = editTextName.getText().toString();
        String personality = editTextPersonality.getText().toString();
        String model = editTextModel.getText().toString();
        String firstMessage = editTextFirstMessage.getText().toString();

        if (name.trim().isEmpty() || personality.trim().isEmpty()) {
            Toast.makeText(this, "Please insert a name and personality", Toast.LENGTH_SHORT).show();
            return;
        }

        Character character = new Character(name, personality, firstMessage, model, "", "", "");

        if (currentCharacterId != -1) {
            // We are in edit mode, so we need to set the ID for the update
            character.setId(currentCharacterId);
            characterViewModel.update(character);
        } else {
            // We are in add mode
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