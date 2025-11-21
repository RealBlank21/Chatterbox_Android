package com.example.testing;

import android.app.Application;
import androidx.lifecycle.LiveData;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class UserRepository {

    private static volatile UserRepository INSTANCE;
    private final UserDao userDao;
    private final ExecutorService executorService;

    private UserRepository(Application application) {
        AppDatabase db = AppDatabase.getInstance(application);
        this.userDao = db.userDao();
        this.executorService = Executors.newSingleThreadExecutor();
    }

    public static UserRepository getInstance(Application application) {
        if (INSTANCE == null) {
            synchronized (UserRepository.class) {
                if (INSTANCE == null) {
                    INSTANCE = new UserRepository(application);
                }
            }
        }
        return INSTANCE;
    }

    public void insertOrUpdate(User user) {
        executorService.execute(() -> userDao.insertOrUpdate(user));
    }

    public LiveData<User> getUser() {
        return userDao.getUser();
    }
}