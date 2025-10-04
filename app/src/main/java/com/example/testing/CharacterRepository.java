package com.example.testing;

import android.app.Application;

import androidx.lifecycle.LiveData;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CharacterRepository {

    private final CharacterDao characterDao;
    private final ExecutorService executorService;

    // Constructor
    public CharacterRepository(Application application) {
        AppDatabase db = AppDatabase.getInstance(application);
        this.characterDao = db.characterDao();
        // Create a background thread pool
        this.executorService = Executors.newSingleThreadExecutor();
    }

    // --- Public API for accessing data ---

    // Method to insert a character (runs on a background thread)
    public void insert(Character character) {
        executorService.execute(() -> characterDao.insert(character));
    }

    // Method to update a character (runs on a background thread)
    public void update(Character character) {
        executorService.execute(() -> characterDao.update(character));
    }

    // Method to delete a character (runs on a background thread)
    public void delete(Character character) {
        executorService.execute(() -> characterDao.delete(character));
    }

    public LiveData<List<Character>> getAllCharacters() {
        return characterDao.getAllCharacters();
    }

    /*
     Note: Methods that retrieve data are more complex because the UI
     needs a way to get the result back from the background thread.
     We will implement these later using LiveData when we build the ViewModel.
     For now, we are setting up the write operations.
    */
}