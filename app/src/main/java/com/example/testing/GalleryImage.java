package com.example.testing;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import java.util.UUID;

// Added indices here to match MIGRATION_19_20
@Entity(tableName = "gallery_image", indices = {@Index(value = {"owner_id", "owner_type"}, name = "index_gallery_image_owner")})
public class GalleryImage {

    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "uuid")
    private String uuid;

    @ColumnInfo(name = "image_path")
    private String imagePath;

    @ColumnInfo(name = "label")
    private String label;

    @ColumnInfo(name = "description")
    private String description;

    @ColumnInfo(name = "owner_id")
    private int ownerId;

    @ColumnInfo(name = "owner_type")
    private String ownerType;

    @ColumnInfo(name = "created_at")
    private long createdAt;

    public GalleryImage() {
    }

    @Ignore
    public GalleryImage(String imagePath, String label, String description, int ownerId, String ownerType) {
        this.uuid = UUID.randomUUID().toString();
        this.imagePath = imagePath;
        this.label = label;
        this.description = description;
        this.ownerId = ownerId;
        this.ownerType = ownerType;
        this.createdAt = System.currentTimeMillis();
    }

    @NonNull
    public String getUuid() { return uuid; }
    public void setUuid(@NonNull String uuid) { this.uuid = uuid; }

    public String getImagePath() { return imagePath; }
    public void setImagePath(String imagePath) { this.imagePath = imagePath; }

    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public int getOwnerId() { return ownerId; }
    public void setOwnerId(int ownerId) { this.ownerId = ownerId; }

    public String getOwnerType() { return ownerType; }
    public void setOwnerType(String ownerType) { this.ownerType = ownerType; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
}