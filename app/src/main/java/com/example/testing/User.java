package com.example.testing;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.ColumnInfo;
import java.io.Serializable;

@Entity(tableName = "user_config")
public class User implements Serializable {

    // Best practice for Serializable
    private static final long serialVersionUID = 2L;

    // --- Database Fields (All fields must be public or have getters/setters) ---

    // 2. Define the Primary Key
    // Since you only expect ONE user/config row, a static ID is appropriate.
    @PrimaryKey
    @ColumnInfo(name = "config_id")
    private int configId = 1; // Always set to 1, ensuring only one row exists

    // Identifiers and Display Fields
    @ColumnInfo(name = "username")
    private String username;

    @ColumnInfo(name = "email")
    private String email;

    @ColumnInfo(name = "profile_image_path")
    private String profileImagePath;

    @ColumnInfo(name = "api_key")
    private String apiKey;

    // Preferences
    @ColumnInfo(name = "preferred_model")
    private String preferredModel;

    @ColumnInfo(name = "global_system_prompt")
    private String globalSystemPrompt;

    // -----------------------------------------------------------------
    // CONSTRUCTOR (For easy object creation)
    // -----------------------------------------------------------------

    public User(String username, String email, String profileImagePath,
                String apiKey, String preferredModel, String globalSystemPrompt) {
        // We do NOT set configId here; Room handles the PK
        this.username = username;
        this.email = email;
        this.profileImagePath = profileImagePath;
        this.apiKey = apiKey;
        this.preferredModel = preferredModel;
        this.globalSystemPrompt = globalSystemPrompt;
    }

    // Default constructor for Room and Serialization (REQUIRED)
    public User() {
    }

    // -----------------------------------------------------------------
    // GETTERS
    // -----------------------------------------------------------------

    public int getConfigId() { return configId; }
    public String getUsername() { return username; }
    public String getEmail() { return email; }
    public String getProfileImagePath() { return profileImagePath; }
    public String getApiKey() { return apiKey; }
    public String getPreferredModel() { return preferredModel; }
    public String getGlobalSystemPrompt() { return globalSystemPrompt; }

    // -----------------------------------------------------------------
    // SETTERS
    // -----------------------------------------------------------------

    public void setConfigId(int configId) { this.configId = configId; }
    public void setUsername(String username) { this.username = username; }
    public void setEmail(String email) { this.email = email; }
    public void setProfileImagePath(String profileImagePath) { this.profileImagePath = profileImagePath; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }
    public void setPreferredModel(String preferredModel) { this.preferredModel = preferredModel; }
    public void setGlobalSystemPrompt(String globalSystemPrompt) { this.globalSystemPrompt = globalSystemPrompt; }
}