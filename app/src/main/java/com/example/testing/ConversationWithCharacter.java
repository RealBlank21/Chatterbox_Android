package com.example.testing;

import androidx.room.ColumnInfo;
import androidx.room.Embedded;

// This class will hold a Conversation object, the character name, and the message count.
public class ConversationWithCharacter {

    @Embedded
    public Conversation conversation;

    @ColumnInfo(name = "name") // From 'character' table
    public String characterName;

    // --- NEW FIELD ---
    @ColumnInfo(name = "message_count")
    public int messageCount;
}