package com.example.testing;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

public class SettingsViewModel extends AndroidViewModel {

    private final UserRepository repository;
    private final LiveData<User> user;

    public SettingsViewModel(@NonNull Application application) {
        super(application);
        repository = new UserRepository(application);
        user = repository.getUser();
    }

    public LiveData<User> getUser() {
        return user;
    }

    // Updated to accept all user fields
    public void saveSettings(String username, String apiKey, String preferredModel, String globalPrompt) {
        User currentUser = user.getValue();

        if (currentUser == null) {
            // If no user exists yet, create a new one. We pass "" for email and image path.
            currentUser = new User(username, "", "", apiKey, preferredModel, globalPrompt);
        } else {
            // If a user already exists, update all its fields
            currentUser.setUsername(username);
            currentUser.setApiKey(apiKey);
            currentUser.setPreferredModel(preferredModel);
            currentUser.setGlobalSystemPrompt(globalPrompt);
        }

        repository.insertOrUpdate(currentUser);
    }
}