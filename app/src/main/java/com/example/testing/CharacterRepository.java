package com.example.testing;

import android.app.Application;
import androidx.lifecycle.LiveData;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CharacterRepository {

    private final CharacterDao characterDao;
    private final ExecutorService executorService;

    public CharacterRepository(Application application) {
        AppDatabase db = AppDatabase.getInstance(application);
        this.characterDao = db.characterDao();
        this.executorService = Executors.newSingleThreadExecutor();
    }

    public void insert(Character character) {
        executorService.execute(() -> characterDao.insert(character));
    }

    public void update(Character character) {
        executorService.execute(() -> characterDao.update(character));
    }

    public void delete(Character character) {
        executorService.execute(() -> characterDao.delete(character));
    }

    public LiveData<List<Character>> getAllCharacters() {
        return characterDao.getAllCharacters();
    }

    // --- NEW ---
    public LiveData<List<Character>> getHiddenCharacters() {
        return characterDao.getHiddenCharacters();
    }
    // -----------

    public LiveData<Character> getCharacterById(int id) {
        return characterDao.getCharacterById(id);
    }
}