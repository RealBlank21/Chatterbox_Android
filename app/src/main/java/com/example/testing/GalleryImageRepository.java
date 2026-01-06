package com.example.testing;

import android.app.Application;
import androidx.lifecycle.LiveData;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class GalleryImageRepository {

    private final GalleryImageDao galleryImageDao;
    private final ExecutorService executorService;
    private static volatile GalleryImageRepository INSTANCE;

    private GalleryImageRepository(Application application) {
        AppDatabase db = AppDatabase.getInstance(application);
        galleryImageDao = db.galleryImageDao();
        executorService = Executors.newFixedThreadPool(4);
    }

    public static GalleryImageRepository getInstance(Application application) {
        if (INSTANCE == null) {
            synchronized (GalleryImageRepository.class) {
                if (INSTANCE == null) {
                    INSTANCE = new GalleryImageRepository(application);
                }
            }
        }
        return INSTANCE;
    }

    public LiveData<List<GalleryImage>> getImagesForOwner(int ownerId, String ownerType) {
        return galleryImageDao.getImagesForOwner(ownerId, ownerType);
    }

    // Synchronous method for System Prompt generation (Background Thread)
    public List<GalleryImage> getImagesForOwnerSync(int ownerId, String ownerType) {
        return galleryImageDao.getImagesForOwnerSync(ownerId, ownerType);
    }

    public void insert(GalleryImage galleryImage) {
        executorService.execute(() -> galleryImageDao.insert(galleryImage));
    }

    public void update(GalleryImage galleryImage) {
        executorService.execute(() -> galleryImageDao.update(galleryImage));
    }

    public void delete(GalleryImage galleryImage) {
        executorService.execute(() -> galleryImageDao.delete(galleryImage));
    }

    public void deleteImagesForOwner(int ownerId, String ownerType) {
        executorService.execute(() -> galleryImageDao.deleteImagesForOwner(ownerId, ownerType));
    }
}