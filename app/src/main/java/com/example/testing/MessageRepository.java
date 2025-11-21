package com.example.testing;

import android.app.Application;
import androidx.lifecycle.LiveData;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MessageRepository {

    private static volatile MessageRepository INSTANCE;
    private final MessageDao messageDao;
    private final ExecutorService executorService;

    private MessageRepository(Application application) {
        AppDatabase db = AppDatabase.getInstance(application);
        this.messageDao = db.messageDao();
        this.executorService = Executors.newSingleThreadExecutor();
    }

    public static MessageRepository getInstance(Application application) {
        if (INSTANCE == null) {
            synchronized (MessageRepository.class) {
                if (INSTANCE == null) {
                    INSTANCE = new MessageRepository(application);
                }
            }
        }
        return INSTANCE;
    }

    public void insert(Message message) {
        executorService.execute(() -> messageDao.insert(message));
    }

    // --- ADDED: Synchronous method for use inside other background threads ---
    public void insertSync(Message message) {
        messageDao.insert(message);
    }

    public void update(Message message) {
        executorService.execute(() -> messageDao.update(message));
    }

    public void delete(Message message) {
        executorService.execute(() -> messageDao.delete(message));
    }

    // --- ADDED: Synchronous method to prevent race conditions during regeneration ---
    public void deleteSync(Message message) {
        messageDao.delete(message);
    }

    public LiveData<List<Message>> getMessagesForConversation(int conversationId) {
        return messageDao.getMessagesForConversation(conversationId);
    }

    public List<Message> getMessagesForConversationSync(int conversationId) {
        return messageDao.getMessagesForConversationSync(conversationId);
    }
}