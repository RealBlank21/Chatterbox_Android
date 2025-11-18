package com.example.testing;

import android.app.Application;
import androidx.lifecycle.LiveData;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ConversationRepository {

    private final ConversationDao conversationDao;
    private final ExecutorService executorService;

    public interface InsertCallback {
        void onInsertFinished(Long newId);
    }

    public ConversationRepository(Application application) {
        AppDatabase db = AppDatabase.getInstance(application);
        this.conversationDao = db.conversationDao();
        this.executorService = Executors.newSingleThreadExecutor();
    }

    public void insert(Conversation conversation, InsertCallback callback) {
        executorService.execute(() -> {
            long newId = conversationDao.insert(conversation);
            if (callback != null) {
                callback.onInsertFinished(newId);
            }
        });
    }

    public void insert(Conversation conversation) {
        insert(conversation, null);
    }

    // --- ADD THIS METHOD ---
    public void updateLastUpdated(int conversationId, long timestamp) {
        executorService.execute(() -> conversationDao.updateLastUpdated(conversationId, timestamp));
    }
    // -----------------------

    public void update(Conversation conversation) {
        executorService.execute(() -> conversationDao.update(conversation));
    }

    // ... (Keep other methods: insertAll, deleteAll, delete, getConversationById, getAllConversationsWithCharacter) ...

    public void insertAll(List<Conversation> conversations) {
        executorService.execute(() -> conversationDao.insertAll(conversations));
    }

    public List<Conversation> getAllConversationsSync() {
        return conversationDao.getAllConversationsSync();
    }

    public void deleteAll() {
        executorService.execute(() -> conversationDao.deleteAll());
    }

    public void delete(Conversation conversation) {
        executorService.execute(() -> conversationDao.delete(conversation));
    }

    public LiveData<Conversation> getConversationById(int conversationId) {
        return conversationDao.getConversationById(conversationId);
    }

    public LiveData<List<ConversationWithCharacter>> getAllConversationsWithCharacter() {
        return conversationDao.getAllConversationsWithCharacter();
    }
}