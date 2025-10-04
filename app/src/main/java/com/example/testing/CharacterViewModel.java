package com.example.testing;

import android.app.Application;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import java.util.List;

public class CharacterViewModel extends AndroidViewModel {

    private final CharacterRepository repository;
    private final LiveData<List<Character>> allCharacters;

    public CharacterViewModel(Application application) {
        super(application);
        repository = new CharacterRepository(application);
        // This is where the magic happens. We get the LiveData from the DAO.
        allCharacters = repository.getAllCharacters();
    }

    // --- Public API for the UI ---

    // Getter for the LiveData list of characters. The UI will observe this.
    public LiveData<List<Character>> getAllCharacters() {
        return allCharacters;
    }

    // Wrapper methods that call the repository. These will be used by the UI
    // to perform actions.
    public void insert(Character character) {
        repository.insert(character);
    }

    public void update(Character character) {
        repository.update(character);
    }

    public void delete(Character character) {
        repository.delete(character);
    }
}