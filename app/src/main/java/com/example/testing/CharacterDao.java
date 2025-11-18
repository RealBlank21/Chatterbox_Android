package com.example.testing;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import androidx.room.Delete;
import androidx.room.OnConflictStrategy;
import java.util.List;

@Dao
public interface CharacterDao {

    @Insert
    void insert(Character character);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<Character> characters);

    @Query("SELECT * FROM character")
    List<Character> getAllCharactersSync();

    @Query("DELETE FROM character")
    void deleteAll();

    @Update
    void update(Character character);

    @Delete
    void delete(Character character);

    @Query("SELECT * FROM character WHERE character_id = :id")
    LiveData<Character> getCharacterById(int id);

    // --- UPDATED: Main list (Not hidden, Favorites first) ---
    @Query("SELECT * FROM character WHERE is_hidden = 0 ORDER BY is_favorite DESC, name ASC")
    LiveData<List<Character>> getAllCharacters();

    // --- NEW: Hidden list ---
    @Query("SELECT * FROM character WHERE is_hidden = 1 ORDER BY name ASC")
    LiveData<List<Character>> getHiddenCharacters();
}