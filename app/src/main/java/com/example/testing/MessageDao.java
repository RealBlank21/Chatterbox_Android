package com.example.testing;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import androidx.room.Delete;
import java.util.List;

@Dao
public interface MessageDao {

    @Insert
    void insert(Message message);

    @Update
    void update(Message message);

    @Delete
    void delete(Message message);

    @Query("SELECT * FROM chat_message WHERE conversation_fk = :conversationId ORDER BY timestamp ASC")
    List<Message> getMessagesForConversation(int conversationId);
}