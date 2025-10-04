package com.example.testing;

import androidx.room.Entity;
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

    // -----------------------------------------------------------------
    // CONSTRUCTORS
    // -----------------------------------------------------------------

    // Constructor required by Room for creating an object from a database row
    public Character() { }

    // Constructor for creating a NEW character
    public Character(String name, String personality, String firstMessage,
                     String model,
                     String characterProfileImagePath,
                     String voiceReferenceId, String voiceReferenceName) {
        this.createdAt = System.currentTimeMillis();
        this.name = name;
        this.personality = personality;
        this.firstMessage = firstMessage;
        this.model = model;
        this.isActive = true;
        this.characterProfileImagePath = characterProfileImagePath;
        this.voiceReferenceId = voiceReferenceId;
        this.voiceReferenceName = voiceReferenceName;
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
}