package com.example.testing;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import java.io.Serializable;

@Entity(tableName = "scenario",
        foreignKeys = @ForeignKey(entity = Character.class,
                parentColumns = "character_id",
                childColumns = "character_id",
                onDelete = ForeignKey.CASCADE),
        indices = {@Index(value = "character_id")})
public class Scenario implements Serializable {

    @PrimaryKey(autoGenerate = true)
    private int id;

    @ColumnInfo(name = "character_id")
    private int characterId;

    @ColumnInfo(name = "name")
    private String name;

    @ColumnInfo(name = "description")
    private String description;

    @ColumnInfo(name = "first_message")
    private String firstMessage;

    @ColumnInfo(name = "is_default", defaultValue = "0")
    private boolean isDefault;

    @ColumnInfo(name = "image_path")
    private String imagePath;

    public Scenario() {
    }

    @Ignore
    public Scenario(int characterId, String name, String description, String firstMessage, boolean isDefault, String imagePath) {
        this.characterId = characterId;
        this.name = name;
        this.description = description;
        this.firstMessage = firstMessage;
        this.isDefault = isDefault;
        this.imagePath = imagePath;
    }

    // Constructor without image path for backward compatibility if needed, or convenience
    @Ignore
    public Scenario(int characterId, String name, String description, String firstMessage, boolean isDefault) {
        this(characterId, name, description, firstMessage, isDefault, "");
    }

    // Getters
    public int getId() { return id; }
    public int getCharacterId() { return characterId; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getFirstMessage() { return firstMessage; }
    public boolean isDefault() { return isDefault; }
    public String getImagePath() { return imagePath; }

    // Setters
    public void setId(int id) { this.id = id; }
    public void setCharacterId(int characterId) { this.characterId = characterId; }
    public void setName(String name) { this.name = name; }
    public void setDescription(String description) { this.description = description; }
    public void setFirstMessage(String firstMessage) { this.firstMessage = firstMessage; }
    public void setDefault(boolean isDefault) { this.isDefault = isDefault; }
    public void setImagePath(String imagePath) { this.imagePath = imagePath; }
}