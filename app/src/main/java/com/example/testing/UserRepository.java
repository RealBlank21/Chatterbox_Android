package com.example.testing;

import android.app.Application;

import androidx.lifecycle.LiveData;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class UserRepository {

    private final UserDao userDao;
    private final ExecutorService executorService;

    public UserRepository(Application application) {
        AppDatabase db = AppDatabase.getInstance(application);
        this.userDao = db.userDao();
        this.executorService = Executors.newSingleThreadExecutor();
    }

    // Since there's only one user, this method inserts or replaces the existing one.
    public void insertOrUpdate(User user) {
        executorService.execute(() -> userDao.insertOrUpdate(user));
    }

    public LiveData<User> getUser() {
        return userDao.getUser();
    }
}