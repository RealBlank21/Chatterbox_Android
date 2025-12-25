package com.example.testing;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.TypedValue;
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
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.switchmaterial.SwitchMaterial;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class AddEditCharacterActivity extends BaseActivity {

    private ImageView imageViewProfilePreview;
    private Button buttonSelectImage;
    private EditText editTextName, editTextPersonality, editTextFirstMessage, editTextTemperature, editTextMaxTokens, editTextContextLimit;
    private AutoCompleteTextView editTextModel;
    private ImageButton buttonRefreshModels;
    private TextView textViewModelInfo;
    private SwitchMaterial switchTimeAwareness;
    private SwitchMaterial switchAllowImageInput;

    // Tags UI
    private AutoCompleteTextView editTextNewTag; // Changed to AutoCompleteTextView
    private ImageButton buttonAddTag;
    private ChipGroup chipGroupTags;
    private final List<String> currentTags = new ArrayList<>();

    private CharacterViewModel characterViewModel;
    private int currentCharacterId = -1;
    private String currentProfileImagePath = "";
    private boolean isFavorite = false;
    private boolean isHidden = false;
    private long createdAt;

    private ArrayAdapter<String> modelsAdapter;
    private List<String> modelIds = new ArrayList<>();

    // Adapter for existing tags
    private ArrayAdapter<String> tagsAdapter;

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
        editTextContextLimit = findViewById(R.id.edit_text_context_limit);
        switchTimeAwareness = findViewById(R.id.switch_time_awareness);
        switchAllowImageInput = findViewById(R.id.switch_allow_image_input);

        // Tags Init
        editTextNewTag = findViewById(R.id.edit_text_new_tag);
        buttonAddTag = findViewById(R.id.button_add_tag);
        chipGroupTags = findViewById(R.id.chip_group_character_tags);

        // --- Model Adapter Setup ---
        modelsAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, modelIds);
        editTextModel.setAdapter(modelsAdapter);

        // --- View Model Setup ---
        characterViewModel = new ViewModelProvider(this).get(CharacterViewModel.class);

        // --- Tags Adapter Setup ---
        tagsAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, new ArrayList<>());
        editTextNewTag.setAdapter(tagsAdapter);

        // Observe all tags to populate suggestions
        characterViewModel.getAllTags().observe(this, tags -> {
            if (tags != null) {
                tagsAdapter.clear();
                tagsAdapter.addAll(tags);
                tagsAdapter.notifyDataSetChanged();
            }
        });

        // When a tag is selected from dropdown, add it immediately
        editTextNewTag.setOnItemClickListener((parent, view, position, id) -> {
            String selectedTag = tagsAdapter.getItem(position);
            addNewTag(selectedTag);
            editTextNewTag.setText("");
        });

        // Add Tag Button Logic (for manual typing)
        buttonAddTag.setOnClickListener(v -> {
            String tag = editTextNewTag.getText().toString().trim();
            addNewTag(tag);
        });

        // --- Model Repository Logic ---
        ModelRepository.getInstance().getModels().observe(this, models -> {
            if (models != null) {
                modelIds.clear();
                for (Model m : models) {
                    modelIds.add(m.getId());
                }
                modelsAdapter.notifyDataSetChanged();
            }
        });

        buttonRefreshModels.setOnClickListener(v -> {
            Toast.makeText(this, "Refreshing models...", Toast.LENGTH_SHORT).show();
            ModelRepository.getInstance().refreshModels();
        });

        editTextModel.setOnItemClickListener((parent, view, position, id) -> {
            String selectedId = modelsAdapter.getItem(position);
            updateModelInfo(selectedId);
        });

        editTextModel.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                updateModelInfo(s.toString());
            }
        });


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
                    updateModelInfo(character.getModel());

                    editTextFirstMessage.setText(character.getFirstMessage());
                    if (character.getTemperature() != null) editTextTemperature.setText(String.valueOf(character.getTemperature()));
                    if (character.getMaxTokens() != null) editTextMaxTokens.setText(String.valueOf(character.getMaxTokens()));
                    if (character.getContextLimit() != null) editTextContextLimit.setText(String.valueOf(character.getContextLimit()));

                    switchTimeAwareness.setChecked(character.isTimeAware());
                    switchAllowImageInput.setChecked(character.isAllowImageInput());

                    currentProfileImagePath = character.getCharacterProfileImagePath();
                    if (!TextUtils.isEmpty(currentProfileImagePath)) {
                        Glide.with(this).load(currentProfileImagePath).into(imageViewProfilePreview);
                    }

                    isFavorite = character.isFavorite();
                    isHidden = character.isHidden();
                    createdAt = character.getCreatedAt();

                    // Load Tags
                    loadTags(character.getTags());
                }
            });
        } else {
            setTitle("Add Character");
        }
    }

    private void addNewTag(String tag) {
        if (!tag.isEmpty()) {
            if (!currentTags.contains(tag)) {
                addTagChip(tag);
                currentTags.add(tag);
                editTextNewTag.setText("");
            } else {
                Toast.makeText(this, "Tag already exists", Toast.LENGTH_SHORT).show();
                editTextNewTag.setText("");
            }
        }
    }

    private void loadTags(String tagsString) {
        currentTags.clear();
        chipGroupTags.removeAllViews();
        if (tagsString != null && !tagsString.isEmpty()) {
            String[] split = tagsString.split("\\|");
            for (String tag : split) {
                if (!tag.trim().isEmpty()) {
                    currentTags.add(tag.trim());
                    addTagChip(tag.trim());
                }
            }
        }
    }

    private void addTagChip(String tag) {
        Chip chip = new Chip(this);
        chip.setText(tag);
        chip.setCloseIconVisible(true);
        chip.setOnCloseIconClickListener(v -> {
            chipGroupTags.removeView(chip);
            currentTags.remove(tag);
        });

        // --- Coloring Logic ---
        int color = getTagColor(tag);
        chip.setTextColor(color);
        chip.setChipStrokeColor(ColorStateList.valueOf(color));
        chip.setChipStrokeWidth(dpToPx(1));
        chip.setChipBackgroundColor(ColorStateList.valueOf(Color.TRANSPARENT));
        chip.setCloseIconTint(ColorStateList.valueOf(color));
        // ----------------------

        chipGroupTags.addView(chip);
    }

    private int getTagColor(String tag) {
        int hash = tag.hashCode();
        float[] hsv = new float[3];
        hsv[0] = Math.abs(hash) % 360;
        hsv[1] = 0.6f + (Math.abs(hash * 7) % 40) / 100f;
        hsv[2] = 0.65f + (Math.abs(hash * 13) % 35) / 100f; // Slightly darker for edit view to be readable
        return Color.HSVToColor(hsv);
    }

    private float dpToPx(int dp) {
        return TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics());
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
        String contextLimitStr = editTextContextLimit.getText().toString();
        boolean isTimeAware = switchTimeAwareness.isChecked();
        boolean allowImageInput = switchAllowImageInput.isChecked();

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

        Integer contextLimit = null;
        if (!TextUtils.isEmpty(contextLimitStr)) {
            try {
                int val = Integer.parseInt(contextLimitStr);
                if (val > 0) contextLimit = val;
            } catch (NumberFormatException e) {
            }
        }

        // Process Tags
        StringBuilder tagsBuilder = new StringBuilder();
        for (String t : currentTags) {
            if (tagsBuilder.length() > 0) tagsBuilder.append("|");
            tagsBuilder.append(t);
        }
        String tags = tagsBuilder.toString();

        Character character = new Character(name, personality, firstMessage, model, currentProfileImagePath, "", "", temperature, maxTokens, isTimeAware, allowImageInput, contextLimit, tags);
        character.setFavorite(isFavorite);
        character.setHidden(isHidden);

        if (currentCharacterId != -1) {
            character.setId(currentCharacterId);
            character.setCreatedAt(createdAt);
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