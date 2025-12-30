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
    private final MessageDao messageDao;
    private final ExecutorService executorService;
    private final Handler mainHandler;

    public interface InsertCallback {
        void onInsertFinished(Long newId);
    }

    public interface DataCallback<T> {
        void onDataLoaded(T data);
    }

    private ConversationRepository(Application application) {
        AppDatabase db = AppDatabase.getInstance(application);
        this.conversationDao = db.conversationDao();
        this.messageDao = db.messageDao();
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
        executorService.execute(() -> {
            messageDao.deleteAll();
            conversationDao.deleteAll();
        });
    }

    public void delete(Conversation conversation) {
        executorService.execute(() -> {
            messageDao.deleteMessagesByConversationId(conversation.getId());
            conversationDao.delete(conversation);
        });
    }

    public void deleteConversations(List<Integer> ids) {
        executorService.execute(() -> {
            messageDao.deleteMessagesByConversationIds(ids);
            conversationDao.deleteConversationsByIds(ids);
        });
    }

    public LiveData<Conversation> getConversationById(int conversationId) {
        return conversationDao.getConversationById(conversationId);
    }

    public Conversation getConversationByIdSync(int conversationId) {
        return conversationDao.getConversationByIdSync(conversationId);
    }

    public LiveData<List<ConversationWithCharacter>> getAllConversationsWithCharacter() {
        return conversationDao.getAllConversationsWithCharacter();
    }
}