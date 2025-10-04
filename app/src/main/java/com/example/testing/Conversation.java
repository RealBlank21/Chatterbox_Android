package com.example.testing;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.ColumnInfo;
import java.io.Serializable;

@Entity(tableName = "conversation")
public class Conversation implements Serializable {

    // --- Fields ---

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "conversation_id")
    private int id;

    // Foreign Key to the Character entity
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

    // -----------------------------------------------------------------
    // CONSTRUCTORS
    // -----------------------------------------------------------------

    // Constructor required by Room for creating an object from a database row
    public Conversation() { }

    // Constructor for creating a NEW conversation (Updated to include isActive)
    public Conversation(int characterId, String title) {
        this.characterId = characterId;
        this.title = title;
        long now = System.currentTimeMillis();
        this.createdAt = now;
        this.lastUpdated = now;
        this.isActive = true; // Defaulting new conversations to active
    }

    // -----------------------------------------------------------------
    // GETTERS
    // -----------------------------------------------------------------

    public int getId() { return id; }
    public int getCharacterId() { return characterId; }
    public String getTitle() { return title; }
    public long getCreatedAt() { return createdAt; }
    public long getLastUpdated() { return lastUpdated; }
    public Boolean getIsActive() { return isActive; }

    // -----------------------------------------------------------------
    // SETTERS
    // -----------------------------------------------------------------

    public void setId(int id) { this.id = id; }
    public void setCharacterId(int characterId) { this.characterId = characterId; }
    public void setTitle(String title) { this.title = title; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
    public void setLastUpdated(long lastUpdated) { this.lastUpdated = lastUpdated; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
}