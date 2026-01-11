package com.example.testing.ui.settings;

import android.app.Application;
import android.content.ContentResolver;
import android.net.Uri;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.example.testing.data.local.AppDatabase;
import com.example.testing.data.local.entity.Character;
import com.example.testing.data.local.entity.Conversation;
import com.example.testing.data.local.entity.Persona;
import com.example.testing.data.local.dao.PersonaDao;
import com.example.testing.data.local.entity.Scenario;
import com.example.testing.data.local.entity.User;
import com.example.testing.data.repository.UserRepository;
import com.example.testing.data.local.entity.Message;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

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
        });
    }

    public void updateUserCurrentPersona(int personaId) {
        User currentUser = user.getValue();
        if (currentUser != null) {
            currentUser.setCurrentPersonaId(personaId);
            repository.insertOrUpdate(currentUser);
        }
    }

    public void saveSettings(String username, String apiKey, String preferredModel, String globalPrompt, int contextLimit,
                             int colorPrimary, int colorSecondary, String viewMode, int currentPersonaId,
                             float temp, float topP, int topK, float freqPen, float presPen, float repPen) {
        User currentUser = user.getValue();

        if (currentUser == null) {
            // Handle creation if needed, though usually currentUser exists here
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

        // Set new fields
        currentUser.setDefaultTemperature(temp);
        currentUser.setDefaultTopP(topP);
        currentUser.setDefaultTopK(topK);
        currentUser.setDefaultFrequencyPenalty(freqPen);
        currentUser.setDefaultPresencePenalty(presPen);
        currentUser.setDefaultRepetitionPenalty(repPen);

        repository.insertOrUpdate(currentUser);
    }

    public void clearAllData() {
        executorService.execute(() -> {
            try {
                db.runInTransaction(() -> {
                    db.messageDao().deleteAll();
                    db.conversationDao().deleteAll();
                    db.scenarioDao().deleteAll();
                    db.characterDao().deleteAll();
                    db.personaDao().deleteAll();
                    // We do NOT delete the User table to preserve API keys and settings
                });
                showToast("All data cleared successfully");
            } catch (Exception e) {
                e.printStackTrace();
                showToast("Failed to clear data: " + e.getMessage());
            }
        });
    }

    public void exportBackup(Uri uri, ContentResolver resolver) {
        executorService.execute(() -> {
            try {
                User user = db.userDao().getUserSync();
                List<Character> characters = db.characterDao().getAllCharactersSync();
                List<Conversation> conversations = db.conversationDao().getAllConversationsSync();
                List<Message> messages = db.messageDao().getAllMessagesSync();
                List<Persona> personas = db.personaDao().getAllPersonasSync();
                List<Scenario> scenarios = db.scenarioDao().getAllScenariosSync();

                // Prepare data for JSON by converting absolute paths to relative 'images/' paths
                for (com.example.testing.data.local.entity.Character c : characters) {
                    if (c.getCharacterProfileImagePath() != null && !c.getCharacterProfileImagePath().isEmpty()) {
                        File imgFile = new File(c.getCharacterProfileImagePath());
                        // Store only filename in the backup JSON, prefixed with folder
                        c.setCharacterProfileImagePath("images/" + imgFile.getName());
                    }
                }

                for (Scenario s : scenarios) {
                    if (s.getImagePath() != null && !s.getImagePath().isEmpty()) {
                        File imgFile = new File(s.getImagePath());
                        s.setImagePath("images/" + imgFile.getName());
                    }
                }

                BackupData backupData = new BackupData(user, characters, conversations, messages, personas, scenarios);

                // Use GsonBuilder to enable pretty printing
                String json = new GsonBuilder().setPrettyPrinting().create().toJson(backupData);

                try (OutputStream outputStream = resolver.openOutputStream(uri);
                     ZipOutputStream zipOut = new ZipOutputStream(new BufferedOutputStream(outputStream))) {

                    // 1. Write JSON file
                    ZipEntry jsonEntry = new ZipEntry("backup.json");
                    zipOut.putNextEntry(jsonEntry);
                    zipOut.write(json.getBytes());
                    zipOut.closeEntry();

                    // 2. Write Images
                    // We need to re-query or iterate the ORIGINAL lists from DB to get the actual file paths
                    List<Character> rawCharacters = db.characterDao().getAllCharactersSync();
                    List<Scenario> rawScenarios = db.scenarioDao().getAllScenariosSync();

                    byte[] buffer = new byte[4096];

                    for (com.example.testing.data.local.entity.Character c : rawCharacters) {
                        String path = c.getCharacterProfileImagePath();
                        if (path != null && !path.isEmpty()) {
                            File file = new File(path);
                            if (file.exists()) {
                                ZipEntry entry = new ZipEntry("images/" + file.getName());
                                zipOut.putNextEntry(entry);
                                try (FileInputStream fis = new FileInputStream(file)) {
                                    int count;
                                    while ((count = fis.read(buffer)) != -1) {
                                        zipOut.write(buffer, 0, count);
                                    }
                                }
                                zipOut.closeEntry();
                            }
                        }
                    }

                    for (Scenario s : rawScenarios) {
                        String path = s.getImagePath();
                        if (path != null && !path.isEmpty()) {
                            File file = new File(path);
                            if (file.exists()) {
                                ZipEntry entry = new ZipEntry("images/" + file.getName());
                                zipOut.putNextEntry(entry);
                                try (FileInputStream fis = new FileInputStream(file)) {
                                    int count;
                                    while ((count = fis.read(buffer)) != -1) {
                                        zipOut.write(buffer, 0, count);
                                    }
                                }
                                zipOut.closeEntry();
                            }
                        }
                    }

                    showToast("Backup Exported Successfully");
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
                File cacheDir = getApplication().getCacheDir();
                File tempZip = new File(cacheDir, "temp_restore.zip");

                // Copy stream to temp file first
                try (InputStream in = resolver.openInputStream(uri);
                     OutputStream out = new FileOutputStream(tempZip)) {
                    byte[] buffer = new byte[4096];
                    int len;
                    while ((len = in.read(buffer)) > 0) out.write(buffer, 0, len);
                }

                String jsonContent = null;
                File imagesDir = new File(getApplication().getFilesDir(), "profile_images");
                if (!imagesDir.exists()) imagesDir.mkdirs();

                try (ZipInputStream zipIn = new ZipInputStream(new BufferedInputStream(new FileInputStream(tempZip)))) {
                    ZipEntry entry;
                    byte[] buffer = new byte[4096];

                    while ((entry = zipIn.getNextEntry()) != null) {
                        String entryName = entry.getName();

                        if (entryName.equals("backup.json")) {
                            // Read JSON
                            StringBuilder sb = new StringBuilder();
                            int len;
                            while ((len = zipIn.read(buffer)) > 0) {
                                sb.append(new String(buffer, 0, len));
                            }
                            jsonContent = sb.toString();
                        } else if (entryName.startsWith("images/")) {
                            // Extract Image
                            File destFile = new File(imagesDir, new File(entryName).getName());
                            try (FileOutputStream fos = new FileOutputStream(destFile)) {
                                int len;
                                while ((len = zipIn.read(buffer)) > 0) {
                                    fos.write(buffer, 0, len);
                                }
                            }
                        }
                        zipIn.closeEntry();
                    }
                }

                if (jsonContent == null) {
                    showToast("Invalid Backup: No backup.json found");
                    return;
                }

                BackupData backupData = new Gson().fromJson(jsonContent, BackupData.class);

                if (backupData == null) {
                    showToast("Invalid Backup Data");
                    return;
                }

                // Restore Data
                db.runInTransaction(() -> {
                    db.messageDao().deleteAll();
                    db.conversationDao().deleteAll();
                    db.scenarioDao().deleteAll();
                    db.characterDao().deleteAll();
                    db.personaDao().deleteAll();

                    if (backupData.user != null) db.userDao().insertOrUpdate(backupData.user);

                    if (backupData.personas != null) {
                        for (Persona p : backupData.personas) {
                            db.personaDao().insert(p);
                        }
                    }

                    // Fix Image Paths for Characters
                    if (backupData.characters != null) {
                        for (Character c : backupData.characters) {
                            String relPath = c.getCharacterProfileImagePath();
                            if (relPath != null && relPath.startsWith("images/")) {
                                File imgFile = new File(imagesDir, new File(relPath).getName());
                                c.setCharacterProfileImagePath(imgFile.getAbsolutePath());
                            }
                            db.characterDao().insert(c);
                        }
                    }

                    // Fix Image Paths for Scenarios
                    if (backupData.scenarios != null) {
                        for (Scenario s : backupData.scenarios) {
                            String relPath = s.getImagePath();
                            if (relPath != null && relPath.startsWith("images/")) {
                                File imgFile = new File(imagesDir, new File(relPath).getName());
                                s.setImagePath(imgFile.getAbsolutePath());
                            }
                            db.scenarioDao().insert(s);
                        }
                    }

                    if (backupData.conversations != null) db.conversationDao().insertAll(backupData.conversations);
                    if (backupData.messages != null) db.messageDao().insertAll(backupData.messages);
                });

                // Cleanup
                tempZip.delete();

                showToast("Backup Restored Successfully");

            } catch (Exception e) {
                e.printStackTrace();
                showToast("Import Failed: " + e.getMessage());
            }
        });
    }

    public void exportCharacters(Uri uri, ContentResolver resolver) {
        executorService.execute(() -> {
            try {
                List<Character> characters = db.characterDao().getAllCharactersSync();
                List<ScenarioExportData> exportList = new ArrayList<>();

                // Helper set to track which images we actually need to zip to avoid duplicates
                Set<String> imagesToZip = new HashSet<>();

                for (Character c : characters) {
                    List<Scenario> scenarios = db.scenarioDao().getScenariosForCharacterSync(c.getId());

                    // Handle Character Image
                    String originalCharImg = c.getCharacterProfileImagePath();
                    if (originalCharImg != null && !originalCharImg.isEmpty()) {
                        File imgFile = new File(originalCharImg);
                        if (imgFile.exists()) {
                            imagesToZip.add(originalCharImg);
                            // Set relative path for export object
                            c.setCharacterProfileImagePath("images/" + imgFile.getName());
                        }
                    }

                    // Handle Scenario Images
                    for (Scenario s : scenarios) {
                        String originalScenImg = s.getImagePath();
                        if (originalScenImg != null && !originalScenImg.isEmpty()) {
                            File imgFile = new File(originalScenImg);
                            if (imgFile.exists()) {
                                imagesToZip.add(originalScenImg);
                                // Set relative path for export object
                                s.setImagePath("images/" + imgFile.getName());
                            }
                        }
                    }

                    exportList.add(new ScenarioExportData(c, scenarios));
                }

                String json = new GsonBuilder().setPrettyPrinting().create().toJson(exportList);

                try (OutputStream outputStream = resolver.openOutputStream(uri);
                     ZipOutputStream zipOut = new ZipOutputStream(new BufferedOutputStream(outputStream))) {

                    // Write JSON
                    ZipEntry jsonEntry = new ZipEntry("characters.json");
                    zipOut.putNextEntry(jsonEntry);
                    zipOut.write(json.getBytes());
                    zipOut.closeEntry();

                    // Write Images
                    byte[] buffer = new byte[4096];
                    for (String path : imagesToZip) {
                        File file = new File(path);
                        if (file.exists()) {
                            ZipEntry entry = new ZipEntry("images/" + file.getName());
                            zipOut.putNextEntry(entry);
                            try (FileInputStream fis = new FileInputStream(file)) {
                                int count;
                                while ((count = fis.read(buffer)) != -1) {
                                    zipOut.write(buffer, 0, count);
                                }
                            }
                            zipOut.closeEntry();
                        }
                    }

                    showToast("Characters Exported Successfully");
                }

            } catch (Exception e) {
                e.printStackTrace();
                showToast("Export Failed: " + e.getMessage());
            }
        });
    }

    public void importCharacters(Uri uri, ContentResolver resolver) {
        executorService.execute(() -> {
            try {
                File cacheDir = getApplication().getCacheDir();
                File tempZip = new File(cacheDir, "temp_char_restore.zip");

                // Copy stream to file
                try (InputStream in = resolver.openInputStream(uri);
                     OutputStream out = new FileOutputStream(tempZip)) {
                    byte[] buffer = new byte[4096];
                    int len;
                    while ((len = in.read(buffer)) > 0) out.write(buffer, 0, len);
                }

                String jsonContent = null;
                File imagesDir = new File(getApplication().getFilesDir(), "profile_images");
                if (!imagesDir.exists()) imagesDir.mkdirs();

                try (ZipInputStream zipIn = new ZipInputStream(new BufferedInputStream(new FileInputStream(tempZip)))) {
                    ZipEntry entry;
                    byte[] buffer = new byte[4096];

                    while ((entry = zipIn.getNextEntry()) != null) {
                        String entryName = entry.getName();
                        if (entryName.equals("characters.json")) {
                            StringBuilder sb = new StringBuilder();
                            int len;
                            while ((len = zipIn.read(buffer)) > 0) sb.append(new String(buffer, 0, len));
                            jsonContent = sb.toString();
                        } else if (entryName.startsWith("images/")) {
                            File destFile = new File(imagesDir, new File(entryName).getName());
                            try (FileOutputStream fos = new FileOutputStream(destFile)) {
                                int len;
                                while ((len = zipIn.read(buffer)) > 0) fos.write(buffer, 0, len);
                            }
                        }
                        zipIn.closeEntry();
                    }
                }

                if (jsonContent == null) {
                    showToast("Invalid File: characters.json not found");
                    return;
                }

                Type listType = new TypeToken<List<ScenarioExportData>>(){}.getType();
                List<ScenarioExportData> importList = new Gson().fromJson(jsonContent, listType);

                if (importList == null) {
                    showToast("Invalid Character Data");
                    return;
                }

                db.runInTransaction(() -> {
                    for (ScenarioExportData data : importList) {
                        Character c = data.character;
                        List<Scenario> scenarios = data.scenarios;

                        // Fix Image Path
                        if (c.getCharacterProfileImagePath() != null && c.getCharacterProfileImagePath().startsWith("images/")) {
                            File img = new File(imagesDir, new File(c.getCharacterProfileImagePath()).getName());
                            c.setCharacterProfileImagePath(img.getAbsolutePath());
                        }

                        // Reset ID for insertion (Append mode)
                        c.setId(0);
                        long newCharId = db.characterDao().insert(c);

                        if (scenarios != null) {
                            for (Scenario s : scenarios) {
                                s.setId(0); // Reset ID
                                s.setCharacterId((int) newCharId); // Link to new character

                                // Fix Scenario Image Path
                                if (s.getImagePath() != null && s.getImagePath().startsWith("images/")) {
                                    File img = new File(imagesDir, new File(s.getImagePath()).getName());
                                    s.setImagePath(img.getAbsolutePath());
                                }
                                db.scenarioDao().insert(s);
                            }
                        }
                    }
                });

                tempZip.delete();
                showToast("Characters Imported Successfully");

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
        List<com.example.testing.data.local.entity.Character> characters;
        List<Conversation> conversations;
        List<Message> messages;
        List<Persona> personas;
        List<Scenario> scenarios;

        public BackupData(User user, List<Character> characters, List<Conversation> conversations, List<Message> messages, List<Persona> personas, List<Scenario> scenarios) {
            this.user = user;
            this.characters = characters;
            this.conversations = conversations;
            this.messages = messages;
            this.personas = personas;
            this.scenarios = scenarios;
        }
    }

    private static class ScenarioExportData {
        Character character;
        List<Scenario> scenarios;

        public ScenarioExportData(Character character, List<Scenario> scenarios) {
            this.character = character;
            this.scenarios = scenarios;
        }
    }
}