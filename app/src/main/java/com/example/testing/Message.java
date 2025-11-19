package com.example.testing;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;
import androidx.room.ColumnInfo;
import androidx.annotation.Nullable; // Import this

@Entity(tableName = "chat_message")
public class Message {

    @PrimaryKey(autoGenerate = true)
    private int index;

    @ColumnInfo(name = "role")
    private String role;

    @Nullable // Mark as Nullable so Room handles nulls gracefully
    @ColumnInfo(name = "content")
    private String content;

    @ColumnInfo(name = "timestamp")
    private long timestamp;

    @ColumnInfo(name = "conversation_fk")
    private int conversationId;

    @Nullable // Mark as Nullable so Room handles nulls gracefully
    @ColumnInfo(name = "image_path")
    private String imagePath;

    public Message(int index, String role, String content, long timestamp, int conversationId, String imagePath) {
        this.index = index;
        this.role = role;
        this.content = content;
        this.timestamp = timestamp;
        this.conversationId = conversationId;
        this.imagePath = imagePath;
    }

    @Ignore
    public Message() {}

    @Ignore
    public Message(String role, String content, int conversationId) {
        this(role, content, conversationId, null);
    }

    @Ignore
    public Message(String role, String content, int conversationId, String imagePath) {
        this.index = 0;
        this.role = role;
        this.content = content;
        this.timestamp = System.currentTimeMillis();
        this.conversationId = conversationId;
        this.imagePath = imagePath;
    }

    public int getIndex() { return index; }
    public String getRole() { return role; }
    public String getContent() { return content; }
    public long getTimestamp() { return timestamp; }
    public int getConversationId() { return conversationId; }
    @Nullable
    public String getImagePath() { return imagePath; }

    public void setIndex(int index) { this.index = index; }
    public void setRole(String role) { this.role = role; }
    public void setContent(String content) { this.content = content; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    public void setConversationId(int conversationId) { this.conversationId = conversationId; }
    public void setImagePath(String imagePath) { this.imagePath = imagePath; }
}