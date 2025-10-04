package com.example.testing;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Transaction;
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
    LiveData<Conversation> getConversationById(int id); // Change return type

    @Query("SELECT * FROM conversation WHERE character_fk = :characterId ORDER BY last_updated DESC")
    List<Conversation> getConversationsForCharacter(int characterId);

    @Transaction
    @Query("SELECT conversation.*, character.name FROM conversation " +
            "INNER JOIN character ON conversation.character_fk = character.character_id " +
            "ORDER BY conversation.last_updated DESC")
    LiveData<List<ConversationWithCharacter>> getAllConversationsWithCharacter();
}