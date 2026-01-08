package com.example.testing.data.local.entity;

import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "chat_message", indices = {@Index(value = "conversation_fk", name = "index_chat_message_conversation_fk")})
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

    @ColumnInfo(name = "token_count", defaultValue = "0")
    private int tokenCount;

    @ColumnInfo(name = "prompt_tokens", defaultValue = "0")
    private int promptTokens;

    @ColumnInfo(name = "completion_tokens", defaultValue = "0")
    private int completionTokens;

    @Nullable
    @ColumnInfo(name = "finish_reason")
    private String finishReason;

    public Message(int index, String role, String content, long timestamp, int conversationId, int tokenCount, int promptTokens, int completionTokens, String finishReason) {
        this.index = index;
        this.role = role;
        this.content = content;
        this.timestamp = timestamp;
        this.conversationId = conversationId;
        this.tokenCount = tokenCount;
        this.promptTokens = promptTokens;
        this.completionTokens = completionTokens;
        this.finishReason = finishReason;
    }

    @Ignore
    public Message() {}

    @Ignore
    public Message(String role, String content, int conversationId) {
        this.index = 0;
        this.role = role;
        this.content = content;
        this.timestamp = System.currentTimeMillis();
        this.conversationId = conversationId;
        this.tokenCount = 0;
        this.promptTokens = 0;
        this.completionTokens = 0;
        this.finishReason = null;
    }

    public int getIndex() { return index; }
    public String getRole() { return role; }
    public String getContent() { return content; }
    public long getTimestamp() { return timestamp; }
    public int getConversationId() { return conversationId; }
    public int getTokenCount() { return tokenCount; }
    public int getPromptTokens() { return promptTokens; }
    public int getCompletionTokens() { return completionTokens; }
    @Nullable
    public String getFinishReason() { return finishReason; }

    public void setIndex(int index) { this.index = index; }
    public void setRole(String role) { this.role = role; }
    public void setContent(String content) { this.content = content; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    public void setConversationId(int conversationId) { this.conversationId = conversationId; }
    public void setTokenCount(int tokenCount) { this.tokenCount = tokenCount; }
    public void setPromptTokens(int promptTokens) { this.promptTokens = promptTokens; }
    public void setCompletionTokens(int completionTokens) { this.completionTokens = completionTokens; }
    public void setFinishReason(String finishReason) { this.finishReason = finishReason; }
}