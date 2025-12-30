package com.example.testing;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.net.Uri;
import android.os.Bundle;
import android.util.TypedValue;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.canhub.cropper.CropImageContract;
import com.canhub.cropper.CropImageContractOptions;
import com.canhub.cropper.CropImageOptions;
import com.google.android.material.button.MaterialButton;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

public class AddEditScenarioActivity extends AppCompatActivity {

    public static final String EXTRA_CHARACTER_ID = "com.example.testing.EXTRA_CHARACTER_ID";
    public static final String EXTRA_SCENARIO = "com.example.testing.EXTRA_SCENARIO";

    private ImageView imageViewScenarioPreview;
    private Button buttonSelectImage;
    private EditText editTextName;
    private EditText editTextDescription;
    private EditText editTextFirstMessage;
    private CheckBox checkBoxDefault;
    private MaterialButton buttonSave;

    private CharacterViewModel characterViewModel;
    private int characterId = -1;
    private Scenario editingScenario;
    private String currentScenarioImagePath = "";

    private final ActivityResultLauncher<CropImageContractOptions> cropImage =
            registerForActivityResult(new CropImageContract(), result -> {
                if (result.isSuccessful()) {
                    Uri uriContent = result.getUriContent();
                    if (uriContent != null) {
                        saveImageToInternalStorage(uriContent);
                        Glide.with(this).load(currentScenarioImagePath).into(imageViewScenarioPreview);
                    }
                } else {
                    Exception error = result.getError();
                    Toast.makeText(this, "Crop failed: " + (error != null ? error.getMessage() : "Unknown"), Toast.LENGTH_SHORT).show();
                }
            });

    private final ActivityResultLauncher<PickVisualMediaRequest> pickMedia =
            registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), uri -> {
                if (uri != null) {
                    CropImageOptions options = new CropImageOptions();
                    options.fixAspectRatio = true;
                    options.aspectRatioX = 1;
                    options.aspectRatioY = 1;
                    cropImage.launch(new CropImageContractOptions(uri, options));
                } else {
                    Toast.makeText(this, "No image selected.", Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit_scenario);

        imageViewScenarioPreview = findViewById(R.id.image_view_scenario_preview);
        buttonSelectImage = findViewById(R.id.button_select_scenario_image);
        editTextName = findViewById(R.id.edit_scenario_name);
        editTextDescription = findViewById(R.id.edit_scenario_description);
        editTextFirstMessage = findViewById(R.id.edit_scenario_first_message);
        checkBoxDefault = findViewById(R.id.check_scenario_default);
        buttonSave = findViewById(R.id.button_save_scenario);

        // Apply Custom Secondary Color to Save Scenario Button (Text and Border)
        int secondaryColor = ThemeUtils.getSecondaryColor(this);
        buttonSave.setTextColor(secondaryColor);
        buttonSave.setStrokeColor(ColorStateList.valueOf(secondaryColor));
        buttonSave.setStrokeWidth((int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1, getResources().getDisplayMetrics()));

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
                currentScenarioImagePath = editingScenario.getImagePath();
                if (currentScenarioImagePath != null && !currentScenarioImagePath.isEmpty()) {
                    Glide.with(this).load(currentScenarioImagePath).into(imageViewScenarioPreview);
                }
            }
        } else {
            setTitle("Add Scenario");
        }

        buttonSelectImage.setOnClickListener(v -> {
            pickMedia.launch(new PickVisualMediaRequest.Builder()
                    .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                    .build());
        });

        buttonSave.setOnClickListener(v -> saveScenario());
    }

    private void saveImageToInternalStorage(Uri uri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            File directory = new File(getFilesDir(), "scenario_images");
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
            currentScenarioImagePath = file.getAbsolutePath();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to save image", Toast.LENGTH_SHORT).show();
        }
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
            editingScenario.setImagePath(currentScenarioImagePath);
            characterViewModel.updateScenario(editingScenario);
        } else {
            Scenario newScenario = new Scenario(characterId, name, description, firstMessage, isDefault, currentScenarioImagePath);
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