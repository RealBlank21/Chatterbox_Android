package com.example.testing;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;
import androidx.room.ColumnInfo;
import androidx.annotation.Nullable;

@Entity(tableName = "chat_message")
public class Message {

    @PrimaryKey(autoGenerate = true)
    private int index;

    @ColumnInfo(name = "role")
    private String role;

    @Nullable
    @ColumnInfo(name = "content")
    private String content;

    @ColumnInfo(name = "timestamp")
    private long timestamp;

    @ColumnInfo(name = "conversation_fk")
    private int conversationId;

    @Nullable
    @ColumnInfo(name = "image_path")
    private String imagePath;

    @ColumnInfo(name = "token_count", defaultValue = "0")
    private int tokenCount; // Total tokens (Input + Output)

    // --- NEW FIELDS ---
    @ColumnInfo(name = "prompt_tokens", defaultValue = "0")
    private int promptTokens; // Input

    @ColumnInfo(name = "completion_tokens", defaultValue = "0")
    private int completionTokens; // Output

    @Nullable
    @ColumnInfo(name = "finish_reason")
    private String finishReason;

    // Constructor used by Room
    public Message(int index, String role, String content, long timestamp, int conversationId, String imagePath, int tokenCount, int promptTokens, int completionTokens, String finishReason) {
        this.index = index;
        this.role = role;
        this.content = content;
        this.timestamp = timestamp;
        this.conversationId = conversationId;
        this.imagePath = imagePath;
        this.tokenCount = tokenCount;
        this.promptTokens = promptTokens;
        this.completionTokens = completionTokens;
        this.finishReason = finishReason;
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
        this.tokenCount = 0;
        this.promptTokens = 0;
        this.completionTokens = 0;
        this.finishReason = null;
    }

    // --- Getters ---
    public int getIndex() { return index; }
    public String getRole() { return role; }
    public String getContent() { return content; }
    public long getTimestamp() { return timestamp; }
    public int getConversationId() { return conversationId; }
    @Nullable
    public String getImagePath() { return imagePath; }
    public int getTokenCount() { return tokenCount; }
    public int getPromptTokens() { return promptTokens; }
    public int getCompletionTokens() { return completionTokens; }
    @Nullable
    public String getFinishReason() { return finishReason; }

    // --- Setters ---
    public void setIndex(int index) { this.index = index; }
    public void setRole(String role) { this.role = role; }
    public void setContent(String content) { this.content = content; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    public void setConversationId(int conversationId) { this.conversationId = conversationId; }
    public void setImagePath(String imagePath) { this.imagePath = imagePath; }
    public void setTokenCount(int tokenCount) { this.tokenCount = tokenCount; }
    public void setPromptTokens(int promptTokens) { this.promptTokens = promptTokens; }
    public void setCompletionTokens(int completionTokens) { this.completionTokens = completionTokens; }
    public void setFinishReason(String finishReason) { this.finishReason = finishReason; }
}