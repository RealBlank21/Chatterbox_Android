package com.example.testing;

import android.graphics.Color;
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
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.lifecycle.ViewModelProvider;

import com.example.testing.network.ApiClient;
import com.example.testing.network.ApiService;
import com.example.testing.network.response.Model;

import org.json.JSONObject;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SettingsActivity extends BaseActivity {

    // Inputs
    private EditText editTextUsername;
    private EditText editTextApiKey;
    private AutoCompleteTextView editTextPreferredModel;
    private ImageButton buttonRefreshModels;
    private TextView textViewModelInfo;
    private EditText editTextGlobalPrompt;
    private EditText editTextContextLimit;
    private TextView textViewCredits;
    private Button buttonSave;
    private Button buttonExport;
    private Button buttonImport;
    private View saveContainer;

    // Persona Inputs
    private Spinner spinnerPersona;
    private Button buttonAddPersona;
    private Button buttonDeletePersona;
    private EditText editTextPersonaName;
    private EditText editTextPersonaDescription;
    private List<Persona> personaList = new ArrayList<>();
    private PersonaAdapter personaAdapter;
    private Persona selectedPersona = null;

    // State tracking for Persona
    private int activePersonaId = -1;
    private boolean initialPersonaLoaded = false;

    // Tabs & Layouts
    private View tabContainer;
    private Button tabPersona, tabLlm, tabAppearance, tabBackup;
    private View layoutPersona, layoutLlm, layoutAppearance, layoutBackup;

    // Appearance Inputs
    private RadioGroup radioGroupListMode;
    private RadioButton radioModeList, radioModeCard;
    private CardView previewPrimary, previewSecondary;
    private EditText hexPrimary, hexSecondary;
    private SeekBar seekPrimaryR, seekPrimaryG, seekPrimaryB;
    private SeekBar seekSecondaryR, seekSecondaryG, seekSecondaryB;
    private int currentColorPrimary = Color.BLACK;
    private int currentColorSecondary = Color.BLACK;

    private SettingsViewModel settingsViewModel;
    private ArrayAdapter<String> modelsAdapter;
    private List<String> modelIds = new ArrayList<>();

    private final ActivityResultLauncher<String> createDocumentLauncher =
            registerForActivityResult(new ActivityResultContracts.CreateDocument("application/json"), uri -> {
                if (uri != null) {
                    settingsViewModel.exportBackup(uri, getContentResolver());
                } else {
                    Toast.makeText(this, "Export cancelled", Toast.LENGTH_SHORT).show();
                }
            });

    private final ActivityResultLauncher<String[]> openDocumentLauncher =
            registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri -> {
                if (uri != null) {
                    settingsViewModel.importBackup(uri, getContentResolver());
                } else {
                    Toast.makeText(this, "Import cancelled", Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        setTitle("Settings");

        initViews();
        initTabs();
        setupColorPickers();
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

        // Persona Views
        spinnerPersona = findViewById(R.id.spinner_persona);
        buttonAddPersona = findViewById(R.id.button_add_persona);
        buttonDeletePersona = findViewById(R.id.button_delete_persona);
        editTextPersonaName = findViewById(R.id.edit_text_persona_name);
        editTextPersonaDescription = findViewById(R.id.edit_text_persona_description);

        buttonSave = findViewById(R.id.button_save_settings);
        buttonExport = findViewById(R.id.button_export_data);
        buttonImport = findViewById(R.id.button_import_data);

        radioGroupListMode = findViewById(R.id.radio_group_list_mode);
        radioModeList = findViewById(R.id.radio_mode_list);
        radioModeCard = findViewById(R.id.radio_mode_card);

        previewPrimary = findViewById(R.id.card_preview_primary);
        previewSecondary = findViewById(R.id.card_preview_secondary);
        hexPrimary = findViewById(R.id.edit_text_hex_primary);
        hexSecondary = findViewById(R.id.edit_text_hex_secondary);
        seekPrimaryR = findViewById(R.id.seekbar_primary_r);
        seekPrimaryG = findViewById(R.id.seekbar_primary_g);
        seekPrimaryB = findViewById(R.id.seekbar_primary_b);
        seekSecondaryR = findViewById(R.id.seekbar_secondary_r);
        seekSecondaryG = findViewById(R.id.seekbar_secondary_g);
        seekSecondaryB = findViewById(R.id.seekbar_secondary_b);
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

    private void setupColorPickers() {
        SeekBar.OnSeekBarChangeListener primaryListener = new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) { updatePrimaryColor(); }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        };
        seekPrimaryR.setOnSeekBarChangeListener(primaryListener);
        seekPrimaryG.setOnSeekBarChangeListener(primaryListener);
        seekPrimaryB.setOnSeekBarChangeListener(primaryListener);

        SeekBar.OnSeekBarChangeListener secondaryListener = new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) { updateSecondaryColor(); }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        };
        seekSecondaryR.setOnSeekBarChangeListener(secondaryListener);
        seekSecondaryG.setOnSeekBarChangeListener(secondaryListener);
        seekSecondaryB.setOnSeekBarChangeListener(secondaryListener);
    }

    private void updatePrimaryColor() {
        int r = seekPrimaryR.getProgress();
        int g = seekPrimaryG.getProgress();
        int b = seekPrimaryB.getProgress();
        currentColorPrimary = Color.rgb(r, g, b);
        previewPrimary.setCardBackgroundColor(currentColorPrimary);
        hexPrimary.setText(String.format("#%02X%02X%02X", r, g, b));
    }

    private void updateSecondaryColor() {
        int r = seekSecondaryR.getProgress();
        int g = seekSecondaryG.getProgress();
        int b = seekSecondaryB.getProgress();
        currentColorSecondary = Color.rgb(r, g, b);
        previewSecondary.setCardBackgroundColor(currentColorSecondary);
        hexSecondary.setText(String.format("#%02X%02X%02X", r, g, b));
    }

    private void setSeekBarsFromColor(int color, SeekBar r, SeekBar g, SeekBar b) {
        r.setProgress(Color.red(color));
        g.setProgress(Color.green(color));
        b.setProgress(Color.blue(color));
    }

    private void setupLogic() {
        settingsViewModel = new ViewModelProvider(this).get(SettingsViewModel.class);

        // --- Persona Logic ---
        personaAdapter = new PersonaAdapter(this, android.R.layout.simple_spinner_item, personaList);
        personaAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerPersona.setAdapter(personaAdapter);

        // Observer for Personas List
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

        // Observer for User Config (Active Persona ID)
        settingsViewModel.getUser().observe(this, user -> {
            if (user != null) {
                if (user.getUsername() != null) editTextUsername.setText(user.getUsername());
                if (user.getApiKey() != null) {
                    editTextApiKey.setText(user.getApiKey());
                    if (!user.getApiKey().isEmpty()) fetchCredits(user.getApiKey());
                }
                if (user.getPreferredModel() != null) {
                    editTextPreferredModel.setText(user.getPreferredModel());
                    updateModelInfo(user.getPreferredModel());
                }
                if (user.getGlobalSystemPrompt() != null) editTextGlobalPrompt.setText(user.getGlobalSystemPrompt());
                editTextContextLimit.setText(String.valueOf(user.getDefaultContextLimit()));

                int p = user.getThemeColorPrimary();
                int s = user.getThemeColorSecondary();

                if (p == 0) p = ThemeUtils.getPrimaryColor(this);
                if (s == 0) s = ThemeUtils.getSecondaryColor(this);

                currentColorPrimary = p;
                currentColorSecondary = s;
                setSeekBarsFromColor(p, seekPrimaryR, seekPrimaryG, seekPrimaryB);
                setSeekBarsFromColor(s, seekSecondaryR, seekSecondaryG, seekSecondaryB);
                updatePrimaryColor();
                updateSecondaryColor();

                if ("card".equals(user.getCharacterListMode())) {
                    radioModeCard.setChecked(true);
                } else {
                    radioModeList.setChecked(true);
                }

                // Update Active Persona ID
                activePersonaId = user.getCurrentPersonaId();
                personaAdapter.notifyDataSetChanged(); // To update the stars
                syncPersonaSelection();
            }
        });

        spinnerPersona.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedPersona = personaList.get(position);
                updatePersonaInputs(selectedPersona);
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
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

        editTextPreferredModel.setOnItemClickListener((parent, view, position, id) -> {
            updateModelInfo(modelsAdapter.getItem(position));
        });

        editTextPreferredModel.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) { updateModelInfo(s.toString()); }
        });

        buttonSave.setOnClickListener(v -> saveSettings());

        buttonExport.setOnClickListener(v -> {
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            String fileName = "chatterbox_backup_" + timeStamp + ".json";
            createDocumentLauncher.launch(fileName);
        });

        buttonImport.setOnClickListener(v -> {
            openDocumentLauncher.launch(new String[]{"application/json"});
        });
    }

    /**
     * Synchronizes the Spinner selection with the activePersonaId.
     * Only auto-selects if we haven't successfully loaded the initial state yet,
     * to prevent jumping around if the user is editing.
     */
    private void syncPersonaSelection() {
        if (personaList.isEmpty() || initialPersonaLoaded) return;

        // If activePersonaId is -1 (default not set), we might default to the first one
        // or wait. If the list is populated, we should probably just pick the first one
        // if activeId is invalid, effectively making it the candidate for "active".

        int indexToSelect = 0; // Default to first
        boolean found = false;

        if (activePersonaId != -1) {
            for (int i = 0; i < personaList.size(); i++) {
                if (personaList.get(i).getId() == activePersonaId) {
                    indexToSelect = i;
                    found = true;
                    break;
                }
            }
        }

        // If we found the active one, or just defaulting to 0, set it.
        // We consider initial load done if we have data.
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

    private void fetchCredits(String apiKey) {
        textViewCredits.setVisibility(View.VISIBLE);
        textViewCredits.setText("Loading credits...");

        ApiService apiService = ApiClient.getClient().create(ApiService.class);
        apiService.getCredits("Bearer " + apiKey).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        String jsonString = response.body().string();
                        JSONObject jsonObject = new JSONObject(jsonString);
                        JSONObject data = jsonObject.optJSONObject("data");

                        if (data != null) {
                            double totalCredits = data.optDouble("total_credits", 0.0);
                            double totalUsage = data.optDouble("total_usage", 0.0);
                            double remainingCredits = totalCredits - totalUsage;

                            DecimalFormat df = new DecimalFormat("#,##0.00");
                            String formattedCredits = df.format(remainingCredits);

                            runOnUiThread(() ->
                                    textViewCredits.setText("Remaining Credits: $" + formattedCredits)
                            );
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        runOnUiThread(() -> textViewCredits.setText("Error reading credits"));
                    }
                } else {
                    runOnUiThread(() -> textViewCredits.setText("Failed to load credits"));
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                runOnUiThread(() -> textViewCredits.setText("Network error"));
            }
        });
    }

    private void saveSettings() {
        String username = editTextUsername.getText().toString().trim();
        String apiKey = editTextApiKey.getText().toString().trim();
        String preferredModel = editTextPreferredModel.getText().toString().trim();
        String globalPrompt = editTextGlobalPrompt.getText().toString().trim();

        int contextLimit = 0;
        try {
            contextLimit = Integer.parseInt(editTextContextLimit.getText().toString().trim());
        } catch (NumberFormatException e) {
            contextLimit = 0;
        }

        if (apiKey.isEmpty()) {
            Toast.makeText(this, "API Key cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }

        // Save Persona Changes
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

        // Get view mode
        String viewMode = radioModeCard.isChecked() ? "card" : "list";

        // Update active persona ID to the one currently selected in the spinner
        int newActivePersonaId = selectedPersona != null ? selectedPersona.getId() : activePersonaId;

        // Save immediately to Prefs
        ThemeUtils.saveColors(this, currentColorPrimary, currentColorSecondary);

        // Save to DB
        settingsViewModel.saveSettings(username, apiKey, preferredModel, globalPrompt, contextLimit, currentColorPrimary, currentColorSecondary, viewMode, newActivePersonaId);
        Toast.makeText(this, "Settings saved! Restarting...", Toast.LENGTH_SHORT).show();

        // Restart Activity to apply changes
        finish();
        startActivity(getIntent());
    }

    // --- Custom Adapter for Persona Spinner ---
    private class PersonaAdapter extends ArrayAdapter<Persona> {

        public PersonaAdapter(@NonNull android.content.Context context, int resource, @NonNull List<Persona> objects) {
            super(context, resource, objects);
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            View view = super.getView(position, convertView, parent);
            enhanceView(view, getItem(position));
            return view;
        }

        @Override
        public View getDropDownView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            View view = super.getDropDownView(position, convertView, parent);
            enhanceView(view, getItem(position));
            return view;
        }

        private void enhanceView(View view, Persona persona) {
            if (view instanceof TextView && persona != null) {
                TextView textView = (TextView) view;
                String text = persona.getName();

                // Add star if this is the active persona
                if (persona.getId() == activePersonaId) {
                    text += " ★";
                }
                textView.setText(text);
            }
        }
    }
}