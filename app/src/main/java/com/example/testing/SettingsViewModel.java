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
    private final ExecutorService executorService;
    private final AppDatabase db;

    public SettingsViewModel(@NonNull Application application) {
        super(application);
        db = AppDatabase.getInstance(application);
        repository = UserRepository.getInstance(application);
        user = repository.getUser();
        executorService = Executors.newSingleThreadExecutor();
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        executorService.shutdown();
    }

    public LiveData<User> getUser() {
        return user;
    }

    // UPDATED SIGNATURE
    public void saveSettings(String username, String apiKey, String preferredModel, String globalPrompt, int contextLimit, int colorPrimary, int colorSecondary, String viewMode) {
        User currentUser = user.getValue();

        if (currentUser == null) {
            currentUser = new User(username, "", "", apiKey, preferredModel, globalPrompt, contextLimit, colorPrimary, colorSecondary, viewMode);
        } else {
            currentUser.setUsername(username);
            currentUser.setApiKey(apiKey);
            currentUser.setPreferredModel(preferredModel);
            currentUser.setGlobalSystemPrompt(globalPrompt);
            currentUser.setDefaultContextLimit(contextLimit);
            currentUser.setThemeColorPrimary(colorPrimary);
            currentUser.setThemeColorSecondary(colorSecondary);
            currentUser.setCharacterListMode(viewMode);
        }

        repository.insertOrUpdate(currentUser);
    }

    public void exportBackup(Uri uri, ContentResolver resolver) {
        executorService.execute(() -> {
            try {
                User user = db.userDao().getUserSync();
                List<Character> characters = db.characterDao().getAllCharactersSync();
                List<Conversation> conversations = db.conversationDao().getAllConversationsSync();
                List<Message> messages = db.messageDao().getAllMessagesSync();

                BackupData backupData = new BackupData(user, characters, conversations, messages);
                String json = new Gson().toJson(backupData);

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

    public void importBackup(Uri uri, ContentResolver resolver) {
        executorService.execute(() -> {
            try {
                StringBuilder stringBuilder = new StringBuilder();
                try (InputStream inputStream = resolver.openInputStream(uri);
                     BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        stringBuilder.append(line);
                    }
                }

                String json = stringBuilder.toString();
                BackupData backupData = new Gson().fromJson(json, BackupData.class);

                if (backupData == null) {
                    showToast("Invalid Backup File");
                    return;
                }

                db.runInTransaction(() -> {
                    db.messageDao().deleteAll();
                    db.conversationDao().deleteAll();
                    db.characterDao().deleteAll();

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