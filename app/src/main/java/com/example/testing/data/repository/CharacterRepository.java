package com.example.testing.data.repository;

import android.app.Application;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Transformations;

import com.example.testing.data.local.AppDatabase;
import com.example.testing.data.local.entity.Character;
import com.example.testing.data.local.dao.CharacterDao;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CharacterRepository {

    private final CharacterDao characterDao;
    private final ExecutorService executorService;
    private static volatile CharacterRepository INSTANCE;

    private CharacterRepository(Application application) {
        AppDatabase db = AppDatabase.getInstance(application);
        characterDao = db.characterDao();
        executorService = Executors.newFixedThreadPool(4);
    }

    public static CharacterRepository getInstance(Application application) {
        if (INSTANCE == null) {
            synchronized (CharacterRepository.class) {
                if (INSTANCE == null) {
                    INSTANCE = new CharacterRepository(application);
                }
            }
        }
        return INSTANCE;
    }

    public LiveData<List<Character>> getFilteredCharacters(boolean isHidden, String searchQuery, String tagFilter) {
        return characterDao.getFilteredCharacters(isHidden, searchQuery, tagFilter);
    }

    public LiveData<List<String>> getAllTags() {
        return Transformations.map(characterDao.getAllTags(), rawTagsList -> {
            Set<String> uniqueTags = new HashSet<>();
            for (String raw : rawTagsList) {
                if (raw != null && !raw.isEmpty()) {
                    String[] split = raw.split("\\|");
                    for (String t : split) {
                        if (!t.trim().isEmpty()) {
                            uniqueTags.add(t.trim());
                        }
                    }
                }
            }
            List<String> sortedTags = new ArrayList<>(uniqueTags);
            Collections.sort(sortedTags);
            return sortedTags;
        });
    }

    public LiveData<com.example.testing.data.local.entity.Character> getCharacterById(int id) {
        return characterDao.getCharacterById(id);
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
}