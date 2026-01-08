package com.example.testing.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import androidx.room.OnConflictStrategy;

import com.example.testing.data.local.entity.User;

@Dao
public interface UserDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertOrUpdate(User user);

    @Update
    void update(User user);

    @Query("SELECT * FROM user_config WHERE config_id = 1")
    LiveData<User> getUser();

    // --- ADD THIS ---
    @Query("SELECT * FROM user_config WHERE config_id = 1")
    User getUserSync();
}