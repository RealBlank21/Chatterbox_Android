package com.example.testing;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;
import androidx.room.ColumnInfo;
import java.io.Serializable;

@Entity(tableName = "user_config")
public class User implements Serializable {

    private static final long serialVersionUID = 3L; // Bumped version

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

    // --- NEW FIELD ---
    @ColumnInfo(name = "default_context_limit", defaultValue = "0")
    private int defaultContextLimit; // 0 means unlimited

    @Ignore
    public User(String username, String email, String profileImagePath,
                String apiKey, String preferredModel, String globalSystemPrompt) {
        this(username, email, profileImagePath, apiKey, preferredModel, globalSystemPrompt, 0);
    }

    @Ignore
    public User(String username, String email, String profileImagePath,
                String apiKey, String preferredModel, String globalSystemPrompt, int defaultContextLimit) {
        this.username = username;
        this.email = email;
        this.profileImagePath = profileImagePath;
        this.apiKey = apiKey;
        this.preferredModel = preferredModel;
        this.globalSystemPrompt = globalSystemPrompt;
        this.defaultContextLimit = defaultContextLimit;
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

    // --- Setters ---
    public void setConfigId(int configId) { this.configId = configId; }
    public void setUsername(String username) { this.username = username; }
    public void setEmail(String email) { this.email = email; }
    public void setProfileImagePath(String profileImagePath) { this.profileImagePath = profileImagePath; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }
    public void setPreferredModel(String preferredModel) { this.preferredModel = preferredModel; }
    public void setGlobalSystemPrompt(String globalSystemPrompt) { this.globalSystemPrompt = globalSystemPrompt; }
    public void setDefaultContextLimit(int defaultContextLimit) { this.defaultContextLimit = defaultContextLimit; }
}