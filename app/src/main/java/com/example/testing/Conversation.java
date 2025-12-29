package com.example.testing;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import java.io.Serializable;

@Entity(tableName = "conversation", indices = {@Index(value = "last_updated", name = "index_conversation_last_updated")})
public class Conversation implements Serializable {

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "conversation_id")
    private int id;

    @ColumnInfo(name = "character_fk")
    private int characterId;

    @ColumnInfo(name = "title")
    private String title;

    @ColumnInfo(name = "created_at")
    private long createdAt;

    @ColumnInfo(name = "last_updated")
    private long lastUpdated;

    @ColumnInfo(name = "is_active")
    private Boolean isActive;

    // --- NEW FIELDS ---
    @ColumnInfo(name = "scenario_id")
    private Integer scenarioId; // Nullable (Can be null if using default/none)

    @ColumnInfo(name = "persona_id")
    private Integer personaId; // Nullable (Can be null if using default user profile)

    public Conversation() { }

    @Ignore
    public Conversation(int characterId, String title) {
        this.characterId = characterId;
        this.title = title;
        long now = System.currentTimeMillis();
        this.createdAt = now;
        this.lastUpdated = now;
        this.isActive = true;
        // Default to null for new fields initially
        this.scenarioId = null;
        this.personaId = null;
    }

    // --- Existing Getters/Setters ---
    public int getId() { return id; }
    public int getCharacterId() { return characterId; }
    public String getTitle() { return title; }
    public long getCreatedAt() { return createdAt; }
    public long getLastUpdated() { return lastUpdated; }
    public Boolean getIsActive() { return isActive; }

    public void setId(int id) { this.id = id; }
    public void setCharacterId(int characterId) { this.characterId = characterId; }
    public void setTitle(String title) { this.title = title; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
    public void setLastUpdated(long lastUpdated) { this.lastUpdated = lastUpdated; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }

    // --- NEW Getters/Setters ---
    public Integer getScenarioId() { return scenarioId; }
    public void setScenarioId(Integer scenarioId) { this.scenarioId = scenarioId; }

    public Integer getPersonaId() { return personaId; }
    public void setPersonaId(Integer personaId) { this.personaId = personaId; }
}