package com.example.testing;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Update;
import androidx.room.Delete;
import androidx.room.OnConflictStrategy;
import java.util.List;

@Dao
public interface ConversationDao {

    @Insert
    long insert(Conversation conversation);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<Conversation> conversations);

    @Query("SELECT * FROM conversation")
    List<Conversation> getAllConversationsSync();

    @Query("DELETE FROM conversation")
    void deleteAll();

    @Update
    void update(Conversation conversation);

    @Query("UPDATE conversation SET last_updated = :timestamp WHERE conversation_id = :id")
    void updateLastUpdated(int id, long timestamp);

    @Delete
    void delete(Conversation conversation);

    @Query("DELETE FROM conversation WHERE conversation_id IN (:conversationIds)")
    void deleteConversationsByIds(List<Integer> conversationIds);

    @Query("SELECT * FROM conversation WHERE conversation_id = :id")
    LiveData<Conversation> getConversationById(int id);

    // --- NEW METHOD FOR VIEWMODEL ---
    @Query("SELECT * FROM conversation WHERE conversation_id = :id")
    Conversation getConversationByIdSync(int id);
    // --------------------------------

    @Query("SELECT * FROM conversation WHERE character_fk = :characterId ORDER BY last_updated DESC")
    List<Conversation> getConversationsForCharacter(int characterId);

    @Transaction
    @Query("SELECT conversation.*, character.name, " +
            "(SELECT COUNT(*) FROM chat_message WHERE conversation_fk = conversation.conversation_id) AS message_count " +
            "FROM conversation " +
            "INNER JOIN character ON conversation.character_fk = character.character_id " +
            "ORDER BY conversation.last_updated DESC")
    LiveData<List<ConversationWithCharacter>> getAllConversationsWithCharacter();

    @Transaction
    @Query("SELECT conversation.*, character.name, " +
            "(SELECT COUNT(*) FROM chat_message WHERE conversation_fk = conversation.conversation_id) AS message_count " +
            "FROM conversation " +
            "INNER JOIN character ON conversation.character_fk = character.character_id " +
            "ORDER BY conversation.last_updated DESC " +
            "LIMIT :limit OFFSET :offset")
    List<ConversationWithCharacter> getConversationsWithCharacterPaged(int limit, int offset);
}