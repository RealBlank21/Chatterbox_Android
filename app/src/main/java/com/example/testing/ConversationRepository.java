package com.example.testing;

import android.app.Application;
import androidx.lifecycle.LiveData;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import android.os.Handler;
import android.os.Looper;

public class ConversationRepository {

    private static volatile ConversationRepository INSTANCE;
    private final ConversationDao conversationDao;
    private final ExecutorService executorService;
    private final Handler mainHandler; // To post results back to UI thread

    public interface InsertCallback {
        void onInsertFinished(Long newId);
    }

    // Callback for paged data
    public interface DataCallback<T> {
        void onDataLoaded(T data);
    }

    private ConversationRepository(Application application) {
        AppDatabase db = AppDatabase.getInstance(application);
        this.conversationDao = db.conversationDao();
        this.executorService = Executors.newSingleThreadExecutor();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    public static ConversationRepository getInstance(Application application) {
        if (INSTANCE == null) {
            synchronized (ConversationRepository.class) {
                if (INSTANCE == null) {
                    INSTANCE = new ConversationRepository(application);
                }
            }
        }
        return INSTANCE;
    }

    // --- PAGINATION METHOD ---
    public void loadConversationsPaged(int limit, int offset, DataCallback<List<ConversationWithCharacter>> callback) {
        executorService.execute(() -> {
            List<ConversationWithCharacter> data = conversationDao.getConversationsWithCharacterPaged(limit, offset);
            mainHandler.post(() -> {
                if (callback != null) {
                    callback.onDataLoaded(data);
                }
            });
        });
    }

    public void insert(Conversation conversation, InsertCallback callback) {
        executorService.execute(() -> {
            long newId = conversationDao.insert(conversation);
            if (callback != null) {
                mainHandler.post(() -> callback.onInsertFinished(newId));
            }
        });
    }

    public void insert(Conversation conversation) {
        insert(conversation, null);
    }

    public void updateLastUpdated(int conversationId, long timestamp) {
        executorService.execute(() -> conversationDao.updateLastUpdated(conversationId, timestamp));
    }

    public void updateLastUpdatedSync(int conversationId, long timestamp) {
        conversationDao.updateLastUpdated(conversationId, timestamp);
    }

    public void update(Conversation conversation) {
        executorService.execute(() -> conversationDao.update(conversation));
    }

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

    public void deleteConversations(List<Integer> ids) {
        executorService.execute(() -> conversationDao.deleteConversationsByIds(ids));
    }

    public LiveData<Conversation> getConversationById(int conversationId) {
        return conversationDao.getConversationById(conversationId);
    }

    public LiveData<List<ConversationWithCharacter>> getAllConversationsWithCharacter() {
        return conversationDao.getAllConversationsWithCharacter();
    }
}