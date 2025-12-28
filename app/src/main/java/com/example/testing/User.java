package com.example.testing;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;
import androidx.room.ColumnInfo;
import java.io.Serializable;

@Entity(tableName = "user_config")
public class User implements Serializable {

    private static final long serialVersionUID = 5L;

    @PrimaryKey
    @ColumnInfo(name = "config_id")
    private int configId = 1;

    @ColumnInfo(name = "username")
    private String username;

    @ColumnInfo(name = "email")
    private String email;

    @ColumnInfo(name = "profile_image_path")
    private String profileImagePath;

    @ColumnInfo(name = "api_key")
    private String apiKey;

    @ColumnInfo(name = "preferred_model")
    private String preferredModel;

    @ColumnInfo(name = "global_system_prompt")
    private String globalSystemPrompt;

    @ColumnInfo(name = "default_context_limit", defaultValue = "0")
    private int defaultContextLimit;

    @ColumnInfo(name = "theme_color_primary", defaultValue = "0")
    private int themeColorPrimary;

    @ColumnInfo(name = "theme_color_secondary", defaultValue = "0")
    private int themeColorSecondary;

    // --- NEW FIELD ---
    @ColumnInfo(name = "character_list_mode", defaultValue = "list")
    private String characterListMode;

    @Ignore
    public User(String username, String email, String profileImagePath,
                String apiKey, String preferredModel, String globalSystemPrompt) {
        this(username, email, profileImagePath, apiKey, preferredModel, globalSystemPrompt, 0, 0, 0, "list");
    }

    @Ignore
    public User(String username, String email, String profileImagePath,
                String apiKey, String preferredModel, String globalSystemPrompt, int defaultContextLimit) {
        this(username, email, profileImagePath, apiKey, preferredModel, globalSystemPrompt, defaultContextLimit, 0, 0, "list");
    }

    @Ignore
    public User(String username, String email, String profileImagePath,
                String apiKey, String preferredModel, String globalSystemPrompt,
                int defaultContextLimit, int themeColorPrimary, int themeColorSecondary) {
        this(username, email, profileImagePath, apiKey, preferredModel, globalSystemPrompt, defaultContextLimit, themeColorPrimary, themeColorSecondary, "list");
    }

    @Ignore
    public User(String username, String email, String profileImagePath,
                String apiKey, String preferredModel, String globalSystemPrompt,
                int defaultContextLimit, int themeColorPrimary, int themeColorSecondary, String characterListMode) {
        this.username = username;
        this.email = email;
        this.profileImagePath = profileImagePath;
        this.apiKey = apiKey;
        this.preferredModel = preferredModel;
        this.globalSystemPrompt = globalSystemPrompt;
        this.defaultContextLimit = defaultContextLimit;
        this.themeColorPrimary = themeColorPrimary;
        this.themeColorSecondary = themeColorSecondary;
        this.characterListMode = characterListMode;
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
}