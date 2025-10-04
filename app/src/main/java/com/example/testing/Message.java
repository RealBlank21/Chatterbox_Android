package com.example.testing;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;
import androidx.room.ColumnInfo;

@Entity(tableName = "chat_message")
public class Message {

    // --- Fields ---

    @PrimaryKey(autoGenerate = true)
    private int index;

    @ColumnInfo(name = "role")
    private String role;

    @ColumnInfo(name = "content")
    private String content; // Kept mutable to allow the setter

    @ColumnInfo(name = "timestamp")
    private long timestamp;

    @ColumnInfo(name = "conversation_fk")
    private int conversationId;

    // -----------------------------------------------------------------
    // CONSTRUCTORS
    // -----------------------------------------------------------------

    // Constructor used by Room to rebuild the object from a database row
    public Message(int index, String role, String content, long timestamp, int conversationId) {
        this.index = index;
        this.role = role;
        this.content = content;
        this.timestamp = timestamp;
        this.conversationId = conversationId;
    }

    @Ignore
    public Message() {}

    // Constructor for creating a *new* message in the application
    @Ignore
    public Message(String role, String content, int conversationId) {
        this.index = 0; // Room will assign the real PK value
        this.role = role;
        this.content = content;
        this.timestamp = System.currentTimeMillis();
        this.conversationId = conversationId;
    }


    // -----------------------------------------------------------------
    // GETTERS
    // -----------------------------------------------------------------

    public int getIndex() { return index; }
    public String getRole() { return role; }
    public String getContent() { return content; }
    public long getTimestamp() { return timestamp; }
    public int getConversationId() { return conversationId; }


    // -----------------------------------------------------------------
    // SETTERS
    // -----------------------------------------------------------------

    public void setIndex(int index) { this.index = index; }
    public void setRole(String role) { this.role = role; }
    public void setContent(String content) { this.content = content; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    public void setConversationId(int conversationId) { this.conversationId = conversationId; }
}