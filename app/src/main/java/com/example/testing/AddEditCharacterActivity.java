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
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.canhub.cropper.CropImageContract;
import com.canhub.cropper.CropImageContractOptions;
import com.canhub.cropper.CropImageOptions;
import com.example.testing.network.response.Model;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.switchmaterial.SwitchMaterial;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class AddEditCharacterActivity extends BaseActivity {

    private ImageView imageViewProfilePreview;
    private Button buttonSelectImage;
    private Button buttonImportJson;
    private EditText editTextName, editTextPersonality, editTextDefaultScenario, editTextFirstMessage, editTextTemperature, editTextMaxTokens, editTextContextLimit;
    private AutoCompleteTextView editTextModel;
    private ImageButton buttonRefreshModels;
    private TextView textViewModelInfo;
    private SwitchMaterial switchTimeAwareness;
    private SwitchMaterial switchAllowImageInput;

    private LinearLayout layoutModelSettingsHeader;
    private LinearLayout layoutModelSettingsContainer;
    private ImageView imageViewSettingsArrow;

    private LinearLayout layoutPersonalityHeader;
    private LinearLayout layoutPersonalityContainer;
    private ImageView imageViewPersonalityArrow;

    private LinearLayout layoutDefaultScenarioHeader;
    private LinearLayout layoutDefaultScenarioContainer;
    private ImageView imageViewDefaultScenarioArrow;

    private AutoCompleteTextView editTextNewTag;
    private ImageButton buttonAddTag;
    private ChipGroup chipGroupTags;
    private final List<String> currentTags = new ArrayList<>();

    private RecyclerView recyclerViewScenarios;
    private ScenarioAdapter scenarioAdapter;
    private MaterialButton buttonAddScenario;
    private LinearLayout layoutScenariosContainer;

    private CharacterViewModel characterViewModel;
    private int currentCharacterId = -1;
    private Character editingCharacter;
    private String currentProfileImagePath = "";
    private boolean isFavorite = false;
    private boolean isHidden = false;
    private long createdAt;
    private String currentVoiceReferenceId = "";
    private String currentVoiceReferenceName = "";
    private int conversationCount = 0;

    private ArrayAdapter<String> modelsAdapter;
    private List<String> modelIds = new ArrayList<>();

    private ArrayAdapter<String> tagsAdapter;

    private final ActivityResultLauncher<CropImageContractOptions> cropImage =
            registerForActivityResult(new CropImageContract(), result -> {
                if (result.isSuccessful()) {
                    Uri uriContent = result.getUriContent();
                    if (uriContent != null) {
                        saveImageToInternalStorage(uriContent);
                        Glide.with(this).load(currentProfileImagePath).into(imageViewProfilePreview);
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

    private final ActivityResultLauncher<String[]> openJsonFile =
            registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri -> {
                if (uri != null) {
                    importCharacterFromJson(uri);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit_character);

        imageViewProfilePreview = findViewById(R.id.image_view_profile_preview);
        buttonSelectImage = findViewById(R.id.button_select_image);
        buttonImportJson = findViewById(R.id.button_import_json);
        editTextName = findViewById(R.id.edit_text_character_name);
        editTextPersonality = findViewById(R.id.edit_text_character_personality);
        editTextDefaultScenario = findViewById(R.id.edit_text_default_scenario);
        editTextModel = findViewById(R.id.edit_text_character_model);
        buttonRefreshModels = findViewById(R.id.button_refresh_models);
        textViewModelInfo = findViewById(R.id.text_view_model_info);
        editTextFirstMessage = findViewById(R.id.edit_text_character_first_message);
        editTextTemperature = findViewById(R.id.edit_text_temperature);
        editTextMaxTokens = findViewById(R.id.edit_text_max_tokens);
        editTextContextLimit = findViewById(R.id.edit_text_context_limit);
        switchTimeAwareness = findViewById(R.id.switch_time_awareness);
        switchAllowImageInput = findViewById(R.id.switch_allow_image_input);

        layoutModelSettingsHeader = findViewById(R.id.layout_model_settings_header);
        layoutModelSettingsContainer = findViewById(R.id.layout_model_settings_container);
        imageViewSettingsArrow = findViewById(R.id.image_view_settings_arrow);

        layoutPersonalityHeader = findViewById(R.id.layout_personality_header);
        layoutPersonalityContainer = findViewById(R.id.layout_personality_container);
        imageViewPersonalityArrow = findViewById(R.id.image_view_personality_arrow);

        layoutDefaultScenarioHeader = findViewById(R.id.layout_default_scenario_header);
        layoutDefaultScenarioContainer = findViewById(R.id.layout_default_scenario_container);
        imageViewDefaultScenarioArrow = findViewById(R.id.image_view_default_scenario_arrow);

        editTextNewTag = findViewById(R.id.edit_text_new_tag);
        buttonAddTag = findViewById(R.id.button_add_tag);
        chipGroupTags = findViewById(R.id.chip_group_character_tags);

        recyclerViewScenarios = findViewById(R.id.recycler_view_scenarios);
        buttonAddScenario = findViewById(R.id.button_add_scenario);
        layoutScenariosContainer = findViewById(R.id.layout_scenarios_container);

        int secondaryColor = ThemeUtils.getSecondaryColor(this);
        buttonAddScenario.setTextColor(secondaryColor);
        buttonAddScenario.setStrokeColor(ColorStateList.valueOf(secondaryColor));
        buttonAddScenario.setStrokeWidth((int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1, getResources().getDisplayMetrics()));

        recyclerViewScenarios.setLayoutManager(new LinearLayoutManager(this));
        scenarioAdapter = new ScenarioAdapter();
        recyclerViewScenarios.setAdapter(scenarioAdapter);

        modelsAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, modelIds);
        editTextModel.setAdapter(modelsAdapter);

        characterViewModel = new ViewModelProvider(this).get(CharacterViewModel.class);

        tagsAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, new ArrayList<>());
        editTextNewTag.setAdapter(tagsAdapter);

        characterViewModel.getAllTags().observe(this, tags -> {
            if (tags != null) {
                tagsAdapter.clear();
                tagsAdapter.addAll(tags);
                tagsAdapter.notifyDataSetChanged();
            }
        });

        editTextNewTag.setOnItemClickListener((parent, view, position, id) -> {
            String selectedTag = tagsAdapter.getItem(position);
            addNewTag(selectedTag);
            editTextNewTag.setText("");
        });

        buttonAddTag.setOnClickListener(v -> {
            String tag = editTextNewTag.getText().toString().trim();
            addNewTag(tag);
        });

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

        layoutModelSettingsHeader.setOnClickListener(v -> {
            if (layoutModelSettingsContainer.getVisibility() == View.VISIBLE) {
                layoutModelSettingsContainer.setVisibility(View.GONE);
                imageViewSettingsArrow.setImageResource(android.R.drawable.arrow_down_float);
            } else {
                layoutModelSettingsContainer.setVisibility(View.VISIBLE);
                imageViewSettingsArrow.setImageResource(android.R.drawable.arrow_up_float);
            }
        });

        layoutPersonalityHeader.setOnClickListener(v -> {
            if (layoutPersonalityContainer.getVisibility() == View.VISIBLE) {
                layoutPersonalityContainer.setVisibility(View.GONE);
                imageViewPersonalityArrow.setImageResource(android.R.drawable.arrow_down_float);
            } else {
                layoutPersonalityContainer.setVisibility(View.VISIBLE);
                imageViewPersonalityArrow.setImageResource(android.R.drawable.arrow_up_float);
            }
        });

        layoutDefaultScenarioHeader.setOnClickListener(v -> {
            if (layoutDefaultScenarioContainer.getVisibility() == View.VISIBLE) {
                layoutDefaultScenarioContainer.setVisibility(View.GONE);
                imageViewDefaultScenarioArrow.setImageResource(android.R.drawable.arrow_down_float);
            } else {
                layoutDefaultScenarioContainer.setVisibility(View.VISIBLE);
                imageViewDefaultScenarioArrow.setImageResource(android.R.drawable.arrow_up_float);
            }
        });

        getSupportActionBar().setHomeAsUpIndicator(android.R.drawable.ic_menu_close_clear_cancel);

        buttonSelectImage.setOnClickListener(v -> {
            pickMedia.launch(new PickVisualMediaRequest.Builder()
                    .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                    .build());
        });

        buttonImportJson.setOnClickListener(v -> {
            openJsonFile.launch(new String[]{"application/json", "text/plain", "*/*"});
        });

        scenarioAdapter.setOnScenarioActionListener(new ScenarioAdapter.OnScenarioActionListener() {
            @Override
            public void onEdit(Scenario scenario) {
                Intent intent = new Intent(AddEditCharacterActivity.this, AddEditScenarioActivity.class);
                intent.putExtra(AddEditScenarioActivity.EXTRA_CHARACTER_ID, currentCharacterId);
                intent.putExtra(AddEditScenarioActivity.EXTRA_SCENARIO, scenario);
                startActivity(intent);
            }

            @Override
            public void onDelete(Scenario scenario) {
                new AlertDialog.Builder(AddEditCharacterActivity.this)
                        .setTitle("Delete Scenario")
                        .setMessage("Are you sure you want to delete this scenario?")
                        .setPositiveButton("Yes", (dialog, which) -> characterViewModel.deleteScenario(scenario))
                        .setNegativeButton("No", null)
                        .show();
            }
        });

        buttonAddScenario.setOnClickListener(v -> {
            Intent intent = new Intent(AddEditCharacterActivity.this, AddEditScenarioActivity.class);
            intent.putExtra(AddEditScenarioActivity.EXTRA_CHARACTER_ID, currentCharacterId);
            startActivity(intent);
        });

        Intent intent = getIntent();
        if (intent.hasExtra("CHARACTER_ID")) {
            setTitle("Edit Character");
            currentCharacterId = intent.getIntExtra("CHARACTER_ID", -1);
            characterViewModel.getCharacterById(currentCharacterId).observe(this, character -> {
                if (character != null) {
                    editingCharacter = character;
                    editTextName.setText(character.getName());
                    editTextPersonality.setText(character.getPersonality());
                    if (character.getDefaultScenario() != null) {
                        editTextDefaultScenario.setText(character.getDefaultScenario());
                    }

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

                    currentVoiceReferenceId = character.getVoiceReferenceId();
                    currentVoiceReferenceName = character.getVoiceReferenceName();
                    conversationCount = character.getConversationCount();

                    isFavorite = character.isFavorite();
                    isHidden = character.isHidden();
                    createdAt = character.getCreatedAt();

                    loadTags(character.getTags());
                    invalidateOptionsMenu();
                }
            });

            layoutScenariosContainer.setVisibility(View.VISIBLE);
            characterViewModel.getScenariosForCharacter(currentCharacterId).observe(this, scenarios -> {
                scenarioAdapter.setScenarios(scenarios);
            });

        } else {
            setTitle("Add Character");
            layoutScenariosContainer.setVisibility(View.GONE);
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

        int color = getTagColor(tag);
        chip.setTextColor(color);
        chip.setChipStrokeColor(ColorStateList.valueOf(color));
        chip.setChipStrokeWidth(dpToPx(1));
        chip.setChipBackgroundColor(ColorStateList.valueOf(Color.TRANSPARENT));
        chip.setCloseIconTint(ColorStateList.valueOf(color));

        chipGroupTags.addView(chip);
    }

    private int getTagColor(String tag) {
        int hash = tag.hashCode();
        float[] hsv = new float[3];
        hsv[0] = Math.abs(hash) % 360;
        hsv[1] = 0.6f + (Math.abs(hash * 7) % 40) / 100f;
        hsv[2] = 0.65f + (Math.abs(hash * 13) % 35) / 100f;
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

    private void importCharacterFromJson(Uri uri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder stringBuilder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line);
            }
            inputStream.close();
            reader.close();

            JSONObject rootJson = new JSONObject(stringBuilder.toString());
            JSONObject charData = rootJson;

            if (rootJson.has("data") && !rootJson.has("name")) {
                charData = rootJson.getJSONObject("data");
            }

            if (charData.has("name")) editTextName.setText(charData.optString("name"));

            String personality = charData.optString("personality");
            if (TextUtils.isEmpty(personality)) personality = charData.optString("description");
            if (TextUtils.isEmpty(personality)) personality = charData.optString("system_prompt");
            if (!TextUtils.isEmpty(personality)) {
                editTextPersonality.setText(personality);
                if (layoutPersonalityContainer.getVisibility() != View.VISIBLE) {
                    layoutPersonalityContainer.setVisibility(View.VISIBLE);
                    imageViewPersonalityArrow.setImageResource(android.R.drawable.arrow_up_float);
                }
            }

            String firstMessage = charData.optString("first_message");
            if (TextUtils.isEmpty(firstMessage)) firstMessage = charData.optString("first_mes");
            if (!TextUtils.isEmpty(firstMessage)) editTextFirstMessage.setText(firstMessage);

            String scenario = charData.optString("default_scenario");
            if (TextUtils.isEmpty(scenario)) scenario = charData.optString("scenario");
            if (!TextUtils.isEmpty(scenario)) {
                editTextDefaultScenario.setText(scenario);
                if (layoutDefaultScenarioContainer.getVisibility() != View.VISIBLE) {
                    layoutDefaultScenarioContainer.setVisibility(View.VISIBLE);
                    imageViewDefaultScenarioArrow.setImageResource(android.R.drawable.arrow_up_float);
                }
            }

            if (charData.has("model")) {
                String model = charData.optString("model");
                editTextModel.setText(model);
                updateModelInfo(model);
            }

            if (charData.has("temperature")) editTextTemperature.setText(String.valueOf(charData.optDouble("temperature")));
            if (charData.has("max_tokens")) editTextMaxTokens.setText(String.valueOf(charData.optInt("max_tokens")));
            if (charData.has("context_limit")) editTextContextLimit.setText(String.valueOf(charData.optInt("context_limit")));

            if (charData.has("tags")) {
                currentTags.clear();
                chipGroupTags.removeAllViews();
                Object tagsObj = charData.get("tags");
                if (tagsObj instanceof JSONArray) {
                    JSONArray tagsArray = (JSONArray) tagsObj;
                    for (int i = 0; i < tagsArray.length(); i++) {
                        String tag = tagsArray.optString(i);
                        if (!tag.isEmpty()) {
                            currentTags.add(tag);
                            addTagChip(tag);
                        }
                    }
                } else if (tagsObj instanceof String) {
                    String tagsStr = (String) tagsObj;
                    String[] tags = tagsStr.split(",");
                    for (String tag : tags) {
                        if (!tag.trim().isEmpty()) {
                            currentTags.add(tag.trim());
                            addTagChip(tag.trim());
                        }
                    }
                }
            }

            Toast.makeText(this, "Character imported successfully", Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to import JSON", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveCharacter() {
        String name = editTextName.getText().toString();
        String personality = editTextPersonality.getText().toString();
        String defaultScenario = editTextDefaultScenario.getText().toString();
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

        StringBuilder tagsBuilder = new StringBuilder();
        for (String t : currentTags) {
            if (tagsBuilder.length() > 0) tagsBuilder.append("|");
            tagsBuilder.append(t);
        }
        String tags = tagsBuilder.toString();

        Character character = new Character(name, personality, firstMessage, model, currentProfileImagePath, currentVoiceReferenceId, currentVoiceReferenceName, temperature, maxTokens, isTimeAware, allowImageInput, contextLimit, tags, defaultScenario);
        character.setFavorite(isFavorite);
        character.setHidden(isHidden);
        character.setConversationCount(conversationCount);

        if (currentCharacterId != -1) {
            character.setId(currentCharacterId);
            character.setCreatedAt(createdAt);
            characterViewModel.update(character);
        } else {
            characterViewModel.insert(character);
        }

        finish();
    }

    private void deleteCharacter() {
        if (editingCharacter != null) {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.delete_confirmation)
                    .setMessage(R.string.are_you_sure_delete)
                    .setPositiveButton(R.string.delete, (dialog, which) -> {
                        characterViewModel.delete(editingCharacter);
                        Toast.makeText(this, "Character deleted", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .setNegativeButton(R.string.cancel, null)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .show();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.add_character_menu, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem favItem = menu.findItem(R.id.action_favorite);
        MenuItem hideItem = menu.findItem(R.id.action_hide);
        MenuItem deleteItem = menu.findItem(R.id.action_delete);

        if (currentCharacterId == -1) {
            favItem.setVisible(false);
            hideItem.setVisible(false);
            deleteItem.setVisible(false);
        } else {
            favItem.setVisible(true);
            hideItem.setVisible(true);
            deleteItem.setVisible(true);

            if (isFavorite) {
                favItem.setIcon(android.R.drawable.btn_star_big_on);
            } else {
                favItem.setIcon(android.R.drawable.btn_star_big_off);
            }

            hideItem.setTitle(isHidden ? "Unhide" : "Hide");
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.save_character) {
            saveCharacter();
            return true;
        } else if (itemId == R.id.action_favorite) {
            isFavorite = !isFavorite;
            invalidateOptionsMenu();
            return true;
        } else if (itemId == R.id.action_hide) {
            isHidden = !isHidden;
            invalidateOptionsMenu();
            Toast.makeText(this, isHidden ? "Will be hidden" : "Will be shown", Toast.LENGTH_SHORT).show();
            return true;
        } else if (itemId == R.id.action_delete) {
            deleteCharacter();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}