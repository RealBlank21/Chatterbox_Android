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

    @Query("SELECT c.*, COUNT(conv.conversation_id) as conversation_count " +
            "FROM character c " +
            "LEFT JOIN conversation conv ON c.character_id = conv.character_fk " +
            "WHERE c.is_hidden = :isHidden " +
            "AND c.name LIKE '%' || :searchQuery || '%' " +
            "AND (:tagFilter IS NULL OR :tagFilter = '' OR c.tags LIKE '%' || :tagFilter || '%') " +
            "GROUP BY c.character_id " +
            "ORDER BY c.is_favorite DESC, c.name ASC")
    LiveData<List<Character>> getFilteredCharacters(boolean isHidden, String searchQuery, String tagFilter);

    @Query("SELECT tags FROM character WHERE tags IS NOT NULL AND tags != ''")
    LiveData<List<String>> getAllTags();
}