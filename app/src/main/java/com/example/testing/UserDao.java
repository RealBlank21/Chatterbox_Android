package com.example.testing;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import androidx.room.OnConflictStrategy;

@Dao
public interface UserDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertOrUpdate(User user);

    @Update
    void update(User user);

    @Query("SELECT * FROM user_config WHERE config_id = 1")
    LiveData<User> getUser(); // Change return type to LiveData
}