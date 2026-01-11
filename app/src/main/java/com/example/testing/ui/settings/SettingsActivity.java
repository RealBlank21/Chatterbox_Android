package com.example.testing.ui.settings;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.cardview.widget.CardView;
import androidx.lifecycle.ViewModelProvider;

import com.github.antonpopoff.colorwheel.ColorWheel;
import com.github.antonpopoff.colorwheel.gradientseekbar.GradientSeekBar;

import com.example.testing.ui.base.BaseActivity;
import com.example.testing.data.repository.ModelRepository;
import com.example.testing.data.local.entity.Persona;
import com.example.testing.R;
import com.example.testing.ui.base.ThemeUtils;
import com.example.testing.data.remote.response.Model;
import com.example.testing.utils.CreditsManager;
import com.example.testing.utils.SettingsAppearanceHelper;
import com.example.testing.utils.SettingsBackupHelper;

import java.util.ArrayList;
import java.util.List;

public class SettingsActivity extends BaseActivity {

    // Inputs
    private EditText editTextUsername;
    private EditText editTextApiKey;
    private AutoCompleteTextView editTextPreferredModel;
    private ImageButton buttonRefreshModels;
    private TextView textViewModelInfo;
    private EditText editTextGlobalPrompt;
    private EditText editTextContextLimit;
    private EditText editTextTemperature;
    private EditText editTextTopP;
    private EditText editTextTopK;
    private EditText editTextFreqPenalty;
    private EditText editTextPresPenalty;
    private EditText editTextRepPenalty;
    private TextView textViewCredits;
    private Button buttonSave;

    // Backup Buttons
    private Button buttonExport, buttonImport;
    private Button buttonExportCharacters, buttonImportCharacters;
    private Button buttonClearData;

    private View saveContainer;

    // Persona Inputs
    private Spinner spinnerPersona;
    private Button buttonAddPersona, buttonDeletePersona;
    private EditText editTextPersonaName, editTextPersonaDescription;
    private List<Persona> personaList = new ArrayList<>();
    private PersonaAdapter personaAdapter;
    private Persona selectedPersona = null;
    private int activePersonaId = -1;
    private boolean initialPersonaLoaded = false;

    // Tabs
    private View tabContainer;
    private Button tabPersona, tabLlm, tabAppearance, tabBackup;
    private View layoutPersona, layoutLlm, layoutAppearance, layoutBackup;

    // Appearance
    private RadioGroup radioGroupListMode;
    private RadioButton radioModeList, radioModeCard;

    // Helpers
    private SettingsViewModel settingsViewModel;
    private SettingsAppearanceHelper appearanceHelper;
    private SettingsBackupHelper backupHelper;

    private ArrayAdapter<String> modelsAdapter;
    private List<String> modelIds = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        setTitle("Settings");
        settingsViewModel = new ViewModelProvider(this).get(SettingsViewModel.class);

