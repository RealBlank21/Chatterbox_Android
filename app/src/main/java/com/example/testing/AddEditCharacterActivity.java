package com.example.testing;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.example.testing.network.response.Model;
import com.google.android.material.switchmaterial.SwitchMaterial;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class AddEditCharacterActivity extends AppCompatActivity {

    private ImageView imageViewProfilePreview;
    private Button buttonSelectImage;
    private EditText editTextName, editTextPersonality, editTextFirstMessage, editTextTemperature, editTextMaxTokens;
    private AutoCompleteTextView editTextModel;
    private ImageButton buttonRefreshModels;
    private TextView textViewModelInfo;
    private SwitchMaterial switchTimeAwareness;

    private CharacterViewModel characterViewModel;
    private int currentCharacterId = -1;
    private String currentProfileImagePath = "";

    private ArrayAdapter<String> modelsAdapter;
    private List<String> modelIds = new ArrayList<>();

    private final ActivityResultLauncher<PickVisualMediaRequest> pickMedia =
            registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), uri -> {
                if (uri != null) {
                    saveImageToInternalStorage(uri);
                    Glide.with(this).load(currentProfileImagePath).into(imageViewProfilePreview);
                } else {
                    Toast.makeText(this, "No image selected.", Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit_character);

        imageViewProfilePreview = findViewById(R.id.image_view_profile_preview);
        buttonSelectImage = findViewById(R.id.button_select_image);
        editTextName = findViewById(R.id.edit_text_character_name);
        editTextPersonality = findViewById(R.id.edit_text_character_personality);
        editTextModel = findViewById(R.id.edit_text_character_model);
        buttonRefreshModels = findViewById(R.id.button_refresh_models);
        textViewModelInfo = findViewById(R.id.text_view_model_info);
        editTextFirstMessage = findViewById(R.id.edit_text_character_first_message);
        editTextTemperature = findViewById(R.id.edit_text_temperature);
        editTextMaxTokens = findViewById(R.id.edit_text_max_tokens);
        switchTimeAwareness = findViewById(R.id.switch_time_awareness);

        // Initialize Adapter for Models
        modelsAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, modelIds);
        editTextModel.setAdapter(modelsAdapter);

        // Load Models from Repository
        ModelRepository.getInstance().getModels().observe(this, models -> {
            if (models != null) {
                modelIds.clear();
                for (Model m : models) {
                    modelIds.add(m.getId());
                }
                modelsAdapter.notifyDataSetChanged();
            }
        });

        // Manual Refresh
        buttonRefreshModels.setOnClickListener(v -> {
            Toast.makeText(this, "Refreshing models...", Toast.LENGTH_SHORT).show();
            ModelRepository.getInstance().refreshModels();
        });

        // Show model info on selection
        editTextModel.setOnItemClickListener((parent, view, position, id) -> {
            String selectedId = modelsAdapter.getItem(position);
            updateModelInfo(selectedId);
        });

        // Text watcher to update info if typed manually
        editTextModel.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                updateModelInfo(s.toString());
            }
        });

        characterViewModel = new ViewModelProvider(this).get(CharacterViewModel.class);

        getSupportActionBar().setHomeAsUpIndicator(android.R.drawable.ic_menu_close_clear_cancel);

        buttonSelectImage.setOnClickListener(v -> {
            pickMedia.launch(new PickVisualMediaRequest.Builder()
                    .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                    .build());
        });

        Intent intent = getIntent();
        if (intent.hasExtra("CHARACTER_ID")) {
            setTitle("Edit Character");
            currentCharacterId = intent.getIntExtra("CHARACTER_ID", -1);
            characterViewModel.getCharacterById(currentCharacterId).observe(this, character -> {
                if (character != null) {
                    editTextName.setText(character.getName());
                    editTextPersonality.setText(character.getPersonality());

                    editTextModel.setText(character.getModel());
                    updateModelInfo(character.getModel()); // Show info for loaded model

                    editTextFirstMessage.setText(character.getFirstMessage());
                    if (character.getTemperature() != null) editTextTemperature.setText(String.valueOf(character.getTemperature()));
                    if (character.getMaxTokens() != null) editTextMaxTokens.setText(String.valueOf(character.getMaxTokens()));
                    switchTimeAwareness.setChecked(character.isTimeAware());

                    currentProfileImagePath = character.getCharacterProfileImagePath();
                    if (!TextUtils.isEmpty(currentProfileImagePath)) {
                        Glide.with(this).load(currentProfileImagePath).into(imageViewProfilePreview);
                    }
                }
            });
        } else {
            setTitle("Add Character");
        }
    }

    private void updateModelInfo(String modelId) {
        Model model = ModelRepository.getInstance().getModelById(modelId);
        if (model != null) {
            textViewModelInfo.setVisibility(View.VISIBLE);
            String info = "Name: " + model.getName() + "\n" +
                    "Context: " + model.getContextLength() + "\n" +
                    model.getFormattedPricing() + "\n\n" +
                    (model.getDescription() != null ? model.getDescription() : "No description available.");
            textViewModelInfo.setText(info);
        } else {
            textViewModelInfo.setVisibility(View.GONE);
        }
    }

    private void saveImageToInternalStorage(Uri uri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            File directory = new File(getFilesDir(), "profile_images");
            if (!directory.exists()) {
                directory.mkdirs();
            }
            File file = new File(directory, UUID.randomUUID().toString() + ".jpg");
            OutputStream outputStream = new FileOutputStream(file);
            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }
            outputStream.close();
            inputStream.close();
            currentProfileImagePath = file.getAbsolutePath();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to save image", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveCharacter() {
        String name = editTextName.getText().toString();
        String personality = editTextPersonality.getText().toString();
        String model = editTextModel.getText().toString();
        String firstMessage = editTextFirstMessage.getText().toString();
        String tempStr = editTextTemperature.getText().toString();
        String maxTokensStr = editTextMaxTokens.getText().toString();
        boolean isTimeAware = switchTimeAwareness.isChecked();

        if (name.trim().isEmpty()) {
            Toast.makeText(this, "Please insert a name", Toast.LENGTH_SHORT).show();
            return;
        }

        Float temperature = null;
        if (!TextUtils.isEmpty(tempStr)) {
            try {
                temperature = Float.parseFloat(tempStr);
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Invalid Temperature format", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        Integer maxTokens = null;
        if (!TextUtils.isEmpty(maxTokensStr)) {
            try {
                maxTokens = Integer.parseInt(maxTokensStr);
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Invalid Max Tokens format", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        Character character = new Character(name, personality, firstMessage, model, currentProfileImagePath, "", "", temperature, maxTokens, isTimeAware);

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
        getMenuInflater().inflate(R.menu.add_character_menu, menu);
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