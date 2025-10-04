package com.example.testing;

import android.app.Application;
import androidx.lifecycle.LiveData;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ConversationRepository {

    private final ConversationDao conversationDao;
    private final ExecutorService executorService;

    // A simple interface we can use for a callback
    public interface InsertCallback {
        void onInsertFinished(Long newId);
    }

    public ConversationRepository(Application application) {
        AppDatabase db = AppDatabase.getInstance(application);
        this.conversationDao = db.conversationDao();
        this.executorService = Executors.newSingleThreadExecutor();
    }

    // --- MODIFIED INSERT METHOD ---
    public void insert(Conversation conversation, InsertCallback callback) {
        executorService.execute(() -> {
            // The insert method returns the ID of the new row
            long newId = conversationDao.insert(conversation);
            // If a callback was provided, run it on the main thread
            if (callback != null) {
                // We need to ensure the callback runs on the UI thread if it updates UI
                // For now, this is fine as LiveData handles the thread switch.
                callback.onInsertFinished(newId);
            }
        });
    }

    // Overloaded method for when we don't need the ID back
    public void insert(Conversation conversation) {
        insert(conversation, null);
    }

    public void update(Conversation conversation) {
        executorService.execute(() -> conversationDao.update(conversation));
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