package com.example.testing;

import androidx.lifecycle.LiveData;
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
    LiveData<List<Message>> getMessagesForConversation(int conversationId); // Change return type

    @Query("SELECT * FROM chat_message WHERE conversation_fk = :conversationId ORDER BY timestamp ASC")
    List<Message> getMessagesForConversationSync(int conversationId);
}