package com.example.testing;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import android.view.View;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import com.example.testing.network.ApiClient;
import com.example.testing.network.ApiService;
import com.example.testing.network.response.Model;
import okhttp3.ResponseBody;
import org.json.JSONObject;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import java.text.DecimalFormat;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

public class SettingsActivity extends AppCompatActivity {

    private EditText editTextUsername;
    private EditText editTextApiKey;
    private AutoCompleteTextView editTextPreferredModel;
    private ImageButton buttonRefreshModels;
    private TextView textViewModelInfo;
    private EditText editTextGlobalPrompt;
    private EditText editTextContextLimit; // NEW
    private TextView textViewCredits;
    private Button buttonSave;

    private Button buttonExport;
    private Button buttonImport;

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

        editTextUsername = findViewById(R.id.edit_text_username);
        editTextApiKey = findViewById(R.id.edit_text_api_key);
        textViewCredits = findViewById(R.id.text_view_credits);
        editTextPreferredModel = findViewById(R.id.edit_text_preferred_model);
        buttonRefreshModels = findViewById(R.id.button_refresh_models);
        textViewModelInfo = findViewById(R.id.text_view_model_info);
        editTextGlobalPrompt = findViewById(R.id.edit_text_global_system_prompt);
        editTextContextLimit = findViewById(R.id.edit_text_context_limit); // NEW
        buttonSave = findViewById(R.id.button_save_settings);

        buttonExport = findViewById(R.id.button_export_data);
        buttonImport = findViewById(R.id.button_import_data);

        // Initialize Adapter for Models
        modelsAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, modelIds);
        editTextPreferredModel.setAdapter(modelsAdapter);

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
        editTextPreferredModel.setOnItemClickListener((parent, view, position, id) -> {
            String selectedId = modelsAdapter.getItem(position);
            updateModelInfo(selectedId);
        });

        // Also try to show info if user types exactly a known model ID
        editTextPreferredModel.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                updateModelInfo(s.toString());
            }
        });

        settingsViewModel = new ViewModelProvider(this).get(SettingsViewModel.class);

        settingsViewModel.getUser().observe(this, user -> {
            if (user != null) {
                if (user.getUsername() != null) editTextUsername.setText(user.getUsername());
                if (user.getApiKey() != null) {
                    editTextApiKey.setText(user.getApiKey());
                    if (!user.getApiKey().isEmpty()) {
                        fetchCredits(user.getApiKey());
                    }
                }
                if (user.getPreferredModel() != null) {
                    editTextPreferredModel.setText(user.getPreferredModel());
                    updateModelInfo(user.getPreferredModel());
                }
                if (user.getGlobalSystemPrompt() != null) editTextGlobalPrompt.setText(user.getGlobalSystemPrompt());

                // Load Context Limit
                editTextContextLimit.setText(String.valueOf(user.getDefaultContextLimit()));
            }
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

        settingsViewModel.saveSettings(username, apiKey, preferredModel, globalPrompt, contextLimit);
        Toast.makeText(this, "Settings saved!", Toast.LENGTH_SHORT).show();
        fetchCredits(apiKey);
        finish();
    }
}