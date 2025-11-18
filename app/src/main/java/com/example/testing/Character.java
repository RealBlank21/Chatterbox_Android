package com.example.testing;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;
import androidx.room.ColumnInfo;

@Entity(tableName = "character")
public class Character {

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "character_id")
    private int id;

    @ColumnInfo(name = "created_at")
    private long createdAt;

    @ColumnInfo(name = "name")
    private String name;

    @ColumnInfo(name = "model")
    private String model;

    @ColumnInfo(name = "personality")
    private String personality; // The System Prompt

    @ColumnInfo(name = "first_message")
    private String firstMessage; // The initial message the bot sends

    @ColumnInfo(name = "is_active")
    private Boolean isActive;

    @ColumnInfo(name = "character_profile_image_path")
    private String characterProfileImagePath;

    @ColumnInfo(name = "voice_reference_id")
    private String voiceReferenceId;

    @ColumnInfo(name = "voice_reference_name")
    private String voiceReferenceName;

    @ColumnInfo(name = "temperature")
    private Float temperature;

    @ColumnInfo(name = "max_tokens")
    private Integer maxTokens;

    // --- NEW FIELDS ---
    @ColumnInfo(name = "is_favorite")
    private boolean isFavorite;

    @ColumnInfo(name = "is_hidden")
    private boolean isHidden;

    // Added defaultValue = "0" (false) to ensure safety for old/migrated data
    @ColumnInfo(name = "is_time_aware", defaultValue = "0")
    private boolean isTimeAware;

    // -----------------------------------------------------------------
    // CONSTRUCTORS
    // -----------------------------------------------------------------

    public Character() { }

    @Ignore
    public Character(String name, String personality, String firstMessage,
                     String model, String characterProfileImagePath,
                     String voiceReferenceId, String voiceReferenceName,
                     Float temperature, Integer maxTokens, boolean isTimeAware) {
        this.createdAt = System.currentTimeMillis();
        this.name = name;
        this.personality = personality;
        this.firstMessage = firstMessage;
        this.model = model;
        this.isActive = true;
        this.characterProfileImagePath = characterProfileImagePath;
        this.voiceReferenceId = voiceReferenceId;
        this.voiceReferenceName = voiceReferenceName;
        this.temperature = temperature;
        this.maxTokens = maxTokens;
        this.isFavorite = false;
        this.isHidden = false;
        this.isTimeAware = isTimeAware;
    }

    // Overloaded constructor for convenience (Defaults isTimeAware to false)
    @Ignore
    public Character(String name, String personality, String firstMessage,
                     String model, String characterProfileImagePath,
                     String voiceReferenceId, String voiceReferenceName) {
        this(name, personality, firstMessage, model, characterProfileImagePath, voiceReferenceId, voiceReferenceName, null, null, false);
    }


    // -----------------------------------------------------------------
    // GETTERS
    // -----------------------------------------------------------------

    public int getId() { return id; }
    public long getCreatedAt() { return createdAt; }
    public String getName() { return name; }
    public String getPersonality() { return personality; }
    public String getFirstMessage() { return firstMessage; }
    public String getModel() { return model; }
    public Boolean getIsActive() { return isActive; }
    public String getCharacterProfileImagePath() { return characterProfileImagePath; }
    public String getVoiceReferenceId() { return voiceReferenceId; }
    public String getVoiceReferenceName() { return voiceReferenceName; }
    public Float getTemperature() { return temperature; }
    public Integer getMaxTokens() { return maxTokens; }
    public boolean isFavorite() { return isFavorite; }
    public boolean isHidden() { return isHidden; }
    public boolean isTimeAware() { return isTimeAware; }

    // -----------------------------------------------------------------
    // SETTERS
    // -----------------------------------------------------------------

    public void setId(int id) { this.id = id; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
    public void setName(String name) { this.name = name; }
    public void setPersonality(String personality) { this.personality = personality; }
    public void setFirstMessage(String firstMessage) { this.firstMessage = firstMessage; }
    public void setModel(String model) {this.model = model; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
    public void setCharacterProfileImagePath(String characterProfileImagePath) { this.characterProfileImagePath = characterProfileImagePath; }
    public void setVoiceReferenceId(String voiceReferenceId) { this.voiceReferenceId = voiceReferenceId; }
    public void setVoiceReferenceName(String voiceReferenceName) { this.voiceReferenceName = voiceReferenceName; }
    public void setTemperature(Float temperature) { this.temperature = temperature; }
    public void setMaxTokens(Integer maxTokens) { this.maxTokens = maxTokens; }
    public void setFavorite(boolean favorite) { isFavorite = favorite; }
    public void setHidden(boolean hidden) { isHidden = hidden; }
    public void setTimeAware(boolean timeAware) { isTimeAware = timeAware; }
}