package com.example.testing;

import android.app.Application;
import android.content.ContentResolver;
import android.net.Uri;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import com.google.gson.Gson;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SettingsViewModel extends AndroidViewModel {

    private final UserRepository repository;
    private final LiveData<User> user;
    private final ExecutorService executorService; // Local executor for Import/Export
    private final AppDatabase db;

    public SettingsViewModel(@NonNull Application application) {
        super(application);
        db = AppDatabase.getInstance(application);

        // FIX: Use getInstance()
        repository = UserRepository.getInstance(application);
        user = repository.getUser();

        // This local executor is still needed for the heavy Import/Export logic
        // that doesn't fit neatly into the existing Repositories.
        executorService = Executors.newSingleThreadExecutor();
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        // FIX: Shutdown the executor to prevent thread leaks when leaving Settings
        executorService.shutdown();
    }

    public LiveData<User> getUser() {
        return user;
    }

    public void saveSettings(String username, String apiKey, String preferredModel, String globalPrompt, int contextLimit) {
        User currentUser = user.getValue();

        if (currentUser == null) {
            currentUser = new User(username, "", "", apiKey, preferredModel, globalPrompt, contextLimit);
        } else {
            currentUser.setUsername(username);
            currentUser.setApiKey(apiKey);
            currentUser.setPreferredModel(preferredModel);
            currentUser.setGlobalSystemPrompt(globalPrompt);
            currentUser.setDefaultContextLimit(contextLimit);
        }

        repository.insertOrUpdate(currentUser);
    }

    // --- EXPORT LOGIC ---
    public void exportBackup(Uri uri, ContentResolver resolver) {
        executorService.execute(() -> {
            try {
                // 1. Gather all data using synchronous DAO methods
                User user = db.userDao().getUserSync();
                List<Character> characters = db.characterDao().getAllCharactersSync();
                List<Conversation> conversations = db.conversationDao().getAllConversationsSync();
                List<Message> messages = db.messageDao().getAllMessagesSync();

                BackupData backupData = new BackupData(user, characters, conversations, messages);

                // 2. Serialize to JSON
                String json = new Gson().toJson(backupData);

                // 3. Write to file
                try (OutputStream outputStream = resolver.openOutputStream(uri)) {
                    if (outputStream != null) {
                        outputStream.write(json.getBytes());
                        showToast("Backup Exported Successfully");
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                showToast("Export Failed: " + e.getMessage());
            }
        });
    }

    // --- IMPORT LOGIC ---
    public void importBackup(Uri uri, ContentResolver resolver) {
        executorService.execute(() -> {
            try {
                // 1. Read JSON from file
                StringBuilder stringBuilder = new StringBuilder();
                try (InputStream inputStream = resolver.openInputStream(uri);
                     BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        stringBuilder.append(line);
                    }
                }

                // 2. Deserialize
                String json = stringBuilder.toString();
                BackupData backupData = new Gson().fromJson(json, BackupData.class);

                if (backupData == null) {
                    showToast("Invalid Backup File");
                    return;
                }

                // 3. Write to DB in a Transaction (Clear old -> Insert new)
                db.runInTransaction(() -> {
                    // Clear existing data
                    db.messageDao().deleteAll();
                    db.conversationDao().deleteAll();
                    db.characterDao().deleteAll();

                    // Restore data
                    if (backupData.user != null) db.userDao().insertOrUpdate(backupData.user);
                    if (backupData.characters != null) db.characterDao().insertAll(backupData.characters);
                    if (backupData.conversations != null) db.conversationDao().insertAll(backupData.conversations);
                    if (backupData.messages != null) db.messageDao().insertAll(backupData.messages);
                });

                showToast("Backup Restored Successfully");

            } catch (Exception e) {
                e.printStackTrace();
                showToast("Import Failed: " + e.getMessage());
            }
        });
    }

    private void showToast(String message) {
        android.os.Handler handler = new android.os.Handler(android.os.Looper.getMainLooper());
        handler.post(() -> Toast.makeText(getApplication(), message, Toast.LENGTH_SHORT).show());
    }

    private static class BackupData {
        User user;
        List<Character> characters;
        List<Conversation> conversations;
        List<Message> messages;

        public BackupData(User user, List<Character> characters, List<Conversation> conversations, List<Message> messages) {
            this.user = user;
            this.characters = characters;
            this.conversations = conversations;
            this.messages = messages;
        }
    }
}