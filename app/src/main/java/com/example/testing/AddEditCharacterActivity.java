package com.example.testing;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
// --- ADD THESE IMPORTS ---
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
// -------------------------
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

public class AddEditCharacterActivity extends AppCompatActivity {

    private ImageView imageViewProfilePreview;
    private Button buttonSelectImage;
    // --- CHANGE THE TYPE FOR EDITTEXTMODEL ---
    private EditText editTextName, editTextPersonality, editTextFirstMessage, editTextTemperature, editTextMaxTokens;
    private AutoCompleteTextView editTextModel;
    // ---------------------------------------

    private CharacterViewModel characterViewModel;
    private int currentCharacterId = -1;
    private String currentProfileImagePath = ""; // To store the path of the selected image

    // The modern way to handle activity results (like picking a photo)
    private final ActivityResultLauncher<PickVisualMediaRequest> pickMedia =
            registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), uri -> {
                if (uri != null) {
                    // Photo was selected
                    saveImageToInternalStorage(uri);
                    Glide.with(this).load(currentProfileImagePath).into(imageViewProfilePreview);
                } else {
                    // No photo was selected
                    Toast.makeText(this, "No image selected.", Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit_character);

        // Find all views
        imageViewProfilePreview = findViewById(R.id.image_view_profile_preview);
        buttonSelectImage = findViewById(R.id.button_select_image);
        editTextName = findViewById(R.id.edit_text_character_name);
        editTextPersonality = findViewById(R.id.edit_text_character_personality);
        // --- THIS ID IS NOW AN AUTOCOMPLETETEXTVIEW ---
        editTextModel = findViewById(R.id.edit_text_character_model);
        // ---------------------------------------------
        editTextFirstMessage = findViewById(R.id.edit_text_character_first_message);
        editTextTemperature = findViewById(R.id.edit_text_temperature);
        editTextMaxTokens = findViewById(R.id.edit_text_max_tokens);

        // --- ADD THIS BLOCK TO SET UP THE ADAPTER ---
        // Get the string array from resources
        String[] models = getResources().getStringArray(R.array.ai_model_suggestions);
        // Create the adapter
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, models);
        // Set the adapter to the AutoCompleteTextView
        editTextModel.setAdapter(adapter);
        // ------------------------------------------

        characterViewModel = new ViewModelProvider(this).get(CharacterViewModel.class);

        getSupportActionBar().setHomeAsUpIndicator(android.R.drawable.ic_menu_close_clear_cancel);

        // Set up the button to launch the photo picker
        buttonSelectImage.setOnClickListener(v -> {
            pickMedia.launch(new PickVisualMediaRequest.Builder()
                    .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                    .build());
        });

        // Check if we are editing and populate fields
        Intent intent = getIntent();
        if (intent.hasExtra("CHARACTER_ID")) {
            setTitle("Edit Character");
            currentCharacterId = intent.getIntExtra("CHARACTER_ID", -1);
            characterViewModel.getCharacterById(currentCharacterId).observe(this, character -> {
                if (character != null) {
                    editTextName.setText(character.getName());
                    editTextPersonality.setText(character.getPersonality());
                    // ... (set other text fields)
                    editTextModel.setText(character.getModel());
                    editTextFirstMessage.setText(character.getFirstMessage());
                    if (character.getTemperature() != null) editTextTemperature.setText(String.valueOf(character.getTemperature()));
                    if (character.getMaxTokens() != null) editTextMaxTokens.setText(String.valueOf(character.getMaxTokens()));

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

    // This helper method copies the selected image to a private directory in your app
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
            currentProfileImagePath = file.getAbsolutePath(); // Store the path
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to save image", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveCharacter() {
        String name = editTextName.getText().toString();
        // ... (get text from other fields)
        String personality = editTextPersonality.getText().toString();
        String model = editTextModel.getText().toString();
        String firstMessage = editTextFirstMessage.getText().toString();
        String tempStr = editTextTemperature.getText().toString();
        String maxTokensStr = editTextMaxTokens.getText().toString();

        if (name.trim().isEmpty()) {
            Toast.makeText(this, "Please insert a name", Toast.LENGTH_SHORT).show();
            return;
        }

        // ... (parsing for temperature and maxTokens remains the same)
        Float temperature = null;
        if (!TextUtils.isEmpty(tempStr)) temperature = Float.parseFloat(tempStr);
        Integer maxTokens = null;
        if (!TextUtils.isEmpty(maxTokensStr)) maxTokens = Integer.parseInt(maxTokensStr);

        // Create the character with the saved image path
        Character character = new Character(name, personality, firstMessage, model, currentProfileImagePath, "", "", temperature, maxTokens);

        if (currentCharacterId != -1) {
            character.setId(currentCharacterId);
            characterViewModel.update(character);
        } else {
            characterViewModel.insert(character);
        }

        finish();
    }

    // ... (onCreateOptionsMenu and onOptionsItemSelected remain the same)
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