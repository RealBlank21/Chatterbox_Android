package com.example.testing;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import androidx.room.Delete;
import java.util.List;

@Dao
public interface ConversationDao {

    @Insert
    long insert(Conversation conversation);

    @Update
    void update(Conversation conversation);

    @Delete
    void delete(Conversation conversation);

    @Query("SELECT * FROM conversation WHERE conversation_id = :id")
    Conversation getConversationById(int id);

    @Query("SELECT * FROM conversation WHERE character_fk = :characterId ORDER BY last_updated DESC")
    List<Conversation> getConversationsForCharacter(int characterId);
}