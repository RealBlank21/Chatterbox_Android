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
public interface MessageDao {

    @Insert
    void insert(Message message);

    // --- ADD THESE METHODS ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<Message> messages);

    @Query("SELECT * FROM chat_message")
    List<Message> getAllMessagesSync();

    @Query("DELETE FROM chat_message")
    void deleteAll();
    // ------------------------

    @Update
    void update(Message message);

    @Delete
    void delete(Message message);

    @Query("SELECT * FROM chat_message WHERE conversation_fk = :conversationId ORDER BY timestamp ASC")
    LiveData<List<Message>> getMessagesForConversation(int conversationId);

    @Query("SELECT * FROM chat_message WHERE conversation_fk = :conversationId ORDER BY timestamp ASC")
    List<Message> getMessagesForConversationSync(int conversationId);
}