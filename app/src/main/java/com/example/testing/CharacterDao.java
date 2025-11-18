package com.example.testing;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import androidx.room.Delete;
import androidx.room.OnConflictStrategy; // Import this
import java.util.List;

@Dao
public interface CharacterDao {

    @Insert
    void insert(Character character);

    // --- ADD THESE METHODS ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<Character> characters);

    @Query("SELECT * FROM character")
    List<Character> getAllCharactersSync();

    @Query("DELETE FROM character")
    void deleteAll();
    // ------------------------

    @Update
    void update(Character character);

    @Delete
    void delete(Character character);

    @Query("SELECT * FROM character WHERE character_id = :id")
    LiveData<Character> getCharacterById(int id);

    @Query("SELECT * FROM character ORDER BY name ASC")
    LiveData<List<Character>> getAllCharacters();
}