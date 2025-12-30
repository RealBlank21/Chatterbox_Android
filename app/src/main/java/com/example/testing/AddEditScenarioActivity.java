package com.example.testing;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

public class AddEditScenarioActivity extends AppCompatActivity {

    public static final String EXTRA_CHARACTER_ID = "com.example.testing.EXTRA_CHARACTER_ID";
    public static final String EXTRA_SCENARIO = "com.example.testing.EXTRA_SCENARIO";

    private EditText editTextName;
    private EditText editTextDescription;
    private EditText editTextFirstMessage;
    private CheckBox checkBoxDefault;
    private Button buttonSave;

    private CharacterViewModel characterViewModel;
    private int characterId = -1;
    private Scenario editingScenario;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit_scenario);

        editTextName = findViewById(R.id.edit_scenario_name);
        editTextDescription = findViewById(R.id.edit_scenario_description);
        editTextFirstMessage = findViewById(R.id.edit_scenario_first_message);
        checkBoxDefault = findViewById(R.id.check_scenario_default);
        buttonSave = findViewById(R.id.button_save_scenario);

        characterViewModel = new ViewModelProvider(this).get(CharacterViewModel.class);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setHomeAsUpIndicator(android.R.drawable.ic_menu_close_clear_cancel);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        Intent intent = getIntent();
        if (intent.hasExtra(EXTRA_CHARACTER_ID)) {
            characterId = intent.getIntExtra(EXTRA_CHARACTER_ID, -1);
        } else {
            Toast.makeText(this, "Error: No Character ID provided", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        if (intent.hasExtra(EXTRA_SCENARIO)) {
            setTitle("Edit Scenario");
            editingScenario = (Scenario) intent.getSerializableExtra(EXTRA_SCENARIO);
            if (editingScenario != null) {
                editTextName.setText(editingScenario.getName());
                editTextDescription.setText(editingScenario.getDescription());
                editTextFirstMessage.setText(editingScenario.getFirstMessage());
                checkBoxDefault.setChecked(editingScenario.isDefault());
            }
        } else {
            setTitle("Add Scenario");
        }

        buttonSave.setOnClickListener(v -> saveScenario());
    }

    private void saveScenario() {
        String name = editTextName.getText().toString().trim();
        String description = editTextDescription.getText().toString().trim();
        String firstMessage = editTextFirstMessage.getText().toString().trim();
        boolean isDefault = checkBoxDefault.isChecked();

        if (name.isEmpty()) {
            Toast.makeText(this, "Please insert a name", Toast.LENGTH_SHORT).show();
            return;
        }

        if (editingScenario != null) {
            editingScenario.setName(name);
            editingScenario.setDescription(description);
            editingScenario.setFirstMessage(firstMessage);
            editingScenario.setDefault(isDefault);
            characterViewModel.updateScenario(editingScenario);
        } else {
            Scenario newScenario = new Scenario(characterId, name, description, firstMessage, isDefault);
            characterViewModel.insertScenario(newScenario);
        }

        finish();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}