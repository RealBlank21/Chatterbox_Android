package com.example.testing.data.local.entity.relations;

import androidx.room.ColumnInfo;
import androidx.room.Embedded;

import com.example.testing.data.local.entity.Conversation;

// This class will hold a Conversation object, the character name, and the message count.
public class ConversationWithCharacter {

    @Embedded
    public Conversation conversation;

    @ColumnInfo(name = "name") // From 'character' table
    public String characterName;

    @ColumnInfo(name = "message_count")
    public int messageCount;

    // --- Helper Methods to access inner Conversation fields directly ---

    public int getId() {
        return conversation != null ? conversation.getId() : -1;
    }

    public int getCharacterId() {
        return conversation != null ? conversation.getCharacterId() : -1;
    }

    public long getLastUpdated() {
        return conversation != null ? conversation.getLastUpdated() : 0;
    }

    // FIX: Use getTitle() instead of getName()
    public String getConversationName() {
        return conversation != null ? conversation.getTitle() : "";
    }
}