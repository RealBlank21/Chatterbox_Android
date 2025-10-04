package com.example.testing;

import android.app.Application;
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
}