package com.example.testing;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import java.util.List;

public class GalleryImageViewModel extends AndroidViewModel {

    private final GalleryImageRepository repository;

    public GalleryImageViewModel(@NonNull Application application) {
        super(application);
        repository = GalleryImageRepository.getInstance(application);
    }

    public LiveData<List<GalleryImage>> getImagesForOwner(int ownerId, String ownerType) {
        return repository.getImagesForOwner(ownerId, ownerType);
    }

    public void insert(GalleryImage galleryImage) {
        repository.insert(galleryImage);
    }

    public void update(GalleryImage galleryImage) {
        repository.update(galleryImage);
    }

    public void delete(GalleryImage galleryImage) {
        repository.delete(galleryImage);
    }
}