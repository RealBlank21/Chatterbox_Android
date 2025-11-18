package com.example.testing;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.view.View;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import com.example.testing.network.ApiClient;
import com.example.testing.network.ApiService;
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
    private EditText editTextGlobalPrompt;
    private TextView textViewCredits;
    private Button buttonSave;

    private Button buttonExport;
    private Button buttonImport;

    private SettingsViewModel settingsViewModel;

    // --- Activity Result Launchers ---
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
    // ---------------------------------

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        setTitle("Settings");

        editTextUsername = findViewById(R.id.edit_text_username);
        editTextApiKey = findViewById(R.id.edit_text_api_key);
        textViewCredits = findViewById(R.id.text_view_credits);
        editTextPreferredModel = findViewById(R.id.edit_text_preferred_model);
        editTextGlobalPrompt = findViewById(R.id.edit_text_global_system_prompt);
        buttonSave = findViewById(R.id.button_save_settings);

        buttonExport = findViewById(R.id.button_export_data);
        buttonImport = findViewById(R.id.button_import_data);

        String[] models = getResources().getStringArray(R.array.ai_model_suggestions);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, models);
        editTextPreferredModel.setAdapter(adapter);

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
                if (user.getPreferredModel() != null) editTextPreferredModel.setText(user.getPreferredModel());
                if (user.getGlobalSystemPrompt() != null) editTextGlobalPrompt.setText(user.getGlobalSystemPrompt());
            }
        });

        buttonSave.setOnClickListener(v -> saveSettings());

        // --- Set Listeners for Export/Import with DEBUG TOASTS ---
        buttonExport.setOnClickListener(v -> {
            Toast.makeText(SettingsActivity.this, "Debug: Export Clicked", Toast.LENGTH_SHORT).show();

            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            String fileName = "chatterbox_backup_" + timeStamp + ".json";
            createDocumentLauncher.launch(fileName);
        });

        buttonImport.setOnClickListener(v -> {
            Toast.makeText(SettingsActivity.this, "Debug: Import Clicked", Toast.LENGTH_SHORT).show();

            openDocumentLauncher.launch(new String[]{"application/json"});
        });
        // ---------------------------------------
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

        if (apiKey.isEmpty()) {
            Toast.makeText(this, "API Key cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }

        settingsViewModel.saveSettings(username, apiKey, preferredModel, globalPrompt);
        Toast.makeText(this, "Settings saved!", Toast.LENGTH_SHORT).show();
        fetchCredits(apiKey);
        finish();
    }
}