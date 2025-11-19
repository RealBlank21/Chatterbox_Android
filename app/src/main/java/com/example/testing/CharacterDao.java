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

    // --- UPDATED: Main list with conversation counts ---
    @Query("SELECT c.*, COUNT(conv.conversation_id) as conversation_count " +
            "FROM character c " +
            "LEFT JOIN conversation conv ON c.character_id = conv.character_fk " +
            "WHERE c.is_hidden = 0 " +
            "GROUP BY c.character_id " +
            "ORDER BY c.is_favorite DESC, c.name ASC")
    LiveData<List<Character>> getAllCharacters();

    // --- UPDATED: Hidden list with conversation counts ---
    @Query("SELECT c.*, COUNT(conv.conversation_id) as conversation_count " +
            "FROM character c " +
            "LEFT JOIN conversation conv ON c.character_id = conv.character_fk " +
            "WHERE c.is_hidden = 1 " +
            "GROUP BY c.character_id " +
            "ORDER BY c.name ASC")
    LiveData<List<Character>> getHiddenCharacters();
}