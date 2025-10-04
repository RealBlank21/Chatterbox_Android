package com.example.testing;

import android.app.Application;

import androidx.lifecycle.LiveData;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MessageRepository {

    private final MessageDao messageDao;
    private final ExecutorService executorService;

    public MessageRepository(Application application) {
        AppDatabase db = AppDatabase.getInstance(application);
        this.messageDao = db.messageDao();
        this.executorService = Executors.newSingleThreadExecutor();
    }

    public void insert(Message message) {
        executorService.execute(() -> messageDao.insert(message));
    }

    public void update(Message message) {
        executorService.execute(() -> messageDao.update(message));
    }

    public void delete(Message message) {
        executorService.execute(() -> messageDao.delete(message));
    }

    public LiveData<List<Message>> getMessagesForConversation(int conversationId) {
        return messageDao.getMessagesForConversation(conversationId);
    }

    public List<Message> getMessagesForConversationSync(int conversationId) {
        return messageDao.getMessagesForConversationSync(conversationId);
    }
}