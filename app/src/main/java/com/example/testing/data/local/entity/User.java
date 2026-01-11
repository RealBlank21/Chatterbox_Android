package com.example.testing.data.local.entity;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;
import androidx.room.ColumnInfo;
import java.io.Serializable;

@Entity(tableName = "user_config")
public class User implements Serializable {

    private static final long serialVersionUID = 6L;

    @PrimaryKey
    @ColumnInfo(name = "config_id")
    private int configId = 1;

    @ColumnInfo(name = "username")
    private String username = "";

    @ColumnInfo(name = "email")
    private String email = "";

    @ColumnInfo(name = "profile_image_path")
    private String profileImagePath = "";

    @ColumnInfo(name = "api_key")
    private String apiKey = "";

    @ColumnInfo(name = "preferred_model")
    private String preferredModel = "";

    @ColumnInfo(name = "global_system_prompt")
    private String globalSystemPrompt = "";

    @ColumnInfo(name = "default_context_limit", defaultValue = "0")
    private int defaultContextLimit = 0;

    @ColumnInfo(name = "default_temperature", defaultValue = "1.0")
    private float defaultTemperature = 1.0f;

    @ColumnInfo(name = "default_top_p", defaultValue = "1.0")
    private float defaultTopP = 1.0f;

    @ColumnInfo(name = "default_top_k", defaultValue = "0")
    private int defaultTopK = 0;

    @ColumnInfo(name = "default_frequency_penalty", defaultValue = "0.0")
    private float defaultFrequencyPenalty = 0.0f;

    @ColumnInfo(name = "default_presence_penalty", defaultValue = "0.0")
    private float defaultPresencePenalty = 0.0f;

    @ColumnInfo(name = "default_repetition_penalty", defaultValue = "1.0")
    private float defaultRepetitionPenalty = 1.0f;

    @ColumnInfo(name = "theme_color_primary", defaultValue = "0")
    private int themeColorPrimary = 0;

    @ColumnInfo(name = "theme_color_secondary", defaultValue = "0")
    private int themeColorSecondary = 0;

    @ColumnInfo(name = "character_list_mode", defaultValue = "list")
    private String characterListMode = "list";

    @ColumnInfo(name = "current_persona_id", defaultValue = "-1")
    private int currentPersonaId = -1;

    @Ignore
    public User(String username, String email, String profileImagePath,
                String apiKey, String preferredModel, String globalSystemPrompt) {
        this(username, email, profileImagePath, apiKey, preferredModel, globalSystemPrompt, 0, 0, 0, "list", -1);
    }

    @Ignore
    public User(String username, String email, String profileImagePath,
                String apiKey, String preferredModel, String globalSystemPrompt, int defaultContextLimit) {
        this(username, email, profileImagePath, apiKey, preferredModel, globalSystemPrompt, defaultContextLimit, 0, 0, "list", -1);
    }

    @Ignore
    public User(String username, String email, String profileImagePath,
                String apiKey, String preferredModel, String globalSystemPrompt,
                int defaultContextLimit, int themeColorPrimary, int themeColorSecondary) {
        this(username, email, profileImagePath, apiKey, preferredModel, globalSystemPrompt, defaultContextLimit, themeColorPrimary, themeColorSecondary, "list", -1);
    }

    @Ignore
    public User(String username, String email, String profileImagePath,
                String apiKey, String preferredModel, String globalSystemPrompt,
                int defaultContextLimit, int themeColorPrimary, int themeColorSecondary, String characterListMode) {
        this(username, email, profileImagePath, apiKey, preferredModel, globalSystemPrompt, defaultContextLimit, themeColorPrimary, themeColorSecondary, characterListMode, -1);
    }

    @Ignore
    public User(String username, String email, String profileImagePath,
                String apiKey, String preferredModel, String globalSystemPrompt,
                int defaultContextLimit, int themeColorPrimary, int themeColorSecondary, String characterListMode, int currentPersonaId) {
        this.username = username;
        this.email = email;
        this.profileImagePath = profileImagePath != null ? profileImagePath : "";
        this.apiKey = apiKey != null ? apiKey : "";
        this.preferredModel = preferredModel != null ? preferredModel : "";
        this.globalSystemPrompt = globalSystemPrompt != null ? globalSystemPrompt : "";
        this.defaultContextLimit = defaultContextLimit;
        this.themeColorPrimary = themeColorPrimary;
        this.themeColorSecondary = themeColorSecondary;
        this.characterListMode = characterListMode != null ? characterListMode : "list";
        this.currentPersonaId = currentPersonaId;
    }

    public User() {
    }

    // --- Getters ---
    public int getConfigId() { return configId; }
    public String getUsername() { return username; }
    public String getEmail() { return email; }
    public String getProfileImagePath() { return profileImagePath; }
    public String getApiKey() { return apiKey; }
    public String getPreferredModel() { return preferredModel; }
    public String getGlobalSystemPrompt() { return globalSystemPrompt; }
    public int getDefaultContextLimit() { return defaultContextLimit; }
    public int getThemeColorPrimary() { return themeColorPrimary; }
    public int getThemeColorSecondary() { return themeColorSecondary; }
    public String getCharacterListMode() { return characterListMode; }
    public int getCurrentPersonaId() { return currentPersonaId; }

    // --- Setters ---
    public void setConfigId(int configId) { this.configId = configId; }
    public void setUsername(String username) { this.username = username; }
    public void setEmail(String email) { this.email = email; }
    public void setProfileImagePath(String profileImagePath) { this.profileImagePath = profileImagePath; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }
    public void setPreferredModel(String preferredModel) { this.preferredModel = preferredModel; }
    public void setGlobalSystemPrompt(String globalSystemPrompt) { this.globalSystemPrompt = globalSystemPrompt; }
    public void setDefaultContextLimit(int defaultContextLimit) { this.defaultContextLimit = defaultContextLimit; }
    public void setThemeColorPrimary(int themeColorPrimary) { this.themeColorPrimary = themeColorPrimary; }
    public void setThemeColorSecondary(int themeColorSecondary) { this.themeColorSecondary = themeColorSecondary; }
    public void setCharacterListMode(String characterListMode) { this.characterListMode = characterListMode; }
    public void setCurrentPersonaId(int currentPersonaId) { this.currentPersonaId = currentPersonaId; }

    public float getDefaultTemperature() { return defaultTemperature; }
    public void setDefaultTemperature(float defaultTemperature) { this.defaultTemperature = defaultTemperature; }

    public float getDefaultTopP() { return defaultTopP; }
    public void setDefaultTopP(float defaultTopP) { this.defaultTopP = defaultTopP; }

    public int getDefaultTopK() { return defaultTopK; }
    public void setDefaultTopK(int defaultTopK) { this.defaultTopK = defaultTopK; }

    public float getDefaultFrequencyPenalty() { return defaultFrequencyPenalty; }
    public void setDefaultFrequencyPenalty(float defaultFrequencyPenalty) { this.defaultFrequencyPenalty = defaultFrequencyPenalty; }

    public float getDefaultPresencePenalty() { return defaultPresencePenalty; }
    public void setDefaultPresencePenalty(float defaultPresencePenalty) { this.defaultPresencePenalty = defaultPresencePenalty; }

    public float getDefaultRepetitionPenalty() { return defaultRepetitionPenalty; }
    public void setDefaultRepetitionPenalty(float defaultRepetitionPenalty) { this.defaultRepetitionPenalty = defaultRepetitionPenalty; }
}