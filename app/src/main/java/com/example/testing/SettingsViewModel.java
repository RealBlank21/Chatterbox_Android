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
    private final PersonaDao personaDao;
    private final LiveData<List<Persona>> allPersonas;
    private final ExecutorService executorService;
    private final AppDatabase db;

    public SettingsViewModel(@NonNull Application application) {
        super(application);
        db = AppDatabase.getInstance(application);
        repository = UserRepository.getInstance(application);
        personaDao = db.personaDao();
        user = repository.getUser();
        allPersonas = personaDao.getAllPersonas();
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

    public LiveData<List<Persona>> getAllPersonas() {
        return allPersonas;
    }

    public void addPersona(String name, String description) {
        executorService.execute(() -> {
            Persona persona = new Persona(name, description);
            long id = personaDao.insert(persona);
            updateUserCurrentPersona((int) id);
        });
    }

    public void updatePersona(Persona persona) {
        executorService.execute(() -> personaDao.update(persona));
    }

    public void deletePersona(Persona persona) {
        executorService.execute(() -> {
            personaDao.delete(persona);
            // If we deleted the current persona, reset selection logic is handled in Activity or by next fetch
        });
    }

    public void updateUserCurrentPersona(int personaId) {
        User currentUser = user.getValue();
        if (currentUser != null) {
            currentUser.setCurrentPersonaId(personaId);
            repository.insertOrUpdate(currentUser);
        }
    }

    public void saveSettings(String username, String apiKey, String preferredModel, String globalPrompt, int contextLimit, int colorPrimary, int colorSecondary, String viewMode, int currentPersonaId) {
        User currentUser = user.getValue();

        if (currentUser == null) {
            currentUser = new User(username, "", "", apiKey, preferredModel, globalPrompt, contextLimit, colorPrimary, colorSecondary, viewMode, currentPersonaId);
        } else {
            currentUser.setUsername(username);
            currentUser.setApiKey(apiKey);
            currentUser.setPreferredModel(preferredModel);
            currentUser.setGlobalSystemPrompt(globalPrompt);
            currentUser.setDefaultContextLimit(contextLimit);
            currentUser.setThemeColorPrimary(colorPrimary);
            currentUser.setThemeColorSecondary(colorSecondary);
            currentUser.setCharacterListMode(viewMode);
            currentUser.setCurrentPersonaId(currentPersonaId);
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
                List<Persona> personas = db.personaDao().getAllPersonasSync();

                BackupData backupData = new BackupData(user, characters, conversations, messages, personas);
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
                    db.personaDao().deleteAll(); // Clear old personas? Maybe

                    if (backupData.user != null) db.userDao().insertOrUpdate(backupData.user);
                    if (backupData.characters != null) db.characterDao().insertAll(backupData.characters);
                    if (backupData.conversations != null) db.conversationDao().insertAll(backupData.conversations);
                    if (backupData.messages != null) db.messageDao().insertAll(backupData.messages);
                    if (backupData.personas != null && !backupData.personas.isEmpty()) {
                        // We need to loop insert to generate IDs or just insertAll if IDs are preserved
                        // Simplest is to clear table and insertAll
                        for (Persona p : backupData.personas) {
                            db.personaDao().insert(p);
                        }
                    }
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
        List<Persona> personas;

        public BackupData(User user, List<Character> characters, List<Conversation> conversations, List<Message> messages, List<Persona> personas) {
            this.user = user;
            this.characters = characters;
            this.conversations = conversations;
            this.messages = messages;
            this.personas = personas;
        }
    }
}