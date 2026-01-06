package com.example.testing;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface GalleryImageDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(GalleryImage galleryImage);

    @Update
    void update(GalleryImage galleryImage);

    @Delete
    void delete(GalleryImage galleryImage);

    @Query("SELECT * FROM gallery_image WHERE uuid = :uuid")
    GalleryImage getImageByUuid(String uuid);

    @Query("SELECT * FROM gallery_image WHERE owner_id = :ownerId AND owner_type = :ownerType ORDER BY created_at DESC")
    LiveData<List<GalleryImage>> getImagesForOwner(int ownerId, String ownerType);

    @Query("SELECT * FROM gallery_image WHERE owner_id = :ownerId AND owner_type = :ownerType")
    List<GalleryImage> getImagesForOwnerSync(int ownerId, String ownerType);

    @Query("DELETE FROM gallery_image WHERE owner_id = :ownerId AND owner_type = :ownerType")
    void deleteImagesForOwner(int ownerId, String ownerType);
}