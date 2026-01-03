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
    long insert(Character character);

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

    @Query("SELECT " +
            "c.character_id, " +
            "c.created_at, " +
            "c.name, " +
            "c.model, " +
            "c.personality, " +
            "COALESCE(NULLIF(s.first_message, ''), c.first_message) as first_message, " +
            "c.is_active, " +
            "COALESCE(NULLIF(s.image_path, ''), c.character_profile_image_path) as character_profile_image_path, " +
            "c.voice_reference_id, " +
            "c.voice_reference_name, " +
            "c.temperature, " +
            "c.max_tokens, " +
            "c.is_favorite, " +
            "c.is_hidden, " +
            "c.is_time_aware, " +
            "c.allow_image_input, " +
            "c.context_limit, " +
            "c.tags, " +
            "c.default_scenario, " +
            "COUNT(DISTINCT conv.conversation_id) as conversation_count " +
            "FROM character c " +
            "LEFT JOIN conversation conv ON c.character_id = conv.character_fk " +
            "LEFT JOIN scenario s ON c.character_id = s.character_id AND s.is_default = 1 " +
            "WHERE c.is_hidden = :isHidden " +
            "AND c.name LIKE '%' || :searchQuery || '%' " +
            "AND (:tagFilter IS NULL OR :tagFilter = '' OR c.tags LIKE '%' || :tagFilter || '%') " +
            "GROUP BY c.character_id " +
            "ORDER BY c.is_favorite DESC, c.name ASC")
    LiveData<List<Character>> getFilteredCharacters(boolean isHidden, String searchQuery, String tagFilter);

    @Query("SELECT tags FROM character WHERE tags IS NOT NULL AND tags != ''")
    LiveData<List<String>> getAllTags();
}