        initViews();
        initHelpers();
        initTabs();
        setupLogic();
        applyDynamicUiColors();
    }

    private void applyDynamicUiColors() {
        int primary = ThemeUtils.getPrimaryColor(this);
        int secondary = ThemeUtils.getSecondaryColor(this);
        if (tabContainer != null) tabContainer.setBackgroundColor(primary);
        if (textViewCredits != null) textViewCredits.setTextColor(secondary);
    }

    private void initViews() {
        tabContainer = findViewById(R.id.layout_tab_container);
        tabPersona = findViewById(R.id.tab_persona);
        tabLlm = findViewById(R.id.tab_llm);
        tabAppearance = findViewById(R.id.tab_appearance);
        tabBackup = findViewById(R.id.tab_backup);

        layoutPersona = findViewById(R.id.layout_persona);
        layoutLlm = findViewById(R.id.layout_llm);
        layoutAppearance = findViewById(R.id.layout_appearance);
        layoutBackup = findViewById(R.id.layout_backup);
        saveContainer = findViewById(R.id.layout_save_container);

        editTextUsername = findViewById(R.id.edit_text_username);
        editTextApiKey = findViewById(R.id.edit_text_api_key);
        textViewCredits = findViewById(R.id.text_view_credits);
        editTextPreferredModel = findViewById(R.id.edit_text_preferred_model);
        buttonRefreshModels = findViewById(R.id.button_refresh_models);
        textViewModelInfo = findViewById(R.id.text_view_model_info);
        editTextGlobalPrompt = findViewById(R.id.edit_text_global_system_prompt);
        editTextContextLimit = findViewById(R.id.edit_text_context_limit);
        editTextTemperature = findViewById(R.id.edit_text_temperature);
        editTextTopP = findViewById(R.id.edit_text_top_p);
        editTextTopK = findViewById(R.id.edit_text_top_k);
        editTextFreqPenalty = findViewById(R.id.edit_text_freq_penalty);
        editTextPresPenalty = findViewById(R.id.edit_text_pres_penalty);
        editTextRepPenalty = findViewById(R.id.edit_text_rep_penalty);

        spinnerPersona = findViewById(R.id.spinner_persona);
        buttonAddPersona = findViewById(R.id.button_add_persona);
        buttonDeletePersona = findViewById(R.id.button_delete_persona);
        editTextPersonaName = findViewById(R.id.edit_text_persona_name);
        editTextPersonaDescription = findViewById(R.id.edit_text_persona_description);

        buttonSave = findViewById(R.id.button_save_settings);

        buttonExport = findViewById(R.id.button_export_data);
        buttonImport = findViewById(R.id.button_import_data);
        buttonExportCharacters = findViewById(R.id.button_export_characters);
        buttonImportCharacters = findViewById(R.id.button_import_characters);
        buttonClearData = findViewById(R.id.button_clear_data);

        radioGroupListMode = findViewById(R.id.radio_group_list_mode);
        radioModeList = findViewById(R.id.radio_mode_list);
        radioModeCard = findViewById(R.id.radio_mode_card);
    }

    private void initHelpers() {
        backupHelper = new SettingsBackupHelper(this, settingsViewModel);

        appearanceHelper = new SettingsAppearanceHelper(
                findViewById(R.id.color_wheel),
                findViewById(R.id.gradient_seek_bar),
                new CardView[]{findViewById(R.id.card_preview_primary), findViewById(R.id.card_preview_secondary)},
                new LinearLayout[]{findViewById(R.id.container_preview_primary), findViewById(R.id.container_preview_secondary)}
        );
    }

    private void initTabs() {
        tabPersona.setOnClickListener(v -> showTab("persona"));
        tabLlm.setOnClickListener(v -> showTab("llm"));
        tabAppearance.setOnClickListener(v -> showTab("appearance"));
        tabBackup.setOnClickListener(v -> showTab("backup"));
        showTab("persona");
    }

    private void showTab(String tabName) {
        layoutPersona.setVisibility(View.GONE);
        layoutLlm.setVisibility(View.GONE);
        layoutAppearance.setVisibility(View.GONE);
        layoutBackup.setVisibility(View.GONE);

        float inactiveAlpha = 0.5f;
        float activeAlpha = 1.0f;

        tabPersona.setAlpha(inactiveAlpha);
        tabLlm.setAlpha(inactiveAlpha);
        tabAppearance.setAlpha(inactiveAlpha);
        tabBackup.setAlpha(inactiveAlpha);

        switch (tabName) {
            case "persona":
                layoutPersona.setVisibility(View.VISIBLE);
                tabPersona.setAlpha(activeAlpha);
                saveContainer.setVisibility(View.VISIBLE);
                break;
            case "llm":
                layoutLlm.setVisibility(View.VISIBLE);
                tabLlm.setAlpha(activeAlpha);
                saveContainer.setVisibility(View.VISIBLE);
                break;
            case "appearance":
                layoutAppearance.setVisibility(View.VISIBLE);
                tabAppearance.setAlpha(activeAlpha);
                saveContainer.setVisibility(View.VISIBLE);
                break;
            case "backup":
                layoutBackup.setVisibility(View.VISIBLE);
                tabBackup.setAlpha(activeAlpha);
                saveContainer.setVisibility(View.GONE);
                break;
        }
    }

    private void setupLogic() {
        // --- Persona Logic ---
        personaAdapter = new PersonaAdapter(this, android.R.layout.simple_spinner_item, personaList);
        personaAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerPersona.setAdapter(personaAdapter);

        settingsViewModel.getAllPersonas().observe(this, personas -> {
            personaList.clear();
            if (personas != null && !personas.isEmpty()) {
                personaList.addAll(personas);
            } else {
                settingsViewModel.addPersona("Default User", "I am a user of this app.");
                return;
            }
            personaAdapter.notifyDataSetChanged();
            syncPersonaSelection();
        });

        settingsViewModel.getUser().observe(this, user -> {
            if (user != null) {
                if (user.getUsername() != null) editTextUsername.setText(user.getUsername());
                if (user.getApiKey() != null) {
                    editTextApiKey.setText(user.getApiKey());
                    if (!user.getApiKey().isEmpty()) CreditsManager.fetchCredits(user.getApiKey(), textViewCredits);
                }
                if (user.getPreferredModel() != null) {
                    editTextPreferredModel.setText(user.getPreferredModel());
                    updateModelInfo(user.getPreferredModel());
                }
                if (user.getGlobalSystemPrompt() != null) editTextGlobalPrompt.setText(user.getGlobalSystemPrompt());
                editTextContextLimit.setText(String.valueOf(user.getDefaultContextLimit()));

                editTextTemperature.setText(String.valueOf(user.getDefaultTemperature()));
                editTextTopP.setText(String.valueOf(user.getDefaultTopP()));
                editTextTopK.setText(String.valueOf(user.getDefaultTopK()));
                editTextFreqPenalty.setText(String.valueOf(user.getDefaultFrequencyPenalty()));
                editTextPresPenalty.setText(String.valueOf(user.getDefaultPresencePenalty()));
                editTextRepPenalty.setText(String.valueOf(user.getDefaultRepetitionPenalty()));

                // Colors
                int p = user.getThemeColorPrimary() != 0 ? user.getThemeColorPrimary() : ThemeUtils.getPrimaryColor(this);
                int s = user.getThemeColorSecondary() != 0 ? user.getThemeColorSecondary() : ThemeUtils.getSecondaryColor(this);
                appearanceHelper.setColors(p, s);

                // View Mode
                if ("card".equals(user.getCharacterListMode())) radioModeCard.setChecked(true);
                else radioModeList.setChecked(true);

                activePersonaId = user.getCurrentPersonaId();
                personaAdapter.notifyDataSetChanged();
                syncPersonaSelection();
            }
        });

        spinnerPersona.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedPersona = personaList.get(position);
                updatePersonaInputs(selectedPersona);
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        buttonAddPersona.setOnClickListener(v -> {
            settingsViewModel.addPersona("New Persona", "");
            Toast.makeText(this, "Created new persona", Toast.LENGTH_SHORT).show();
        });

        buttonDeletePersona.setOnClickListener(v -> {
            if (selectedPersona != null) {
                if (personaList.size() <= 1) {
                    Toast.makeText(this, "Cannot delete the last persona", Toast.LENGTH_SHORT).show();
                    return;
                }
                settingsViewModel.deletePersona(selectedPersona);
                Toast.makeText(this, "Persona deleted", Toast.LENGTH_SHORT).show();
            }
        });

        // --- LLM Logic ---
        modelsAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, modelIds);
        editTextPreferredModel.setAdapter(modelsAdapter);

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

        editTextPreferredModel.setOnItemClickListener((parent, view, position, id) -> updateModelInfo(modelsAdapter.getItem(position)));
        editTextPreferredModel.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) { updateModelInfo(s.toString()); }
        });

        buttonSave.setOnClickListener(v -> saveSettings());

        // --- Backup Logic Delegates ---
        buttonExport.setOnClickListener(v -> backupHelper.launchFullBackupExport());
        buttonImport.setOnClickListener(v -> backupHelper.launchFullBackupImport());
        buttonExportCharacters.setOnClickListener(v -> backupHelper.launchCharacterExport());
        buttonImportCharacters.setOnClickListener(v -> backupHelper.launchCharacterImport());

        buttonClearData.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("Clear All Data")
                    .setMessage("Are you sure you want to delete all characters, messages, and personas? This action cannot be undone.")
                    .setPositiveButton("Clear Data", (dialog, which) -> settingsViewModel.clearAllData())
                    .setNegativeButton("Cancel", null)
                    .show();
        });
    }

    private void syncPersonaSelection() {
        if (personaList.isEmpty() || initialPersonaLoaded) return;
        int indexToSelect = 0;
        if (activePersonaId != -1) {
            for (int i = 0; i < personaList.size(); i++) {
                if (personaList.get(i).getId() == activePersonaId) {
                    indexToSelect = i;
                    break;
                }
            }
        }
        spinnerPersona.setSelection(indexToSelect);
        selectedPersona = personaList.get(indexToSelect);
        updatePersonaInputs(selectedPersona);
        initialPersonaLoaded = true;
    }

    private void updatePersonaInputs(Persona persona) {
        if (persona != null) {
            editTextPersonaName.setText(persona.getName());
            editTextPersonaDescription.setText(persona.getDescription());
        } else {
            editTextPersonaName.setText("");
            editTextPersonaDescription.setText("");
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

    private void saveSettings() {
        String username = editTextUsername.getText().toString().trim();
        String apiKey = editTextApiKey.getText().toString().trim();
        String preferredModel = editTextPreferredModel.getText().toString().trim();
        String globalPrompt = editTextGlobalPrompt.getText().toString().trim();

        float temp = 1.0f;
        float topP = 1.0f;
        int topK = 0;
        float freqPen = 0.0f;
        float presPen = 0.0f;
        float repPen = 1.0f;

        int contextLimit = 0;
        try {
            if (!editTextTemperature.getText().toString().isEmpty()) temp = Float.parseFloat(editTextTemperature.getText().toString());
            if (!editTextTopP.getText().toString().isEmpty()) topP = Float.parseFloat(editTextTopP.getText().toString());
            if (!editTextTopK.getText().toString().isEmpty()) topK = Integer.parseInt(editTextTopK.getText().toString());
            if (!editTextFreqPenalty.getText().toString().isEmpty()) freqPen = Float.parseFloat(editTextFreqPenalty.getText().toString());
            if (!editTextPresPenalty.getText().toString().isEmpty()) presPen = Float.parseFloat(editTextPresPenalty.getText().toString());
            if (!editTextRepPenalty.getText().toString().isEmpty()) repPen = Float.parseFloat(editTextRepPenalty.getText().toString());

            contextLimit = Integer.parseInt(editTextContextLimit.getText().toString().trim());
        } catch (NumberFormatException e) {
            contextLimit = 0;
        }

        if (apiKey.isEmpty()) {
            Toast.makeText(this, "API Key cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedPersona != null) {
            String pName = editTextPersonaName.getText().toString().trim();
            String pDesc = editTextPersonaDescription.getText().toString().trim();
            if (pName.isEmpty()) {
                Toast.makeText(this, "Persona name cannot be empty", Toast.LENGTH_SHORT).show();
                return;
            }
            selectedPersona.setName(pName);
            selectedPersona.setDescription(pDesc);
            settingsViewModel.updatePersona(selectedPersona);
        }

        String viewMode = radioModeCard.isChecked() ? "card" : "list";
        int newActivePersonaId = selectedPersona != null ? selectedPersona.getId() : activePersonaId;
        int pColor = appearanceHelper.getPrimaryColor();
        int sColor = appearanceHelper.getSecondaryColor();

        ThemeUtils.saveColors(this, pColor, sColor);
        settingsViewModel.saveSettings(username, apiKey, preferredModel, globalPrompt, contextLimit, pColor, sColor, viewMode, newActivePersonaId,
                temp, topP, topK, freqPen, presPen, repPen);
        Toast.makeText(this, "Settings saved! Restarting...", Toast.LENGTH_SHORT).show();

        finish();
        startActivity(getIntent());
    }

    private class PersonaAdapter extends ArrayAdapter<Persona> {
        public PersonaAdapter(@NonNull android.content.Context context, int resource, @NonNull List<Persona> objects) {
            super(context, resource, objects);
        }

        @NonNull @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            View view = super.getView(position, convertView, parent);
            enhanceView(view, getItem(position));
            return view;
        }

        @Override public View getDropDownView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            View view = super.getDropDownView(position, convertView, parent);
            enhanceView(view, getItem(position));
            return view;
        }

        private void enhanceView(View view, Persona persona) {
            if (view instanceof TextView && persona != null) {
                TextView textView = (TextView) view;
                String text = persona.getName();
                if (persona.getId() == activePersonaId) text += " ★";
                textView.setText(text);
            }
        }
    }
}