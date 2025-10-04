package com.example.testing;

import android.app.Application;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ConversationRepository {

    private final ConversationDao conversationDao;
    private final ExecutorService executorService;

    public ConversationRepository(Application application) {
        AppDatabase db = AppDatabase.getInstance(application);
        this.conversationDao = db.conversationDao();
        this.executorService = Executors.newSingleThreadExecutor();
    }

    public void insert(Conversation conversation) {
        executorService.execute(() -> conversationDao.insert(conversation));
    }

    public void update(Conversation conversation) {
        executorService.execute(() -> conversationDao.update(conversation));
    }

    public void delete(Conversation conversation) {
        executorService.execute(() -> conversationDao.delete(conversation));
    }
}